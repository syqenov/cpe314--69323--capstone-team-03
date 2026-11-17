package pawtrack.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/** Stores reservations separately from the FIFO waitlist, which remains intact. */
public final class ReservationDao {
    public record Reservation(int id, int animalId, int adopterId, LocalDateTime reservedAt,
                              LocalDateTime expiresAt, String status) { }

    private final Connection connection;

    public ReservationDao(Database database) {
        this.connection = database.connection();
    }

    /** Returns the earliest queued applicant, not a randomly selected row. */
    public Optional<Integer> firstQueuedAdopter(int animalId) throws SQLException {
        String sql = "SELECT adopter_id FROM waitlist_entry WHERE animal_id = ? "
                + "AND NOT EXISTS (SELECT 1 FROM adoption_reservation r "
                + "WHERE r.animal_id = waitlist_entry.animal_id "
                + "AND r.adopter_id = waitlist_entry.adopter_id "
                + "AND r.status IN ('EXPIRED','CANCELLED','COMPLETED')) "
                + "ORDER BY position ASC, requested_at ASC, id ASC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, animalId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getInt(1)) : Optional.empty();
            }
        }
    }

    public Optional<Reservation> activeForAnimal(int animalId) throws SQLException {
        String sql = "SELECT * FROM adoption_reservation WHERE animal_id = ? AND status = 'ACTIVE' LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, animalId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /** Expire overdue active reservations, preserving historical rows and queue entries. */
    public int expireDue(LocalDateTime now) throws SQLException {
        String sql = "UPDATE adoption_reservation SET status = 'EXPIRED' "
                + "WHERE status = 'ACTIVE' AND expires_at <= ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, now.toString());
            return ps.executeUpdate();
        }
    }

    public boolean cancelActive(int animalId) throws SQLException {
        String sql = "UPDATE adoption_reservation SET status = 'CANCELLED' "
                + "WHERE animal_id = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, animalId);
            return ps.executeUpdate() == 1;
        }
    }

    /** Reservation events belonging only to the specified adopter, newest first. */
    public record ReservationEvent(Reservation reservation, String animalName) { }

    public java.util.List<ReservationEvent> recentForAdopter(int adopterId, int limit) throws SQLException {
        String sql = "SELECT r.*, a.name AS animal_name FROM adoption_reservation r "
                + "JOIN animal a ON a.id = r.animal_id WHERE r.adopter_id = ? "
                + "ORDER BY r.id DESC LIMIT ?";
        java.util.List<ReservationEvent> results = new java.util.ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, adopterId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(new ReservationEvent(map(rs), rs.getString("animal_name")));
            }
        }
        return results;
    }

    /** Most recent status for an applicant, for staff review history. */
    public Optional<Reservation> latestForApplicant(int animalId, int adopterId) throws SQLException {
        String sql = "SELECT * FROM adoption_reservation WHERE animal_id = ? "
                + "AND adopter_id = ? ORDER BY id DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, animalId);
            ps.setInt(2, adopterId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Reservation insertActive(int animalId, int adopterId, LocalDateTime now, LocalDateTime until)
            throws SQLException {
        String sql = "INSERT INTO adoption_reservation "
                + "(animal_id, adopter_id, reserved_at, expires_at, status) "
                + "VALUES (?, ?, ?, ?, 'ACTIVE')";
        try (PreparedStatement ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, animalId);
            ps.setInt(2, adopterId);
            ps.setString(3, now.toString());
            ps.setString(4, until.toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return new Reservation(rs.getInt(1), animalId, adopterId, now, until, "ACTIVE");
                }
            }
        }
        throw new SQLException("Could not read the new reservation ID");
    }

    public boolean markCompleted(int reservationId) throws SQLException {
        String sql = "UPDATE adoption_reservation SET status = 'COMPLETED' "
                + "WHERE id = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            return ps.executeUpdate() == 1;
        }
    }

    private Reservation map(ResultSet rs) throws SQLException {
        return new Reservation(rs.getInt("id"), rs.getInt("animal_id"), rs.getInt("adopter_id"),
                LocalDateTime.parse(rs.getString("reserved_at")),
                LocalDateTime.parse(rs.getString("expires_at")), rs.getString("status"));
    }
}
