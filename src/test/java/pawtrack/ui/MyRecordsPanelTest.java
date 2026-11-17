package pawtrack.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JScrollPane;
import org.junit.jupiter.api.Test;

class MyRecordsPanelTest {
    @Test
    void showsReadOnlyRecordsAreaInsideScrollPane() {
        MyRecordsPanel panel = new MyRecordsPanel(Color.WHITE);
        assertFalse(panel.getRecordsArea().isEditable());
        assertTrue(panel.getLayout() instanceof BorderLayout);
        assertTrue(panel.getComponent(0) instanceof JScrollPane);
        assertSame(panel.getRecordsArea(),
                ((JScrollPane) panel.getComponent(0)).getViewport().getView());
    }

    @Test
    void keepsTextAndCaretAccessibleToMainFrame() {
        MyRecordsPanel panel = new MyRecordsPanel(Color.WHITE);
        panel.getRecordsArea().setText("My Waitlist\n  (none)\n");
        panel.getRecordsArea().setCaretPosition(0);
        assertEquals("My Waitlist\n  (none)\n", panel.getRecordsArea().getText());
        assertEquals(0, panel.getRecordsArea().getCaretPosition());
    }

    @Test
    void adoptsExistingSurfaceColorAndRejectsNull() {
        Color blue = new Color(0xB7D7F0);
        MyRecordsPanel panel = new MyRecordsPanel(blue);
        assertEquals(blue, panel.getBackground());
        assertEquals(blue, panel.getRecordsArea().getBackground());
        assertThrows(NullPointerException.class, () -> new MyRecordsPanel(null));
    }
}
