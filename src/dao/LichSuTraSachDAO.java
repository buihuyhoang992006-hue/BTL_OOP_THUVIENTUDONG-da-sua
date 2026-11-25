package dao;

import model.ChiTietPhieuMuon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LichSuTraSachDAO {

    public List<ChiTietPhieuMuon> lay5LuotTraGanNhat(
            int idDocGia
    ) throws SQLException {

        if (idDocGia <= 0) {
            throw new IllegalArgumentException(
                    "ID độc giả không hợp lệ."
            );
        }

        String sql = """
                SELECT
                    ct.id,
                    ct.so_luong,
                    ct.han_tra,
                    ct.ngay_tra,
                    ct.tinh_trang_tra
                FROM chi_tiet_phieu_muon ct
                JOIN phieu_muon pm
                    ON pm.id = ct.id_phieu_muon
                WHERE pm.id_doc_gia = ?
                  AND ct.ngay_tra IS NOT NULL
                  AND ct.han_tra IS NOT NULL
                  AND ct.so_luong > 0
                  AND (
                      ct.tinh_trang_tra IS NULL
                      OR ct.tinh_trang_tra <> 'LAM_MAT'
                  )
                ORDER BY ct.ngay_tra DESC, ct.id DESC
                LIMIT 5
                """;

        List<ChiTietPhieuMuon> ketQua = new ArrayList<>();

        try (
                Connection connection =
                        KetNoiDatabase.taoKetNoiChoGiaoDich();

                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {
            ps.setInt(1, idDocGia);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ChiTietPhieuMuon dong =
                            new ChiTietPhieuMuon();

                    dong.setId(rs.getInt("id"));
                    dong.setSoLuong(rs.getInt("so_luong"));
                    dong.setHanTra(rs.getDate("han_tra"));
                    dong.setNgayTra(rs.getDate("ngay_tra"));
                    dong.setTinhTrangTra(
                            rs.getString("tinh_trang_tra")
                    );

                    ketQua.add(dong);
                }
            }
        }

        return ketQua;
    }
}