package pawtrack.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Staff-only presentation of the existing in-session activity messages.
 * Does not invent audit records, query private adopter data, or claim to be a
 * persistent/immutable audit system. MainFrame remains the sole event source.
 */
public final class PawTrackActivityLogPanel extends JPanel {
    private static final Color NAVY = PawTrackTheme.NAVY;
    private static final Color BLUE = PawTrackTheme.BLUE;
    private static final Color MUTED = PawTrackTheme.MUTED;
    private static final Color CANVAS = PawTrackTheme.CANVAS;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.ENGLISH);
    private final List<Entry> entries = new ArrayList<>();
    private final JPanel feed = new JPanel();
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Time", "Event", "Details"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTextField search = new JTextField();
    private final JComboBox<String> kind = new JComboBox<>(new String[]{"All event types", "Animal activity", "Adoptions & waitlist", "Imports", "System"});
    private final JLabel countAll = new JLabel("0");
    private final JLabel countAnimal = new JLabel("0");
    private final JLabel countAdoption = new JLabel("0");
    private final JLabel countSystem = new JLabel("0");
    private final JTextArea latest = new JTextArea("No recorded events yet");
    private final JLabel latestTime = new JLabel(" ");
    private final JLabel footer = new JLabel("Showing 0 of 0 in-session events");
    private final JTable details = new JTable(model);

    private static final class Entry {
        final LocalDateTime time = LocalDateTime.now();
        final String message, type;
        Entry(String message) { this.message = message; this.type = classify(message); }
    }

    public PawTrackActivityLogPanel() {
        super(new BorderLayout());
        setBackground(CANVAS);
        ScrollColumn root = new ScrollColumn();
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(17, 22, 20, 22));
        JScrollPane outer = new JScrollPane(root, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outer.setBorder(null);
        outer.getViewport().setBackground(CANVAS);
        outer.getVerticalScrollBar().setUnitIncrement(18);
        add(outer, BorderLayout.CENTER);

        JPanel hero = new SoftCard(PawTrackTheme.HERO_START, PawTrackTheme.HERO_END, 23);
        hero.setLayout(new BorderLayout(15, 0));
        hero.setBorder(new EmptyBorder(13, 22, 13, 22));
        JPanel heroText = new JPanel(new GridLayout(3, 1, 0, 3));
        heroText.setOpaque(false);
        heroText.add(label("STAFF WORKSPACE  /  ACTIVITY CENTER", 11, true, new Color(0xC4DCFF)));
        heroText.add(label("Activity Log", 27, true, Color.WHITE));
        heroText.add(label("Recent actions, operational updates and session events in one place.", 12, false, new Color(0xE9F2FF)));
        hero.add(heroText, BorderLayout.CENTER);
        // A painted dot replaces the unsupported Unicode symbol that rendered as a square.
        JPanel liveBadge = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(255, 255, 255, 32));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g.setColor(new Color(0x89F0BD));
                g.fillOval(12, (getHeight() - 8) / 2, 8, 8);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        liveBadge.setOpaque(false);
        liveBadge.setBorder(new EmptyBorder(0, 20, 0, 12));
        liveBadge.add(label("SESSION UPDATES", 11, true, Color.WHITE));
        JPanel badgeHolder = new JPanel(new GridBagLayout());
        badgeHolder.setOpaque(false);
        badgeHolder.add(liveBadge);
        hero.add(badgeHolder, BorderLayout.EAST);
        hero.setPreferredSize(new Dimension(1060, 104));
        addTo(root, hero, 13);

        JPanel metrics = new JPanel(new GridLayout(1, 4, 13, 0));
        metrics.setOpaque(false);
        metrics.add(metric("ALL EVENTS", countAll, "This application session", BLUE, new Color(0xE5EEFF)));
        metrics.add(metric("ANIMAL EVENTS", countAnimal, "Animals and records", new Color(0xD65785), new Color(0xFFEAF2)));
        metrics.add(metric("ADOPTION UPDATES", countAdoption, "Interest and adoptions", new Color(0x7460C8), new Color(0xEEEAFF)));
        metrics.add(metric("SYSTEM / IMPORT", countSystem, "Operational messages", new Color(0x14977C), new Color(0xE0F7EF)));
        metrics.setPreferredSize(new Dimension(1060, 112));
        addTo(root, metrics, 13);

        JPanel feedAndInfo = new JPanel(new BorderLayout(13, 0));
        feedAndInfo.setOpaque(false);
        JPanel feedCard = new WhiteCard(new BorderLayout(0, 10));
        feedCard.setBorder(new EmptyBorder(18, 18, 16, 18));
        JPanel tools = new JPanel(new BorderLayout(8, 10));
        tools.setOpaque(false);
        tools.add(label("Activity Feed", 20, true, NAVY), BorderLayout.NORTH);
        JPanel filters = new JPanel(new BorderLayout(8, 0));
        filters.setOpaque(false);
        search.setFont(font(12, false));
        search.setToolTipText("Search activity messages");
        search.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xC6D9F6)), new EmptyBorder(9, 12, 9, 12)));
        filters.add(search, BorderLayout.CENTER);
        kind.setPreferredSize(new Dimension(166, 36));
        kind.setBackground(Color.WHITE);
        kind.setFont(font(11, false));
        filters.add(kind, BorderLayout.EAST);
        tools.add(filters, BorderLayout.CENTER);
        feedCard.add(tools, BorderLayout.NORTH);
        feed.setOpaque(false);
        feed.setLayout(new BoxLayout(feed, BoxLayout.Y_AXIS));
        JScrollPane feedScroll = new JScrollPane(feed, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        feedScroll.setBorder(null);
        feedScroll.getViewport().setBackground(Color.WHITE);
        feedScroll.getVerticalScrollBar().setUnitIncrement(16);
        feedCard.add(feedScroll, BorderLayout.CENTER);
        feedAndInfo.add(feedCard, BorderLayout.CENTER);

        JPanel aside = new JPanel(new GridLayout(2, 1, 0, 13));
        aside.setOpaque(false);
        JPanel lastCard = new WhiteCard(new BorderLayout(0, 12));
        lastCard.setBorder(new EmptyBorder(18, 17, 18, 17));
        lastCard.add(label("Latest Event", 18, true, NAVY), BorderLayout.NORTH);
        JPanel lastBody = new JPanel(new BorderLayout(0, 9));
        lastBody.setOpaque(false);
        latest.setFont(font(13, true));
        latest.setForeground(NAVY);
        latest.setEditable(false);
        latest.setFocusable(false);
        latest.setLineWrap(true);
        latest.setWrapStyleWord(true);
        latest.setOpaque(false);
        latest.setBorder(null);
        // The full event remains readable even for long messages.
        JScrollPane latestScroll = new JScrollPane(latest,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        latestScroll.setOpaque(false);
        latestScroll.getViewport().setOpaque(false);
        latestScroll.setBorder(null);
        latestScroll.getVerticalScrollBar().setUnitIncrement(12);
        lastBody.add(latestScroll, BorderLayout.CENTER);
        latestTime.setFont(font(11, false));
        latestTime.setForeground(MUTED);
        JPanel lastFooter = new JPanel(new GridLayout(2, 1, 0, 4));
        lastFooter.setOpaque(false);
        lastFooter.add(latestTime);
        lastFooter.add(label("Select a feed item to inspect its message.", 11, false, MUTED));
        lastBody.add(lastFooter, BorderLayout.SOUTH);
        lastCard.add(lastBody, BorderLayout.CENTER);
        aside.add(lastCard);
        JPanel noteCard = new SoftCard(new Color(0xE7F0FF), new Color(0xEDF9F6), 18);
        noteCard.setLayout(new BorderLayout(0, 10));
        noteCard.setBorder(new EmptyBorder(18, 17, 18, 17));
        noteCard.add(label("About this activity feed", 17, true, NAVY), BorderLayout.NORTH);
        JTextArea note = new JTextArea("Shows messages recorded during this PawTrack session. This is not a persistent audit history.\n\nUse search and event types to find updates.");
        note.setOpaque(false); note.setEditable(false); note.setLineWrap(true); note.setWrapStyleWord(true);
        note.setFont(font(12, false)); note.setForeground(MUTED);
        noteCard.add(note, BorderLayout.CENTER);
        aside.add(noteCard);
        aside.setPreferredSize(new Dimension(300, 400));
        feedAndInfo.add(aside, BorderLayout.EAST);
        feedAndInfo.setPreferredSize(new Dimension(1060, 397));
        addTo(root, feedAndInfo, 13);

        JPanel tableCard = new WhiteCard(new BorderLayout(0, 11));
        tableCard.setBorder(new EmptyBorder(17, 18, 15, 18));
        JPanel tableTop = new JPanel(new BorderLayout()); tableTop.setOpaque(false);
        tableTop.add(label("Event Details", 19, true, NAVY), BorderLayout.WEST);
        footer.setFont(font(11, false)); footer.setForeground(MUTED);
        tableTop.add(footer, BorderLayout.EAST);
        tableCard.add(tableTop, BorderLayout.NORTH);
        details.setFont(font(12, false)); details.setForeground(NAVY);
        details.setRowHeight(32); details.setGridColor(new Color(0xE0EAF8));
        details.setShowVerticalLines(false); details.setSelectionBackground(new Color(0xDCEBFF));
        details.setSelectionForeground(NAVY);
        details.getTableHeader().setFont(font(12, true));
        details.getTableHeader().setBackground(new Color(0xE7F0FF));
        details.getTableHeader().setForeground(NAVY);
        details.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focused, row, column);
                setFont(font(12, true));
                setBackground(PawTrackTheme.isDark() ? PawTrackTheme.DARK_SIDEBAR : new Color(0xE7F0FF));
                setForeground(PawTrackTheme.isDark() ? PawTrackTheme.DARK_TEXT : NAVY);
                setBorder(new EmptyBorder(0, 9, 0, 9));
                setOpaque(true);
                return this;
            }
        });
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setBorder(new EmptyBorder(0, 9, 0, 9));
        details.setDefaultRenderer(Object.class, renderer);
        details.getColumnModel().getColumn(0).setPreferredWidth(150);
        details.getColumnModel().getColumn(1).setPreferredWidth(170);
        details.getColumnModel().getColumn(2).setPreferredWidth(680);
        JScrollPane tableScroll = new JScrollPane(details);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(0xE0EAF8)));
        tableCard.add(tableScroll, BorderLayout.CENTER);
        tableCard.setPreferredSize(new Dimension(1060, 210));
        addTo(root, tableCard, 0);

        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(); }
            public void removeUpdate(DocumentEvent e) { refresh(); }
            public void changedUpdate(DocumentEvent e) { refresh(); }
        });
        kind.addActionListener(e -> refresh());
        refresh();
    }

    private static String classify(String message) {
        String s = message.toLowerCase(Locale.ROOT);
        if (s.contains("adopt") || s.contains("waitlist") || s.contains("interest") || s.contains("reserv") || s.contains("application")) return "Adoptions & waitlist";
        if (s.contains("animal") || s.contains("classification") || s.contains("status change") || s.contains("pet ")) return "Animal activity";
        if (s.contains("import") || s.contains("csv")) return "Imports";
        return "System";
    }

    /** Adds one REAL application message, without changing persistence or logging services. */
    public void appendEvent(String message) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> appendEvent(message)); return;
        }
        if (message == null || message.isBlank()) return;
        entries.add(new Entry(message));
        refresh();
    }

    public int eventCount() { return entries.size(); }

    private void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh); return;
        }
        int a = 0, adoption = 0, system = 0;
        for (Entry e : entries) {
            if ("Animal activity".equals(e.type)) a++;
            else if ("Adoptions & waitlist".equals(e.type)) adoption++;
            else system++;
        }
        countAll.setText(Integer.toString(entries.size()));
        countAnimal.setText(Integer.toString(a));
        countAdoption.setText(Integer.toString(adoption));
        countSystem.setText(Integer.toString(system));
        feed.removeAll();
        model.setRowCount(0);
        String text = search.getText().trim().toLowerCase(Locale.ROOT);
        String selected = String.valueOf(kind.getSelectedItem());
        int shown = 0;
        List<Entry> reverse = new ArrayList<>(entries);
        Collections.reverse(reverse);
        for (Entry e : reverse) {
            if (!selected.equals("All event types") &&
                    !selected.equals(e.type) && !(selected.equals("System") && e.type.equals("System"))) continue;
            if (!text.isEmpty() && !e.message.toLowerCase(Locale.ROOT).contains(text) && !e.type.toLowerCase(Locale.ROOT).contains(text)) continue;
            JPanel row = new SoftCard(shown % 2 == 0 ? new Color(0xEFF5FF) : new Color(0xFFF4F9), Color.WHITE, 15);
            row.setLayout(new BorderLayout(13, 4));
            row.setBorder(new EmptyBorder(9, 12, 9, 12));
            JLabel icon = label(e.type.equals("Adoptions & waitlist") ? "♥" : e.type.equals("Animal activity") ? "●" : e.type.equals("Imports") ? "↓" : "✦", 20, true,
                    e.type.equals("Adoptions & waitlist") ? new Color(0xD65186) : BLUE);
            row.add(icon, BorderLayout.WEST);
            JLabel description = label(e.message, 12, true, NAVY);
            description.setToolTipText(e.message);
            row.add(description, BorderLayout.CENTER);
            row.add(label(e.time.format(CLOCK), 11, false, MUTED), BorderLayout.EAST);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
            row.setPreferredSize(new Dimension(400, 52));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            final Entry chosen = e;
            row.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent evt) { select(chosen); }
            });
            feed.add(row);
            feed.add(Box.createVerticalStrut(6));
            model.addRow(new Object[]{e.time.format(CLOCK), e.type, e.message});
            shown++;
        }
        if (shown == 0) {
            JLabel empty = label("No matching activity yet. Actions performed in this session appear here.", 12, false, MUTED);
            empty.setBorder(new EmptyBorder(25, 14, 25, 14));
            feed.add(empty);
        }
        footer.setText("Showing " + shown + " of " + entries.size() + " in-session events");
        if (!entries.isEmpty()) select(entries.get(entries.size() - 1));
        // Entries are rebuilt on every event or filter change. They must be
        // themed after insertion, not only when the main window first opens.
        if (PawTrackTheme.isDark()) PawTrackThemeManager.applyCurrent(feed);
        feed.revalidate(); feed.repaint();
    }

    private void select(Entry entry) {
        latest.setText(entry.message);
        latest.setCaretPosition(0);
        latest.setToolTipText(entry.message);
        latestTime.setText(entry.type + "  •  " + entry.time.format(CLOCK));
    }

    private static void addTo(JPanel parent, JComponent child, int gap) {
        child.setAlignmentX(Component.LEFT_ALIGNMENT);
        child.setMaximumSize(new Dimension(Integer.MAX_VALUE, child.getPreferredSize().height));
        parent.add(child);
        if (gap > 0) parent.add(Box.createVerticalStrut(gap));
    }

    private static JLabel label(String text, int size, boolean bold, Color color) {
        JLabel result = new JLabel(text);
        result.setFont(font(size, bold)); result.setForeground(color); return result;
    }
    private static Font font(int size, boolean bold) { return new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size); }

    private static JPanel metric(String title, JLabel value, String detail, Color accent, Color tint) {
        JPanel card = new SoftCard(Color.WHITE, tint, 20);
        card.setLayout(new BorderLayout()); card.setBorder(new EmptyBorder(15, 16, 12, 14));
        JPanel lines = new JPanel(new GridLayout(3, 1, 0, 2)); lines.setOpaque(false);
        lines.add(label(title, 11, true, MUTED));
        value.setFont(font(28, true)); value.setForeground(NAVY); lines.add(value);
        lines.add(label(detail, 11, false, accent)); card.add(lines, BorderLayout.CENTER);
        return card;
    }
    private static final class ScrollColumn extends JPanel implements Scrollable {
        ScrollColumn() { setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            PawTrackBackdrop.paint(graphics, this);
        }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 20; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return Math.max(100, visibleRect.height - 55); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
    private static final class WhiteCard extends JPanel {
        WhiteCard(LayoutManager layout) { super(layout); setBackground(Color.WHITE); }
    }
    private static final class SoftCard extends JPanel {
        private final Color left, right; private final int radius;
        SoftCard(Color left, Color right, int radius) { this.left = left; this.right = right; this.radius = radius; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, PawTrackTheme.fill(left), getWidth(), getHeight(), PawTrackTheme.fill(right)));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.dispose(); super.paintComponent(g);
        }
    }
}
