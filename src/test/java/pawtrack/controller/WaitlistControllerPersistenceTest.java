package pawtrack.controller;

import org.junit.jupiter.api.Test;
import pawtrack.dao.AdopterDao;
import pawtrack.dao.AnimalDao;
import pawtrack.dao.Database;
import pawtrack.dao.WaitlistDao;
import pawtrack.model.Adopter;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.Dog;
import pawtrack.service.StatusHistoryService;
import pawtrack.service.WaitlistService;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaitlistControllerPersistenceTest {

    @Test
    void pollingOneAdopterKeepsOthersInOrder() throws Exception {
        try (Database db = Database.openInMemory()) {
            AnimalDao animals = new AnimalDao(db);
            AdopterDao adopters = new AdopterDao(db);
            WaitlistDao entries = new WaitlistDao(db);
            WaitlistService fifo = new WaitlistService();
            WaitlistController controller = new WaitlistController(
                    fifo, entries, adopters, animals, new StatusHistoryService());
            Animal animal = new Dog(0, "Buddy", "Retriever", 24,
                    LocalDate.of(2026, 1, 1), AnimalStatus.AVAILABLE);
            animals.insert(animal);

            assertEquals(1, controller.recordInterest(animal, new Adopter("A", "a@example.com", "")));
            assertEquals(2, controller.recordInterest(animal, new Adopter("B", "b@example.com", "")));
            assertEquals(3, controller.recordInterest(animal, new Adopter("C", "c@example.com", "")));
            assertEquals("A", controller.pollNext(animal.getId()).orElseThrow().getName());
            assertEquals(2, entries.loadAll().size());
            assertEquals("B", fifo.peekNext(animal.getId()).orElseThrow().getName());
            assertEquals(1, entries.loadAll().get(0).position());
            assertEquals(2, entries.loadAll().get(1).position());
            assertEquals("B", controller.pollNext(animal.getId()).orElseThrow().getName());
            assertEquals(1, entries.loadAll().size());
        }
    }

    @Test
    void duplicateInterestPreservesOriginalRowAndPosition() throws Exception {
        try (Database db = Database.openInMemory()) {
            AnimalDao animals = new AnimalDao(db);
            AdopterDao adopters = new AdopterDao(db);
            WaitlistDao entries = new WaitlistDao(db);
            WaitlistService fifo = new WaitlistService();
            WaitlistController controller = new WaitlistController(
                    fifo, entries, adopters, animals, new StatusHistoryService());
            Animal animal = new Dog(0, "Buddy", "Retriever", 24,
                    LocalDate.of(2026, 1, 1), AnimalStatus.AVAILABLE);
            animals.insert(animal);
            assertEquals(1, controller.recordInterest(animal, new Adopter("A", "a@example.com", "")));
            var original = entries.loadAll().get(0);
            assertEquals(1, controller.recordInterest(animal, new Adopter("A", "A@example.com", "")));
            assertEquals(1, entries.loadAll().size());
            assertEquals(original.id(), entries.loadAll().get(0).id());
            assertEquals(original.requestedAt(), entries.loadAll().get(0).requestedAt());
            assertTrue(fifo.hasAnyone(animal.getId()));
        }
    }
}
