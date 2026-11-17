package pawtrack.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared navigation sidebar.
 *
 * Navigation-only mode (used by MainFrame) groups pages into MAIN / MANAGE /
 * SYSTEM sections, gives every page its own vector icon, collapses to an
 * icon-only rail, and pins a Log out button to the bottom. The panel paints a
 * soft navy gradient with faint paw prints to match the PawTrack header.
 */
public final class PawTrackSidebar extends JPanel {
    private static final Color NAVY = new Color(0x1B3157);
    private static final Color BLUE = new Color(0x356ABE);
    private static final Color HEADING = new Color(0xAFC5E1);
    private static final Color DIVIDER = new Color(0x2D4D88);
    private static final Color ICON = new Color(0xD8E9FF);
    private static final Color LOGOUT_ICON = new Color(0xFFD9D9);
    private static final String[] SECTIONS = {"MAIN", "MANAGE", "SYSTEM"};
    private static final int EXPANDED_WIDTH = 248;
    private static final int COLLAPSED_WIDTH = 76;
    private static final int NAV_HEIGHT = 40;

    private final Runnable onLogout;
    private final List<JComponent> sectionHeadings = new ArrayList<>();

    public PawTrackSidebar(List<? extends JButton> pages, JScrollPane actions) {
        this(pages, actions, false, true, null);
    }

    /** When navigationOnly is true, actions live in the Tools & Settings page. */
    public PawTrackSidebar(List<? extends JButton> pages, JScrollPane actions, boolean navigationOnly) {
        this(pages, actions, navigationOnly, true, null);
    }

    public PawTrackSidebar(List<? extends JButton> pages, JScrollPane actions, boolean navigationOnly, boolean staff) {
        this(pages, actions, navigationOnly, staff, null);
    }

