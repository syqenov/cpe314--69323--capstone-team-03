package pawtrack.ui;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.GradientPaint;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lightweight, dependency-free bar chart. Draws a titled, labelled chart with a
 * category axis, a value axis, grid lines, value labels and rotated category
 * labels - used to visualise the results of the data-mining pass.
 */
public final class BarChartPanel extends JComponent {

    private String title = "";
    private String categoryAxisLabel = "";
    private String valueAxisLabel = "";
    private String valueFormat = "%.0f";
    private String emptyMessage = "No data to display";
    private final List<String> categories = new ArrayList<>();
    private final List<Double> values = new ArrayList<>();
    private boolean horizontalCategories;

    private static final Color BAR_COLOR = new Color(0x3D7EA6);
    private static final Color BAR_COLOR_ALT = new Color(0x2A5D7C);
    private static final Color AXIS_COLOR = new Color(0x2B2B2B);
    private static final Color GRID_COLOR = new Color(0xDDDDDD);

    // Custom Java2D text bypasses Swing component foreground updates.
    private static Color axisText() { return PawTrackTheme.isDark() ? PawTrackTheme.DARK_TEXT : AXIS_COLOR; }
    private static Color mutedText() { return PawTrackTheme.isDark() ? PawTrackTheme.DARK_MUTED : new Color(0x536884); }
    private static Color chartGrid() { return PawTrackTheme.isDark() ? PawTrackTheme.DARK_BORDER : GRID_COLOR; }

