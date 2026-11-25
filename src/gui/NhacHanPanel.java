package gui;

import dao.ThongBaoDAO;
import model.ThongBaoChiTiet;
import org.kordamp.ikonli.feather.Feather;
import service.EmailService;
import service.ThongBaoService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Giao diện Trung tâm Nhắc hạn & Gửi Email Tự động.
 */
public class NhacHanPanel extends JPanel {

    public static final String PHAM_VI_NGAY_MAI = "Hạn ngày mai (Gửi tự động)";
    public static final String PHAM_VI_3_NGAY = "Sắp đến hạn (0 - 3 ngày tới)";
    public static final String PHAM_VI_QUA_HAN = "Quá hạn chưa trả";

    private static final ZoneId MUI_GIO = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter THOI_GIAN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final ThongBaoDAO thongBaoDAO = new ThongBaoDAO();
    private final ThongBaoService thongBaoService = ThongBaoService.getInstance();

    private final JLabel lblPhamVi = new JLabel();
    private final JLabel lblSoLieu = new JLabel("Chưa tải dữ liệu.");
    private final JLabel lblTieuDeDenHan = new JLabel("SÁCH CHƯA TRẢ CÓ HẠN NGÀY MAI");

    private final JComboBox<String> cbPhamViXem = new JComboBox<>(new String[] {
            PHAM_VI_NGAY_MAI,
            PHAM_VI_3_NGAY,
            PHAM_VI_QUA_HAN
    });
    private final JTextField txtTimKiem = new JTextField(18);

    private final JButton btnLamMoi = taoNutAction("Làm mới", Feather.REFRESH_CW, Color.WHITE, ThemeConfig.TEXT_PRIMARY,
            ThemeConfig.BORDER);
    private final JButton btnXemTruoc = taoNutAction("Xem trước email", Feather.EYE, Color.WHITE,
            ThemeConfig.TEXT_PRIMARY, ThemeConfig.BORDER);
    private final JButton btnQuetGui = taoNutAction("Gửi nhắc hạn ngày mai", Feather.SEND, ThemeConfig.PRIMARY, Color.WHITE,
            ThemeConfig.PRIMARY_HOVER);
    private final JButton btnMoMuonTra = taoNutAction("Xử lý tại Mượn & Trả", Feather.REPEAT, Color.WHITE,
            ThemeConfig.PRIMARY, ThemeConfig.BORDER);
    private final JButton btnChiTiet = taoNutAction("Chi tiết lịch sử", Feather.CLOCK, Color.WHITE,
            ThemeConfig.TEXT_PRIMARY, ThemeConfig.BORDER);

    private final DefaultTableModel modelDenHan = taoModel(
            new String[] {
                    "ID dòng",
                    "Mã phiếu",
                    "Mã thẻ",
                    "Độc giả",
                    "Tên sách",
                    "Số bản",
                    "Hạn trả",
                    "Tình trạng hạn trả",
                    "Email nhắc hạn",
                    "Lần thử"
            },
            0, 5, 9);

    private final DefaultTableModel modelLichSu = taoModel(
            new String[] {
                    "ID thông báo",
                    "Loại email",
                    "Mã thẻ",
                    "Độc giả",
                    "Tên sách",
                    "Email nhận",
                    "Trạng thái",
                    "Lần thử",
                    "Lần thử cuối",
                    "SMTP chấp nhận lúc"
            },
            0, 7);

    private final JTable bangDenHan = new JTable(modelDenHan);
    private final TableRowSorter<DefaultTableModel> sorterDenHan = new TableRowSorter<>(modelDenHan);
    private final JTable bangLichSu = new JTable(modelLichSu);

    private List<ThongBaoChiTiet> danhSachDenHan = List.of();
    private List<ThongBaoChiTiet> danhSachLichSu = List.of();

    private boolean dangXuLy;
    private boolean dangCapNhatBoLoc;
    private boolean canTaiLaiSauKhiXong;
    private boolean cauHinhGuiHopLe;
    private String moTaPhamVi = "";

    private record DuLieu(
            LocalDate ngayXet,
            String phamViXem,
            List<ThongBaoChiTiet> denHan,
            List<ThongBaoChiTiet> lichSu) {
    }

    public NhacHanPanel() {
        setLayout(new BorderLayout());
        setBackground(ThemeConfig.BG_PAGE);

        khoiTaoGiaoDien();
        ganSuKien();
        taiDuLieu();
    }

