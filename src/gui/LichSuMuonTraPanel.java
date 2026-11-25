package gui;

import dao.PhieuMuonDAO;
import model.ChiTietPhieuMuon;
import model.PhieuMuon;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * Màn hình Lịch sử mượn / trả sách — Thiết kế chuẩn Modern SaaS Dashboard theo
 * UI_STYLE_GUIDE.md.
 */
public class LichSuMuonTraPanel extends JPanel {

    private final JTextField txtTimKiem = new JTextField(22);

    private final JComboBox<String> cbTrangThai = new JComboBox<>(new String[] {
            "Tất cả",
            "Đang mượn",
            "Chưa trả quá hạn",
            "Trả sớm",
            "Đúng hạn",
            "Đã trả muộn",
            "Chốt mất trước hạn",
            "Chốt mất đúng hạn",
            "Chốt mất muộn"
    });

    private final JComboBox<String> cbTinhTrang = new JComboBox<>(new String[] {
            "Tất cả tình trạng",
            "Bình thường",
            "Hỏng - loại khỏi kho",
            "Mất - đã chốt",
            "Chưa ghi nhận"
    });

    private final JButton btnTimKiem = new JButton("Tìm kiếm");
    private final JButton btnLamMoi = new JButton("Làm mới");
    private final JButton btnChiTiet = new JButton("Xem chi tiết");
    private final JButton btnXuatCsv = new JButton("Xuất Excel (CSV)");

    private final JLabel lblKetQua = new JLabel(" ");

