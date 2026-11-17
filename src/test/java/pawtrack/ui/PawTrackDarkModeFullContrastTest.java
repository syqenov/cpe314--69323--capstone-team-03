package pawtrack.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** Regression coverage for Windows-LAF/native controls and late-created cards. */
class PawTrackDarkModeFullContrastTest {
    @AfterEach void restore() { PawTrackTheme.setDark(false); }

    @Test void comboBoxesHaveVisibleDarkTextAndRestoreTheirOriginalUI() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PawTrackTheme.setDark(false);
            JComboBox<String> combo = new JComboBox<>(new String[]{"All statuses", "Available"});
            combo.setBackground(Color.WHITE);
            combo.setForeground(PawTrackTheme.NAVY);
            JPanel root = new JPanel(); root.add(combo);
            Object originalUI = combo.getUI();
            PawTrackTheme.setDark(true);
            PawTrackThemeManager.applyCurrent(root);
            assertEquals(PawTrackTheme.DARK_INPUT, combo.getBackground());
            assertEquals(PawTrackTheme.DARK_TEXT, combo.getForeground());
            Component cell = combo.getRenderer().getListCellRendererComponent(
                    new JList<String>(), "Available", 0, false, false);
            assertEquals(PawTrackTheme.DARK_INPUT, cell.getBackground());
            assertEquals(PawTrackTheme.DARK_TEXT, cell.getForeground());
            PawTrackTheme.setDark(false);
            PawTrackThemeManager.applyCurrent(root);
            assertSame(originalUI, combo.getUI());
            assertEquals(Color.WHITE, combo.getBackground());
            assertEquals(PawTrackTheme.NAVY, combo.getForeground());
        });
    }

    @Test void topBarButtonsDrawDarkFillAndRestoreLightFill() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PawTrackTheme.setDark(false);
            PawTrackHeader header = new PawTrackHeader("ADOPTER", () -> {});
            JButton toggle = findButton(header, "globalThemeToggle");
            assertNotNull(toggle);
            PawTrackTheme.setDark(true);
            PawTrackThemeManager.applyCurrent(header);
            assertEquals(PawTrackTheme.DARK_INPUT.getRGB(), paint(toggle, 130, 38).getRGB(7, 7));
            assertEquals(PawTrackTheme.DARK_TEXT, toggle.getForeground());
            PawTrackTheme.setDark(false);
            PawTrackThemeManager.applyCurrent(header);
            assertEquals(new Color(0xEDF3FD).getRGB(), paint(toggle, 130, 38).getRGB(7, 7));
        });
    }

    @Test void chartsRenderLightLabelsOnDarkBackground() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PawTrackTheme.setDark(false);
            BarChartPanel chart = new BarChartPanel();
            chart.setFont(new Font("Dialog", Font.PLAIN, 12));
            chart.setData("Adoptions by Species", "Species", "Adoptions",
                    Map.of("Dog", 50, "Cat", 30), "%.0f");
            PawTrackTheme.setDark(true);
            PawTrackThemeManager.applyCurrent(chart);
            BufferedImage snapshot = paint(chart, 800, 410);
            int lightInk = 0;
            // The title sits at y=28. Dark titles previously disappeared into the card.
            for (int y = 12; y < 33; y++) for (int x = 180; x < 620; x++) {
                Color pixel = new Color(snapshot.getRGB(x, y));
                if (pixel.getRed() > 175 && pixel.getGreen() > 185 && pixel.getBlue() > 190) lightInk++;
            }
            assertTrue(lightInk > 12, "Chart title must be drawn in visible light ink");
        });
    }

    @Test void newActivityMessagesFollowBothThemes() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PawTrackTheme.setDark(false);
            PawTrackActivityLogPanel activity = new PawTrackActivityLogPanel();
            PawTrackTheme.setDark(true);
            PawTrackThemeManager.applyCurrent(activity);
            activity.appendEvent("Classification tree loaded: 22 nodes");
            JLabel message = findLabel(activity, "Classification tree loaded:");
            assertNotNull(message);
            assertEquals(PawTrackTheme.DARK_TEXT, message.getForeground());
            PawTrackTheme.setDark(false);
            PawTrackThemeManager.applyCurrent(activity);
            assertEquals(PawTrackTheme.NAVY, message.getForeground());
        });
    }

    @Test void confirmationButtonsAndWaitlistRefreshAreReadable() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PawTrackTheme.setDark(false);
            JOptionPane options = new JOptionPane("Log out?", JOptionPane.QUESTION_MESSAGE);
            JButton yes = new JButton("Yes"); options.add(yes);
            JButton refresh = new JButton("Refresh Waitlist");
            WaitlistPanel waitlist = new WaitlistPanel(PawTrackTheme.SURFACE, PawTrackTheme.BLUE,
                    PawTrackTheme.LINE, refresh, new JButton("Reserve"), new JButton("Complete"),
                    new JButton("Cancel"), sc -> {});
            PawTrackTheme.setDark(true);
            PawTrackThemeManager.applyCurrent(options);
            PawTrackThemeManager.applyCurrent(waitlist);
            assertEquals(PawTrackTheme.DARK_INPUT, yes.getBackground());
            assertEquals(PawTrackTheme.DARK_TEXT, yes.getForeground());
            assertEquals(PawTrackTheme.DARK_TEXT, refresh.getForeground());
            BufferedImage pixels = paint(refresh, 210, 55);
            Color fill = new Color(pixels.getRGB(8, 15));
            assertTrue(fill.getRed() < 110 && fill.getGreen() < 130,
                    "Waitlist Refresh background must not stay white in dark mode");
        });
    }

    private static BufferedImage paint(Component c, int width, int height) {
        c.setSize(width, height);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = image.createGraphics();
        try { c.paint(graphics); } finally { graphics.dispose(); }
        return image;
    }
    private static JButton findButton(Container container, String name) {
        for (Component c : container.getComponents()) {
            if (c instanceof JButton button && name.equals(button.getName())) return button;
            if (c instanceof Container nested) {
                JButton result = findButton(nested, name);
                if (result != null) return result;
            }
        }
        return null;
    }
    private static JLabel findLabel(Container container, String text) {
        for (Component c : container.getComponents()) {
            if (c instanceof JLabel label && label.getText() != null
                    && label.getText().contains(text)) return label;
            if (c instanceof Container nested) {
                JLabel result = findLabel(nested, text);
                if (result != null) return result;
            }
        }
        return null;
    }
}
