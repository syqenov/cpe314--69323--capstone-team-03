package pawtrack.model;

public enum Species {
    DOG("Dog"),
    CAT("Cat");

    private final String label;

    Species(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Species fromLabel(String value) {
        if (value == null) {
            return CAT;
        }
        String trimmed = value.trim();
        for (Species species : values()) {
            if (species.label.equalsIgnoreCase(trimmed) || species.name().equalsIgnoreCase(trimmed)) {
                return species;
            }
        }
        return CAT;
    }
}