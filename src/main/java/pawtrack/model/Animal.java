package pawtrack.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Abstract base of the animal hierarchy.
 */
public abstract class Animal {

    public static final int JUVENILE_MAX_MONTHS = 6;

    private int id;
    private final String name;
    private final String breed;
    private final int ageMonths;
    private final LocalDate intakeDate;
    private final String source; // e.g., "stray", "surrender"
    private final LocalDate createdAt;
    private AnimalStatus status;
    private Integer classificationId;

    protected Animal(int id, String name, String breed, int ageMonths,
                     LocalDate intakeDate, String source, LocalDate createdAt, AnimalStatus status) {
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
        this.source = (source == null || source.isBlank()) ? "stray" : source.trim().toLowerCase();
        this.createdAt = createdAt == null ? LocalDate.now() : createdAt;
        this.status = status == null ? AnimalStatus.INTAKE : status;
    }

    protected Animal(int id, String name, String breed, int ageMonths,
                     LocalDate intakeDate, AnimalStatus status) {
        this(id, name, breed, ageMonths, intakeDate, "stray", LocalDate.now(), status);
    }

    public abstract Species species();

    public abstract double adoptionFee();

    public abstract String careSchedule();

    public abstract boolean isSenior();

    public boolean isJuvenile() {
        return ageMonths < JUVENILE_MAX_MONTHS;
    }

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

    public String getSource() {
        return source;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
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
        return String.format("#%d %s (%s, %s, %d mo) [%s - %s]",
                id, name, species().label(), breed, ageMonths, source, status.label());
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
