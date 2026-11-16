package pawtrack.service;

import pawtrack.controller.AuthController;
import pawtrack.controller.UnauthorizedException;
import pawtrack.dao.ReservationDao;
import pawtrack.model.Role;
import pawtrack.model.User;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** Read-only reservation updates for the currently authenticated adopter. */
public final class AdopterNotificationService {
    public record Update(String animalName, String status, LocalDateTime occurredAt, String message) { }

    private final AuthController auth;
    private final ReservationDao reservations;
    private final Clock clock;

    public AdopterNotificationService(AuthController auth, ReservationDao reservations) {
        this(auth, reservations, Clock.systemDefaultZone());
    }

    public AdopterNotificationService(AuthController auth, ReservationDao reservations, Clock clock) {
        this.auth = Objects.requireNonNull(auth);
        this.reservations = Objects.requireNonNull(reservations);
        this.clock = Objects.requireNonNull(clock);
    }

    /** Never accepts an adopter ID supplied by a UI control. */
    public List<Update> myUpdates(int limit) throws SQLException {
        User current = auth.getCurrentUser();
        if (current == null || current.getRole() != Role.ADOPTER || current.getAdopterId() == null) {
            throw new UnauthorizedException("An authenticated adopter is required");
        }
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Notification limit must be between 1 and 100");
        }
        return reservations.recentForAdopter(current.getAdopterId(), limit).stream()
                .map(event -> new Update(event.animalName(), status(event.reservation(), LocalDateTime.now(clock)),
                        event.reservation().reservedAt(), message(event.reservation(), LocalDateTime.now(clock))))
                .toList();
    }

    public static String status(ReservationDao.Reservation reservation, LocalDateTime now) {
        if (reservation.status().equals("ACTIVE") && !reservation.expiresAt().isAfter(now)) return "EXPIRED";
        if (reservation.status().equals("ACTIVE")) return "RESERVED";
        return reservation.status();
    }

    public static String message(ReservationDao.Reservation reservation, LocalDateTime now) {
        return switch (status(reservation, now)) {
            case "RESERVED" -> "Your application was reserved until " + reservation.expiresAt().toString().replace('T', ' ');
            case "CANCELLED" -> "The reservation was cancelled by shelter staff";
            case "EXPIRED" -> "The reservation deadline has passed";
            case "COMPLETED" -> "Your adoption was completed";
            default -> "Application update";
        };
    }
}
