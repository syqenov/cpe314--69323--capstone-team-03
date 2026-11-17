package pawtrack.ui;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import static org.junit.jupiter.api.Assertions.*;

class AnimalStatusFilterTest {
    @Test
    void statusFilterShowsMatchesAndRestoresAll() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            String[] headers = {"ID", "Name", "Species", "Breed", "Age", "Gender", "Status"};
            DefaultTableModel model = new DefaultTableModel(headers, 0);
            model.addRow(new Object[]{1, "A", "Dog", "Mixed", 1, "", "Available"});
            model.addRow(new Object[]{2, "B", "Cat", "Mixed", 1, "", "Adopted"});
            JTable table = new JTable(model);
            table.setRowSorter(new TableRowSorter<>(model));
            JLabel message = new JLabel();
            AnimalStatusFilter filter = new AnimalStatusFilter(table, message, Color.BLUE);
            filter.applyStatusFilter("Available");
            assertEquals(1, table.getRowCount());
            assertEquals("Filtered by status: Available", message.getText());
            filter.applyStatusFilter("Adopted");
            assertEquals(1, table.getRowCount());
            filter.applyStatusFilter(null);
            assertEquals(2, table.getRowCount());
            assertEquals("Showing all animals", message.getText());
        });
    }

    @Test
    void filterQuotesRegexSpecialCharacters() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel model = new DefaultTableModel(new Object[]{"A", "B", "C", "D", "E", "F", "Status"}, 0);
            model.addRow(new Object[]{1, 2, 3, 4, 5, 6, "Waiting (review)"});
            model.addRow(new Object[]{1, 2, 3, 4, 5, 6, "Waiting review"});
            JTable table = new JTable(model);
            table.setRowSorter(new TableRowSorter<>(model));
            AnimalStatusFilter filter = new AnimalStatusFilter(table, new JLabel(), Color.BLUE);
            filter.applyStatusFilter("Waiting (review)");
            assertEquals(1, table.getRowCount());
        });
    }
}
