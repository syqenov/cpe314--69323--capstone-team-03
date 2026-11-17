package pawtrack.ui;

import java.awt.Color;

/**
 * Shared light-theme palette for the PawTrack Swing workspace.
 * Semantic status colors (success, pending, error) remain separate.
 * Visual tokens only: no controller, persistence, or navigation logic.
 */
public final class PawTrackTheme {
    // The single theme state for the desktop workspace. Only theme actions update it.
    private static volatile boolean dark;
    public static boolean isDark() { return dark; }
    public static void setDark(boolean enabled) { dark = enabled; }

    // Approved Midnight Navy colors, shared by Swing controls and Java2D painters.
    public static final Color DARK_CANVAS = new Color(0x101B2B);
    public static final Color DARK_SIDEBAR = new Color(0x152A45);
    public static final Color DARK_SURFACE = new Color(0x20334D);
    public static final Color DARK_INPUT = new Color(0x263B55);
    public static final Color DARK_ACCENT = new Color(0x729DF5);
    public static final Color DARK_SELECTED = new Color(0x315FAD);
    public static final Color DARK_BORDER = new Color(0x344A66);
    public static final Color DARK_TEXT = new Color(0xF1F6FF);
    public static final Color DARK_MUTED = new Color(0xA9BCD3);
    public static final Color DARK_HOVER = new Color(0x2A4669);

    public static Color canvas() { return dark ? DARK_CANVAS : CANVAS; }
    public static Color surface() { return dark ? DARK_SURFACE : SURFACE; }
    public static Color accent() { return dark ? DARK_ACCENT : BLUE; }
    public static Color text() { return dark ? DARK_TEXT : NAVY; }
    public static Color muted() { return dark ? DARK_MUTED : MUTED; }
    public static Color line() { return dark ? DARK_BORDER : LINE; }

    /** For custom-painted card and background fills; semantic status hues remain colored. */
    public static Color fill(Color light) {
        if (!dark || light == null) return light;
        if (light.getAlpha() < 200) return light;
        int r = light.getRed(), g = light.getGreen(), b = light.getBlue();
        if (r > 205 && g > 205 && b > 205) {
            return r > 247 && g > 247 ? DARK_SURFACE : DARK_INPUT;
        }
        if (r < 72 && g < 118 && b < 175) return DARK_HOVER;
        // Preserve strong colors for status/alerts; tone down pale blue hero surfaces.
        if (b > r && b > g && r > 140) return DARK_INPUT;
        return light;
    }

    private PawTrackTheme() { }

    public static final Color NAVY = new Color(0x1B345D);
    public static final Color BLUE = new Color(0x2E62AD);
    public static final Color BLUE_HOVER = new Color(0x3974C6);
    public static final Color SIDEBAR_BUTTON = new Color(0x294972);
    public static final Color CANVAS = new Color(0xEFF5FF);
    public static final Color SURFACE = new Color(0xF8FBFF);
    public static final Color ALT_ROW = new Color(0xEAF3FF);
    public static final Color LINE = new Color(0xD6E3F6);
    public static final Color MUTED = new Color(0x617697);
    public static final Color HERO_START = new Color(0x1D457F);
    public static final Color HERO_END = new Color(0x3E79C8);
}
