package gui;

import dao.DatTruocDAO;
import dao.KetNoiDatabase;
import dao.ThongBaoDAO;
import model.DatTruocChiTiet;
import model.ThongBaoChiTiet;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import service.DatTruocService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.Connection;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Màn hình Đặt trước sách & Thông báo — Thiết kế chuẩn Modern SaaS Dashboard
 * theo UI_STYLE_GUIDE.md.
 */
public class DatTruocPanel extends JPanel {
    private final DatTruocService datTruocService = new DatTruocService();
    private final DatTruocDAO datTruocDAO = new DatTruocDAO();
    private final ThongBaoDAO thongBaoDAO = new ThongBaoDAO();

    private JTextField txtIdDocGia, txtIdSach, txtSoLuong, txtTimKiem;
    private JComboBox<String> cbLocTrangThai;
    private JButton btnGuiDatTruoc, btnHuyDatTruoc, btnChoMuonNgay;
    private JLabel lblKetQuaHangDoi;
    private DefaultTableModel modelGioDat, modelDanhSachDat, modelThongBao;
    private JTable tblDanhSachDat;
    private TableRowSorter<DefaultTableModel> sorterHangDoi;
    private final Map<Integer, Integer> gioDatTruoc = new HashMap<>();
    private List<DatTruocChiTiet> danhSachHienTai = new ArrayList<>();
    private boolean dangCapNhatBoLoc;

    public DatTruocPanel() {
        setLayout(new BorderLayout());
        setBackground(ThemeConfig.BG_PAGE);

        // ============================================================
        // BỐ CỤC PAGE-LEVEL SCROLLPANE:
        //   - Phần trên (>= 430px): Cột trái (350px) + Cột phải (giãn hết)
        //   - Phần dưới (>= 320px): Hộp thư thông báo trải rộng 100%
        // ============================================================
        ThemeConfig.ScrollablePanel pageContent = new ThemeConfig.ScrollablePanel();
        pageContent.setLayout(new BorderLayout(0, 22));
        pageContent.setOpaque(false);
        pageContent.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel pnlTop = new JPanel(new BorderLayout(20, 0));
        pnlTop.setOpaque(false);

        // ── CỘT TRÁI (350px cố định): 1. TẠO ĐẶT TRƯỚC SÁCH ───────
        JPanel pnlLeft = createSurfaceCard(
                "1. TẠO ĐẶT TRƯỚC SÁCH",
                "Nhập độc giả và sách cần giữ chỗ",
                Feather.PLUS_CIRCLE);
        pnlLeft.setPreferredSize(new Dimension(350, 510));
        pnlLeft.setMinimumSize(new Dimension(350, 430));

        JPanel pnlInput = new JPanel(new GridBagLayout());
        pnlInput.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 2, 5, 2);

        txtIdDocGia = new JTextField();
        txtIdDocGia.setFont(ThemeConfig.FONT_INPUT);
        txtIdDocGia.setPreferredSize(new Dimension(120, ThemeConfig.INPUT_HEIGHT));
        txtIdDocGia.putClientProperty("JTextField.placeholderText", "Nhập ID độc giả...");

        txtIdSach = new JTextField();
        txtIdSach.setFont(ThemeConfig.FONT_INPUT);
        txtIdSach.setPreferredSize(new Dimension(120, ThemeConfig.INPUT_HEIGHT));
        txtIdSach.putClientProperty("JTextField.placeholderText", "Nhập ID sách...");