    public BarChartPanel() {
        setOpaque(true);
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(660, 370));
    }

    public void setData(String title, String categoryAxisLabel, String valueAxisLabel,
                        Map<String, ? extends Number> data, String valueFormat) {
        this.title = title;
        // Hierarchical classification paths need one row each, not rotated X labels.
        this.horizontalCategories = "Classification".equalsIgnoreCase(categoryAxisLabel);
        this.categoryAxisLabel = categoryAxisLabel;
        this.valueAxisLabel = valueAxisLabel;
        this.valueFormat = valueFormat;
        categories.clear();
        values.clear();
        if (data != null) {
            for (Map.Entry<String, ? extends Number> entry : data.entrySet()) {
                categories.add(entry.getKey());
                values.add(entry.getValue() == null ? 0.0 : entry.getValue().doubleValue());
            }
        }
        repaint();
    }

    public void clear(String message) {
        categories.clear();
        values.clear();
        emptyMessage = message;
        horizontalCategories = false;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        if (categories.isEmpty()) {
            g2.setColor(axisText());
            g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
            drawCentred(g2, emptyMessage, width / 2, height / 2);
            g2.dispose();
            return;
        }

        if (horizontalCategories) {
            paintHorizontalClassification(g2, width, height);
            g2.dispose();
            return;
        }

        int left = 78;
        int right = 30;
        int top = 58;
        int bottom = 76;
        int plotWidth = Math.max(10, width - left - right);
        int plotHeight = Math.max(10, height - top - bottom);

        g2.setColor(axisText());
        g2.setFont(getFont().deriveFont(Font.BOLD, 16f));
        drawCentred(g2, title, width / 2, 28);

        double max = 0;
        for (double value : values) {
            max = Math.max(max, value);
        }
        double axisMax = niceCeiling(Math.max(max, 1.0));

        int ticks = 5;
        for (int i = 0; i <= ticks; i++) {
            int y = top + plotHeight - (int) Math.round(plotHeight * (i / (double) ticks));
            g2.setColor(chartGrid());
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(left, y, left + plotWidth, y);
            g2.setColor(axisText());
            g2.setFont(getFont().deriveFont(11f));
            String tick = String.format(valueFormat, axisMax * i / ticks);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(tick, left - 10 - fm.stringWidth(tick), y + fm.getAscent() / 2 - 2);
        }

        g2.setColor(axisText());
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawLine(left, top, left, top + plotHeight);
        g2.drawLine(left, top + plotHeight, left + plotWidth, top + plotHeight);

        int count = values.size();
        double slot = plotWidth / (double) count;
        double barWidth = Math.min(slot * 0.52, 126);

        for (int i = 0; i < count; i++) {
            double value = values.get(i);
            int barHeight = (int) Math.round(plotHeight * (value / axisMax));
            int x = (int) Math.round(left + slot * i + (slot - barWidth) / 2);
            int y = top + plotHeight - barHeight;

            // Solid, legible bars with a soft two-tone finish.
            Color first = i % 2 == 0 ? new Color(0x367BE0) : new Color(0x267CAB);
            Color second = i % 2 == 0 ? new Color(0x2056AD) : new Color(0x174B77);
            g2.setPaint(new GradientPaint(x, y, first, x, y + Math.max(barHeight, 1), second));
            g2.fillRoundRect(x, y, (int) Math.round(barWidth), Math.max(barHeight, 2), 12, 12);

            g2.setColor(axisText());
            g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
            String valueLabel = String.format(valueFormat, value);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(valueLabel, x + (int) ((barWidth - fm.stringWidth(valueLabel)) / 2), y - 5);

            g2.setColor(axisText());
            g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
            String category = truncate(categories.get(i), 22);
            int labelX = x + (int) (barWidth / 2);
            int labelY = top + plotHeight + 24;
            if (g2.getFontMetrics().stringWidth(category) <= slot - 12) {
                // Short labels such as Cat and Dog stay horizontal.
                drawCentred(g2, category, labelX, labelY);
            } else {
                // Keep long breed names readable without overlapping adjacent bars.
                drawRotatedCategory(g2, category, labelX, labelY);
            }
        }

        g2.setColor(axisText());
        g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
        drawCentred(g2, categoryAxisLabel, left + plotWidth / 2, height - 10);
        drawVerticalLabel(g2, valueAxisLabel, 18, top + plotHeight / 2);

        g2.dispose();
    }

    /**
     * Classification-specific horizontal bars. Long ancestry paths are shown
     * as the most informative trailing sections, with the full path available
     * in tooltips. Each category gets its own readable row.
     */
    private void paintHorizontalClassification(Graphics2D g2, int width, int height) {
        int count = categories.size();
        int left = Math.min(Math.max(190, width * 34 / 100), 380);
        left = Math.min(left, Math.max(140, width - 180));
        int right = 68;
        int top = 60;
        int bottom = 46;
        int plotWidth = Math.max(45, width - left - right);
        int plotHeight = Math.max(60, height - top - bottom);
        int rowHeight = Math.max(24, plotHeight / Math.max(1, count));
        double max = 0;
        for (double value : values) max = Math.max(max, value);
        double axisMax = niceCeiling(Math.max(1, max));

        g2.setColor(axisText());
        g2.setFont(getFont().deriveFont(Font.BOLD, 16f));
        drawCentred(g2, title, width / 2, 28);

        // Readable vertical grid lines and a horizontal count scale.
        g2.setStroke(new BasicStroke(1f));
        for (int tick = 0; tick <= 4; tick++) {
            int x = left + (int) Math.round(plotWidth * tick / 4.0);
            g2.setColor(chartGrid());
            g2.drawLine(x, top - 7, x, top + plotHeight);
            g2.setColor(mutedText());
            g2.setFont(getFont().deriveFont(10f));
            String label = String.format(valueFormat, axisMax * tick / 4.0);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, x - fm.stringWidth(label) / 2, top + plotHeight + 18);
        }

        for (int i = 0; i < count; i++) {
            int centreY = top + (int) Math.round((i + .5) * plotHeight / count);
            int barHeight = Math.min(22, Math.max(12, rowHeight - 7));
            int barY = centreY - barHeight / 2;
            double value = values.get(i);
            int barWidth = Math.max(value > 0 ? 2 : 0,
                    (int) Math.round(plotWidth * value / axisMax));

            if (i % 2 == 0) {
                g2.setColor(PawTrackTheme.isDark() ? PawTrackTheme.DARK_INPUT : new Color(0xF5F8FD));
                g2.fillRoundRect(6, centreY - rowHeight / 2, width - 12, rowHeight, 8, 8);
            }
            String readable = compactHierarchy(categories.get(i));
            g2.setFont(getFont().deriveFont(Font.PLAIN, 11.5f));
            g2.setColor(axisText());
            FontMetrics fm = g2.getFontMetrics();
            readable = fitEnd(readable, fm, Math.max(50, left - 25));
            g2.drawString(readable, Math.max(7, left - 14 - fm.stringWidth(readable)),
                    centreY + fm.getAscent() / 2 - 2);

            Color first = i % 2 == 0 ? new Color(0x397DE4) : new Color(0x287CAD);
            Color second = i % 2 == 0 ? new Color(0x1A54AD) : new Color(0x144F79);
            g2.setPaint(new GradientPaint(left, barY, first, left + Math.max(1, barWidth), barY, second));
            if (barWidth > 0) g2.fillRoundRect(left, barY, barWidth, barHeight, 9, 9);

            g2.setColor(axisText());
            g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
            String number = String.format(valueFormat, value);
            int numberX = Math.min(width - g2.getFontMetrics().stringWidth(number) - 5, left + barWidth + 8);
            g2.drawString(number, numberX, centreY + g2.getFontMetrics().getAscent() / 2 - 2);
        }
        g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
        g2.setColor(axisText());
        drawCentred(g2, valueAxisLabel, left + plotWidth / 2, height - 5);
    }

    private static String compactHierarchy(String category) {
        if (category == null) return "";
        // Avoid repeating 'All Animals' for every hierarchy branch.
        String[] parts = category.split("\\s*>\\s*");
        if (parts.length <= 2) return category;
        return parts[parts.length - 2].trim() + "  ›  " + parts[parts.length - 1].trim();
    }

    private static String fitEnd(String value, FontMetrics fm, int available) {
        if (fm.stringWidth(value) <= available) return value;
        String ellipsis = "…";
        for (int i = value.length() - 1; i >= 0; i--) {
            String candidate = ellipsis + value.substring(i);
            if (fm.stringWidth(candidate) <= available) return candidate;
        }
        return ellipsis;
    }

    public boolean isHorizontalClassificationChart() {
        return horizontalCategories;
    }

    private void drawCentred(Graphics2D g2, String text, int centreX, int baselineY) {
        if (text == null || text.isEmpty()) {
            return;
        }
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, centreX - fm.stringWidth(text) / 2, baselineY);
    }

    private void drawRotatedCategory(Graphics2D g2, String text, int x, int y) {
        AffineTransform original = g2.getTransform();
        g2.rotate(Math.toRadians(-40), x, y);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, x - fm.stringWidth(text) + 8, y);
        g2.setTransform(original);
    }

    private void drawVerticalLabel(Graphics2D g2, String text, int x, int centreY) {
        if (text == null || text.isEmpty()) {
            return;
        }
        AffineTransform original = g2.getTransform();
        g2.rotate(-Math.PI / 2, x, centreY);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, x - fm.stringWidth(text) / 2, centreY);
        g2.setTransform(original);
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1) + "\u2026";
    }

    static double niceCeiling(double value) {
        double exponent = Math.floor(Math.log10(value));
        double base = Math.pow(10, exponent);
        double fraction = value / base;
        double nice;
        if (fraction <= 1) {
            nice = 1;
        } else if (fraction <= 2) {
            nice = 2;
        } else if (fraction <= 5) {
            nice = 5;
        } else {
            nice = 10;
        }
        return nice * base;
    }

    // Getters for testing
    public String getTitle() {
        return title;
    }

    public String getEmptyMessage() {
        return emptyMessage;
    }

    public String getValueFormat() {
        return valueFormat;
    }
}
