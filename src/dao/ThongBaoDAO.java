package dao;

import model.ThongBaoChiTiet;
import model.DocGia;
import service.EmailService;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ThongBaoDAO {

    public static final String MOC_NHAC = "TRUOC_HAN_1_NGAY";
    private static final String MOC_DAT_TRUOC = "DEN_LUOT_DAT_TRUOC";

    /*
     * Chỉ các bản ghi thuộc phạm vi này được luồng nhắc hạn xử lý.
     * Thông báo cũ có moc_nhac NULL được giữ nguyên.
     */
    private static final String PHAM_VI = """
            tb.kenh_gui = 'EMAIL'
            AND tb.loai_thong_bao = 'SAP_DEN_HAN'
            AND tb.moc_nhac = ?
            """;

    private static final String PHAM_VI_DAT_TRUOC = """
            tb.kenh_gui = 'EMAIL'
            AND tb.loai_thong_bao = 'DEN_LUOT_DAT_TRUOC'
            AND tb.moc_nhac = 'DEN_LUOT_DAT_TRUOC'
            """;

    private static final String PHAM_VI_THE_GUI = """
            LEFT(CAST(dg.ma_doc_gia AS BINARY), LENGTH(?)) = CAST(? AS BINARY)
            """;

    private static final String DAT_TRUOC_CON_HAN = """
            dt.trang_thai = 'DA_THONG_BAO'
            AND dt.han_nhan_sach > NOW()
            AND dg.trang_thai = 'HOAT_DONG'
            AND dg.diem_tich_luy >= %d
            AND sd.trang_thai = 1
            """.formatted(DocGia.NGUONG_TAM_KHOA_MUON);

    private static final String EMAIL_DAT_TRUOC_CHO_GUI = """
            (tb.trang_thai = 'CHUA_GUI'
             OR (tb.trang_thai = 'LOI'
                 AND tb.so_lan_thu < 3
                 AND tb.lan_thu_cuoi < DATE_SUB(NOW(), INTERVAL 15 MINUTE)))
            """;

    private static final String SQL_DOC_THONG_BAO = """
            SELECT tb.*,
                   dg.ma_doc_gia,
                   dg.ho_ten AS ten_doc_gia,
                   dg.email AS email_doc_gia,
                   p.ma_phieu,
                   COALESCE(s.ten_sach, sd.ten_sach) AS ten_sach,
                   ct.so_luong,
                   ct.han_tra
            FROM thong_bao tb
            LEFT JOIN doc_gia dg ON dg.id = tb.id_doc_gia
            LEFT JOIN chi_tiet_phieu_muon ct
                   ON ct.id = tb.id_chi_tiet
            LEFT JOIN phieu_muon p ON p.id = ct.id_phieu_muon
            LEFT JOIN sach s ON s.id = ct.id_sach
            LEFT JOIN dat_truoc dt ON dt.id = tb.id_dat_truoc AND dt.id_doc_gia = tb.id_doc_gia
            LEFT JOIN sach sd ON sd.id = dt.id_sach
            """;

    // ================= LỜI GỌI CŨ =================

    /*
     * Giữ chữ ký cho phần Đặt trước cũ vẫn biên dịch.
     * EMAIL chỉ được tạo ở trạng thái CHUA_GUI.
     * Hàm này không có bằng chứng SMTP đã chấp nhận.
     */
    public boolean taoThongBao(
            Connection conn,
            int idDocGia,
            String loaiThongBao,
            String noiDung,
            String kenhGui,
            String trangThai
    ) throws SQLException {

        if (conn == null
                || idDocGia <= 0
                || kenhGui == null
                || trangThai == null
                || !List.of("EMAIL", "SYSTEM").contains(kenhGui)
                || !List.of("CHUA_GUI", "DA_GUI").contains(trangThai)) {
            throw new SQLException("Dữ liệu thông báo không hợp lệ.");
        }

        String trangThaiLuu = "EMAIL".equals(kenhGui)
                ? "CHUA_GUI"
                : trangThai;

        String sql = """
                INSERT INTO thong_bao
                    (ma_thong_bao, id_doc_gia, loai_thong_bao,
                     noi_dung, kenh_gui, ngay_gui, trang_thai)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sinhMaThongBao());
            ps.setInt(2, idDocGia);
            ps.setString(3, loaiThongBao);
            ps.setString(4, noiDung);
            ps.setString(5, kenhGui);

            ps.setTimestamp(
                    6,
                    "DA_GUI".equals(trangThaiLuu)
                            ? new Timestamp(System.currentTimeMillis())
                            : null
            );

            ps.setString(7, trangThaiLuu);

            return ps.executeUpdate() == 1;
        }
    }

    public List<ThongBaoChiTiet> getThongBaoTheoDocGia(int idDocGia) {
        try {
            return docDanhSach(
                    SQL_DOC_THONG_BAO
                            + """
                              WHERE tb.id_doc_gia = ?
                              ORDER BY tb.ngay_tao DESC, tb.id DESC
                              """,
                    idDocGia
            );
        } catch (SQLException e) {
            System.err.println(
                    "Lỗi đọc thông báo độc giả: " + e.getMessage()
            );
            return new ArrayList<>();
        }
    }

    // ================= EMAIL ĐẶT TRƯỚC =================

    public void taoEmailDatTruoc(Connection conn, int idDatTruoc, int idDocGia,
            String maDocGia, String emailDocGia, String tieuDe, String noiDung) throws SQLException {
        if (emailDocGia == null || emailDocGia.isBlank()) {
            return;
        }
        String tienTo;
        try {
            tienTo = EmailService.tienToMaTheGui();
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Độc giả vẫn có thông báo trong hệ thống nếu chưa cấu hình gửi email.
            return;
        }
        if (maDocGia == null || !maDocGia.startsWith(tienTo)) {
            return;
        }
        if (tieuDe != null) {
            tieuDe = tieuDe.replace('\r', ' ').replace('\n', ' ').strip();
            if (tieuDe.codePointCount(0, tieuDe.length()) > 255) {
                tieuDe = tieuDe.substring(0, tieuDe.offsetByCodePoints(0, 255));
            }
        }
        kiemTraNoiDung(tieuDe, noiDung);
        String sql = """
                INSERT INTO thong_bao
                    (ma_thong_bao, id_doc_gia, loai_thong_bao, noi_dung, kenh_gui,
                     trang_thai, id_dat_truoc, moc_nhac, tieu_de)
                VALUES (?, ?, 'DEN_LUOT_DAT_TRUOC', ?, 'EMAIL', 'CHUA_GUI', ?, ?, ?)
                ON DUPLICATE KEY UPDATE id = thong_bao.id
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ganThamSo(ps, sinhMaThongBao(), idDocGia, noiDung, idDatTruoc, MOC_DAT_TRUOC, tieuDe);
            ps.executeUpdate();
        }
    }

    public void huyEmailDatTruoc(Connection conn, int idDatTruoc, String lyDo) throws SQLException {
        String sql = """
                UPDATE thong_bao tb
                SET trang_thai = 'HUY', loi_gui = ?
                WHERE tb.id_dat_truoc = ? AND tb.trang_thai IN ('CHUA_GUI', 'LOI') AND
                """ + PHAM_VI_DAT_TRUOC;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ganThamSo(ps, lyDo, idDatTruoc);
            ps.executeUpdate();
        }
    }

    public List<ThongBaoChiTiet> layEmailDatTruocChoGui(String tienTo) throws SQLException {
        return docDanhSach(SQL_DOC_THONG_BAO + " WHERE " + PHAM_VI_DAT_TRUOC
                + " AND " + DAT_TRUOC_CON_HAN + " AND " + PHAM_VI_THE_GUI
                + " AND " + EMAIL_DAT_TRUOC_CHO_GUI + " ORDER BY tb.id LIMIT 100", tienTo, tienTo);
    }

    public boolean nhanEmailDatTruoc(int id) throws SQLException {
        String sql = """
                UPDATE thong_bao tb
                SET tb.trang_thai = 'DANG_GUI',
                    tb.so_lan_thu = tb.so_lan_thu + 1,
                    tb.lan_thu_cuoi = NOW(),
                    tb.email_nhan = NULL,
                    tb.loi_gui = NULL
                WHERE tb.id = ? AND
                """ + PHAM_VI_DAT_TRUOC + " AND " + EMAIL_DAT_TRUOC_CHO_GUI;
        return capNhat(sql, id) == 1;
    }

    public ThongBaoChiTiet docEmailDatTruocDangXuLy(int id, String tienTo) throws SQLException {
        List<ThongBaoChiTiet> danhSach = docDanhSach(SQL_DOC_THONG_BAO
                + " WHERE tb.id = ? AND tb.trang_thai = 'DANG_GUI' AND " + PHAM_VI_DAT_TRUOC
                + " AND " + DAT_TRUOC_CON_HAN + " AND " + PHAM_VI_THE_GUI, id, tienTo, tienTo);
        return danhSach.isEmpty() ? null : danhSach.get(0);
    }

    public boolean luuDiaChiEmailDatTruoc(int id, String emailNhan) throws SQLException {
        return capNhat("""
                UPDATE thong_bao tb
                SET tb.email_nhan = ?
                WHERE tb.id = ? AND tb.trang_thai = 'DANG_GUI' AND
                """ + PHAM_VI_DAT_TRUOC, emailNhan, id) == 1;
    }

    public boolean ketThucGuiEmailDatTruoc(int id, String trangThai, String emailNhan, String loiGui)
            throws SQLException {
        return ketThucXuLy(id, trangThai, emailNhan, loiGui, "DEN_LUOT_DAT_TRUOC", MOC_DAT_TRUOC);
    }

    public void donHangDoiEmailDatTruoc(String tienTo) throws SQLException {
        List<ThongBaoChiTiet> canDon = docDanhSach(SQL_DOC_THONG_BAO + " WHERE "
                + PHAM_VI_DAT_TRUOC + " AND " + PHAM_VI_THE_GUI + """
                  AND ((tb.trang_thai = 'DANG_GUI'
                        AND (tb.lan_thu_cuoi IS NULL OR tb.lan_thu_cuoi < DATE_SUB(NOW(), INTERVAL 10 MINUTE)))
                    OR (tb.trang_thai IN ('CHUA_GUI', 'LOI')
                        AND (dt.id IS NULL OR dt.trang_thai <> 'DA_THONG_BAO'
                            OR dt.han_nhan_sach IS NULL OR dt.han_nhan_sach <= NOW()
                            OR dg.trang_thai <> 'HOAT_DONG' OR dg.diem_tich_luy < %d
                            OR sd.id IS NULL OR sd.trang_thai <> 1)))
                  ORDER BY tb.id LIMIT 100
                """.formatted(DocGia.NGUONG_TAM_KHOA_MUON), tienTo, tienTo);
        String sql = """
                UPDATE thong_bao tb
                SET tb.trang_thai = ?, tb.loi_gui = ?
                WHERE tb.id = ? AND tb.trang_thai = ? AND
                """ + PHAM_VI_DAT_TRUOC;
        for (ThongBaoChiTiet email : canDon) {
            boolean gianDoan = "DANG_GUI".equals(email.getTrangThai());
            capNhat(sql, gianDoan ? "CAN_KIEM_TRA" : "HUY",
                    gianDoan ? "Phiên gửi chưa ghi nhận kết quả; cần kiểm tra."
                            : "Lượt giữ sách không còn đủ điều kiện gửi email.",
                    email.getId(), email.getTrangThai());
        }
    }

    // ================= DÒNG MƯỢN CẦN NHẮC =================

    public List<ThongBaoChiTiet> layDongCanNhac(
            LocalDate ngayXet
    ) throws SQLException {
        return docDongCanNhac(null, ngayXet);
    }

    /*
     * idChiTiet là ID của chi_tiet_phieu_muon,
     * không phải ID thông báo.
     */
    public ThongBaoChiTiet layDongCanNhacTheoId(
            int idChiTiet,
            LocalDate ngayXet
    ) throws SQLException {
        if (idChiTiet <= 0) {
            return null;
        }

        List<ThongBaoChiTiet> danhSach =
                docDongCanNhac(idChiTiet, ngayXet);

        return danhSach.isEmpty() ? null : danhSach.get(0);
    }

    private List<ThongBaoChiTiet> docDongCanNhac(
            Integer idChiTiet,
            LocalDate ngayXet
    ) throws SQLException {
        String sql = """
                SELECT tb.*,
                       ct.id AS chi_tiet_hien_tai,
                       p.id_doc_gia AS doc_gia_hien_tai,
                       dg.ma_doc_gia,
                       dg.ho_ten AS ten_doc_gia,
                       dg.email AS email_doc_gia,
                       p.ma_phieu,
                       s.ten_sach,
                       ct.so_luong,
                       ct.han_tra
                FROM chi_tiet_phieu_muon ct
                JOIN phieu_muon p ON p.id = ct.id_phieu_muon
                JOIN doc_gia dg ON dg.id = p.id_doc_gia
                JOIN sach s ON s.id = ct.id_sach
                LEFT JOIN thong_bao tb
                       ON tb.id_chi_tiet = ct.id
                      AND tb.moc_nhac = ?
                      AND tb.kenh_gui = 'EMAIL'
                      AND tb.loai_thong_bao = 'SAP_DEN_HAN'
                WHERE ct.ngay_tra IS NULL
                  AND ct.trang_thai = 'DANG_MUON'
                  AND ct.so_luong > 0
                  AND ct.han_tra = ?
                """
                + (idChiTiet == null ? "" : " AND ct.id = ? ")
                + " ORDER BY ct.id";

        List<ThongBaoChiTiet> danhSach = new ArrayList<>();

        try (
                Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, MOC_NHAC);
            ps.setDate(2, ngayMai(ngayXet));

            if (idChiTiet != null) {
                ps.setInt(3, idChiTiet);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ThongBaoChiTiet tb = mapThongBao(rs);

                    // Lấy đúng độc giả và dòng mượn hiện tại.
                    tb.setIdDocGia(rs.getInt("doc_gia_hien_tai"));
                    tb.setIdChiTiet(rs.getInt("chi_tiet_hien_tai"));
                    tb.setMocNhac(MOC_NHAC);

                    /*
                     * id = 0: chưa có bản ghi hàng đợi.
                     * Truy vấn xem trước không tạo thông báo.
                     */
                    if (tb.getId() == 0) {
                        tb.setLoaiThongBao("SAP_DEN_HAN");
                        tb.setKenhGui("EMAIL");
                        tb.setTrangThai("CHUA_GUI");
                        tb.setNoiDung("");
                    }

                    danhSach.add(tb);
                }
            }
        }

        return danhSach;
    }

    public List<ThongBaoChiTiet> layDongTheoKhoangHanTra(
            LocalDate tuNgay,
            LocalDate denNgay
    ) throws SQLException {
        if (tuNgay == null || denNgay == null || denNgay.isBefore(tuNgay)) {
            return new ArrayList<>();
        }

        String sql = """
                SELECT tb.*,
                       ct.id AS chi_tiet_hien_tai,
                       p.id_doc_gia AS doc_gia_hien_tai,
                       dg.ma_doc_gia,
                       dg.ho_ten AS ten_doc_gia,
                       dg.email AS email_doc_gia,
                       p.ma_phieu,
                       s.ten_sach,
                       ct.so_luong,
                       ct.han_tra
                FROM chi_tiet_phieu_muon ct
                JOIN phieu_muon p ON p.id = ct.id_phieu_muon
                JOIN doc_gia dg ON dg.id = p.id_doc_gia
                JOIN sach s ON s.id = ct.id_sach
                LEFT JOIN thong_bao tb
                       ON tb.id_chi_tiet = ct.id
                      AND tb.moc_nhac = ?
                      AND tb.kenh_gui = 'EMAIL'
                      AND tb.loai_thong_bao = 'SAP_DEN_HAN'
                WHERE ct.ngay_tra IS NULL
                  AND ct.trang_thai = 'DANG_MUON'
                  AND ct.so_luong > 0
                  AND ct.han_tra BETWEEN ? AND ?
                ORDER BY ct.han_tra ASC, ct.id ASC
                """;

        return docDongMuonKemThongBao(
                sql,
                MOC_NHAC,
                java.sql.Date.valueOf(tuNgay),
                java.sql.Date.valueOf(denNgay)
        );
    }

    public List<ThongBaoChiTiet> layDongQuaHanChuaTra(
            LocalDate ngayXet
    ) throws SQLException {
        if (ngayXet == null) {
            return new ArrayList<>();
        }

        String sql = """
                SELECT tb.*,
                       ct.id AS chi_tiet_hien_tai,
                       p.id_doc_gia AS doc_gia_hien_tai,
                       dg.ma_doc_gia,
                       dg.ho_ten AS ten_doc_gia,
                       dg.email AS email_doc_gia,
                       p.ma_phieu,
                       s.ten_sach,
                       ct.so_luong,
                       ct.han_tra
                FROM chi_tiet_phieu_muon ct
                JOIN phieu_muon p ON p.id = ct.id_phieu_muon
                JOIN doc_gia dg ON dg.id = p.id_doc_gia
                JOIN sach s ON s.id = ct.id_sach
                LEFT JOIN thong_bao tb
                       ON tb.id_chi_tiet = ct.id
                      AND tb.moc_nhac = ?
                      AND tb.kenh_gui = 'EMAIL'
                      AND tb.loai_thong_bao = 'SAP_DEN_HAN'
                WHERE ct.ngay_tra IS NULL
                  AND ct.trang_thai = 'DANG_MUON'
                  AND ct.so_luong > 0
                  AND ct.han_tra < ?
                ORDER BY ct.han_tra ASC, ct.id ASC
                """;

        return docDongMuonKemThongBao(
                sql,
                MOC_NHAC,
                java.sql.Date.valueOf(ngayXet)
        );
    }

    public List<ThongBaoChiTiet> layThongBaoMoiNhat(int gioiHan) {
        int soDong = Math.max(1, Math.min(gioiHan, 200));
        try {
            return docDanhSach(
                    SQL_DOC_THONG_BAO
                            + " ORDER BY tb.ngay_tao DESC, tb.id DESC LIMIT ?",
                    soDong
            );
        } catch (SQLException e) {
            System.err.println("Lỗi đọc thông báo mới nhất: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<ThongBaoChiTiet> docDongMuonKemThongBao(
            String sql,
            Object... thamSo
    ) throws SQLException {
        List<ThongBaoChiTiet> danhSach = new ArrayList<>();
        try (
                Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ganThamSo(ps, thamSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ThongBaoChiTiet tb = mapThongBao(rs);
                    tb.setIdDocGia(rs.getInt("doc_gia_hien_tai"));
                    tb.setIdChiTiet(rs.getInt("chi_tiet_hien_tai"));
                    tb.setMocNhac(MOC_NHAC);
                    if (tb.getId() == 0) {
                        tb.setLoaiThongBao("SAP_DEN_HAN");
                        tb.setKenhGui("EMAIL");
                        tb.setTrangThai("CHUA_GUI");
                        tb.setNoiDung("");
                    }
                    danhSach.add(tb);
                }
            }
        }
        return danhSach;
    }

    // ================= TẠO HÀNG ĐỢI =================

    public int taoHoacLayNhacHan(
            int idChiTiet,
            LocalDate ngayXet,
            String tieuDe,
            String noiDung
    ) throws SQLException {
        if (idChiTiet <= 0) {
            return 0;
        }

        kiemTraNoiDung(tieuDe, noiDung);

        /*
         * id_doc_gia được lấy từ phiếu mượn trong MySQL.
         * Nếu khóa dòng/mốc đã tồn tại, giữ nội dung và trạng thái cũ.
         */
        String sql = """
                INSERT INTO thong_bao
                    (ma_thong_bao, id_doc_gia, loai_thong_bao,
                     noi_dung, kenh_gui, trang_thai,
                     id_chi_tiet, moc_nhac, tieu_de)
                SELECT ?, p.id_doc_gia, 'SAP_DEN_HAN',
                       ?, 'EMAIL', 'CHUA_GUI',
                       ct.id, ?, ?
                FROM chi_tiet_phieu_muon ct
                JOIN phieu_muon p ON p.id = ct.id_phieu_muon
                WHERE ct.id = ?
                  AND ct.ngay_tra IS NULL
                  AND ct.trang_thai = 'DANG_MUON'
                  AND ct.so_luong > 0
                  AND ct.han_tra = ?
                ON DUPLICATE KEY UPDATE
                    id = LAST_INSERT_ID(thong_bao.id)
                """;

        try (
                Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich()
        ) {
            conn.setAutoCommit(true);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ganThamSo(
                        ps,
                        sinhMaThongBao(),
                        noiDung,
                        MOC_NHAC,
                        tieuDe,
                        idChiTiet,
                        ngayMai(ngayXet)
                );

                // Không kiểm tra == 1 vì nhánh duplicate có thể trả 0/2.
                ps.executeUpdate();
            }

            String sqlLayId =
                    "SELECT tb.id FROM thong_bao tb "
                            + "WHERE tb.id_chi_tiet = ? AND "
                            + PHAM_VI;

            try (
                    PreparedStatement ps =
                            conn.prepareStatement(sqlLayId)
            ) {
                ganThamSo(ps, idChiTiet, MOC_NHAC);

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt("id") : 0;
                }
            }
        }
    }

    // ================= NHẬN XỬ LÝ =================

    /*
     * Chỉ tác vụ cập nhật được đúng một dòng mới được xử lý tiếp.
     * Connection riêng tự commit trước khi hàm trả về.
     *
     * Service phải đọc lại dòng mượn sau khi nhận xử lý,
     * trước khi gọi SMTP.
     */
    public boolean nhanXuLy(
            int idThongBao,
            int idChiTiet,
            int idDocGia
    ) throws SQLException {
        if (idThongBao <= 0 || idChiTiet <= 0 || idDocGia <= 0) {
            return false;
        }

        String sql = """
                UPDATE thong_bao tb
                SET trang_thai = 'DANG_GUI',
                    so_lan_thu = so_lan_thu + 1,
                    lan_thu_cuoi = NOW(),
                    loi_gui = NULL,
                    ngay_gui = NULL
                WHERE tb.id = ?
                  AND tb.id_chi_tiet = ?
                  AND tb.id_doc_gia = ?
                  AND
                """
                + PHAM_VI
                + """
                  AND tb.trang_thai IN ('CHUA_GUI', 'LOI', 'HUY')
                  """;

        return capNhat(
                sql,
                idThongBao,
                idChiTiet,
                idDocGia,
                MOC_NHAC
        ) == 1;
    }

    // ================= LƯU NỘI DUNG TRƯỚC SMTP =================

    public boolean capNhatNoiDungTruocGui(
            int idThongBao,
            int idChiTiet,
            int idDocGia,
            String emailNhan,
            String tieuDe,
            String noiDung
    ) throws SQLException {
        kiemTraNoiDung(tieuDe, noiDung);

        if (idThongBao <= 0
                || idChiTiet <= 0
                || idDocGia <= 0
                || emailNhan == null
                || emailNhan.isBlank()
                || emailNhan.length() > 254) {
            return false;
        }

        String sql = """
                UPDATE thong_bao tb
                SET email_nhan = ?,
                    tieu_de = ?,
                    noi_dung = ?
                WHERE tb.id = ?
                  AND tb.id_chi_tiet = ?
                  AND tb.id_doc_gia = ?
                  AND
                """
                + PHAM_VI
                + " AND tb.trang_thai = 'DANG_GUI'";

        return capNhat(
                sql,
                emailNhan,
                tieuDe,
                noiDung,
                idThongBao,
                idChiTiet,
                idDocGia,
                MOC_NHAC
        ) == 1;
    }

    // ================= GHI KẾT QUẢ =================

    /*
     * DA_GUI: chỉ gọi sau khi SMTP đã chấp nhận.
     * HUY: chỉ dùng khi chưa gọi SMTP và dòng không còn hợp lệ.
     */
    public boolean ketThucXuLy(
            int idThongBao,
            String trangThai,
            String emailNhan,
            String loiGui
    ) throws SQLException {
        return ketThucXuLy(idThongBao, trangThai, emailNhan, loiGui, "SAP_DEN_HAN", MOC_NHAC);
    }

    private boolean ketThucXuLy(int idThongBao, String trangThai, String emailNhan, String loiGui,
            String loaiThongBao, String mocNhac) throws SQLException {
        if (idThongBao <= 0
                || trangThai == null
                || !List.of(
                "DA_GUI", "LOI", "CAN_KIEM_TRA", "HUY"
        ).contains(trangThai)) {
            throw new IllegalArgumentException(
                    "Kết quả xử lý không hợp lệ."
            );
        }

        if ("DA_GUI".equals(trangThai)
                && (emailNhan == null || emailNhan.isBlank())) {
            throw new IllegalArgumentException(
                    "Thiếu địa chỉ thực tế đã gửi."
            );
        }

        String ghiChu = "DA_GUI".equals(trangThai)
                ? null
                : loiGui;

        String sql = """
                UPDATE thong_bao tb
                SET trang_thai = ?,
                    email_nhan = ?,
                    loi_gui = ?,
                    ngay_gui = CASE
                        WHEN ? = 'DA_GUI' THEN NOW()
                        ELSE NULL
                    END
                WHERE tb.id = ? AND tb.kenh_gui = 'EMAIL'
                  AND tb.loai_thong_bao = ? AND tb.moc_nhac = ?
                  AND tb.trang_thai = 'DANG_GUI'
                """;

        return capNhat(
                sql,
                trangThai,
                emailNhan,
                ghiChu,
                trangThai,
                idThongBao,
                loaiThongBao,
                mocNhac
        ) == 1;
    }

    // ================= HỦY NHẮC KHÔNG CÒN HỢP LỆ =================

    public int huyThongBaoKhongConDieuKien(
            LocalDate ngayXet
    ) throws SQLException {
        String sql = """
                UPDATE thong_bao tb
                LEFT JOIN chi_tiet_phieu_muon ct
                       ON ct.id = tb.id_chi_tiet
                LEFT JOIN phieu_muon p
                       ON p.id = ct.id_phieu_muon
                SET tb.trang_thai = 'HUY',
                    tb.loi_gui = 'Dòng mượn không còn đủ điều kiện nhắc.',
                    tb.ngay_gui = NULL
                WHERE
                """
                + PHAM_VI
                + """
                  AND tb.trang_thai IN ('CHUA_GUI', 'LOI')
                  AND (
                      ct.id IS NULL
                      OR p.id IS NULL
                      OR ct.ngay_tra IS NOT NULL
                      OR ct.trang_thai <> 'DANG_MUON'
                      OR ct.so_luong <= 0
                      OR ct.han_tra IS NULL
                      OR ct.han_tra <> ?
                      OR p.id_doc_gia <> tb.id_doc_gia
                  )
                  """;

        return capNhat(sql, MOC_NHAC, ngayMai(ngayXet));
    }

    public int huyNhacHanKhiDaTra(int idChiTiet) {
        if (idChiTiet <= 0) {
            return 0;
        }
        String sql = """
                UPDATE thong_bao tb
                SET tb.trang_thai = 'HUY',
                    tb.loi_gui = 'Độc giả đã hoàn trả sách trước hạn gửi email.',
                    tb.ngay_gui = NULL
                WHERE tb.id_chi_tiet = ?
                  AND tb.kenh_gui = 'EMAIL'
                  AND tb.loai_thong_bao = 'SAP_DEN_HAN'
                  AND tb.moc_nhac = ?
                  AND tb.trang_thai IN ('CHUA_GUI', 'LOI')
                """;
        try {
            return capNhat(sql, idChiTiet, MOC_NHAC);
        } catch (SQLException e) {
            System.err.println("Lỗi hủy nhắc hạn khi trả sách: " + e.getMessage());
            return 0;
        }
    }

    // ================= PHIÊN GỬI BỊ GIÁN ĐOẠN =================

    public int danhDauPhienGuiGianDoan() throws SQLException {
        String sql = """
                UPDATE thong_bao tb
                SET trang_thai = 'CAN_KIEM_TRA',
                    loi_gui = 'Phiên gửi chưa ghi nhận kết quả; cần kiểm tra.',
                    ngay_gui = NULL
                WHERE
                """
                + PHAM_VI
                + """
                  AND tb.trang_thai = 'DANG_GUI'
                  AND (
                      tb.lan_thu_cuoi IS NULL
                      OR tb.lan_thu_cuoi < DATE_SUB(NOW(), INTERVAL 10 MINUTE)
                  )
                  """;

        return capNhat(sql, MOC_NHAC);
    }

    // ================= LỊCH SỬ NHẮC HẠN =================

    public List<ThongBaoChiTiet> layLichSuEmail(int gioiHan) throws SQLException {
        int soDong = Math.max(1, Math.min(gioiHan, 500));
        return docDanhSach(SQL_DOC_THONG_BAO
                + " WHERE (" + PHAM_VI + ") OR (" + PHAM_VI_DAT_TRUOC + ")"
                + " ORDER BY tb.ngay_tao DESC, tb.id DESC LIMIT ?", MOC_NHAC, soDong);
    }

    public List<ThongBaoChiTiet> layLichSuNhacHan(
            int gioiHan
    ) throws SQLException {
        int soDong = Math.max(1, Math.min(gioiHan, 500));

        return docDanhSach(
                SQL_DOC_THONG_BAO
                        + " WHERE "
                        + PHAM_VI
                        + " ORDER BY tb.ngay_tao DESC, tb.id DESC LIMIT ?",
                MOC_NHAC,
                soDong
        );
    }

    // idThongBao là ID của bảng thong_bao.
    public ThongBaoChiTiet layThongBaoTheoId(
            int idThongBao
    ) throws SQLException {
        if (idThongBao <= 0) {
            return null;
        }

        List<ThongBaoChiTiet> danhSach = docDanhSach(
                SQL_DOC_THONG_BAO
                        + " WHERE tb.id = ? AND "
                        + PHAM_VI,
                idThongBao,
                MOC_NHAC
        );

        return danhSach.isEmpty() ? null : danhSach.get(0);
    }

    // ================= HÀM HỖ TRỢ =================

    private int capNhat(
            String sql,
            Object... thamSo
    ) throws SQLException {
        try (
                Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich()
        ) {
            conn.setAutoCommit(true);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ganThamSo(ps, thamSo);
                return ps.executeUpdate();
            }
        }
    }

    private List<ThongBaoChiTiet> docDanhSach(
            String sql,
            Object... thamSo
    ) throws SQLException {
        List<ThongBaoChiTiet> danhSach = new ArrayList<>();

        try (
                Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ganThamSo(ps, thamSo);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    danhSach.add(mapThongBao(rs));
                }
            }
        }

        return danhSach;
    }

    private void ganThamSo(
            PreparedStatement ps,
            Object... thamSo
    ) throws SQLException {
        for (int i = 0; i < thamSo.length; i++) {
            ps.setObject(i + 1, thamSo[i]);
        }
    }

    private java.sql.Date ngayMai(LocalDate ngayXet) {
        if (ngayXet == null) {
            throw new IllegalArgumentException("Thiếu ngày xét.");
        }

        return java.sql.Date.valueOf(ngayXet.plusDays(1));
    }

    private String sinhMaThongBao() {
        return "TB" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 28);
    }

    private void kiemTraNoiDung(
            String tieuDe,
            String noiDung
    ) {
        if (tieuDe == null
                || tieuDe.isBlank()
                || tieuDe.codePointCount(0, tieuDe.length()) > 255
                || noiDung == null
                || noiDung.isBlank()) {
            throw new IllegalArgumentException(
                    "Tiêu đề hoặc nội dung thông báo không hợp lệ."
            );
        }
    }

    private ThongBaoChiTiet mapThongBao(
            ResultSet rs
    ) throws SQLException {
        ThongBaoChiTiet tb = new ThongBaoChiTiet();

        tb.setId(rs.getInt("id"));
        tb.setMaThongBao(rs.getString("ma_thong_bao"));
        tb.setIdDocGia(rs.getInt("id_doc_gia"));
        tb.setLoaiThongBao(rs.getString("loai_thong_bao"));
        tb.setNoiDung(rs.getString("noi_dung"));
        tb.setKenhGui(rs.getString("kenh_gui"));
        tb.setNgayTao(rs.getTimestamp("ngay_tao"));
        tb.setNgayGui(rs.getTimestamp("ngay_gui"));
        tb.setTrangThai(rs.getString("trang_thai"));

        int idChiTiet = rs.getInt("id_chi_tiet");
        tb.setIdChiTiet(rs.wasNull() ? null : idChiTiet);

        tb.setMocNhac(rs.getString("moc_nhac"));
        tb.setEmailNhan(rs.getString("email_nhan"));
        tb.setTieuDe(rs.getString("tieu_de"));
        tb.setSoLanThu(rs.getInt("so_lan_thu"));
        tb.setLanThuCuoi(rs.getTimestamp("lan_thu_cuoi"));
        tb.setLoiGui(rs.getString("loi_gui"));

        tb.setMaDocGia(rs.getString("ma_doc_gia"));
        tb.setTenDocGia(rs.getString("ten_doc_gia"));
        tb.setEmailDocGia(rs.getString("email_doc_gia"));
        tb.setMaPhieu(rs.getString("ma_phieu"));
        tb.setTenSach(rs.getString("ten_sach"));
        tb.setSoLuong(rs.getInt("so_luong"));

        java.sql.Date hanTra = rs.getDate("han_tra");
        tb.setHanTra(hanTra == null ? null : hanTra.toLocalDate());

        return tb;
    }
}
