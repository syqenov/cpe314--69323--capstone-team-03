package pawtrack.controller;

import pawtrack.dao.AdopterDao;
import pawtrack.dao.AdoptionRecordDao;
import pawtrack.dao.AnimalDao;
import pawtrack.model.Adopter;
import pawtrack.model.AdoptionRecord;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.StatusChange;
import pawtrack.service.StatusHistoryService;

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

    public AdoptionController(AnimalDao animalDao, AdopterDao adopterDao, AdoptionRecordDao recordDao,
                              StatusHistoryService history, AuthController auth) {
        this.animalDao = animalDao;
        this.adopterDao = adopterDao;
        this.recordDao = recordDao;
        this.history = history;
        this.auth = auth;
    }

    /**
     * Completes an adoption: persists the adopter, writes the adoption record,
     * marks the animal ADOPTED and records the status change for undo.
     */
    public AdoptionRecord markAdopted(Animal animal, Adopter adopter) throws SQLException {
        if (auth != null) {
            auth.requireStaff();
        }
        Adopter persisted = adopterDao.findOrCreate(adopter);
        AdoptionRecord record = new AdoptionRecord(animal.getId(), persisted.getId(), LocalDate.now(),
                animal.adoptionFee());
        recordDao.insert(record);
        AnimalStatus previous = animal.getStatus();
        animal.setStatus(AnimalStatus.ADOPTED);
        animalDao.updateStatus(animal.getId(), AnimalStatus.ADOPTED);
        history.record(animal.getId(), previous, AnimalStatus.ADOPTED);
        return record;
    }

    /** Reverts the most recent status change; returns the undone change if any. */
    public Optional<StatusChange> undoLastStatus() throws SQLException {
        Optional<StatusChange> undone = history.undo();
        if (undone.isPresent()) {
            StatusChange change = undone.get();
            animalDao.updateStatus(change.animalId(), change.from());
        }
        return undone;
    }

    /** All adoption records for a specific adopter. */
    public List<AdoptionRecord> findAdoptionsByAdopterId(int adopterId) throws SQLException {
        return recordDao.findByAdopterId(adopterId);
    }

    public AdoptionRecordDao getRecordDao() {
        return recordDao;
    }
}