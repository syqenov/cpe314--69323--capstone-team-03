package pawtrack.controller;

import org.junit.jupiter.api.Test;
import pawtrack.dao.*;
import pawtrack.model.*;
import pawtrack.service.*;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AdopterOwnershipTest {
    @Test
    void newAccountIsPermanentlyLinked() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            User created = auth.createAccount("newperson", "Abcdef1!");
            assertNotNull(created.getAdopterId());
            assertEquals(created.getAdopterId(), auth.login("newperson", "Abcdef1!").orElseThrow().getAdopterId());
            assertTrue(new AdopterDao(db).findById(created.getAdopterId()).isPresent());
        }
    }

    @Test
    void legacyAccountIsLinkedWithoutClaimingOtherPersonsRecord() throws Exception {
        try (Database db = Database.openInMemory()) {
            AdopterDao people = new AdopterDao(db);
            int oldId = people.insert(new Adopter("user", "somebody@example.com", ""));
            AuthController auth = new AuthController(new UserDao(db));
            User linked = auth.login("user", "user123").orElseThrow();
            assertNotNull(linked.getAdopterId());
            assertNotEquals(oldId, linked.getAdopterId());
            assertEquals(linked.getAdopterId(), auth.login("user", "user123").orElseThrow().getAdopterId());
        }
    }

    @Test
    void adopterCannotAccessSomeoneElsesRecords() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            User first = auth.createAccount("first", "Abcdef1!");
            User second = auth.createAccount("second", "Abcdef1!");
            auth.login("first", "Abcdef1!");
            WaitlistController queue = new WaitlistController(new WaitlistService(),
                    new WaitlistDao(db), new AdopterDao(db), new AnimalDao(db),
                    new StatusHistoryService(), auth);
            assertThrows(UnauthorizedException.class,
                    () -> queue.findByAdopterId(second.getAdopterId()));
            assertDoesNotThrow(() -> queue.findByAdopterId(first.getAdopterId()));
            assertThrows(UnauthorizedException.class,
                    () -> queue.recordInterest(new Dog(1, "Buddy", "Retriever", 24,
                            LocalDate.now(), AnimalStatus.AVAILABLE),
                            new AdopterDao(db).findById(second.getAdopterId()).orElseThrow()));
            assertThrows(UnauthorizedException.class,
                    () -> auth.requireAdopterOwnership(second.getAdopterId()));
        }
    }

    @Test
    void adopterCannotReadOtherAdoptionRecords() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            auth.createAccount("first", "Abcdef1!");
            User second = auth.createAccount("second", "Abcdef1!");
            auth.login("first", "Abcdef1!");
            AdoptionController controller = new AdoptionController(new AnimalDao(db),
                    new AdopterDao(db), new AdoptionRecordDao(db), new StatusHistoryService(), auth);
            assertThrows(UnauthorizedException.class,
                    () -> controller.findAdoptionsByAdopterId(second.getAdopterId()));
        }
    }
}
