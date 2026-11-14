
package pawtrack.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.Species;
import pawtrack.service.WaitlistService;

import java.time.LocalDate;
import java.util.List;

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
        Animal animal = Animal.create(
                Species.DOG,
                1,
                "Buddy",
                "Labrador",
                12,
                LocalDate.now(),
                AnimalStatus.AVAILABLE
        );

        model.setAnimals(List.of(animal));

        assertEquals(1, model.getRowCount());
        assertEquals("Buddy", model.getValueAt(0, 1));
    }

    @Test
    void testGetColumnCount() {
        assertEquals(9, model.getColumnCount());
    }

    @Test
    void testColumnNames() {
        assertEquals("ID", model.getColumnName(0));
        assertEquals("Name", model.getColumnName(1));
        assertEquals("Species", model.getColumnName(2));
        assertEquals("Breed", model.getColumnName(3));
        assertEquals("Age (mo)", model.getColumnName(4));
        assertEquals("Intake Date", model.getColumnName(5));
        assertEquals("Status", model.getColumnName(6));
        assertEquals("Fee ($)", model.getColumnName(7));
        assertEquals("Waitlist", model.getColumnName(8));
    }

    @Test
    void testWaitlistColumnShowsCount() {
        Animal animal = Animal.create(
                Species.CAT,
                1,
                "Whiskers",
                "Siamese",
                12,
                LocalDate.now(),
                AnimalStatus.AVAILABLE
        );

        waitlist.addInterest(
                1,
                new pawtrack.model.Adopter(
                        "John",
                        "john@test.com",
                        "555-1234"
                )
        );

        model.setAnimals(List.of(animal));

        assertEquals(1, model.getValueAt(0, 8));
    }

    @Test
    void testGetAnimalAtReturnsAnimal() {
        Animal animal = Animal.create(
                Species.DOG,
                1,
                "Buddy",
                "Labrador",
                12,
                LocalDate.now(),
                AnimalStatus.AVAILABLE
        );

        model.setAnimals(List.of(animal));

        assertSame(animal, model.getAnimalAt(0));
    }

    @Test
    void testGetAnimalAtInvalidIndexReturnsNull() {
        assertNull(model.getAnimalAt(0));
        assertNull(model.getAnimalAt(-1));
    }

    @Test
    void testGetAnimalsReturnsCopy() {
        Animal animal = Animal.create(
                Species.DOG,
                1,
                "Buddy",
                "Labrador",
                12,
                LocalDate.now(),
                AnimalStatus.AVAILABLE
        );

        model.setAnimals(List.of(animal));

        List<Animal> result = model.getAnimals();
        result.clear();

        assertEquals(1, model.getRowCount());
    }
}
