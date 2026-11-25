package gui;

import dao.ViPhamDAO;
import model.ViPham;
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
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * Màn hình Quản lý Vi phạm — Chuẩn Data-Dense Dashboard theo UI_STYLE_GUIDE.md
 * & ThemeConfig.
 */
public class QuanLyViPhamPanel extends JPanel {

    private final ViPhamDAO dao = new ViPhamDAO();

    private final JTextField txtTimKiem = new JTextField(22);

    private final JComboBox<String> cbTrangThai = new JComboBox<>(new String[] {
            "Tất cả",
            "Chưa xử lý",
            "Đã xử lý"
    });

    private final JComboBox<String> cbLoaiViPham = new JComboBox<>(new String[] {
            "Tất cả loại",
            "TRA_MUON",
            "LAM_HONG",
            "LAM_MAT"
    });

    private final JButton btnTimKiem = new JButton("Tìm kiếm");
    private final JButton btnLamMoi = new JButton("Làm mới");
    private final JButton btnXuatCsv = new JButton("Xuất Excel (CSV)");
    private final JButton btnInPhieuPhat = new JButton("Xem & In biên bản");
    private final JButton btnXuLy = new JButton("Xác nhận xử lý");
    private final JLabel lblTrangThaiThaoTac = ThemeConfig.createStatusLabel();

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] {
                    "ID",
                    "Mã vi phạm",
                    "Độc giả",
                    "Mã phiếu",
                    "Tên sách",
                    "Loại vi phạm",
                    "Số ngày quá hạn",
                    "Hình thức xử lý",
                    "Ngày ghi nhận",
                    "Trạng thái",
                    "Mô tả"
            }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable table = new JTable(tableModel);

    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);

    private final JLabel lblTong = new JLabel();

    public QuanLyViPhamPanel() {
        setLayout(new BorderLayout());
        setBackground(ThemeConfig.BG_PAGE);

        ThemeConfig.ScrollablePanel pageContent = new ThemeConfig.ScrollablePanel();
        pageContent.setLayout(new BorderLayout(0, 22));
        pageContent.setOpaque(false);
        pageContent.setBorder(new EmptyBorder(24, 28, 24, 28));

        // ============================================================
        // 1. CARD TRÊN: BỘ LỌC & THAO TÁC XỬ LÝ
        // ============================================================
        JPanel cardFilter = new ThemeConfig.ModernCardPanel(new BorderLayout(0, 16));

        JPanel titleBlock = ThemeConfig.createTitleBlock(
                "QUẢN LÝ VI PHẠM NỘI QUY",
                "Theo dõi, đối soát các trường hợp mượn quá hạn, sách hỏng hoặc làm mất sách",
                Feather.ALERT_TRIANGLE);
        cardFilter.add(titleBlock, BorderLayout.NORTH);

        JPanel filterGroup = new JPanel(new ThemeConfig.WrapFlowLayout(FlowLayout.LEFT, 12, 8));
        filterGroup.setOpaque(false);

        JLabel lblKey = new JLabel("Tìm nhanh:");
        lblKey.setFont(ThemeConfig.FONT_LABEL);
        lblKey.setForeground(ThemeConfig.TEXT_PRIMARY);

        JPanel searchBar = ThemeConfig.createSearchBar(
                txtTimKiem, "Gõ độc giả, mã VP, mã phiếu, sách...", 265);

        JLabel lblStatus = new JLabel("Trạng thái:");
        lblStatus.setFont(ThemeConfig.FONT_LABEL);
        lblStatus.setForeground(ThemeConfig.TEXT_PRIMARY);

        cbTrangThai.setFont(ThemeConfig.FONT_BODY);
        cbTrangThai.setPreferredSize(new Dimension(140, ThemeConfig.INPUT_HEIGHT));

        JLabel lblLoai = new JLabel("Loại:");
        lblLoai.setFont(ThemeConfig.FONT_LABEL);
        lblLoai.setForeground(ThemeConfig.TEXT_PRIMARY);

        cbLoaiViPham.setFont(ThemeConfig.FONT_BODY);
        cbLoaiViPham.setPreferredSize(new Dimension(140, ThemeConfig.INPUT_HEIGHT));
        cbLoaiViPham.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String raw = Objects.toString(value, "");
                setText(switch (raw) {
                    case "TRA_MUON" -> "Trả muộn";
                    case "LAM_HONG" -> "Làm hỏng";
                    case "LAM_MAT" -> "Làm mất";
                    default -> raw;
                });
                return this;
            }
        });

        ThemeConfig.styleActionButton(btnTimKiem, Feather.SEARCH, ThemeConfig.PRIMARY, Color.WHITE,
                ThemeConfig.PRIMARY_HOVER);
        ThemeConfig.styleActionButton(btnLamMoi, Feather.REFRESH_CW, Color.WHITE, ThemeConfig.TEXT_PRIMARY,
                ThemeConfig.BORDER);
        ThemeConfig.styleActionButton(btnXuatCsv, Feather.DOWNLOAD, Color.WHITE, ThemeConfig.SUCCESS_HOVER,
                ThemeConfig.BORDER);

        filterGroup.add(lblKey);
        filterGroup.add(searchBar);
        filterGroup.add(lblStatus);
        filterGroup.add(cbTrangThai);
        filterGroup.add(lblLoai);
        filterGroup.add(cbLoaiViPham);
        filterGroup.add(btnTimKiem);
        filterGroup.add(btnLamMoi);
        filterGroup.add(btnXuatCsv);

        ThemeConfig.styleActionButton(btnInPhieuPhat, Feather.PRINTER, Color.WHITE, ThemeConfig.PRIMARY,
                ThemeConfig.BORDER);
        ThemeConfig.styleActionButton(btnXuLy, Feather.CHECK_CIRCLE, ThemeConfig.SUCCESS, Color.WHITE,
                ThemeConfig.SUCCESS_HOVER);

        btnInPhieuPhat.setEnabled(false);
        btnXuLy.setEnabled(false);

        JPanel actionGroup = new JPanel(new ThemeConfig.WrapFlowLayout(FlowLayout.RIGHT, 12, 8));
        actionGroup.setOpaque(false);
        actionGroup.add(btnInPhieuPhat);
        actionGroup.add(btnXuLy);

        JPanel bottomBar = new JPanel(new BorderLayout(12, 0));
        bottomBar.setOpaque(false);
        bottomBar.add(lblTrangThaiThaoTac, BorderLayout.CENTER);
        bottomBar.add(actionGroup, BorderLayout.EAST);

        cardFilter.add(filterGroup, BorderLayout.CENTER);
        cardFilter.add(bottomBar, BorderLayout.SOUTH);

        pageContent.add(cardFilter, BorderLayout.NORTH);

        // ============================================================
        // 2. CARD DƯỚI: DANH SÁCH VI PHẠM (Data-Dense Table)
        // ============================================================
        JPanel cardTable = new ThemeConfig.ModernCardPanel(new BorderLayout(0, 16));

        JPanel headerTable = new JPanel(new BorderLayout());
        headerTable.setOpaque(false);

        JLabel lblTableTitle = new JLabel("DANH SÁCH VI PHẠM GHI NHẬN");
        lblTableTitle.setIcon(ThemeConfig.getIcon(Feather.LIST, 18, ThemeConfig.PRIMARY));
        lblTableTitle.setIconTextGap(8);
        lblTableTitle.setFont(ThemeConfig.FONT_H3);
        lblTableTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        lblTableTitle.setBorder(new EmptyBorder(0, 0, 2, 0));

        lblTong.setFont(ThemeConfig.FONT_SM_BOLD);
        lblTong.setForeground(ThemeConfig.PRIMARY);
        lblTong.setBackground(ThemeConfig.PRIMARY_LIGHT);
        lblTong.setOpaque(true);
        lblTong.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xBFDBFE), 1, true),
                new EmptyBorder(4, 10, 4, 10)));

        headerTable.add(lblTableTitle, BorderLayout.WEST);
        headerTable.add(lblTong, BorderLayout.EAST);
        cardTable.add(headerTable, BorderLayout.NORTH);

        ThemeConfig.styleTable(table);
        table.setRowHeight(ThemeConfig.TABLE_ROW_HEIGHT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSorter(sorter);

        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setCellRenderer(new CenterRenderer());
        cm.getColumn(1).setCellRenderer(new CenterRenderer());
        cm.getColumn(2).setCellRenderer(new LeftRenderer());
        cm.getColumn(3).setCellRenderer(new CenterRenderer());
        cm.getColumn(4).setCellRenderer(new LeftRenderer());
        cm.getColumn(5).setCellRenderer(new LoaiViPhamBadgeRenderer());
        cm.getColumn(6).setCellRenderer(new OverdueDaysRenderer());
        cm.getColumn(7).setCellRenderer(new LeftRenderer());
        cm.getColumn(8).setCellRenderer(new CenterRenderer());
        cm.getColumn(9).setCellRenderer(new TrangThaiViPhamBadgeRenderer());
        cm.getColumn(10).setCellRenderer(new LeftRenderer());

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER, 1));
        scrollTable.getViewport().setBackground(ThemeConfig.SURFACE_CARD);
        scrollTable.setPreferredSize(new Dimension(0, 440));
        scrollTable.setMinimumSize(new Dimension(0, 390));
        ThemeConfig.installSaaSDataGrid(
                scrollTable, table, Feather.CHECK_CIRCLE,
                "Không có hồ sơ vi phạm nội quy",
                "Hệ thống hiện không ghi nhận trường hợp quá hạn, làm hỏng hay mất sách nào.",
                56, 185, 170, 190, 200, 125, 120, 205, 145, 130, 240);
        cardTable.add(scrollTable, BorderLayout.CENTER);

        pageContent.add(cardTable, BorderLayout.CENTER);
        JScrollPane pageScroll = ThemeConfig.createPageScrollPane(pageContent);
        add(pageScroll, BorderLayout.CENTER);

        btnTimKiem.addActionListener(e -> locDuLieu());
        cbTrangThai.addActionListener(e -> locDuLieu());
        cbLoaiViPham.addActionListener(e -> locDuLieu());
        txtTimKiem.addActionListener(e -> locDuLieu());

        txtTimKiem.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { locDuLieu(); }
            @Override public void removeUpdate(DocumentEvent e) { locDuLieu(); }
            @Override public void changedUpdate(DocumentEvent e) { locDuLieu(); }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                capNhatTrangThaiNut();
            }
        });

        btnLamMoi.addActionListener(e -> {
            txtTimKiem.setText("");
            cbTrangThai.setSelectedIndex(0);
            cbLoaiViPham.setSelectedIndex(0);
            ThemeConfig.showStatus(lblTrangThaiThaoTac, true, "");
            taiDuLieu();
        });

        btnXuatCsv.addActionListener(e ->
                CsvExportUtil.exportTableToCsv(this, table, "Danh_Sach_Vi_Pham", "DANH SÁCH VI PHẠM"));
        btnInPhieuPhat.addActionListener(e -> xemVaInPhieuPhat());
        btnXuLy.addActionListener(e -> xuLyViPham());

        taiDuLieu();
        SwingUtilities.invokeLater(() -> pageScroll.getVerticalScrollBar().setValue(0));
    }

    // ================= TẢI DANH SÁCH VI PHẠM =================

    public void taiDuLieu() {
        tableModel.setRowCount(0);
        List<ViPham> danhSach = dao.layDanhSachViPham();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        for (ViPham vp : danhSach) {
            String ngayGhiNhan = "";
            if (vp.getNgayGhiNhan() != null) {
                ngayGhiNhan = sdf.format(vp.getNgayGhiNhan());
            }

            int diemTru = tinhDiemTruThamChieu(vp.getLoaiViPham(), vp.getSoNgayQuaHan());
            long boiThuong = vp.getTienPhat() > 0 || "DA_XU_LY".equalsIgnoreCase(vp.getTrangThai())
                    ? vp.getTienPhat()
                    : tinhTienPhatThamChieu(vp.getLoaiViPham(), vp.getSoNgayQuaHan());
            String hinhThuc = "TRA_MUON".equals(vp.getLoaiViPham())
                    ? ("-" + diemTru + " điểm (0 đ)")
                    : ("-" + diemTru + " điểm" + (boiThuong > 0 ? " • Bồi thường " + String.format("%,d đ", boiThuong) : ""));

            tableModel.addRow(new Object[] {
                    vp.getId(),
                    vp.getMaViPham(),
                    vp.getTenDocGia(),
                    vp.getMaPhieu(),
                    vp.getTenSach(),
                    vp.getLoaiViPham(),
                    vp.getSoNgayQuaHan(),
                    hinhThuc,
                    ngayGhiNhan,
                    vp.getTrangThai(),
                    vp.getGhiChuXuLy() == null ? vp.getMoTa()
                            : Objects.toString(vp.getMoTa(), "") + " | Xử lý: " + vp.getGhiChuXuLy()
            });
        }

        locDuLieu();
    }

    // ================= TÌM KIẾM VÀ LỌC VI PHẠM =================

    private void locDuLieu() {
        String tuKhoa = ThemeConfig.normalizeText(txtTimKiem.getText());
        String trangThai = Objects.toString(cbTrangThai.getSelectedItem(), "Tất cả");
        String loaiLoc = Objects.toString(cbLoaiViPham.getSelectedItem(), "Tất cả loại");

        sorter.setRowFilter(
                new RowFilter<DefaultTableModel, Integer>() {
                    @Override
                    public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                        String noiDung = ThemeConfig.normalizeText(entry.getStringValue(1) + " " +
                                entry.getStringValue(2) + " " +
                                entry.getStringValue(3) + " " +
                                entry.getStringValue(4) + " " +
                                entry.getStringValue(5) + " " +
                                entry.getStringValue(7) + " " +
                                entry.getStringValue(10));

                        boolean dungTuKhoa = tuKhoa.isEmpty() || noiDung.contains(tuKhoa);
                        String trangThaiDong = entry.getStringValue(9);
                        String loaiDong = entry.getStringValue(5);

                        boolean dungTrangThai = "Tất cả".equals(trangThai)
                                || ("Chưa xử lý".equals(trangThai)
                                        && "CHUA_XU_LY".equals(trangThaiDong))
                                || ("Đã xử lý".equals(trangThai)
                                        && "DA_XU_LY".equals(trangThaiDong));

                        boolean dungLoai = "Tất cả loại".equals(loaiLoc)
                                || loaiLoc.equals(loaiDong);

                        return dungTuKhoa && dungTrangThai && dungLoai;
                    }
                });

        lblTong.setText("Hiển thị " + table.getRowCount() + " / " + tableModel.getRowCount() + " vi phạm");
        capNhatTrangThaiNut();
    }

    public void focusTimKiem() {
        txtTimKiem.requestFocusInWindow();
        txtTimKiem.selectAll();
    }

    public void moVaLocViPham(String tuKhoa, String trangThaiLoc) {
        cbLoaiViPham.setSelectedIndex(0);
        if (trangThaiLoc != null && !trangThaiLoc.isBlank()) {
            cbTrangThai.setSelectedItem(trangThaiLoc);
        } else {
            cbTrangThai.setSelectedIndex(0);
        }
        txtTimKiem.setText(tuKhoa == null ? "" : tuKhoa);
        ThemeConfig.showStatus(lblTrangThaiThaoTac, true,
                tuKhoa != null && !tuKhoa.isBlank()
                        ? "Đang lọc vi phạm liên quan tới: " + tuKhoa
                        : "Đang hiển thị danh sách vi phạm.");
        taiDuLieu();
        if (table.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
            table.scrollRectToVisible(table.getCellRect(0, 0, true));
        }
    }

    private void capNhatTrangThaiNut() {
        int dongChon = table.getSelectedRow();
        if (dongChon < 0) {
            btnInPhieuPhat.setEnabled(false);
            btnXuLy.setEnabled(false);
            return;
        }
        int dongModel = table.convertRowIndexToModel(dongChon);
        String trangThai = Objects.toString(tableModel.getValueAt(dongModel, 9), "");
        btnInPhieuPhat.setEnabled(true);
        btnXuLy.setEnabled("CHUA_XU_LY".equalsIgnoreCase(trangThai));
    }

    public static long tinhTienPhatThamChieu(String loaiViPham, int soNgayQuaHan) {
        return ViPhamDAO.tinhTienPhatMacDinh(loaiViPham, soNgayQuaHan);
    }

    public static long tinhTongPhatDuKienKhiTra(String tinhTrangTra, int soNgayQuaHan) {
        return ViPhamDAO.tinhTienPhatMacDinh(tinhTrangTra, soNgayQuaHan);
    }

    public static int tinhDiemTruThamChieu(String loaiViPham, int soNgayQuaHan) {
        return ViPhamDAO.tinhDiemTruViPham(loaiViPham, soNgayQuaHan);
    }

    private void xemVaInPhieuPhat() {
        int dongChon = table.getSelectedRow();
        if (dongChon < 0) {
            ThemeConfig.toastWarning(this, "Hãy chọn một dòng vi phạm để xem & in biên bản.");
            return;
        }
        int dongModel = table.convertRowIndexToModel(dongChon);
        moBienBan(((Number) tableModel.getValueAt(dongModel, 0)).intValue());
    }

    private void moBienBan(int idViPham) {
        btnInPhieuPhat.setEnabled(false);
        new SwingWorker<ViPham, Void>() {
            @Override
            protected ViPham doInBackground() throws Exception {
                return dao.layBienBanTheoId(idViPham);
            }

            @Override
            protected void done() {
                capNhatTrangThaiNut();
                try {
                    ViPham vp = get();
                    if (vp == null) {
                        ThemeConfig.toastWarning(QuanLyViPhamPanel.this, "Không tìm thấy biên bản vi phạm.");
                        return;
                    }
                    boolean daXuLy = "DA_XU_LY".equals(vp.getTrangThai());
                    java.util.Date ngayBienBan = daXuLy ? vp.getNgayXuLy() : vp.getNgayGhiNhan();
                    String ngay = ngayBienBan == null ? null
                            : new SimpleDateFormat("dd/MM/yyyy HH:mm").format(ngayBienBan);
                    ReceiptDialog.showPhieuPhat(
                            QuanLyViPhamPanel.this, vp.getMaViPham(), ngay, vp.getTenDocGia(),
                            vp.getMaPhieu(), vp.getTenSach(), vp.getLoaiViPham(), vp.getSoNgayQuaHan(),
                            vp.getTienPhat(), vp.getMoTa(), daXuLy ? "Đã xử lý" : "Chưa xử lý",
                            vp.getGhiChuXuLy(), tinhDiemTruThamChieu(vp.getLoaiViPham(), vp.getSoNgayQuaHan()),
                            vp.getDiemKhoiPhuc());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    System.err.println("Lỗi tải biên bản: " + e.getCause().getMessage());
                    ThemeConfig.toastError(QuanLyViPhamPanel.this, "Không tải được dữ liệu để in biên bản.");
                }
            }
        }.execute();
    }

    // ================= ĐÁNH DẤU VI PHẠM ĐÃ XỬ LÝ =================

    private void xuLyViPham() {
        int dongChon = table.getSelectedRow();
        if (dongChon < 0) {
            ThemeConfig.toastWarning(this, "Hãy chọn một vi phạm cần xử lý.");
            return;
        }

        int dongModel = table.convertRowIndexToModel(dongChon);
        Object rawId = tableModel.getValueAt(dongModel, 0);
        if (!(rawId instanceof Number nId)) {
            return;
        }
        int idViPham = nId.intValue();
        String maViPham = Objects.toString(tableModel.getValueAt(dongModel, 1), "");
        String tenDocGia = Objects.toString(tableModel.getValueAt(dongModel, 2), "");
        String loaiViPham = Objects.toString(tableModel.getValueAt(dongModel, 5), "");
        int soNgayQuaHan = tableModel.getValueAt(dongModel, 6) instanceof Number n ? n.intValue() : 0;
        String trangThai = Objects.toString(tableModel.getValueAt(dongModel, 9), "");

        if ("DA_XU_LY".equals(trangThai)) {
            ThemeConfig.toastInfo(this, "Vi phạm này đã được xử lý trước đó.");
            return;
        }

        boolean laTraMuon = "TRA_MUON".equals(loaiViPham);
        int diemTru = tinhDiemTruThamChieu(loaiViPham, soNgayQuaHan);
        long goiYBoiThuong = tinhTienPhatThamChieu(loaiViPham, soNgayQuaHan);

        JTextField txtTienBoiThuong = ThemeConfig.createTextField("Số tiền bồi thường (VNĐ)...");
        txtTienBoiThuong.setText(String.valueOf(goiYBoiThuong));
        JSpinner spDiemKhoiPhuc = new JSpinner(new SpinnerNumberModel(0, 0, 500, 5));
        spDiemKhoiPhuc.setFont(ThemeConfig.FONT_INPUT);
        JTextField txtGhiChuThu = ThemeConfig.createTextField("Ghi chú xác nhận xử lý (tùy chọn)...");

        Object[] formThu;
        if (laTraMuon) {
            formThu = new Object[] {
                    "Độc giả: " + tenDocGia + "  |  Mã VP: " + maViPham,
                    "Lỗi trả sách muộn: " + soNgayQuaHan + " ngày quá hạn",
                    "Hình thức kỷ luật: Đã trừ -" + diemTru + " điểm tích lũy (-"
                            + ViPhamDAO.DIEM_TRU_MOI_NGAY_QUA_HAN + " điểm/ngày). Phí phạt tiền mặt: 0 VNĐ.",
                    "Điểm uy tín khôi phục sau khi nhắc nhở / khắc phục (tùy chọn):",
                    spDiemKhoiPhuc,
                    "Ghi chú xử lý:",
                    txtGhiChuThu
            };
        } else {
            formThu = new Object[] {
                    "Độc giả: " + tenDocGia + "  |  Mã VP: " + maViPham,
                    "Loại vi phạm: " + loaiViPham + " (Đã trừ -" + diemTru + " điểm uy tín)",
                    "Số tiền bồi thường sách hư hỏng / mất (VNĐ):",
                    txtTienBoiThuong,
                    "Điểm uy tín khôi phục sau bồi thường (tùy chọn):",
                    spDiemKhoiPhuc,
                    "Ghi chú xử lý:",
                    txtGhiChuThu
            };
        }

        int xacNhan = JOptionPane.showConfirmDialog(
                this,
                formThu,
                laTraMuon ? "Xác nhận xử lý vi phạm trả muộn (trừ điểm)" : "Xác nhận bồi thường & xử lý vi phạm",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (xacNhan != JOptionPane.OK_OPTION) {
            return;
        }

        long tienBoiThuong = 0L;
        if (!laTraMuon) {
            try {
                tienBoiThuong = Long.parseLong(txtTienBoiThuong.getText().replace(",", "").replace(".", "").trim());
                if (tienBoiThuong < 0) {
                    ThemeConfig.toastWarning(this, "Số tiền bồi thường không được âm.");
                    return;
                }
            } catch (NumberFormatException ex) {
                ThemeConfig.toastWarning(this, "Số tiền bồi thường phải là số nguyên hợp lệ.");
                return;
            }
        }

        int diemKhoiPhuc = spDiemKhoiPhuc.getValue() instanceof Number n ? Math.max(0, n.intValue()) : 0;
        String ghiChu = txtGhiChuThu.getText().trim();
        boolean thanhCong = dao.danhDauDaXuLy(idViPham, tienBoiThuong, diemKhoiPhuc, ghiChu);

        if (thanhCong) {
            String tomTat = laTraMuon
                    ? ("Đã xử lý vi phạm " + maViPham + " (-" + diemTru + " điểm"
                            + (diemKhoiPhuc > 0 ? ", khôi phục +" + diemKhoiPhuc + " điểm" : "") + ").")
                    : ("Đã xử lý vi phạm " + maViPham + " (Bồi thường: "
                            + String.format("%,d", tienBoiThuong) + " VNĐ).");
            ThemeConfig.showStatus(lblTrangThaiThaoTac, true, tomTat);
            ThemeConfig.toastSuccess(this, tomTat);
            taiDuLieu();
            MainFrame.refreshMainDashboard(this);

            moBienBan(idViPham);
        } else {
            ThemeConfig.showStatus(lblTrangThaiThaoTac, false, "Không thể xử lý vi phạm đã chọn.");
            ThemeConfig.toastError(this, "Không thể xử lý vi phạm đã chọn.");
        }
    }

    // ============================================================
    // RENDERERS CHO BẢNG VI PHẠM
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

    private static class OverdueDaysRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.RIGHT);
            setBorder(new EmptyBorder(0, 10, 0, 10));

            int days = 0;
            if (value instanceof Number n) {
                days = n.intValue();
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

    private static class LoaiViPhamBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            String raw = value == null ? "" : value.toString().trim();

            JPanel wrapper = new JPanel(new GridBagLayout());
            if (isSelected) {
                wrapper.setBackground(ThemeConfig.snap(ThemeConfig.PRIMARY_LIGHT));
            } else {
                wrapper.setBackground(row % 2 == 0 ? ThemeConfig.snap(ThemeConfig.SURFACE_CARD) : ThemeConfig.snap(ThemeConfig.BG_PAGE));
            }

            JLabel badge = new JLabel(raw) {
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

            if (raw.contains("MẤT") || raw.contains("MAT")) {
                badge.setBackground(ThemeConfig.STATUS_ERROR_BG);
                badge.setForeground(ThemeConfig.STATUS_ERROR_FG);
            } else if (raw.contains("HỎNG") || raw.contains("HONG")) {
                badge.setBackground(ThemeConfig.STATUS_WARN_BG);
                badge.setForeground(ThemeConfig.STATUS_WARN_FG);
            } else {
                badge.setBackground(ThemeConfig.PRIMARY_LIGHT);
                badge.setForeground(ThemeConfig.STATUS_INFO_FG);
            }

            wrapper.add(badge);
            return wrapper;
        }
    }

    private static class TrangThaiViPhamBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            String st = value == null ? "" : value.toString();
            boolean daXuLy = "DA_XU_LY".equalsIgnoreCase(st) || "Đã xử lý".equalsIgnoreCase(st);

            JPanel wrapper = new JPanel(new GridBagLayout());
            if (isSelected) {
                wrapper.setBackground(ThemeConfig.snap(ThemeConfig.PRIMARY_LIGHT));
            } else {
                wrapper.setBackground(row % 2 == 0 ? ThemeConfig.snap(ThemeConfig.SURFACE_CARD) : ThemeConfig.snap(ThemeConfig.BG_PAGE));
            }

            JLabel badge = new JLabel(daXuLy ? "Đã xử lý" : "Chưa xử lý") {
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

            if (daXuLy) {
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