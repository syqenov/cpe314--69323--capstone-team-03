package pawtrack.model;

/**
 * Lifecycle of an animal inside the shelter. Transitions are tracked by the
 * undo {@link pawtrack.service.StatusHistoryService}.
 */
public enum AnimalStatus {
    INTAKE("Intake"),
    AVAILABLE("Available"),
    PENDING("Pending Interest"),
    ADOPTED("Adopted"),
    RETURNED("Returned");

    private final String label;

    AnimalStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static AnimalStatus fromLabel(String value) {
        if (value == null) {
            return INTAKE;
        }
        String trimmed = value.trim();
        for (AnimalStatus status : values()) {
            if (status.label.equalsIgnoreCase(trimmed) || status.name().equalsIgnoreCase(trimmed)) {
                return status;
            }
        }
        return INTAKE;
    }
}
