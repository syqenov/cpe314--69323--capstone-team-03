package pawtrack.ui;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PawTrackShellTest {
    @Test void headerDisplaysAppName() {
        PawTrackHeader header = new PawTrackHeader("STAFF");
        assertTrue(header.getComponentCount() >= 2);
        assertEquals(Color.WHITE, header.getBackground());
    }

    @Test void sidebarKeepsNavigationAndActions() {
        JButton animal = new JButton("Animals");
        JButton records = new JButton("My Records");
        JScrollPane actions = new JScrollPane(new JPanel());
        PawTrackSidebar sidebar = new PawTrackSidebar(List.of(animal, records), actions);
        assertEquals(290, sidebar.getPreferredSize().width);
        assertNotNull(((BorderLayout) sidebar.getLayout()).getLayoutComponent(BorderLayout.NORTH));
        assertNotNull(((BorderLayout) sidebar.getLayout()).getLayoutComponent(BorderLayout.CENTER));
    }

    @Test void sidebarRejectsMissingControls() {
        assertThrows(IllegalArgumentException.class, () -> new PawTrackSidebar(null, new JScrollPane()));
    }
}
