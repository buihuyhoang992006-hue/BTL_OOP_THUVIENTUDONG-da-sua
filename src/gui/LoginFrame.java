package gui;

import dao.KetNoiDatabase;
import dao.NguoiDungDAO;
import model.NguoiDung;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/**
 * Giao diện đăng nhập — thiết kế split-screen 2 cột.
 */
public class LoginFrame extends JFrame {

    private static final int W = 880;
    private static final int H = 540;

    private JTextField txtTenDangNhap;
    private JPasswordField txtMatKhau;
    private JCheckBox chkHienMatKhau;
    private JButton btnDangNhap;
    private JButton btnThoat;
    private JButton btnThemeToggle;

    private final NguoiDungDAO nguoiDungDAO;
    private boolean dangXacThuc;

    public LoginFrame() {
        ThemeConfig.ensureInitialized();
        this.nguoiDungDAO = new NguoiDungDAO();
        khoiTaoGiaoDien();
    }

    // ================================================================
    // 1. KHỞI TẠO FRAME & LAYOUT CHÍNH
    // ================================================================
    private void khoiTaoGiaoDien() {
        setTitle("Đăng nhập — Hệ thống Quản lý Thư viện Tự động");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(W, H);
        setMinimumSize(new Dimension(W, H));
        setLocationRelativeTo(null);
        setResizable(false);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                thoatChuongTrinh();
            }
        });

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.X_AXIS));

        JPanel brandPanel = buildBrandPanel();
        JPanel formPanel = buildFormPanel();

        brandPanel.setPreferredSize(new Dimension((int) (W * 0.45), H));
        brandPanel.setMaximumSize(new Dimension((int) (W * 0.45), Integer.MAX_VALUE));
        brandPanel.setMinimumSize(new Dimension((int) (W * 0.45), H));

        root.add(brandPanel);
        root.add(formPanel);
        setContentPane(root);

        txtTenDangNhap.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    txtMatKhau.requestFocus();
            }
        });
        txtMatKhau.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    xuLyDangNhap();
            }
        });

        setVisible(true);
        ThemeConfig.applyThemeToTree(this);
        txtTenDangNhap.requestFocusInWindow();
    }

    // ================================================================
    // 2. BRAND PANEL (cột trái)
    // ================================================================
    private JPanel buildBrandPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth(), h = getHeight();

                GradientPaint grad = new GradientPaint(
                        0, 0, ThemeConfig.BG_TOPBAR,
                        0, h, ThemeConfig.BG_SIDEBAR,
                        false);
                g2.setPaint(grad);
                g2.fillRect(0, 0, w, h);

                g2.setColor(new Color(0xFF, 0xFF, 0xFF, 15));
                g2.fillOval(w - 120, -60, 200, 200);
                g2.setColor(new Color(0xFF, 0xFF, 0xFF, 8));
                g2.fillOval(w - 60, h - 100, 160, 160);

                g2.dispose();
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(40, 32, 40, 32));
        panel.setOpaque(true);
        panel.setBackground(ThemeConfig.BG_TOPBAR);

        JPanel bookIcon = new BookIconPanel();
        bookIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        bookIcon.setMaximumSize(new Dimension(80, 80));
        bookIcon.setOpaque(false);

        JLabel lblSys = new JLabel("<html><div style='text-align:center'>"
                + "HỆ THỐNG QUẢN LÝ<br>THƯ VIỆN TỰ ĐỘNG</div></html>");
        lblSys.setFont(ThemeConfig.fontBold(18f));
        lblSys.setForeground(Color.WHITE);
        lblSys.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblSys.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblTagline = new JLabel("THUVIENTUDONG v2.0");
        lblTagline.setFont(ThemeConfig.FONT_SM);
        lblTagline.setForeground(ThemeConfig.TEXT_ON_DARK);
        lblTagline.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSeparator sep = new JSeparator();
        sep.setForeground(ThemeConfig.DIVIDER_DARK);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        JPanel features = buildFeaturesPanel();
        features.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(Box.createVerticalGlue());
        panel.add(bookIcon);
        panel.add(Box.createVerticalStrut(20));
        panel.add(lblSys);
        panel.add(Box.createVerticalStrut(6));
        panel.add(lblTagline);
        panel.add(Box.createVerticalStrut(24));
        panel.add(sep);
        panel.add(Box.createVerticalStrut(20));
        panel.add(features);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    /** Panel 3 tính năng nổi bật sử dụng Ikonli Feather Icons */
    private JPanel buildFeaturesPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        Object[][] items = {
                { Feather.REPEAT, "Mượn trả tự động" },
                { Feather.CLOCK, "Hàng đợi đặt trước" },
                { Feather.BELL, "Nhắc hạn thông minh" }
        };

        for (Object[] item : items) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 3));
            row.setOpaque(false);

            JLabel icon = new JLabel(ThemeConfig.getIcon((Ikon) item[0], 16, new Color(0x60A5FA)));

            JLabel text = new JLabel((String) item[1]);
            text.setFont(ThemeConfig.FONT_BODY);
            text.setForeground(new Color(0xCBD5E1));

            row.add(icon);
            row.add(text);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            p.add(row);
        }
        return p;
    }

    // ================================================================
    // 3. FORM PANEL (cột phải)
    // ================================================================
    private JPanel buildFormPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(ThemeConfig.SURFACE_CARD);
        outer.putClientProperty("ThemeConfig.refresh", (Runnable) () -> {
            outer.setBackground(ThemeConfig.snap(ThemeConfig.SURFACE_CARD));
            capNhatNutThemeDangNhap();
        });

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 14));
        topBar.setOpaque(false);
        btnThemeToggle = new JButton();
        capNhatNutThemeDangNhap();
        btnThemeToggle.setToolTipText("Chuyển đổi giao diện Sáng / Tối");
        btnThemeToggle.addActionListener(e -> {
            ThemeConfig.toggleDarkMode(this);
            capNhatNutThemeDangNhap();
        });
        topBar.add(btnThemeToggle);
        outer.add(topBar, BorderLayout.NORTH);

        JPanel formCenterWrap = new JPanel(new GridBagLayout());
        formCenterWrap.setOpaque(false);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(0, 48, 24, 48));
        form.setMaximumSize(new Dimension(420, Integer.MAX_VALUE));

        JLabel lblTitle = new JLabel("Đăng nhập hệ thống");
        lblTitle.setFont(ThemeConfig.FONT_H2);
        lblTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        lblTitle.setBorder(new EmptyBorder(0, 0, 2, 0));
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSub = new JLabel("Nhập thông tin tài khoản của bạn");
        lblSub.setFont(ThemeConfig.FONT_SM);
        lblSub.setForeground(ThemeConfig.TEXT_SECONDARY);
        lblSub.setBorder(new EmptyBorder(0, 0, 2, 0));
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblUserName = makeFormLabel("Tên đăng nhập");

        txtTenDangNhap = ThemeConfig.createTextField("Nhập tên đăng nhập...");
        txtTenDangNhap.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtTenDangNhap.setMaximumSize(new Dimension(Integer.MAX_VALUE, ThemeConfig.INPUT_HEIGHT));

        JLabel lblPass = makeFormLabel("Mật khẩu");

        // Dùng placeholder chữ mờ "Nhập mật khẩu..." thay vì "••••••••" để tránh hiểu
        // nhầm đã có sẵn mật khẩu
        txtMatKhau = ThemeConfig.createPasswordField("Nhập mật khẩu...");
        txtMatKhau.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtMatKhau.setMaximumSize(new Dimension(Integer.MAX_VALUE, ThemeConfig.INPUT_HEIGHT));
        txtMatKhau.setEchoChar('•');

        chkHienMatKhau = new JCheckBox("Hiện mật khẩu");
        chkHienMatKhau.setFont(ThemeConfig.FONT_SM);
        chkHienMatKhau.setForeground(ThemeConfig.TEXT_SECONDARY);
        chkHienMatKhau.setOpaque(false);
        chkHienMatKhau.setFocusPainted(false);
        chkHienMatKhau.setAlignmentX(Component.LEFT_ALIGNMENT);
        chkHienMatKhau.addActionListener(e -> txtMatKhau.setEchoChar(chkHienMatKhau.isSelected() ? (char) 0 : '•'));

        btnDangNhap = ThemeConfig.createPrimaryButton("Đăng nhập", Feather.LOG_IN);
        btnDangNhap.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnDangNhap.addActionListener(e -> xuLyDangNhap());

        btnThoat = ThemeConfig.createSecondaryButton("Thoát chương trình", Feather.LOG_OUT);
        btnThoat.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnThoat.addActionListener(e -> thoatChuongTrinh());

        int btnH = Math.max(ThemeConfig.BTN_HEIGHT, btnThoat.getPreferredSize().height);
        btnDangNhap.setMaximumSize(new Dimension(Integer.MAX_VALUE, btnH));
        btnThoat.setMaximumSize(new Dimension(Integer.MAX_VALUE, btnH));

        form.add(lblTitle);
        form.add(Box.createVerticalStrut(4));
        form.add(lblSub);
        form.add(Box.createVerticalStrut(26));

        form.add(lblUserName);
        form.add(Box.createVerticalStrut(8));
        form.add(txtTenDangNhap);
        form.add(Box.createVerticalStrut(16));

        form.add(lblPass);
        form.add(Box.createVerticalStrut(8));
        form.add(txtMatKhau);
        form.add(Box.createVerticalStrut(12));

        form.add(chkHienMatKhau);
        form.add(Box.createVerticalStrut(24));

        form.add(btnDangNhap);
        form.add(Box.createVerticalStrut(12));
        form.add(btnThoat);

        formCenterWrap.add(form);
        outer.add(formCenterWrap, BorderLayout.CENTER);
        return outer;
    }

    private void capNhatNutThemeDangNhap() {
        if (btnThemeToggle == null) {
            return;
        }
        boolean dark = ThemeConfig.isDarkMode();
        btnThemeToggle.setText(dark ? "Chế độ Sáng" : "Chế độ Tối");
        ThemeConfig.styleActionButton(
                btnThemeToggle,
                dark ? Feather.SUN : Feather.MOON,
                ThemeConfig.SURFACE_CARD,
                dark ? new Color(0xFBBF24) : ThemeConfig.TEXT_PRIMARY,
                ThemeConfig.BORDER);
    }

    private JLabel makeFormLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(ThemeConfig.FONT_LABEL);
        lbl.setForeground(ThemeConfig.TEXT_PRIMARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    // ================================================================
    // 4. ICON SÁCH JAVA 2D
    // ================================================================
    private static class BookIconPanel extends JPanel {
        BookIconPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(80, 72));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);

            int cx = getWidth() / 2;
            int cy = getHeight() / 2 + 2;

            Color bookLight = new Color(0xFF, 0xFF, 0xFF, 220);
            Color bookShadow = new Color(0xFF, 0xFF, 0xFF, 90);
            Color spine = new Color(0x2563EB);
            Color lineColor = new Color(0xFF, 0xFF, 0xFF, 100);

            int[] xLeft = { cx - 32, cx, cx, cx - 32 };
            int[] yLeft = { cy + 24, cy + 20, cy - 22, cy - 18 };
            g2.setColor(bookShadow);
            g2.fillPolygon(xLeft, yLeft, 4);

            int[] xRight = { cx, cx + 32, cx + 32, cx };
            int[] yRight = { cy + 20, cy + 24, cy - 18, cy - 22 };
            g2.setColor(bookLight);
            g2.fillPolygon(xRight, yRight, 4);

            g2.setColor(spine);
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(cx, cy - 22, cx, cy + 20);

            g2.setColor(new Color(0xFF, 0xFF, 0xFF, 180));
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawPolygon(xLeft, yLeft, 4);
            g2.drawPolygon(xRight, yRight, 4);

            g2.setColor(lineColor);
            g2.setStroke(new BasicStroke(1f));
            for (int i = 0; i < 3; i++) {
                int y = cy - 10 + i * 8;
                g2.drawLine(cx + 7, y, cx + 26, y);
            }

            g2.setColor(new Color(0xFF, 0xFF, 0xFF, 180));
            g2.fillOval(cx + 24, cy - 20, 5, 5);

            g2.dispose();
        }
    }

    // ================================================================
    // 5. ĐĂNG NHẬP
    // ================================================================
    private void xuLyDangNhap() {
        if (dangXacThuc) {
            return;
        }
        String tenDN = txtTenDangNhap.getText().trim();
        char[] kyTu = txtMatKhau.getPassword();
        String matKhau = new String(kyTu).trim();
        Arrays.fill(kyTu, '\0');

        if (tenDN.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên đăng nhập!",
                    "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            txtTenDangNhap.requestFocusInWindow();
            return;
        }
        if (matKhau.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập mật khẩu!",
                    "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            txtMatKhau.requestFocusInWindow();
            return;
        }

        datTrangThaiXacThuc(true);
        new SwingWorker<NguoiDungDAO.KetQuaDangNhap, Void>() {
            @Override
            protected NguoiDungDAO.KetQuaDangNhap doInBackground() throws SQLException {
                return nguoiDungDAO.xacThuc(tenDN, matKhau);
            }

            @Override
            protected void done() {
                datTrangThaiXacThuc(false);
                if (!isDisplayable()) {
                    return;
                }
                try {
                    hienThiKetQuaDangNhap(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    hienThiLoiDuLieu(e.getCause());
                } catch (RuntimeException e) {
                    hienThiLoiDuLieu(e);
                }
            }
        }.execute();
    }

    private void datTrangThaiXacThuc(boolean dangXuLy) {
        dangXacThuc = dangXuLy;
        btnDangNhap.setEnabled(!dangXuLy);
        btnDangNhap.setText(dangXuLy ? "Đang đăng nhập..." : "Đăng nhập");
        txtTenDangNhap.setEnabled(!dangXuLy);
        txtMatKhau.setEnabled(!dangXuLy);
    }

    private void hienThiKetQuaDangNhap(NguoiDungDAO.KetQuaDangNhap ketQua) {
        if (ketQua.trangThai() == NguoiDungDAO.TrangThaiDangNhap.THANH_CONG) {
            NguoiDung nd = ketQua.nguoiDung();
            txtMatKhau.setText("");
            JOptionPane.showMessageDialog(this,
                    "Xin chào " + nd.getHoTen() + "!\nVai trò: " + nd.getTenVaiTro(),
                    "Đăng nhập thành công", JOptionPane.PLAIN_MESSAGE);
            new MainFrame(nd);
            dispose();
            return;
        }

        String thongBao = ketQua.trangThai() == NguoiDungDAO.TrangThaiDangNhap.DA_KHOA
                ? "Tài khoản này đã bị khóa."
                : "Tên đăng nhập hoặc mật khẩu không chính xác.";
        JOptionPane.showMessageDialog(this, thongBao, "Lỗi", JOptionPane.ERROR_MESSAGE);
        txtMatKhau.setText("");
        txtMatKhau.requestFocusInWindow();
    }

    private void hienThiLoiDuLieu(Throwable loi) {
        Throwable nguyenNhan = loi;
        while (nguyenNhan != null) {
            if (nguyenNhan instanceof KetNoiDatabase.ThieuCauTrucException) {
                JOptionPane.showMessageDialog(this, nguyenNhan.getMessage(),
                        "Database chưa được cập nhật", JOptionPane.ERROR_MESSAGE);
                return;
            }
            nguyenNhan = nguyenNhan.getCause();
        }
        String thongBao = loi instanceof SQLException || loi.getCause() instanceof SQLException
                ? "Không kết nối hoặc không đọc được dữ liệu. Kiểm tra MySQL và file db.properties."
                : "Không thể mở ứng dụng. Hãy thử đăng nhập lại.";
        JOptionPane.showMessageDialog(this, thongBao, "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
    }

    private void thoatChuongTrinh() {
        Object[] options = { "Thoát", "Hủy" };
        int confirm = JOptionPane.showOptionDialog(
                this,
                "Bạn có chắc chắn muốn thoát chương trình?",
                "Xác nhận thoát",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null, options, options[1]);
        if (confirm == JOptionPane.YES_OPTION) {
            KetNoiDatabase.dongKetNoi();
            System.exit(0);
        }
    }

}
