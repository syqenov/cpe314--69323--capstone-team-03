package pawtrack.db;

import pawtrack.model.Adopter;
import pawtrack.model.AdoptionRecord;
import pawtrack.model.AnimalStatus;
import pawtrack.model.Dog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AdoptionRecordDaoTest {

    private static final LocalDate INTAKE =
            LocalDate.of(2024, 1, 1);

    private static final LocalDate ADOPTION =
            LocalDate.of(2024, 2, 1);

    private int createAnimal(Database database) throws SQLException {
        AnimalDao animalDao = new AnimalDao(database);

        return animalDao.insert(
                new Dog(0, "Rex", "Beagle", 24,
                        INTAKE, AnimalStatus.ADOPTED)
        );
    }

    private int createAdopter(Database database) throws SQLException {
        AdopterDao adopterDao = new AdopterDao(database);

        return adopterDao.insert(
                new Adopter("Jane Doe", "jane@example.com", "555")
        );
    }

    @Test
    @DisplayName("Insert adoption record and assign generated ID")
    void insertAdoptionRecord(@TempDir Path tempDir) throws SQLException {

        try (Database database = Database.openFile(
                tempDir.resolve("adoption-test.db").toString())) {

            int animalId = createAnimal(database);
            int adopterId = createAdopter(database);

            AdoptionRecordDao dao = new AdoptionRecordDao(database);

            AdoptionRecord record = new AdoptionRecord(
                    animalId, adopterId, ADOPTION, 250.00
            );

            int generatedId = dao.insert(record);

            assertTrue(generatedId > 0);
            assertEquals(generatedId, record.getId());
            assertEquals(1, dao.count());
        }
    }

    @Test
    @DisplayName("Find all adoption records")
    void findAllAdoptionRecords(@TempDir Path tempDir) throws SQLException {

        try (Database database = Database.openFile(
                tempDir.resolve("adoption-test.db").toString())) {

            int animalId = createAnimal(database);
            int adopterId = createAdopter(database);

            AdoptionRecordDao dao = new AdoptionRecordDao(database);

            dao.insert(new AdoptionRecord(
                    animalId, adopterId, ADOPTION, 250.00));

            List<AdoptionRecord> records = dao.findAll();

            assertEquals(1, records.size());
            assertEquals(animalId, records.get(0).getAnimalId());
            assertEquals(adopterId, records.get(0).getAdopterId());
            assertEquals(ADOPTION, records.get(0).getAdoptionDate());
            assertEquals(250.00, records.get(0).getFeePaid(), 0.001);
        }
    }

    @Test
    @DisplayName("Find adoption records by animal ID")
    void findByAnimalId(@TempDir Path tempDir) throws SQLException {

        try (Database database = Database.openFile(
                tempDir.resolve("adoption-test.db").toString())) {

            int firstAnimal = createAnimal(database);

            int secondAnimal = new AnimalDao(database).insert(
                    new Dog(0, "Buddy", "Poodle", 18,
                            INTAKE, AnimalStatus.ADOPTED)
            );

            int adopterId = createAdopter(database);

            AdoptionRecordDao dao = new AdoptionRecordDao(database);

            dao.insert(new AdoptionRecord(
                    firstAnimal, adopterId, ADOPTION, 250.00));

            dao.insert(new AdoptionRecord(
                    secondAnimal, adopterId, ADOPTION.plusDays(5), 300.00));

            List<AdoptionRecord> records =
                    dao.findByAnimalId(firstAnimal);

            assertEquals(1, records.size());
            assertEquals(firstAnimal, records.get(0).getAnimalId());
        }
    }

    @Test
    @DisplayName("Find latest adoption date for an animal")
    void latestAdoptionDate(@TempDir Path tempDir) throws SQLException {

        try (Database database = Database.openFile(
                tempDir.resolve("adoption-test.db").toString())) {

            int animalId = createAnimal(database);
            int adopterId = createAdopter(database);

            AdoptionRecordDao dao = new AdoptionRecordDao(database);

            dao.insert(new AdoptionRecord(
                    animalId, adopterId, ADOPTION, 250.00));

            dao.insert(new AdoptionRecord(
                    animalId, adopterId, ADOPTION.plusDays(10), 300.00));

            Optional<LocalDate> latest =
                    dao.latestAdoptionDate(animalId);

            assertTrue(latest.isPresent());
            assertEquals(ADOPTION.plusDays(10), latest.get());
        }
    }

    @Test
    @DisplayName("Return empty when animal has no adoption record")
    void latestAdoptionDateEmpty(@TempDir Path tempDir) throws SQLException {

        try (Database database = Database.openFile(
                tempDir.resolve("adoption-test.db").toString())) {

            int animalId = createAnimal(database);

            AdoptionRecordDao dao = new AdoptionRecordDao(database);

            assertTrue(dao.latestAdoptionDate(animalId).isEmpty());
        }
    }

    @Test
    @DisplayName("Count adoption records correctly")
    void countAdoptionRecords(@TempDir Path tempDir) throws SQLException {

        try (Database database = Database.openFile(
                tempDir.resolve("adoption-test.db").toString())) {

            int animalId = createAnimal(database);
            int adopterId = createAdopter(database);

            AdoptionRecordDao dao = new AdoptionRecordDao(database);

            assertEquals(0, dao.count());

            dao.insert(new AdoptionRecord(
                    animalId, adopterId, ADOPTION, 250.00));

            dao.insert(new AdoptionRecord(
                    animalId, adopterId, ADOPTION.plusDays(5), 300.00));

            assertEquals(2, dao.count());
        }
    }

    @Test
    @DisplayName("Calculate average shelter stay by species")
    void averageStayDaysBySpecies(@TempDir Path tempDir) throws SQLException {

        try (Database database = Database.openFile(
                tempDir.resolve("adoption-test.db").toString())) {

            int animalId = createAnimal(database);
            int adopterId = createAdopter(database);

            AdoptionRecordDao dao = new AdoptionRecordDao(database);

            dao.insert(new AdoptionRecord(
                    animalId, adopterId, ADOPTION, 250.00));

            List<Object[]> results = dao.averageStayDaysBySpecies();

            assertEquals(1, results.size());
            assertEquals("DOG", results.get(0)[0]);
            assertEquals(31.0, (Double) results.get(0)[1], 0.001);
        }
    }

    @Test
    @DisplayName("Reject adoption record with invalid animal ID")
    void rejectInvalidAnimalId(@TempDir Path tempDir) throws SQLException {

        try (Database database = Database.openFile(
                tempDir.resolve("adoption-test.db").toString())) {

            int adopterId = createAdopter(database);

            AdoptionRecordDao dao = new AdoptionRecordDao(database);

            AdoptionRecord invalid = new AdoptionRecord(
                    9999, adopterId, ADOPTION, 250.00
            );

            assertThrows(SQLException.class, () -> dao.insert(invalid));
        }
    }
}