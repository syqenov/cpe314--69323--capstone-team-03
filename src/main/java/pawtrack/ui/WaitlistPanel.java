package pawtrack.ui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import pawtrack.model.Animal;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;

/**
 * Presentation-only staff waitlist screen. All business actions are supplied by
 * MainFrame and still go through its existing controller/authorization logic.
 * The table model and JTable stay stable for MainFrame's row-to-model mapping.
 */
public final class WaitlistPanel extends JPanel {
    @Override protected void paintComponent(java.awt.Graphics graphics) {
        super.paintComponent(graphics);
        PawTrackBackdrop.paint(graphics, this);
    }

    private static final Color NAVY = PawTrackTheme.NAVY;
    private static final Color BLUE = PawTrackTheme.BLUE;
    private static final Color TEXT = PawTrackTheme.NAVY;
    private static final Color SUB = PawTrackTheme.MUTED;
    private static final Color CANVAS = PawTrackTheme.CANVAS;
    private static final Color EDGE = PawTrackTheme.LINE;

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JScrollPane scrollPane;
    private final JLabel allCount = new JLabel("0");
    private final JLabel completedCount = new JLabel("0");
    private final JLabel waitingCount = new JLabel("0");
    private final JLabel cancelledCount = new JLabel("0");
    private final JLabel showing = new JLabel("No requests yet");
    private final JPopupMenu rowActions = new JPopupMenu();
    private Runnable viewApplicationAction;
    private final JTextField search = new PlaceholderSearchField();
    private final JComboBox<String> filter = new JComboBox<>(new String[]{
            "All statuses", "WAITING", "RESERVED", "IN_REVIEW", "APPROVED", "COMPLETED", "CANCELLED", "EXPIRED"
    });
    private final TableRowSorter<DefaultTableModel> sorter;
    // Model-row identity is retained independently from visual sorting/filtering.
    private final Map<Integer, Animal> modelAnimals = new HashMap<>();
    private final Map<Integer, ImageIcon> animalPhotos = new HashMap<>();


    public WaitlistPanel(Color surface, Color accent, Color grid,
                         JButton refresh, JButton reserve, JButton complete, JButton cancel,
                         Consumer<JScrollPane> styleScrollPane) {
        this(surface, accent, grid, refresh, reserve, complete, cancel, styleScrollPane, null);
    }

