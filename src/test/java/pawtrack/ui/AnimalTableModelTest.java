package pawtrack.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.Species;
import pawtrack.service.WaitlistService;
import static org.junit.jupiter.api.Assertions.*;

class AnimalTableModelTest {

    private AnimalTableModel model;
    private WaitlistService waitlist;

    @BeforeEach
    void setUp() {
        waitlist = new WaitlistService();
        model = new AnimalTableModel(waitlist);
    }

    @Test
    void testInitialRowCount() {
        assertEquals(0, model.getRowCount());
    }

    @Test
    void testSetAnimals() {
        var animal = Animal.create(Species.DOG, 1, "Buddy", "Labrador", 12, java.time.LocalDate.now(), AnimalStatus.AVAILABLE);
        model.setAnimals(java.util.List.of(animal));
        assertEquals(1, model.getRowCount());
        assertEquals("Buddy", model.getValueAt(0, 1));
    }

    @Test
    void testGetColumnCount() {
        assertEquals(7, model.getColumnCount());
    }

    @Test
    void testColumnNames() {
        assertEquals("ID", model.getColumnName(0));
        assertEquals("Name", model.getColumnName(1));
        assertEquals("Species", model.getColumnName(2));
        assertEquals("Breed", model.getColumnName(3));
        assertEquals("Status", model.getColumnName(4));
        assertEquals("Classification", model.getColumnName(5));
        assertEquals("Waitlist", model.getColumnName(6));
    }

    @Test
    void testWaitlistColumnShowsCount() {
        var animal = Animal.create(Species.CAT, 1, "Whiskers", "Siamese", 12, java.time.LocalDate.now(), AnimalStatus.AVAILABLE);
        waitlist.addInterest(1, new pawtrack.model.Adopter("John", "john@test.com", "555-1234"));
        model.setAnimals(java.util.List.of(animal));
        assertEquals("1", model.getValueAt(0, 6));
    }
}