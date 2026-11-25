package model;

import java.sql.Timestamp;

public class DatTruocChiTiet {
    private int id;
    private String maDatTruoc;
    private int idDocGia;
    private int idSach;
    private Timestamp ngayDat;
    private Timestamp hanNhanSach;
    private String trangThai; // DANG_CHO, DA_THONG_BAO, DA_NHAN, DA_HUY, HET_HAN
    private String maDocGia;
    private String tenDocGia;
    private String soDienThoai;
    private String emailDocGia;
    private String maSach;
    private String tenSach;

    public DatTruocChiTiet() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getMaDatTruoc() { return maDatTruoc; }
    public void setMaDatTruoc(String maDatTruoc) { this.maDatTruoc = maDatTruoc; }
    public int getIdDocGia() { return idDocGia; }
    public void setIdDocGia(int idDocGia) { this.idDocGia = idDocGia; }
    public int getIdSach() { return idSach; }
    public void setIdSach(int idSach) { this.idSach = idSach; }
    public Timestamp getNgayDat() { return ngayDat; }
    public void setNgayDat(Timestamp ngayDat) { this.ngayDat = ngayDat; }
    public Timestamp getHanNhanSach() { return hanNhanSach; }
    public void setHanNhanSach(Timestamp hanNhanSach) { this.hanNhanSach = hanNhanSach; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public String getMaDocGia() { return maDocGia; }
    public void setMaDocGia(String maDocGia) { this.maDocGia = maDocGia; }
    public String getTenDocGia() { return tenDocGia; }
    public void setTenDocGia(String tenDocGia) { this.tenDocGia = tenDocGia; }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    public String getEmailDocGia() { return emailDocGia; }
    public void setEmailDocGia(String emailDocGia) { this.emailDocGia = emailDocGia; }
    public String getMaSach() { return maSach; }
    public void setMaSach(String maSach) { this.maSach = maSach; }
    public String getTenSach() { return tenSach; }
    public void setTenSach(String tenSach) { this.tenSach = tenSach; }
}