package pawtrack.model;

import java.time.LocalDateTime;

/**
 * Immutable record of a single status transition. Instances are pushed onto the
 * undo {@link java.util.Stack} (actually an {@link java.util.ArrayDeque}).
 *
 * @param animalId the affected animal
 * @param from     status before the change
 * @param to       status after the change
 * @param at       when the change happened
 */
public record StatusChange(int animalId, AnimalStatus from, AnimalStatus to, LocalDateTime at) {

    public StatusChange {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Status change requires both from and to states");
        }
        if (at == null) {
            at = LocalDateTime.now();
        }
    }

    public static StatusChange of(int animalId, AnimalStatus from, AnimalStatus to) {
        return new StatusChange(animalId, from, to, LocalDateTime.now());
    }

    @Override
    public String toString() {
        return String.format("animal #%d: %s -> %s @ %s", animalId, from.label(), to.label(), at);
    }
}
