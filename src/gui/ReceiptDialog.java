package gui;

import org.kordamp.ikonli.feather.Feather;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Hộp thoại Xem trước và In Phiếu Mượn / Biên lai Thu Phạt sạch đẹp.
 */
public class ReceiptDialog extends JDialog {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final JEditorPane editorPane;
    private final String htmlContent;
    private final String printHeader;
    private final String defaultFilePrefix;

    private ReceiptDialog(
            Window owner,
            String dialogTitle,
            String dialogSubtitle,
            String printHeader,
            String defaultFilePrefix,
            String htmlContent) {
        super(owner, dialogTitle, ModalityType.APPLICATION_MODAL);
        this.htmlContent = htmlContent;
        this.printHeader = printHeader;
        this.defaultFilePrefix = defaultFilePrefix;

        setSize(580, 650);
        setMinimumSize(new Dimension(520, 560));
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(ThemeConfig.BG_PAGE);
        root.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Header
        JPanel headerPanel = ThemeConfig.createTitleBlock(dialogTitle, dialogSubtitle, Feather.FILE_TEXT);
        root.add(headerPanel, BorderLayout.NORTH);

        // Body HTML inside white card
        JPanel bodyCard = ThemeConfig.createCard();
        bodyCard.setLayout(new BorderLayout());

        editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        editorPane.setFont(ThemeConfig.FONT_BODY);
        editorPane.setEditable(false);
        editorPane.setBackground(Color.WHITE);
        editorPane.setText(htmlContent);
        editorPane.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        bodyCard.add(scrollPane, BorderLayout.CENTER);
        root.add(bodyCard, BorderLayout.CENTER);

        // Footer Actions
        JPanel footer = new JPanel(new ThemeConfig.WrapFlowLayout(FlowLayout.RIGHT, 10, 4));
        footer.setOpaque(false);

        JButton btnLuuHtml = ThemeConfig.styleActionButton(
                new JButton("Lưu bản HTML"),
                Feather.DOWNLOAD,
                Color.WHITE,
                ThemeConfig.TEXT_PRIMARY,
                ThemeConfig.BORDER);

        JButton btnIn = ThemeConfig.styleActionButton(
                new JButton("In phiếu / Xuất PDF"),
                Feather.PRINTER,
                ThemeConfig.PRIMARY,
                Color.WHITE,
                ThemeConfig.PRIMARY_HOVER);

        JButton btnDong = ThemeConfig.styleActionButton(
                new JButton("Đóng"),
                Feather.X,
                Color.WHITE,
                ThemeConfig.TEXT_PRIMARY,
                ThemeConfig.BORDER);

        btnLuuHtml.addActionListener(e -> luuHtml());
        btnIn.addActionListener(e -> inPhieu());
        btnDong.addActionListener(e -> dispose());

        footer.add(btnLuuHtml);
        footer.add(btnIn);
        footer.add(btnDong);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void luuHtml() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Lưu bản biên lai HTML");
        chooser.setFileFilter(new FileNameExtensionFilter("HTML Document (*.html)", "html"));
        chooser.setSelectedFile(new File(defaultFilePrefix + "_" + LocalDateTime.now().format(FILE_TIME) + ".html"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".html")) {
            file = new File(file.getParentFile(), file.getName() + ".html");
        }

        if (file.exists()) {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "File \"" + file.getName() + "\" đã tồn tại. Ghi đè?",
                    "Xác nhận ghi đè",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            Files.writeString(file.toPath(), htmlContent, StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(
                    this,
                    "Đã lưu biên lai tại:\n" + file.getAbsolutePath(),
                    "Lưu thành công",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không thể lưu file HTML: " + ex.getMessage(),
                    "Lỗi lưu file",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void inPhieu() {
        try {
            boolean done = editorPane.print(
                    new MessageFormat(printHeader),
                    new MessageFormat("Trang {0}"),
                    true,
                    null,
                    null,
                    true);
            if (done) {
                JOptionPane.showMessageDialog(
                        this,
                        "Đã gửi lệnh in / xuất PDF thành công.",
                        "Hoàn tất",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không thể thực hiện in phiếu: " + ex.getMessage(),
                    "Lỗi máy in",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void showPhieuMuon(
            Component parent,
            String maPhieu,
            String ngayLap,
            String maThe,
            String tenDocGia,
            String tenThuThu,
            String tenSach,
            int soLuong,
            String hanTra,
            String trangThai) {
        showPhieuMuon(parent, maPhieu, ngayLap, maThe, tenDocGia, tenThuThu,
                tenSach, soLuong, hanTra, trangThai,
                0L, 0L, null, 0L, 0L, 0);
    }

    public static void showPhieuMuon(
            Component parent,
            String maPhieu,
            String ngayLap,
            String maThe,
            String tenDocGia,
            String tenThuThu,
            String tenSach,
            int soLuong,
            String hanTra,
            String trangThai,
            long donGiaThue,
            long tongTienThue,
            String maVoucher,
            long giamGiaVoucher,
            long phiSauGiam,
            int diemCong) {
        java.util.List<String[]> rows = new java.util.ArrayList<>(java.util.List.of(
                new String[] { "CCCD / mã thẻ", safe(model.DocGia.maTheRutGon(maThe)) },
                new String[] { "Họ tên độc giả", safe(tenDocGia) },
                new String[] { "Thủ thư lập phiếu", safe(tenThuThu) },
                new String[] { "Tên sách mượn", "<b>" + safe(tenSach) + "</b>" },
                new String[] { "Số lượng", soLuong + " bản" },
                new String[] { "Hạn trả dự kiến", "<b>" + safe(hanTra) + "</b>" }
        ));
        if (tongTienThue > 0 || donGiaThue > 0 || phiSauGiam > 0) {
            rows.add(new String[] { "Đơn giá thuê", String.format("%,d đ / cuốn", Math.max(0L, donGiaThue)) });
            rows.add(new String[] { "Tổng tiền thuê", String.format("%,d đ", Math.max(0L, tongTienThue)) });
            if (giamGiaVoucher > 0 || (maVoucher != null && !maVoucher.isBlank())) {
                String vcText = (maVoucher != null && !maVoucher.isBlank() ? safe(maVoucher) + " — " : "")
                        + "- " + String.format("%,d đ", Math.max(0L, giamGiaVoucher));
                rows.add(new String[] { "Giảm giá Voucher", "<span style='color:#15803D;font-weight:bold;'>" + vcText + "</span>" });
            }
            rows.add(new String[] { "Phí thuê sau giảm", "<span style='color:#1D4ED8;font-size:14px;font-weight:bold;'>"
                    + String.format("%,d đ", Math.max(0L, phiSauGiam)) + "</span>" });
        }
        if (diemCong > 0) {
            rows.add(new String[] { "Điểm tích lũy cộng", "<span style='color:#7C3AED;font-weight:bold;'>+"
                    + diemCong + " điểm</span>" });
        }
        rows.add(new String[] { "Trạng thái", "<span style='color:#1D4ED8;font-weight:bold;'>" + safe(trangThai) + "</span>" });

        String html = buildReceiptHtml(
                "PHIẾU MƯỢN SÁCH",
                "Mã phiếu: " + safe(maPhieu) + "  |  Ngày lập: " + safe(ngayLap),
                rows.toArray(new String[0][]),
                "Vui lòng bảo quản tài liệu cẩn thận và hoàn trả đúng hạn quy định. "
                        + "Mọi trường hợp trả muộn, làm hỏng hoặc mất sách sẽ bị xử lý theo nội quy thư viện.",
                "Người mượn sách",
                "Thủ thư lập phiếu");

        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        ReceiptDialog dialog = new ReceiptDialog(
                owner,
                "Xem & In Phiếu Mượn Sách",
                "Biên lai xác nhận giao dịch mượn tài liệu thư viện",
                "PHIEU MUON SACH - " + safeText(maPhieu),
                "Phieu_Muon_" + sanitizeFilePart(maPhieu),
                html);
        dialog.setVisible(true);
    }

    public static void showPhieuPhat(
            Component parent,
            String maViPham,
            String ngayLap,
            String tenDocGia,
            String maPhieuMuon,
            String tenSach,
            String loaiViPham,
            int soNgayQuaHan,
            long soTienPhat,
            String moTa,
            String trangThai,
            String ghiChuThu) {
        int macDinhTru = "TRA_MUON".equals(loaiViPham)
                ? Math.max(1, soNgayQuaHan) * dao.ViPhamDAO.DIEM_TRU_MOI_NGAY_QUA_HAN
                : dao.ViPhamDAO.DIEM_TRU_HONG_MAT;
        showPhieuPhat(parent, maViPham, ngayLap, tenDocGia, maPhieuMuon, tenSach,
                loaiViPham, soNgayQuaHan, soTienPhat, moTa, trangThai, ghiChuThu, macDinhTru, 0);
    }

    public static void showPhieuPhat(
            Component parent,
            String maViPham,
            String ngayLap,
            String tenDocGia,
            String maPhieuMuon,
            String tenSach,
            String loaiViPham,
            int soNgayQuaHan,
            long soTienBoiThuong,
            String moTa,
            String trangThai,
            String ghiChuThu,
            int diemTru,
            Integer diemKhoiPhuc) {
        boolean laTraMuon = "TRA_MUON".equals(loaiViPham);
        boolean daXuLy = "Đã xử lý".equals(trangThai);
        String formattedComp = String.format("%,d VNĐ", Math.max(0L, soTienBoiThuong));
        java.util.List<String[]> rows = new java.util.ArrayList<>(java.util.List.of(
                new String[] { "Họ tên độc giả", "<b>" + safe(tenDocGia) + "</b>" },
                new String[] { "Mã phiếu mượn", safe(maPhieuMuon) },
                new String[] { "Sách liên quan", safe(tenSach) },
                new String[] { "Loại vi phạm", safe(loaiViPham) },
                new String[] { "Số ngày quá hạn", soNgayQuaHan + " ngày" },
                new String[] { "Điểm tích lũy bị trừ", "<span style='color:#B91C1C;font-size:14px;font-weight:bold;'>-"
                        + Math.max(0, diemTru) + " điểm</span>" }
        ));
        if (laTraMuon) {
            rows.add(new String[] { "Phí phạt tiền mặt", "0 VNĐ (Áp dụng chính sách trừ điểm tích lũy)" });
        } else {
            rows.add(new String[] { daXuLy ? "Bồi thường đã ghi nhận" : "Bồi thường dự kiến", "<span style='color:#B91C1C;font-size:14px;font-weight:bold;'>"
                    + escapeHtml(formattedComp) + "</span>" });
        }
        if (diemKhoiPhuc != null && diemKhoiPhuc > 0) {
            rows.add(new String[] { "Điểm uy tín khôi phục", "<span style='color:#15803D;font-weight:bold;'>+"
                    + diemKhoiPhuc + " điểm</span>" });
        }
        if (daXuLy && diemKhoiPhuc == null) {
            rows.add(new String[] { "Điểm uy tín khôi phục", "Chưa có dữ liệu" });
        }
        rows.add(new String[] { "Chi tiết vi phạm", safe(moTa) });
        rows.add(new String[] { "Ghi chú xử lý", safe(ghiChuThu) });
        rows.add(new String[] { "Trạng thái hồ sơ", "<span style='color:#15803D;font-weight:bold;'>"
                + safe(trangThai) + "</span>" });

        String html = buildReceiptHtml(
                daXuLy && !laTraMuon ? "BIÊN LAI BỒI THƯỜNG HƯ HỎNG / MẤT SÁCH" : "BIÊN BẢN VI PHẠM",
                "Mã vi phạm: " + safe(maViPham)
                        + (daXuLy ? "  |  Ngày xử lý: " : "  |  Ngày ghi nhận: ") + safe(ngayLap),
                rows.toArray(new String[0][]),
                "Quy định xử lý vi phạm thư viện: Trả sách muộn trừ -2 điểm tích lũy/ngày (không phạt tiền mặt); "
                        + "làm hỏng hoặc mất sách trừ -30 điểm uy tín kèm bồi thường giá trị sách (50.000đ - 100.000đ).",
                "Độc giả xác nhận",
                "Thủ thư lập biên bản");

        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        ReceiptDialog dialog = new ReceiptDialog(
                owner,
                "Xem & In Biên Bản Vi Phạm",
                "Chứng từ xác nhận xử lý vi phạm thư viện",
                "BIEN BAN VI PHAM - " + safeText(maViPham),
                "Bien_Ban_Vi_Pham_" + sanitizeFilePart(maViPham),
                html);
        dialog.setVisible(true);
    }

    private static String buildReceiptHtml(
            String docTitle,
            String subInfo,
            String[][] rows,
            String noteText,
            String leftSig,
            String rightSig) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta charset='UTF-8'></head><body style='font-family:").append(ThemeConfig.FONT_FAMILY)
                .append(",sans-serif;font-size:13px;color:#0F172A;margin:14px;'>");
        sb.append("<div style='text-align:center;border-bottom:2px solid #1E293B;padding-bottom:10px;margin-bottom:14px;'>");
        sb.append("<div style='font-size:11px;font-weight:bold;color:#475569;letter-spacing:0.5px;'>")
                .append("THƯ VIỆN TỰ ĐỘNG - TRUNG TÂM HỌC LIỆU</div>");
        sb.append("<div style='font-size:18px;font-weight:bold;color:#182B49;margin-top:4px;'>")
                .append(escapeHtml(docTitle)).append("</div>");
        sb.append("<div style='font-size:11px;color:#64748B;margin-top:4px;'>")
                .append(subInfo).append("</div>");
        sb.append("</div>");

        sb.append("<table width='100%' cellpadding='7' cellspacing='0' ")
                .append("style='border-collapse:collapse;font-size:12px;border:1px solid #E2E8F0;'>");
        for (int i = 0; i < rows.length; i++) {
            String bg = (i % 2 == 0) ? "#FFFFFF" : "#F8FAFC";
            sb.append("<tr style='background-color:").append(bg).append(";'>");
            sb.append("<td width='36%' style='border-bottom:1px solid #E2E8F0;color:#475569;font-weight:bold;'>")
                    .append(escapeHtml(rows[i][0])).append("</td>");
            sb.append("<td width='64%' style='border-bottom:1px solid #E2E8F0;color:#0F172A;'>")
                    .append(rows[i][1]).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</table>");

        sb.append("<div style='margin-top:14px;padding:10px;background-color:#F8FAFC;")
                .append("border:1px solid #E2E8F0;font-size:11px;color:#475569;'>")
                .append("<b>Lưu ý:</b> ").append(escapeHtml(noteText)).append("</div>");

        sb.append("<table width='100%' cellpadding='6' cellspacing='0' style='margin-top:22px;font-size:12px;'>");
        sb.append("<tr>");
        sb.append("<td width='50%' align='center'><b>").append(escapeHtml(leftSig))
                .append("</b><br><span style='color:#94A3B8;font-size:11px;'>(Ký và ghi rõ họ tên)</span></td>");
        sb.append("<td width='50%' align='center'><b>").append(escapeHtml(rightSig))
                .append("</b><br><span style='color:#94A3B8;font-size:11px;'>(Ký và ghi rõ họ tên)</span></td>");
        sb.append("</tr>");
        sb.append("</table>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String safe(String val) {
        if (val == null || val.isBlank()) {
            return "—";
        }
        return escapeHtml(val.trim());
    }

    private static String safeText(String val) {
        return (val == null || val.isBlank()) ? "N/A" : val.trim();
    }

    private static String sanitizeFilePart(String val) {
        if (val == null || val.isBlank()) {
            return "Phieu";
        }
        return val.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
