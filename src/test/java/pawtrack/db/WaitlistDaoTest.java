package pawtrack.db;

import pawtrack.model.Adopter;
import pawtrack.model.AnimalStatus;
import pawtrack.model.Dog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WaitlistDaoTest {

    private static final LocalDate INTAKE =
            LocalDate.of(2024, 1, 1);

    private static final LocalDateTime REQUESTED_AT =
            LocalDateTime.of(2024, 2, 1, 10, 30);

    private int createAnimal(Database database, String name)
            throws SQLException {

        AnimalDao animalDao = new AnimalDao(database);

        return animalDao.insert(
                new Dog(0, name, "Beagle", 24,
                        INTAKE, AnimalStatus.AVAILABLE)
        );
    }

    private int createAdopter(Database database, String name, String email)
            throws SQLException {

        AdopterDao adopterDao = new AdopterDao(database);

        return adopterDao.insert(
                new Adopter(name, email, "555")
        );
    }

    @Test
    @DisplayName("Insert a waitlist entry successfully")
    void insertWaitlistEntry(@TempDir Path tempDir) throws SQLException {

        try (Database database = Database.openFile(
                tempDir.resolve("waitlist-test.db").toString())) {

            int animalId = createAnimal(database, "Rex");
            int adopterId = createAdopter(
                    database, "Jane", "jane@example.com");

            WaitlistDao dao = new WaitlistDao(database);

            int inserted = dao.insert(
                    animalId, adopterId, REQUESTED_AT, 1);

            assertEquals(1, inserted);

            List<WaitlistDao.Entry> entries = dao.loadAll();

            assertEquals(1, entries.size());
            assertEquals(animalId, entries.get(0).animalId());
            assertEquals(adopterId, entries.get(0).adopterId());
            assertEquals(REQUESTED_AT, entries.get(0).requestedAt());
            assertEquals(1, entries.get(0).position());
        }
    }

    @Test
    @DisplayName("Load all waitlist entries in queue order")
    void loadAllWaitlistEntries(@TempDir Path tempDir) throws SQLException {

        try (Database database = Database.openFile(
                tempDir.resolve("waitlist-test.db").toString())) {

            int animalId = createAnimal(database, "Rex");

            int firstAdopter = createAdopter(
                    database, "Jane", "jane@example.com");

            int secondAdopter = createAdopter(
                    database, "John", "john@example.com");

            WaitlistDao dao = new WaitlistDao(database);

            dao.insert(animalId, secondAdopter,
                    REQUESTED_AT.plusHours(1), 2);

            dao.insert(animalId, firstAdopter,
                    REQUESTED_AT, 1);

            List<WaitlistDao.Entry> entries = dao.loadAll();

            assertEquals(2, entries.size());

            assertEquals(1, entries.get(0).position());
            assertEquals(firstAdopter, entries.get(0).adopterId());

            assertEquals(2, entries.get(1).position());
            assertEquals(secondAdopter, entries.get(1).adopterId());
        }
    }

    @Test
    @DisplayName("Delete a specific adopter from the waitlist")
    void deleteWaitlistEntry(@TempDir Path tempDir) throws SQLException {

        try (Database database = Database.openFile(
                tempDir.resolve("waitlist-test.db").toString())) {

            int animalId = createAnimal(database, "Rex");

            int adopterId = createAdopter(
                    database, "Jane", "jane@example.com");

            WaitlistDao dao = new WaitlistDao(database);

            dao.insert(animalId, adopterId, REQUESTED_AT, 1);

            assertEquals(1, dao.delete(animalId, adopterId));
            assertTrue(dao.loadAll().isEmpty());
        }
    }

    @Test
    @DisplayName("Return zero when deleting a non-existent waitlist entry")
    void deleteNonExistentEntry(@TempDir Path tempDir) throws SQLException {

        try (Database database = Database.openFile(
                tempDir.resolve("waitlist-test.db").toString())) {

            int animalId = createAnimal(database, "Rex");
            int adopterId = createAdopter(
                    database, "Jane", "jane@example.com");

            WaitlistDao dao = new WaitlistDao(database);

            assertEquals(0, dao.delete(animalId, adopterId));
        }
    }

    @Test
    @DisplayName("Delete all waitlist entries for a specific animal")
    void deleteByAnimal(@TempDir Path tempDir) throws SQLException {

        try (Database database = Database.openFile(
                tempDir.resolve("waitlist-test.db").toString())) {

            int firstAnimal = createAnimal(database, "Rex");
            int secondAnimal = createAnimal(database, "Buddy");

            int firstAdopter = createAdopter(
                    database, "Jane", "jane@example.com");

            int secondAdopter = createAdopter(
                    database, "John", "john@example.com");

            WaitlistDao dao = new WaitlistDao(database);

            dao.insert(firstAnimal, firstAdopter, REQUESTED_AT, 1);
            dao.insert(firstAnimal, secondAdopter, REQUESTED_AT, 2);
            dao.insert(secondAnimal, firstAdopter, REQUESTED_AT, 1);

            assertEquals(2, dao.deleteByAnimal(firstAnimal));

            List<WaitlistDao.Entry> remaining = dao.loadAll();

            assertEquals(1, remaining.size());
            assertEquals(secondAnimal, remaining.get(0).animalId());
        }
    }

    @Test
    @DisplayName("Replace an existing entry for the same animal and adopter")
    void replaceDuplicateEntry(@TempDir Path tempDir) throws SQLException {

        try (Database database = Database.openFile(
                tempDir.resolve("waitlist-test.db").toString())) {

            int animalId = createAnimal(database, "Rex");

            int adopterId = createAdopter(
                    database, "Jane", "jane@example.com");

            WaitlistDao dao = new WaitlistDao(database);

            dao.insert(animalId, adopterId, REQUESTED_AT, 1);

            dao.insert(animalId, adopterId,
                    REQUESTED_AT.plusHours(2), 3);

            List<WaitlistDao.Entry> entries = dao.loadAll();

            assertEquals(1, entries.size());
            assertEquals(3, entries.get(0).position());
            assertEquals(
                    REQUESTED_AT.plusHours(2),
                    entries.get(0).requestedAt()
            );
        }
    }

    @Test
    @DisplayName("Reject waitlist entry with invalid animal ID")
    void rejectInvalidAnimalId(@TempDir Path tempDir) throws SQLException {

        try (Database database = Database.openFile(
                tempDir.resolve("waitlist-test.db").toString())) {

            int adopterId = createAdopter(
                    database, "Jane", "jane@example.com");

            WaitlistDao dao = new WaitlistDao(database);

            assertThrows(SQLException.class, () ->
                    dao.insert(9999, adopterId, REQUESTED_AT, 1)
            );
        }
    }

    @Test
    @DisplayName("Reject waitlist entry with invalid adopter ID")
    void rejectInvalidAdopterId(@TempDir Path tempDir) throws SQLException {

        try (Database database = Database.openFile(
                tempDir.resolve("waitlist-test.db").toString())) {

            int animalId = createAnimal(database, "Rex");

            WaitlistDao dao = new WaitlistDao(database);

            assertThrows(SQLException.class, () ->
                    dao.insert(animalId, 9999, REQUESTED_AT, 1)
            );
        }
    }
}