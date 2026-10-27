package com.petshelter.gui;

import com.petshelter.service.AnalyticsReport;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
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
    private final JButton refreshButton = new JButton("Refresh Analytics");
    private final BarChartPanel chart = new BarChartPanel();
    private final JTextArea summary = new JTextArea(6, 40);

    private AnalyticsReport report;
    private Map<String, Long> classificationCounts = new LinkedHashMap<>();

    public AnalyticsPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controls.add(new JLabel("Metric:"));
        controls.add(metricBox);
        controls.add(refreshButton);
        add(controls, BorderLayout.NORTH);

        add(chart, BorderLayout.CENTER);

        summary.setEditable(false);
        summary.setLineWrap(true);
        summary.setWrapStyleWord(true);
        summary.setBorder(BorderFactory.createTitledBorder("Mined summary"));
        add(new JScrollPane(summary), BorderLayout.SOUTH);

        metricBox.addActionListener(event -> updateChart());
        chart.clear("Run an analysis to display the chart");
    }

    public void setRefreshAction(Runnable action) {
        refreshButton.addActionListener(event -> action.run());
    }

    public void showReport(AnalyticsReport report, Map<String, Long> classificationCounts, int waitlistSize) {
        this.report = report;
        this.classificationCounts = classificationCounts == null ? Map.of() : classificationCounts;

        if (report == null) {
            summary.setText("");
            chart.clear("No analytics available");
            return;
        }

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
}
