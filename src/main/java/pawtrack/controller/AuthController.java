package pawtrack.controller;

import pawtrack.dao.UserDao;
import pawtrack.model.Role;
import pawtrack.model.User;
import pawtrack.service.PasswordHasher;
import pawtrack.service.PasswordPolicy;

import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles authentication and session state.
 */
public final class AuthController {
    private static final Logger LOGGER = Logger.getLogger(AuthController.class.getName());
    private final UserDao userDao;
    private User currentUser;

    public AuthController(UserDao userDao) {
        this.userDao = userDao;
    }

    public Optional<User> login(String username, String password) {
        try {
            Optional<User> found = userDao.findByUsername(username);
            if (found.isPresent()) {
                User user = found.get();
                if (PasswordHasher.verify(password, user.getPasswordSalt(), user.getPasswordHash())) {
                    User linked = userDao.ensureLinkedAdopter(user);
                    currentUser = linked;
                    return Optional.of(linked);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Login failed", e);
        }
        currentUser = null;
        return Optional.empty();
    }

    /** Password changes require the existing password, even for an active session. */
    public void changeMyPassword(String oldPassword, String newPassword) throws SQLException {
        User account = currentUser;
        if (account == null || account.getId() == null) throw new UnauthorizedException("Login required");
        if (!PasswordHasher.verify(oldPassword, account.getPasswordSalt(), account.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        PasswordPolicy.validate(newPassword);
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(newPassword, salt);
        userDao.updatePassword(account.getId(), hash, salt);
        currentUser = new User(account.getId(), account.getUsername(), hash, salt,
                account.getRole(), account.getAdopterId());
    }

    public void logout() {
        currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isStaff() {
        return currentUser != null && currentUser.getRole() == Role.STAFF;
    }

    public boolean isAdopter() {
        return currentUser != null && currentUser.getRole() == Role.ADOPTER;
    }

    /** Blocks an adopter from reading or modifying someone else's records. */
    public void requireAdopterOwnership(int adopterId) {
        if (currentUser == null) {
            throw new UnauthorizedException("Login required");
        }
        if (currentUser.getRole() == Role.STAFF) {
            return;
        }
        if (currentUser.getAdopterId() == null
                || currentUser.getAdopterId() != adopterId) {
            throw new UnauthorizedException("You can only access your own records");
        }
    }

    public void requireStaff() {
        if (!isStaff()) {
            throw new UnauthorizedException("Staff access required");
        }
    }

    /**
     * Creates a new ADOPTER account after enforcing the password policy and
     * rejecting duplicate usernames.
     *
     * @throws IllegalArgumentException if the username is blank or already taken
     * @throws PasswordPolicy.WeakPasswordException if the password is too weak
     */
    public User createAccount(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }
        final String cleanName = username.trim();
        PasswordPolicy.validate(password);
        try {
            if (userDao.existsIgnoreCase(cleanName)) {
                throw new IllegalArgumentException("Username already exists.");
            }
            String salt = PasswordHasher.generateSalt();
            String hash = PasswordHasher.hash(password, salt);
            User user = new User(null, cleanName, hash, salt, Role.ADOPTER, null);
            User created = userDao.createLinkedAdopter(user);
            LOGGER.info("Created new adopter account: " + user.getUsername());
            return created;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint")) {
                throw new IllegalArgumentException("Username already exists.", e);
            }
            LOGGER.log(Level.SEVERE, "Account creation failed", e);
            throw new IllegalStateException("Could not create account", e);
        }
    }
}
