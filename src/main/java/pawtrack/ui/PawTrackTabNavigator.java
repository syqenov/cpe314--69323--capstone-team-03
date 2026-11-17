package pawtrack.ui;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;

/** Keeps the CardLayout tab selection and highlighted button state consistent. */
final class PawTrackTabNavigator {
    private PawTrackTabNavigator() { }

    static void select(String title, CardLayout cards, JPanel content,
                       List<RoundedUi.RoundedButton> buttons, List<String> names,
                       Color selected, Color unselected,
                       Runnable refreshBadge, Runnable refreshDashboard) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(cards, "cards");
        Objects.requireNonNull(content, "content");
        if (buttons.size() != names.size()) {
            throw new IllegalStateException("Tab names and buttons do not match");
        }
        cards.show(content, title);
        refreshBadge.run();
        if ("Dashboard".equals(title)) refreshDashboard.run();
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setBaseColor(names.get(i).equals(title) ? selected : unselected);
        }
    }
}
