package gui;

import dao.ThongKeDAO;
import org.kordamp.ikonli.feather.Feather;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.RoundRectangle2D;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * Màn hình thống kê phí thuê và bồi thường đã ghi nhận.
 * Thiết kế chuẩn Modern SaaS Dashboard, Java 2D chart, tương thích Light/Dark Mode.
 */
public class ThongKeDoanhThuPanel extends JPanel {

    private static final DateTimeFormatter DINH_DANG_GIO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ThongKeDAO thongKeDAO = new ThongKeDAO();

    private ThongKeDAO.PhamViThoiGian phamViHienTai = ThongKeDAO.PhamViThoiGian.TAT_CA;
    private final Map<ThongKeDAO.PhamViThoiGian, JButton> filterButtons =
            new EnumMap<>(ThongKeDAO.PhamViThoiGian.class);

    private final JButton btnLamMoi = new JButton("Làm mới");
    private final JButton btnXuatCsv = new JButton("Xuất báo cáo (CSV)");

    private final JLabel lblTongPhiThue = new JLabel("—");
    private final JLabel lblSubPhiThue = new JLabel("Theo ngày lập phiếu mượn");
    private final JLabel lblTongGiamGia = new JLabel("—");
    private final JLabel lblSubGiamGia = new JLabel("Theo ngày lập phiếu mượn");
    private final JLabel lblPhiSauGiam = new JLabel("—");
    private final JLabel lblSubPhiSauGiam = new JLabel("Theo phí sau voucher trên phiếu mượn");
    private final JLabel lblBoiThuong = new JLabel("—");
    private final JLabel lblChartSubtitle = new JLabel("Đang tải thống kê…");
    private final JLabel lblKetQua = new JLabel("0 giao dịch");

