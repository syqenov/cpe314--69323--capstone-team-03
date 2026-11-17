package pawtrack.ui;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class AnimalBrowsePanelTest {
    private JTable table() {
        DefaultTableModel model = new DefaultTableModel(new String[]{
                "ID", "Name", "Species", "Breed", "Age", "Date", "Status", "Fee", "Queue"}, 0);
        model.addRow(new Object[]{1, "Milo", "Dog", "Golden Retriever", 18, "2026-01-01", "Available", 50, 0});
        model.addRow(new Object[]{2, "Luna", "Cat", "Siamese", 12, "2026-01-02", "Adopted", 30, 0});
        model.addRow(new Object[]{3, "Max", "Dog", "Beagle", 15, "2026-01-03", "Available", 40, 1});
        return new JTable(model);
    }

    @Test void searchFiltersWithoutChangingSourceRows() {
        JTable table = table();
        AnimalBrowsePanel panel = new AnimalBrowsePanel(table, false, () -> {}, () -> {});
        panel.setSearchText("luna");
        assertEquals(1, panel.visibleAnimalCount());
        assertEquals(3, table.getModel().getRowCount());
        panel.setSearchText("[");
        assertEquals(0, panel.visibleAnimalCount()); // special characters are treated literally
    }

    @Test void statusAndSearchCombine() {
        AnimalBrowsePanel panel = new AnimalBrowsePanel(table(), true, () -> {}, () -> {});
        panel.setStatusFilter("Available");
        assertEquals(2, panel.visibleAnimalCount());
        panel.setSearchText("max");
        assertEquals(1, panel.visibleAnimalCount());
        panel.setStatusFilter("Adopted");
        assertEquals(0, panel.visibleAnimalCount());
    }

    @Test void adopterActionCallsExistingControllerCallback() {
        AtomicInteger calls = new AtomicInteger();
        AnimalBrowsePanel panel = new AnimalBrowsePanel(table(), false, () -> {}, calls::incrementAndGet);
        JButton button = findButton(panel, "Record interest");
        assertNotNull(button);
        button.doClick();
        assertEquals(1, calls.get());
        assertNull(findButton(new AnimalBrowsePanel(table(), true, () -> {}, () -> {}), "Record interest"));
    }

    private JButton findButton(Container c, String label) {
        for (Component child : c.getComponents()) {
            if (child instanceof JButton b && label.equals(b.getText())) return b;
            if (child instanceof Container nested) {
                JButton found = findButton(nested, label);
                if (found != null) return found;
            }
        }
        return null;
    }
}
