package pawtrack.ui;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class PawTrackDashboardTest {
    private static final Runnable NOOP = () -> {};

    @Test void staffDashboardUpdatesMetrics() {
        PawTrackDashboard panel = new PawTrackDashboard(true, "admin", NOOP, NOOP, NOOP);
        panel.setStats(12, 8, 4);
        assertTrue(panel.isStaffDashboard());
        assertEquals("12", panel.metricText(0));
        assertEquals("8", panel.metricText(1));
        assertEquals("4", panel.metricText(2));
    }

    @Test void adopterDashboardDisplaysOwnStatsWithoutRequiringStaffData() {
        PawTrackDashboard panel = new PawTrackDashboard(false, "user", NOOP, NOOP, NOOP);
        panel.setStats(6, 2, 1);
        assertFalse(panel.isStaffDashboard());
        assertEquals("2", panel.metricText(1));
        assertEquals("1", panel.metricText(2));
    }

    @Test void quickActionRunsNavigationCallback() {
        AtomicInteger selected = new AtomicInteger();
        PawTrackDashboard panel = new PawTrackDashboard(true, "admin", () -> selected.incrementAndGet(), NOOP, NOOP);
        clickByText(panel, "Manage animals");
        assertEquals(1, selected.get());
    }

    private static void clickByText(java.awt.Container root, String text) {
        for (java.awt.Component component : root.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) {
                button.doClick();
                return;
            }
            if (component instanceof java.awt.Container child) {
                clickByText(child, text);
            }
        }
    }
}
