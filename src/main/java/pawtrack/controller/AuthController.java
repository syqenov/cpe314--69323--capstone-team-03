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
                    currentUser = user;
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Login failed", e);
        }
        currentUser = null;
        return Optional.empty();
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
        PasswordPolicy.validate(password);
        try {
            if (userDao.findByUsername(username).isPresent()) {
                throw new IllegalArgumentException("Username already exists.");
            }
            String salt = PasswordHasher.generateSalt();
            String hash = PasswordHasher.hash(password, salt);
            User user = new User(null, username.trim(), hash, salt, Role.ADOPTER, null);
            int id = userDao.create(user);
            LOGGER.info("Created new adopter account: " + user.getUsername());
            return new User(id, user.getUsername(), hash, salt, Role.ADOPTER, null);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Account creation failed", e);
            throw new IllegalStateException("Could not create account", e);
        }
    }
}
