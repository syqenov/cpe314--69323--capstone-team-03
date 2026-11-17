package pawtrack.ui;

import org.junit.jupiter.api.Test;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.Species;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PawTrackDashboardRefreshTest {
    private static final Runnable NOOP = () -> {};

    private List<Animal> sample() {
        return List.of(
                Animal.create(Species.DOG, 1, "Buddy", "Retriever", 18,
                        LocalDate.of(2026, 11, 1), AnimalStatus.AVAILABLE),
                Animal.create(Species.CAT, 2, "Luna", "Siamese", 12,
                        LocalDate.of(2026, 11, 2), AnimalStatus.ADOPTED),
                Animal.create(Species.CAT, 3, "Milo", "Tabby", 7,
                        LocalDate.of(2026, 11, 3), AnimalStatus.INTAKE)
        );
    }

    @Test void adopterMetricsStayBoundToOwnRecords() {
        PawTrackDashboard dashboard = new PawTrackDashboard(false, "demo", NOOP, NOOP, NOOP);
        dashboard.setStats(3, 2, 1);
        dashboard.setAnimalSnapshot(sample());
        assertEquals("3", dashboard.metricText(0));
        assertEquals("2", dashboard.metricText(1));
        assertEquals("1", dashboard.metricText(2));
        assertEquals("1", dashboard.metricText(3));
    }

    @Test void staffMetricsMatchAvailableAndPendingInputs() {
        PawTrackDashboard dashboard = new PawTrackDashboard(true, "staff", NOOP, NOOP, NOOP);
        dashboard.setAnimalSnapshot(sample());
        dashboard.setStats(3, 1, 7);
        assertEquals("3", dashboard.metricText(0));
        assertEquals("1", dashboard.metricText(1));
        assertEquals("7", dashboard.metricText(2));
        assertEquals("1", dashboard.metricText(3));
    }

    @Test void dashboardNavigationCallbacksStillWork() {
        AtomicInteger count = new AtomicInteger();
        PawTrackDashboard dashboard = new PawTrackDashboard(true, "staff", count::incrementAndGet, NOOP, NOOP);
        javax.swing.JButton manage = findButton(dashboard, "Manage animals");
        assertNotNull(manage);
        manage.doClick();
        assertEquals(1, count.get());
    }

    private javax.swing.JButton findButton(java.awt.Container parent, String text) {
        for (java.awt.Component c : parent.getComponents()) {
            if (c instanceof javax.swing.JButton b && text.equals(b.getText())) return b;
            if (c instanceof java.awt.Container child) {
                javax.swing.JButton b = findButton(child, text);
                if (b != null) return b;
            }
        }
        return null;
    }
}
