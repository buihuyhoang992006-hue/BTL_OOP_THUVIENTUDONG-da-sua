package model;

/**
 * Lớp trừu tượng Person - Lớp cha cho User và Employee
 * Áp dụng: Abstract, Encapsulation (OOP)
 */
public abstract class Person {
    protected int id;
    protected String hoTen;
    protected String soDienThoai;
    protected String email;
    protected String diaChi;

    // Constructor mặc định
    public Person() {}

    // Constructor có tham số
    public Person(int id, String hoTen, String soDienThoai, String email, String diaChi) {
        this.id = id;
        this.hoTen = hoTen;
        this.soDienThoai = soDienThoai;
        this.email = email;
        this.diaChi = diaChi;
    }

    // Phương thức trừu tượng - bắt buộc lớp con override
    public abstract String getLoai();
    public abstract String getThongTinChiTiet();

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }

    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }

    @Override
    public String toString() {
        return "[" + getLoai() + "] " + hoTen + " - SĐT: " + soDienThoai;
    }
}
