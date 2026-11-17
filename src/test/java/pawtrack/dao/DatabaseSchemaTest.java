package pawtrack.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards schema initialisation: every table used by any feature must exist on new and migrated databases. */
class DatabaseSchemaTest {

    private static final List<String> REQUIRED_TABLES = List.of(
            "classification", "adopter", "animal", "adoption_record", "waitlist_entry",
            "adoption_reservation", "application_review", "notification_read", "users",
            "adoption_application", "adoption_application_history", "account_preferences",
            "application_notice_read", "account_profile");

    private static Set<String> names(Connection c, String type) throws SQLException {
        Set<String> out = new HashSet<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM sqlite_master WHERE type = '" + type + "'")) {
            while (rs.next()) {
                out.add(rs.getString(1));
            }
        }
        return out;
    }

    private static Set<String> columns(Connection c, String table) throws SQLException {
        Set<String> out = new HashSet<>();
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                out.add(rs.getString("name"));
            }
        }
        return out;
    }

    private static void assertFullSchema(Database db) throws SQLException {
        Connection c = db.connection();
        Set<String> tables = names(c, "table");
        for (String table : REQUIRED_TABLES) {
            assertTrue(tables.contains(table), "missing table " + table);
        }
        assertTrue(columns(c, "animal").containsAll(List.of("source", "created_at")));
        assertTrue(columns(c, "adopter").contains("created_at"));
        assertTrue(names(c, "index").contains("one_active_adoption_application"));
        assertTrue(names(c, "index").contains("users_one_account_per_adopter"));
        try (Statement st = c.createStatement()) {                       // tables are usable, not just present
            for (String table : List.of("adoption_application", "account_preferences",
                    "adoption_application_history", "application_notice_read", "account_profile")) {
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    assertTrue(rs.next());
                }
            }
        }
    }

    @Test
    void newDatabaseHasEveryRequiredTable() throws SQLException {
        try (Database db = Database.openInMemory()) {
            assertFullSchema(db);
        }
    }

    @Test
    void preFeatureDatabaseGainsFeatureTablesWithoutLosingData(@TempDir Path dir) throws SQLException {
        String file = dir.resolve("old.db").toString();
        try (Connection raw = DriverManager.getConnection("jdbc:sqlite:" + file); Statement st = raw.createStatement()) {
            st.execute("CREATE TABLE adopter (id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,"
                    + "email TEXT NOT NULL COLLATE NOCASE UNIQUE,phone TEXT)");
            st.execute("CREATE TABLE animal (id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,"
                    + "species TEXT NOT NULL,breed TEXT NOT NULL,age_months INTEGER NOT NULL CHECK (age_months >= 0),"
                    + "intake_date TEXT NOT NULL,status TEXT NOT NULL,classification_id INTEGER)");
            st.execute("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT,username TEXT NOT NULL UNIQUE,"
                    + "password_hash TEXT NOT NULL,password_salt TEXT NOT NULL,"
                    + "role TEXT NOT NULL CHECK (role IN ('STAFF','ADOPTER')),adopter_id INTEGER REFERENCES adopter(id))");
            st.execute("INSERT INTO adopter (name,email,phone) VALUES ('Ann','ann@example.com','')");
            st.execute("INSERT INTO animal (name,species,breed,age_months,intake_date,status) "
                    + "VALUES ('Rex','Dog','Beagle',24,'2024-01-01','AVAILABLE')");
            st.execute("INSERT INTO users (username,password_hash,password_salt,role,adopter_id) VALUES ('ann','h','s','ADOPTER',1)");
        }
        for (int run = 0; run < 2; run++) {                              // second open proves idempotence
            try (Database db = Database.openFile(file)) {
                assertFullSchema(db);
                assertEquals(1, new AnimalDao(db).count());
                assertEquals(1, new AdopterDao(db).count());
                assertEquals(1, new UserDao(db).findByUsername("ann").orElseThrow().getAdopterId().intValue());
            }
        }
    }
    @Test
    void existingFeatureDataAndUserCredentialsSurviveRepeatedInitialization(@TempDir Path dir) throws SQLException {
        String file = dir.resolve("with-features.db").toString();
        String originalHash;
        try (Database db = Database.openFile(file)) {
            Connection c = db.connection();
            try (Statement st = c.createStatement()) {
                st.executeUpdate("INSERT INTO adopter(name,email,phone) VALUES('Owner','owner@example.com','123')");
                st.executeUpdate("INSERT INTO animal(name,species,breed,age_months,intake_date,status) "
                        + "VALUES('Rex','Dog','Beagle',24,'2024-01-01','AVAILABLE')");
                st.executeUpdate("INSERT INTO adoption_application(animal_id,adopter_id,applicant_name,applicant_email,answers,status,submitted_at,updated_at) "
                        + "VALUES(1,1,'Owner','owner@example.com','{}','PENDING_REVIEW','2026-11-01','2026-11-01')");
                st.executeUpdate("INSERT INTO adoption_application_history(application_id,status,note,occurred_at) "
                        + "VALUES(1,'PENDING_REVIEW','submitted','2026-11-01')");
                st.executeUpdate("INSERT INTO account_preferences(user_id,pref_key,pref_value) "
                        + "VALUES(1,'notification','enabled')");
                st.executeUpdate("INSERT INTO application_notice_read(user_id,application_id,status,updated_at) "
                        + "VALUES(1,1,'PENDING_REVIEW','2026-11-01')");
                st.executeUpdate("INSERT INTO account_profile(user_id,full_name,email) "
                        + "VALUES(1,'Staff Member','staff@example.com')");
            }
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT password_hash FROM users WHERE id = 1")) {
                assertTrue(rs.next());
                originalHash = rs.getString(1);
            }
        }
        for (int attempt = 0; attempt < 2; attempt++) {
            try (Database db = Database.openFile(file)) {
                assertFullSchema(db);
                Connection c = db.connection();
                for (String table : List.of("adoption_application", "adoption_application_history",
                        "account_preferences", "application_notice_read", "account_profile")) {
                    try (Statement st = c.createStatement();
                         ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
                        assertTrue(rs.next());
                        assertEquals(1, rs.getInt(1), table + " must retain its existing row");
                    }
                }
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("SELECT password_hash FROM users WHERE id = 1")) {
                    assertTrue(rs.next());
                    assertEquals(originalHash, rs.getString(1));
                }
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("PRAGMA foreign_key_check")) {
                    assertTrue(!rs.next(), "foreign key violations introduced by initialization");
                }
            }
        }
    }

}
