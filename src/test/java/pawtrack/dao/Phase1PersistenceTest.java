package pawtrack.dao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pawtrack.model.Adopter;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.Dog;
import pawtrack.model.Species;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Phase 1: source/createdAt persistence, safe migration of legacy rows, adopter-link audit. */
class Phase1PersistenceTest {

    @Test
    @DisplayName("Animal.source and createdAt survive insert, update, retrieval and restart")
    void animalSourceAndCreatedAtPersist(@TempDir Path dir) throws SQLException {
        String file = dir.resolve("shelter.db").toString();
        LocalDate created = LocalDate.of(2024, 3, 5);
        int id;
        try (Database db = Database.openFile(file)) {
            AnimalDao dao = new AnimalDao(db);
            Animal rex = Animal.create(Species.DOG, 0, "Rex", "Beagle", 24, LocalDate.of(2024, 1, 1),
                    "Surrender", created, AnimalStatus.AVAILABLE);
            id = dao.insert(rex);
            Animal loaded = dao.findById(id).orElseThrow();
            assertEquals("surrender", loaded.getSource());
            assertEquals(created, loaded.getCreatedAt());
        }
        try (Database db = Database.openFile(file)) {
            AnimalDao dao = new AnimalDao(db);
            Animal loaded = dao.findById(id).orElseThrow();
            assertEquals("surrender", loaded.getSource());
            assertEquals(created, loaded.getCreatedAt());

            assertTrue(dao.updateSource(id, " Transfer "));
            assertTrue(dao.updateDetails(id, "Rex II", "Beagle", 25, LocalDate.of(2024, 1, 2), "owner-return"));
            Animal updated = dao.findById(id).orElseThrow();
            assertEquals("owner-return", updated.getSource());
            assertEquals("Rex II", updated.getName());
            assertEquals(created, updated.getCreatedAt());          // never rewritten by updates

            // The legacy 5-argument update must not clobber the stored source.
            assertTrue(dao.updateDetails(id, "Rex III", "Beagle", 26, LocalDate.of(2024, 1, 2)));
            assertEquals("owner-return", dao.findById(id).orElseThrow().getSource());
        }
    }

    @Test
    @DisplayName("An animal built with the legacy constructor keeps one stable createdAt after restart")
    void legacyConstructorCreatedAtIsStable(@TempDir Path dir) throws SQLException {
        String file = dir.resolve("shelter.db").toString();
        Animal dog = new Dog(0, "Ace", "Poodle", 18, LocalDate.of(2024, 1, 1), AnimalStatus.AVAILABLE);
        int id;
        try (Database db = Database.openFile(file)) {
            id = new AnimalDao(db).insert(dog);
        }
        try (Database db = Database.openFile(file)) {
            Animal loaded = new AnimalDao(db).findById(id).orElseThrow();
            assertEquals(dog.getCreatedAt(), loaded.getCreatedAt());
            assertEquals("stray", loaded.getSource());
        }
    }

    @Test
    @DisplayName("Adopter.createdAt is stored and mapped back")
    void adopterCreatedAtPersists(@TempDir Path dir) throws SQLException {
        String file = dir.resolve("shelter.db").toString();
        LocalDate created = LocalDate.of(2023, 5, 6);
        int id;
        try (Database db = Database.openFile(file)) {
            id = new AdopterDao(db).insert(new Adopter(0, "Jane Doe", "jane@example.com", "555", created));
        }
        try (Database db = Database.openFile(file)) {
            AdopterDao dao = new AdopterDao(db);
            assertEquals(created, dao.findById(id).orElseThrow().getCreatedAt());
            assertEquals(created, dao.findByEmail("JANE@example.com").orElseThrow().getCreatedAt());
        }
    }

    @Test
    @DisplayName("A pre-Phase-1 database is migrated in place without losing rows; legacy accounts are repaired")
    void legacyDatabaseIsMigratedAndAudited(@TempDir Path dir) throws SQLException {
        String file = dir.resolve("legacy.db").toString();
        try (Connection raw = DriverManager.getConnection("jdbc:sqlite:" + file);
             Statement st = raw.createStatement()) {
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
                    + "VALUES ('Rex','Dog','Beagle',24,'2024-01-01','Available')");
            st.execute("INSERT INTO animal (name,species,breed,age_months,intake_date,status) "
                    + "VALUES ('Mia','Cat','Siamese',5,'2025-02-03','Available')");
            st.execute("INSERT INTO users (username,password_hash,password_salt,role,adopter_id) VALUES ('boss','h','s','STAFF',NULL)");
            st.execute("INSERT INTO users (username,password_hash,password_salt,role,adopter_id) VALUES ('nolink','h','s','ADOPTER',NULL)");
            st.execute("INSERT INTO users (username,password_hash,password_salt,role,adopter_id) VALUES ('dangling','h','s','ADOPTER',999)");
            st.execute("INSERT INTO users (username,password_hash,password_salt,role,adopter_id) VALUES ('owner','h','s','ADOPTER',1)");
            st.execute("INSERT INTO users (username,password_hash,password_salt,role,adopter_id) VALUES ('sharer','h','s','ADOPTER',1)");
        }

        int adoptersAfterFirstOpen;
        try (Database db = Database.openFile(file)) {
            AnimalDao animals = new AnimalDao(db);
            assertEquals(2, animals.count());                                   // nothing deleted
            Animal rex = animals.findAll().get(0);
            assertEquals("stray", rex.getSource());                              // legacy default
            assertEquals(LocalDate.of(2024, 1, 1), rex.getCreatedAt());          // legacy: intake date
            assertEquals(LocalDate.of(2025, 2, 3), animals.findAll().get(1).getCreatedAt());

            AdopterDao adopters = new AdopterDao(db);
            Adopter ann = adopters.findById(1).orElseThrow();
            assertEquals("Ann", ann.getName());
            assertNotNull(ann.getCreatedAt());
            assertFalse(ann.getCreatedAt().isAfter(LocalDate.now()));

            UserDao users = new UserDao(db);
            assertEquals(0, users.countInvalidAdopterLinks());
            assertEquals(1, users.findByUsername("owner").orElseThrow().getAdopterId().intValue()); // existing link kept
            assertNull(users.findByUsername("boss").orElseThrow().getAdopterId());          // staff untouched
            assertEquals(4, distinctAdopterLinks(db));                                      // 4 accounts, 4 profiles
            adoptersAfterFirstOpen = adopters.count();
        }
        try (Database db = Database.openFile(file)) {                           // idempotent on restart
            assertEquals(adoptersAfterFirstOpen, new AdopterDao(db).count());
            assertEquals(0, db.auditAdopterLinks());
            assertEquals(LocalDate.of(2024, 1, 1), new AnimalDao(db).findAll().get(0).getCreatedAt());
        }
    }

    @Test
    @DisplayName("The seeded default adopter account is linked to a valid adopter row")
    void defaultAdopterAccountIsLinked() throws SQLException {
        try (Database db = Database.openInMemory()) {
            UserDao users = new UserDao(db);
            assertEquals(0, users.countInvalidAdopterLinks());
            assertNotNull(users.findByUsername("user").orElseThrow().getAdopterId());
        }
    }

    private static int distinctAdopterLinks(Database db) throws SQLException {
        try (Statement st = db.connection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(DISTINCT adopter_id) FROM users WHERE role = 'ADOPTER'")) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }
}
