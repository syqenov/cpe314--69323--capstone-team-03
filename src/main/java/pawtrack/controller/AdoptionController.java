package pawtrack.controller;

import pawtrack.dao.AdopterDao;
import pawtrack.dao.AdoptionRecordDao;
import pawtrack.dao.AnimalDao;
import pawtrack.dao.Database;
import pawtrack.model.Adopter;
import pawtrack.model.AdoptionRecord;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.StatusChange;
import pawtrack.service.StatusHistoryService;
import pawtrack.service.AdoptionTransactionService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Handles adoption workflow and status changes. Contains the persistence and
 * business rules so the UI only needs to call one method per click.
 */
public final class AdoptionController {

    private final AnimalDao animalDao;
    private final AdopterDao adopterDao;
    private final AdoptionRecordDao recordDao;
    private final StatusHistoryService history;
    private final AuthController auth;
    private final AdoptionTransactionService adoptionTransactions;
    private final Database database;

    /**
     * Legacy constructor retained for read-only callers and existing tests.
     * Completing an adoption requires the database-aware constructor below.
     */
    @Deprecated
    public AdoptionController(AnimalDao animalDao, AdopterDao adopterDao, AdoptionRecordDao recordDao,
                              StatusHistoryService history, AuthController auth) {
        this.animalDao = animalDao;
        this.adopterDao = adopterDao;
        this.recordDao = recordDao;
        this.history = history;
        this.auth = auth;
        this.adoptionTransactions = null;
        this.database = null;
    }

    public AdoptionController(Database database, AnimalDao animalDao, AdopterDao adopterDao,
                              AdoptionRecordDao recordDao, StatusHistoryService history, AuthController auth) {
        this.animalDao = animalDao;
        this.adopterDao = adopterDao;
        this.recordDao = recordDao;
        this.history = history;
        this.auth = auth;
        this.adoptionTransactions = new AdoptionTransactionService(database, animalDao, adopterDao,
                recordDao, history);
        this.database = database;
    }

    private void requireStaff() {
        if (auth == null) {
            throw new UnauthorizedException("Authenticated staff session required");
        }
        auth.requireStaff();
    }

    /** Staff-only lookup for an adoption completion confirmation. */
    public Optional<Adopter> findAdopterForStaff(int adopterId) throws SQLException {
        requireStaff();
        return adopterDao.findById(adopterId);
    }

    /** Returns only the signed-in adopter's own profile. */
    public Optional<Adopter> findMyAdopter() throws SQLException {
        if (auth == null || auth.getCurrentUser() == null) {
            throw new UnauthorizedException("Login required");
        }
        Integer adopterId = auth.getCurrentUser().getAdopterId();
        if (adopterId == null) return Optional.empty();
        auth.requireAdopterOwnership(adopterId);
        return adopterDao.findById(adopterId);
    }

    /** Staff-only data source for the adoption trend panel. */
    public List<AdoptionRecord> allAdoptionsForStaff() throws SQLException {
        requireStaff();
        return recordDao.findAll();
    }

    /**
     * Completes an adoption: persists the adopter, writes the adoption record,
     * marks the animal ADOPTED and records the status change for undo.
     */
    public AdoptionRecord markAdopted(Animal animal, Adopter adopter) throws SQLException {
        requireStaff();
        if (adoptionTransactions == null) {
            throw new IllegalStateException("A database-aware adoption controller is required");
        }
        return adoptionTransactions.complete(animal, adopter);
    }

    /**
     * Staff completes an existing reservation using stored animal and adopter IDs.
     * No free-text adopter data is accepted from the UI.
     */
    public AdoptionRecord completeReserved(int animalId) throws SQLException {
        requireStaff();
        if (adoptionTransactions == null) {
            throw new IllegalStateException("A database-aware adoption controller is required");
        }
        pawtrack.dao.ReservationDao reservations =
                new pawtrack.dao.ReservationDao(database);
        reservations.expireDue(java.time.LocalDateTime.now());
        var active = reservations.activeForAnimal(animalId)
                .orElseThrow(() -> new IllegalStateException("No active reservation for this animal"));
        Animal animal = animalDao.findById(animalId)
                .orElseThrow(() -> new SQLException("Animal not found"));
        Adopter adopter = adopterDao.findById(active.adopterId())
                .orElseThrow(() -> new SQLException("Reserved adopter not found"));
        return adoptionTransactions.completeReserved(animal, adopter);
    }

    /** Reverts the most recent status change; returns the undone change if any. */
    public Optional<StatusChange> undoLastStatus() throws SQLException {
        requireStaff();
        // Undoing ADOPTED without reversing the adoption record would corrupt
        // the application state. A dedicated reversal workflow is required.
        if (history.peekMostRecent().filter(change -> change.to() == AnimalStatus.ADOPTED).isPresent()) {
            throw new SQLException("Completed adoptions cannot be undone using status history");
        }
        Optional<StatusChange> undone = history.undo();
        if (undone.isPresent()) {
            StatusChange change = undone.get();
            animalDao.updateStatus(change.animalId(), change.from());
        }
        return undone;
    }

    /** All adoption records for a specific adopter. */
    public List<AdoptionRecord> findAdoptionsByAdopterId(int adopterId) throws SQLException {
        if (auth == null) {
            throw new UnauthorizedException("Login required");
        }
        auth.requireAdopterOwnership(adopterId);
        return recordDao.findByAdopterId(adopterId);
    }

}
