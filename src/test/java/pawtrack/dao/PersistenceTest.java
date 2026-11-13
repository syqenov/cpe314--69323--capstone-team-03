package pawtrack.dao;

import pawtrack.model.Adopter;
import pawtrack.model.AdoptionRecord;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.Dog;
import pawtrack.service.ClassificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JDBC persistence tests against a real (temporary file) SQLite database.
 */
class PersistenceTest {

    private static final LocalDate INTAKE = LocalDate.of(2024, 1, 1);

    @Test
    @DisplayName("An animal round-trips through SQLite and keeps its concrete type")
    void animalRoundTrip(@TempDir Path tempDir) throws SQLException {
        try (Database database = Database.openFile(tempDir.resolve("shelter.db").toString())) {
            AnimalDao dao = new AnimalDao(database);
            Animal dog = new Dog(0, "Rex", "Beagle", 24, INTAKE, AnimalStatus.AVAILABLE);

            int id = dao.insert(dog);

            assertTrue(id > 0);
            assertEquals(id, dog.getId());
            Animal loaded = dao.findById(id).orElseThrow();
            assertInstanceOf(Dog.class, loaded);
            assertEquals("Rex", loaded.getName());
            assertEquals("Beagle", loaded.getBreed());
            assertEquals(AnimalStatus.AVAILABLE, loaded.getStatus());
        }
    }

    @Test
    @DisplayName("Status updates and the breed frequency aggregation are persisted")
    void statusUpdateAndAggregation(@TempDir Path tempDir) throws SQLException {
        try (Database database = Database.openFile(tempDir.resolve("shelter.db").toString())) {
            AnimalDao dao = new AnimalDao(database);
            int first = dao.insert(new Dog(0, "Rex", "Beagle", 24, INTAKE, AnimalStatus.AVAILABLE));
            dao.insert(new Dog(0, "Fido", "Beagle", 30, INTAKE, AnimalStatus.AVAILABLE));
            dao.insert(new Dog(0, "Ace", "Poodle", 18, INTAKE, AnimalStatus.AVAILABLE));

            assertTrue(dao.updateStatus(first, AnimalStatus.ADOPTED));
            assertEquals(AnimalStatus.ADOPTED, dao.findById(first).orElseThrow().getStatus());

            Map<String, Integer> byBreed = dao.countByBreed();
            assertEquals(2, byBreed.get("Beagle"));
            assertEquals(1, byBreed.get("Poodle"));
        }
    }

    @Test
    @DisplayName("Adoption records persist and join back to their animal")
    void adoptionJoin(@TempDir Path tempDir) throws SQLException {
        try (Database database = Database.openFile(tempDir.resolve("shelter.db").toString())) {
            AnimalDao animalDao = new AnimalDao(database);
            AdopterDao adopterDao = new AdopterDao(database);
            AdoptionRecordDao recordDao = new AdoptionRecordDao(database);

            int animalId = animalDao.insert(new Dog(0, "Rex", "Beagle", 24, INTAKE, AnimalStatus.ADOPTED));
            Adopter adopter = adopterDao.findOrCreate(new Adopter("Jane Doe", "jane@example.com", "555"));

            recordDao.insert(new AdoptionRecord(animalId, adopter.getId(), LocalDate.of(2024, 2, 1), 250.0));

            assertEquals(1, recordDao.findAll().size());
            assertEquals(LocalDate.of(2024, 2, 1),
                    recordDao.latestAdoptionDate(animalId).orElseThrow());
            assertEquals(animalId, recordDao.findAll().get(0).getAnimalId());
        }
    }

    @Test
    @DisplayName("Foreign keys reject an adoption for a non-existent animal (edge case)")
    void foreignKeyEnforced(@TempDir Path tempDir) throws SQLException {
        try (Database database = Database.openFile(tempDir.resolve("shelter.db").toString())) {
            AdoptionRecordDao recordDao = new AdoptionRecordDao(database);
            AdoptionRecord orphan = new AdoptionRecord(0, 4242, 1, LocalDate.now(), 10.0);

            assertThrows(SQLException.class, () -> recordDao.insert(orphan));
        }
    }

    @Test
    @DisplayName("Adopter emails are unique (edge case)")
    void uniqueEmailEnforced(@TempDir Path tempDir) throws SQLException {
        try (Database database = Database.openFile(tempDir.resolve("shelter.db").toString())) {
            AdopterDao dao = new AdopterDao(database);
            dao.insert(new Adopter("Jane", "jane@example.com", ""));

            assertThrows(SQLException.class, () -> dao.insert(new Adopter("Janet", "JANE@example.com", "")));
        }
    }

    @Test
    @DisplayName("Classification tree persists and reloads with stable ids")
    void classificationPersistence(@TempDir Path tempDir) throws SQLException {
        try (Database database = Database.openFile(tempDir.resolve("shelter.db").toString())) {
            ClassificationDao dao = new ClassificationDao(database);
            ClassificationService service = ClassificationService.loadOrSeed(dao);

            assertEquals(22, service.nodeCount());
            assertEquals(22, dao.loadAll().size());

            // loading again must not reseed
            ClassificationService reloaded = ClassificationService.loadOrSeed(dao);
            assertEquals(22, reloaded.nodeCount());
            assertTrue(reloaded.findByPath("All Animals > Dog > Sporting Group > Retriever").isPresent());
        }
    }
}
