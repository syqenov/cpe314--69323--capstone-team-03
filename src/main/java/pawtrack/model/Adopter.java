package pawtrack.model;

import java.util.Locale;
import java.util.Objects;

/**
 * Person who may express interest in an animal and/or complete an adoption.
 * The email is the natural key used to de-duplicate waitlist entries.
 */
public final class Adopter {

    private int id;
    private final String name;
    private final String email;
    private final String phone;

    public Adopter(int id, String name, String email, String phone) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Adopter name must not be blank");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Adopter email must not be blank");
        }
        this.id = id;
        this.name = name.trim();
        this.email = email.trim();
        this.phone = phone == null ? "" : phone.trim();
    }

    public Adopter(String name, String email, String phone) {
        this(0, name, email, phone);
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

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    @Override
    public String toString() {
        return name + " <" + email + ">";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Adopter)) {
            return false;
        }
        Adopter adopter = (Adopter) other;
        return email.toLowerCase(Locale.ROOT).equals(adopter.email.toLowerCase(Locale.ROOT));
    }

    @Override
    public int hashCode() {
        return Objects.hash(email.toLowerCase(Locale.ROOT));
    }
}
