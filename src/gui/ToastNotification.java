package gui;

import org.kordamp.ikonli.feather.Feather;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Thông báo nổi bo góc (Toast Notification) trượt nhẹ ở góc dưới bên phải cửa sổ,
 * tự đóng sau thời gian cấu hình (mặc định 3 giây) và tạm dừng khi rê chuột.
 */
public final class ToastNotification {

    public enum ToastType {
        SUCCESS, WARNING, ERROR, INFO
    }

    private static final int TOAST_WIDTH = 360;
    private static final int TOAST_HEIGHT = 64;
    private static final int GAP_Y = 10;
    private static final int MARGIN_RIGHT = 22;
    private static final int MARGIN_BOTTOM = 44;
    private static final int DEFAULT_DURATION_MS = 3000;

    private static final List<ActiveToast> ACTIVE_TOASTS = new ArrayList<>();

    private ToastNotification() {
    }

    public static void show(Component parent, String message, ToastType type) {
        show(parent, message, type, DEFAULT_DURATION_MS);
    }

    public static void show(Component parent, String message, ToastType type, int durationMs) {
        if (message == null || message.isBlank()) {
            return;
        }
        Runnable task = () -> displayToast(parent, message.trim(), type == null ? ToastType.INFO : type,
                Math.max(1200, durationMs));
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    private static void displayToast(Component parent, String message, ToastType type, int durationMs) {
        Window owner = resolveOwner(parent);
        if (owner == null || !owner.isDisplayable()) {
            return;
        }

        Color accent;
        Color badgeBg;
        Feather iconCode;
        String title;
        switch (type) {
            case SUCCESS -> {
                accent = ThemeConfig.SUCCESS;
                badgeBg = ThemeConfig.STATUS_OK_BG;
                iconCode = Feather.CHECK_CIRCLE;
                title = "Thành công";
            }
            case WARNING -> {
                accent = new Color(0xD97706);
                badgeBg = ThemeConfig.STATUS_WARN_BG;
                iconCode = Feather.ALERT_TRIANGLE;
                title = "Cảnh báo";
            }
            case ERROR -> {
                accent = ThemeConfig.DANGER;
                badgeBg = ThemeConfig.STATUS_ERROR_BG;
                iconCode = Feather.X_CIRCLE;
                title = "Lỗi";
            }
            default -> {
                accent = ThemeConfig.PRIMARY;
                badgeBg = ThemeConfig.PRIMARY_LIGHT;
                iconCode = Feather.INFO;
                title = "Thông báo";
            }
        }

        JWindow window = new JWindow(owner);
        window.setFocusableWindowState(false);
        window.setAlwaysOnTop(false);

        boolean supportsTranslucency = isPerPixelTranslucencySupported(owner);
        if (supportsTranslucency) {
            try {
                window.setBackground(new Color(0, 0, 0, 0));
            } catch (UnsupportedOperationException ignored) {
                supportsTranslucency = false;
            }
        }

        JPanel content = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int pad = 4;
                int cardW = w - pad * 2;
                int cardH = h - pad * 2;

                // Đổ bóng mềm đa lớp
                for (int i = 3; i >= 1; i--) {
                    g2.setColor(new Color(15, 23, 42, ThemeConfig.isDarkMode() ? 45 : 14 * (4 - i)));
                    g2.fill(new RoundRectangle2D.Float(pad - i + 2f, pad + i, cardW + (i - 2) * 2f, cardH, 16, 16));
                }

                // Nền thẻ
                g2.setColor(ThemeConfig.SURFACE_CARD);
                RoundRectangle2D cardShape = new RoundRectangle2D.Float(pad, pad, cardW, cardH, 14, 14);
                g2.fill(cardShape);

                // Dải màu nhấn bên trái
                Shape oldClip = g2.getClip();
                g2.setClip(cardShape);
                g2.setColor(accent);
                g2.fillRect(pad, pad, 5, cardH);
                g2.setClip(oldClip);

                // Viền sắc nét 1px
                g2.setColor(ThemeConfig.BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(pad, pad, cardW - 1f, cardH - 1f, 14, 14));

                g2.dispose();
            }
        };
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(12, 18, 12, 14));

        JPanel iconWrap = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(badgeBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
        };
        iconWrap.setOpaque(false);
        iconWrap.setPreferredSize(new Dimension(34, 34));
        iconWrap.add(new JLabel(ThemeConfig.getIcon(iconCode, 18, accent)));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(ThemeConfig.FONT_LABEL);
        lblTitle.setForeground(accent);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        String escaped = message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        JLabel lblMsg = new JLabel("<html><div style='width:225px;'>" + escaped + "</div></html>");
        lblMsg.setFont(ThemeConfig.FONT_SM);
        lblMsg.setForeground(ThemeConfig.TEXT_PRIMARY);
        lblMsg.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(lblTitle);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(lblMsg);

        JLabel btnClose = new JLabel(ThemeConfig.getIcon(Feather.X, 14, ThemeConfig.TEXT_MUTED));
        btnClose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClose.setBorder(new EmptyBorder(2, 4, 2, 2));

        JPanel leftWrap = new JPanel(new GridBagLayout());
        leftWrap.setOpaque(false);
        leftWrap.add(iconWrap);

        JPanel rightWrap = new JPanel(new BorderLayout());
        rightWrap.setOpaque(false);
        rightWrap.add(btnClose, BorderLayout.NORTH);

        content.add(leftWrap, BorderLayout.WEST);
        content.add(textPanel, BorderLayout.CENTER);
        content.add(rightWrap, BorderLayout.EAST);

        window.setContentPane(content);
        Dimension pref = content.getPreferredSize();
        int width = TOAST_WIDTH;
        int height = Math.max(TOAST_HEIGHT, pref.height + 8);
        window.setSize(width, height);
        if (!supportsTranslucency) {
            try {
                window.setShape(new RoundRectangle2D.Float(4, 4, width - 8f, height - 8f, 14, 14));
            } catch (UnsupportedOperationException ignored) {
            }
        }

        ActiveToast toast = new ActiveToast(owner, window, width, height, durationMs);
        synchronized (ACTIVE_TOASTS) {
            ACTIVE_TOASTS.add(toast);
            if (ACTIVE_TOASTS.size() > 4) {
                ActiveToast oldest = ACTIVE_TOASTS.get(0);
                oldest.closeImmediately();
            }
            repositionToasts(owner);
        }

        MouseAdapter hoverPause = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                toast.paused = true;
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Point pt = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), content);
                if (!content.contains(pt)) {
                    toast.paused = false;
                }
            }
        };
        attachRecursive(content, hoverPause);
        btnClose.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toast.startSlideOut();
            }
        });

        toast.startSlideIn();
    }

    private static void attachRecursive(Component c, MouseAdapter adapter) {
        c.addMouseListener(adapter);
        if (c instanceof Container container) {
            for (Component child : container.getComponents()) {
                attachRecursive(child, adapter);
            }
        }
    }

    private static Window resolveOwner(Component parent) {
        if (parent instanceof Window w) {
            return w;
        }
        if (parent != null) {
            Window w = SwingUtilities.getWindowAncestor(parent);
            if (w != null) {
                return w;
            }
        }
        for (Window w : Window.getWindows()) {
            if (w.isShowing() && w.isActive()) {
                return w;
            }
        }
        for (Frame f : Frame.getFrames()) {
            if (f.isShowing()) {
                return f;
            }
        }
        return null;
    }

    private static boolean isPerPixelTranslucencySupported(Window owner) {
        try {
            GraphicsDevice gd = owner.getGraphicsConfiguration().getDevice();
            return gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void repositionToasts(Window owner) {
        Rectangle bounds = owner.getBounds();
        int targetX = bounds.x + bounds.width - TOAST_WIDTH - MARGIN_RIGHT;
        int currentBottomY = bounds.y + bounds.height - MARGIN_BOTTOM;

        for (int i = ACTIVE_TOASTS.size() - 1; i >= 0; i--) {
            ActiveToast t = ACTIVE_TOASTS.get(i);
            if (t.owner != owner) {
                continue;
            }
            int targetY = currentBottomY - t.height;
            t.targetX = targetX;
            t.targetY = targetY;
            if (!t.animatingIn && !t.animatingOut) {
                t.window.setLocation(targetX, targetY);
            }
            currentBottomY = targetY - GAP_Y;
        }
    }

    private static final class ActiveToast {
        final Window owner;
        final JWindow window;
        final int width;
        final int height;
        int targetX;
        int targetY;
        boolean animatingIn = true;
        boolean animatingOut = false;
        boolean paused = false;
        final long holdMs;
        long elapsedWhileHolding = 0;

        Timer slideInTimer;
        Timer holdTimer;
        Timer slideOutTimer;

        ActiveToast(Window owner, JWindow window, int width, int height, int holdMs) {
            this.owner = owner;
            this.window = window;
            this.width = width;
            this.height = height;
            this.holdMs = holdMs;
        }

        void startSlideIn() {
            final int startX = targetX + 44;
            window.setLocation(startX, targetY);
            window.setVisible(true);

            final int steps = 10;
            final int[] tick = { 0 };
            slideInTimer = new Timer(16, e -> {
                tick[0]++;
                double t = Math.min(1.0, (double) tick[0] / steps);
                double eased = 1.0 - Math.pow(1.0 - t, 3);
                int curX = (int) Math.round(startX + (targetX - startX) * eased);
                if (window.isDisplayable()) {
                    window.setLocation(curX, targetY);
                }
                if (tick[0] >= steps) {
                    slideInTimer.stop();
                    animatingIn = false;
                    startHoldTimer();
                }
            });
            slideInTimer.start();
        }

        void startHoldTimer() {
            holdTimer = new Timer(100, e -> {
                if (!owner.isShowing() || !window.isDisplayable()) {
                    closeImmediately();
                    return;
                }
                if (!paused) {
                    elapsedWhileHolding += 100;
                    if (elapsedWhileHolding >= holdMs) {
                        holdTimer.stop();
                        startSlideOut();
                    }
                }
            });
            holdTimer.start();
        }

        void startSlideOut() {
            if (animatingOut) {
                return;
            }
            animatingIn = false;
            animatingOut = true;
            if (slideInTimer != null) slideInTimer.stop();
            if (holdTimer != null) holdTimer.stop();

            final int startX = window.getX();
            final int endX = targetX + width + 24;
            final int steps = 10;
            final int[] tick = { 0 };
            slideOutTimer = new Timer(16, e -> {
                tick[0]++;
                double t = Math.min(1.0, (double) tick[0] / steps);
                double eased = t * t;
                int curX = (int) Math.round(startX + (endX - startX) * eased);
                if (window.isDisplayable()) {
                    window.setLocation(curX, targetY);
                }
                if (tick[0] >= steps) {
                    closeImmediately();
                }
            });
            slideOutTimer.start();
        }

        void closeImmediately() {
            if (slideInTimer != null) slideInTimer.stop();
            if (holdTimer != null) holdTimer.stop();
            if (slideOutTimer != null) slideOutTimer.stop();
            if (window.isDisplayable()) {
                window.setVisible(false);
                window.dispose();
            }
            synchronized (ACTIVE_TOASTS) {
                Iterator<ActiveToast> it = ACTIVE_TOASTS.iterator();
                while (it.hasNext()) {
                    if (it.next() == this) {
                        it.remove();
                        break;
                    }
                }
                repositionToasts(owner);
            }
        }
    }
}
