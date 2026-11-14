package pawtrack.dao;

import pawtrack.model.Adopter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC data access for {@link Adopter}.
 */
public final class AdopterDao {

    private final Connection connection;

    public AdopterDao(Database database) {
        this.connection = database.connection();
    }

    public int insert(Adopter adopter) throws SQLException {
        String sql = "INSERT INTO adopter (name, email, phone) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, adopter.getName());
            ps.setString(2, adopter.getEmail());
            ps.setString(3, adopter.getPhone());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    adopter.assignId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Failed to insert adopter: " + adopter.getName());
    }

    public Optional<Adopter> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM adopter WHERE lower(email) = lower(?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /** Looks up an adopter by name or email (used to link a login to their records). */
    public Optional<Adopter> findByNameOrEmail(String value) throws SQLException {
        String sql = "SELECT * FROM adopter WHERE lower(name) = lower(?) OR lower(email) = lower(?) LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setString(2, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Adopter> findById(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM adopter WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Adopter> findAll() throws SQLException {
        List<Adopter> adopters = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM adopter ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                adopters.add(map(rs));
            }
        }
        return adopters;
    }

    public int count() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM adopter")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Returns the existing adopter for the email, or inserts a new one. */
    public Adopter findOrCreate(Adopter adopter) throws SQLException {
        Optional<Adopter> existing = findByEmail(adopter.getEmail());
        if (existing.isPresent()) {
            return existing.get();
        }
        insert(adopter);
        return adopter;
    }

    private Adopter map(ResultSet rs) throws SQLException {
        return new Adopter(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("phone"));
    }
}
