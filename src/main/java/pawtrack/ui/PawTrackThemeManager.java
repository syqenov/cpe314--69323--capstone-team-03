package pawtrack.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.plaf.ScrollBarUI;
import javax.swing.plaf.ComboBoxUI;
import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.ListCellRenderer;
import java.awt.Rectangle;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Reversible Swing theme application. Captures the actual light-mode values per
 * component once, so toggling does not progressively darken colors or discard
 * custom UI values. Works with prebuilt tabs, scroll views and table headers.
 */
public final class PawTrackThemeManager {
    private PawTrackThemeManager() {}
    private record Original(Color background, Color foreground, Border border,
                            Color selectionBackground, Color selectionForeground,
                            Color gridColor) {}
    private static final Map<Component, Original> originals =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<JTabbedPane, Color[]> tabOriginals =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static boolean dialogHookInstalled;
    public static synchronized void installDialogHook() {
        if (dialogHookInstalled) return;
        dialogHookInstalled = true;
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (!(event instanceof java.awt.event.WindowEvent windowEvent)) return;
            if (windowEvent.getID() != java.awt.event.WindowEvent.WINDOW_OPENED) return;
            if (!(windowEvent.getWindow() instanceof Dialog dialog)) return;
            SwingUtilities.invokeLater(() -> applyCurrent(dialog));
        }, AWTEvent.WINDOW_EVENT_MASK);
    }

    public static void apply(Window window, boolean dark) {
        installDialogHook();
        PawTrackTheme.setDark(dark);
        applyComponent(window, dark);
        if (window != null) { window.invalidate(); window.validate(); window.repaint(); }
    }

    /** Theme a newly opened dialog or page without changing the stored choice. */
    public static void applyCurrent(Component component) {
        applyComponent(component, PawTrackTheme.isDark());
        if (component != null) component.repaint();
    }

    private static void applyComponent(Component component, boolean dark) {
        if (component == null) return;
        Original previous = originals.computeIfAbsent(component, PawTrackThemeManager::snapshot);
        Color bg = dark ? background(previous.background, component) : previous.background;
        Color fg = dark ? foreground(previous.foreground, component) : previous.foreground;
        if (bg != null) component.setBackground(bg);
        if (fg != null) component.setForeground(fg);
        if (component instanceof JComponent jc) {
            if (previous.border instanceof LineBorder lb) {
                jc.setBorder(dark ? new LineBorder(PawTrackTheme.DARK_BORDER,
                        lb.getThickness(), lb.getRoundedCorners()) : previous.border);
            } else if (!dark && previous.border != null) jc.setBorder(previous.border);
        }
        if (component instanceof javax.swing.text.JTextComponent input) {
            if (dark) {
                input.setCaretColor(PawTrackTheme.DARK_TEXT);
                input.setSelectionColor(PawTrackTheme.DARK_SELECTED);
                input.setSelectedTextColor(Color.WHITE);
            } else {
                input.setCaretColor(previous.foreground == null ? PawTrackTheme.NAVY : previous.foreground);
            }
        }
        if (component instanceof JTable table) {
            installHeaderContrastRenderer(table.getTableHeader());
            if (previous.selectionBackground != null) table.setSelectionBackground(dark ? PawTrackTheme.DARK_SELECTED : previous.selectionBackground);
            if (previous.selectionForeground != null) table.setSelectionForeground(dark ? Color.WHITE : previous.selectionForeground);
            if (previous.gridColor != null) table.setGridColor(dark ? PawTrackTheme.DARK_BORDER : previous.gridColor);
            applyComponent(table.getTableHeader(), dark);
        }
        if (component instanceof JScrollBar scroll) {
            final String key = "pawtrack.originalScrollbarUI";
            if (dark) {
                if (scroll.getClientProperty(key) == null) scroll.putClientProperty(key, scroll.getUI());
                if (!(scroll.getUI() instanceof MidnightScrollBarUI)) scroll.setUI(new MidnightScrollBarUI());
            } else if (scroll.getClientProperty(key) instanceof ScrollBarUI ui
                    && scroll.getUI() != ui) scroll.setUI(ui);
        }
        if (component instanceof JComboBox<?> combo) {
            applyComboContrast(combo, dark);
        }
        if (component instanceof JButton button &&
                SwingUtilities.getAncestorOfClass(JOptionPane.class, button) != null) {
            applyOptionButtonContrast(button, dark);
        }
        if (component instanceof JList<?> list) {
            if (previous.selectionBackground != null) list.setSelectionBackground(dark ? PawTrackTheme.DARK_SELECTED : previous.selectionBackground);
            if (previous.selectionForeground != null) list.setSelectionForeground(dark ? Color.WHITE : previous.selectionForeground);
        }
        if (component instanceof JTabbedPane tabs) {
            Color[] initial = tabOriginals.computeIfAbsent(tabs, pane -> {
                Color[] values = new Color[pane.getTabCount()];
                for (int i = 0; i < values.length; i++) values[i] = pane.getForegroundAt(i);
                return values;
            });
            for (int i = 0; i < tabs.getTabCount(); i++) {
                if (dark) tabs.setForegroundAt(i, i == tabs.getSelectedIndex()
                        ? Color.WHITE : PawTrackTheme.DARK_TEXT);
                else if (i < initial.length) tabs.setForegroundAt(i, initial[i]);
            }
        }
        if (component instanceof JScrollPane scroll) {
            applyComponent(scroll.getViewport(), dark);
            applyComponent(scroll.getColumnHeader(), dark);
        }
        // Custom and default Swing labels can otherwise retain navy text on navy cards.
        // Use the nearest opaque ancestor for transparent labels; keep light-mode
        // colors untouched, and preserve semantic colors if they are readable.
        if (dark && (component instanceof JLabel || component instanceof JButton
                || component instanceof JComboBox<?>)) {
            Color visibleBackground = visibleBackground(component);
            Color currentText = component.getForeground();
            if (visibleBackground != null && currentText != null
                    && contrast(currentText, visibleBackground) < 4.5) {
                component.setForeground(contrast(PawTrackTheme.DARK_TEXT, visibleBackground)
                        >= contrast(PawTrackTheme.NAVY, visibleBackground)
                        ? PawTrackTheme.DARK_TEXT : PawTrackTheme.NAVY);
            }
        }
        if (component instanceof Container parent) {
            for (Component child : parent.getComponents()) applyComponent(child, dark);
        }
    }

    /** Windows Look & Feel can paint a white native combo while the generic
     * theme has already changed its text to near-white. Use a reversible Basic
     * UI + explicit list renderer in dark mode only. Restore the exact original
     * UI and renderer when returning to light mode. */
    private static final String COMBO_UI = "pawtrack.originalComboUI";
    private static final String COMBO_RENDERER = "pawtrack.originalComboRenderer";
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void applyComboContrast(JComboBox<?> combo, boolean dark) {
        if (dark) {
            if (combo.getClientProperty(COMBO_UI) == null) combo.putClientProperty(COMBO_UI, combo.getUI());
            if (combo.getClientProperty(COMBO_RENDERER) == null)
                combo.putClientProperty(COMBO_RENDERER, combo.getRenderer());
            if (!(combo.getUI() instanceof MidnightComboBoxUI)) combo.setUI(new MidnightComboBoxUI());
            if (!(combo.getRenderer() instanceof MidnightComboRenderer)) {
                ListCellRenderer original = (ListCellRenderer) combo.getClientProperty(COMBO_RENDERER);
                combo.setRenderer(new MidnightComboRenderer(original));
            }
            combo.setBackground(PawTrackTheme.DARK_INPUT);
            combo.setForeground(PawTrackTheme.DARK_TEXT);
        } else {
            Object oldUI = combo.getClientProperty(COMBO_UI);
            if (oldUI instanceof ComboBoxUI ui && combo.getUI() != ui) combo.setUI(ui);
            Object renderer = combo.getClientProperty(COMBO_RENDERER);
            if (renderer instanceof ListCellRenderer original && combo.getRenderer() != original)
                combo.setRenderer(original);
            combo.putClientProperty(COMBO_UI, null);
            combo.putClientProperty(COMBO_RENDERER, null);
        }
    }

    private static final class MidnightComboRenderer implements ListCellRenderer<Object> {
        private final ListCellRenderer<Object> delegate;
        MidnightComboRenderer(ListCellRenderer<Object> delegate) { this.delegate = delegate; }
        @Override public Component getListCellRendererComponent(JList<? extends Object> list,
                Object value, int index, boolean selected, boolean focused) {
            Component cell = delegate.getListCellRendererComponent(list, value, index, selected, focused);
            cell.setBackground(selected ? PawTrackTheme.DARK_SELECTED : PawTrackTheme.DARK_INPUT);
            cell.setForeground(PawTrackTheme.DARK_TEXT);
            if (cell instanceof JComponent jc) jc.setOpaque(true);
            return cell;
        }
    }

    private static final class MidnightComboBoxUI extends BasicComboBoxUI {
        @Override protected JButton createArrowButton() {
            BasicArrowButton arrow = new BasicArrowButton(SwingConstants.SOUTH,
                    PawTrackTheme.DARK_INPUT, PawTrackTheme.DARK_BORDER,
                    PawTrackTheme.DARK_MUTED, PawTrackTheme.DARK_BORDER);
            arrow.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
            return arrow;
        }
        @Override public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            g.setColor(PawTrackTheme.DARK_INPUT);
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    /** OptionPane confirmation buttons are created after the theme switch and
     * may retain Windows' white native fill with white text. Change only those
     * buttons, leaving every app-specific rounded button untouched. */
    private static final String OPTION_UI = "pawtrack.originalOptionButtonUI";
    private static void applyOptionButtonContrast(JButton button, boolean dark) {
        if (dark) {
            if (button.getClientProperty(OPTION_UI) == null) button.putClientProperty(OPTION_UI, button.getUI());
            if (!(button.getUI() instanceof BasicButtonUI)) button.setUI(new BasicButtonUI());
            button.setContentAreaFilled(true);
            button.setOpaque(true);
            button.setBackground(PawTrackTheme.DARK_INPUT);
            button.setForeground(PawTrackTheme.DARK_TEXT);
        } else {
            if (button.getClientProperty(OPTION_UI) instanceof ButtonUI ui) {
                button.setUI(ui);
                button.putClientProperty(OPTION_UI, null);
            }
        }
    }

    /** The renderer, not just JTableHeader, determines painted column colors. */
    private static void installHeaderContrastRenderer(JTableHeader header) {
        if (header == null) return;
        TableCellRenderer delegate = header.getDefaultRenderer();
        if (delegate == null || delegate instanceof ContrastHeaderRenderer) return;
        header.setDefaultRenderer(new ContrastHeaderRenderer(delegate));
    }

    private static final class ContrastHeaderRenderer implements TableCellRenderer {
        private final TableCellRenderer delegate;
        private ContrastHeaderRenderer(TableCellRenderer delegate) { this.delegate = delegate; }

        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int col) {
            Component component = delegate.getTableCellRendererComponent(table, value,
                    selected, focused, row, col);
            if (PawTrackTheme.isDark()) {
                component.setBackground(PawTrackTheme.DARK_SIDEBAR);
                component.setForeground(PawTrackTheme.DARK_TEXT);
                if (component instanceof JComponent jc) jc.setOpaque(true);
            } else if (PawTrackTheme.DARK_SIDEBAR.equals(component.getBackground())
                    && PawTrackTheme.DARK_TEXT.equals(component.getForeground())) {
                // Some L&F renderers reuse the same JLabel and do not restore
                // their original colors themselves on the next theme switch.
                component.setBackground(table.getTableHeader().getBackground());
                component.setForeground(table.getTableHeader().getForeground());
            }
            return component;
        }
    }

    private static Color visibleBackground(Component component) {
        for (Component parent = component; parent != null; parent = parent.getParent()) {
            if ((!(parent instanceof JComponent jc) || jc.isOpaque()) && parent.getBackground() != null) return parent.getBackground();
        }
        return null;
    }

    private static double contrast(Color a, Color b) {
        double l1 = luminance(a), l2 = luminance(b);
        return (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
    }

    private static double luminance(Color c) {
        double r = channel(c.getRed()), g = channel(c.getGreen()), b = channel(c.getBlue());
        return r * 0.2126 + g * 0.7152 + b * 0.0722;
    }

    private static double channel(int value) {
        double s = value / 255.0;
        return s <= 0.04045 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    private static Original snapshot(Component c) {
        Color sb = null, sf = null, grid = null;
        if (c instanceof JTable t) { sb = t.getSelectionBackground(); sf = t.getSelectionForeground(); grid = t.getGridColor(); }
        if (c instanceof JList<?> list) { sb = list.getSelectionBackground(); sf = list.getSelectionForeground(); }
        return new Original(c.getBackground(), c.getForeground(),
                c instanceof JComponent jc ? jc.getBorder() : null, sb, sf, grid);
    }

    private static final class MidnightScrollBarUI extends BasicScrollBarUI {
        @Override protected void configureScrollBarColors() {
            trackColor = PawTrackTheme.DARK_CANVAS;
            thumbColor = PawTrackTheme.DARK_HOVER;
            thumbHighlightColor = PawTrackTheme.DARK_BORDER;
            thumbLightShadowColor = PawTrackTheme.DARK_SURFACE;
            thumbDarkShadowColor = PawTrackTheme.DARK_SIDEBAR;
        }
        @Override protected JButton createDecreaseButton(int orientation) {
            JButton button = super.createDecreaseButton(orientation);
            button.setBackground(PawTrackTheme.DARK_SURFACE);
            button.setForeground(PawTrackTheme.DARK_TEXT);
            return button;
        }
        @Override protected JButton createIncreaseButton(int orientation) {
            JButton button = super.createIncreaseButton(orientation);
            button.setBackground(PawTrackTheme.DARK_SURFACE);
            button.setForeground(PawTrackTheme.DARK_TEXT);
            return button;
        }
    }

    private static Color background(Color color, Component c) {
        if (color == null) return null;
        int r = color.getRed(), g = color.getGreen(), b = color.getBlue();
        if (c instanceof javax.swing.text.JTextComponent || c instanceof JComboBox || c instanceof JTable || c instanceof JList)
            return PawTrackTheme.DARK_INPUT;
        if (c instanceof JViewport) return PawTrackTheme.DARK_CANVAS;
        if (c instanceof JTableHeader) return PawTrackTheme.DARK_SIDEBAR;
        if (color.equals(PawTrackTheme.CANVAS) || color.equals(new Color(0xF2F6FE))
                || color.equals(new Color(0xF3F7FD)) || color.equals(new Color(0xF5F8FD)))
            return PawTrackTheme.DARK_CANVAS;
        if (c instanceof PawTrackSidebar) return PawTrackTheme.DARK_SIDEBAR;
        if (c instanceof PawTrackHeader) return PawTrackTheme.DARK_SURFACE;
        if (c instanceof JButton button) {
            if (r > 120 && b > 140 && r < 235) return PawTrackTheme.DARK_SELECTED;
            if (b > r * 1.24 && b > g * 1.08) return PawTrackTheme.DARK_HOVER;
            if (r > 210 && g > 210 && b > 210) return PawTrackTheme.DARK_INPUT;
        }
        if (c instanceof JPanel && !((JPanel)c).isOpaque()) return color;
        if (r > 235 && g > 235 && b > 235) return PawTrackTheme.DARK_SURFACE;
        if (r > 195 && g > 205 && b > 215) return PawTrackTheme.DARK_INPUT;
        if (r < 68 && g < 105 && b < 180) return PawTrackTheme.DARK_SIDEBAR;
        return color;
    }

    private static Color foreground(Color color, Component c) {
        if (color == null) return null;
        int r = color.getRed(), g = color.getGreen(), b = color.getBlue();
        if (c instanceof JButton && (r > 235 && g > 235 && b > 235)) return Color.WHITE;
        if (c instanceof javax.swing.text.JTextComponent || c instanceof JTableHeader)
            return PawTrackTheme.DARK_TEXT;
        if (r > 222 && g > 222 && b > 222) return Color.WHITE;
        if (r < 135 && g < 150 && b < 185 && Math.abs(b-r) < 105) {
            return r > 76 ? PawTrackTheme.DARK_MUTED : PawTrackTheme.DARK_TEXT;
        }
        if (b > r + 32 && b > g + 6 && r < 135) return PawTrackTheme.DARK_ACCENT;
        return color; // semantic success, warning, danger and image accent colors
    }
}
