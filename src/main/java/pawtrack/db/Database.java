package pawtrack.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


public final class Database implements AutoCloseable {

    private final Connection connection;

    public Database(String jdbcUrl) throws SQLException {
        this.connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        initSchema();
    }

    public static Database openFile(String path) throws SQLException {
        return new Database("jdbc:sqlite:" + path);
    }

    public static Database openInMemory() throws SQLException {
        return new Database("jdbc:sqlite::memory:");
    }

    public Connection connection() {
        return connection;
    }

    private void initSchema() throws SQLException {
        String[] ddl = {
                "CREATE TABLE IF NOT EXISTS classification ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "name TEXT NOT NULL,"
                        + "parent_id INTEGER REFERENCES classification(id) ON DELETE CASCADE)",

                "CREATE TABLE IF NOT EXISTS adopter ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "name TEXT NOT NULL,"
                        + "email TEXT NOT NULL COLLATE NOCASE UNIQUE,"
                        + "phone TEXT)",

                "CREATE TABLE IF NOT EXISTS animal ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "name TEXT NOT NULL,"
                        + "species TEXT NOT NULL,"
                        + "breed TEXT NOT NULL,"
                        + "age_months INTEGER NOT NULL CHECK (age_months >= 0),"
                        + "intake_date TEXT NOT NULL,"
                        + "status TEXT NOT NULL,"
                        + "classification_id INTEGER REFERENCES classification(id))",

                "CREATE TABLE IF NOT EXISTS adoption_record ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "animal_id INTEGER NOT NULL REFERENCES animal(id) ON DELETE CASCADE,"
                        + "adopter_id INTEGER NOT NULL REFERENCES adopter(id) ON DELETE CASCADE,"
                        + "adoption_date TEXT NOT NULL,"
                        + "fee_paid REAL NOT NULL DEFAULT 0)",

                "CREATE TABLE IF NOT EXISTS waitlist_entry ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "animal_id INTEGER NOT NULL REFERENCES animal(id) ON DELETE CASCADE,"
                        + "adopter_id INTEGER NOT NULL REFERENCES adopter(id) ON DELETE CASCADE,"
                        + "requested_at TEXT NOT NULL,"
                        + "position INTEGER NOT NULL,"
                        + "UNIQUE(animal_id, adopter_id))"
        };
        try (Statement statement = connection.createStatement()) {
            for (String sql : ddl) {
                statement.execute(sql);
            }
        }
    }

    @Override
    public void close() throws SQLException {
        if (!connection.isClosed()) {
            connection.close();
        }
    }
}
