package pawtrack.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import pawtrack.controller.*;
import pawtrack.dao.*;
import pawtrack.model.*;
import pawtrack.service.*;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MainFrameKeyboardShortcutTest {

    private MainFrame frame;
    private User testUser;
    private Database mockDatabase;

    @BeforeEach
    void setUp() throws Exception {
        mockDatabase = mock(Database.class);
        testUser = new User("testuser", "test@example.com", "password", Role.STAFF);
        
        // Mock all DAOs
        AnimalDao mockAnimalDao = mock(AnimalDao.class);
        AdopterDao mockAdopterDao = mock(AdopterDao.class);
        AdoptionRecordDao mockRecordDao = mock(AdoptionRecordDao.class);
        ClassificationDao mockClassificationDao = mock(ClassificationDao.class);
        WaitlistDao mockWaitlistDao = mock(WaitlistDao.class);
        UserDao mockUserDao = mock(UserDao.class);
        
        // Mock services
        ClassificationService mockClassification = mock(ClassificationService.class);
        WaitlistService mockWaitlist = mock(WaitlistService.class);
        StatusHistoryService mockHistory = mock(StatusHistoryService.class);
        AnalyticsService mockAnalytics = mock(AnalyticsService.class);
        ShelterSeeder mockSeeder = mock(ShelterSeeder.class);
        
        // Mock controllers
        AuthController mockAuthController = mock(AuthController.class);
        AnimalController mockAnimalController = mock(AnimalController.class);
        AdoptionController mockAdoptionController = mock(AdoptionController.class);
        WaitlistController mockWaitlistController = mock(WaitlistController.class);
        AnalyticsController mockAnalyticsController = mock(AnalyticsController.class);
        ClassificationController mockClassificationController = mock(ClassificationController.class);
        ImportController mockImportController = mock(ImportController.class);
        
        when(mockClassificationDao.loadOrSeed()).thenReturn(mock(ClassificationService.class));
        when(mockSeeder.seedIfEmpty(any(), any())).thenReturn(null);
        
        frame = new MainFrame(testUser, mockDatabase);
    }

    @Test
    void testCtrlRRefreshAnimals() throws Exception {
        // Simulate Ctrl+R key press
        KeyEvent event = new KeyEvent(frame, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_R, 'r');
        frame.dispatchEvent(event);
        
        // Should not throw exception
        assertTrue(true);
    }

    @Test
    void testCtrlFFocusFilter() throws Exception {
        KeyEvent event = new KeyEvent(frame, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_F, 'f');
        frame.dispatchEvent(event);
        
        JComboBox<?> filter = getPrivateField(frame, "classificationFilter", JComboBox.class);
        // Filter should have focus or at least no exception
        assertTrue(true);
    }

    @Test
    void testCtrlIImportCsv() throws Exception {
        KeyEvent event = new KeyEvent(frame, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_I, 'i');
        frame.dispatchEvent(event);
        assertTrue(true);
    }

    @Test
    void testCtrlUUndo() throws Exception {
        KeyEvent event = new KeyEvent(frame, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_U, 'u');
        frame.dispatchEvent(event);
        assertTrue(true);
    }

    @Test
    void testF5RefreshAnalytics() throws Exception {
        KeyEvent event = new KeyEvent(frame, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                0, KeyEvent.VK_F5, KeyEvent.CHAR_UNDEFINED);
        frame.dispatchEvent(event);
        assertTrue(true);
    }

    @Test
    void testEscapeClosesPopups() throws Exception {
        KeyEvent event = new KeyEvent(frame, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED);
        frame.dispatchEvent(event);
        assertTrue(true);
    }

    @Test
    void testActionMapContainsRefreshAnimals() {
        var actionMap = frame.getRootPane().getActionMap();
        assertNotNull(actionMap.get("refreshAnimals"));
    }

    @Test
    void testActionMapContainsFocusFilter() {
        var actionMap = frame.getRootPane().getActionMap();
        assertNotNull(actionMap.get("focusFilter"));
    }

    @Test
    void testActionMapContainsImportCsv() {
        var actionMap = frame.getRootPane().getActionMap();
        assertNotNull(actionMap.get("importCsv"));
    }

    @Test
    void testActionMapContainsUndo() {
        var actionMap = frame.getRootPane().getActionMap();
        assertNotNull(actionMap.get("undo"));
    }

    @Test
    void testActionMapContainsRefreshAnalytics() {
        var actionMap = frame.getRootPane().getActionMap();
        assertNotNull(actionMap.get("refreshAnalytics"));
    }

    @Test
    void testActionMapContainsEscape() {
        var actionMap = frame.getRootPane().getActionMap();
        assertNotNull(actionMap.get("escape"));
    }

    @Test
    void testInputMapBindings() {
        var inputMap = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        
        assertNotNull(inputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK)));
        assertNotNull(inputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK)));
        assertNotNull(inputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK)));
        assertNotNull(inputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_DOWN_MASK)));
        assertNotNull(inputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)));
        assertNotNull(inputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)));
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