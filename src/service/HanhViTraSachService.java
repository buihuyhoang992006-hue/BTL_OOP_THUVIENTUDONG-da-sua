package service;

import dao.LichSuTraSachDAO;
import model.ChiTietPhieuMuon;
import model.NhomHanhVi;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class HanhViTraSachService {

    private static final ZoneId MUI_GIO =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private final LichSuTraSachDAO lichSuDAO =
            new LichSuTraSachDAO();

    public NhomHanhVi layNhomHanhVi(
            int idDocGia
    ) throws SQLException {

        List<ChiTietPhieuMuon> lichSu =
                lichSuDAO.lay5LuotTraGanNhat(idDocGia);

        return phanNhom(lichSu);
    }

    /*
     * Hàm thuần Java để kiểm tra quy tắc phân nhóm.
     * Đầu vào là tối đa 5 dòng trả hợp lệ đã được chọn.
     */
    public static NhomHanhVi phanNhom(
            List<ChiTietPhieuMuon> lichSu
    ) {
        if (lichSu == null) {
            throw new IllegalArgumentException(
                    "Danh sách lịch sử không được null."
            );
        }

        if (lichSu.size() > 5) {
            throw new IllegalArgumentException(
                    "Chỉ phân tích tối đa 5 lượt trả gần nhất."
            );
        }

        int soLuot = lichSu.size();
        int soLuotMuon = 0;

        LocalDate ngayXet = LocalDate.now(MUI_GIO);

        for (ChiTietPhieuMuon dong : lichSu) {
            if (dong == null
                    || dong.getHanTra() == null
                    || dong.getNgayTra() == null
                    || dong.getSoLuong() <= 0
                    || "LAM_MAT".equals(
                    dong.getTinhTrangTra()
            )) {
                throw new IllegalArgumentException(
                        "Lịch sử phải gồm các dòng đã trả hợp lệ, "
                                + "không gồm dòng chốt mất sách."
                );
            }

            /*
             * Vì đã có ngayTra, hàm trong model tính số ngày muộn
             * theo ngày trả thực tế, không theo ngày hiện tại.
             */
            if (dong.tinhSoNgayQuaHan(ngayXet) > 0) {
                soLuotMuon++;
            }
        }

        if (soLuot == 0) {
            return NhomHanhVi.CHUA_CO_LICH_SU;
        }

        if (soLuot < 3) {
            return NhomHanhVi.CHUA_RO_XU_HUONG;
        }

        int soLuotDungHan = soLuot - soLuotMuon;

        if (soLuotMuon * 100 >= soLuot * 60) {
            return NhomHanhVi.THUONG_TRA_MUON;
        }

        if (soLuotDungHan * 100 >= soLuot * 60) {
            return NhomHanhVi.THUONG_DUNG_HAN;
        }

        return NhomHanhVi.CHUA_RO_XU_HUONG;
    }
}