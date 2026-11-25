package dao;

import model.Voucher;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO thao tác bảng voucher_doc_gia (Voucher đổi thưởng từ điểm tích lũy).
 */
public class VoucherDAO {

    /**
     * Thêm voucher mới.
     */
    public boolean themVoucher(Voucher voucher) {
        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich()) {
            conn.setAutoCommit(false);
            themVoucher(conn, voucher);
            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Thêm voucher mới trong giao dịch có sẵn.
     */
    public void themVoucher(Connection giaoDich, Voucher voucher) throws SQLException {
        String sql = """
                INSERT INTO voucher_doc_gia
                    (ma_voucher, id_doc_gia, ten_uu_dai, loai_giam, gia_tri_giam, giam_toi_da, trang_thai, ngay_doi)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = giaoDich.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, voucher.getMaVoucher());
            ps.setInt(2, voucher.getIdDocGia());
            ps.setString(3, voucher.moTaUuDai());
            ps.setString(4, voucher.getLoaiGiam() == null ? "TIEN_MAT" : voucher.getLoaiGiam());
            ps.setBigDecimal(5, BigDecimal.valueOf(voucher.getGiaTriGiam()));
            if (voucher.getGiamToiDa() == null) {
                ps.setNull(6, Types.DECIMAL);
            } else {
                ps.setBigDecimal(6, BigDecimal.valueOf(voucher.getGiamToiDa()));
            }
            String trangThai = voucher.getTrangThai() == null || voucher.getTrangThai().isBlank()
                    ? "CHUA_DUNG"
                    : voucher.getTrangThai();
            ps.setString(7, trangThai);
            Timestamp ngayDoi = voucher.getNgayDoi() != null
                    ? Timestamp.valueOf(voucher.getNgayDoi())
                    : new Timestamp(System.currentTimeMillis());
            ps.setTimestamp(8, ngayDoi);

            if (ps.executeUpdate() != 1) {
                throw new SQLException("Không tạo được voucher mới trong bảng voucher_doc_gia.");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    voucher.setId(keys.getInt(1));
                }
            }
        }
    }

    /**
     * Khóa và lấy voucher hợp lệ (CHUA_DUNG) để áp dụng trong giao dịch mượn sách.
     */
    public Voucher khoaVaLayVoucherHopLe(Connection giaoDich, int idVoucher, int idDocGia) throws SQLException {
        String sql = """
                SELECT *
                FROM voucher_doc_gia
                WHERE id = ?
                  AND id_doc_gia = ?
                  AND trang_thai = 'CHUA_DUNG'
                FOR UPDATE
                """;
        try (PreparedStatement ps = giaoDich.prepareStatement(sql)) {
            ps.setInt(1, idVoucher);
            ps.setInt(2, idDocGia);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToVoucher(rs);
                }
            }
        }
        return null;
    }

    /**
     * Đánh dấu voucher đã sử dụng trong giao dịch có sẵn.
     */
    public void danhDauDaSuDung(Connection giaoDich, int idVoucher) throws SQLException {
        String sql = """
                UPDATE voucher_doc_gia
                SET trang_thai = 'DA_DUNG',
                    ngay_su_dung = NOW()
                WHERE id = ?
                  AND trang_thai = 'CHUA_DUNG'
                """;
        try (PreparedStatement ps = giaoDich.prepareStatement(sql)) {
            ps.setInt(1, idVoucher);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Voucher đã được sử dụng hoặc không hợp lệ.");
            }
        }
    }

    /**
     * Lấy tất cả voucher của một độc giả.
     */
    public List<Voucher> getByDocGia(int idDocGia) {
        List<Voucher> danhSach = new ArrayList<>();
        String sql = "SELECT * FROM voucher_doc_gia WHERE id_doc_gia = ? ORDER BY ngay_doi DESC, id DESC";

        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDocGia);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    danhSach.add(mapResultSetToVoucher(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return danhSach;
    }

    /**
     * Lấy voucher theo ID.
     */
    public Voucher getById(int idVoucher) {
        String sql = "SELECT * FROM voucher_doc_gia WHERE id = ?";

        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVoucher);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToVoucher(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Lấy danh sách voucher chưa sử dụng của độc giả.
     */
    public List<Voucher> getVoucherChuaDung(int idDocGia) {
        List<Voucher> danhSach = new ArrayList<>();
        String sql = """
                SELECT *
                FROM voucher_doc_gia
                WHERE id_doc_gia = ?
                  AND trang_thai = 'CHUA_DUNG'
                ORDER BY ngay_doi DESC, id DESC
                """;

        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDocGia);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    danhSach.add(mapResultSetToVoucher(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return danhSach;
    }

    /**
     * Giữ tương thích với code gọi tên cũ getVoucherConHan.
     */
    public List<Voucher> getVoucherConHan(int idDocGia) {
        return getVoucherChuaDung(idDocGia);
    }

    /**
     * Đánh dấu voucher đã sử dụng (kết nối độc lập).
     */
    public boolean danhDauDaSuDung(int idVoucher) {
        String sql = """
                UPDATE voucher_doc_gia
                SET trang_thai = 'DA_DUNG',
                    ngay_su_dung = NOW()
                WHERE id = ?
                  AND trang_thai = 'CHUA_DUNG'
                """;

        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVoucher);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int capNhatVoucherHetHan() {
        return 0;
    }

    private Voucher mapResultSetToVoucher(ResultSet rs) throws SQLException {
        Voucher voucher = new Voucher();
        voucher.setId(rs.getInt("id"));
        voucher.setMaVoucher(rs.getString("ma_voucher"));
        voucher.setIdDocGia(rs.getInt("id_doc_gia"));
        voucher.setTenUuDai(rs.getString("ten_uu_dai"));
        voucher.setLoaiGiam(rs.getString("loai_giam"));
        voucher.setGiaTriGiam(rs.getDouble("gia_tri_giam"));
        BigDecimal toiDa = rs.getBigDecimal("giam_toi_da");
        voucher.setGiamToiDa(toiDa == null ? null : toiDa.doubleValue());
        voucher.setTrangThai(rs.getString("trang_thai"));

        Timestamp ngayDoi = rs.getTimestamp("ngay_doi");
        if (ngayDoi != null) {
            voucher.setNgayDoi(ngayDoi.toLocalDateTime());
        }
        Timestamp ngaySuDung = rs.getTimestamp("ngay_su_dung");
        if (ngaySuDung != null) {
            voucher.setNgaySuDung(ngaySuDung.toLocalDateTime());
        }
        voucher.setHanSuDung(null);
        return voucher;
    }
}
