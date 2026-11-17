package pawtrack.service;

import org.junit.jupiter.api.Test;
import pawtrack.controller.AuthController;
import pawtrack.controller.UnauthorizedException;
import pawtrack.dao.*;
import pawtrack.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReservationServiceTest {
    private static Animal dog() {
        return new Dog(0, "Atlas", "Retriever", 24, LocalDate.now(), AnimalStatus.AVAILABLE);
    }

    private static AuthController staff(Database db) {
        AuthController auth = new AuthController(new UserDao(db));
        assertTrue(auth.login("admin", "admin123").isPresent());
        return auth;
    }

    @Test
    void reservesFirstApplicantAndKeepsWaitlistOrder() throws Exception {
        try (Database db = Database.openInMemory()) {
            Animal pet = dog();
            new AnimalDao(db).insert(pet);
            AdopterDao adopters = new AdopterDao(db);
            int first = adopters.insert(new Adopter("First", "first@example.test", ""));
            int second = adopters.insert(new Adopter("Second", "second@example.test", ""));
            WaitlistDao queue = new WaitlistDao(db);
            queue.insert(pet.getId(), first, LocalDateTime.now().minusDays(2), 1);
            queue.insert(pet.getId(), second, LocalDateTime.now().minusDays(1), 2);
            ReservationService service = new ReservationService(db, new ReservationDao(db),
                    new AnimalDao(db), staff(db));
            ReservationDao.Reservation reservation = service.reserveFirst(pet.getId());
            assertEquals(first, reservation.adopterId());
            assertEquals(72, java.time.Duration.between(reservation.reservedAt(), reservation.expiresAt()).toHours());
            assertEquals(2, queue.loadAll().size());
            assertEquals(1, queue.loadAll().get(0).position());
            assertEquals(2, queue.loadAll().get(1).position());
        }
    }

    @Test
    void preventsSecondActiveReservation() throws Exception {
        try (Database db = Database.openInMemory()) {
            Animal pet = dog();
            new AnimalDao(db).insert(pet);
            int adopterId = new AdopterDao(db).insert(new Adopter("First", "first@example.test", ""));
            new WaitlistDao(db).insert(pet.getId(), adopterId, LocalDateTime.now(), 1);
            ReservationService service = new ReservationService(db, new ReservationDao(db),
                    new AnimalDao(db), staff(db));
            service.reserveFirst(pet.getId());
            assertThrows(IllegalStateException.class, () -> service.reserveFirst(pet.getId()));
            assertEquals(adopterId, service.activeForAnimal(pet.getId()).orElseThrow().adopterId());
        }
    }

    @Test
    void adopterCannotReserve() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            assertTrue(auth.login("user", "user123").isPresent());
            ReservationService service = new ReservationService(db, new ReservationDao(db),
                    new AnimalDao(db), auth);
            assertThrows(UnauthorizedException.class, () -> service.reserveFirst(1));
            assertThrows(UnauthorizedException.class, () -> service.activeForAnimal(1));
        }
    }

    @Test
    void blocksAdoptionByPersonOtherThanReservedApplicant() throws Exception {
        try (Database db = Database.openInMemory()) {
            Animal pet = dog();
            new AnimalDao(db).insert(pet);
            AdopterDao adopters = new AdopterDao(db);
            Adopter first = new Adopter("First", "first@example.test", "");
            Adopter second = new Adopter("Second", "second@example.test", "");
            adopters.insert(first);
            adopters.insert(second);
            new WaitlistDao(db).insert(pet.getId(), first.getId(), LocalDateTime.now(), 1);
            AuthController auth = staff(db);
            ReservationDao reserveDao = new ReservationDao(db);
            new ReservationService(db, reserveDao, new AnimalDao(db), auth).reserveFirst(pet.getId());
            AdoptionTransactionService transactions = new AdoptionTransactionService(db, new AnimalDao(db),
                    adopters, new AdoptionRecordDao(db), new StatusHistoryService());
            assertThrows(IllegalStateException.class, () -> transactions.complete(pet, second));
            assertEquals(AnimalStatus.AVAILABLE, new AnimalDao(db).findById(pet.getId()).orElseThrow().getStatus());
            assertEquals("ACTIVE", reserveDao.activeForAnimal(pet.getId()).orElseThrow().status());
            transactions.complete(pet, first);
            assertEquals(AnimalStatus.ADOPTED, new AnimalDao(db).findById(pet.getId()).orElseThrow().getStatus());
            assertTrue(reserveDao.activeForAnimal(pet.getId()).isEmpty());
        }
    }

    @Test
    void staffCompletesSelectedReservationWithoutReenteringIdentity() throws Exception {
        try (Database db = Database.openInMemory()) {
            Animal pet = dog();
            AnimalDao animals = new AnimalDao(db);
            animals.insert(pet);
            AdopterDao adopters = new AdopterDao(db);
            Adopter first = new Adopter("First", "first@example.test", "");
            Adopter second = new Adopter("Second", "second@example.test", "");
            adopters.insert(first);
            adopters.insert(second);
            WaitlistDao queue = new WaitlistDao(db);
            queue.insert(pet.getId(), first.getId(), LocalDateTime.now().minusDays(1), 1);
            queue.insert(pet.getId(), second.getId(), LocalDateTime.now(), 2);
            AuthController auth = staff(db);
            ReservationDao reservationDao = new ReservationDao(db);
            new ReservationService(db, reservationDao, animals, auth).reserveFirst(pet.getId());
            AdoptionRecordDao records = new AdoptionRecordDao(db);
            pawtrack.controller.AdoptionController controller =
                    new pawtrack.controller.AdoptionController(db, animals, adopters, records,
                            new pawtrack.service.StatusHistoryService(), auth);
            AdoptionRecord completed = controller.completeReserved(pet.getId());
            assertEquals(first.getId(), completed.getAdopterId());
            assertEquals(AnimalStatus.ADOPTED, animals.findById(pet.getId()).orElseThrow().getStatus());
            assertTrue(reservationDao.activeForAnimal(pet.getId()).isEmpty());
            assertEquals(2, queue.loadAll().size());
            assertThrows(IllegalStateException.class, () -> controller.completeReserved(pet.getId()));
        }
    }

    @Test
    void cannotCompleteWithoutActiveReservation() throws Exception {
        try (Database db = Database.openInMemory()) {
            Animal pet = dog();
            AnimalDao animals = new AnimalDao(db);
            animals.insert(pet);
            AdopterDao adopters = new AdopterDao(db);
            AuthController auth = staff(db);
            pawtrack.controller.AdoptionController controller =
                    new pawtrack.controller.AdoptionController(db, animals, adopters,
                            new AdoptionRecordDao(db), new pawtrack.service.StatusHistoryService(), auth);
            assertThrows(IllegalStateException.class, () -> controller.completeReserved(pet.getId()));
            assertEquals(AnimalStatus.AVAILABLE, animals.findById(pet.getId()).orElseThrow().getStatus());
        }
    }

    @Test
    void expiredReservationAutomaticallyYieldsToNextQueuedApplicant() throws Exception {
        try (Database db = Database.openInMemory()) {
            Animal pet = dog();
            AnimalDao animals = new AnimalDao(db);
            animals.insert(pet);
            AdopterDao adopters = new AdopterDao(db);
            int first = adopters.insert(new Adopter("First", "expiry-first@example.test", ""));
            int second = adopters.insert(new Adopter("Second", "expiry-second@example.test", ""));
            WaitlistDao queue = new WaitlistDao(db);
            queue.insert(pet.getId(), first, LocalDateTime.now().minusDays(2), 1);
            queue.insert(pet.getId(), second, LocalDateTime.now().minusDays(1), 2);
            var fixedTime = java.time.Instant.parse("2026-11-15T12:00:00Z");
            var start = java.time.Clock.fixed(fixedTime, java.time.ZoneOffset.UTC);
            ReservationDao dao = new ReservationDao(db);
            ReservationService initial = new ReservationService(db, dao, animals, staff(db), start);
            assertEquals(first, initial.reserveFirst(pet.getId()).adopterId());
            var afterDeadline = java.time.Clock.offset(start, java.time.Duration.ofHours(73));
            ReservationService next = new ReservationService(db, dao, animals, staff(db), afterDeadline);
            assertTrue(next.activeForAnimal(pet.getId()).isEmpty());
            assertEquals("EXPIRED", dao.latestForApplicant(pet.getId(), first).orElseThrow().status());
            assertEquals(second, next.reserveFirst(pet.getId()).adopterId());
            assertEquals(2, queue.loadAll().size());
        }
    }

    @Test
    void cancellingReservationPreservesQueueAndAllowsNextApplicant() throws Exception {
        try (Database db = Database.openInMemory()) {
            Animal pet = dog();
            AnimalDao animals = new AnimalDao(db);
            animals.insert(pet);
            AdopterDao adopters = new AdopterDao(db);
            int first = adopters.insert(new Adopter("First", "cancel-first@example.test", ""));
            int second = adopters.insert(new Adopter("Second", "cancel-second@example.test", ""));
            WaitlistDao queue = new WaitlistDao(db);
            queue.insert(pet.getId(), first, LocalDateTime.now().minusDays(2), 1);
            queue.insert(pet.getId(), second, LocalDateTime.now().minusDays(1), 2);
            ReservationDao dao = new ReservationDao(db);
            ReservationService service = new ReservationService(db, dao, animals, staff(db));
            assertEquals(first, service.reserveFirst(pet.getId()).adopterId());
            service.cancelReservation(pet.getId());
            assertEquals("CANCELLED", dao.latestForApplicant(pet.getId(), first).orElseThrow().status());
            assertThrows(IllegalStateException.class, () -> service.cancelReservation(pet.getId()));
            assertEquals(second, service.reserveFirst(pet.getId()).adopterId());
            assertEquals(2, queue.loadAll().size());
        }
    }

    @Test
    void adopterCannotExpireOrCancelReservations() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            assertTrue(auth.login("user", "user123").isPresent());
            ReservationService service = new ReservationService(db, new ReservationDao(db),
                    new AnimalDao(db), auth);
            assertThrows(UnauthorizedException.class, service::expireOverdue);
            assertThrows(UnauthorizedException.class, () -> service.cancelReservation(1));
        }
    }

}
