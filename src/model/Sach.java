package model;

/**
 * Lớp Sach - đại diện cho sách trong hệ thống thư viện.
 * Áp dụng: Encapsulation, Constructor Overloading.
 */
public class Sach {

    private int id;
    private String maSach;
    private String tenSach;
    private String tacGia;
    private String nhaXuatBan;
    private int namXuatBan;

    private int soLuongTong;
    private int soLuongCon;

    private int idDanhMuc;
    private String tenDanhMuc;

    private String moTa;
    private String hinhAnh;

    private long giaThue = 10_000L;
    private boolean trangThai;

    // Constructor mặc định
    public Sach() {
        this.trangThai = true;
    }

    // Constructor đầy đủ
    public Sach(
            int id,
            String maSach,
            String tenSach,
            String tacGia,
            String nhaXuatBan,
            int namXuatBan,
            int soLuongTong,
            int soLuongCon,
            int idDanhMuc,
            String moTa
    ) {
        this.id = id;
        this.maSach = maSach;
        this.tenSach = tenSach;
        this.tacGia = tacGia;
        this.nhaXuatBan = nhaXuatBan;
        this.namXuatBan = namXuatBan;
        this.soLuongTong = soLuongTong;
        this.soLuongCon = soLuongCon;
        this.idDanhMuc = idDanhMuc;
        this.moTa = moTa;
        this.trangThai = true;
    }

    // Kiểm tra sách còn có thể mượn hay không
    public boolean conSach() {
        return trangThai && soLuongCon > 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMaSach() {
        return maSach;
    }

    public void setMaSach(String maSach) {
        this.maSach = maSach;
    }

    public String getTenSach() {
        return tenSach;
    }

    public void setTenSach(String tenSach) {
        this.tenSach = tenSach;
    }

    public String getTacGia() {
        return tacGia;
    }

    public void setTacGia(String tacGia) {
        this.tacGia = tacGia;
    }

    public String getNhaXuatBan() {
        return nhaXuatBan;
    }

    public void setNhaXuatBan(String nhaXuatBan) {
        this.nhaXuatBan = nhaXuatBan;
    }

    public int getNamXuatBan() {
        return namXuatBan;
    }

    public void setNamXuatBan(int namXuatBan) {
        this.namXuatBan = namXuatBan;
    }

    public int getSoLuongTong() {
        return soLuongTong;
    }

    public void setSoLuongTong(int soLuongTong) {
        this.soLuongTong = soLuongTong;
    }

    public int getSoLuongCon() {
        return soLuongCon;
    }

    public void setSoLuongCon(int soLuongCon) {
        this.soLuongCon = soLuongCon;
    }

    public int getIdDanhMuc() {
        return idDanhMuc;
    }

    public void setIdDanhMuc(int idDanhMuc) {
        this.idDanhMuc = idDanhMuc;
    }

    public String getTenDanhMuc() {
        return tenDanhMuc;
    }

    public void setTenDanhMuc(String tenDanhMuc) {
        this.tenDanhMuc = tenDanhMuc;
    }

    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }

    public String getHinhAnh() {
        return hinhAnh;
    }

    public void setHinhAnh(String hinhAnh) {
        this.hinhAnh = hinhAnh;
    }

    public long getGiaThue() {
        return giaThue;
    }

    public void setGiaThue(long giaThue) {
        this.giaThue = Math.max(0L, giaThue);
    }

    public boolean isTrangThai() {
        return trangThai;
    }

    public void setTrangThai(boolean trangThai) {
        this.trangThai = trangThai;
    }

    @Override
    public String toString() {
        return maSach + " - " + tenSach + " (" + tacGia + ")";
    }
}