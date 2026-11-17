package pawtrack.controller;

import pawtrack.dao.AdopterDao;
import pawtrack.dao.AnimalDao;
import pawtrack.dao.WaitlistDao;
import pawtrack.dao.ReservationDao;
import pawtrack.dao.Database;
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
    private final AuthController auth;
    private final ReservationDao reservationDao;

    public WaitlistController(WaitlistService waitlist, WaitlistDao waitlistDao,
                              AdopterDao adopterDao, AnimalDao animalDao,
                              StatusHistoryService history) {
        this(waitlist, waitlistDao, adopterDao, animalDao, history, null);
    }

    public WaitlistController(WaitlistService waitlist, WaitlistDao waitlistDao,
                              AdopterDao adopterDao, AnimalDao animalDao,
                              StatusHistoryService history, AuthController auth) {
        this(waitlist, waitlistDao, adopterDao, animalDao, history, auth, null);
    }

    public WaitlistController(WaitlistService waitlist, WaitlistDao waitlistDao,
                              AdopterDao adopterDao, AnimalDao animalDao,
                              StatusHistoryService history, AuthController auth,
                              ReservationDao reservationDao) {
        this.auth = auth;
        this.reservationDao = reservationDao;
        this.waitlist = waitlist;
        this.waitlistDao = waitlistDao;
        this.adopterDao = adopterDao;
        this.animalDao = animalDao;
        this.history = history;
    }

    public int recordInterest(Animal animal, Adopter adopter) throws SQLException {
        if (auth != null && auth.getCurrentUser() == null) {
            throw new UnauthorizedException("Login required");
        }
        Adopter persisted;
        if (auth != null && auth.isAdopter()) {
            // The authenticated adopter ID is the only identity; name/email on the argument are ignored.
            Integer ownId = auth.getCurrentUser().getAdopterId();
            if (ownId == null || adopter == null || adopter.getId() != ownId) {
                throw new UnauthorizedException("You can only register your own interest");
            }
            persisted = adopterDao.findById(ownId)
                    .orElseThrow(() -> new UnauthorizedException("Your adopter profile was not found"));
        } else {
            persisted = adopterDao.findOrCreate(adopter);
        }
        int existingPosition = waitlist.queueFor(animal.getId()).indexOf(persisted);
        if (existingPosition >= 0) {
            return existingPosition + 1;
        }
        int position = waitlist.size(animal.getId()) + 1;
        int inserted = waitlistDao.insert(animal.getId(), persisted.getId(), LocalDateTime.now(), position);
        if (inserted == 0) {
            throw new SQLException("This adopter already has a saved waitlist entry. Reload the waitlist.");
        }
        waitlist.addInterest(animal.getId(), persisted);
        if (animal.getStatus() != AnimalStatus.PENDING) {
            AnimalStatus previous = animal.getStatus();
            animal.setStatus(AnimalStatus.PENDING);
            animalDao.updateStatus(animal.getId(), AnimalStatus.PENDING);
            history.record(animal.getId(), previous, AnimalStatus.PENDING);
        }
        return position;
    }

    /** Uses the reservation dependency supplied by the application composition root. */
    public void withdrawMyInterest(int animalId) throws SQLException {
        if (reservationDao == null) {
            throw new IllegalStateException("Reservation dependency is not configured");
        }
        withdrawMyInterest(animalId, reservationDao);
    }

    /** Adopters may withdraw only their own pending requests, not active reservations. */
    public void withdrawMyInterest(int animalId, ReservationDao reservationDao) throws SQLException {
        if (auth == null || !auth.isAdopter() || auth.getCurrentUser().getAdopterId() == null)
            throw new UnauthorizedException("Adopter login required");
        int adopterId = auth.getCurrentUser().getAdopterId();
        var active = reservationDao.activeForAnimal(animalId);
        if (active.isPresent() && active.get().adopterId() == adopterId
                && active.get().expiresAt().isAfter(java.time.LocalDateTime.now()))
            throw new IllegalStateException("Contact shelter staff to cancel an active reservation");
        WaitlistDao.Entry own = waitlistDao.findByAdopterId(adopterId).stream()
                .filter(e -> e.animalId() == animalId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No pending request for this animal"));
        if (!waitlistDao.removeAndShift(animalId, adopterId, own.position()))
            throw new SQLException("The waitlist changed; refresh and try again");
        waitlist.removeInterest(animalId, adopterId);
    }

    public Optional<Adopter> pollNext(int animalId) throws SQLException {
        if (auth != null) auth.requireStaff();
        Optional<Adopter> next = waitlist.peekNext(animalId);
        if (next.isEmpty()) {
            return Optional.empty();
        }
        boolean deleted = waitlistDao.removeAndShift(animalId, next.get().getId(), 1);
        if (!deleted) {
            throw new SQLException("Waitlist is out of sync with the database. Reload it.");
        }
        return waitlist.pollNext(animalId);
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
        if (auth != null) {
            auth.requireStaff();
        }
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
        if (auth != null) {
            auth.requireAdopterOwnership(adopterId);
        }
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