    public WaitlistPanel(Color surface, Color accent, Color grid,
                         JButton refresh, JButton reserve, JButton complete, JButton cancel,
                         Consumer<JScrollPane> styleScrollPane, Runnable viewDetails) {
        super(new BorderLayout());
        Objects.requireNonNull(surface, "surface");
        Objects.requireNonNull(accent, "accent");
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(styleScrollPane, "styleScrollPane");
        setBackground(CANVAS);
        setBorder(new EmptyBorder(14, 16, 14, 16));

        tableModel = new DefaultTableModel(
                new String[]{"Animal", "Adopter", "Email", "Requested", "Position", "Review"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        table.setName("waitlistRequestsTable");
        table.setRowHeight(66);
        table.setFillsViewportHeight(true);
        table.setBackground(Color.WHITE);
        table.setForeground(TEXT);
        table.setSelectionBackground(new Color(0xDDEBFF));
        table.setSelectionForeground(TEXT);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setPreferredSize(new Dimension(0, 46));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        int[] widths = {275, 155, 255, 142, 83, 165};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(i == 5
                    ? new StatusRenderer()
                    : i == 0 ? new AnimalRenderer()
                    : i == 4 ? new PositionRenderer() : new RowRenderer(i));
        }
        sorter = new TableRowSorter<>(tableModel);
        // Position sorts numerically and the status renderer never changes the model value.
        sorter.setComparator(4, (a, b) -> Integer.compare(asInt(a), asInt(b)));
        table.setRowSorter(sorter);
        scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        styleScrollPane.accept(scrollPane);

        this.viewApplicationAction = viewDetails;
        // Keep the lightweight constructor used by existing tests compatible.
        // The production MainFrame supplies viewDetails and receives the full design.
        if (viewDetails == null) {
            WaitlistActionsPanel legacyActions = new WaitlistActionsPanel(refresh, reserve, complete, cancel);
            add(legacyActions, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);
            return;
        }

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setOpaque(false);
        add(content, BorderLayout.CENTER);
        JPanel upper = new JPanel();
        upper.setLayout(new BoxLayout(upper, BoxLayout.Y_AXIS));
        upper.setOpaque(false);
        content.add(upper, BorderLayout.NORTH);

        JPanel hero = new Hero();
        hero.setLayout(new BorderLayout());
        hero.setBorder(new EmptyBorder(14, 22, 14, 22));
        hero.setPreferredSize(new Dimension(100, 107));
        JPanel heroWords = new JPanel(new GridLayout(2, 1, 0, 2));
        heroWords.setOpaque(false);
        heroWords.add(text("Applications & Reservations", 23, true, Color.WHITE));
        heroWords.add(text("Manage adoption requests, waitlists and reservations in one place.", 12, false,
                new Color(0xE2EDFF)));
        hero.add(new CalendarEmblem(), BorderLayout.WEST);
        heroWords.setBorder(new EmptyBorder(0, 16, 0, 0));
        hero.add(new JLabel(""), BorderLayout.EAST);
        hero.add(heroWords, BorderLayout.CENTER);
        upper.add(hero);
        upper.add(Box.createVerticalStrut(12));

        JButton details = new JButton("View Application");
        details.setName("waitlistViewApplication");
        details.addActionListener(e -> viewDetails.run());
        styleButton(details, new Color(0xEEF2FF), new Color(0x294D9B), new Color(0xB6C6FF), ActionIcon.Kind.DOCUMENT);
        details.setToolTipText("Open the currently selected waitlist application");
        // Five buttons always stay on ONE LINE. When the window becomes too
        // narrow, scroll the action strip horizontally instead of wrapping.
        JPanel actions = new JPanel(new GridLayout(1, 5, 9, 0));
        actions.setOpaque(false);
        actions.setPreferredSize(new Dimension(1035, 55));
        styleButton(refresh, new Color(0xEAF4FF), new Color(0x1F4E79), new Color(0x9BCBE8), ActionIcon.Kind.REFRESH);
        styleButton(reserve, new Color(0x0F6E8C), Color.WHITE, new Color(0x0A5977), ActionIcon.Kind.PEOPLE);
        styleButton(complete, new Color(0x169E86), Color.WHITE, new Color(0x0D866F), ActionIcon.Kind.CHECK);
        styleButton(cancel, new Color(0xF8E8EC), new Color(0xB23A48), new Color(0xE5A6B3), ActionIcon.Kind.CROSS);
        actions.add(refresh);
        actions.add(reserve);
        actions.add(complete);
        actions.add(cancel);
        actions.add(details);
        JScrollPane actionScroll = new JScrollPane(actions,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        actionScroll.setBorder(BorderFactory.createEmptyBorder());
        actionScroll.setOpaque(false);
        actionScroll.getViewport().setOpaque(false);
        actionScroll.getHorizontalScrollBar().setUnitIncrement(25);
        actionScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        actionScroll.setPreferredSize(new Dimension(400, 64));
        upper.add(actionScroll);
        upper.add(Box.createVerticalStrut(8));

        JPanel counts = new JPanel(new GridLayout(1, 4, 12, 0));
        counts.setOpaque(false);
        counts.setMaximumSize(new Dimension(Integer.MAX_VALUE, 93));
        counts.setPreferredSize(new Dimension(400, 93));
        counts.add(stat("TOTAL REQUESTS", allCount, new Color(0xDFEEFF), BLUE, ActionIcon.Kind.DOCUMENT));
        counts.add(stat("COMPLETED", completedCount, new Color(0xE1F8EC), new Color(0x159C64), ActionIcon.Kind.CHECK));
        counts.add(stat("WAITING", waitingCount, new Color(0xEEE7FF), new Color(0x7855C5), ActionIcon.Kind.CLOCK));
        counts.add(stat("CANCELLED", cancelledCount, new Color(0xFFE9E9), new Color(0xC34A58), ActionIcon.Kind.CROSS));
        upper.add(counts);
        upper.add(Box.createVerticalStrut(10));

        JPanel filters = new JPanel(new BorderLayout(14, 0));
        filters.setOpaque(false);
        filters.setPreferredSize(new Dimension(400, 44));
        filters.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JPanel searchPanel = new RoundedSurface(Color.WHITE, 20, EDGE);
        searchPanel.setLayout(new BorderLayout(8, 0));
        searchPanel.setBorder(new EmptyBorder(7, 14, 7, 14));
        searchPanel.add(new SearchIcon(), BorderLayout.WEST);
        search.setName("waitlistSearch");
        search.setToolTipText("Filter by animal, adopter, email or date");
        search.setBorder(BorderFactory.createEmptyBorder());
        search.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        search.setOpaque(false);
        search.putClientProperty("JTextField.placeholderText", "Search by animal, adopter, or email...");
        search.setToolTipText("Search by animal, adopter, or email...");
        searchPanel.add(search, BorderLayout.CENTER);
        filters.add(searchPanel, BorderLayout.CENTER);
        filter.setName("waitlistStatusFilter");
        filter.setBackground(Color.WHITE);
        filter.setForeground(TEXT);
        filter.setFont(new Font("Segoe UI", Font.BOLD, 12));
        filter.setPreferredSize(new Dimension(185, 42));
        JPanel filterWrap = new RoundedSurface(Color.WHITE, 20, EDGE);
        filterWrap.setLayout(new BorderLayout(8, 0));
        filterWrap.setBorder(new EmptyBorder(2, 12, 2, 8));
        filterWrap.add(new FilterIcon(), BorderLayout.WEST);
        filter.setOpaque(false);
        filter.setBorder(BorderFactory.createEmptyBorder());
        filterWrap.add(filter, BorderLayout.CENTER);
        filterWrap.setPreferredSize(new Dimension(216, 44));
        filters.add(filterWrap, BorderLayout.EAST);
        upper.add(filters);

        JPanel tableCard = new RoundedSurface(Color.WHITE, 20, EDGE);
        tableCard.setLayout(new BorderLayout(0, 7));
        tableCard.setBorder(new EmptyBorder(11, 12, 11, 12));
        JPanel tableHead = new JPanel(new BorderLayout());
        tableHead.setOpaque(false);
        tableHead.add(text("Waitlist queue", 16, true, TEXT), BorderLayout.WEST);
        showing.setForeground(SUB);
        showing.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableHead.add(showing, BorderLayout.EAST);
        tableCard.add(tableHead, BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);
        content.add(tableCard, BorderLayout.CENTER);

        tableModel.addTableModelListener(e -> {
            // The existing MainFrame clears and repopulates rows on refresh.
            if (tableModel.getRowCount() == 0) modelAnimals.clear();
            SwingUtilities.invokeLater(this::updateCounts);
        });
        sorter.addRowSorterListener(e -> updateShowing());
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        filter.addActionListener(e -> applyFilter());

        // Reuse the exact same secured actions as the visible buttons. No
        // direct database writes or alternate permission paths are introduced.
        addRowAction("Refresh waitlist", refresh);
        addRowAction("Reserve first in queue", reserve);
        addRowAction("Complete selected adoption", complete);
        addRowAction("Cancel reservation", cancel);
        table.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { handleRowMouse(e); }
            @Override public void mouseReleased(MouseEvent e) { handleRowMouse(e); }
        });
        table.setToolTipText("Click an animal photo to view its application; right-click for actions");
        updateCounts();
    }

