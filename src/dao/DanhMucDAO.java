package dao;

import model.DanhMuc;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DanhMucDAO - Truy vấn dữ liệu danh mục sách
 */
public class DanhMucDAO {
    private Connection conn;

    public DanhMucDAO() {
        this.conn = KetNoiDatabase.getInstance().getConnection();
    }

    public List<DanhMuc> layTatCaDanhMuc() {
        List<DanhMuc> ds = new ArrayList<>();
        String sql = "SELECT * FROM danh_muc ORDER BY ten_danh_muc";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                ds.add(new DanhMuc(rs.getInt("id"), rs.getString("ten_danh_muc"), rs.getString("mo_ta")));
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy danh mục: " + e.getMessage());
        }
        return ds;
    }

    public boolean themDanhMuc(DanhMuc dm) {
        String sql = "INSERT INTO danh_muc (ten_danh_muc, mo_ta) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dm.getTenDanhMuc());
            ps.setString(2, dm.getMoTa());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi thêm danh mục: " + e.getMessage());
            return false;
        }
    }

    public boolean capNhatDanhMuc(DanhMuc dm) {
        String sql = "UPDATE danh_muc SET ten_danh_muc=?, mo_ta=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dm.getTenDanhMuc());
            ps.setString(2, dm.getMoTa());
            ps.setInt(3, dm.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi cập nhật danh mục: " + e.getMessage());
            return false;
        }
    }

    public boolean xoaDanhMuc(int id) {
        String sql = "DELETE FROM danh_muc WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi xóa danh mục: " + e.getMessage());
            return false;
        }
    }
}
