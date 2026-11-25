package gui;

import dao.ThongKeDAO;
import org.kordamp.ikonli.feather.Feather;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class BangXepHangSachPanel extends JPanel {
    private final ThongKeDAO thongKeDAO = new ThongKeDAO();
    private final JLabel lblTrangThai = new JLabel(" ");
    private final JButton btnLamMoi = new JButton("Làm mới");
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Hạng", "Mã sách", "Tên sách", "Lượt mượn", "Tổng số bản mượn"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return switch (column) {
                case 0 -> Integer.class;
                case 3, 4 -> Long.class;
                default -> String.class;
            };
        }
    };
    private boolean dangTai;
    private boolean taiLai;

    public BangXepHangSachPanel() {
        super(new BorderLayout());
        setBorder(new EmptyBorder(24, 28, 24, 28));
        setBackground(ThemeConfig.BG_PAGE);

        JPanel card = ThemeConfig.createCard();
        card.setLayout(new BorderLayout(12, 16));
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.add(ThemeConfig.createTitleBlock("Bảng xếp hạng sách",
                "Mỗi phiếu tính một lượt cho từng đầu sách. Khi bằng lượt, ưu tiên số bản rồi đến mã sách.",
                Feather.AWARD), BorderLayout.CENTER);
        ThemeConfig.styleActionButton(btnLamMoi, Feather.REFRESH_CW, ThemeConfig.SURFACE_CARD,
                ThemeConfig.PRIMARY, ThemeConfig.BORDER);
        btnLamMoi.addActionListener(e -> taiDuLieu());
        header.add(btnLamMoi, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JTable table = new JTable(model);
        ThemeConfig.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        ThemeConfig.installSaaSDataGrid(scroll, table, Feather.BOOK, "Chưa có lượt mượn",
                "Lập phiếu mượn để xem đầu sách trong bảng xếp hạng.", 70, 130, 310, 110, 160);
        card.add(scroll, BorderLayout.CENTER);
        lblTrangThai.setFont(ThemeConfig.FONT_SM);
        card.add(lblTrangThai, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

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
        btnLamMoi.setEnabled(false);
        model.setRowCount(0);
        lblTrangThai.setText("Đang tải bảng xếp hạng…");
        new SwingWorker<List<ThongKeDAO.HangSach>, Void>() {
            @Override
            protected List<ThongKeDAO.HangSach> doInBackground() throws Exception {
                return thongKeDAO.layBangXepHangSach();
            }

            @Override
            protected void done() {
                dangTai = false;
                if (taiLai) {
                    taiLai = false;
                    taiDuLieu();
                    return;
                }
                try {
                    List<ThongKeDAO.HangSach> rows = get();
                    for (ThongKeDAO.HangSach sach : rows) {
                        model.addRow(new Object[]{model.getRowCount() + 1, sach.maSach(), sach.tenSach(),
                                sach.soLuot(), sach.soBan()});
                    }
                    lblTrangThai.setText(rows.size() + " đầu sách đã được mượn · Toàn bộ lịch sử");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    lblTrangThai.setText("Đã dừng tải bảng xếp hạng.");
                } catch (ExecutionException e) {
                    System.err.println("Lỗi tải bảng xếp hạng sách: " + e.getCause().getMessage());
                    lblTrangThai.setText("Không tải được bảng xếp hạng. Kiểm tra kết nối MySQL rồi bấm Làm mới.");
                } finally {
                    btnLamMoi.setEnabled(true);
                }
            }
        }.execute();
    }
}
