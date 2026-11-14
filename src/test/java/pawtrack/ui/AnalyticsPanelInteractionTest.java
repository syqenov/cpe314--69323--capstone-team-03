
package pawtrack.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import pawtrack.service.AnalyticsReport;

import javax.swing.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnalyticsPanelInteractionTest {

    private AnalyticsPanel panel;

    @BeforeEach
    void setUp() {
        panel = new AnalyticsPanel();
    }

    @Test
    void testInitialState() {
        assertNotNull(panel);

        assertEquals(4, panel.getMetricBox().getItemCount());
        assertEquals(
                "Adoptions by Species",
                panel.getMetricBox().getItemAt(0)
        );
        assertEquals(
                "Top Adopted Breeds (Top 8)",
                panel.getMetricBox().getItemAt(1)
        );
        assertEquals(
                "Average Shelter Stay (days) by Species",
                panel.getMetricBox().getItemAt(2)
        );
        assertEquals(
                "Adoptions by Classification (recursive)",
                panel.getMetricBox().getItemAt(3)
        );
    }

    @Test
    void testShowReportWithNullReport() {
        panel.showReport(null, Map.of(), 0);

        assertEquals("", panel.getSummary().getText());
    }

    @Test
    void testShowReportWithValidReport() {
        AnalyticsReport report = mock(AnalyticsReport.class);

        when(report.totalAnimals()).thenReturn(100);
        when(report.totalAdoptions()).thenReturn(50);
        when(report.mostAdoptedSpecies()).thenReturn("Dog");
        when(report.mostAdoptedBreed()).thenReturn("Labrador");
        when(report.averageStayDaysOverall()).thenReturn(15.5);
        when(report.hasAdoptions()).thenReturn(true);

        when(report.adoptionsBySpecies())
                .thenReturn(Map.of("Dog", 30L, "Cat", 20L));

        when(report.adoptionsByBreed())
                .thenReturn(Map.of("Labrador", 15L, "Siamese", 10L));

        when(report.averageStayDaysBySpecies())
                .thenReturn(Map.of("Dog", 14.2, "Cat", 18.7));

        panel.showReport(report, Map.of("Mammal", 50L), 5);

        String summary = panel.getSummary().getText();

        assertTrue(summary.contains("Animals in shelter dataset: 100"));
        assertTrue(summary.contains("Completed adoptions: 50"));
        assertTrue(summary.contains("Most adopted species: Dog"));
        assertTrue(summary.contains("Most adopted breed: Labrador"));
        assertTrue(summary.contains("Average shelter stay: 15.5 days"));
        assertTrue(summary.contains("Adopters currently on waitlists: 5"));
    }

    @Test
    void testShowReportWithNoAdoptions() {
        AnalyticsReport report = mock(AnalyticsReport.class);

        when(report.hasAdoptions()).thenReturn(false);
        when(report.totalAnimals()).thenReturn(10);
        when(report.totalAdoptions()).thenReturn(0);
        when(report.mostAdoptedSpecies()).thenReturn("-");
        when(report.mostAdoptedBreed()).thenReturn("-");
        when(report.averageStayDaysOverall()).thenReturn(0.0);

        panel.showReport(report, Map.of(), 0);

        String summary = panel.getSummary().getText();

        assertTrue(summary.contains("Completed adoptions: 0"));
    }

    @Test
    void testMetricBoxActionListenerUpdatesChart() {
        AnalyticsReport report = mock(AnalyticsReport.class);

        when(report.hasAdoptions()).thenReturn(true);
        when(report.adoptionsBySpecies())
                .thenReturn(Map.of("Dog", 30L));
        when(report.adoptionsByBreed())
                .thenReturn(Map.of("Labrador", 15L));
        when(report.averageStayDaysBySpecies())
                .thenReturn(Map.of("Dog", 14.2));

        panel.showReport(report, Map.of(), 0);

        panel.getMetricBox().setSelectedIndex(1);

        assertNotNull(panel.getChart());
    }

    @Test
    void testRefreshActionCanBeSet() {
        boolean[] called = {false};

        panel.setRefreshAction(() -> called[0] = true);
        panel.getRefreshButton().doClick();

        assertTrue(called[0]);
    }

    @Test
    void testSummaryAreaIsNotEditable() {
        assertFalse(panel.getSummary().isEditable());
    }

    @Test
    void testSummaryAreaLineWrap() {
        assertTrue(panel.getSummary().getLineWrap());
        assertTrue(panel.getSummary().getWrapStyleWord());
    }

    @Test
    void testChartNotNull() {
        assertNotNull(panel.getChart());
    }

    @Test
    void testMetricBoxItemsCount() {
        assertEquals(4, panel.getMetricBox().getItemCount());
    }
}
