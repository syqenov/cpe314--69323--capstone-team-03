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

class AnimalControllerAuthorizationTest {

    private static Animal dog() {
        return new Dog(0, "StaffOnly", "Mixed", 12,
                LocalDate.of(2026, 1, 1), AnimalStatus.AVAILABLE);
    }

    @Test
    void noSessionAndAdopterCannotCreateOrDeleteAnimals() throws Exception {
        try (Database database = Database.openInMemory()) {
            AnimalDao dao = new AnimalDao(database);
            AuthController auth = new AuthController(new UserDao(database));
            AnimalController controller = new AnimalController(dao, auth);
            Animal animal = dog();

            assertThrows(UnauthorizedException.class, () -> controller.addAnimal(animal));
            auth.login("user", "user123").orElseThrow();
            assertThrows(UnauthorizedException.class, () -> controller.addAnimal(animal));
            assertEquals(0, dao.findAll().size());

            auth.login("admin", "admin123").orElseThrow();
            controller.addAnimal(animal);
            assertEquals(1, dao.findAll().size());

            auth.login("user", "user123").orElseThrow();
            assertThrows(UnauthorizedException.class, () -> controller.updateAnimal(animal));
            assertThrows(UnauthorizedException.class, () -> controller.deleteAnimal(animal.getId()));
            assertEquals(1, dao.findAll().size());
        }
    }

    @Test
    void staffCanUpdateAndDeleteButLogoutRevokesPermission() throws Exception {
        try (Database database = Database.openInMemory()) {
            AnimalDao dao = new AnimalDao(database);
            AuthController auth = new AuthController(new UserDao(database));
            auth.login("admin", "admin123").orElseThrow();
            AnimalController controller = new AnimalController(dao, auth);
            Animal animal = dog();
            controller.addAnimal(animal);
            assertTrue(controller.updateAnimal(animal));
            auth.logout();
            assertThrows(UnauthorizedException.class, () -> controller.deleteAnimal(animal.getId()));
            auth.login("admin", "admin123").orElseThrow();
            assertTrue(controller.deleteAnimal(animal.getId()));
        }
    }

    @Test
    void unauthenticatedLegacyControllerCannotMutateData() throws Exception {
        try (Database database = Database.openInMemory()) {
            AnimalController legacy = new AnimalController(new AnimalDao(database));
            assertThrows(UnauthorizedException.class, () -> legacy.addAnimal(dog()));
        }
    }
}
