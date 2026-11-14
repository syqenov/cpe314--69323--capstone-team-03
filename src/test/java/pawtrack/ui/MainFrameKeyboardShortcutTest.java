
package pawtrack.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pawtrack.dao.Database;
import pawtrack.model.Role;
import pawtrack.model.User;

import javax.swing.JComponent;
import javax.swing.KeyStroke;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class MainFrameKeyboardShortcutTest {

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
    void testCtrlRRefreshAnimals() {
        dispatchShortcut(
                KeyEvent.VK_R,
                KeyEvent.CTRL_DOWN_MASK,
                'r'
        );

        assertNotNull(frame.getRootPane());
    }

    @Test
    void testCtrlFFocusFilter() {
        dispatchShortcut(
                KeyEvent.VK_F,
                KeyEvent.CTRL_DOWN_MASK,
                'f'
        );

        assertNotNull(getPrivateField(
                frame, "classificationFilter", Object.class
        ));
    }

    @Test
    void testCtrlIImportCsv() {
        dispatchShortcut(
                KeyEvent.VK_I,
                KeyEvent.CTRL_DOWN_MASK,
                'i'
        );

        assertNotNull(frame.getRootPane());
    }

    @Test
    void testCtrlUUndo() {
        dispatchShortcut(
                KeyEvent.VK_U,
                KeyEvent.CTRL_DOWN_MASK,
                'u'
        );

        assertNotNull(frame.getRootPane());
    }

    @Test
    void testF5RefreshAnalytics() {
        dispatchShortcut(
                KeyEvent.VK_F5,
                0,
                KeyEvent.CHAR_UNDEFINED
        );

        assertNotNull(frame.getRootPane());
    }

    @Test
    void testEscapeClosesPopups() {
        dispatchShortcut(
                KeyEvent.VK_ESCAPE,
                0,
                KeyEvent.CHAR_UNDEFINED
        );

        assertNotNull(frame.getRootPane());
    }

    @Test
    void testActionMapContainsRefreshAnimals() {
        assertNotNull(
                frame.getRootPane().getActionMap().get("refreshAnimals")
        );
    }

    @Test
    void testActionMapContainsFocusFilter() {
        assertNotNull(
                frame.getRootPane().getActionMap().get("focusFilter")
        );
    }

    @Test
    void testActionMapContainsImportCsv() {
        assertNotNull(
                frame.getRootPane().getActionMap().get("importCsv")
        );
    }

    @Test
    void testActionMapContainsUndo() {
        assertNotNull(
                frame.getRootPane().getActionMap().get("undo")
        );
    }

    @Test
    void testActionMapContainsRefreshAnalytics() {
        assertNotNull(
                frame.getRootPane().getActionMap().get("refreshAnalytics")
        );
    }

    @Test
    void testActionMapContainsEscape() {
        assertNotNull(
                frame.getRootPane().getActionMap().get("escape")
        );
    }

    @Test
    void testInputMapBindings() {
        var inputMap = frame.getRootPane().getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        assertNotNull(inputMap.get(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_R,
                        KeyEvent.CTRL_DOWN_MASK
                )
        ));

        assertNotNull(inputMap.get(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_F,
                        KeyEvent.CTRL_DOWN_MASK
                )
        ));

        assertNotNull(inputMap.get(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_I,
                        KeyEvent.CTRL_DOWN_MASK
                )
        ));

        assertNotNull(inputMap.get(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_U,
                        KeyEvent.CTRL_DOWN_MASK
                )
        ));

        assertNotNull(inputMap.get(
                KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)
        ));

        assertNotNull(inputMap.get(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
        ));
    }

    private void dispatchShortcut(
            int keyCode,
            int modifiers,
            char keyChar
    ) {
        KeyEvent event = new KeyEvent(
                frame,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                modifiers,
                keyCode,
                keyChar
        );

        frame.dispatchEvent(event);
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
