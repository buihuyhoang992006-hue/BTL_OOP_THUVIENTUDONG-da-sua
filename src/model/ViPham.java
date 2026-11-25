package model;

import java.util.Date;

public class ViPham {

    private int id;
    private String maViPham;

    private int idDocGia;
    private String tenDocGia;

    private Integer idChiTiet;

    // Thông tin phụ để hiển thị
    private String maPhieu;
    private String tenSach;

    private String loaiViPham;
    private int soNgayQuaHan;

    private Date ngayGhiNhan;

    private String moTa;
    private String trangThai;
    private long tienPhat;
    private Date ngayXuLy;
    private Integer diemKhoiPhuc;
    private String ghiChuXuLy;


    public ViPham() {
    }


    public ViPham(
            int idDocGia,
            Integer idChiTiet,
            String loaiViPham,
            int soNgayQuaHan,
            String moTa
    ) {

        this.idDocGia = idDocGia;
        this.idChiTiet = idChiTiet;
        this.loaiViPham = loaiViPham;
        this.soNgayQuaHan = soNgayQuaHan;
        this.moTa = moTa;
        this.trangThai = "CHUA_XU_LY";
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getMaViPham() {
        return maViPham;
    }

    public void setMaViPham(String maViPham) {
        this.maViPham = maViPham;
    }


    public int getIdDocGia() {
        return idDocGia;
    }

    public void setIdDocGia(int idDocGia) {
        this.idDocGia = idDocGia;
    }


    public String getTenDocGia() {
        return tenDocGia;
    }

    public void setTenDocGia(String tenDocGia) {
        this.tenDocGia = tenDocGia;
    }


    public Integer getIdChiTiet() {
        return idChiTiet;
    }

    public void setIdChiTiet(Integer idChiTiet) {
        this.idChiTiet = idChiTiet;
    }


    public String getMaPhieu() {
        return maPhieu;
    }

    public void setMaPhieu(String maPhieu) {
        this.maPhieu = maPhieu;
    }


    public String getTenSach() {
        return tenSach;
    }

    public void setTenSach(String tenSach) {
        this.tenSach = tenSach;
    }


    public String getLoaiViPham() {
        return loaiViPham;
    }

    public void setLoaiViPham(String loaiViPham) {
        this.loaiViPham = loaiViPham;
    }


    public int getSoNgayQuaHan() {
        return soNgayQuaHan;
    }

    public void setSoNgayQuaHan(int soNgayQuaHan) {
        this.soNgayQuaHan = soNgayQuaHan;
    }


    public Date getNgayGhiNhan() {
        return ngayGhiNhan;
    }

    public void setNgayGhiNhan(Date ngayGhiNhan) {
        this.ngayGhiNhan = ngayGhiNhan;
    }


    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }


    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public long getTienPhat() {
        return tienPhat;
    }

    public void setTienPhat(long tienPhat) {
        this.tienPhat = Math.max(0L, tienPhat);
    }

    public Date getNgayXuLy() {
        return ngayXuLy;
    }

    public void setNgayXuLy(Date ngayXuLy) {
        this.ngayXuLy = ngayXuLy;
    }

    public Integer getDiemKhoiPhuc() {
        return diemKhoiPhuc;
    }

    public void setDiemKhoiPhuc(Integer diemKhoiPhuc) {
        this.diemKhoiPhuc = diemKhoiPhuc;
    }

    public String getGhiChuXuLy() {
        return ghiChuXuLy;
    }

    public void setGhiChuXuLy(String ghiChuXuLy) {
        this.ghiChuXuLy = ghiChuXuLy;
    }
}