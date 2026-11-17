package pawtrack.controller;

import org.junit.jupiter.api.Test;
import pawtrack.dao.AnimalDao;
import pawtrack.dao.Database;
import pawtrack.dao.UserDao;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.Dog;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class AnimalControllerMissingAnimalTest {
    @Test
    void staleAnimalEditDoesNotMutateOrInsertAnything() throws Exception {
        try (Database database = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(database));
            auth.login("admin", "admin123").orElseThrow();
            AnimalDao dao = new AnimalDao(database);
            AnimalController controller = new AnimalController(dao, auth);
            Animal stale = new Dog(999999, "Old", "Mixed", 18,
                    LocalDate.of(2026, 1, 1), AnimalStatus.ADOPTED);
            assertFalse(controller.updateAnimal(stale));
            assertTrue(dao.findAll().isEmpty());
        }
    }
}
