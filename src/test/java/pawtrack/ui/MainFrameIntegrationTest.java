
package pawtrack.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pawtrack.dao.Database;
import pawtrack.model.Role;
import pawtrack.model.User;
import pawtrack.service.AnalyticsService;
import pawtrack.service.StatusHistoryService;
import pawtrack.service.WaitlistService;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTable;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class MainFrameIntegrationTest {

    private MainFrame frame;
    private User testUser;
    private Database database;

    @BeforeEach
    void setUp() throws Exception {
        database = Database.openInMemory();

        testUser = new User(
                1,
                "testuser",
                "hash",
                "salt",
                Role.STAFF,
                null
        );

        frame = new MainFrame(testUser, database);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (frame != null) {
            frame.dispose();
        }

        if (database != null) {
            database.close();
        }
    }

    @Test
    void testFrameInitialization() {
        assertNotNull(frame);
        assertEquals(
                "Pet Shelter & Adoption Tracker",
                frame.getTitle()
        );
        assertNotNull(frame.getRootPane());
    }

    @Test
    void testUserRoleStaff() {
        assertEquals(Role.STAFF, frame.getUser().getRole());
    }

    @Test
    void testAnalyticsPanelNotNull() {
        AnalyticsPanel panel = getPrivateField(
                frame, "analyticsPanel", AnalyticsPanel.class
        );

        assertNotNull(panel);
    }

    @Test
    void testTableModelNotNull() {
        AnimalTableModel model = getPrivateField(
                frame, "tableModel", AnimalTableModel.class
        );

        assertNotNull(model);
    }

    @Test
    void testWaitlistServiceNotNull() {
        WaitlistService waitlist = getPrivateField(
                frame, "waitlist", WaitlistService.class
        );

        assertNotNull(waitlist);
    }

    @Test
    void testHistoryServiceNotNull() {
        StatusHistoryService history = getPrivateField(
                frame, "history", StatusHistoryService.class
        );

        assertNotNull(history);
    }

    @Test
    void testAnalyticsServiceNotNull() {
        AnalyticsService analytics = getPrivateField(
                frame, "analytics", AnalyticsService.class
        );

        assertNotNull(analytics);
    }

    @Test
    void testProgressBarNotNull() {
        JProgressBar progressBar = getPrivateField(
                frame, "progressBar", JProgressBar.class
        );

        assertNotNull(progressBar);
        assertEquals(0, progressBar.getMinimum());
        assertEquals(100, progressBar.getMaximum());
    }

    @Test
    void testStatusLabelNotNull() {
        JLabel statusLabel = getPrivateField(
                frame, "statusLabel", JLabel.class
        );

        assertNotNull(statusLabel);
        assertEquals("Showing all animals", statusLabel.getText());
    }

    @Test
    void testClassificationFilterNotNull() {
        JComboBox<?> filter = getPrivateField(
                frame, "classificationFilter", JComboBox.class
        );

        assertNotNull(filter);
    }

    @Test
    void testAnimalTableNotNull() {
        JTable table = getPrivateField(
                frame, "animalTable", JTable.class
        );

        assertNotNull(table);
        assertEquals(9, table.getColumnCount());
    }

    @Test
    void testSpinnerNotNull() {
        Object spinner = getPrivateField(
                frame, "spinner", Object.class
        );

        assertNotNull(spinner);
    }

    @Test
    void testToolbarMinimizedFlag() {
        Boolean minimized = getPrivateField(
                frame, "toolbarMinimized", Boolean.class
        );

        assertNotNull(minimized);
        assertFalse(minimized);
    }

    @Test
    void testRowStripingEnabled() {
        Boolean striping = getPrivateField(
                frame, "rowStripingEnabled", Boolean.class
        );

        assertNotNull(striping);
        assertTrue(striping);
    }

    private <T> T getPrivateField(
            Object object,
            String fieldName,
            Class<T> type
    ) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return type.cast(field.get(object));
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to access field: " + fieldName, e
            );
        }
    }
}
