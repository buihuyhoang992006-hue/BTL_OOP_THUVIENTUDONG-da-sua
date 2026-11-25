package model;

import java.time.LocalDate;

public class DatTruoc {

    private int maDatTruoc;
    private int maDocGia;
    private int maSach;
    private LocalDate ngayDat;
    private LocalDate hanNhanSach;
    private String trangThai;

    public DatTruoc() {
    }

    public DatTruoc(int maDatTruoc, int maDocGia, int maSach,
                    LocalDate ngayDat, LocalDate hanNhanSach,
                    String trangThai) {
        this.maDatTruoc = maDatTruoc;
        this.maDocGia = maDocGia;
        this.maSach = maSach;
        this.ngayDat = ngayDat;
        this.hanNhanSach = hanNhanSach;
        this.trangThai = trangThai;
    }

    public int getMaDatTruoc() {
        return maDatTruoc;
    }

    public void setMaDatTruoc(int maDatTruoc) {
        this.maDatTruoc = maDatTruoc;
    }

    public int getMaDocGia() {
        return maDocGia;
    }

    public void setMaDocGia(int maDocGia) {
        this.maDocGia = maDocGia;
    }

    public int getMaSach() {
        return maSach;
    }

    public void setMaSach(int maSach) {
        this.maSach = maSach;
    }

    public LocalDate getNgayDat() {
        return ngayDat;
    }

    public void setNgayDat(LocalDate ngayDat) {
        this.ngayDat = ngayDat;
    }

    public LocalDate getHanNhanSach() {
        return hanNhanSach;
    }

    public void setHanNhanSach(LocalDate hanNhanSach) {
        this.hanNhanSach = hanNhanSach;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }
}