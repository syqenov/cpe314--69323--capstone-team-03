package pawtrack.ui;

import org.junit.jupiter.api.Test;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.Species;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PetProfilePanelTest {
    @Test void dogProfileContainsCorrectModel() {
        Animal dog = Animal.create(Species.DOG, 9, "Buddy", "Beagle", 18,
                LocalDate.of(2026, 1, 5), AnimalStatus.AVAILABLE);
        PetProfilePanel panel = new PetProfilePanel(dog);
        assertSame(dog, panel.getAnimal());
        assertTrue(panel.getLoadImageButton().isEnabled());
        assertTrue(findText(panel, "Buddy"));
        assertTrue(findText(panel, "Beagle"));
        assertTrue(findText(panel, "Available"));
    }

    @Test void catProfileUsesSameComponent() {
        Animal cat = Animal.create(Species.CAT, 3, "Luna", "Siamese", 30,
                LocalDate.of(2026, 1, 5), AnimalStatus.PENDING);
        PetProfilePanel panel = new PetProfilePanel(cat);
        assertTrue(findText(panel, "Luna"));
        assertTrue(findText(panel, "Pending Interest"));
    }

    @Test void browserProfileButtonTriggersCallback() {
        DefaultTableModel m = new DefaultTableModel(new String[]{"ID", "Name", "Species", "Breed", "Age", "Date", "Status", "Fee", "Queue"}, 0);
        m.addRow(new Object[]{1,"Buddy","Dog","Beagle",18,"2026-01-01","Available","250",0});
        AtomicInteger calls = new AtomicInteger();
        AnimalBrowsePanel panel = new AnimalBrowsePanel(new JTable(m), false, () -> {}, () -> {}, calls::incrementAndGet);
        JButton button = findButton(panel, "View pet profile");
        assertNotNull(button);
        button.doClick();
        assertEquals(1, calls.get());
    }

    private static boolean findText(Container root, String value) {
        for (Component c : root.getComponents()) {
            if (c instanceof JLabel label && label.getText().contains(value)) return true;
            if (c instanceof Container child && findText(child, value)) return true;
        }
        return false;
    }
    private static JButton findButton(Container root, String title) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton b && b.getText().equals(title)) return b;
            if (c instanceof Container child) {
                JButton found = findButton(child, title);
                if (found != null) return found;
            }
        }
        return null;
    }
}
