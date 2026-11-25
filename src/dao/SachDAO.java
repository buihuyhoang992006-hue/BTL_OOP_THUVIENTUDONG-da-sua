package dao;

import model.Sach;
import service.DatTruocService;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SachDAO {

    private static final String SQL_CHON_SACH = """
            SELECT s.*, dm.ten_danh_muc
            FROM sach s
            LEFT JOIN danh_muc dm ON s.id_danh_muc = dm.id
            """;

    // ================= ĐỌC SÁCH =================

    public List<Sach> getAll() {
        return truyVanDanhSach(
                SQL_CHON_SACH + "ORDER BY s.id DESC"
        );
    }

    public Sach getById(int id) {
        if (id <= 0) {
            return null;
        }

        List<Sach> danhSach = truyVanDanhSach(
                SQL_CHON_SACH + "WHERE s.id = ?",
                id
        );

        return danhSach.isEmpty() ? null : danhSach.get(0);
    }

    public Sach getByMaSach(String maSach) {
        if (maSach == null || maSach.isBlank()) {
            return null;
        }

        List<Sach> danhSach = truyVanDanhSach(
                SQL_CHON_SACH + "WHERE s.ma_sach = ?",
                maSach.trim()
        );

        return danhSach.isEmpty() ? null : danhSach.get(0);
    }

    public List<Sach> timKiem(String tuKhoa) {
        String keyword = "%"
                + (tuKhoa == null ? "" : tuKhoa.trim())
                + "%";

        return truyVanDanhSach(
                SQL_CHON_SACH + """
                        WHERE s.ma_sach LIKE ?
                           OR s.ten_sach LIKE ?
                           OR s.tac_gia LIKE ?
                        ORDER BY s.ten_sach
                        """,
                keyword,
                keyword,
                keyword
        );
    }

    private List<Sach> truyVanDanhSach(
            String sql,
            Object... thamSo
    ) {
        List<Sach> danhSach = new ArrayList<>();

        try (
                Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            for (int i = 0; i < thamSo.length; i++) {
                ps.setObject(i + 1, thamSo[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    danhSach.add(mapResultSetToSach(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Lỗi đọc sách: " + e.getMessage());
        }

        return danhSach;
    }

    // ================= THÊM SÁCH =================

    public boolean themSach(Sach sach) {
        if (!duLieuHopLe(sach)
                || sach.getSoLuongCon() != sach.getSoLuongTong()) {
            return false;
        }

        String sql = """
                INSERT INTO sach
                    (ma_sach, ten_sach, tac_gia, nha_xuat_ban,
                     nam_xuat_ban, so_luong_tong, so_luong_con,
                     id_danh_muc, mo_ta, hinh_anh, trang_thai, gia_thue)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (
                Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ganThongTinSach(ps, sach);
            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            System.err.println("Lỗi thêm sách: " + e.getMessage());
            return false;
        }
    }

    // ================= CẬP NHẬT SÁCH =================

    public boolean capNhatSach(
            Sach sach,
            int soLuongTongCu,
            int soLuongConCu
    ) {
        if (!duLieuHopLe(sach)
                || sach.getId() <= 0
                || soLuongTongCu < 0
                || soLuongConCu < 0
                || soLuongConCu > soLuongTongCu) {
            return false;
        }

        // Giữ nguyên số bản đang được mượn.
        long soLuongConDuKien =
                (long) soLuongConCu
                        + sach.getSoLuongTong()
                        - soLuongTongCu;

        if (soLuongConDuKien != sach.getSoLuongCon()) {
            return false;
        }

        /*
         * Hai điều kiện số lượng cũ được kiểm tra ngay trong UPDATE.
         * Nếu có lượt mượn/trả làm đổi kho, câu UPDATE không ghi dữ liệu.
         */
        String sql = """
                UPDATE sach
                SET ma_sach = ?,
                    ten_sach = ?,
                    tac_gia = ?,
                    nha_xuat_ban = ?,
                    nam_xuat_ban = ?,
                    so_luong_tong = ?,
                    so_luong_con = ?,
                    id_danh_muc = ?,
                    mo_ta = ?,
                    hinh_anh = ?,
                    trang_thai = ?,
                    gia_thue = ?
                WHERE id = ?
                  AND so_luong_tong = ?
                  AND so_luong_con = ?
                """;

        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich()) {
            conn.setAutoCommit(false);
            try {
                // Reserve/borrow/stock editing all take the same book-row lock.
                try (PreparedStatement lock = conn.prepareStatement(
                        "SELECT so_luong_tong, so_luong_con FROM sach WHERE id = ? FOR UPDATE")) {
                    lock.setInt(1, sach.getId());
                    try (ResultSet rs = lock.executeQuery()) {
                        if (!rs.next() || rs.getInt(1) != soLuongTongCu || rs.getInt(2) != soLuongConCu) {
                            conn.rollback();
                            return false;
                        }
                    }
                }
                DatTruocService datTruoc = new DatTruocService();
                if (sach.isTrangThai()) {
                    DatTruocService.KetQuaHangDoiTraSach queue = datTruoc.dongBoHangDoi(conn, sach.getId());
                    int held = queue.danhSachDangGiuHienTai().size() + queue.danhSachDenLuotMoi().size();
                    if (sach.getSoLuongCon() < held) {
                        conn.rollback();
                        return false;
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ganThongTinSach(ps, sach);
                    ps.setInt(13, sach.getId());
                    ps.setInt(14, soLuongTongCu);
                    ps.setInt(15, soLuongConCu);
                    if (ps.executeUpdate() != 1) {
                        conn.rollback();
                        return false;
                    }
                }
                // Restock promotes waiting readers; inactivation cancels holds and unsent messages.
                datTruoc.dongBoHangDoi(conn, sach.getId());
                conn.commit();
                DatTruocService.yeuCauGuiSauCommit();
                return true;
            } catch (SQLException | RuntimeException ex) {
                try { conn.rollback(); } catch (SQLException rollback) { ex.addSuppressed(rollback); }
                System.err.println("Không cập nhật được sách/kho/hàng đợi: " + ex.getMessage());
                return false;
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi kết nối cập nhật sách: " + ex.getMessage());
            return false;
        }
    }

    private boolean duLieuHopLe(Sach sach) {
        return sach != null
                && sach.getMaSach() != null
                && !sach.getMaSach().isBlank()
                && sach.getTenSach() != null
                && !sach.getTenSach().isBlank()
                && sach.getNamXuatBan() > 0
                && sach.getIdDanhMuc() >= 0
                && sach.getSoLuongTong() >= 0
                && sach.getSoLuongCon() >= 0
                && sach.getSoLuongCon() <= sach.getSoLuongTong()
                && sach.getGiaThue() >= 0;
    }

    private void ganThongTinSach(
            PreparedStatement ps,
            Sach sach
    ) throws SQLException {
        ps.setString(1, sach.getMaSach());
        ps.setString(2, sach.getTenSach());
        ps.setString(3, sach.getTacGia());
        ps.setString(4, sach.getNhaXuatBan());
        ps.setInt(5, sach.getNamXuatBan());

        ps.setInt(6, sach.getSoLuongTong());
        ps.setInt(7, sach.getSoLuongCon());

        // Model dùng 0 để biểu diễn sách chưa phân loại.
        if (sach.getIdDanhMuc() == 0) {
            ps.setNull(8, Types.INTEGER);
        } else {
            ps.setInt(8, sach.getIdDanhMuc());
        }

        ps.setString(9, sach.getMoTa());
        ps.setString(10, sach.getHinhAnh());
        ps.setBoolean(11, sach.isTrangThai());
        ps.setBigDecimal(12, BigDecimal.valueOf(sach.getGiaThue()));
    }

    // ================= XÓA SÁCH =================

    public boolean xoaSach(int id) {
        if (id <= 0) {
            return false;
        }

        String sql = "DELETE FROM sach WHERE id = ?";

        try (
                Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            System.err.println("Lỗi xóa sách: " + e.getMessage());
            return false;
        }
    }

    // ================= KIỂM TRA MÃ SÁCH =================

    public boolean tonTaiMaSach(String maSach) {
        if (maSach == null || maSach.isBlank()) {
            return false;
        }

        String sql = """
                SELECT COUNT(*)
                FROM sach
                WHERE ma_sach = ?
                """;

        try (
                Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, maSach.trim());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println(
                    "Lỗi kiểm tra mã sách: " + e.getMessage()
            );
            return false;
        }
    }

    // ================= ÁNH XẠ MODEL =================

    private Sach mapResultSetToSach(ResultSet rs)
            throws SQLException {
        Sach sach = new Sach();

        sach.setId(rs.getInt("id"));
        sach.setMaSach(rs.getString("ma_sach"));
        sach.setTenSach(rs.getString("ten_sach"));
        sach.setTacGia(rs.getString("tac_gia"));
        sach.setNhaXuatBan(rs.getString("nha_xuat_ban"));
        sach.setNamXuatBan(rs.getInt("nam_xuat_ban"));

        sach.setSoLuongTong(rs.getInt("so_luong_tong"));
        sach.setSoLuongCon(rs.getInt("so_luong_con"));

        sach.setIdDanhMuc(rs.getInt("id_danh_muc"));
        sach.setTenDanhMuc(rs.getString("ten_danh_muc"));

        sach.setMoTa(rs.getString("mo_ta"));
        sach.setHinhAnh(rs.getString("hinh_anh"));
        sach.setGiaThue(Math.round(rs.getDouble("gia_thue")));
        sach.setTrangThai(rs.getBoolean("trang_thai"));

        return sach;
    }
}