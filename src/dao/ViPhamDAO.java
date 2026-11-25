package dao;

import model.ViPham;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ViPhamDAO {

    public static final int DIEM_TRU_MOI_NGAY_QUA_HAN = 2;
    public static final int DIEM_TRU_HONG_MAT = 30;

    // ================= GHI NHẬN VI PHẠM =================

    /*
     * Dùng connection của giao dịch trả sách.
     *
     * Người gọi phải:
     * - Tắt autoCommit.
     * - Khóa dòng mượn bằng SELECT ... FOR UPDATE.
     * - Tự commit hoặc rollback toàn bộ giao dịch.
     *
     * Hàm này không mở, đóng hoặc commit connection.
     */
    public void ghiNhanViPham(
            Connection giaoDich,
            int idDocGia,
            int idChiTiet,
            String loai,
            int soNgayQuaHan,
            String moTa
    ) throws SQLException {

        if (giaoDich == null
                || giaoDich.getAutoCommit()
                || idDocGia <= 0
                || idChiTiet <= 0
                || loai == null
                || !List.of("TRA_MUON", "LAM_HONG", "LAM_MAT")
                .contains(loai)
                || soNgayQuaHan < 0
                || ("TRA_MUON".equals(loai) && soNgayQuaHan == 0)) {

            throw new SQLException("Dữ liệu vi phạm không hợp lệ.");
        }

        long tienPhat = tinhTienPhatMacDinh(loai, soNgayQuaHan);
        int diemTru = tinhDiemTruViPham(loai, soNgayQuaHan);

        String sqlThem = """
                INSERT INTO vi_pham
                    (ma_vi_pham, id_doc_gia, id_chi_tiet,
                     loai_vi_pham, so_ngay_qua_han, mo_ta, tien_phat)
                SELECT ?, ?, ?, ?, ?, ?, ?
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM vi_pham
                    WHERE id_chi_tiet = ?
                      AND loai_vi_pham = ?
                )
                """;

        String maViPham = "VP" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 28);

        try (
                PreparedStatement ps =
                        giaoDich.prepareStatement(sqlThem)
        ) {
            ps.setString(1, maViPham);
            ps.setInt(2, idDocGia);
            ps.setInt(3, idChiTiet);
            ps.setString(4, loai);
            ps.setInt(5, soNgayQuaHan);
            ps.setString(6, moTa);
            ps.setBigDecimal(7, BigDecimal.valueOf(tienPhat));
            ps.setInt(8, idChiTiet);
            ps.setString(9, loai);

            if (ps.executeUpdate() != 1) {
                throw new SQLException(
                        "Vi phạm đã tồn tại hoặc không thể ghi nhận."
                );
            }
        }

        String sqlTangSoLan = """
                UPDATE doc_gia
                SET so_lan_vi_pham = so_lan_vi_pham + 1,
                    diem_tich_luy = diem_tich_luy - ?,
                    diem_thuong = diem_thuong - ?
                WHERE id = ?
                """;

        try (
                PreparedStatement ps =
                        giaoDich.prepareStatement(sqlTangSoLan)
        ) {
            ps.setInt(1, diemTru);
            ps.setInt(2, diemTru);
            ps.setInt(3, idDocGia);

            if (ps.executeUpdate() != 1) {
                throw new SQLException(
                        "Không cập nhật được số lần vi phạm."
                );
            }
        }

        if (diemTru > 0) {
            String noiDungTru = "TRA_MUON".equals(loai)
                    ? "Trừ điểm trả sách muộn (" + soNgayQuaHan + " ngày x "
                            + DIEM_TRU_MOI_NGAY_QUA_HAN + " điểm): -" + diemTru + " điểm"
                    : "Trừ điểm làm hỏng/mất sách (" + loai + "): -" + diemTru + " điểm";
            new DiemThuongDAO().themLichSu(
                    giaoDich,
                    idDocGia,
                    diemTru,
                    "TRU",
                    noiDungTru);
        }
    }

    public static long tinhTienPhatMacDinh(String loaiViPham, int soNgayQuaHan) {
        return switch (loaiViPham == null ? "" : loaiViPham) {
            case "LAM_HONG" -> 50_000L;
            case "LAM_MAT" -> 100_000L;
            default -> 0L;
        };
    }

    public static int tinhDiemTruViPham(String loaiViPham, int soNgayQuaHan) {
        return switch (loaiViPham == null ? "" : loaiViPham) {
            case "LAM_HONG", "LAM_MAT" -> DIEM_TRU_HONG_MAT;
            default -> Math.max(1, soNgayQuaHan) * DIEM_TRU_MOI_NGAY_QUA_HAN;
        };
    }

    // ================= GHI NHẬN TRẢ MUỘN =================

    /*
     * Giữ chữ ký đang được PhieuMuonDAO.traSach(int) sử dụng.
     * Dùng lại hàm ghiNhanViPham để thống nhất cách ghi và tăng bộ đếm.
     */
    public boolean ghiNhanTraMuon(
            Connection giaoDich,
            int idChiTiet
    ) throws SQLException {

        if (giaoDich == null
                || giaoDich.getAutoCommit()
                || idChiTiet <= 0) {
            throw new SQLException(
                    "Ghi vi phạm phải nằm trong giao dịch trả sách."
            );
        }

        String sqlThongTin = """
                SELECT p.id_doc_gia, ct.so_ngay_qua_han
                FROM chi_tiet_phieu_muon ct
                JOIN phieu_muon p ON ct.id_phieu_muon = p.id
                WHERE ct.id = ?
                  AND ct.trang_thai = 'QUA_HAN'
                  AND ct.ngay_tra IS NOT NULL
                """;

        int idDocGia;
        int soNgayQuaHan;

        try (
                PreparedStatement ps =
                        giaoDich.prepareStatement(sqlThongTin)
        ) {
            ps.setInt(1, idChiTiet);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException(
                            "Không tìm thấy dòng mượn đã trả quá hạn."
                    );
                }

                idDocGia = rs.getInt("id_doc_gia");
                soNgayQuaHan = rs.getInt("so_ngay_qua_han");
            }
        }

        ghiNhanViPham(
                giaoDich,
                idDocGia,
                idChiTiet,
                "TRA_MUON",
                soNgayQuaHan,
                "Trả sách quá hạn " + soNgayQuaHan + " ngày."
        );

        return true;
    }

    // ================= DANH SÁCH VI PHẠM =================

    private static final String COT_VI_PHAM = """
            vp.*, dg.ho_ten AS ten_doc_gia, pm.ma_phieu, s.ten_sach
            """;
    private static final String BANG_VI_PHAM = """
            FROM vi_pham vp
            JOIN doc_gia dg ON vp.id_doc_gia = dg.id
            LEFT JOIN chi_tiet_phieu_muon ct ON vp.id_chi_tiet = ct.id
            LEFT JOIN phieu_muon pm ON ct.id_phieu_muon = pm.id
            LEFT JOIN sach s ON ct.id_sach = s.id
            """;

    public List<ViPham> layDanhSachViPham() {
        return docDanhSach("SELECT " + COT_VI_PHAM + BANG_VI_PHAM + "ORDER BY vp.id DESC", null);
    }

    public List<ViPham> layViPhamTheoChiTiet(int idChiTiet) {
        if (idChiTiet <= 0) {
            return List.of();
        }
        return docDanhSach(
                "SELECT " + COT_VI_PHAM + BANG_VI_PHAM + "WHERE vp.id_chi_tiet = ? ORDER BY vp.id DESC",
                idChiTiet);
    }

    private List<ViPham> docDanhSach(String sql, Integer id) {
        List<ViPham> danhSach = new ArrayList<>();
        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (id != null) {
                ps.setInt(1, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    danhSach.add(docViPham(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi tải danh sách vi phạm: " + e.getMessage());
        }
        return danhSach;
    }

    public ViPham layBienBanTheoId(int idViPham) throws SQLException {
        // Bản cũ chỉ gắn ID vi phạm vào nhật ký cộng điểm. Khôi phục từ
        // đúng bút toán của vi phạm này nếu cột mới chưa có dữ liệu.
        String sql = "SELECT " + COT_VI_PHAM + """
                , COALESCE(vp.diem_khoi_phuc, (
                    SELECT MAX(ls.so_diem)
                    FROM lich_su_diem ls
                    WHERE ls.id_doc_gia = vp.id_doc_gia AND ls.loai = 'CONG'
                      AND ls.noi_dung = CONCAT('Khôi phục +', ls.so_diem,
                          ' điểm sau khi khắc phục vi phạm #', vp.id)
                  )) AS diem_khoi_phuc_bien_ban
                """ + BANG_VI_PHAM + "WHERE vp.id = ?";
        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idViPham);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                ViPham vp = docViPham(rs);
                vp.setDiemKhoiPhuc(rs.getObject("diem_khoi_phuc_bien_ban", Integer.class));
                return vp;
            }
        }
    }

    private ViPham docViPham(ResultSet rs) throws SQLException {
        ViPham vp = new ViPham();
        vp.setId(rs.getInt("id"));
        vp.setMaViPham(rs.getString("ma_vi_pham"));
        vp.setIdDocGia(rs.getInt("id_doc_gia"));
        vp.setTenDocGia(rs.getString("ten_doc_gia"));
        vp.setIdChiTiet(rs.getObject("id_chi_tiet", Integer.class));
        vp.setMaPhieu(rs.getString("ma_phieu"));
        vp.setTenSach(rs.getString("ten_sach"));
        vp.setLoaiViPham(rs.getString("loai_vi_pham"));
        vp.setSoNgayQuaHan(rs.getInt("so_ngay_qua_han"));
        vp.setNgayGhiNhan(rs.getTimestamp("ngay_ghi_nhan"));
        vp.setMoTa(rs.getString("mo_ta"));
        vp.setTrangThai(rs.getString("trang_thai"));
        vp.setTienPhat(Math.round(rs.getDouble("tien_phat")));
        vp.setNgayXuLy(rs.getTimestamp("ngay_xu_ly"));
        vp.setDiemKhoiPhuc(rs.getObject("diem_khoi_phuc", Integer.class));
        vp.setGhiChuXuLy(rs.getString("ghi_chu_xu_ly"));
        return vp;
    }

    // ================= ĐÁNH DẤU ĐÃ XỬ LÝ =================

    public boolean danhDauDaXuLy(int idViPham) {
        return danhDauDaXuLy(idViPham, 0L, 0, null);
    }

    public boolean danhDauDaXuLy(int idViPham, long tienPhatThucThu, String ghiChuThu) {
        return danhDauDaXuLy(idViPham, tienPhatThucThu, 0, ghiChuThu);
    }

    public boolean danhDauDaXuLy(int idViPham, long tienBoiThuong, int diemKhoiPhuc, String ghiChuThu) {
        if (idViPham <= 0 || tienBoiThuong < 0 || diemKhoiPhuc < 0) {
            return false;
        }

        String ghiChu = ghiChuThu == null ? "" : ghiChuThu.trim();
        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich()) {
            conn.setAutoCommit(false);
            try {
                int idDocGia;
                String sqlLock = """
                        SELECT id_doc_gia, trang_thai
                        FROM vi_pham
                        WHERE id = ?
                        FOR UPDATE
                        """;
                try (PreparedStatement ps = conn.prepareStatement(sqlLock)) {
                    ps.setInt(1, idViPham);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || !"CHUA_XU_LY".equals(rs.getString("trang_thai"))) {
                            conn.rollback();
                            return false;
                        }
                        idDocGia = rs.getInt("id_doc_gia");
                    }
                }

                String sqlUpdate = """
                        UPDATE vi_pham
                        SET trang_thai = 'DA_XU_LY',
                            tien_phat = ?,
                            ngay_xu_ly = NOW(),
                            diem_khoi_phuc = ?,
                            ghi_chu_xu_ly = ?
                        WHERE id = ?
                          AND trang_thai = 'CHUA_XU_LY'
                        """;
                try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                    ps.setBigDecimal(1, BigDecimal.valueOf(tienBoiThuong));
                    ps.setInt(2, diemKhoiPhuc);
                    ps.setString(3, ghiChu.isEmpty() ? null : ghiChu);
                    ps.setInt(4, idViPham);
                    if (ps.executeUpdate() != 1) {
                        conn.rollback();
                        return false;
                    }
                }

                if (diemKhoiPhuc > 0) {
                    new DocGiaDAO().congTruDiem(conn, idDocGia, diemKhoiPhuc);
                    new DiemThuongDAO().themLichSu(
                            conn,
                            idDocGia,
                            diemKhoiPhuc,
                            "CONG",
                            "Khôi phục +" + diemKhoiPhuc + " điểm sau khi khắc phục vi phạm #" + idViPham);
                }

                conn.commit();
                return true;
            } catch (SQLException | RuntimeException ex) {
                try {
                    conn.rollback();
                } catch (SQLException suppressed) {
                    ex.addSuppressed(suppressed);
                }
                System.err.println("Lỗi xử lý vi phạm: " + ex.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Lỗi kết nối khi xử lý vi phạm: " + e.getMessage());
            return false;
        }
    }
}