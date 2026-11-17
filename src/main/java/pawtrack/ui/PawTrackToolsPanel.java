package pawtrack.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Tools & Settings: a focused, role-aware hub.
 *
 * Reuses existing MainFrame actions, so no workflow/controller changes are needed.
 * Staff-only actions are present only when the original authorized buttons exist.
 */
public final class PawTrackToolsPanel extends JPanel {
    @Override protected void paintComponent(java.awt.Graphics graphics) {
        super.paintComponent(graphics);
        PawTrackBackdrop.paint(graphics, this);
    }

    private static final Color NAVY = PawTrackTheme.NAVY;
    private static final Color BLUE = PawTrackTheme.BLUE;
    private static final Color MUTED = PawTrackTheme.MUTED;
    private static final Color BORDER = PawTrackTheme.LINE;
    private static final Color CANVAS = PawTrackTheme.CANVAS;
    private final JScrollPane originalToolbar;
    private final Runnable openAccountSettings;
    private final Runnable toggleAppearance;
    private final boolean staff;

    public PawTrackToolsPanel(JScrollPane actions) {
        this(actions, null);
    }

    public PawTrackToolsPanel(JScrollPane actions, Runnable openAccountSettings) {
        this(actions, openAccountSettings, null);
    }

    public PawTrackToolsPanel(JScrollPane actions, Runnable openAccountSettings,
                             Runnable toggleAppearance) {
        super(new BorderLayout());
        if (actions == null) throw new IllegalArgumentException("Actions are required");
        this.originalToolbar = actions;
        this.openAccountSettings = openAccountSettings;
        this.toggleAppearance = toggleAppearance;
        this.staff = findButton(actions, "Add Animal") != null;
        setBackground(CANVAS);

        JPanel content = new JPanel(new BorderLayout(0, 17));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(19, 22, 24, 22));
        content.add(buildHeader(), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 16, 16));
        grid.setOpaque(false);
        grid.add(appearanceCard());
        if (staff) grid.add(dataCard());
        grid.add(helpCard());
        if (staff) grid.add(administrationCard());

        // A wrapper keeps cards at a natural height, rather than stretching them
        // to fill the full height of the application window.
        JPanel stack = new JPanel(new BorderLayout());
        stack.setOpaque(false);
        stack.add(grid, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(stack);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.getHorizontalScrollBar().setUnitIncrement(20);
        content.add(scroll, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(14, 0)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setPaint(new GradientPaint(0, 0, PawTrackTheme.isDark() ? PawTrackTheme.DARK_INPUT : new Color(0xE0ECFF), getWidth(), getHeight(), PawTrackTheme.isDark() ? PawTrackTheme.DARK_SURFACE : new Color(0xF3F8FF)));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g.setColor(new Color(49, 95, 173, 22));
                g.fillOval(getWidth() - 145, -55, 185, 185);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(16, 22, 16, 22));
        header.setPreferredSize(new Dimension(100, 110));
        JPanel labels = new JPanel(new GridLayout(3, 1, 0, 3));
        labels.setOpaque(false);
        labels.add(text("WORKSPACE  /  PREFERENCES", 10, true, BLUE));
        labels.add(text("Tools & Settings", 25, true, NAVY));
        labels.add(text("Your essential preferences, data utilities and help — all in one place.", 12, false, MUTED));
        header.add(labels, BorderLayout.CENTER);
        JLabel badge = text(staff ? "STAFF TOOLS" : "MY WORKSPACE", 11, true, BLUE);
        header.add(badge, BorderLayout.EAST);
        return header;
    }

    private JPanel appearanceCard() {
        JPanel card = section("Appearance", "Customize how PawTrack looks.", "DISPLAY", new Color(0xDCE9FF));
        JPanel items = body(card);
        JButton theme = toggleAppearance != null
                ? actionButton("Switch Light / Dark", "Switch the full workspace theme", toggleAppearance)
                : linkedButton("Switch Light / Dark", "Use PawTrack's existing theme control", "Dark", "Light");
        items.add(theme);
        note(items, "Your theme preference is saved to this account and restored on sign-in.");
        return card;
    }

    private JPanel dataCard() {
        JPanel card = section("Data Management", "Export shelter data when needed.", "DATA", new Color(0xDDF5EA));
        JPanel items = body(card);
        items.add(linkedButton("Export Animals CSV", "Download animal records as CSV (Ctrl+E)", "Export CSV"));
        note(items, "For intake files, use the dedicated Import Records page.");
        note(items, "Database and photo backup is managed separately; no untested backup control was added.");
        return card;
    }

    private JPanel administrationCard() {
        JPanel card = section("Staff Administration", "Authorized actions for shelter staff.", "STAFF", new Color(0xF2E6FF));
        JPanel items = body(card);
        items.add(linkedButton("Undo Last Status Change", "Revert the last eligible animal status change (Ctrl+U)", "Undo Last Status Change"));
        // Staff animal-management actions now appear directly on the Animals page.
        // Keep only the administrative undo operation here.
        note(items, "Add, edit and delete animal records from the Animals page.");
        return card;
    }

    private JPanel helpCard() {
        JPanel card = section("Keyboard Shortcuts", "Navigate faster without extra toolbars.", "HELP", new Color(0xE1F5F4));
        JPanel items = body(card);
        items.add(actionButton("View keyboard shortcuts", "See the keyboard commands supported by PawTrack", this::showShortcuts));
        note(items, "Ctrl+R Refresh   •   Ctrl+F Classification   •   Ctrl+T Theme");
        note(items, "About: PawTrack is a Java desktop app with local SQLite data.");
        return card;
    }

    private static JPanel section(String name, String subtitle, String marker, Color markerTint) {
        JPanel card = new JPanel(new BorderLayout(0, 16)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(39, 75, 135, 16));
                g.fill(new RoundRectangle2D.Double(2, 5, getWidth()-5, getHeight()-7, 23, 23));
                g.setColor(PawTrackTheme.surface());
                g.fill(new RoundRectangle2D.Double(1, 1, getWidth()-5, getHeight()-9, 22, 22));
                g.setColor(PawTrackTheme.line());
                g.draw(new RoundRectangle2D.Double(1, 1, getWidth()-5, getHeight()-9, 22, 22));
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(17, 19, 22, 22));
        JPanel heading = new JPanel(new BorderLayout(13, 0));
        heading.setOpaque(false);
        JLabel icon = text(marker, 10, true, BLUE);
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        icon.setOpaque(true);
        icon.setBackground(markerTint);
        icon.setPreferredSize(new Dimension(62, 46));
        heading.add(icon, BorderLayout.WEST);
        JPanel headings = new JPanel(new GridLayout(2, 1, 0, 2));
        headings.setOpaque(false);
        headings.add(text(name, 18, true, NAVY));
        headings.add(text(subtitle, 11, false, MUTED));
        heading.add(headings, BorderLayout.CENTER);
        card.add(heading, BorderLayout.NORTH);
        return card;
    }

    private static JPanel body(JPanel card) {
        JPanel items = new JPanel();
        items.setOpaque(false);
        items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
        card.add(items, BorderLayout.CENTER);
        return items;
    }

    private static void note(JPanel to, String message) {
        JLabel label = text("• " + message, 11, false, MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(4, 3, 2, 3));
        to.add(label);
    }

    private JButton linkedButton(String title, String tooltip, String... oldLabels) {
        JButton original = null;
        for (String oldLabel : oldLabels) {
            original = findButton(originalToolbar, oldLabel);
            if (original != null) break;
        }
        JButton linked = original;
        JButton b = actionButton(title, tooltip, () -> {
            if (linked != null && linked.isEnabled()) linked.doClick();
        });
        if (linked == null || !linked.isEnabled()) {
            b.setEnabled(false);
            b.setToolTipText("Unavailable for this account or application state");
        }
        return b;
    }

    private static JButton actionButton(String title, String tooltip, Runnable callback) {
        JButton b = new JButton(title) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(PawTrackTheme.isDark() ? PawTrackTheme.DARK_INPUT : (isEnabled() ? new Color(0xEDF4FF) : new Color(0xF4F6F9)));
                g.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                g.setColor(PawTrackTheme.isDark() ? PawTrackTheme.DARK_BORDER : (isEnabled() ? new Color(0xA8C4F1) : new Color(0xE1E5EB)));
                g.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(11, 13, 11, 13));
        b.setForeground(NAVY);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        b.setMinimumSize(new Dimension(145, 42));
        b.setPreferredSize(new Dimension(280, 42));
        b.setToolTipText(tooltip);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener((ActionEvent e) -> callback.run());
        return b;
    }

    private static JLabel text(String s, int size, boolean bold, Color color) {
        JLabel l = new JLabel(s);
        l.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size));
        l.setForeground(color);
        return l;
    }

    private static JButton findButton(Component root, String text) {
        if (root instanceof JButton b && b.getText() != null && b.getText().contains(text)) return b;
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                JButton found = findButton(child, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void showShortcuts() {
        JOptionPane.showMessageDialog(this,
            "Ctrl+R  Refresh Animals\nCtrl+F  Focus Classification Filter\n" +
            "Ctrl+E  Export Animals CSV\nCtrl+I  Import CSV (staff)\n" +
            "Ctrl+U  Undo Last Status Change (staff)\nCtrl+T  Toggle Theme\n" +
            "F5       Refresh Analytics (staff)",
            "PawTrack Keyboard Shortcuts", JOptionPane.INFORMATION_MESSAGE);
    }

}