    private void addRowAction(String title, JButton existingButton) {
        JMenuItem item = new JMenuItem(title);
        item.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        item.addActionListener(e -> {
            if (existingButton.isEnabled()) existingButton.doClick();
        });
        rowActions.add(item);
    }

    private void handleRowMouse(MouseEvent e) {
        if (e.isPopupTrigger()) {
            int row = table.rowAtPoint(e.getPoint());
            if (row >= 0 && !table.isRowSelected(row)) {
                table.setRowSelectionInterval(row, row);
            }
            // Never show record-specific actions without a valid row; refresh
            // remains available through the main button at all times.
            rowActions.getComponent(1).setEnabled(row >= 0);
            rowActions.getComponent(2).setEnabled(row >= 0);
            rowActions.getComponent(3).setEnabled(row >= 0);
            rowActions.show(table, e.getX(), e.getY());
            return;
        }
        if (e.getID() == MouseEvent.MOUSE_RELEASED && SwingUtilities.isLeftMouseButton(e)
                && e.getClickCount() == 1) {
            int row = table.rowAtPoint(e.getPoint());
            int column = table.columnAtPoint(e.getPoint());
            if (row >= 0 && column == 0) {
                java.awt.Rectangle cell = table.getCellRect(row, 0, true);
                // Only the thumbnail (not the entire cell) opens the dialog.
                if (e.getX() - cell.x >= 8 && e.getX() - cell.x <= 68) {
                    table.setRowSelectionInterval(row, row);
                    if (viewApplicationAction != null) viewApplicationAction.run();
                }
            }
        }
    }

