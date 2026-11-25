package model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Voucher {

    private int id;
    private String maVoucher;
    private int idDocGia;

    private String tenUuDai;
    private String loaiGiam; // "TIEN_MAT" | "PHAN_TRAM"
    private double giaTriGiam;
    private Double giamToiDa;

    private double phanTramGiam;

    private LocalDate ngayTao;
    private LocalDate hanSuDung;
    private LocalDateTime ngayDoi;
    private LocalDateTime ngaySuDung;

    private String trangThai;

    public Voucher() {
        this.loaiGiam = "TIEN_MAT";
        this.giaTriGiam = 5000.0;
        this.giamToiDa = 5000.0;
        this.tenUuDai = "Giảm 5.000đ phí thuê sách";
        this.phanTramGiam = 0.0;
        this.ngayTao = LocalDate.now();
        this.ngayDoi = LocalDateTime.now();
        this.trangThai = "CHUA_DUNG";
    }

    public Voucher(int id,
                   String maVoucher,
                   int idDocGia,
                   double phanTramGiam,
                   LocalDate ngayTao,
                   LocalDate hanSuDung,
                   String trangThai) {
        this.id = id;
        this.maVoucher = maVoucher;
        this.idDocGia = idDocGia;
        this.loaiGiam = "PHAN_TRAM";
        this.giaTriGiam = phanTramGiam;
        this.phanTramGiam = phanTramGiam;
        this.tenUuDai = "Giảm " + Math.round(phanTramGiam) + "% phí thuê sách";
        this.ngayTao = ngayTao;
        this.hanSuDung = hanSuDung;
        this.ngayDoi = ngayTao != null ? ngayTao.atStartOfDay() : LocalDateTime.now();
        this.trangThai = trangThai;
    }

    public boolean conHan() {
        return ("CHUA_DUNG".equalsIgnoreCase(trangThai) || "CHUA_SU_DUNG".equalsIgnoreCase(trangThai))
                && (hanSuDung == null || !LocalDate.now().isAfter(hanSuDung));
    }

    /**
     * Tính số tiền được giảm (VNĐ) khi áp dụng Voucher lên tổng tiền thuê.
     * Hỗ trợ cả giảm tiền mặt cố định và giảm theo % có giới hạn tối đa.
     */
    public long tinhSoTienGiam(long tongTienThue) {
        if (tongTienThue <= 0) {
            return 0L;
        }
        long giam;
        if ("TIEN_MAT".equalsIgnoreCase(loaiGiam)) {
            giam = Math.round(giaTriGiam);
        } else {
            double tyLe = ("PHAN_TRAM".equalsIgnoreCase(loaiGiam) && giaTriGiam > 0)
                    ? giaTriGiam
                    : phanTramGiam;
            giam = Math.round(tongTienThue * (tyLe / 100.0));
        }
        if (giamToiDa != null && giamToiDa > 0) {
            giam = Math.min(giam, Math.round(giamToiDa));
        }
        return Math.min(tongTienThue, Math.max(0L, giam));
    }

    public String moTaUuDai() {
        if (tenUuDai != null && !tenUuDai.isBlank()) {
            return tenUuDai;
        }
        if ("TIEN_MAT".equalsIgnoreCase(loaiGiam)) {
            return "Giảm " + String.format("%,dđ", Math.round(giaTriGiam)) + " phí thuê sách";
        }
        String coBan = "Giảm " + Math.round(giaTriGiam > 0 ? giaTriGiam : phanTramGiam) + "% hóa đơn";
        if (giamToiDa != null && giamToiDa > 0) {
            return coBan + " (tối đa " + String.format("%,dđ", Math.round(giamToiDa)) + ")";
        }
        return coBan;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMaVoucher() {
        return maVoucher;
    }

    public void setMaVoucher(String maVoucher) {
        this.maVoucher = maVoucher;
    }

    public int getIdDocGia() {
        return idDocGia;
    }

    public void setIdDocGia(int idDocGia) {
        this.idDocGia = idDocGia;
    }

    public String getTenUuDai() {
        return tenUuDai;
    }

    public void setTenUuDai(String tenUuDai) {
        this.tenUuDai = tenUuDai;
    }

    public String getLoaiGiam() {
        return loaiGiam;
    }

    public void setLoaiGiam(String loaiGiam) {
        this.loaiGiam = loaiGiam;
    }

    public double getGiaTriGiam() {
        return giaTriGiam;
    }

    public void setGiaTriGiam(double giaTriGiam) {
        this.giaTriGiam = giaTriGiam;
        if ("PHAN_TRAM".equalsIgnoreCase(loaiGiam)) {
            this.phanTramGiam = giaTriGiam;
        }
    }

    public Double getGiamToiDa() {
        return giamToiDa;
    }

    public void setGiamToiDa(Double giamToiDa) {
        this.giamToiDa = giamToiDa;
    }

    public double getPhanTramGiam() {
        return ("PHAN_TRAM".equalsIgnoreCase(loaiGiam) && giaTriGiam > 0) ? giaTriGiam : phanTramGiam;
    }

    public void setPhanTramGiam(double phanTramGiam) {
        this.phanTramGiam = phanTramGiam;
        if ("PHAN_TRAM".equalsIgnoreCase(loaiGiam)) {
            this.giaTriGiam = phanTramGiam;
        }
    }

    public LocalDate getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDate ngayTao) {
        this.ngayTao = ngayTao;
    }

    public LocalDate getHanSuDung() {
        return hanSuDung;
    }

    public void setHanSuDung(LocalDate hanSuDung) {
        this.hanSuDung = hanSuDung;
    }

    public LocalDateTime getNgayDoi() {
        return ngayDoi;
    }

    public void setNgayDoi(LocalDateTime ngayDoi) {
        this.ngayDoi = ngayDoi;
        if (ngayDoi != null) {
            this.ngayTao = ngayDoi.toLocalDate();
        }
    }

    public LocalDateTime getNgaySuDung() {
        return ngaySuDung;
    }

    public void setNgaySuDung(LocalDateTime ngaySuDung) {
        this.ngaySuDung = ngaySuDung;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    @Override
    public String toString() {
        return maVoucher + " — " + moTaUuDai();
    }
}
