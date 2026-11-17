package pawtrack.service;

import pawtrack.controller.AuthController;
import pawtrack.controller.UnauthorizedException;
import pawtrack.dao.ReservationDao;
import pawtrack.model.User;
import pawtrack.model.Role;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/** Read-only, account-scoped status of an adopter's own waitlist application. */
public final class MyApplicationStatusService {
    private final AuthController auth;
    private final ReservationDao reservations;
    private final Clock clock;

    public MyApplicationStatusService(AuthController auth, ReservationDao reservations) {
        this(auth, reservations, Clock.systemDefaultZone());
    }

    public MyApplicationStatusService(AuthController auth, ReservationDao reservations, Clock clock) {
        this.auth = Objects.requireNonNull(auth);
        this.reservations = Objects.requireNonNull(reservations);
        this.clock = Objects.requireNonNull(clock);
    }

    /** The adopter id comes exclusively from the authenticated session, never from the UI. */
    public ApplicationStatus statusForAnimal(int animalId) throws SQLException {
        User session = auth.getCurrentUser();
        if (session == null || session.getRole() != Role.ADOPTER || session.getAdopterId() == null) {
            throw new UnauthorizedException("A linked adopter session is required");
        }
        if (animalId <= 0) {
            throw new IllegalArgumentException("Invalid animal ID");
        }
        Optional<ReservationDao.Reservation> latest =
                reservations.latestForApplicant(animalId, session.getAdopterId());
        return classify(latest, LocalDateTime.now(clock));
    }

    public record ApplicationStatus(String label, String details) {
        public ApplicationStatus {
            Objects.requireNonNull(label);
            Objects.requireNonNull(details);
        }
        public String display() {
            return details.isBlank() ? label : label + " — " + details;
        }
    }

    /** Presentation logic only; does not modify the queue or reservation database. */
    public static ApplicationStatus classify(Optional<ReservationDao.Reservation> latest, LocalDateTime now) {
        Objects.requireNonNull(latest);
        Objects.requireNonNull(now);
        if (latest.isEmpty()) {
            return new ApplicationStatus("WAITING", "In the FIFO queue");
        }
        ReservationDao.Reservation value = latest.get();
        return switch (value.status()) {
            case "ACTIVE" -> !value.expiresAt().isAfter(now)
                    ? new ApplicationStatus("EXPIRED", "Reservation deadline passed")
                    : new ApplicationStatus("RESERVED", "Until " + value.expiresAt().toString().replace('T', ' '));
            case "EXPIRED" -> new ApplicationStatus("EXPIRED", "Reservation deadline passed");
            case "CANCELLED" -> new ApplicationStatus("CANCELLED", "Reservation cancelled by staff");
            case "COMPLETED" -> new ApplicationStatus("COMPLETED", "Adoption finalized");
            default -> new ApplicationStatus("WAITING", "In the FIFO queue");
        };
    }
}
