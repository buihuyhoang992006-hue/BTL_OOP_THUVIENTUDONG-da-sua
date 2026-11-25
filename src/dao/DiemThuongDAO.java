package dao;

import model.LichSuDiem;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DiemThuongDAO {

    /**
     * Thêm một bản ghi lịch sử điểm (kết nối độc lập).
     */
    public boolean themLichSu(LichSuDiem lichSu) {
        if (lichSu == null) {
            return false;
        }
        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich()) {
            conn.setAutoCommit(false);
            themLichSu(
                    conn,
                    lichSu.getIdDocGia(),
                    lichSu.getSoDiem(),
                    lichSu.getLoai(),
                    lichSu.getNoiDung(),
                    lichSu.getNgayTao());
            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Thêm một bản ghi lịch sử điểm trong giao dịch có sẵn.
     */
    public void themLichSu(
            Connection giaoDich,
            int idDocGia,
            int soDiem,
            String loai,
            String noiDung) throws SQLException {
        themLichSu(giaoDich, idDocGia, soDiem, loai, noiDung, LocalDateTime.now());
    }

    private void themLichSu(
            Connection giaoDich,
            int idDocGia,
            int soDiem,
            String loai,
            String noiDung,
            LocalDateTime ngayTao) throws SQLException {
        if (giaoDich == null || giaoDich.getAutoCommit() || idDocGia <= 0 || soDiem == 0) {
            return;
        }

        String maLichSu = "LSD" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 27);
        String loaiChuan = "TRU".equalsIgnoreCase(loai) ? "TRU" : "CONG";
        LocalDateTime thoiGian = ngayTao == null ? LocalDateTime.now() : ngayTao;

        String sql = "INSERT INTO lich_su_diem "
                + "(ma_lich_su, id_doc_gia, so_diem, loai, noi_dung, ngay_tao) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = giaoDich.prepareStatement(sql)) {
            ps.setString(1, maLichSu);
            ps.setInt(2, idDocGia);
            ps.setInt(3, Math.abs(soDiem));
            ps.setString(4, loaiChuan);
            ps.setString(5, noiDung);
            ps.setTimestamp(6, Timestamp.valueOf(thoiGian));
            ps.executeUpdate();
        }
    }

    /**
     * Lấy lịch sử điểm của một độc giả.
     */
    public List<LichSuDiem> getByDocGia(
            int idDocGia) {

        List<LichSuDiem> danhSach =
                new ArrayList<>();

        String sql =
                "SELECT * FROM lich_su_diem " +
                        "WHERE id_doc_gia = ? " +
                        "ORDER BY ngay_tao DESC";

        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idDocGia);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    danhSach.add(
                            mapResultSetToLichSuDiem(rs)
                    );
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return danhSach;
    }

    /**
     * Tổng số điểm đã từng được cộng.
     *
     * Chức năng này chủ yếu dùng thống kê.
     * Điểm hiện tại vẫn lấy từ doc_gia.diem_thuong.
     */
    public int getTongDiemDaNhan(
            int idDocGia) {

        String sql =
                "SELECT COALESCE(SUM(so_diem), 0) " +
                        "FROM lich_su_diem " +
                        "WHERE id_doc_gia = ?";

        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idDocGia);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Đếm số lần độc giả được thưởng.
     */
    public int demSoLanDuocThuong(
            int idDocGia) {

        String sql =
                "SELECT COUNT(*) " +
                        "FROM lich_su_diem " +
                        "WHERE id_doc_gia = ?";

        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idDocGia);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * ResultSet -> LichSuDiem.
     */
    private LichSuDiem mapResultSetToLichSuDiem(
            ResultSet rs) throws SQLException {

        LichSuDiem lichSu =
                new LichSuDiem();

        lichSu.setId(
                rs.getInt("id")
        );

        lichSu.setMaLichSu(
                rs.getString("ma_lich_su")
        );

        lichSu.setIdDocGia(
                rs.getInt("id_doc_gia")
        );

        lichSu.setSoDiem(
                rs.getInt("so_diem")
        );

        lichSu.setLoai(
                rs.getString("loai")
        );

        lichSu.setNoiDung(
                rs.getString("noi_dung")
        );

        Timestamp ngayTao =
                rs.getTimestamp("ngay_tao");

        if (ngayTao != null) {
            lichSu.setNgayTao(
                    ngayTao.toLocalDateTime()
            );
        }

        return lichSu;
    }
}