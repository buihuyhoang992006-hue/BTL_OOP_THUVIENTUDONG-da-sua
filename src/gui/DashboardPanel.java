package gui;

import dao.ThongKeDAO;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

/**
 * Màn hình tổng hợp số liệu hoạt động thư viện (Dashboard).
 * Thiết kế Modern SaaS Dashboard chuẩn UI_STYLE_GUIDE.md & ThemeConfig:
 * <ul>
 *   <li>6 thẻ chỉ số KPI lớn 34px Bold, có badge icon bo góc nền pastel sinh động</li>
 *   <li>2 bảng dữ liệu mật độ cao: Top 5 sách mượn nhiều & Lượt mượn 6 tháng gần nhất</li>
 *   <li>Bộ icon Feather vector sắc nét qua ThemeConfig.getIcon</li>
 * </ul>
 */
public class DashboardPanel extends JPanel {

    private final ThongKeDAO thongKeDAO;
    private final JLabel lblTongSach = new JLabel("0", SwingConstants.LEFT);
    private final JLabel lblTongDocGia = new JLabel("0", SwingConstants.LEFT);
    private final JLabel lblDangMuon = new JLabel("0", SwingConstants.LEFT);
    private final JLabel lblSapDenHan = new JLabel("0", SwingConstants.LEFT);
    private final JLabel lblQuaHan = new JLabel("0", SwingConstants.LEFT);
    private final JLabel lblDatTruoc = new JLabel("0", SwingConstants.LEFT);
    private final AnalyticsChartsPanel analyticsChartsPanel = new AnalyticsChartsPanel();

    private final DefaultTableModel topModel = taoModel(
            new String[]{"STT", "Tên sách", "Số bản đã mượn"});
    private final DefaultTableModel monthModel = taoModel(
            new String[]{"Tháng", "Số phiếu mượn"});

    public DashboardPanel() {
        this.thongKeDAO = new ThongKeDAO();
        setLayout(new BorderLayout(0, 20));
        setBackground(ThemeConfig.BG_PAGE);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        // ── Header Dashboard ───────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel title = new JLabel("BẢNG ĐIỀU KHIỂN HOẠT ĐỘNG THƯ VIỆN");
        title.setIcon(ThemeConfig.getIcon(Feather.BAR_CHART_2, 24, ThemeConfig.PRIMARY));
        title.setIconTextGap(10);
        title.setFont(ThemeConfig.FONT_H1);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(0, 0, 2, 0));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Thống kê thời gian thực về tài nguyên sách, độc giả và tiến độ mượn trả");
        subtitle.setFont(ThemeConfig.FONT_SM);
        subtitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        subtitle.setBorder(new EmptyBorder(0, 0, 2, 0));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(subtitle);

        JButton btnLamMoi = ThemeConfig.createSecondaryButton("Làm mới dữ liệu", Feather.REFRESH_CW);
        btnLamMoi.setIcon(ThemeConfig.getIcon(Feather.REFRESH_CW, 16, ThemeConfig.PRIMARY));
        btnLamMoi.addActionListener(e -> taiDuLieu());

