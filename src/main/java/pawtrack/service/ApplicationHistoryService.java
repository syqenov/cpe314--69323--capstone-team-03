package pawtrack.service;

import pawtrack.controller.AuthController;
import pawtrack.dao.Database;
import java.sql.*;
import java.util.*;

/** Staff-only, read-only historical reservation events. */
public final class ApplicationHistoryService {
    public record Entry(int id, String animal, String adopter, String status, String reservedAt, String expiresAt) {}
    private final AuthController auth;
    private final Database database;
    public ApplicationHistoryService(AuthController auth, Database database) {
        this.auth = Objects.requireNonNull(auth);
        this.database = Objects.requireNonNull(database);
    }
    public List<Entry> recent(int limit) throws SQLException {
        auth.requireStaff();
        if (limit < 1 || limit > 500) throw new IllegalArgumentException("Limit must be 1 to 500");
        String sql = "SELECT r.id, a.name AS animal_name, d.name AS adopter_name, r.status, "
                + "r.reserved_at, r.expires_at FROM adoption_reservation r "
                + "JOIN animal a ON a.id=r.animal_id JOIN adopter d ON d.id=r.adopter_id "
                + "ORDER BY r.id DESC LIMIT ?";
        List<Entry> out = new ArrayList<>();
        try (PreparedStatement ps = database.connection().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new Entry(rs.getInt("id"), rs.getString("animal_name"),
                        rs.getString("adopter_name"), rs.getString("status"),
                        rs.getString("reserved_at"), rs.getString("expires_at")));
            }
        }
        return out;
    }
}
