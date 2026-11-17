package pawtrack.ui;

import pawtrack.service.AnalyticsReport;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Scrollable;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.border.EmptyBorder;
import java.awt.GraphicsEnvironment;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import java.awt.event.HierarchyEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Data-mining dashboard: a metric selector, a refresh button and a clearly
 * labelled {@link BarChartPanel} plus a textual summary of the mined figures.
 */
public final class AnalyticsPanel extends JPanel {

    private static final String SPECIES = "Adoptions by Species";
    private static final String BREEDS = "Top Adopted Breeds (Top 8)";
    private static final String STAY = "Average Shelter Stay (days) by Species";
    private static final String CLASSIFICATION = "Adoptions by Classification (recursive)";

    private final JComboBox<String> metricBox = new JComboBox<>(
            new String[]{SPECIES, BREEDS, STAY, CLASSIFICATION});
    private final JButton refreshButton = new RefreshDataButton();
    private final BarChartPanel chart = new BarChartPanel();
    private JPanel chartCard;
    private ScrollablePage scrollablePage;
    private final JTextArea summary = new JTextArea(6, 40);
    private final JLabel totalAnimals = new JLabel("—");
    private final JLabel totalAdoptions = new JLabel("—");
    private final JLabel averageStay = new JLabel("—");
    private final JLabel pendingWaitlist = new JLabel("—");

    private final JLabel[] animatedLabels = {totalAnimals, totalAdoptions, averageStay, pendingWaitlist};
    private final double[] displayed = new double[4];
    private final double[] target = new double[4];
    private final Timer[] timers = new Timer[4];
    private final JPanel insightGrid = new JPanel(new GridLayout(1, 3, 12, 0));
    private final JLabel topSpecies = new JLabel("—");
    private final JLabel topBreed = new JLabel("—");
    private final JLabel completed = new JLabel("—");

    private AnalyticsReport report;
    private Map<String, Long> classificationCounts = new LinkedHashMap<>();

    private static final Color INK = PawTrackTheme.NAVY;
    private static final Color MUTED = PawTrackTheme.MUTED;
    private static final Color BLUE = PawTrackTheme.BLUE;
    private static final Color GREEN = new Color(0x159D80);
    private static final Color PINK = new Color(0xD34E86);
    private static final Color PURPLE = new Color(0x7B61C7);
    private final JLabel chartDescription = new JLabel("Explore your shelter's adoption patterns");
    private final JLabel selectionHint = new JLabel("Choose a metric to compare results");

