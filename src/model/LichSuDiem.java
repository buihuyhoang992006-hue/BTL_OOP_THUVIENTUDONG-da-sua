package model;

import java.time.LocalDateTime;

public class LichSuDiem {

    private int id;
    private String maLichSu;
    private int idDocGia;

    private int soDiem;
    private String loai;
    private String noiDung;

    private LocalDateTime ngayTao;

    public LichSuDiem() {
        this.ngayTao = LocalDateTime.now();
    }

    public LichSuDiem(int id,
                      int idDocGia,
                      int soDiem,
                      String loai,
                      String noiDung,
                      LocalDateTime ngayTao) {

        this.id = id;
        this.idDocGia = idDocGia;
        this.soDiem = soDiem;
        this.loai = loai;
        this.noiDung = noiDung;
        this.ngayTao = ngayTao;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMaLichSu() {
        return maLichSu;
    }

    public void setMaLichSu(String maLichSu) {
        this.maLichSu = maLichSu;
    }

    public int getIdDocGia() {
        return idDocGia;
    }

    public void setIdDocGia(int idDocGia) {
        this.idDocGia = idDocGia;
    }

    public int getSoDiem() {
        return soDiem;
    }

    public void setSoDiem(int soDiem) {
        this.soDiem = soDiem;
    }

    public String getLoai() {
        return loai;
    }

    public void setLoai(String loai) {
        this.loai = loai;
    }

    public String getNoiDung() {
        return noiDung;
    }

    public void setNoiDung(String noiDung) {
        this.noiDung = noiDung;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }
}