    /**
     * Called only by MainFrame's existing waitlist refresh logic, immediately
     * after adding a model row. No change to column values or selection mapping.
     */
    public void registerAnimalForRow(int modelRow, Animal animal) {
        if (animal == null || modelRow < 0 || modelRow >= tableModel.getRowCount()) return;
        modelAnimals.put(modelRow, animal);
        int id = animal.getId();
        if (id <= 0 || animalPhotos.containsKey(id)) return;
        PetImageCache.get().load(animal, 42, 42,
                loaded -> {
                    if (loaded.icon() != null) {
                        animalPhotos.put(id, loaded.icon());
                        table.repaint();
                    }
                }, ignored -> { /* Use vector paw placeholder offline. */ });
    }

    private void updateCounts() {
        if (allCount.getParent() == null) return;
        int completed = 0, waiting = 0, cancelled = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String status = String.valueOf(tableModel.getValueAt(i, 5)).toUpperCase(Locale.ROOT);
            if ("COMPLETED".equals(status)) completed++;
            else if ("CANCELLED".equals(status)) cancelled++;
            else if ("WAITING".equals(status)) waiting++;
        }
        allCount.setText(Integer.toString(tableModel.getRowCount()));
        completedCount.setText(Integer.toString(completed));
        waitingCount.setText(Integer.toString(waiting));
        cancelledCount.setText(Integer.toString(cancelled));
        updateShowing();
    }

    private void updateShowing() {
        showing.setText(table.getRowCount() + " of " + tableModel.getRowCount() + " requests");
    }

    private void applyFilter() {
        String term = search.getText().trim().toLowerCase(Locale.ROOT);
        String status = String.valueOf(filter.getSelectedItem());
        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                boolean okStatus = "All statuses".equals(status)
                        || status.equalsIgnoreCase(String.valueOf(entry.getValue(5)));
                if (!okStatus) return false;
                if (term.isEmpty()) return true;
                for (int i = 0; i < entry.getValueCount(); i++) {
                    if (String.valueOf(entry.getValue(i)).toLowerCase(Locale.ROOT).contains(term)) return true;
                }
                return false;
            }
        });
        updateShowing();
    }

    private static int asInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (NumberFormatException ex) { return 0; }
    }

    private static JLabel text(String s, int size, boolean bold, Color foreground) {
        JLabel l = new JLabel(s);
        l.setForeground(foreground);
        l.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size));
        return l;
    }

    private static JPanel stat(String title, JLabel value, Color background, Color accent, ActionIcon.Kind icon) {
        JPanel card = new RoundedSurface(background, 21, Color.WHITE);
        card.setLayout(new BorderLayout(8, 0));
        card.setBorder(new EmptyBorder(10, 12, 10, 8));
        JLabel mark = new JLabel(new ActionIcon(icon, accent, 31));
        mark.setHorizontalAlignment(SwingConstants.CENTER);
        mark.setPreferredSize(new Dimension(45, 45));
        card.add(mark, BorderLayout.WEST);
        JPanel stack = new JPanel(new GridLayout(2, 1, 0, 1));
        stack.setOpaque(false);
        stack.add(text(title, 10, true, accent));
        value.setFont(new Font("Segoe UI", Font.BOLD, 25));
        value.setForeground(TEXT);
        stack.add(value);
        card.add(stack, BorderLayout.CENTER);
        return card;
    }

    private static void styleButton(JButton button, Color background, Color foreground,
                                    Color border, ActionIcon.Kind icon) {
        String label = button.getText();
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setBorder(new EmptyBorder(7, 5, 11, 5));
        button.setIcon(new ActionIcon(icon, foreground, 17));
        button.setIconTextGap(7);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText(label);
        button.setUI(new RoundedActionUI(background, border));
    }

    /** Drawn gradient and shadow, independent of the current Windows look and feel. */
    private static final class RoundedActionUI extends BasicButtonUI {
        private final Color color, outline;
        RoundedActionUI(Color color, Color outline) {
            this.color = color; this.outline = outline;
        }
        @Override public void paint(Graphics g, JComponent c) {
            Graphics2D x = (Graphics2D) g.create();
            try {
                x.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = c.getWidth(), h = c.getHeight();
                // A soft shadow remains inside the component bounds.
                x.setColor(new Color(21, 47, 91, 40));
                x.fillRoundRect(2, 6, Math.max(0, w - 4), Math.max(0, h - 9), 20, 20);
                JButton b = (JButton) c;
                Color bg = b.isEnabled() ? color : new Color(0xD9E2EE);
                // Pale action fills paired with light text disappeared in dark mode.
                if (PawTrackTheme.isDark() && color.getRed() > 205 && color.getGreen() > 205
                        && color.getBlue() > 205) {
                    bg = PawTrackTheme.DARK_INPUT;
                    b.setForeground(b.isEnabled() ? PawTrackTheme.DARK_TEXT : PawTrackTheme.DARK_MUTED);
                }
                if (b.getModel().isPressed()) bg = bg.darker();
                else if (b.getModel().isRollover() && b.isEnabled()) bg = bg.brighter();
                x.setPaint(new GradientPaint(0, 0, bg.brighter(), 0, h, bg));
                x.fillRoundRect(1, 1, Math.max(0, w - 3), Math.max(0, h - 9), 19, 19);
                x.setColor(PawTrackTheme.isDark() && color.getRed() > 205 && color.getGreen() > 205
                        && color.getBlue() > 205 ? PawTrackTheme.DARK_BORDER : outline);
                x.setStroke(new BasicStroke(1f));
                x.drawRoundRect(1, 1, Math.max(0, w - 3), Math.max(0, h - 9), 19, 19);
            } finally { x.dispose(); }
            super.paint(g, c);
        }
    }

    /** Simple vector symbols: consistent across systems, no missing emoji font. */
    private static final class ActionIcon implements Icon {
        enum Kind { DOCUMENT, REFRESH, PEOPLE, CHECK, CROSS, CLOCK }
        private final Kind kind;
        private final Color color;
        private final int size;
        ActionIcon(Kind kind, Color color, int size) { this.kind=kind; this.color=color; this.size=size; }
        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
        @Override public void paintIcon(Component c, Graphics g, int px, int py) {
            Graphics2D x=(Graphics2D)g.create();
            try {
                x.translate(px, py);
                x.scale(size / 24.0, size / 24.0);
                x.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                x.setColor(PawTrackTheme.isDark() && (kind == Kind.REFRESH || kind == Kind.CROSS)
                        ? PawTrackTheme.DARK_TEXT : color);
                x.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                switch (kind) {
                    case DOCUMENT -> {
                        x.drawRoundRect(5, 3, 14, 18, 3, 3);
                        x.drawLine(8, 9, 16, 9); x.drawLine(8, 13, 16, 13); x.drawLine(8, 17, 13, 17);
                    }
                    case REFRESH -> {
                        x.drawArc(4, 4, 16, 16, 34, 285);
                        x.drawLine(19, 5, 20, 11); x.drawLine(20, 11, 14, 10);
                    }
                    case PEOPLE -> {
                        x.drawOval(8, 4, 8, 8); x.fillRoundRect(6, 14, 12, 7, 5, 5);
                        x.fillOval(2, 8, 5, 5); x.fillOval(17, 8, 5, 5);
                    }
                    case CHECK -> { x.drawLine(4, 12, 10, 18); x.drawLine(10, 18, 21, 5); }
                    case CROSS -> { x.drawLine(5, 5, 19, 19); x.drawLine(19, 5, 5, 19); }
                    case CLOCK -> {
                        x.drawOval(3, 3, 18, 18); x.drawLine(12, 7, 12, 12); x.drawLine(12, 12, 16, 15);
                    }
                }
            } finally { x.dispose(); }
        }
    }

    /** The header paints behind existing brand art, not a separate image window. */
    private static final class Hero extends JPanel {
        private BufferedImage pets;
        Hero() {
            setOpaque(false);
            try (InputStream in = getClass().getResourceAsStream("/pawtrack-login-art.jpg")) {
                if (in != null) pets = ImageIO.read(in);
            } catch (Exception ignored) { /* Gradient-only fallback. */ }
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D gg = (Graphics2D) g.create();
            gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gg.setClip(new RoundRectangle2D.Double(0, 0, getWidth()-1, getHeight()-1, 22, 22));
            gg.setPaint(new GradientPaint(0, 0, PawTrackTheme.HERO_START, getWidth(), 0, PawTrackTheme.HERO_END));
            gg.fillRect(0, 0, getWidth(), getHeight());
            gg.setColor(new Color(255, 255, 255, 30));
            gg.fillOval(getWidth()-330, -115, 360, 230);
            gg.fillOval(getWidth()-430, 70, 340, 100);
            if (pets != null && getWidth() >= 750) {
                // Existing approved PawTrack dog/cat artwork; crop just the pets.
                int dstW = 195, dstH = Math.min(103, getHeight());
                int sx1 = (int)(pets.getWidth() * .02);
                int sy1 = (int)(pets.getHeight() * .46);
                int sx2 = (int)(pets.getWidth() * .97);
                int sy2 = (int)(pets.getHeight() * .91);
                gg.drawImage(pets, getWidth()-dstW-30, getHeight()-dstH,
                        getWidth()-30, getHeight(), sx1, sy1, sx2, sy2, null);
                // Soft fade on the artwork's leading edge instead of a hard
                // rectangular image boundary.
                gg.setPaint(new GradientPaint(getWidth()-dstW-36, 0, new Color(0x346FBC),
                        getWidth()-dstW+25, 0, new Color(52, 111, 188, 0)));
                gg.fillRect(getWidth()-dstW-37, 0, 65, getHeight());
            }
            gg.dispose();
            super.paintComponent(g);
        }
    }

    private static final class CalendarEmblem extends JComponent {
        CalendarEmblem() { setPreferredSize(new Dimension(52, 60)); setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D x = (Graphics2D) g.create();
            x.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            x.setColor(new Color(0xE8F2FF)); x.fillOval(2, 5, 48, 48);
            x.setColor(new Color(0x2259A6)); x.setStroke(new BasicStroke(2.5f));
            x.drawRoundRect(14, 19, 24, 23, 4, 4);
            x.drawLine(14, 26, 38, 26); x.drawLine(20, 15, 20, 22); x.drawLine(32, 15, 32, 22);
            x.fillOval(20, 31, 5, 5); x.fillOval(28, 31, 5, 5);
            x.dispose();
        }
    }

    private static final class PlaceholderSearchField extends JTextField {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!getText().isEmpty()) return;
            Graphics2D x = (Graphics2D) g.create();
            x.setFont(getFont());
            x.setColor(new Color(0x7387A3));
            FontMetrics fm = x.getFontMetrics();
            x.drawString("Search by animal, adopter, or email...", 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            x.dispose();
        }
    }

    private static final class SearchIcon extends JComponent {
        SearchIcon() { setPreferredSize(new Dimension(21, 22)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D x = (Graphics2D) g.create();
            x.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            x.setColor(BLUE); x.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            x.drawOval(2, 2, 11, 11); x.drawLine(12, 13, 19, 20);
            x.dispose();
        }
    }

    private static final class FilterIcon extends JComponent {
        FilterIcon() {
            setPreferredSize(new Dimension(22, 22));
            setMinimumSize(new Dimension(22, 22));
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D x = (Graphics2D) g.create();
            try {
                x.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                x.setColor(new Color(0x2C65B5));
                x.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                java.awt.geom.Path2D funnel = new java.awt.geom.Path2D.Double();
                funnel.moveTo(cx - 8, cy - 7);
                funnel.lineTo(cx + 8, cy - 7);
                funnel.lineTo(cx + 2, cy);
                funnel.lineTo(cx + 2, cy + 6);
                funnel.lineTo(cx - 2, cy + 8);
                funnel.lineTo(cx - 2, cy);
                funnel.closePath();
                x.draw(funnel);
            } finally {
                x.dispose();
            }
        }
    }

    private static final class RoundedSurface extends JPanel {
        private final Color bg, outline;
        private final int radius;
        RoundedSurface(Color bg, int radius, Color outline) {
            this.bg = bg; this.radius = radius; this.outline = outline;
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D gg = (Graphics2D) g.create();
            gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gg.setColor(PawTrackTheme.fill(bg));
            gg.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, radius, radius);
            gg.setColor(PawTrackTheme.isDark() ? PawTrackTheme.DARK_BORDER : outline);
            gg.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, radius, radius);
            gg.dispose();
            super.paintComponent(g);
        }
    }

    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        HeaderRenderer() {
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new EmptyBorder(0, 12, 0, 5));
            setFont(new Font("Segoe UI", Font.BOLD, 12));
        }
        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            setBackground(new Color(0x214D85));
            setForeground(Color.WHITE);
            setHorizontalAlignment(column >= 4 ? SwingConstants.CENTER : SwingConstants.LEFT);
            setOpaque(true);
            return this;
        }
    }

    private static final class RowRenderer extends DefaultTableCellRenderer {
        private final int column;
        RowRenderer(int column) { this.column = column; setBorder(new EmptyBorder(0, 12, 0, 7)); }
        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            setBackground(PawTrackTheme.isDark() ? (selected ? PawTrackTheme.DARK_SELECTED :
                    (row % 2 == 0 ? PawTrackTheme.DARK_SURFACE : PawTrackTheme.DARK_INPUT)) :
                    (selected ? new Color(0xDCEAFF) : (row % 2 == 0 ? new Color(0xEDF5FF) : Color.WHITE)));
            setForeground(PawTrackTheme.text());
            setFont(new Font("Segoe UI", this.column == 0 ? Font.BOLD : Font.PLAIN, 12));
            if (this.column == 4) setHorizontalAlignment(SwingConstants.CENTER);
            else setHorizontalAlignment(SwingConstants.LEFT);
            return this;
        }
    }

    /** A centered queue marker, kept separate from the numeric model value. */
    private static final class PositionRenderer extends JPanel implements TableCellRenderer {
        private String value;
        private boolean selected;
        private int row;
        PositionRenderer() { setOpaque(false); }
        @Override public Component getTableCellRendererComponent(JTable table, Object data,
                boolean isSelected, boolean focused, int row, int column) {
            value = String.valueOf(data);
            selected = isSelected;
            this.row = row;
            return this;
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D x = (Graphics2D) g.create();
            x.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            x.setColor(PawTrackTheme.isDark() ? (selected ? PawTrackTheme.DARK_SELECTED :
                    (row % 2 == 0 ? PawTrackTheme.DARK_SURFACE : PawTrackTheme.DARK_INPUT)) :
                    (selected ? new Color(0xDCEAFF) : (row % 2 == 0 ? new Color(0xEDF5FF) : Color.WHITE)));
            x.fillRect(0, 0, getWidth(), getHeight());
            int diameter = 31, left = (getWidth() - diameter)/2;
            int top = (getHeight() - diameter)/2;
            x.setColor(PawTrackTheme.isDark() ? PawTrackTheme.DARK_HOVER : new Color(0xD7E9FF));
            x.fillOval(left, top, diameter, diameter);
            x.setColor(PawTrackTheme.isDark() ? PawTrackTheme.DARK_ACCENT : new Color(0x205C9E));
            x.setFont(new Font("Segoe UI", Font.BOLD, 13));
            FontMetrics metrics = x.getFontMetrics();
            x.drawString(value, (getWidth()-metrics.stringWidth(value))/2,
                    (getHeight()+metrics.getAscent()-metrics.getDescent())/2);
            x.dispose();
        }
    }

    private final class AnimalRenderer extends JPanel implements TableCellRenderer {
        private final JLabel thumb = new JLabel();
        private final JLabel title = new JLabel();
        private final JLabel type = new JLabel();
        AnimalRenderer() {
            super(new BorderLayout(10, 0));
            setOpaque(true);
            setBorder(new EmptyBorder(5, 12, 5, 6));
            thumb.setPreferredSize(new Dimension(42, 42));
            thumb.setHorizontalAlignment(SwingConstants.CENTER);
            thumb.setOpaque(true);
            JPanel words = new JPanel(new GridLayout(2, 1, 0, 0));
            words.setOpaque(false);
            title.setFont(new Font("Segoe UI", Font.BOLD, 12));
            title.setForeground(TEXT);
            type.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            type.setForeground(SUB);
            words.add(title); words.add(type);
            add(thumb, BorderLayout.WEST);
            add(words, BorderLayout.CENTER);
        }
        @Override public Component getTableCellRendererComponent(JTable t, Object value,
                boolean selected, boolean focused, int viewRow, int column) {
            int modelRow = t.convertRowIndexToModel(viewRow);
            Animal animal = modelAnimals.get(modelRow);
            String raw = String.valueOf(value);
            String species = animal == null
                    ? (raw.contains("(Cat)") ? "Cat" : raw.contains("(Dog)") ? "Dog" : "Animal")
                    : animal.species().label();
            title.setText(raw);
            type.setText(species);
            title.setForeground(PawTrackTheme.text());
            type.setForeground(PawTrackTheme.muted());
            ImageIcon photo = animal == null ? null : animalPhotos.get(animal.getId());
            thumb.setIcon(photo);
            thumb.setText(photo == null ? ("Dog".equalsIgnoreCase(species) ? "D" : "C") : "");
            thumb.setFont(new Font("Segoe UI", Font.BOLD, 15));
            thumb.setForeground(PawTrackTheme.isDark() ? PawTrackTheme.DARK_ACCENT : BLUE);
            thumb.setBackground(PawTrackTheme.isDark() ? PawTrackTheme.DARK_INPUT : new Color(0xDCEBFF));
            setBackground(PawTrackTheme.isDark() ? (selected ? PawTrackTheme.DARK_SELECTED :
                    (viewRow % 2 == 0 ? PawTrackTheme.DARK_SURFACE : PawTrackTheme.DARK_INPUT)) :
                    (selected ? new Color(0xDCEAFF) : (viewRow % 2 == 0 ? new Color(0xEDF5FF) : Color.WHITE)));
            return this;
        }
    }

    /** Rounded status badge without changing the underlying status text. */
    private static final class StatusRenderer extends JPanel implements TableCellRenderer {
        private String status = "";
        private Color bg = new Color(0xFFF0D8), fg = new Color(0x8D5B13);
        private boolean selected;
        private int rowIndex;
        StatusRenderer() { setOpaque(false); }
        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean focused, int row, int column) {
            status = String.valueOf(value).toUpperCase(Locale.ROOT);
            selected = isSelected;
            rowIndex = row;
            switch (status) {
                case "COMPLETED" -> { bg = new Color(0xCFF4DD); fg = new Color(0x096638); }
                case "CANCELLED", "EXPIRED" -> { bg = new Color(0xFFE1E4); fg = new Color(0x9D2331); }
                case "WAITING" -> { bg = new Color(0xDDEBFF); fg = new Color(0x1B549C); }
                case "RESERVED", "APPROVED" -> { bg = new Color(0xEAE1FF); fg = new Color(0x5945A2); }
                default -> { bg = new Color(0xFFF0D8); fg = new Color(0x8D5B13); }
            }
            return this;
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D x = (Graphics2D) g.create();
            x.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            x.setColor(PawTrackTheme.isDark() ? (selected ? PawTrackTheme.DARK_SELECTED :
                    (rowIndex % 2 == 0 ? PawTrackTheme.DARK_SURFACE : PawTrackTheme.DARK_INPUT)) :
                    (selected ? new Color(0xDCEAFF) : (rowIndex % 2 == 0 ? new Color(0xEDF5FF) : Color.WHITE)));
            x.fillRect(0, 0, getWidth(), getHeight());
            int pillW = Math.min(Math.max(68, getWidth()-12), Math.max(118, status.length()*8 + 48));
            int pillH = 30;
            int px = (getWidth()-pillW)/2, py = (getHeight()-pillH)/2;
            x.setColor(bg); x.fillRoundRect(px, py, pillW, pillH, pillH, pillH);
            x.setColor(fg);
            x.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = px+15, cy = py+15;
            if (status.equals("COMPLETED")) {
                x.drawLine(cx-6, cy, cx-2, cy+4); x.drawLine(cx-2, cy+4, cx+6, cy-5);
            } else if (status.equals("CANCELLED") || status.equals("EXPIRED")) {
                x.drawLine(cx-5, cy-5, cx+5, cy+5); x.drawLine(cx+5, cy-5, cx-5, cy+5);
            } else {
                x.drawOval(cx-6, cy-6, 12, 12); x.drawLine(cx, cy, cx, cy-4); x.drawLine(cx, cy, cx+4, cy+2);
            }
            x.setFont(new Font("Segoe UI", Font.BOLD, 11));
            FontMetrics fm = x.getFontMetrics();
            x.drawString(status, px+30, cy+(fm.getAscent()-fm.getDescent())/2);
            x.dispose();
        }
    }

    public DefaultTableModel getTableModel() { return tableModel; }
    public JTable getTable() { return table; }
    public JScrollPane getScrollPane() { return scrollPane; }
}
