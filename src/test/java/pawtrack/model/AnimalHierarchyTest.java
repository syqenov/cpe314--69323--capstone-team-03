package pawtrack.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Standard + edge-case tests for the abstract {@link Animal} hierarchy and its
 * fee / care-schedule rules.
 */
class AnimalHierarchyTest {

    private static final LocalDate INTAKE = LocalDate.of(2024, 1, 1);

    @Test
    @DisplayName("Adult dog and cat use their standard fees")
    void standardFees() {
        assertEquals(250.0, new Dog("Rex", "Beagle", 24, INTAKE).adoptionFee());
        assertEquals(150.0, new Cat("Mia", "Siamese", 24, INTAKE).adoptionFee());
    }

    @ParameterizedTest
    @CsvSource({
            "DOG, 3, 300.0",
            "DOG, 5, 300.0",
            "DOG, 6, 250.0",
            "DOG, 83, 250.0",
            "DOG, 84, 100.0",
            "CAT, 2, 200.0",
            "CAT, 6, 150.0",
            "CAT, 119, 150.0",
            "CAT, 120, 75.0"
    })
    @DisplayName("Fee boundaries follow each species' rules")
    void feeBoundaries(Species species, int ageMonths, double expectedFee) {
        Animal animal = Animal.create(species, 0, "Test", "Mixed", ageMonths, INTAKE, AnimalStatus.AVAILABLE);
        assertEquals(expectedFee, animal.adoptionFee(), 0.0001);
    }

    @Test
    @DisplayName("Juvenile and senior flags are mutually exclusive at the boundaries")
    void ageFlags() {
        Dog puppy = new Dog("Pup", "Beagle", 5, INTAKE);
        assertTrue(puppy.isJuvenile());
        assertFalse(puppy.isSenior());

        Dog adult = new Dog("Adult", "Beagle", 6, INTAKE);
        assertFalse(adult.isJuvenile());

        Dog senior = new Dog("Old", "Beagle", 84, INTAKE);
        assertTrue(senior.isSenior());
        assertFalse(senior.isJuvenile());
    }

    @Test
    @DisplayName("Each species exposes a distinct, non-blank care schedule")
    void careSchedulesDiffer() {
        String dog = new Dog("Rex", "Beagle", 24, INTAKE).careSchedule();
        String cat = new Cat("Mia", "Siamese", 24, INTAKE).careSchedule();

        assertFalse(dog.isBlank());
        assertFalse(cat.isBlank());
        assertNotEquals(dog, cat);
    }

    @Test
    @DisplayName("Factory builds the correct concrete subclass")
    void factoryBuildsSubclass() {
        assertInstanceOf(Dog.class, Animal.create(Species.DOG, 1, "A", "Beagle", 12, INTAKE, AnimalStatus.INTAKE));
        assertInstanceOf(Cat.class, Animal.create(Species.CAT, 1, "A", "Siamese", 12, INTAKE, AnimalStatus.INTAKE));
        assertInstanceOf(Cat.class, Animal.create(null, 1, "A", "Siamese", 12, INTAKE, AnimalStatus.INTAKE));
    }

    @Test
    @DisplayName("Invalid construction input is rejected (edge cases)")
    void invalidInputRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Dog("", "Beagle", 12, INTAKE));
        assertThrows(IllegalArgumentException.class, () -> new Dog("Rex", " ", 12, INTAKE));
        assertThrows(IllegalArgumentException.class, () -> new Dog("Rex", "Beagle", -1, INTAKE));
        assertThrows(IllegalArgumentException.class, () -> new Dog("Rex", "Beagle", 12, null));
    }

    @Test
    @DisplayName("Species labels parse leniently")
    void speciesParsing() {
        assertEquals(Species.DOG, Species.fromLabel("dog"));
        assertEquals(Species.CAT, Species.fromLabel(" Cat "));
        assertEquals(Species.CAT, Species.fromLabel("hamster"));
        assertEquals(Species.CAT, Species.fromLabel(null));
    }
}
