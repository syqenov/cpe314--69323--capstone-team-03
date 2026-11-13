package pawtrack.model;

import java.time.LocalDate;

/**
 * Concrete {@link Animal} implementation for cats.
 *
 * <p>Cats have a lower standard fee than dogs and reach senior status later
 * (10 years), which changes both the fee and the care schedule.</p>
 */
public final class Cat extends Animal {

    public static final double KITTEN_FEE = 200.0;
    public static final double STANDARD_FEE = 150.0;
    public static final double SENIOR_FEE = 75.0;
    public static final int SENIOR_AGE_MONTHS = 120; // 10 years

    public Cat(int id, String name, String breed, int ageMonths,
               LocalDate intakeDate, AnimalStatus status) {
        super(id, name, breed, ageMonths, intakeDate, status);
    }

    public Cat(String name, String breed, int ageMonths, LocalDate intakeDate) {
        this(0, name, breed, ageMonths, intakeDate, AnimalStatus.INTAKE);
    }

    @Override
    public Species species() {
        return Species.CAT;
    }

    @Override
    public boolean isSenior() {
        return getAgeMonths() >= SENIOR_AGE_MONTHS;
    }

    @Override
    public double adoptionFee() {
        if (isJuvenile()) {
            return KITTEN_FEE;
        }
        if (isSenior()) {
            return SENIOR_FEE;
        }
        return STANDARD_FEE;
    }

    @Override
    public String careSchedule() {
        return "Litter tray cleaned daily, 30 min indoor play/day, vet check every 6 months, annual vaccinations, monthly nail trim";
    }
}
