package pawtrack.service;

import pawtrack.dao.AdopterDao;
import pawtrack.dao.AdoptionRecordDao;
import pawtrack.dao.AnimalDao;
import pawtrack.dao.Database;
import pawtrack.dao.ReservationDao;
import pawtrack.model.Adopter;
import pawtrack.model.AdoptionRecord;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Completes an adoption as one database transaction. Reservations and request
 * closure are deliberately separate work scheduled for the next migration.
 */
public final class AdoptionTransactionService {
    private final Database database;
    private final AnimalDao animalDao;
    private final AdopterDao adopterDao;
    private final AdoptionRecordDao recordDao;
    private final StatusHistoryService history;

    public AdoptionTransactionService(Database database, AnimalDao animalDao, AdopterDao adopterDao,
                                      AdoptionRecordDao recordDao, StatusHistoryService history) {
        this.database = Objects.requireNonNull(database);
        this.animalDao = Objects.requireNonNull(animalDao);
        this.adopterDao = Objects.requireNonNull(adopterDao);
        this.recordDao = Objects.requireNonNull(recordDao);
        this.history = Objects.requireNonNull(history);
    }

    /**
     * Atomically persists adopter, adoption record, and ADOPTED status.
     * Memory state and undo history are updated only after a successful commit.
     */
    public AdoptionRecord complete(Animal animal, Adopter adopter) throws SQLException {
        return completeInternal(animal, adopter, false);
    }

    /** Complete only an active reservation; cannot fall back to an unreserved adoption. */
    public AdoptionRecord completeReserved(Animal animal, Adopter adopter) throws SQLException {
        return completeInternal(animal, adopter, true);
    }

    private AdoptionRecord completeInternal(Animal animal, Adopter adopter, boolean requireReservation)
            throws SQLException {
        Objects.requireNonNull(animal, "Animal is required");
        Objects.requireNonNull(adopter, "Adopter is required");
        if (animal.getId() <= 0) {
            throw new IllegalArgumentException("Animal must be saved before adoption");
        }
        Connection connection = database.connection();
        synchronized (database) {
            if (!connection.getAutoCommit()) {
                throw new SQLException("Database is already handling another transaction");
            }
            // The transaction may fail after one or more successful SQL statements.
            // In that case, rollback restores all related database rows.
            connection.setAutoCommit(false);
            try {
                AnimalStatus oldStatus = animalDao.findById(animal.getId())
                        .orElseThrow(() -> new SQLException("Animal no longer exists"))
                        .getStatus();
                if (oldStatus == AnimalStatus.ADOPTED) {
                    throw new IllegalStateException("This animal has already been adopted");
                }
                if (recordDao.existsForAnimal(animal.getId())) {
                    throw new IllegalStateException("An adoption record already exists for this animal");
                }
                Adopter persisted = adopterDao.findOrCreate(adopter);
                // A reservation must belong to the same adopter who completes adoption.
                ReservationDao reservationDao = new ReservationDao(database);
                // A reservation that has passed its deadline cannot be completed.
                reservationDao.expireDue(LocalDateTime.now());
                var active = reservationDao.activeForAnimal(animal.getId());
                if (requireReservation && active.isEmpty()) {
                    throw new IllegalStateException("There is no active reservation for this animal");
                }
                if (active.isPresent() && active.get().adopterId() != persisted.getId()) {
                    throw new IllegalStateException("This animal is reserved for a different adopter");
                }
                if (!animalDao.updateStatusIfNotAdopted(animal.getId())) {
                    throw new SQLException("Animal changed before adoption could be completed");
                }
                AdoptionRecord record = new AdoptionRecord(animal.getId(), persisted.getId(),
                        LocalDate.now(), animal.adoptionFee());
                recordDao.insert(record);
                if (active.isPresent() && !reservationDao.markCompleted(active.get().id())) {
                    throw new SQLException("Reservation changed before adoption completion");
                }
                connection.commit();
                animal.setStatus(AnimalStatus.ADOPTED);
                history.record(animal.getId(), oldStatus, AnimalStatus.ADOPTED);
                return record;
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
