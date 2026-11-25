package gui;

import dao.DocGiaDAO;
import dao.VoucherDAO;
import exception.DuLieuKhongHopLeException;
import model.DocGia;
import model.Voucher;
import org.kordamp.ikonli.feather.Feather;
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
import java.awt.datatransfer.StringSelection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Màn hình Quản lý Độc giả — Chuẩn CRUD Management Dashboard theo
 * UI_STYLE_GUIDE.md & ThemeConfig.
 */
public class QuanLyDocGiaPanel extends JPanel {

    private final DocGiaDAO docGiaDAO = new DocGiaDAO();
    private final VoucherDAO voucherDAO = new VoucherDAO();
    private final RewardService rewardService = new RewardService();

    private final JTextField txtMaDocGia = new JTextField();
    private final JTextField txtHoTen = new JTextField();
    private final JTextField txtEmail = new JTextField();
    private final JTextField txtSoDienThoai = new JTextField();
    private final JTextField txtDiaChi = new JTextField();

    private final JLabel lblDiemThuongForm = new JLabel("0 điểm");
    private final JLabel lblHangTheForm = new JLabel("Thành viên");
    private final JLabel lblSoLanViPham = new JLabel("0");

    private final JCheckBox chkTrangThai = new JCheckBox("Thẻ đang hoạt động", true);

    private final JTextField txtTimKiem = new JTextField(22);
    private final JComboBox<String> cbLocTrangThai = new JComboBox<>(new String[] {
            "Tất cả trạng thái",
            "Hoạt động",
            "Bị khóa",
            "Điểm âm (< 0)",
            "Tạm khóa mượn (< -50)",
            "Đủ điểm đổi quà (>= 50)",
            "Có vi phạm (> 0)",
            "Không vi phạm (0)"
    });
    private final JLabel lblKetQuaLoc = new JLabel("Hiển thị 0 / 0 độc giả");
    private final JLabel lblTrangThaiForm = ThemeConfig.createStatusLabel();

    private final JButton btnThem = new JButton("Thêm độc giả");
    private final JButton btnSua = new JButton("Cập nhật");
    private final JButton btnXoa = new JButton("Xóa");
    private final JButton btnLamMoi = new JButton("Làm mới form");
    private final JButton btnDoiVoucher = new JButton("Đổi thưởng Voucher");

