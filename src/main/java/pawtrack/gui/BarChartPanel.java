package com.petshelter.gui;

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

    private static final Color BAR_COLOR = new Color(0x3D7EA6);
    private static final Color BAR_COLOR_ALT = new Color(0x2A5D7C);
    private static final Color AXIS_COLOR = new Color(0x2B2B2B);
    private static final Color GRID_COLOR = new Color(0xDDDDDD);

    public BarChartPanel() {
        setOpaque(true);
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(660, 440));
    }

    public void setData(String title, String categoryAxisLabel, String valueAxisLabel,
                        Map<String, ? extends Number> data, String valueFormat) {
        this.title = title;
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
            g2.setColor(AXIS_COLOR);
            g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
            drawCentred(g2, emptyMessage, width / 2, height / 2);
            g2.dispose();
            return;
        }

        int left = 78;
        int right = 30;
        int top = 54;
        int bottom = 96;
        int plotWidth = Math.max(10, width - left - right);
        int plotHeight = Math.max(10, height - top - bottom);

        g2.setColor(AXIS_COLOR);
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
            g2.setColor(GRID_COLOR);
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(left, y, left + plotWidth, y);
            g2.setColor(AXIS_COLOR);
            g2.setFont(getFont().deriveFont(11f));
            String tick = String.format(valueFormat, axisMax * i / ticks);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(tick, left - 10 - fm.stringWidth(tick), y + fm.getAscent() / 2 - 2);
        }

        g2.setColor(AXIS_COLOR);
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawLine(left, top, left, top + plotHeight);
        g2.drawLine(left, top + plotHeight, left + plotWidth, top + plotHeight);

        int count = values.size();
        double slot = plotWidth / (double) count;
        double barWidth = Math.min(slot * 0.62, 90);

        for (int i = 0; i < count; i++) {
            double value = values.get(i);
            int barHeight = (int) Math.round(plotHeight * (value / axisMax));
            int x = (int) Math.round(left + slot * i + (slot - barWidth) / 2);
            int y = top + plotHeight - barHeight;

            g2.setColor(i % 2 == 0 ? BAR_COLOR : BAR_COLOR_ALT);
            g2.fillRoundRect(x, y, (int) Math.round(barWidth), Math.max(barHeight, 2), 8, 8);

            g2.setColor(AXIS_COLOR);
            g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
            String valueLabel = String.format(valueFormat, value);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(valueLabel, x + (int) ((barWidth - fm.stringWidth(valueLabel)) / 2), y - 5);

            g2.setFont(getFont().deriveFont(11f));
            drawRotatedCategory(g2, truncate(categories.get(i), 22), x + (int) (barWidth / 2), top + plotHeight + 12);
        }

        g2.setColor(AXIS_COLOR);
        g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
        drawCentred(g2, categoryAxisLabel, left + plotWidth / 2, height - 10);
        drawVerticalLabel(g2, valueAxisLabel, 18, top + plotHeight / 2);

        g2.dispose();
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
}