        txtSoLuong = new JTextField("1");
        txtSoLuong.setFont(ThemeConfig.FONT_INPUT);
        txtSoLuong.setPreferredSize(new Dimension(120, ThemeConfig.INPUT_HEIGHT));
        txtSoLuong.putClientProperty("JTextField.placeholderText", "Số lượng...");

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        pnlInput.add(createInputLabel("ID Độc giả:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        pnlInput.add(txtIdDocGia, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        pnlInput.add(createInputLabel("ID Sách:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        pnlInput.add(txtIdSach, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        pnlInput.add(createInputLabel("Số lượng:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        pnlInput.add(txtSoLuong, gbc);

        JButton btnThemVaoGio = ThemeConfig.styleActionButton(
                new JButton("Thêm vào giỏ"),
                Feather.PLUS,
                Color.WHITE,
                ThemeConfig.TEXT_PRIMARY,
                ThemeConfig.BORDER);

        btnGuiDatTruoc = ThemeConfig.styleActionButton(
                new JButton("Xác nhận đặt trước"),
                Feather.CHECK,
                ThemeConfig.SUCCESS,
                Color.WHITE,
                ThemeConfig.SUCCESS_HOVER);
        btnGuiDatTruoc.setEnabled(false);

        JPanel pnlBtns1 = new JPanel(new GridLayout(2, 1, 0, 8));
        pnlBtns1.setOpaque(false);
        pnlBtns1.add(btnThemVaoGio);
        pnlBtns1.add(btnGuiDatTruoc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(8, 2, 6, 2);
        pnlInput.add(pnlBtns1, gbc);

        pnlLeft.add(pnlInput, BorderLayout.NORTH);

        modelGioDat = new DefaultTableModel(new String[] { "ID Sách", "Số lượng" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable tblGio = new JTable(modelGioDat);
        ThemeConfig.styleTable(tblGio);
        tblGio.setRowHeight(ThemeConfig.TABLE_ROW_HEIGHT);
        tblGio.getColumnModel().getColumn(0).setCellRenderer(new CenterCellRenderer());
        tblGio.getColumnModel().getColumn(1).setCellRenderer(new RightCellRenderer());

        JScrollPane scrollGio = new JScrollPane(tblGio);
        scrollGio.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER, 1));
        scrollGio.setPreferredSize(new Dimension(0, 165));
        scrollGio.setMinimumSize(new Dimension(0, 150));
        ThemeConfig.installSaaSDataGrid(
                scrollGio, tblGio, Feather.PACKAGE,
                "Giỏ đặt trước đang trống",
                "Nhập ID Sách và số lượng rồi nhấn 'Thêm vào giỏ'.",
                120, 120);
        pnlLeft.add(scrollGio, BorderLayout.CENTER);

        // ── CỘT PHẢI (giãn hết phần còn lại): 2. HÀNG ĐỢI & GIỮ SÁCH ──
        JPanel pnlMid = new ThemeConfig.ModernCardPanel(
                new BorderLayout(0, 14), new Insets(20, 24, 20, 24));
        pnlMid.setMinimumSize(new Dimension(420, 430));

        JPanel midHeader = new JPanel(new BorderLayout(12, 0));
        midHeader.setOpaque(false);
        midHeader.add(
                ThemeConfig.createTitleBlock(
                        "2. HÀNG ĐỢI & GIỮ SÁCH",
                        "Theo dõi thứ tự FIFO, hạn giữ sách và chuyển tiếp sang Mượn sách",
                        Feather.LIST),
                BorderLayout.WEST);

        lblKetQuaHangDoi = new JLabel("0 lượt");
        lblKetQuaHangDoi.setFont(ThemeConfig.FONT_SM_BOLD);
        lblKetQuaHangDoi.setForeground(ThemeConfig.PRIMARY);
        lblKetQuaHangDoi.setBackground(ThemeConfig.PRIMARY_LIGHT);
        lblKetQuaHangDoi.setOpaque(true);
        lblKetQuaHangDoi.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xBFDBFE), 1, true),
                new EmptyBorder(4, 10, 4, 10)));
        JPanel countBadgeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 2));
        countBadgeWrap.setOpaque(false);
        countBadgeWrap.add(lblKetQuaHangDoi);
        midHeader.add(countBadgeWrap, BorderLayout.EAST);
        pnlMid.add(midHeader, BorderLayout.NORTH);

        txtTimKiem = new JTextField(16);
        JPanel searchBarMid = ThemeConfig.createSearchBar(
                txtTimKiem, "Tìm mã ĐT, độc giả, tên sách...", 250);

        cbLocTrangThai = new JComboBox<>(new String[] {
                "Tất cả trạng thái",
                "Đang chờ & Đang giữ sách",
                "Đang chờ",
                "Đang giữ sách",
                "Đã nhận sách",
                "Đã hủy / Hết hạn"
        });
        cbLocTrangThai.setFont(ThemeConfig.FONT_INPUT);
        cbLocTrangThai.setPreferredSize(new Dimension(195, ThemeConfig.INPUT_HEIGHT));

