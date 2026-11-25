package gui;

import dao.KetNoiDatabase;
import dao.NguoiDungDAO;
import dao.ThongKeDAO;
import model.NguoiDung;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Giao diện chính của Hệ thống Quản lý Thư viện Tự động.
 */
public class MainFrame extends JFrame {

    private static final String CARD_BXH = "BXH_SACH";
    private static final String CARD_TONG_QUAN = "TONG_QUAN";
    private static final String CARD_DOANH_THU = "DOANH_THU";
    private static final String CARD_SACH = "SACH";
    private static final String CARD_MUON_TRA = "MUON_TRA";
    private static final String CARD_DAT_TRUOC = "DAT_TRUOC";
    private static final String CARD_LICH_SU = "LICH_SU";
    private static final String CARD_VI_PHAM = "VI_PHAM";
    private static final String CARD_DOC_GIA = "DOC_GIA";
    private static final String CARD_NHAC_HAN = "NHAC_HAN";
    private static final String CARD_TAI_KHOAN = "TAI_KHOAN";

    private static final int COMPACT_SIDEBAR_WIDTH = 256;
    private static final int COLLAPSED_SIDEBAR_WIDTH = 68;

    private final NguoiDung nguoiDungHienTai;
    private final ThongKeDAO thongKeDAO;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainContentPanel = new JPanel(cardLayout);
    private final List<SidebarNavItem> navItems = new ArrayList<>();

    private String currentCardKey = CARD_TONG_QUAN;
    private boolean sidebarCollapsed = false;
    private boolean dangDoiMatKhau;
    private Timer sidebarAnimTimer;
    private JPanel sidebarPanel;
    private JPanel logoPanel;
    private JPanel logoBrandWrapper;
    private JLabel lblSidebarToggleIcon;
    private JLabel lblSidebarVer;

    private JButton btnThemeToggle;
    private JLabel lblPageTitle;
    private JLabel lblPageSubtitle;

    private final JLabel lblStatTongSach = new JLabel("...", SwingConstants.LEFT);
    private final JLabel lblStatTongDocGia = new JLabel("...", SwingConstants.LEFT);
    private final JLabel lblStatDangMuon = new JLabel("...", SwingConstants.LEFT);
    private final JLabel lblStatSapDenHan = new JLabel("...", SwingConstants.LEFT);
    private final JLabel lblStatQuaHan = new JLabel("...", SwingConstants.LEFT);
    private final JLabel lblStatDatTruoc = new JLabel("...", SwingConstants.LEFT);
    private final AnalyticsChartsPanel analyticsChartsPanel = new AnalyticsChartsPanel();

    private final BangXepHangSachPanel bxhPanel = new BangXepHangSachPanel();
    private final ThongKeDoanhThuPanel doanhThuPanel;
    private final QuanLySachPanel sachPanel;
    private final MuonTraSachPanel muonTraPanel;
    private final DatTruocPanel datTruocPanel;
    private final LichSuMuonTraPanel lichSuPanel;
    private final QuanLyViPhamPanel viPhamPanel;
    private final QuanLyDocGiaPanel docGiaPanel;
    private final NhacHanPanel nhacHanPanel;
    private final QuanLyNguoiDungPanel nguoiDungPanel;

    public MainFrame(NguoiDung nd) {
        if (nd == null) {
            throw new IllegalArgumentException("Chưa có thông tin người đăng nhập.");
        }
        ThemeConfig.ensureInitialized();
        this.nguoiDungHienTai = nd;
        this.thongKeDAO = new ThongKeDAO();

        setTitle("Hệ thống Quản lý Thư viện Tự động - "
                + nd.getHoTen() + " (" + nd.getTenVaiTro() + ")");

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1320, 840);
        setMinimumSize(new Dimension(1150, 720));
        setLocationRelativeTo(null);
        setExtendedState(MAXIMIZED_BOTH);

        doanhThuPanel = new ThongKeDoanhThuPanel();
        sachPanel = new QuanLySachPanel(nd);
        muonTraPanel = new MuonTraSachPanel(nd);
        datTruocPanel = new DatTruocPanel();
        lichSuPanel = new LichSuMuonTraPanel();
        viPhamPanel = new QuanLyViPhamPanel();
        docGiaPanel = new QuanLyDocGiaPanel();
        nhacHanPanel = new NhacHanPanel();
        nguoiDungPanel = nd.isAdmin() ? new QuanLyNguoiDungPanel(nd.getId()) : null;

