package service;

import dao.DatTruocDAO;
import dao.KetNoiDatabase;
import dao.ThongBaoDAO;
import model.DatTruocChiTiet;
import model.DocGia;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * so_luong_con counts usable copies physically on the shelf, including holds.
 * A book-row lock serializes every allocation, cancellation and pickup.
 * No method invents copies or sends SMTP while a database transaction is open.
 */
public class DatTruocService {
    private static final int SO_GIO_GIU_SACH = 24;
    private final DatTruocDAO dao = new DatTruocDAO();
    private final ThongBaoDAO thongBao = new ThongBaoDAO();
    private static ScheduledFuture<?> lichQuet;
    private static final ScheduledExecutorService LICH = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "dat-truoc-expiry");
        thread.setDaemon(true);
        return thread;
    });

    public record KetQuaHangDoiTraSach(int idSach, String maSach, String tenSach, int soBanHoanKho,
                                      List<DatTruocChiTiet> danhSachDenLuotMoi,
                                      List<DatTruocChiTiet> danhSachDangGiuHienTai, int soLuongConChoTiep) {
        public KetQuaHangDoiTraSach {
            danhSachDenLuotMoi = List.copyOf(danhSachDenLuotMoi);
            danhSachDangGiuHienTai = List.copyOf(danhSachDangGiuHienTai);
        }
        public boolean coNguoiCanGiuSach() {
            return !danhSachDenLuotMoi.isEmpty() || !danhSachDangGiuHienTai.isEmpty();
        }
        public DatTruocChiTiet nguoiUuTienDauTien() {
            if (!danhSachDangGiuHienTai.isEmpty()) return danhSachDangGiuHienTai.get(0);
            return danhSachDenLuotMoi.isEmpty() ? null : danhSachDenLuotMoi.get(0);
        }
    }

    private record SachGiu(String ma, String ten, int soCon, boolean hoatDong) { }
    private record BanDoc(String ma, String ten, String email, boolean duocMuon) { }

    public static synchronized void batDauTuDong() {
        if (lichQuet != null && !lichQuet.isCancelled() && !lichQuet.isDone()) return;
        lichQuet = LICH.scheduleWithFixedDelay(() -> {
            try { new DatTruocService().quetVaChuyenLuotQuaHan(); }
            catch (RuntimeException ex) { System.err.println("Không quét được đặt trước: " + ex.getMessage()); }
        }, 0, 1, TimeUnit.MINUTES);
    }

    public static synchronized void dungTuDong() {
        if (lichQuet != null) lichQuet.cancel(false);
    }

    public static void yeuCauGuiSauCommit() {
        try { ThongBaoService.getInstance().yeuCauXuLyEmailDatTruoc(); }
        catch (RuntimeException ex) {
            // Queue is durable: dispatch failure must never turn a committed checkout into failure.
            System.err.println("Email đặt trước đã lưu, sẽ xử lý ở lượt nền tiếp theo.");
        }
    }

    private SachGiu khoaSach(Connection conn, int bookId) throws SQLException {
        if (conn == null || conn.getAutoCommit()) throw new SQLException("Đặt trước phải dùng giao dịch.");
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT ma_sach, ten_sach, so_luong_con, trang_thai FROM sach WHERE id = ? FOR UPDATE
                """)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Không tìm thấy sách.");
                int remaining = rs.getInt("so_luong_con");
                if (remaining < 0) throw new SQLException("Tồn kho không hợp lệ.");
                return new SachGiu(rs.getString("ma_sach"), rs.getString("ten_sach"), remaining,
                        rs.getBoolean("trang_thai"));
            }
        }
    }

    private Map<Integer, BanDoc> docBanDoc(Connection conn, List<DatTruocChiTiet> rows) throws SQLException {
        Map<Integer, BanDoc> result = new HashMap<>();
        SortedSet<Integer> ids = new TreeSet<>();
        for (DatTruocChiTiet row : rows) ids.add(row.getIdDocGia());
        for (int id : ids) {
            // Current locking read: not an old REPEATABLE READ snapshot.
            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT ma_doc_gia, ho_ten, email, trang_thai, diem_tich_luy
                    FROM doc_gia WHERE id = ? FOR SHARE
                    """)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) result.put(id, new BanDoc(rs.getString("ma_doc_gia"),
                            rs.getString("ho_ten"), rs.getString("email"),
                            "HOAT_DONG".equals(rs.getString("trang_thai"))
                                    && rs.getInt("diem_tich_luy") >= DocGia.NGUONG_TAM_KHOA_MUON));
                }
            }
        }
        return result;
    }

    private void ketThuc(Connection conn, DatTruocChiTiet row, String state, String reason) throws SQLException {
        if (!dao.capNhatTrangThaiChiTiet(conn, row.getId(), state, null))
            throw new SQLException("Lượt đặt trước đã thay đổi.");
        thongBao.huyEmailDatTruoc(conn, row.getId(), reason);
        row.setTrangThai(state);
        row.setHanNhanSach(null);
    }

    /** Called inside borrow/return/restock transactions; reentrant on an already locked book. */
    public KetQuaHangDoiTraSach dongBoHangDoi(Connection conn, int bookId) throws SQLException {
        SachGiu book = khoaSach(conn, bookId);
        List<DatTruocChiTiet> rows = dao.layCacDongHoatDongCoKhoa(conn, bookId);
        Map<Integer, BanDoc> readers = docBanDoc(conn, rows);
        Set<Integer> expired = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT id FROM dat_truoc WHERE id_sach = ? AND trang_thai = 'DA_THONG_BAO'
                AND (han_nhan_sach IS NULL OR han_nhan_sach < NOW()) FOR UPDATE
                """)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) expired.add(rs.getInt(1)); }
        }
        List<DatTruocChiTiet> held = new ArrayList<>();
        for (DatTruocChiTiet row : rows) {
            BanDoc reader = readers.get(row.getIdDocGia());
            if (!book.hoatDong() || reader == null || !reader.duocMuon()) {
                ketThuc(conn, row, "DA_HUY", "Sách/thẻ không còn phục vụ mượn.");
            } else if (expired.contains(row.getId())) {
                ketThuc(conn, row, "HET_HAN", "Đã hết thời hạn nhận sách.");
            } else if ("DA_THONG_BAO".equals(row.getTrangThai())) {
                // Repair pre-existing over-allocation without manufacturing inventory.
                if (held.size() < book.soCon()) held.add(row);
                else ketThuc(conn, row, "DA_HUY", "Không còn đủ bản trên kệ để giữ.");
            }
        }
        List<DatTruocChiTiet> promoted = new ArrayList<>();
        int free = book.hoatDong() ? book.soCon() - held.size() : 0;
        int waiting = 0;
        for (DatTruocChiTiet row : rows) {
            if (!"DANG_CHO".equals(row.getTrangThai())) continue;
            if (free <= 0) { waiting++; continue; }
            try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE dat_truoc SET trang_thai = 'DA_THONG_BAO',
                        han_nhan_sach = DATE_ADD(NOW(), INTERVAL 24 HOUR)
                    WHERE id = ? AND trang_thai = 'DANG_CHO'
                    """)) {
                ps.setInt(1, row.getId());
                if (ps.executeUpdate() != 1) throw new SQLException("Không giữ được sách cho hàng đợi.");
            }
            DatTruocChiTiet fresh = dao.getChiTietById(conn, row.getId());
            BanDoc reader = readers.get(row.getIdDocGia());
            row.setTrangThai("DA_THONG_BAO");
            row.setHanNhanSach(fresh.getHanNhanSach());
            row.setMaDocGia(reader.ma()); row.setTenDocGia(reader.ten()); row.setEmailDocGia(reader.email());
            row.setMaSach(book.ma()); row.setTenSach(book.ten());
            String body = "Chào " + reader.ten() + ", thư viện đang giữ 1 bản sách '" + book.ten()
                    + "' cho bạn. Mã đặt: " + row.getMaDatTruoc() + ". Hạn nhận: " + row.getHanNhanSach()
                    + ". Khi hết hạn, hệ thống sẽ chuyển cho người tiếp theo.";
            thongBao.taoThongBao(conn, row.getIdDocGia(), "DEN_LUOT_DAT_TRUOC", body, "SYSTEM", "DA_GUI");
            thongBao.taoEmailDatTruoc(conn, row.getId(), row.getIdDocGia(), reader.ma(), reader.email(),
                    "Đến lượt nhận sách đặt trước: " + book.ten(), body);
            promoted.add(row);
            free--;
        }
        for (DatTruocChiTiet row : held) {
            BanDoc reader = readers.get(row.getIdDocGia());
            row.setMaDocGia(reader.ma()); row.setTenDocGia(reader.ten()); row.setEmailDocGia(reader.email());
            row.setMaSach(book.ma()); row.setTenSach(book.ten());
        }
        return new KetQuaHangDoiTraSach(bookId, book.ma(), book.ten(), 0, promoted, held, waiting);
    }

    /** Hard FIFO check and reservation pickup, BEFORE deducting physical shelf stock. */
    public void chuanBiMuonTrongGiaoDich(Connection conn, int readerId, int bookId, int quantity) throws SQLException {
        KetQuaHangDoiTraSach state = dongBoHangDoi(conn, bookId);
        SachGiu book = khoaSach(conn, bookId);
        List<DatTruocChiTiet> holds = new ArrayList<>(state.danhSachDangGiuHienTai());
        holds.addAll(state.danhSachDenLuotMoi());
        int others = 0;
        for (DatTruocChiTiet row : holds) if (row.getIdDocGia() != readerId) others++;
        if (quantity <= 0 || quantity > book.soCon() - others)
            throw new SQLException("Sách đang được giữ cho người khác theo FIFO; không đủ bản trống.");
        int consumed = 0;
        for (DatTruocChiTiet row : holds) {
            if (consumed == quantity) break;
            if (row.getIdDocGia() == readerId) {
                ketThuc(conn, row, "DA_NHAN", "Độc giả đã nhận sách trong giao dịch mượn.");
                consumed++;
            }
        }
    }

    public String dangKyDatTruoc(int readerId, Map<Integer, Integer> requested) {
        if (readerId <= 0 || requested == null || requested.isEmpty()) return "Thiếu độc giả hoặc sách đặt trước.";
        for (Map.Entry<Integer, Integer> e : requested.entrySet())
            if (e.getKey() == null || e.getKey() <= 0 || e.getValue() == null || e.getValue() <= 0 || e.getValue() > 999)
                return "ID và số lượng đặt trước phải hợp lệ (1–999 bản).";
        SortedMap<Integer, Integer> books = new TreeMap<>(requested);
        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich()) {
            conn.setAutoCommit(false);
            try {
                // Stable book order for multi-book reservations.
                for (int id : books.keySet()) khoaSach(conn, id);
                for (int id : books.keySet()) dongBoHangDoi(conn, id);
                try (PreparedStatement ps = conn.prepareStatement("""
                        SELECT trang_thai, diem_tich_luy FROM doc_gia WHERE id = ? FOR UPDATE
                        """)) {
                    ps.setInt(1, readerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || !"HOAT_DONG".equals(rs.getString(1))
                                || rs.getInt(2) < DocGia.NGUONG_TAM_KHOA_MUON)
                            throw new SQLException("Thẻ độc giả không được phép đặt trước.");
                    }
                }
                for (int id : books.keySet()) {
                    SachGiu book = khoaSach(conn, id);
                    if (!book.hoatDong()) throw new SQLException("Sách đang ngừng phục vụ.");
                    int held = dao.getDangGiuConHanTheoSach(conn, id).size();
                    if (book.soCon() > held) throw new SQLException("Sách còn bản trống; hãy lập phiếu mượn trực tiếp.");
                }
                dao.taoDatTruocNhieuSach(conn, readerId, books);
                conn.commit();
            } catch (SQLException | RuntimeException ex) {
                conn.rollback(); throw ex;
            }
            yeuCauGuiSauCommit();
            return "Đặt trước thành công! Các bản sẽ được giữ theo hàng đợi FIFO.";
        } catch (SQLException | RuntimeException ex) {
            return "Đặt trước thất bại: " + ex.getMessage();
        }
    }

    public boolean huyVaChuyenLuotTiepTheo(int reservationId, boolean expiredOnly) {
        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich()) {
            int bookId;
            try (PreparedStatement ps = conn.prepareStatement("SELECT id_sach FROM dat_truoc WHERE id = ?")) {
                ps.setInt(1, reservationId);
                try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) return false; bookId = rs.getInt(1); }
            }
            conn.setAutoCommit(false);
            try {
                khoaSach(conn, bookId);
                DatTruocChiTiet row = dao.getChiTietById(conn, reservationId);
                if (row == null || !List.of("DANG_CHO", "DA_THONG_BAO").contains(row.getTrangThai())) {
                    conn.rollback(); return false;
                }
                if (expiredOnly) {
                    try (PreparedStatement ps = conn.prepareStatement("""
                            SELECT id FROM dat_truoc WHERE id = ? AND trang_thai = 'DA_THONG_BAO'
                              AND (han_nhan_sach IS NULL OR han_nhan_sach < NOW()) FOR UPDATE
                            """)) {
                        ps.setInt(1, reservationId);
                        try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) { conn.rollback(); return false; } }
                    }
                }
                ketThuc(conn, row, expiredOnly ? "HET_HAN" : "DA_HUY", "Đã hủy/hết hạn nhận sách.");
                dongBoHangDoi(conn, bookId);
                conn.commit();
            } catch (SQLException | RuntimeException ex) { conn.rollback(); throw ex; }
            yeuCauGuiSauCommit();
            return true;
        } catch (SQLException | RuntimeException ex) {
            System.err.println("Không hủy được đặt trước: " + ex.getMessage()); return false;
        }
    }

    public void quetVaChuyenLuotQuaHan() {
        try {
            for (int bookId : dao.laySachCoHangDoi()) {
                try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich()) {
                    conn.setAutoCommit(false);
                    try { dongBoHangDoi(conn, bookId); conn.commit(); }
                    catch (SQLException | RuntimeException ex) { conn.rollback(); throw ex; }
                } catch (SQLException | RuntimeException ex) {
                    System.err.println("Không đồng bộ được hàng đợi sách #" + bookId + ": " + ex.getMessage());
                }
            }
            yeuCauGuiSauCommit();
        } catch (SQLException ex) { throw new IllegalStateException("Không đọc được hàng đợi đặt trước.", ex); }
    }

    /** Read-only postcommit display; allocation was already part of checkout. */
    public KetQuaHangDoiTraSach layTinhTrangHangDoi(int bookId, int returned) {
        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich()) {
            String code = "", name = "Sách #" + bookId;
            try (PreparedStatement ps = conn.prepareStatement("SELECT ma_sach, ten_sach FROM sach WHERE id = ?")) {
                ps.setInt(1, bookId);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) { code = rs.getString(1); name = rs.getString(2); } }
            }
            return new KetQuaHangDoiTraSach(bookId, code, name, returned, List.of(),
                    dao.getDangGiuConHanTheoSach(conn, bookId), dao.demDangChoTheoSach(conn, bookId));
        } catch (SQLException ex) {
            System.err.println("Giao dịch đã thành công; không tải được thông tin hàng đợi.");
            return new KetQuaHangDoiTraSach(bookId, "", "", returned, List.of(), List.of(), 0);
        }
    }

    /** No stock ingress may be simulated via reservations. Use the stock editor or actual return. */
    @Deprecated
    public void xuLyKhiCoSachVe(Connection conn, int idSach, int soLuongVe) throws SQLException {
        throw new SQLFeatureNotSupportedException("Không hỗ trợ nhập kho giả lập. Hãy cập nhật kho hoặc nhận sách trả.");
    }
}
