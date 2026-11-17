package pawtrack.service;

import org.junit.jupiter.api.Test;
import pawtrack.controller.AuthController;
import pawtrack.controller.UnauthorizedException;
import pawtrack.dao.*;
import pawtrack.model.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdopterNotificationServiceTest {
    @Test void rejectsUnsignedAndStaffSessions() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            var service = new AdopterNotificationService(auth, new ReservationDao(db));
            assertThrows(UnauthorizedException.class, () -> service.myUpdates(10));
            auth.login("admin", "admin123");
            assertThrows(UnauthorizedException.class, () -> service.myUpdates(10));
        }
    }

    @Test void scopesNotificationsToSignedInAdopter() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            auth.createAccount("alpha", "Abcdef1!");
            auth.createAccount("bravo", "Abcdef1!");
            auth.login("alpha", "Abcdef1!");
            int first = auth.getCurrentUser().getAdopterId();
            auth.login("bravo", "Abcdef1!");
            int second = auth.getCurrentUser().getAdopterId();
            var reservations = new ReservationDao(db);
            try (var stmt = db.connection().createStatement()) {
                stmt.executeUpdate("INSERT INTO animal(id,name,species,breed,age_months,intake_date,status) VALUES (101,'Milo','DOG','Mixed',12,'2026-11-01','AVAILABLE')");
                stmt.executeUpdate("INSERT INTO animal(id,name,species,breed,age_months,intake_date,status) VALUES (102,'Luna','CAT','Mixed',12,'2026-11-01','AVAILABLE')");
            }
            LocalDateTime now = LocalDateTime.now();
            reservations.insertActive(101, first, now, now.plusDays(3));
            reservations.insertActive(102, second, now, now.plusDays(3));
            var service = new AdopterNotificationService(auth, reservations);
            assertEquals(List.of("Luna"), service.myUpdates(10).stream().map(AdopterNotificationService.Update::animalName).toList());
            auth.login("alpha", "Abcdef1!");
            assertEquals(List.of("Milo"), service.myUpdates(10).stream().map(AdopterNotificationService.Update::animalName).toList());
        }
    }

    @Test void displaysReservationChangesAndValidatesLimits() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            auth.createAccount("alpha", "Abcdef1!");
            auth.login("alpha", "Abcdef1!");
            var service = new AdopterNotificationService(auth, new ReservationDao(db));
            assertTrue(service.myUpdates(10).isEmpty());
            assertThrows(IllegalArgumentException.class, () -> service.myUpdates(0));
            LocalDateTime now = LocalDateTime.now();
            var active = new ReservationDao.Reservation(1,1,1,now,now.plusDays(3),"ACTIVE");
            assertEquals("RESERVED", AdopterNotificationService.status(active, now));
            assertEquals("EXPIRED", AdopterNotificationService.status(active, now.plusDays(4)));
            assertEquals("CANCELLED", AdopterNotificationService.status(
                    new ReservationDao.Reservation(1,1,1,now,now.plusDays(3),"CANCELLED"), now));
        }
    }
}
