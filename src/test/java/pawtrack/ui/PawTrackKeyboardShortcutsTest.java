package pawtrack.ui;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.event.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class PawTrackKeyboardShortcutsTest {
    @Test void shortcutBindingsDelegateActions() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JRootPane root = new JRootPane();
            AtomicInteger refresh = new AtomicInteger(), filter = new AtomicInteger(),
                    importCount = new AtomicInteger(), undo = new AtomicInteger(),
                    export = new AtomicInteger(), analytics = new AtomicInteger(), esc = new AtomicInteger();
            PawTrackKeyboardShortcuts.install(root, refresh::incrementAndGet, filter::incrementAndGet,
                    importCount::incrementAndGet, undo::incrementAndGet, export::incrementAndGet,
                    analytics::incrementAndGet, esc::incrementAndGet);
            int ctrl = InputEvent.CTRL_DOWN_MASK;
            verify(root, KeyEvent.VK_R, ctrl, "refreshAnimals");
            verify(root, KeyEvent.VK_F, ctrl, "focusFilter");
            verify(root, KeyEvent.VK_I, ctrl, "importCsv");
            verify(root, KeyEvent.VK_U, ctrl, "undo");
            verify(root, KeyEvent.VK_E, ctrl, "exportCsv");
            verify(root, KeyEvent.VK_F5, 0, "refreshAnalytics");
            verify(root, KeyEvent.VK_ESCAPE, 0, "escape");
            assertEquals(1, refresh.get()); assertEquals(1, filter.get());
            assertEquals(1, importCount.get()); assertEquals(1, undo.get());
            assertEquals(1, export.get()); assertEquals(1, analytics.get()); assertEquals(1, esc.get());
        });
    }
    private static void verify(JRootPane root, int key, int mods, String expected) {
        Object name = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(KeyStroke.getKeyStroke(key, mods));
        assertEquals(expected, name);
        Action action = root.getActionMap().get(name);
        assertNotNull(action);
        action.actionPerformed(new ActionEvent(root, ActionEvent.ACTION_PERFORMED, expected));
    }
}
