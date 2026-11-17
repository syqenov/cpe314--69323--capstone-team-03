package pawtrack.service;

import pawtrack.model.Adopter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Adoption-interest waitlist.
 *
 * <p>Each animal owns a <strong>FIFO Queue</strong> ({@link ArrayDeque} used as a
 * {@link Deque}) of interested adopters, so the earliest interest is served
 * first. The email address is the natural key, therefore duplicate interests are
 * ignored and the existing queue position is returned.</p>
 */
public final class WaitlistService {

    // A dashboard SwingWorker may read totals while the EDT changes the FIFO.
    // Serialize every public queue operation; collection snapshots are defensive copies.
    private final Map<Integer, Deque<Adopter>> waitlists = new HashMap<>();

    public synchronized int size(int animalId) {
        Deque<Adopter> queue = waitlists.get(animalId);
        return queue == null ? 0 : queue.size();
    }

    public synchronized boolean hasAnyone(int animalId) {
        return size(animalId) > 0;
    }

    /**
     * Appends an interest to the animal's queue.
     *
     * @return the 1-based queue position of the adopter
     */
    public synchronized int addInterest(int animalId, Adopter adopter) {
        if (adopter == null) {
            throw new IllegalArgumentException("adopter must not be null");
        }
        Deque<Adopter> queue = waitlists.computeIfAbsent(animalId, key -> new ArrayDeque<>());
        int existing = indexOf(queue, adopter);
        if (existing >= 0) {
            return existing + 1;
        }
        queue.addLast(adopter);
        return queue.size();
    }

    /** Peeks the next adopter in line without removing them. */
    public synchronized Optional<Adopter> peekNext(int animalId) {
        Deque<Adopter> queue = waitlists.get(animalId);
        if (queue == null || queue.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(queue.peekFirst());
    }

    /** Removes and returns the next adopter in line (FIFO). */
    public synchronized Optional<Adopter> pollNext(int animalId) {
        Deque<Adopter> queue = waitlists.get(animalId);
        if (queue == null || queue.isEmpty()) {
            return Optional.empty();
        }
        Adopter next = queue.pollFirst();
        if (queue.isEmpty()) {
            waitlists.remove(animalId);
        }
        return Optional.of(next);
    }

    public synchronized List<Adopter> queueFor(int animalId) {
        Deque<Adopter> queue = waitlists.get(animalId);
        return queue == null ? new ArrayList<>() : new ArrayList<>(queue);
    }

    public synchronized Set<Integer> animalIds() {
        return new HashSet<>(waitlists.keySet());
    }

    public synchronized int totalWaiters() {
        int total = 0;
        for (Deque<Adopter> queue : waitlists.values()) {
            total += queue.size();
        }
        return total;
    }

    /** Removes a specific adopter without changing the order of other applicants. */
    public synchronized boolean removeInterest(int animalId, int adopterId) {
        Deque<Adopter> queue = waitlists.get(animalId);
        if (queue == null) return false;
        boolean removed = queue.removeIf(a -> a.getId() == adopterId);
        if (queue.isEmpty()) waitlists.remove(animalId);
        return removed;
    }

    public synchronized void clear(int animalId) {
        waitlists.remove(animalId);
    }

    private int indexOf(Deque<Adopter> queue, Adopter adopter) {
        int index = 0;
        for (Adopter queued : queue) {
            if ((queued.getId() > 0 && adopter.getId() > 0 && queued.getId() == adopter.getId())
                    || queued.getEmail().equalsIgnoreCase(adopter.getEmail())) {
                return index;
            }
            index++;
        }
        return -1;
    }
}
