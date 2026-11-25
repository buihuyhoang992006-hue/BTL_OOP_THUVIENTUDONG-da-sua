package model;

/**
 * Lớp NhanVien - kế thừa từ Person
 * Áp dụng: Inheritance, Method Overriding
 */
public class NhanVien extends Person {
    private String maNhanVien;
    private String chucVu;
    private double luongCoBan;
    private String ngayVaoLam;

    public NhanVien() {
        super();
    }

    public NhanVien(int id, String hoTen, String soDienThoai, String email,
                    String diaChi, String maNhanVien, String chucVu, double luongCoBan, String ngayVaoLam) {
        super(id, hoTen, soDienThoai, email, diaChi);
        this.maNhanVien = maNhanVien;
        this.chucVu = chucVu;
        this.luongCoBan = luongCoBan;
        this.ngayVaoLam = ngayVaoLam;
    }

    @Override
    public String getLoai() {
        return "Nhân viên";
    }

    @Override
    public String getThongTinChiTiet() {
        return "Nhân viên: " + this.hoTen +
               "\nMã NV: " + this.maNhanVien +
               "\nChức vụ: " + this.chucVu +
               "\nLương cơ bản: " + String.format("%,.0f VNĐ", luongCoBan) +
               "\nNgày vào làm: " + this.ngayVaoLam;
    }

    public String getMaNhanVien() { return maNhanVien; }
    public void setMaNhanVien(String maNhanVien) { this.maNhanVien = maNhanVien; }

    public String getChucVu() { return chucVu; }
    public void setChucVu(String chucVu) { this.chucVu = chucVu; }

    public double getLuongCoBan() { return luongCoBan; }
    public void setLuongCoBan(double luongCoBan) { this.luongCoBan = luongCoBan; }

    public String getNgayVaoLam() { return ngayVaoLam; }
    public void setNgayVaoLam(String ngayVaoLam) { this.ngayVaoLam = ngayVaoLam; }
}
