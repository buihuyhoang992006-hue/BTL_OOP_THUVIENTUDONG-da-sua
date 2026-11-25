package model;

import java.time.LocalDate;

/**
 * Lớp DocGia - đại diện cho độc giả trong hệ thống thư viện.
 * Áp dụng: Encapsulation, Constructor Overloading.
 */
public class DocGia extends Person {
    public static final int NGUONG_TAM_KHOA_MUON = -50;

    private String maDocGia;
    private int diemThuong;
    private int soLanViPham;

    private LocalDate ngayDangKy;
    private boolean trangThai;

    public DocGia() {
        this.diemThuong = 0;
        this.soLanViPham = 0;
        this.ngayDangKy = LocalDate.now();
        this.trangThai = true;
    }

    public DocGia(
            int id,
            String maDocGia,
            String hoTen,
            String email,
            String soDienThoai,
            String diaChi,
            int diemThuong,
            int soLanViPham,
            LocalDate ngayDangKy,
            boolean trangThai
    ) {
        super(id, hoTen, soDienThoai, email, diaChi);

        this.maDocGia = maDocGia;
        this.diemThuong = diemThuong;
        this.soLanViPham = soLanViPham;
        this.ngayDangKy = ngayDangKy;
        this.trangThai = trangThai;
    }

    public boolean duocPhepMuonSach() {
        return trangThai && diemThuong >= NGUONG_TAM_KHOA_MUON;
    }

    public boolean biTamKhoaDoDiemAm() {
        return diemThuong < NGUONG_TAM_KHOA_MUON;
    }

    public static boolean laCccdHopLe(String maThe) {
        return maThe != null && maThe.matches("[0-9]{12}");
    }

    public static String maTheRutGon(String maThe) {
        return laCccdHopLe(maThe) ? "••••••••" + maThe.substring(8) : maThe;
    }

    public String getMaDocGia() {
        return maDocGia;
    }

    public void setMaDocGia(String maDocGia) {
        this.maDocGia = maDocGia;
    }

    public int getDiemThuong() {
        return diemThuong;
    }

    public void setDiemThuong(int diemThuong) {
        this.diemThuong = diemThuong;
    }

    public int getDiemTichLuy() {
        return diemThuong;
    }

    public void setDiemTichLuy(int diemTichLuy) {
        this.diemThuong = diemTichLuy;
    }

    public int getSoLanViPham() {
        return soLanViPham;
    }

    public void setSoLanViPham(int soLanViPham) {
        this.soLanViPham = soLanViPham;
    }

    public LocalDate getNgayDangKy() {
        return ngayDangKy;
    }

    public void setNgayDangKy(LocalDate ngayDangKy) {
        this.ngayDangKy = ngayDangKy;
    }

    public boolean isTrangThai() {
        return trangThai;
    }

    public void setTrangThai(boolean trangThai) {
        this.trangThai = trangThai;
    }

    public String getHangThe() {
        if (diemThuong < NGUONG_TAM_KHOA_MUON) {
            return "Tạm khóa mượn";
        }
        if (diemThuong < 0) {
            return "Cảnh báo";
        }
        if (diemThuong < 100) {
            return "Thành viên";
        }
        if (diemThuong < 180) {
            return "Bạc";
        }
        if (diemThuong < 300) {
            return "Vàng";
        }
        return "Kim cương";
    }

    @Override
    public String toString() {
        return maTheRutGon(maDocGia) + " - " + hoTen;
    }
    @Override
    public String getLoai() {
        return "Độc giả";
    }

    @Override
    public String getThongTinChiTiet() {
        return "Mã thẻ: " + maTheRutGon(maDocGia)
                + "\nHọ tên: " + getHoTen()
                + "\nEmail: " + getEmail()
                + "\nSố lần vi phạm: " + soLanViPham
                + "\nTrạng thái: "
                + (trangThai ? "Hoạt động" : "Bị khóa");
    }
}