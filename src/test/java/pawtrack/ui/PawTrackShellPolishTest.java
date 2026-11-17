package pawtrack.ui;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class PawTrackShellPolishTest {
    @Test void navigationIsScrollableAndKeepsPages() {
        JButton first = new JButton("Dashboard");
        JButton second = new JButton("Animals");
        PawTrackSidebar sidebar = new PawTrackSidebar(List.of(first, second), new JScrollPane(new JPanel()));
        Component north = ((BorderLayout) sidebar.getLayout()).getLayoutComponent(BorderLayout.NORTH);
        assertInstanceOf(JScrollPane.class, north);
        JPanel navigation = (JPanel) ((JScrollPane) north).getViewport().getView();
        assertTrue(List.of(navigation.getComponents()).contains(first));
        assertTrue(List.of(navigation.getComponents()).contains(second));
    }

    @Test void actionsRemainAvailableInScrollableCenter() {
        JScrollPane actions = new JScrollPane(new JPanel());
        PawTrackSidebar sidebar = new PawTrackSidebar(List.of(new JButton("Dashboard")), actions);
        JPanel holder = (JPanel) ((BorderLayout) sidebar.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        assertSame(actions, ((BorderLayout) holder.getLayout()).getLayoutComponent(BorderLayout.CENTER));
        assertEquals(16, actions.getVerticalScrollBar().getUnitIncrement());
    }

    @Test void notificationsRemainAdopterOnlyAndClickable() {
        AtomicInteger clicks = new AtomicInteger();
        PawTrackHeader adopter = new PawTrackHeader("ADOPTER", clicks::incrementAndGet);
        PawTrackHeader staff = new PawTrackHeader("STAFF", clicks::incrementAndGet);
        JPanel adopterActions = (JPanel) ((BorderLayout) adopter.getLayout()).getLayoutComponent(BorderLayout.EAST);
        JPanel staffActions = (JPanel) ((BorderLayout) staff.getLayout()).getLayoutComponent(BorderLayout.EAST);
        assertEquals(2, adopterActions.getComponentCount());
        assertEquals(1, staffActions.getComponentCount());
        ((JButton) adopterActions.getComponent(0)).doClick();
        assertEquals(1, clicks.get());
    }
}
