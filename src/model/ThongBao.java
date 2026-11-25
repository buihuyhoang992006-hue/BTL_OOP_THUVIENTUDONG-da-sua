package model;

import java.time.LocalDateTime;

public class ThongBao {

    private int maThongBao;
    private int maNguoiDung;
    private String loaiThongBao;
    private String noiDung;
    private String kenhGui;
    private LocalDateTime ngayTao;
    private LocalDateTime ngayGui;
    private String trangThai;

    public ThongBao() {
    }

    public ThongBao(int maThongBao,
                    int maNguoiDung,
                    String loaiThongBao,
                    String noiDung,
                    String kenhGui,
                    LocalDateTime ngayTao,
                    LocalDateTime ngayGui,
                    String trangThai) {

        this.maThongBao = maThongBao;
        this.maNguoiDung = maNguoiDung;
        this.loaiThongBao = loaiThongBao;
        this.noiDung = noiDung;
        this.kenhGui = kenhGui;
        this.ngayTao = ngayTao;
        this.ngayGui = ngayGui;
        this.trangThai = trangThai;
    }

    public int getMaThongBao() {
        return maThongBao;
    }

    public void setMaThongBao(int maThongBao) {
        this.maThongBao = maThongBao;
    }

    public int getMaNguoiDung() {
        return maNguoiDung;
    }

    public void setMaNguoiDung(int maNguoiDung) {
        this.maNguoiDung = maNguoiDung;
    }

    public String getLoaiThongBao() {
        return loaiThongBao;
    }

    public void setLoaiThongBao(String loaiThongBao) {
        this.loaiThongBao = loaiThongBao;
    }

    public String getNoiDung() {
        return noiDung;
    }

    public void setNoiDung(String noiDung) {
        this.noiDung = noiDung;
    }

    public String getKenhGui() {
        return kenhGui;
    }

    public void setKenhGui(String kenhGui) {
        this.kenhGui = kenhGui;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }

    public LocalDateTime getNgayGui() {
        return ngayGui;
    }

    public void setNgayGui(LocalDateTime ngayGui) {
        this.ngayGui = ngayGui;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }
}