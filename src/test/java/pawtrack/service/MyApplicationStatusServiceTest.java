package pawtrack.service;

import org.junit.jupiter.api.Test;
import pawtrack.controller.AuthController;
import pawtrack.controller.UnauthorizedException;
import pawtrack.dao.*;
import pawtrack.model.*;

import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MyApplicationStatusServiceTest {
    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 16, 9, 0);
    private static final Clock CLOCK = Clock.fixed(START.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    @Test void waitingWhenNoReservationExists() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            auth.createAccount("first", "Abcdef1!");
            auth.login("first", "Abcdef1!");
            var status = new MyApplicationStatusService(auth, new ReservationDao(db), CLOCK).statusForAnimal(5);
            assertEquals("WAITING", status.label());
            assertTrue(status.display().contains("FIFO"));
        }
    }

    @Test void reservationShowsDeadlineAndExpiredStatus() {
        var reservation = new ReservationDao.Reservation(1, 5, 8, START.minusHours(1), START.plusHours(71), "ACTIVE");
        var now = MyApplicationStatusService.classify(Optional.of(reservation), START);
        assertEquals("RESERVED", now.label());
        assertTrue(now.display().contains("2026-11-19"));
        assertEquals("EXPIRED", MyApplicationStatusService.classify(Optional.of(reservation), START.plusDays(4)).label());
        var cancelled = new ReservationDao.Reservation(1,5,8,START,START.plusDays(3),"CANCELLED");
        assertEquals("CANCELLED", MyApplicationStatusService.classify(Optional.of(cancelled),START).label());
    }

    @Test void requiresAuthenticatedLinkedAdopterAndNeverUsesAnArbitraryId() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            var service = new MyApplicationStatusService(auth, new ReservationDao(db), CLOCK);
            assertThrows(UnauthorizedException.class, () -> service.statusForAnimal(1));
            auth.login("admin", "admin123");
            assertThrows(UnauthorizedException.class, () -> service.statusForAnimal(1));
            auth.createAccount("first", "Abcdef1!");
            auth.login("first", "Abcdef1!");
            assertThrows(IllegalArgumentException.class, () -> service.statusForAnimal(0));
            assertEquals("WAITING", service.statusForAnimal(1).label());
        }
    }
}
