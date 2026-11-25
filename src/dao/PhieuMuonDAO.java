package dao;

import model.ChiTietPhieuMuon;
import model.DocGia;
import model.PhieuMuon;
import model.Voucher;
import service.RewardService;
import service.DatTruocService;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PhieuMuonDAO {
    private static final ZoneId MUI_GIO = ZoneId.of("Asia/Ho_Chi_Minh");

    public record KetQuaLapPhieuMuon(
            int idPhieu,
            String maPhieu,
            long donGiaThue,
            long tongTienThue,
            long giamGiaVoucher,
            long thucThu,
            int diemCong,
            String maVoucherApDung,
            LocalDateTime ngayLap) {
        public long thanhTien() {
            return thucThu;
        }
    }

    // ================= DANH SÁCH PHIẾU MƯỢN =================

    public List<PhieuMuon> layPhieuDangMuon() {
        return layDanhSachPhieu(true);
    }

    public List<PhieuMuon> layTatCaPhieuMuon() {
        return layDanhSachPhieu(false);
    }

    private List<PhieuMuon> layDanhSachPhieu(boolean chiDangMuon) {
        List<PhieuMuon> danhSach = new ArrayList<>();

        String sql = """
                SELECT p.*,
                       dg.ho_ten AS ten_doc_gia,
                       nd.ho_ten AS ten_thu_thu
                FROM phieu_muon p
                JOIN doc_gia dg ON p.id_doc_gia = dg.id
                LEFT JOIN nguoi_dung nd ON p.id_thu_thu = nd.id
                """
                + (chiDangMuon
                ? "WHERE p.trang_thai = 'DANG_MUON' "
                : "")
                + "ORDER BY p.id DESC";

        try (
                Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                danhSach.add(mapPhieuMuon(rs));
            }
        } catch (SQLException e) {
            System.err.println(
                    "Lỗi lấy danh sách phiếu mượn: " + e.getMessage()
            );
        }

        return danhSach;
    }

    private PhieuMuon mapPhieuMuon(ResultSet rs) throws SQLException {
        PhieuMuon pm = new PhieuMuon();

        pm.setId(rs.getInt("id"));
        pm.setMaPhieu(rs.getString("ma_phieu"));
        pm.setIdDocGia(rs.getInt("id_doc_gia"));

        // Database dùng id_thu_thu.
        // Model hiện tại vẫn dùng tên idNhanVien.
        pm.setIdNhanVien(rs.getInt("id_thu_thu"));

        pm.setNgayMuon(rs.getTimestamp("ngay_muon"));
        pm.setTrangThai(rs.getString("trang_thai"));
        pm.setTenDocGia(rs.getString("ten_doc_gia"));
        pm.setTenNhanVien(rs.getString("ten_thu_thu"));

        int idVoucher = rs.getInt("id_voucher");
        pm.setIdVoucher(rs.wasNull() ? null : idVoucher);
        pm.setMaVoucherApDung(rs.getString("ma_voucher_ap_dung"));
        pm.setTongTienThue(Math.round(rs.getDouble("tong_tien_thue")));
        pm.setGiamGiaVoucher(Math.round(rs.getDouble("giam_gia_voucher")));
        pm.setThanhTien(Math.round(rs.getDouble("thanh_tien")));
        pm.setDiemCong(rs.getInt("diem_cong"));

        return pm;
    }

    // ================= CHI TIẾT PHIẾU MƯỢN =================

    public List<ChiTietPhieuMuon> layChiTietPhieuMuon(int idPhieuMuon) {
        List<ChiTietPhieuMuon> danhSach = new ArrayList<>();

        String sql = """
                SELECT ct.*, s.ma_sach, s.ten_sach
                FROM chi_tiet_phieu_muon ct
                JOIN sach s ON ct.id_sach = s.id
                WHERE ct.id_phieu_muon = ?
                """;

        try (
                Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, idPhieuMuon);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ChiTietPhieuMuon ct = new ChiTietPhieuMuon();

                    ct.setId(rs.getInt("id"));
                    ct.setIdPhieuMuon(rs.getInt("id_phieu_muon"));
                    ct.setIdSach(rs.getInt("id_sach"));

                    ct.setMaSach(rs.getString("ma_sach"));
                    ct.setTenSach(rs.getString("ten_sach"));

                    ct.setSoLuong(rs.getInt("so_luong"));
                    ct.setHanTra(rs.getDate("han_tra"));
                    ct.setNgayTra(rs.getDate("ngay_tra"));

                    ct.setTrangThai(rs.getString("trang_thai"));
                    ct.setSoNgayQuaHan(rs.getInt("so_ngay_qua_han"));

                    ct.setTinhTrangTra(rs.getString("tinh_trang_tra"));
                    ct.setGhiChuTra(rs.getString("ghi_chu_tra"));
                    ct.setDonGiaThue(Math.round(rs.getDouble("don_gia_thue")));

                    danhSach.add(ct);
                }
            }
        } catch (SQLException e) {
            System.err.println(
                    "Lỗi lấy chi tiết phiếu mượn: " + e.getMessage()
            );
        }

        return danhSach;
    }

    // ================= KIỂM TRA ĐỘC GIẢ =================

    public boolean kiemTraDocGiaHopLe(int idDocGia) {
        if (idDocGia <= 0) {
            return false;
        }

        String sql = """
                SELECT 1
                FROM doc_gia
                WHERE id = ?
                  AND trang_thai = 'HOAT_DONG'
                """;

        try (
                Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, idDocGia);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println(
                    "Lỗi kiểm tra độc giả: " + e.getMessage()
            );
            return false;
        }
    }

    public boolean kiemTraDocGiaHopLe(
            Connection connGiaoDich,
            int idDocGia
    ) throws SQLException {
        if (idDocGia <= 0) {
            return false;
        }

        String sql = """
                SELECT diem_tich_luy
                FROM doc_gia
                WHERE id = ?
                  AND trang_thai = 'HOAT_DONG'
                FOR UPDATE
                """;

        try (PreparedStatement ps = connGiaoDich.prepareStatement(sql)) {
            ps.setInt(1, idDocGia);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                int diemTichLuy = rs.getInt("diem_tich_luy");
                if (diemTichLuy < DocGia.NGUONG_TAM_KHOA_MUON) {
                    throw new SQLException(
                            "Độc giả đang bị tạm khóa quyền mượn do điểm tích lũy ("
                                    + diemTichLuy + ") thấp hơn " + DocGia.NGUONG_TAM_KHOA_MUON + ".");
                }
                return true;
            }
        }
    }

    // ================= MƯỢN SÁCH =================

    public boolean muonSach(
            int idDocGia,
            int idThuThu,
            int idSach,
            int soLuong,
            LocalDate hanTra
    ) {
        return muonSachCoThanhToan(idDocGia, idThuThu, idSach, soLuong, hanTra, null) != null;
    }

    public KetQuaLapPhieuMuon muonSachCoThanhToan(
            int idDocGia,
            int idThuThu,
            int idSach,
            int soLuong,
            LocalDate hanTra,
            Integer idVoucher
    ) {
        if (idDocGia <= 0
                || idThuThu <= 0
                || idSach <= 0
                || soLuong <= 0
                || hanTra == null
                || hanTra.isBefore(LocalDate.now(MUI_GIO))) {
            System.err.println("Dữ liệu mượn sách không hợp lệ.");
            return null;
        }

        try (
                Connection giaoDich =
                        KetNoiDatabase.taoKetNoiChoGiaoDich()
        ) {
            giaoDich.setAutoCommit(false);

            try {
                long donGiaThue;
                String sqlKhoaSach = """
                        SELECT gia_thue, so_luong_con, trang_thai
                        FROM sach
                        WHERE id = ?
                        FOR UPDATE
                        """;
                try (PreparedStatement ps = giaoDich.prepareStatement(sqlKhoaSach)) {
                    ps.setInt(1, idSach);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || !rs.getBoolean("trang_thai") || rs.getInt("so_luong_con") < soLuong) {
                            throw new SQLException(
                                    "Sách không tồn tại, ngừng sử dụng hoặc không đủ số lượng."
                            );
                        }
                        donGiaThue = Math.max(0L, Math.round(rs.getDouble("gia_thue")));
                    }
                }

                // Book lock first; then allocate/check FIFO and consume the caller's holds.
                // Reservation pickup rolls back together with stock, voucher and loan.
                new DatTruocService().chuanBiMuonTrongGiaoDich(giaoDich, idDocGia, idSach, soLuong);

                if (!kiemTraDocGiaHopLe(giaoDich, idDocGia)) {
                    throw new SQLException(
                            "Độc giả không tồn tại hoặc đang bị khóa."
                    );
                }

                String sqlThuThu = """
                        SELECT 1
                        FROM nguoi_dung
                        WHERE id = ?
                          AND trang_thai = 1
                          AND vai_tro IN ('ADMIN', 'THU_THU')
                        FOR UPDATE
                        """;

                try (
                        PreparedStatement ps =
                                giaoDich.prepareStatement(sqlThuThu)
                ) {
                    ps.setInt(1, idThuThu);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "Tài khoản không có quyền lập phiếu "
                                            + "hoặc đang bị khóa."
                            );
                        }
                    }
                }

                String sqlTruKho = """
                        UPDATE sach
                        SET so_luong_con = so_luong_con - ?
                        WHERE id = ?
                          AND trang_thai = 1
                          AND so_luong_con >= ?
                        """;

                try (
                        PreparedStatement ps =
                                giaoDich.prepareStatement(sqlTruKho)
                ) {
                    ps.setInt(1, soLuong);
                    ps.setInt(2, idSach);
                    ps.setInt(3, soLuong);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "Sách không tồn tại, ngừng sử dụng "
                                        + "hoặc không đủ số lượng."
                        );
                    }
                }

                long tongTienThue = donGiaThue * soLuong;
                long giamGiaVoucher = 0L;
                Integer idVoucherLuu = null;
                String maVoucherApDung = null;

                if (idVoucher != null && idVoucher > 0) {
                    VoucherDAO voucherDAO = new VoucherDAO();
                    Voucher voucher = voucherDAO.khoaVaLayVoucherHopLe(giaoDich, idVoucher, idDocGia);
                    if (voucher == null) {
                        throw new SQLException("Mã Voucher không hợp lệ, đã sử dụng hoặc không tồn tại.");
                    }
                    giamGiaVoucher = voucher.tinhSoTienGiam(tongTienThue);
                    idVoucherLuu = voucher.getId();
                    maVoucherApDung = voucher.getMaVoucher();
                    voucherDAO.danhDauDaSuDung(giaoDich, voucher.getId());
                }

                long thanhTien = Math.max(0L, tongTienThue - giamGiaVoucher);
                int diemCong = soLuong * RewardService.DIEM_CONG_MOI_CUON_MUON;

                LocalDateTime ngayLap = LocalDateTime.now(MUI_GIO).withNano(0);

                String sqlPhieu = """
                        INSERT INTO phieu_muon
                            (ma_phieu, id_doc_gia, id_thu_thu, trang_thai,
                             id_voucher, ma_voucher_ap_dung, tong_tien_thue, giam_gia_voucher, thanh_tien, diem_cong, ngay_muon)
                        VALUES (?, ?, ?, 'DANG_MUON', ?, ?, ?, ?, ?, ?, ?)
                        """;

                int idPhieu;

                try (
                        PreparedStatement ps = giaoDich.prepareStatement(
                                sqlPhieu,
                                Statement.RETURN_GENERATED_KEYS
                        )
                ) {
                    // Mã tạm chỉ tồn tại trong giao dịch, cho tới khi MySQL cấp ID.
                    ps.setString(1, UUID.randomUUID().toString().substring(0, 30));
                    ps.setInt(2, idDocGia);
                    ps.setInt(3, idThuThu);
                    if (idVoucherLuu == null) {
                        ps.setNull(4, Types.INTEGER);
                    } else {
                        ps.setInt(4, idVoucherLuu);
                    }
                    ps.setString(5, maVoucherApDung);
                    ps.setBigDecimal(6, BigDecimal.valueOf(tongTienThue));
                    ps.setBigDecimal(7, BigDecimal.valueOf(giamGiaVoucher));
                    ps.setBigDecimal(8, BigDecimal.valueOf(thanhTien));
                    ps.setInt(9, diemCong);
                    ps.setObject(10, ngayLap);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "Không tạo được phiếu mượn."
                        );
                    }

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "Không lấy được ID phiếu mượn."
                            );
                        }

                        idPhieu = rs.getInt(1);
                    }
                }

                String maPhieu = String.format(Locale.ROOT, "PM-%s-%06d",
                        ngayLap.toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE), idPhieu);
                capNhatMotDong(giaoDich, "UPDATE phieu_muon SET ma_phieu = ? WHERE id = ?", maPhieu, idPhieu);

                String sqlChiTiet = """
                        INSERT INTO chi_tiet_phieu_muon
                            (id_phieu_muon, id_sach,
                             so_luong, han_tra, trang_thai, don_gia_thue)
                        VALUES (?, ?, ?, ?, 'DANG_MUON', ?)
                        """;

                try (
                        PreparedStatement ps =
                                giaoDich.prepareStatement(sqlChiTiet)
                ) {
                    ps.setInt(1, idPhieu);
                    ps.setInt(2, idSach);
                    ps.setInt(3, soLuong);
                    ps.setDate(4, java.sql.Date.valueOf(hanTra));
                    ps.setBigDecimal(5, BigDecimal.valueOf(donGiaThue));

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "Không tạo được chi tiết phiếu mượn."
                        );
                    }
                }

                if (diemCong > 0) {
                    new DocGiaDAO().congTruDiem(giaoDich, idDocGia, diemCong);
                    new DiemThuongDAO().themLichSu(
                            giaoDich,
                            idDocGia,
                            diemCong,
                            "CONG",
                            "Tích điểm mượn " + soLuong + " cuốn (+10 điểm/cuốn • Phiếu " + maPhieu + ")");
                }

                giaoDich.commit();
                DatTruocService.yeuCauGuiSauCommit();

                System.out.println(
                        "Mượn thành công! Mã phiếu: " + maPhieu
                );
                return new KetQuaLapPhieuMuon(
                        idPhieu,
                        maPhieu,
                        donGiaThue,
                        tongTienThue,
                        giamGiaVoucher,
                        thanhTien,
                        diemCong,
                        maVoucherApDung,
                        ngayLap);

            } catch (SQLException | RuntimeException e) {
                try {
                    giaoDich.rollback();
                } catch (SQLException loiRollback) {
                    e.addSuppressed(loiRollback);
                }

                System.err.println(
                        "Mượn sách thất bại: " + e.getMessage()
                );
                return null;
            }

        } catch (SQLException e) {
            System.err.println(
                    "Lỗi kết nối giao dịch mượn: " + e.getMessage()
            );
            return null;
        }
    }

    // ================= TRẢ SÁCH =================

    /*
     * Giữ để tương thích giao diện hiện tại.
     * Hàm này xử lý trả bình thường cho toàn bộ dòng.
     *
     * Khi cập nhật giao diện, dùng overload bốn tham số bên dưới
     * để truyền số bản đã xác nhận, tình trạng và ghi chú.
     */
    public boolean traSach(int idChiTietPhieuMuon) {
        return xuLyTraSach(
                idChiTietPhieuMuon,
                null,
                "BINH_THUONG",
                ""
        );
    }

    public boolean traSach(
            int idChiTietPhieuMuon,
            int soLuongDaXacNhan,
            String tinhTrang,
            String ghiChu
    ) {
        return xuLyTraSach(
                idChiTietPhieuMuon,
                soLuongDaXacNhan,
                tinhTrang,
                ghiChu
        );
    }

    private boolean xuLyTraSach(
            int idChiTiet,
            Integer soLuongDaXacNhan,
            String tinhTrang,
            String ghiChu
    ) {
        if (idChiTiet <= 0
                || (soLuongDaXacNhan != null
                && soLuongDaXacNhan <= 0)
                || tinhTrang == null
                || !List.of("BINH_THUONG", "LAM_HONG", "LAM_MAT")
                .contains(tinhTrang)) {
            System.err.println("Dữ liệu trả sách không hợp lệ.");
            return false;
        }

        String ghiChuSach = ghiChu == null ? "" : ghiChu.trim();

        if (ghiChuSach.length() > 2000
                || (!"BINH_THUONG".equals(tinhTrang)
                && ghiChuSach.isEmpty())) {
            System.err.println(
                    "Ghi chú tối đa 2.000 ký tự "
                            + "và bắt buộc khi hỏng/mất."
            );
            return false;
        }

        try (
                Connection giaoDich =
                        KetNoiDatabase.taoKetNoiChoGiaoDich()
        ) {
            giaoDich.setAutoCommit(false);

            try {
                // 1. Xác định phiếu chứa dòng mượn.
                int idPhieu;

                try (
                        PreparedStatement ps = giaoDich.prepareStatement("""
                                SELECT id_phieu_muon
                                FROM chi_tiet_phieu_muon
                                WHERE id = ?
                                """)
                ) {
                    ps.setInt(1, idChiTiet);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "Không tìm thấy dòng mượn."
                            );
                        }

                        idPhieu = rs.getInt("id_phieu_muon");
                    }
                }

                // 2. Khóa phiếu trước để các dòng cùng phiếu
                // không được kết thúc đồng thời ngoài kiểm soát.
                int idDocGia;
                String maPhieuHienTai;

                try (
                        PreparedStatement ps = giaoDich.prepareStatement("""
                                SELECT id_doc_gia, ma_phieu
                                FROM phieu_muon
                                WHERE id = ?
                                FOR UPDATE
                                """)
                ) {
                    ps.setInt(1, idPhieu);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "Không tìm thấy phiếu mượn."
                            );
                        }

                        idDocGia = rs.getInt("id_doc_gia");
                        maPhieuHienTai = rs.getString("ma_phieu");
                    }
                }

                // 3. Khóa và đọc lại dòng mượn hiện tại.
                ChiTietPhieuMuon ct = new ChiTietPhieuMuon();

                try (
                        PreparedStatement ps = giaoDich.prepareStatement("""
                                SELECT *
                                FROM chi_tiet_phieu_muon
                                WHERE id = ?
                                  AND id_phieu_muon = ?
                                FOR UPDATE
                                """)
                ) {
                    ps.setInt(1, idChiTiet);
                    ps.setInt(2, idPhieu);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "Dòng mượn đã thay đổi hoặc không tồn tại."
                            );
                        }

                        if (rs.getDate("ngay_tra") != null
                                || !"DANG_MUON".equals(
                                rs.getString("trang_thai"))) {
                            throw new SQLException(
                                    "Dòng mượn này đã được xử lý."
                            );
                        }

                        int soLuong = rs.getInt("so_luong");
                        java.sql.Date hanTra = rs.getDate("han_tra");

                        if (soLuong <= 0 || hanTra == null) {
                            throw new SQLException(
                                    "Số lượng hoặc hạn trả không hợp lệ."
                            );
                        }

                        if (soLuongDaXacNhan != null
                                && soLuong != soLuongDaXacNhan) {
                            throw new SQLException(
                                    "Số bản đã thay đổi; hãy làm mới "
                                            + "và xác nhận lại."
                            );
                        }

                        ct.setId(idChiTiet);
                        ct.setIdPhieuMuon(idPhieu);
                        ct.setIdSach(rs.getInt("id_sach"));
                        ct.setSoLuong(soLuong);
                        ct.setHanTra(hanTra);
                    }
                }

                // Serialize inventory with reservation allocation before updating reader points.
                try (PreparedStatement ps = giaoDich.prepareStatement(
                        "SELECT id FROM sach WHERE id = ? FOR UPDATE")) {
                    ps.setInt(1, ct.getIdSach());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Không tìm thấy sách của dòng mượn.");
                    }
                }

                // 4. Tính ngày kết thúc và trạng thái.
                // Với sách mất, đây là ngày chốt mất.
                LocalDate ngayChot = LocalDate.now(MUI_GIO);

                ct.setNgayTra(java.sql.Date.valueOf(ngayChot));
                ct.setTinhTrangTra(tinhTrang);
                ct.setGhiChuTra(ghiChuSach);

                String trangThai = ct.xacDinhTrangThai(ngayChot);
                int soNgayMuon = ct.tinhSoNgayQuaHan(ngayChot);
                int soLuong = ct.getSoLuong();

                // 5. Lưu lịch sử, tình trạng và ghi chú.
                capNhatMotDong(
                        giaoDich,
                        """
                        UPDATE chi_tiet_phieu_muon
                        SET ngay_tra = ?,
                            trang_thai = ?,
                            so_ngay_qua_han = ?,
                            tinh_trang_tra = ?,
                            ghi_chu_tra = ?
                        WHERE id = ?
                          AND ngay_tra IS NULL
                          AND trang_thai = 'DANG_MUON'
                        """,
                        java.sql.Date.valueOf(ngayChot),
                        trangThai,
                        soNgayMuon,
                        tinhTrang,
                        ghiChuSach,
                        idChiTiet
                );

                // 6. Ghi vi phạm và tăng bộ đếm cùng giao dịch.
                ViPhamDAO viPhamDAO = new ViPhamDAO();

                if (soNgayMuon > 0) {
                    viPhamDAO.ghiNhanViPham(
                            giaoDich,
                            idDocGia,
                            idChiTiet,
                            "TRA_MUON",
                            soNgayMuon,
                            "Kết thúc dòng mượn muộn "
                                    + soNgayMuon
                                    + " ngày; số bản: "
                                    + soLuong
                                    + ". "
                                    + ghiChuSach
                    );
                }

                if (!"BINH_THUONG".equals(tinhTrang)) {
                    viPhamDAO.ghiNhanViPham(
                            giaoDich,
                            idDocGia,
                            idChiTiet,
                            tinhTrang,
                            0,
                            "Số bản: " + soLuong + ". " + ghiChuSach
                    );
                }

                // 7. Cập nhật kho.
                if ("BINH_THUONG".equals(tinhTrang)) {
                    capNhatMotDong(
                            giaoDich,
                            """
                            UPDATE sach
                            SET so_luong_con = so_luong_con + ?
                            WHERE id = ?
                              AND so_luong_con >= 0
                              AND so_luong_con + ? <= so_luong_tong
                            """,
                            soLuong,
                            ct.getIdSach(),
                            soLuong
                    );
                } else {
                    /*
                     * Hỏng/mất: không cộng số còn.
                     * Loại số bản này khỏi tổng kho phục vụ mượn.
                     */
                    capNhatMotDong(
                            giaoDich,
                            """
                            UPDATE sach
                            SET so_luong_tong = so_luong_tong - ?
                            WHERE id = ?
                              AND so_luong_con >= 0
                              AND so_luong_tong >= ?
                              AND so_luong_tong - ? >= so_luong_con
                            """,
                            soLuong,
                            ct.getIdSach(),
                            soLuong,
                            soLuong
                    );
                }

                // Returned stock is allocated to FIFO holds inside THIS same transaction.
                new DatTruocService().dongBoHangDoi(giaoDich, ct.getIdSach());

                /*
                 * 8. Đọc có khóa để kiểm tra dữ liệu hiện tại,
                 * kể cả khi giao dịch có snapshot từ truy vấn đầu.
                 */
                boolean conChuaTra;

                try (
                        PreparedStatement ps = giaoDich.prepareStatement("""
                                SELECT id
                                FROM chi_tiet_phieu_muon
                                WHERE id_phieu_muon = ?
                                  AND ngay_tra IS NULL
                                LIMIT 1
                                FOR UPDATE
                                """)
                ) {
                    ps.setInt(1, idPhieu);

                    try (ResultSet rs = ps.executeQuery()) {
                        conChuaTra = rs.next();
                    }
                }

                if (!conChuaTra) {
                    capNhatMotDong(
                            giaoDich,
                            """
                            UPDATE phieu_muon
                            SET trang_thai = 'DA_HOAN_THANH'
                            WHERE id = ?
                            """,
                            idPhieu
                    );

                    boolean coLoiTrongPhieu;
                    try (PreparedStatement psCheck = giaoDich.prepareStatement("""
                            SELECT 1
                            FROM chi_tiet_phieu_muon
                            WHERE id_phieu_muon = ?
                              AND (so_ngay_qua_han > 0
                                   OR (tinh_trang_tra IS NOT NULL AND tinh_trang_tra <> 'BINH_THUONG'))
                            LIMIT 1 FOR UPDATE
                            """)) {
                        psCheck.setInt(1, idPhieu);
                        try (ResultSet rsCheck = psCheck.executeQuery()) {
                            coLoiTrongPhieu = rsCheck.next();
                        }
                    }

                    if (!coLoiTrongPhieu) {
                        int diemHoanTat = RewardService.DIEM_CONG_HOAN_TAT_PHIEU_DUNG_HAN;
                        new DocGiaDAO().congTruDiem(giaoDich, idDocGia, diemHoanTat);
                        capNhatMotDong(
                                giaoDich,
                                "UPDATE phieu_muon SET diem_cong = diem_cong + ? WHERE id = ?",
                                diemHoanTat,
                                idPhieu
                        );
                        new DiemThuongDAO().themLichSu(
                                giaoDich,
                                idDocGia,
                                diemHoanTat,
                                "CONG",
                                "Hoàn trả phiếu mượn " + maPhieuHienTai
                                        + " đúng hạn: +" + diemHoanTat + " điểm");
                    }
                }

                giaoDich.commit();
                DatTruocService.yeuCauGuiSauCommit();

                System.out.println(
                        "Xử lý thành công dòng " + idChiTiet
                                + "; tình trạng: " + tinhTrang
                                + "; số bản: " + soLuong
                );
                return true;

            } catch (SQLException | RuntimeException e) {
                try {
                    giaoDich.rollback();
                } catch (SQLException loiRollback) {
                    e.addSuppressed(loiRollback);
                }

                System.err.println(
                        "Trả/chốt mất thất bại: " + e.getMessage()
                );
                return false;
            }

        } catch (SQLException e) {
            System.err.println(
                    "Lỗi kết nối giao dịch trả: " + e.getMessage()
            );
            return false;
        }
    }

    // ================= HELPER CẬP NHẬT =================

    private void capNhatMotDong(
            Connection giaoDich,
            String sql,
            Object... thamSo
    ) throws SQLException {
        try (
                PreparedStatement ps =
                        giaoDich.prepareStatement(sql)
        ) {
            for (int i = 0; i < thamSo.length; i++) {
                ps.setObject(i + 1, thamSo[i]);
            }

            if (ps.executeUpdate() != 1) {
                throw new SQLException(
                        "Không cập nhật được dữ liệu "
                                + "hoặc tồn kho không nhất quán."
                );
            }
        }
    }
}