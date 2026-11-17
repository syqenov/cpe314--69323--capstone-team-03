package pawtrack.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.function.IntSupplier;

/** Polished, reusable top bar. No controller or database code. */
public final class PawTrackHeader extends JPanel {
    private static final Color NAVY = new Color(0x172E55);
    private static final Color BLUE = new Color(0x356ABE);
    private JButton notificationButton;
    private JButton appearanceButton;
    private Runnable appearanceAction;
    private java.util.function.BooleanSupplier darkTheme;
    private IntSupplier unreadCountSupplier;

    public PawTrackHeader(String role) { this(role, null); }
    public PawTrackHeader(String role, Runnable onNotifications) {
        this(role, onNotifications, null);
    }
    public PawTrackHeader(String role, Runnable onNotifications, IntSupplier unreadCountSupplier) {
        super(new BorderLayout(18, 0));
        this.unreadCountSupplier = unreadCountSupplier;
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE0E9F8)),
                BorderFactory.createEmptyBorder(12, 22, 12, 22)));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 11, 0));
        brand.setOpaque(false);
        brand.add(new PawLogo());
        JLabel appName = new JLabel("PawTrack");
        appName.setForeground(NAVY);
        appName.setFont(new Font("Segoe UI", Font.BOLD, 22));
        brand.add(appName);
        JLabel divider = new JLabel("  |  ");
        divider.setForeground(new Color(0xB9C8DF));
        brand.add(divider);
        JLabel sub = new JLabel("Pet Shelter & Adoption Tracker");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(0x7386A2));
        brand.add(sub);
        add(brand, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        JLabel connected = new JLabel("● Connected");
        connected.setFont(new Font("Segoe UI", Font.BOLD, 11));
        connected.setForeground(new Color(0x26965D));
        connected.setToolTipText("Application connected to its local data store");
        if ("ADOPTER".equalsIgnoreCase(role) && onNotifications != null) {
            notificationButton = topButton("Notifications");
            notificationButton.addActionListener(e -> {
                onNotifications.run();
                refreshNotificationBadge();
            });
            right.add(notificationButton);
        }
        appearanceButton = topButton("Dark Mode");
        appearanceButton.setName("globalThemeToggle");
        appearanceButton.setToolTipText("Switch between Light and Midnight Navy themes (Ctrl+T)");
        appearanceButton.addActionListener(e -> { if (appearanceAction != null) appearanceAction.run(); });
        right.add(appearanceButton);
        JLabel avatar = new JLabel(" " + ("STAFF".equalsIgnoreCase(role) ? "ST" : "AD") + " ", SwingConstants.CENTER);
        avatar.setOpaque(true);
        avatar.setBackground(new Color(0x2D4D88));
        avatar.setForeground(Color.WHITE);
        avatar.setFont(new Font("Segoe UI", Font.BOLD, 11));
        avatar.setPreferredSize(new Dimension(36, 36));
        avatar.setBorder(BorderFactory.createLineBorder(new Color(0x2D4D88), 1, true));
        JLabel roleLabel = new JLabel("STAFF".equalsIgnoreCase(role) ? "Staff workspace" : "Adopter workspace");
        roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        roleLabel.setForeground(NAVY);
        JPanel account = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 0));
        account.setOpaque(false);
        account.add(connected);
        account.add(avatar);
        account.add(roleLabel);
        right.add(account);
        add(right, BorderLayout.EAST);
        refreshNotificationBadge();
    }

    /** Set by MainFrame; this view does not own persistence or account services. */
    public void setAppearanceAction(Runnable action, java.util.function.BooleanSupplier isDark) {
        this.appearanceAction = action;
        this.darkTheme = isDark;
        refreshAppearanceButton();
    }
    public void refreshAppearanceButton() {
        if (appearanceButton == null) return;
        boolean dark = darkTheme != null && darkTheme.getAsBoolean();
        appearanceButton.setText(dark ? "Light Mode" : "Dark Mode");
    }

    private static JButton topButton(String name) {
        JButton button = new HeaderActionButton(name);
        button.setOpaque(false);
        button.setBackground(new Color(0xEDF3FD));
        button.setForeground(BLUE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xDDE8FA), 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        return button;
    }

    /** Native Windows buttons ignored the new dark background while drawing
     * near-white labels. Explicitly paint both surface and text theme colors. */
    private static final class HeaderActionButton extends JButton {
        HeaderActionButton(String label) {
            super(label);
            setContentAreaFilled(false);
            setBorderPainted(false);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean dark = PawTrackTheme.isDark();
            g.setColor(dark ? (getModel().isRollover() ? PawTrackTheme.DARK_HOVER
                    : PawTrackTheme.DARK_INPUT) : new Color(0xEDF3FD));
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 11, 11);
            g.setColor(dark ? PawTrackTheme.DARK_BORDER : new Color(0xDDE8FA));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 11, 11);
            g.dispose();
            setForeground(dark ? PawTrackTheme.DARK_TEXT : BLUE);
            super.paintComponent(graphics);
        }
    }

    public void refreshNotificationBadge() {
        if (notificationButton == null || unreadCountSupplier == null) return;
        try {
            int unread = Math.max(0, unreadCountSupplier.getAsInt());
            notificationButton.setText(unread == 0 ? "Notifications" : "Notifications  (" + unread + ")");
        } catch (RuntimeException ex) {
            notificationButton.setText("Notifications");
        }
    }

    private static final class PawLogo extends JComponent {
        PawLogo() { setPreferredSize(new Dimension(37, 37)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D gg = (Graphics2D) g.create();
            gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gg.setColor(PawTrackTheme.isDark() ? PawTrackTheme.DARK_INPUT : new Color(0xE5EEFD));
            gg.fillRoundRect(0, 0, 37, 37, 12, 12);
            gg.setColor(PawTrackTheme.isDark() ? PawTrackTheme.DARK_ACCENT : BLUE);
            gg.fillOval(13, 18, 13, 11);
            gg.fillOval(8, 10, 6, 8);
            gg.fillOval(16, 6, 6, 8);
            gg.fillOval(25, 10, 6, 8);
            gg.dispose();
        }
    }
}
