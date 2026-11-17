package pawtrack.ui;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class PawTrackTabNavigatorTest {
    @Test void selectsCardsAndCallsDashboardRefreshOnlyForDashboard() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CardLayout layout = new CardLayout();
            JPanel content = new JPanel(layout);
            JPanel dashboard = new JPanel(), animals = new JPanel();
            content.add(dashboard, "Dashboard");
            content.add(animals, "Animals");
            RoundedUi.RoundedButton first = new RoundedUi.RoundedButton("Dashboard", 18, Color.GRAY);
            RoundedUi.RoundedButton second = new RoundedUi.RoundedButton("Animals", 18, Color.GRAY);
            AtomicInteger badge = new AtomicInteger(), refresh = new AtomicInteger();
            List<RoundedUi.RoundedButton> buttons = List.of(first, second);
            List<String> names = List.of("Dashboard", "Animals");
            PawTrackTabNavigator.select("Animals", layout, content, buttons, names,
                    Color.BLUE, Color.GRAY, badge::incrementAndGet, refresh::incrementAndGet);
            assertFalse(dashboard.isVisible()); assertTrue(animals.isVisible());
            assertEquals(1, badge.get()); assertEquals(0, refresh.get());
            PawTrackTabNavigator.select("Dashboard", layout, content, buttons, names,
                    Color.BLUE, Color.GRAY, badge::incrementAndGet, refresh::incrementAndGet);
            assertTrue(dashboard.isVisible()); assertFalse(animals.isVisible());
            assertEquals(2, badge.get()); assertEquals(1, refresh.get());
        });
    }
}
