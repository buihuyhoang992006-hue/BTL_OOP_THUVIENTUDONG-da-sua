package model;

import java.sql.Timestamp;
import java.time.LocalDate;

public class ThongBaoChiTiet {

    // Các trường trong bảng thong_bao.
    private int id;
    private String maThongBao;
    private int idDocGia;
    private String loaiThongBao;
    private String noiDung;
    private String kenhGui;
    private Timestamp ngayTao;
    private Timestamp ngayGui;
    private String trangThai;

    private Integer idChiTiet;
    private String mocNhac;
    private String emailNhan;
    private String tieuDe;
    private int soLanThu;
    private Timestamp lanThuCuoi;
    private String loiGui;

    // Thông tin hiện tại lấy bằng JOIN.
    private String maDocGia;
    private String tenDocGia;
    private String emailDocGia;
    private String maPhieu;
    private String tenSach;
    private int soLuong;
    private LocalDate hanTra;

    public ThongBaoChiTiet() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMaThongBao() {
        return maThongBao;
    }

    public void setMaThongBao(String maThongBao) {
        this.maThongBao = maThongBao;
    }

    public int getIdDocGia() {
        return idDocGia;
    }

    public void setIdDocGia(int idDocGia) {
        this.idDocGia = idDocGia;
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

    public Timestamp getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(Timestamp ngayTao) {
        this.ngayTao = ngayTao;
    }

    public Timestamp getNgayGui() {
        return ngayGui;
    }

    public void setNgayGui(Timestamp ngayGui) {
        this.ngayGui = ngayGui;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public Integer getIdChiTiet() {
        return idChiTiet;
    }

    public void setIdChiTiet(Integer idChiTiet) {
        this.idChiTiet = idChiTiet;
    }

    public String getMocNhac() {
        return mocNhac;
    }

    public void setMocNhac(String mocNhac) {
        this.mocNhac = mocNhac;
    }

    public String getEmailNhan() {
        return emailNhan;
    }

    public void setEmailNhan(String emailNhan) {
        this.emailNhan = emailNhan;
    }

    public String getTieuDe() {
        return tieuDe;
    }

    public void setTieuDe(String tieuDe) {
        this.tieuDe = tieuDe;
    }

    public int getSoLanThu() {
        return soLanThu;
    }

    public void setSoLanThu(int soLanThu) {
        this.soLanThu = soLanThu;
    }

    public Timestamp getLanThuCuoi() {
        return lanThuCuoi;
    }

    public void setLanThuCuoi(Timestamp lanThuCuoi) {
        this.lanThuCuoi = lanThuCuoi;
    }

    public String getLoiGui() {
        return loiGui;
    }

    public void setLoiGui(String loiGui) {
        this.loiGui = loiGui;
    }

    public String getMaDocGia() {
        return maDocGia;
    }

    public void setMaDocGia(String maDocGia) {
        this.maDocGia = maDocGia;
    }

    public String getTenDocGia() {
        return tenDocGia;
    }

    public void setTenDocGia(String tenDocGia) {
        this.tenDocGia = tenDocGia;
    }

    public String getEmailDocGia() {
        return emailDocGia;
    }

    public void setEmailDocGia(String emailDocGia) {
        this.emailDocGia = emailDocGia;
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

    public int getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(int soLuong) {
        this.soLuong = soLuong;
    }

    public LocalDate getHanTra() {
        return hanTra;
    }

    public void setHanTra(LocalDate hanTra) {
        this.hanTra = hanTra;
    }
}