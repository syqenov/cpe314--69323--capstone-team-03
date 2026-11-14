package pawtrack.service;

/**
 * Enforces password strength rules for new accounts:
 * minimum length, at least one letter, one number, and one symbol.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;

    private PasswordPolicy() {
    }

    /**
     * Validates a raw password against the policy.
     *
     * @throws WeakPasswordException with a user-friendly message if the password is too weak
     */
    public static void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new WeakPasswordException("Password must be at least " + MIN_LENGTH + " characters long.");
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        boolean hasSymbol = false;
        for (int i = 0; i < password.length(); i++) {
            char ch = password.charAt(i);
            if (Character.isLetter(ch)) {
                hasLetter = true;
            } else if (Character.isDigit(ch)) {
                hasDigit = true;
            } else if (!Character.isWhitespace(ch)) {
                hasSymbol = true;
            }
        }
        if (!hasLetter) {
            throw new WeakPasswordException("Password must contain at least one letter.");
        }
        if (!hasDigit) {
            throw new WeakPasswordException("Password must contain at least one number.");
        }
        if (!hasSymbol) {
            throw new WeakPasswordException("Password must contain at least one symbol (e.g. ! @ # $).");
        }
    }

    /** Thrown when a password does not meet the strength requirements. */
    public static final class WeakPasswordException extends RuntimeException {
        public WeakPasswordException(String message) {
            super(message);
        }
    }
}