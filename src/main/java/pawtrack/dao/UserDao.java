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