    private int selectedId = -1;
    private List<Object> noiDungDaNap = List.of();
    private List<DocGia> danhSachDocGiaHienTai = new ArrayList<>();

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] {
                    "ID",
                    "CCCD / mã thẻ",
                    "Họ tên",
                    "Email",
                    "SĐT",
                    "Điểm tích lũy",
                    "Hạng thẻ",
                    "Số lần vi phạm",
                    "Ngày đăng ký",
                    "Trạng thái"
            },
            0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0, 5, 7 -> Integer.class;
                default -> String.class;
            };
        }
    };

    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);

    public QuanLyDocGiaPanel() {
        setLayout(new BorderLayout());
        setBackground(ThemeConfig.BG_PAGE);

        ThemeConfig.ScrollablePanel noiDung = new ThemeConfig.ScrollablePanel();
        noiDung.setLayout(new BorderLayout(0, 22));
        noiDung.setOpaque(false);
        noiDung.setBorder(new EmptyBorder(24, 28, 24, 28));
        noiDung.add(taoFormNhap(), BorderLayout.NORTH);
        noiDung.add(taoBangDocGia(), BorderLayout.CENTER);

        JScrollPane pageScroll = ThemeConfig.createPageScrollPane(noiDung);
        add(pageScroll, BorderLayout.CENTER);

        taiDuLieu();
        SwingUtilities.invokeLater(() -> pageScroll.getVerticalScrollBar().setValue(0));
    }

    // ============================================================
    // 1. FORM THÔNG TIN ĐỘC GIẢ (Top-Aligned Labels)
    // ============================================================
    private JPanel taoFormNhap() {
        JPanel card = new ThemeConfig.ModernCardPanel(new BorderLayout(0, 16));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titleBlock = ThemeConfig.createTitleBlock(
                "HỒ SƠ THẺ ĐỘC GIẢ",
                "Thêm mới độc giả hoặc chọn dòng trong danh sách để cập nhật thông tin",
                Feather.USERS);

        JPanel viPhamBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        viPhamBox.setOpaque(false);

        JLabel lblDiemTag = new JLabel("Điểm:");
        lblDiemTag.setFont(ThemeConfig.FONT_SM);
        lblDiemTag.setForeground(ThemeConfig.TEXT_SECONDARY);

        lblDiemThuongForm.setFont(ThemeConfig.FONT_LABEL);
        lblDiemThuongForm.setForeground(ThemeConfig.PRIMARY);
        lblDiemThuongForm.setBackground(ThemeConfig.PRIMARY_LIGHT);
        lblDiemThuongForm.setOpaque(true);
        lblDiemThuongForm.setBorder(new EmptyBorder(3, 10, 3, 10));

        lblHangTheForm.setFont(ThemeConfig.FONT_LABEL);
        lblHangTheForm.setForeground(ThemeConfig.TEXT_PRIMARY);
        lblHangTheForm.setBackground(ThemeConfig.SUBTLE_SURFACE);
        lblHangTheForm.setOpaque(true);
        lblHangTheForm.setBorder(new EmptyBorder(3, 10, 3, 10));

        JLabel lblVpTag = new JLabel("Vi phạm:");
        lblVpTag.setFont(ThemeConfig.FONT_SM);
        lblVpTag.setForeground(ThemeConfig.TEXT_SECONDARY);

        lblSoLanViPham.setFont(ThemeConfig.FONT_LABEL);
        lblSoLanViPham.setForeground(ThemeConfig.TEXT_PRIMARY);
        lblSoLanViPham.setBackground(ThemeConfig.SUBTLE_SURFACE);
        lblSoLanViPham.setOpaque(true);
        lblSoLanViPham.setBorder(new EmptyBorder(3, 10, 3, 10));

        viPhamBox.add(lblDiemTag);
        viPhamBox.add(lblDiemThuongForm);
        viPhamBox.add(lblHangTheForm);
        viPhamBox.add(lblVpTag);
        viPhamBox.add(lblSoLanViPham);

        headerPanel.add(titleBlock, BorderLayout.WEST);
        headerPanel.add(viPhamBox, BorderLayout.EAST);
        card.add(headerPanel, BorderLayout.NORTH);

        JPanel formGrid = new JPanel(new GridLayout(3, 2, 18, 14));
        formGrid.setOpaque(false);

        setupField(txtMaDocGia, "Nhập 12 chữ số, kể cả số 0 đầu");
        setupField(txtHoTen, "Họ và tên đầy đủ...");
        setupField(txtEmail, "email@domain.com...");
        setupField(txtSoDienThoai, "Số điện thoại (9-11 số)...");
        setupField(txtDiaChi, "Nhập địa chỉ liên hệ nếu có");

        chkTrangThai.setFont(ThemeConfig.FONT_BODY);
        chkTrangThai.setOpaque(false);
        chkTrangThai.setFocusPainted(false);

        formGrid.add(createFieldGroup("CCCD / mã thẻ cũ", true, txtMaDocGia));
        formGrid.add(createFieldGroup("Họ và tên", true, txtHoTen));
        formGrid.add(createFieldGroup("Email", true, txtEmail));
        formGrid.add(createFieldGroup("Số điện thoại", true, txtSoDienThoai));
        formGrid.add(createFieldGroup("Địa chỉ liên hệ", false, txtDiaChi));

        JPanel statusWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 18));
        statusWrapper.setOpaque(false);
        statusWrapper.add(chkTrangThai);
        formGrid.add(statusWrapper);

        card.add(formGrid, BorderLayout.CENTER);

        // Thanh nút CRUD sử dụng Ikonli Feather Icons + khóa nút sửa/xóa khi chưa chọn dòng
        ThemeConfig.styleActionButton(btnThem, Feather.PLUS, ThemeConfig.PRIMARY, Color.WHITE, ThemeConfig.PRIMARY_HOVER);
        ThemeConfig.styleActionButton(btnSua, Feather.EDIT_2, new Color(0x0284C7), Color.WHITE, new Color(0x0369A1));
        ThemeConfig.styleActionButton(btnXoa, Feather.TRASH_2, new Color(0xFEF2F2), ThemeConfig.DANGER,
                new Color(0xFECACA));
        ThemeConfig.styleActionButton(btnLamMoi, Feather.REFRESH_CW, Color.WHITE, ThemeConfig.TEXT_PRIMARY,
                ThemeConfig.BORDER);

        btnSua.setEnabled(false);
        btnXoa.setEnabled(false);

        JPanel nut = new JPanel(new ThemeConfig.WrapFlowLayout(FlowLayout.RIGHT, 12, 8));
        nut.setOpaque(false);
        nut.add(btnLamMoi);
        nut.add(btnXoa);
        nut.add(btnSua);
        nut.add(btnThem);

        JPanel formFooter = new JPanel(new BorderLayout(12, 0));
        formFooter.setOpaque(false);
        formFooter.add(lblTrangThaiForm, BorderLayout.CENTER);
        formFooter.add(nut, BorderLayout.EAST);

        card.add(formFooter, BorderLayout.SOUTH);

        btnThem.addActionListener(e -> themDocGia());
        btnSua.addActionListener(e -> capNhatDocGia());
        btnXoa.addActionListener(e -> xoaDocGia());

        btnLamMoi.addActionListener(e -> {
            txtTimKiem.setText("");
            cbLocTrangThai.setSelectedIndex(0);
            ThemeConfig.showStatus(lblTrangThaiForm, true, "");
            taiDuLieu();
        });

        return card;
    }

    private void setupField(JTextField field, String placeholder) {
        field.setFont(ThemeConfig.FONT_BODY);
        field.setPreferredSize(new Dimension(0, ThemeConfig.INPUT_HEIGHT));
        field.putClientProperty("JTextField.placeholderText", placeholder);
    }

    private JPanel createFieldGroup(String labelText, boolean required, JComponent comp) {
        JPanel group = new JPanel(new BorderLayout(0, 4));
        group.setOpaque(false);

        JLabel label = new JLabel(
                "<html>" + labelText + (required ? " <font color='#DC2626'>*</font>" : "") + "</html>");
        label.setFont(ThemeConfig.FONT_LABEL);
        label.setForeground(ThemeConfig.TEXT_PRIMARY);

        group.add(label, BorderLayout.NORTH);
        group.add(comp, BorderLayout.CENTER);
        return group;
    }

    // ============================================================
    // 2. BẢNG DANH SÁCH ĐỘC GIẢ (Data-Dense Table)
    // ============================================================
    private JPanel taoBangDocGia() {
        JPanel card = new ThemeConfig.ModernCardPanel(new BorderLayout(0, 16));

        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);

        JPanel searchGroup = new JPanel(new ThemeConfig.WrapFlowLayout(FlowLayout.LEFT, 12, 8));
        searchGroup.setOpaque(false);

        JLabel lblKey = new JLabel("Tìm nhanh:");
        lblKey.setFont(ThemeConfig.FONT_LABEL);
        lblKey.setForeground(ThemeConfig.TEXT_PRIMARY);

        JPanel searchBar = ThemeConfig.createSearchBar(
                txtTimKiem, "Tìm CCCD, mã thẻ cũ, tên, email, SĐT...", 270);

        JLabel lblLoc = new JLabel("Lọc:");
        lblLoc.setFont(ThemeConfig.FONT_LABEL);
        lblLoc.setForeground(ThemeConfig.TEXT_PRIMARY);

        cbLocTrangThai.setFont(ThemeConfig.FONT_BODY);
        cbLocTrangThai.setPreferredSize(new Dimension(175, ThemeConfig.INPUT_HEIGHT));

        JButton btnTim = ThemeConfig.styleActionButton(
                new JButton("Tìm kiếm"),
                Feather.SEARCH,
                ThemeConfig.PRIMARY,
                Color.WHITE,
                ThemeConfig.PRIMARY_HOVER);

        JButton btnTatCa = ThemeConfig.styleActionButton(
                new JButton("Tất cả độc giả"),
                Feather.USERS,
                Color.WHITE,
                ThemeConfig.TEXT_PRIMARY,
                ThemeConfig.BORDER);

        ThemeConfig.styleActionButton(
                btnDoiVoucher,
                Feather.GIFT,
                new Color(0x7C3AED),
                Color.WHITE,
                new Color(0x6D28D9));

        searchGroup.add(lblKey);
        searchGroup.add(searchBar);
        searchGroup.add(lblLoc);
        searchGroup.add(cbLocTrangThai);
        searchGroup.add(btnTim);
        searchGroup.add(btnTatCa);
        searchGroup.add(btnDoiVoucher);

        lblKetQuaLoc.setFont(ThemeConfig.FONT_SM_BOLD);
        lblKetQuaLoc.setForeground(ThemeConfig.PRIMARY);
        lblKetQuaLoc.setBackground(ThemeConfig.PRIMARY_LIGHT);
        lblKetQuaLoc.setOpaque(true);
        lblKetQuaLoc.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xBFDBFE), 1, true),
                new EmptyBorder(4, 10, 4, 10)));

        JPanel countWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        countWrapper.setOpaque(false);
        countWrapper.add(lblKetQuaLoc);

        toolbar.add(searchGroup, BorderLayout.CENTER);
        toolbar.add(countWrapper, BorderLayout.EAST);
        card.add(toolbar, BorderLayout.NORTH);

        ThemeConfig.styleTable(table);
        table.setRowHeight(ThemeConfig.TABLE_ROW_HEIGHT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSorter(sorter);

        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(50);
        cm.getColumn(1).setPreferredWidth(135);
        cm.getColumn(2).setPreferredWidth(175);
        cm.getColumn(3).setPreferredWidth(195);
        cm.getColumn(4).setPreferredWidth(110);
        cm.getColumn(5).setPreferredWidth(118);
        cm.getColumn(6).setPreferredWidth(112);
        cm.getColumn(7).setPreferredWidth(125);
        cm.getColumn(8).setPreferredWidth(110);
        cm.getColumn(9).setPreferredWidth(110);

        cm.getColumn(0).setCellRenderer(new CenterRenderer());
        cm.getColumn(1).setCellRenderer(new CenterRenderer());
        cm.getColumn(2).setCellRenderer(new LeftRenderer());
        cm.getColumn(3).setCellRenderer(new LeftRenderer());
        cm.getColumn(4).setCellRenderer(new CenterRenderer());
        cm.getColumn(5).setCellRenderer(new DiemTichLuyBadgeRenderer());
        cm.getColumn(6).setCellRenderer(new HangTheBadgeRenderer());
        cm.getColumn(7).setCellRenderer(new ViPhamBadgeRenderer());
        cm.getColumn(8).setCellRenderer(new CenterRenderer());
        cm.getColumn(9).setCellRenderer(new TrangThaiDocGiaBadgeRenderer());

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER, 1));
        scrollTable.getViewport().setBackground(ThemeConfig.SURFACE_CARD);
        scrollTable.setPreferredSize(new Dimension(0, 430));
        scrollTable.setMinimumSize(new Dimension(0, 380));
        ThemeConfig.installSaaSDataGrid(
                scrollTable, table, Feather.USERS,
                "Chưa có hồ sơ thẻ độc giả",
                "Điền biểu mẫu phía trên và nhấn 'Thêm độc giả' để cấp thẻ thư viện mới.",
                52, 135, 175, 195, 110, 118, 112, 125, 110, 110);
        card.add(scrollTable, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                hienThiDocGiaDangChon();
            }
        });

        btnTim.addActionListener(e -> apDungBoLoc());
        txtTimKiem.addActionListener(e -> apDungBoLoc());
        cbLocTrangThai.addActionListener(e -> apDungBoLoc());
        btnDoiVoucher.addActionListener(e -> moHopThoaiDoiVoucher());

        txtTimKiem.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { apDungBoLoc(); }
            @Override public void removeUpdate(DocumentEvent e) { apDungBoLoc(); }
            @Override public void changedUpdate(DocumentEvent e) { apDungBoLoc(); }
        });

        btnTatCa.addActionListener(e -> {
            txtTimKiem.setText("");
            cbLocTrangThai.setSelectedIndex(0);
            ThemeConfig.showStatus(lblTrangThaiForm, true, "");
            taiDuLieuGiuLuaChon();
        });

        return card;
    }

    // ================= TẢI / TÌM KIẾM =================

    public void taiDuLieu() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::taiDuLieu);
            return;
        }

        hienThiDanhSach(docGiaDAO.getAll());
    }

    public void taiDuLieuGiuLuaChon() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::taiDuLieuGiuLuaChon);
            return;
        }
        if (!noiDungDaNap.equals(noiDungForm())) {
            return;
        }
        int prevId = selectedId;
        hienThiDanhSach(docGiaDAO.getAll());
        if (prevId > 0) {
            chonLaiDocGiaTrongBang(prevId);
        }
    }

    private void hienThiDanhSach(List<DocGia> danhSach) {
        this.danhSachDocGiaHienTai = danhSach == null ? new ArrayList<>() : new ArrayList<>(danhSach);
        lamMoiForm();
        tableModel.setRowCount(0);

        for (DocGia dg : this.danhSachDocGiaHienTai) {
            tableModel.addRow(new Object[] {
                    dg.getId(),
                    DocGia.maTheRutGon(dg.getMaDocGia()),
                    dg.getHoTen(),
                    dg.getEmail(),
                    dg.getSoDienThoai(),
                    dg.getDiemThuong(),
                    dg.getHangThe(),
                    dg.getSoLanViPham(),
                    dg.getNgayDangKy() == null
                            ? ""
                            : dg.getNgayDangKy().toString(),
                    dg.isTrangThai() ? "Hoạt động" : "Bị khóa"
            });
        }
        apDungBoLoc();
    }

    private void apDungBoLoc() {
        String tuKhoa = ThemeConfig.normalizeText(txtTimKiem.getText());
        String locTrangThai = Objects.toString(cbLocTrangThai.getSelectedItem(), "Tất cả trạng thái");

        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                String gopText = ThemeConfig.normalizeText(
                        entry.getStringValue(1) + " "
                                + entry.getStringValue(2) + " "
                                + entry.getStringValue(3) + " "
                                + entry.getStringValue(4) + " "
                                + entry.getStringValue(6) + " "
                                + entry.getStringValue(8));

                int dongModel = entry.getIdentifier();
                String maThe = dongModel < danhSachDocGiaHienTai.size()
                        ? danhSachDocGiaHienTai.get(dongModel).getMaDocGia() : "";
                boolean khopTuKhoa = tuKhoa.isEmpty() || gopText.contains(tuKhoa)
                        || ThemeConfig.normalizeText(maThe).contains(tuKhoa);

                int diemTichLuy = 0;
                Object diemVal = entry.getValue(5);
                if (diemVal instanceof Number n) {
                    diemTichLuy = n.intValue();
                }

                int soLanVp = 0;
                Object vpVal = entry.getValue(7);
                if (vpVal instanceof Number n) {
                    soLanVp = n.intValue();
                }
                String trangThai = entry.getStringValue(9);

                boolean khopTrangThai = switch (locTrangThai) {
                    case "Hoạt động" -> "Hoạt động".equals(trangThai);
                    case "Bị khóa" -> "Bị khóa".equals(trangThai);
                    case "Điểm âm (< 0)" -> diemTichLuy < 0;
                    case "Tạm khóa mượn (< -50)" -> diemTichLuy < DocGia.NGUONG_TAM_KHOA_MUON;
                    case "Đủ điểm đổi quà (>= 50)" -> diemTichLuy >= 50;
                    case "Có vi phạm (> 0)" -> soLanVp > 0;
                    case "Không vi phạm (0)" -> soLanVp == 0;
                    default -> true;
                };

                return khopTuKhoa && khopTrangThai;
            }
        });

        lblKetQuaLoc.setText("Hiển thị " + table.getRowCount() + " / " + tableModel.getRowCount() + " độc giả");
    }

    public void focusTimKiem() {
        txtTimKiem.requestFocusInWindow();
        txtTimKiem.selectAll();
    }

    public void moVaLocTrangThai(String trangThai) {
        txtTimKiem.setText("");
        if (trangThai != null && !trangThai.isBlank()) {
            cbLocTrangThai.setSelectedItem(trangThai);
        } else {
            cbLocTrangThai.setSelectedIndex(0);
        }
        taiDuLieuGiuLuaChon();
    }

    // ================= THÊM ĐỘC GIẢ =================

    private void themDocGia() {
        String maThe = txtMaDocGia.getText().trim();
        if (!kiemTraDuLieu(maThe, null)) {
            return;
        }

        String email = txtEmail.getText().trim();

        if (docGiaDAO.tonTaiMaDocGia(maThe)) {
            ThemeConfig.showStatus(lblTrangThaiForm, false, "CCCD đã được sử dụng.");
            ThemeConfig.toastWarning(this, "CCCD đã được sử dụng.");
            return;
        }

        if (docGiaDAO.tonTaiEmail(email)) {
            ThemeConfig.showStatus(lblTrangThaiForm, false, "Email đã được độc giả khác sử dụng.");
            ThemeConfig.toastWarning(this, "Email đã được độc giả khác sử dụng.");
            return;
        }

        DocGia dg = new DocGia();
        ganThongTinTuForm(dg, maThe);

        dg.setDiemThuong(0);
        dg.setSoLanViPham(0);
        dg.setNgayDangKy(LocalDate.now());

        if (docGiaDAO.themDocGia(dg)) {
            ThemeConfig.showStatus(lblTrangThaiForm, true, "Đã thêm độc giả \"" + dg.getHoTen() + "\".");
            ThemeConfig.toastSuccess(this, "Đã thêm độc giả \"" + dg.getHoTen() + "\".");
            taiDuLieu();
            MainFrame.refreshMainDashboard(this);
        } else {
            ThemeConfig.showStatus(lblTrangThaiForm, false, "Không thêm được độc giả. Kiểm tra kết nối.");
            ThemeConfig.toastError(this, "Không thêm được độc giả. Hãy kiểm tra thông tin và kết nối.");
        }
    }

    // ================= CẬP NHẬT ĐỘC GIẢ =================

    private void capNhatDocGia() {
        if (selectedId <= 0) {
            ThemeConfig.toastWarning(this, "Hãy chọn độc giả cần cập nhật.");
            return;
        }

        DocGia dg = docGiaDAO.getById(selectedId);

        if (dg == null) {
            ThemeConfig.showStatus(lblTrangThaiForm, false, "Không đọc được thông tin độc giả.");
            ThemeConfig.toastWarning(this, "Không đọc được độc giả. Hãy làm mới danh sách.");
            return;
        }

        String maCu = dg.getMaDocGia();
        String maNhap = txtMaDocGia.getText();
        String maMoi = Objects.equals(maNhap, maCu) ? maCu : maNhap.trim();
        if (!kiemTraDuLieu(maMoi, maCu)) {
            return;
        }
        String emailMoi = txtEmail.getText().trim();

        DocGia trungMa = docGiaDAO.getByMaDocGia(maMoi);

        if (trungMa != null && trungMa.getId() != selectedId) {
            ThemeConfig.showStatus(lblTrangThaiForm, false, "CCCD đã thuộc về độc giả khác.");
            ThemeConfig.toastWarning(this, "CCCD đã thuộc về độc giả khác.");
            return;
        }

        String emailCu = dg.getEmail() == null
                ? ""
                : dg.getEmail();

        if (!emailMoi.equalsIgnoreCase(emailCu)
                && docGiaDAO.tonTaiEmail(emailMoi)) {
            ThemeConfig.showStatus(lblTrangThaiForm, false, "Email đã được sử dụng.");
            ThemeConfig.toastWarning(this, "Email đã được sử dụng.");
            return;
        }

        ganThongTinTuForm(dg, maMoi);

        if (docGiaDAO.capNhatDocGia(dg)) {
            ThemeConfig.showStatus(lblTrangThaiForm, true, "Đã cập nhật hồ sơ \"" + dg.getHoTen() + "\".");
            ThemeConfig.toastSuccess(this, "Đã cập nhật hồ sơ \"" + dg.getHoTen() + "\".");
            taiDuLieu();
            MainFrame.refreshMainDashboard(this);
        } else {
            ThemeConfig.showStatus(lblTrangThaiForm, false, "Cập nhật độc giả thất bại.");
            ThemeConfig.toastError(this, "Không cập nhật được độc giả. Hãy làm mới và kiểm tra thông tin.");
        }
    }

    private void ganThongTinTuForm(DocGia dg, String maThe) {
        dg.setMaDocGia(maThe);
        dg.setHoTen(txtHoTen.getText().trim());
        dg.setEmail(txtEmail.getText().trim());
        dg.setSoDienThoai(txtSoDienThoai.getText().trim());
        dg.setDiaChi(txtDiaChi.getText().trim());
        dg.setTrangThai(chkTrangThai.isSelected());
    }

    // ================= XÓA ĐỘC GIẢ =================

    private void xoaDocGia() {
        if (selectedId <= 0) {
            JOptionPane.showMessageDialog(
                    this, "Hãy chọn độc giả cần xóa.");
            return;
        }

        int chon = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc muốn xóa độc giả đã chọn?",
                "Xác nhận",
                JOptionPane.YES_NO_OPTION);

        if (chon != JOptionPane.YES_OPTION) {
            return;
        }

        if (docGiaDAO.xoaDocGia(selectedId)) {
            ThemeConfig.showStatus(lblTrangThaiForm, true, "Đã xóa hồ sơ độc giả.");
            ThemeConfig.toastSuccess(this, "Đã xóa hồ sơ độc giả.");
            taiDuLieu();
            MainFrame.refreshMainDashboard(this);
        } else {
            ThemeConfig.showStatus(lblTrangThaiForm, false, "Không thể xóa độc giả đã có lịch sử mượn/vi phạm.");
            ThemeConfig.toastWarning(this, "Không thể xóa độc giả đã có lịch sử. Hãy chuyển sang trạng thái bị khóa.");
        }
    }

    // ================= CHỌN ĐỘC GIẢ =================

    private void hienThiDocGiaDangChon() {
        int dongChon = table.getSelectedRow();

        if (dongChon < 0) {
            btnSua.setEnabled(false);
            btnXoa.setEnabled(false);
            return;
        }

        int dongModel = table.convertRowIndexToModel(dongChon);

        int id = ((Number) tableModel.getValueAt(dongModel, 0))
                .intValue();

        DocGia dg = docGiaDAO.getById(id);

        if (dg == null) {
            lamMoiForm();
            ThemeConfig.toastWarning(this, "Không đọc được thông tin độc giả đã chọn.");
            return;
        }

        selectedId = dg.getId();
        btnSua.setEnabled(true);
        btnXoa.setEnabled(true);

        txtMaDocGia.setText(dg.getMaDocGia());
        txtHoTen.setText(dg.getHoTen());
        txtEmail.setText(dg.getEmail());
        txtSoDienThoai.setText(dg.getSoDienThoai());
        txtDiaChi.setText(dg.getDiaChi());

        chkTrangThai.setSelected(dg.isTrangThai());

        int diem = dg.getDiemThuong();
        lblDiemThuongForm.setText(diem + " điểm");
        if (diem < 0) {
            lblDiemThuongForm.setForeground(Color.WHITE);
            lblDiemThuongForm.setBackground(new ThemeConfig.ThemeColor(0xDC2626, 0x991B1B));
        } else if (diem >= 180) {
            lblDiemThuongForm.setForeground(ThemeConfig.STATUS_WARN_FG);
            lblDiemThuongForm.setBackground(ThemeConfig.STATUS_WARN_BG);
        } else if (diem > 0) {
            lblDiemThuongForm.setForeground(ThemeConfig.STATUS_OK_FG);
            lblDiemThuongForm.setBackground(ThemeConfig.STATUS_OK_BG);
        } else {
            lblDiemThuongForm.setForeground(ThemeConfig.TEXT_SECONDARY);
            lblDiemThuongForm.setBackground(ThemeConfig.SUBTLE_SURFACE);
        }

        lblHangTheForm.setText(dg.getHangThe());
        if (dg.biTamKhoaDoDiemAm()) {
            lblHangTheForm.setForeground(Color.WHITE);
            lblHangTheForm.setBackground(new ThemeConfig.ThemeColor(0xDC2626, 0x991B1B));
        } else if (diem < 0) {
            lblHangTheForm.setForeground(ThemeConfig.STATUS_ERROR_FG);
            lblHangTheForm.setBackground(ThemeConfig.STATUS_ERROR_BG);
        } else if (diem >= 300) {
            lblHangTheForm.setForeground(new ThemeConfig.ThemeColor(0x7C3AED, 0xDDD6FE));
            lblHangTheForm.setBackground(new ThemeConfig.ThemeColor(0xF3E8FF, 0x3B0764));
        } else if (diem >= 180) {
            lblHangTheForm.setForeground(ThemeConfig.STATUS_WARN_FG);
            lblHangTheForm.setBackground(ThemeConfig.STATUS_WARN_BG);
        } else if (diem >= 100) {
            lblHangTheForm.setForeground(ThemeConfig.STATUS_INFO_FG);
            lblHangTheForm.setBackground(ThemeConfig.PRIMARY_LIGHT);
        } else {
            lblHangTheForm.setForeground(ThemeConfig.TEXT_SECONDARY);
            lblHangTheForm.setBackground(ThemeConfig.SUBTLE_SURFACE);
        }

        lblSoLanViPham.setText(
                String.valueOf(dg.getSoLanViPham()));

        if (dg.getSoLanViPham() > 0) {
            lblSoLanViPham.setForeground(ThemeConfig.DANGER);
            lblSoLanViPham.setBackground(ThemeConfig.STATUS_ERROR_BG);
        } else {
            lblSoLanViPham.setForeground(ThemeConfig.TEXT_PRIMARY);
            lblSoLanViPham.setBackground(ThemeConfig.SUBTLE_SURFACE);
        }
        noiDungDaNap = noiDungForm();
    }

    private void moHopThoaiDoiVoucher() {
        List<DocGia> ds = danhSachDocGiaHienTai.isEmpty() ? docGiaDAO.getAll() : danhSachDocGiaHienTai;
        if (ds.isEmpty()) {
            ThemeConfig.toastWarning(this, "Chưa có dữ liệu độc giả để đổi thưởng.");
            return;
        }

        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Đổi điểm tích lũy lấy Voucher", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(580, 580);
        dialog.setMinimumSize(new Dimension(540, 540));
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(ThemeConfig.BG_PAGE);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel header = ThemeConfig.createTitleBlock(
                "ĐỔI THƯỞNG VOUCHER KHÁCH HÀNG",
                "Chọn độc giả và gói đổi điểm để tự động trừ điểm và phát hành mã Voucher giảm giá",
                Feather.GIFT);
        root.add(header, BorderLayout.NORTH);

        JPanel body = new ThemeConfig.ModernCardPanel(new BorderLayout(0, 14), new Insets(18, 20, 18, 20));

        JComboBox<DocGia> cbDocGiaDoi = new JComboBox<>();
        cbDocGiaDoi.setFont(ThemeConfig.FONT_BODY);
        cbDocGiaDoi.setPreferredSize(new Dimension(0, ThemeConfig.INPUT_HEIGHT));
        cbDocGiaDoi.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof DocGia dg) {
                    setText(dg.getMaDocGia() + " - " + dg.getHoTen()
                            + " (" + dg.getDiemThuong() + " điểm • " + dg.getHangThe() + ")");
                }
                return this;
            }
        });

        DocGia chonSan = null;
        for (DocGia dg : ds) {
            cbDocGiaDoi.addItem(dg);
            if (dg.getId() == selectedId) {
                chonSan = dg;
            }
        }
        if (chonSan != null) {
            cbDocGiaDoi.setSelectedItem(chonSan);
        }

        JPanel infoBox = new JPanel(new GridLayout(1, 3, 12, 8));
        infoBox.setBackground(ThemeConfig.SUBTLE_SURFACE);
        infoBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)));

        JLabel lblDiemHienCo = new JLabel("0 điểm");
        lblDiemHienCo.setFont(ThemeConfig.FONT_H3);
        JLabel lblHangHienCo = new JLabel("Thành viên");
        lblHangHienCo.setFont(ThemeConfig.FONT_H3);
        JLabel lblVoucherHienCo = new JLabel("0 mã chưa dùng");
        lblVoucherHienCo.setFont(ThemeConfig.FONT_BTN);
        lblVoucherHienCo.setForeground(ThemeConfig.PRIMARY);

        infoBox.add(taoStatNho("Điểm tích lũy hiện có", lblDiemHienCo));
        infoBox.add(taoStatNho("Hạng thẻ hiện tại", lblHangHienCo));
        infoBox.add(taoStatNho("Voucher chưa dùng", lblVoucherHienCo));

        JPanel topReader = new JPanel(new BorderLayout(0, 10));
        topReader.setOpaque(false);
        topReader.add(createFieldGroup("Độc giả nhận Voucher", true, cbDocGiaDoi), BorderLayout.NORTH);
        topReader.add(infoBox, BorderLayout.CENTER);
        body.add(topReader, BorderLayout.NORTH);

        JPanel packagesPanel = new JPanel(new GridLayout(4, 1, 0, 10));
        packagesPanel.setOpaque(false);

        ButtonGroup group = new ButtonGroup();
        RewardService.GoiDoiVoucher[] gois = RewardService.GoiDoiVoucher.values();
        JRadioButton[] radios = new JRadioButton[gois.length];
        for (int i = 0; i < gois.length; i++) {
            RewardService.GoiDoiVoucher goi = gois[i];
            JRadioButton rb = new JRadioButton(goi.moTa());
            rb.setFont(ThemeConfig.FONT_BODY);
            rb.setForeground(ThemeConfig.TEXT_PRIMARY);
            rb.setBackground(ThemeConfig.SUBTLE_SURFACE);
            rb.setOpaque(true);
            rb.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeConfig.BORDER, 1, true),
                    new EmptyBorder(10, 12, 10, 12)));
            rb.setBorderPainted(true);
            rb.setFocusPainted(false);
            group.add(rb);
            radios[i] = rb;
            packagesPanel.add(rb);
        }

        JLabel lblLuuY = new JLabel(" ");
        lblLuuY.setFont(ThemeConfig.FONT_SM);
        lblLuuY.setForeground(ThemeConfig.TEXT_SECONDARY);

        JPanel centerPack = new JPanel(new BorderLayout(0, 10));
        centerPack.setOpaque(false);
        centerPack.add(createFieldGroup("Chọn gói đổi thưởng", true, packagesPanel), BorderLayout.CENTER);
        centerPack.add(lblLuuY, BorderLayout.SOUTH);
        body.add(centerPack, BorderLayout.CENTER);

        root.add(body, BorderLayout.CENTER);

        JButton btnXacNhanDoi = ThemeConfig.styleActionButton(
                new JButton("Đổi thưởng & Cấp Voucher"),
                Feather.CHECK_CIRCLE,
                new Color(0x7C3AED),
                Color.WHITE,
                new Color(0x6D28D9));
        JButton btnDong = ThemeConfig.styleActionButton(
                new JButton("Đóng"),
                Feather.X,
                ThemeConfig.SURFACE_CARD,
                ThemeConfig.TEXT_PRIMARY,
                ThemeConfig.BORDER);

        Runnable capNhatTrangThaiDialog = () -> {
            DocGia dgChon = (DocGia) cbDocGiaDoi.getSelectedItem();
            if (dgChon == null) {
                btnXacNhanDoi.setEnabled(false);
                return;
            }
            DocGia fresh = docGiaDAO.getById(dgChon.getId());
            if (fresh != null) {
                dgChon.setDiemThuong(fresh.getDiemThuong());
                dgChon.setTrangThai(fresh.isTrangThai());
            }
            int diem = dgChon.getDiemThuong();
            lblDiemHienCo.setText(diem + " điểm");
            lblDiemHienCo.setForeground(diem < 0 ? ThemeConfig.DANGER
                    : (diem >= 50 ? ThemeConfig.SUCCESS : ThemeConfig.TEXT_PRIMARY));
            lblHangHienCo.setText(dgChon.getHangThe());
            lblHangHienCo.setForeground(diem < 0 ? ThemeConfig.DANGER : ThemeConfig.PRIMARY);

            List<Voucher> vouchers = voucherDAO.getVoucherChuaDung(dgChon.getId());
            lblVoucherHienCo.setText(vouchers.size() + " mã chưa dùng");

            int firstValid = -1;
            int selectedIdx = -1;
            for (int i = 0; i < gois.length; i++) {
                boolean duDiem = dgChon.isTrangThai() && diem >= gois[i].diemCanDoi();
                radios[i].setEnabled(duDiem);
                if (duDiem && firstValid == -1) {
                    firstValid = i;
                }
                if (radios[i].isSelected() && duDiem) {
                    selectedIdx = i;
                }
            }
            if (selectedIdx == -1) {
                group.clearSelection();
                if (firstValid >= 0) {
                    radios[firstValid].setSelected(true);
                }
            }

            if (!dgChon.isTrangThai()) {
                lblLuuY.setText("Thẻ độc giả này đang bị khóa, không thể thực hiện đổi thưởng.");
                lblLuuY.setForeground(ThemeConfig.DANGER);
                btnXacNhanDoi.setEnabled(false);
            } else if (firstValid == -1) {
                lblLuuY.setText("Độc giả cần tối thiểu 50 điểm tích lũy để đổi gói Voucher thấp nhất.");
                lblLuuY.setForeground(ThemeConfig.DANGER);
                btnXacNhanDoi.setEnabled(false);
            } else {
                lblLuuY.setText("Mã Voucher được lưu vào ví độc giả và áp dụng trực tiếp tại màn hình Mượn & Trả sách.");
                lblLuuY.setForeground(ThemeConfig.TEXT_SECONDARY);
                btnXacNhanDoi.setEnabled(true);
            }
        };

        cbDocGiaDoi.addActionListener(e -> capNhatTrangThaiDialog.run());
        capNhatTrangThaiDialog.run();

        btnDong.addActionListener(e -> dialog.dispose());
        btnXacNhanDoi.addActionListener(e -> {
            DocGia dgChon = (DocGia) cbDocGiaDoi.getSelectedItem();
            RewardService.GoiDoiVoucher goiChon = null;
            for (int i = 0; i < gois.length; i++) {
                if (radios[i].isSelected() && radios[i].isEnabled()) {
                    goiChon = gois[i];
                    break;
                }
            }
            if (dgChon == null || goiChon == null) {
                ThemeConfig.toastWarning(dialog, "Vui lòng chọn gói đổi thưởng hợp lệ.");
                return;
            }

            btnXacNhanDoi.setEnabled(false);
            btnDong.setEnabled(false);
            final int targetId = dgChon.getId();
            final RewardService.GoiDoiVoucher finalGoi = goiChon;

            new SwingWorker<Voucher, Void>() {
                @Override
                protected Voucher doInBackground() throws Exception {
                    return rewardService.doiDiemLayVoucher(targetId, finalGoi);
                }

                @Override
                protected void done() {
                    btnDong.setEnabled(true);
                    try {
                        Voucher moi = get();
                        dialog.dispose();
                        taiDuLieu();
                        chonLaiDocGiaTrongBang(targetId);
                        MainFrame.refreshMainDashboard(QuanLyDocGiaPanel.this);

                        String msg = "Đã cấp Voucher thành công cho " + dgChon.getHoTen() + "!\n\n"
                                + "Mã Voucher: " + moi.getMaVoucher() + "\n"
                                + "Mức ưu đãi: " + moi.moTaUuDai() + "\n"
                                + "Trạng thái: Chưa dùng\n"
                                + "Điểm đã trừ: -" + finalGoi.diemCanDoi() + " điểm";

                        Object[] opts = { "Sao chép mã Voucher", "Đóng" };
                        int pick = JOptionPane.showOptionDialog(
                                QuanLyDocGiaPanel.this,
                                msg,
                                "Cấp Voucher thành công",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.INFORMATION_MESSAGE,
                                null,
                                opts,
                                opts[0]);
                        if (pick == 0) {
                            Toolkit.getDefaultToolkit().getSystemClipboard()
                                    .setContents(new StringSelection(moi.getMaVoucher()), null);
                            ThemeConfig.toastSuccess(QuanLyDocGiaPanel.this,
                                    "Đã sao chép mã " + moi.getMaVoucher() + " vào clipboard.");
                        } else {
                            ThemeConfig.toastSuccess(QuanLyDocGiaPanel.this,
                                    "Đã cấp Voucher " + moi.getMaVoucher() + " (-" + finalGoi.diemCanDoi() + " điểm).");
                        }
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        String errMsg = cause instanceof DuLieuKhongHopLeException
                                ? cause.getMessage()
                                : "Không thể đổi thưởng Voucher: " + cause.getMessage();
                        ThemeConfig.toastWarning(dialog, errMsg);
                        capNhatTrangThaiDialog.run();
                    }
                }
            }.execute();
        });

        JPanel footer = new JPanel(new ThemeConfig.WrapFlowLayout(FlowLayout.RIGHT, 10, 4));
        footer.setOpaque(false);
        footer.add(btnDong);
        footer.add(btnXacNhanDoi);
        root.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        ThemeConfig.applyThemeToTree(dialog);
        dialog.setVisible(true);
    }

    private JPanel taoStatNho(String title, JLabel val) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(ThemeConfig.FONT_SM);
        t.setForeground(ThemeConfig.TEXT_SECONDARY);
        p.add(t, BorderLayout.NORTH);
        p.add(val, BorderLayout.CENTER);
        return p;
    }

    private void chonLaiDocGiaTrongBang(int idDocGia) {
        for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            Object idVal = tableModel.getValueAt(modelRow, 0);
            if (idVal instanceof Number n && n.intValue() == idDocGia) {
                table.setRowSelectionInterval(viewRow, viewRow);
                table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
                break;
            }
        }
    }

    // ================= KIỂM TRA DỮ LIỆU =================

    private boolean kiemTraDuLieu(String maThe, String maCu) {
        String hoTen = txtHoTen.getText().trim();
        String email = txtEmail.getText().trim();
        String soDienThoai = txtSoDienThoai.getText().trim();

        if (!DocGia.laCccdHopLe(maThe) && (maThe.isBlank() || !Objects.equals(maThe, maCu))) {
            ThemeConfig.showStatus(lblTrangThaiForm, false, "CCCD phải gồm đúng 12 chữ số.");
            ThemeConfig.toastWarning(this, "Thêm hồ sơ hoặc đổi mã thẻ cần CCCD gồm 12 chữ số.");
            txtMaDocGia.requestFocusInWindow();
            return false;
        }
        if (hoTen.isEmpty()) {
            ThemeConfig.showStatus(lblTrangThaiForm, false, "Họ tên không được để trống.");
            ThemeConfig.toastWarning(this, "Họ tên không được để trống.");
            return false;
        }

        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            ThemeConfig.showStatus(lblTrangThaiForm, false, "Định dạng email chưa hợp lệ.");
            ThemeConfig.toastWarning(this, "Vui lòng nhập địa chỉ email hợp lệ.");
            return false;
        }

        if (!soDienThoai.matches("\\d{9,11}")) {
            ThemeConfig.showStatus(lblTrangThaiForm, false, "Số điện thoại phải gồm 9 đến 11 chữ số.");
            ThemeConfig.toastWarning(this, "Số điện thoại phải gồm 9 đến 11 chữ số.");
            return false;
        }

        return true;
    }

    private void lamMoiForm() {
        selectedId = -1;
        btnSua.setEnabled(false);
        btnXoa.setEnabled(false);

        txtMaDocGia.setText("");
        txtHoTen.setText("");
        txtEmail.setText("");
        txtSoDienThoai.setText("");
        txtDiaChi.setText("");

        lblDiemThuongForm.setText("0 điểm");
        lblDiemThuongForm.setForeground(ThemeConfig.PRIMARY);
        lblDiemThuongForm.setBackground(ThemeConfig.PRIMARY_LIGHT);
        lblHangTheForm.setText("Thành viên");
        lblHangTheForm.setForeground(ThemeConfig.TEXT_PRIMARY);
        lblHangTheForm.setBackground(ThemeConfig.SUBTLE_SURFACE);
        lblSoLanViPham.setText("0");
        lblSoLanViPham.setForeground(ThemeConfig.TEXT_PRIMARY);
        lblSoLanViPham.setBackground(ThemeConfig.SUBTLE_SURFACE);
        chkTrangThai.setSelected(true);

        table.clearSelection();
        noiDungDaNap = noiDungForm();
    }

    private List<Object> noiDungForm() {
        return List.of(txtMaDocGia.getText(), txtHoTen.getText(), txtEmail.getText(),
                txtSoDienThoai.getText(), txtDiaChi.getText(), chkTrangThai.isSelected());
    }

    // ============================================================
    // RENDERERS CHO BẢNG ĐỘC GIẢ
    // ============================================================

    private static class LeftRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(ThemeConfig.FONT_BODY);
            setBorder(new EmptyBorder(0, 10, 0, 10));
            setHorizontalAlignment(SwingConstants.LEFT);
            if (!isSelected) {
                setBackground(row % 2 == 0 ? ThemeConfig.snap(ThemeConfig.SURFACE_CARD) : ThemeConfig.snap(ThemeConfig.BG_PAGE));
                setForeground(ThemeConfig.snap(ThemeConfig.TEXT_PRIMARY));
            }
            return this;
        }
    }

    private static class CenterRenderer extends DefaultTableCellRenderer {
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

    private static class DiemTichLuyBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            int diem = 0;
            if (value instanceof Number n) {
                diem = n.intValue();
            }

            JPanel wrapper = new JPanel(new GridBagLayout());
            if (isSelected) {
                wrapper.setBackground(ThemeConfig.snap(ThemeConfig.PRIMARY_LIGHT));
            } else {
                wrapper.setBackground(row % 2 == 0 ? ThemeConfig.snap(ThemeConfig.SURFACE_CARD) : ThemeConfig.snap(ThemeConfig.BG_PAGE));
            }

            String text = (diem > 0 ? "+" : "") + diem + " điểm";
            JLabel badge = new JLabel(text) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            badge.setOpaque(false);
            badge.setHorizontalAlignment(SwingConstants.CENTER);
            badge.setFont(ThemeConfig.FONT_BADGE);
            badge.setBorder(new EmptyBorder(3, 9, 3, 9));

            if (diem < 0) {
                badge.setBackground(new ThemeConfig.ThemeColor(0xDC2626, 0x991B1B));
                badge.setForeground(Color.WHITE);
            } else if (diem >= 180) {
                badge.setBackground(ThemeConfig.STATUS_WARN_BG);
                badge.setForeground(ThemeConfig.STATUS_WARN_FG);
            } else if (diem > 0) {
                badge.setBackground(ThemeConfig.STATUS_OK_BG);
                badge.setForeground(ThemeConfig.STATUS_OK_FG);
            } else {
                badge.setBackground(ThemeConfig.SUBTLE_SURFACE);
                badge.setForeground(ThemeConfig.TEXT_SECONDARY);
            }

            wrapper.add(badge);
            return wrapper;
        }
    }

    private static class HangTheBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            String hang = value == null ? "Thành viên" : value.toString().trim();

            JPanel wrapper = new JPanel(new GridBagLayout());
            if (isSelected) {
                wrapper.setBackground(ThemeConfig.snap(ThemeConfig.PRIMARY_LIGHT));
            } else {
                wrapper.setBackground(row % 2 == 0 ? ThemeConfig.snap(ThemeConfig.SURFACE_CARD) : ThemeConfig.snap(ThemeConfig.BG_PAGE));
            }

            JLabel badge = new JLabel(hang) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            badge.setOpaque(false);
            badge.setHorizontalAlignment(SwingConstants.CENTER);
            badge.setFont(ThemeConfig.FONT_BADGE);
            badge.setBorder(new EmptyBorder(3, 9, 3, 9));

            switch (hang) {
                case "Tạm khóa mượn" -> {
                    badge.setBackground(new ThemeConfig.ThemeColor(0xDC2626, 0x991B1B));
                    badge.setForeground(Color.WHITE);
                }
                case "Cảnh báo" -> {
                    badge.setBackground(ThemeConfig.STATUS_ERROR_BG);
                    badge.setForeground(ThemeConfig.STATUS_ERROR_FG);
                }
                case "Kim cương" -> {
                    badge.setBackground(new ThemeConfig.ThemeColor(0xF3E8FF, 0x3B0764));
                    badge.setForeground(new ThemeConfig.ThemeColor(0x7C3AED, 0xDDD6FE));
                }
                case "Vàng" -> {
                    badge.setBackground(ThemeConfig.STATUS_WARN_BG);
                    badge.setForeground(ThemeConfig.STATUS_WARN_FG);
                }
                case "Bạc" -> {
                    badge.setBackground(ThemeConfig.PRIMARY_LIGHT);
                    badge.setForeground(ThemeConfig.STATUS_INFO_FG);
                }
                default -> {
                    badge.setBackground(ThemeConfig.SUBTLE_SURFACE);
                    badge.setForeground(ThemeConfig.TEXT_SECONDARY);
                }
            }

            wrapper.add(badge);
            return wrapper;
        }
    }

    private static class ViPhamBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            int vp = 0;
            if (value instanceof Number n) {
                vp = n.intValue();
            }

            JPanel wrapper = new JPanel(new GridBagLayout());
            if (isSelected) {
                wrapper.setBackground(ThemeConfig.snap(ThemeConfig.PRIMARY_LIGHT));
            } else {
                wrapper.setBackground(row % 2 == 0 ? ThemeConfig.snap(ThemeConfig.SURFACE_CARD) : ThemeConfig.snap(ThemeConfig.BG_PAGE));
            }

            JLabel badge = new JLabel(vp == 0 ? "0 lần" : vp + " lần vi phạm") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            badge.setOpaque(false);
            badge.setHorizontalAlignment(SwingConstants.CENTER);
            badge.setFont(ThemeConfig.FONT_BADGE);
            badge.setBorder(new EmptyBorder(2, 8, 2, 8));

            if (vp > 0) {
                badge.setBackground(ThemeConfig.STATUS_ERROR_BG);
                badge.setForeground(ThemeConfig.STATUS_ERROR_FG);
            } else {
                badge.setBackground(ThemeConfig.SUBTLE_SURFACE);
                badge.setForeground(ThemeConfig.TEXT_SECONDARY);
            }

            wrapper.add(badge);
            return wrapper;
        }
    }

    private static class TrangThaiDocGiaBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            String st = value == null ? "" : value.toString();
            boolean active = "Hoạt động".equalsIgnoreCase(st);

            JPanel wrapper = new JPanel(new GridBagLayout());
            if (isSelected) {
                wrapper.setBackground(ThemeConfig.snap(ThemeConfig.PRIMARY_LIGHT));
            } else {
                wrapper.setBackground(row % 2 == 0 ? ThemeConfig.snap(ThemeConfig.SURFACE_CARD) : ThemeConfig.snap(ThemeConfig.BG_PAGE));
            }

            JLabel badge = new JLabel(active ? "Hoạt động" : "Bị khóa") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            badge.setOpaque(false);
            badge.setHorizontalAlignment(SwingConstants.CENTER);
            badge.setFont(ThemeConfig.FONT_BADGE);
            badge.setBorder(new EmptyBorder(2, 8, 2, 8));

            if (active) {
                badge.setBackground(ThemeConfig.STATUS_OK_BG);
                badge.setForeground(ThemeConfig.STATUS_OK_FG);
            } else {
                badge.setBackground(ThemeConfig.STATUS_ERROR_BG);
                badge.setForeground(ThemeConfig.STATUS_ERROR_FG);
            }

            wrapper.add(badge);
            return wrapper;
        }
    }
}