        khoiTaoGiaoDien();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                xuLyThoat();
            }
        });

        taiDuLieuThongKe();

        setVisible(true);
    }

    // ================================================================
    // 1. CẤU TRÚC GIAO DIỆN CHÍNH (SIDEBAR + HEADER + CARD CONTENT)
    // ================================================================
    private void khoiTaoGiaoDien() {
        setLayout(new BorderLayout());
        ToolTipManager.sharedInstance().setInitialDelay(180);

        sidebarPanel = taoSidebar();
        add(sidebarPanel, BorderLayout.WEST);

        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.setBackground(ThemeConfig.BG_PAGE);

        JPanel header = taoHeader();
        centerContainer.add(header, BorderLayout.NORTH);

        mainContentPanel.setBackground(ThemeConfig.BG_PAGE);
        mainContentPanel.add(taoTongQuan(), CARD_TONG_QUAN);
        mainContentPanel.add(doanhThuPanel, CARD_DOANH_THU);
        mainContentPanel.add(bxhPanel, CARD_BXH);
        mainContentPanel.add(sachPanel, CARD_SACH);
        mainContentPanel.add(muonTraPanel, CARD_MUON_TRA);
        mainContentPanel.add(datTruocPanel, CARD_DAT_TRUOC);
        mainContentPanel.add(lichSuPanel, CARD_LICH_SU);
        mainContentPanel.add(viPhamPanel, CARD_VI_PHAM);
        mainContentPanel.add(docGiaPanel, CARD_DOC_GIA);
        mainContentPanel.add(nhacHanPanel, CARD_NHAC_HAN);

        if (nguoiDungPanel != null) {
            mainContentPanel.add(nguoiDungPanel, CARD_TAI_KHOAN);
        }

        centerContainer.add(mainContentPanel, BorderLayout.CENTER);
        centerContainer.add(taoStatusBar(), BorderLayout.SOUTH);

        add(centerContainer, BorderLayout.CENTER);

        dangKyPhimTatToanCuc();
        chuyenManHinh(CARD_TONG_QUAN, "Tổng quan", "Bảng điều khiển và số liệu hoạt động thư viện");
        capNhatNutTheme();
        ThemeConfig.applyThemeToTree(this);
    }

    // ================================================================
    // 2. SIDEBAR ĐIỀU HƯỚNG BÊN TRÁI (COLLAPSIBLE 240px <-> 68px)
    // ================================================================
    private JPanel taoSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(COMPACT_SIDEBAR_WIDTH, 0));
        sidebar.setBackground(ThemeConfig.BG_SIDEBAR);

        logoPanel = new JPanel(new BorderLayout(8, 0));
        logoPanel.setBackground(ThemeConfig.BG_TOPBAR);
        logoPanel.setPreferredSize(new Dimension(COMPACT_SIDEBAR_WIDTH, 76));
        logoPanel.setBorder(new EmptyBorder(16, 14, 16, 12));

        JPanel logoBadge = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeConfig.PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        logoBadge.setOpaque(false);
        logoBadge.setPreferredSize(new Dimension(36, 36));
        logoBadge.add(new JLabel(ThemeConfig.getIcon(Feather.BOOK_OPEN, 20, Color.WHITE)));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel lblLogoTitle = new JLabel("THƯ VIỆN TĐH");
        lblLogoTitle.setFont(ThemeConfig.fontBold(15f));
        lblLogoTitle.setForeground(Color.WHITE);

        JLabel lblLogoSub = new JLabel("HỆ THỐNG TỰ ĐỘNG");
        lblLogoSub.setFont(ThemeConfig.fontBold(11f));
        lblLogoSub.setForeground(new Color(0x94A3B8));

        titleBlock.add(Box.createVerticalGlue());
        titleBlock.add(lblLogoTitle);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(lblLogoSub);
        titleBlock.add(Box.createVerticalGlue());

        logoBrandWrapper = new JPanel(new BorderLayout(10, 0));
        logoBrandWrapper.setOpaque(false);
        logoBrandWrapper.add(logoBadge, BorderLayout.WEST);
        logoBrandWrapper.add(titleBlock, BorderLayout.CENTER);

        JPanel sidebarToggleTile = new JPanel(new GridBagLayout()) {
            private boolean hover;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
                    @Override public void mouseClicked(MouseEvent e) { toggleSidebar(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hover ? new Color(0x334155) : new Color(0x1E293B));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        sidebarToggleTile.setOpaque(false);
        sidebarToggleTile.setPreferredSize(new Dimension(30, 30));
        sidebarToggleTile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sidebarToggleTile.setToolTipText("Thu gọn / Mở rộng thanh điều hướng (Ctrl+B)");
        lblSidebarToggleIcon = new JLabel(ThemeConfig.getIcon(Feather.CHEVRON_LEFT, 16, ThemeConfig.TEXT_ON_DARK));
        sidebarToggleTile.add(lblSidebarToggleIcon);

        JPanel toggleWrap = new JPanel(new GridBagLayout());
        toggleWrap.setOpaque(false);
        toggleWrap.add(sidebarToggleTile);

        logoPanel.add(logoBrandWrapper, BorderLayout.CENTER);
        logoPanel.add(toggleWrap, BorderLayout.EAST);

        JPanel menuContainer = new JPanel();
        menuContainer.setLayout(new BoxLayout(menuContainer, BoxLayout.Y_AXIS));
        menuContainer.setBackground(ThemeConfig.BG_SIDEBAR);
        menuContainer.setBorder(new EmptyBorder(10, 0, 10, 0));

        themNavMoi(menuContainer, Feather.GRID, "Tổng quan", CARD_TONG_QUAN,
                "Bảng điều khiển và số liệu hoạt động thư viện", "Ctrl+1");
        themNavMoi(menuContainer, Feather.DOLLAR_SIGN, "Thống kê phí thư viện", CARD_DOANH_THU,
                "Phí thuê sau voucher và tiền bồi thường đã ghi nhận", "Ctrl+0");
        themNavMoi(menuContainer, Feather.AWARD, "BXH sách", CARD_BXH,
                "Xếp hạng sách theo số lượt mượn", "");
        themNavMoi(menuContainer, Feather.BOOK, "Quản lý sách", CARD_SACH,
                "Tra cứu danh mục và quản lý thông tin kho sách", "Ctrl+2");
        themNavMoi(menuContainer, Feather.REPEAT, "Mượn & Trả sách", CARD_MUON_TRA,
                "Tạo phiếu mượn và xử lý nhận trả sách tự động", "Ctrl+3");
        themNavMoi(menuContainer, Feather.CLOCK, "Đặt trước sách", CARD_DAT_TRUOC,
                "Hàng đợi giữ chỗ sách và thông báo nhận sách", "Ctrl+4");
        themNavMoi(menuContainer, Feather.ROTATE_CCW, "Lịch sử mượn trả", CARD_LICH_SU,
                "Theo dõi chi tiết hạn trả và tiến độ mượn sách", "Ctrl+5");
        themNavMoi(menuContainer, Feather.ALERT_TRIANGLE, "Quản lý vi phạm", CARD_VI_PHAM,
                "Xem và xử lý các trường hợp quá hạn, hư hao sách", "Ctrl+6");
        themNavMoi(menuContainer, Feather.USERS, "Quản lý độc giả", CARD_DOC_GIA,
                "Hồ sơ thẻ thư viện, điểm thưởng và lịch sử vi phạm", "Ctrl+7");
        themNavMoi(menuContainer, Feather.MAIL, "Nhắc hạn email", CARD_NHAC_HAN,
                "Hệ thống gửi email thông báo và nhắc hạn tự động", "Ctrl+8");

        if (nguoiDungHienTai.isAdmin()) {
            JSeparator sep = new JSeparator();
            sep.setForeground(ThemeConfig.DIVIDER_DARK);
            sep.setBackground(ThemeConfig.DIVIDER_DARK);
            sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            menuContainer.add(Box.createVerticalStrut(6));
            menuContainer.add(sep);
            menuContainer.add(Box.createVerticalStrut(6));

            themNavMoi(menuContainer, Feather.SHIELD, "Quản lý tài khoản", CARD_TAI_KHOAN,
                    "Phân quyền và quản lý tài khoản người dùng hệ thống", "Ctrl+9");
        }

        JScrollPane scrollNav = new JScrollPane(menuContainer);
        scrollNav.setBorder(null);
        scrollNav.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollNav.getVerticalScrollBar().setPreferredSize(new Dimension(5, 0));
        scrollNav.getViewport().setBackground(ThemeConfig.BG_SIDEBAR);

        JPanel footerPanel = new JPanel(new BorderLayout(8, 0));
        footerPanel.setBackground(new Color(0x182538));
        footerPanel.setPreferredSize(new Dimension(COMPACT_SIDEBAR_WIDTH, 46));
        footerPanel.setBorder(new EmptyBorder(8, 14, 8, 14));

        lblSidebarVer = new JLabel("Phiên bản 2.0 • SaaS Pro Max");
        lblSidebarVer.setFont(ThemeConfig.FONT_CAP);
        lblSidebarVer.setForeground(new Color(0x94A3B8));
        footerPanel.add(lblSidebarVer, BorderLayout.WEST);

        sidebar.add(logoPanel, BorderLayout.NORTH);
        sidebar.add(scrollNav, BorderLayout.CENTER);
        sidebar.add(footerPanel, BorderLayout.SOUTH);

        return sidebar;
    }

    private void themNavMoi(JPanel container, Ikon iconCode, String title, String cardKey, String subtitle, String shortcut) {
        SidebarNavItem item = new SidebarNavItem(iconCode, title, cardKey, shortcut, () -> {
            chuyenManHinh(cardKey, title, subtitle);
        });
        navItems.add(item);
        container.add(item);
        container.add(Box.createVerticalStrut(2));
    }

    private void toggleSidebar() {
        sidebarCollapsed = !sidebarCollapsed;
        final int targetWidth = sidebarCollapsed ? COLLAPSED_SIDEBAR_WIDTH : COMPACT_SIDEBAR_WIDTH;
        final int startWidth = sidebarPanel.getWidth() > 0 ? sidebarPanel.getWidth()
                : (sidebarCollapsed ? COMPACT_SIDEBAR_WIDTH : COLLAPSED_SIDEBAR_WIDTH);

        if (sidebarCollapsed) {
            logoBrandWrapper.setVisible(false);
            logoPanel.setBorder(new EmptyBorder(16, 10, 16, 10));
            lblSidebarToggleIcon.setIcon(ThemeConfig.getIcon(Feather.MENU, 18, ThemeConfig.TEXT_ON_DARK));
            lblSidebarVer.setVisible(false);
        } else {
            logoBrandWrapper.setVisible(true);
            logoPanel.setBorder(new EmptyBorder(16, 14, 16, 12));
            lblSidebarToggleIcon.setIcon(ThemeConfig.getIcon(Feather.CHEVRON_LEFT, 16, ThemeConfig.TEXT_ON_DARK));
            lblSidebarVer.setVisible(true);
        }

        for (SidebarNavItem item : navItems) {
            item.setCollapsed(sidebarCollapsed);
        }

        if (sidebarAnimTimer != null && sidebarAnimTimer.isRunning()) {
            sidebarAnimTimer.stop();
        }
        final int steps = 7;
        final int[] tick = { 0 };
        sidebarAnimTimer = new Timer(14, e -> {
            tick[0]++;
            double t = Math.min(1.0, (double) tick[0] / steps);
            double eased = 1.0 - Math.pow(1.0 - t, 3);
            int curW = (int) Math.round(startWidth + (targetWidth - startWidth) * eased);
            sidebarPanel.setPreferredSize(new Dimension(curW, 0));
            logoPanel.setPreferredSize(new Dimension(curW, 76));
            sidebarPanel.revalidate();
            getContentPane().revalidate();
            if (tick[0] >= steps) {
                sidebarAnimTimer.stop();
            }
        });
        sidebarAnimTimer.start();
    }

    // ================================================================
    // 3. HEADER THANH ĐẦU TRANG (76px - Borderless SaaS Top Chrome)
    // ================================================================
    private JPanel taoHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(ThemeConfig.HEADER_BG);
        header.setPreferredSize(new Dimension(0, 82));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeConfig.BORDER),
                new EmptyBorder(10, 28, 10, 28)));
        header.putClientProperty("ThemeConfig.refresh", (Runnable) () -> {
            header.setBackground(ThemeConfig.snap(ThemeConfig.HEADER_BG));
            header.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeConfig.BORDER),
                    new EmptyBorder(10, 28, 10, 28)));
        });

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        lblPageTitle = new JLabel("Tổng quan");
        lblPageTitle.setFont(ThemeConfig.FONT_H1);
        lblPageTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        lblPageTitle.setBorder(new EmptyBorder(0, 0, 2, 0));
        lblPageTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblPageSubtitle = new JLabel("Bảng điều khiển và số liệu hoạt động thư viện");
        lblPageSubtitle.setFont(ThemeConfig.FONT_SM);
        lblPageSubtitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        lblPageSubtitle.setBorder(new EmptyBorder(0, 0, 2, 0));
        lblPageSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        titlePanel.add(Box.createVerticalGlue());
        titlePanel.add(lblPageTitle);
        titlePanel.add(Box.createVerticalStrut(3));
        titlePanel.add(lblPageSubtitle);
        titlePanel.add(Box.createVerticalGlue());

        header.add(titlePanel, BorderLayout.WEST);

        JPanel rightWrapper = new JPanel(new GridBagLayout());
        rightWrapper.setOpaque(false);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightActions.setOpaque(false);

        JPanel userBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        userBox.setOpaque(false);

        JLabel lblUserName = new JLabel(nguoiDungHienTai.getHoTen());
        lblUserName.setFont(ThemeConfig.FONT_BTN);
        lblUserName.setForeground(ThemeConfig.TEXT_PRIMARY);

        JLabel lblRoleBadge = taoRoleBadge(nguoiDungHienTai.getTenVaiTro(), nguoiDungHienTai.isAdmin());

        userBox.add(lblUserName);
        userBox.add(lblRoleBadge);
        rightActions.add(userBox);

        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 28));
        sep.setForeground(ThemeConfig.BORDER);
        rightActions.add(sep);

        btnThemeToggle = new JButton();
        capNhatNutTheme();
        btnThemeToggle.setToolTipText("Chuyển chế độ Sáng / Tối (Ctrl+Shift+D)");
        btnThemeToggle.addActionListener(e -> toggleCheDoMau());
        rightActions.add(btnThemeToggle);

        JButton btnTroGiup = ThemeConfig.styleActionButton(
                new JButton("Phím tắt"),
                Feather.HELP_CIRCLE,
                ThemeConfig.SURFACE_CARD,
                ThemeConfig.TEXT_PRIMARY,
                ThemeConfig.BORDER);
        btnTroGiup.setToolTipText("Xem danh sách phím tắt & thông tin phần mềm");
        btnTroGiup.addActionListener(e -> hienThiTroGiupVaPhimTat());
        rightActions.add(btnTroGiup);

        JButton btnDoiMK = ThemeConfig.styleActionButton(
                new JButton("Đổi mật khẩu"),
                Feather.KEY,
                ThemeConfig.SURFACE_CARD,
                ThemeConfig.TEXT_PRIMARY,
                ThemeConfig.BORDER);
        btnDoiMK.addActionListener(e -> doiMatKhau());
        rightActions.add(btnDoiMK);

        JButton btnLogout = ThemeConfig.styleActionButton(
                new JButton("Đăng xuất"),
                Feather.LOG_OUT,
                ThemeConfig.DANGER_SOFT_BG,
                ThemeConfig.DANGER,
                ThemeConfig.DANGER_SOFT_BORDER);
        btnLogout.addActionListener(e -> dangXuat());
        rightActions.add(btnLogout);

        rightWrapper.add(rightActions);
        header.add(rightWrapper, BorderLayout.EAST);
        return header;
    }

    private void capNhatNutTheme() {
        if (btnThemeToggle == null) {
            return;
        }
        boolean dark = ThemeConfig.isDarkMode();
        btnThemeToggle.setText(dark ? "Sáng" : "Tối");
        ThemeConfig.styleActionButton(
                btnThemeToggle,
                dark ? Feather.SUN : Feather.MOON,
                ThemeConfig.SURFACE_CARD,
                dark ? new Color(0xFBBF24) : ThemeConfig.TEXT_PRIMARY,
                ThemeConfig.BORDER);
    }

    private void toggleCheDoMau() {
        ThemeConfig.toggleDarkMode(this);
        capNhatNutTheme();
        ThemeConfig.toastInfo(
                this,
                ThemeConfig.isDarkMode()
                        ? "Đã chuyển sang giao diện Tối (Carbon Slate Theme)."
                        : "Đã chuyển sang giao diện Sáng (Modern Light Theme).");
    }

    private void hienThiTroGiupVaPhimTat() {
        JOptionPane.showMessageDialog(
                this,
                "PHÍM TẮT ĐIỀU HƯỚNG NHANH\n"
                        + "  • Ctrl + 1 .. 9 : Chuyển nhanh giữa các phân hệ chức năng\n"
                        + "  • Ctrl + 0      : Mở nhanh Thống kê phí thư viện\n"
                        + "  • Ctrl + F      : Đưa con trỏ vào ô tìm kiếm của màn hình hiện tại\n"
                        + "  • F5            : Làm mới dữ liệu màn hình đang mở\n"
                        + "  • Ctrl + B      : Thu gọn / mở rộng thanh điều hướng Sidebar\n"
                        + "  • Ctrl+Shift+D  : Chuyển đổi Light Mode / Carbon Slate Dark Mode\n\n"
                        + "HỆ THỐNG QUẢN LÝ THƯ VIỆN TỰ ĐỘNG (THUVIENTUDONG v2.0)\n"
                        + "Công nghệ: Java Swing + FlatLaf (Light/Dark) + Java 2D + Ikonli Feather + MySQL",
                "Phím tắt & Thông tin hệ thống",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private JLabel taoRoleBadge(String tenVaiTro, boolean isAdmin) {
        JLabel badge = new JLabel(tenVaiTro.toUpperCase());
        badge.setFont(ThemeConfig.FONT_BADGE);
        badge.setOpaque(true);
        badge.setBorder(new EmptyBorder(3, 8, 3, 8));

        if (isAdmin) {
            badge.setBackground(ThemeConfig.STATUS_PENDING_BG);
            badge.setForeground(ThemeConfig.STATUS_PENDING_FG);
        } else {
            badge.setBackground(ThemeConfig.STATUS_OK_BG);
            badge.setForeground(ThemeConfig.STATUS_OK_FG);
        }
        return badge;
    }

    // ================================================================
    // 4. MÀN HÌNH "TỔNG QUAN" — CO GIÃN VỪA KHÍT 100% MÀN HÌNH
    // ================================================================
    private JComponent taoTongQuan() {
        // Container triển khai ThemeConfig.ScrollablePanel để khóa chiều rộng vừa khít viewport, không tràn ngang
        ThemeConfig.ScrollablePanel container = new ThemeConfig.ScrollablePanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(ThemeConfig.BG_PAGE);
        container.setBorder(new EmptyBorder(24, 28, 24, 28));

        // ── Tiêu đề Section 1 ─────────────────────────────────────
        JPanel statHeader = new JPanel(new BorderLayout());
        statHeader.setOpaque(false);
        statHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSec1 = new JLabel("CHỈ SỐ HOẠT ĐỘNG THỜI GIAN THỰC");
        lblSec1.setIcon(ThemeConfig.getIcon(Feather.BAR_CHART_2, 18, ThemeConfig.PRIMARY));
        lblSec1.setIconTextGap(8);
        lblSec1.setFont(ThemeConfig.FONT_H3);
        lblSec1.setForeground(ThemeConfig.TEXT_SECONDARY);
        lblSec1.setBorder(new EmptyBorder(0, 0, 2, 0));

        JButton btnReload = ThemeConfig.styleActionButton(
                new JButton("Làm mới dữ liệu"),
                Feather.REFRESH_CW,
                ThemeConfig.SURFACE_CARD,
                ThemeConfig.PRIMARY,
                ThemeConfig.BORDER);
        btnReload.addActionListener(e -> {
            taiDuLieuThongKe();
            ThemeConfig.toastSuccess(this, "Đã cập nhật số liệu Tổng quan.");
        });

        statHeader.add(lblSec1, BorderLayout.WEST);
        statHeader.add(btnReload, BorderLayout.EAST);
        statHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, statHeader.getPreferredSize().height + 4));
        container.add(statHeader);
        container.add(Box.createVerticalStrut(14));

        // ── 6 Thẻ Thống Kê (2 hàng x 3 cột) ───────────────────────
        JPanel statGrid = new JPanel(new GridLayout(2, 3, 20, 20));
        statGrid.setOpaque(false);
        statGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        statGrid.add(taoStatCard("Tổng bản sách", lblStatTongSach, Feather.BOOK,
                ThemeConfig.PRIMARY, new Color(0xDBEAFE), "Xem kho sách đang hoạt động",
                () -> chuyenManHinh(CARD_SACH, "Quản lý sách",
                        "Tra cứu danh mục và quản lý thông tin kho sách",
                        () -> sachPanel.moVaLocTrangThai("Hoạt động"))));
        statGrid.add(taoStatCard("Độc giả hoạt động", lblStatTongDocGia, Feather.USERS,
                ThemeConfig.SUCCESS, new Color(0xDCFCE7), "Xem hồ sơ bạn đọc hoạt động",
                () -> chuyenManHinh(CARD_DOC_GIA, "Quản lý độc giả",
                        "Hồ sơ thẻ thư viện, điểm thưởng và lịch sử vi phạm",
                        () -> docGiaPanel.moVaLocTrangThai("Hoạt động"))));
        statGrid.add(taoStatCard("Bản sách đang mượn", lblStatDangMuon, Feather.REPEAT,
                new Color(0x6366F1), new Color(0xEEF2FF), "Xem toàn bộ bản sách đang mượn",
                () -> moMuonTraVaLoc("Tất cả đang mượn")));
        statGrid.add(taoStatCard("Sắp đến hạn (3 ngày)", lblStatSapDenHan, Feather.CLOCK,
                ThemeConfig.STATUS_WARN_FG, new Color(0xFEF3C7), "Mở danh sách sắp đến hạn & nhắc email",
                () -> moNhacHan(false)));
        statGrid.add(taoStatCard("Bản sách quá hạn", lblStatQuaHan, Feather.ALERT_TRIANGLE,
                ThemeConfig.DANGER, new Color(0xFEE2E2), "Mở Quản lý vi phạm để đối soát & thu phạt",
                () -> moViPhamTheoTuKhoa("", "Chưa xử lý")));
        statGrid.add(taoStatCard("Đặt trước đang chờ", lblStatDatTruoc, Feather.CLOCK,
                new Color(0x8B5CF6), new Color(0xF3E8FF), "Xem hàng đợi đặt trước & giữ sách",
                () -> moDatTruocHangDoi("")));

        statGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, statGrid.getPreferredSize().height));
        container.add(statGrid);
        container.add(Box.createVerticalStrut(26));

        // ── Tiêu đề Section: Phân tích trực quan ─────────────────
        JPanel chartHeader = new JPanel(new BorderLayout());
        chartHeader.setOpaque(false);
        chartHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSecCharts = new JLabel("PHÂN TÍCH TRỰC QUAN HOẠT ĐỘNG");
        lblSecCharts.setIcon(ThemeConfig.getIcon(Feather.PIE_CHART, 18, ThemeConfig.PRIMARY));
        lblSecCharts.setIconTextGap(8);
        lblSecCharts.setFont(ThemeConfig.FONT_H3);
        lblSecCharts.setForeground(ThemeConfig.TEXT_SECONDARY);
        lblSecCharts.setBorder(new EmptyBorder(0, 0, 2, 0));
        chartHeader.add(lblSecCharts, BorderLayout.WEST);
        chartHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, chartHeader.getPreferredSize().height + 4));

        container.add(chartHeader);
        container.add(Box.createVerticalStrut(14));
        container.add(analyticsChartsPanel);
        container.add(Box.createVerticalStrut(26));

        // ── Tiêu đề Section 2 ─────────────────────────────────────
        JPanel navHeader = new JPanel(new BorderLayout());
        navHeader.setOpaque(false);
        navHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSec2 = new JLabel("TRUY CẬP NHANH CÁC CHỨC NĂNG");
        lblSec2.setIcon(ThemeConfig.getIcon(Feather.GRID, 18, ThemeConfig.PRIMARY));
        lblSec2.setIconTextGap(8);
        lblSec2.setFont(ThemeConfig.FONT_H3);
        lblSec2.setForeground(ThemeConfig.TEXT_SECONDARY);
        lblSec2.setBorder(new EmptyBorder(0, 0, 2, 0));
        navHeader.add(lblSec2, BorderLayout.WEST);
        navHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, navHeader.getPreferredSize().height + 4));

        container.add(navHeader);
        container.add(Box.createVerticalStrut(14));

        // ── Các Card Điều Hướng Nhanh (4 cột tự co giãn vừa màn hình) ──
        JPanel navGrid = new JPanel(new GridLayout(0, 4, 20, 20));
        navGrid.setOpaque(false);
        navGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        navGrid.add(taoQuickNavCard("Thống kê phí thư viện",
                "Phí thuê sách, giảm giá Voucher và tiền phạt theo kỳ",
                Feather.DOLLAR_SIGN, new Color(0x059669), new Color(0xD1FAE5),
                () -> chuyenManHinh(CARD_DOANH_THU, "Thống kê phí thư viện",
                        "Phí thuê sau voucher và tiền bồi thường đã ghi nhận")));

        navGrid.add(taoQuickNavCard("Quản lý sách",
                "Tra cứu danh mục, thêm mới và quản lý kho sách",
                Feather.BOOK, new Color(0x2563EB), new Color(0xDBEAFE),
                () -> chuyenManHinh(CARD_SACH, "Quản lý sách", "Tra cứu danh mục và quản lý thông tin kho sách")));

        navGrid.add(taoQuickNavCard("Mượn & Trả sách",
                "Lập phiếu mượn mới và xử lý nhận trả theo dòng",
                Feather.REPEAT, new Color(0x16A34A), new Color(0xDCFCE7),
                () -> chuyenManHinh(CARD_MUON_TRA, "Mượn & Trả sách",
                        "Tạo phiếu mượn và xử lý nhận trả sách tự động")));

        navGrid.add(taoQuickNavCard("Đặt trước sách",
                "Hàng đợi giữ chỗ sách và thông báo nhận sách",
                Feather.CLOCK, new Color(0x8B5CF6), new Color(0xF3E8FF),
                () -> chuyenManHinh(CARD_DAT_TRUOC, "Đặt trước sách", "Hàng đợi giữ chỗ sách và thông báo nhận sách")));

        navGrid.add(taoQuickNavCard("Lịch sử mượn trả",
                "Theo dõi chi tiết hạn trả, trạng thái từng độc giả",
                Feather.ROTATE_CCW, new Color(0x0F766E), new Color(0xCCFBF1),
                () -> chuyenManHinh(CARD_LICH_SU, "Lịch sử mượn trả",
                        "Theo dõi chi tiết hạn trả và tiến độ mượn sách")));

        navGrid.add(taoQuickNavCard("Quản lý vi phạm",
                "Ghi nhận quá hạn, hư hao sách và thu phí phạt",
                Feather.ALERT_TRIANGLE, new Color(0xD97706), new Color(0xFEF3C7),
                () -> chuyenManHinh(CARD_VI_PHAM, "Quản lý vi phạm",
                        "Xem và xử lý các trường hợp quá hạn, hư hao sách")));

        navGrid.add(taoQuickNavCard("Quản lý độc giả",
                "Hồ sơ thẻ bạn đọc, tích điểm và nhóm hành vi",
                Feather.USERS, new Color(0x4F46E5), new Color(0xEEF2FF),
                () -> chuyenManHinh(CARD_DOC_GIA, "Quản lý độc giả",
                        "Hồ sơ thẻ thư viện, điểm thưởng và lịch sử vi phạm")));

        navGrid.add(taoQuickNavCard("Nhắc hạn email",
                "Gửi thông báo tự động và xem lịch sử email nhắc",
                Feather.MAIL, new Color(0x9333EA), new Color(0xF3E8FF),
                () -> chuyenManHinh(CARD_NHAC_HAN, "Nhắc hạn email",
                        "Hệ thống gửi email thông báo và nhắc hạn tự động")));

        if (nguoiDungHienTai.isAdmin()) {
            navGrid.add(taoQuickNavCard("Quản lý tài khoản",
                    "Phân quyền, cấp tài khoản và khóa người dùng",
                    Feather.SHIELD, new Color(0x334155), new Color(0xF1F5F9),
                    () -> chuyenManHinh(CARD_TAI_KHOAN, "Quản lý tài khoản",
                            "Phân quyền và quản lý tài khoản người dùng hệ thống")));
        }

        navGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, navGrid.getPreferredSize().height));
        container.add(navGrid);
        container.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(container);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(ThemeConfig.BG_PAGE);
        return scrollPane;
    }

    private JPanel taoStatCard(String title, JLabel valLabel, Ikon iconCode, Color accentColor, Color pastelBg,
            String subNote, Runnable onClick) {
        ThemeConfig.ModernCardPanel card = new ThemeConfig.ModernCardPanel(
                new BorderLayout(12, 8), new Insets(20, 24, 20, 24));
        if (onClick != null) {
            card.setToolTipText("Nhấn để mở: " + subNote);
        }

        JPanel topRow = new JPanel(new BorderLayout(8, 0));
        topRow.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(ThemeConfig.FONT_LABEL);
        lblTitle.setForeground(ThemeConfig.TEXT_SECONDARY);

        JPanel iconBadge = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(pastelBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconBadge.setOpaque(false);
        iconBadge.setPreferredSize(new Dimension(34, 34));
        iconBadge.add(new JLabel(ThemeConfig.getIcon(iconCode, 19, accentColor)));

        topRow.add(lblTitle, BorderLayout.WEST);
        topRow.add(iconBadge, BorderLayout.EAST);

        valLabel.setFont(ThemeConfig.fontBold(32f));
        valLabel.setForeground(accentColor);
        valLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel bottomRow = new JPanel(new BorderLayout(8, 0));
        bottomRow.setOpaque(false);
        bottomRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblNote = new JLabel(subNote);
        lblNote.setFont(ThemeConfig.FONT_SM);
        lblNote.setForeground(ThemeConfig.TEXT_MUTED);
        lblNote.setBorder(new EmptyBorder(0, 0, 2, 0));
        bottomRow.add(lblNote, BorderLayout.CENTER);

        if (onClick != null) {
            JLabel lblArrow = new JLabel(ThemeConfig.getIcon(Feather.ARROW_RIGHT, 14, accentColor));
            bottomRow.add(lblArrow, BorderLayout.EAST);
        }

        JPanel centerBlock = new JPanel();
        centerBlock.setLayout(new BoxLayout(centerBlock, BoxLayout.Y_AXIS));
        centerBlock.setOpaque(false);
        centerBlock.add(valLabel);
        centerBlock.add(Box.createVerticalStrut(4));
        centerBlock.add(bottomRow);

        card.add(topRow, BorderLayout.NORTH);
        card.add(centerBlock, BorderLayout.CENTER);

        if (onClick != null) {
            card.enableInteractive(accentColor, onClick);
        }

        return card;
    }

    private JPanel taoQuickNavCard(String title, String desc, Ikon iconCode, Color tagColor, Color pastelBg,
            Runnable action) {
        ThemeConfig.ModernCardPanel card = new ThemeConfig.ModernCardPanel(
                new BorderLayout(12, 10), new Insets(20, 24, 20, 20));
        card.setLeftAccentColor(tagColor);

        JPanel iconBadge = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(pastelBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconBadge.setOpaque(false);
        iconBadge.setPreferredSize(new Dimension(36, 36));
        iconBadge.add(new JLabel(ThemeConfig.getIcon(iconCode, 19, tagColor)));

        JPanel iconBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        iconBox.setOpaque(false);
        iconBox.add(iconBadge);

        JPanel textBlock = new JPanel(new BorderLayout(0, 4));
        textBlock.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(ThemeConfig.fontBold(15f));
        lblTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        lblTitle.setBorder(new EmptyBorder(0, 0, 2, 0));

        JLabel lblDesc = new JLabel("<html>" + desc + "</html>");
        lblDesc.setFont(ThemeConfig.FONT_SM);
        lblDesc.setForeground(ThemeConfig.TEXT_SECONDARY);

        textBlock.add(lblTitle, BorderLayout.NORTH);
        textBlock.add(lblDesc, BorderLayout.CENTER);

        card.add(iconBox, BorderLayout.NORTH);
        card.add(textBlock, BorderLayout.CENTER);
        card.enableInteractive(tagColor, action);

        return card;
    }

    // ================================================================
    // 5. CHUYỂN MÀN HÌNH & PHÍM TẮT TOÀN CỤC
    // ================================================================
    private void chuyenManHinh(String cardKey, String pageTitle, String pageSubtitle) {
        chuyenManHinh(cardKey, pageTitle, pageSubtitle, null);
    }

    private void chuyenManHinh(String cardKey, String pageTitle, String pageSubtitle, Runnable customLoadAction) {
        currentCardKey = cardKey;
        cardLayout.show(mainContentPanel, cardKey);

        if (lblPageTitle != null)
            lblPageTitle.setText(pageTitle);
        if (lblPageSubtitle != null)
            lblPageSubtitle.setText(pageSubtitle);

        for (SidebarNavItem item : navItems) {
            item.setActive(item.getCardKey().equals(cardKey));
        }

        if (customLoadAction != null) {
            customLoadAction.run();
            return;
        }

        switch (cardKey) {
            case CARD_TONG_QUAN -> taiDuLieuThongKe();
            case CARD_DOANH_THU -> doanhThuPanel.taiDuLieu();
            case CARD_BXH       -> bxhPanel.taiDuLieu();
            case CARD_SACH      -> sachPanel.taiDuLieuGiuLuaChon();
            case CARD_MUON_TRA  -> muonTraPanel.lamMoi();
            case CARD_DAT_TRUOC -> datTruocPanel.taiDuLieu();
            case CARD_LICH_SU   -> lichSuPanel.taiDuLieu();
            case CARD_VI_PHAM   -> viPhamPanel.taiDuLieu();
            case CARD_DOC_GIA   -> docGiaPanel.taiDuLieuGiuLuaChon();
            case CARD_NHAC_HAN  -> nhacHanPanel.taiDuLieu();
            case CARD_TAI_KHOAN -> {
                if (nguoiDungPanel != null) {
                    nguoiDungPanel.taiDuLieu();
                }
            }
            default -> {
            }
        }
    }

    private void dangKyPhimTatToanCuc() {
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        record TabShortcut(int num, String cardKey, String title, String subtitle, boolean adminOnly) {}
        TabShortcut[] shortcuts = {
                new TabShortcut(0, CARD_DOANH_THU, "Thống kê phí thư viện", "Phí thuê sau voucher và tiền bồi thường đã ghi nhận", false),
                new TabShortcut(1, CARD_TONG_QUAN, "Tổng quan", "Bảng điều khiển và số liệu hoạt động thư viện", false),
                new TabShortcut(2, CARD_SACH, "Quản lý sách", "Tra cứu danh mục và quản lý thông tin kho sách", false),
                new TabShortcut(3, CARD_MUON_TRA, "Mượn & Trả sách", "Tạo phiếu mượn và xử lý nhận trả sách tự động", false),
                new TabShortcut(4, CARD_DAT_TRUOC, "Đặt trước sách", "Hàng đợi giữ chỗ sách và thông báo nhận sách", false),
                new TabShortcut(5, CARD_LICH_SU, "Lịch sử mượn trả", "Theo dõi chi tiết hạn trả và tiến độ mượn sách", false),
                new TabShortcut(6, CARD_VI_PHAM, "Quản lý vi phạm", "Xem và xử lý các trường hợp quá hạn, hư hao sách", false),
                new TabShortcut(7, CARD_DOC_GIA, "Quản lý độc giả", "Hồ sơ thẻ thư viện, điểm thưởng và lịch sử vi phạm", false),
                new TabShortcut(8, CARD_NHAC_HAN, "Nhắc hạn email", "Hệ thống gửi email thông báo và nhắc hạn tự động", false),
                new TabShortcut(9, CARD_TAI_KHOAN, "Quản lý tài khoản", "Phân quyền và quản lý tài khoản người dùng hệ thống", true)
        };

        int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        for (TabShortcut s : shortcuts) {
            String actionKey = "nav.tab." + s.num();
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_0 + s.num(), shortcutMask), actionKey);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0 + s.num(), shortcutMask), actionKey);
            am.put(actionKey, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (s.adminOnly() && !nguoiDungHienTai.isAdmin()) {
                        ThemeConfig.toastWarning(MainFrame.this, "Chức năng Quản lý tài khoản chỉ dành cho Quản trị viên.");
                        return;
                    }
                    chuyenManHinh(s.cardKey(), s.title(), s.subtitle());
                }
            });
        }

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, shortcutMask), "global.focusSearch");
        am.put("global.focusSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                focusTimKiemHienTai();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "global.refresh");
        am.put("global.refresh", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                lamMoiManHinhHienTai();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, shortcutMask), "global.toggleSidebar");
        am.put("global.toggleSidebar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleSidebar();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, shortcutMask | KeyEvent.SHIFT_DOWN_MASK), "global.toggleTheme");
        am.put("global.toggleTheme", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleCheDoMau();
            }
        });
    }

    private void focusTimKiemHienTai() {
        switch (currentCardKey) {
            case CARD_DOANH_THU -> doanhThuPanel.focusTimKiem();
            case CARD_SACH      -> sachPanel.focusTimKiem();
            case CARD_MUON_TRA  -> muonTraPanel.focusTimKiem();
            case CARD_DAT_TRUOC -> datTruocPanel.focusTimKiem();
            case CARD_LICH_SU   -> lichSuPanel.focusTimKiem();
            case CARD_VI_PHAM   -> viPhamPanel.focusTimKiem();
            case CARD_DOC_GIA   -> docGiaPanel.focusTimKiem();
            case CARD_NHAC_HAN  -> nhacHanPanel.focusTimKiem();
            case CARD_TAI_KHOAN -> {
                if (nguoiDungPanel != null) {
                    nguoiDungPanel.focusTimKiem();
                }
            }
            default -> ThemeConfig.toastInfo(this,
                    "Nhấn Ctrl + 2..8 để chuyển màn hình và dùng Ctrl + F tìm kiếm nhanh.");
        }
    }

    private void lamMoiManHinhHienTai() {
        switch (currentCardKey) {
            case CARD_TONG_QUAN -> taiDuLieuThongKe();
            case CARD_DOANH_THU -> doanhThuPanel.taiDuLieu();
            case CARD_BXH       -> bxhPanel.taiDuLieu();
            case CARD_SACH      -> sachPanel.taiDuLieuGiuLuaChon();
            case CARD_MUON_TRA  -> muonTraPanel.lamMoi();
            case CARD_DAT_TRUOC -> datTruocPanel.taiDuLieu();
            case CARD_LICH_SU   -> lichSuPanel.taiDuLieu();
            case CARD_VI_PHAM   -> viPhamPanel.taiDuLieu();
            case CARD_DOC_GIA   -> docGiaPanel.taiDuLieuGiuLuaChon();
            case CARD_NHAC_HAN  -> nhacHanPanel.taiDuLieu();
            case CARD_TAI_KHOAN -> {
                if (nguoiDungPanel != null) {
                    nguoiDungPanel.taiDuLieu();
                }
            }
            default -> {
            }
        }
        if (!CARD_TONG_QUAN.equals(currentCardKey)) {
            taiDuLieuThongKe();
        }
        ThemeConfig.toastSuccess(this, "Đã làm mới dữ liệu (F5).");
    }

    public static MainFrame of(Component source) {
        Window window = source == null ? null : SwingUtilities.getWindowAncestor(source);
        return window instanceof MainFrame mf ? mf : null;
    }

    public void moMuonTraVaLoc(String boLoc) {
        chuyenManHinh(CARD_MUON_TRA, "Mượn & Trả sách",
                "Tạo phiếu mượn và xử lý nhận trả sách tự động",
                () -> muonTraPanel.moVaLoc(boLoc));
    }

    public void moMuonTraChoDocGia(int idDocGia, int idSach) {
        chuyenManHinh(CARD_MUON_TRA, "Mượn & Trả sách",
                "Tạo phiếu mượn và xử lý nhận trả sách tự động",
                () -> muonTraPanel.chuanBiMuonChoDocGia(idDocGia, idSach));
    }

    public void moDatTruocHangDoi(String tuKhoaSach) {
        chuyenManHinh(CARD_DAT_TRUOC, "Đặt trước sách",
                "Hàng đợi giữ chỗ sách và thông báo nhận sách",
                () -> {
                    if (tuKhoaSach != null && !tuKhoaSach.isBlank()) {
                        datTruocPanel.moVaLocTheoSach(tuKhoaSach);
                    } else {
                        datTruocPanel.moVaLocDangCho();
                    }
                });
    }

    public void moViPhamTheoTuKhoa(String tuKhoa, String trangThaiLoc) {
        chuyenManHinh(CARD_VI_PHAM, "Quản lý vi phạm",
                "Xem và xử lý các trường hợp quá hạn, hư hao sách",
                () -> viPhamPanel.moVaLocViPham(tuKhoa, trangThaiLoc));
    }

    public void moNhacHan(boolean quaHan) {
        chuyenManHinh(CARD_NHAC_HAN, "Nhắc hạn email",
                "Hệ thống gửi email thông báo và nhắc hạn tự động",
                () -> {
                    if (quaHan) {
                        nhacHanPanel.moVaLocQuaHan();
                    } else {
                        nhacHanPanel.moVaLocSapDenHan3Ngay();
                    }
                });
    }

    public void moLichSuLocTrangThai(String trangThai) {
        chuyenManHinh(CARD_LICH_SU, "Lịch sử mượn trả",
                "Theo dõi chi tiết hạn trả và tiến độ mượn sách",
                () -> lichSuPanel.moVaLocTrangThai(trangThai));
    }

    public void moSachLocTrangThai(String trangThai) {
        chuyenManHinh(CARD_SACH, "Quản lý sách",
                "Tra cứu danh mục và quản lý thông tin kho sách",
                () -> sachPanel.moVaLocTrangThai(trangThai));
    }

    public void moDocGiaLocTrangThai(String trangThai) {
        chuyenManHinh(CARD_DOC_GIA, "Quản lý độc giả",
                "Hồ sơ thẻ thư viện, điểm thưởng và lịch sử vi phạm",
                () -> docGiaPanel.moVaLocTrangThai(trangThai));
    }

    public static void refreshMainDashboard(Component source) {
        Window window = source == null ? null : SwingUtilities.getWindowAncestor(source);
        if (window instanceof MainFrame mf) {
            mf.lamMoiDongBoCacTab(source);
        }
    }

    private void lamMoiDongBoCacTab(Component source) {
        taiDuLieuThongKe();
        SwingUtilities.invokeLater(() -> {
            if (source != doanhThuPanel && !SwingUtilities.isDescendingFrom(source, doanhThuPanel)) {
                doanhThuPanel.taiDuLieu();
            }
            if (source != sachPanel && !SwingUtilities.isDescendingFrom(source, sachPanel)) {
                sachPanel.taiDuLieuGiuLuaChon();
            }
            if (source != docGiaPanel && !SwingUtilities.isDescendingFrom(source, docGiaPanel)) {
                docGiaPanel.taiDuLieuGiuLuaChon();
            }
            if (source != muonTraPanel && !SwingUtilities.isDescendingFrom(source, muonTraPanel)) {
                muonTraPanel.lamMoi();
            }
            if (source != viPhamPanel && !SwingUtilities.isDescendingFrom(source, viPhamPanel)) {
                viPhamPanel.taiDuLieu();
            }
            if (source != lichSuPanel && !SwingUtilities.isDescendingFrom(source, lichSuPanel)) {
                lichSuPanel.taiDuLieu();
            }
            if (source != datTruocPanel && !SwingUtilities.isDescendingFrom(source, datTruocPanel)) {
                datTruocPanel.taiDuLieu();
            }
            if (source != nhacHanPanel && !SwingUtilities.isDescendingFrom(source, nhacHanPanel)) {
                nhacHanPanel.taiDuLieu();
            }
        });
    }

    public void taiDuLieuThongKe() {
        new SwingWorker<ThongKeDAO.DuLieuTongQuan, Void>() {
            @Override
            protected ThongKeDAO.DuLieuTongQuan doInBackground() {
                return thongKeDAO.layDuLieuTongQuan();
            }

            @Override
            protected void done() {
                try {
                    ThongKeDAO.DuLieuTongQuan data = get();
                    lblStatTongSach.setText(String.format("%,d", data.tongSach()));
                    lblStatTongDocGia.setText(String.format("%,d", data.tongDocGia()));
                    lblStatDangMuon.setText(String.format("%,d", data.soDangMuon()));
                    lblStatSapDenHan.setText(String.format("%,d", data.soSapDenHan()));
                    lblStatQuaHan.setText(String.format("%,d", data.soQuaHan()));
                    lblStatDatTruoc.setText(String.format("%,d", data.soDatTruoc()));
                    analyticsChartsPanel.capNhatDuLieu(data.xuHuong7Ngay(), data.coCauKho());
                } catch (Exception e) {
                    System.err.println("Không tải được dữ liệu Dashboard: " + e.getMessage());
                }
            }
        }.execute();
    }

    // ================================================================
    // 6. STATUS BAR DƯỚI CÙNG
    // ================================================================
    private JPanel taoStatusBar() {
        JPanel thanhTrangThai = new JPanel(new BorderLayout(15, 0));
        thanhTrangThai.setBackground(ThemeConfig.BG_PAGE);
        thanhTrangThai.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeConfig.BORDER),
                new EmptyBorder(6, 22, 6, 22)));

        JLabel lblUserRole = new JLabel("Người dùng: " + nguoiDungHienTai.getHoTen()
                + "  |  Tài khoản: @" + nguoiDungHienTai.getTenDangNhap()
                + "  |  Quyền hạn: " + nguoiDungHienTai.getTenVaiTro());
        lblUserRole.setIcon(ThemeConfig.getIcon(Feather.USER, 14, ThemeConfig.TEXT_SECONDARY));
        lblUserRole.setIconTextGap(6);
        lblUserRole.setFont(ThemeConfig.FONT_SM);
        lblUserRole.setForeground(ThemeConfig.TEXT_SECONDARY);

        String thoiGian = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        JLabel lblTime = new JLabel(thoiGian);
        lblTime.setIcon(ThemeConfig.getIcon(Feather.CLOCK, 14, ThemeConfig.TEXT_MUTED));
        lblTime.setIconTextGap(6);
        lblTime.setFont(ThemeConfig.FONT_SM);
        lblTime.setForeground(ThemeConfig.TEXT_MUTED);

        thanhTrangThai.add(lblUserRole, BorderLayout.WEST);
        thanhTrangThai.add(lblTime, BorderLayout.EAST);
        return thanhTrangThai;
    }

    // ================================================================
    // 7. LOGIC THAO TÁC TÀI KHOẢN
    // ================================================================
    private void dangXuat() {
        int chon = JOptionPane.showConfirmDialog(
                this,
                "Bạn muốn đăng xuất tài khoản hiện tại?",
                "Đăng xuất",
                JOptionPane.OK_CANCEL_OPTION);

        if (chon == JOptionPane.OK_OPTION) {
            dispose();
            new LoginFrame();
        }
    }

    private void doiMatKhau() {
        if (dangDoiMatKhau) {
            return;
        }
        JPasswordField matKhauCu = new JPasswordField();
        JPasswordField matKhauMoi = new JPasswordField();
        JPasswordField xacNhan = new JPasswordField();

        Object[] noiDung = {
                "Mật khẩu hiện tại:", matKhauCu,
                "Mật khẩu mới:", matKhauMoi,
                "Nhập lại mật khẩu mới:", xacNhan
        };

        int chon = JOptionPane.showConfirmDialog(
                this,
                noiDung,
                "Đổi mật khẩu",
                JOptionPane.OK_CANCEL_OPTION);

        if (chon != JOptionPane.OK_OPTION) {
            return;
        }

        String cu = new String(matKhauCu.getPassword()).trim();
        String moi = new String(matKhauMoi.getPassword()).trim();
        String nhapLai = new String(xacNhan.getPassword()).trim();

        if (cu.isEmpty() || moi.isEmpty() || nhapLai.isEmpty()) {
            ThemeConfig.toastWarning(this, "Vui lòng nhập đầy đủ thông tin mật khẩu.");
            return;
        }

        if (!moi.equals(nhapLai)) {
            ThemeConfig.toastWarning(this, "Mật khẩu mới và phần xác nhận không khớp.");
            return;
        }

        dangDoiMatKhau = true;
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return new NguoiDungDAO().doiMatKhauHienTai(nguoiDungHienTai.getId(), cu, moi);
            }

            @Override
            protected void done() {
                dangDoiMatKhau = false;
                if (!isDisplayable()) {
                    return;
                }
                try {
                    if (get()) {
                        ThemeConfig.toastSuccess(MainFrame.this, "Đổi mật khẩu thành công.");
                    } else {
                        ThemeConfig.toastError(MainFrame.this, "Mật khẩu hiện tại không đúng hoặc tài khoản đã bị khóa.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    ThemeConfig.toastError(MainFrame.this, "Thao tác đổi mật khẩu bị gián đoạn.");
                } catch (ExecutionException e) {
                    String thongBao = e.getCause() instanceof IllegalArgumentException
                            ? e.getCause().getMessage()
                            : "Không đổi được mật khẩu. Hãy kiểm tra kết nối MySQL.";
                    ThemeConfig.toastError(MainFrame.this, thongBao);
                }
            }
        }.execute();
    }

    private void xuLyThoat() {
        int chon = JOptionPane.showConfirmDialog(
                this,
                "Bạn muốn thoát chương trình?",
                "Thoát",
                JOptionPane.OK_CANCEL_OPTION);

        if (chon == JOptionPane.OK_OPTION) {
            KetNoiDatabase.dongKetNoi();
            dispose();
            System.exit(0);
        }
    }

    // ================================================================
    // 8. INNER COMPONENT (COLLAPSIBLE SIDEBAR NAV ITEM)
    // ================================================================
    private static class SidebarNavItem extends JPanel {
        private final String cardKey;
        private final String title;
        private final String shortcut;
        private final Ikon iconCode;
        private boolean active = false;
        private boolean collapsed = false;

        private final JLabel lblIcon;
        private final JLabel lblText;

        public SidebarNavItem(Ikon iconCode, String title, String cardKey, String shortcut, Runnable onClick) {
            this.cardKey = cardKey;
            this.title = title;
            this.shortcut = shortcut;
            this.iconCode = iconCode;
            setLayout(new BorderLayout(12, 0));
            setBackground(ThemeConfig.BG_SIDEBAR);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, ThemeConfig.NAV_ITEM_HEIGHT));
            setPreferredSize(new Dimension(COMPACT_SIDEBAR_WIDTH, ThemeConfig.NAV_ITEM_HEIGHT));
            setBorder(new EmptyBorder(0, 16, 0, 12));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            lblIcon = new JLabel(ThemeConfig.getIcon(iconCode, 17, ThemeConfig.TEXT_MUTED));
            lblIcon.setHorizontalAlignment(SwingConstants.CENTER);

            lblText = new JLabel(title);
            lblText.setFont(ThemeConfig.FONT_BODY);
            lblText.setForeground(ThemeConfig.TEXT_MUTED);
            lblText.setBorder(new EmptyBorder(0, 0, 1, 0));

            add(lblIcon, BorderLayout.WEST);
            add(lblText, BorderLayout.CENTER);
            updateTooltip();

            MouseAdapter adapter = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!active) {
                        setBackground(new Color(0x334155));
                        lblText.setForeground(ThemeConfig.TEXT_ON_DARK);
                        lblIcon.setIcon(ThemeConfig.getIcon(iconCode, 17, ThemeConfig.TEXT_ON_DARK));
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    Point pt = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), SidebarNavItem.this);
                    if (!SidebarNavItem.this.contains(pt) && !active) {
                        setBackground(ThemeConfig.BG_SIDEBAR);
                        lblText.setForeground(ThemeConfig.TEXT_MUTED);
                        lblIcon.setIcon(ThemeConfig.getIcon(iconCode, 17, ThemeConfig.TEXT_MUTED));
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    onClick.run();
                }
            };
            addMouseListener(adapter);
            lblIcon.addMouseListener(adapter);
            lblText.addMouseListener(adapter);
        }

        private void updateTooltip() {
            String tip = shortcut != null && !shortcut.isBlank() ? title + " (" + shortcut + ")" : title;
            setToolTipText(collapsed ? tip : shortcut);
            lblIcon.setToolTipText(collapsed ? tip : null);
        }

        public void setCollapsed(boolean isCollapsed) {
            this.collapsed = isCollapsed;
            removeAll();
            if (collapsed) {
                setBorder(new EmptyBorder(0, 0, 0, 0));
                setPreferredSize(new Dimension(COLLAPSED_SIDEBAR_WIDTH, ThemeConfig.NAV_ITEM_HEIGHT));
                lblText.setVisible(false);
                add(lblIcon, BorderLayout.CENTER);
            } else {
                setBorder(new EmptyBorder(0, 16, 0, 12));
                setPreferredSize(new Dimension(COMPACT_SIDEBAR_WIDTH, ThemeConfig.NAV_ITEM_HEIGHT));
                lblText.setVisible(true);
                add(lblIcon, BorderLayout.WEST);
                add(lblText, BorderLayout.CENTER);
            }
            updateTooltip();
            revalidate();
            repaint();
        }

        public String getCardKey() {
            return cardKey;
        }

        public void setActive(boolean isActive) {
            this.active = isActive;
            if (active) {
                setBackground(new Color(0x1E3A8A));
                lblText.setFont(ThemeConfig.FONT_NAV);
                lblText.setForeground(Color.WHITE);
                lblIcon.setIcon(ThemeConfig.getIcon(iconCode, 17, Color.WHITE));
            } else {
                setBackground(ThemeConfig.BG_SIDEBAR);
                lblText.setFont(ThemeConfig.FONT_BODY);
                lblText.setForeground(ThemeConfig.TEXT_MUTED);
                lblIcon.setIcon(ThemeConfig.getIcon(iconCode, 17, ThemeConfig.TEXT_MUTED));
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (active) {
                g.setColor(ThemeConfig.PRIMARY);
                g.fillRect(0, 0, 4, getHeight());
            }
        }
    }
}
