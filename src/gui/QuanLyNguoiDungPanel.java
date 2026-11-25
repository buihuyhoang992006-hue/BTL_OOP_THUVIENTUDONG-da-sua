package gui;

import dao.NguoiDungDAO;
import model.NguoiDung;
import org.kordamp.ikonli.feather.Feather;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Giao diện Quản lý người dùng (Dành riêng cho ADMIN)
 * Chức năng: Xem, Thêm, Sửa, Khóa tài khoản Thủ thư/Quản trị viên
 */
public class QuanLyNguoiDungPanel extends JPanel {

    private final NguoiDungDAO nguoiDungDAO = new NguoiDungDAO();
    private final int idQuanTri;
    private DefaultTableModel tableModel;
    private JTable tableNguoiDung;
    private TableRowSorter<DefaultTableModel> sorter;

    private JTextField txtId, txtHoTen, txtTaiKhoan, txtSdt, txtEmail, txtDiaChi, txtTimKiem;
    private JPasswordField txtMatKhau;
    private List<NguoiDung> danhSachTaiKhoan = List.of();
    private NguoiDung taiKhoanDangChon;
    private boolean dangTaiLaiBang;
    private boolean dangLuu;
    private JComboBox<String> cbVaiTro, cbTrangThai;
    private JButton btnThem, btnSua, btnKhoa, btnLamMoi;
    private final JLabel lblTrangThaiForm = ThemeConfig.createStatusLabel();

    public QuanLyNguoiDungPanel(int idQuanTri) {
        this.idQuanTri = idQuanTri;
        khoiTaoGiaoDien();
        taiDuLieuLenBang();
    }

