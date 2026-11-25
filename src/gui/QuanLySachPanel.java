package gui;

import dao.SachDAO;
import model.NguoiDung;
import model.Sach;
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
import java.time.Year;
import java.util.List;
import java.util.Objects;

/**
 * Màn hình Quản lý Sách — Chuẩn CRUD Management Dashboard theo
 * UI_STYLE_GUIDE.md & ThemeConfig.
 */
public class QuanLySachPanel extends JPanel {

    private final SachDAO sachDAO = new SachDAO();

    private final JTextField txtMaSach = new JTextField();
    private final JTextField txtTenSach = new JTextField();
    private final JTextField txtTacGia = new JTextField();
    private final JTextField txtNhaXuatBan = new JTextField();
    private final JTextField txtNamXuatBan = new JTextField();
    private final JTextField txtSoLuongTong = new JTextField();
    private final JTextField txtGiaThue = new JTextField("10000");
    private final JTextField txtIdDanhMuc = new JTextField();
    private final JTextField txtHinhAnh = new JTextField();

    private final JTextArea txtMoTa = new JTextArea(3, 20);
    private final JCheckBox chkTrangThai = new JCheckBox("Đang hoạt động", true);

    private final JLabel lblSoLuongCon = new JLabel("—");
    private final JTextField txtTimKiem = new JTextField(22);
    private final JComboBox<String> cbLocTrangThai = new JComboBox<>(new String[] {
            "Tất cả trạng thái",
            "Còn sách (> 0)",
            "Hết sách (0)",
            "Hoạt động",
            "Ngừng"
    });
    private final JLabel lblKetQuaLoc = new JLabel("Hiển thị 0 / 0 đầu sách");
    private final JLabel lblTrangThaiForm = ThemeConfig.createStatusLabel();

    private final JButton btnThem = new JButton("Thêm sách");
    private final JButton btnSua = new JButton("Cập nhật");
    private final JButton btnXoa = new JButton("Xóa");
    private final JButton btnLamMoi = new JButton("Làm mới form");

