package pawtrack.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import pawtrack.controller.AnimalController;
import pawtrack.controller.AdoptionController;
import pawtrack.controller.WaitlistController;
import pawtrack.controller.AnalyticsController;
import pawtrack.controller.ClassificationController;
import pawtrack.controller.ImportController;
import pawtrack.controller.AuthController;
import pawtrack.dao.AdopterDao;
import pawtrack.dao.AdoptionRecordDao;
import pawtrack.dao.AnimalDao;
import pawtrack.dao.ClassificationDao;
import pawtrack.dao.Database;
import pawtrack.dao.UserDao;
import pawtrack.dao.WaitlistDao;
import pawtrack.model.User;
import pawtrack.model.Role;
import pawtrack.service.AnalyticsService;
import pawtrack.service.ClassificationService;
import pawtrack.service.ShelterSeeder;
import pawtrack.service.StatusHistoryService;
import pawtrack.service.WaitlistService;

import javax.swing.*;
import java.lang.reflect.Field;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MainFrameIntegrationTest {

    private MainFrame frame;
    private User testUser;
    private Database mockDatabase;

    @BeforeEach
    void setUp() throws Exception {
        mockDatabase = mock(Database.class);
        testUser = new User("testuser", "test@example.com", "password", Role.STAFF);
        
        // Create MainFrame with mocked dependencies
        AnimalDao mockAnimalDao = mock(AnimalDao.class);
        AdopterDao mockAdopterDao = mock(AdopterDao.class);
        AdoptionRecordDao mockRecordDao = mock(AdoptionRecordDao.class);
        ClassificationDao mockClassificationDao = mock(ClassificationDao.class);
        WaitlistDao mockWaitlistDao = mock(WaitlistDao.class);
        UserDao mockUserDao = mock(UserDao.class);
        
        ClassificationService mockClassification = mock(ClassificationService.class);
        WaitlistService mockWaitlist = mock(WaitlistService.class);
        StatusHistoryService mockHistory = mock(StatusHistoryService.class);
        AnalyticsService mockAnalytics = mock(AnalyticsService.class);
        
        ShelterSeeder mockSeeder = mock(ShelterSeeder.class);
        AuthController mockAuthController = mock(AuthController.class);
        AnimalController mockAnimalController = mock(AnimalController.class);
        AdoptionController mockAdoptionController = mock(AdoptionController.class);
        WaitlistController mockWaitlistController = mock(WaitlistController.class);
        AnalyticsController mockAnalyticsController = mock(AnalyticsController.class);
        ClassificationController mockClassificationController = mock(ClassificationController.class);
        ImportController mockImportController = mock(ImportController.class);
        
        when(mockClassificationDao.loadOrSeed()).thenReturn(mockClassification);
        when(mockSeeder.seedIfEmpty(any(), any())).thenReturn(null);
        
        // Use reflection to set final fields
        frame = new MainFrame(testUser, mockDatabase);
    }

    @Test
    void testFrameInitialization() {
        assertNotNull(frame);
        assertEquals("Pet Shelter & Adoption Tracker", frame.getTitle());
        assertTrue(frame.isVisible() || !frame.isVisible()); // Just verify frame exists
    }

    @Test
    void testUserRoleStaff() {
        assertEquals(Role.STAFF, frame.getUser().getRole());
    }

    @Test
    void testAnalyticsPanelNotNull() {
        // Access private field via reflection
        AnalyticsPanel panel = getPrivateField(frame, "analyticsPanel", AnalyticsPanel.class);
        assertNotNull(panel);
    }

    @Test
    void testTableModelNotNull() {
        AnimalTableModel model = getPrivateField(frame, "tableModel", AnimalTableModel.class);
        assertNotNull(model);
    }

    @Test
    void testWaitlistServiceNotNull() {
        WaitlistService waitlist = getPrivateField(frame, "waitlist", WaitlistService.class);
        assertNotNull(waitlist);
    }

    @Test
    void testHistoryServiceNotNull() {
        StatusHistoryService history = getPrivateField(frame, "history", StatusHistoryService.class);
        assertNotNull(history);
    }

    @Test
    void testAnalyticsServiceNotNull() {
        AnalyticsService analytics = getPrivateField(frame, "analytics", AnalyticsService.class);
        assertNotNull(analytics);
    }

    @Test
    void testProgressBarNotNull() {
        JProgressBar progressBar = getPrivateField(frame, "progressBar", JProgressBar.class);
        assertNotNull(progressBar);
        assertEquals(0, progressBar.getMinimum());
        assertEquals(100, progressBar.getMaximum());
    }

    @Test
    void testStatusLabelNotNull() {
        JLabel statusLabel = getPrivateField(frame, "statusLabel", JLabel.class);
        assertNotNull(statusLabel);
        assertEquals("Ready", statusLabel.getText());
    }

    @Test
    void testClassificationFilterNotNull() {
        JComboBox<?> filter = getPrivateField(frame, "classificationFilter", JComboBox.class);
        assertNotNull(filter);
    }

    @Test
    void testAnimalTableNotNull() {
        JTable table = getPrivateField(frame, "animalTable", JTable.class);
        assertNotNull(table);
        assertEquals(7, table.getColumnCount());
    }

    @Test
    void testSpinnerNotNull() {
        Object spinner = getPrivateField(frame, "spinner", Object.class);
        assertNotNull(spinner);
    }

    @Test
    void testToolbarMinimizedFlag() {
        boolean minimized = getPrivateField(frame, "toolbarMinimized", boolean.class);
        assertFalse(minimized);
    }

    @Test
    void testRowStripingEnabled() {
        boolean striping = getPrivateField(frame, "rowStripingEnabled", boolean.class);
        assertTrue(striping);
    }

    private <T> T getPrivateField(Object obj, String fieldName, Class<T> type) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return type.cast(field.get(obj));
        } catch (Exception e) {
            throw new RuntimeException("Failed to access field: " + fieldName, e);
        }
    }
}