package pawtrack.service;

import org.junit.jupiter.api.Test;
import pawtrack.controller.*;
import pawtrack.dao.*;
import pawtrack.model.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class CombinedM3FeaturesTest {
    @Test void adopterCanEditOwnProfile() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            auth.createAccount("alex", "Password1!"); auth.login("alex", "Password1!");
            AccountSettingsService svc = new AccountSettingsService(auth, new AdopterDao(db));
            svc.updateMyContact("Alex Smith", "alex@example.com", "12345");
            assertEquals("Alex Smith", svc.myProfile().getName());
            assertEquals("alex@example.com", svc.myProfile().getEmail());
        }
    }
    @Test void profilesCannotBeAccessedWithoutAdopterLogin() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            AccountSettingsService svc = new AccountSettingsService(auth, new AdopterDao(db));
            assertThrows(UnauthorizedException.class, svc::myProfile);
            auth.login("admin", "admin123");
            assertThrows(UnauthorizedException.class, svc::myProfile);
        }
    }
    @Test void invalidProfileEmailIsRejected() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            auth.createAccount("alex", "Password1!"); auth.login("alex", "Password1!");
            AccountSettingsService svc = new AccountSettingsService(auth, new AdopterDao(db));
            assertThrows(IllegalArgumentException.class, () -> svc.updateMyContact("Alex", "not-email", ""));
        }
    }
    @Test void passwordChangeRequiresOldPassword() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            auth.createAccount("alex", "Password1!"); auth.login("alex", "Password1!");
            assertThrows(IllegalArgumentException.class, () -> auth.changeMyPassword("wrong", "NewPassw0rd!"));
            auth.changeMyPassword("Password1!", "NewPassw0rd!");
            auth.logout();
            assertTrue(auth.login("alex", "Password1!").isEmpty());
            assertTrue(auth.login("alex", "NewPassw0rd!").isPresent());
        }
    }
    @Test void adopterCanWithdrawOwnPendingRequestWithoutReorderingOtherAnimal() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            User first = auth.createAccount("first", "Password1!");
            User second = auth.createAccount("second", "Password1!");
            AnimalDao animals = new AnimalDao(db);
            Dog dog = new Dog("Biscuit", "Retriever", 12, LocalDate.now());
            animals.insert(dog);
            WaitlistService memory = new WaitlistService();
            WaitlistController queue = new WaitlistController(memory, new WaitlistDao(db), new AdopterDao(db),
                    animals, new StatusHistoryService(), auth);
            auth.login("first", "Password1!");
            queue.recordInterest(dog, new AdopterDao(db).findById(first.getAdopterId()).orElseThrow());
            auth.login("second", "Password1!");
            queue.recordInterest(dog, new AdopterDao(db).findById(second.getAdopterId()).orElseThrow());
            auth.login("first", "Password1!");
            queue.withdrawMyInterest(dog.getId(), new ReservationDao(db));
            var rows = new WaitlistDao(db).loadAll();
            assertEquals(1, rows.size());
            assertEquals(second.getAdopterId().intValue(), rows.get(0).adopterId());
            assertEquals(1, rows.get(0).position());
            assertEquals(1, memory.size(dog.getId()));
        }
    }
    @Test void cannotWithdrawSomebodyElsesRequest() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            auth.createAccount("first", "Password1!");
            auth.login("first", "Password1!");
            WaitlistController queue = new WaitlistController(new WaitlistService(), new WaitlistDao(db),
                    new AdopterDao(db), new AnimalDao(db), new StatusHistoryService(), auth);
            assertThrows(IllegalArgumentException.class, () -> queue.withdrawMyInterest(123, new ReservationDao(db)));
        }
    }
    @Test void notificationReadStateIsPrivateAndPersisted() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            User first = auth.createAccount("first", "Password1!");
            auth.createAccount("second", "Password1!");
            Dog dog = new Dog("Biscuit", "Retriever", 12, LocalDate.now());
            new AnimalDao(db).insert(dog);
            new ReservationDao(db).insertActive(dog.getId(), first.getAdopterId(),
                    LocalDateTime.now(), LocalDateTime.now().plusHours(48));
            auth.login("first", "Password1!");
            NotificationReadService svc = new NotificationReadService(db, auth, new ReservationDao(db));
            assertEquals(1, svc.unreadCount());
            var notice = svc.myNotices().get(0);
            svc.markRead(notice.reservationId(), notice.status());
            assertEquals(0, svc.unreadCount());
            auth.login("second", "Password1!");
            assertEquals(0, svc.unreadCount());
            assertThrows(UnauthorizedException.class, () -> svc.markRead(notice.reservationId(), notice.status()));
        }
    }
    @Test void anonymousUsersCannotReadNotifications() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            NotificationReadService svc = new NotificationReadService(db, auth, new ReservationDao(db));
            assertThrows(UnauthorizedException.class, svc::unreadCount);
        }
    }
    @Test void staffReviewRespectsFifoAndAdopterSeesOnlyOwnStage() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            User first = auth.createAccount("first", "Password1!");
            User second = auth.createAccount("second", "Password1!");
            Dog dog = new Dog("Milo", "Retriever", 12, LocalDate.now());
            new AnimalDao(db).insert(dog);
            WaitlistController queue = new WaitlistController(new WaitlistService(), new WaitlistDao(db),
                    new AdopterDao(db), new AnimalDao(db), new StatusHistoryService(), auth);
            auth.login("first", "Password1!");
            queue.recordInterest(dog, new AdopterDao(db).findById(first.getAdopterId()).orElseThrow());
            auth.login("second", "Password1!");
            queue.recordInterest(dog, new AdopterDao(db).findById(second.getAdopterId()).orElseThrow());
            auth.login("admin", "admin123");
            ApplicationReviewService service = new ApplicationReviewService(auth, db, new ReservationDao(db));
            assertThrows(IllegalStateException.class,
                    () -> service.startFirstReview(dog.getId(), second.getAdopterId(), "second first"));
            service.startFirstReview(dog.getId(), first.getAdopterId(), "Documents reviewed");
            assertEquals("UNDER_REVIEW", service.get(dog.getId(), first.getAdopterId()).orElseThrow().status());
            auth.login("first", "Password1!");
            assertEquals("UNDER_REVIEW", service.myStage(dog.getId()).orElseThrow());
            auth.login("second", "Password1!");
            assertTrue(service.myStage(dog.getId()).isEmpty());
        }
    }
    @Test void staffHistoryIsPrivate() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            ApplicationHistoryService service = new ApplicationHistoryService(auth, db);
            assertThrows(UnauthorizedException.class, () -> service.recent(10));
            auth.login("admin", "admin123");
            assertTrue(service.recent(10).isEmpty());
        }
    }
    @Test void onlyOwnAdoptionReceiptIsVisible() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            User first = auth.createAccount("first", "Password1!");
            auth.createAccount("second", "Password1!");
            Dog dog = new Dog("Milo", "Retriever", 12, LocalDate.now());
            new AnimalDao(db).insert(dog);
            AdoptionRecord record = new AdoptionRecord(dog.getId(), first.getAdopterId(), LocalDate.now(), 100.0);
            new AdoptionRecordDao(db).insert(record);
            AdoptionReceiptService service = new AdoptionReceiptService(auth,
                    new AdoptionRecordDao(db), new AnimalDao(db), new AdopterDao(db));
            auth.login("second", "Password1!");
            assertThrows(UnauthorizedException.class, () -> service.receiptFor(record.getId()));
            auth.login("first", "Password1!");
            assertTrue(service.receiptFor(record.getId()).contains("Milo"));
        }
    }

}
