package pawtrack.dao;

import pawtrack.model.ClassificationNode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistence for the self-referencing classification tree.
 */
public final class ClassificationDao {

    /** Flat row as read back from the {@code classification} table. */
    public record Row(int id, String name, Integer parentId) {
    }

    private final Connection connection;

    public ClassificationDao(Database database) {
        this.connection = database.connection();
    }

    public int count() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM classification")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Recursively persists a whole subtree, assigning database ids to the nodes. */
    public void persistTree(ClassificationNode root, Integer parentId) throws SQLException {
        int id = insert(root.getName(), parentId);
        root.assignId(id);
        for (ClassificationNode child : root.getChildren()) {
            persistTree(child, id);
        }
    }

    public int insert(String name, Integer parentId) throws SQLException {
        String sql = "INSERT INTO classification (name, parent_id) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            if (parentId == null) {
                ps.setNull(2, java.sql.Types.INTEGER);
            } else {
                ps.setInt(2, parentId);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to insert classification: " + name);
    }

    public List<Row> loadAll() throws SQLException {
        String sql = "SELECT id, name, parent_id FROM classification ORDER BY id";
        List<Row> rows = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int parent = rs.getInt("parent_id");
                Integer parentId = rs.wasNull() ? null : parent;
                rows.add(new Row(rs.getInt("id"), rs.getString("name"), parentId));
            }
        }
        return rows;
    }

    /** Finds a single node name by id (used when re-hydrating animal links). */
    public String findName(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT name FROM classification WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }
}
