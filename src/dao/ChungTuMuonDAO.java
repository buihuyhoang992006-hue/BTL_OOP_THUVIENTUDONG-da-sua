package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ChungTuMuonDAO {
    public record ChungTuMuon(
            String maPhieu,
            LocalDateTime ngayLap,
            String maThe,
            String tenDocGia,
            String tenThuThu,
            String tenSach,
            int soLuong,
            LocalDate hanTra,
            long donGiaThue,
            long tongTienThue,
            String maVoucher,
            long giamGia,
            long phiSauGiam,
            int diemCong) {}

    public ChungTuMuon layTheoDongMuon(int idChiTiet) throws SQLException {
        String sql = """
                SELECT pm.ma_phieu, pm.ngay_muon, dg.ma_doc_gia,
                       dg.ho_ten AS ten_doc_gia, nd.ho_ten AS ten_thu_thu,
                       s.ten_sach, ct.so_luong, ct.han_tra, ct.don_gia_thue,
                       pm.tong_tien_thue, pm.ma_voucher_ap_dung, pm.giam_gia_voucher,
                       pm.thanh_tien, pm.diem_cong
                FROM chi_tiet_phieu_muon ct
                JOIN phieu_muon pm ON pm.id = ct.id_phieu_muon
                JOIN doc_gia dg ON dg.id = pm.id_doc_gia
                LEFT JOIN nguoi_dung nd ON nd.id = pm.id_thu_thu
                JOIN sach s ON s.id = ct.id_sach
                WHERE ct.id = ?
                """;
        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idChiTiet);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new ChungTuMuon(
                        rs.getString("ma_phieu"), rs.getObject("ngay_muon", LocalDateTime.class),
                        rs.getString("ma_doc_gia"), rs.getString("ten_doc_gia"),
                        rs.getString("ten_thu_thu"), rs.getString("ten_sach"),
                        rs.getInt("so_luong"), rs.getObject("han_tra", LocalDate.class),
                        Math.round(rs.getDouble("don_gia_thue")), Math.round(rs.getDouble("tong_tien_thue")),
                        rs.getString("ma_voucher_ap_dung"), Math.round(rs.getDouble("giam_gia_voucher")),
                        Math.round(rs.getDouble("thanh_tien")), rs.getInt("diem_cong"));
            }
        }
    }
}
