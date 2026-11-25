package model;

import java.sql.Timestamp;

/**
 * Lớp NguoiDung - kế thừa từ Person
 * Đại diện cho bảng nguoi_dung trong CSDL thu_vien_tu_dong
 * Áp dụng: Inheritance (extends), Encapsulation, Polymorphism (Override)
 */
public class NguoiDung extends Person {

    public static final String VAI_TRO_ADMIN = "ADMIN";
    public static final String VAI_TRO_THU_THU = "THU_THU";

    private String tenDangNhap;
    private String matKhau;
    private String vaiTro;       // ADMIN hoặc THU_THU
    private boolean trangThai;   // true (1) = hoạt động, false (0) = bị khóa
    private Timestamp ngayTao;   // Thời gian tạo tài khoản

    public NguoiDung() {
        super();
        this.vaiTro = VAI_TRO_THU_THU;
        this.trangThai = true;
    }

    public NguoiDung(int id, String hoTen, String soDienThoai, String email,
                     String diaChi, String tenDangNhap, String matKhau, String vaiTro) {
        super(id, hoTen, soDienThoai, email, diaChi);
        this.tenDangNhap = tenDangNhap;
        this.matKhau = matKhau;
        this.vaiTro = vaiTro;
        this.trangThai = true;
    }

    public NguoiDung(int id, String hoTen, String soDienThoai, String email,
                     String diaChi, String tenDangNhap, String matKhau,
                     String vaiTro, boolean trangThai, Timestamp ngayTao) {
        super(id, hoTen, soDienThoai, email, diaChi);
        this.tenDangNhap = tenDangNhap;
        this.matKhau = matKhau;
        this.vaiTro = vaiTro;
        this.trangThai = trangThai;
        this.ngayTao = ngayTao;
    }

    @Override
    public String getLoai() {
        return getTenVaiTro();
    }

    @Override
    public String getThongTinChiTiet() {
        return "Họ tên: " + this.hoTen +
               "\nTài khoản: " + this.tenDangNhap +
               "\nVai trò: " + getTenVaiTro() + " (" + this.vaiTro + ")" +
               "\nSĐT: " + this.soDienThoai +
               "\nEmail: " + this.email +
               "\nTrạng thái: " + (this.trangThai ? "Đang hoạt động" : "Đã khóa");
    }

    // Hiển thị tên vai trò tiếng Việt trên giao diện
    public String getTenVaiTro() {
        if (isAdmin()) {
            return "Quản trị viên";
        }
        return "Thủ thư";
    }

    // Kiểm tra quyền ADMIN
    public boolean isAdmin() {
        return VAI_TRO_ADMIN.equalsIgnoreCase(this.vaiTro);
    }

    // Kiểm tra quyền THU_THU
    public boolean isThuThu() {
        return VAI_TRO_THU_THU.equalsIgnoreCase(this.vaiTro);
    }

    // Getters & Setters
    public String getTenDangNhap() { return tenDangNhap; }
    public void setTenDangNhap(String tenDangNhap) { this.tenDangNhap = tenDangNhap; }

    public String getMatKhau() { return matKhau; }
    public void setMatKhau(String matKhau) { this.matKhau = matKhau; }

    public String getVaiTro() { return vaiTro; }
    public void setVaiTro(String vaiTro) { this.vaiTro = vaiTro; }

    public boolean isTrangThai() { return trangThai; }
    public void setTrangThai(boolean trangThai) { this.trangThai = trangThai; }

    public Timestamp getNgayTao() { return ngayTao; }
    public void setNgayTao(Timestamp ngayTao) { this.ngayTao = ngayTao; }
}