    private void khoiTaoGiaoDien() {
        setLayout(new BorderLayout());
        setBackground(ThemeConfig.BG_PAGE);

        // ===== PHẦN TRÊN: FORM THÔNG TIN (SURFACE CARD) =====
        JPanel formCard = ThemeConfig.createCard();
        formCard.setLayout(new BorderLayout(0, 16));

        JPanel titlePanel = ThemeConfig.createTitleBlock(
                "THÔNG TIN TÀI KHOẢN NGƯỜI DÙNG",
                "Nhập và quản lý thông tin tài khoản nhân viên thư viện và quản trị viên hệ thống",
                Feather.USER_CHECK);
        formCard.add(titlePanel, BorderLayout.NORTH);

        // Form Fields (3 cột theo lưới chuẩn)
        JPanel gridFields = new JPanel(new GridLayout(3, 3, 18, 14));
        gridFields.setOpaque(false);

        // Hàng 1
        txtId = ThemeConfig.createTextField("ID tự sinh");
        txtId.setEditable(false);
        txtId.setBackground(ThemeConfig.SUBTLE_SURFACE);
        gridFields.add(taoFieldGroup("ID hệ thống", txtId, false));

        txtTaiKhoan = ThemeConfig.createTextField("Nhập tên đăng nhập...");
        gridFields.add(taoFieldGroup("Tên đăng nhập", txtTaiKhoan, true));

        txtMatKhau = ThemeConfig.createPasswordField("Nhập mật khẩu ban đầu...");
        gridFields.add(taoFieldGroup("Mật khẩu", txtMatKhau, true));

        // Hàng 2
        txtHoTen = ThemeConfig.createTextField("Nhập họ và tên đầy đủ...");
        gridFields.add(taoFieldGroup("Họ và tên", txtHoTen, true));

        cbVaiTro = new JComboBox<>(new String[] { "THU_THU", "ADMIN" });
        cbVaiTro.setFont(ThemeConfig.FONT_INPUT);
        cbVaiTro.setPreferredSize(new Dimension(0, ThemeConfig.INPUT_HEIGHT));
        gridFields.add(taoFieldGroup("Vai trò tài khoản", cbVaiTro, true));

        cbTrangThai = new JComboBox<>(new String[] { "Hoạt động", "Đã khóa" });
        cbTrangThai.setFont(ThemeConfig.FONT_INPUT);
        cbTrangThai.setPreferredSize(new Dimension(0, ThemeConfig.INPUT_HEIGHT));
        gridFields.add(taoFieldGroup("Trạng thái", cbTrangThai, true));

        // Hàng 3
        txtSdt = ThemeConfig.createTextField("Số điện thoại liên hệ...");
        gridFields.add(taoFieldGroup("Số điện thoại", txtSdt, false));

        txtEmail = ThemeConfig.createTextField("Địa chỉ email cá nhân...");
        gridFields.add(taoFieldGroup("Email", txtEmail, false));

        txtDiaChi = ThemeConfig.createTextField("Địa chỉ thường trú...");
        gridFields.add(taoFieldGroup("Địa chỉ", txtDiaChi, false));

        formCard.add(gridFields, BorderLayout.CENTER);

        // Toolbar buttons: Gỡ bỏ PreferredSize cố định để không bao giờ bị cắt chữ
        // thành "..."
        JPanel actionToolbar = new JPanel(new ThemeConfig.WrapFlowLayout(FlowLayout.RIGHT, 12, 8));
        actionToolbar.setOpaque(false);

        btnLamMoi = taoNutAction("Làm mới", Feather.REFRESH_CW, Color.WHITE, ThemeConfig.TEXT_PRIMARY,
                ThemeConfig.BORDER);
        btnKhoa = taoNutAction("Khóa / Mở khóa", Feather.SHIELD, new Color(0xFEF2F2), ThemeConfig.DANGER,
                new Color(0xFECACA));
        btnSua = taoNutAction("Cập nhật", Feather.EDIT_2, new Color(0x0284C7), Color.WHITE, new Color(0x0369A1));
        btnThem = taoNutAction("Thêm tài khoản", Feather.PLUS, ThemeConfig.PRIMARY, Color.WHITE,
                ThemeConfig.PRIMARY_HOVER);

        btnSua.setEnabled(false);
        btnKhoa.setEnabled(false);

        actionToolbar.add(btnLamMoi);
        actionToolbar.add(btnKhoa);
        actionToolbar.add(btnSua);
        actionToolbar.add(btnThem);

        JPanel formFooter = new JPanel(new BorderLayout(12, 0));
        formFooter.setOpaque(false);
        formFooter.add(lblTrangThaiForm, BorderLayout.CENTER);
        formFooter.add(actionToolbar, BorderLayout.EAST);

        formCard.add(formFooter, BorderLayout.SOUTH);

        // ===== PHẦN DƯỚI: BẢNG DỮ LIỆU (SURFACE CARD) =====
        JPanel tableCard = ThemeConfig.createCard();
        tableCard.setLayout(new BorderLayout(0, 16));

        JPanel tableHeaderBar = new JPanel(new BorderLayout(12, 0));
        tableHeaderBar.setOpaque(false);

        JLabel lblTableTitle = new JLabel("DANH SÁCH TÀI KHOẢN NGƯỜI DÙNG HỆ THỐNG");
        lblTableTitle.setIcon(ThemeConfig.getIcon(Feather.LIST, 18, ThemeConfig.PRIMARY));
        lblTableTitle.setIconTextGap(8);
        lblTableTitle.setFont(ThemeConfig.FONT_H3);
        lblTableTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        lblTableTitle.setBorder(new EmptyBorder(0, 0, 2, 0));

        txtTimKiem = new JTextField(20);
        JPanel searchBar = ThemeConfig.createSearchBar(
                txtTimKiem, "Tìm nhanh họ tên, tài khoản, SĐT, email...", 290);
        txtTimKiem.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { apDungBoLoc(); }
            @Override public void removeUpdate(DocumentEvent e) { apDungBoLoc(); }
            @Override public void changedUpdate(DocumentEvent e) { apDungBoLoc(); }
        });

        tableHeaderBar.add(lblTableTitle, BorderLayout.WEST);
        tableHeaderBar.add(searchBar, BorderLayout.EAST);
        tableCard.add(tableHeaderBar, BorderLayout.NORTH);

        String[] columnNames = { "ID", "Họ tên", "Tài khoản", "Vai trò", "SĐT", "Email", "Trạng thái" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableNguoiDung = new JTable(tableModel);
        ThemeConfig.styleTable(tableNguoiDung);
        tableNguoiDung.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sorter = new TableRowSorter<>(tableModel);
        tableNguoiDung.setRowSorter(sorter);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        tableNguoiDung.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tableNguoiDung.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        tableNguoiDung.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        tableNguoiDung.getColumnModel().getColumn(0).setPreferredWidth(50);
        tableNguoiDung.getColumnModel().getColumn(1).setPreferredWidth(160);
        tableNguoiDung.getColumnModel().getColumn(2).setPreferredWidth(120);
        tableNguoiDung.getColumnModel().getColumn(3).setPreferredWidth(140);
        tableNguoiDung.getColumnModel().getColumn(4).setPreferredWidth(100);
        tableNguoiDung.getColumnModel().getColumn(5).setPreferredWidth(160);
        tableNguoiDung.getColumnModel().getColumn(6).setPreferredWidth(120);

        // Custom Renderer cho Vai trò (ADMIN / THU_THU) dùng Ikonli Feather
        tableNguoiDung.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                        column);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setIconTextGap(6);
                lbl.setOpaque(true);
                String val = value != null ? value.toString() : "";
                if ("ADMIN".equals(val)) {
                    if (isSelected) {
                        lbl.setBackground(ThemeConfig.snap(ThemeConfig.PRIMARY_LIGHT));
                        lbl.setForeground(ThemeConfig.isDarkMode() ? new Color(0xDBEAFE) : ThemeConfig.PRIMARY_PRESSED);
                        lbl.setIcon(ThemeConfig.getIcon(Feather.SHIELD, 14, ThemeConfig.isDarkMode() ? new Color(0xDBEAFE) : ThemeConfig.PRIMARY_PRESSED));
                    } else {
                        lbl.setBackground(ThemeConfig.snap(ThemeConfig.STATUS_PENDING_BG));
                        lbl.setForeground(ThemeConfig.snap(ThemeConfig.STATUS_PENDING_FG));
                        lbl.setIcon(ThemeConfig.getIcon(Feather.SHIELD, 14, ThemeConfig.snap(ThemeConfig.STATUS_PENDING_FG)));
                    }
                    lbl.setText("Quản trị viên");
                } else {
                    if (isSelected) {
                        lbl.setBackground(ThemeConfig.snap(ThemeConfig.PRIMARY_LIGHT));
                        lbl.setForeground(ThemeConfig.isDarkMode() ? new Color(0xDBEAFE) : ThemeConfig.PRIMARY_PRESSED);
                        lbl.setIcon(ThemeConfig.getIcon(Feather.BOOK_OPEN, 14, ThemeConfig.isDarkMode() ? new Color(0xDBEAFE) : ThemeConfig.PRIMARY_PRESSED));
                    } else {
                        lbl.setBackground(ThemeConfig.snap(ThemeConfig.SUBTLE_SURFACE));
                        lbl.setForeground(ThemeConfig.snap(ThemeConfig.TEXT_SECONDARY));
                        lbl.setIcon(ThemeConfig.getIcon(Feather.BOOK_OPEN, 14, ThemeConfig.snap(ThemeConfig.TEXT_SECONDARY)));
                    }
                    lbl.setText("Thủ thư");
                }
                lbl.setFont(ThemeConfig.FONT_LABEL);
                return lbl;
            }
        });

        tableNguoiDung.getColumnModel().getColumn(6).setCellRenderer(new ThemeConfig.StatusBadgeRenderer());

        JScrollPane scrollPane = new JScrollPane(tableNguoiDung);
        scrollPane.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER, 1));
        scrollPane.setPreferredSize(new Dimension(0, 400));
        scrollPane.setMinimumSize(new Dimension(0, 360));
        ThemeConfig.installSaaSDataGrid(
                scrollPane, tableNguoiDung, Feather.USERS,
                "Chưa có tài khoản người dùng",
                "Sử dụng biểu mẫu phía trên để tạo tài khoản nhân sự mới.",
                52, 170, 125, 145, 110, 180, 125);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        ThemeConfig.ScrollablePanel pageContent = new ThemeConfig.ScrollablePanel();
        pageContent.setLayout(new BorderLayout(0, 22));
        pageContent.setOpaque(false);
        pageContent.setBorder(new EmptyBorder(24, 28, 24, 28));
        pageContent.add(formCard, BorderLayout.NORTH);
        pageContent.add(tableCard, BorderLayout.CENTER);

        JScrollPane pageScroll = ThemeConfig.createPageScrollPane(pageContent);
        add(pageScroll, BorderLayout.CENTER);
        SwingUtilities.invokeLater(() -> pageScroll.getVerticalScrollBar().setValue(0));

        // ===== GẮN SỰ KIỆN =====
        btnLamMoi.addActionListener(e -> {
            if (formDaSua() && JOptionPane.showConfirmDialog(this,
                    "Bỏ nội dung chưa lưu và tải lại danh sách tài khoản?",
                    "Làm mới", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }
            lamMoiForm();
            taiDuLieuLenBang();
        });
        btnThem.addActionListener(e -> themTaiKhoan());
        btnSua.addActionListener(e -> capNhatTaiKhoan());
        btnKhoa.addActionListener(e -> khoaTaiKhoan());

        tableNguoiDung.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !dangTaiLaiBang && !dangLuu) {
                hienThiTaiKhoanDangChon();
            }
        });
    }

    private void hienThiTaiKhoanDangChon() {
        int viewRow = tableNguoiDung.getSelectedRow();
        if (viewRow < 0) {
            return;
        }
        int row = tableNguoiDung.convertRowIndexToModel(viewRow);
        taiKhoanDangChon = danhSachTaiKhoan.get(row);
        txtId.setText(Integer.toString(taiKhoanDangChon.getId()));
        txtHoTen.setText(taiKhoanDangChon.getHoTen());
        txtTaiKhoan.setText(taiKhoanDangChon.getTenDangNhap());
        txtMatKhau.setText("");
        cbVaiTro.setSelectedItem(taiKhoanDangChon.getVaiTro());
        txtSdt.setText(taiKhoanDangChon.getSoDienThoai());
        txtEmail.setText(taiKhoanDangChon.getEmail());
        txtDiaChi.setText(taiKhoanDangChon.getDiaChi());
        cbTrangThai.setSelectedIndex(taiKhoanDangChon.isTrangThai() ? 0 : 1);
        capNhatTrangThaiNut();
    }

    private void capNhatTrangThaiNut() {
        boolean coTaiKhoan = taiKhoanDangChon != null;
        btnSua.setEnabled(!dangLuu && coTaiKhoan);
        btnThem.setEnabled(!dangLuu && !coTaiKhoan);
        btnKhoa.setEnabled(!dangLuu && coTaiKhoan);
        btnKhoa.setText(coTaiKhoan ? (taiKhoanDangChon.isTrangThai() ? "Khóa" : "Mở khóa") : "Khóa / Mở khóa");
        btnLamMoi.setEnabled(!dangLuu);
        txtTaiKhoan.setEditable(!dangLuu && !coTaiKhoan);
        txtMatKhau.setEditable(!dangLuu && !coTaiKhoan);
        txtHoTen.setEditable(!dangLuu);
        txtSdt.setEditable(!dangLuu);
        txtEmail.setEditable(!dangLuu);
        txtDiaChi.setEditable(!dangLuu);
        cbVaiTro.setEnabled(!dangLuu);
        cbTrangThai.setEnabled(!dangLuu);
        tableNguoiDung.setEnabled(!dangLuu);
    }

    private boolean formDaSua() {
        if (taiKhoanDangChon == null) {
            char[] kyTu = txtMatKhau.getPassword();
            boolean coMatKhau = kyTu.length > 0;
            Arrays.fill(kyTu, '\0');
            return coMatKhau || !txtTaiKhoan.getText().isBlank() || !txtHoTen.getText().isBlank()
                    || !txtSdt.getText().isBlank() || !txtEmail.getText().isBlank() || !txtDiaChi.getText().isBlank()
                    || cbVaiTro.getSelectedIndex() != 0 || cbTrangThai.getSelectedIndex() != 0;
        }
        return !txtHoTen.getText().equals(Objects.toString(taiKhoanDangChon.getHoTen(), ""))
                || !txtSdt.getText().equals(Objects.toString(taiKhoanDangChon.getSoDienThoai(), ""))
                || !txtEmail.getText().equals(Objects.toString(taiKhoanDangChon.getEmail(), ""))
                || !txtDiaChi.getText().equals(Objects.toString(taiKhoanDangChon.getDiaChi(), ""))
                || !Objects.equals(cbVaiTro.getSelectedItem(), taiKhoanDangChon.getVaiTro())
                || (cbTrangThai.getSelectedIndex() == 0) != taiKhoanDangChon.isTrangThai();
    }

    public void focusTimKiem() {
        txtTimKiem.requestFocusInWindow();
        txtTimKiem.selectAll();
    }

    public void taiDuLieu() {
        if (!dangLuu && !formDaSua()) {
            taiDuLieuLenBang();
        }
    }

    private void apDungBoLoc() {
        if (sorter == null) {
            return;
        }
        String tuKhoa = ThemeConfig.normalizeText(txtTimKiem.getText());
        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                if (tuKhoa.isEmpty()) {
                    return true;
                }
                String gop = ThemeConfig.normalizeText(
                        entry.getStringValue(1) + " "
                                + entry.getStringValue(2) + " "
                                + entry.getStringValue(3) + " "
                                + entry.getStringValue(4) + " "
                                + entry.getStringValue(5));
                return gop.contains(tuKhoa);
            }
        });
    }

    private JButton taoNutAction(String text, Feather icon, Color bg, Color fg, Color borderColor) {
        return ThemeConfig.styleActionButton(new JButton(text), icon, bg, fg, borderColor);
    }

    private JPanel taoFieldGroup(String labelText, JComponent component, boolean required) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);

        JPanel lblPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        lblPanel.setOpaque(false);

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(ThemeConfig.FONT_LABEL);
        lbl.setForeground(ThemeConfig.TEXT_PRIMARY);
        lblPanel.add(lbl);

        if (required) {
            JLabel star = new JLabel(" *");
            star.setFont(ThemeConfig.FONT_LABEL);
            star.setForeground(ThemeConfig.DANGER);
            lblPanel.add(star);
        }

        panel.add(lblPanel, BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private void taiDuLieuLenBang() {
        List<NguoiDung> moi;
        try {
            moi = nguoiDungDAO.layTatCaNguoiDung(idQuanTri);
        } catch (SQLException e) {
            hienThiLoi("Không tải được danh sách tài khoản. Kiểm tra kết nối MySQL.");
            return;
        }

        int idChon = taiKhoanDangChon == null ? 0 : taiKhoanDangChon.getId();
        dangTaiLaiBang = true;
        try {
            danhSachTaiKhoan = moi;
            tableModel.setRowCount(0);
            for (NguoiDung nd : moi) {
                String trangThai = nd.isTrangThai() ? "Hoạt động" : "Đã khóa";
                tableModel.addRow(new Object[] {
                        nd.getId(), nd.getHoTen(), nd.getTenDangNhap(),
                        nd.getVaiTro(), nd.getSoDienThoai(), nd.getEmail(), trangThai
                });
            }
        } finally {
            dangTaiLaiBang = false;
        }
        for (int i = 0; i < moi.size(); i++) {
            if (moi.get(i).getId() == idChon) {
                int viewRow = tableNguoiDung.convertRowIndexToView(i);
                if (viewRow >= 0) {
                    tableNguoiDung.setRowSelectionInterval(viewRow, viewRow);
                    return;
                }
                break;
            }
        }
        lamMoiForm();
        if (moi.isEmpty()) {
            hienThiLoi("Phiên hiện tại đã mất quyền quản trị tài khoản.");
        }
    }

    private void lamMoiForm() {
        taiKhoanDangChon = null;
        txtId.setText("");
        txtHoTen.setText("");
        txtTaiKhoan.setText("");
        txtMatKhau.setText("");
        txtSdt.setText("");
        txtEmail.setText("");
        txtDiaChi.setText("");
        cbVaiTro.setSelectedIndex(0);
        cbTrangThai.setSelectedIndex(0);
        tableNguoiDung.clearSelection();
        capNhatTrangThaiNut();
    }

    private boolean kiemTraLienHeHopLe() {
        String sdt = txtSdt.getText().trim();
        String email = txtEmail.getText().trim();
        if (!sdt.isEmpty() && !sdt.matches("\\d{9,11}")) {
            ThemeConfig.showStatus(lblTrangThaiForm, false, "Số điện thoại phải gồm 9-11 chữ số.");
            ThemeConfig.toastWarning(this, "Số điện thoại phải gồm 9 đến 11 chữ số.");
            return false;
        }
        if (!email.isEmpty() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            ThemeConfig.showStatus(lblTrangThaiForm, false, "Địa chỉ email không hợp lệ.");
            ThemeConfig.toastWarning(this, "Địa chỉ email không đúng định dạng.");
            return false;
        }
        return true;
    }

    private void themTaiKhoan() {
        char[] kyTu = txtMatKhau.getPassword();
        String matKhau = new String(kyTu).trim();
        Arrays.fill(kyTu, '\0');
        NguoiDung nd = layThongTinForm();
        if (nd.getHoTen().isEmpty() || nd.getTenDangNhap().isEmpty() || matKhau.isEmpty()) {
            hienThiLoi("Họ tên, tài khoản và mật khẩu không được để trống.");
            return;
        }
        if (!kiemTraLienHeHopLe()) {
            return;
        }
        nd.setMatKhau(matKhau);
        luuTaiKhoan(() -> nguoiDungDAO.themNguoiDung(idQuanTri, nd),
                "Đã thêm tài khoản @" + nd.getTenDangNhap() + ".", true);
    }

    private void capNhatTaiKhoan() {
        if (taiKhoanDangChon == null) {
            hienThiLoi("Vui lòng chọn tài khoản cần sửa.");
            return;
        }
        NguoiDung nd = layThongTinForm();
        if (nd.getHoTen().isEmpty()) {
            hienThiLoi("Họ tên không được để trống.");
            return;
        }
        if (!kiemTraLienHeHopLe()) {
            return;
        }
        nd.setId(taiKhoanDangChon.getId());
        luuTaiKhoan(() -> nguoiDungDAO.capNhatNguoiDung(idQuanTri, nd),
                "Đã cập nhật tài khoản @" + nd.getTenDangNhap() + ".", false);
    }

    private NguoiDung layThongTinForm() {
        NguoiDung nd = new NguoiDung();
        nd.setHoTen(txtHoTen.getText().trim());
        nd.setTenDangNhap(txtTaiKhoan.getText().trim());
        nd.setSoDienThoai(txtSdt.getText().trim());
        nd.setEmail(txtEmail.getText().trim());
        nd.setDiaChi(txtDiaChi.getText().trim());
        nd.setVaiTro((String) cbVaiTro.getSelectedItem());
        nd.setTrangThai(cbTrangThai.getSelectedIndex() == 0);
        return nd;
    }

    private void khoaTaiKhoan() {
        NguoiDung nd = taiKhoanDangChon;
        if (nd == null) {
            hienThiLoi("Vui lòng chọn một tài khoản từ bảng để thao tác.");
            return;
        }
        String thaoTac = nd.isTrangThai() ? "khóa" : "mở khóa";
        String thongBao = "Bạn có muốn " + thaoTac + " tài khoản @" + nd.getTenDangNhap() + "?";
        if (formDaSua()) {
            thongBao += "\nNội dung chưa lưu của form sẽ bị bỏ nếu thao tác thành công.";
        }
        int xacNhan = JOptionPane.showConfirmDialog(this, thongBao,
                "Cập nhật trạng thái", JOptionPane.YES_NO_OPTION);
        if (xacNhan == JOptionPane.YES_OPTION) {
            luuTaiKhoan(() -> nguoiDungDAO.doiTrangThaiNguoiDung(idQuanTri, nd.getId(), nd.isTrangThai()),
                    "Đã " + thaoTac + " tài khoản @" + nd.getTenDangNhap() + ".", false);
        }
    }

    private void luuTaiKhoan(Callable<NguoiDungDAO.KetQuaCapNhat> yeuCau, String thongBao, boolean themMoi) {
        if (dangLuu) {
            return;
        }
        dangLuu = true;
        capNhatTrangThaiNut();

        new SwingWorker<NguoiDungDAO.KetQuaCapNhat, Void>() {
            @Override
            protected NguoiDungDAO.KetQuaCapNhat doInBackground() throws Exception {
                return yeuCau.call();
            }

            @Override
            protected void done() {
                dangLuu = false;
                capNhatTrangThaiNut();
                try {
                    NguoiDungDAO.KetQuaCapNhat ketQua = get();
                    if (ketQua == NguoiDungDAO.KetQuaCapNhat.THANH_CONG) {
                        ThemeConfig.showStatus(lblTrangThaiForm, true, thongBao);
                        ThemeConfig.toastSuccess(QuanLyNguoiDungPanel.this, thongBao);
                        if (themMoi) {
                            lamMoiForm();
                        }
                        taiDuLieuLenBang();
                        MainFrame.refreshMainDashboard(QuanLyNguoiDungPanel.this);
                    } else {
                        hienThiLoi(switch (ketQua) {
                            case HET_QUYEN -> "Phiên hiện tại đã mất quyền quản trị tài khoản.";
                            case KHONG_TIM_THAY -> "Tài khoản đã không còn tồn tại. Hãy làm mới.";
                            case TRANG_THAI_DA_DOI -> "Trạng thái tài khoản đã thay đổi. Hãy làm mới.";
                            case ADMIN_CUOI_CUNG -> "Phải giữ ít nhất một quản trị viên đang hoạt động.";
                            case TAI_KHOAN_DANG_DUNG -> "Không thể khóa hoặc hạ quyền chính tài khoản đang dùng.";
                            case THANH_CONG -> throw new IllegalStateException();
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof SQLException sql && sql.getErrorCode() == 1062) {
                        hienThiLoi("Tên đăng nhập đã tồn tại.");
                    } else {
                        hienThiLoi("Không lưu được tài khoản. Kiểm tra thông tin nhập và kết nối MySQL.");
                    }
                }
            }
        }.execute();
    }

    private void hienThiLoi(String thongBao) {
        ThemeConfig.showStatus(lblTrangThaiForm, false, thongBao);
        ThemeConfig.toastError(this, thongBao);
    }
}
