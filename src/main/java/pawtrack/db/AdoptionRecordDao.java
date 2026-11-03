package pawtrack.db;

import pawtrack.model.AdoptionRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC data access for {@link AdoptionRecord} and the join queries that feed the
 * analytics panel.
 */
public final class AdoptionRecordDao {

    private final Connection connection;

    public AdoptionRecordDao(Database database) {
        this.connection = database.connection();
    }

    public int insert(AdoptionRecord record) throws SQLException {
        String sql = "INSERT INTO adoption_record (animal_id, adopter_id, adoption_date, fee_paid) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, record.getAnimalId());
            ps.setInt(2, record.getAdopterId());
            ps.setString(3, record.getAdoptionDate().toString());
            ps.setDouble(4, record.getFeePaid());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    record.assignId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Failed to insert adoption record");
    }

    public List<AdoptionRecord> findAll() throws SQLException {
        List<AdoptionRecord> records = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM adoption_record ORDER BY adoption_date");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                records.add(map(rs));
            }
        }
        return records;
    }

    public List<AdoptionRecord> findByAnimalId(int animalId) throws SQLException {
        String sql = "SELECT * FROM adoption_record WHERE animal_id = ? ORDER BY adoption_date";
        List<AdoptionRecord> records = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, animalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(map(rs));
                }
            }
        }
        return records;
    }

    public Optional<LocalDate> latestAdoptionDate(int animalId) throws SQLException {
        String sql = "SELECT adoption_date FROM adoption_record WHERE animal_id = ? "
                + "ORDER BY adoption_date DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, animalId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(LocalDate.parse(rs.getString(1))) : Optional.empty();
            }
        }
    }

    public int count() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM adoption_record")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Aggregate join used by the analytics panel: average shelter stay in days
     * grouped by animal species.
     */
    public List<Object[]> averageStayDaysBySpecies() throws SQLException {
        String sql = "SELECT a.species AS species, AVG(julianday(r.adoption_date) - julianday(a.intake_date)) AS avg_days "
                + "FROM adoption_record r JOIN animal a ON a.id = r.animal_id "
                + "GROUP BY a.species ORDER BY avg_days DESC";
        List<Object[]> rows = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[]{rs.getString("species"), rs.getDouble("avg_days")});
            }
        }
        return rows;
    }

    private AdoptionRecord map(ResultSet rs) throws SQLException {
        return new AdoptionRecord(
                rs.getInt("id"),
                rs.getInt("animal_id"),
                rs.getInt("adopter_id"),
                LocalDate.parse(rs.getString("adoption_date")),
                rs.getDouble("fee_paid"));
    }
}
