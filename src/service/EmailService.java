package service;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class EmailService {

    public static final String DA_GUI = "DA_GUI";
    public static final String LOI = "LOI";
    public static final String CAN_KIEM_TRA = "CAN_KIEM_TRA";

    private static final String UTF8 = "UTF-8";
    private static final String TIMEOUT_MS = "10000";

    // ================= KẾT QUẢ GỬI =================

    public static final class KetQuaGui {

        private final String trangThai;
        private final String emailNhan;
        private final String loi;

        private KetQuaGui(
                String trangThai,
                String emailNhan,
                String loi
        ) {
            this.trangThai = trangThai;
            this.emailNhan = emailNhan;
            this.loi = loi;
        }

        public String getTrangThai() {
            return trangThai;
        }

        public String getEmailNhan() {
            return emailNhan;
        }

        public String getLoi() {
            return loi;
        }

        public boolean isDaGui() {
            return DA_GUI.equals(trangThai);
        }
    }

    // ================= CHẾ ĐỘ VÀ NGƯỜI NHẬN =================

    public static String cheDoGui() {
        String cheDo = layBien("SMTP_MODE", "TEST")
                .toUpperCase(Locale.ROOT);

        if (!List.of("TEST", "REAL").contains(cheDo)) {
            throw new IllegalArgumentException(
                    "SMTP_MODE phải là TEST hoặc REAL."
            );
        }

        return cheDo;
    }

    // Chuỗi rỗng biểu thị chế độ gửi thật. TEST luôn yêu cầu tiền tố.
    public static String tienToMaTheGui() {
        if ("REAL".equals(cheDoGui())) {
            return "";
        }
        String tienTo = layBien("SMTP_TEST_READER_PREFIX", "");
        if (tienTo.isEmpty()) {
            throw new IllegalStateException(
                    "Thiếu SMTP_TEST_READER_PREFIX. Chế độ TEST yêu cầu tiền tố mã thẻ demo.");
        }
        return tienTo;
    }

    public static String diaChiNhanThucTe(String emailDocGia) {
        // Kiểm tra email từ MySQL trước.
        InternetAddress diaChiGoc =
                motDiaChi(emailDocGia, "Email độc giả");

        if ("TEST".equals(cheDoGui())) {
            return motDiaChi(
                    layBien("SMTP_TEST_TO", ""),
                    "SMTP_TEST_TO"
            ).getAddress();
        }

        return diaChiGoc.getAddress();
    }

    /*
     * Chỉ kiểm tra các biến cấu hình.
     * Không kết nối SMTP, không gửi email, không ghi database.
     */
    public void kiemTraCauHinh() {
        docCauHinhSmtp();

        if ("TEST".equals(cheDoGui())) {
            motDiaChi(
                    layBien("SMTP_TEST_TO", ""),
                    "SMTP_TEST_TO"
            );
        }
    }

    // ================= GỬI EMAIL =================

    /*
     * Hàm chạy đồng bộ và trả kết quả sau khi SMTP phản hồi.
     * Gọi từ SwingWorker hoặc tác vụ nền, không gọi trực tiếp trên EDT.
     *
     * Hàm này không cập nhật database.
     */
    public KetQuaGui guiEmail(
            String emailDocGia,
            String tieuDe,
            String noiDung
    ) {
        Transport transport = null;
        CauHinhSmtp cauHinh = null;
        String emailNhan = null;

        boolean daBatDauGui = false;

        try {
            emailNhan = diaChiNhanThucTe(emailDocGia);
            kiemTraNoiDung(tieuDe, noiDung);

            cauHinh = docCauHinhSmtp();

            Properties props = taoThuocTinh(cauHinh);
            Session session = Session.getInstance(props);
            session.setDebug(false);

            MimeMessage message = new MimeMessage(session);

            message.setFrom(new InternetAddress(
                    cauHinh.emailGui,
                    cauHinh.tenNguoiGui,
                    UTF8
            ));

            message.setRecipient(
                    Message.RecipientType.TO,
                    motDiaChi(emailNhan, "Email nhận")
            );

            message.setSubject(tieuDe.trim(), UTF8);
            message.setText(noiDung, UTF8);
            message.setSentDate(new Date());

            // Hoàn thiện Message-ID và các header MIME trước khi gửi.
            message.saveChanges();

            transport = session.getTransport("smtp");

            if (cauHinh.xacThuc) {
                transport.connect(
                        cauHinh.host,
                        cauHinh.port,
                        cauHinh.user,
                        cauHinh.matKhau
                );
            } else {
                transport.connect(
                        cauHinh.host,
                        cauHinh.port,
                        null,
                        null
                );
            }

            /*
             * Từ lúc bắt đầu sendMessage, lỗi kết nối có thể khiến
             * ứng dụng chưa biết SMTP đã nhận email hay chưa.
             */
            daBatDauGui = true;

            transport.sendMessage(
                    message,
                    message.getAllRecipients()
            );

            // sendMessage trả về bình thường: SMTP đã chấp nhận.
            return new KetQuaGui(
                    DA_GUI,
                    emailNhan,
                    null
            );

        } catch (Exception e) {
            return new KetQuaGui(
                    daBatDauGui ? CAN_KIEM_TRA : LOI,
                    emailNhan,
                    rutGonLoi(e, cauHinh)
            );

        } finally {
            if (transport != null) {
                try {
                    transport.close();
                } catch (Exception e) {
                    /*
                     * Lỗi đóng kết nối không được đổi kết quả
                     * của email đã được SMTP chấp nhận.
                     */
                    System.err.println(
                            "[SMTP close] " + rutGonLoi(e, cauHinh)
                    );
                }
            }
        }
    }

    // ================= KIỂM TRA ĐỊA CHỈ / NỘI DUNG =================

    private static InternetAddress motDiaChi(
            String giaTri,
            String nhan
    ) {
        if (giaTri == null
                || giaTri.isBlank()
                || coXuongDong(giaTri)) {
            throw new IllegalArgumentException(
                    nhan + " đang trống hoặc không hợp lệ."
            );
        }

        try {
            InternetAddress[] danhSach =
                    InternetAddress.parse(giaTri.trim(), true);

            if (danhSach.length != 1
                    || danhSach[0].isGroup()) {
                throw new IllegalArgumentException(
                        nhan + " phải chứa đúng một địa chỉ email."
                );
            }

            danhSach[0].validate();
            return danhSach[0];

        } catch (AddressException e) {
            throw new IllegalArgumentException(
                    nhan + " không hợp lệ."
            );
        }
    }

    private static void kiemTraNoiDung(
            String tieuDe,
            String noiDung
    ) {
        if (tieuDe == null
                || tieuDe.isBlank()
                || coXuongDong(tieuDe)) {
            throw new IllegalArgumentException(
                    "Tiêu đề email đang trống hoặc không hợp lệ."
            );
        }

        if (noiDung == null || noiDung.isBlank()) {
            throw new IllegalArgumentException(
                    "Nội dung email không được trống."
            );
        }
    }

    private static boolean coXuongDong(String giaTri) {
        return giaTri.indexOf('\r') >= 0
                || giaTri.indexOf('\n') >= 0;
    }

    // ================= ĐỌC CẤU HÌNH =================

    private static CauHinhSmtp docCauHinhSmtp() {
        String host = layBien("SMTP_HOST", "");

        if (host.isEmpty() || coXuongDong(host)) {
            throw new IllegalArgumentException(
                    "Chưa cấu hình SMTP_HOST hợp lệ."
            );
        }

        String baoMat = layBien("SMTP_SECURITY", "STARTTLS")
                .toUpperCase(Locale.ROOT);

        if (!List.of("STARTTLS", "SSL", "NONE").contains(baoMat)) {
            throw new IllegalArgumentException(
                    "SMTP_SECURITY phải là STARTTLS, SSL hoặc NONE."
            );
        }

        int port;

        try {
            port = Integer.parseInt(
                    layBien(
                            "SMTP_PORT",
                            "SSL".equals(baoMat) ? "465" : "587"
                    )
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "SMTP_PORT phải là số nguyên."
            );
        }

        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException(
                    "SMTP_PORT phải từ 1 đến 65535."
            );
        }

        String authText = layBien("SMTP_AUTH", "true")
                .toLowerCase(Locale.ROOT);

        if (!List.of("true", "false").contains(authText)) {
            throw new IllegalArgumentException(
                    "SMTP_AUTH phải là true hoặc false."
            );
        }

        boolean xacThuc = Boolean.parseBoolean(authText);
        String user = layBien("SMTP_USER", "");

        // Giữ nguyên mật khẩu, không tự trim hoặc bỏ dấu cách.
        String matKhau = System.getenv("SMTP_PASSWORD");

        if (matKhau == null) {
            matKhau = "";
        }

        if (xacThuc && (user.isEmpty() || matKhau.isBlank())) {
            throw new IllegalArgumentException(
                    "Chưa cấu hình SMTP_USER hoặc SMTP_PASSWORD."
            );
        }

        String emailGui = motDiaChi(
                layBien("SMTP_FROM", user),
                "SMTP_FROM"
        ).getAddress();

        String tenNguoiGui =
                layBien("SMTP_FROM_NAME", "Thư viện");

        if (coXuongDong(tenNguoiGui)) {
            throw new IllegalArgumentException(
                    "SMTP_FROM_NAME không hợp lệ."
            );
        }

        return new CauHinhSmtp(
                host,
                port,
                user,
                matKhau,
                emailGui,
                tenNguoiGui,
                baoMat,
                xacThuc
        );
    }

    private static Properties taoThuocTinh(CauHinhSmtp cauHinh) {
        Properties props = new Properties();

        boolean startTls = "STARTTLS".equals(cauHinh.baoMat);
        boolean ssl = "SSL".equals(cauHinh.baoMat);

        props.setProperty(
                "mail.smtp.auth",
                Boolean.toString(cauHinh.xacThuc)
        );

        props.setProperty(
                "mail.smtp.starttls.enable",
                Boolean.toString(startTls)
        );

        props.setProperty(
                "mail.smtp.starttls.required",
                Boolean.toString(startTls)
        );

        props.setProperty(
                "mail.smtp.ssl.enable",
                Boolean.toString(ssl)
        );

        props.setProperty("mail.smtp.ssl.checkserveridentity", "true");
        props.setProperty("mail.smtp.from", cauHinh.emailGui);

        props.setProperty("mail.smtp.connectiontimeout", TIMEOUT_MS);
        props.setProperty("mail.smtp.timeout", TIMEOUT_MS);
        props.setProperty("mail.smtp.writetimeout", TIMEOUT_MS);

        props.setProperty("mail.smtp.sendpartial", "false");
        props.setProperty("mail.smtp.quitwait", "false");
        props.setProperty("mail.debug.auth", "false");

        return props;
    }

    private static String layBien(
            String tenBien,
            String macDinh
    ) {
        String giaTri = System.getenv(tenBien);

        return giaTri == null || giaTri.isBlank()
                ? macDinh
                : giaTri.trim();
    }

    private static String rutGonLoi(
            Exception loi,
            CauHinhSmtp cauHinh
    ) {
        String thongBao = loi.getClass().getSimpleName();

        if (loi.getMessage() != null) {
            thongBao += ": " + loi.getMessage();
        }

        if (cauHinh != null && !cauHinh.matKhau.isEmpty()) {
            thongBao = thongBao.replace(
                    cauHinh.matKhau,
                    "[AN_MAT_KHAU]"
            );
        }

        return thongBao.length() > 1000
                ? thongBao.substring(0, 1000)
                : thongBao;
    }

    private static final class CauHinhSmtp {

        private final String host;
        private final int port;
        private final String user;
        private final String matKhau;
        private final String emailGui;
        private final String tenNguoiGui;
        private final String baoMat;
        private final boolean xacThuc;

        private CauHinhSmtp(
                String host,
                int port,
                String user,
                String matKhau,
                String emailGui,
                String tenNguoiGui,
                String baoMat,
                boolean xacThuc
        ) {
            this.host = host;
            this.port = port;
            this.user = user;
            this.matKhau = matKhau;
            this.emailGui = emailGui;
            this.tenNguoiGui = tenNguoiGui;
            this.baoMat = baoMat;
            this.xacThuc = xacThuc;
        }
    }
}
