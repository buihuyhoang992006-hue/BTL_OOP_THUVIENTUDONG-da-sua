package service;

import dao.DiemThuongDAO;
import dao.DocGiaDAO;
import dao.KetNoiDatabase;
import dao.VoucherDAO;
import exception.DuLieuKhongHopLeException;
import model.DocGia;
import model.Voucher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class RewardService {

    public static final int DIEM_CONG_MOI_CUON_MUON = 10;
    public static final int DIEM_CONG_HOAN_TAT_PHIEU_DUNG_HAN = 5;

    public enum GoiDoiVoucher {
        GOI_50(50, "Giảm 5.000đ phí thuê sách", "TIEN_MAT", 5000.0, 5000.0, "VC5K"),
        GOI_100(100, "Giảm 15.000đ phí thuê sách", "TIEN_MAT", 15000.0, 15000.0, "VC15K"),
        GOI_180(180, "Giảm 50% hóa đơn (tối đa 30.000đ)", "PHAN_TRAM", 50.0, 30000.0, "VC50P"),
        GOI_300(300, "Miễn phí 100% hóa đơn (tối đa 50.000đ)", "PHAN_TRAM", 100.0, 50000.0, "VCFREE");

        private final int diemCanDoi;
        private final String tenUuDai;
        private final String loaiGiam;
        private final double giaTriGiam;
        private final Double giamToiDa;
        private final String tienToMa;

        GoiDoiVoucher(int diemCanDoi, String tenUuDai, String loaiGiam,
                      double giaTriGiam, Double giamToiDa, String tienToMa) {
            this.diemCanDoi = diemCanDoi;
            this.tenUuDai = tenUuDai;
            this.loaiGiam = loaiGiam;
            this.giaTriGiam = giaTriGiam;
            this.giamToiDa = giamToiDa;
            this.tienToMa = tienToMa;
        }

        public int diemCanDoi() {
            return diemCanDoi;
        }

        public String tenUuDai() {
            return tenUuDai;
        }

        public String loaiGiam() {
            return loaiGiam;
        }

        public double giaTriGiam() {
            return giaTriGiam;
        }

        public Double giamToiDa() {
            return giamToiDa;
        }

        public String tienToMa() {
            return tienToMa;
        }

        public String moTa() {
            return diemCanDoi + " điểm  →  " + tenUuDai;
        }

        @Override
        public String toString() {
            return moTa();
        }
    }

    private final DocGiaDAO docGiaDAO;
    private final DiemThuongDAO diemThuongDAO;
    private final VoucherDAO voucherDAO;

    public RewardService() {
        this.docGiaDAO = new DocGiaDAO();
        this.diemThuongDAO = new DiemThuongDAO();
        this.voucherDAO = new VoucherDAO();
    }

    public static int tinhDiemDuKienKhiMuon(int soLuong) {
        return Math.max(0, soLuong) * DIEM_CONG_MOI_CUON_MUON;
    }

    /**
     * Đổi điểm tích lũy của độc giả sang mã Voucher trong bảng voucher_doc_gia.
     */
    public Voucher doiDiemLayVoucher(int idDocGia, GoiDoiVoucher goi) throws DuLieuKhongHopLeException {
        if (idDocGia <= 0 || goi == null) {
            throw new DuLieuKhongHopLeException("Đổi thưởng Voucher", "Thiếu thông tin độc giả hoặc gói đổi thưởng.");
        }

        try (Connection giaoDich = KetNoiDatabase.taoKetNoiChoGiaoDich()) {
            giaoDich.setAutoCommit(false);

            try {
                int diemHienTai;
                String trangThaiThe;
                String hoTen;

                String sqlLock = """
                        SELECT ho_ten, diem_tich_luy, trang_thai
                        FROM doc_gia
                        WHERE id = ?
                        FOR UPDATE
                        """;
                try (PreparedStatement ps = giaoDich.prepareStatement(sqlLock)) {
                    ps.setInt(1, idDocGia);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new DuLieuKhongHopLeException("Độc giả", "Không tìm thấy hồ sơ độc giả.");
                        }
                        hoTen = rs.getString("ho_ten");
                        diemHienTai = rs.getInt("diem_tich_luy");
                        trangThaiThe = rs.getString("trang_thai");
                    }
                }

                if (!"HOAT_DONG".equals(trangThaiThe)) {
                    throw new DuLieuKhongHopLeException(
                            "Thẻ độc giả",
                            "Thẻ của độc giả " + hoTen + " đang bị khóa, không thể đổi thưởng.");
                }

                if (diemHienTai < goi.diemCanDoi()) {
                    throw new DuLieuKhongHopLeException(
                            "Điểm tích lũy",
                            "Độc giả hiện có " + diemHienTai + " điểm, cần tối thiểu "
                                    + goi.diemCanDoi() + " điểm để đổi gói \"" + goi.tenUuDai() + "\".");
                }

                docGiaDAO.congTruDiem(giaoDich, idDocGia, -goi.diemCanDoi());

                Voucher voucher = new Voucher();
                voucher.setIdDocGia(idDocGia);
                voucher.setMaVoucher(taoMaVoucher(idDocGia, goi.tienToMa()));
                voucher.setTenUuDai(goi.tenUuDai());
                voucher.setLoaiGiam(goi.loaiGiam());
                voucher.setGiaTriGiam(goi.giaTriGiam());
                voucher.setGiamToiDa(goi.giamToiDa());
                voucher.setTrangThai("CHUA_DUNG");
                voucher.setNgayDoi(LocalDateTime.now());
                voucher.setHanSuDung(null);
                voucherDAO.themVoucher(giaoDich, voucher);

                diemThuongDAO.themLichSu(
                        giaoDich,
                        idDocGia,
                        goi.diemCanDoi(),
                        "TRU",
                        "Đổi " + goi.diemCanDoi() + " điểm lấy mã " + voucher.getMaVoucher()
                                + " (" + goi.tenUuDai() + ")");

                giaoDich.commit();
                return voucher;
            } catch (DuLieuKhongHopLeException ex) {
                try {
                    giaoDich.rollback();
                } catch (SQLException suppressed) {
                    ex.addSuppressed(suppressed);
                }
                throw ex;
            } catch (SQLException | RuntimeException ex) {
                try {
                    giaoDich.rollback();
                } catch (SQLException suppressed) {
                    ex.addSuppressed(suppressed);
                }
                throw new RuntimeException("Lỗi đổi thưởng Voucher: " + ex.getMessage(), ex);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể kết nối CSDL khi đổi Voucher: " + e.getMessage(), e);
        }
    }

    /**
     * Hàm cộng điểm chung.
     */
    private boolean congDiem(int idDocGia,
                             int soDiem,
                             String maLyDo,
                             String noiDung) {
        if (soDiem <= 0) {
            return false;
        }

        DocGia docGia = docGiaDAO.getById(idDocGia);
        if (docGia == null) {
            return false;
        }

        int diemMoi = docGia.getDiemTichLuy() + soDiem;
        if (!docGiaDAO.capNhatDiemTichLuy(idDocGia, diemMoi)) {
            return false;
        }

        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich()) {
            conn.setAutoCommit(false);
            diemThuongDAO.themLichSu(conn, idDocGia, soDiem, "CONG", "[" + maLyDo + "] " + noiDung);
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean thuongMuonSach(int idDocGia) {
        return congDiem(idDocGia, DIEM_CONG_MOI_CUON_MUON, "MUON_SACH", "Mượn sách thành công (+10 điểm/cuốn)");
    }

    public boolean thuongNhanSachDatTruoc(int idDocGia) {
        return congDiem(idDocGia, 2, "NHAN_DAT_TRUOC", "Nhận sách đã đặt trước");
    }

    public boolean thuongTraSach(int idDocGia,
                                 LocalDate hanTra,
                                 LocalDate ngayTra) {
        if (hanTra == null || ngayTra == null) {
            return false;
        }
        if (!ngayTra.isAfter(hanTra)) {
            return congDiem(idDocGia, DIEM_CONG_HOAN_TAT_PHIEU_DUNG_HAN,
                    "TRA_DUNG_HAN", "Trả sách đúng hạn/sớm (+5 điểm/phiếu)");
        }
        return true;
    }

    private String taoMaVoucher(int idDocGia, String tienToMa) {
        String rand = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return tienToMa + "-" + idDocGia + "-" + rand;
    }
}
