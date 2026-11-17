package pawtrack.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import org.junit.jupiter.api.Test;

class WaitlistPanelTest {
    private WaitlistPanel create() {
        return new WaitlistPanel(Color.WHITE, Color.BLUE, Color.GRAY,
                new JButton("Refresh"), new JButton("Reserve"),
                new JButton("Complete"), new JButton("Cancel"), scroll -> {});
    }

    @Test void showsActionsAndTableTogether() {
        WaitlistPanel panel = create();
        assertTrue(panel.getLayout() instanceof BorderLayout);
        assertTrue(panel.getComponent(0) instanceof WaitlistActionsPanel);
        assertSame(panel.getTable(), panel.getScrollPane().getViewport().getView());
        assertEquals(2, panel.getComponentCount());
    }

    @Test void preservesAllSixColumnsAndReadOnlyCells() {
        WaitlistPanel panel = create();
        assertEquals(6, panel.getTableModel().getColumnCount());
        assertEquals("Review", panel.getTableModel().getColumnName(5));
        panel.getTableModel().addRow(new Object[]{"Milo", "Adopter", "email", "Today", 1, "WAITING"});
        assertFalse(panel.getTableModel().isCellEditable(0, 0));
        assertEquals("Milo", panel.getTable().getValueAt(0, 0));
    }

    @Test void keepsTableVisibleAndAppliesScrollStyling() {
        final boolean[] styled = {false};
        WaitlistPanel panel = new WaitlistPanel(Color.WHITE, Color.BLUE, Color.GRAY,
                new JButton(), new JButton(), new JButton(), new JButton(),
                scroll -> styled[0] = scroll instanceof JScrollPane);
        assertTrue(styled[0]);
        assertSame(panel.getScrollPane(), ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.CENTER));
    }
}
