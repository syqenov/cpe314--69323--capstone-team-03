package pawtrack.model;

import java.time.LocalDate;

/**
 * Concrete {@link Animal} implementation for dogs.
 *
 * <p>Fee rules: puppies cost more (vaccination/sterilisation overhead), adult
 * dogs are the standard fee and senior dogs receive a reduced "senior adoption"
 * fee.</p>
 */
public final class Dog extends Animal {

    public static final double PUPPY_FEE = 300.0;
    public static final double STANDARD_FEE = 250.0;
    public static final double SENIOR_FEE = 100.0;
    public static final int SENIOR_AGE_MONTHS = 84; // 7 years

    public Dog(int id, String name, String breed, int ageMonths,
               LocalDate intakeDate, AnimalStatus status) {
        super(id, name, breed, ageMonths, intakeDate, status);
    }

    public Dog(String name, String breed, int ageMonths, LocalDate intakeDate) {
        this(0, name, breed, ageMonths, intakeDate, AnimalStatus.INTAKE);
    }

    @Override
    public Species species() {
        return Species.DOG;
    }

    @Override
    public boolean isSenior() {
        return getAgeMonths() >= SENIOR_AGE_MONTHS;
    }

    @Override
    public double adoptionFee() {
        if (isJuvenile()) {
            return PUPPY_FEE;
        }
        if (isSenior()) {
            return SENIOR_FEE;
        }
        return STANDARD_FEE;
    }

    @Override
    public String careSchedule() {
        return "2 walks/day, bath every 4 weeks, monthly vet check, annual vaccinations, daily enrichment play";
    }
}
