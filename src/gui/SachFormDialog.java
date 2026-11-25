package gui;

import dao.DanhMucDAO;
import dao.SachDAO;
import model.DanhMuc;
import model.Sach;
import org.kordamp.ikonli.feather.Feather;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.Year;

/**
 * Hộp thoại Thêm / Chỉnh sửa sách — Chuẩn Modal Dialog UX theo UI_STYLE_GUIDE.md.
 * <ul>
 *   <li>Top-aligned labels, đánh dấu * đỏ cho các trường bắt buộc</li>
 *   <li>Inputs chiều cao 36px, padding thoáng đãng</li>
 *   <li>Nút Lưu (Primary Blue), Hủy (Secondary outline)</li>
 *   <li>Bảo toàn 100% logic validation và lưu dữ liệu</li>
 * </ul>
 */
public class SachFormDialog extends JDialog {

    private final Sach sach;
    private final SachDAO sachDAO;
    private final DanhMucDAO danhMucDAO;

    private final int soLuongTongCu;
    private final int soLuongConCu;

    private boolean daLuu;
    private boolean danhMucHopLe = true;

    private final JTextField txtMaSach = new JTextField();
    private final JTextField txtTenSach = new JTextField();
    private final JTextField txtTacGia = new JTextField();
    private final JTextField txtNXB = new JTextField();

    private final JTextField txtNamXB =
            new JTextField(String.valueOf(Year.now().getValue()));

    private final JTextField txtSoLuongTong = new JTextField("0");
    private final JTextField txtGiaThue = new JTextField("10000");
    private final JTextArea txtMoTa = new JTextArea(3, 20);
    private final JComboBox<DanhMuc> cbDanhMuc = new JComboBox<>();

    private final JButton btnLuu = new JButton();

    public SachFormDialog(
            JFrame parent,
            Sach sach,
            SachDAO sachDAO,
            DanhMucDAO danhMucDAO
    ) {
        super(
                parent,
                sach == null ? "Thêm sách mới" : "Chỉnh sửa thông tin sách",
                true
        );

        this.sach = sach;
        this.sachDAO = sachDAO;
        this.danhMucDAO = danhMucDAO;

        this.soLuongTongCu =
                sach == null ? 0 : sach.getSoLuongTong();

        this.soLuongConCu =
                sach == null ? 0 : sach.getSoLuongCon();

        khoiTaoGiaoDien();

        if (sach != null) {
            dienDuLieu();
        }
    }

