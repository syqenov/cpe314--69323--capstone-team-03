package pawtrack.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Abstract base of the animal hierarchy.
 *
 * <p>Every concrete species must define its own <em>adoption fee</em> and
 * <em>care schedule</em> rules, which is the core reason this class is abstract
 * rather than a single "god" class with a switch.</p>
 */
public abstract class Animal {

    /** Animals younger than this are treated as juveniles (puppy / kitten / etc.). */
    public static final int JUVENILE_MAX_MONTHS = 6;

    private int id;
    private final String name;
    private final String breed;
    private final int ageMonths;
    private final LocalDate intakeDate;
    private AnimalStatus status;
    private Integer classificationId;

    protected Animal(int id, String name, String breed, int ageMonths,
                     LocalDate intakeDate, AnimalStatus status) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Animal name must not be blank");
        }
        if (breed == null || breed.isBlank()) {
            throw new IllegalArgumentException("Animal breed must not be blank");
        }
        if (ageMonths < 0) {
            throw new IllegalArgumentException("Age in months must not be negative: " + ageMonths);
        }
        if (intakeDate == null) {
            throw new IllegalArgumentException("Intake date is required");
        }
        this.id = id;
        this.name = name.trim();
        this.breed = breed.trim();
        this.ageMonths = ageMonths;
        this.intakeDate = intakeDate;
        this.status = status == null ? AnimalStatus.INTAKE : status;
    }

    /** @return the species this concrete animal belongs to. */
    public abstract Species species();

    /** @return the adoption fee (in dollars) following this species' pricing rules. */
    public abstract double adoptionFee();

    /** @return a human readable care schedule for this species. */
    public abstract String careSchedule();

    /** @return true when the animal is past this species' senior threshold. */
    public abstract boolean isSenior();

    public boolean isJuvenile() {
        return ageMonths < JUVENILE_MAX_MONTHS;
    }

    /**
     * Factory used when reconstructing animals from a database row or CSV log,
     * so callers never need to know which concrete subclass to build.
     */
    public static Animal create(Species species, int id, String name, String breed,
                                int ageMonths, LocalDate intakeDate, AnimalStatus status) {
        Species resolved = species == null ? Species.CAT : species;
        switch (resolved) {
            case DOG:
                return new Dog(id, name, breed, ageMonths, intakeDate, status);
            default:
                return new Cat(id, name, breed, ageMonths, intakeDate, status);
        }
    }

    /** Assigns the database generated key exactly once. */
    public void assignId(int generatedId) {
        if (this.id == 0 && generatedId > 0) {
            this.id = generatedId;
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBreed() {
        return breed;
    }

    public int getAgeMonths() {
        return ageMonths;
    }

    public LocalDate getIntakeDate() {
        return intakeDate;
    }

    public AnimalStatus getStatus() {
        return status;
    }

    public void setStatus(AnimalStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public Integer getClassificationId() {
        return classificationId;
    }

    public void setClassificationId(Integer classificationId) {
        this.classificationId = classificationId;
    }

    @Override
    public String toString() {
        return String.format("#%d %s (%s, %s, %d mo) [%s]",
                id, name, species().label(), breed, ageMonths, status.label());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Animal)) {
            return false;
        }
        Animal animal = (Animal) other;
        return id != 0 && id == animal.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
