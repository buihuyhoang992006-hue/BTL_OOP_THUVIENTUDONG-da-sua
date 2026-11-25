package service;

import dao.ThongBaoDAO;
import model.NhomHanhVi;
import model.ThongBaoChiTiet;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ThongBaoService {

    private static final ZoneId MUI_GIO =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private static final DateTimeFormatter DINH_DANG_NGAY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String LOI_NHAC_MAU =
            "Thư viện xin nhắc bạn có sách đến hạn trả vào ngày mai.";

    private static final String HUONG_DAN_AI = """
            Bạn hỗ trợ soạn lời nhắc trả sách cho thư viện.

            Tin nhắn người dùng chỉ chứa tên một nhóm hành vi:
            - CHUA_CO_LICH_SU: chưa có lịch sử trả sách.
            - CHUA_RO_XU_HUONG: chưa đủ căn cứ xác định xu hướng.
            - THUONG_DUNG_HAN: thường trả sớm hoặc đúng hạn.
            - THUONG_TRA_MUON: thường trả muộn trong lịch sử gần đây.

            Viết đúng hai câu ngắn bằng tiếng Việt, tổng cộng tối đa
            80 từ, thân thiện và lịch sự, nhắc trả sách vào ngày mai.

            Với THUONG_DUNG_HAN, có thể cảm ơn việc giữ hẹn.
            Với THUONG_TRA_MUON, nhẹ nhàng gợi ý sắp xếp thời gian.
            Với hai nhóm còn lại, dùng lời nhắc trung tính.

            Không nêu tên nhóm, số lần vi phạm hoặc lịch sử cụ thể.
            Không khẳng định người đọc luôn có một thói quen nào đó.
            Không tự đặt tên người, tên sách, ngày tháng, số bản,
            mức phạt, quy định thư viện hoặc thông tin liên hệ.
            Không viết lời chào hoặc chữ ký.
            Không dùng Markdown, HTML, emoji hoặc liên kết.
            Chỉ trả về hai câu lời nhắc.
            """;

    private static final String BO_QUA = "BO_QUA";

    private static final ThongBaoService INSTANCE =
            new ThongBaoService();

    private final ThongBaoDAO thongBaoDAO =
            new ThongBaoDAO();

    private final EmailService emailService =
            new EmailService();

    private final HanhViTraSachService hanhViService =
            new HanhViTraSachService();

    private final AiService aiService =
            new AiService();

    private final AtomicBoolean dangQuet =
            new AtomicBoolean(false);

    private final ScheduledExecutorService boLap =
            Executors.newSingleThreadScheduledExecutor(congViec -> {
                Thread thread = new Thread(
                        congViec,
                        "thu-vien-nhac-han"
                );
                thread.setDaemon(true);
                return thread;
            });

    private ScheduledFuture<?> lichQuet;

    private ThongBaoService() {
    }

    public static ThongBaoService getInstance() {
        return INSTANCE;
    }

    public record BanXemTruoc(
            int idChiTiet,
            String maDocGia,
            String tenDocGia,
            String emailDocGia,
            String emailNhan,
            String cheDoGui,
            String tieuDe,
            String noiDung,
            String ghiChu
    ) {
    }

    public record KetQuaQuet(
            int soDongDenHan,
            int daGui,
            int loi,
            int canKiemTra,
            int huy,
            int boQua,
            String ghiChu
    ) {
        public String tomTat() {
            return "Dòng đến hạn trong phạm vi quét: " + soDongDenHan
                    + "\nĐã gửi và lưu kết quả: " + daGui
                    + "\nLỗi xử lý: " + loi
                    + "\nCần kiểm tra: " + canKiemTra
                    + "\nĐã hủy: " + huy
                    + "\nBỏ qua: " + boQua
                    + "\n" + ghiChu;
        }
    }

    /*
     * Chỉ chứa lời dẫn và ghi chú.
     * Toàn bộ email được ghép từ dữ liệu MySQL sau cùng.
     */
    private record LoiNhac(
            String loiDan,
            String ghiChu
    ) {
    }

    public synchronized void batDauTuDong() {
        if (lichQuet != null
                && !lichQuet.isCancelled()
                && !lichQuet.isDone()) {
            return;
        }

        lichQuet = boLap.scheduleWithFixedDelay(() -> {
            quetEmailDatTruoc();
            try {
                KetQuaQuet ketQua = quetVaGui();
                System.out.println("[Nhắc hạn]\n" + ketQua.tomTat());
            } catch (RuntimeException e) {
                System.err.println(
                        "[Nhắc hạn] Lượt quét gặp lỗi: "
                                + loiAnToan(e)
                );
            }
        }, 0, 15, TimeUnit.MINUTES);
    }

    // Gọi sau commit. Tác vụ định kỳ vẫn xử lý hàng đợi nếu ứng dụng
    // đóng ngay sau khi nghiệp vụ đặt trước đã lưu thành công.
    public void yeuCauXuLyEmailDatTruoc() {
        boLap.execute(this::quetEmailDatTruoc);
    }

    private void quetEmailDatTruoc() {
        try {
            String tienTo = EmailService.tienToMaTheGui();
            emailService.kiemTraCauHinh();
            thongBaoDAO.donHangDoiEmailDatTruoc(tienTo);

            for (ThongBaoChiTiet email : thongBaoDAO.layEmailDatTruocChoGui(tienTo)) {
                xuLyEmailDatTruoc(email.getId(), tienTo);
            }
        } catch (SQLException | RuntimeException e) {
            System.err.println("[Email đặt trước] " + loiAnToan(e));
        }
    }

    private void xuLyEmailDatTruoc(int id, String tienTo) throws SQLException {
        if (!thongBaoDAO.nhanEmailDatTruoc(id)) {
            return;
        }
        // Đọc sau khi nhận việc, trên kết nối không giữ khóa đặt trước/kho sách.
        boolean daGoiSmtp = false;
        String emailNhan = null;
        try {
            ThongBaoChiTiet email = thongBaoDAO.docEmailDatTruocDangXuLy(id, tienTo);
            if (email == null) {
                ghiKetQua(id, "HUY", null, "Lượt giữ sách không còn đủ điều kiện gửi email.", true);
                return;
            }
            emailNhan = EmailService.diaChiNhanThucTe(email.getEmailDocGia());
            if (!thongBaoDAO.luuDiaChiEmailDatTruoc(id, emailNhan)) {
                return;
            }
            daGoiSmtp = true;
            EmailService.KetQuaGui ketQua = emailService.guiEmail(
                    email.getEmailDocGia(), email.getTieuDe(), email.getNoiDung());
            ghiKetQua(id, ketQua.getTrangThai(), ketQua.getEmailNhan(), ketQua.getLoi(), true);
        } catch (SQLException | RuntimeException e) {
            ghiKetQua(id, daGoiSmtp ? "CAN_KIEM_TRA" : "LOI", emailNhan, loiAnToan(e), true);
        }
    }

    public synchronized void dungTuDong() {
        if (lichQuet != null) {
            lichQuet.cancel(false);
            lichQuet = null;
        }
    }

    /*
     * Chỉ đọc MySQL và gọi AI để tạo bản nháp.
     * Không tạo hàng đợi, đổi trạng thái hay gọi SMTP.
     * Giao diện gọi hàm này qua SwingWorker.
     */
    public BanXemTruoc xemTruoc(
            int idChiTiet
    ) throws SQLException {

        if (idChiTiet <= 0) {
            throw new IllegalArgumentException(
                    "ID dòng mượn không hợp lệ."
            );
        }

        kiemTraBiNgat();

        ThongBaoChiTiet banDau =
                thongBaoDAO.layDongCanNhacTheoId(
                        idChiTiet,
                        homNay()
                );

        if (banDau == null) {
            throw new IllegalArgumentException(
                    "Dòng mượn phải chưa trả và có hạn trả vào ngày mai."
            );
        }

        LoiNhac loiNhac = soanLoiNhac(
                banDau.getIdDocGia()
        );

        kiemTraBiNgat();

        // Đọc lại vì AI có thể cần vài giây để trả lời.
        ThongBaoChiTiet moi =
                thongBaoDAO.layDongCanNhacTheoId(
                        idChiTiet,
                        homNay()
                );

        if (moi == null
                || moi.getIdDocGia() != banDau.getIdDocGia()) {
            throw new IllegalArgumentException(
                    "Dòng mượn đã thay đổi hoặc không còn "
                            + "đủ điều kiện nhắc hạn. Hãy làm mới."
            );
        }

        String cheDo = "CHUA_XAC_DINH";
        String emailNhan = "Chưa xác định";

        String ghiChu =
                loiNhac.ghiChu()
                        + "\nĐây là bản nháp xem trước. "
                        + "Chưa tạo hàng đợi hoặc gửi email."
                        + "\nKhi gửi, AI sẽ soạn lại; "
                        + "cách diễn đạt có thể khác bản xem trước.";

        try {
            cheDo = EmailService.cheDoGui();
            String tienTo = EmailService.tienToMaTheGui();
            if (trongPhamVi(moi, !tienTo.isEmpty(), tienTo)) {
                emailNhan = EmailService.diaChiNhanThucTe(moi.getEmailDocGia());
            } else {
                emailNhan = "Ngoài phạm vi TEST";
                ghiChu += "\nLượt gửi thử chỉ xử lý mã thẻ bắt đầu bằng " + tienTo + ".";
            }
        } catch (RuntimeException e) {
            ghiChu += "\nChưa xác định được địa chỉ nhận: "
                    + loiAnToan(e);
        }

        return new BanXemTruoc(
                idChiTiet,
                giaTri(moi.getMaDocGia(), ""),
                giaTri(moi.getTenDocGia(), "Bạn đọc"),
                giaTri(moi.getEmailDocGia(), ""),
                emailNhan,
                cheDo,
                taoTieuDe(moi),
                taoNoiDung(moi, loiNhac.loiDan()),
                ghiChu
        );
    }

    /*
     * Có truy vấn MySQL, gọi AI và SMTP.
     * Chỉ gọi trên luồng nền.
     */
    public KetQuaQuet quetVaGui() {
        if (!dangQuet.compareAndSet(false, true)) {
            return new KetQuaQuet(
                    0, 0, 0, 0, 0, 0,
                    "Một lượt quét khác đang chạy."
            );
        }

        int soDongDenHan = 0;
        int daGui = 0;
        int loi = 0;
        int canKiemTra = 0;
        int huy = 0;
        int boQua = 0;
        String ghiChu = "";

        try {
            kiemTraBiNgat();

            String tienToDemo = EmailService.tienToMaTheGui();
            boolean cheDoThu = !tienToDemo.isEmpty();

            emailService.kiemTraCauHinh();

            LocalDate ngayXet = homNay();

            if (!cheDoThu) {
                canKiemTra +=
                        thongBaoDAO.danhDauPhienGuiGianDoan();

                huy += thongBaoDAO.huyThongBaoKhongConDieuKien(
                        ngayXet
                );
            }

            /*
             * TEST không chạy cập nhật trạng thái hàng loạt,
             * để tránh tác động độc giả ngoài phạm vi demo.
             */
            List<ThongBaoChiTiet> danhSach =
                    thongBaoDAO.layDongCanNhac(ngayXet);

            ghiChu = cheDoThu
                    ? "TEST: chỉ xử lý mã thẻ bắt đầu bằng "
                    + tienToDemo
                    : "REAL: xử lý các dòng mượn đủ điều kiện.";

            for (ThongBaoChiTiet dong : danhSach) {
                kiemTraBiNgat();

                if (!trongPhamVi(dong, cheDoThu, tienToDemo)) {
                    continue;
                }

                soDongDenHan++;

                String trangThai = dong.getTrangThai();

                if ("DA_GUI".equals(trangThai)
                        || "DANG_GUI".equals(trangThai)
                        || "CAN_KIEM_TRA".equals(trangThai)) {
                    boQua++;
                    continue;
                }

                try {
                    String ketQua = xuLyDong(
                            dong,
                            cheDoThu,
                            tienToDemo
                    );

                    switch (ketQua) {
                        case "DA_GUI" -> daGui++;
                        case "LOI" -> loi++;
                        case "CAN_KIEM_TRA" -> canKiemTra++;
                        case "HUY" -> huy++;
                        default -> boQua++;
                    }
                } catch (SQLException | RuntimeException e) {
                    loi++;

                    System.err.println(
                            "[Nhắc hạn] Không xử lý được dòng mượn "
                                    + dong.getIdChiTiet()
                                    + ": " + loiAnToan(e)
                    );
                }
            }
        } catch (SQLException | RuntimeException e) {
            loi++;
            ghiChu = "Lượt quét dừng: " + loiAnToan(e);
        } finally {
            dangQuet.set(false);
        }

        return new KetQuaQuet(
                soDongDenHan,
                daGui,
                loi,
                canKiemTra,
                huy,
                boQua,
                ghiChu
        );
    }

    private String xuLyDong(
            ThongBaoChiTiet dong,
            boolean cheDoThu,
            String tienToDemo
    ) throws SQLException {

        kiemTraBiNgat();

        Integer idChiTiet = dong.getIdChiTiet();

        if (idChiTiet == null || idChiTiet <= 0) {
            return BO_QUA;
        }

        int idDocGia = dong.getIdDocGia();

        /*
         * Tạo hàng đợi bằng nội dung mẫu trước.
         * Chỉ gọi AI sau khi nhận xử lý thành công.
         */
        int idThongBao = thongBaoDAO.taoHoacLayNhacHan(
                idChiTiet,
                homNay(),
                taoTieuDe(dong),
                taoNoiDung(dong, LOI_NHAC_MAU)
        );

        if (idThongBao <= 0) {
            return BO_QUA;
        }

        if (!thongBaoDAO.nhanXuLy(
                idThongBao,
                idChiTiet,
                idDocGia
        )) {
            return BO_QUA;
        }

        boolean daGoiSmtp = false;
        String emailNhan = null;

        try {
            ThongBaoChiTiet truocAi =
                    thongBaoDAO.layDongCanNhacTheoId(
                            idChiTiet,
                            homNay()
                    );

            String ketThuc = dungNeuKhongConDieuKien(
                    truocAi,
                    idThongBao,
                    idDocGia,
                    cheDoThu,
                    tienToDemo
            );

            if (ketThuc != null) {
                return ketThuc;
            }

            LoiNhac loiNhac = soanLoiNhac(idDocGia);

            kiemTraBiNgat();

            /*
             * Sau khi chờ AI, đọc lại dữ liệu để tránh gửi cho
             * dòng vừa trả hoặc không còn đến hạn ngày mai.
             */
            ThongBaoChiTiet moi =
                    thongBaoDAO.layDongCanNhacTheoId(
                            idChiTiet,
                            homNay()
                    );

            ketThuc = dungNeuKhongConDieuKien(
                    moi,
                    idThongBao,
                    idDocGia,
                    cheDoThu,
                    tienToDemo
            );

            if (ketThuc != null) {
                return ketThuc;
            }

            String tieuDe = taoTieuDe(moi);

            // Ghép thông tin bằng snapshot MySQL sau cùng.
            String noiDung = taoNoiDung(
                    moi,
                    loiNhac.loiDan()
            );

            emailNhan = EmailService.diaChiNhanThucTe(
                    moi.getEmailDocGia()
            );

            kiemTraBiNgat();

            boolean daLuuNoiDung =
                    thongBaoDAO.capNhatNoiDungTruocGui(
                            idThongBao,
                            idChiTiet,
                            idDocGia,
                            emailNhan,
                            tieuDe,
                            noiDung
                    );

            if (!daLuuNoiDung) {
                System.err.println(
                        "[Nhắc hạn] Dấu xử lý thông báo "
                                + idThongBao
                                + " đã thay đổi; dừng gửi để kiểm tra."
                );
                return "CAN_KIEM_TRA";
            }

            kiemTraBiNgat();

            daGoiSmtp = true;

            EmailService.KetQuaGui ketQua =
                    emailService.guiEmail(
                            moi.getEmailDocGia(),
                            tieuDe,
                            noiDung
                    );

            return ghiKetQua(
                    idThongBao,
                    ketQua.getTrangThai(),
                    ketQua.getEmailNhan(),
                    ketQua.getLoi()
            );
        } catch (SQLException | RuntimeException e) {
            return ghiKetQua(
                    idThongBao,
                    daGoiSmtp ? "CAN_KIEM_TRA" : "LOI",
                    emailNhan,
                    loiAnToan(e)
            );
        }
    }

    /*
     * Trả null nếu còn đủ điều kiện để tiếp tục.
     * Chỉ hủy trước khi gọi SMTP.
     */
    private String dungNeuKhongConDieuKien(
            ThongBaoChiTiet dong,
            int idThongBao,
            int idDocGia,
            boolean cheDoThu,
            String tienToDemo
    ) {
        if (dong == null) {
            return ghiKetQua(
                    idThongBao,
                    "HUY",
                    null,
                    "Dòng mượn không còn đủ điều kiện nhắc hạn."
            );
        }

        if (dong.getId() != idThongBao
                || !"DANG_GUI".equals(dong.getTrangThai())) {
            System.err.println(
                    "[Nhắc hạn] Dấu xử lý đã thay đổi ở thông báo "
                            + idThongBao
                            + "; dừng gửi để kiểm tra."
            );
            return "CAN_KIEM_TRA";
        }

        if (dong.getIdDocGia() != idDocGia
                || !trongPhamVi(dong, cheDoThu, tienToDemo)) {
            return ghiKetQua(
                    idThongBao,
                    "HUY",
                    null,
                    "Độc giả hoặc phạm vi xử lý đã thay đổi."
            );
        }

        return null;
    }

    private LoiNhac soanLoiNhac(int idDocGia) {
        NhomHanhVi nhom = null;

        try {
            kiemTraBiNgat();

            nhom = hanhViService.layNhomHanhVi(idDocGia);

            kiemTraBiNgat();

            /*
             * Ranh giới dữ liệu:
             * AI chỉ nhận tên enum, không nhận ID độc giả,
             * tên, email, sách, hạn trả hay lịch sử chi tiết.
             */
            String loiDan = aiService.taoVanBan(
                    HUONG_DAN_AI,
                    nhom.name()
            );

            kiemTraBiNgat();

            return new LoiNhac(
                    kiemTraLoiDan(loiDan),
                    "Soạn lời nhắc bằng AI. Nhóm hành vi: "
                            + nhom.getMoTa() + "."
            );
        } catch (
                SQLException
                | AiService.AiException
                | RuntimeException e
        ) {
            /*
             * Bị ngắt thì dừng tác vụ.
             * Lỗi AI thông thường mới dùng mẫu dự phòng.
             */
            kiemTraBiNgat();

            String ghiChu;

            if (nhom == null) {
                ghiChu =
                        "Dùng mẫu cố định vì chưa đọc/phân tích được "
                                + "lịch sử trả sách.";
            } else {
                ghiChu =
                        "Dùng mẫu cố định. Nhóm hành vi: "
                                + nhom.getMoTa() + ".";
            }

            ghiChu += " Lý do: " + loiAnToan(e);

            System.err.println("[Nhắc hạn] " + ghiChu);

            return new LoiNhac(
                    LOI_NHAC_MAU,
                    ghiChu
            );
        }
    }

    private static String kiemTraLoiDan(String loiDan) {
        if (loiDan == null || loiDan.isBlank()) {
            throw new IllegalArgumentException(
                    "Lời nhắc AI rỗng."
            );
        }

        String ketQua = loiDan.trim().replaceAll("\\s+", " ");

        String chuThuong = ketQua.toLowerCase(Locale.ROOT);

        if (ketQua.length() > 600
                || ketQua.contains("<")
                || ketQua.contains(">")
                || ketQua.contains("```")
                || ketQua.contains("@")
                || chuThuong.contains("http://")
                || chuThuong.contains("https://")
                || chuThuong.contains("www.")) {
            throw new IllegalArgumentException(
                    "Lời nhắc AI không đúng định dạng văn bản ngắn."
            );
        }

        return ketQua;
    }

    private String ghiKetQua(
            int idThongBao,
            String trangThai,
            String emailNhan,
            String loiGui
    ) {
        return ghiKetQua(idThongBao, trangThai, emailNhan, loiGui, false);
    }

    private String ghiKetQua(int idThongBao, String trangThai, String emailNhan, String loiGui,
            boolean datTruoc) {
        String nhan = datTruoc ? "[Email đặt trước]" : "[Nhắc hạn]";
        try {
            boolean daLuu = datTruoc
                    ? thongBaoDAO.ketThucGuiEmailDatTruoc(idThongBao, trangThai, emailNhan, loiGui)
                    : thongBaoDAO.ketThucXuLy(idThongBao, trangThai, emailNhan, loiGui);

            if (daLuu) {
                return trangThai;
            }

            System.err.println(
                    nhan + " Trạng thái thông báo "
                            + idThongBao
                            + " đã thay đổi; cần kiểm tra lịch sử."
            );
        } catch (SQLException | RuntimeException e) {
            System.err.println(
                    nhan + " Không lưu được kết quả thông báo "
                            + idThongBao
                            + ". Cần kiểm tra trước khi xử lý tiếp: "
                            + loiAnToan(e)
            );
        }

        return "CAN_KIEM_TRA";
    }

    private String taoTieuDe(ThongBaoChiTiet dong) {
        return "Nhắc hạn trả sách ngày "
                + dong.getHanTra().format(DINH_DANG_NGAY);
    }

    private String taoNoiDung(
            ThongBaoChiTiet dong,
            String loiDan
    ) {
        return "Chào bạn "
                + giaTri(dong.getTenDocGia(), "bạn đọc")
                + ",\n\n"
                + loiDan
                + "\n\n"
                + "Tên sách: "
                + giaTri(dong.getTenSach(), "Chưa có tên")
                + "\n"
                + "Số bản: " + dong.getSoLuong()
                + "\n"
                + "Hạn trả: "
                + dong.getHanTra().format(DINH_DANG_NGAY)
                + "\n"
                + "Mã phiếu mượn: "
                + giaTri(dong.getMaPhieu(), "Chưa có mã")
                + "\n\n"
                + "Nếu bạn vừa hoàn tất trả sách, "
                + "xin bỏ qua lời nhắc này.\n\n"
                + "Cảm ơn bạn!\n"
                + "Thư viện";
    }

    private static boolean trongPhamVi(
            ThongBaoChiTiet dong,
            boolean cheDoThu,
            String tienToDemo
    ) {
        if (!cheDoThu) {
            return true;
        }

        String maDocGia = dong.getMaDocGia();

        return maDocGia != null
                && maDocGia.startsWith(tienToDemo);
    }

    private static void kiemTraBiNgat() {
        if (Thread.currentThread().isInterrupted()) {
            throw new IllegalStateException(
                    "Tác vụ nhắc hạn đã bị ngắt."
            );
        }
    }

    private static LocalDate homNay() {
        return LocalDate.now(MUI_GIO);
    }

    private static String giaTri(
            String noiDung,
            String macDinh
    ) {
        return noiDung == null || noiDung.isBlank()
                ? macDinh
                : noiDung.trim();
    }

    private static String loiAnToan(Exception e) {
        String noiDung = e.getMessage();

        if (noiDung == null || noiDung.isBlank()) {
            noiDung = e.getClass().getSimpleName();
        }

        for (String tenBien : new String[]{
                "SMTP_PASSWORD",
                "AI_API_KEY"
        }) {
            String biMat = System.getenv(tenBien);

            if (biMat != null && !biMat.isEmpty()) {
                noiDung = noiDung.replace(biMat, "***");
            }
        }

        return noiDung.length() > 1000
                ? noiDung.substring(0, 1000)
                : noiDung;
    }
}