    /** onLogout may be null; the Log out button is only shown when it is provided. */
    public PawTrackSidebar(List<? extends JButton> pages, JScrollPane actions, boolean navigationOnly,
                           boolean staff, Runnable onLogout) {
        super(new BorderLayout(0, 0));
        if (pages == null || actions == null) {
            throw new IllegalArgumentException("Pages and actions are required");
        }
        this.onLogout = onLogout;
        setBackground(NAVY);
        setOpaque(true);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0xD9E4F1)));
        if (navigationOnly) {
            installCompactNavigation(pages, staff);
        } else {
            installLegacyNavigation(pages, actions);
        }
    }

    // ------------------------------------------------------------ painting

    /** Soft vertical gradient plus faint decorative paw prints. */
    @Override protected void paintComponent(Graphics g) {
        Graphics2D gg = (Graphics2D) g.create();
        gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gg.setPaint(new GradientPaint(0, 0, PawTrackTheme.isDark() ? PawTrackTheme.DARK_SIDEBAR : new Color(0x1F3868), 0, getHeight(), PawTrackTheme.isDark() ? new Color(0x101B2B) : new Color(0x121F3B)));
        gg.fillRect(0, 0, getWidth(), getHeight());
        if (getWidth() > 150 && getHeight() > 460) {
            gg.setColor(new Color(255, 255, 255, 13));
            paw(gg, getWidth() - 96, getHeight() - 215, 1.9);
            paw(gg, 14, getHeight() - 305, 1.15);
        }
        gg.dispose();
    }

    /** Same paw shape as the header logo (drawn in a 37x37 box, then scaled). */
    private static void paw(Graphics2D g, int x, int y, double scale) {
        Graphics2D p = (Graphics2D) g.create();
        p.translate(x, y);
        p.scale(scale, scale);
        p.fillOval(13, 18, 13, 11);
        p.fillOval(8, 10, 6, 8);
        p.fillOval(16, 6, 6, 8);
        p.fillOval(25, 10, 6, 8);
        p.dispose();
    }

    // ------------------------------------------------------------ sections

    private static String sectionOf(String title) {
        return switch (title) {
            case "Dashboard", "Animals", "My Records" -> "MAIN";
            case "Shelter Analytics", "Waitlist Requests", "Application History",
                 "Activity Log", "Import Records" -> "MANAGE";
            default -> "SYSTEM";
        };
    }

    private static void styleNavButton(JButton button, String original, boolean collapsed) {
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setIcon(new NavigationIcon(original, ICON));
        button.setIconTextGap(14);
        button.setText(collapsed ? "" : original);
        button.setToolTipText(collapsed ? original : null);
        button.setHorizontalAlignment(collapsed ? SwingConstants.CENTER : SwingConstants.LEFT);
        button.setBorder(BorderFactory.createEmptyBorder(0, collapsed ? 0 : 15, 0, collapsed ? 0 : 10));
        button.setPreferredSize(new Dimension(collapsed ? 48 : 220, NAV_HEIGHT));
        button.setMinimumSize(new Dimension(collapsed ? 48 : 155, NAV_HEIGHT));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, NAV_HEIGHT));
    }

    // -------------------------------------------------- compact (main) mode

    private void installCompactNavigation(List<? extends JButton> pages, boolean staff) {
        setPreferredSize(new Dimension(EXPANDED_WIDTH, 0));
        setMinimumSize(new Dimension(COLLAPSED_WIDTH, 0));
        final String[] labels = pages.stream().map(JButton::getText).toArray(String[]::new);
        final boolean[] collapsed = {false};

        // ---- navigation, grouped into sections (transparent so the gradient shows)
        JPanel navigation = new JPanel();
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));
        navigation.setOpaque(false);
        navigation.setBorder(BorderFactory.createEmptyBorder(4, 10, 10, 10));
        for (String section : SECTIONS) {
            List<JButton> group = new ArrayList<>();
            for (JButton page : pages) {
                if (section.equals(sectionOf(page.getText()))) group.add(page);
            }
            if (group.isEmpty()) continue;
            SectionHeading heading = new SectionHeading(section);
            sectionHeadings.add(heading);
            navigation.add(heading);
            navigation.add(Box.createVerticalStrut(7));
            for (JButton page : group) {
                styleNavButton(page, page.getText(), false);
                navigation.add(page);
                navigation.add(Box.createVerticalStrut(5));
            }
            navigation.add(Box.createVerticalStrut(8));
        }
        navigation.add(Box.createVerticalGlue());

        JScrollPane navigationScroll = new JScrollPane(navigation,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        navigationScroll.setBorder(null);
        navigationScroll.setOpaque(false);
        navigationScroll.getViewport().setOpaque(false);
        navigationScroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        navigationScroll.getVerticalScrollBar().setUnitIncrement(16);

        // ---- branding: paw badge + name, with the collapse toggle
        JPanel branding = new JPanel(new BorderLayout(0, 9));
        branding.setOpaque(false);
        branding.setBorder(BorderFactory.createEmptyBorder(18, 13, 12, 13));
        JPanel brandBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brandBox.setOpaque(false);
        brandBox.add(new PawBadge());
        JLabel logo = new JLabel("PAWTRACK");
        logo.setName("navigationBrand");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        logo.setForeground(Color.WHITE);
        brandBox.add(logo);
        branding.add(brandBox, BorderLayout.CENTER);
        JButton toggle = new JButton("<");
        toggle.setOpaque(true);
        toggle.setBackground(new Color(0x416CAC));
        toggle.setBorderPainted(false);
        toggle.setContentAreaFilled(true);
        toggle.setName("navigationCollapseToggle");
        toggle.setToolTipText("Collapse sidebar");
        toggle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        toggle.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        toggle.setForeground(Color.WHITE);
        toggle.setFocusPainted(false);
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggle.setPreferredSize(new Dimension(42, 38));
        toggle.setMinimumSize(new Dimension(42, 38));
        branding.add(toggle, BorderLayout.EAST);
        add(branding, BorderLayout.NORTH);
        add(navigationScroll, BorderLayout.CENTER);

        // ---- footer: Log out + workspace chip
        JPanel footer = new JPanel(new BorderLayout(0, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, DIVIDER),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        final LogoutButton logoutButton = onLogout == null ? null : new LogoutButton();
        if (logoutButton != null) {
            logoutButton.addActionListener(e -> confirmLogout());
            footer.add(logoutButton, BorderLayout.CENTER);
        }
        JPanel chipHolder = new JPanel(new BorderLayout());
        chipHolder.setOpaque(false);
        chipHolder.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        chipHolder.add(new WorkspaceChip(staff ? "SHELTER WORKSPACE" : "ADOPTER WORKSPACE"), BorderLayout.CENTER);
        footer.add(chipHolder, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);

        toggle.addActionListener(e -> {
            collapsed[0] = !collapsed[0];
            boolean c = collapsed[0];
            toggle.setText(c ? ">" : "<");
            toggle.setToolTipText(c ? "Expand sidebar" : "Collapse sidebar");
            setPreferredSize(new Dimension(c ? COLLAPSED_WIDTH : EXPANDED_WIDTH, 0));
            branding.setBorder(BorderFactory.createEmptyBorder(18, c ? 11 : 13, 12, c ? 11 : 13));
            brandBox.setVisible(!c);
            chipHolder.setVisible(!c);
            for (JComponent heading : sectionHeadings) heading.setVisible(!c);
            for (int i = 0; i < pages.size(); i++) {
                styleNavButton(pages.get(i), labels[i], c);
            }
            if (logoutButton != null) logoutButton.setCollapsed(c);
            revalidate();
            if (getParent() != null) getParent().revalidate();
            repaint();
        });
    }

    private void confirmLogout() {
        if (onLogout == null) return;
        if (PawTrackSimpleDialog.confirm(SwingUtilities.getWindowAncestor(this),
                "Log out", "Are you sure you want to log out?")) onLogout.run();
    }

    // ------------------------------------------------ legacy (flat) layout

    private void installLegacyNavigation(List<? extends JButton> pages, JScrollPane actions) {
        setPreferredSize(new Dimension(290, 0));
        setMinimumSize(new Dimension(225, 0));
        JPanel navigation = new JPanel();
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));
        navigation.setOpaque(true);
        navigation.setBackground(NAVY);
        navigation.setBorder(BorderFactory.createEmptyBorder(18, 12, 14, 12));
        JLabel title = new JLabel("PAGES");
        title.setFont(new Font("Segoe UI", Font.BOLD, 11));
        title.setForeground(HEADING);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        navigation.add(title);
        navigation.add(Box.createVerticalStrut(12));
        for (JButton button : pages) {
            styleNavButton(button, button.getText(), false);
            button.setPreferredSize(new Dimension(264, 42));
            navigation.add(button);
            navigation.add(Box.createVerticalStrut(7));
        }
        JScrollPane navigationScroll = new JScrollPane(navigation,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        navigationScroll.setBorder(null);
        navigationScroll.setOpaque(false);
        navigationScroll.getViewport().setBackground(NAVY);
        navigationScroll.getVerticalScrollBar().setUnitIncrement(16);
        navigationScroll.setPreferredSize(new Dimension(290, 248));
        add(navigationScroll, BorderLayout.NORTH);

        JPanel actionsHolder = new JPanel(new BorderLayout());
        actionsHolder.setOpaque(false);
        actionsHolder.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));
        JLabel toolsHeading = new JLabel("  TOOLS & ACTIONS");
        toolsHeading.setFont(new Font("Segoe UI", Font.BOLD, 11));
        toolsHeading.setForeground(HEADING);
        toolsHeading.setBorder(BorderFactory.createEmptyBorder(0, 8, 9, 0));
        actionsHolder.add(toolsHeading, BorderLayout.NORTH);
        actions.setBorder(null);
        actions.getVerticalScrollBar().setUnitIncrement(16);
        actions.getViewport().setBackground(new Color(0xA5C9E7));
        actionsHolder.add(actions, BorderLayout.CENTER);
        add(actionsHolder, BorderLayout.CENTER);
    }

    // --------------------------------------------------------- small pieces

    /** Rounded paw badge, same look as the header logo. */
    private static final class PawBadge extends JComponent {
        PawBadge() { setPreferredSize(new Dimension(37, 37)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D gg = (Graphics2D) g.create();
            gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gg.setColor(new Color(0xE5EEFD));
            gg.fillRoundRect(0, 0, 37, 37, 12, 12);
            gg.setColor(BLUE);
            paw(gg, 0, 0, 1.0);
            gg.dispose();
        }
    }

    /** Section title followed by a thin rule, e.g. "MANAGE ────". */
    private static final class SectionHeading extends JPanel {
        SectionHeading(String text) {
            super(new BorderLayout(8, 0));
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setBorder(BorderFactory.createEmptyBorder(6, 8, 0, 6));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
            JLabel label = new JLabel(text);
            label.setFont(new Font("Segoe UI", Font.BOLD, 10));
            label.setForeground(HEADING);
            add(label, BorderLayout.WEST);
            JComponent rule = new JComponent() {
                @Override protected void paintComponent(Graphics g) {
                    g.setColor(new Color(255, 255, 255, 38));
                    g.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
                }
            };
            rule.setPreferredSize(new Dimension(10, 12));
            add(rule, BorderLayout.CENTER);
        }
    }

    /** Small rounded pill with a green status dot, e.g. "● SHELTER WORKSPACE". */
    private static final class WorkspaceChip extends JPanel {
        WorkspaceChip(String text) {
            super(new FlowLayout(FlowLayout.LEFT, 8, 7));
            setOpaque(false);
            JComponent dot = new JComponent() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D gg = (Graphics2D) g.create();
                    gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    gg.setColor(new Color(0x3DDC97));
                    gg.fillOval(0, 0, 8, 8);
                    gg.dispose();
                }
            };
            dot.setPreferredSize(new Dimension(8, 8));
            add(dot);
            JLabel label = new JLabel(text);
            label.setFont(new Font("Segoe UI", Font.BOLD, 10));
            label.setForeground(ICON);
            add(label);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D gg = (Graphics2D) g.create();
            gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gg.setColor(new Color(255, 255, 255, 20));
            gg.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            gg.setColor(new Color(255, 255, 255, 34));
            gg.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            gg.dispose();
            super.paintComponent(g);
        }
    }

    // ------------------------------------------------------- log out button

    private static final class LogoutButton extends JButton {
        private boolean hover;

        LogoutButton() {
            super("Log out");
            setIcon(new NavigationIcon("Log out", LOGOUT_ICON));
            setIconTextGap(12);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(220, NAV_HEIGHT));
            setMinimumSize(new Dimension(48, NAV_HEIGHT));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, NAV_HEIGHT));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        void setCollapsed(boolean collapsed) {
            setText(collapsed ? "" : "Log out");
            setToolTipText(collapsed ? "Log out" : null);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D gg = (Graphics2D) g.create();
            gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gg.setColor(hover ? new Color(0xA63A3A) : new Color(0x8A2B2B));
            gg.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            gg.dispose();
            super.paintComponent(g);
        }
    }

    // ---------------------------------------------------------------- icons

    /** Tiny vector navigation symbols avoid emoji/tofu glyphs on Windows. */
    private static final class NavigationIcon implements Icon {
        private final String title;
        private final Color color;

        NavigationIcon(String title, Color color) {
            this.title = title;
            this.color = color;
        }

        @Override public int getIconWidth() { return 19; }
        @Override public int getIconHeight() { return 19; }

        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D gg = (Graphics2D) g.create();
            gg.translate(x, y);
            gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gg.setColor(color);
            gg.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (title) {
                case "Dashboard" -> {
                    gg.fillRoundRect(1, 1, 7, 7, 2, 2);
                    gg.fillRoundRect(11, 1, 7, 7, 2, 2);
                    gg.fillRoundRect(1, 11, 7, 7, 2, 2);
                    gg.fillRoundRect(11, 11, 7, 7, 2, 2);
                }
                case "Animals" -> {
                    gg.fillOval(6, 9, 9, 8); gg.fillOval(2, 5, 4, 5);
                    gg.fillOval(8, 1, 4, 5); gg.fillOval(14, 4, 4, 5);
                }
                case "My Records" -> {
                    gg.drawRoundRect(3, 3, 13, 15, 3, 3);
                    gg.fillRoundRect(7, 1, 5, 4, 2, 2);
                    gg.drawLine(6, 9, 13, 9); gg.drawLine(6, 13, 11, 13);
                }
                case "Shelter Analytics", "Analytics" -> {
                    gg.drawLine(2, 16, 2, 5); gg.drawLine(2, 16, 18, 16);
                    gg.fillRect(5, 10, 3, 5); gg.fillRect(10, 6, 3, 9);
                    gg.fillRect(15, 3, 3, 12);
                }
                case "Waitlist Requests" -> {
                    for (int row = 0; row < 3; row++) {
                        int cy = 4 + row * 5;
                        gg.fillOval(1, cy - 2, 4, 4);
                        gg.drawLine(8, cy, 17, cy);
                    }
                }
                case "Application History" -> {
                    gg.drawOval(2, 2, 15, 15);
                    gg.drawLine(9, 5, 9, 10); gg.drawLine(9, 10, 13, 12);
                }
                case "Activity Log" ->
                    gg.drawPolyline(new int[]{1, 5, 8, 11, 14, 18}, new int[]{10, 10, 3, 16, 10, 10}, 6);
                case "Import Records" -> {
                    gg.drawLine(9, 2, 9, 11);
                    gg.drawLine(5, 8, 9, 12); gg.drawLine(13, 8, 9, 12);
                    gg.drawPolyline(new int[]{3, 3, 16, 16}, new int[]{12, 16, 16, 12}, 4);
                }
                case "Tools & Settings" -> {
                    gg.drawLine(2, 4, 17, 4);   gg.fillOval(10, 2, 5, 5);
                    gg.drawLine(2, 10, 17, 10); gg.fillOval(4, 8, 5, 5);
                    gg.drawLine(2, 16, 17, 16); gg.fillOval(9, 14, 5, 5);
                }
                case "Account Settings" -> {
                    gg.fillOval(6, 2, 7, 7);
                    gg.drawArc(2, 11, 15, 14, 0, 180);
                }
                case "Log out" -> {
                    gg.drawPolyline(new int[]{9, 2, 2, 9}, new int[]{2, 2, 17, 17}, 4);
                    gg.drawLine(8, 10, 17, 10);
                    gg.drawLine(13, 6, 17, 10); gg.drawLine(13, 14, 17, 10);
                }
                default -> {
                    gg.drawRoundRect(2, 2, 15, 15, 3, 3);
                    gg.drawLine(5, 7, 15, 7); gg.drawLine(5, 11, 13, 11);
                }
            }
            gg.dispose();
        }
    }
}
