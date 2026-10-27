package pawtrack.model;


import java.time.LocalDate;

public abstract class Animal {
    private int id;
    private final String name;
    private final int ageMonths;
    private final LocalDate intakeDate;
    private AnimalStatus status;
    private final int classificationId;
    private final String imageUrl;

    protected Animal(int id, String name, int ageMonths, LocalDate intakeDate,
                     AnimalStatus status, int classificationId, String imageUrl) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name is required");
        if (ageMonths < 0) throw new IllegalArgumentException("Age must be 0 or more months");
        if (intakeDate == null) throw new IllegalArgumentException("Intake date is required");
        this.id = id;
        this.name = name.trim();
        this.ageMonths = ageMonths;
        this.intakeDate = intakeDate;
        this.status = (status == null) ? AnimalStatus.AVAILABLE : status;
        this.classificationId = classificationId;
        this.imageUrl = imageUrl;
    }

    public abstract Species species();
    public abstract double calculateAdoptionFee();
    public abstract String getCareSchedule();
    protected abstract int seniorThresholdMonths();

    public boolean isSenior() { return ageMonths >= seniorThresholdMonths(); }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public int getAgeMonths() { return ageMonths; }
    public LocalDate getIntakeDate() { return intakeDate; }
    public AnimalStatus getStatus() { return status; }
    public void setStatus(AnimalStatus status) { this.status = status; }
    public int getClassificationId() { return classificationId; }
    public String getImageUrl() { return imageUrl; }
}