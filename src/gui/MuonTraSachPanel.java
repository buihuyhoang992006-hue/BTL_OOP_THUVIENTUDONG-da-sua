package gui;

import dao.ChungTuMuonDAO;
import dao.DatTruocDAO;
import dao.DocGiaDAO;
import dao.PhieuMuonDAO;
import dao.SachDAO;
import dao.ThongBaoDAO;
import dao.ViPhamDAO;
import dao.VoucherDAO;
import exception.DuLieuKhongHopLeException;
import model.ChiTietPhieuMuon;
import model.DatTruocChiTiet;
import model.DocGia;
import model.NguoiDung;
import model.PhieuMuon;
import model.Sach;
import model.ViPham;
import model.Voucher;
import org.kordamp.ikonli.feather.Feather;
import service.DatTruocService;
import service.MuonTraService;
import service.RewardService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Màn hình Mượn / Trả sách — Thiết kế chuẩn Modern SaaS Dashboard theo
 * UI_STYLE_GUIDE.md.
 */
public class MuonTraSachPanel extends JPanel {
    private static final ZoneId MUI_GIO = ZoneId.of("Asia/Ho_Chi_Minh");

    private final NguoiDung thuThu;
    private final MuonTraService muonTraService = new MuonTraService();
    private final DatTruocService datTruocService = new DatTruocService();
    private final DatTruocDAO datTruocDAO = new DatTruocDAO();
    private final VoucherDAO voucherDAO = new VoucherDAO();

    private record VoucherOption(Voucher voucher) {
        @Override
        public String toString() {
            if (voucher == null) {
                return "Không áp dụng Voucher";
            }
            return voucher.toString();
        }
    }

    private final JComboBox<DocGia> cbDocGia = new JComboBox<>();
    private final JComboBox<Sach> cbSach = new JComboBox<>();
    private final JComboBox<VoucherOption> cbVoucher = new JComboBox<>();

    private final JTextField txtMaThe = new JTextField(12);

