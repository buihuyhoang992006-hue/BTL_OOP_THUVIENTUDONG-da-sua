package service;

import dao.DocGiaDAO;
import dao.PhieuMuonDAO;
import exception.DuLieuKhongHopLeException;
import model.DocGia;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class MuonTraService {
    private static final ZoneId MUI_GIO = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PhieuMuonDAO phieuMuonDAO = new PhieuMuonDAO();

    // ================= MƯỢN SÁCH =================

    public boolean muonSach(
            int idDocGia,
            int idThuThu,
            int idSach,
            int soLuong,
            LocalDate hanTra
    ) throws DuLieuKhongHopLeException {

        if (idDocGia <= 0 || idThuThu <= 0 || idSach <= 0) {
            throw new DuLieuKhongHopLeException(
                    "Phiếu mượn",
                    "Thiếu độc giả, thủ thư hoặc sách."
            );
        }

        if (soLuong <= 0) {
            throw new DuLieuKhongHopLeException(
                    "Số lượng",
                    "Phải lớn hơn 0."
            );
        }

        if (hanTra == null || hanTra.isBefore(LocalDate.now(MUI_GIO))) {
            throw new DuLieuKhongHopLeException(
                    "Hạn trả",
                    "Không được trước ngày hôm nay."
            );
        }

        kiemTraQuyenMuonTheoDiem(idDocGia);

        return phieuMuonDAO.muonSach(
                idDocGia,
                idThuThu,
                idSach,
                soLuong,
                hanTra
        );
    }

    public PhieuMuonDAO.KetQuaLapPhieuMuon muonSachCoThanhToan(
            int idDocGia,
            int idThuThu,
            int idSach,
            int soLuong,
            LocalDate hanTra,
            Integer idVoucher
    ) throws DuLieuKhongHopLeException {

        if (idDocGia <= 0 || idThuThu <= 0 || idSach <= 0) {
            throw new DuLieuKhongHopLeException(
                    "Phiếu mượn",
                    "Thiếu độc giả, thủ thư hoặc sách."
            );
        }

        if (soLuong <= 0) {
            throw new DuLieuKhongHopLeException(
                    "Số lượng",
                    "Phải lớn hơn 0."
            );
        }

        if (hanTra == null || hanTra.isBefore(LocalDate.now(MUI_GIO))) {
            throw new DuLieuKhongHopLeException(
                    "Hạn trả",
                    "Không được trước ngày hôm nay."
            );
        }

        kiemTraQuyenMuonTheoDiem(idDocGia);

        return phieuMuonDAO.muonSachCoThanhToan(
                idDocGia,
                idThuThu,
                idSach,
                soLuong,
                hanTra,
                idVoucher
        );
    }

    private void kiemTraQuyenMuonTheoDiem(int idDocGia) throws DuLieuKhongHopLeException {
        DocGia dg = new DocGiaDAO().getById(idDocGia);
        if (dg == null) {
            throw new DuLieuKhongHopLeException("Độc giả", "Không tìm thấy hồ sơ độc giả.");
        }
        if (!dg.isTrangThai()) {
            throw new DuLieuKhongHopLeException("Thẻ độc giả", "Thẻ độc giả hiện đang bị khóa.");
        }
        if (dg.biTamKhoaDoDiemAm()) {
            throw new DuLieuKhongHopLeException(
                    "Tạm khóa quyền mượn",
                    "Độc giả hiện có " + dg.getDiemThuong() + " điểm tích lũy (< "
                            + DocGia.NGUONG_TAM_KHOA_MUON + " điểm). "
                            + "Tạm khóa quyền mượn mới cho đến khi khắc phục vi phạm.");
        }
    }

    // ================= TRẢ SÁCH / CHỐT MẤT =================

    public boolean traSach(
            int idChiTiet,
            int soLuongDaXacNhan,
            String tinhTrang,
            String ghiChu
    ) throws DuLieuKhongHopLeException {

        if (idChiTiet <= 0 || soLuongDaXacNhan <= 0) {
            throw new DuLieuKhongHopLeException(
                    "Dòng mượn",
                    "ID và số lượng phải hợp lệ."
            );
        }

        if (tinhTrang == null
                || !List.of("BINH_THUONG", "LAM_HONG", "LAM_MAT")
                .contains(tinhTrang)) {
            throw new DuLieuKhongHopLeException(
                    "Tình trạng",
                    "Hãy chọn bình thường, hỏng hoặc mất."
            );
        }

        String ghiChuSach = ghiChu == null ? "" : ghiChu.trim();

        if (ghiChuSach.length() > 2000) {
            throw new DuLieuKhongHopLeException(
                    "Ghi chú",
                    "Tối đa 2.000 ký tự."
            );
        }

        if (!"BINH_THUONG".equals(tinhTrang)
                && ghiChuSach.isEmpty()) {
            throw new DuLieuKhongHopLeException(
                    "Ghi chú",
                    "Bắt buộc nhập khi sách bị hỏng hoặc mất."
            );
        }

        return phieuMuonDAO.traSach(
                idChiTiet,
                soLuongDaXacNhan,
                tinhTrang,
                ghiChuSach
        );
    }
}