package pawtrack.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {

    @Test
    void acceptsStrongPassword() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("Abcdef1!"));
    }

    @Test
    void rejectsTooShort() {
        assertThrows(PasswordPolicy.WeakPasswordException.class,
                () -> PasswordPolicy.validate("Ab1!"));
    }

    @Test
    void rejectsMissingNumber() {
        assertThrows(PasswordPolicy.WeakPasswordException.class,
                () -> PasswordPolicy.validate("Abcdefg!"));
    }

    @Test
    void rejectsMissingSymbol() {
        assertThrows(PasswordPolicy.WeakPasswordException.class,
                () -> PasswordPolicy.validate("Abcdefg1"));
    }

    @Test
    void rejectsMissingLetter() {
        assertThrows(PasswordPolicy.WeakPasswordException.class,
                () -> PasswordPolicy.validate("12345678!"));
    }

    @Test
    void rejectsNull() {
        assertThrows(PasswordPolicy.WeakPasswordException.class,
                () -> PasswordPolicy.validate(null));
    }
}