    private final RevenueBarChartCanvas chartCanvas = new RevenueBarChartCanvas();
    private final JTextField txtTimKiem = new JTextField(20);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] {
                    "Mã phiếu",
                    "Ngày lập phiếu",
                    "Độc giả",
                    "Mã Voucher",
                    "Tiền thuê sách",
                    "Giảm giá Voucher",
                    "Phí sau giảm"
            },
            0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 4, 5, 6 -> Long.class;
                default -> String.class;
            };
        }
    };

    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
    private boolean dangTai;
    private boolean taiLai;

    public ThongKeDoanhThuPanel() {
        setLayout(new BorderLayout());
        setBackground(ThemeConfig.BG_PAGE);

        ThemeConfig.ScrollablePanel pageContent = new ThemeConfig.ScrollablePanel();
        pageContent.setLayout(new BoxLayout(pageContent, BoxLayout.Y_AXIS));
        pageContent.setOpaque(false);
        pageContent.setBorder(new EmptyBorder(24, 28, 24, 28));

        pageContent.add(taoCardBoLoc());
        pageContent.add(Box.createVerticalStrut(20));
        pageContent.add(taoKhuVucKpi());
        pageContent.add(Box.createVerticalStrut(20));
        pageContent.add(taoCardBieuDo());
        pageContent.add(Box.createVerticalStrut(20));
        pageContent.add(taoCardBangChiTiet());

        JScrollPane pageScroll = ThemeConfig.createPageScrollPane(pageContent);
        add(pageScroll, BorderLayout.CENTER);

        taiDuLieu();
        SwingUtilities.invokeLater(() -> pageScroll.getVerticalScrollBar().setValue(0));
    }

    // =========================================================================
    // 1. HEADER & BỘ LỌC THỜI GIAN
    // =========================================================================
    private JPanel taoCardBoLoc() {
        ThemeConfig.ModernCardPanel card = new ThemeConfig.ModernCardPanel(
                new BorderLayout(0, 14), new Insets(20, 24, 18, 24));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel topRow = new JPanel(new BorderLayout(12, 0));
        topRow.setOpaque(false);
        topRow.add(
                ThemeConfig.createTitleBlock(
                        "THỐNG KÊ PHÍ THƯ VIỆN",
                        "Phí thuê theo ngày lập phiếu và bồi thường theo ngày xử lý vi phạm",
                        Feather.DOLLAR_SIGN),
                BorderLayout.WEST);

        JPanel rightActions = new JPanel(new ThemeConfig.WrapFlowLayout(FlowLayout.RIGHT, 10, 4));
        rightActions.setOpaque(false);
        ThemeConfig.styleActionButton(
                btnLamMoi, Feather.REFRESH_CW,
                ThemeConfig.SURFACE_CARD, ThemeConfig.TEXT_PRIMARY, ThemeConfig.BORDER);
        ThemeConfig.styleActionButton(
                btnXuatCsv, Feather.DOWNLOAD,
                ThemeConfig.SURFACE_CARD, ThemeConfig.SUCCESS_HOVER, ThemeConfig.BORDER);

        btnLamMoi.addActionListener(e -> taiDuLieu());
        btnXuatCsv.addActionListener(e -> CsvExportUtil.exportTableToCsv(
                this,
                table,
                "Bao_Cao_Phi_Thue_" + phamViHienTai.name().toLowerCase(),
                "BÁO CÁO PHÍ THUÊ (" + phamViHienTai.nhanHienThi() + ")"));

        rightActions.add(btnLamMoi);
        rightActions.add(btnXuatCsv);
        topRow.add(rightActions, BorderLayout.EAST);

        JPanel filterBar = new JPanel(new ThemeConfig.WrapFlowLayout(FlowLayout.LEFT, 10, 6));
        filterBar.setOpaque(false);
        filterBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeConfig.BORDER),
                new EmptyBorder(10, 0, 0, 0)));

        JLabel lblFilter = new JLabel("Khoảng thời gian:");
        lblFilter.setIcon(ThemeConfig.getIcon(Feather.CALENDAR, 15, ThemeConfig.PRIMARY));
        lblFilter.setIconTextGap(6);
        lblFilter.setFont(ThemeConfig.FONT_LABEL);
        lblFilter.setForeground(ThemeConfig.TEXT_PRIMARY);
        filterBar.add(lblFilter);

        for (ThongKeDAO.PhamViThoiGian pv : ThongKeDAO.PhamViThoiGian.values()) {
            JButton btn = new JButton(pv.nhanHienThi());
            btn.addActionListener(e -> {
                if (phamViHienTai != pv) {
                    phamViHienTai = pv;
                    capNhatStyleNutLoc();
                    taiDuLieu();
                }
            });
            filterButtons.put(pv, btn);
            filterBar.add(btn);
        }
        capNhatStyleNutLoc();
        filterBar.putClientProperty("ThemeConfig.refresh", (Runnable) this::capNhatStyleNutLoc);

        card.add(topRow, BorderLayout.NORTH);
        card.add(filterBar, BorderLayout.SOUTH);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height + 16));
        return card;
    }

    private void capNhatStyleNutLoc() {
        for (Map.Entry<ThongKeDAO.PhamViThoiGian, JButton> entry : filterButtons.entrySet()) {
            boolean active = entry.getKey() == phamViHienTai;
            JButton btn = entry.getValue();
            if (active) {
                ThemeConfig.styleActionButton(
                        btn, Feather.CHECK,
                        ThemeConfig.PRIMARY, Color.WHITE, ThemeConfig.PRIMARY_HOVER);
            } else {
                ThemeConfig.styleActionButton(
                        btn, null,
                        ThemeConfig.SURFACE_CARD, ThemeConfig.TEXT_PRIMARY, ThemeConfig.BORDER);
            }
        }
    }

    // =========================================================================
    // 2. THỐNG KÊ PHÍ VÀ BỒI THƯỜNG
    // =========================================================================
    private JPanel taoKhuVucKpi() {
        JPanel grid = new JPanel(new GridLayout(2, 2, 20, 20));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        grid.add(taoKpiCard(
                "Tổng phí thuê sách (trước ưu đãi)",
                lblTongPhiThue,
                lblSubPhiThue,
                Feather.BOOK_OPEN,
                ThemeConfig.PRIMARY,
                new ThemeConfig.ThemeColor(0xDBEAFE, 0x1E3A8A)));

        grid.add(taoKpiCard(
                "Tổng ưu đãi giảm giá Voucher",
                lblTongGiamGia,
                lblSubGiamGia,
                Feather.GIFT,
                new ThemeConfig.ThemeColor(0x7C3AED, 0xA78BFA),
                new ThemeConfig.ThemeColor(0xF3E8FF, 0x3B0764)));

        grid.add(taoKpiCard(
                "Phí thuê sau giảm",
                lblPhiSauGiam,
                lblSubPhiSauGiam,
                Feather.DOLLAR_SIGN,
                ThemeConfig.SUCCESS,
                new ThemeConfig.ThemeColor(0xDCFCE7, 0x14532D)));

        JLabel subBoiThuong = new JLabel("Số tiền ghi khi xử lý hỏng/mất sách trong kỳ");
        grid.add(taoKpiCard(
                "Bồi thường đã ghi nhận",
                lblBoiThuong,
                subBoiThuong,
                Feather.FILE_TEXT,
                new ThemeConfig.ThemeColor(0x6366F1, 0x818CF8),
                new ThemeConfig.ThemeColor(0xEEF2FF, 0x312E81)));

        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, grid.getPreferredSize().height + 8));
        return grid;
    }

    private JPanel taoKpiCard(
            String title,
            JLabel valueLabel,
            JLabel subLabel,
            Feather icon,
            Color accentColor,
            Color pastelBg) {
        ThemeConfig.ModernCardPanel card = new ThemeConfig.ModernCardPanel(
                new BorderLayout(12, 8), new Insets(20, 24, 20, 24));
        card.setLeftAccentColor(accentColor);

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
                g2.setColor(ThemeConfig.snap(pastelBg));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconBadge.setOpaque(false);
        iconBadge.setPreferredSize(new Dimension(36, 36));
        iconBadge.add(new JLabel(ThemeConfig.getIcon(icon, 19, accentColor)));

        topRow.add(lblTitle, BorderLayout.WEST);
        topRow.add(iconBadge, BorderLayout.EAST);

        valueLabel.setFont(ThemeConfig.fontBold(28f));
        valueLabel.setForeground(accentColor);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        subLabel.setFont(ThemeConfig.FONT_SM);
        subLabel.setForeground(ThemeConfig.TEXT_MUTED);
        subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.add(valueLabel);
        center.add(Box.createVerticalStrut(4));
        center.add(subLabel);

        card.add(topRow, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        return card;
    }

    // =========================================================================
    // 3. BIỂU ĐỒ PHÍ THUÊ
    // =========================================================================
    private JPanel taoCardBieuDo() {
        ThemeConfig.ModernCardPanel card = new ThemeConfig.ModernCardPanel(
                new BorderLayout(0, 12), new Insets(20, 24, 18, 24));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel lblTitle = new JLabel("PHÍ THUÊ TRƯỚC VÀ SAU VOUCHER");
        lblTitle.setIcon(ThemeConfig.getIcon(Feather.BAR_CHART_2, 18, ThemeConfig.PRIMARY));
        lblTitle.setIconTextGap(8);
        lblTitle.setFont(ThemeConfig.FONT_H3);
        lblTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblChartSubtitle.setFont(ThemeConfig.FONT_SM);
        lblChartSubtitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        lblChartSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleBlock.add(lblTitle);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(lblChartSubtitle);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 4));
        legend.setOpaque(false);
        legend.add(taoLegendItem("Phí thuê gốc", new ThemeConfig.ThemeColor(0x2563EB, 0x3B82F6)));
        legend.add(taoLegendItem("Phí sau giảm", new ThemeConfig.ThemeColor(0x16A34A, 0x22C55E)));

        header.add(titleBlock, BorderLayout.WEST);
        header.add(legend, BorderLayout.EAST);

        card.add(header, BorderLayout.NORTH);
        card.add(chartCanvas, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(0, 355));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 370));
        return card;
    }

    private static JPanel taoLegendItem(String text, Color dotColor) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        item.setOpaque(false);
        JComponent dot = new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(10, 10);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeConfig.snap(dotColor));
                g2.fillOval(0, 0, 9, 9);
                g2.dispose();
            }
        };
        JLabel lbl = new JLabel(text);
        lbl.setFont(ThemeConfig.FONT_SM);
        lbl.setForeground(ThemeConfig.TEXT_SECONDARY);
        item.add(dot);
        item.add(lbl);
        return item;
    }

    // =========================================================================
    // 4. BẢNG CHI TIẾT PHÍ THUÊ
    // =========================================================================
    private JPanel taoCardBangChiTiet() {
        ThemeConfig.ModernCardPanel card = new ThemeConfig.ModernCardPanel(new BorderLayout(0, 14));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);

        JPanel leftTitle = ThemeConfig.createTitleBlock(
                "CHI TIẾT PHÍ THUÊ",
                "Chi tiết theo phiếu mượn. Tệp CSV xuất các dòng đang hiển thị.",
                Feather.LIST);

        lblKetQua.setFont(ThemeConfig.FONT_SM_BOLD);
        lblKetQua.setForeground(ThemeConfig.PRIMARY);
        lblKetQua.setBackground(ThemeConfig.PRIMARY_LIGHT);
        lblKetQua.setOpaque(true);
        lblKetQua.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xBFDBFE), 1, true),
                new EmptyBorder(4, 10, 4, 10)));

        JPanel rightGroup = new JPanel(new ThemeConfig.WrapFlowLayout(FlowLayout.RIGHT, 12, 4));
        rightGroup.setOpaque(false);
        rightGroup.add(ThemeConfig.createSearchBar(txtTimKiem, "Tìm mã phiếu, độc giả...", 255));
        rightGroup.add(lblKetQua);

        header.add(leftTitle, BorderLayout.WEST);
        header.add(rightGroup, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        ThemeConfig.styleTable(table);
        table.setRowHeight(ThemeConfig.TABLE_ROW_HEIGHT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSorter(sorter);

        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setCellRenderer(new CenterRenderer(true));
        cm.getColumn(1).setCellRenderer(new CenterRenderer(false));
        cm.getColumn(2).setCellRenderer(new LeftRenderer());
        cm.getColumn(3).setCellRenderer(new CenterRenderer(false));
        cm.getColumn(4).setCellRenderer(new CurrencyRenderer(ThemeConfig.TEXT_PRIMARY, false, ""));
        cm.getColumn(5).setCellRenderer(new CurrencyRenderer(ThemeConfig.SUCCESS, false, "- "));
        cm.getColumn(6).setCellRenderer(new CurrencyRenderer(ThemeConfig.PRIMARY, true, ""));

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER, 1));
        scrollTable.getViewport().setBackground(ThemeConfig.SURFACE_CARD);
        scrollTable.setPreferredSize(new Dimension(0, 430));
        scrollTable.setMinimumSize(new Dimension(0, 380));
        ThemeConfig.installSaaSDataGrid(
                scrollTable, table, Feather.DOLLAR_SIGN,
                "Không có dòng phí thuê hiển thị",
                "Kiểm tra trạng thái báo cáo hoặc thay đổi bộ lọc.",
                190, 145, 215, 145, 130, 135, 135);
        card.add(scrollTable, BorderLayout.CENTER);

        txtTimKiem.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { apDungBoLoc(); }
            @Override public void removeUpdate(DocumentEvent e) { apDungBoLoc(); }
            @Override public void changedUpdate(DocumentEvent e) { apDungBoLoc(); }
        });

        return card;
    }

    // =========================================================================
    // 5. TẢI & CẬP NHẬT DỮ LIỆU
    // =========================================================================
    public void taiDuLieu() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::taiDuLieu);
            return;
        }
        if (dangTai) {
            taiLai = true;
            return;
        }
        dangTai = true;
        final ThongKeDAO.PhamViThoiGian targetPhamVi = phamViHienTai;
        hienThiTrangThaiTai("Đang tải thống kê…");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<ThongKeDAO.BaoCaoDoanhThu, Void>() {
            @Override
            protected ThongKeDAO.BaoCaoDoanhThu doInBackground() {
                return thongKeDAO.layBaoCaoDoanhThu(targetPhamVi);
            }

            @Override
            protected void done() {
                dangTai = false;
                if (taiLai) {
                    taiLai = false;
                    taiDuLieu();
                    return;
                }
                setCursor(Cursor.getDefaultCursor());
                try {
                    capNhatGiaoDien(get(), targetPhamVi);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    hienThiTrangThaiTai("Đã dừng tải thống kê.");
                } catch (ExecutionException e) {
                    System.err.println("Không tải được thống kê phí: " + e.getCause().getMessage());
                    hienThiTrangThaiTai("Không tải được thống kê. Kiểm tra kết nối và bấm Làm mới.");
                }
            }
        }.execute();
    }

    private void hienThiTrangThaiTai(String thongBao) {
        lblTongPhiThue.setText("—");
        lblTongGiamGia.setText("—");
        lblPhiSauGiam.setText("—");
        lblBoiThuong.setText("—");
        lblSubGiamGia.setText("Theo ngày lập phiếu mượn");
        lblSubPhiSauGiam.setText("Theo phí sau voucher trên phiếu mượn");
        lblChartSubtitle.setText(thongBao);
        chartCanvas.setDuLieu(List.of());
        tableModel.setRowCount(0);
        lblKetQua.setText("—");
        btnXuatCsv.setEnabled(false);
    }

    private void capNhatGiaoDien(ThongKeDAO.BaoCaoDoanhThu bc, ThongKeDAO.PhamViThoiGian pv) {
        lblTongPhiThue.setText(String.format("%,d đ", bc.tongTienThueGoc()));
        lblSubPhiThue.setText("Tổng giá trị phí thuê trước ưu đãi trong kỳ");
        lblTongGiamGia.setText(String.format("- %,d đ", bc.tongGiamGiaVoucher()));
        lblSubGiamGia.setText(String.format("%,d phiếu áp dụng mã Voucher đổi thưởng", bc.soPhieuDungVoucher()));
        lblPhiSauGiam.setText(String.format("%,d đ", bc.phiSauGiam()));
        lblSubPhiSauGiam.setText(String.format("%,d phiếu có tính phí thuê trong kỳ", bc.soPhieuPhatSinh()));
        lblBoiThuong.setText(String.format("%,d đ", bc.boiThuongDaGhiNhan()));

        String moTaKy = switch (pv) {
            case HOM_NAY -> "Biểu đồ phí thuê theo khung giờ trong ngày hôm nay";
            case BAY_NGAY -> "Biểu đồ phí thuê theo từng ngày trong 7 ngày gần nhất";
            case THANG_NAY -> "Biểu đồ phí thuê theo từng ngày trong tháng hiện tại";
            case TAT_CA -> "Biểu đồ phí thuê tổng hợp theo từng tháng";
        };
        lblChartSubtitle.setText(moTaKy);
        chartCanvas.setDuLieu(bc.bieuDo());

        tableModel.setRowCount(0);
        for (ThongKeDAO.DongGiaoDichDoanhThu d : bc.giaoDich()) {
            String ngayStr = d.ngayLapPhieu() == null
                    ? ""
                    : DINH_DANG_GIO.format(d.ngayLapPhieu());
            String voucherStr = (d.maVoucher() == null || d.maVoucher().isBlank()) ? "—" : d.maVoucher();
            tableModel.addRow(new Object[] {
                    d.maPhieu(),
                    ngayStr,
                    d.docGiaHienThi(),
                    voucherStr,
                    d.tienThueSach(),
                    d.giamGiaVoucher(),
                    d.phiSauGiam()
            });
        }
        apDungBoLoc();
        btnXuatCsv.setEnabled(tableModel.getRowCount() > 0);
    }

    private void apDungBoLoc() {
        String tuKhoa = ThemeConfig.normalizeText(txtTimKiem.getText());
        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                if (tuKhoa.isEmpty()) {
                    return true;
                }
                String gop = ThemeConfig.normalizeText(
                        entry.getStringValue(0) + " "
                                + entry.getStringValue(1) + " "
                                + entry.getStringValue(2) + " "
                                + entry.getStringValue(3));
                return gop.contains(tuKhoa);
            }
        });
        lblKetQua.setText("Hiển thị " + table.getRowCount() + " / " + tableModel.getRowCount() + " giao dịch");
    }

    public void focusTimKiem() {
        Container parent = txtTimKiem.getParent();
        if (parent instanceof JComponent jc) {
            jc.scrollRectToVisible(new Rectangle(0, 0, jc.getWidth(), jc.getHeight()));
        }
        txtTimKiem.requestFocusInWindow();
        txtTimKiem.selectAll();
    }

    // =========================================================================
    // 6. JAVA 2D GROUPED BAR CHART CANVAS
    // =========================================================================
    private static final class RevenueBarChartCanvas extends JPanel {
        private static final Color THUE_TOP = new ThemeConfig.ThemeColor(0x60A5FA, 0x60A5FA);
        private static final Color THUE_BOT = new ThemeConfig.ThemeColor(0x2563EB, 0x3B82F6);
        private static final Color SAU_GIAM_TOP = new ThemeConfig.ThemeColor(0x4ADE80, 0x4ADE80);
        private static final Color SAU_GIAM_BOT = new ThemeConfig.ThemeColor(0x16A34A, 0x22C55E);

        private List<ThongKeDAO.MocDoanhThuBieuDo> duLieu = List.of();
        private int hoveredIndex = -1;

        RevenueBarChartCanvas() {
            setOpaque(false);
            setPreferredSize(new Dimension(600, 260));

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int idx = timCotTheoToaDo(e.getX(), e.getY());
                    if (idx != hoveredIndex) {
                        hoveredIndex = idx;
                        setCursor(idx >= 0
                                ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                : Cursor.getDefaultCursor());
                        repaint();
                    }
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent e) {
                    if (hoveredIndex != -1) {
                        hoveredIndex = -1;
                        setCursor(Cursor.getDefaultCursor());
                        repaint();
                    }
                }
            });
        }

        void setDuLieu(List<ThongKeDAO.MocDoanhThuBieuDo> data) {
            this.duLieu = List.copyOf(data);
            hoveredIndex = -1;
            repaint();
        }

        private int timCotTheoToaDo(int mx, int my) {
            if (duLieu.isEmpty()) {
                return -1;
            }
            int plotX = 62;
            int plotY = 26;
            int plotW = getWidth() - plotX - 16;
            int plotH = getHeight() - plotY - 30;
            if (plotW <= 20 || plotH <= 20 || my < plotY || my > plotY + plotH || mx < plotX || mx > plotX + plotW) {
                return -1;
            }
            double slotW = (double) plotW / duLieu.size();
            int idx = (int) ((mx - plotX) / slotW);
            return (idx >= 0 && idx < duLieu.size()) ? idx : -1;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            int w = getWidth();
            int h = getHeight();
            int plotX = 62;
            int plotY = 26;
            int plotW = Math.max(40, w - plotX - 16);
            int plotH = Math.max(40, h - plotY - 30);

            long rawMax = 0L;
            for (ThongKeDAO.MocDoanhThuBieuDo m : duLieu) {
                rawMax = Math.max(rawMax, Math.max(m.tongTienThueGoc(), m.phiSauGiam()));
            }
            long yMax = tinhMocTranDep(Math.max(20_000L, rawMax));
            long step = yMax / 4L;

            Stroke defaultStroke = g2.getStroke();
            Stroke dashed = new BasicStroke(
                    1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 4f}, 0f);
            g2.setFont(ThemeConfig.fontPlain(12f));
            FontMetrics fmAxis = g2.getFontMetrics();

            for (int tick = 0; tick <= 4; tick++) {
                long val = tick * step;
                int y = plotY + plotH - (int) Math.round((val / (double) yMax) * plotH);
                g2.setStroke(tick == 0 ? defaultStroke : dashed);
                g2.setColor(ThemeConfig.snap(ThemeConfig.BORDER));
                g2.drawLine(plotX, y, plotX + plotW, y);

                String label = dinhDangTienNgan(val);
                int labelW = fmAxis.stringWidth(label);
                g2.setColor(ThemeConfig.snap(ThemeConfig.TEXT_MUTED));
                g2.drawString(label, plotX - 8 - labelW, y + fmAxis.getAscent() / 2 - 1);
            }
            g2.setStroke(defaultStroke);

            if (duLieu.isEmpty()) {
                veTrangThaiKhongDuLieu(g2, plotX, plotY, plotW, plotH, "Chưa có dữ liệu biểu đồ");
                g2.dispose();
                return;
            }

            int n = duLieu.size();
            double slotW = (double) plotW / n;
            int barW = Math.min(24, Math.max(5, (int) ((slotW - 10) / 2.3)));
            int gap = Math.max(2, Math.min(6, barW / 3));
            int labelStride = n > 20 ? 3 : (n > 12 ? 2 : 1);

            Font xFontNormal = ThemeConfig.fontPlain(11.5f);
            Font xFontBold = ThemeConfig.fontBold(11.5f);

            for (int i = 0; i < n; i++) {
                ThongKeDAO.MocDoanhThuBieuDo item = duLieu.get(i);
                double slotX = plotX + i * slotW;
                boolean isHover = (i == hoveredIndex);

                if (isHover) {
                    g2.setColor(ThemeConfig.snap(ThemeConfig.SURFACE_HOVER));
                    g2.fill(new RoundRectangle2D.Double(slotX + 2, plotY, slotW - 4, plotH, 10, 10));
                }

                double groupW = barW * 2.0 + gap;
                double xGoc = slotX + (slotW - groupW) / 2.0;
                double xThuc = xGoc + barW + gap;

                veCot(g2, xGoc, plotY, plotH, barW, item.tongTienThueGoc(), yMax, THUE_TOP, THUE_BOT, isHover);
                veCot(g2, xThuc, plotY, plotH, barW, item.phiSauGiam(), yMax, SAU_GIAM_TOP, SAU_GIAM_BOT, isHover);

                if (isHover || i % labelStride == 0 || i == n - 1) {
                    g2.setFont(isHover || i == n - 1 ? xFontBold : xFontNormal);
                    g2.setColor(isHover || i == n - 1
                            ? ThemeConfig.snap(ThemeConfig.TEXT_PRIMARY)
                            : ThemeConfig.snap(ThemeConfig.TEXT_MUTED));
                    String xText = item.nhanMoc();
                    int xTextW = g2.getFontMetrics().stringWidth(xText);
                    g2.drawString(xText,
                            (int) Math.round(slotX + (slotW - xTextW) / 2.0),
                            plotY + plotH + 18);
                }
            }

            if (rawMax == 0) {
                veTrangThaiKhongDuLieu(g2, plotX, plotY, plotW, plotH,
                        "Chưa phát sinh phí thuê trong kỳ đã chọn");
            }

            if (hoveredIndex >= 0 && hoveredIndex < n) {
                ThongKeDAO.MocDoanhThuBieuDo item = duLieu.get(hoveredIndex);
                String tip = item.nhanMoc()
                        + "  •  Phí thuê gốc: " + String.format("%,d đ", item.tongTienThueGoc())
                        + "  |  Sau giảm: " + String.format("%,d đ", item.phiSauGiam());
                veTooltip(g2, tip, plotX, plotW, plotX + (hoveredIndex + 0.5) * slotW);
            }

            g2.dispose();
        }

        private static long tinhMocTranDep(long target) {
            long stepRaw = Math.max(1_000L, (long) Math.ceil(target / 4.0));
            long mag = 1_000L;
            while (mag * 10L <= stepRaw) {
                mag *= 10L;
            }
            long[] niceMult = { 1, 2, 5, 10 };
            long best = mag * 10L;
            for (long m : niceMult) {
                if (m * mag >= stepRaw) {
                    best = m * mag;
                    break;
                }
            }
            return best * 4L;
        }

        private static String dinhDangTienNgan(long val) {
            if (val <= 0) {
                return "0";
            }
            if (val >= 1_000_000L) {
                double m = val / 1_000_000.0;
                return (Math.abs(m - Math.rint(m)) < 0.05)
                        ? String.format("%.0fM", m)
                        : String.format("%.1fM", m);
            }
            if (val >= 1_000L) {
                return (val / 1_000L) + "k";
            }
            return String.valueOf(val);
        }

        private static void veCot(
                Graphics2D g2, double x, int plotY, int plotH, int barW,
                long val, long yMax, Color cTop, Color cBot, boolean isHover) {
            if (val <= 0 || yMax <= 0) {
                return;
            }
            double ratio = Math.min(1.0, val / (double) yMax);
            double barH = Math.max(4.0, ratio * plotH);
            double y = plotY + plotH - barH;
            double arc = Math.min(8.0, barW * 0.7);

            Paint oldPaint = g2.getPaint();
            Color top = ThemeConfig.snap(cTop);
            Color bot = ThemeConfig.snap(cBot);
            if (isHover) {
                top = top.brighter();
            }
            g2.setPaint(new GradientPaint((float) x, (float) y, top, (float) x, (float) (plotY + plotH), bot));
            g2.fill(new RoundRectangle2D.Double(x, y, barW, barH, arc, arc));
            g2.setPaint(oldPaint);
        }

        private static void veTrangThaiKhongDuLieu(
                Graphics2D g2, int plotX, int plotY, int plotW, int plotH, String msg) {
            g2.setFont(ThemeConfig.fontPlain(13.5f));
            g2.setColor(ThemeConfig.snap(ThemeConfig.TEXT_MUTED));
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(msg);
            g2.drawString(msg, plotX + (plotW - tw) / 2, plotY + plotH / 2 + fm.getAscent() / 2);
        }

        private static void veTooltip(Graphics2D g2, String tip, int plotX, int plotW, double centerX) {
            g2.setFont(ThemeConfig.fontBold(12f));
            FontMetrics fm = g2.getFontMetrics();
            int boxW = fm.stringWidth(tip) + 20;
            int boxH = 26;
            int bx = (int) Math.round(centerX - boxW / 2.0);
            bx = Math.max(plotX, Math.min(plotX + plotW - boxW, bx));
            int by = 2;

            g2.setColor(ThemeConfig.snap(new ThemeConfig.ThemeColor(0x0F172A, 0x334155)));
            g2.fillRoundRect(bx, by, boxW, boxH, 8, 8);
            g2.setColor(Color.WHITE);
            g2.drawString(tip, bx + 10, by + 17);
        }
    }

    // =========================================================================
    // 7. RENDERERS CHO BẢNG PHÍ THUÊ
    // =========================================================================
    private static class LeftRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(ThemeConfig.FONT_BODY);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            setHorizontalAlignment(SwingConstants.LEFT);
            if (!isSelected) {
                setBackground(row % 2 == 0 ? ThemeConfig.snap(ThemeConfig.SURFACE_CARD) : ThemeConfig.snap(ThemeConfig.BG_PAGE));
                setForeground(ThemeConfig.snap(ThemeConfig.TEXT_PRIMARY));
            }
            return this;
        }
    }

    private static class CenterRenderer extends DefaultTableCellRenderer {
        private final boolean bold;

        CenterRenderer(boolean bold) {
            this.bold = bold;
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(bold ? ThemeConfig.FONT_BTN : ThemeConfig.FONT_BODY);
            setHorizontalAlignment(SwingConstants.CENTER);
            if (!isSelected) {
                setBackground(row % 2 == 0 ? ThemeConfig.snap(ThemeConfig.SURFACE_CARD) : ThemeConfig.snap(ThemeConfig.BG_PAGE));
                setForeground(ThemeConfig.snap(ThemeConfig.TEXT_PRIMARY));
            }
            return this;
        }
    }

    private static class CurrencyRenderer extends DefaultTableCellRenderer {
        private final Color positiveColor;
        private final boolean boldPositive;
        private final String prefixPositive;

        CurrencyRenderer(Color positiveColor, boolean boldPositive, String prefixPositive) {
            this.positiveColor = positiveColor;
            this.boldPositive = boldPositive;
            this.prefixPositive = Objects.toString(prefixPositive, "");
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            long amount = value instanceof Number n ? n.longValue() : 0L;
            setText(amount > 0 ? prefixPositive + String.format("%,d đ", amount) : "0 đ");
            setHorizontalAlignment(SwingConstants.RIGHT);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            setFont(amount > 0 && boldPositive ? ThemeConfig.FONT_BTN : ThemeConfig.FONT_BODY);
            if (!isSelected) {
                setBackground(row % 2 == 0 ? ThemeConfig.snap(ThemeConfig.SURFACE_CARD) : ThemeConfig.snap(ThemeConfig.BG_PAGE));
                setForeground(amount > 0 ? ThemeConfig.snap(positiveColor) : ThemeConfig.snap(ThemeConfig.TEXT_MUTED));
            }
            return this;
        }
    }
}
