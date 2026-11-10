package pawtrack.security;

import java.time.Instant;

/**
 * Handles login authentication with brute-force rate limiting.
 *
 * <p>After 3 failed attempts within a short window, the account is locked
 * for 60 seconds. Failed attempt counts reset on successful login.</p>
 */
public final class LoginManager {

    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long LOCKOUT_SECONDS = 60;

    private int failedAttempts = 0;
    private Instant lockoutUntil = null;

    /**
     * Attempts to log in with the given username and password.
     *
     * @return the user's Role if credentials are valid, or null if invalid
     * @throws LockedException if the account is currently locked out
     */
    public Credentials.Role login(String username, String password) throws LockedException {
        if (isLocked()) {
            throw new LockedException(remainingLockoutSeconds());
        }

        Credentials.UserCredentials creds = Credentials.get(username);
        if (creds != null && creds.password.equals(password)) {
            failedAttempts = 0;
            lockoutUntil = null;
            return creds.role;
        }

        failedAttempts++;
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            lockoutUntil = Instant.now().plusSeconds(LOCKOUT_SECONDS);
        }
        return null;
    }

    private boolean isLocked() {
        if (lockoutUntil == null) return false;
        if (Instant.now().isAfter(lockoutUntil)) {
            lockoutUntil = null;
            failedAttempts = 0;
            return false;
        }
        return true;
    }

    private long remainingLockoutSeconds() {
        if (lockoutUntil == null) return 0;
        return java.time.Duration.between(Instant.now(), lockoutUntil).getSeconds() + 1;
    }

    /** Thrown when the account is temporarily locked due to failed attempts. */
    public static final class LockedException extends Exception {
        private final long seconds;
        LockedException(long seconds) {
            super("Account locked. Try again in " + seconds + " second(s).");
            this.seconds = seconds;
        }
        public long getSeconds() { return seconds; }
    }

    public int getFailedAttempts() { return failedAttempts; }
}
