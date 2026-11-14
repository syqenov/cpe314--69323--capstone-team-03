package pawtrack.dao;

import pawtrack.model.Role;
import pawtrack.model.User;
import pawtrack.service.PasswordHasher;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserDaoTest {

    @Test
    void createAndFindByUsername() throws SQLException {
        try (Database db = Database.openInMemory()) {
            UserDao dao = new UserDao(db);
            String salt = PasswordHasher.generateSalt();
            String hash = PasswordHasher.hash("secret", salt);
            User user = new User(null, "alice", hash, salt, Role.STAFF, null);
            int id = dao.create(user);
            assertTrue(id > 0);

            Optional<User> found = dao.findByUsername("alice");
            assertTrue(found.isPresent());
            assertEquals("alice", found.get().getUsername());
            assertEquals(Role.STAFF, found.get().getRole());
        }
    }

    @Test
    void findUnknownUserReturnsEmpty() throws SQLException {
        try (Database db = Database.openInMemory()) {
            UserDao dao = new UserDao(db);
            assertTrue(dao.findByUsername("nobody").isEmpty());
        }
    }
}
