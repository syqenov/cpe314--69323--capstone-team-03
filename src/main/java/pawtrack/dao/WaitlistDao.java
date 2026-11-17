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
    private final Database database;

    public WaitlistDao(Database database) {
        this.database = java.util.Objects.requireNonNull(database, "database");
        this.connection = database.connection();
    }

    public int insert(int animalId, int adopterId, LocalDateTime requestedAt, int position) throws SQLException {
        String sql = "INSERT INTO waitlist_entry (animal_id, adopter_id, requested_at, position) "
                + "VALUES (?, ?, ?, ?) ON CONFLICT(animal_id, adopter_id) DO NOTHING";
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

    /**
     * Removes exactly one queue entry and shifts later positions down. Both SQL
     * changes are committed together, so a failed update cannot leave a gap.
     * The caller must update its in-memory FIFO only after this succeeds.
     */
    public boolean removeAndShift(int animalId, int adopterId, int position) throws SQLException {
        // Multi-statement FIFO edits share the same lock as adoption and CSV import.
        synchronized (database) {
            return removeAndShiftUnderDatabaseLock(animalId, adopterId, position);
        }
    }

    private boolean removeAndShiftUnderDatabaseLock(int animalId, int adopterId, int position)
            throws SQLException {
        if (!connection.getAutoCommit()) {
            throw new SQLException("Waitlist update requires an available database transaction");
        }
        connection.setAutoCommit(false);
        try {
            int removed = delete(animalId, adopterId);
            if (removed == 0) {
                connection.rollback();
                return false;
            }
            String sql = "UPDATE waitlist_entry SET position = position - 1 "
                    + "WHERE animal_id = ? AND position > ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, animalId);
                ps.setInt(2, position);
                ps.executeUpdate();
            }
            connection.commit();
            return true;
        } catch (SQLException | RuntimeException failure) {
            connection.rollback();
            throw failure;
        } finally {
            connection.setAutoCommit(true);
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
