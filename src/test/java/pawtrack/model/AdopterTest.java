package pawtrack.model;

	import org.junit.jupiter.api.DisplayName;
	import org.junit.jupiter.api.Test;

	import static org.junit.jupiter.api.Assertions.assertEquals;
	import static org.junit.jupiter.api.Assertions.assertNotEquals;
	import static org.junit.jupiter.api.Assertions.assertThrows;

	/**
	 * Identity and validation rules for {@link Adopter}.
	 */
	class AdopterTest {

	    @Test
	    @DisplayName("Email is the natural key and is case-insensitive")
	    void emailIdentity() {
	        Adopter first = new Adopter("Jane Doe", "jane@example.com", "555");
	        Adopter second = new Adopter("J. Doe", "JANE@example.com", "666");

	        assertEquals(first, second);
	        assertEquals(first.hashCode(), second.hashCode());
	    }

	    @Test
	    @DisplayName("Different emails are different adopters")
	    void differentEmails() {
	        Adopter first = new Adopter("Jane", "jane@example.com", "555");
	        Adopter second = new Adopter("Jane", "other@example.com", "555");
	        assertNotEquals(first, second);
	    }

	    @Test
	    @DisplayName("Blank name or email is rejected (edge cases)")
	    void rejectsBlankFields() {
	        assertThrows(IllegalArgumentException.class, () -> new Adopter("", "jane@example.com", ""));
	        assertThrows(IllegalArgumentException.class, () -> new Adopter("Jane", "  ", ""));
	    }
	}

