package pawtrack.ui;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class PawTrackUnifiedGuiTest {
    @Test void sharedNavigationKeepsAdopterFooterAndCollapse() {
        JButton dashboard = new JButton("Dashboard");
        JButton records = new JButton("My Records");
        PawTrackSidebar sidebar = new PawTrackSidebar(List.of(dashboard, records),
                new JScrollPane(new JPanel()), true, false);
        Component footer = ((BorderLayout) sidebar.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
        assertNotNull(findLabel(footer, "ADOPTER WORKSPACE"));
        JPanel brand = (JPanel) ((BorderLayout) sidebar.getLayout()).getLayoutComponent(BorderLayout.NORTH);
        JButton toggle = null;
        for (Component c : brand.getComponents()) if (c instanceof JButton b) toggle = b;
        assertNotNull(toggle);
        toggle.doClick();
        assertEquals("", dashboard.getText());
        assertNotNull(dashboard.getIcon());
        assertEquals("Dashboard", dashboard.getToolTipText());
        toggle.doClick();
        assertEquals("Dashboard", dashboard.getText());
    }

    @Test void animalGalleryAndTableUseSameFilteredRows() {
        DefaultTableModel data = new DefaultTableModel(new String[]{
                "ID", "Name", "Species", "Breed", "Age", "Intake", "Status", "Fee", "Waitlist"},0);
        data.addRow(new Object[]{1,"Buddy","Dog","Retriever",18,"2026-11-01","Available",250,0});
        data.addRow(new Object[]{2,"Luna","Cat","Siamese",10,"2026-11-02","Adopted",200,1});
        AtomicInteger clicks = new AtomicInteger();
        AnimalBrowsePanel panel = new AnimalBrowsePanel(new JTable(data), false,
                () -> {}, clicks::incrementAndGet);
        panel.setSearchText("Buddy");
        assertEquals(1, panel.visibleAnimalCount());
        AbstractButton gallery = find(panel, "Gallery");
        AbstractButton table = find(panel, "Table");
        assertNotNull(gallery); assertNotNull(table);
        gallery.doClick();
        assertTrue(gallery.isSelected());
        table.doClick();
        assertTrue(table.isSelected());
        assertEquals(1, panel.visibleAnimalCount());
        find(panel, "Record interest").doClick();
        assertEquals(1, clicks.get());
    }

    @Test void staffDashboardUsesRealKpiInputsWithoutStaticSampleNumbers() {
        PawTrackDashboard dashboard = new PawTrackDashboard(true, "admin",
                () -> {}, () -> {}, () -> {});
        dashboard.setStats(6009, 500, 6);
        dashboard.setStaffKpis(3467, 45.5, 6);
        dashboard.setMonthlyAdoptions(List.of());
        assertEquals("6009", dashboard.metricText(0));
        assertEquals("3467", dashboard.metricText(1));
        assertEquals("46 days", dashboard.metricText(2));
        assertEquals("6", dashboard.metricText(3));
    }

    private AbstractButton find(Container root, String text) {
        for (Component c : root.getComponents()) {
            if (c instanceof AbstractButton b && text.equals(b.getText())) return b;
            if (c instanceof Container nested) {
                AbstractButton found = find(nested, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private JLabel findLabel(Component root, String text) {
        if (root instanceof JLabel label && text.equals(label.getText())) return label;
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                JLabel label = findLabel(child, text);
                if (label != null) return label;
            }
        }
        return null;
    }
}
