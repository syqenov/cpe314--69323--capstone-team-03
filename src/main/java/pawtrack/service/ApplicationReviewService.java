package pawtrack.service;

import pawtrack.controller.AuthController;
import pawtrack.dao.Database;
import pawtrack.dao.ReservationDao;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/** Staff-only application review notes and stages; approval still uses FIFO reservations. */
public final class ApplicationReviewService {
    public record Review(String status, String note, LocalDateTime updatedAt) {}
    private final AuthController auth;
    private final Database database;
    private final ReservationDao reservations;
    public ApplicationReviewService(AuthController auth, Database database, ReservationDao reservations) {
        this.auth = Objects.requireNonNull(auth);
        this.database = Objects.requireNonNull(database);
        this.reservations = Objects.requireNonNull(reservations);
    }
    public Optional<Review> get(int animalId, int adopterId) throws SQLException {
        auth.requireStaff();
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT status, staff_note, updated_at FROM application_review WHERE animal_id=? AND adopter_id=?")) {
            ps.setInt(1, animalId); ps.setInt(2, adopterId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(new Review(rs.getString(1), rs.getString(2),
                        LocalDateTime.parse(rs.getString(3)))) : Optional.empty();
            }
        }
    }
    /** A logged-in adopter may see only the review stage of their own request. */
    public Optional<String> myStage(int animalId) throws SQLException {
        var user = auth.getCurrentUser();
        if (user == null || !auth.isAdopter() || user.getAdopterId() == null)
            throw new pawtrack.controller.UnauthorizedException("Adopter login required");
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT status FROM application_review WHERE animal_id=? AND adopter_id=?")) {
            ps.setInt(1, animalId); ps.setInt(2, user.getAdopterId());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
            }
        }
    }

    /** First eligible queued applicant only: later applicants must wait their turn. */
    public void startFirstReview(int animalId, int adopterId, String staffNote) throws SQLException {
        auth.requireStaff();
        int first = reservations.firstQueuedAdopter(animalId)
                .orElseThrow(() -> new IllegalStateException("No eligible applicant"));
        if (first != adopterId) throw new IllegalStateException("Review must follow FIFO order");
        if (reservations.activeForAnimal(animalId).isPresent())
            throw new IllegalStateException("This animal is already reserved");
        upsert(animalId, adopterId, "UNDER_REVIEW", staffNote);
    }
    public void markApproved(int animalId, int adopterId) throws SQLException {
        auth.requireStaff();
        if (reservations.activeForAnimal(animalId).stream().noneMatch(r -> r.adopterId() == adopterId))
            throw new IllegalStateException("Approval requires an active reservation for this applicant");
        upsert(animalId, adopterId, "APPROVED", get(animalId, adopterId).map(Review::note).orElse(""));
    }
    private void upsert(int animalId, int adopterId, String status, String note) throws SQLException {
        String cleaned = note == null ? "" : note.strip();
        if (cleaned.length() > 500) throw new IllegalArgumentException("Staff note must be 500 characters or less");
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO application_review(animal_id, adopter_id, status, staff_note, updated_at) VALUES(?,?,?,?,?) "
                + "ON CONFLICT(animal_id,adopter_id) DO UPDATE SET status=excluded.status, "
                + "staff_note=excluded.staff_note, updated_at=excluded.updated_at")) {
            ps.setInt(1, animalId); ps.setInt(2, adopterId); ps.setString(3, status);
            ps.setString(4, cleaned); ps.setString(5, LocalDateTime.now().toString());
            ps.executeUpdate();
        }
    }
}
