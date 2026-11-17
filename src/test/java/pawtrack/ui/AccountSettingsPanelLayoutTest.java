package pawtrack.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pawtrack.controller.AuthController;
import pawtrack.dao.AdopterDao;
import pawtrack.dao.Database;
import pawtrack.dao.UserDao;
import pawtrack.service.AccountPreferencesService;
import pawtrack.service.AccountSettingsService;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AccountSettingsPanelLayoutTest {
    private Database database;
    private AuthController auth;

    @BeforeEach
    void setUp() throws Exception {
        database = Database.openInMemory();
        auth = new AuthController(new UserDao(database));
        assertTrue(auth.login("user", "user123").isPresent());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (database != null) database.close();
    }

    @Test
    void profileSecurityAndPreferencesKeepTheirActionButtonsInsideScrollablePages() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                AccountSettingsPanel panel = newPanel();
                JTabbedPane tabs = find(panel, JTabbedPane.class);
                assertNotNull(tabs);
                assertEquals(3, tabs.getTabCount());
                assertEquals("My Profile", tabs.getTitleAt(0));
                assertEquals("Security", tabs.getTitleAt(1));
                assertEquals("Preferences", tabs.getTitleAt(2));
                String[] actions = {"Save Changes", "Update Password", "Save Preferences"};
                for (int i = 0; i < tabs.getTabCount(); i++) {
                    assertInstanceOf(JScrollPane.class, tabs.getComponentAt(i));
                    JScrollPane scroll = (JScrollPane) tabs.getComponentAt(i);
                    assertEquals(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                            scroll.getVerticalScrollBarPolicy());
                    assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER,
                            scroll.getHorizontalScrollBarPolicy());
                    assertNotNull(findButton(scroll.getViewport().getView(), actions[i]));
                }
            } catch (Throwable t) { failure.set(t); }
        });
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    @Test
    void profileInputHeightsStayCompactWhenWorkspaceIsResized() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                AccountSettingsPanel panel = newPanel();
                panel.setSize(860, 500);
                panel.doLayout();
                for (String fieldName : new String[]{"name", "email", "phone"}) {
                    Field field = AccountSettingsPanel.class.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    JTextField input = (JTextField) field.get(panel);
                    assertTrue(input.getPreferredSize().height <= 48,
                            fieldName + " should not stretch to a full grid row");
                    assertTrue(input.getPreferredSize().height >= 36);
                }
            } catch (Throwable t) { failure.set(t); }
        });
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    private AccountSettingsPanel newPanel() {
        return new AccountSettingsPanel(auth,
                new AccountSettingsService(auth, new AdopterDao(database)),
                new AccountPreferencesService(database, auth));
    }

    private static JButton findButton(Component root, String text) {
        if (root instanceof JButton button && text.equals(button.getText())) return button;
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                JButton match = findButton(child, text);
                if (match != null) return match;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends JComponent> T find(Component root, Class<T> type) {
        if (type.isInstance(root)) return (T) root;
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T match = find(child, type);
                if (match != null) return match;
            }
        }
        return null;
    }
}
