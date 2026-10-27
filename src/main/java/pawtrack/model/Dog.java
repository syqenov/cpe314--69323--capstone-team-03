package pawtrack.model;


import java.time.LocalDate;

public class Dog extends Animal {
    public Dog(int id, String name, int ageMonths, LocalDate intakeDate,
               AnimalStatus status, int classificationId, String imageUrl) {
        super(id, name, ageMonths, intakeDate, status, classificationId, imageUrl);
    }

    @Override public Species species() { return Species.DOG; }

    @Override public double calculateAdoptionFee() {
        return getAgeMonths() < 12 ? 2000.0 : 1500.0;   // 1,500 plus 500 under 12 months
    }

    @Override public String getCareSchedule() { return "Walk 2x/day, vaccines"; }

    @Override protected int seniorThresholdMonths() { return 96; }
}