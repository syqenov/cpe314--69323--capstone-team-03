package pawtrack.controller;

import org.junit.jupiter.api.Test;
import pawtrack.dao.AdopterDao;
import pawtrack.dao.AdoptionRecordDao;
import pawtrack.dao.AnimalDao;
import pawtrack.dao.Database;
import pawtrack.dao.UserDao;
import pawtrack.model.Adopter;
import pawtrack.model.AnimalStatus;
import pawtrack.model.Dog;
import pawtrack.service.StatusHistoryService;

import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AdoptionTransactionTest {
    private Dog saveDog(AnimalDao animalDao) throws SQLException {
        Dog dog = new Dog(0, "Buddy", "Retriever", 24, LocalDate.now(), AnimalStatus.AVAILABLE);
        animalDao.insert(dog);
        return dog;
    }

    private AdoptionController controller(Database db, StatusHistoryService history) {
        AuthController auth = new AuthController(new UserDao(db));
        assertTrue(auth.login("admin", "admin123").isPresent());
        return new AdoptionController(db, new AnimalDao(db), new AdopterDao(db),
                new AdoptionRecordDao(db), history, auth);
    }

    @Test
    void completesOneAdoptionAndUpdatesAnimalAtomically() throws Exception {
        try (Database db = Database.openInMemory()) {
            Dog dog = saveDog(new AnimalDao(db));
            StatusHistoryService history = new StatusHistoryService();
            var record = controller(db, history).markAdopted(dog,
                    new Adopter("Test Adopter", "test@example.com", ""));
            assertTrue(record.getId() > 0);
            assertEquals(1, new AdoptionRecordDao(db).count());
            assertEquals(AnimalStatus.ADOPTED, new AnimalDao(db).findById(dog.getId()).orElseThrow().getStatus());
            assertEquals(AnimalStatus.ADOPTED, dog.getStatus());
            assertEquals(1, history.size());
        }
    }

    @Test
    void preventsSecondAdoptionForSameAnimal() throws Exception {
        try (Database db = Database.openInMemory()) {
            Dog dog = saveDog(new AnimalDao(db));
            AdoptionController controller = controller(db, new StatusHistoryService());
            controller.markAdopted(dog, new Adopter("First", "first@example.com", ""));
            assertThrows(IllegalStateException.class,
                    () -> controller.markAdopted(dog, new Adopter("Second", "second@example.com", "")));
            assertEquals(1, new AdoptionRecordDao(db).count());
        }
    }

    @Test
    void failedRecordInsertRollsBackAdopterAndAnimalChange() throws Exception {
        try (Database db = Database.openInMemory()) {
            Dog dog = saveDog(new AnimalDao(db));
            StatusHistoryService history = new StatusHistoryService();
            AdoptionController controller = controller(db, history);
            try (var st = db.connection().createStatement()) {
                st.execute("CREATE TRIGGER block_adoption BEFORE INSERT ON adoption_record "
                        + "BEGIN SELECT RAISE(ABORT, 'test failure'); END");
            }
            assertThrows(SQLException.class, () -> controller.markAdopted(dog,
                    new Adopter("Rollback", "rollback@example.com", "")));
            assertEquals(0, new AdoptionRecordDao(db).count());
            assertTrue(new AdopterDao(db).findByEmail("rollback@example.com").isEmpty());
            assertEquals(AnimalStatus.AVAILABLE, new AnimalDao(db).findById(dog.getId()).orElseThrow().getStatus());
            assertEquals(AnimalStatus.AVAILABLE, dog.getStatus());
            assertTrue(history.isEmpty());
        }
    }

    @Test
    void undoCannotContradictCompletedAdoption() throws Exception {
        try (Database db = Database.openInMemory()) {
            Dog dog = saveDog(new AnimalDao(db));
            StatusHistoryService history = new StatusHistoryService();
            AdoptionController controller = controller(db, history);
            controller.markAdopted(dog, new Adopter("Adopter", "adopter@example.com", ""));
            assertThrows(SQLException.class, controller::undoLastStatus);
            assertEquals(1, new AdoptionRecordDao(db).count());
            assertEquals(AnimalStatus.ADOPTED, new AnimalDao(db).findById(dog.getId()).orElseThrow().getStatus());
        }
    }
}
