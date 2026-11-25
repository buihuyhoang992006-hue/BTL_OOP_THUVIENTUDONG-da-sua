
package dao;

import model.DocGia;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DocGiaDAO {

    // ========================================
    // LẤY TOÀN BỘ ĐỘC GIẢ
    // ========================================

    public List<DocGia> getAll() {

        List<DocGia> danhSach = new ArrayList<>();

        String sql =
                "SELECT * FROM doc_gia ORDER BY id DESC";

        try (Connection conn =
                     KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                danhSach.add(mapResultSetToDocGia(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return danhSach;
    }

    // ========================================
    // TÌM ĐỘC GIẢ THEO ID
    // ========================================

    public DocGia getById(int id) {

        String sql =
                "SELECT * FROM doc_gia WHERE id = ?";

        try (Connection conn =
                     KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return mapResultSetToDocGia(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // ========================================
    // TÌM ĐỘC GIẢ THEO MÃ
    // ========================================

    public DocGia getByMaDocGia(String maDocGia) {

        String sql =
                "SELECT * FROM doc_gia WHERE ma_doc_gia = ?";

        try (Connection conn =
                     KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, maDocGia);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return mapResultSetToDocGia(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // ========================================
    // THÊM ĐỘC GIẢ
    // ========================================

    public boolean themDocGia(DocGia docGia) {
        if (docGia == null || !DocGia.laCccdHopLe(docGia.getMaDocGia())) {
            return false;
        }

        /*
         * Database chính thức:
         *
         * ngay_tao được MySQL tự tạo.
         * trang_thai là ENUM:
         * 'HOAT_DONG' hoặc 'BI_KHOA'.
         */

        String sql =
                "INSERT INTO doc_gia " +
                        "(ma_doc_gia, ho_ten, email, " +
                        "so_dien_thoai, dia_chi, " +
                        "diem_tich_luy, diem_thuong, so_lan_vi_pham, trang_thai) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn =
                     KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, docGia.getMaDocGia());
            ps.setString(2, docGia.getHoTen());
            ps.setString(3, docGia.getEmail());
            ps.setString(4, docGia.getSoDienThoai());
            ps.setString(5, docGia.getDiaChi());

            ps.setInt(6, docGia.getDiemTichLuy());
            ps.setInt(7, docGia.getDiemTichLuy());
            ps.setInt(8, docGia.getSoLanViPham());

            // Chuyển boolean Java thành ENUM MySQL
            ps.setString(
                    9,
                    docGia.isTrangThai()
                            ? "HOAT_DONG"
                            : "BI_KHOA"
            );

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Không lưu được hồ sơ độc giả (MySQL " + e.getErrorCode() + ").");
        }

        return false;
    }

    // ========================================
    // CẬP NHẬT ĐỘC GIẢ
    // ========================================

    public boolean capNhatDocGia(DocGia docGia) {
        if (docGia == null || docGia.getId() <= 0 || docGia.getMaDocGia() == null
                || docGia.getMaDocGia().isBlank()) {
            return false;
        }

        /*
         * Chỉ cập nhật thông tin cá nhân và trạng thái.
         *
         * Không ghi đè:
         * - diem_thuong
         * - so_lan_vi_pham
         * - ngay_tao
         *
         * Điểm và vi phạm có các hàm cập nhật riêng.
         */

        String sql =
                "UPDATE doc_gia SET " +
                        "ma_doc_gia = ?, " +
                        "ho_ten = ?, " +
                        "email = ?, " +
                        "so_dien_thoai = ?, " +
                        "dia_chi = ?, " +
                        "trang_thai = ? " +
                        "WHERE id = ? AND (? OR CAST(ma_doc_gia AS BINARY) = CAST(? AS BINARY))";

        try (Connection conn =
                     KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, docGia.getMaDocGia());
            ps.setString(2, docGia.getHoTen());
            ps.setString(3, docGia.getEmail());
            ps.setString(4, docGia.getSoDienThoai());
            ps.setString(5, docGia.getDiaChi());

            ps.setString(
                    6,
                    docGia.isTrangThai()
                            ? "HOAT_DONG"
                            : "BI_KHOA"
            );

            ps.setInt(7, docGia.getId());
            // Mã thẻ cũ vẫn được lưu nếu giữ nguyên. Khi đổi mã, yêu cầu CCCD.
            ps.setBoolean(8, DocGia.laCccdHopLe(docGia.getMaDocGia()));
            ps.setString(9, docGia.getMaDocGia());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Không lưu được hồ sơ độc giả (MySQL " + e.getErrorCode() + ").");
        }

        return false;
    }

    // ========================================
    // XÓA ĐỘC GIẢ
    // ========================================

    public boolean xoaDocGia(int id) {

        String sql =
                "DELETE FROM doc_gia WHERE id = ?";

        try (Connection conn =
                     KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // ========================================
    // TÌM KIẾM ĐỘC GIẢ
    // ========================================

    public List<DocGia> timKiem(String tuKhoa) {

        List<DocGia> danhSach = new ArrayList<>();

        String sql =
                "SELECT * FROM doc_gia " +
                        "WHERE ma_doc_gia LIKE ? " +
                        "OR ho_ten LIKE ? " +
                        "OR email LIKE ? " +
                        "OR so_dien_thoai LIKE ? " +
                        "ORDER BY ho_ten";

        try (Connection conn =
                     KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String keyword = "%" + tuKhoa + "%";

            ps.setString(1, keyword);
            ps.setString(2, keyword);
            ps.setString(3, keyword);
            ps.setString(4, keyword);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    danhSach.add(mapResultSetToDocGia(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return danhSach;
    }

    // ========================================
    // CẬP NHẬT ĐIỂM THƯỞNG
    // ========================================

    public boolean capNhatDiemTichLuy(
            int idDocGia,
            int diemMoi
    ) {
        return capNhatDiemThuong(idDocGia, diemMoi);
    }

    public boolean capNhatDiemThuong(
            int idDocGia,
            int diemMoi
    ) {
        String sql =
                "UPDATE doc_gia " +
                        "SET diem_tich_luy = ?, diem_thuong = ? " +
                        "WHERE id = ?";

        try (Connection conn =
                     KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, diemMoi);
            ps.setInt(2, diemMoi);
            ps.setInt(3, idDocGia);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public void congTruDiem(
            Connection giaoDich,
            int idDocGia,
            int deltaDiem
    ) throws SQLException {
        if (giaoDich == null || giaoDich.getAutoCommit() || idDocGia <= 0 || deltaDiem == 0) {
            return;
        }
        String sql = "UPDATE doc_gia "
                + "SET diem_tich_luy = diem_tich_luy + ?, diem_thuong = diem_thuong + ? "
                + "WHERE id = ?";
        try (PreparedStatement ps = giaoDich.prepareStatement(sql)) {
            ps.setInt(1, deltaDiem);
            ps.setInt(2, deltaDiem);
            ps.setInt(3, idDocGia);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Không cập nhật được điểm tích lũy độc giả.");
            }
        }
    }

    // ========================================
    // CẬP NHẬT SỐ LẦN VI PHẠM
    // ========================================

    public boolean capNhatSoLanViPham(
            int idDocGia,
            int soLanViPham
    ) {

        if (soLanViPham < 0) {
            return false;
        }

        String sql =
                "UPDATE doc_gia " +
                        "SET so_lan_vi_pham = ? " +
                        "WHERE id = ?";

        try (Connection conn =
                     KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, soLanViPham);
            ps.setInt(2, idDocGia);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // ========================================
    // KHÓA / MỞ KHÓA ĐỘC GIẢ
    // ========================================

    public boolean capNhatTrangThai(
            int idDocGia,
            boolean trangThai
    ) {

        String sql =
                "UPDATE doc_gia " +
                        "SET trang_thai = ? " +
                        "WHERE id = ?";

        try (Connection conn =
                     KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(
                    1,
                    trangThai ? "HOAT_DONG" : "BI_KHOA"
            );

            ps.setInt(2, idDocGia);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // ========================================
    // KIỂM TRA TRÙNG MÃ ĐỘC GIẢ
    // ========================================

    public boolean tonTaiMaDocGia(String maDocGia) {

        String sql =
                "SELECT COUNT(*) FROM doc_gia " +
                        "WHERE ma_doc_gia = ?";

        try (Connection conn =
                     KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, maDocGia);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // ========================================
    // KIỂM TRA EMAIL ĐÃ TỒN TẠI
    // ========================================

    public boolean tonTaiEmail(String email) {

        String sql =
                "SELECT COUNT(*) FROM doc_gia WHERE email = ?";

        try (Connection conn =
                     KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // ========================================
    // CHUYỂN RESULTSET -> DOCGIA
    // ========================================

    private DocGia mapResultSetToDocGia(ResultSet rs)
            throws SQLException {

        DocGia docGia = new DocGia();

        docGia.setId(rs.getInt("id"));

        docGia.setMaDocGia(
                rs.getString("ma_doc_gia")
        );

        docGia.setHoTen(
                rs.getString("ho_ten")
        );

        docGia.setEmail(
                rs.getString("email")
        );

        docGia.setSoDienThoai(
                rs.getString("so_dien_thoai")
        );

        docGia.setDiaChi(
                rs.getString("dia_chi")
        );

        docGia.setDiemTichLuy(
                rs.getInt("diem_tich_luy")
        );

        docGia.setSoLanViPham(
                rs.getInt("so_lan_vi_pham")
        );

        // Database dùng ngay_tao (TIMESTAMP).
        // Model DocGia dùng ngayDangKy (LocalDate).
        Timestamp ngayTao = rs.getTimestamp("ngay_tao");

        if (ngayTao != null) {
            docGia.setNgayDangKy(
                    ngayTao.toLocalDateTime().toLocalDate()
            );
        }

        // Chuyển ENUM MySQL thành boolean Java
        String trangThai = rs.getString("trang_thai");

        docGia.setTrangThai(
                "HOAT_DONG".equals(trangThai)
        );

        return docGia;
    }
}
