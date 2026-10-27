package pawtrack.service;

import pawtrack.model.AnimalStatus;
import pawtrack.model.StatusChange;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Undo history for animal status changes, implemented as a <strong>Stack</strong>
 * ({@link ArrayDeque} used with push/pop = last-in-first-out).
 *
 * <p>"Undo last status change" pops the most recent {@link StatusChange} so the
 * caller can restore the animal to its previous state.</p>
 */
public final class StatusHistoryService {

    private static final int MAX_HISTORY = 500;

    private final Deque<StatusChange> undoStack = new ArrayDeque<>();

    public void record(int animalId, AnimalStatus from, AnimalStatus to) {
        record(StatusChange.of(animalId, from, to));
    }

    public void record(StatusChange change) {
        if (change == null) {
            return;
        }
        undoStack.push(change);
        while (undoStack.size() > MAX_HISTORY) {
            undoStack.removeLast();
        }
    }

    /** Pops the most recent change, or empty when there is nothing to undo. */
    public Optional<StatusChange> undo() {
        if (undoStack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(undoStack.pop());
    }

    public Optional<StatusChange> peekMostRecent() {
        return undoStack.isEmpty() ? Optional.empty() : Optional.of(undoStack.peek());
    }

    public int size() {
        return undoStack.size();
    }

    public boolean isEmpty() {
        return undoStack.isEmpty();
    }

    public void clear() {
        undoStack.clear();
    }

    /** @return an immutable snapshot, most recent first. */
    public List<StatusChange> snapshot() {
        return new ArrayList<>(undoStack);
    }
}