        header.add(titleBlock, BorderLayout.WEST);
        header.add(btnLamMoi, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ── Main Content Container (khóa chiều rộng vừa khít viewport) ──
        ThemeConfig.ScrollablePanel content = new ThemeConfig.ScrollablePanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Section Title: Chỉ số KPI
        JLabel lblSec1 = new JLabel("CHỈ SỐ TỔNG QUAN HOẠT ĐỘNG");
        lblSec1.setIcon(ThemeConfig.getIcon(Feather.ACTIVITY, 18, ThemeConfig.PRIMARY));
        lblSec1.setIconTextGap(8);
        lblSec1.setFont(ThemeConfig.FONT_H3);
        lblSec1.setForeground(ThemeConfig.TEXT_SECONDARY);
        lblSec1.setBorder(new EmptyBorder(0, 0, 2, 0));
        lblSec1.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(lblSec1);
        content.add(Box.createVerticalStrut(12));

        // 6 Thẻ KPI Thống Kê (Tương tác điều hướng trực tiếp)
        JPanel cards = new JPanel(new GridLayout(2, 3, 20, 20));
        cards.setOpaque(false);
        cards.setAlignmentX(Component.LEFT_ALIGNMENT);

        cards.add(taoCard("Tổng bản sách", lblTongSach, Feather.BOOK,
                ThemeConfig.PRIMARY, new Color(0xDBEAFE), "Xem kho sách đang hoạt động",
                () -> {
                    MainFrame mf = MainFrame.of(this);
                    if (mf != null) mf.moSachLocTrangThai("Hoạt động");
                }));
        cards.add(taoCard("Độc giả hoạt động", lblTongDocGia, Feather.USERS,
                ThemeConfig.SUCCESS, new Color(0xDCFCE7), "Xem hồ sơ độc giả hoạt động",
                () -> {
                    MainFrame mf = MainFrame.of(this);
                    if (mf != null) mf.moDocGiaLocTrangThai("Hoạt động");
                }));
        cards.add(taoCard("Bản sách đang mượn", lblDangMuon, Feather.REPEAT,
                new Color(0x6366F1), new Color(0xEEF2FF), "Xem bản sách đang lưu hành",
                () -> {
                    MainFrame mf = MainFrame.of(this);
                    if (mf != null) mf.moMuonTraVaLoc("Tất cả đang mượn");
                }));
        cards.add(taoCard("Sắp đến hạn (3 ngày)", lblSapDenHan, Feather.CLOCK,
                ThemeConfig.STATUS_WARN_FG, new Color(0xFEF3C7), "Mở Nhắc hạn email",
                () -> {
                    MainFrame mf = MainFrame.of(this);
                    if (mf != null) mf.moNhacHan(false);
                }));
        cards.add(taoCard("Bản sách quá hạn", lblQuaHan, Feather.ALERT_TRIANGLE,
                ThemeConfig.DANGER, new Color(0xFEE2E2), "Mở Quản lý vi phạm",
                () -> {
                    MainFrame mf = MainFrame.of(this);
                    if (mf != null) mf.moViPhamTheoTuKhoa("", "Chưa xử lý");
                }));
        cards.add(taoCard("Đặt trước đang chờ", lblDatTruoc, Feather.CLOCK,
                new Color(0x8B5CF6), new Color(0xF3E8FF), "Mở hàng đợi Đặt trước",
                () -> {
                    MainFrame mf = MainFrame.of(this);
                    if (mf != null) mf.moDatTruocHangDoi("");
                }));

        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, cards.getPreferredSize().height));
        content.add(cards);
        content.add(Box.createVerticalStrut(26));

        // ── Section: Biểu đồ Phân tích Trực quan ─────────────────
        JLabel lblSecCharts = new JLabel("PHÂN TÍCH TRỰC QUAN HOẠT ĐỘNG");
        lblSecCharts.setIcon(ThemeConfig.getIcon(Feather.PIE_CHART, 18, ThemeConfig.PRIMARY));
        lblSecCharts.setIconTextGap(8);
        lblSecCharts.setFont(ThemeConfig.FONT_H3);
        lblSecCharts.setForeground(ThemeConfig.TEXT_SECONDARY);
        lblSecCharts.setBorder(new EmptyBorder(0, 0, 2, 0));
        lblSecCharts.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(lblSecCharts);
        content.add(Box.createVerticalStrut(14));
        content.add(analyticsChartsPanel);
        content.add(Box.createVerticalStrut(26));

        // ── Section 2: 2 Bảng Thống Kê Chi Tiết ──────────────────
        JPanel tablesPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        tablesPanel.setOpaque(false);
        tablesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Table Card 1: Top 5 sách
        JPanel topCard = ThemeConfig.createCard();
        topCard.setLayout(new BorderLayout(0, 10));

        JLabel topTitle = new JLabel("TOP 5 SÁCH ĐƯỢC MƯỢN NHIỀU");
        topTitle.setIcon(ThemeConfig.getIcon(Feather.AWARD, 18, ThemeConfig.PRIMARY));
        topTitle.setIconTextGap(8);
        topTitle.setFont(ThemeConfig.FONT_H3);
        topTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        topTitle.setBorder(new EmptyBorder(0, 0, 2, 0));
        topCard.add(topTitle, BorderLayout.NORTH);

        JTable topTable = new JTable(topModel);
        ThemeConfig.styleTable(topTable);
        topTable.setRowHeight(ThemeConfig.TABLE_ROW_HEIGHT);
        topTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        topTable.getColumnModel().getColumn(1).setPreferredWidth(240);
        topTable.getColumnModel().getColumn(2).setPreferredWidth(110);

        JScrollPane topScroll = new JScrollPane(topTable);
        topScroll.setPreferredSize(new Dimension(0, 220));
        topScroll.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER, 1));
        ThemeConfig.installSaaSDataGrid(
                topScroll, topTable, Feather.AWARD,
                "Chưa có dữ liệu mượn sách",
                "Thống kê Top 5 sách được mượn nhiều sẽ hiển thị tại đây.",
                55, 250, 120);
        topCard.add(topScroll, BorderLayout.CENTER);

        // Table Card 2: Số phiếu mượn theo tháng
        JPanel monthCard = ThemeConfig.createCard();
        monthCard.setLayout(new BorderLayout(0, 10));

        JLabel monthTitle = new JLabel("SỐ PHIẾU MƯỢN 6 THÁNG GẦN NHẤT");
        monthTitle.setIcon(ThemeConfig.getIcon(Feather.CALENDAR, 18, ThemeConfig.PRIMARY));
        monthTitle.setIconTextGap(8);
        monthTitle.setFont(ThemeConfig.FONT_H3);
        monthTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        monthTitle.setBorder(new EmptyBorder(0, 0, 2, 0));
        monthCard.add(monthTitle, BorderLayout.NORTH);

        JTable monthTable = new JTable(monthModel);
        ThemeConfig.styleTable(monthTable);
        monthTable.setRowHeight(ThemeConfig.TABLE_ROW_HEIGHT);
        monthTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        monthTable.getColumnModel().getColumn(1).setPreferredWidth(140);

        JScrollPane monthScroll = new JScrollPane(monthTable);
        monthScroll.setPreferredSize(new Dimension(0, 220));
        monthScroll.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER, 1));
        ThemeConfig.installSaaSDataGrid(
                monthScroll, monthTable, Feather.CALENDAR,
                "Chưa có lượt mượn trong 6 tháng qua",
                "Số lượng phiếu mượn theo từng tháng sẽ được thống kê tại đây.",
                180, 140);
        monthCard.add(monthScroll, BorderLayout.CENTER);

        tablesPanel.add(topCard);
        tablesPanel.add(monthCard);
        content.add(tablesPanel);

        JScrollPane scrollPane = ThemeConfig.createPageScrollPane(content);

        add(scrollPane, BorderLayout.CENTER);
        taiDuLieu();
    }

    private static DefaultTableModel taoModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /**
     * Tạo thẻ thống kê KPI chuẩn UI/UX Pro Max với ModernCardPanel & Hover Elevation
     */
    private JPanel taoCard(String title, JLabel valueLabel, Ikon iconCode, Color iconColor, Color pastelBg,
            String subNote, Runnable onClick) {
        ThemeConfig.ModernCardPanel card = new ThemeConfig.ModernCardPanel(
                new BorderLayout(14, 10), new Insets(20, 24, 20, 24));

        // Header thẻ: Tiêu đề + Icon badge bo góc màu pastel
        JPanel topRow = new JPanel(new BorderLayout(10, 0));
        topRow.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(ThemeConfig.FONT_LABEL);
        lblTitle.setForeground(ThemeConfig.TEXT_SECONDARY);

        // Icon badge tròn / vuông bo góc nền pastel
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
        iconBadge.setPreferredSize(new Dimension(38, 38));
        JLabel lblIcon = new JLabel(ThemeConfig.getIcon(iconCode, 22, iconColor));
        iconBadge.add(lblIcon);

        topRow.add(lblTitle, BorderLayout.WEST);
        topRow.add(iconBadge, BorderLayout.EAST);

        // Số liệu KPI lớn 34px Bold
        valueLabel.setFont(ThemeConfig.FONT_KPI);
        valueLabel.setForeground(iconColor);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel bottomRow = new JPanel(new BorderLayout(8, 0));
        bottomRow.setOpaque(false);
        bottomRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblNote = new JLabel(subNote);
        lblNote.setFont(ThemeConfig.FONT_SM);
        lblNote.setForeground(ThemeConfig.TEXT_MUTED);
        lblNote.setBorder(new EmptyBorder(0, 0, 2, 0));
        bottomRow.add(lblNote, BorderLayout.CENTER);
        if (onClick != null) {
            bottomRow.add(new JLabel(ThemeConfig.getIcon(Feather.ARROW_RIGHT, 14, iconColor)), BorderLayout.EAST);
        }

        JPanel centerBlock = new JPanel();
        centerBlock.setLayout(new BoxLayout(centerBlock, BoxLayout.Y_AXIS));
        centerBlock.setOpaque(false);
        centerBlock.add(valueLabel);
        centerBlock.add(Box.createVerticalStrut(4));
        centerBlock.add(bottomRow);

        card.add(topRow, BorderLayout.NORTH);
        card.add(centerBlock, BorderLayout.CENTER);
        if (onClick != null) {
            card.enableInteractive(iconColor, onClick);
        }
        return card;
    }

    /** Đọc lại toàn bộ số liệu từ MySQL. */
    public final void taiDuLieu() {
        try {
            lblTongSach.setText(String.format("%,d", thongKeDAO.getTongSach()));
            lblTongDocGia.setText(String.format("%,d", thongKeDAO.getTongDocGia()));
            lblDangMuon.setText(String.format("%,d", thongKeDAO.getSoDangMuon()));
            lblSapDenHan.setText(String.format("%,d", thongKeDAO.getSoSapDenHan()));
            lblQuaHan.setText(String.format("%,d", thongKeDAO.getSoQuaHan()));
            lblDatTruoc.setText(String.format("%,d", thongKeDAO.getSoDatTruoc()));

            analyticsChartsPanel.capNhatDuLieu(
                    thongKeDAO.getXuHuongMuonTra7Ngay(),
                    thongKeDAO.getCoCauTrangThaiKhoSach());

            topModel.setRowCount(0);
            for (Object[] row : thongKeDAO.getTop5SachMuonNhieu()) {
                topModel.addRow(row);
            }

            monthModel.setRowCount(0);
            for (Map.Entry<String, Integer> entry : thongKeDAO.getLuotMuonTheoThang().entrySet()) {
                monthModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không tải được dữ liệu Dashboard.\n"
                            + "Hãy kiểm tra kết nối MySQL và cấu trúc database.\n\n"
                            + e.getMessage(),
                    "Lỗi thống kê",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /** Chạy riêng panel để kiểm thử giao diện khi chưa tích hợp MainFrame. */
    public static void main(String[] args) {
        ThemeConfig.initTheme();
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Kiểm thử Dashboard");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1100, 800);
            frame.setLocationRelativeTo(null);
            frame.setContentPane(new DashboardPanel());
            frame.setVisible(true);
        });
    }
}