    private final JSpinner spSoLuong = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));

    private final JTextField txtHanTra = new JTextField(LocalDate.now(MUI_GIO).plusDays(14).toString());

    private final JLabel lblDonGiaThue = new JLabel("0 đ / cuốn");
    private final JLabel lblTongTienThue = new JLabel("0 đ");
    private final JLabel lblGiamGiaVoucher = new JLabel("0 đ");
    private final JLabel lblKhachThanhToan = new JLabel("0 đ");
    private final JLabel lblDiemDuKien = new JLabel("+10 điểm");
    private final JPanel pnlCanhBaoDiemAm = new JPanel(new BorderLayout(8, 0));
    private final JLabel lblCanhBaoDiemAm = new JLabel();

    private final JButton btnChonThe = new JButton("Chọn thẻ");
    private final JButton btnMuon = new JButton("Mượn sách");
    private final JButton btnInPhieu = new JButton("Xem & In phiếu mượn");
    private final JButton btnTra = new JButton("Trả sách đã chọn");
    private final JButton btnLamMoi = new JButton("Làm mới");
    private final JButton btnMoNhacHan = new JButton("Nhắc hạn Email");
    private final JButton btnMoViPham = new JButton("Quản lý vi phạm");

    private final JTextField txtTimKiemDangMuon = new JTextField(18);
    private final JComboBox<String> cbLocDangMuon = new JComboBox<>(new String[] {
            "Tất cả đang mượn",
            "Sắp đến hạn (3 ngày)",
            "Chưa trả quá hạn",
            "Trong hạn (> 3 ngày)"
    });

    private final JLabel lblThongTin = new JLabel(" ");
    private final JLabel lblTonKho = new JLabel("Chưa chọn sách.");

    private boolean dangXuLy;
    private boolean dangCapNhatBoLoc;
    private boolean canLamMoiLai;
    private Integer pendingSelectDocGiaId;
    private Integer pendingSelectSachId;

    private final Map<Integer, DongMuonInfo> mapDongMuon = new LinkedHashMap<>();

    private record DongMuonInfo(
            int idChiTiet,
            int idPhieu,
            String maPhieu,
            int idDocGia,
            String maDocGia,
            String tenDocGia,
            int idSach,
            String maSach,
            String tenSach,
            int soLuong,
            LocalDate hanTra,
            int soNgayQuaHan,
            String tenTrangThai) {
    }

    private record KetQuaTraSach(
            boolean thanhCong,
            List<ViPham> dsViPhamMoi,
            DatTruocService.KetQuaHangDoiTraSach ketQuaHangDoi) {
    }

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] {
                    "ID chi tiết",
                    "Mã phiếu",
                    "Độc giả",
                    "Tên sách",
                    "Số lượng",
                    "Hạn trả",
                    "Trạng thái",
                    "Số ngày quá hạn"
            },
            0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0, 4, 7 -> Integer.class;
                default -> String.class;
            };
        }
    };

    private final JTable tblDangMuon = new JTable(tableModel);
    private final TableRowSorter<DefaultTableModel> sorterDangMuon = new TableRowSorter<>(tableModel);
    private final JTabbedPane tabMuonTra = new JTabbedPane();

    public MuonTraSachPanel(NguoiDung thuThu) {
        this.thuThu = thuThu;

        setLayout(new BorderLayout());
        setBackground(ThemeConfig.BG_PAGE);

        ThemeConfig.ScrollablePanel pageContent = new ThemeConfig.ScrollablePanel();
        pageContent.setLayout(new BorderLayout(0, 22));
        pageContent.setOpaque(false);
        pageContent.setBorder(new EmptyBorder(24, 28, 24, 28));

        // ============================================================
        // 1. CARD TRÊN: LẬP PHIẾU MƯỢN MỚI (Quick-Action Flow)
        // ============================================================
        JPanel cardMuonMoi = new ThemeConfig.ModernCardPanel(new BorderLayout(0, 16));

        JPanel headerCard1 = new JPanel(new BorderLayout());
        headerCard1.setOpaque(false);

        JPanel titleBlock1 = ThemeConfig.createTitleBlock(
                "LẬP PHIẾU MƯỢN MỚI",
                "Mã phiếu được cấp khi lưu, theo mẫu PM-YYYYMMDD-000123.",
                Feather.FILE_PLUS);

        lblTonKho.setIcon(ThemeConfig.getIcon(Feather.PACKAGE, 14, ThemeConfig.PRIMARY));
        lblTonKho.setIconTextGap(6);
        lblTonKho.setFont(ThemeConfig.FONT_SM_BOLD);
        lblTonKho.setForeground(ThemeConfig.PRIMARY);
        lblTonKho.setBackground(ThemeConfig.PRIMARY_LIGHT);
        lblTonKho.setOpaque(true);
        lblTonKho.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xBFDBFE), 1, true),
                new EmptyBorder(4, 10, 4, 10)));

        headerCard1.add(titleBlock1, BorderLayout.WEST);
        headerCard1.add(lblTonKho, BorderLayout.EAST);
        cardMuonMoi.add(headerCard1, BorderLayout.NORTH);

        JPanel formGrid = new JPanel(new GridBagLayout());
        formGrid.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(7, 9, 7, 9);

        txtMaThe.setFont(ThemeConfig.FONT_BODY);
        txtMaThe.setPreferredSize(new Dimension(140, ThemeConfig.INPUT_HEIGHT));
        txtMaThe.putClientProperty("JTextField.placeholderText", "Nhập CCCD hoặc mã thẻ cũ");

        cbDocGia.setFont(ThemeConfig.FONT_BODY);
        cbDocGia.setPreferredSize(new Dimension(220, ThemeConfig.INPUT_HEIGHT));
        cbDocGia.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof DocGia dg) {
                    setText(dg.getMaDocGia() + " - " + dg.getHoTen()
                            + " (" + dg.getDiemThuong() + "đ • " + dg.getHangThe() + ")");
                }
                return this;
            }
        });

        cbSach.setFont(ThemeConfig.FONT_BODY);
        cbSach.setPreferredSize(new Dimension(280, ThemeConfig.INPUT_HEIGHT));
        cbSach.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Sach s) {
                    setText(s.getMaSach() + " - " + s.getTenSach()
                            + " (Giá: " + String.format("%,d đ", s.getGiaThue())
                            + " • Còn: " + s.getSoLuongCon() + ")");
                }
                return this;
            }
        });

        cbVoucher.setFont(ThemeConfig.FONT_BODY);
        cbVoucher.setPreferredSize(new Dimension(280, ThemeConfig.INPUT_HEIGHT));
        cbVoucher.addItem(new VoucherOption(null));

        spSoLuong.setFont(ThemeConfig.FONT_BODY);
        spSoLuong.setPreferredSize(new Dimension(90, ThemeConfig.INPUT_HEIGHT));

        txtHanTra.setFont(ThemeConfig.FONT_BODY);
        txtHanTra.setPreferredSize(new Dimension(130, ThemeConfig.INPUT_HEIGHT));
        txtHanTra.putClientProperty("JTextField.placeholderText", "yyyy-MM-dd");

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        formGrid.add(createFormLabel("CCCD / mã thẻ:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        JPanel pnlMaThe = new JPanel(new BorderLayout(6, 0));
        pnlMaThe.setOpaque(false);
        pnlMaThe.add(txtMaThe, BorderLayout.CENTER);

        ThemeConfig.styleActionButton(btnChonThe, Feather.CREDIT_CARD, Color.WHITE, ThemeConfig.TEXT_PRIMARY,
                ThemeConfig.BORDER);
        pnlMaThe.add(btnChonThe, BorderLayout.EAST);
        formGrid.add(pnlMaThe, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        formGrid.add(createFormLabel("Độc giả:"), gbc);

        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.weightx = 0.7;
        formGrid.add(cbDocGia, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        formGrid.add(createFormLabel("Sách còn kho:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.5;
        formGrid.add(cbSach, gbc);

        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        formGrid.add(createFormLabel("Số lượng:"), gbc);

        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.weightx = 0.5;
        JPanel pnlHanTra = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlHanTra.setOpaque(false);
        pnlHanTra.add(spSoLuong);
        pnlHanTra.add(createFormLabel("Hạn trả:"));
        pnlHanTra.add(txtHanTra);
        formGrid.add(pnlHanTra, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        formGrid.add(createFormLabel("Mã Voucher:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        formGrid.add(cbVoucher, gbc);
        gbc.gridwidth = 1;

        JPanel pnlTomTatPhi = new JPanel(new GridLayout(1, 5, 12, 8));
        pnlTomTatPhi.setBackground(ThemeConfig.SUBTLE_SURFACE);
        pnlTomTatPhi.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER, 1, true),
                new EmptyBorder(10, 14, 10, 14)));
        pnlTomTatPhi.add(taoOMinStat("Đơn giá thuê", lblDonGiaThue, ThemeConfig.TEXT_PRIMARY));
        pnlTomTatPhi.add(taoOMinStat("Tổng tiền thuê", lblTongTienThue, ThemeConfig.TEXT_PRIMARY));
        pnlTomTatPhi.add(taoOMinStat("Giảm giá Voucher", lblGiamGiaVoucher, ThemeConfig.SUCCESS));
        pnlTomTatPhi.add(taoOMinStat("Khách cần thanh toán", lblKhachThanhToan, ThemeConfig.PRIMARY));
        pnlTomTatPhi.add(taoOMinStat("Điểm dự kiến nhận", lblDiemDuKien, new ThemeConfig.ThemeColor(0x7C3AED, 0xA78BFA)));

        pnlCanhBaoDiemAm.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.DANGER, 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        pnlCanhBaoDiemAm.setBackground(ThemeConfig.STATUS_ERROR_BG);
        lblCanhBaoDiemAm.setFont(ThemeConfig.FONT_SM_BOLD);
        lblCanhBaoDiemAm.setIconTextGap(8);
        pnlCanhBaoDiemAm.add(lblCanhBaoDiemAm, BorderLayout.CENTER);
        pnlCanhBaoDiemAm.setVisible(false);

        JPanel centerMuonMoi = new JPanel(new BorderLayout(0, 12));
        centerMuonMoi.setOpaque(false);
        centerMuonMoi.add(formGrid, BorderLayout.NORTH);
        centerMuonMoi.add(pnlTomTatPhi, BorderLayout.CENTER);
        centerMuonMoi.add(pnlCanhBaoDiemAm, BorderLayout.SOUTH);

        cardMuonMoi.add(centerMuonMoi, BorderLayout.CENTER);

        // Nút hành động của Card 1 sử dụng Ikonli Feather Icons
        JPanel actionsCard1 = new JPanel(new ThemeConfig.WrapFlowLayout(FlowLayout.RIGHT, 12, 8));
        actionsCard1.setOpaque(false);

        ThemeConfig.styleActionButton(btnLamMoi, Feather.REFRESH_CW, Color.WHITE, ThemeConfig.TEXT_PRIMARY,
                ThemeConfig.BORDER);
        ThemeConfig.styleActionButton(btnMuon, Feather.BOOK_OPEN, ThemeConfig.PRIMARY, Color.WHITE,
                ThemeConfig.PRIMARY_HOVER);

        actionsCard1.add(btnLamMoi);
        actionsCard1.add(btnMuon);
        cardMuonMoi.add(actionsCard1, BorderLayout.SOUTH);

        pageContent.add(cardMuonMoi, BorderLayout.NORTH);

        // ============================================================
        // 2. CARD DƯỚI: DANH SÁCH ĐANG MƯỢN & TRẢ SÁCH (Data-Dense Table)
        // ============================================================
        JPanel cardTable = new ThemeConfig.ModernCardPanel(new BorderLayout(0, 16));

        JPanel headerCard2 = new JPanel(new BorderLayout(12, 0));
        headerCard2.setOpaque(false);

        JPanel titleBlock2 = new JPanel();
        titleBlock2.setLayout(new BoxLayout(titleBlock2, BoxLayout.Y_AXIS));
        titleBlock2.setOpaque(false);
        JLabel lblTitle2 = new JLabel("SÁCH ĐANG CHỜ TRẢ");
        lblTitle2.setIcon(ThemeConfig.getIcon(Feather.REPEAT, 18, ThemeConfig.PRIMARY));
        lblTitle2.setIconTextGap(8);
        lblTitle2.setFont(ThemeConfig.FONT_H3);
        lblTitle2.setForeground(ThemeConfig.TEXT_PRIMARY);
        lblTitle2.setBorder(new EmptyBorder(0, 0, 2, 0));
        lblTitle2.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblThongTin.setFont(ThemeConfig.FONT_SM);
        lblThongTin.setForeground(ThemeConfig.TEXT_SECONDARY);
        lblThongTin.setBorder(new EmptyBorder(0, 0, 2, 0));
        lblThongTin.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleBlock2.add(lblTitle2);
        titleBlock2.add(Box.createVerticalStrut(3));
        titleBlock2.add(lblThongTin);

        ThemeConfig.styleActionButton(btnInPhieu, Feather.PRINTER, Color.WHITE, ThemeConfig.PRIMARY,
                ThemeConfig.BORDER);
        ThemeConfig.styleActionButton(btnTra, Feather.CHECK_CIRCLE, ThemeConfig.SUCCESS, Color.WHITE,
                ThemeConfig.SUCCESS_HOVER);
        ThemeConfig.styleActionButton(btnMoNhacHan, Feather.MAIL, Color.WHITE, ThemeConfig.TEXT_PRIMARY,
                ThemeConfig.BORDER);
        ThemeConfig.styleActionButton(btnMoViPham, Feather.ALERT_TRIANGLE, Color.WHITE, ThemeConfig.DANGER,
                ThemeConfig.BORDER);

        btnInPhieu.setEnabled(false);
        btnTra.setEnabled(false);

        JPanel actionsCard2 = new JPanel(new ThemeConfig.WrapFlowLayout(FlowLayout.RIGHT, 12, 8));
        actionsCard2.setOpaque(false);
        actionsCard2.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeConfig.BORDER),
                new EmptyBorder(6, 0, 0, 0)));
        actionsCard2.add(btnMoNhacHan);
        actionsCard2.add(btnMoViPham);
        actionsCard2.add(btnInPhieu);
        actionsCard2.add(btnTra);

        headerCard2.add(titleBlock2, BorderLayout.WEST);

        JPanel searchBar = ThemeConfig.createSearchBar(
                txtTimKiemDangMuon, "Mã phiếu, CCCD, tên độc giả, sách...", 275);

        cbLocDangMuon.setFont(ThemeConfig.FONT_BODY);
        cbLocDangMuon.setPreferredSize(new Dimension(190, ThemeConfig.INPUT_HEIGHT));

        JPanel filterBarCard2 = new JPanel(new ThemeConfig.WrapFlowLayout(FlowLayout.LEFT, 12, 8));
        filterBarCard2.setOpaque(false);
        filterBarCard2.add(createFormLabel("Tìm nhanh:"));
        filterBarCard2.add(searchBar);
        filterBarCard2.add(createFormLabel("Lọc trạng thái:"));
        filterBarCard2.add(cbLocDangMuon);

        JPanel topCard2 = new JPanel(new BorderLayout(0, 10));
        topCard2.setOpaque(false);
        topCard2.add(headerCard2, BorderLayout.NORTH);
        topCard2.add(filterBarCard2, BorderLayout.CENTER);
        topCard2.add(actionsCard2, BorderLayout.SOUTH);
        cardTable.add(topCard2, BorderLayout.NORTH);

        ThemeConfig.styleTable(tblDangMuon);
        tblDangMuon.setRowHeight(ThemeConfig.TABLE_ROW_HEIGHT);
        tblDangMuon.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblDangMuon.setRowSorter(sorterDangMuon);

        TableColumnModel colModel = tblDangMuon.getColumnModel();
        colModel.getColumn(0).setCellRenderer(new CenterAlignedCellRenderer());
        colModel.getColumn(1).setCellRenderer(new CenterAlignedCellRenderer());
        colModel.getColumn(2).setCellRenderer(new LeftAlignedCellRenderer());
        colModel.getColumn(3).setCellRenderer(new LeftAlignedCellRenderer());
        colModel.getColumn(4).setCellRenderer(new RightAlignedCellRenderer());
        colModel.getColumn(5).setCellRenderer(new CenterAlignedCellRenderer());
        colModel.getColumn(6).setCellRenderer(new StatusPillCellRenderer());
        colModel.getColumn(7).setCellRenderer(new OverdueCellRenderer());

        JScrollPane scrollTable = new JScrollPane(tblDangMuon);
        scrollTable.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER, 1));
        scrollTable.getViewport().setBackground(ThemeConfig.SURFACE_CARD);
        scrollTable.setPreferredSize(new Dimension(0, 420));
        scrollTable.setMinimumSize(new Dimension(0, 380));
        ThemeConfig.installSaaSDataGrid(
                scrollTable, tblDangMuon, Feather.CHECK_CIRCLE,
                "Hiện không có bản sách nào đang mượn",
                "Mở tab Mượn sách để lập phiếu mới.",
                85, 220, 180, 260, 85, 110, 165, 135);
        cardTable.add(scrollTable, BorderLayout.CENTER);

        cardTable.setBorder(new EmptyBorder(18, 20, 18, 20));
        tabMuonTra.addTab("Mượn sách", ThemeConfig.getIcon(Feather.BOOK_OPEN, 16, ThemeConfig.PRIMARY),
                ThemeConfig.createPageScrollPane(pageContent));
        tabMuonTra.addTab("Trả sách", ThemeConfig.getIcon(Feather.REPEAT, 16, ThemeConfig.PRIMARY), cardTable);
        add(tabMuonTra, BorderLayout.CENTER);

        // ============================================================
        // 3. GẮN SỰ KIỆN
        // ============================================================
        btnChonThe.addActionListener(e -> chonDocGiaTheoMa());
        txtMaThe.addActionListener(e -> chonDocGiaTheoMa());

        btnMuon.addActionListener(e -> muonSach());
        btnInPhieu.addActionListener(e -> xemVaInPhieuDangChon());
        btnTra.addActionListener(e -> traSach());
        btnLamMoi.addActionListener(e -> lamMoi());
        btnMoNhacHan.addActionListener(e -> {
            MainFrame mf = MainFrame.of(this);
            if (mf != null) {
                mf.moNhacHan("Chưa trả quá hạn".equals(cbLocDangMuon.getSelectedItem()));
            }
        });
        btnMoViPham.addActionListener(e -> {
            MainFrame mf = MainFrame.of(this);
            if (mf != null) {
                mf.moViPhamTheoTuKhoa("", "Chưa xử lý");
            }
        });

        cbLocDangMuon.addActionListener(e -> {
            if (!dangCapNhatBoLoc) {
                apDungBoLocDangMuon();
            }
        });
        txtTimKiemDangMuon.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { if (!dangCapNhatBoLoc) apDungBoLocDangMuon(); }
            @Override public void removeUpdate(DocumentEvent e) { if (!dangCapNhatBoLoc) apDungBoLocDangMuon(); }
            @Override public void changedUpdate(DocumentEvent e) { if (!dangCapNhatBoLoc) apDungBoLocDangMuon(); }
        });

        tblDangMuon.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                capNhatNutBang();
            }
        });

        cbDocGia.addActionListener(e -> {
            DocGia dg = (DocGia) cbDocGia.getSelectedItem();
            txtMaThe.setText(dg == null ? "" : dg.getMaDocGia());
            taiVoucherChoDocGiaDangChon();
        });

        cbSach.addActionListener(e -> {
            capNhatTonKhoHienThi();
            capNhatTamTinhPhi();
        });

        spSoLuong.addChangeListener(e -> capNhatTamTinhPhi());
        cbVoucher.addActionListener(e -> capNhatTamTinhPhi());

        lamMoi();

    }

    private JPanel taoOMinStat(String title, JLabel valLabel, Color accent) {
        JPanel box = new JPanel(new BorderLayout(0, 3));
        box.setOpaque(false);
        JLabel lbl = new JLabel(title);
        lbl.setFont(ThemeConfig.FONT_SM);
        lbl.setForeground(ThemeConfig.TEXT_SECONDARY);
        valLabel.setFont(ThemeConfig.FONT_BTN);
        valLabel.setForeground(accent);
        box.add(lbl, BorderLayout.NORTH);
        box.add(valLabel, BorderLayout.CENTER);
        return box;
    }

    private JLabel createFormLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(ThemeConfig.FONT_LABEL);
        lbl.setForeground(ThemeConfig.TEXT_PRIMARY);
        return lbl;
    }

    // ================= TẢI DỮ LIỆU =================

    private static class DuLieuManHinh {
        private final List<DocGia> docGia = new ArrayList<>();
        private final List<Sach> sach = new ArrayList<>();
        private final List<Object[]> dongMuon = new ArrayList<>();
        private final List<DongMuonInfo> chiTietDong = new ArrayList<>();
    }

    public void focusTimKiem() {
        tabMuonTra.setSelectedIndex(1);
        txtTimKiemDangMuon.requestFocusInWindow();
        txtTimKiemDangMuon.selectAll();
    }

    public void chuanBiMuonChoDocGia(int idDocGia, int idSach) {
        this.pendingSelectDocGiaId = idDocGia > 0 ? idDocGia : null;
        this.pendingSelectSachId = idSach > 0 ? idSach : null;
        tabMuonTra.setSelectedIndex(0);
        if (dangXuLy) {
            canLamMoiLai = true;
        } else {
            lamMoi();
        }
    }

    public void moVaLoc(String trangThaiLoc) {
        dangCapNhatBoLoc = true;
        try {
            txtTimKiemDangMuon.setText("");
            if (trangThaiLoc != null && !trangThaiLoc.isBlank()) {
                cbLocDangMuon.setSelectedItem(trangThaiLoc);
            } else {
                cbLocDangMuon.setSelectedIndex(0);
            }
        } finally {
            dangCapNhatBoLoc = false;
        }
        tabMuonTra.setSelectedIndex(1);
        if (dangXuLy) {
            apDungBoLocDangMuon();
            canLamMoiLai = true;
        } else {
            lamMoi();
        }
    }

    public void lamMoi() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::lamMoi);
            return;
        }

        if (dangXuLy) {
            canLamMoiLai = true;
            return;
        }

        DocGia dgCu = (DocGia) cbDocGia.getSelectedItem();
        Sach sachCu = (Sach) cbSach.getSelectedItem();

        int idDocGiaCu = dgCu == null ? -1 : dgCu.getId();
        int idSachCu = sachCu == null ? -1 : sachCu.getId();

        datTrangThaiXuLy(true);
        lblThongTin.setText("Đang tải dữ liệu...");

        new SwingWorker<DuLieuManHinh, Void>() {
            @Override
            protected DuLieuManHinh doInBackground() {
                DuLieuManHinh duLieu = new DuLieuManHinh();
                Map<Integer, String> mapMaDocGia = new HashMap<>();

                for (DocGia dg : new DocGiaDAO().getAll()) {
                    mapMaDocGia.put(dg.getId(), dg.getMaDocGia());
                    if (dg.isTrangThai()) {
                        duLieu.docGia.add(dg);
                    }
                }

                for (Sach sach : new SachDAO().getAll()) {
                    if (sach.conSach()) {
                        duLieu.sach.add(sach);
                    }
                }

                PhieuMuonDAO dao = new PhieuMuonDAO();
                LocalDate ngayXet = LocalDate.now(MUI_GIO);

                for (PhieuMuon pm : dao.layPhieuDangMuon()) {
                    for (ChiTietPhieuMuon ct : dao.layChiTietPhieuMuon(pm.getId())) {

                        if (ct.getNgayTra() != null
                                || !"DANG_MUON".equals(ct.getTrangThai())) {
                            continue;
                        }

                        LocalDate hanTraLocal = ct.getHanTra() == null
                                ? null
                                : new java.sql.Date(ct.getHanTra().getTime()).toLocalDate();
                        int soNgayQuaHan = ct.tinhSoNgayQuaHan(ngayXet);
                        String maTrangThai = ct.xacDinhTrangThai(ngayXet);

                        String tenTrangThai;
                        if ("CHUA_TRA_QUA_HAN".equals(maTrangThai)) {
                            tenTrangThai = "Chưa trả quá hạn";
                        } else if (hanTraLocal != null
                                && !hanTraLocal.isBefore(ngayXet)
                                && ChronoUnit.DAYS.between(ngayXet, hanTraLocal) <= 3) {
                            tenTrangThai = "Sắp đến hạn";
                        } else {
                            tenTrangThai = "Đang mượn";
                        }

                        duLieu.dongMuon.add(new Object[] {
                                ct.getId(),
                                pm.getMaPhieu(),
                                pm.getTenDocGia(),
                                ct.getTenSach(),
                                ct.getSoLuong(),
                                dinhDangNgay(ct.getHanTra()),
                                tenTrangThai,
                                soNgayQuaHan
                        });

                        duLieu.chiTietDong.add(new DongMuonInfo(
                                ct.getId(),
                                pm.getId(),
                                pm.getMaPhieu(),
                                pm.getIdDocGia(),
                                mapMaDocGia.getOrDefault(pm.getIdDocGia(), ""),
                                pm.getTenDocGia(),
                                ct.getIdSach(),
                                ct.getMaSach(),
                                ct.getTenSach(),
                                ct.getSoLuong(),
                                hanTraLocal,
                                soNgayQuaHan,
                                tenTrangThai));
                    }
                }

                return duLieu;
            }

            @Override
            protected void done() {
                try {
                    hienThiDuLieu(get(), idDocGiaCu, idSachCu);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    lblThongTin.setText("Đã dừng tải dữ liệu.");

                } catch (ExecutionException e) {
                    lblThongTin.setText("Không tải được dữ liệu.");

                    hienThiLoi(
                            e.getCause(),
                            "Không tải được dữ liệu. "
                                    + "Hãy kiểm tra kết nối và thử làm mới.");

                } finally {
                    datTrangThaiXuLy(false);
                }

                if (canLamMoiLai) {
                    canLamMoiLai = false;
                    lamMoi();
                }
            }
        }.execute();
    }

    private void hienThiDuLieu(
            DuLieuManHinh duLieu,
            int idDocGiaCu,
            int idSachCu) {
        if (pendingSelectDocGiaId != null) {
            idDocGiaCu = pendingSelectDocGiaId;
            pendingSelectDocGiaId = null;
        }
        if (pendingSelectSachId != null) {
            idSachCu = pendingSelectSachId;
            pendingSelectSachId = null;
        }

        cbDocGia.removeAllItems();

        DocGia docGiaChonLai = null;

        for (DocGia dg : duLieu.docGia) {
            cbDocGia.addItem(dg);

            if (dg.getId() == idDocGiaCu) {
                docGiaChonLai = dg;
            }
        }

        cbDocGia.setSelectedItem(docGiaChonLai);

        cbSach.removeAllItems();

        Sach sachChonLai = null;

        for (Sach sach : duLieu.sach) {
            cbSach.addItem(sach);

            if (sach.getId() == idSachCu) {
                sachChonLai = sach;
            }
        }

        cbSach.setSelectedItem(sachChonLai);

        tableModel.setRowCount(0);
        mapDongMuon.clear();

        for (Object[] dong : duLieu.dongMuon) {
            tableModel.addRow(dong);
        }
        for (DongMuonInfo info : duLieu.chiTietDong) {
            mapDongMuon.put(info.idChiTiet(), info);
        }

        apDungBoLocDangMuon();
        capNhatTonKhoHienThi();
        taiVoucherChoDocGiaDangChon();
        capNhatTamTinhPhi();
    }

    private void apDungBoLocDangMuon() {
        String tuKhoa = ThemeConfig.normalizeText(txtTimKiemDangMuon.getText());
        String locTrangThai = Objects.toString(cbLocDangMuon.getSelectedItem(), "Tất cả đang mượn");

        sorterDangMuon.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                DongMuonInfo info = mapDongMuon.get((Integer) entry.getValue(0));
                String gop = ThemeConfig.normalizeText(
                        (info == null ? "" : info.maDocGia() + " " + info.maSach() + " ") + entry.getStringValue(0) + " "
                                + entry.getStringValue(1) + " "
                                + entry.getStringValue(2) + " "
                                + entry.getStringValue(3));
                boolean khopTuKhoa = tuKhoa.isEmpty() || gop.contains(tuKhoa);

                String trangThai = entry.getStringValue(6);
                boolean khopTrangThai = switch (locTrangThai) {
                    case "Sắp đến hạn (3 ngày)" -> "Sắp đến hạn".equals(trangThai);
                    case "Chưa trả quá hạn" -> "Chưa trả quá hạn".equals(trangThai);
                    case "Trong hạn (> 3 ngày)" -> "Đang mượn".equals(trangThai);
                    default -> true;
                };

                return khopTuKhoa && khopTrangThai;
            }
        });

        lblThongTin.setText(
                "Đang hiển thị " + tblDangMuon.getRowCount() + " / " + tableModel.getRowCount()
                        + " dòng chưa trả. Chọn một dòng rồi bấm Trả sách đã chọn.");
        capNhatNutBang();
    }

    private String dinhDangNgay(java.util.Date ngay) {
        if (ngay == null) {
            return "";
        }

        return new java.sql.Date(ngay.getTime())
                .toLocalDate()
                .toString();
    }

    private void capNhatTonKhoHienThi() {
        Sach sach = (Sach) cbSach.getSelectedItem();

        lblTonKho.setText(
                sach == null
                        ? "Chưa chọn sách."
                        : "Còn: " + sach.getSoLuongCon() + " cuốn • Giá: "
                                + String.format("%,d đ", sach.getGiaThue()));
    }

    private void taiVoucherChoDocGiaDangChon() {
        DocGia dg = (DocGia) cbDocGia.getSelectedItem();
        capNhatCanhBaoDocGia(dg);
        cbVoucher.removeAllItems();
        cbVoucher.addItem(new VoucherOption(null));
        if (dg == null || dg.getId() <= 0) {
            capNhatTamTinhPhi();
            return;
        }
        final int targetIdDocGia = dg.getId();
        new SwingWorker<List<Voucher>, Void>() {
            @Override
            protected List<Voucher> doInBackground() {
                return voucherDAO.getVoucherChuaDung(targetIdDocGia);
            }

            @Override
            protected void done() {
                DocGia current = (DocGia) cbDocGia.getSelectedItem();
                if (current == null || current.getId() != targetIdDocGia) {
                    return;
                }
                try {
                    List<Voucher> ds = get();
                    cbVoucher.removeAllItems();
                    cbVoucher.addItem(new VoucherOption(null));
                    for (Voucher v : ds) {
                        cbVoucher.addItem(new VoucherOption(v));
                    }
                } catch (Exception ignored) {
                }
                capNhatTamTinhPhi();
            }
        }.execute();
    }

    private void capNhatCanhBaoDocGia(DocGia dg) {
        if (dg == null || dg.getDiemThuong() >= 0) {
            pnlCanhBaoDiemAm.setVisible(false);
            return;
        }
        int diem = dg.getDiemThuong();
        if (dg.biTamKhoaDoDiemAm()) {
            pnlCanhBaoDiemAm.setBackground(ThemeConfig.STATUS_ERROR_BG);
            pnlCanhBaoDiemAm.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeConfig.DANGER, 1, true),
                    new EmptyBorder(8, 12, 8, 12)));
            lblCanhBaoDiemAm.setIcon(ThemeConfig.getIcon(Feather.LOCK, 16, ThemeConfig.DANGER));
            lblCanhBaoDiemAm.setForeground(ThemeConfig.STATUS_ERROR_FG);
            lblCanhBaoDiemAm.setText("TẠM KHÓA QUYỀN MƯỢN: Độc giả đang có " + diem
                    + " điểm (< " + DocGia.NGUONG_TAM_KHOA_MUON + " điểm). Cần khắc phục vi phạm trước khi lập phiếu mới.");
        } else {
            pnlCanhBaoDiemAm.setBackground(ThemeConfig.STATUS_WARN_BG);
            pnlCanhBaoDiemAm.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeConfig.STATUS_WARN_FG, 1, true),
                    new EmptyBorder(8, 12, 8, 12)));
            lblCanhBaoDiemAm.setIcon(ThemeConfig.getIcon(Feather.ALERT_TRIANGLE, 16, ThemeConfig.STATUS_WARN_FG));
            lblCanhBaoDiemAm.setForeground(ThemeConfig.STATUS_WARN_FG);
            lblCanhBaoDiemAm.setText("CẢNH BÁO ĐIỂM ÂM (" + diem
                    + " điểm): Độc giả từng có vi phạm / trả muộn. Thủ thư vui lòng nhắc nhở trả sách đúng hạn.");
        }
        pnlCanhBaoDiemAm.setVisible(true);
    }

    private void capNhatTamTinhPhi() {
        Sach sach = (Sach) cbSach.getSelectedItem();
        int soLuong = 1;
        Object val = spSoLuong.getValue();
        if (val instanceof Number n) {
            soLuong = Math.max(1, n.intValue());
        }

        long donGia = sach != null ? Math.max(0L, sach.getGiaThue()) : 0L;
        long tongTien = donGia * soLuong;

        VoucherOption vc = (VoucherOption) cbVoucher.getSelectedItem();
        Voucher voucherChon = vc != null ? vc.voucher() : null;
        long giamGia = voucherChon != null ? voucherChon.tinhSoTienGiam(tongTien) : 0L;
        long thanhToan = Math.max(0L, tongTien - giamGia);
        int diemDuKien = RewardService.tinhDiemDuKienKhiMuon(soLuong);

        lblDonGiaThue.setText(String.format("%,d đ / cuốn", donGia));
        lblTongTienThue.setText(String.format("%,d đ", tongTien));
        lblGiamGiaVoucher.setText(voucherChon != null
                ? String.format("- %,d đ (%s)", giamGia, voucherChon.moTaUuDai())
                : "0 đ");
        lblKhachThanhToan.setText(String.format("%,d đ", thanhToan));
        lblDiemDuKien.setText("+" + diemDuKien + " điểm (+10đ/cuốn)");
    }

    // ================= CHỌN ĐỘC GIẢ =================

    private void chonDocGiaTheoMa() {
        if (dangXuLy) {
            return;
        }

        String ma = txtMaThe.getText().trim();

        if (ma.isEmpty()) {
            ThemeConfig.toastWarning(this, "Hãy nhập mã thẻ độc giả.");
            return;
        }

        for (int i = 0; i < cbDocGia.getItemCount(); i++) {
            DocGia dg = cbDocGia.getItemAt(i);

            if (dg.getMaDocGia().equalsIgnoreCase(ma)) {
                cbDocGia.setSelectedIndex(i);
                return;
            }
        }

        cbDocGia.setSelectedIndex(-1);
        txtMaThe.setText(ma);

        ThemeConfig.toastWarning(this, "Không tìm thấy thẻ đang hoạt động. Hãy kiểm tra mã thẻ hoặc làm mới.");
    }

    // ================= MƯỢN SÁCH =================

    private void muonSach() {
        if (dangXuLy) {
            return;
        }

        DocGia docGia = (DocGia) cbDocGia.getSelectedItem();
        Sach sach = (Sach) cbSach.getSelectedItem();

        if (docGia == null || sach == null || thuThu == null) {
            ThemeConfig.toastWarning(this, "Hãy chọn độc giả và sách trước khi lập phiếu mượn.");
            return;
        }

        if (!txtMaThe.getText().trim()
                .equalsIgnoreCase(docGia.getMaDocGia())) {
            ThemeConfig.toastWarning(this, "Hãy nhấn Chọn thẻ để xác nhận mã thẻ vừa nhập.");
            return;
        }

        try {
            spSoLuong.commitEdit();

            int soLuong = ((Number) spSoLuong.getValue()).intValue();
            if (soLuong <= 0) {
                ThemeConfig.toastWarning(this, "Số lượng mượn phải lớn hơn 0.");
                return;
            }
            if (soLuong > sach.getSoLuongCon()) {
                ThemeConfig.toastWarning(this,
                        "Số lượng mượn (" + soLuong + ") vượt quá số sách còn (" + sach.getSoLuongCon() + ").");
                return;
            }

            LocalDate hanTra = LocalDate.parse(txtHanTra.getText().trim());
            if (hanTra.isBefore(LocalDate.now(MUI_GIO))) {
                ThemeConfig.toastWarning(this, "Hạn trả không được trước ngày hôm nay.");
                return;
            }

            int idDocGia = docGia.getId();
            int idSach = sach.getId();
            int idThuThu = thuThu.getId();
            String maThe = docGia.getMaDocGia();
            String tenDocGia = docGia.getHoTen();
            String tenThuThu = thuThu.getHoTen();
            String tenSach = sach.getTenSach();
            String hanTraStr = hanTra.toString();

            DocGia freshDg = new DocGiaDAO().getById(idDocGia);
            DocGia docGiaXet = freshDg != null ? freshDg : docGia;
            if (docGiaXet.biTamKhoaDoDiemAm()) {
                Object[] lockOpts = { "Mở Quản lý vi phạm", "Đóng" };
                int pick = JOptionPane.showOptionDialog(
                        this,
                        "TẠM KHÓA QUYỀN MƯỢN SÁCH\n\n"
                                + "Độc giả: " + tenDocGia + " (" + maThe + ")\n"
                                + "Điểm tích lũy hiện tại: " + docGiaXet.getDiemThuong() + " điểm (Ngưỡng khóa: < "
                                + DocGia.NGUONG_TAM_KHOA_MUON + " điểm).\n\n"
                                + "Hệ thống tạm khóa quyền mượn mới cho đến khi độc giả khắc phục vi phạm.",
                        "Cảnh báo tạm khóa quyền mượn",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        lockOpts,
                        lockOpts[0]);
                if (pick == 0) {
                    MainFrame mf = MainFrame.of(this);
                    if (mf != null) {
                        mf.moViPhamTheoTuKhoa(tenDocGia, "Chưa xử lý");
                    }
                }
                return;
            }
            if (docGiaXet.getDiemThuong() < 0) {
                int xacNhanDiemAm = JOptionPane.showConfirmDialog(
                        this,
                        "NHẮC NHỞ ĐỘC GIẢ ĐANG CÓ ĐIỂM TÍCH LŨY ÂM\n\n"
                                + "Độc giả: " + tenDocGia + " (" + maThe + ")\n"
                                + "Điểm tích lũy hiện tại: " + docGiaXet.getDiemThuong()
                                + " điểm (Hạng thẻ: " + docGiaXet.getHangThe() + ").\n\n"
                                + "Vui lòng nhắc nhở độc giả trả sách đúng hạn để tích lũy lại điểm uy tín.\n"
                                + "Tiếp tục lập phiếu mượn sách cho độc giả này?",
                        "Cảnh báo điểm tích lũy âm",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (xacNhanDiemAm != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            VoucherOption voucherChon = (VoucherOption) cbVoucher.getSelectedItem();
            Integer idVoucherChon = (voucherChon != null && voucherChon.voucher() != null)
                    ? voucherChon.voucher().getId()
                    : null;
            PhieuMuonDAO.KetQuaLapPhieuMuon[] ketQuaMuonRef = new PhieuMuonDAO.KetQuaLapPhieuMuon[1];

            // FIFO availability is checked atomically in PhieuMuonDAO.
            // A stale UI warning must never authorize borrowing somebody else's held copy.

            chayNghiepVu(
                    () -> {
                        PhieuMuonDAO.KetQuaLapPhieuMuon kq = muonTraService.muonSachCoThanhToan(
                                idDocGia,
                                idThuThu,
                                idSach,
                                soLuong,
                                hanTra,
                                idVoucherChon);
                        ketQuaMuonRef[0] = kq;
                        boolean ok = kq != null;
                        return ok;
                    },
                    "Lập phiếu mượn thành công (+" + RewardService.tinhDiemDuKienKhiMuon(soLuong) + " điểm tích lũy).",
                    () -> {
                        PhieuMuonDAO.KetQuaLapPhieuMuon kq = ketQuaMuonRef[0];
                        String maPhieuMoi = kq.maPhieu();
                        long donGiaThue = kq.donGiaThue();
                        long tongTienThue = kq.tongTienThue();
                        String maVoucher = kq.maVoucherApDung();
                        long giamGia = kq.giamGiaVoucher();
                        long thucThu = kq.thucThu();
                        int diemCong = kq.diemCong();

                        String ngayLap = kq.ngayLap().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                        ReceiptDialog.showPhieuMuon(
                                MuonTraSachPanel.this,
                                maPhieuMoi,
                                ngayLap,
                                maThe,
                                tenDocGia,
                                tenThuThu,
                                tenSach,
                                soLuong,
                                hanTraStr,
                                "Đang mượn",
                                donGiaThue,
                                tongTienThue,
                                maVoucher,
                                giamGia,
                                thucThu,
                                diemCong);
                    });

        } catch (DateTimeParseException | ParseException e) {
            ThemeConfig.toastWarning(this, "Kiểm tra số lượng và hạn trả theo định dạng yyyy-MM-dd.");
        }
    }

    private void xemVaInPhieuDangChon() {
        int dongChon = tblDangMuon.getSelectedRow();
        if (dongChon < 0) {
            ThemeConfig.toastWarning(this, "Hãy chọn một dòng đang mượn để xem & in phiếu.");
            return;
        }

        int dongModel = tblDangMuon.convertRowIndexToModel(dongChon);
        int idChiTiet = ((Number) tableModel.getValueAt(dongModel, 0)).intValue();
        String trangThai = Objects.toString(tableModel.getValueAt(dongModel, 6), "Đang mượn");
        btnInPhieu.setEnabled(false);
        new SwingWorker<ChungTuMuonDAO.ChungTuMuon, Void>() {
            @Override
            protected ChungTuMuonDAO.ChungTuMuon doInBackground() throws Exception {
                return new ChungTuMuonDAO().layTheoDongMuon(idChiTiet);
            }

            @Override
            protected void done() {
                btnInPhieu.setEnabled(tblDangMuon.getSelectedRow() >= 0);
                try {
                    ChungTuMuonDAO.ChungTuMuon phieu = get();
                    if (phieu == null) {
                        ThemeConfig.toastWarning(MuonTraSachPanel.this, "Không tìm thấy phiếu mượn.");
                        return;
                    }
                    String ngayLap = phieu.ngayLap() == null ? null
                            : phieu.ngayLap().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                    ReceiptDialog.showPhieuMuon(
                            MuonTraSachPanel.this, phieu.maPhieu(), ngayLap, phieu.maThe(),
                            phieu.tenDocGia(), phieu.tenThuThu(), phieu.tenSach(), phieu.soLuong(),
                            Objects.toString(phieu.hanTra(), ""), trangThai, phieu.donGiaThue(),
                            phieu.tongTienThue(), phieu.maVoucher(), phieu.giamGia(),
                            phieu.phiSauGiam(), phieu.diemCong());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    System.err.println("Lỗi tải phiếu mượn: " + e.getCause().getMessage());
                    ThemeConfig.toastError(MuonTraSachPanel.this, "Không tải được dữ liệu để in phiếu.");
                }
            }
        }.execute();
    }

    // ================= TRẢ SÁCH / CHỐT MẤT =================

    private void traSach() {
        if (dangXuLy) {
            return;
        }

        int dongChon = tblDangMuon.getSelectedRow();

        if (dongChon < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Hãy chọn dòng sách cần trả hoặc chốt mất.");
            return;
        }

        int dongModel = tblDangMuon.convertRowIndexToModel(dongChon);

        int idChiTiet = ((Number) tableModel.getValueAt(dongModel, 0))
                .intValue();

        int soLuong = ((Number) tableModel.getValueAt(dongModel, 4))
                .intValue();

        String maPhieu = Objects.toString(tableModel.getValueAt(dongModel, 1), "");
        String tenDocGia = Objects.toString(tableModel.getValueAt(dongModel, 2), "");
        String tenSach = String.valueOf(tableModel.getValueAt(dongModel, 3));
        String hanTraText = Objects.toString(tableModel.getValueAt(dongModel, 5), "");

        DongMuonInfo info = mapDongMuon.get(idChiTiet);
        if (info == null) {
            ThemeConfig.toastWarning(this, "Hãy làm mới và chọn lại dòng cần trả.");
            return;
        }
        LocalDate hanTraLocal = info.hanTra();
        if (hanTraLocal == null && !hanTraText.isBlank()) {
            try {
                hanTraLocal = LocalDate.parse(hanTraText);
            } catch (DateTimeParseException ignored) {
            }
        }
        LocalDate ngayTraThucTe = LocalDate.now(MUI_GIO);
        int soNgayQuaHan = hanTraLocal != null
                ? (int) Math.max(0, ChronoUnit.DAYS.between(hanTraLocal, ngayTraThucTe))
                : (tableModel.getValueAt(dongModel, 7) instanceof Number n ? Math.max(0, n.intValue()) : 0);

        List<DatTruocChiTiet> hangDoiHienTai = info.idSach() > 0
                ? datTruocDAO.layHangDoiHoatDongTheoSach(info.idSach())
                : List.of();

        JComboBox<String> cbTinhTrang = new JComboBox<>(
                new String[] {
                        "Bình thường",
                        "Hỏng - loại khỏi kho",
                        "Mất - chốt mất"
                });

        JTextArea txtGhiChu = new JTextArea(4, 34);
        txtGhiChu.setLineWrap(true);
        txtGhiChu.setWrapStyleWord(true);

        // Banner cảnh báo quá hạn & mức phạt dự kiến
        JPanel pnlCanhBaoPhat = new JPanel(new BorderLayout(0, 8));
        pnlCanhBaoPhat.setBackground(ThemeConfig.STATUS_ERROR_BG);
        pnlCanhBaoPhat.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.DANGER, 1, true),
                new EmptyBorder(10, 12, 10, 12)));

        JLabel lblCanhBaoPhat = new JLabel();
        lblCanhBaoPhat.setIcon(ThemeConfig.getIcon(Feather.ALERT_TRIANGLE, 18, ThemeConfig.DANGER));
        lblCanhBaoPhat.setIconTextGap(10);
        lblCanhBaoPhat.setFont(ThemeConfig.FONT_SM);
        lblCanhBaoPhat.setForeground(ThemeConfig.STATUS_ERROR_FG);

        JCheckBox chkXacNhanViPham = new JCheckBox(
                "Xác nhận tự động tạo bản ghi vi phạm sang Quản lý vi phạm", true);
        chkXacNhanViPham.setOpaque(false);
        chkXacNhanViPham.setFont(ThemeConfig.FONT_LABEL);
        chkXacNhanViPham.setForeground(ThemeConfig.STATUS_ERROR_FG);

        pnlCanhBaoPhat.add(lblCanhBaoPhat, BorderLayout.CENTER);
        pnlCanhBaoPhat.add(chkXacNhanViPham, BorderLayout.SOUTH);

        // Banner nhắc hàng đợi đặt trước FIFO (nếu có người đang chờ sách này)
        JPanel pnlThongBaoDatTruoc = new JPanel(new BorderLayout(8, 4));
        pnlThongBaoDatTruoc.setBackground(ThemeConfig.STATUS_WARN_BG);
        pnlThongBaoDatTruoc.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.STATUS_WARN_FG, 1, true),
                new EmptyBorder(10, 12, 10, 12)));

        JLabel lblHangDoiDatTruoc = new JLabel();
        lblHangDoiDatTruoc.setIcon(ThemeConfig.getIcon(Feather.BELL, 18, ThemeConfig.STATUS_WARN_FG));
        lblHangDoiDatTruoc.setIconTextGap(10);
        lblHangDoiDatTruoc.setFont(ThemeConfig.FONT_SM);
        lblHangDoiDatTruoc.setForeground(ThemeConfig.STATUS_WARN_FG);
        if (!hangDoiHienTai.isEmpty()) {
            DatTruocChiTiet dauHang = hangDoiHienTai.get(0);
            lblHangDoiDatTruoc.setText("<html><b>LƯU Ý HÀNG ĐỢI ĐẶT TRƯỚC:</b> Cuốn sách này đang có <b>"
                    + hangDoiHienTai.size() + "</b> lượt chờ giữ sách.<br/>Người đứng đầu hàng đợi (FIFO): <b>"
                    + Objects.toString(dauHang.getTenDocGia(), "Độc giả #" + dauHang.getIdDocGia())
                    + "</b> (" + Objects.toString(dauHang.getMaDocGia(), "—") + ") — Mã đặt: <b>"
                    + Objects.toString(dauHang.getMaDatTruoc(), "") + "</b>.</html>");
        }
        pnlThongBaoDatTruoc.add(lblHangDoiDatTruoc, BorderLayout.CENTER);

        JPanel pnlDialogBody = new JPanel();
        pnlDialogBody.setLayout(new BoxLayout(pnlDialogBody, BoxLayout.Y_AXIS));
        pnlDialogBody.setBackground(ThemeConfig.SURFACE_CARD);
        pnlDialogBody.setBorder(new EmptyBorder(6, 6, 6, 6));

        JLabel lblThongTinDong = new JLabel("<html><b>Phiếu:</b> " + maPhieu
                + " &nbsp;|&nbsp; <b>Độc giả:</b> " + tenDocGia + "<br/>"
                + "<b>Sách:</b> " + tenSach + " (<b>" + soLuong + " bản</b>)<br/>"
                + "<b>Hạn trả:</b> " + (hanTraLocal != null ? hanTraLocal : hanTraText)
                + " &nbsp;|&nbsp; <b>Ngày trả thực tế:</b> " + ngayTraThucTe + "</html>");
        lblThongTinDong.setFont(ThemeConfig.FONT_BODY);
        lblThongTinDong.setForeground(ThemeConfig.TEXT_PRIMARY);
        lblThongTinDong.setAlignmentX(Component.LEFT_ALIGNMENT);

        pnlCanhBaoPhat.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlThongBaoDatTruoc.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel pnlForm = new JPanel(new GridBagLayout());
        pnlForm.setOpaque(false);
        pnlForm.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints fgbc = new GridBagConstraints();
        fgbc.gridx = 0;
        fgbc.gridy = 0;
        fgbc.weightx = 1.0;
        fgbc.fill = GridBagConstraints.HORIZONTAL;
        fgbc.insets = new Insets(4, 0, 4, 0);
        pnlForm.add(createFormLabel("Tình trạng sách khi nhận:"), fgbc);
        fgbc.gridy = 1;
        pnlForm.add(cbTinhTrang, fgbc);
        fgbc.gridy = 2;
        pnlForm.add(createFormLabel("Ghi chú (bắt buộc với hỏng/mất, tối đa 2.000 ký tự):"), fgbc);
        fgbc.gridy = 3;
        pnlForm.add(new JScrollPane(txtGhiChu), fgbc);

        pnlDialogBody.add(lblThongTinDong);
        pnlDialogBody.add(Box.createVerticalStrut(10));
        pnlDialogBody.add(pnlCanhBaoPhat);
        pnlDialogBody.add(Box.createVerticalStrut(8));
        pnlDialogBody.add(pnlThongBaoDatTruoc);
        pnlDialogBody.add(Box.createVerticalStrut(8));
        pnlDialogBody.add(pnlForm);

        Runnable capNhatCanhBao = () -> {
            String tinhTrangTam = switch (cbTinhTrang.getSelectedIndex()) {
                case 1 -> "LAM_HONG";
                case 2 -> "LAM_MAT";
                default -> "BINH_THUONG";
            };
            boolean coViPham = soNgayQuaHan > 0 || !"BINH_THUONG".equals(tinhTrangTam);
            long tongBoiThuong = QuanLyViPhamPanel.tinhTongPhatDuKienKhiTra(tinhTrangTam, soNgayQuaHan);
            int diemTruTreHan = soNgayQuaHan > 0 ? soNgayQuaHan * ViPhamDAO.DIEM_TRU_MOI_NGAY_QUA_HAN : 0;
            int diemTruHongMat = !"BINH_THUONG".equals(tinhTrangTam) ? ViPhamDAO.DIEM_TRU_HONG_MAT : 0;
            int tongDiemTru = diemTruTreHan + diemTruHongMat;

            if (coViPham) {
                StringBuilder chiTietPhat = new StringBuilder("<html><b>CẢNH BÁO VI PHẠM &amp; XỬ LÝ ĐIỂM TÍCH LŨY:</b><br/>");
                if (soNgayQuaHan > 0) {
                    chiTietPhat.append("• Trả muộn <b>").append(soNgayQuaHan)
                            .append(" ngày</b>: Trừ <b>-").append(diemTruTreHan)
                            .append(" điểm tích lũy</b> (-").append(ViPhamDAO.DIEM_TRU_MOI_NGAY_QUA_HAN)
                            .append(" điểm/ngày) • Phí phạt tiền mặt: <b>0 VNĐ</b><br/>");
                }
                if ("LAM_HONG".equals(tinhTrangTam)) {
                    chiTietPhat.append("• Làm hỏng sách: Bồi thường <b>50,000 VNĐ</b> &amp; trừ <b>-")
                            .append(ViPhamDAO.DIEM_TRU_HONG_MAT).append(" điểm</b> uy tín<br/>");
                } else if ("LAM_MAT".equals(tinhTrangTam)) {
                    chiTietPhat.append("• Làm mất sách: Bồi thường <b>100,000 VNĐ</b> &amp; trừ <b>-")
                            .append(ViPhamDAO.DIEM_TRU_HONG_MAT).append(" điểm</b> uy tín<br/>");
                }
                chiTietPhat.append("• <b>Tổng điểm trừ dự kiến: -").append(tongDiemTru).append(" điểm</b>");
                if (tongBoiThuong > 0) {
                    chiTietPhat.append(" &nbsp;|&nbsp; <b>Bồi thường sách: ")
                            .append(String.format("%,d VNĐ", tongBoiThuong)).append("</b>");
                }
                chiTietPhat.append("</html>");
                lblCanhBaoPhat.setText(chiTietPhat.toString());
            }

            pnlCanhBaoPhat.setVisible(coViPham);
            pnlThongBaoDatTruoc.setVisible("BINH_THUONG".equals(tinhTrangTam) && !hangDoiHienTai.isEmpty());
            pnlDialogBody.revalidate();
            pnlDialogBody.repaint();
            Window w = SwingUtilities.getWindowAncestor(pnlDialogBody);
            if (w != null) {
                w.pack();
            }
        };

        cbTinhTrang.addActionListener(e -> capNhatCanhBao.run());
        capNhatCanhBao.run();

        while (true) {
            int chon = JOptionPane.showConfirmDialog(
                    this,
                    pnlDialogBody,
                    "Trả sách / chốt mất",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);

            if (chon != JOptionPane.OK_OPTION) {
                return;
            }

            String tinhTrang = switch (cbTinhTrang.getSelectedIndex()) {
                case 1 -> "LAM_HONG";
                case 2 -> "LAM_MAT";
                default -> "BINH_THUONG";
            };

            String ghiChu = txtGhiChu.getText().trim();

            if (ghiChu.length() > 2000
                    || (!"BINH_THUONG".equals(tinhTrang)
                            && ghiChu.isEmpty())) {
                JOptionPane.showMessageDialog(
                        this,
                        "Ghi chú tối đa 2.000 ký tự "
                                + "và bắt buộc khi hỏng/mất.");
                continue;
            }

            if (pnlCanhBaoPhat.isVisible() && !chkXacNhanViPham.isSelected()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Vui lòng tích xác nhận ghi nhận vi phạm để đồng bộ sang Quản lý vi phạm.",
                        "Cần xác nhận ghi nhận vi phạm",
                        JOptionPane.WARNING_MESSAGE);
                continue;
            }

            boolean binhThuong = "BINH_THUONG".equals(tinhTrang);
            long boiThuongDuKien = QuanLyViPhamPanel.tinhTongPhatDuKienKhiTra(tinhTrang, soNgayQuaHan);
            int diemTruDuKien = (soNgayQuaHan > 0 ? soNgayQuaHan * ViPhamDAO.DIEM_TRU_MOI_NGAY_QUA_HAN : 0)
                    + (binhThuong ? 0 : ViPhamDAO.DIEM_TRU_HONG_MAT);

            String tomTat = "Phiếu: " + maPhieu + "  |  Độc giả: " + tenDocGia
                    + "\nSách: " + tenSach
                    + "\nSố bản xử lý: " + soLuong
                    + "\nHạn trả: " + (hanTraLocal != null ? hanTraLocal : hanTraText)
                    + "  |  Ngày trả thực tế: " + ngayTraThucTe
                    + (soNgayQuaHan > 0 ? "\nSố ngày quá hạn: " + soNgayQuaHan + " ngày (Phạt tiền mặt: 0 VNĐ)" : "")
                    + "\nTình trạng: " + cbTinhTrang.getSelectedItem()
                    + (diemTruDuKien > 0
                            ? "\nĐiểm tích lũy trừ tự động: -" + diemTruDuKien + " điểm"
                            : "")
                    + (boiThuongDuKien > 0
                            ? "\nTiền bồi thường sách hư hỏng/mất: " + String.format("%,d VNĐ", boiThuongDuKien)
                            : "")
                    + "\nSố bản cộng lại kho cho mượn: " + (binhThuong ? soLuong : 0)
                    + "\nSố bản loại khỏi tổng kho: " + (binhThuong ? 0 : soLuong)
                    + "\nGhi chú: " + (ghiChu.isEmpty() ? "Không có" : ghiChu);

            JTextArea txtXacNhan = new JTextArea(tomTat, 11, 46);
            txtXacNhan.setEditable(false);
            txtXacNhan.setLineWrap(true);
            txtXacNhan.setWrapStyleWord(true);
            txtXacNhan.setCaretPosition(0);

            int xacNhan = JOptionPane.showConfirmDialog(
                    this,
                    new JScrollPane(txtXacNhan),
                    "Xác nhận xử lý toàn bộ dòng",
                    JOptionPane.YES_NO_OPTION);

            if (xacNhan != JOptionPane.YES_OPTION) {
                return;
            }

            chayNghiepVuTraSach(info, soLuong, tinhTrang, ghiChu);
            return;
        }
    }

    private void chayNghiepVuTraSach(
            DongMuonInfo info,
            int soLuong,
            String tinhTrang,
            String ghiChu) {
        if (dangXuLy) {
            return;
        }

        datTrangThaiXuLy(true);
        lblThongTin.setText("Đang xử lý trả sách và kiểm tra liên kết...");

        new SwingWorker<KetQuaTraSach, Void>() {
            @Override
            protected KetQuaTraSach doInBackground() throws Exception {
                boolean thanhCong = muonTraService.traSach(
                        info.idChiTiet(),
                        soLuong,
                        tinhTrang,
                        ghiChu);
                if (!thanhCong) {
                    return new KetQuaTraSach(false, List.of(), null);
                }

                try {
                    new ThongBaoDAO().huyNhacHanKhiDaTra(info.idChiTiet());
                } catch (Exception ex) {
                    // Return has committed; the reminder worker also rechecks the live loan.
                    System.err.println("Đã trả sách; cập nhật nhắc hạn sẽ được kiểm tra lại ở lượt nền.");
                }
                List<ViPham> dsViPham = new ViPhamDAO().layViPhamTheoChiTiet(info.idChiTiet());
                DatTruocService.KetQuaHangDoiTraSach ketQuaHangDoi = "BINH_THUONG".equals(tinhTrang) && info.idSach() > 0
                        ? datTruocService.layTinhTrangHangDoi(info.idSach(), soLuong)
                        : null;

                return new KetQuaTraSach(true, dsViPham, ketQuaHangDoi);
            }

            @Override
            protected void done() {
                boolean canLamMoi = false;
                KetQuaTraSach ketQua = null;

                try {
                    ketQua = get();
                    canLamMoi = true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    lblThongTin.setText("Đã dừng thao tác.");
                } catch (ExecutionException e) {
                    Throwable nguyenNhan = e.getCause();
                    canLamMoi = !(nguyenNhan instanceof DuLieuKhongHopLeException);
                    lblThongTin.setText("Thao tác chưa hoàn thành.");
                    hienThiLoi(
                            nguyenNhan,
                            "Có lỗi khi xử lý trả sách. Hãy kiểm tra kết nối và thử lại.");
                } finally {
                    datTrangThaiXuLy(false);
                }

                if (canLamMoi) {
                    lamMoi();
                    MainFrame.refreshMainDashboard(MuonTraSachPanel.this);
                }

                if (ketQua == null) {
                    return;
                }

                if (!ketQua.thanhCong()) {
                    JOptionPane.showMessageDialog(
                            MuonTraSachPanel.this,
                            "Không xử lý được. Hãy làm mới danh sách và kiểm tra lại.");
                    return;
                }

                if (ketQua.ketQuaHangDoi() != null && ketQua.ketQuaHangDoi().coNguoiCanGiuSach()) {
                    hienThiThongBaoGiuSachFifo(info, ketQua.ketQuaHangDoi(), ketQua.dsViPhamMoi());
                } else if (!ketQua.dsViPhamMoi().isEmpty()) {
                    hienThiKetQuaViPhamSauTra(info, ketQua.dsViPhamMoi(), tinhTrang);
                } else {
                    JOptionPane.showMessageDialog(
                            MuonTraSachPanel.this,
                            "LAM_MAT".equals(tinhTrang) ? "Đã chốt mất sách." : "Đã nhận sách trả.",
                            "Hoàn tất trả sách",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }.execute();
    }

    private void hienThiThongBaoGiuSachFifo(
            DongMuonInfo info,
            DatTruocService.KetQuaHangDoiTraSach ketQuaHangDoi,
            List<ViPham> dsViPham) {
        DatTruocChiTiet uuTien = ketQuaHangDoi.nguoiUuTienDauTien();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(ThemeConfig.SURFACE_CARD);
        root.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel banner = new JPanel(new BorderLayout(10, 4));
        banner.setBackground(ThemeConfig.STATUS_WARN_BG);
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.STATUS_WARN_FG, 1, true),
                new EmptyBorder(12, 14, 12, 14)));
        JLabel lblBanner = new JLabel();
        lblBanner.setIcon(ThemeConfig.getIcon(Feather.BELL, 22, ThemeConfig.STATUS_WARN_FG));
        lblBanner.setIconTextGap(12);
        lblBanner.setFont(ThemeConfig.FONT_BODY);
        lblBanner.setForeground(ThemeConfig.STATUS_WARN_FG);
        lblBanner.setText("<html><b>CẦN GIỮ SÁCH TẠI QUẦY CHO NGƯỜI ĐẶT TRƯỚC (FIFO)</b><br/>"
                + "Sách vừa trả: <b>" + info.tenSach() + "</b>"
                + (ketQuaHangDoi.maSach() != null && !ketQuaHangDoi.maSach().isBlank()
                        ? " [" + ketQuaHangDoi.maSach() + "]"
                        : "")
                + "</html>");
        banner.add(lblBanner, BorderLayout.CENTER);
        root.add(banner, BorderLayout.NORTH);

        StringBuilder sb = new StringBuilder();
        if (uuTien != null) {
            sb.append("NGƯỜI ĐỨNG ĐẦU HÀNG ĐỢI ĐƯỢC ƯU TIÊN NHẬN SÁCH:\n");
            sb.append("  • Độc giả: ").append(Objects.toString(uuTien.getTenDocGia(), "Độc giả #" + uuTien.getIdDocGia()))
                    .append(" (Mã thẻ: ").append(Objects.toString(uuTien.getMaDocGia(), "—")).append(")\n");
            sb.append("  • Mã đặt trước: ").append(Objects.toString(uuTien.getMaDatTruoc(), "—"))
                    .append("  |  Trạng thái: Đang giữ sách\n");
            sb.append("  • SĐT: ").append(Objects.toString(uuTien.getSoDienThoai(), "—"))
                    .append("  |  Email: ").append(Objects.toString(uuTien.getEmailDocGia(), "—")).append("\n");
            sb.append("  • Hạn giữ sách đến: ").append(
                    uuTien.getHanNhanSach() != null ? sdf.format(uuTien.getHanNhanSach()) : "24 giờ tới").append("\n");
        }
        List<DatTruocChiTiet> dangGiu = ketQuaHangDoi.danhSachDangGiuHienTai();
        if (dangGiu.size() > 1) {
            sb.append("\nCÁC LƯỢT ĐANG GIỮ SÁCH (").append(dangGiu.size()).append(" bản):\n");
            for (int i = 0; i < dangGiu.size(); i++) {
                DatTruocChiTiet dt = dangGiu.get(i);
                sb.append("  ").append(i + 1).append(". ")
                        .append(Objects.toString(dt.getTenDocGia(), "Độc giả #" + dt.getIdDocGia()))
                        .append(" (").append(Objects.toString(dt.getMaDocGia(), "—")).append(") - ")
                        .append(dt.getMaDatTruoc()).append("\n");
            }
        }
        if (ketQuaHangDoi.soLuongConChoTiep() > 0) {
            sb.append("\nSố lượt vẫn đang xếp hàng chờ tiếp: ").append(ketQuaHangDoi.soLuongConChoTiep()).append(" lượt.\n");
        }
        if (!dsViPham.isEmpty()) {
            sb.append("\nVI PHẠM ĐÃ TỰ ĐỘNG GHI NHẬN TỪ LẦN TRẢ NÀY:\n");
            for (ViPham vp : dsViPham) {
                long tien = QuanLyViPhamPanel.tinhTienPhatThamChieu(vp.getLoaiViPham(), vp.getSoNgayQuaHan());
                int diemTru = QuanLyViPhamPanel.tinhDiemTruThamChieu(vp.getLoaiViPham(), vp.getSoNgayQuaHan());
                sb.append("  • ").append(vp.getMaViPham()).append(" [").append(vp.getLoaiViPham()).append("]")
                        .append(vp.getSoNgayQuaHan() > 0 ? " - Quá hạn " + vp.getSoNgayQuaHan() + " ngày" : "")
                        .append(" - Trừ: -").append(diemTru).append(" điểm")
                        .append(tien > 0 ? " | Bồi thường: " + String.format("%,d VNĐ", tien) : " (Không phạt tiền mặt)")
                        .append("\n");
            }
        }

        JTextArea txtInfo = new JTextArea(sb.toString(), 11, 52);
        txtInfo.setEditable(false);
        txtInfo.setLineWrap(true);
        txtInfo.setWrapStyleWord(true);
        txtInfo.setFont(ThemeConfig.FONT_BODY);
        txtInfo.setBorder(new EmptyBorder(8, 10, 8, 10));
        root.add(new JScrollPane(txtInfo), BorderLayout.CENTER);

        List<String> options = new ArrayList<>();
        options.add("Lập phiếu mượn cho người đầu hàng");
        options.add("Mở hàng đợi Đặt trước");
        if (!dsViPham.isEmpty()) {
            options.add("Mở Quản lý vi phạm");
        }
        options.add("Đã giữ sách tại quầy");

        int luaChon = JOptionPane.showOptionDialog(
                this,
                root,
                "Giữ sách cho độc giả đặt trước (FIFO)",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options.toArray(),
                options.get(0));

        if (luaChon >= 0 && luaChon < options.size()) {
            String chon = options.get(luaChon);
            MainFrame mf = MainFrame.of(this);
            if ("Lập phiếu mượn cho người đầu hàng".equals(chon) && uuTien != null) {
                chuanBiMuonChoDocGia(uuTien.getIdDocGia(), info.idSach());
            } else if ("Mở hàng đợi Đặt trước".equals(chon) && mf != null) {
                mf.moDatTruocHangDoi(info.tenSach());
            } else if ("Mở Quản lý vi phạm".equals(chon) && mf != null) {
                mf.moViPhamTheoTuKhoa(info.maPhieu(), "Chưa xử lý");
            }
        }
    }

    private void hienThiKetQuaViPhamSauTra(
            DongMuonInfo info,
            List<ViPham> dsViPham,
            String tinhTrang) {
        StringBuilder sb = new StringBuilder();
        sb.append("LAM_MAT".equals(tinhTrang) ? "Đã chốt mất sách" : "Đã nhận sách trả")
                .append(" và tự động đồng bộ vi phạm sang Quản lý vi phạm:\n\n");
        for (ViPham vp : dsViPham) {
            long tien = QuanLyViPhamPanel.tinhTienPhatThamChieu(vp.getLoaiViPham(), vp.getSoNgayQuaHan());
            int diemTru = QuanLyViPhamPanel.tinhDiemTruThamChieu(vp.getLoaiViPham(), vp.getSoNgayQuaHan());
            sb.append("• Mã vi phạm: ").append(vp.getMaViPham())
                    .append("\n  Loại: ").append(vp.getLoaiViPham())
                    .append(vp.getSoNgayQuaHan() > 0 ? " (" + vp.getSoNgayQuaHan() + " ngày quá hạn)" : "")
                    .append("\n  Điểm tích lũy đã trừ: -").append(diemTru).append(" điểm");
            if (tien > 0) {
                sb.append("\n  Bồi thường sách hư hỏng/mất: ").append(String.format("%,d VNĐ", tien));
            } else {
                sb.append("\n  Phí phạt tiền mặt: 0 VNĐ (Áp dụng chính sách trừ điểm)");
            }
            sb.append("\n\n");
        }

        Object[] luaChon = { "Mở Quản lý vi phạm", "Đóng" };
        int chon = JOptionPane.showOptionDialog(
                this,
                sb.toString().trim(),
                "Đã ghi nhận vi phạm khi trả sách",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                luaChon,
                luaChon[0]);

        if (chon == 0) {
            MainFrame mf = MainFrame.of(this);
            if (mf != null) {
                mf.moViPhamTheoTuKhoa(info.maPhieu(), "Chưa xử lý");
            }
        }
    }

    // ================= CHẠY NGHIỆP VỤ NỀN =================

    private void chayNghiepVu(
            Callable<Boolean> congViec,
            String thongBaoThanhCong,
            Runnable sauThanhCong) {
        if (dangXuLy) {
            return;
        }

        datTrangThaiXuLy(true);
        lblThongTin.setText("Đang xử lý...");

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return congViec.call();
            }

            @Override
            protected void done() {
                boolean canLamMoi = false;
                boolean thanhCong = false;

                try {
                    thanhCong = Boolean.TRUE.equals(get());

                    canLamMoi = true;

                    if (thanhCong) {
                        ThemeConfig.toastSuccess(MuonTraSachPanel.this, thongBaoThanhCong);
                    } else {
                        ThemeConfig.toastWarning(MuonTraSachPanel.this,
                                "Không xử lý được. Hãy làm mới danh sách và kiểm tra lại.");
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    lblThongTin.setText("Đã dừng thao tác.");

                } catch (ExecutionException e) {
                    Throwable nguyenNhan = e.getCause();

                    canLamMoi = !(nguyenNhan instanceof DuLieuKhongHopLeException);

                    lblThongTin.setText("Thao tác chưa hoàn thành.");

                    hienThiLoi(
                            nguyenNhan,
                            "Có lỗi khi xử lý. "
                                    + "Hãy kiểm tra kết nối và thử lại.");

                } finally {
                    datTrangThaiXuLy(false);
                }

                if (canLamMoi) {
                    lamMoi();
                    MainFrame.refreshMainDashboard(MuonTraSachPanel.this);
                }
                if (thanhCong && sauThanhCong != null) {
                    sauThanhCong.run();
                }
            }
        }.execute();
    }

    private void capNhatNutBang() {
        boolean coDong = !dangXuLy && tblDangMuon.getSelectedRow() >= 0;
        btnTra.setEnabled(coDong);
        btnInPhieu.setEnabled(coDong);
    }

    private void datTrangThaiXuLy(boolean ban) {
        dangXuLy = ban;

        cbDocGia.setEnabled(!ban);
        cbSach.setEnabled(!ban);
        cbVoucher.setEnabled(!ban);
        txtMaThe.setEnabled(!ban);
        spSoLuong.setEnabled(!ban);
        txtHanTra.setEnabled(!ban);

        btnChonThe.setEnabled(!ban);
        btnMuon.setEnabled(!ban);
        btnLamMoi.setEnabled(!ban);
        btnMoNhacHan.setEnabled(!ban);
        btnMoViPham.setEnabled(!ban);
        txtTimKiemDangMuon.setEnabled(!ban);
        cbLocDangMuon.setEnabled(!ban);
        capNhatNutBang();

        tblDangMuon.setEnabled(!ban);

        setCursor(
                ban
                        ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                        : Cursor.getDefaultCursor());
    }

    private void hienThiLoi(
            Throwable loi,
            String thongBaoChung) {
        String thongBao = thongBaoChung;

        if (loi instanceof DuLieuKhongHopLeException) {
            thongBao = loi.getMessage();
        } else if (loi != null) {
            loi.printStackTrace();
        }

        ThemeConfig.toastWarning(this, thongBao);
    }

    // ================================================================
    // RENDERERS CHO BẢNG DỮ LIỆU
    // ================================================================

    private static class LeftAlignedCellRenderer extends DefaultTableCellRenderer {
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

    private static class CenterAlignedCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(ThemeConfig.FONT_BODY);
            setHorizontalAlignment(SwingConstants.CENTER);
            if (!isSelected) {
                setBackground(row % 2 == 0 ? ThemeConfig.snap(ThemeConfig.SURFACE_CARD) : ThemeConfig.snap(ThemeConfig.BG_PAGE));
                setForeground(ThemeConfig.snap(ThemeConfig.TEXT_PRIMARY));
            }
            return this;
        }
    }

    private static class RightAlignedCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(ThemeConfig.FONT_BODY);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            setHorizontalAlignment(SwingConstants.RIGHT);
            if (!isSelected) {
                setBackground(row % 2 == 0 ? ThemeConfig.snap(ThemeConfig.SURFACE_CARD) : ThemeConfig.snap(ThemeConfig.BG_PAGE));
                setForeground(ThemeConfig.snap(ThemeConfig.TEXT_PRIMARY));
            }
            return this;
        }
    }

    private static class OverdueCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            setHorizontalAlignment(SwingConstants.RIGHT);

            int days = 0;
            if (value instanceof Number num) {
                days = num.intValue();
            }

            if (!isSelected) {
                setBackground(row % 2 == 0 ? ThemeConfig.snap(ThemeConfig.SURFACE_CARD) : ThemeConfig.snap(ThemeConfig.BG_PAGE));
                if (days > 0) {
                    setFont(ThemeConfig.FONT_BTN);
                    setForeground(ThemeConfig.DANGER);
                } else {
                    setFont(ThemeConfig.FONT_BODY);
                    setForeground(ThemeConfig.snap(ThemeConfig.TEXT_MUTED));
                }
            }
            return this;
        }
    }

    private static class StatusPillCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            String status = value == null ? "" : value.toString().trim();

            JPanel wrapper = new JPanel(new GridBagLayout());
            if (isSelected) {
                wrapper.setBackground(ThemeConfig.snap(ThemeConfig.PRIMARY_LIGHT));
            } else {
                wrapper.setBackground(row % 2 == 0 ? ThemeConfig.snap(ThemeConfig.SURFACE_CARD) : ThemeConfig.snap(ThemeConfig.BG_PAGE));
            }

            JLabel badge = new JLabel(status) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            badge.setOpaque(false);
            badge.setHorizontalAlignment(SwingConstants.CENTER);
            badge.setFont(ThemeConfig.FONT_BADGE);
            badge.setBorder(new EmptyBorder(3, 10, 3, 10));

            switch (status) {
                case "Đang hoạt động", "Trả sớm", "Đúng hạn", "Đã hoàn thành", "Đã trả" -> {
                    badge.setBackground(ThemeConfig.STATUS_OK_BG);
                    badge.setForeground(ThemeConfig.STATUS_OK_FG);
                }
                case "Đang mượn", "Đang chờ", "Đã thông báo" -> {
                    badge.setBackground(ThemeConfig.PRIMARY_LIGHT);
                    badge.setForeground(ThemeConfig.STATUS_INFO_FG);
                }
                case "Sắp đến hạn", "Chờ xác nhận" -> {
                    badge.setBackground(ThemeConfig.STATUS_WARN_BG);
                    badge.setForeground(ThemeConfig.STATUS_WARN_FG);
                }
                case "Chưa trả quá hạn", "Đã trả muộn", "Đã hủy", "Hết hạn" -> {
                    badge.setBackground(ThemeConfig.STATUS_ERROR_BG);
                    badge.setForeground(ThemeConfig.STATUS_ERROR_FG);
                }
                default -> {
                    badge.setBackground(ThemeConfig.SUBTLE_SURFACE);
                    badge.setForeground(ThemeConfig.TEXT_PRIMARY);
                }
            }

            wrapper.add(badge);
            return wrapper;
        }
    }
}