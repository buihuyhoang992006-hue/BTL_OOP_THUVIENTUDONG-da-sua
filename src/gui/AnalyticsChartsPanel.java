package gui;

import dao.ThongKeDAO;
import org.kordamp.ikonli.feather.Feather;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Khu vực Phân tích Trực quan (Java 2D thuần, không dùng thư viện ngoài)
 * gồm Biểu đồ cột bo góc xu hướng Mượn/Trả 7 ngày và Biểu đồ vòng cơ cấu kho sách.
 */
public final class AnalyticsChartsPanel extends JPanel {

    private final RoundedBarChartCanvas barChartCanvas = new RoundedBarChartCanvas();
    private final DonutChartCanvas donutChartCanvas = new DonutChartCanvas();

    public AnalyticsChartsPanel() {
        setLayout(new GridBagLayout());
        setOpaque(false);
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setPreferredSize(new Dimension(0, 316));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 330));

        // ── Card 1 (Trái): Biểu đồ cột bo góc 7 ngày ─────────────────
        ThemeConfig.ModernCardPanel barCard = new ThemeConfig.ModernCardPanel(
                new BorderLayout(0, 12), new Insets(20, 24, 18, 24));

        JPanel barHeader = new JPanel(new BorderLayout(10, 0));
        barHeader.setOpaque(false);
        barHeader.add(
                ThemeConfig.createTitleBlock(
                        "XU HƯỚNG MƯỢN & TRẢ SÁCH 7 NGÀY GẦN NHẤT",
                        "Thống kê số bản sách phát sinh mượn mới và hoàn trả theo từng ngày",
                        Feather.BAR_CHART_2),
                BorderLayout.WEST);

        JPanel barLegend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 2));
        barLegend.setOpaque(false);
        barLegend.add(taoLegendItem("Mượn sách", new ThemeConfig.ThemeColor(0x2563EB, 0x3B82F6)));
        barLegend.add(taoLegendItem("Trả sách", new ThemeConfig.ThemeColor(0x16A34A, 0x22C55E)));
        barHeader.add(barLegend, BorderLayout.EAST);

        barCard.add(barHeader, BorderLayout.NORTH);
        barCard.add(barChartCanvas, BorderLayout.CENTER);

        // ── Card 2 (Phải): Biểu đồ vòng cơ cấu kho sách ─────────────
        ThemeConfig.ModernCardPanel donutCard = new ThemeConfig.ModernCardPanel(
                new BorderLayout(0, 12), new Insets(20, 24, 18, 24));

        JPanel donutHeader = new JPanel(new BorderLayout(8, 0));
        donutHeader.setOpaque(false);
        donutHeader.add(
                ThemeConfig.createTitleBlock(
                        "CƠ CẤU TRẠNG THÁI KHO SÁCH",
                        "Các bản đang trong kho hoặc được mượn",
                        Feather.PIE_CHART),
                BorderLayout.CENTER);

        donutCard.add(donutHeader, BorderLayout.NORTH);
        donutCard.add(donutChartCanvas, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        gbc.gridx = 0;
        gbc.weightx = 0.58;
        gbc.insets = new Insets(0, 0, 0, 20);
        add(barCard, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.42;
        gbc.insets = new Insets(0, 0, 0, 0);
        add(donutCard, gbc);
    }

    /** Cập nhật dữ liệu cho cả 2 biểu đồ và vẽ lại giao diện. */
    public void capNhatDuLieu(
            List<ThongKeDAO.NgayMuonTra> xuHuong7Ngay,
            ThongKeDAO.CoCauKhoSach coCauKho) {
        barChartCanvas.setDuLieu(xuHuong7Ngay);
        donutChartCanvas.setDuLieu(coCauKho);
    }

    private static JPanel taoLegendItem(String text, Color dotColor) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        item.setOpaque(false);

        JComponent dot = new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(10, 10);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeConfig.snap(dotColor));
                g2.fillOval(0, 0, 9, 9);
                g2.dispose();
            }
        };

        JLabel lbl = new JLabel(text);
        lbl.setFont(ThemeConfig.FONT_CAP);
        lbl.setForeground(ThemeConfig.TEXT_SECONDARY);
        item.add(dot);
        item.add(lbl);
        return item;
    }

    // =========================================================================
    // 1. BIỂU ĐỒ CỘT BO GÓC (ROUNDED BAR CHART) - JAVA 2D
    // =========================================================================
    private static final class RoundedBarChartCanvas extends JPanel {
        private static final Color MUON_TOP = new ThemeConfig.ThemeColor(0x60A5FA, 0x60A5FA);
        private static final Color MUON_BOT = new ThemeConfig.ThemeColor(0x2563EB, 0x3B82F6);
        private static final Color TRA_TOP  = new ThemeConfig.ThemeColor(0x4ADE80, 0x4ADE80);
        private static final Color TRA_BOT  = new ThemeConfig.ThemeColor(0x16A34A, 0x22C55E);

        private List<ThongKeDAO.NgayMuonTra> duLieu = List.of();
        private int hoveredIndex = -1;

        RoundedBarChartCanvas() {
            setOpaque(false);
            setPreferredSize(new Dimension(420, 200));

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int idx = timCotTheoToaDo(e.getX(), e.getY());
                    if (idx != hoveredIndex) {
                        hoveredIndex = idx;
                        setCursor(idx >= 0
                                ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                : Cursor.getDefaultCursor());
                        repaint();
                    }
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent e) {
                    if (hoveredIndex != -1) {
                        hoveredIndex = -1;
                        setCursor(Cursor.getDefaultCursor());
                        repaint();
                    }
                }
            });
        }

        void setDuLieu(List<ThongKeDAO.NgayMuonTra> data) {
            this.duLieu = data == null ? List.of() : new ArrayList<>(data);
            repaint();
        }

        private int timCotTheoToaDo(int mx, int my) {
            if (duLieu.isEmpty()) {
                return -1;
            }
            int plotX = 36;
            int plotY = 24;
            int plotW = getWidth() - plotX - 12;
            int plotH = getHeight() - plotY - 28;
            if (plotW <= 20 || plotH <= 20 || my < plotY || my > plotY + plotH) {
                return -1;
            }
            if (mx < plotX || mx > plotX + plotW) {
                return -1;
            }
            double slotW = (double) plotW / duLieu.size();
            int idx = (int) ((mx - plotX) / slotW);
            return (idx >= 0 && idx < duLieu.size()) ? idx : -1;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            int w = getWidth();
            int h = getHeight();
            int plotX = 36;
            int plotY = 24;
            int plotW = Math.max(40, w - plotX - 12);
            int plotH = Math.max(40, h - plotY - 28);

            int rawMax = 0;
            for (ThongKeDAO.NgayMuonTra item : duLieu) {
                rawMax = Math.max(rawMax, Math.max(item.soBanMuon(), item.soBanTra()));
            }
            int step = Math.max(1, (int) Math.ceil(Math.max(4, rawMax) / 4.0));
            int yMax = step * 4;

            // Lưới ngang nét đứt & nhãn trục Y
            Stroke defaultStroke = g2.getStroke();
            Stroke dashed = new BasicStroke(
                    1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 4f}, 0f);
            Font axisFont = ThemeConfig.fontPlain(12f);
            g2.setFont(axisFont);
            FontMetrics fmAxis = g2.getFontMetrics();

            for (int tick = 0; tick <= 4; tick++) {
                int val = tick * step;
                int y = plotY + plotH - (int) Math.round((val / (double) yMax) * plotH);
                if (tick == 0) {
                    g2.setStroke(defaultStroke);
                    g2.setColor(ThemeConfig.snap(ThemeConfig.BORDER));
                    g2.drawLine(plotX, y, plotX + plotW, y);
                } else {
                    g2.setStroke(dashed);
                    g2.setColor(ThemeConfig.snap(ThemeConfig.BORDER));
                    g2.drawLine(plotX, y, plotX + plotW, y);
                }

                String label = String.valueOf(val);
                int labelW = fmAxis.stringWidth(label);
                g2.setColor(ThemeConfig.snap(ThemeConfig.TEXT_MUTED));
                g2.drawString(label, plotX - 8 - labelW, y + fmAxis.getAscent() / 2 - 1);
            }
            g2.setStroke(defaultStroke);

            if (duLieu.isEmpty()) {
                veTrangThaiKhongDuLieu(g2, plotX, plotY, plotW, plotH, "Đang chờ dữ liệu thống kê 7 ngày...");
                g2.dispose();
                return;
            }

            int n = duLieu.size();
            double slotW = (double) plotW / n;
            int barW = Math.min(20, Math.max(7, (int) ((slotW - 14) / 2.2)));
            int gap = Math.max(3, Math.min(6, barW / 3));

            Font valFont = ThemeConfig.fontBold(11f);
            Font xFontNormal = ThemeConfig.fontPlain(12f);
            Font xFontBold = ThemeConfig.fontBold(12f);

            for (int i = 0; i < n; i++) {
                ThongKeDAO.NgayMuonTra item = duLieu.get(i);
                double slotX = plotX + i * slotW;
                boolean isHover = (i == hoveredIndex);

                if (isHover) {
                    g2.setColor(ThemeConfig.snap(ThemeConfig.SURFACE_HOVER));
                    g2.fill(new RoundRectangle2D.Double(slotX + 3, plotY, slotW - 6, plotH, 10, 10));
                }

                double groupW = barW * 2.0 + gap;
                double xMuon = slotX + (slotW - groupW) / 2.0;
                double xTra = xMuon + barW + gap;

                veCotBoGoc(g2, xMuon, plotY, plotH, barW, item.soBanMuon(), yMax,
                        MUON_TOP, MUON_BOT, isHover, valFont);
                veCotBoGoc(g2, xTra, plotY, plotH, barW, item.soBanTra(), yMax,
                        TRA_TOP, TRA_BOT, isHover, valFont);

                // Nhãn ngày trên trục X
                boolean laHomNay = (i == n - 1);
                g2.setFont(laHomNay || isHover ? xFontBold : xFontNormal);
                g2.setColor(isHover || laHomNay
                        ? ThemeConfig.snap(ThemeConfig.TEXT_PRIMARY)
                        : ThemeConfig.snap(ThemeConfig.TEXT_MUTED));
                String xText = item.nhanNgay();
                int xTextW = g2.getFontMetrics().stringWidth(xText);
                g2.drawString(xText,
                        (int) Math.round(slotX + (slotW - xTextW) / 2.0),
                        plotY + plotH + 17);
            }

            if (rawMax == 0) {
                veTrangThaiKhongDuLieu(g2, plotX, plotY, plotW, plotH,
                        "Chưa phát sinh lượt mượn / trả trong 7 ngày qua");
            }

            // Tooltip khi rê chuột vào cột ngày
            if (hoveredIndex >= 0 && hoveredIndex < n) {
                ThongKeDAO.NgayMuonTra item = duLieu.get(hoveredIndex);
                String tip = "Ngày " + item.nhanNgay()
                        + ": Mượn " + item.soBanMuon()
                        + " bản  |  Trả " + item.soBanTra() + " bản";
                veTooltipBieuDo(g2, tip, plotX, plotW, plotX + (hoveredIndex + 0.5) * slotW);
            }

            g2.dispose();
        }

        private void veCotBoGoc(
                Graphics2D g2,
                double x,
                int plotY,
                int plotH,
                int barW,
                int value,
                int yMax,
                Color topColor,
                Color bottomColor,
                boolean isHover,
                Font valFont) {
            if (value <= 0) {
                return;
            }
            double barH = Math.max(5.0, (value / (double) yMax) * plotH);
            double barY = plotY + plotH - barH;
            double radius = Math.min(6.0, Math.min(barW / 2.0, barH));

            Path2D.Double shape = new Path2D.Double();
            shape.moveTo(x, plotY + plotH);
            shape.lineTo(x, barY + radius);
            shape.quadTo(x, barY, x + radius, barY);
            shape.lineTo(x + barW - radius, barY);
            shape.quadTo(x + barW, barY, x + barW, barY + radius);
            shape.lineTo(x + barW, plotY + plotH);
            shape.closePath();

            Color cTop = ThemeConfig.snap(topColor);
            Color cBot = ThemeConfig.snap(bottomColor);
            if (isHover) {
                cTop = cTop.brighter();
            }
            g2.setPaint(new GradientPaint((float) x, (float) barY, cTop, (float) x, (float) (plotY + plotH), cBot));
            g2.fill(shape);

            // Số liệu trên đỉnh cột
            String valStr = String.valueOf(value);
            g2.setFont(valFont);
            FontMetrics fm = g2.getFontMetrics();
            int strW = fm.stringWidth(valStr);
            int labelY = Math.max(plotY - 4, (int) Math.round(barY - 5));
            g2.setColor(isHover
                    ? ThemeConfig.snap(ThemeConfig.TEXT_PRIMARY)
                    : ThemeConfig.snap(ThemeConfig.TEXT_SECONDARY));
            g2.drawString(valStr, (int) Math.round(x + (barW - strW) / 2.0), labelY);
        }

        private static void veTrangThaiKhongDuLieu(
                Graphics2D g2, int plotX, int plotY, int plotW, int plotH, String text) {
            g2.setFont(ThemeConfig.FONT_CAP);
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(text);
            int padX = 14;
            int boxW = tw + padX * 2;
            int boxH = 26;
            int bx = plotX + (plotW - boxW) / 2;
            int by = plotY + (plotH - boxH) / 2;

            g2.setColor(ThemeConfig.snap(ThemeConfig.SUBTLE_SURFACE));
            g2.fill(new RoundRectangle2D.Double(bx, by, boxW, boxH, 14, 14));
            g2.setColor(ThemeConfig.snap(ThemeConfig.BORDER));
            g2.draw(new RoundRectangle2D.Double(bx, by, boxW, boxH, 14, 14));
            g2.setColor(ThemeConfig.snap(ThemeConfig.TEXT_MUTED));
            g2.drawString(text, bx + padX, by + (boxH - fm.getHeight()) / 2 + fm.getAscent());
        }

        private static void veTooltipBieuDo(
                Graphics2D g2, String text, int plotX, int plotW, double centerX) {
            g2.setFont(ThemeConfig.fontBold(12f));
            FontMetrics fm = g2.getFontMetrics();
            int boxW = fm.stringWidth(text) + 18;
            int boxH = 24;
            int bx = (int) Math.round(centerX - boxW / 2.0);
            bx = Math.max(plotX, Math.min(plotX + plotW - boxW, bx));
            int by = 2;

            g2.setColor(ThemeConfig.snap(ThemeConfig.BG_SIDEBAR));
            g2.fill(new RoundRectangle2D.Double(bx, by, boxW, boxH, 10, 10));
            g2.setColor(ThemeConfig.snap(ThemeConfig.TEXT_ON_DARK));
            g2.drawString(text, bx + 9, by + (boxH - fm.getHeight()) / 2 + fm.getAscent());
        }
    }

    // =========================================================================
    // 2. BIỂU ĐỒ VÒNG (DONUT CHART) CƠ CẤU TRẠNG THÁI KHO SÁCH - JAVA 2D
    // =========================================================================
    private static final class DonutChartCanvas extends JPanel {
        private static final String[] NHAN = {
                "Đang ở quầy",
                "Mượn trong hạn",
                "Chưa trả quá hạn"
        };
        private static final Color[] MAU_CHINH = {
                new ThemeConfig.ThemeColor(0x16A34A, 0x22C55E),
                new ThemeConfig.ThemeColor(0x2563EB, 0x3B82F6),
                new ThemeConfig.ThemeColor(0xDC2626, 0xEF4444)
        };
        private static final Color[] MAU_NEN_PILL = {
                ThemeConfig.STATUS_OK_BG,
                ThemeConfig.PRIMARY_LIGHT,
                ThemeConfig.STATUS_ERROR_BG
        };
        private static final Color[] MAU_CHU_PILL = {
                ThemeConfig.STATUS_OK_FG,
                ThemeConfig.STATUS_INFO_FG,
                ThemeConfig.STATUS_ERROR_FG
        };

        private ThongKeDAO.CoCauKhoSach coCau = new ThongKeDAO.CoCauKhoSach(0, 0, 0);
        private int hoveredSlice = -1;

        private final double[] arcStarts = new double[3];
        private final double[] arcExtents = new double[3];
        private int donutCx;
        private int donutCy;
        private double donutRadius;
        private float donutStroke;

        private int legendX;
        private int legendW;
        private final int[] legendRowY = new int[3];
        private int legendRowH;

        DonutChartCanvas() {
            setOpaque(false);
            setPreferredSize(new Dimension(320, 200));

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int idx = timPhanKhucTheoToaDo(e.getX(), e.getY());
                    if (idx != hoveredSlice) {
                        hoveredSlice = idx;
                        setCursor(idx >= 0
                                ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                : Cursor.getDefaultCursor());
                        repaint();
                    }
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent e) {
                    if (hoveredSlice != -1) {
                        hoveredSlice = -1;
                        setCursor(Cursor.getDefaultCursor());
                        repaint();
                    }
                }
            });
        }

        void setDuLieu(ThongKeDAO.CoCauKhoSach data) {
            this.coCau = data == null ? new ThongKeDAO.CoCauKhoSach(0, 0, 0) : data;
            repaint();
        }

        private int timPhanKhucTheoToaDo(int mx, int my) {
            if (mx >= legendX && mx <= legendX + legendW && legendRowH > 0) {
                for (int i = 0; i < 3; i++) {
                    if (my >= legendRowY[i] && my <= legendRowY[i] + legendRowH) {
                        return i;
                    }
                }
            }

            if (coCau.tong() <= 0 || donutRadius <= 0) {
                return -1;
            }
            double dx = mx - donutCx;
            double dy = my - donutCy;
            double dist = Math.hypot(dx, dy);
            double inner = donutRadius - donutStroke / 2.0 - 6.0;
            double outer = donutRadius + donutStroke / 2.0 + 6.0;
            if (dist < inner || dist > outer) {
                return -1;
            }

            double deg = Math.toDegrees(Math.atan2(-dy, dx));
            if (deg < 0) {
                deg += 360.0;
            }
            for (int i = 0; i < 3; i++) {
                if (arcExtents[i] <= 0) {
                    continue;
                }
                double start = norm360(arcStarts[i]);
                double diff = norm360(start - deg);
                if (diff >= 0 && diff <= arcExtents[i]) {
                    return i;
                }
            }
            return -1;
        }

        private static double norm360(double deg) {
            double r = deg % 360.0;
            return r < 0 ? r + 360.0 : r;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            int w = getWidth();
            int h = getHeight();
            int[] values = { coCau.sanSang(), coCau.dangMuon(), coCau.quaHanChuaTra() };
            int total = coCau.tong();

            int donutAreaW = Math.max(136, Math.min(188, (int) (w * 0.44)));
            int dia = Math.max(84, Math.min(donutAreaW - 24, h - 24));
            donutStroke = Math.max(16f, Math.min(25f, dia * 0.22f));
            donutRadius = (dia - donutStroke) / 2.0;
            donutCx = donutAreaW / 2;
            donutCy = h / 2;

            // Vòng nền Track của Donut
            g2.setStroke(new BasicStroke(donutStroke, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            g2.setColor(ThemeConfig.snap(ThemeConfig.SUBTLE_SURFACE));
            g2.draw(new Arc2D.Double(
                    donutCx - donutRadius, donutCy - donutRadius,
                    donutRadius * 2.0, donutRadius * 2.0,
                    0, 360, Arc2D.OPEN));

            int nonZeroCount = 0;
            for (int v : values) {
                if (v > 0) {
                    nonZeroCount++;
                }
            }

            double gapDeg = nonZeroCount > 1 ? 2.4 : 0.0;
            double cursorAngle = 90.0;
            for (int i = 0; i < 3; i++) {
                if (values[i] <= 0 || total <= 0) {
                    arcStarts[i] = 0;
                    arcExtents[i] = 0;
                    continue;
                }
                double sweep = (values[i] / (double) total) * 360.0;
                double drawSweep = Math.max(1.8, sweep - gapDeg);
                double drawStart = cursorAngle - gapDeg / 2.0;

                arcStarts[i] = drawStart;
                arcExtents[i] = drawSweep;

                boolean isHover = (i == hoveredSlice);
                float stroke = isHover ? donutStroke + 4.5f : donutStroke;
                Color baseColor = ThemeConfig.snap(MAU_CHINH[i]);
                Color drawColor = (hoveredSlice >= 0 && !isHover)
                        ? new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 165)
                        : baseColor;

                g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
                g2.setColor(drawColor);
                g2.draw(new Arc2D.Double(
                        donutCx - donutRadius, donutCy - donutRadius,
                        donutRadius * 2.0, donutRadius * 2.0,
                        drawStart, -drawSweep, Arc2D.OPEN));

                cursorAngle -= sweep;
            }

            // Chữ ở tâm vòng Donut
            String centerMain;
            String centerSub;
            Color centerColor;
            if (hoveredSlice >= 0 && total > 0) {
                double pct = values[hoveredSlice] * 100.0 / total;
                centerMain = String.format("%.1f%%", pct);
                centerSub = NHAN[hoveredSlice];
                centerColor = ThemeConfig.snap(MAU_CHINH[hoveredSlice]);
            } else {
                centerMain = String.format("%,d", total);
                centerSub = "Tổng bản sách";
                centerColor = ThemeConfig.snap(ThemeConfig.TEXT_PRIMARY);
            }

            Font centerMainFont = ThemeConfig.fontBold(20f);
            Font centerSubFont = ThemeConfig.fontPlain(12f);
            g2.setFont(centerMainFont);
            FontMetrics fmMain = g2.getFontMetrics();
            g2.setColor(centerColor);
            int mainW = fmMain.stringWidth(centerMain);
            g2.drawString(centerMain, donutCx - mainW / 2, donutCy - 1);

            g2.setFont(centerSubFont);
            FontMetrics fmSub = g2.getFontMetrics();
            g2.setColor(ThemeConfig.snap(ThemeConfig.TEXT_MUTED));
            int subW = fmSub.stringWidth(centerSub);
            g2.drawString(centerSub, donutCx - subW / 2, donutCy + fmSub.getAscent() + 4);

            // Chú thích 3 trạng thái & tỷ lệ % bên phải
            legendX = donutAreaW + 6;
            legendW = Math.max(120, w - legendX - 4);
            int blockGap = 8;
            legendRowH = Math.max(38, Math.min(48, (h - blockGap * 2 - 12) / 3));
            int totalLegendH = legendRowH * 3 + blockGap * 2;
            int startY = Math.max(4, (h - totalLegendH) / 2);

            Font labelFont = ThemeConfig.fontBold(13f);
            Font countFont = ThemeConfig.fontPlain(12f);
            Font pctFont = ThemeConfig.fontBold(12f);

            for (int i = 0; i < 3; i++) {
                int ry = startY + i * (legendRowH + blockGap);
                legendRowY[i] = ry;
                boolean isHover = (i == hoveredSlice);

                g2.setColor(isHover
                        ? ThemeConfig.snap(ThemeConfig.SURFACE_HOVER)
                        : ThemeConfig.snap(ThemeConfig.SUBTLE_SURFACE));
                g2.fill(new RoundRectangle2D.Double(legendX, ry, legendW, legendRowH, 10, 10));

                g2.setStroke(new BasicStroke(1f));
                g2.setColor(isHover ? ThemeConfig.snap(MAU_CHINH[i]) : ThemeConfig.snap(ThemeConfig.BORDER));
                g2.draw(new RoundRectangle2D.Double(legendX, ry, legendW, legendRowH, 10, 10));

                // Dot màu
                int dotY = ry + legendRowH / 2 - 5;
                g2.setColor(ThemeConfig.snap(MAU_CHINH[i]));
                g2.fillOval(legendX + 10, dotY, 10, 10);

                // Tỷ lệ phần trăm dạng pill bên phải
                double pct = total > 0 ? (values[i] * 100.0 / total) : 0.0;
                String pctStr = String.format("%.1f%%", pct);
                g2.setFont(pctFont);
                FontMetrics fmPct = g2.getFontMetrics();
                int pillW = Math.max(48, fmPct.stringWidth(pctStr) + 14);
                int pillH = 22;
                int pillX = legendX + legendW - pillW - 8;
                int pillY = ry + (legendRowH - pillH) / 2;

                g2.setColor(ThemeConfig.snap(MAU_NEN_PILL[i]));
                g2.fill(new RoundRectangle2D.Double(pillX, pillY, pillW, pillH, 12, 12));
                g2.setColor(ThemeConfig.snap(MAU_CHU_PILL[i]));
                g2.drawString(pctStr,
                        pillX + (pillW - fmPct.stringWidth(pctStr)) / 2,
                        pillY + (pillH - fmPct.getHeight()) / 2 + fmPct.getAscent());

                // Tên trạng thái & số bản
                int textX = legendX + 27;
                g2.setFont(labelFont);
                FontMetrics fmLbl = g2.getFontMetrics();
                g2.setColor(ThemeConfig.snap(ThemeConfig.TEXT_PRIMARY));
                g2.drawString(NHAN[i], textX, ry + 17);

                g2.setFont(countFont);
                g2.setColor(ThemeConfig.snap(ThemeConfig.TEXT_MUTED));
                g2.drawString(String.format("%,d bản sách", values[i]), textX, ry + legendRowH - 8);
            }

            g2.dispose();
        }
    }
}
