package pawtrack.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Completed adoption joining an {@link Animal} with an {@link Adopter}.
 */
public final class AdoptionRecord {

    private int id;
    private final int animalId;
    private final int adopterId;
    private final LocalDate adoptionDate;
    private final double feePaid;

    public AdoptionRecord(int id, int animalId, int adopterId, LocalDate adoptionDate, double feePaid) {
        if (animalId <= 0) {
            throw new IllegalArgumentException("animalId must reference a persisted animal");
        }
        if (adopterId <= 0) {
            throw new IllegalArgumentException("adopterId must reference a persisted adopter");
        }
        if (adoptionDate == null) {
            throw new IllegalArgumentException("Adoption date is required");
        }
        if (feePaid < 0) {
            throw new IllegalArgumentException("Fee paid must not be negative");
        }
        this.id = id;
        this.animalId = animalId;
        this.adopterId = adopterId;
        this.adoptionDate = adoptionDate;
        this.feePaid = feePaid;
    }

    public AdoptionRecord(int animalId, int adopterId, LocalDate adoptionDate, double feePaid) {
        this(0, animalId, adopterId, adoptionDate, feePaid);
    }

    public void assignId(int generatedId) {
        if (this.id == 0 && generatedId > 0) {
            this.id = generatedId;
        }
    }

    public int getId() {
        return id;
    }

    public int getAnimalId() {
        return animalId;
    }

    public int getAdopterId() {
        return adopterId;
    }

    public LocalDate getAdoptionDate() {
        return adoptionDate;
    }

    public double getFeePaid() {
        return feePaid;
    }

    @Override
    public String toString() {
        return String.format("Adoption#%d animal=%d adopter=%d on %s ($%.2f)",
                id, animalId, adopterId, adoptionDate, feePaid);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AdoptionRecord)) {
            return false;
        }
        AdoptionRecord that = (AdoptionRecord) other;
        return id != 0 && id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
