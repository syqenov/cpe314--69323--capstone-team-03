package pawtrack.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;

/** Lightweight theme checks, runnable in Maven's headless test environment. */
class PawTrackDarkModeTest {
    @AfterEach void reset() { PawTrackTheme.setDark(false); }

    @Test void approvedMidnightNavyPaletteIsUsed() {
        PawTrackTheme.setDark(true);
        assertEquals(new Color(0x101B2B), PawTrackTheme.canvas());
        assertEquals(new Color(0x20334D), PawTrackTheme.surface());
        assertEquals(new Color(0xF1F6FF), PawTrackTheme.text());
        assertEquals(new Color(0x729DF5), PawTrackTheme.accent());
    }

    @Test void nestedInputsAndLabelsReturnToOriginalColorsAfterSwitch() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel root = new JPanel(new BorderLayout());
            root.setBackground(new Color(0xEFF5FF));
            JLabel title = new JLabel("Settings");
            title.setForeground(new Color(0x1B345D));
            JTextField input = new JTextField("Sample");
            input.setBackground(Color.WHITE);
            root.add(title, BorderLayout.NORTH);
            root.add(input, BorderLayout.CENTER);
            Color oldBg = root.getBackground();
            Color oldText = title.getForeground();
            Color oldInput = input.getBackground();
            PawTrackTheme.setDark(true);
            PawTrackThemeManager.applyCurrent(root);
            assertEquals(PawTrackTheme.DARK_CANVAS, root.getBackground());
            assertEquals(PawTrackTheme.DARK_TEXT, title.getForeground());
            assertEquals(PawTrackTheme.DARK_INPUT, input.getBackground());
            PawTrackTheme.setDark(false);
            PawTrackThemeManager.applyCurrent(root);
            assertEquals(oldBg, root.getBackground());
            assertEquals(oldText, title.getForeground());
            assertEquals(oldInput, input.getBackground());
        });
    }

    @Test void roundedCardsPaintDarkAndRestoreLightSurfaceWithoutRecreation() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel card = PremiumUi.card(new BorderLayout());
            card.setSize(240, 140);
            // The approved light card surface is #F8FBFF, not pure white.
            // Test both directions on the SAME component to catch stale paints.
            PawTrackTheme.setDark(false);
            BufferedImage light = paint(card);
            assertEquals(PawTrackTheme.SURFACE.getRGB(), light.getRGB(30, 30));

            PawTrackTheme.setDark(true);
            BufferedImage dark = paint(card);
            assertEquals(PawTrackTheme.DARK_SURFACE.getRGB(), dark.getRGB(30, 30));

            PawTrackTheme.setDark(false);
            BufferedImage restored = paint(card);
            assertEquals(PawTrackTheme.SURFACE.getRGB(), restored.getRGB(30, 30));
            assertEquals(light.getRGB(30, 30), restored.getRGB(30, 30));
        });
    }

    private static BufferedImage paint(JPanel panel) {
        BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try { panel.paint(graphics); }
        finally { graphics.dispose(); }
        return image;
    }

    @Test void headerIncludesLiveThemeControl() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PawTrackHeader header = new PawTrackHeader("ADOPTER");
            boolean[] dark = {false};
            header.setAppearanceAction(() -> dark[0] = !dark[0], () -> dark[0]);
            JButton toggle = findButton(header, "globalThemeToggle");
            assertNotNull(toggle);
            assertEquals("Dark Mode", toggle.getText());
            toggle.doClick();
            header.refreshAppearanceButton();
            assertEquals("Light Mode", toggle.getText());
        });
    }

    private JButton findButton(Container panel, String name) {
        for (Component c : panel.getComponents()) {
            if (c instanceof JButton b && name.equals(b.getName())) return b;
            if (c instanceof Container child) {
                JButton result = findButton(child, name);
                if (result != null) return result;
            }
        }
        return null;
    }
}
