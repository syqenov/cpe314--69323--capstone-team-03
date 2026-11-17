package pawtrack.service;

import pawtrack.controller.AuthController;
import pawtrack.dao.AnimalDao;
import pawtrack.dao.Database;
import pawtrack.dao.ReservationDao;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/** Staff-only, atomic first-in-line reservation with expiry and cancellation. */
public final class ReservationService {
    private final Database database;
    private final ReservationDao reservations;
    private final AnimalDao animals;
    private final AuthController auth;
    private final Clock clock;

    public ReservationService(Database database, ReservationDao reservations, AnimalDao animals,
                              AuthController auth) {
        this(database, reservations, animals, auth, Clock.systemDefaultZone());
    }

    public ReservationService(Database database, ReservationDao reservations, AnimalDao animals,
                              AuthController auth, Clock clock) {
        this.database = Objects.requireNonNull(database);
        this.reservations = Objects.requireNonNull(reservations);
        this.animals = Objects.requireNonNull(animals);
        this.auth = Objects.requireNonNull(auth);
        this.clock = Objects.requireNonNull(clock);
    }

    public Optional<ReservationDao.Reservation> activeForAnimal(int animalId) throws SQLException {
        auth.requireStaff();
        expireOverdue();
        return reservations.activeForAnimal(animalId);
    }

    /** Idempotent, clock-based expiry; safe to call on each waitlist refresh. */
    public int expireOverdue() throws SQLException {
        auth.requireStaff();
        synchronized (database) {
            return reservations.expireDue(LocalDateTime.now(clock));
        }
    }

    /** Cancel only the active reservation; later applicants keep their positions. */
    public void cancelReservation(int animalId) throws SQLException {
        auth.requireStaff();
        synchronized (database) {
            expireOverdue();
            if (!reservations.cancelActive(animalId)) {
                throw new IllegalStateException("No active reservation to cancel");
            }
        }
    }

    public Optional<ReservationDao.Reservation> latestForApplicant(int animalId, int adopterId)
            throws SQLException {
        auth.requireStaff();
        expireOverdue();
        return reservations.latestForApplicant(animalId, adopterId);
    }

    public ReservationDao.Reservation reserveFirst(int animalId) throws SQLException {
        auth.requireStaff();
        expireOverdue();
        synchronized (database) {
            Connection connection = database.connection();
            if (!connection.getAutoCommit()) {
                throw new SQLException("Another database transaction is in progress");
            }
            connection.setAutoCommit(false);
            try {
                Animal animal = animals.findById(animalId)
                        .orElseThrow(() -> new IllegalArgumentException("Animal not found"));
                if (animal.getStatus() == AnimalStatus.ADOPTED) {
                    throw new IllegalStateException("An adopted animal cannot be reserved");
                }
                if (reservations.activeForAnimal(animalId).isPresent()) {
                    throw new IllegalStateException("This animal already has an active reservation");
                }
                int firstAdopter = reservations.firstQueuedAdopter(animalId)
                        .orElseThrow(() -> new IllegalStateException("No applicants in this animal's waitlist"));
                LocalDateTime now = LocalDateTime.now(clock);
                ReservationDao.Reservation reservation = reservations.insertActive(animalId,
                        firstAdopter, now, now.plusHours(72));
                connection.commit();
                return reservation;
            } catch (SQLException | RuntimeException failure) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw failure;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }
}
