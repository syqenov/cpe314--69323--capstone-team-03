package pawtrack.ui;

import org.junit.jupiter.api.Test;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import static org.junit.jupiter.api.Assertions.*;

class PawTrackCompleteGuiTest {
    @Test void compactSidebarCanCollapseAndRestoreFullNavigation() {
        JButton dashboard = new JButton("Dashboard");
        JButton tools = new JButton("Tools & Settings");
        PawTrackSidebar sidebar = new PawTrackSidebar(List.of(dashboard, tools),
                new JScrollPane(new JPanel()), true);
        JButton toggle = null;
        for (Component component : ((JPanel) ((BorderLayout) sidebar.getLayout())
                .getLayoutComponent(BorderLayout.NORTH)).getComponents()) {
            if (component instanceof JButton button) toggle = button;
        }
        assertNotNull(toggle);
        assertEquals(248, sidebar.getPreferredSize().width);
        toggle.doClick();
        assertEquals(76, sidebar.getPreferredSize().width);
        assertEquals("", dashboard.getText()); // icon-only rail preserves space
        assertNotNull(dashboard.getIcon());
        assertEquals("Dashboard", dashboard.getToolTipText());
        assertEquals("Tools & Settings", tools.getToolTipText());
        toggle.doClick();
        assertEquals(248, sidebar.getPreferredSize().width);
        assertEquals("Dashboard", dashboard.getText());
    }

    @Test void toolsPageKeepsExistingActionsAccessible() {
        JButton action = new JButton("Import Intake History (CSV)");
        JPanel actions = new JPanel();
        actions.add(action);
        JScrollPane scroll = new JScrollPane(actions);
        PawTrackToolsPanel panel = new PawTrackToolsPanel(scroll);
        assertNotNull(panel);
        assertSame(actions, scroll.getViewport().getView());
        assertSame(action, actions.getComponent(0));
        assertThrows(IllegalArgumentException.class, () -> new PawTrackToolsPanel(null));
    }

    @Test void readOnlyRecordsRemainAccessibleAfterVisualPolish() {
        MyRecordsPanel panel = new MyRecordsPanel(java.awt.Color.WHITE);
        assertFalse(panel.getRecordsArea().isEditable());
        assertTrue(panel.getRecordsArea().getLineWrap());
        panel.getRecordsArea().setText("My Waitlist\n  Milo [WAITING]");
        assertTrue(panel.getRecordsArea().getText().contains("WAITING"));
    }
}
