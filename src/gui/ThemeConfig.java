package gui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.RowSorterListener;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.ColorModel;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * ThemeConfig — Design System Source of Truth
 * Áp dụng UI_STYLE_GUIDE.md cho Java Swing + FlatLaf (Light Mode & Carbon Slate Dark Mode).
 */
public final class ThemeConfig {

    static {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
    }

    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeConfig.class);
    private static final String PREF_DARK_MODE = "darkMode";

    private static volatile boolean darkMode = loadSavedDarkMode();
    private static volatile boolean themeInitialized = false;
    private static final List<Runnable> THEME_LISTENERS = new ArrayList<>();

    private ThemeConfig() {}   // utility class – không khởi tạo

    /** Đọc trạng thái Sáng/Tối đã lưu trong Preferences (mặc định: false = Light Mode). */
    public static boolean loadSavedDarkMode() {
        try {
            return PREFS.getBoolean(PREF_DARK_MODE, false);
        } catch (Exception e) {
            return false;
        }
    }

    private static void saveDarkModePreference(boolean dark) {
        try {
            PREFS.putBoolean(PREF_DARK_MODE, dark);
            PREFS.flush();
        } catch (Exception e) {
            System.err.println("Không lưu được trạng thái giao diện: " + e.getMessage());
        }
    }

    /**
     * Khởi tạo Look & Feel (FlatLightLaf / FlatDarkLaf) theo trạng thái đã lưu
     * và bật khử răng cưa chữ toàn cục trước khi tạo cửa sổ Swing.
     */
    public static void initTheme() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        darkMode = loadSavedDarkMode();
        if (darkMode) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        applyFlatLafProperties();
        themeInitialized = true;
    }

    /** Bảo đảm theme đã được nạp đúng theo cấu hình lưu trữ trước khi dựng UI frame. */
    public static void ensureInitialized() {
        if (!themeInitialized) {
            initTheme();
            return;
        }
        LookAndFeel laf = UIManager.getLookAndFeel();
        boolean lafIsDark = laf != null && laf.getClass().getSimpleName().contains("Dark");
        if (laf == null || lafIsDark != darkMode) {
            if (darkMode) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
            applyFlatLafProperties();
        }
    }

    /**
     * Màu ngữ nghĩa tự động chuyển đổi giữa Light Mode và Carbon Slate Dark Mode
     * khi repaint mà vẫn giữ nguyên hằng số public static final Color tương thích ngược.
     */
    public static final class ThemeColor extends Color {
        private final Color light;
        private final Color dark;

        public ThemeColor(int lightRgb, int darkRgb) {
            super(lightRgb);
            this.light = new Color(lightRgb);
            this.dark = new Color(darkRgb);
        }

        private Color active() {
            return darkMode ? dark : light;
        }

        public Color snapshot() {
            Color c = active();
            return new Color(c.getRGB(), true);
        }

        @Override public int getRed()   { return active().getRed(); }
        @Override public int getGreen() { return active().getGreen(); }
        @Override public int getBlue()  { return active().getBlue(); }
        @Override public int getAlpha() { return active().getAlpha(); }
        @Override public int getRGB()   { return active().getRGB(); }
        @Override public Color brighter() { return active().brighter(); }
        @Override public Color darker()   { return active().darker(); }
        @Override public int getTransparency() { return active().getTransparency(); }
        @Override public ColorSpace getColorSpace() { return active().getColorSpace(); }
        @Override public float[] getRGBComponents(float[] compArray) { return active().getRGBComponents(compArray); }
        @Override public float[] getRGBColorComponents(float[] compArray) { return active().getRGBColorComponents(compArray); }
        @Override public float[] getComponents(float[] compArray) { return active().getComponents(compArray); }
        @Override public float[] getColorComponents(float[] compArray) { return active().getColorComponents(compArray); }
        @Override public float[] getComponents(ColorSpace cspace, float[] compArray) { return active().getComponents(cspace, compArray); }
        @Override public float[] getColorComponents(ColorSpace cspace, float[] compArray) { return active().getColorComponents(cspace, compArray); }

        @Override
        public synchronized PaintContext createContext(ColorModel cm, Rectangle r, Rectangle2D r2d,
                AffineTransform xform, RenderingHints hints) {
            return active().createContext(cm, r, r2d, xform, hints);
        }

        @Override
        public int hashCode() {
            return active().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj instanceof ThemeColor tc) {
                return light.equals(tc.light) && dark.equals(tc.dark);
            }
            return obj instanceof Color c && active().equals(c);
        }
    }

    public static boolean isDarkMode() {
        return darkMode;
    }

    public static void addThemeChangeListener(Runnable listener) {
        if (listener != null && !THEME_LISTENERS.contains(listener)) {
            THEME_LISTENERS.add(listener);
        }
    }

    public static void toggleDarkMode(Window rootWindow) {
        setDarkMode(!darkMode, rootWindow);
    }

    public static void setDarkMode(boolean dark, Window rootWindow) {
        saveDarkModePreference(dark);
        if (darkMode == dark && themeInitialized) {
            return;
        }
        darkMode = dark;
        if (darkMode) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        applyFlatLafProperties();
        FlatLaf.updateUI();
        themeInitialized = true;

        if (rootWindow != null) {
            applyThemeToTree(rootWindow);
            rootWindow.repaint();
        } else {
            for (Window w : Window.getWindows()) {
                if (w.isDisplayable()) {
                    applyThemeToTree(w);
                    w.repaint();
                }
            }
        }

        for (Runnable listener : List.copyOf(THEME_LISTENERS)) {
            listener.run();
        }
    }

    public static void applyThemeToTree(Component root) {
        if (root == null) {
            return;
        }
        if (root instanceof JComponent jc) {
            Object cb = jc.getClientProperty("ThemeConfig.refresh");
            if (cb instanceof Runnable r) {
                r.run();
            }
            Border border = jc.getBorder();
            if (border instanceof LineBorder) {
                jc.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
            } else if (border instanceof CompoundBorder compBorder && compBorder.getOutsideBorder() instanceof LineBorder) {
                if (jc instanceof JPanel && jc.getBackground() instanceof ThemeColor) {
                    jc.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(BORDER, 1, true),
                            compBorder.getInsideBorder()));
                }
            }
        }
        if (root instanceof JTable table) {
            table.setBackground(snap(SURFACE_CARD));
            table.setForeground(snap(TEXT_PRIMARY));
            table.setGridColor(snap(BORDER));
            table.setSelectionBackground(snap(PRIMARY_LIGHT));
            table.setSelectionForeground(darkMode ? new Color(0xDBEAFE) : PRIMARY_PRESSED);
            JTableHeader header = table.getTableHeader();
            if (header != null) {
                header.setBackground(snap(SUBTLE_SURFACE));
                header.setForeground(snap(TEXT_SECONDARY));
                header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
                header.repaint();
            }
        } else if (root instanceof JScrollPane sp) {
            JViewport vp = sp.getViewport();
            if (vp != null) {
                Component view = vp.getView();
                if (view instanceof JTable) {
                    vp.setBackground(snap(SURFACE_CARD));
                } else if (sp.getHorizontalScrollBarPolicy() == ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER) {
                    Color vpBg = vp.getBackground();
                    ThemeColor sidebarToken = (ThemeColor) BG_SIDEBAR;
                    if (!Objects.equals(vpBg, sidebarToken.light)
                            && !Objects.equals(vpBg, sidebarToken.dark)) {
                        vp.setBackground(snap(BG_PAGE));
                    }
                }
            }
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyThemeToTree(child);
            }
        }
    }

    public static Color snap(Color color) {
        return color instanceof ThemeColor tc ? tc.snapshot() : color;
    }

    // =========================================================
    // 1. COLOR TOKENS (Light Mode <-> Carbon Slate Dark Mode)
    // =========================================================

    // — Nền & bề mặt —
    public static final Color BG_PAGE        = new ThemeColor(0xF8FAFC, 0x0F172A);
    public static final Color BG_SIDEBAR     = new ThemeColor(0x1E293B, 0x0B1120);
    public static final Color BG_TOPBAR      = new ThemeColor(0x182B49, 0x090E1A);
    public static final Color HEADER_BG      = new ThemeColor(0xFFFFFF, 0x111827);
    public static final Color SURFACE_CARD   = new ThemeColor(0xFFFFFF, 0x1E293B);
    public static final Color SURFACE_HOVER  = new ThemeColor(0xE9EFF8, 0x263449);
    public static final Color SUBTLE_SURFACE = new ThemeColor(0xF1F5F9, 0x243044);
    public static final Color BORDER         = new ThemeColor(0xE2E8F0, 0x334155);
    public static final Color BORDER_FOCUS   = new ThemeColor(0x2563EB, 0x3B82F6);
    public static final Color DIVIDER_DARK   = new ThemeColor(0x334155, 0x1E293B);

    // — Màu chữ —
    public static final Color TEXT_PRIMARY   = new ThemeColor(0x0F172A, 0xF1F5F9);
    public static final Color TEXT_SECONDARY = new ThemeColor(0x475569, 0xCBD5E1);
    public static final Color TEXT_MUTED     = new ThemeColor(0x64748B, 0x94A3B8);
    public static final Color TEXT_ON_DARK   = new ThemeColor(0xF1F5F9, 0xF8FAFC);
    public static final Color TEXT_LINK      = new ThemeColor(0x2563EB, 0x60A5FA);

    // — Brand Accent —
    public static final Color PRIMARY        = new Color(0x2563EB);
    public static final Color PRIMARY_HOVER  = new Color(0x1D4ED8);
    public static final Color PRIMARY_PRESSED= new Color(0x1E40AF);
    public static final Color PRIMARY_LIGHT  = new ThemeColor(0xDBEAFE, 0x1E3A8A);
    public static final Color PRIMARY_DARK   = new Color(0x1E3A8A);

    // — Trạng thái —
    public static final Color STATUS_OK_FG      = new ThemeColor(0x15803D, 0x4ADE80);
    public static final Color STATUS_OK_BG      = new ThemeColor(0xDCFCE7, 0x064E3B);
    public static final Color STATUS_WARN_FG    = new ThemeColor(0xB45309, 0xFBBF24);
    public static final Color STATUS_WARN_BG    = new ThemeColor(0xFEF3C7, 0x78350F);
    public static final Color STATUS_ERROR_FG   = new ThemeColor(0xB91C1C, 0xF87171);
    public static final Color STATUS_ERROR_BG   = new ThemeColor(0xFEE2E2, 0x7F1D1D);
    public static final Color STATUS_PENDING_FG = new ThemeColor(0x4338CA, 0xA5B4FC);
    public static final Color STATUS_PENDING_BG = new ThemeColor(0xEEF2FF, 0x312E81);
    public static final Color STATUS_INFO_FG    = new ThemeColor(0x0369A1, 0x38BDF8);
    public static final Color STATUS_INFO_BG    = new ThemeColor(0xE0F2FE, 0x0C4A6E);

    // — Màu Danger (xóa / vi phạm) —
    public static final Color DANGER             = new Color(0xDC2626);
    public static final Color DANGER_HOVER       = new Color(0xB91C1C);
    public static final Color DANGER_PRESSED     = new Color(0x991B1B);
    public static final Color DANGER_SOFT_BG     = new ThemeColor(0xFEF2F2, 0x3F1D24);
    public static final Color DANGER_SOFT_BORDER = new ThemeColor(0xFECACA, 0x7F1D1D);

    // — Màu Success (xác nhận / đúng hạn) —
    public static final Color SUCCESS        = new Color(0x16A34A);
    public static final Color SUCCESS_HOVER  = new Color(0x15803D);
    public static final Color SUCCESS_PRESSED= new Color(0x166534);

    // =========================================================
    // 2. SPACING & SIZING
    // =========================================================

    public static final int SPACE_1  = 4;
    public static final int SPACE_2  = 8;
    public static final int SPACE_3  = 12;
    public static final int SPACE_4  = 16;
    public static final int SPACE_5  = 20;
    public static final int SPACE_6  = 24;
    public static final int SPACE_8  = 32;
    public static final int SPACE_12 = 48;

    public static final int RADIUS_SM = 4;
    public static final int RADIUS_MD = 6;
    public static final int RADIUS_LG = 8;
    public static final int RADIUS_XL = 12;

    public static final int TOPBAR_HEIGHT    = 58;
    public static final int SIDEBAR_WIDTH    = 265;
    public static final int TABLE_ROW_HEIGHT = 40;
    public static final int INPUT_HEIGHT     = 40;
    public static final int BTN_HEIGHT       = 40;
    public static final int NAV_ITEM_HEIGHT  = 46;

    // =========================================================
    // 3. FONTS  (Chuỗi ưu tiên: Inter -> Roboto -> Segoe UI -> SansSerif)
    // =========================================================

    public static final String FONT_FAMILY = resolvePrimaryFontFamily();

    private static String resolvePrimaryFontFamily() {
        String[] candidates = { "Inter", "Roboto", "Segoe UI" };
        try {
            String[] families = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            for (String candidate : candidates) {
                for (String installed : families) {
                    if (candidate.equalsIgnoreCase(installed)) {
                        return installed;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return Font.SANS_SERIF;
    }

    public static Font font(int style, float size) {
        return new Font(FONT_FAMILY, style, Math.round(size)).deriveFont(style, size);
    }

    public static Font fontPlain(float size) {
        return font(Font.PLAIN, size);
    }

    public static Font fontBold(float size) {
        return font(Font.BOLD, size);
    }

    public static final Font FONT_H1      = fontBold(22f);   // Tiêu đề trang
    public static final Font FONT_H2      = fontBold(19f);   // Tiêu đề section
    public static final Font FONT_H3      = fontBold(17f);   // Tiêu đề Card
    public static final Font FONT_KPI     = fontBold(34f);   // Số liệu KPI trên thẻ
    public static final Font FONT_BODY    = fontPlain(15f);  // Chữ nội dung
    public static final Font FONT_INPUT   = fontPlain(15f);  // Ô nhập liệu
    public static final Font FONT_TABLE   = fontPlain(15f);  // Chữ ô bảng dữ liệu
    public static final Font FONT_TH      = fontBold(15f);   // Tiêu đề cột bảng (Header)
    public static final Font FONT_LABEL   = fontBold(14.5f); // Nhãn form
    public static final Font FONT_BTN     = fontBold(14.5f); // Chữ nút bấm
    public static final Font FONT_NAV     = fontBold(15f);   // Nút menu Sidebar đang chọn
    public static final Font FONT_SM      = fontPlain(13.5f);// Chữ mô tả phụ
    public static final Font FONT_SM_BOLD = fontBold(13.5f); // Nhãn đếm phụ
    public static final Font FONT_BADGE   = fontBold(13f);   // Badge / Pill trạng thái
    public static final Font FONT_CAP     = fontPlain(12.5f);// Chữ chú thích nhỏ

    // =========================================================
    // 4. FLATLAF UIManager PROPERTIES
    // =========================================================

    /**
     * Gọi sau {@code FlatLightLaf.setup()} trong {@code Main.main()}.
     * Cấu hình bo góc, màu sắc, font và spacing toàn cục theo UI_STYLE_GUIDE.
     */
    public static void applyFlatLafProperties() {

        // ── Bật anti-aliasing chữ ───────────────────────────────
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // ── Bo góc (arc) toàn cục ───────────────────────────────
        UIManager.put("Button.arc",          RADIUS_MD * 2);   // 12 → FlatLaf dùng diameter
        UIManager.put("Component.arc",       RADIUS_MD * 2);
        UIManager.put("TextComponent.arc",   RADIUS_MD * 2);
        UIManager.put("ScrollBar.arc",       RADIUS_SM * 2);
        UIManager.put("ProgressBar.arc",     RADIUS_SM * 2);

        // ── Scrollbar mảnh, hiện khi hover ──────────────────────
        UIManager.put("ScrollBar.width",               8);
        UIManager.put("ScrollBar.thumbInsets",         new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.thumb",               darkMode ? new Color(0x475569) : new Color(0xCBD5E1));
        UIManager.put("ScrollBar.hoverThumbColor",     darkMode ? new Color(0x64748B) : new Color(0x94A3B8));
        UIManager.put("ScrollBar.pressedThumbColor",   darkMode ? new Color(0x94A3B8) : new Color(0x64748B));
        UIManager.put("ScrollBar.showButtons",         false);
        UIManager.put("ScrollBar.trackInsets",         new Insets(2, 4, 2, 4));

        // ── Màu nền trang & component ───────────────────────────
        UIManager.put("Panel.background",           snap(BG_PAGE));
        UIManager.put("ScrollPane.background",      snap(BG_PAGE));
        UIManager.put("Viewport.background",        snap(BG_PAGE));
        UIManager.put("TabbedPane.background",      snap(BG_PAGE));
        UIManager.put("SplitPane.background",       snap(BG_PAGE));
        UIManager.put("Label.foreground",           snap(TEXT_PRIMARY));
        UIManager.put("CheckBox.foreground",        snap(TEXT_PRIMARY));
        UIManager.put("RadioButton.foreground",     snap(TEXT_PRIMARY));
        UIManager.put("OptionPane.background",      snap(SURFACE_CARD));
        UIManager.put("OptionPane.messageForeground", snap(TEXT_PRIMARY));

        // ── Button (FlatLaf keys) ────────────────────────────────
        UIManager.put("Button.background",           PRIMARY);
        UIManager.put("Button.foreground",           Color.WHITE);
        UIManager.put("Button.hoverBackground",      PRIMARY_HOVER);
        UIManager.put("Button.pressedBackground",    PRIMARY_PRESSED);
        UIManager.put("Button.disabledBackground",   snap(SUBTLE_SURFACE));
        UIManager.put("Button.disabledForeground",   snap(TEXT_MUTED));
        UIManager.put("Button.font",                 FONT_BTN);
        UIManager.put("Button.focusedBorderColor",   new Color(0x93C5FD));
        UIManager.put("Button.innerFocusWidth",      1f);
        UIManager.put("Button.borderWidth",          0);

        // ── TextField / PasswordField / ComboBox / TextArea ──────
        UIManager.put("TextField.background",        snap(SURFACE_CARD));
        UIManager.put("TextField.foreground",        snap(TEXT_PRIMARY));
        UIManager.put("TextField.placeholderForeground", snap(TEXT_MUTED));
        UIManager.put("TextField.focusedBorderColor", snap(BORDER_FOCUS));
        UIManager.put("TextField.borderColor",       snap(BORDER));
        UIManager.put("TextField.caretForeground",   PRIMARY);
        UIManager.put("TextArea.background",         snap(SURFACE_CARD));
        UIManager.put("TextArea.foreground",         snap(TEXT_PRIMARY));
        UIManager.put("PasswordField.background",    snap(SURFACE_CARD));
        UIManager.put("PasswordField.foreground",    snap(TEXT_PRIMARY));
        UIManager.put("PasswordField.focusedBorderColor", snap(BORDER_FOCUS));
        UIManager.put("PasswordField.borderColor",   snap(BORDER));
        UIManager.put("ComboBox.background",         snap(SURFACE_CARD));
        UIManager.put("ComboBox.foreground",         snap(TEXT_PRIMARY));
        UIManager.put("ComboBox.borderColor",        snap(BORDER));
        UIManager.put("ComboBox.focusedBorderColor", snap(BORDER_FOCUS));
        UIManager.put("Spinner.background",          snap(SURFACE_CARD));
        UIManager.put("Spinner.foreground",          snap(TEXT_PRIMARY));

        // ── Table ────────────────────────────────────────────────
        UIManager.put("Table.background",            snap(SURFACE_CARD));
        UIManager.put("Table.foreground",            snap(TEXT_PRIMARY));
        UIManager.put("Table.alternateRowColor",     snap(BG_PAGE));
        UIManager.put("Table.selectionBackground",   snap(PRIMARY_LIGHT));
        UIManager.put("Table.selectionForeground",   darkMode ? new Color(0xDBEAFE) : PRIMARY_PRESSED);
        UIManager.put("Table.gridColor",             snap(BORDER));
        UIManager.put("Table.showHorizontalLines",   true);
        UIManager.put("Table.showVerticalLines",     false);
        UIManager.put("Table.intercellSpacing",      new Dimension(0, 1));
        UIManager.put("Table.font",                  new FontUIResource(FONT_TABLE));
        UIManager.put("TableHeader.background",      snap(SUBTLE_SURFACE));
        UIManager.put("TableHeader.foreground",      snap(TEXT_SECONDARY));
        UIManager.put("TableHeader.separatorColor",  snap(BORDER));
        UIManager.put("TableHeader.font",            new FontUIResource(FONT_TH));

        // ── TabbedPane ───────────────────────────────────────────
        UIManager.put("TabbedPane.selectedBackground",  snap(SURFACE_CARD));
        UIManager.put("TabbedPane.foreground",          snap(TEXT_SECONDARY));
        UIManager.put("TabbedPane.selectedForeground",  snap(TEXT_LINK));
        UIManager.put("TabbedPane.underlineColor",      PRIMARY);
        UIManager.put("TabbedPane.inactiveUnderlineColor", new Color(0x00000000, true));
        UIManager.put("TabbedPane.tabAreaBackground",   snap(SUBTLE_SURFACE));
        UIManager.put("TabbedPane.hoverColor",          snap(SURFACE_HOVER));
        UIManager.put("TabbedPane.font",                new FontUIResource(FONT_BTN));
        UIManager.put("TabbedPane.tabInsets",           new Insets(10, 16, 10, 16));

        // ── ToolTip ──────────────────────────────────────────────
        UIManager.put("ToolTip.background",          snap(BG_SIDEBAR));
        UIManager.put("ToolTip.foreground",          snap(TEXT_ON_DARK));
        UIManager.put("ToolTip.font",                new FontUIResource(FONT_SM));

        // ── Global font (FlatLaf defaultFont + toàn bộ key .font) ──
        FontUIResource defaultFont = new FontUIResource(FONT_BODY);
        FontUIResource inputFont = new FontUIResource(FONT_INPUT);
        FontUIResource btnFont = new FontUIResource(FONT_BTN);
        FontUIResource labelFont = new FontUIResource(FONT_LABEL);
        FontUIResource tableFont = new FontUIResource(FONT_TABLE);
        FontUIResource thFont = new FontUIResource(FONT_TH);
        FontUIResource smFont = new FontUIResource(FONT_SM);

        UIManager.put("defaultFont", defaultFont);
        for (Object key : UIManager.getLookAndFeelDefaults().keySet().toArray()) {
            String k = key.toString();
            if (k.endsWith(".font")) {
                UIManager.put(key, defaultFont);
            }
        }
        UIManager.put("Button.font",             btnFont);
        UIManager.put("ToggleButton.font",       btnFont);
        UIManager.put("TabbedPane.font",         btnFont);
        UIManager.put("Table.font",              tableFont);
        UIManager.put("TableHeader.font",        thFont);
        UIManager.put("TextField.font",          inputFont);
        UIManager.put("FormattedTextField.font", inputFont);
        UIManager.put("PasswordField.font",      inputFont);
        UIManager.put("TextArea.font",           inputFont);
        UIManager.put("ComboBox.font",           inputFont);
        UIManager.put("Spinner.font",            inputFont);
        UIManager.put("Label.font",              defaultFont);
        UIManager.put("CheckBox.font",           defaultFont);
        UIManager.put("RadioButton.font",        defaultFont);
        UIManager.put("TitledBorder.font",       labelFont);
        UIManager.put("OptionPane.messageFont",  defaultFont);
        UIManager.put("OptionPane.buttonFont",   btnFont);
        UIManager.put("ToolTip.font",            smFont);
    }

    // =========================================================
    // 5. BUTTON FACTORIES
    // =========================================================

    /**
     * Nút Primary (xanh dương) — hành động chính.
     * Ví dụ: "Thêm mới", "Lưu", "Xác nhận"
     */
    public static JButton createPrimaryButton(String text) {
        return buildButton(text, PRIMARY, PRIMARY_HOVER, PRIMARY_PRESSED, Color.WHITE);
    }

    public static JButton createPrimaryButton(String text, Ikon iconCode) {
        JButton btn = createPrimaryButton(text);
        if (iconCode != null) {
            btn.setIcon(getIcon(iconCode, 16, Color.WHITE));
            btn.setDisabledIcon(getIcon(iconCode, 16, TEXT_MUTED));
        }
        return btn;
    }

    /**
     * Nút Success (xanh lá) — xác nhận tích cực.
     * Ví dụ: "Cho mượn", "Xác nhận trả", "Kích hoạt"
     */
    public static JButton createSuccessButton(String text) {
        return buildButton(text, SUCCESS, SUCCESS_HOVER, SUCCESS_PRESSED, Color.WHITE);
    }

    public static JButton createSuccessButton(String text, Ikon iconCode) {
        JButton btn = createSuccessButton(text);
        if (iconCode != null) {
            btn.setIcon(getIcon(iconCode, 16, Color.WHITE));
            btn.setDisabledIcon(getIcon(iconCode, 16, TEXT_MUTED));
        }
        return btn;
    }

    /**
     * Nút Danger (đỏ) — hành động nguy hiểm.
     * Ví dụ: "Xóa", "Hủy phiếu", "Xử lý vi phạm"
     */
    public static JButton createDangerButton(String text) {
        return buildButton(text, DANGER, DANGER_HOVER, DANGER_PRESSED, Color.WHITE);
    }

    public static JButton createDangerButton(String text, Ikon iconCode) {
        JButton btn = createDangerButton(text);
        if (iconCode != null) {
            btn.setIcon(getIcon(iconCode, 16, Color.WHITE));
            btn.setDisabledIcon(getIcon(iconCode, 16, TEXT_MUTED));
        }
        return btn;
    }

    /**
     * Nút Secondary (trắng + viền) — hành động phụ.
     * Ví dụ: "Hủy", "Đóng", "Làm mới"
     */
    public static JButton createSecondaryButton(String text) {
        JButton btn = buildButton(text, SURFACE_CARD, SURFACE_HOVER, BORDER, TEXT_PRIMARY);
        btn.setIconTextGap(8);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(9, 18, 9, 18)
        ));
        return btn;
    }

    public static JButton createSecondaryButton(String text, Ikon iconCode) {
        JButton btn = createSecondaryButton(text);
        if (iconCode != null) {
            btn.putClientProperty("ThemeConfig.refresh", (Runnable) () -> {
                btn.setIcon(getIcon(iconCode, 16, TEXT_PRIMARY));
                btn.setDisabledIcon(getIcon(iconCode, 16, TEXT_MUTED));
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER, 1, true),
                        BorderFactory.createEmptyBorder(9, 18, 9, 18)));
            });
            ((Runnable) btn.getClientProperty("ThemeConfig.refresh")).run();
        }
        return btn;
    }

    /**
     * Chuẩn hóa nút hành động FlatLaf với padding thoáng 9x18 (đạt chiều cao 40px),
     * iconTextGap 8, hỗ trợ tự thích ứng Light/Dark Mode và trạng thái disabled rõ ràng.
     */
    public static JButton styleActionButton(
            JButton btn,
            Ikon iconCode,
            Color bg,
            Color fg,
            Color borderColor) {
        Color resolvedBg = resolveThemeBg(bg);
        Color resolvedBorder = resolveThemeBorder(bg, borderColor);
        Color resolvedFg = resolveThemeFg(fg);

        btn.setFont(FONT_BTN);
        btn.setIconTextGap(8);
        btn.setFocusPainted(false);

        Runnable applyState = () -> {
            if (iconCode != null) {
                btn.setIcon(getIcon(iconCode, 16, snap(resolvedFg)));
                btn.setDisabledIcon(getIcon(iconCode, 16, snap(TEXT_MUTED)));
            }
            if (btn.isEnabled()) {
                btn.setBackground(snap(resolvedBg));
                btn.setForeground(snap(resolvedFg));
                btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(resolvedBorder, 1, true),
                        BorderFactory.createEmptyBorder(9, 18, 9, 18)));
            } else {
                btn.setBackground(snap(SUBTLE_SURFACE));
                btn.setForeground(snap(TEXT_MUTED));
                btn.setCursor(Cursor.getDefaultCursor());
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER, 1, true),
                        BorderFactory.createEmptyBorder(9, 18, 9, 18)));
            }
        };

        applyState.run();
        btn.putClientProperty("ThemeConfig.refresh", applyState);
        btn.addPropertyChangeListener("enabled", evt -> applyState.run());
        return btn;
    }

    private static Color resolveThemeBg(Color bg) {
        if (bg == null || bg.equals(Color.WHITE)) {
            return SURFACE_CARD;
        }
        int rgb = bg.getRGB() & 0xFFFFFF;
        if (rgb == 0xFEF2F2) {
            return DANGER_SOFT_BG;
        }
        return bg;
    }

    private static Color resolveThemeBorder(Color bg, Color borderColor) {
        if (borderColor == null || borderColor.equals(BORDER)) {
            return BORDER;
        }
        int rgb = borderColor.getRGB() & 0xFFFFFF;
        if (rgb == 0xFECACA) {
            return DANGER_SOFT_BORDER;
        }
        return borderColor;
    }

    private static Color resolveThemeFg(Color fg) {
        if (fg != null && fg.equals(SUCCESS_HOVER)) {
            return STATUS_OK_FG;
        }
        return fg != null ? fg : TEXT_PRIMARY;
    }

    /** Internal helper — tạo rounded button với màu tuỳ chỉnh. */
    private static JButton buildButton(String text,
                                       Color bg, Color hoverBg, Color pressedBg,
                                       Color fg) {
        JButton btn = new JButton(text) {
            private Color currentBg = bg;

            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) {
                        if (isEnabled()) { currentBg = hoverBg; repaint(); }
                    }
                    @Override public void mouseExited(MouseEvent e) {
                        if (isEnabled()) { currentBg = bg; repaint(); }
                    }
                    @Override public void mousePressed(MouseEvent e) {
                        if (isEnabled()) { currentBg = pressedBg; repaint(); }
                    }
                    @Override public void mouseReleased(MouseEvent e) {
                        if (isEnabled()) { currentBg = hoverBg; repaint(); }
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isEnabled() ? snap(currentBg) : snap(SUBTLE_SURFACE));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(),
                                 RADIUS_MD * 2, RADIUS_MD * 2);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setFont(FONT_BTN);
        btn.setForeground(fg);
        btn.setBackground(bg);          // FlatLaf cần giá trị này
        btn.setOpaque(false);           // paintComponent tự vẽ nền
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setIconTextGap(8);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        return btn;
    }

    // =========================================================
    // 6. INPUT FIELD FACTORIES
    // =========================================================

    /**
     * JTextField với placeholder mờ và focus border xanh.
     */
    public static JTextField createTextField(String placeholder, int columns) {
        JTextField field = new JTextField(columns) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SURFACE_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(),
                                 RADIUS_MD * 2, RADIUS_MD * 2);
                g2.dispose();
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()
                        && placeholder != null && !placeholder.isEmpty()) {
                    Graphics2D g3 = (Graphics2D) g.create();
                    g3.setFont(getFont());
                    g3.setColor(TEXT_MUTED);
                    Insets ins = getInsets();
                    FontMetrics fm = g3.getFontMetrics();
                    int y = ins.top + (getHeight() - ins.top - ins.bottom
                                       - fm.getHeight()) / 2 + fm.getAscent();
                    g3.drawString(placeholder, ins.left, y);
                    g3.dispose();
                }
            }
        };

        field.setFont(FONT_INPUT);
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(SURFACE_CARD);
        field.setOpaque(false);
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, INPUT_HEIGHT));

        Color[] borderRef = { BORDER };
        int[]   widthRef  = { 1 };

        Runnable refreshBorder = () -> field.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderRef[0], widthRef[0], true),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)
            )
        );
        refreshBorder.run();
        field.putClientProperty("ThemeConfig.refresh", (Runnable) () -> {
            field.setForeground(snap(TEXT_PRIMARY));
            field.setBackground(snap(SURFACE_CARD));
            refreshBorder.run();
        });

        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                borderRef[0] = BORDER_FOCUS; widthRef[0] = 2;
                refreshBorder.run(); field.repaint();
            }
            @Override public void focusLost(FocusEvent e) {
                borderRef[0] = BORDER; widthRef[0] = 1;
                refreshBorder.run(); field.repaint();
            }
        });

        return field;
    }

    /** Shorthand: TextField không cần chỉ định columns. */
    public static JTextField createTextField(String placeholder) {
        return createTextField(placeholder, 0);
    }

    /**
     * Bọc JTextField thành thanh Search hiện đại bo góc 10px:
     * có icon kính lúp Feather.SEARCH bên trái (đổi màu theo focus)
     * và nút X (Feather.X) xóa nhanh từ khóa bên phải khi có nội dung.
     */
    public static JPanel createSearchBar(JTextField field, String placeholder, int preferredWidth) {
        int barW = Math.max(170, preferredWidth);

        field.setFont(FONT_INPUT);
        field.setForeground(snap(TEXT_PRIMARY));
        field.setCaretColor(PRIMARY);
        field.setOpaque(false);
        field.setPreferredSize(null);
        field.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 4));
        if (placeholder != null && !placeholder.isEmpty()) {
            field.putClientProperty("JTextField.placeholderText", placeholder);
        }
        field.putClientProperty("JTextField.showClearButton", Boolean.FALSE);

        JLabel iconSearch = new JLabel();
        iconSearch.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        iconSearch.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));

        JLabel clearTrigger = new JLabel();
        clearTrigger.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearTrigger.setToolTipText("Xóa nhanh từ khóa (Esc)");
        clearTrigger.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        clearTrigger.setVisible(!field.getText().isEmpty());

        boolean[] clearHovered = { false };

        JPanel container = new JPanel(new BorderLayout(6, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                boolean focused = field.isFocusOwner();

                if (focused) {
                    Color focusClr = snap(BORDER_FOCUS);
                    g2.setColor(new Color(focusClr.getRed(), focusClr.getGreen(), focusClr.getBlue(), 26));
                    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 22f, 22f));
                }

                g2.setColor(snap(SURFACE_CARD));
                g2.fill(new RoundRectangle2D.Float(1f, 1f, w - 2f, h - 2f, 20f, 20f));

                g2.setColor(focused ? snap(BORDER_FOCUS) : snap(BORDER));
                g2.setStroke(new BasicStroke(focused ? 1.6f : 1f));
                g2.draw(new RoundRectangle2D.Float(1f, 1f, w - 2.5f, h - 2.5f, 20f, 20f));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        container.setOpaque(false);
        container.setPreferredSize(new Dimension(barW, INPUT_HEIGHT));
        container.setMinimumSize(new Dimension(Math.min(barW, 160), INPUT_HEIGHT));
        container.setBorder(BorderFactory.createEmptyBorder(1, 12, 1, 8));

        Runnable refreshVisuals = () -> {
            boolean focused = field.isFocusOwner();
            field.setForeground(snap(TEXT_PRIMARY));
            field.setCaretColor(PRIMARY);
            iconSearch.setIcon(getIcon(Feather.SEARCH, 16, focused ? snap(BORDER_FOCUS) : snap(TEXT_MUTED)));
            clearTrigger.setIcon(getIcon(Feather.X, 14, clearHovered[0] ? DANGER : snap(TEXT_MUTED)));
            container.repaint();
        };

        Runnable syncClearState = () -> {
            boolean hasText = !field.getText().isEmpty();
            if (clearTrigger.isVisible() != hasText) {
                clearTrigger.setVisible(hasText);
                container.revalidate();
            }
            container.repaint();
        };

        refreshVisuals.run();
        container.putClientProperty("ThemeConfig.refresh", refreshVisuals);

        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { refreshVisuals.run(); }
            @Override public void focusLost(FocusEvent e)   { refreshVisuals.run(); }
        });
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { syncClearState.run(); }
            @Override public void removeUpdate(DocumentEvent e) { syncClearState.run(); }
            @Override public void changedUpdate(DocumentEvent e) { syncClearState.run(); }
        });

        // Phím ESC xóa nhanh từ khóa ngay trong ô tìm kiếm
        field.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "search.clear");
        field.getActionMap().put("search.clear", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!field.getText().isEmpty()) {
                    field.setText("");
                }
            }
        });

        iconSearch.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                field.requestFocusInWindow();
            }
        });

        clearTrigger.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                clearHovered[0] = true;
                refreshVisuals.run();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                clearHovered[0] = false;
                refreshVisuals.run();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                field.setText("");
                field.requestFocusInWindow();
            }
        });

        container.add(iconSearch, BorderLayout.WEST);
        container.add(field, BorderLayout.CENTER);
        container.add(clearTrigger, BorderLayout.EAST);
        return container;
    }

    /**
     * JPasswordField với focus border xanh, cùng style với createTextField.
     */
    public static JPasswordField createPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SURFACE_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(),
                                 RADIUS_MD * 2, RADIUS_MD * 2);
                g2.dispose();
                super.paintComponent(g);
                if (getPassword().length == 0 && !isFocusOwner()
                        && placeholder != null && !placeholder.isEmpty()) {
                    Graphics2D g3 = (Graphics2D) g.create();
                    g3.setFont(getFont());
                    g3.setColor(TEXT_MUTED);
                    Insets ins = getInsets();
                    FontMetrics fm = g3.getFontMetrics();
                    int y = ins.top + (getHeight() - ins.top - ins.bottom
                                       - fm.getHeight()) / 2 + fm.getAscent();
                    g3.drawString(placeholder, ins.left, y);
                    g3.dispose();
                }
            }
        };

        field.setFont(FONT_INPUT);
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(SURFACE_CARD);
        field.setOpaque(false);
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, INPUT_HEIGHT));

        Color[] borderRef = { BORDER };
        int[]   widthRef  = { 1 };
        Runnable refreshBorder = () -> field.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderRef[0], widthRef[0], true),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)
            )
        );
        refreshBorder.run();
        field.putClientProperty("ThemeConfig.refresh", (Runnable) () -> {
            field.setForeground(snap(TEXT_PRIMARY));
            field.setBackground(snap(SURFACE_CARD));
            refreshBorder.run();
        });

        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                borderRef[0] = BORDER_FOCUS; widthRef[0] = 2;
                refreshBorder.run(); field.repaint();
            }
            @Override public void focusLost(FocusEvent e) {
                borderRef[0] = BORDER; widthRef[0] = 1;
                refreshBorder.run(); field.repaint();
            }
        });

        return field;
    }

    // =========================================================
    // 7. TABLE STYLING
    // =========================================================

    public static void styleTable(JTable table) {
        table.setRowHeight(TABLE_ROW_HEIGHT);
        table.setFont(FONT_TABLE);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(snap(BORDER));
        table.setIntercellSpacing(new Dimension(0, 1));

        table.setBackground(snap(SURFACE_CARD));
        table.setForeground(snap(TEXT_PRIMARY));
        table.setSelectionBackground(snap(PRIMARY_LIGHT));
        table.setSelectionForeground(darkMode ? new Color(0xDBEAFE) : PRIMARY_PRESSED);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setBackground(snap(SUBTLE_SURFACE));
        header.setForeground(snap(TEXT_SECONDARY));
        header.setFont(FONT_TH);
        header.setBorder(BorderFactory.createMatteBorder(
                0, 0, 1, 0, BORDER));
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 42));

        StripedHoverRenderer renderer = new StripedHoverRenderer(table);
        TableColumnModel cm = table.getColumnModel();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            cm.getColumn(i).setCellRenderer(renderer);
        }

        table.putClientProperty("ThemeConfig.hoverRow", -1);
        if (!Boolean.TRUE.equals(table.getClientProperty("ThemeConfig.hoverListenerInstalled"))) {
            table.putClientProperty("ThemeConfig.hoverListenerInstalled", Boolean.TRUE);
            table.addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int row = table.rowAtPoint(e.getPoint());
                    Object current = table.getClientProperty("ThemeConfig.hoverRow");
                    int prev = current instanceof Integer n ? n : -1;
                    if (row != prev) {
                        renderer.hoverRow = row;
                        table.putClientProperty("ThemeConfig.hoverRow", row);
                        table.repaint();
                    }
                }
            });
            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent e) {
                    Object current = table.getClientProperty("ThemeConfig.hoverRow");
                    int prev = current instanceof Integer n ? n : -1;
                    if (prev != -1) {
                        renderer.hoverRow = -1;
                        table.putClientProperty("ThemeConfig.hoverRow", -1);
                        table.repaint();
                    }
                }
            });
        }
    }

    /**
     * Nâng cấp JTable và JScrollPane lên chuẩn SaaS Data Grid:
     * 1) Universal Row Hover Highlight hoạt động trên cả các cột custom renderer/badge
     * 2) Empty State chuyên nghiệp giữa vùng bảng khi chưa có dữ liệu hoặc tìm kiếm không khớp
     * 3) Smart Column Sizing (AUTO_RESIZE) tránh bóp nghẹt chữ hoặc thừa khoảng trống
     */
    public static void installSaaSDataGrid(
            JScrollPane scrollPane,
            JTable table,
            Ikon emptyIcon,
            String emptyTitle,
            String emptySubtitle,
            int... preferredColWidths) {
        if (scrollPane == null || table == null) {
            return;
        }
        if (table.getRowHeight() < TABLE_ROW_HEIGHT) {
            table.setRowHeight(TABLE_ROW_HEIGHT);
        }
        table.setFont(FONT_TABLE);

        // 1. Gắn Universal Hover Delegator cho tất cả cột (kể cả cột dùng custom renderer)
        TableColumnModel cm = table.getColumnModel();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            TableColumn col = cm.getColumn(i);
            TableCellRenderer current = col.getCellRenderer();
            if (!(current instanceof HoverDelegatingRenderer)) {
                col.setCellRenderer(new HoverDelegatingRenderer(current));
            }
        }

        // 2. Smart Column Sizing: cân đối minWidth/preferredWidth & tự động bật/tắt AUTO_RESIZE
        int count = cm.getColumnCount();
        final int[] prefs = new int[count];
        int sumReadable = 0;
        for (int i = 0; i < count; i++) {
            TableColumn col = cm.getColumn(i);
            int pref = (preferredColWidths != null && i < preferredColWidths.length && preferredColWidths[i] > 0)
                    ? preferredColWidths[i]
                    : Math.max(85, col.getPreferredWidth());
            prefs[i] = pref;
            int minW = Math.max(46, Math.min(pref, (int) Math.round(pref * 0.68)));
            col.setMinWidth(minW);
            col.setPreferredWidth(pref);
            if (pref <= 65) {
                col.setMaxWidth(Math.max(95, pref + 28));
            }
            sumReadable += Math.max(minW, (int) Math.round(pref * 0.80));
        }
        final int totalReadableWidth = sumReadable;

        // 3. Empty State Viewport thay thế viewport mặc định của JScrollPane
        EmptyStateViewport viewport = new EmptyStateViewport(table, emptyIcon, emptyTitle, emptySubtitle);
        viewport.setView(table);
        viewport.setBackground(snap(SURFACE_CARD));
        scrollPane.setViewport(viewport);
        if (table.getTableHeader() != null) {
            scrollPane.setColumnHeaderView(table.getTableHeader());
        }
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER, 1));

        Runnable updateResizeMode = () -> {
            int vpWidth = viewport.getWidth();
            if (vpWidth <= 0 || count == 0) {
                return;
            }
            if (vpWidth >= totalReadableWidth) {
                if (table.getAutoResizeMode() != JTable.AUTO_RESIZE_ALL_COLUMNS) {
                    table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
                }
            } else {
                if (table.getAutoResizeMode() != JTable.AUTO_RESIZE_OFF) {
                    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                    for (int i = 0; i < cm.getColumnCount() && i < prefs.length; i++) {
                        cm.getColumn(i).setPreferredWidth(prefs[i]);
                    }
                }
            }
        };

        viewport.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateResizeMode.run();
            }
        });
        SwingUtilities.invokeLater(updateResizeMode);

        // Repaint Empty State khi dữ liệu bảng hoặc bộ lọc RowSorter thay đổi
        TableModelListener modelListener = e -> viewport.repaint();
        table.getModel().addTableModelListener(modelListener);

        RowSorterListener sorterListener = e -> viewport.repaint();
        if (table.getRowSorter() != null) {
            table.getRowSorter().addRowSorterListener(sorterListener);
        }
        table.addPropertyChangeListener("rowSorter", evt -> {
            if (evt.getOldValue() instanceof RowSorter<?> oldSorter) {
                oldSorter.removeRowSorterListener(sorterListener);
            }
            if (evt.getNewValue() instanceof RowSorter<?> newSorter) {
                newSorter.addRowSorterListener(sorterListener);
            }
            viewport.repaint();
        });

        enableNestedScrollForwarding(scrollPane);
    }

    /**
     * Khi bảng con nằm trong một trang cuộn (Page JScrollPane) và không cần cuộn
     * hoặc đã chạm biên trên/dưới, tự động chuyển tiếp lăn chuột ra thanh cuộn trang.
     */
    public static void enableNestedScrollForwarding(JScrollPane innerScroll) {
        if (innerScroll == null
                || Boolean.TRUE.equals(innerScroll.getClientProperty("ThemeConfig.wheelForwardingInstalled"))) {
            return;
        }
        innerScroll.putClientProperty("ThemeConfig.wheelForwardingInstalled", Boolean.TRUE);
        innerScroll.addMouseWheelListener((MouseWheelEvent e) -> {
            JScrollPane outer = (JScrollPane) SwingUtilities.getAncestorOfClass(
                    JScrollPane.class, innerScroll.getParent());
            if (outer == null) {
                return;
            }
            JScrollBar vBar = innerScroll.getVerticalScrollBar();
            boolean shouldForward = vBar == null || !vBar.isVisible();
            if (!shouldForward) {
                int rot = e.getWheelRotation();
                if (rot < 0 && vBar.getValue() <= vBar.getMinimum()) {
                    shouldForward = true;
                } else if (rot > 0 && vBar.getValue() + vBar.getVisibleAmount() >= vBar.getMaximum()) {
                    shouldForward = true;
                }
            }
            if (shouldForward) {
                outer.dispatchEvent(SwingUtilities.convertMouseEvent(innerScroll, e, outer));
            }
        });
    }

    // =========================================================
    // 8. STATUS BADGE RENDERER
    // =========================================================

    public static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            String status = value == null ? "" : value.toString().trim();
            int hoverRow = getTableHoverRow(table);

            JPanel wrapper = new JPanel(new GridBagLayout());
            if (isSelected) {
                wrapper.setBackground(snap(PRIMARY_LIGHT));
            } else if (row == hoverRow) {
                wrapper.setBackground(snap(SURFACE_HOVER));
            } else {
                wrapper.setBackground(row % 2 == 0 ? snap(SURFACE_CARD) : snap(BG_PAGE));
            }

            JLabel pill = new JLabel(status) {
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
            pill.setOpaque(false);
            pill.setHorizontalAlignment(SwingConstants.CENTER);
            pill.setFont(FONT_BADGE);
            pill.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));

            switch (status) {
                case "Đúng hạn": case "Hoạt động": case "Đã trả":
                case "Đã gửi": case "Đã xử lý": case "Đã hoàn thành":
                    pill.setBackground(snap(STATUS_OK_BG));
                    pill.setForeground(snap(STATUS_OK_FG));
                    break;
                case "Sắp đến hạn": case "Hàng đợi": case "Chờ duyệt":
                case "Chờ gửi": case "Chờ xác nhận":
                    pill.setBackground(snap(STATUS_WARN_BG));
                    pill.setForeground(snap(STATUS_WARN_FG));
                    break;
                case "Quá hạn": case "Vi phạm": case "Bị khóa": case "Đã khóa":
                case "Lỗi gửi": case "Cần kiểm tra": case "Đã hủy": case "Chưa xử lý":
                    pill.setBackground(snap(STATUS_ERROR_BG));
                    pill.setForeground(snap(STATUS_ERROR_FG));
                    break;
                case "Chờ xử lý": case "Đang xử lý":
                case "Đang mượn": case "Đang chờ":
                    pill.setBackground(snap(STATUS_PENDING_BG));
                    pill.setForeground(snap(STATUS_PENDING_FG));
                    break;
                default:
                    pill.setBackground(snap(SUBTLE_SURFACE));
                    pill.setForeground(snap(TEXT_SECONDARY));
                    break;
            }

            wrapper.add(pill);
            return wrapper;
        }
    }

    // =========================================================
    // 9. INTERNAL — Striped + Hover row renderer & SaaS Data Grid Helpers
    // =========================================================

    private static int getTableHoverRow(JTable table) {
        if (table == null) {
            return -1;
        }
        Object val = table.getClientProperty("ThemeConfig.hoverRow");
        return val instanceof Integer n ? n : -1;
    }

    private static boolean isNeutralCellBackground(Color bg) {
        if (bg == null) {
            return true;
        }
        int rgb = bg.getRGB() & 0xFFFFFF;
        return rgb == 0xFFFFFF || rgb == 0xF8FAFC || rgb == 0xE9EFF8
                || rgb == 0x1E293B || rgb == 0x0F172A || rgb == 0x263449;
    }

    private static final class HoverDelegatingRenderer implements TableCellRenderer {
        private final TableCellRenderer delegate;

        HoverDelegatingRenderer(TableCellRenderer delegate) {
            this.delegate = delegate;
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            TableCellRenderer target = delegate != null
                    ? delegate
                    : table.getDefaultRenderer(table.getColumnClass(column));
            Component comp = target.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                int hoverRow = getTableHoverRow(table);
                Color stripeBg = row % 2 == 0 ? snap(SURFACE_CARD) : snap(BG_PAGE);
                Color desiredBg = (row == hoverRow) ? snap(SURFACE_HOVER) : stripeBg;

                if (comp instanceof JPanel panel) {
                    panel.setOpaque(true);
                    panel.setBackground(desiredBg);
                } else if (comp instanceof JComponent jc && isNeutralCellBackground(jc.getBackground())) {
                    jc.setBackground(desiredBg);
                }
            }
            return comp;
        }
    }

    private static final class EmptyStateViewport extends JViewport {
        private final JTable table;
        private final Ikon emptyIcon;
        private final String emptyTitle;
        private final String emptySubtitle;

        EmptyStateViewport(JTable table, Ikon emptyIcon, String emptyTitle, String emptySubtitle) {
            this.table = table;
            this.emptyIcon = emptyIcon;
            this.emptyTitle = emptyTitle;
            this.emptySubtitle = emptySubtitle;
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (table.getRowCount() > 0) {
                return;
            }
            int w = getWidth();
            int h = getHeight();
            if (w < 160 || h < 70) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            boolean isFilteredEmpty = table.getModel().getRowCount() > 0;
            Ikon iconCode = isFilteredEmpty ? Feather.SEARCH : (emptyIcon != null ? emptyIcon : Feather.INBOX);
            String title = isFilteredEmpty
                    ? "Không tìm thấy kết quả phù hợp"
                    : (emptyTitle != null && !emptyTitle.isBlank() ? emptyTitle : "Chưa có dữ liệu");
            String subtitle = isFilteredEmpty
                    ? "Hãy thử từ khóa khác, nhấn nút X trên ô tìm kiếm hoặc đặt lại bộ lọc."
                    : (emptySubtitle != null ? emptySubtitle : "");

            boolean compact = h < 155;
            int badgeSize = compact ? 38 : 56;
            int iconSize = compact ? 18 : 26;

            Font titleFont = fontBold(compact ? 14f : 15.5f);
            Font subFont = FONT_SM;
            FontMetrics fmTitle = g2.getFontMetrics(titleFont);
            FontMetrics fmSub = g2.getFontMetrics(subFont);

            List<String> subLines = (!compact || h >= 115) && !subtitle.isBlank()
                    ? wrapTextLines(subtitle, fmSub, Math.min(440, w - 48))
                    : List.of();

            int totalH = badgeSize + 10 + fmTitle.getHeight()
                    + (subLines.isEmpty() ? 0 : 6 + subLines.size() * fmSub.getHeight());
            int topY = Math.max(10, (h - totalH) / 2);

            int badgeX = (w - badgeSize) / 2;
            g2.setColor(snap(SUBTLE_SURFACE));
            g2.fillOval(badgeX, topY, badgeSize, badgeSize);
            g2.setColor(snap(BORDER));
            g2.drawOval(badgeX, topY, badgeSize, badgeSize);

            Icon icon = getIcon(iconCode, iconSize, snap(TEXT_MUTED));
            int ix = badgeX + (badgeSize - icon.getIconWidth()) / 2;
            int iy = topY + (badgeSize - icon.getIconHeight()) / 2;
            icon.paintIcon(this, g2, ix, iy);

            int textY = topY + badgeSize + 10 + fmTitle.getAscent();
            g2.setFont(titleFont);
            g2.setColor(snap(TEXT_SECONDARY));
            int titleW = fmTitle.stringWidth(title);
            g2.drawString(title, Math.max(12, (w - titleW) / 2), textY);

            if (!subLines.isEmpty()) {
                g2.setFont(subFont);
                g2.setColor(snap(TEXT_MUTED));
                int lineY = topY + badgeSize + 10 + fmTitle.getHeight() + 4 + fmSub.getAscent();
                for (String line : subLines) {
                    int lw = fmSub.stringWidth(line);
                    g2.drawString(line, Math.max(12, (w - lw) / 2), lineY);
                    lineY += fmSub.getHeight();
                }
            }

            g2.dispose();
        }

        private static List<String> wrapTextLines(String text, FontMetrics fm, int maxWidth) {
            List<String> lines = new ArrayList<>();
            if (maxWidth <= 40) {
                lines.add(text);
                return lines;
            }
            StringBuilder current = new StringBuilder();
            for (String word : text.split("\\s+")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (fm.stringWidth(candidate) <= maxWidth) {
                    current.setLength(0);
                    current.append(candidate);
                } else {
                    if (!current.isEmpty()) {
                        lines.add(current.toString());
                        current.setLength(0);
                    }
                    current.append(word);
                }
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
            return lines;
        }
    }

    private static class StripedHoverRenderer extends DefaultTableCellRenderer {
        int hoverRow = -1;

        StripedHoverRenderer(JTable table) { }

        @Override
        public Component getTableCellRendererComponent(
                JTable t, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(t, value, isSelected,
                                                hasFocus, row, column);
            setFont(FONT_TABLE);
            setBorder(BorderFactory.createEmptyBorder(0, SPACE_3, 0, SPACE_3));

            int activeHover = Math.max(hoverRow, getTableHoverRow(t));
            if (isSelected) {
                setBackground(snap(PRIMARY_LIGHT));
                setForeground(darkMode ? new Color(0xDBEAFE) : PRIMARY_PRESSED);
            } else if (row == activeHover) {
                setBackground(snap(SURFACE_HOVER));
                setForeground(snap(TEXT_PRIMARY));
            } else {
                setBackground(row % 2 == 0 ? snap(SURFACE_CARD) : snap(BG_PAGE));
                setForeground(snap(TEXT_PRIMARY));
            }
            return this;
        }
    }

    // =========================================================
    // 10. MODERN CARD PANEL (14px rounded + soft drop shadow + hover elevation)
    // =========================================================

    /**
     * Card bề mặt cao cấp vẽ bằng Java 2D: bo góc 14px, đổ bóng mềm 2-3px,
     * viền 1px sắc nét và hỗ trợ Hover Elevation cho thẻ tương tác.
     */
    public static class ModernCardPanel extends JPanel {
        private static final int CORNER_ARC = 14;
        private static final int SHADOW_LEFT = 3;
        private static final int SHADOW_RIGHT = 3;
        private static final int SHADOW_TOP = 2;
        private static final int SHADOW_BOTTOM = 4;

        private Color hoverAccentColor;
        private Color leftAccentColor;
        private boolean interactive;
        private boolean hovered;

        public ModernCardPanel() {
            this(new BorderLayout(), new Insets(SPACE_5, SPACE_6, SPACE_5, SPACE_6));
        }

        public ModernCardPanel(LayoutManager layout) {
            this(layout, new Insets(SPACE_5, SPACE_6, SPACE_5, SPACE_6));
        }

        public ModernCardPanel(LayoutManager layout, Insets innerPadding) {
            super(layout);
            setOpaque(false);
            Insets pad = innerPadding != null ? innerPadding : new Insets(SPACE_5, SPACE_6, SPACE_5, SPACE_6);
            setBorder(new EmptyBorder(
                    SHADOW_TOP + pad.top,
                    SHADOW_LEFT + pad.left,
                    SHADOW_BOTTOM + pad.bottom,
                    SHADOW_RIGHT + pad.right));
        }

        public void setLeftAccentColor(Color accent) {
            this.leftAccentColor = accent;
            repaint();
        }

        public void enableInteractive(Color accentColor, Runnable onClick) {
            this.interactive = true;
            this.hoverAccentColor = accentColor != null ? accentColor : PRIMARY;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            MouseAdapter adapter = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!hovered) {
                        hovered = true;
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    Point pt = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), ModernCardPanel.this);
                    if (!ModernCardPanel.this.contains(pt) && hovered) {
                        hovered = false;
                        repaint();
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (onClick != null && SwingUtilities.isLeftMouseButton(e)) {
                        onClick.run();
                    }
                }
            };
            attachMouseRecursive(this, adapter);
        }

        private static void attachMouseRecursive(Component comp, MouseAdapter adapter) {
            comp.addMouseListener(adapter);
            if (comp instanceof Container container) {
                for (Component child : container.getComponents()) {
                    attachMouseRecursive(child, adapter);
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int lift = (interactive && hovered) ? 1 : 0;
            int x = SHADOW_LEFT;
            int y = SHADOW_TOP - lift;
            int w = getWidth() - SHADOW_LEFT - SHADOW_RIGHT;
            int h = getHeight() - SHADOW_TOP - SHADOW_BOTTOM;

            if (w > 4 && h > 4) {
                int shadowLayers = hovered ? 4 : 3;
                int baseAlpha = darkMode ? (hovered ? 26 : 18) : (hovered ? 12 : 8);
                for (int i = shadowLayers; i >= 1; i--) {
                    int alpha = Math.max(3, baseAlpha - (i - 1) * 2);
                    g2.setColor(new Color(15, 23, 42, alpha));
                    g2.fill(new RoundRectangle2D.Float(
                            x - (i - 1) * 0.6f,
                            y + i * 0.8f,
                            w + (i - 1) * 1.2f,
                            h + i * 0.5f,
                            CORNER_ARC + i,
                            CORNER_ARC + i));
                }

                RoundRectangle2D cardShape = new RoundRectangle2D.Float(x, y, w, h, CORNER_ARC, CORNER_ARC);
                Color surface = (interactive && hovered) ? snap(SURFACE_HOVER) : snap(SURFACE_CARD);
                g2.setColor(surface);
                g2.fill(cardShape);

                if (leftAccentColor != null) {
                    Shape oldClip = g2.getClip();
                    g2.setClip(cardShape);
                    g2.setColor(snap(leftAccentColor));
                    g2.fillRect(x, y, 4, h);
                    g2.setClip(oldClip);
                }

                Color borderClr = (interactive && hovered && hoverAccentColor != null)
                        ? snap(hoverAccentColor)
                        : snap(BORDER);
                g2.setColor(borderClr);
                g2.setStroke(new BasicStroke(hovered && interactive ? 1.4f : 1f));
                g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1f, h - 1f, CORNER_ARC, CORNER_ARC));
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Tạo ModernCardPanel chuẩn với padding thoáng 20x24px, bo góc 14px và bóng mềm.
     */
    public static JPanel createCard() {
        return new ModernCardPanel(new BorderLayout(), new Insets(SPACE_5, SPACE_6, SPACE_5, SPACE_6));
    }

    /** Helper hiển thị Toast Notification nhanh gọn không chặn luồng người dùng. */
    public static void toastSuccess(Component parent, String message) {
        ToastNotification.show(parent, message, ToastNotification.ToastType.SUCCESS);
    }

    public static void toastWarning(Component parent, String message) {
        ToastNotification.show(parent, message, ToastNotification.ToastType.WARNING);
    }

    public static void toastError(Component parent, String message) {
        ToastNotification.show(parent, message, ToastNotification.ToastType.ERROR);
    }

    public static void toastInfo(Component parent, String message) {
        ToastNotification.show(parent, message, ToastNotification.ToastType.INFO);
    }

    /**
     * Tạo JLabel tiêu đề section (H2).
     */
    public static JLabel createSectionTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_H2);
        lbl.setForeground(TEXT_PRIMARY);
        return lbl;
    }

    // =========================================================
    // 11. IKONLI HELPER
    // =========================================================

    /**
     * Tạo Icon vector Ikonli với mã icon, kích thước và màu sắc chỉ định.
     *
     * @param iconCode Mã icon (ví dụ: Feather.BOOK_OPEN)
     * @param size     Kích thước icon tính bằng pixel (ví dụ: 18, 24)
     * @param color    Màu sắc của icon
     * @return Icon giao diện Swing sẵn sàng gắn vào JLabel, JButton...
     */
    public static Icon getIcon(Ikon iconCode, int size, Color color) {
        return FontIcon.of(iconCode, size, color);
    }

    // =========================================================
    // 12. LAYOUT & TITLE HELPERS
    // =========================================================

    /**
     * Tạo khối tiêu đề Card dùng BoxLayout dọc và khoảng đệm dưới 2px
     * để bảo vệ nét dưới các chữ cái tiếng Việt (g, q, p, y).
     */
    public static JPanel createTitleBlock(String title, String subtitle, Ikon iconCode) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(FONT_H3);
        lblTitle.setForeground(TEXT_PRIMARY);
        lblTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (iconCode != null) {
            lblTitle.setIcon(getIcon(iconCode, 18, PRIMARY));
            lblTitle.setIconTextGap(8);
        }
        panel.add(lblTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            panel.add(Box.createVerticalStrut(3));
            JLabel lblSub = new JLabel(subtitle);
            lblSub.setFont(FONT_SM);
            lblSub.setForeground(TEXT_SECONDARY);
            lblSub.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
            lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(lblSub);
        }
        return panel;
    }

    /**
     * Bọc nội dung trang (triển khai ScrollablePanel) trong JScrollPane cuộn dọc mượt,
     * triệt tiêu hoàn toàn thanh cuộn ngang và tự đồng bộ màu nền Sáng/Tối.
     */
    public static JScrollPane createPageScrollPane(JComponent pageContent) {
        JScrollPane sp = new JScrollPane(
                pageContent,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.setBackground(snap(BG_PAGE));
        sp.getViewport().setBackground(snap(BG_PAGE));
        sp.getVerticalScrollBar().setUnitIncrement(18);
        sp.putClientProperty("ThemeConfig.refresh", (Runnable) () -> {
            sp.setBackground(snap(BG_PAGE));
            if (sp.getViewport() != null) {
                sp.getViewport().setBackground(snap(BG_PAGE));
            }
        });
        return sp;
    }

    /**
     * JPanel khóa chiều rộng vừa khít viewport của JScrollPane, triệt tiêu thanh cuộn ngang.
     */
    public static class ScrollablePanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 64;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /**
     * FlowLayout tự động xuống dòng và tính lại chiều cao preferred/minimum
     * khi container thu hẹp chiều ngang, tránh cắt khuất nút bấm.
     */
    public static class WrapFlowLayout extends FlowLayout {
        public WrapFlowLayout() {
            super();
        }

        public WrapFlowLayout(int align) {
            super(align);
        }

        public WrapFlowLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension minimum = layoutSize(target, false);
            minimum.width -= (getHgap() + 1);
            return minimum;
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                Container container = target;
                while (container.getSize().width == 0 && container.getParent() != null) {
                    container = container.getParent();
                }
                if (targetWidth == 0) {
                    targetWidth = container.getSize().width;
                }
                if (targetWidth == 0) {
                    targetWidth = Integer.MAX_VALUE;
                }

                int hgap = getHgap();
                int vgap = getVgap();
                Insets insets = target.getInsets();
                int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
                int maxWidth = targetWidth - horizontalInsetsAndGap;

                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0;
                int rowHeight = 0;

                int nmembers = target.getComponentCount();
                for (int i = 0; i < nmembers; i++) {
                    Component m = target.getComponent(i);
                    if (m.isVisible()) {
                        Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                        if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                            addRow(dim, rowWidth, rowHeight);
                            rowWidth = 0;
                            rowHeight = 0;
                        }
                        if (rowWidth != 0) {
                            rowWidth += hgap;
                        }
                        rowWidth += d.width;
                        rowHeight = Math.max(rowHeight, d.height);
                    }
                }
                addRow(dim, rowWidth, rowHeight);

                dim.width += horizontalInsetsAndGap;
                dim.height += insets.top + insets.bottom + vgap * 2;

                Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
                if (scrollPane != null && target.isValid()) {
                    dim.width -= (hgap + 1);
                }
                return dim;
            }
        }

        private void addRow(Dimension dim, int rowWidth, int rowHeight) {
            dim.width = Math.max(dim.width, rowWidth);
            if (dim.height > 0) {
                dim.height += getVgap();
            }
            dim.height += rowHeight;
        }
    }

    // =========================================================
    // 13. LIVE SEARCH & STATUS FEEDBACK HELPERS
    // =========================================================

    /**
     * Chuẩn hóa chuỗi tiếng Việt (bỏ dấu, chữ thường) phục vụ tìm kiếm tức thời.
     */
    public static String normalizeText(String input) {
        if (input == null) {
            return "";
        }
        String lower = input.toLowerCase(Locale.ROOT).trim();
        return Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('\u0111', 'd');
    }

    /** Tạo nhãn hiển thị trạng thái thao tác (thành công / lỗi). */
    public static JLabel createStatusLabel() {
        JLabel lbl = new JLabel(" ");
        lbl.setFont(FONT_SM);
        lbl.setIconTextGap(6);
        lbl.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        lbl.setOpaque(false);
        return lbl;
    }

    /** Cập nhật nội dung và màu trạng thái rõ ràng cho người dùng. */
    public static void showStatus(JLabel lbl, boolean success, String message) {
        if (lbl == null) {
            return;
        }
        if (message == null || message.isBlank()) {
            lbl.setText(" ");
            lbl.setIcon(null);
            lbl.setOpaque(false);
            return;
        }
        lbl.setText(message);
        lbl.setOpaque(true);
        if (success) {
            lbl.setIcon(getIcon(Feather.CHECK_CIRCLE, 15, STATUS_OK_FG));
            lbl.setBackground(STATUS_OK_BG);
            lbl.setForeground(STATUS_OK_FG);
        } else {
            lbl.setIcon(getIcon(Feather.ALERT_CIRCLE, 15, STATUS_ERROR_FG));
            lbl.setBackground(STATUS_ERROR_BG);
            lbl.setForeground(STATUS_ERROR_FG);
        }
    }
}
