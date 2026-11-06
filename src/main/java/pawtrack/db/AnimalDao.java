package pawtrack.db;

import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.Species;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC data access for {@link Animal}, using {@link PreparedStatement} for every
 * query.
 */
public final class AnimalDao {

    private final Connection connection;

    public AnimalDao(Database database) {
        this.connection = database.connection();
    }

    public int insert(Animal animal) throws SQLException {
        String sql = "INSERT INTO animal (name, species, breed, age_months, intake_date, status, classification_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, animal.getName());
            ps.setString(2, animal.species().label());
            ps.setString(3, animal.getBreed());
            ps.setInt(4, animal.getAgeMonths());
            ps.setString(5, animal.getIntakeDate().toString());
            ps.setString(6, animal.getStatus().name());
            if (animal.getClassificationId() == null) {
                ps.setNull(7, java.sql.Types.INTEGER);
            } else {
                ps.setInt(7, animal.getClassificationId());
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    animal.assignId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Failed to insert animal: " + animal.getName());
    }

    public boolean updateStatus(int animalId, AnimalStatus status) throws SQLException {
        String sql = "UPDATE animal SET status = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, animalId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateClassification(int animalId, Integer classificationId) throws SQLException {
        String sql = "UPDATE animal SET classification_id = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (classificationId == null) {
                ps.setNull(1, java.sql.Types.INTEGER);
            } else {
                ps.setInt(1, classificationId);
            }
            ps.setInt(2, animalId);
            return ps.executeUpdate() == 1;
        }
    }

    public Optional<Animal> findById(int id) throws SQLException {
        String sql = "SELECT * FROM animal WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Animal> findAll() throws SQLException {
        String sql = "SELECT * FROM animal ORDER BY id";
        List<Animal> animals = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                animals.add(map(rs));
            }
        }
        return animals;
    }

    public int count() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM animal")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** SQL frequency aggregation: animals per breed. */
    public Map<String, Integer> countByBreed() throws SQLException {
        String sql = "SELECT breed, COUNT(*) AS total FROM animal GROUP BY breed ORDER BY total DESC, breed";
        Map<String, Integer> result = new LinkedHashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("breed"), rs.getInt("total"));
            }
        }
        return result;
    }

    public boolean deleteById(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM animal WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    private Animal map(ResultSet rs) throws SQLException {
        Species species = Species.fromLabel(rs.getString("species"));
        Animal animal = Animal.create(
                species,
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("breed"),
                rs.getInt("age_months"),
                LocalDate.parse(rs.getString("intake_date")),
                AnimalStatus.fromLabel(rs.getString("status")));
        int classificationId = rs.getInt("classification_id");
        if (!rs.wasNull()) {
            animal.setClassificationId(classificationId);
        }
        return animal;
    }
}