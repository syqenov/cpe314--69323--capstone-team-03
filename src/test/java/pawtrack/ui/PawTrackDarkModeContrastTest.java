package pawtrack.ui;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.lang.reflect.Constructor;
import static org.junit.jupiter.api.Assertions.*;

/** Regressions for white-on-white headers, unreadable statuses and theme reversal. */
class PawTrackDarkModeContrastTest {
    private static JTable sampleTable() {
        JTable table = new JTable(new DefaultTableModel(
                new Object[][] {{1, "Buddy", "Alex", "Today", "WAITING"}},
                new String[] {"ID", "Pet", "Applicant", "Submitted", "Status"}));
        PremiumUi.styleTable(table);
        return table;
    }

    @Test void myApplicationsHeadersAreReadableInDarkAndLight() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PawTrackTheme.setDark(false);
            JTable table = sampleTable();
            JPanel page = new JPanel(new BorderLayout());
            page.add(new JScrollPane(table));
            PawTrackTheme.setDark(true);
            PawTrackThemeManager.applyCurrent(page);
            Component darkHeader = table.getTableHeader().getDefaultRenderer()
                    .getTableCellRendererComponent(table, "Applicant", false, false, -1, 2);
            assertEquals(PawTrackTheme.DARK_SIDEBAR, darkHeader.getBackground());
            assertEquals(PawTrackTheme.DARK_TEXT, darkHeader.getForeground());
            assertTrue(contrast(darkHeader.getBackground(), darkHeader.getForeground()) >= 4.5);
            PawTrackTheme.setDark(false);
            PawTrackThemeManager.applyCurrent(page);
            Component lightHeader = table.getTableHeader().getDefaultRenderer()
                    .getTableCellRendererComponent(table, "Applicant", false, false, -1, 2);
            assertEquals(new Color(0xE3EDFF), lightHeader.getBackground());
            assertEquals(PawTrackTheme.NAVY, lightHeader.getForeground());
        });
    }

    @Test void numberAndStatusCellsHaveSufficientDarkContrast() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PawTrackTheme.setDark(false);
            JTable table = sampleTable();
            PawTrackTheme.setDark(true);
            PawTrackThemeManager.applyCurrent(table);
            TableCellRenderer number = table.getDefaultRenderer(Number.class);
            Component id = number.getTableCellRendererComponent(table, 1, false, false, 0, 0);
            assertTrue(contrast(id.getBackground(), id.getForeground()) >= 4.5);
            TableCellRenderer ordinary = table.getDefaultRenderer(Object.class);
            for (String status : new String[]{"WAITING", "COMPLETED", "Buddy"}) {
                Component cell = ordinary.getTableCellRendererComponent(table, status, false, false, 0, 4);
                assertTrue(contrast(cell.getBackground(), cell.getForeground()) >= 4.5,
                        "Insufficient contrast for " + status);
            }
            PawTrackTheme.setDark(false);
        });
    }

    @Test void labelsCreatedWhileDarkReturnToOriginalLightText() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PawTrackTheme.setDark(true);
            JLabel title = PremiumUi.title("New pet", 16);
            JLabel caption = PremiumUi.caption("Recently added");
            assertEquals(PawTrackTheme.DARK_TEXT, title.getForeground());
            assertEquals(PawTrackTheme.DARK_MUTED, caption.getForeground());
            JPanel page = new JPanel(new GridLayout(2, 1));
            page.add(title); page.add(caption);
            PawTrackTheme.setDark(false);
            PawTrackThemeManager.applyCurrent(page);
            assertEquals(PawTrackTheme.NAVY, title.getForeground());
            assertEquals(PawTrackTheme.MUTED, caption.getForeground());
        });
    }

    @Test void accountSettingsTabsRestoreTheirExactLightColors() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PawTrackTheme.setDark(false);
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("My Profile", new JPanel());
            tabs.addTab("Security", new JPanel());
            tabs.setForegroundAt(0, Color.WHITE);
            tabs.setForegroundAt(1, PawTrackTheme.NAVY);
            PawTrackTheme.setDark(true);
            PawTrackThemeManager.applyCurrent(tabs);
            assertEquals(PawTrackTheme.DARK_TEXT, tabs.getForegroundAt(1));
            PawTrackTheme.setDark(false);
            PawTrackThemeManager.applyCurrent(tabs);
            assertEquals(Color.WHITE, tabs.getForegroundAt(0));
            assertEquals(PawTrackTheme.NAVY, tabs.getForegroundAt(1));
        });
    }

    @Test void applicationHistoryCellsUseDarkRowsAndLightText() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Class<?> cls = Class.forName("pawtrack.ui.ApplicationHistoryPanel$HistoryCell");
                Constructor<?> ctor = cls.getDeclaredConstructor(int.class);
                ctor.setAccessible(true);
                TableCellRenderer renderer = (TableCellRenderer) ctor.newInstance(1);
                JTable table = new JTable(1, 1);
                PawTrackTheme.setDark(true);
                Component dark = renderer.getTableCellRendererComponent(table, "Buddy", false, false, 0, 0);
                assertEquals(PawTrackTheme.DARK_SURFACE, dark.getBackground());
                assertEquals(PawTrackTheme.DARK_TEXT, dark.getForeground());
                PawTrackTheme.setDark(false);
                Component light = renderer.getTableCellRendererComponent(table, "Buddy", false, false, 0, 0);
                assertEquals(Color.WHITE, light.getBackground());
                assertEquals(PawTrackTheme.NAVY, light.getForeground());
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError(failure);
            } finally {
                PawTrackTheme.setDark(false);
            }
        });
    }

    @Test void activityLogUsesReadableDarkTableHeaders() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PawTrackTheme.setDark(false);
            PawTrackActivityLogPanel panel = new PawTrackActivityLogPanel();
            JTable table = findTable(panel);
            assertTrue(table != null);
            PawTrackTheme.setDark(true);
            PawTrackThemeManager.applyCurrent(panel);
            Component header = table.getTableHeader().getDefaultRenderer()
                    .getTableCellRendererComponent(table, "Details", false, false, -1, 2);
            assertEquals(PawTrackTheme.DARK_SIDEBAR, header.getBackground());
            assertEquals(PawTrackTheme.DARK_TEXT, header.getForeground());
            PawTrackTheme.setDark(false);
        });
    }

    private static JTable findTable(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JTable table) return table;
            if (child instanceof Container nested) {
                JTable result = findTable(nested);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static double contrast(Color a, Color b) {
        double x = luminance(a), y = luminance(b);
        return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
    }
    private static double luminance(Color c) {
        return channel(c.getRed()) * 0.2126 + channel(c.getGreen()) * 0.7152
                + channel(c.getBlue()) * 0.0722;
    }
    private static double channel(int v) {
        double s = v / 255.0;
        return s <= 0.04045 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }
}