        JButton btnTaiDuLieu = ThemeConfig.styleActionButton(
                new JButton("Lọc theo ĐG"),
                Feather.USER,
                ThemeConfig.PRIMARY,
                Color.WHITE,
                ThemeConfig.PRIMARY_HOVER);

        JButton btnXemTatCa = ThemeConfig.styleActionButton(
                new JButton("Tất cả"),
                Feather.LIST,
                Color.WHITE,
                ThemeConfig.TEXT_PRIMARY,
                ThemeConfig.BORDER);

        JPanel pnlMidFilter = new JPanel(new ThemeConfig.WrapFlowLayout(FlowLayout.LEFT, 12, 8));
        pnlMidFilter.setOpaque(false);
        pnlMidFilter.add(searchBarMid);
        pnlMidFilter.add(cbLocTrangThai);
        pnlMidFilter.add(btnTaiDuLieu);
        pnlMidFilter.add(btnXemTatCa);

        modelDanhSachDat = new DefaultTableModel(
                new String[] { "ID", "Mã ĐT", "Độc giả", "Sách", "SL", "Trạng thái", "Hạn giữ" },
                0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return (columnIndex == 0 || columnIndex == 4) ? Integer.class : String.class;
            }
        };
        tblDanhSachDat = new JTable(modelDanhSachDat);
        ThemeConfig.styleTable(tblDanhSachDat);
        tblDanhSachDat.setRowHeight(ThemeConfig.TABLE_ROW_HEIGHT);
        tblDanhSachDat.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sorterHangDoi = new TableRowSorter<>(modelDanhSachDat);
        tblDanhSachDat.setRowSorter(sorterHangDoi);

        TableColumnModel cmMid = tblDanhSachDat.getColumnModel();
        cmMid.getColumn(0).setCellRenderer(new CenterCellRenderer());
        cmMid.getColumn(1).setCellRenderer(new CenterCellRenderer());
        cmMid.getColumn(2).setCellRenderer(new LeftCellRenderer());
        cmMid.getColumn(3).setCellRenderer(new LeftCellRenderer());
        cmMid.getColumn(4).setCellRenderer(new CenterCellRenderer());
        cmMid.getColumn(5).setCellRenderer(new DatTruocStatusRenderer());
        cmMid.getColumn(6).setCellRenderer(new CenterCellRenderer());

        JScrollPane scrollMid = new JScrollPane(tblDanhSachDat);
        scrollMid.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER, 1));
        scrollMid.setPreferredSize(new Dimension(0, 270));
        scrollMid.setMinimumSize(new Dimension(0, 240));
        ThemeConfig.installSaaSDataGrid(
                scrollMid, tblDanhSachDat, Feather.INBOX,
                "Hàng đợi đặt trước đang trống",
                "Hiện chưa có yêu cầu giữ chỗ sách nào trong hàng đợi.",
                52, 145, 175, 220, 56, 140, 140);

        JPanel pnlMidBody = new JPanel(new BorderLayout(0, 8));
        pnlMidBody.setOpaque(false);
        pnlMidBody.add(pnlMidFilter, BorderLayout.NORTH);
        pnlMidBody.add(scrollMid, BorderLayout.CENTER);
        pnlMid.add(pnlMidBody, BorderLayout.CENTER);

        JButton btnQuetQuaHan = ThemeConfig.styleActionButton(
                new JButton("Quét quá hạn chuyển lượt"),
                Feather.CLOCK,
                Color.WHITE,
                ThemeConfig.TEXT_PRIMARY,
                ThemeConfig.BORDER);

        btnChoMuonNgay = ThemeConfig.styleActionButton(
                new JButton("Cho mượn sách giữ"),
                Feather.BOOK_OPEN,
                ThemeConfig.PRIMARY,
                Color.WHITE,
                ThemeConfig.PRIMARY_HOVER);
        btnChoMuonNgay.setEnabled(false);

        btnHuyDatTruoc = ThemeConfig.styleActionButton(
                new JButton("Hủy / Chuyển lượt"),
                Feather.X_CIRCLE,
                new Color(0xFEF2F2),
                ThemeConfig.DANGER,
                new Color(0xFECACA));
        btnHuyDatTruoc.setEnabled(false);

        JPanel pnlMidFooter = new JPanel(new ThemeConfig.WrapFlowLayout(FlowLayout.RIGHT, 10, 8));
        pnlMidFooter.setOpaque(false);
        pnlMidFooter.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeConfig.BORDER),
                new EmptyBorder(8, 0, 0, 0)));
        pnlMidFooter.add(btnQuetQuaHan);
        pnlMidFooter.add(btnChoMuonNgay);
        pnlMidFooter.add(btnHuyDatTruoc);
        pnlMid.add(pnlMidFooter, BorderLayout.SOUTH);

        pnlTop.add(pnlLeft, BorderLayout.WEST);
        pnlTop.add(pnlMid, BorderLayout.CENTER);

        // ── PHẦN DƯỚI (rộng 100%, cao >= 320px): 3. HỘP THƯ THÔNG BÁO ─
        JPanel pnlRight = createSurfaceCard(
                "3. HỘP THƯ THÔNG BÁO",
                "Lịch sử thông báo và trạng thái chuyển thư",
                Feather.MAIL);
        pnlRight.setMinimumSize(new Dimension(0, 320));

        modelThongBao = new DefaultTableModel(
                new String[] { "ID", "Loại", "Kênh", "Độc giả", "Nội dung thông báo", "Trạng thái", "Thời gian" },
                0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Integer.class : String.class;
            }
        };
        JTable tblThongBao = new JTable(modelThongBao);
        ThemeConfig.styleTable(tblThongBao);
        tblThongBao.setRowHeight(ThemeConfig.TABLE_ROW_HEIGHT);

        TableColumnModel cmRight = tblThongBao.getColumnModel();
        cmRight.getColumn(0).setCellRenderer(new CenterCellRenderer());
        cmRight.getColumn(1).setCellRenderer(new DatTruocStatusRenderer());
        cmRight.getColumn(2).setCellRenderer(new DatTruocStatusRenderer());
        cmRight.getColumn(3).setCellRenderer(new LeftCellRenderer());
        cmRight.getColumn(4).setCellRenderer(new LeftCellRenderer());
        cmRight.getColumn(5).setCellRenderer(new DatTruocStatusRenderer());
        cmRight.getColumn(6).setCellRenderer(new CenterCellRenderer());

        JScrollPane scrollRight = new JScrollPane(tblThongBao);
        scrollRight.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER, 1));
        scrollRight.setPreferredSize(new Dimension(0, 240));
        scrollRight.setMinimumSize(new Dimension(0, 220));
        ThemeConfig.installSaaSDataGrid(
                scrollRight, tblThongBao, Feather.MAIL,
                "Chưa có thông báo tự động",
                "Nhật ký thông báo giữ sách và nhắc nhận sẽ hiển thị tại đây.",
                58, 145, 95, 185, 560, 145, 145);
        cmRight.getColumn(0).setMaxWidth(80);
        cmRight.getColumn(1).setMaxWidth(175);
        cmRight.getColumn(2).setMaxWidth(115);
        cmRight.getColumn(3).setMaxWidth(240);
        cmRight.getColumn(5).setMaxWidth(190);
        cmRight.getColumn(6).setMaxWidth(175);
        pnlRight.add(scrollRight, BorderLayout.CENTER);

        pageContent.add(pnlTop, BorderLayout.NORTH);
        pageContent.add(pnlRight, BorderLayout.CENTER);

        JScrollPane pageScroll = ThemeConfig.createPageScrollPane(pageContent);
        add(pageScroll, BorderLayout.CENTER);
        SwingUtilities.invokeLater(() -> pageScroll.getVerticalScrollBar().setValue(0));

        // ============================================================
        // 3. XỬ LÝ SỰ KIỆN
        // ============================================================
        btnThemVaoGio.addActionListener(e -> {
            try {
                int idSach = Integer.parseInt(txtIdSach.getText().trim());
                int sl = Integer.parseInt(txtSoLuong.getText().trim());
                if (sl <= 0) {
                    ThemeConfig.toastWarning(this, "Số lượng đặt trước phải lớn hơn 0.");
                    return;
                }
                gioDatTruoc.put(idSach, gioDatTruoc.getOrDefault(idSach, 0) + sl);
                capNhatBangGio();
                ThemeConfig.toastInfo(this, "Đã thêm sách #" + idSach + " (SL: " + sl + ") vào giỏ.");
            } catch (NumberFormatException ex) {
                ThemeConfig.toastWarning(this, "Vui lòng nhập ID Sách và Số lượng hợp lệ.");
            }
        });

        tblDanhSachDat.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                capNhatNutBangDatTruoc();
            }
        });

        txtTimKiem.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { if (!dangCapNhatBoLoc) apDungBoLocHangDoi(); }
            @Override public void removeUpdate(DocumentEvent e) { if (!dangCapNhatBoLoc) apDungBoLocHangDoi(); }
            @Override public void changedUpdate(DocumentEvent e) { if (!dangCapNhatBoLoc) apDungBoLocHangDoi(); }
        });

        cbLocTrangThai.addActionListener(e -> {
            if (!dangCapNhatBoLoc) {
                apDungBoLocHangDoi();
            }
        });

        btnGuiDatTruoc.addActionListener(e -> {
            try {
                String idStr = txtIdDocGia.getText().trim();
                if (idStr.isEmpty()) {
                    ThemeConfig.toastWarning(this, "Vui lòng nhập ID Độc giả để xác nhận đặt trước.");
                    return;
                }
                int idDocGia = Integer.parseInt(idStr);
                String ketQua = datTruocService.dangKyDatTruoc(idDocGia, gioDatTruoc);
                if (ketQua.contains("thành công")) {
                    ThemeConfig.toastSuccess(this, ketQua);
                } else {
                    ThemeConfig.toastWarning(this, ketQua);
                }
                gioDatTruoc.clear();
                capNhatBangGio();
                taiLaiDuLieu(idDocGia);
                MainFrame.refreshMainDashboard(this);
            } catch (NumberFormatException ex) {
                ThemeConfig.toastWarning(this, "ID Độc giả phải là số nguyên.");
            }
        });

        btnTaiDuLieu.addActionListener(e -> {
            String raw = txtIdDocGia.getText().trim();
            if (raw.isEmpty()) {
                taiLaiDuLieu(null);
                return;
            }
            try {
                taiLaiDuLieu(Integer.parseInt(raw));
            } catch (NumberFormatException ex) {
                ThemeConfig.toastWarning(this, "ID Độc giả phải là số hoặc để trống để xem tất cả.");
            }
        });

        btnXemTatCa.addActionListener(e -> {
            dangCapNhatBoLoc = true;
            try {
                txtIdDocGia.setText("");
                txtTimKiem.setText("");
                cbLocTrangThai.setSelectedIndex(0);
            } finally {
                dangCapNhatBoLoc = false;
            }
            taiLaiDuLieu(null);
        });

        btnChoMuonNgay.addActionListener(e -> {
            int row = tblDanhSachDat.getSelectedRow();
            if (row < 0) {
                return;
            }
            int modelRow = tblDanhSachDat.convertRowIndexToModel(row);
            if (modelRow < 0 || modelRow >= danhSachHienTai.size()) {
                return;
            }
            DatTruocChiTiet dt = danhSachHienTai.get(modelRow);
            MainFrame mf = MainFrame.of(this);
            if (mf != null) {
                mf.moMuonTraChoDocGia(dt.getIdDocGia(), dt.getIdSach());
            }
        });

        btnHuyDatTruoc.addActionListener(e -> {
            int row = tblDanhSachDat.getSelectedRow();
            if (row == -1) {
                ThemeConfig.toastWarning(this, "Hãy chọn 1 dòng đặt trước ở bảng giữa để hủy.");
                return;
            }
            int modelRow = tblDanhSachDat.convertRowIndexToModel(row);
            int idDatTruoc = ((Number) modelDanhSachDat.getValueAt(modelRow, 0)).intValue();
            if (datTruocService.huyVaChuyenLuotTiepTheo(idDatTruoc, false)) {
                ThemeConfig.toastSuccess(this, "Đã hủy lượt đặt và kiểm tra hàng đợi.");
                taiDuLieu();
                MainFrame.refreshMainDashboard(this);
            } else {
                ThemeConfig.toastWarning(this, "Không hủy được lượt đặt. Hãy làm mới danh sách và kiểm tra lại.");
            }
        });

        btnQuetQuaHan.addActionListener(e -> {
            datTruocService.quetVaChuyenLuotQuaHan();
            ThemeConfig.toastSuccess(this, "Đã quét các phiếu quá hạn nhận và chuyển lượt kế tiếp.");
            taiDuLieu();
            MainFrame.refreshMainDashboard(this);
        });

        taiDuLieu();
    }

    public void focusTimKiem() {
        txtTimKiem.requestFocusInWindow();
        txtTimKiem.selectAll();
    }

    public void moVaLocDangCho() {
        dangCapNhatBoLoc = true;
        try {
            txtIdDocGia.setText("");
            txtTimKiem.setText("");
            cbLocTrangThai.setSelectedItem("Đang chờ & Đang giữ sách");
        } finally {
            dangCapNhatBoLoc = false;
        }
        taiDuLieu();
    }

    public void moVaLocTheoSach(String tuKhoaSach) {
        dangCapNhatBoLoc = true;
        try {
            txtIdDocGia.setText("");
            cbLocTrangThai.setSelectedItem("Đang chờ & Đang giữ sách");
            txtTimKiem.setText(tuKhoaSach == null ? "" : tuKhoaSach);
        } finally {
            dangCapNhatBoLoc = false;
        }
        taiDuLieu();
    }

    private JPanel createSurfaceCard(String title, String subtitle, Ikon icon) {
        JPanel card = new ThemeConfig.ModernCardPanel(new BorderLayout(0, 14), new Insets(20, 24, 20, 24));

        JPanel header = ThemeConfig.createTitleBlock(title, subtitle, icon);
        card.add(header, BorderLayout.NORTH);
        return card;
    }

    private JLabel createInputLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(ThemeConfig.FONT_LABEL);
        lbl.setForeground(ThemeConfig.TEXT_PRIMARY);
        return lbl;
    }

    private void capNhatBangGio() {
        modelGioDat.setRowCount(0);
        gioDatTruoc.forEach((idSach, sl) -> modelGioDat.addRow(new Object[] { idSach, sl }));
        btnGuiDatTruoc.setEnabled(!gioDatTruoc.isEmpty());
    }

    public void taiDuLieu() {
        String raw = txtIdDocGia.getText().trim();
        if (raw.isEmpty()) {
            taiLaiDuLieu(null);
            return;
        }
        try {
            taiLaiDuLieu(Integer.parseInt(raw));
        } catch (NumberFormatException ignored) {
            taiLaiDuLieu(null);
        }
    }

    private void taiLaiDuLieu(Integer idDocGia) {
        try {
            modelDanhSachDat.setRowCount(0);
            danhSachHienTai = idDocGia == null
                    ? datTruocDAO.layTatCaDatTruocChiTiet()
                    : datTruocDAO.layDatTruocChiTietTheoDocGia(idDocGia);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            for (DatTruocChiTiet dt : danhSachHienTai) {
                String docGiaHienThi = dt.getTenDocGia() != null
                        ? dt.getTenDocGia() + (dt.getMaDocGia() != null ? " (" + dt.getMaDocGia() + ")" : "")
                        : "Độc giả #" + dt.getIdDocGia();
                String sachHienThi = dt.getTenSach() != null
                        ? dt.getTenSach() + (dt.getMaSach() != null ? " [" + dt.getMaSach() + "]" : "")
                        : "Sách #" + dt.getIdSach();
                String hanNhan = dt.getHanNhanSach() == null ? "—" : sdf.format(dt.getHanNhanSach());

                modelDanhSachDat.addRow(new Object[] {
                        dt.getId(),
                        dt.getMaDatTruoc(),
                        docGiaHienThi,
                        sachHienThi,
                        1,
                        dt.getTrangThai(),
                        hanNhan
                });
            }

            apDungBoLocHangDoi();

            modelThongBao.setRowCount(0);
            List<ThongBaoChiTiet> dsTB = idDocGia == null
                    ? thongBaoDAO.layThongBaoMoiNhat(100)
                    : thongBaoDAO.getThongBaoTheoDocGia(idDocGia);
            for (ThongBaoChiTiet tb : dsTB) {
                String docGiaTB = tb.getTenDocGia() != null && !tb.getTenDocGia().isBlank()
                        ? tb.getTenDocGia() + (tb.getMaDocGia() != null ? " (" + tb.getMaDocGia() + ")" : "")
                        : (tb.getIdDocGia() > 0 ? "Độc giả #" + tb.getIdDocGia() : "—");
                Timestamp tsTB = tb.getNgayGui() != null ? tb.getNgayGui() : tb.getNgayTao();
                String thoiGianTB = tsTB == null ? "—" : sdf.format(tsTB);
                modelThongBao.addRow(new Object[] {
                        tb.getId(),
                        tb.getLoaiThongBao(),
                        tb.getKenhGui(),
                        docGiaTB,
                        tb.getNoiDung(),
                        "SYSTEM".equals(tb.getKenhGui()) && "DA_GUI".equals(tb.getTrangThai())
                                ? "Đã ghi nhận" : tb.getTrangThai(),
                        thoiGianTB
                });
            }
        } catch (Exception ex) {
            System.err.println("Lỗi tải dữ liệu đặt trước: " + ex.getMessage());
            lblKetQuaHangDoi.setText("Không tải được dữ liệu");
            capNhatNutBangDatTruoc();
        }
    }

    private void apDungBoLocHangDoi() {
        String tuKhoa = ThemeConfig.normalizeText(txtTimKiem.getText());
        String locTrangThai = Objects.toString(cbLocTrangThai.getSelectedItem(), "Tất cả trạng thái");

        sorterHangDoi.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                String gop = ThemeConfig.normalizeText(
                        entry.getStringValue(0) + " "
                                + entry.getStringValue(1) + " "
                                + entry.getStringValue(2) + " "
                                + entry.getStringValue(3));
                boolean khopTuKhoa = tuKhoa.isEmpty() || gop.contains(tuKhoa);

                String rawStatus = entry.getStringValue(5);
                boolean khopTrangThai = switch (locTrangThai) {
                    case "Đang chờ & Đang giữ sách" ->
                        "DANG_CHO".equals(rawStatus) || "DA_THONG_BAO".equals(rawStatus);
                    case "Đang chờ" -> "DANG_CHO".equals(rawStatus);
                    case "Đang giữ sách" -> "DA_THONG_BAO".equals(rawStatus);
                    case "Đã nhận sách" -> "DA_NHAN".equals(rawStatus);
                    case "Đã hủy / Hết hạn" -> "DA_HUY".equals(rawStatus) || "HET_HAN".equals(rawStatus);
                    default -> true;
                };

                return khopTuKhoa && khopTrangThai;
            }
        });

        lblKetQuaHangDoi.setText("Hiển thị " + tblDanhSachDat.getRowCount() + " / " + modelDanhSachDat.getRowCount() + " lượt");
        capNhatNutBangDatTruoc();
    }

    private void capNhatNutBangDatTruoc() {
        int viewRow = tblDanhSachDat.getSelectedRow();
        if (viewRow < 0) {
            btnHuyDatTruoc.setEnabled(false);
            btnChoMuonNgay.setEnabled(false);
            return;
        }
        int modelRow = tblDanhSachDat.convertRowIndexToModel(viewRow);
        String rawStatus = Objects.toString(modelDanhSachDat.getValueAt(modelRow, 5), "");
        boolean conHieuLuc = "DANG_CHO".equals(rawStatus) || "DA_THONG_BAO".equals(rawStatus);
        btnHuyDatTruoc.setEnabled(conHieuLuc);
        btnChoMuonNgay.setEnabled(conHieuLuc);
    }

    // ================================================================
    // RENDERERS CHO CÁC BẢNG TRONG DAT TRUOC
    // ================================================================

    private static class CenterCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(ThemeConfig.FONT_TABLE);
            setHorizontalAlignment(SwingConstants.CENTER);
            if (!isSelected) {
                setBackground(row % 2 == 0 ? ThemeConfig.snap(ThemeConfig.SURFACE_CARD) : ThemeConfig.snap(ThemeConfig.BG_PAGE));
                setForeground(ThemeConfig.snap(ThemeConfig.TEXT_PRIMARY));
            }
            return this;
        }
    }

    private static class LeftCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(ThemeConfig.FONT_TABLE);
            setBorder(new EmptyBorder(0, 10, 0, 10));
            setHorizontalAlignment(SwingConstants.LEFT);
            setToolTipText(value != null && !value.toString().isBlank() ? value.toString() : null);
            if (!isSelected) {
                setBackground(row % 2 == 0 ? ThemeConfig.snap(ThemeConfig.SURFACE_CARD) : ThemeConfig.snap(ThemeConfig.BG_PAGE));
                setForeground(ThemeConfig.snap(ThemeConfig.TEXT_PRIMARY));
            }
            return this;
        }
    }

    private static class RightCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(ThemeConfig.FONT_TABLE);
            setBorder(new EmptyBorder(0, 10, 0, 10));
            setHorizontalAlignment(SwingConstants.RIGHT);
            if (!isSelected) {
                setBackground(row % 2 == 0 ? ThemeConfig.snap(ThemeConfig.SURFACE_CARD) : ThemeConfig.snap(ThemeConfig.BG_PAGE));
                setForeground(ThemeConfig.snap(ThemeConfig.TEXT_PRIMARY));
            }
            return this;
        }
    }

    private static class DatTruocStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            String raw = value == null ? "" : value.toString().trim();
            String display = switch (raw) {
                case "CHO_XAC_NHAN" -> "Chờ xác nhận";
                case "DANG_CHO" -> "Đang chờ";
                case "DA_THONG_BAO" -> "Đang giữ sách";
                case "DA_CO_SACH" -> "Đã có sách";
                case "DA_NHAN", "HOAN_THANH" -> "Đã nhận sách";
                case "DA_HUY" -> "Đã hủy";
                case "HET_HAN", "QUA_HAN" -> "Hết hạn";
                case "DEN_LUOT_DAT_TRUOC" -> "Đến lượt nhận";
                case "SAP_DEN_HAN" -> "Sắp đến hạn";
                case "SYSTEM" -> "Hệ thống";
                case "EMAIL" -> "Email";
                case "CHUA_GUI" -> "Chờ gửi";
                case "DANG_GUI" -> "Đang gửi";
                case "DA_GUI" -> "Đã chuyển SMTP";
                case "LOI" -> "Lỗi gửi";
                case "CAN_KIEM_TRA" -> "Cần kiểm tra";
                case "HUY" -> "Đã hủy gửi";
                default -> raw;
            };

            JPanel wrapper = new JPanel(new GridBagLayout());
            if (isSelected) {
                wrapper.setBackground(ThemeConfig.snap(ThemeConfig.PRIMARY_LIGHT));
            } else {
                wrapper.setBackground(row % 2 == 0 ? ThemeConfig.snap(ThemeConfig.SURFACE_CARD) : ThemeConfig.snap(ThemeConfig.BG_PAGE));
            }

            JLabel badge = new JLabel(display) {
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

            switch (raw) {
                case "DA_NHAN", "HOAN_THANH", "Đã hoàn thành", "Đang hoạt động" -> {
                    badge.setBackground(ThemeConfig.STATUS_OK_BG);
                    badge.setForeground(ThemeConfig.STATUS_OK_FG);
                }
                case "DANG_CHO", "Đang chờ", "EMAIL", "SMS", "SYSTEM", "DEN_LUOT_DAT_TRUOC" -> {
                    badge.setBackground(ThemeConfig.PRIMARY_LIGHT);
                    badge.setForeground(ThemeConfig.STATUS_INFO_FG);
                }
                case "DA_THONG_BAO", "Đang giữ sách", "CHO_XAC_NHAN", "Chờ xác nhận", "DA_CO_SACH", "Đã có sách",
                        "SAP_DEN_HAN", "Sắp đến hạn" -> {
                    badge.setBackground(ThemeConfig.STATUS_WARN_BG);
                    badge.setForeground(ThemeConfig.STATUS_WARN_FG);
                }
                case "DA_HUY", "Đã hủy", "HET_HAN", "QUA_HAN", "Hết hạn", "Quá hạn" -> {
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

    public static void main(String[] args) {
        ThemeConfig.initTheme();
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Đặt trước sách");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1280, 780);
            frame.setLocationRelativeTo(null);
            frame.add(new DatTruocPanel());
            frame.setVisible(true);
        });
    }
}