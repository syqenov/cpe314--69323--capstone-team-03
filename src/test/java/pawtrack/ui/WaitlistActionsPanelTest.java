package pawtrack.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;

class WaitlistActionsPanelTest {
    @Test
    void displaysAllFourActionsInTwoRows() {
        WaitlistActionsPanel panel = new WaitlistActionsPanel(
                new JButton("Refresh Waitlist"), new JButton("Reserve First in Queue"),
                new JButton("Complete Selected Adoption"), new JButton("Cancel Reservation"));
        assertEquals(2, panel.getComponentCount());
        JPanel first = (JPanel) panel.getComponent(0);
        JPanel second = (JPanel) panel.getComponent(1);
        assertEquals(2, first.getComponentCount());
        assertEquals(2, second.getComponentCount());
        assertEquals("Refresh Waitlist", ((JButton) first.getComponent(0)).getText());
        assertEquals("Reserve First in Queue", ((JButton) first.getComponent(1)).getText());
        assertEquals("Complete Selected Adoption", ((JButton) second.getComponent(0)).getText());
        assertEquals("Cancel Reservation", ((JButton) second.getComponent(1)).getText());
        assertFalse(panel.isOpaque());
    }

    @Test
    void retainsOriginalButtonActions() {
        AtomicInteger calls = new AtomicInteger();
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(event -> calls.incrementAndGet());
        WaitlistActionsPanel panel = new WaitlistActionsPanel(refresh, new JButton("Reserve"),
                new JButton("Complete"), new JButton("Cancel"));
        ((JButton) ((JPanel) panel.getComponent(0)).getComponent(0)).doClick();
        assertEquals(1, calls.get());
    }

    @Test
    void rejectsMissingActionButtons() {
        assertThrows(NullPointerException.class,
                () -> new WaitlistActionsPanel(null, new JButton(), new JButton(), new JButton()));
    }
}
