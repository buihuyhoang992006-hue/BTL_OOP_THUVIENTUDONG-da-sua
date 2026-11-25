import gui.LoginFrame;
import gui.ThemeConfig;
import service.ThongBaoService;
import service.DatTruocService;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        /*
         * Khởi động một lần cho toàn bộ ứng dụng.
         * Công việc thực tế chạy trên luồng nền của service.
         */
        ThongBaoService.getInstance().batDauTuDong();
        DatTruocService.batDauTuDong();

        // ── Nạp giao diện Sáng / Tối đã lưu TRƯỚC invokeLater ──
        ThemeConfig.initTheme();

        SwingUtilities.invokeLater(() -> {
            new LoginFrame();
        });
    }
}