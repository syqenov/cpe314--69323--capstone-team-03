package pawtrack.controller;

import pawtrack.dao.Database;
import pawtrack.dao.UserDao;
import pawtrack.model.Role;
import pawtrack.model.User;
import pawtrack.service.PasswordPolicy;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthControllerTest {

    @Test
    void successfulLoginReturnsUser() throws SQLException {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            Optional<User> user = auth.login("admin", "admin123");
            assertTrue(user.isPresent());
            assertEquals("admin", user.get().getUsername());
            assertEquals(Role.STAFF, user.get().getRole());
            assertTrue(auth.isStaff());
        }
    }

    @Test
    void wrongPasswordFails() throws SQLException {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            Optional<User> user = auth.login("admin", "wrong-password");
            assertTrue(user.isEmpty(), "Wrong password must not authenticate");
            assertNull(auth.getCurrentUser(), "Session must stay empty on failure");
        }
    }

    @Test
    void unknownUserFails() throws SQLException {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            Optional<User> user = auth.login("nobody", "secret");
            assertTrue(user.isEmpty(), "Unknown user must not authenticate");
        }
    }

    @Test
    void adopterCannotPerformStaffAction() throws SQLException {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            assertTrue(auth.login("user", "user123").isPresent());
            assertTrue(auth.isAdopter());
            assertFalse(auth.isStaff());
            assertThrows(UnauthorizedException.class, auth::requireStaff,
                    "ADOPTER must be blocked from staff-only actions");
        }
    }

    @Test
    void staffCanPerformStaffAction() throws SQLException {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            assertTrue(auth.login("admin", "admin123").isPresent());
            assertDoesNotThrow(auth::requireStaff);
        }
    }

    @Test
    void createAccountSucceedsWithStrongPassword() throws SQLException {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            User created = auth.createAccount("newcomer", "Abcdef1!");
            assertEquals("newcomer", created.getUsername());
            assertEquals(Role.ADOPTER, created.getRole());
            assertTrue(auth.login("newcomer", "Abcdef1!").isPresent(),
                    "New account must be able to log in");
        }
    }

    @Test
    void createAccountRejectsDuplicateUsername() throws SQLException {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            auth.createAccount("dupe", "Abcdef1!");
            assertThrows(IllegalArgumentException.class,
                    () -> auth.createAccount("dupe", "Abcdef1!"));
        }
    }

    @Test
    void createAccountRejectsWeakPassword() throws SQLException {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            assertThrows(PasswordPolicy.WeakPasswordException.class,
                    () -> auth.createAccount("weakuser", "password"));
        }
    }
}