    public AnalyticsPanel() {
        super(new BorderLayout());
        setBackground(PawTrackTheme.CANVAS);
        setBorder(new EmptyBorder(16, 22, 16, 22));

        // The page is scrollable vertically and always fills the viewport width.
        // Without this, BorderLayout compresses the chart almost to zero height
        // when the JFrame is smaller than the sum of the card heights.
        ScrollablePage page = new ScrollablePage();
        scrollablePage = page;
        JScrollPane viewport = new JScrollPane(page,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        viewport.setBorder(null);
        viewport.getViewport().setBackground(getBackground());
        viewport.getVerticalScrollBar().setUnitIncrement(20);
        viewport.setOpaque(false);
        add(viewport, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(0, 13));
        top.setOpaque(false);
        JPanel hero = new GradientCard(PawTrackTheme.HERO_START, PawTrackTheme.HERO_END, 24);
        hero.setLayout(new BorderLayout(20, 0));
        hero.setBorder(new EmptyBorder(16, 22, 16, 22));
        JPanel heroText = new JPanel(new GridLayout(3, 1, 0, 1));
        heroText.setOpaque(false);
        JLabel eyebrow = label("SHELTER PERFORMANCE  /  LIVE OVERVIEW", 10, true, new Color(0xBED8FF));
        JLabel heading = label("Shelter Analytics", 26, true, Color.WHITE);
        JLabel subheading = label("Understand adoption trends, celebrate milestones, and plan ahead.", 12, false, new Color(0xE0ECFF));
        heroText.add(eyebrow);
        heroText.add(heading);
        heroText.add(subheading);
        hero.add(heroText, BorderLayout.CENTER);
        JLabel emblem = label("✦  PAWTRACK INSIGHTS", 12, true, Color.WHITE);
        emblem.setHorizontalAlignment(JLabel.RIGHT);
        hero.add(emblem, BorderLayout.EAST);
        hero.setPreferredSize(new Dimension(0, 105));
        top.add(hero, BorderLayout.NORTH);

        JPanel stats = new JPanel(new GridLayout(1, 4, 13, 0));
        stats.setOpaque(false);
        stats.setPreferredSize(new Dimension(0, 116));
        stats.add(metricCard("01", "TOTAL ANIMALS", totalAnimals, "Animals in shelter records", BLUE, new Color(0xE7F0FF)));
        stats.add(metricCard("02", "TOTAL ADOPTIONS", totalAdoptions, "Successful placements", GREEN, new Color(0xE3F7F1)));
        stats.add(metricCard("03", "AVERAGE SHELTER STAY", averageStay, "Days before adoption", PURPLE, new Color(0xEEE9FF)));
        stats.add(metricCard("04", "WAITLIST REQUESTS", pendingWaitlist, "Interested adopters in queue", PINK, new Color(0xFDEAF2)));
        top.add(stats, BorderLayout.CENTER);
        page.add(top, BorderLayout.NORTH);

        chartCard = whiteCard(new BorderLayout(0, 10));
        chartCard.setBorder(new EmptyBorder(18, 20, 16, 20));
        JPanel controls = new JPanel(new BorderLayout(10, 8));
        controls.setOpaque(false);
        JPanel chartHead = new JPanel(new GridLayout(2, 1, 0, 3));
        chartHead.setOpaque(false);
        chartHead.add(label("Adoption insights", 19, true, INK));
        chartDescription.setForeground(MUTED);
        chartDescription.setFont(new Font("SansSerif", Font.PLAIN, 12));
        chartHead.add(chartDescription);
        // Stack the title above the controls so neither is clipped at 900-1100px.
        controls.add(chartHead, BorderLayout.NORTH);

        JPanel options = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        options.setOpaque(false);
        metricBox.setFont(new Font("SansSerif", Font.PLAIN, 12));
        metricBox.setPreferredSize(new Dimension(285, 36));
        metricBox.setBackground(Color.WHITE);
        options.add(metricBox);
        refreshButton.setText("Refresh data");
        refreshButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFocusPainted(false);
        refreshButton.setPreferredSize(new Dimension(138, 39));
        options.add(refreshButton);
        controls.add(options, BorderLayout.SOUTH);
        chartCard.add(controls, BorderLayout.NORTH);

        JPanel chartBody = new JPanel(new BorderLayout(0, 3));
        chartBody.setOpaque(false);
        // Reserve a real chart height rather than letting BorderLayout shrink it.
        chart.setPreferredSize(new Dimension(640, 370));
        chart.setMinimumSize(new Dimension(0, 345));
        chartBody.add(chart, BorderLayout.CENTER);
        selectionHint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        selectionHint.setForeground(MUTED);
        selectionHint.setHorizontalAlignment(JLabel.CENTER);
        chartBody.add(selectionHint, BorderLayout.SOUTH);
        chartCard.add(chartBody, BorderLayout.CENTER);
        chartCard.setPreferredSize(new Dimension(750, 500));
        page.add(chartCard, BorderLayout.CENTER);

        JPanel highlights = new JPanel(new BorderLayout(0, 9));
        highlights.setOpaque(false);
        highlights.add(label("Shelter highlights", 17, true, INK), BorderLayout.NORTH);
        insightGrid.setLayout(new GridLayout(1, 3, 13, 0));
        insightGrid.setOpaque(false);
        insightGrid.add(insightCard("TOP SPECIES", topSpecies, "Most frequently adopted", BLUE, new Color(0xE7F0FF)));
        insightGrid.add(insightCard("TOP BREED", topBreed, "Shelter favorite", PURPLE, new Color(0xEEE9FF)));
        insightGrid.add(insightCard("COMPLETED ADOPTIONS", completed, "Happy endings recorded", GREEN, new Color(0xE3F7F1)));
        insightGrid.setPreferredSize(new Dimension(0, 94));
        highlights.add(insightGrid, BorderLayout.CENTER);
        page.add(highlights, BorderLayout.SOUTH);

        // Four metrics across on wide screens, two-by-two on compact windows.
        // Only height changes; the label objects and their counters are reused.
        page.addComponentListener(new ComponentAdapter() {
            private boolean compact = false;
            @Override public void componentResized(ComponentEvent e) {
                boolean nowCompact = page.getWidth() < 930;
                if (nowCompact == compact) return;
                compact = nowCompact;
                stats.setLayout(new GridLayout(nowCompact ? 2 : 1,
                        nowCompact ? 2 : 4, 12, 12));
                stats.setPreferredSize(new Dimension(0, nowCompact ? 236 : 116));
                stats.revalidate();
                page.revalidate();
            }
        });

        summary.setEditable(false);
        summary.setLineWrap(true);
        summary.setWrapStyleWord(true);
        metricBox.addActionListener(event -> updateChart());
        chart.clear("Run an analysis to display the chart");
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                SwingUtilities.invokeLater(this::replayCounterAnimation);
            } else if (!isShowing()) {
                for (Timer timer : timers) if (timer != null) timer.stop();
            }
        });
    }

    /** Scroll vertically without forcing a horizontal scrollbar or shrinking charts. */
    private static final class ScrollablePage extends JPanel implements Scrollable {
        private ScrollablePage() {
            super(new BorderLayout(0, 14));
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            PawTrackBackdrop.paint(graphics, this);
        }
        @Override public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }
        @Override public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) {
            return 20;
        }
        @Override public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) {
            return Math.max(40, r.height - 40);
        }
        @Override public boolean getScrollableTracksViewportWidth() {
            return true;
        }
        @Override public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static JLabel label(String text, int size, boolean bold, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size));
        l.setForeground(color);
        return l;
    }

    private static JPanel whiteCard(BorderLayout layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(12, 15, 12, 15));
        return p;
    }

    private static JPanel metricCard(String number, String name, JLabel value, String detail, Color accent, Color tint) {
        JPanel card = new GradientCard(Color.WHITE, tint, 18);
        card.setLayout(new BorderLayout(8, 4));
        card.setBorder(new EmptyBorder(14, 16, 12, 12));
        JLabel symbol = label(number, 15, true, accent);
        symbol.setPreferredSize(new Dimension(35, 35));
        symbol.setHorizontalAlignment(JLabel.CENTER);
        card.add(symbol, BorderLayout.WEST);
        JPanel text = new JPanel(new GridLayout(3, 1, 0, 2));
        text.setOpaque(false);
        text.add(label(name, 10, true, MUTED));
        value.setFont(new Font("SansSerif", Font.BOLD, 23));
        value.setForeground(INK);
        text.add(value);
        text.add(label(detail, 10, false, accent));
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private static JPanel insightCard(String name, JLabel value, String detail, Color accent, Color tint) {
        JPanel card = new GradientCard(Color.WHITE, tint, 16);
        card.setLayout(new GridLayout(3, 1, 0, 3));
        card.setBorder(new EmptyBorder(12, 16, 12, 16));
        card.add(label(name, 10, true, accent));
        value.setFont(new Font("SansSerif", Font.BOLD, 18));
        value.setForeground(INK);
        card.add(value);
        card.add(label(detail, 10, false, MUTED));
        return card;
    }

    /** Paint the button ourselves: native Windows LAF ignored the white text. */
    private static final class RefreshDataButton extends JButton {
        RefreshDataButton() {
            super("Refresh data");
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("SansSerif", Font.BOLD, 13));
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            Color first = getModel().isPressed() ? new Color(0x174189) : new Color(0x2259AF);
            Color last = getModel().isRollover() ? new Color(0x4B8AF1) : new Color(0x3978DB);
            g2.setPaint(new GradientPaint(0, 0, first, getWidth(), 0, last));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class GradientCard extends JPanel {
        private final Color start, end;
        private final int radius;
        GradientCard(Color start, Color end, int radius) {
            this.start = start;
            this.end = end;
            this.radius = radius;
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, PawTrackTheme.fill(start), getWidth(), getHeight(), PawTrackTheme.fill(end)));
            g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private String formatMetric(int index, double number) {
        if (index == 2) return String.format(java.util.Locale.US, "%.1f days", number);
        return String.format(java.util.Locale.US, "%,d", Math.round(number));
    }

    private void replayCounterAnimation() {
        if (report == null || !isShowing() || GraphicsEnvironment.isHeadless()) return;
        for (int i = 0; i < animatedLabels.length; i++) animateMetric(i, true);
    }

    private void animateMetric(int index, boolean replay) {
        if (timers[index] != null) timers[index].stop();
        double from = replay ? 0 : displayed[index];
        double to = target[index];
        if (!isShowing() || GraphicsEnvironment.isHeadless() || from == to) {
            displayed[index] = to;
            animatedLabels[index].setText(formatMetric(index, to));
            return;
        }
        displayed[index] = from;
        animatedLabels[index].setText(formatMetric(index, from));
        final long start = System.nanoTime();
        final int durationMs = 650;
        Timer timer = new Timer(16, null);
        timer.addActionListener(e -> {
            double t = Math.min(1.0, (System.nanoTime() - start) / 1e6 / durationMs);
            double eased = 1 - Math.pow(1 - t, 3);
            displayed[index] = from + (to - from) * eased;
            animatedLabels[index].setText(formatMetric(index, displayed[index]));
            if (t >= 1) {
                displayed[index] = to;
                animatedLabels[index].setText(formatMetric(index, to));
                timer.stop();
            }
        });
        timers[index] = timer;
        timer.start();
    }

    public void setRefreshAction(Runnable action) {
        refreshButton.addActionListener(event -> action.run());
    }

    public void showReport(AnalyticsReport report, Map<String, Long> classificationCounts, int waitlistSize) {
        this.report = report;
        this.classificationCounts = classificationCounts == null ? Map.of() : classificationCounts;

        if (report == null) {
            summary.setText("");
            for (Timer timer : timers) if (timer != null) timer.stop();
            for (JLabel label : animatedLabels) label.setText("—");
            topSpecies.setText("—"); topBreed.setText("—"); completed.setText("—");
            chart.clear("No analytics available");
            return;
        }

        target[0] = Math.max(0, report.totalAnimals());
        target[1] = Math.max(0, report.totalAdoptions());
        target[2] = Math.max(0, report.averageStayDaysOverall());
        target[3] = Math.max(0, waitlistSize);
        for (int i = 0; i < animatedLabels.length; i++) animateMetric(i, false);
        topSpecies.setText(String.valueOf(report.mostAdoptedSpecies()));
        topBreed.setText(String.valueOf(report.mostAdoptedBreed()));
        completed.setText(String.format(java.util.Locale.US, "%,d", report.totalAdoptions()));
        StringBuilder text = new StringBuilder();
        text.append(String.format("Animals in shelter dataset: %d%n", report.totalAnimals()));
        text.append(String.format("Completed adoptions: %d%n", report.totalAdoptions()));
        text.append(String.format("Most adopted species: %s%n", report.mostAdoptedSpecies()));
        text.append(String.format("Most adopted breed: %s%n", report.mostAdoptedBreed()));
        text.append(String.format("Average shelter stay: %.1f days%n", report.averageStayDaysOverall()));
        text.append(String.format("Adopters currently on waitlists: %d%n", waitlistSize));
        summary.setText(text.toString());
        summary.setCaretPosition(0);

        updateChart();
        revalidate();
        repaint();
    }

    private void updateChart() {
        if (report == null || !report.hasAdoptions()) {
            chart.clear("No adoption history yet - import the CSV intake log");
            return;
        }
        String metric = (String) metricBox.getSelectedItem();
        if (metric == null) {
            return;
        }
        chartDescription.setText(metric);
        selectionHint.setText("Based on recorded shelter adoption data");
        switch (metric) {
            case SPECIES -> chart.setData("Adoptions by Species", "Species", "Adoptions",
                    report.adoptionsBySpecies(), "%.0f");
            case BREEDS -> chart.setData("Top 8 Adopted Breeds", "Breed", "Adoptions",
                    limit(report.adoptionsByBreed(), 8), "%.0f");
            case STAY -> chart.setData("Average Shelter Stay by Species", "Species", "Days",
                    report.averageStayDaysBySpecies(), "%.1f");
            case CLASSIFICATION -> chart.setData("Adoptions per Classification (recursive roll-up)",
                    "Classification", "Adoptions", topPositive(classificationCounts, 12), "%.0f");
            default -> chart.clear("Unknown metric");
        }
        // Classification has up to twelve long hierarchy paths. Give each
        // horizontal row space and let the existing page scroll naturally.
        int count = CLASSIFICATION.equals(metric) ? topPositive(classificationCounts, 12).size() : 0;
        int chartHeight = count > 0 ? Math.max(370, count * 35 + 100) : 370;
        chart.setPreferredSize(new Dimension(640, chartHeight));
        if (chartCard != null) chartCard.setPreferredSize(new Dimension(750, chartHeight + 130));
        chart.revalidate();
        if (scrollablePage != null) scrollablePage.revalidate();
    }

    private static Map<String, Long> limit(Map<String, Long> source, int max) {
        Map<String, Long> limited = new LinkedHashMap<>();
        int index = 0;
        for (Map.Entry<String, Long> entry : source.entrySet()) {
            if (index++ >= max) {
                break;
            }
            limited.put(entry.getKey(), entry.getValue());
        }
        return limited;
    }

    private static Map<String, Long> topPositive(Map<String, Long> source, int max) {
        Map<String, Long> positive = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : source.entrySet()) {
            if (entry.getValue() > 0) {
                positive.put(entry.getKey(), entry.getValue());
                if (positive.size() >= max) {
                    break;
                }
            }
        }
        return positive;
    }

    // Getters for testing
    public BarChartPanel getChart() {
        return chart;
    }

    public JTextArea getSummary() {
        return summary;
    }

    public JComboBox<String> getMetricBox() {
        return metricBox;
    }

    public JButton getRefreshButton() {
        return refreshButton;
    }
}
