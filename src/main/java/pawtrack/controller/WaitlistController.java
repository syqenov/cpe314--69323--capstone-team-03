package pawtrack.controller;

import pawtrack.dao.AdopterDao;
import pawtrack.dao.AnimalDao;
import pawtrack.dao.WaitlistDao;
import pawtrack.model.Adopter;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.service.StatusHistoryService;
import pawtrack.service.WaitlistService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class WaitlistController {
    private final WaitlistService waitlist;
    private final WaitlistDao waitlistDao;
    private final AdopterDao adopterDao;
    private final AnimalDao animalDao;
    private final StatusHistoryService history;

    public WaitlistController(WaitlistService waitlist, WaitlistDao waitlistDao,
                              AdopterDao adopterDao, AnimalDao animalDao,
                              StatusHistoryService history) {
        this.waitlist = waitlist;
        this.waitlistDao = waitlistDao;
        this.adopterDao = adopterDao;
        this.animalDao = animalDao;
        this.history = history;
    }

    public int recordInterest(Animal animal, Adopter adopter) throws SQLException {
        Adopter persisted = adopterDao.findOrCreate(adopter);
        int position = waitlist.addInterest(animal.getId(), persisted);
        waitlistDao.insert(animal.getId(), persisted.getId(), LocalDateTime.now(), position);
        if (animal.getStatus() != AnimalStatus.PENDING) {
            AnimalStatus previous = animal.getStatus();
            animal.setStatus(AnimalStatus.PENDING);
            animalDao.updateStatus(animal.getId(), AnimalStatus.PENDING);
            history.record(animal.getId(), previous, AnimalStatus.PENDING);
        }
        return position;
    }

    public Optional<Adopter> pollNext(int animalId) throws SQLException {
        Optional<Adopter> next = waitlist.pollNext(animalId);
        waitlistDao.deleteByAnimal(animalId);
        return next;
    }

    public WaitlistService getWaitlist() {
        return waitlist;
    }

    public void loadFromDb() throws SQLException {
        for (WaitlistDao.Entry entry : waitlistDao.loadAll()) {
            adopterDao.findById(entry.adopterId())
                    .ifPresent(adopter -> waitlist.addInterest(entry.animalId(), adopter));
        }
    }

    /** A waitlist row enriched with the animal and adopter objects. */
    public record WaitlistRequest(Animal animal, Adopter adopter, LocalDateTime requestedAt, int position) {
    }

    /** All waitlist requests (staff view), enriched with animal + adopter. */
    public List<WaitlistRequest> getRequests() throws SQLException {
        List<WaitlistRequest> requests = new ArrayList<>();
        for (WaitlistDao.Entry entry : waitlistDao.loadAll()) {
            Optional<Animal> animal = animalDao.findById(entry.animalId());
            Optional<Adopter> adopter = adopterDao.findById(entry.adopterId());
            if (animal.isPresent() && adopter.isPresent()) {
                requests.add(new WaitlistRequest(animal.get(), adopter.get(),
                        entry.requestedAt(), entry.position()));
            }
        }
        return requests;
    }

    /** Waitlist requests belonging to a specific adopter (adopter view). */
    public List<WaitlistRequest> findByAdopterId(int adopterId) throws SQLException {
        List<WaitlistRequest> requests = new ArrayList<>();
        for (WaitlistDao.Entry entry : waitlistDao.findByAdopterId(adopterId)) {
            Optional<Animal> animal = animalDao.findById(entry.animalId());
            Optional<Adopter> adopter = adopterDao.findById(entry.adopterId());
            if (animal.isPresent() && adopter.isPresent()) {
                requests.add(new WaitlistRequest(animal.get(), adopter.get(),
                        entry.requestedAt(), entry.position()));
            }
        }
        return requests;
    }
}
