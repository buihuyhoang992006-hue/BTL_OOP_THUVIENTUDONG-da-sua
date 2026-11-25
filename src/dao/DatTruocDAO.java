package dao;

import model.DatTruocChiTiet;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Read/write reservation rows. Business transitions are serialized by the book lock in DatTruocService. */
public class DatTruocDAO {
    private static final String SELECT_DETAILS = """
            SELECT dt.*, dg.ma_doc_gia, dg.ho_ten AS ten_doc_gia,
                   dg.so_dien_thoai, dg.email AS email_doc_gia,
                   s.ma_sach, s.ten_sach
            FROM dat_truoc dt
            JOIN doc_gia dg ON dg.id = dt.id_doc_gia
            JOIN sach s ON s.id = dt.id_sach
            """;

    public boolean taoDatTruocNhieuSach(Connection conn, int idDocGia,
                                      Map<Integer, Integer> danhSachSachDat) throws SQLException {
        String sql = """
                INSERT INTO dat_truoc (ma_dat_truoc, id_doc_gia, id_sach, trang_thai)
                VALUES (?, ?, ?, 'DANG_CHO')
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<Integer, Integer> entry : danhSachSachDat.entrySet()) {
                for (int i = 0; i < entry.getValue(); i++) {
                    ps.setString(1, "DT" + UUID.randomUUID().toString().replace("-", "").substring(0, 28));
                    ps.setInt(2, idDocGia);
                    ps.setInt(3, entry.getKey());
                    if (ps.executeUpdate() != 1) throw new SQLException("Không tạo được lượt đặt trước.");
                }
            }
        }
        return true;
    }

    public DatTruocChiTiet getChiTietById(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM dat_truoc WHERE id = ? FOR UPDATE")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs, false) : null;
            }
        }
    }

    public boolean capNhatTrangThaiChiTiet(Connection conn, int id, String state, Timestamp deadline)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE dat_truoc SET trang_thai = ?, han_nhan_sach = ? WHERE id = ?")) {
            ps.setString(1, state);
            ps.setTimestamp(2, deadline);
            ps.setInt(3, id);
            return ps.executeUpdate() == 1;
        }
    }

    public List<DatTruocChiTiet> layCacDongHoatDongCoKhoa(Connection conn, int bookId) throws SQLException {
        List<DatTruocChiTiet> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT * FROM dat_truoc
                WHERE id_sach = ? AND trang_thai IN ('DA_THONG_BAO', 'DANG_CHO')
                ORDER BY ngay_dat, id FOR UPDATE
                """)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs, false));
            }
        }
        return result;
    }

    public List<Integer> laySachCoHangDoi() throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement("""
                     SELECT DISTINCT id_sach FROM dat_truoc
                     WHERE trang_thai IN ('DANG_CHO', 'DA_THONG_BAO') ORDER BY id_sach
                     """); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) ids.add(rs.getInt(1));
        }
        return ids;
    }

    public List<DatTruocChiTiet> getDanhSachDatTruocTheoDocGia(int idDocGia) {
        return layDatTruocChiTietTheoDocGia(idDocGia);
    }

    public List<DatTruocChiTiet> layTatCaDatTruocChiTiet() {
        return doc(SELECT_DETAILS + " ORDER BY FIELD(dt.trang_thai, 'DA_THONG_BAO', 'DANG_CHO', 'DA_NHAN', 'HET_HAN', 'DA_HUY'), dt.ngay_dat, dt.id");
    }

    public List<DatTruocChiTiet> layDatTruocChiTietTheoDocGia(int readerId) {
        return doc(SELECT_DETAILS + " WHERE dt.id_doc_gia = ? ORDER BY dt.ngay_dat DESC, dt.id DESC", readerId);
    }

    public List<DatTruocChiTiet> layHangDoiHoatDongTheoSach(int bookId) {
        return doc(SELECT_DETAILS + " WHERE dt.id_sach = ? AND dt.trang_thai IN ('DA_THONG_BAO', 'DANG_CHO')"
                + " ORDER BY FIELD(dt.trang_thai, 'DA_THONG_BAO', 'DANG_CHO'), dt.ngay_dat, dt.id", bookId);
    }

    public List<DatTruocChiTiet> getDangGiuConHanTheoSach(Connection conn, int bookId) throws SQLException {
        return doc(conn, SELECT_DETAILS + """
                 WHERE dt.id_sach = ? AND dt.trang_thai = 'DA_THONG_BAO'
                   AND dt.han_nhan_sach >= NOW() AND dg.trang_thai = 'HOAT_DONG'
                   AND dg.diem_tich_luy >= -50
                 ORDER BY dt.ngay_dat, dt.id
                """, bookId);
    }

    public int demDangChoTheoSach(Connection conn, int bookId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM dat_truoc WHERE id_sach = ? AND trang_thai = 'DANG_CHO'")) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    private List<DatTruocChiTiet> doc(String sql, Object... args) {
        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich()) {
            return doc(conn, sql, args);
        } catch (SQLException ex) {
            throw new IllegalStateException("Không đọc được dữ liệu đặt trước.", ex);
        }
    }

    private List<DatTruocChiTiet> doc(Connection conn, String sql, Object... args) throws SQLException {
        List<DatTruocChiTiet> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs, true));
            }
        }
        return result;
    }

    private DatTruocChiTiet mapRow(ResultSet rs, boolean details) throws SQLException {
        DatTruocChiTiet dt = new DatTruocChiTiet();
        dt.setId(rs.getInt("id"));
        dt.setMaDatTruoc(rs.getString("ma_dat_truoc"));
        dt.setIdDocGia(rs.getInt("id_doc_gia"));
        dt.setIdSach(rs.getInt("id_sach"));
        dt.setNgayDat(rs.getTimestamp("ngay_dat"));
        dt.setHanNhanSach(rs.getTimestamp("han_nhan_sach"));
        dt.setTrangThai(rs.getString("trang_thai"));
        if (details) {
            dt.setMaDocGia(rs.getString("ma_doc_gia"));
            dt.setTenDocGia(rs.getString("ten_doc_gia"));
            dt.setEmailDocGia(rs.getString("email_doc_gia"));
            dt.setSoDienThoai(rs.getString("so_dien_thoai"));
            dt.setMaSach(rs.getString("ma_sach"));
            dt.setTenSach(rs.getString("ten_sach"));
        }
        return dt;
    }
}