    private boolean dangTai;

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] {
                    "Mã phiếu",
                    "Độc giả",
                    "Tên sách",
                    "Số lượng",
                    "Ngày mượn",
                    "Hạn trả",
                    "Ngày trả/chốt mất",
                    "Trạng thái",
                    "Số ngày quá hạn",
                    "Tình trạng",
                    "Ghi chú"
            },
            0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 3, 8 -> Integer.class;
                default -> String.class;
            };
        }
    };

    private final JTable table = new JTable(tableModel);

    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);

    public LichSuMuonTraPanel() {
        setLayout(new BorderLayout());
        setBackground(ThemeConfig.BG_PAGE);

        ThemeConfig.ScrollablePanel pageContent = new ThemeConfig.ScrollablePanel();
        pageContent.setLayout(new BorderLayout(0, 22));
        pageContent.setOpaque(false);
        pageContent.setBorder(new EmptyBorder(24, 28, 24, 28));

        // ============================================================
        // 1. CARD TRÊN: BỘ LỌC & TÌM KIẾM LỊCH SỬ
        // ============================================================
        JPanel cardFilter = new ThemeConfig.ModernCardPanel(new BorderLayout(0, 16));

        JPanel titleBlock = ThemeConfig.createTitleBlock(
                "LỊCH SỬ MƯỢN / TRẢ SÁCH",
                "Tra cứu toàn diện nhật ký lưu hành, tiến độ trả sách và các trường hợp chốt mất sách",
                Feather.ROTATE_CCW);
        cardFilter.add(titleBlock, BorderLayout.NORTH);

        JPanel pnlCongCu = new JPanel(new ThemeConfig.WrapFlowLayout(FlowLayout.LEFT, 12, 8));
        pnlCongCu.setOpaque(false);

        JLabel lblKey = new JLabel("Từ khóa:");
        lblKey.setFont(ThemeConfig.FONT_LABEL);
        lblKey.setForeground(ThemeConfig.TEXT_PRIMARY);
        pnlCongCu.add(lblKey);

        pnlCongCu.add(ThemeConfig.createSearchBar(
                txtTimKiem, "Tìm theo mã phiếu, tên sách, độc giả...", 270));

        JLabel lblStatus = new JLabel("Trạng thái:");
        lblStatus.setFont(ThemeConfig.FONT_LABEL);
        lblStatus.setForeground(ThemeConfig.TEXT_PRIMARY);
        pnlCongCu.add(lblStatus);

        cbTrangThai.setFont(ThemeConfig.FONT_BODY);
        cbTrangThai.setPreferredSize(new Dimension(170, ThemeConfig.INPUT_HEIGHT));
        pnlCongCu.add(cbTrangThai);

        JLabel lblCond = new JLabel("Tình trạng:");
        lblCond.setFont(ThemeConfig.FONT_LABEL);
        lblCond.setForeground(ThemeConfig.TEXT_PRIMARY);
        pnlCongCu.add(lblCond);

        cbTinhTrang.setFont(ThemeConfig.FONT_BODY);
        cbTinhTrang.setPreferredSize(new Dimension(175, ThemeConfig.INPUT_HEIGHT));
        pnlCongCu.add(cbTinhTrang);

        ThemeConfig.styleActionButton(btnTimKiem, Feather.SEARCH, ThemeConfig.PRIMARY, Color.WHITE,
                ThemeConfig.PRIMARY_HOVER);
        pnlCongCu.add(btnTimKiem);

        ThemeConfig.styleActionButton(btnLamMoi, Feather.REFRESH_CW, Color.WHITE, ThemeConfig.TEXT_PRIMARY,
                ThemeConfig.BORDER);
        pnlCongCu.add(btnLamMoi);

        ThemeConfig.styleActionButton(btnChiTiet, Feather.EYE, Color.WHITE, ThemeConfig.TEXT_PRIMARY,
                ThemeConfig.BORDER);
        btnChiTiet.setEnabled(false);
        pnlCongCu.add(btnChiTiet);

        ThemeConfig.styleActionButton(btnXuatCsv, Feather.DOWNLOAD, Color.WHITE, ThemeConfig.SUCCESS_HOVER,
                ThemeConfig.BORDER);
        pnlCongCu.add(btnXuatCsv);

        cardFilter.add(pnlCongCu, BorderLayout.CENTER);
        pageContent.add(cardFilter, BorderLayout.NORTH);

        // ============================================================
        // 2. CARD DƯỚI: BẢNG DỮ LIỆU LỊCH SỬ MẬT ĐỘ CAO
        // ============================================================
        JPanel cardTable = new ThemeConfig.ModernCardPanel(new BorderLayout(0, 16));

        JPanel headerTable = new JPanel(new BorderLayout());
        headerTable.setOpaque(false);

        JLabel lblTableTitle = new JLabel("DANH SÁCH CHI TIẾT LỊCH SỬ LƯU HÀNH");
        lblTableTitle.setIcon(ThemeConfig.getIcon(Feather.FILE_TEXT, 18, ThemeConfig.PRIMARY));
        lblTableTitle.setIconTextGap(8);
        lblTableTitle.setFont(ThemeConfig.FONT_H3);
        lblTableTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        lblTableTitle.setBorder(new EmptyBorder(0, 0, 2, 0));

        lblKetQua.setFont(ThemeConfig.FONT_SM_BOLD);
        lblKetQua.setForeground(ThemeConfig.PRIMARY);
        lblKetQua.setBackground(ThemeConfig.PRIMARY_LIGHT);
        lblKetQua.setOpaque(true);
        lblKetQua.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xBFDBFE), 1, true),
                new EmptyBorder(4, 10, 4, 10)));

        headerTable.add(lblTableTitle, BorderLayout.WEST);
        headerTable.add(lblKetQua, BorderLayout.EAST);
        cardTable.add(headerTable, BorderLayout.NORTH);

        ThemeConfig.styleTable(table);
        table.setRowHeight(ThemeConfig.TABLE_ROW_HEIGHT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSorter(sorter);

        TableColumnModel colModel = table.getColumnModel();
        colModel.getColumn(0).setCellRenderer(new CenterCellRenderer());
        colModel.getColumn(1).setCellRenderer(new LeftCellRenderer());
        colModel.getColumn(2).setCellRenderer(new LeftCellRenderer());
        colModel.getColumn(3).setCellRenderer(new RightCellRenderer());
        colModel.getColumn(4).setCellRenderer(new CenterCellRenderer());
        colModel.getColumn(5).setCellRenderer(new CenterCellRenderer());
        colModel.getColumn(6).setCellRenderer(new CenterCellRenderer());
        colModel.getColumn(7).setCellRenderer(new LichSuStatusRenderer());
        colModel.getColumn(8).setCellRenderer(new OverdueCellRenderer());
        colModel.getColumn(9).setCellRenderer(new TinhTrangStatusRenderer());
        colModel.getColumn(10).setCellRenderer(new LeftCellRenderer());

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER, 1));
        scrollTable.getViewport().setBackground(ThemeConfig.SURFACE_CARD);
        scrollTable.setPreferredSize(new Dimension(0, 450));
        scrollTable.setMinimumSize(new Dimension(0, 400));
        ThemeConfig.installSaaSDataGrid(
                scrollTable, table, Feather.ROTATE_CCW,
                "Chưa có lịch sử lưu hành sách",
                "Nhật ký chi tiết các giao dịch mượn và trả sách sẽ hiển thị tại đây.",
                220, 175, 240, 82, 108, 108, 135, 165, 125, 165, 260);
        cardTable.add(scrollTable, BorderLayout.CENTER);

        pageContent.add(cardTable, BorderLayout.CENTER);
        JScrollPane pageScroll = ThemeConfig.createPageScrollPane(pageContent);
        add(pageScroll, BorderLayout.CENTER);

        // ============================================================
        // 3. GẮN SỰ KIỆN GỐC (GIỮ NGUYÊN 100%)
        // ============================================================
        btnTimKiem.addActionListener(e -> apDungBoLoc());
        txtTimKiem.addActionListener(e -> apDungBoLoc());
        cbTrangThai.addActionListener(e -> apDungBoLoc());
        cbTinhTrang.addActionListener(e -> apDungBoLoc());

        txtTimKiem.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { apDungBoLoc(); }
            @Override public void removeUpdate(DocumentEvent e) { apDungBoLoc(); }
            @Override public void changedUpdate(DocumentEvent e) { apDungBoLoc(); }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                capNhatNutChiTiet();
            }
        });

        btnLamMoi.addActionListener(e -> {
            txtTimKiem.setText("");
            cbTrangThai.setSelectedIndex(0);
            cbTinhTrang.setSelectedIndex(0);
            taiDuLieu();
        });
        btnChiTiet.addActionListener(e -> xemChiTiet());
        btnXuatCsv.addActionListener(e ->
                CsvExportUtil.exportTableToCsv(this, table, "Lich_Su_Muon_Tra", "LỊCH SỬ MƯỢN TRẢ"));

        taiDuLieu();
        SwingUtilities.invokeLater(() -> pageScroll.getVerticalScrollBar().setValue(0));
    }

    // ================= TẢI LỊCH SỬ =================

    public void taiDuLieu() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::taiDuLieu);
            return;
        }

        if (dangTai) {
            return;
        }

        datTrangThaiTai(true);
        lblKetQua.setText("Đang tải lịch sử...");

        new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() {
                List<Object[]> cacDong = new ArrayList<>();

                PhieuMuonDAO dao = new PhieuMuonDAO();
                LocalDate ngayXet = LocalDate.now();

                for (PhieuMuon pm : dao.layTatCaPhieuMuon()) {
                    for (ChiTietPhieuMuon ct : dao.layChiTietPhieuMuon(pm.getId())) {

                        String ghiChu = ct.getGhiChuTra() == null
                                ? ""
                                : ct.getGhiChuTra();

                        cacDong.add(new Object[] {
                                pm.getMaPhieu(),
                                pm.getTenDocGia(),
                                ct.getTenSach(),
                                ct.getSoLuong(),
                                dinhDangNgay(pm.getNgayMuon()),
                                dinhDangNgay(ct.getHanTra()),
                                dinhDangNgay(ct.getNgayTra()),
                                tenTrangThai(ct, ngayXet),
                                ct.tinhSoNgayQuaHan(ngayXet),
                                tenTinhTrang(ct.getTinhTrangTra()),
                                ghiChu
                        });
                    }
                }

                return cacDong;
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> cacDong = get();

                    tableModel.setRowCount(0);

                    for (Object[] dong : cacDong) {
                        tableModel.addRow(dong);
                    }

                    apDungBoLoc();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    lblKetQua.setText("Đã dừng tải lịch sử.");

                } catch (ExecutionException e) {
                    if (e.getCause() != null) {
                        e.getCause().printStackTrace();
                    }

                    lblKetQua.setText(
                            "Không tải được lịch sử.");

                    ThemeConfig.toastWarning(
                            LichSuMuonTraPanel.this,
                            "Không tải được lịch sử. Hãy kiểm tra kết nối và thử làm mới.");

                } finally {
                    datTrangThaiTai(false);
                }
            }
        }.execute();
    }

    // ================= TÊN TRẠNG THÁI =================

    private String tenTrangThai(
            ChiTietPhieuMuon ct,
            LocalDate ngayXet) {
        String maTrangThai = ct.xacDinhTrangThai(ngayXet);
        boolean matSach = "LAM_MAT".equals(ct.getTinhTrangTra());

        return switch (maTrangThai) {
            case "DANG_MUON" -> "Đang mượn";

            case "CHUA_TRA_QUA_HAN" -> "Chưa trả quá hạn";

            case "TRA_SOM" -> matSach
                    ? "Chốt mất trước hạn"
                    : "Trả sớm";

            case "DUNG_HAN" -> matSach
                    ? "Chốt mất đúng hạn"
                    : "Đúng hạn";

            case "QUA_HAN" -> matSach
                    ? "Chốt mất muộn"
                    : "Đã trả muộn";

            default -> maTrangThai;
        };
    }

    private String tenTinhTrang(String tinhTrang) {
        if (tinhTrang == null) {
            return "Chưa ghi nhận";
        }

        return switch (tinhTrang) {
            case "BINH_THUONG" -> "Bình thường";
            case "LAM_HONG" -> "Hỏng - loại khỏi kho";
            case "LAM_MAT" -> "Mất - đã chốt";
            default -> tinhTrang;
        };
    }

    private String dinhDangNgay(java.util.Date ngay) {
        if (ngay == null) {
            return "";
        }

        return new java.sql.Date(ngay.getTime())
                .toLocalDate()
                .toString();
    }

    // ================= TÌM KIẾM / LỌC =================

    private void apDungBoLoc() {
        String tuKhoa = ThemeConfig.normalizeText(txtTimKiem.getText());
        String trangThai = Objects.toString(cbTrangThai.getSelectedItem(), "Tất cả");
        String tinhTrang = Objects.toString(cbTinhTrang.getSelectedItem(), "Tất cả tình trạng");

        sorter.setRowFilter(
                new RowFilter<DefaultTableModel, Integer>() {
                    @Override
                    public boolean include(
                            Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                        String noiDung = ThemeConfig.normalizeText(entry.getStringValue(0) + " "
                                + entry.getStringValue(1) + " "
                                + entry.getStringValue(2) + " "
                                + entry.getStringValue(7) + " "
                                + entry.getStringValue(9) + " "
                                + entry.getStringValue(10));

                        boolean dungTuKhoa = tuKhoa.isEmpty() || noiDung.contains(tuKhoa);

                        boolean dungTrangThai = "Tất cả".equals(trangThai)
                                || entry.getStringValue(7).equals(trangThai);

                        boolean dungTinhTrang = "Tất cả tình trạng".equals(tinhTrang)
                                || entry.getStringValue(9).equals(tinhTrang);

                        return dungTuKhoa && dungTrangThai && dungTinhTrang;
                    }
                });

        lblKetQua.setText(
                "Hiển thị " + table.getRowCount()
                        + " / " + tableModel.getRowCount()
                        + " dòng mượn.");
        capNhatNutChiTiet();
    }

    public void focusTimKiem() {
        txtTimKiem.requestFocusInWindow();
        txtTimKiem.selectAll();
    }

    public void moVaLocTrangThai(String trangThai) {
        txtTimKiem.setText("");
        cbTinhTrang.setSelectedIndex(0);
        if (trangThai != null && !trangThai.isBlank()) {
            cbTrangThai.setSelectedItem(trangThai);
        } else {
            cbTrangThai.setSelectedIndex(0);
        }
        taiDuLieu();
    }

    private void capNhatNutChiTiet() {
        btnChiTiet.setEnabled(!dangTai && table.getSelectedRow() >= 0);
    }

    // ================= XEM ĐẦY ĐỦ MỘT DÒNG =================

    private void xemChiTiet() {
        int dongChon = table.getSelectedRow();

        if (dongChon < 0) {
            ThemeConfig.toastWarning(this, "Hãy chọn một dòng lịch sử.");
            return;
        }

        int dongModel = table.convertRowIndexToModel(dongChon);

        StringBuilder noiDung = new StringBuilder();

        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            Object giaTri = tableModel.getValueAt(dongModel, i);

            noiDung.append(tableModel.getColumnName(i))
                    .append(": ")
                    .append(giaTri == null ? "" : giaTri)
                    .append("\n\n");
        }

        JTextArea txtChiTiet = new JTextArea(noiDung.toString(), 16, 50);

        txtChiTiet.setEditable(false);
        txtChiTiet.setLineWrap(true);
        txtChiTiet.setWrapStyleWord(true);
        txtChiTiet.setCaretPosition(0);

        JOptionPane.showMessageDialog(
                this,
                new JScrollPane(txtChiTiet),
                "Chi tiết lịch sử mượn / trả",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // ================= TRẠNG THÁI TẢI =================

    private void datTrangThaiTai(boolean ban) {
        dangTai = ban;

        txtTimKiem.setEnabled(!ban);
        cbTrangThai.setEnabled(!ban);
        cbTinhTrang.setEnabled(!ban);

        btnTimKiem.setEnabled(!ban);
        btnLamMoi.setEnabled(!ban);
        btnXuatCsv.setEnabled(!ban);
        capNhatNutChiTiet();

        setCursor(
                ban
                        ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                        : Cursor.getDefaultCursor());
    }

    // ================================================================
    // RENDERERS CHO BẢNG LỊCH SỬ
    // ================================================================

    private static class CenterCellRenderer extends DefaultTableCellRenderer {
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

    private static class LeftCellRenderer extends DefaultTableCellRenderer {
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

    private static class RightCellRenderer extends DefaultTableCellRenderer {
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

    private static class LichSuStatusRenderer extends DefaultTableCellRenderer {
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
                case "Trả sớm", "Đúng hạn", "Đã trả", "Đang hoạt động" -> {
                    badge.setBackground(ThemeConfig.STATUS_OK_BG);
                    badge.setForeground(ThemeConfig.STATUS_OK_FG);
                }
                case "Đang mượn", "Đang chờ" -> {
                    badge.setBackground(ThemeConfig.PRIMARY_LIGHT);
                    badge.setForeground(ThemeConfig.STATUS_INFO_FG);
                }
                case "Sắp đến hạn", "Chờ xác nhận" -> {
                    badge.setBackground(ThemeConfig.STATUS_WARN_BG);
                    badge.setForeground(ThemeConfig.STATUS_WARN_FG);
                }
                case "Chưa trả quá hạn", "Đã trả muộn", "Chốt mất trước hạn", "Chốt mất đúng hạn", "Chốt mất muộn" -> {
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

    private static class TinhTrangStatusRenderer extends DefaultTableCellRenderer {
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
                case "Bình thường" -> {
                    badge.setBackground(ThemeConfig.STATUS_OK_BG);
                    badge.setForeground(ThemeConfig.STATUS_OK_FG);
                }
                case "Hỏng - loại khỏi kho" -> {
                    badge.setBackground(ThemeConfig.STATUS_WARN_BG);
                    badge.setForeground(ThemeConfig.STATUS_WARN_FG);
                }
                case "Mất - đã chốt" -> {
                    badge.setBackground(ThemeConfig.STATUS_ERROR_BG);
                    badge.setForeground(ThemeConfig.STATUS_ERROR_FG);
                }
                default -> {
                    badge.setBackground(ThemeConfig.SUBTLE_SURFACE);
                    badge.setForeground(ThemeConfig.TEXT_MUTED);
                }
            }

            wrapper.add(badge);
            return wrapper;
        }
    }
}