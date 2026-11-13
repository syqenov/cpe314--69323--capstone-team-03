package pawtrack.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistence for the per-animal adoption-interest waitlist. The in-memory
 * Queue lives in {@link pawtrack.service.WaitlistService}; this DAO mirrors
 * it to disk so interests survive a restart.
 */
public final class WaitlistDao {

    /** Persisted waitlist row. */
    public record Entry(int id, int animalId, int adopterId, LocalDateTime requestedAt, int position) {
    }

    private final Connection connection;

    public WaitlistDao(Database database) {
        this.connection = database.connection();
    }

    public int insert(int animalId, int adopterId, LocalDateTime requestedAt, int position) throws SQLException {
        String sql = "INSERT OR REPLACE INTO waitlist_entry (animal_id, adopter_id, requested_at, position) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, animalId);
            ps.setInt(2, adopterId);
            ps.setString(3, requestedAt.toString());
            ps.setInt(4, position);
            return ps.executeUpdate();
        }
    }

    public int delete(int animalId, int adopterId) throws SQLException {
        String sql = "DELETE FROM waitlist_entry WHERE animal_id = ? AND adopter_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, animalId);
            ps.setInt(2, adopterId);
            return ps.executeUpdate();
        }
    }

    public int deleteByAnimal(int animalId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM waitlist_entry WHERE animal_id = ?")) {
            ps.setInt(1, animalId);
            return ps.executeUpdate();
        }
    }

    public List<Entry> loadAll() throws SQLException {
        String sql = "SELECT * FROM waitlist_entry ORDER BY animal_id, position";
        List<Entry> entries = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                entries.add(new Entry(
                        rs.getInt("id"),
                        rs.getInt("animal_id"),
                        rs.getInt("adopter_id"),
                        LocalDateTime.parse(rs.getString("requested_at")),
                        rs.getInt("position")));
            }
        }
        return entries;
    }

    /** Waitlist entries that belong to a specific adopter. */
    public List<Entry> findByAdopterId(int adopterId) throws SQLException {
        String sql = "SELECT * FROM waitlist_entry WHERE adopter_id = ? ORDER BY requested_at";
        List<Entry> entries = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, adopterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new Entry(
                            rs.getInt("id"),
                            rs.getInt("animal_id"),
                            rs.getInt("adopter_id"),
                            LocalDateTime.parse(rs.getString("requested_at")),
                            rs.getInt("position")));
                }
            }
        }
        return entries;
    }
}
