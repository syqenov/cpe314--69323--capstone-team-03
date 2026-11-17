package pawtrack.service;

import pawtrack.controller.AuthController;
import pawtrack.controller.UnauthorizedException;
import pawtrack.dao.Database;
import pawtrack.dao.ReservationDao;
import pawtrack.model.Role;
import pawtrack.model.User;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** Persisted read receipts for account-owned reservation updates. */
public final class NotificationReadService {
    public record Notice(int reservationId, String animal, String status, boolean read) {}
    private final Database database;
    private final AuthController auth;
    private final ReservationDao reservations;
    public NotificationReadService(Database database, AuthController auth, ReservationDao reservations) {
        this.database = Objects.requireNonNull(database);
        this.auth = Objects.requireNonNull(auth);
        this.reservations = Objects.requireNonNull(reservations);
    }
    private User me() {
        User user = auth.getCurrentUser();
        if (user == null || user.getRole() != Role.ADOPTER || user.getAdopterId() == null || user.getId() == null)
            throw new UnauthorizedException("Adopter login required");
        return user;
    }
    public List<Notice> myNotices() throws SQLException {
        User user = me();
        List<Notice> output = new ArrayList<>();
        String sql = "SELECT 1 FROM notification_read WHERE user_id=? AND reservation_id=? AND status=?";
        for (var e : reservations.recentForAdopter(user.getAdopterId(), 100)) {
            var r = e.reservation();
            String status = AdopterNotificationService.status(r, LocalDateTime.now());
            try (PreparedStatement ps = database.connection().prepareStatement(sql)) {
                ps.setInt(1,user.getId()); ps.setInt(2,r.id()); ps.setString(3,status);
                try (ResultSet rs = ps.executeQuery()) {
                    output.add(new Notice(r.id(),e.animalName(),status,rs.next()));
                }
            }
        }
        return output;
    }
    public record ApplicationNotice(int applicationId,String animal,String status,boolean read,String updatedAt) {}
    public List<ApplicationNotice> applicationNotices() throws SQLException {
        User user=me(); List<ApplicationNotice> output=new ArrayList<>();
        try(PreparedStatement ps=database.connection().prepareStatement(
            "SELECT ap.id,a.name,ap.status,ap.updated_at, nr.user_id FROM adoption_application ap " +
            "JOIN animal a ON a.id=ap.animal_id LEFT JOIN application_notice_read nr ON nr.application_id=ap.id " +
            "AND nr.user_id=? AND nr.status=ap.status AND nr.updated_at=ap.updated_at " +
            "WHERE ap.adopter_id=? ORDER BY ap.updated_at DESC")) {
            ps.setInt(1,user.getId());ps.setInt(2,user.getAdopterId());
            try(ResultSet rs=ps.executeQuery()){while(rs.next())output.add(new ApplicationNotice(
                rs.getInt(1),rs.getString(2),rs.getString(3),rs.getObject(5)!=null,rs.getString(4)));
            }
        }return output;
    }
    public int unreadCount() throws SQLException {
        return (int) myNotices().stream().filter(n -> !n.read()).count() +
            (int)applicationNotices().stream().filter(n -> !n.read()).count();
    }
    /** Only marks a notice confirmed to belong to the current account. */
    public void markRead(int reservationId, String status) throws SQLException {
        User user = me();
        boolean owned = myNotices().stream().anyMatch(n -> n.reservationId() == reservationId && n.status().equals(status));
        if (!owned) throw new UnauthorizedException("Notification does not belong to this account");
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT OR IGNORE INTO notification_read(user_id,reservation_id,status) VALUES(?,?,?)")) {
            ps.setInt(1,user.getId()); ps.setInt(2,reservationId); ps.setString(3,status); ps.executeUpdate();
        }
    }
    public void markAllRead() throws SQLException {
        for (Notice n : myNotices()) if (!n.read()) markRead(n.reservationId(), n.status());
        User user=me();
        for(ApplicationNotice n:applicationNotices()) if(!n.read()){
            try(PreparedStatement ps=database.connection().prepareStatement(
                "INSERT OR IGNORE INTO application_notice_read(user_id,application_id,status,updated_at) VALUES(?,?,?,?)")){
                ps.setInt(1,user.getId());ps.setInt(2,n.applicationId());ps.setString(3,n.status());ps.setString(4,n.updatedAt());ps.executeUpdate();
            }
        }
    }
}