    private static JButton taoNutAction(String text, Feather icon, Color bg, Color fg, Color borderColor) {
        return ThemeConfig.styleActionButton(new JButton(text), icon, bg, fg, borderColor);
    }

    private void khoiTaoGiaoDien() {
        // ===== PHẦN TRÊN: HEADER CARD & TOOLBAR 2 TẦNG RÕ RÀNG =====
        JPanel headerCard = ThemeConfig.createCard();
        headerCard.setLayout(new BorderLayout(0, 14));

        JPanel thongTin = new JPanel();
        thongTin.setLayout(new BoxLayout(thongTin, BoxLayout.Y_AXIS));
        thongTin.setOpaque(false);

        lblPhamVi.setIconTextGap(8);
        lblPhamVi.setFont(ThemeConfig.FONT_LABEL);
        lblPhamVi.setForeground(ThemeConfig.TEXT_PRIMARY);
        lblPhamVi.setBorder(new EmptyBorder(0, 0, 2, 0));
        lblPhamVi.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblSoLieu.setFont(ThemeConfig.FONT_BODY);
        lblSoLieu.setForeground(ThemeConfig.TEXT_SECONDARY);
        lblSoLieu.setBorder(new EmptyBorder(0, 0, 2, 0));
        lblSoLieu.setAlignmentX(Component.LEFT_ALIGNMENT);

        thongTin.add(lblPhamVi);
        thongTin.add(Box.createVerticalStrut(4));
        thongTin.add(lblSoLieu);

        // Điều khiển tách 2 tầng: Hàng 1 lọc & tìm kiếm (trái), Hàng 2 nút hành động (phải)
        JPanel thanhCongCu = new JPanel();
        thanhCongCu.setLayout(new BoxLayout(thanhCongCu, BoxLayout.Y_AXIS));
        thanhCongCu.setOpaque(false);

        JPanel hangLocTim = new JPanel(new ThemeConfig.WrapFlowLayout(FlowLayout.LEFT, 12, 8));
        hangLocTim.setOpaque(false);
        hangLocTim.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblLoc = new JLabel("Phạm vi xem:");
        lblLoc.setFont(ThemeConfig.FONT_LABEL);
        lblLoc.setForeground(ThemeConfig.TEXT_PRIMARY);
        cbPhamViXem.setFont(ThemeConfig.FONT_INPUT);
        cbPhamViXem.setPreferredSize(new Dimension(235, ThemeConfig.INPUT_HEIGHT));

        hangLocTim.add(lblLoc);
        hangLocTim.add(cbPhamViXem);
        hangLocTim.add(ThemeConfig.createSearchBar(
                txtTimKiem, "Tìm mã phiếu, thẻ, độc giả, sách...", 280));

        JPanel hangNutHanhDong = new JPanel(new ThemeConfig.WrapFlowLayout(FlowLayout.RIGHT, 12, 8));
        hangNutHanhDong.setOpaque(false);
        hangNutHanhDong.setAlignmentX(Component.LEFT_ALIGNMENT);
        hangNutHanhDong.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeConfig.BORDER),
                new EmptyBorder(6, 0, 0, 0)));
        hangNutHanhDong.add(btnLamMoi);
        hangNutHanhDong.add(btnXemTruoc);
        hangNutHanhDong.add(btnQuetGui);
        hangNutHanhDong.add(btnChiTiet);

        thanhCongCu.add(hangLocTim);
        thanhCongCu.add(Box.createVerticalStrut(6));
        thanhCongCu.add(hangNutHanhDong);

        headerCard.add(thongTin, BorderLayout.NORTH);
        headerCard.add(thanhCongCu, BorderLayout.CENTER);

        // ===== CẤU HÌNH BẢNG & RENDERER =====
        cauHinhBang(bangDenHan, false);
        bangDenHan.setRowSorter(sorterDenHan);
        cauHinhBang(bangLichSu, true);

        JPanel cardDenHan = ThemeConfig.createCard();
        cardDenHan.setLayout(new BorderLayout(0, 12));

        JPanel denHanHeader = new JPanel(new BorderLayout(12, 0));
        denHanHeader.setOpaque(false);
        lblTieuDeDenHan.setIcon(ThemeConfig.getIcon(Feather.CALENDAR, 18, ThemeConfig.PRIMARY));
        lblTieuDeDenHan.setIconTextGap(8);
        lblTieuDeDenHan.setFont(ThemeConfig.FONT_H3);
        lblTieuDeDenHan.setForeground(ThemeConfig.TEXT_PRIMARY);
        lblTieuDeDenHan.setBorder(new EmptyBorder(0, 0, 2, 0));
        denHanHeader.add(lblTieuDeDenHan, BorderLayout.WEST);
        denHanHeader.add(btnMoMuonTra, BorderLayout.EAST);
        cardDenHan.add(denHanHeader, BorderLayout.NORTH);

        JScrollPane cuonDenHan = new JScrollPane(bangDenHan);
        cuonDenHan.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER, 1));
        cuonDenHan.setPreferredSize(new Dimension(0, 360));
        cuonDenHan.setMinimumSize(new Dimension(0, 340));
        ThemeConfig.installSaaSDataGrid(
                cuonDenHan, bangDenHan, Feather.CALENDAR,
                "Không có sách nào đến hạn trong phạm vi xem",
                "Hãy thử đổi phạm vi xem hoặc xóa từ khóa tìm kiếm.",
                65, 95, 90, 150, 195, 68, 105, 125, 115, 70);
        cardDenHan.add(cuonDenHan, BorderLayout.CENTER);

        JPanel cardLichSu = ThemeConfig.createCard();
        cardLichSu.setLayout(new BorderLayout(0, 12));
        JLabel lblTieuDeLichSu = new JLabel("LỊCH SỬ EMAIL NHẮC HẠN VÀ ĐẶT TRƯỚC");
        lblTieuDeLichSu.setIcon(ThemeConfig.getIcon(Feather.ARCHIVE, 18, ThemeConfig.PRIMARY));
        lblTieuDeLichSu.setIconTextGap(8);
        lblTieuDeLichSu.setFont(ThemeConfig.FONT_H3);
        lblTieuDeLichSu.setForeground(ThemeConfig.TEXT_PRIMARY);
        lblTieuDeLichSu.setBorder(new EmptyBorder(0, 0, 2, 0));
        cardLichSu.add(lblTieuDeLichSu, BorderLayout.NORTH);

        JScrollPane cuonLichSu = new JScrollPane(bangLichSu);
        cuonLichSu.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER, 1));
        cuonLichSu.setPreferredSize(new Dimension(0, 380));
        cuonLichSu.setMinimumSize(new Dimension(0, 350));
        ThemeConfig.installSaaSDataGrid(
                cuonLichSu, bangLichSu, Feather.ARCHIVE,
                "Chưa có lịch sử email",
                "Theo dõi trạng thái email nhắc hạn và email báo lượt đặt trước tại đây.",
                85, 115, 90, 135, 180, 165, 115, 68, 135, 135);
        cardLichSu.add(cuonLichSu, BorderLayout.CENTER);

        JPanel tablesStack = new JPanel(new BorderLayout(0, 22));
        tablesStack.setOpaque(false);
        tablesStack.add(cardDenHan, BorderLayout.NORTH);
        tablesStack.add(cardLichSu, BorderLayout.CENTER);

        ThemeConfig.ScrollablePanel pageContent = new ThemeConfig.ScrollablePanel();
        pageContent.setLayout(new BorderLayout(0, 22));
        pageContent.setOpaque(false);
        pageContent.setBorder(new EmptyBorder(24, 28, 24, 28));
        pageContent.add(headerCard, BorderLayout.NORTH);
        pageContent.add(tablesStack, BorderLayout.CENTER);

        JScrollPane pageScroll = ThemeConfig.createPageScrollPane(pageContent);
        add(pageScroll, BorderLayout.CENTER);
        SwingUtilities.invokeLater(() -> pageScroll.getVerticalScrollBar().setValue(0));
    }

    private void ganSuKien() {
        btnLamMoi.addActionListener(e -> taiDuLieu());
        btnXemTruoc.addActionListener(e -> xemTruoc());
        btnQuetGui.addActionListener(e -> quetVaGui());
        btnChiTiet.addActionListener(e -> xemChiTietLichSu());
        btnMoMuonTra.addActionListener(e -> {
            MainFrame mf = MainFrame.of(this);
            if (mf != null) {
                String loc = PHAM_VI_QUA_HAN.equals(cbPhamViXem.getSelectedItem())
                        ? "Chưa trả quá hạn"
                        : "Sắp đến hạn (3 ngày)";
                mf.moMuonTraVaLoc(loc);
            }
        });

        cbPhamViXem.addActionListener(e -> {
            if (!dangCapNhatBoLoc) {
                taiDuLieu();
            }
        });

        txtTimKiem.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { apDungBoLocDenHan(); }
            @Override public void removeUpdate(DocumentEvent e) { apDungBoLocDenHan(); }
            @Override public void changedUpdate(DocumentEvent e) { apDungBoLocDenHan(); }
        });

        bangDenHan.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                capNhatNut();
            }
        });

        bangLichSu.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                capNhatNut();
            }
        });
    }

    public void focusTimKiem() {
        txtTimKiem.requestFocusInWindow();
        txtTimKiem.selectAll();
    }

    public void moVaLocSapDenHan3Ngay() {
        dangCapNhatBoLoc = true;
        try {
            cbPhamViXem.setSelectedItem(PHAM_VI_3_NGAY);
            txtTimKiem.setText("");
        } finally {
            dangCapNhatBoLoc = false;
        }
        taiDuLieu();
    }

    public void moVaLocQuaHan() {
        dangCapNhatBoLoc = true;
        try {
            cbPhamViXem.setSelectedItem(PHAM_VI_QUA_HAN);
            txtTimKiem.setText("");
        } finally {
            dangCapNhatBoLoc = false;
        }
        taiDuLieu();
    }

    public void taiDuLieu() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::taiDuLieu);
            return;
        }

        if (dangXuLy) {
            canTaiLaiSauKhiXong = true;
            return;
        }

        canTaiLaiSauKhiXong = false;
        String phamVi = Objects.toString(cbPhamViXem.getSelectedItem(), PHAM_VI_NGAY_MAI);
        lblTieuDeDenHan.setText(switch (phamVi) {
            case PHAM_VI_3_NGAY -> "DANH SÁCH BẢN SÁCH SẮP ĐẾN HẠN TRONG 3 NGÀY TỚI";
            case PHAM_VI_QUA_HAN -> "DANH SÁCH BẢN SÁCH QUÁ HẠN CHƯA TRẢ";
            default -> "SÁCH CHƯA TRẢ CÓ HẠN NGÀY MAI";
        });

        capNhatPhamVi();
        xoaDuLieu();
        lblSoLieu.setText("Đang tải dữ liệu...");

        LocalDate ngayXet = LocalDate.now(MUI_GIO);

        chayNen(
                () -> {
                    List<ThongBaoChiTiet> dsDenHan = switch (phamVi) {
                        case PHAM_VI_3_NGAY -> thongBaoDAO.layDongTheoKhoangHanTra(ngayXet, ngayXet.plusDays(3));
                        case PHAM_VI_QUA_HAN -> thongBaoDAO.layDongQuaHanChuaTra(ngayXet);
                        default -> thongBaoDAO.layDongCanNhac(ngayXet);
                    };
                    return new DuLieu(
                            ngayXet,
                            phamVi,
                            dsDenHan,
                            thongBaoDAO.layLichSuEmail(200));
                },
                duLieu -> {
                    danhSachDenHan = List.copyOf(duLieu.denHan());
                    danhSachLichSu = List.copyOf(duLieu.lichSu());

                    for (ThongBaoChiTiet dong : danhSachDenHan) {
                        LocalDate hanTra = dong.getHanTra();
                        String tinhTrangHanTra = hanTra == null ? "—"
                                : hanTra.isBefore(duLieu.ngayXet()) ? "Quá hạn"
                                : hanTra.isEqual(duLieu.ngayXet()) ? "Hạn hôm nay" : "Sắp đến hạn";
                        String trangThaiEmail = dong.getId() > 0
                                ? hienThiTrangThai(dong.getTrangThai()) : "Chưa tạo";

                        modelDenHan.addRow(new Object[] {
                                dong.getIdChiTiet(),
                                dong.getMaPhieu(),
                                dong.getMaDocGia(),
                                dong.getTenDocGia(),
                                dong.getTenSach(),
                                dong.getSoLuong(),
                                dinhDangNgay(dong.getHanTra()),
                                tinhTrangHanTra,
                                trangThaiEmail,
                                dong.getSoLanThu()
                        });
                    }

                    for (ThongBaoChiTiet dong : danhSachLichSu) {
                        modelLichSu.addRow(new Object[] {
                                dong.getId(),
                                hienThiLoaiEmail(dong),
                                dong.getMaDocGia(),
                                dong.getTenDocGia(),
                                dong.getTenSach(),
                                giaTri(dong.getEmailNhan()),
                                hienThiTrangThai(dong.getTrangThai()),
                                dong.getSoLanThu(),
                                dinhDangThoiGian(dong.getLanThuCuoi()),
                                dinhDangThoiGian(dong.getNgayGui())
                        });
                    }

                    apDungBoLocDenHan();

                    String moTaMoc = switch (duLieu.phamViXem()) {
                        case PHAM_VI_3_NGAY -> "Sắp đến hạn ("
                                + duLieu.ngayXet().format(NGAY) + " -> "
                                + duLieu.ngayXet().plusDays(3).format(NGAY) + ")";
                        case PHAM_VI_QUA_HAN -> "Quá hạn trước ngày " + duLieu.ngayXet().format(NGAY);
                        default -> "Hạn trả ngày " + duLieu.ngayXet().plusDays(1).format(NGAY);
                    };

                    lblSoLieu.setText(
                            moTaMoc + ": " + danhSachDenHan.size()
                                    + " dòng mượn. Hiển thị "
                                    + danhSachLichSu.size()
                                    + " thông báo gần đây.");

                    capNhatNut();
                });
    }

    private void apDungBoLocDenHan() {
        String tuKhoa = ThemeConfig.normalizeText(txtTimKiem.getText());
        sorterDenHan.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                if (tuKhoa.isEmpty()) {
                    return true;
                }
                String gop = ThemeConfig.normalizeText(
                        entry.getStringValue(0) + " "
                                + entry.getStringValue(1) + " "
                                + entry.getStringValue(2) + " "
                                + entry.getStringValue(3) + " "
                                + entry.getStringValue(4) + " "
                                + entry.getStringValue(7) + " "
                                + entry.getStringValue(8));
                return gop.contains(tuKhoa);
            }
        });
    }

    private void xemTruoc() {
        if (dangXuLy) {
            return;
        }

        int dongChon = bangDenHan.getSelectedRow();
        if (dongChon < 0) {
            return;
        }

        int dongModel = bangDenHan.convertRowIndexToModel(dongChon);
        ThongBaoChiTiet dong = danhSachDenHan.get(dongModel);
        Integer idChiTiet = dong.getIdChiTiet();

        if (idChiTiet == null) {
            return;
        }

        chayNen(
                () -> thongBaoService.xemTruoc(idChiTiet),
                this::hienThiEmailPreview);
    }

    private void hienThiEmailPreview(service.ThongBaoService.BanXemTruoc ban) {
        if (!isShowing() || ban == null) {
            return;
        }

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Xem trước email nhắc hạn",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(750, 600);
        dialog.setLocationRelativeTo(this);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(ThemeConfig.BG_PAGE);
        content.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel headerCard = ThemeConfig.createCard();
        headerCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 6, 4, 6);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.15;
        headerCard.add(new JLabel("Chế độ gửi:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.85;
        JLabel lblCheDo = new JLabel(ban.cheDoGui());
        lblCheDo.setFont(ThemeConfig.FONT_LABEL);
        headerCard.add(lblCheDo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        headerCard.add(new JLabel("Người nhận:"), gbc);
        gbc.gridx = 1;
        headerCard.add(new JLabel(ban.tenDocGia() + " (" + ban.maDocGia() + ") — Hồ sơ: " + ban.emailDocGia()), gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        headerCard.add(new JLabel("Email nhận:"), gbc);
        gbc.gridx = 1;
        JLabel lblEmail = new JLabel(ban.emailNhan());
        lblEmail.setFont(ThemeConfig.FONT_LABEL);
        lblEmail.setForeground(ThemeConfig.PRIMARY);
        headerCard.add(lblEmail, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        headerCard.add(new JLabel("Tiêu đề:"), gbc);
        gbc.gridx = 1;
        JLabel lblTieuDe = new JLabel(ban.tieuDe());
        lblTieuDe.setFont(ThemeConfig.FONT_LABEL);
        headerCard.add(lblTieuDe, gbc);

        content.add(headerCard, BorderLayout.NORTH);

        JPanel bodyCard = ThemeConfig.createCard();
        bodyCard.setLayout(new BorderLayout(0, 8));

        JTextArea txtNoiDung = taoVungVanBan(ban.noiDung() + "\n\n--- GHI CHÚ ---\n" + ban.ghiChu());
        JScrollPane scrollNoiDung = new JScrollPane(txtNoiDung);
        scrollNoiDung.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER, 1));
        bodyCard.add(scrollNoiDung, BorderLayout.CENTER);

        content.add(bodyCard, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottomBar.setOpaque(false);
        JButton btnDong = taoNutAction("Đóng", Feather.X, Color.WHITE, ThemeConfig.TEXT_PRIMARY, ThemeConfig.BORDER);
        btnDong.addActionListener(e -> dialog.dispose());
        bottomBar.add(btnDong);

        content.add(bottomBar, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private void quetVaGui() {
        if (dangXuLy) {
            return;
        }

        capNhatPhamVi();
        capNhatNut();

        if (!cauHinhGuiHopLe) {
            return;
        }

        JTextArea noiDung = taoVungVanBan(
                moTaPhamVi
                        + "\n\nTác vụ quét toàn bộ dòng mượn chưa trả có hạn ngày mai, "
                        + "kể cả dòng đang ẩn theo từ khóa tìm kiếm."
                        + "\nCác thông báo đã gửi hoặc cần kiểm tra "
                        + "sẽ được bỏ qua."
                        + "\n\nBạn có chắc chắn muốn bắt đầu tiến trình gửi email?");

        JScrollPane cuon = new JScrollPane(noiDung);
        cuon.setPreferredSize(new Dimension(650, 220));

        Object[] luaChon = { "Hủy", "Quét và gửi" };

        int ketQua = JOptionPane.showOptionDialog(
                this,
                cuon,
                "Xác nhận quét và gửi email nhắc hạn",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                luaChon,
                luaChon[0]);

        if (ketQua != 1) {
            return;
        }

        chayNen(
                thongBaoService::quetVaGui,
                ketQuaQuet -> {
                    hienThiVanBan("Kết quả quét nhắc hạn", ketQuaQuet.tomTat());
                    taiDuLieu();
                });
    }

    private void xemChiTietLichSu() {
        if (dangXuLy) {
            return;
        }

        int dongChon = bangLichSu.getSelectedRow();
        if (dongChon < 0) {
            return;
        }

        int dongModel = bangLichSu.convertRowIndexToModel(dongChon);
        ThongBaoChiTiet dong = danhSachLichSu.get(dongModel);

        String noiDung = "Mã thông báo: " + giaTri(dong.getMaThongBao())
                + "\nLoại email: " + hienThiLoaiEmail(dong)
                + "\nID dòng mượn: " + (dong.getIdChiTiet() == null ? "—" : dong.getIdChiTiet())
                + "\nMã thẻ: " + giaTri(dong.getMaDocGia())
                + "\nĐộc giả: " + giaTri(dong.getTenDocGia())
                + "\nTên sách: " + giaTri(dong.getTenSach())
                + "\nEmail nhận đã lưu: " + giaTri(dong.getEmailNhan())
                + "\nMốc nhắc: " + giaTri(dong.getMocNhac())
                + "\nTrạng thái: " + hienThiTrangThai(dong.getTrangThai())
                + "\nSố lần nhận xử lý: " + dong.getSoLanThu()
                + "\nNgày tạo: " + dinhDangThoiGian(dong.getNgayTao())
                + "\nLần thử cuối: " + dinhDangThoiGian(dong.getLanThuCuoi())
                + "\nSMTP chấp nhận lúc: " + dinhDangThoiGian(dong.getNgayGui())
                + "\n\nTiêu đề đã lưu:\n" + giaTri(dong.getTieuDe())
                + "\n\nNội dung đã lưu:\n" + giaTri(dong.getNoiDung())
                + "\n\nLỗi / Ghi chú phản hồi:\n" + giaTri(dong.getLoiGui());

        if ("CAN_KIEM_TRA".equals(dong.getTrangThai())
                || "DANG_GUI".equals(dong.getTrangThai())) {
            noiDung += "\n\nGhi chú: Chưa có kết luận chắc chắn về lần gửi này."
                    + " Ngày SMTP chấp nhận để trống không khẳng định email chưa được gửi.";
        }

        hienThiVanBan("Chi tiết lịch sử nhắc hạn", noiDung);
    }

    private void capNhatPhamVi() {
        cauHinhGuiHopLe = false;

        try {
            String cheDo = EmailService.cheDoGui();
            boolean cheDoThu = "TEST".equals(cheDo);
            String tienTo = EmailService.tienToMaTheGui();

            new EmailService().kiemTraCauHinh();

            if (cheDoThu) {
                moTaPhamVi = "Chế độ thử: Mã thẻ bắt đầu bằng [" + tienTo.trim()
                        + "]. Email nhận thử: " + giaTri(System.getenv("SMTP_TEST_TO"));
                lblPhamVi.setIcon(ThemeConfig.getIcon(Feather.SLIDERS, 16, ThemeConfig.PRIMARY));
            } else {
                moTaPhamVi = "Chế độ gửi thật: Gửi trực tiếp đến email trong hồ sơ của từng độc giả đủ điều kiện.";
                lblPhamVi.setIcon(ThemeConfig.getIcon(Feather.GLOBE, 16, ThemeConfig.SUCCESS));
            }

            cauHinhGuiHopLe = true;
            lblPhamVi.setText(moTaPhamVi);
            lblPhamVi.setToolTipText(null);
            btnQuetGui.setToolTipText("Quét toàn bộ dòng chưa trả có hạn ngày mai trong phạm vi gửi.");
        } catch (RuntimeException e) {
            moTaPhamVi = "Chưa đủ cấu hình gửi email SMTP.";
            lblPhamVi.setIcon(ThemeConfig.getIcon(Feather.ALERT_TRIANGLE, 16, ThemeConfig.STATUS_WARN_FG));
            lblPhamVi.setText(moTaPhamVi + " Bạn vẫn có thể tải dữ liệu và xem trước nội dung.");

            String thongDiep = thongDiepLoi(e);
            lblPhamVi.setToolTipText(thongDiep);
            btnQuetGui.setToolTipText(thongDiep);
        }
    }

    private void capNhatNut() {
        btnLamMoi.setEnabled(!dangXuLy);
        int hang = bangDenHan.getSelectedRow();
        int dong = hang < 0 ? -1 : bangDenHan.convertRowIndexToModel(hang);
        boolean hanNgayMai = dong >= 0 && dong < danhSachDenHan.size()
                && LocalDate.now(MUI_GIO).plusDays(1).equals(danhSachDenHan.get(dong).getHanTra());
        btnXemTruoc.setEnabled(!dangXuLy && hanNgayMai);
        btnXemTruoc.setToolTipText("Chọn dòng sách chưa trả có hạn ngày mai để xem mẫu email.");
        btnChiTiet.setEnabled(!dangXuLy && bangLichSu.getSelectedRow() >= 0);
        btnQuetGui.setEnabled(!dangXuLy && cauHinhGuiHopLe);
        btnMoMuonTra.setEnabled(!dangXuLy);
        cbPhamViXem.setEnabled(!dangXuLy);
        txtTimKiem.setEnabled(!dangXuLy);

        bangDenHan.setEnabled(!dangXuLy);
        bangLichSu.setEnabled(!dangXuLy);
    }

    private void xoaDuLieu() {
        danhSachDenHan = List.of();
        danhSachLichSu = List.of();
        modelDenHan.setRowCount(0);
        modelLichSu.setRowCount(0);
    }

    private <T> void chayNen(Callable<T> congViec, Consumer<T> khiThanhCong) {
        if (dangXuLy) {
            return;
        }

        dangXuLy = true;
        capNhatNut();

        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return congViec.call();
            }

            @Override
            protected void done() {
                T ketQua;
                try {
                    ketQua = get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    baoLoi(e);
                    return;
                } catch (ExecutionException e) {
                    baoLoi(e.getCause() == null ? e : e.getCause());
                    return;
                } finally {
                    dangXuLy = false;
                    capNhatNut();
                }

                khiThanhCong.accept(ketQua);

                if (canTaiLaiSauKhiXong) {
                    canTaiLaiSauKhiXong = false;
                    taiDuLieu();
                }
            }
        }.execute();
    }

    private void hienThiVanBan(String tieuDe, String noiDung) {
        if (!isShowing()) {
            return;
        }

        JScrollPane cuon = new JScrollPane(taoVungVanBan(noiDung));
        cuon.setPreferredSize(new Dimension(820, 520));

        JOptionPane.showMessageDialog(
                this,
                cuon,
                tieuDe,
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void baoLoi(Throwable e) {
        String thongDiep = thongDiepLoi(e);
        lblSoLieu.setText("Có lỗi xảy ra. Nhấn Làm mới để tải lại dữ liệu.");

        if (isShowing()) {
            JOptionPane.showMessageDialog(
                    this,
                    thongDiep,
                    "Không hoàn tất thao tác",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            System.err.println("[Nhắc hạn] " + thongDiep);
        }
    }

    private static JTextArea taoVungVanBan(String noiDung) {
        JTextArea vung = new JTextArea(noiDung);
        vung.setEditable(false);
        vung.setLineWrap(true);
        vung.setWrapStyleWord(true);
        vung.setFont(ThemeConfig.FONT_BODY);
        vung.setMargin(new Insets(12, 12, 12, 12));
        vung.setCaretPosition(0);
        return vung;
    }

    private static void cauHinhBang(JTable bang, boolean lichSu) {
        ThemeConfig.styleTable(bang);
        if (lichSu) {
            bang.setAutoCreateRowSorter(true);
        }
        bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bang.getTableHeader().setReorderingAllowed(false);
        bang.setFillsViewportHeight(true);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

        ThemeConfig.StatusBadgeRenderer badgeRenderer = new ThemeConfig.StatusBadgeRenderer();

        if (!lichSu) {
            bang.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
            bang.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
            bang.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
            bang.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);
            bang.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);
            bang.getColumnModel().getColumn(7).setCellRenderer(badgeRenderer);
            bang.getColumnModel().getColumn(8).setCellRenderer(badgeRenderer);
            bang.getColumnModel().getColumn(9).setCellRenderer(centerRenderer);

            bang.getColumnModel().getColumn(0).setPreferredWidth(60);
            bang.getColumnModel().getColumn(1).setPreferredWidth(80);
            bang.getColumnModel().getColumn(2).setPreferredWidth(90);
            bang.getColumnModel().getColumn(3).setPreferredWidth(140);
            bang.getColumnModel().getColumn(4).setPreferredWidth(180);
            bang.getColumnModel().getColumn(5).setPreferredWidth(60);
            bang.getColumnModel().getColumn(6).setPreferredWidth(100);
            bang.getColumnModel().getColumn(7).setPreferredWidth(110);
            bang.getColumnModel().getColumn(8).setPreferredWidth(110);
            bang.getColumnModel().getColumn(9).setPreferredWidth(65);
        } else {
            bang.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
            bang.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
            bang.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
            bang.getColumnModel().getColumn(6).setCellRenderer(badgeRenderer);
            bang.getColumnModel().getColumn(7).setCellRenderer(centerRenderer);
            bang.getColumnModel().getColumn(8).setCellRenderer(centerRenderer);
            bang.getColumnModel().getColumn(9).setCellRenderer(centerRenderer);

            bang.getColumnModel().getColumn(0).setPreferredWidth(80);
            bang.getColumnModel().getColumn(1).setPreferredWidth(60);
            bang.getColumnModel().getColumn(2).setPreferredWidth(90);
            bang.getColumnModel().getColumn(3).setPreferredWidth(130);
            bang.getColumnModel().getColumn(4).setPreferredWidth(170);
            bang.getColumnModel().getColumn(5).setPreferredWidth(160);
            bang.getColumnModel().getColumn(6).setPreferredWidth(110);
            bang.getColumnModel().getColumn(7).setPreferredWidth(65);
            bang.getColumnModel().getColumn(8).setPreferredWidth(130);
            bang.getColumnModel().getColumn(9).setPreferredWidth(130);
        }
    }

    private static DefaultTableModel taoModel(String[] tenCot, int... cotSoNguyen) {
        return new DefaultTableModel(tenCot, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                for (int cot : cotSoNguyen) {
                    if (cot == columnIndex) {
                        return Integer.class;
                    }
                }
                return String.class;
            }
        };
    }

    private static String hienThiTrangThai(String trangThai) {
        if (trangThai == null) {
            return "Chưa xác định";
        }

        return switch (trangThai) {
            case "CHUA_GUI" -> "Chờ gửi";
            case "DANG_GUI" -> "Đang xử lý";
            case "DA_GUI" -> "Đã gửi";
            case "LOI" -> "Lỗi gửi";
            case "CAN_KIEM_TRA" -> "Cần kiểm tra";
            case "HUY" -> "Đã hủy";
            default -> trangThai;
        };
    }

    private static String hienThiLoaiEmail(ThongBaoChiTiet thongBao) {
        return "DEN_LUOT_DAT_TRUOC".equals(thongBao.getLoaiThongBao())
                ? "Đặt trước" : "Nhắc hạn";
    }

    private static String dinhDangNgay(LocalDate ngay) {
        return ngay == null ? "—" : ngay.format(NGAY);
    }

    private static String dinhDangThoiGian(Timestamp thoiGian) {
        return thoiGian == null ? "—" : thoiGian.toLocalDateTime().format(THOI_GIAN);
    }

    private static String giaTri(String noiDung) {
        return noiDung == null || noiDung.isBlank() ? "—" : noiDung;
    }

    private static String thongDiepLoi(Throwable e) {
        String thongDiep = e.getMessage();
        if (thongDiep == null || thongDiep.isBlank()) {
            thongDiep = e.getClass().getSimpleName();
        }

        String matKhau = System.getenv("SMTP_PASSWORD");
        if (matKhau != null && !matKhau.isEmpty()) {
            thongDiep = thongDiep.replace(matKhau, "***");
        }

        return thongDiep.length() > 1000 ? thongDiep.substring(0, 1000) : thongDiep;
    }
}
