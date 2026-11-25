package gui;

import org.kordamp.ikonli.feather.Feather;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tiện ích xuất dữ liệu JTable ra file Excel CSV chuẩn UTF-8 BOM.
 */
public final class CsvExportUtil {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");

    private CsvExportUtil() {
    }

    /**
     * Xuất các dòng đang hiển thị trên JTable (theo đúng bộ lọc/sắp xếp hiện tại)
     * ra file CSV có UTF-8 BOM để mở tiếng Việt chuẩn trên Microsoft Excel.
     */
    public static void exportTableToCsv(
            Component parent,
            JTable table,
            String defaultPrefix,
            String reportTitle) {
        if (table == null || table.getRowCount() == 0) {
            ThemeConfig.toastWarning(parent, "Không có dữ liệu hiển thị trong bảng để xuất ra file Excel (CSV).");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Xuất báo cáo " + (reportTitle == null ? "" : reportTitle) + " ra Excel (CSV)");
        chooser.setFileFilter(new FileNameExtensionFilter("Excel CSV (*.csv)", "csv"));
        String suggestedName = (defaultPrefix == null || defaultPrefix.isBlank() ? "Bao_Cao" : defaultPrefix)
                + "_" + LocalDateTime.now().format(FILE_TIME) + ".csv";
        chooser.setSelectedFile(new File(suggestedName));

        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getParentFile(), file.getName() + ".csv");
        }

        if (file.exists()) {
            int confirm = JOptionPane.showConfirmDialog(
                    parent,
                    "File \"" + file.getName() + "\" đã tồn tại.\nBạn có muốn ghi đè không?",
                    "Xác nhận ghi đè",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {

            // Ghi UTF-8 BOM (0xEF, 0xBB, 0xBF) để Excel tự nhận diện tiếng Việt UTF-8
            fos.write(0xEF);
            fos.write(0xBB);
            fos.write(0xBF);

            int colCount = table.getColumnCount();
            int rowCount = table.getRowCount();

            StringBuilder headerLine = new StringBuilder();
            for (int c = 0; c < colCount; c++) {
                if (c > 0) {
                    headerLine.append(',');
                }
                headerLine.append(escapeCsv(table.getColumnName(c)));
            }
            writer.write(headerLine.toString());
            writer.write("\r\n");

            for (int r = 0; r < rowCount; r++) {
                StringBuilder rowLine = new StringBuilder();
                for (int c = 0; c < colCount; c++) {
                    if (c > 0) {
                        rowLine.append(',');
                    }
                    Object value = table.getValueAt(r, c);
                    rowLine.append(escapeCsv(value));
                }
                writer.write(rowLine.toString());
                writer.write("\r\n");
            }
            writer.flush();

            Object[] options = { "Mở file ngay", "Đóng" };
            JLabel msgLabel = new JLabel(
                    "<html>Đã xuất thành công <b>" + rowCount + "</b> dòng dữ liệu ra file:<br>"
                            + escapeHtml(file.getAbsolutePath()) + "</html>");
            msgLabel.setIcon(ThemeConfig.getIcon(Feather.CHECK_CIRCLE, 18, ThemeConfig.SUCCESS));
            msgLabel.setIconTextGap(8);
            msgLabel.setFont(ThemeConfig.FONT_BODY);

            int choice = JOptionPane.showOptionDialog(
                    parent,
                    msgLabel,
                    "Xuất Excel (CSV) thành công",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    options[1]);

            if (choice == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().open(file);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(
                            parent,
                            "Đã lưu file nhưng không mở tự động được:\n" + ex.getMessage(),
                            "Thông báo",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    parent,
                    "Không thể ghi file CSV: " + ex.getMessage(),
                    "Lỗi xuất file",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String escapeCsv(Object val) {
        if (val == null) {
            return "";
        }
        String text = val.toString();
        if (!text.isEmpty()) {
            char first = text.charAt(0);
            boolean isNegativeNumber = first == '-' && text.matches("^-\\d+(\\.\\d+)?$");
            if ((first == '=' || first == '+' || first == '-' || first == '@') && !isNegativeNumber) {
                text = "'" + text;
            }
        }
        if (text.contains(",") || text.contains("\"") || text.contains("\r") || text.contains("\n")) {
            text = "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
