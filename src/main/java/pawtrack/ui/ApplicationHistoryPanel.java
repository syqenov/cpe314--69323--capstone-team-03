package pawtrack.ui;

import pawtrack.service.ApplicationHistoryService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Read-only, staff-only presentation of the existing SQLite reservation history. */
public final class ApplicationHistoryPanel extends JPanel {
    @Override protected void paintComponent(java.awt.Graphics graphics) {
        super.paintComponent(graphics);
        PawTrackBackdrop.paint(graphics, this);
    }

    private static final Color NAVY = PawTrackTheme.NAVY;
    private static final Color BLUE = PawTrackTheme.BLUE;
    private static final Color MUTED = PawTrackTheme.MUTED;
    private static final Color CANVAS = PawTrackTheme.CANVAS;
    private static final Color BORDER = PawTrackTheme.LINE;
    private static final Font REGULAR = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font SEMIBOLD = new Font("Segoe UI", Font.BOLD, 12);

    private final ApplicationHistoryService service;
    // Keep the same six columns and their original record mapping.
    private final DefaultTableModel model = new DefaultTableModel(
        new String[] { "ID", "Animal", "Applicant", "Status", "Reserved at", "Deadline" }, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
        @Override public Class<?> getColumnClass(int column) { return column == 0 ? Integer.class : String.class; }
    };
    private final JTable table = new JTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final JTextField search = new JTextField() {
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (!getText().isEmpty() || isFocusOwner()) return;
            Graphics2D g = (Graphics2D) graphics.create();
            g.setColor(new Color(0x8195B3));
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            g.drawString("Search animal, applicant, ID, or status...", 3,
                (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    };
    private final JComboBox<String> statusFilter = new JComboBox<>(
        new String[] { "All statuses", "COMPLETED", "RESERVED", "CANCELLED", "EXPIRED", "WAITING", "IN_REVIEW", "APPROVED" });
    private final JLabel totalValue = new JLabel("0");
    private final JLabel completedValue = new JLabel("0");
    private final JLabel cancelledValue = new JLabel("0");
    private final JLabel otherValue = new JLabel("0");
    private final JLabel showingValue = new JLabel("No history loaded");
    private final JLabel infoValue = new JLabel("Reservation activity stored in PawTrack");

    public ApplicationHistoryPanel(ApplicationHistoryService service) {
        super(new BorderLayout());
        this.service = Objects.requireNonNull(service, "service");
        setBackground(CANVAS);
        setBorder(new EmptyBorder(14, 17, 14, 17));

        JPanel page = new JPanel(new BorderLayout(0, 13));
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(0, 0, 0, 3));
        add(page, BorderLayout.CENTER);

        JPanel top = clear(new BorderLayout(0, 12));
        top.add(buildHero(), BorderLayout.NORTH);
        top.add(buildStats(), BorderLayout.CENTER);
        page.add(top, BorderLayout.NORTH);

        JPanel historyCard = roundedPanel(Color.WHITE, BORDER, 21);
        historyCard.setLayout(new BorderLayout(0, 12));
        historyCard.setBorder(new EmptyBorder(17, 18, 14, 18));
        historyCard.add(buildToolbar(), BorderLayout.NORTH);
        historyCard.add(buildTable(), BorderLayout.CENTER);
        page.add(historyCard, BorderLayout.CENTER);

        // Scroll the whole page rather than shrinking table rows when the window is short.
        JPanel footer = clear(new BorderLayout());
        footer.setBorder(new EmptyBorder(1, 4, 1, 4));
        infoValue.setFont(REGULAR);
        infoValue.setForeground(MUTED);
        footer.add(infoValue, BorderLayout.WEST);
        page.add(footer, BorderLayout.SOUTH);

        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });
        statusFilter.addActionListener(e -> applyFilters());
        refresh();
    }

    private JComponent buildHero() {
        JPanel hero = new JPanel(new BorderLayout(16, 0)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setPaint(new GradientPaint(0, 0, PawTrackTheme.HERO_START, getWidth(), getHeight(), PawTrackTheme.HERO_END));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g.setColor(new Color(255, 255, 255, 21));
                g.fillOval(getWidth() - 260, -100, 310, 200);
                g.fillOval(getWidth() - 160, 50, 205, 120);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        hero.setOpaque(false);
        hero.setPreferredSize(new Dimension(200, 130));
        hero.setBorder(new EmptyBorder(18, 22, 18, 22));
        JLabel emblem = new JLabel(new DrawnIcon("clock", Color.WHITE, 44));
        emblem.setHorizontalAlignment(SwingConstants.CENTER);
        emblem.setPreferredSize(new Dimension(66, 70));
        hero.add(emblem, BorderLayout.WEST);

        // Avoid GridLayout here: fixed equal-height cells clip descenders (p, g, y).
        JPanel text = clear(new GridBagLayout());
        GridBagConstraints tc = new GridBagConstraints();
        tc.gridx = 0; tc.anchor = GridBagConstraints.WEST; tc.fill = GridBagConstraints.HORIZONTAL;
        tc.weightx = 1.0;
        tc.gridy = 0; tc.insets = new Insets(0, 0, 5, 0);
        text.add(label("SHELTER WORKSPACE  /  RESERVATION RECORDS", 10, true, new Color(0xCFDEFF)), tc);
        tc.gridy = 1; tc.insets = new Insets(0, 0, 6, 0);
        JLabel historyTitle = label("Application History", 27, true, Color.WHITE);
        historyTitle.setBorder(new EmptyBorder(1, 0, 4, 0));
        text.add(historyTitle, tc);
        tc.gridy = 2; tc.insets = new Insets(0, 0, 0, 0);
        text.add(label("Explore completed, cancelled and past adoption reservations.", 12, false, new Color(0xEBF3FF)), tc);
        hero.add(text, BorderLayout.CENTER);
        JLabel tag = label("PAWTRACK  •  HISTORY", 11, true, Color.WHITE);
        tag.setHorizontalAlignment(SwingConstants.RIGHT);
        hero.add(tag, BorderLayout.EAST);
        return hero;
    }

    private JComponent buildStats() {
        JPanel cards = clear(new GridLayout(1, 4, 12, 0));
        cards.setPreferredSize(new Dimension(0, 105));
        cards.add(stat("TOTAL RECORDS", totalValue, "Reservations in history", "document", new Color(0x326FCB), new Color(0xE3EFFF)));
        cards.add(stat("COMPLETED", completedValue, "Adoptions finalized", "check", new Color(0x14875B), new Color(0xE1F7EC)));
        cards.add(stat("CANCELLED", cancelledValue, "Reservations cancelled", "cross", new Color(0xC84963), new Color(0xFFE8EE)));
        cards.add(stat("OTHER STATUS", otherValue, "Remaining reservation states", "clock", new Color(0x7752C8), new Color(0xEDE8FD)));
        return cards;
    }

    private JComponent stat(String title, JLabel value, String subtitle, String glyph, Color accent, Color tint) {
        JPanel p = roundedPanel(tint, null, 20);
        p.setLayout(new BorderLayout(10, 0));
        p.setBorder(new EmptyBorder(12, 14, 12, 9));
        JLabel icon = new JLabel(new DrawnIcon(glyph, accent, 30));
        icon.setPreferredSize(new Dimension(46, 48));
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(icon, BorderLayout.WEST);
        JPanel words = clear(new GridLayout(3, 1, 0, 1));
        words.add(label(title, 10, true, accent));
        value.setFont(new Font("Segoe UI", Font.BOLD, 24));
        value.setForeground(NAVY);
        words.add(value);
        words.add(label(subtitle, 10, false, MUTED));
        p.add(words, BorderLayout.CENTER);
        return p;
    }

    private JComponent buildToolbar() {
        JPanel all = clear(new BorderLayout(0, 12));
        JPanel titles = clear(new BorderLayout());
        JPanel heading = clear(new GridLayout(2, 1, 0, 2));
        heading.add(label("Reservation timeline", 19, true, NAVY));
        heading.add(label("Review the history of adoption reservations", 12, false, MUTED));
        titles.add(heading, BorderLayout.WEST);
        JButton refresh = new PrimaryButton("Refresh history", new DrawnIcon("refresh", Color.WHITE, 17));
        refresh.setName("historyRefresh");
        refresh.setFont(SEMIBOLD);
        refresh.setForeground(Color.WHITE);
        refresh.setBackground(BLUE);
        refresh.setOpaque(false);
        refresh.setFocusPainted(false);
        refresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refresh.setBorder(new EmptyBorder(11, 18, 11, 18));
        refresh.setPreferredSize(new Dimension(166, 43));
        refresh.addActionListener(e -> refresh());
        titles.add(refresh, BorderLayout.EAST);
        all.add(titles, BorderLayout.NORTH);

        JPanel controls = clear(new BorderLayout(12, 0));
        JPanel searchCard = roundedPanel(new Color(0xF7FAFF), BORDER, 17);
        searchCard.setLayout(new BorderLayout(8, 0));
        searchCard.setBorder(new EmptyBorder(6, 13, 6, 11));
        JLabel magnifier = new JLabel(new DrawnIcon("search", BLUE, 20));
        magnifier.setPreferredSize(new Dimension(24, 28));
        searchCard.add(magnifier, BorderLayout.WEST);
        search.setName("historySearch");
        search.setBorder(BorderFactory.createEmptyBorder());
        search.setOpaque(false);
        search.setFont(REGULAR);
        search.putClientProperty("JTextField.placeholderText", "Search animal, applicant, ID, or status...");
        search.setToolTipText("Search animal, applicant, ID, or status");
        searchCard.add(search, BorderLayout.CENTER);
        controls.add(searchCard, BorderLayout.CENTER);
        statusFilter.setName("historyStatusFilter");
        statusFilter.setFont(SEMIBOLD);
        statusFilter.setBackground(Color.WHITE);
        statusFilter.setForeground(NAVY);
        statusFilter.setPreferredSize(new Dimension(188, 42));
        controls.add(statusFilter, BorderLayout.EAST);
        all.add(controls, BorderLayout.CENTER);
        showingValue.setForeground(MUTED);
        showingValue.setFont(REGULAR);
        all.add(showingValue, BorderLayout.SOUTH);
        return all;
    }

    private JComponent buildTable() {
        table.setName("applicationHistoryTable");
        table.setRowHeight(53);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setForeground(NAVY);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(0xDCEBFF));
        table.setSelectionForeground(NAVY);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setRowSorter(sorter);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 44));
        table.getTableHeader().setDefaultRenderer(new TableHeader());
        table.getColumnModel().getColumn(0).setPreferredWidth(58);
        table.getColumnModel().getColumn(1).setPreferredWidth(170);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);
        table.getColumnModel().getColumn(3).setPreferredWidth(176);
        table.getColumnModel().getColumn(4).setPreferredWidth(205);
        table.getColumnModel().getColumn(5).setPreferredWidth(205);
        for (int c = 0; c < table.getColumnCount(); c++) {
            table.getColumnModel().getColumn(c).setCellRenderer(c == 3 ? new StatusBadge() : new HistoryCell(c));
        }
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(22);
        scroll.setPreferredSize(new Dimension(800, 270));
        return scroll;
    }

    private void applyFilters() {
        String query = search.getText() == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        String status = Objects.toString(statusFilter.getSelectedItem(), "All statuses");
        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> e) {
                if (!"All statuses".equals(status) && !status.equalsIgnoreCase(Objects.toString(e.getValue(3), ""))) return false;
                if (query.isBlank()) return true;
                for (int c = 0; c < e.getValueCount(); c++) {
                    if (Objects.toString(e.getValue(c), "").toLowerCase(Locale.ROOT).contains(query)) return true;
                }
                return false;
            }
        });
        showingValue.setText("Showing " + table.getRowCount() + " of " + model.getRowCount() + " history records");
    }

    public void refresh() {
        try {
            List<ApplicationHistoryService.Entry> entries = service.recent(200);
            model.setRowCount(0);
            int completedCount = 0, cancelledCount = 0;
            for (ApplicationHistoryService.Entry entry : entries) {
                String status = Objects.toString(entry.status(), "");
                if ("COMPLETED".equalsIgnoreCase(status)) completedCount++;
                if ("CANCELLED".equalsIgnoreCase(status)) cancelledCount++;
                model.addRow(new Object[] { entry.id(), entry.animal(), entry.adopter(), status,
                    readableDate(entry.reservedAt()), readableDate(entry.expiresAt()) });
            }
            totalValue.setText(Integer.toString(entries.size()));
            completedValue.setText(Integer.toString(completedCount));
            cancelledValue.setText(Integer.toString(cancelledCount));
            otherValue.setText(Integer.toString(entries.size() - completedCount - cancelledCount));
            infoValue.setText("Showing the most recent " + entries.size() + " saved reservation record(s) • Read-only history");
            applyFilters();
        } catch (SQLException err) {
            // Keep the previous visible data rather than clearing the UI on a transient error.
            JOptionPane.showMessageDialog(this, "Could not load history: " + err.getMessage(),
                "Application History", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String readableDate(Object value) {
        if (value == null) return "—";
        String s = value.toString();
        return s.length() > 19 ? s.substring(0, 19).replace('T', ' ') : s.replace('T', ' ');
    }

    private static JLabel label(String text, int size, boolean bold, Color fg) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size));
        l.setForeground(fg);
        return l;
    }

    private static JPanel clear(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    private static JPanel roundedPanel(Color color, Color outline, int radius) {
        return new JPanel() {
            { setOpaque(false); }
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(PawTrackTheme.fill(color));
                g.fillRoundRect(0, 0, Math.max(0, getWidth()-1), Math.max(0, getHeight()-1), radius, radius);
                if (outline != null) {
                    g.setColor(PawTrackTheme.isDark() ? PawTrackTheme.DARK_BORDER : outline);
                    g.drawRoundRect(0, 0, Math.max(0, getWidth()-1), Math.max(0, getHeight()-1), radius, radius);
                }
                g.dispose();
                super.paintComponent(graphics);
            }
        };
    }

    private static final class TableHeader extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                                     boolean focus, int row, int col) {
            super.getTableCellRendererComponent(table, value, selected, focus, row, col);
            setBackground(new Color(0x27568E));
            setForeground(Color.WHITE);
            setFont(SEMIBOLD);
            setBorder(new EmptyBorder(0, 11, 0, 5));
            return this;
        }
    }

    private static class HistoryCell extends DefaultTableCellRenderer {
        private final int column;
        HistoryCell(int column) { this.column = column; }
        @Override public Component getTableCellRendererComponent(JTable t, Object val, boolean selected,
                                                                     boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, val, selected, focus, row, col);
            setBorder(new EmptyBorder(0, 12, 0, 6));
            setBackground(PawTrackTheme.isDark()
                    ? (selected ? PawTrackTheme.DARK_SELECTED : (row % 2 == 0 ? PawTrackTheme.DARK_SURFACE : PawTrackTheme.DARK_INPUT))
                    : (selected ? new Color(0xDCEBFF) : row % 2 == 0 ? Color.WHITE : new Color(0xF2F7FF)));
            setForeground(PawTrackTheme.text());
            setFont(new Font("Segoe UI", column == 1 ? Font.BOLD : Font.PLAIN, 12));
            if (column == 0) setHorizontalAlignment(SwingConstants.CENTER);
            else setHorizontalAlignment(SwingConstants.LEFT);
            return this;
        }
    }

    /** Status chips paint their background exactly once, including row stripes. */
    private static final class StatusBadge extends DefaultTableCellRenderer {
        private Color chipFill = new Color(0xDEEAFE);
        private Color chipText = new Color(0x315FAF);
        private String type = "clock";
        @Override public Component getTableCellRendererComponent(JTable t, Object val, boolean selected,
                                                                  boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, val, selected, focus, row, col);
            String status = Objects.toString(val, "").toUpperCase(Locale.ROOT);
            switch (status) {
                case "COMPLETED", "APPROVED" -> {
                    chipFill = new Color(0xD2F5E1); chipText = new Color(0x14754F); type = "check";
                }
                case "CANCELLED" -> {
                    chipFill = new Color(0xFFE0E5); chipText = new Color(0xA8324B); type = "cross";
                }
                case "EXPIRED" -> {
                    chipFill = new Color(0xFBE9D4); chipText = new Color(0x995D19); type = "clock";
                }
                default -> {
                    chipFill = new Color(0xDEEAFE); chipText = new Color(0x315FAF); type = "clock";
                }
            }
            setText(status.replace('_', ' '));
            setForeground(chipText);
            setFont(SEMIBOLD);
            setOpaque(false);
            setBorder(null);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBackground(PawTrackTheme.isDark()
                    ? (selected ? PawTrackTheme.DARK_SELECTED : (row % 2 == 0 ? PawTrackTheme.DARK_SURFACE : PawTrackTheme.DARK_INPUT))
                    : (selected ? new Color(0xDCEBFF) : row % 2 == 0 ? Color.WHITE : new Color(0xF2F7FF)));
            return this;
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            String text = getText();
            FontMetrics fm = g.getFontMetrics(getFont());
            int chipW = Math.min(getWidth() - 14, Math.max(100, fm.stringWidth(text) + 42));
            int chipH = 30;
            int x = (getWidth() - chipW) / 2, y = (getHeight() - chipH) / 2;
            g.setColor(chipFill);
            g.fillRoundRect(x, y, chipW, chipH, 22, 22);
            new DrawnIcon(type, chipText, 14).paintIcon(this, g, x + 11, y + 8);
            g.setFont(getFont());
            g.setColor(chipText);
            int textX = x + 31;
            g.drawString(text, textX, y + (chipH + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    /** Platform-independent vector icon; avoids missing-font square glyphs on Windows. */
    private static final class DrawnIcon implements Icon {
        private final String type;
        private final Color color;
        private final int size;
        DrawnIcon(String type, Color color, int size) {
            this.type = type; this.color = color; this.size = size;
        }
        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
        @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.translate(x, y);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);
            g.setStroke(new BasicStroke(Math.max(1.8f, size / 11f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double scale = size / 24.0;
            g.scale(scale, scale);
            switch (type) {
                case "search" -> { g.drawOval(3, 3, 13, 13); g.drawLine(14, 14, 22, 22); }
                case "document" -> { g.drawRoundRect(5, 2, 14, 20, 2, 2); g.drawLine(8, 8, 16, 8); g.drawLine(8, 12, 16, 12); g.drawLine(8, 16, 14, 16); }
                case "check" -> { g.drawLine(3, 12, 9, 18); g.drawLine(9, 18, 21, 5); }
                case "cross" -> { g.drawLine(5, 5, 19, 19); g.drawLine(19, 5, 5, 19); }
                case "clock" -> { g.drawOval(3, 3, 18, 18); g.drawLine(12, 7, 12, 12); g.drawLine(12, 12, 16, 15); }
                case "refresh" -> {
                    g.drawArc(4, 4, 16, 16, 45, 275);
                    g.drawLine(20, 5, 20, 12); g.drawLine(20, 12, 14, 10);
                }
                default -> g.drawOval(4, 4, 16, 16);
            }
            g.dispose();
        }
    }

    /** A solid rounded blue button whose appearance does not depend on Look and Feel. */
    private static final class PrimaryButton extends JButton {
        PrimaryButton(String text, Icon icon) {
            super(text, icon);
            setHorizontalAlignment(SwingConstants.CENTER);
            setIconTextGap(9);
            setContentAreaFilled(false);
            setBorderPainted(false);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0xC7D5E9));
            g.fillRoundRect(0, 3, getWidth() - 1, getHeight() - 3, 16, 16);
            g.setPaint(new GradientPaint(0, 0, getModel().isRollover() ? new Color(0x3D83E1) : BLUE,
                    0, getHeight(), new Color(0x1C4F9A)));
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 4, 16, 16);
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
