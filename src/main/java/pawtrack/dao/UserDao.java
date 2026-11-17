package pawtrack.dao;

import pawtrack.model.Role;
import pawtrack.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * DAO for application users.
 */
public final class UserDao {
    private final Connection connection;

    public UserDao(Database database) {
        this.connection = database.connection();
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    /** Case-insensitive, whitespace-trimmed username check used to block duplicate accounts. */
    public boolean existsIgnoreCase(String username) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM users WHERE lower(username) = lower(?)")) {
            ps.setString(1, username == null ? "" : username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Adopter accounts whose adopter_id is missing or does not reference an adopter row. */
    public int countInvalidAdopterLinks() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users u WHERE u.role = 'ADOPTER' AND "
                + "(u.adopter_id IS NULL OR NOT EXISTS (SELECT 1 FROM adopter a WHERE a.id = u.adopter_id))";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int create(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, password_salt, role, adopter_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getPasswordSalt());
            ps.setString(4, user.getRole().name());
            if (user.getAdopterId() == null) {
                ps.setNull(5, java.sql.Types.INTEGER);
            } else {
                ps.setInt(5, user.getAdopterId());
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to create user");
    }

    /**
     * Creates both records atomically. The synthetic address is an internal
     * placeholder, not a verified contact email. A future profile form must
     * explicitly collect and validate an actual address before notifications.
     */
    public User createLinkedAdopter(User user) throws SQLException {
        if (user.getRole() != Role.ADOPTER) {
            throw new IllegalArgumentException("Only adopter accounts can be linked");
        }
        if (!connection.getAutoCommit()) {
            throw new SQLException("Cannot create account during an active transaction");
        }
        connection.setAutoCommit(false);
        try {
            int id = create(user);
            int adopterId = insertAccountAdopter(id, user.getUsername());
            linkAdopter(id, adopterId);
            connection.commit();
            return new User(id, user.getUsername(), user.getPasswordHash(),
                    user.getPasswordSalt(), Role.ADOPTER, adopterId);
        } catch (SQLException | RuntimeException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /** Repairs legacy adopter accounts without claiming records by matching text. */
    public User ensureLinkedAdopter(User user) throws SQLException {
        if (user.getRole() != Role.ADOPTER || user.getAdopterId() != null) {
            return user;
        }
        if (user.getId() == null || !connection.getAutoCommit()) {
            throw new SQLException("Cannot link adopter during an active transaction");
        }
        connection.setAutoCommit(false);
        try {
            // Re-read under transaction to avoid overwriting an existing link.
            Integer adopterId = null;
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT adopter_id FROM users WHERE id = ?")) {
                ps.setInt(1, user.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Account does not exist");
                    }
                    adopterId = rs.getObject(1) == null ? null : rs.getInt(1);
                }
            }
            if (adopterId == null) {
                adopterId = insertAccountAdopter(user.getId(), user.getUsername());
                linkAdopter(user.getId(), adopterId);
            }
            connection.commit();
            return new User(user.getId(), user.getUsername(), user.getPasswordHash(),
                    user.getPasswordSalt(), user.getRole(), adopterId);
        } catch (SQLException | RuntimeException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private int insertAccountAdopter(int userId, String username) throws SQLException {
        // The account ID prevents clashes and protects preexisting shelter contacts.
        String email = "account-" + userId + "@pawtrack.invalid";
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO adopter (name, email, phone, created_at) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, "");
            ps.setString(4, java.time.LocalDate.now().toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Could not create linked adopter profile");
    }

    private void linkAdopter(int userId, int adopterId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE users SET adopter_id = ? WHERE id = ? AND adopter_id IS NULL")) {
            ps.setInt(1, adopterId);
            ps.setInt(2, userId);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Could not attach adopter profile to account");
            }
        }
    }

    public void updatePassword(int userId, String hash, String salt) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE users SET password_hash = ?, password_salt = ? WHERE id = ?")) {
            ps.setString(1, hash);
            ps.setString(2, salt);
            ps.setInt(3, userId);
            if (ps.executeUpdate() != 1) throw new SQLException("Account not found");
        }
    }

    public int count() throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private User map(ResultSet rs) throws SQLException {
        Integer adopterId = rs.getObject("adopter_id") == null ? null : rs.getInt("adopter_id");
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("password_salt"),
                Role.valueOf(rs.getString("role")),
                adopterId);
    }
}
