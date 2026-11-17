package pawtrack.ui;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

/** Installs window-level keyboard shortcuts, separate from application actions. */
final class PawTrackKeyboardShortcuts {
    private PawTrackKeyboardShortcuts() { }

    static void install(JRootPane root,
                        Runnable refreshAnimals, Runnable focusFilter,
                        Runnable importCsv, Runnable undo,
                        Runnable exportCsv, Runnable refreshAnalytics,
                        Runnable dismissPopup) {
        Objects.requireNonNull(root, "root");
        bind(root, KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK, "refreshAnimals", refreshAnimals);
        bind(root, KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK, "focusFilter", focusFilter);
        bind(root, KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK, "importCsv", importCsv);
        bind(root, KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK, "undo", undo);
        bind(root, KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK, "exportCsv", exportCsv);
        bind(root, KeyEvent.VK_F5, 0, "refreshAnalytics", refreshAnalytics);
        bind(root, KeyEvent.VK_ESCAPE, 0, "escape", dismissPopup);
    }

    private static void bind(JRootPane root, int key, int modifiers, String name, Runnable action) {
        Objects.requireNonNull(action, name);
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(key, modifiers), name);
        root.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                action.run();
            }
        });
    }
}