    private void khoiTaoGiaoDien() {
        setSize(640, 680);
        setLocationRelativeTo(getParent());
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
        mainPanel.setBackground(ThemeConfig.SURFACE_CARD);
        mainPanel.setBorder(new EmptyBorder(24, 28, 24, 28));

        // Header Dialog
        JPanel header = ThemeConfig.createTitleBlock(
                sach == null ? "THÊM ĐẦU SÁCH MỚI" : "CHỈNH SỬA ĐẦU SÁCH",
                "Vui lòng điền đầy đủ các trường thông tin có dấu sao đỏ (*)",
                Feather.BOOK);
        mainPanel.add(header, BorderLayout.NORTH);

        // Form Fields (GridBagLayout: 2 cột cân đối, Top-Aligned Labels)
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(7, 9, 7, 9);

        // Styling inputs
        setupInput(txtMaSach, "Mã sách duy nhất...");
        setupInput(txtTenSach, "Tên sách...");
        setupInput(txtTacGia, "Tên tác giả...");
        setupInput(txtNXB, "Nhà xuất bản...");
        setupInput(txtNamXB, "Năm xuất bản...");
        setupInput(txtSoLuongTong, "Số lượng tổng...");
        setupInput(txtGiaThue, "Giá thuê / lượt (VNĐ)...");

        cbDanhMuc.setFont(ThemeConfig.FONT_BODY);
        cbDanhMuc.setPreferredSize(new Dimension(0, ThemeConfig.INPUT_HEIGHT));
        cbDanhMuc.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) {
                    setText("Chưa phân loại");
                }
                return this;
            }
        });

        cbDanhMuc.addItem(null);
        for (DanhMuc dm : danhMucDAO.layTatCaDanhMuc()) {
            cbDanhMuc.addItem(dm);
        }

        txtMaSach.setEditable(sach == null);

        // Row 0
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.5;
        form.add(createFieldBlock("Mã sách", true, txtMaSach), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.5;
        form.add(createFieldBlock("Tên sách", true, txtTenSach), gbc);

        // Row 1
        gbc.gridx = 0; gbc.gridy = 1;
        form.add(createFieldBlock("Tác giả", true, txtTacGia), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        form.add(createFieldBlock("Nhà xuất bản", false, txtNXB), gbc);

        // Row 2
        gbc.gridx = 0; gbc.gridy = 2;
        form.add(createFieldBlock("Năm xuất bản", true, txtNamXB), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        form.add(createFieldBlock("Danh mục", false, cbDanhMuc), gbc);

        // Row 3
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        form.add(createFieldBlock("Số lượng tổng", true, txtSoLuongTong), gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 1;
        form.add(createFieldBlock("Giá thuê / lượt (VNĐ)", true, txtGiaThue), gbc);

        // Row 4: Mô tả
        txtMoTa.setFont(ThemeConfig.FONT_BODY);
        txtMoTa.setLineWrap(true);
        txtMoTa.setWrapStyleWord(true);
        JScrollPane scrollMoTa = new JScrollPane(txtMoTa);
        scrollMoTa.setPreferredSize(new Dimension(0, 80));
        scrollMoTa.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER, 1));

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        form.add(createFieldBlock("Mô tả nội dung:", false, scrollMoTa), gbc);

        mainPanel.add(form, BorderLayout.CENTER);

        // Buttons
        btnLuu.setText(sach == null ? "Thêm sách" : "Lưu thay đổi");
        ThemeConfig.styleActionButton(
                btnLuu,
                sach == null ? Feather.PLUS : Feather.CHECK,
                ThemeConfig.PRIMARY,
                Color.WHITE,
                ThemeConfig.PRIMARY_HOVER);

        JButton btnHuy = ThemeConfig.styleActionButton(
                new JButton("Hủy bỏ"),
                Feather.X,
                ThemeConfig.SURFACE_CARD,
                ThemeConfig.TEXT_PRIMARY,
                ThemeConfig.BORDER);

        btnLuu.addActionListener(e -> luuSach());
        btnHuy.addActionListener(e -> dispose());

        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        nut.setOpaque(false);
        nut.add(btnHuy);
        nut.add(btnLuu);

        mainPanel.add(nut, BorderLayout.SOUTH);
        setContentPane(mainPanel);
    }

    private void setupInput(JTextField tf, String placeholder) {
        tf.setFont(ThemeConfig.FONT_BODY);
        tf.setPreferredSize(new Dimension(0, ThemeConfig.INPUT_HEIGHT));
        tf.putClientProperty("JTextField.placeholderText", placeholder);
    }

    private JPanel createFieldBlock(String labelText, boolean required, JComponent comp) {
        JPanel block = new JPanel(new BorderLayout(0, 4));
        block.setOpaque(false);

        JLabel lbl = new JLabel("<html>" + labelText + (required ? " <font color='#DC2626'>*</font>" : "") + "</html>");
        lbl.setFont(ThemeConfig.FONT_LABEL);
        lbl.setForeground(ThemeConfig.TEXT_PRIMARY);

        block.add(lbl, BorderLayout.NORTH);
        block.add(comp, BorderLayout.CENTER);
        return block;
    }

    private void dienDuLieu() {
        txtMaSach.setText(sach.getMaSach());
        txtTenSach.setText(sach.getTenSach());
        txtTacGia.setText(sach.getTacGia());
        txtNXB.setText(sach.getNhaXuatBan());

        txtNamXB.setText(
                String.valueOf(sach.getNamXuatBan())
        );

        txtSoLuongTong.setText(
                String.valueOf(soLuongTongCu)
        );
        txtGiaThue.setText(
                String.valueOf(sach.getGiaThue())
        );

        txtMoTa.setText(sach.getMoTa());

        cbDanhMuc.setSelectedItem(null);

        if (sach.getIdDanhMuc() > 0) {
            boolean timThay = false;

            for (int i = 0; i < cbDanhMuc.getItemCount(); i++) {
                DanhMuc dm = cbDanhMuc.getItemAt(i);

                if (dm != null
                        && dm.getId() == sach.getIdDanhMuc()) {
                    cbDanhMuc.setSelectedIndex(i);
                    timThay = true;
                    break;
                }
            }

            if (!timThay) {
                danhMucHopLe = false;
                btnLuu.setEnabled(false);

                JOptionPane.showMessageDialog(
                        this,
                        "Chưa tải được danh mục của sách.\n"
                                + "Hãy đóng hộp thoại và tải lại dữ liệu."
                );
            }
        }
    }

    private void luuSach() {
        if (!danhMucHopLe) {
            return;
        }

        String maSach = txtMaSach.getText().trim();
        String tenSach = txtTenSach.getText().trim();

        if (maSach.isEmpty() || tenSach.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Mã sách và tên sách không được trống."
            );
            return;
        }

        int namXuatBan;
        int soLuongTongMoi;
        long giaThue;

        try {
            namXuatBan = Integer.parseInt(
                    txtNamXB.getText().trim()
            );

            soLuongTongMoi = Integer.parseInt(
                    txtSoLuongTong.getText().trim()
            );

            giaThue = Long.parseLong(
                    txtGiaThue.getText().replace(",", "").replace(".", "").trim()
            );

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Năm xuất bản, số lượng và giá thuê phải là số nguyên."
            );
            return;
        }

        if (namXuatBan <= 0 || soLuongTongMoi < 0 || giaThue < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Năm phải lớn hơn 0; số lượng và giá thuê không được âm."
            );
            return;
        }

        if (sach == null && sachDAO.tonTaiMaSach(maSach)) {
            JOptionPane.showMessageDialog(
                    this, "Mã sách đã tồn tại."
            );
            return;
        }

        long soLuongConMoi = sach == null
                ? soLuongTongMoi
                : (long) soLuongConCu
                + soLuongTongMoi
                - soLuongTongCu;

        if (soLuongConMoi < 0
                || soLuongConMoi > Integer.MAX_VALUE) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không thể giảm tổng số bản thấp hơn "
                            + "số bản đang được mượn."
            );
            return;
        }

        Sach s = new Sach();

        if (sach != null) {
            s.setId(sach.getId());
        }

        s.setMaSach(maSach);
        s.setTenSach(tenSach);
        s.setTacGia(txtTacGia.getText().trim());
        s.setNhaXuatBan(txtNXB.getText().trim());
        s.setNamXuatBan(namXuatBan);

        s.setSoLuongTong(soLuongTongMoi);
        s.setSoLuongCon((int) soLuongConMoi);
        s.setGiaThue(giaThue);

        DanhMuc dm = (DanhMuc) cbDanhMuc.getSelectedItem();
        s.setIdDanhMuc(dm == null ? 0 : dm.getId());

        s.setMoTa(txtMoTa.getText().trim());

        if (sach == null) {
            s.setHinhAnh("");
            s.setTrangThai(true);
        } else {
            s.setHinhAnh(sach.getHinhAnh());
            s.setTrangThai(sach.isTrangThai());
        }

        boolean thanhCong = sach == null
                ? sachDAO.themSach(s)
                : sachDAO.capNhatSach(
                s,
                soLuongTongCu,
                soLuongConCu
        );

        if (thanhCong) {
            daLuu = true;

            JOptionPane.showMessageDialog(
                    this, "Lưu sách thành công."
            );

            dispose();
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "Không lưu được. Hãy kiểm tra mã sách, danh mục.\n"
                            + "Nếu đang sửa, hãy đóng hộp thoại "
                            + "và tải lại sách trước khi sửa tiếp."
            );
        }
    }

    public boolean isDaLuu() {
        return daLuu;
    }
}
