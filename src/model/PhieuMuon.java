package model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Lớp PhieuMuon - lưu thông tin một phiếu mượn sách
 */
public class PhieuMuon {

    private int id;
    private String maPhieu;

    private int idDocGia;
    private String tenDocGia;

    private int idNhanVien;
    private String tenNhanVien;

    private Date ngayMuon;
    private Date hanTra;
    private Date ngayTra;

    private String trangThai;
    private String ghiChu;

    private Integer idVoucher;
    private String maVoucherApDung;
    private long tongTienThue;
    private long giamGiaVoucher;
    private long thucThu;
    private int diemCong;

    // Một phiếu có thể chứa nhiều sách
    private List<ChiTietPhieuMuon> danhSachChiTiet;


    // Constructor rỗng
    public PhieuMuon() {
        this.danhSachChiTiet = new ArrayList<>();
        this.ngayMuon = new Date();
        this.trangThai = "DANG_MUON";
    }


    // Constructor dùng khi tạo phiếu mượn mới
    public PhieuMuon(String maPhieu,
                     int idDocGia,
                     int idNhanVien,
                     Date ngayMuon,
                     Date hanTra) {

        this();

        this.maPhieu = maPhieu;
        this.idDocGia = idDocGia;
        this.idNhanVien = idNhanVien;
        this.ngayMuon = ngayMuon;
        this.hanTra = hanTra;
    }


    // Thêm một cuốn sách vào phiếu
    public void themChiTiet(ChiTietPhieuMuon chiTiet) {
        danhSachChiTiet.add(chiTiet);
    }


    // ================= GETTER / SETTER =================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getMaPhieu() {
        return maPhieu;
    }

    public void setMaPhieu(String maPhieu) {
        this.maPhieu = maPhieu;
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


    public int getIdNhanVien() {
        return idNhanVien;
    }

    public void setIdNhanVien(int idNhanVien) {
        this.idNhanVien = idNhanVien;
    }


    public String getTenNhanVien() {
        return tenNhanVien;
    }

    public void setTenNhanVien(String tenNhanVien) {
        this.tenNhanVien = tenNhanVien;
    }


    public Date getNgayMuon() {
        return ngayMuon;
    }

    public void setNgayMuon(Date ngayMuon) {
        this.ngayMuon = ngayMuon;
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


    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public Integer getIdVoucher() {
        return idVoucher;
    }

    public void setIdVoucher(Integer idVoucher) {
        this.idVoucher = idVoucher;
    }

    public String getMaVoucherApDung() {
        return maVoucherApDung;
    }

    public void setMaVoucherApDung(String maVoucherApDung) {
        this.maVoucherApDung = maVoucherApDung;
    }

    public long getTongTienThue() {
        return tongTienThue;
    }

    public void setTongTienThue(long tongTienThue) {
        this.tongTienThue = Math.max(0L, tongTienThue);
    }

    public long getGiamGiaVoucher() {
        return giamGiaVoucher;
    }

    public void setGiamGiaVoucher(long giamGiaVoucher) {
        this.giamGiaVoucher = Math.max(0L, giamGiaVoucher);
    }

    public long getThucThu() {
        return thucThu;
    }

    public void setThucThu(long thucThu) {
        this.thucThu = Math.max(0L, thucThu);
    }

    public long getThanhTien() {
        return thucThu;
    }

    public void setThanhTien(long thanhTien) {
        this.thucThu = Math.max(0L, thanhTien);
    }

    public int getDiemCong() {
        return diemCong;
    }

    public void setDiemCong(int diemCong) {
        this.diemCong = diemCong;
    }

    public List<ChiTietPhieuMuon> getDanhSachChiTiet() {
        return danhSachChiTiet;
    }

    public void setDanhSachChiTiet(List<ChiTietPhieuMuon> danhSachChiTiet) {
        this.danhSachChiTiet = danhSachChiTiet;
    }
    public static void main(String[] args) {

        // Tạo 1 phiếu mượn để test
        PhieuMuon pm = new PhieuMuon();

        pm.setMaPhieu("PM001");
        pm.setIdDocGia(4);
        pm.setTenDocGia("Nguyễn Văn An");

        pm.setIdNhanVien(2);
        pm.setTenNhanVien("Nguyễn Thị Hương");

        pm.setTrangThai("DANG_MUON");

        // Tạo sách thứ 1
        ChiTietPhieuMuon ct1 = new ChiTietPhieuMuon(
                3,
                "SACH0003",
                "Lập Trình Java Căn Bản",
                1
        );

        // Tạo sách thứ 2
        ChiTietPhieuMuon ct2 = new ChiTietPhieuMuon(
                1,
                "SACH0001",
                "Đắc Nhân Tâm",
                2
        );

        // Thêm sách vào phiếu
        pm.themChiTiet(ct1);
        pm.themChiTiet(ct2);

        // In ra console
        System.out.println("===== PHIEU MUON =====");
        System.out.println("Ma phieu: " + pm.getMaPhieu());
        System.out.println("Doc gia: " + pm.getTenDocGia());
        System.out.println("Nhan vien: " + pm.getTenNhanVien());
        System.out.println("Trang thai: " + pm.getTrangThai());

        System.out.println("\n===== DANH SACH SACH =====");

        for (ChiTietPhieuMuon ct : pm.getDanhSachChiTiet()) {
            System.out.println(
                    ct.getMaSach()
                            + " | "
                            + ct.getTenSach()
                            + " | So luong: "
                            + ct.getSoLuong()
            );
        }

        System.out.println(
                "\nSo loai sach muon: "
                        + pm.getDanhSachChiTiet().size()
        );
    }
}