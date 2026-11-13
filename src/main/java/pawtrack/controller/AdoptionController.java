package pawtrack.controller;

import pawtrack.dao.AdopterDao;
import pawtrack.dao.AdoptionRecordDao;
import pawtrack.dao.AnimalDao;
import pawtrack.dao.Database;
import pawtrack.dao.WaitlistDao;
import pawtrack.model.Adopter;
import pawtrack.model.AdoptionRecord;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.StatusChange;
import pawtrack.service.StatusHistoryService;
import pawtrack.service.WaitlistService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Handles adoption workflow, waitlist coordination, and status changes. 
 * Encapsulates transaction logic and role checks.
 */
public final class AdoptionController {

    private final Database database;
    private final AnimalDao animalDao;
    private final AdopterDao adopterDao;
    private final AdoptionRecordDao recordDao;
    private final WaitlistDao waitlistDao;
    private final StatusHistoryService history;
    private final WaitlistService waitlist;
    private final AuthController auth;

    public AdoptionController(Database database, AnimalDao animalDao, AdopterDao adopterDao, 
            AdoptionRecordDao recordDao, WaitlistDao waitlistDao,
            StatusHistoryService history, WaitlistService waitlist, 
            AuthController auth) {
				this.database = database;
				this.animalDao = animalDao;
				this.adopterDao = adopterDao;
				this.recordDao = recordDao;
				this.waitlistDao = waitlistDao;
				this.history = history;
				this.waitlist = waitlist;
				this.auth = auth;
			}

    /**
     * Completes an adoption within a database transaction: persists the adopter, 
     * writes the adoption record, updates status, and polls/removes the next waitlist entry.
     */
    public AdoptionRecord markAdopted(Animal animal, Adopter adopter) throws SQLException {
        if (auth != null) {
            auth.requireStaff();
        }
        try {
            database.beginTransaction();

            Adopter persisted = adopterDao.findOrCreate(adopter);
            AdoptionRecord record = new AdoptionRecord(animal.getId(), persisted.getId(), LocalDate.now(),
                    animal.adoptionFee());
            recordDao.insert(record);
            
            AnimalStatus previous = animal.getStatus();
            animal.setStatus(AnimalStatus.ADOPTED);
            animalDao.updateStatus(animal.getId(), AnimalStatus.ADOPTED);
            history.record(animal.getId(), previous, AnimalStatus.ADOPTED);

            // Fix waitlist bug: poll next and delete only that specific entry from DB
            Optional<Adopter> nextInLine = waitlist.pollNext(animal.getId());
            if (nextInLine.isPresent()) {
                waitlistDao.delete(animal.getId(), nextInLine.get().getId());
            }

            database.commit();
            return record;
        } catch (SQLException | RuntimeException e) {
            database.rollback();
            throw e;
        }
    }

    /**
     * Records adoption interest for an adopter, appending them to the waitlist queue.
     */
    public int recordInterest(Animal animal, Adopter adopter) throws SQLException {
        try {
            database.beginTransaction();

            Adopter persisted = adopterDao.findOrCreate(adopter);
            int position = waitlist.addInterest(animal.getId(), persisted);
            waitlistDao.insert(animal.getId(), persisted.getId(), LocalDateTime.now(), position);

            if (animal.getStatus() != AnimalStatus.PENDING) {
                AnimalStatus previous = animal.getStatus();
                animal.setStatus(AnimalStatus.PENDING);
                animalDao.updateStatus(animal.getId(), AnimalStatus.PENDING);
                history.record(animal.getId(), previous, AnimalStatus.PENDING);
            }

            database.commit();
            return position;
        } catch (SQLException | RuntimeException e) {
            database.rollback();
            throw e;
        }
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