    private int selectedId = -1;
    private List<Object> noiDungDaNap = List.of();
    private int soLuongTongCu;
    private int soLuongConHienTai;

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] {
                    "ID", "Mã sách", "Tên sách", "Tác giả",
                    "NXB", "Năm", "Giá thuê", "SL tổng", "SL còn",
                    "Danh mục", "Trạng thái"
            },
            0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0, 5, 7, 8 -> Integer.class;
                case 6 -> Long.class;
                default -> String.class;
            };
        }
    };

    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);

    public QuanLySachPanel() {
        setLayout(new BorderLayout());
        setBackground(ThemeConfig.BG_PAGE);

        ThemeConfig.ScrollablePanel noiDung = new ThemeConfig.ScrollablePanel();
        noiDung.setLayout(new BorderLayout(0, 22));
        noiDung.setOpaque(false);
        noiDung.setBorder(new EmptyBorder(24, 28, 24, 28));
        noiDung.add(taoFormNhap(), BorderLayout.NORTH);
        noiDung.add(taoBangSach(), BorderLayout.CENTER);

        JScrollPane pageScroll = ThemeConfig.createPageScrollPane(noiDung);
        add(pageScroll, BorderLayout.CENTER);

        loadTable();
        SwingUtilities.invokeLater(() -> pageScroll.getVerticalScrollBar().setValue(0));
    }

    public QuanLySachPanel(NguoiDung nguoiDung) {
        this();
    }

    // ============================================================
    // 1. FORM NHẬP LIỆU THÔNG TIN SÁCH (Top-Aligned Labels)
    // ============================================================
    private JPanel taoFormNhap() {
        JPanel card = new ThemeConfig.ModernCardPanel(new BorderLayout(0, 16));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titleBlock = ThemeConfig.createTitleBlock(
                "THÔNG TIN ĐẦU SÁCH",
                "Thêm mới hoặc chọn dòng trong bảng để chỉnh sửa thông tin sách",
                Feather.BOOK);

        JPanel tonKhoBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        tonKhoBox.setOpaque(false);
        JLabel lblTonKhoTag = new JLabel("Số bản còn trong kho:");
        lblTonKhoTag.setFont(ThemeConfig.FONT_SM);
        lblTonKhoTag.setForeground(ThemeConfig.TEXT_SECONDARY);

        lblSoLuongCon.setFont(ThemeConfig.FONT_BTN);
        lblSoLuongCon.setForeground(ThemeConfig.PRIMARY);
        lblSoLuongCon.setBackground(ThemeConfig.PRIMARY_LIGHT);
        lblSoLuongCon.setOpaque(true);
        lblSoLuongCon.setBorder(new EmptyBorder(3, 10, 3, 10));

        tonKhoBox.add(lblTonKhoTag);
        tonKhoBox.add(lblSoLuongCon);

        headerPanel.add(titleBlock, BorderLayout.WEST);
        headerPanel.add(tonKhoBox, BorderLayout.EAST);
        card.add(headerPanel, BorderLayout.NORTH);

        JPanel formGrid = new JPanel(new GridBagLayout());
        formGrid.setOpaque(false);
        GridBagConstraints fg = new GridBagConstraints();
        fg.fill = GridBagConstraints.HORIZONTAL;
        fg.weightx = 1.0;
        fg.insets = new Insets(6, 9, 6, 9);

        setupField(txtMaSach, "Nhập mã sách duy nhất...");
        setupField(txtTenSach, "Tên sách...");
        setupField(txtTacGia, "Tên tác giả...");
        setupField(txtNhaXuatBan, "Nhà xuất bản...");
        setupField(txtNamXuatBan, "Năm xuất bản (VD: 2024)...");
        setupField(txtSoLuongTong, "Tổng số bản nhập về...");
        setupField(txtGiaThue, "Giá thuê / lượt (VD: 10000)...");
        setupField(txtIdDanhMuc, "Mã danh mục (tùy chọn)...");
        setupField(txtHinhAnh, "Tên file ảnh / URL...");

        chkTrangThai.setFont(ThemeConfig.FONT_BODY);
        chkTrangThai.setOpaque(false);
        chkTrangThai.setFocusPainted(false);

        fg.gridy = 0;
        fg.gridx = 0; formGrid.add(createFieldGroup("Mã sách", true, txtMaSach), fg);
        fg.gridx = 1; formGrid.add(createFieldGroup("Tên sách", true, txtTenSach), fg);
        fg.gridx = 2; formGrid.add(createFieldGroup("Tác giả", true, txtTacGia), fg);

        fg.gridy = 1;
        fg.gridx = 0; formGrid.add(createFieldGroup("Nhà xuất bản", false, txtNhaXuatBan), fg);
        fg.gridx = 1; formGrid.add(createFieldGroup("Năm xuất bản", true, txtNamXuatBan), fg);
        fg.gridx = 2; formGrid.add(createFieldGroup("Số lượng tổng", true, txtSoLuongTong), fg);

        fg.gridy = 2;
        fg.gridx = 0; formGrid.add(createFieldGroup("Giá thuê / lượt (VNĐ)", true, txtGiaThue), fg);
        fg.gridx = 1; formGrid.add(createFieldGroup("ID Danh mục", false, txtIdDanhMuc), fg);
        fg.gridx = 2; formGrid.add(createFieldGroup("Hình ảnh", false, txtHinhAnh), fg);

        txtMoTa.setFont(ThemeConfig.FONT_BODY);
        txtMoTa.setLineWrap(true);
        txtMoTa.setWrapStyleWord(true);
        JScrollPane scrollMoTa = new JScrollPane(txtMoTa);
        scrollMoTa.setPreferredSize(new Dimension(0, 56));
        scrollMoTa.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER, 1));

        JPanel moTaHeader = new JPanel(new BorderLayout(12, 0));
        moTaHeader.setOpaque(false);
        JLabel lblMoTa = new JLabel("Mô tả tóm tắt nội dung:");
        lblMoTa.setFont(ThemeConfig.FONT_LABEL);
        lblMoTa.setForeground(ThemeConfig.TEXT_PRIMARY);
        moTaHeader.add(lblMoTa, BorderLayout.WEST);
        moTaHeader.add(chkTrangThai, BorderLayout.EAST);

        JPanel moTaGroup = new JPanel(new BorderLayout(0, 4));
        moTaGroup.setOpaque(false);
        moTaGroup.add(moTaHeader, BorderLayout.NORTH);
        moTaGroup.add(scrollMoTa, BorderLayout.CENTER);

        JPanel centerForm = new JPanel(new BorderLayout(0, 10));
        centerForm.setOpaque(false);
        centerForm.add(formGrid, BorderLayout.NORTH);
        centerForm.add(moTaGroup, BorderLayout.CENTER);
        card.add(centerForm, BorderLayout.CENTER);

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
        formFooter.add(lblTrangThaiForm, BorderLayout.WEST);
        formFooter.add(nut, BorderLayout.EAST);

        card.add(formFooter, BorderLayout.SOUTH);

        btnThem.addActionListener(e -> themSach());
        btnSua.addActionListener(e -> capNhatSach());
        btnXoa.addActionListener(e -> xoaSach());
        btnLamMoi.addActionListener(e -> {
            txtTimKiem.setText("");
            cbLocTrangThai.setSelectedIndex(0);
            ThemeConfig.showStatus(lblTrangThaiForm, true, "");
            loadTable();
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
    // 2. BẢNG DỮ LIỆU KHO SÁCH (Data-Dense Table)
    // ============================================================
    private JPanel taoBangSach() {
        JPanel card = new ThemeConfig.ModernCardPanel(new BorderLayout(0, 14));

        JPanel cardHeader = new JPanel(new BorderLayout(12, 0));
        cardHeader.setOpaque(false);
        cardHeader.add(
                ThemeConfig.createTitleBlock(
                        "DANH SÁCH SÁCH TRONG THƯ VIỆN",
                        "Tra cứu nhanh, lọc trạng thái tồn kho và chọn dòng để chỉnh sửa",
                        Feather.LIST),
                BorderLayout.WEST);

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
        cardHeader.add(countWrapper, BorderLayout.EAST);

        JPanel searchGroup = new JPanel(new ThemeConfig.WrapFlowLayout(FlowLayout.LEFT, 12, 8));
        searchGroup.setOpaque(false);

        JLabel lblKey = new JLabel("Tìm nhanh:");
        lblKey.setFont(ThemeConfig.FONT_LABEL);
        lblKey.setForeground(ThemeConfig.TEXT_PRIMARY);

        JPanel searchBar = ThemeConfig.createSearchBar(txtTimKiem, "Gõ tên sách, mã, tác giả, NXB...", 280);

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
                new JButton("Đặt lại lọc"),
                Feather.LIST,
                Color.WHITE,
                ThemeConfig.TEXT_PRIMARY,
                ThemeConfig.BORDER);

        JButton btnXuatCsv = ThemeConfig.styleActionButton(
                new JButton("Xuất Excel (CSV)"),
                Feather.DOWNLOAD,
                Color.WHITE,
                ThemeConfig.SUCCESS_HOVER,
                ThemeConfig.BORDER);

        searchGroup.add(lblKey);
        searchGroup.add(searchBar);
        searchGroup.add(lblLoc);
        searchGroup.add(cbLocTrangThai);
        searchGroup.add(btnTim);
        searchGroup.add(btnTatCa);
        searchGroup.add(btnXuatCsv);

        JPanel topSection = new JPanel(new BorderLayout(0, 10));
        topSection.setOpaque(false);
        topSection.add(cardHeader, BorderLayout.NORTH);
        topSection.add(searchGroup, BorderLayout.SOUTH);
        card.add(topSection, BorderLayout.NORTH);

        ThemeConfig.styleTable(table);
        table.setRowHeight(ThemeConfig.TABLE_ROW_HEIGHT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSorter(sorter);

        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(50);
        cm.getColumn(1).setPreferredWidth(100);
        cm.getColumn(2).setPreferredWidth(220);
        cm.getColumn(3).setPreferredWidth(150);
        cm.getColumn(4).setPreferredWidth(125);
        cm.getColumn(5).setPreferredWidth(68);
        cm.getColumn(6).setPreferredWidth(115);
        cm.getColumn(7).setPreferredWidth(75);
        cm.getColumn(8).setPreferredWidth(115);
        cm.getColumn(9).setPreferredWidth(120);
        cm.getColumn(10).setPreferredWidth(110);

        cm.getColumn(0).setCellRenderer(new CenterRenderer());
        cm.getColumn(1).setCellRenderer(new CenterRenderer());
        cm.getColumn(2).setCellRenderer(new LeftRenderer());
        cm.getColumn(3).setCellRenderer(new LeftRenderer());
        cm.getColumn(4).setCellRenderer(new LeftRenderer());
        cm.getColumn(5).setCellRenderer(new CenterRenderer());
        cm.getColumn(6).setCellRenderer(new GiaThueCurrencyRenderer());
        cm.getColumn(7).setCellRenderer(new RightRenderer());
        cm.getColumn(8).setCellRenderer(new SoLuongConBadgeRenderer());
        cm.getColumn(9).setCellRenderer(new LeftRenderer());
        cm.getColumn(10).setCellRenderer(new TrangThaiBadgeRenderer());

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER, 1));
        scrollTable.getViewport().setBackground(ThemeConfig.SURFACE_CARD);
        scrollTable.setPreferredSize(new Dimension(0, 450));
        scrollTable.setMinimumSize(new Dimension(0, 420));
        ThemeConfig.installSaaSDataGrid(
                scrollTable, table, Feather.BOOK,
                "Kho sách hiện đang trống",
                "Điền thông tin đầu sách ở biểu mẫu phía trên và nhấn 'Thêm sách' để khởi tạo.",
                52, 100, 220, 150, 125, 70, 115, 78, 115, 125, 115);
        card.add(scrollTable, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                hienThiSachDangChon();
            }
        });

        btnTim.addActionListener(e -> apDungBoLoc());
        txtTimKiem.addActionListener(e -> apDungBoLoc());
        cbLocTrangThai.addActionListener(e -> apDungBoLoc());

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

        btnXuatCsv.addActionListener(e ->
                CsvExportUtil.exportTableToCsv(this, table, "Danh_Muc_Sach", "DANH MỤC SÁCH"));

        return card;
    }

    // ================= TẢI DANH SÁCH =================

    private void loadTable() {
        hienThiDanhSach(sachDAO.getAll());
    }

    public void taiDuLieuGiuLuaChon() {
        if (!noiDungDaNap.equals(noiDungForm())) {
            return;
        }
        int prevId = selectedId;
        loadTable();
        if (prevId > 0) {
            for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                Object idVal = tableModel.getValueAt(modelRow, 0);
                if (idVal instanceof Number n && n.intValue() == prevId) {
                    table.setRowSelectionInterval(viewRow, viewRow);
                    break;
                }
            }
        }
    }

    private void hienThiDanhSach(List<Sach> danhSach) {
        lamMoiForm();
        tableModel.setRowCount(0);

        for (Sach sach : danhSach) {
            tableModel.addRow(new Object[] {
                    sach.getId(),
                    sach.getMaSach(),
                    sach.getTenSach(),
                    sach.getTacGia(),
                    sach.getNhaXuatBan(),
                    sach.getNamXuatBan(),
                    sach.getGiaThue(),
                    sach.getSoLuongTong(),
                    sach.getSoLuongCon(),
                    sach.getTenDanhMuc() == null
                            ? "Chưa phân loại"
                            : sach.getTenDanhMuc(),
                    sach.isTrangThai() ? "Hoạt động" : "Ngừng"
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
                                + entry.getStringValue(5) + " "
                                + entry.getStringValue(9));

                boolean khopTuKhoa = tuKhoa.isEmpty() || gopText.contains(tuKhoa);

                int slCon = 0;
                Object slVal = entry.getValue(8);
                if (slVal instanceof Number n) {
                    slCon = n.intValue();
                }
                String trangThai = entry.getStringValue(10);

                boolean khopTrangThai = switch (locTrangThai) {
                    case "Còn sách (> 0)" -> slCon > 0;
                    case "Hết sách (0)" -> slCon == 0;
                    case "Hoạt động" -> "Hoạt động".equals(trangThai);
                    case "Ngừng" -> "Ngừng".equals(trangThai);
                    default -> true;
                };

                return khopTuKhoa && khopTrangThai;
            }
        });

        lblKetQuaLoc.setText("Hiển thị " + table.getRowCount() + " / " + tableModel.getRowCount() + " đầu sách");
    }

    public void focusTimKiem() {
        Container parent = txtTimKiem.getParent();
        if (parent instanceof JComponent jc) {
            jc.scrollRectToVisible(new Rectangle(0, 0, jc.getWidth(), jc.getHeight()));
        }
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

    // ================= THÊM =================

    private void themSach() {
        if (!kiemTraDuLieu()) {
            return;
        }

        if (sachDAO.tonTaiMaSach(txtMaSach.getText().trim())) {
            ThemeConfig.showStatus(lblTrangThaiForm, false, "Mã sách đã tồn tại trong hệ thống.");
            ThemeConfig.toastWarning(this, "Mã sách đã tồn tại trong hệ thống.");
            return;
        }

        Sach sach = docSachTuForm();
        sach.setSoLuongCon(sach.getSoLuongTong());

        if (sachDAO.themSach(sach)) {
            ThemeConfig.showStatus(lblTrangThaiForm, true, "Đã thêm sách \"" + sach.getTenSach() + "\" thành công.");
            ThemeConfig.toastSuccess(this, "Đã thêm đầu sách \"" + sach.getTenSach() + "\".");
            loadTable();
            MainFrame.refreshMainDashboard(this);
        } else {
            ThemeConfig.showStatus(lblTrangThaiForm, false, "Không thêm được sách. Kiểm tra kết nối và dữ liệu.");
            ThemeConfig.toastError(this, "Không thêm được sách. Hãy kiểm tra mã sách, danh mục và kết nối.");
        }
    }

    // ================= CẬP NHẬT =================

    private void capNhatSach() {
        if (selectedId <= 0) {
            ThemeConfig.toastWarning(this, "Hãy chọn sách cần cập nhật.");
            return;
        }

        if (!kiemTraDuLieu()) {
            return;
        }

        String maMoi = txtMaSach.getText().trim();
        Sach trung = sachDAO.getByMaSach(maMoi);
        if (trung != null && trung.getId() != selectedId) {
            ThemeConfig.showStatus(lblTrangThaiForm, false, "Mã sách \"" + maMoi + "\" đã thuộc về đầu sách khác.");
            ThemeConfig.toastWarning(this, "Mã sách \"" + maMoi + "\" đã thuộc về một đầu sách khác.");
            return;
        }

        Sach sach = docSachTuForm();
        sach.setId(selectedId);

        long soLuongConMoi = (long) soLuongConHienTai
                + sach.getSoLuongTong()
                - soLuongTongCu;

        if (soLuongConMoi < 0
                || soLuongConMoi > Integer.MAX_VALUE) {
            ThemeConfig.showStatus(lblTrangThaiForm, false, "Tổng số lượng mới nhỏ hơn số sách đang được mượn.");
            ThemeConfig.toastWarning(this, "Không thể giảm tổng số bản thấp hơn số bản đang được mượn.");
            return;
        }

        sach.setSoLuongCon((int) soLuongConMoi);

        boolean thanhCong = sachDAO.capNhatSach(
                sach,
                soLuongTongCu,
                soLuongConHienTai);

        if (thanhCong) {
            ThemeConfig.showStatus(lblTrangThaiForm, true, "Đã cập nhật đầu sách \"" + sach.getTenSach() + "\".");
            ThemeConfig.toastSuccess(this, "Đã cập nhật đầu sách \"" + sach.getTenSach() + "\".");
            loadTable();
            MainFrame.refreshMainDashboard(this);
        } else {
            ThemeConfig.showStatus(lblTrangThaiForm, false, "Cập nhật thất bại hoặc tồn kho vừa thay đổi.");
            ThemeConfig.toastError(this, "Không lưu được. Tồn kho có thể đã thay đổi, hãy làm mới và thử lại.");
        }
    }

    private Sach docSachTuForm() {
        Sach sach = new Sach();

        sach.setMaSach(txtMaSach.getText().trim());
        sach.setTenSach(txtTenSach.getText().trim());
        sach.setTacGia(txtTacGia.getText().trim());
        sach.setNhaXuatBan(txtNhaXuatBan.getText().trim());

        sach.setNamXuatBan(
                Integer.parseInt(txtNamXuatBan.getText().trim()));

        sach.setSoLuongTong(
                Integer.parseInt(txtSoLuongTong.getText().trim()));

        sach.setGiaThue(
                parseGiaThue(txtGiaThue.getText()));

        String idDanhMuc = txtIdDanhMuc.getText().trim();

        sach.setIdDanhMuc(
                idDanhMuc.isEmpty()
                        ? 0
                        : Integer.parseInt(idDanhMuc));

        sach.setMoTa(txtMoTa.getText().trim());
        sach.setHinhAnh(txtHinhAnh.getText().trim());
        sach.setTrangThai(chkTrangThai.isSelected());

        return sach;
    }

    // ================= XÓA =================

    private void xoaSach() {
        if (selectedId <= 0) {
            JOptionPane.showMessageDialog(
                    this, "Hãy chọn sách cần xóa.");
            return;
        }

        int chon = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc muốn xóa sách đã chọn?",
                "Xác nhận",
                JOptionPane.YES_NO_OPTION);

        if (chon != JOptionPane.YES_OPTION) {
            return;
        }

        if (sachDAO.xoaSach(selectedId)) {
            ThemeConfig.showStatus(lblTrangThaiForm, true, "Đã xóa đầu sách được chọn.");
            ThemeConfig.toastSuccess(this, "Đã xóa đầu sách được chọn.");
            loadTable();
            MainFrame.refreshMainDashboard(this);
        } else {
            ThemeConfig.showStatus(lblTrangThaiForm, false, "Không thể xóa sách đang có lịch sử mượn trả.");
            ThemeConfig.toastWarning(this, "Không thể xóa sách đã có lịch sử mượn. Hãy chuyển sang ngừng sử dụng.");
        }
    }

    // ================= TÌM KIẾM / CHỌN SÁCH =================

    private void hienThiSachDangChon() {
        int dongChon = table.getSelectedRow();

        if (dongChon < 0) {
            btnSua.setEnabled(false);
            btnXoa.setEnabled(false);
            return;
        }

        int dongModel = table.convertRowIndexToModel(dongChon);

        int id = ((Number) tableModel.getValueAt(dongModel, 0))
                .intValue();

        Sach sach = sachDAO.getById(id);

        if (sach == null) {
            lamMoiForm();
            ThemeConfig.toastWarning(this, "Không đọc được sách đã chọn. Hãy làm mới danh sách.");
            return;
        }

        selectedId = sach.getId();
        btnSua.setEnabled(true);
        btnXoa.setEnabled(true);

        soLuongTongCu = sach.getSoLuongTong();
        soLuongConHienTai = sach.getSoLuongCon();

        txtMaSach.setText(sach.getMaSach());
        txtTenSach.setText(sach.getTenSach());
        txtTacGia.setText(sach.getTacGia());
        txtNhaXuatBan.setText(sach.getNhaXuatBan());

        txtNamXuatBan.setText(
                String.valueOf(sach.getNamXuatBan()));

        txtSoLuongTong.setText(
                String.valueOf(soLuongTongCu));

        txtGiaThue.setText(
                String.valueOf(sach.getGiaThue()));

        txtIdDanhMuc.setText(
                sach.getIdDanhMuc() == 0
                        ? ""
                        : String.valueOf(sach.getIdDanhMuc()));

        txtMoTa.setText(sach.getMoTa());
        txtHinhAnh.setText(sach.getHinhAnh());
        chkTrangThai.setSelected(sach.isTrangThai());

        lblSoLuongCon.setText(
                String.valueOf(soLuongConHienTai));
        noiDungDaNap = noiDungForm();
    }

    // ================= KIỂM TRA DỮ LIỆU =================

    private boolean kiemTraDuLieu() {
        if (txtMaSach.getText().trim().isEmpty()
                || txtTenSach.getText().trim().isEmpty()
                || txtTacGia.getText().trim().isEmpty()) {
            ThemeConfig.showStatus(lblTrangThaiForm, false, "Vui lòng nhập đủ Mã sách, Tên sách và Tác giả.");
            ThemeConfig.toastWarning(this, "Mã sách, tên sách và tác giả không được để trống.");
            return false;
        }

        try {
            int nam = Integer.parseInt(
                    txtNamXuatBan.getText().trim());

            int tong = Integer.parseInt(
                    txtSoLuongTong.getText().trim());

            long giaThue = parseGiaThue(txtGiaThue.getText());

            String idText = txtIdDanhMuc.getText().trim();
            int idDanhMuc = idText.isEmpty()
                    ? 0
                    : Integer.parseInt(idText);

            int namToiDa = Year.now().getValue() + 1;
            if (nam < 1500 || nam > namToiDa) {
                ThemeConfig.showStatus(lblTrangThaiForm, false, "Năm xuất bản không hợp lệ (1500 - " + namToiDa + ").");
                ThemeConfig.toastWarning(this, "Năm xuất bản phải từ 1500 đến " + namToiDa + ".");
                return false;
            }

            if (tong < 0 || idDanhMuc < 0 || giaThue < 0) {
                ThemeConfig.showStatus(lblTrangThaiForm, false, "Số lượng tổng, giá thuê và ID danh mục không được âm.");
                ThemeConfig.toastWarning(this, "Số lượng, giá thuê và ID danh mục không được âm.");
                return false;
            }

        } catch (NumberFormatException e) {
            ThemeConfig.showStatus(lblTrangThaiForm, false, "Năm, số lượng, giá thuê và ID danh mục phải là số nguyên.");
            ThemeConfig.toastWarning(this, "Năm, số lượng, giá thuê và ID danh mục phải là số nguyên.");
            return false;
        }

        return true;
    }

    private static long parseGiaThue(String raw) {
        String cleaned = raw == null ? "" : raw.replace(",", "").replace(".", "").trim();
        if (cleaned.isEmpty()) {
            throw new NumberFormatException("Giá thuê trống");
        }
        return Long.parseLong(cleaned);
    }

    private void lamMoiForm() {
        selectedId = -1;
        soLuongTongCu = 0;
        soLuongConHienTai = 0;

        btnSua.setEnabled(false);
        btnXoa.setEnabled(false);

        JTextField[] cacO = {
                txtMaSach, txtTenSach, txtTacGia, txtNhaXuatBan,
                txtNamXuatBan, txtSoLuongTong,
                txtIdDanhMuc, txtHinhAnh
        };

        for (JTextField o : cacO) {
            o.setText("");
        }
        txtGiaThue.setText("10000");

        txtMoTa.setText("");
        chkTrangThai.setSelected(true);
        lblSoLuongCon.setText("—");
        table.clearSelection();
        noiDungDaNap = noiDungForm();
    }

    private List<Object> noiDungForm() {
        return List.of(txtMaSach.getText(), txtTenSach.getText(), txtTacGia.getText(),
                txtNhaXuatBan.getText(), txtNamXuatBan.getText(), txtSoLuongTong.getText(),
                txtGiaThue.getText(), txtIdDanhMuc.getText(), txtHinhAnh.getText(), txtMoTa.getText(),
                chkTrangThai.isSelected());
    }

    // ============================================================
    // RENDERERS CHO BẢNG SÁCH
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

    private static class GiaThueCurrencyRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            long amount = value instanceof Number n ? n.longValue() : 0L;
            setText(String.format("%,d đ", amount));
            setFont(ThemeConfig.FONT_BTN);
            setBorder(new EmptyBorder(0, 10, 0, 10));
            setHorizontalAlignment(SwingConstants.RIGHT);
            if (!isSelected) {
                setBackground(row % 2 == 0 ? ThemeConfig.snap(ThemeConfig.SURFACE_CARD) : ThemeConfig.snap(ThemeConfig.BG_PAGE));
                setForeground(ThemeConfig.snap(ThemeConfig.PRIMARY));
            }
            return this;
        }
    }

    private static class RightRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(ThemeConfig.FONT_BODY);
            setBorder(new EmptyBorder(0, 10, 0, 10));
            setHorizontalAlignment(SwingConstants.RIGHT);
            if (!isSelected) {
                setBackground(row % 2 == 0 ? ThemeConfig.snap(ThemeConfig.SURFACE_CARD) : ThemeConfig.snap(ThemeConfig.BG_PAGE));
                setForeground(ThemeConfig.snap(ThemeConfig.TEXT_PRIMARY));
            }
            return this;
        }
    }

    private static class SoLuongConBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            int sl = 0;
            if (value instanceof Number n) {
                sl = n.intValue();
            }

            JPanel wrapper = new JPanel(new GridBagLayout());
            if (isSelected) {
                wrapper.setBackground(ThemeConfig.snap(ThemeConfig.PRIMARY_LIGHT));
            } else {
                wrapper.setBackground(row % 2 == 0 ? ThemeConfig.snap(ThemeConfig.SURFACE_CARD) : ThemeConfig.snap(ThemeConfig.BG_PAGE));
            }

            String text = sl == 0 ? "Hết sách (0)" : "Còn " + sl;
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
            badge.setBorder(new EmptyBorder(2, 8, 2, 8));

            if (sl == 0) {
                badge.setBackground(ThemeConfig.STATUS_ERROR_BG);
                badge.setForeground(ThemeConfig.STATUS_ERROR_FG);
            } else {
                badge.setBackground(ThemeConfig.STATUS_OK_BG);
                badge.setForeground(ThemeConfig.STATUS_OK_FG);
            }

            wrapper.add(badge);
            return wrapper;
        }
    }

    private static class TrangThaiBadgeRenderer extends DefaultTableCellRenderer {
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

            JLabel badge = new JLabel(active ? "Hoạt động" : "Ngừng") {
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
                badge.setBackground(ThemeConfig.SUBTLE_SURFACE);
                badge.setForeground(ThemeConfig.TEXT_MUTED);
            }

            wrapper.add(badge);
            return wrapper;
        }
    }
}