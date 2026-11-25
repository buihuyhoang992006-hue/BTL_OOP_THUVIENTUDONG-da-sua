package dao;

import model.NguoiDung;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NguoiDungDAO {
    public enum TrangThaiDangNhap { THANH_CONG, SAI_THONG_TIN, DA_KHOA }
    public record KetQuaDangNhap(TrangThaiDangNhap trangThai, NguoiDung nguoiDung) {}

    public enum KetQuaCapNhat {
        THANH_CONG,
        HET_QUYEN,
        KHONG_TIM_THAY,
        TRANG_THAI_DA_DOI,
        ADMIN_CUOI_CUNG,
        TAI_KHOAN_DANG_DUNG
    }

    private record TrangThaiTaiKhoan(boolean hoatDong, boolean admin) {
        boolean quanTriHoatDong() {
            return hoatDong && admin;
        }
    }

    private record NhomTaiKhoan(boolean conQuyenQuanTri, TrangThaiTaiKhoan mucTieu, int soAdminHoatDong) {}

    @FunctionalInterface
    private interface NghiepVu<T> {
        T thucHien(Connection conn) throws SQLException;
    }

    public KetQuaDangNhap xacThuc(String tenDangNhap, String matKhau) throws SQLException {
        String sql = "SELECT * FROM nguoi_dung WHERE ten_dang_nhap = ? FOR UPDATE";
        return giaoDich(conn -> {
            NguoiDung nguoiDung;
            String matKhauDaLuu;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, tenDangNhap);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return new KetQuaDangNhap(TrangThaiDangNhap.SAI_THONG_TIN, null);
                    }
                    nguoiDung = mapResultSet(rs);
                    matKhauDaLuu = rs.getString("mat_khau");
                }
            }
            if (!MatKhauNguoiDung.dung(matKhau, matKhauDaLuu)) {
                return new KetQuaDangNhap(TrangThaiDangNhap.SAI_THONG_TIN, null);
            }
            if (!nguoiDung.isTrangThai()) {
                return new KetQuaDangNhap(TrangThaiDangNhap.DA_KHOA, null);
            }
            if (!MatKhauNguoiDung.daBam(matKhauDaLuu)) {
                luuMatKhau(conn, nguoiDung.getId(), MatKhauNguoiDung.bam(matKhau));
            }
            return new KetQuaDangNhap(TrangThaiDangNhap.THANH_CONG, nguoiDung);
        });
    }

    public NguoiDung dangNhap(String tenDangNhap, String matKhau) throws SQLException {
        return xacThuc(tenDangNhap, matKhau).nguoiDung();
    }

    public boolean doiMatKhauHienTai(int id, String matKhauCu, String matKhauMoi) throws SQLException {
        if (matKhauMoi == null || matKhauMoi.isBlank()) {
            return false;
        }
        return giaoDich(conn -> {
            String matKhauDaLuu;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT mat_khau FROM nguoi_dung WHERE id = ? AND trang_thai = 1 FOR UPDATE")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                    matKhauDaLuu = rs.getString("mat_khau");
                }
            }
            if (!MatKhauNguoiDung.dung(matKhauCu, matKhauDaLuu)) {
                return false;
            }
            luuMatKhau(conn, id, MatKhauNguoiDung.bam(matKhauMoi));
            return true;
        });
    }

    public List<NguoiDung> layTatCaNguoiDung(int idQuanTri) throws SQLException {
        String sql = """
                SELECT nd.id, nd.ho_ten, nd.so_dien_thoai, nd.email, nd.dia_chi,
                       nd.ten_dang_nhap, nd.vai_tro, nd.trang_thai, nd.ngay_tao
                FROM nguoi_dung nd
                JOIN nguoi_dung qt ON qt.id = ? AND qt.vai_tro = 'ADMIN' AND qt.trang_thai = 1
                ORDER BY nd.id DESC
                """;
        List<NguoiDung> danhSach = new ArrayList<>();
        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idQuanTri);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    danhSach.add(mapResultSet(rs));
                }
            }
        }
        return danhSach;
    }

    public KetQuaCapNhat themNguoiDung(int idQuanTri, NguoiDung nd) throws SQLException {
        String matKhauDaBam = MatKhauNguoiDung.bam(nd.getMatKhau());
        String sql = """
                INSERT INTO nguoi_dung (ho_ten, so_dien_thoai, email, dia_chi,
                                        ten_dang_nhap, mat_khau, vai_tro, trang_thai)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        return giaoDich(conn -> {
            if (!khoaTaiKhoan(conn, idQuanTri, 0).conQuyenQuanTri()) {
                return KetQuaCapNhat.HET_QUYEN;
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nd.getHoTen());
                ps.setString(2, nd.getSoDienThoai());
                ps.setString(3, nd.getEmail());
                ps.setString(4, nd.getDiaChi());
                ps.setString(5, nd.getTenDangNhap());
                ps.setString(6, matKhauDaBam);
                ps.setString(7, nd.getVaiTro());
                ps.setBoolean(8, nd.isTrangThai());
                ps.executeUpdate();
            }
            return KetQuaCapNhat.THANH_CONG;
        });
    }

    public KetQuaCapNhat capNhatNguoiDung(int idQuanTri, NguoiDung nd) throws SQLException {
        String sql = """
                UPDATE nguoi_dung
                SET ho_ten = ?, so_dien_thoai = ?, email = ?, dia_chi = ?, vai_tro = ?, trang_thai = ?
                WHERE id = ?
                """;
        return giaoDich(conn -> {
            NhomTaiKhoan nhom = khoaTaiKhoan(conn, idQuanTri, nd.getId());
            KetQuaCapNhat kiemTra = kiemTraThayDoi(nhom, idQuanTri == nd.getId(), nd.isAdmin() && nd.isTrangThai());
            if (kiemTra != KetQuaCapNhat.THANH_CONG) {
                return kiemTra;
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nd.getHoTen());
                ps.setString(2, nd.getSoDienThoai());
                ps.setString(3, nd.getEmail());
                ps.setString(4, nd.getDiaChi());
                ps.setString(5, nd.getVaiTro());
                ps.setBoolean(6, nd.isTrangThai());
                ps.setInt(7, nd.getId());
                ps.executeUpdate();
            }
            return KetQuaCapNhat.THANH_CONG;
        });
    }

    public KetQuaCapNhat doiTrangThaiNguoiDung(int idQuanTri, int id, boolean dangHoatDong) throws SQLException {
        return giaoDich(conn -> {
            NhomTaiKhoan nhom = khoaTaiKhoan(conn, idQuanTri, id);
            if (nhom.conQuyenQuanTri() && nhom.mucTieu() != null
                    && nhom.mucTieu().hoatDong() != dangHoatDong) {
                return KetQuaCapNhat.TRANG_THAI_DA_DOI;
            }
            boolean hoatDongMoi = !dangHoatDong;
            boolean laAdminHoatDong = nhom.mucTieu() != null && nhom.mucTieu().admin() && hoatDongMoi;
            KetQuaCapNhat kiemTra = kiemTraThayDoi(nhom, idQuanTri == id, laAdminHoatDong);
            if (kiemTra != KetQuaCapNhat.THANH_CONG) {
                return kiemTra;
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE nguoi_dung SET trang_thai = ? WHERE id = ?")) {
                ps.setBoolean(1, hoatDongMoi);
                ps.setInt(2, id);
                ps.executeUpdate();
            }
            return KetQuaCapNhat.THANH_CONG;
        });
    }

    private NhomTaiKhoan khoaTaiKhoan(Connection conn, int idQuanTri, int idMucTieu) throws SQLException {
        boolean conQuyenQuanTri = false;
        TrangThaiTaiKhoan mucTieu = null;
        int soAdminHoatDong = 0;

        // Các thao tác quản trị khóa theo cùng thứ tự trước khi xét quyền và đếm admin.
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT id, vai_tro, trang_thai FROM nguoi_dung FORCE INDEX (PRIMARY) ORDER BY id FOR UPDATE
                """); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                TrangThaiTaiKhoan taiKhoan = new TrangThaiTaiKhoan(rs.getBoolean("trang_thai"),
                        NguoiDung.VAI_TRO_ADMIN.equals(rs.getString("vai_tro")));
                if (taiKhoan.quanTriHoatDong()) {
                    soAdminHoatDong++;
                }
                if (id == idQuanTri) {
                    conQuyenQuanTri = taiKhoan.quanTriHoatDong();
                }
                if (id == idMucTieu) {
                    mucTieu = taiKhoan;
                }
            }
        }
        return new NhomTaiKhoan(conQuyenQuanTri, mucTieu, soAdminHoatDong);
    }

    private KetQuaCapNhat kiemTraThayDoi(NhomTaiKhoan nhom, boolean dangTuCapNhat, boolean seLaAdminHoatDong) {
        if (!nhom.conQuyenQuanTri()) {
            return KetQuaCapNhat.HET_QUYEN;
        }
        if (nhom.mucTieu() == null) {
            return KetQuaCapNhat.KHONG_TIM_THAY;
        }
        if (nhom.mucTieu().quanTriHoatDong() && !seLaAdminHoatDong) {
            if (nhom.soAdminHoatDong() == 1) {
                return KetQuaCapNhat.ADMIN_CUOI_CUNG;
            }
            if (dangTuCapNhat) {
                return KetQuaCapNhat.TAI_KHOAN_DANG_DUNG;
            }
        }
        return KetQuaCapNhat.THANH_CONG;
    }

    private <T> T giaoDich(NghiepVu<T> nghiepVu) throws SQLException {
        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich()) {
            conn.setAutoCommit(false);
            try {
                T ketQua = nghiepVu.thucHien(conn);
                conn.commit();
                return ketQua;
            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollBackError) {
                    e.addSuppressed(rollBackError);
                }
                throw e;
            }
        }
    }

    private void luuMatKhau(Connection conn, int id, String matKhauDaBam) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE nguoi_dung SET mat_khau = ? WHERE id = ?")) {
            ps.setString(1, matKhauDaBam);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    private NguoiDung mapResultSet(ResultSet rs) throws SQLException {
        NguoiDung nd = new NguoiDung();
        nd.setId(rs.getInt("id"));
        nd.setHoTen(rs.getString("ho_ten"));
        nd.setSoDienThoai(rs.getString("so_dien_thoai"));
        nd.setEmail(rs.getString("email"));
        nd.setDiaChi(rs.getString("dia_chi"));
        nd.setTenDangNhap(rs.getString("ten_dang_nhap"));
        nd.setVaiTro(rs.getString("vai_tro"));
        nd.setTrangThai(rs.getBoolean("trang_thai"));
        nd.setNgayTao(rs.getTimestamp("ngay_tao"));
        return nd;
    }
}
