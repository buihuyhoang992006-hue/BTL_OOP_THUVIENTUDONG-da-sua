package model;

import java.util.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
/**
 * Lớp ChiTietPhieuMuon
 * Đại diện cho từng cuốn sách trong một phiếu mượn
 */
public class ChiTietPhieuMuon {

    private int id;

    private int idPhieuMuon;

    private int idSach;
    private String maSach;
    private String tenSach;

    private int soLuong;

    private Date hanTra;
    private Date ngayTra;

    private String trangThai;

    private int soNgayQuaHan;

    private String tinhTrangTra;
    private String ghiChuTra;
    private long donGiaThue;
    // Constructor rỗng
    public ChiTietPhieuMuon() {
        this.trangThai = "DANG_MUON";
        this.soNgayQuaHan = 0;
    }


    // Constructor thường dùng khi thêm sách vào phiếu
    public ChiTietPhieuMuon(int idSach,
                            String maSach,
                            String tenSach,
                            int soLuong) {

        this();

        this.idSach = idSach;
        this.maSach = maSach;
        this.tenSach = tenSach;
        this.soLuong = soLuong;
    }


    // ================= GETTER / SETTER =================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public int getIdPhieuMuon() {
        return idPhieuMuon;
    }

    public void setIdPhieuMuon(int idPhieuMuon) {
        this.idPhieuMuon = idPhieuMuon;
    }


    public int getIdSach() {
        return idSach;
    }

    public void setIdSach(int idSach) {
        this.idSach = idSach;
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


    public int getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(int soLuong) {
        this.soLuong = soLuong;
    }

    public Date getHanTra() {
        return hanTra;
    }

    public void setHanTra(Date hanTra) {
        this.hanTra = hanTra;
    }

    public Date getNgayTra() {
        return ngayTra;
    }

    public void setNgayTra(Date ngayTra) {
        this.ngayTra = ngayTra;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public int getSoNgayQuaHan() {
        return soNgayQuaHan;
    }

    public void setSoNgayQuaHan(int soNgayQuaHan) {
        this.soNgayQuaHan = soNgayQuaHan;
    }
    public String getTinhTrangTra() {
        return tinhTrangTra;
    }

    public void setTinhTrangTra(String tinhTrangTra) {
        this.tinhTrangTra = tinhTrangTra;
    }

    public String getGhiChuTra() {
        return ghiChuTra;
    }

    public void setGhiChuTra(String ghiChuTra) {
        this.ghiChuTra = ghiChuTra;
    }

    public long getDonGiaThue() {
        return donGiaThue;
    }

    public void setDonGiaThue(long donGiaThue) {
        this.donGiaThue = Math.max(0L, donGiaThue);
    }

    private LocalDate chuyenNgay(Date value) {
        return new java.sql.Date(value.getTime()).toLocalDate();
    }

    public int tinhSoNgayQuaHan(LocalDate ngayXet) {
        if (hanTra == null || ngayXet == null) {
            throw new IllegalStateException(
                    "Thiếu hạn trả hoặc ngày xét.");
        }

        LocalDate ngayKetThuc = ngayTra == null
                ? ngayXet
                : chuyenNgay(ngayTra);

        return (int) Math.max(
                0,
                ChronoUnit.DAYS.between(
                        chuyenNgay(hanTra), ngayKetThuc)
        );
    }

    public String xacDinhTrangThai(LocalDate ngayXet) {
        if (hanTra == null || ngayXet == null) {
            throw new IllegalStateException(
                    "Thiếu hạn trả hoặc ngày xét.");
        }

        LocalDate han = chuyenNgay(hanTra);

        if (ngayTra == null) {
            return ngayXet.isAfter(han)
                    ? "CHUA_TRA_QUA_HAN"
                    : "DANG_MUON";
        }

        LocalDate ngayKetThuc = chuyenNgay(ngayTra);

        if (ngayKetThuc.isBefore(han)) {
            return "TRA_SOM";
        }

        return ngayKetThuc.isEqual(han)
                ? "DUNG_HAN"
                : "QUA_HAN";
    }
}