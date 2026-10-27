package pawtrack.model;


import java.time.LocalDate;

public class Cat extends Animal {
    public Cat(int id, String name, int ageMonths, LocalDate intakeDate,
               AnimalStatus status, int classificationId, String imageUrl) {
        super(id, name, ageMonths, intakeDate, status, classificationId, imageUrl);
    }

    @Override public Species species() { return Species.CAT; }

    @Override public double calculateAdoptionFee() {
        return isSenior() ? 500.0 : 1000.0;             // half price from 84 months
    }

    @Override public String getCareSchedule() { return "Litter daily, vaccines"; }

    @Override protected int seniorThresholdMonths() { return 84; }
}