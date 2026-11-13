package pawtrack.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class AnalyticsPanelTest {

    private AnalyticsPanel panel;

    @BeforeEach
    void setUp() {
        panel = new AnalyticsPanel();
    }

    @Test
    void testInitialState() {
        assertNotNull(panel);
        assertNotNull(panel.getChart());
        assertNotNull(panel.getSummary());
    }

    @Test
    void testShowReportWithNullReport() {
        panel.showReport(null, java.util.Map.of(), 0);
        assertEquals("", panel.getSummary().getText());
    }

    @Test
    void testMetricBoxHasItems() {
        assertEquals(4, panel.getMetricBox().getItemCount());
    }

    @Test
    void testRefreshActionCanBeSet() {
        boolean[] called = {false};
        panel.setRefreshAction(() -> called[0] = true);
        panel.getRefreshButton().doClick();
        assertTrue(called[0]);
    }
}