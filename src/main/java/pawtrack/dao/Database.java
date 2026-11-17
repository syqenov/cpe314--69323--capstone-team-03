	package pawtrack.dao;
	
	import pawtrack.service.PasswordHasher;
	
	import java.sql.Connection;
	import java.sql.DriverManager;
	import java.sql.PreparedStatement;
	import java.sql.ResultSet;
	import java.sql.SQLException;
	import java.sql.Statement;
	import java.util.logging.Logger;
	
	/**
	 * Owns the single JDBC {@link Connection} to the local SQLite database and
	 * creates the schema on first use.
	 *
	 * <p>Tables: {@code classification}, {@code animal}, {@code adopter},
	 * {@code adoption_record}, {@code waitlist_entry}, and {@code users}.</p>
	 */
	public final class Database implements AutoCloseable {
	
	    private static final Logger LOGGER = Logger.getLogger(Database.class.getName());
	    private static final String DEFAULT_ADMIN_USER = "admin";
	    private static final String DEFAULT_ADMIN_PASS = "admin123";
	    private static final String DEFAULT_USER = "user";
	    private static final String DEFAULT_USER_PASS = "user123";
	
	    private final Connection connection;
	
	    public Database(String jdbcUrl) throws SQLException {
	        this.connection = DriverManager.getConnection(jdbcUrl);
	        try (Statement statement = connection.createStatement()) {
	            statement.execute("PRAGMA foreign_keys = ON");
	        }
	        initSchema();
        migrateSchema();
        seedDefaultUsers();
        auditAdopterLinks();          // every ADOPTER account gets exactly one valid adopter profile
        enforceOneAccountPerAdopter();
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
	                        + "phone TEXT,"
	                        + "created_at TEXT NOT NULL DEFAULT (date('now','localtime')))",
	                "CREATE TABLE IF NOT EXISTS animal ("
	                        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
	                        + "name TEXT NOT NULL,"
	                        + "species TEXT NOT NULL,"
	                        + "breed TEXT NOT NULL,"
	                        + "age_months INTEGER NOT NULL CHECK (age_months >= 0),"
	                        + "intake_date TEXT NOT NULL,"
	                        + "status TEXT NOT NULL,"
	                        + "classification_id INTEGER REFERENCES classification(id),"
	                        + "source TEXT NOT NULL DEFAULT 'stray',"
	                        + "created_at TEXT NOT NULL DEFAULT (date('now','localtime')))",
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
	                        + "UNIQUE(animal_id, adopter_id))",
	                "CREATE TABLE IF NOT EXISTS adoption_reservation ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "animal_id INTEGER NOT NULL REFERENCES animal(id) ON DELETE CASCADE,"
                        + "adopter_id INTEGER NOT NULL REFERENCES adopter(id) ON DELETE CASCADE,"
                        + "reserved_at TEXT NOT NULL,"
                        + "expires_at TEXT NOT NULL,"
                        + "status TEXT NOT NULL CHECK(status IN ('ACTIVE','COMPLETED','CANCELLED','EXPIRED')))",
                "CREATE UNIQUE INDEX IF NOT EXISTS only_one_active_reservation "
                        + "ON adoption_reservation(animal_id) WHERE status = 'ACTIVE'",
                "CREATE TABLE IF NOT EXISTS application_review ("
                        + "animal_id INTEGER NOT NULL REFERENCES animal(id) ON DELETE CASCADE,"
                        + "adopter_id INTEGER NOT NULL REFERENCES adopter(id) ON DELETE CASCADE,"
                        + "status TEXT NOT NULL CHECK(status IN ('UNDER_REVIEW','APPROVED')) ,"
                        + "staff_note TEXT NOT NULL DEFAULT '',"
                        + "updated_at TEXT NOT NULL,"
                        + "PRIMARY KEY(animal_id, adopter_id))",
                "CREATE TABLE IF NOT EXISTS notification_read ("
                        + "user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,"
                        + "reservation_id INTEGER NOT NULL REFERENCES adoption_reservation(id) ON DELETE CASCADE,"
                        + "status TEXT NOT NULL,"
                        + "PRIMARY KEY(user_id, reservation_id, status))",
                "CREATE TABLE IF NOT EXISTS users ("
	                        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
	                        + "username TEXT NOT NULL UNIQUE,"
	                        + "password_hash TEXT NOT NULL,"
	                        + "password_salt TEXT NOT NULL,"
	                        + "role TEXT NOT NULL CHECK (role IN ('STAFF','ADOPTER')),"
	                        + "adopter_id INTEGER REFERENCES adopter(id))"
	        };
	        // Additive, non-destructive schema extensions for adoption applications and settings.
	        String[] featureDdl = {
	            "CREATE TABLE IF NOT EXISTS adoption_application (" +
	                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
	                "animal_id INTEGER NOT NULL REFERENCES animal(id)," +
	                "adopter_id INTEGER NOT NULL REFERENCES adopter(id)," +
	                "applicant_name TEXT NOT NULL, applicant_email TEXT NOT NULL," +
	                "applicant_phone TEXT, applicant_address TEXT, answers TEXT NOT NULL," +
	                "status TEXT NOT NULL, submitted_at TEXT NOT NULL, updated_at TEXT NOT NULL," +
	                "reviewed_by INTEGER REFERENCES users(id), reviewed_at TEXT," +
	                "staff_message TEXT NOT NULL DEFAULT '')",
	            "CREATE UNIQUE INDEX IF NOT EXISTS one_active_adoption_application ON adoption_application(animal_id,adopter_id) " +
	                "WHERE status IN ('PENDING_REVIEW','UNDER_REVIEW','MORE_INFO_NEEDED','APPROVED')",
	            "CREATE TABLE IF NOT EXISTS adoption_application_history (" +
	                "id INTEGER PRIMARY KEY AUTOINCREMENT, application_id INTEGER NOT NULL REFERENCES adoption_application(id)," +
	                "status TEXT NOT NULL, actor_id INTEGER REFERENCES users(id), note TEXT, occurred_at TEXT NOT NULL)",
	            "CREATE TABLE IF NOT EXISTS account_preferences (" +
	                "user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE," +
	                "pref_key TEXT NOT NULL, pref_value TEXT NOT NULL, PRIMARY KEY(user_id,pref_key))",
	            "CREATE TABLE IF NOT EXISTS application_notice_read (" +
	                "user_id INTEGER NOT NULL REFERENCES users(id), application_id INTEGER NOT NULL REFERENCES adoption_application(id)," +
	                "status TEXT NOT NULL, updated_at TEXT NOT NULL," +
	                "PRIMARY KEY(user_id, application_id, status, updated_at))",
	            "CREATE TABLE IF NOT EXISTS account_profile (" +
	                "user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE," +
	                "full_name TEXT, email TEXT, phone TEXT, address TEXT, preferred_contact TEXT)"
	        };
	        try (Statement statement = connection.createStatement()) {
	            for (String sql : ddl) {
	                statement.execute(sql);
	            }
	            for (String sql : featureDdl) { statement.execute(sql); }
	        }
	    }
	
	    // ------------------------------------------------------------------
    // Phase 1: additive, idempotent migrations (never drops or deletes data)
    // ------------------------------------------------------------------

    /**
     * Adds {@code animal.source}, {@code animal.created_at} and {@code adopter.created_at} to
     * databases created before those columns existed, then backfills legacy rows.
     * Legacy animals get {@code created_at = intake_date}; legacy adopters get their earliest
     * known adoption/waitlist date (else the migration date). Values are stored, so they stay
     * stable across restarts.
     */
    private void migrateSchema() throws SQLException {
        boolean previous = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            addColumnIfMissing("animal", "source", "TEXT NOT NULL DEFAULT 'stray'");
            addColumnIfMissing("animal", "created_at", "TEXT");
            addColumnIfMissing("adopter", "created_at", "TEXT");
            try (Statement st = connection.createStatement()) {
                st.executeUpdate("UPDATE animal SET source = 'stray' WHERE source IS NULL OR trim(source) = ''");
                st.executeUpdate("UPDATE animal SET created_at = CASE "
                        + "WHEN intake_date GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]*' "
                        + "THEN substr(intake_date, 1, 10) ELSE date('now','localtime') END "
                        + "WHERE created_at IS NULL OR trim(created_at) = ''");
                st.executeUpdate("UPDATE adopter SET created_at = COALESCE("
                        + "(SELECT MIN(substr(r.adoption_date, 1, 10)) FROM adoption_record r WHERE r.adopter_id = adopter.id), "
                        + "(SELECT MIN(substr(w.requested_at, 1, 10)) FROM waitlist_entry w WHERE w.adopter_id = adopter.id), "
                        + "date('now','localtime')) WHERE created_at IS NULL OR trim(created_at) = ''");
            }
            connection.commit();
        } catch (SQLException | RuntimeException failure) {
            connection.rollback();
            throw failure;
        } finally {
            connection.setAutoCommit(previous);
        }
    }

    private void addColumnIfMissing(String table, String column, String definition) throws SQLException {
        boolean present = false;
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    present = true;
                }
            }
        }
        if (!present) {
            try (Statement st = connection.createStatement()) {
                st.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            }
            LOGGER.info("Migrated schema: added " + table + "." + column);
        }
    }

    /**
     * Audits {@code users.adopter_id}: every ADOPTER account must point to an existing adopter
     * row that no other account uses. Accounts with a missing, dangling or shared link get their
     * own account-scoped adopter profile (reusing the account's placeholder profile if it already
     * exists). No adopter, application, waitlist or adoption row is deleted or re-pointed.
     *
     * @return number of accounts repaired
     */
    public int auditAdopterLinks() throws SQLException {
        boolean previous = connection.getAutoCommit();
        connection.setAutoCommit(false);
        int repaired = 0;
        try {
            try (Statement st = connection.createStatement()) {
                // Staff accounts never need an adopter profile; clear only dangling references.
                st.executeUpdate("UPDATE users SET adopter_id = NULL WHERE role <> 'ADOPTER' "
                        + "AND adopter_id IS NOT NULL AND adopter_id NOT IN (SELECT id FROM adopter)");
            }
            java.util.List<Integer> userIds = new java.util.ArrayList<>();
            java.util.List<String> names = new java.util.ArrayList<>();
            String find = "SELECT u.id, u.username FROM users u WHERE u.role = 'ADOPTER' AND ("
                    + "u.adopter_id IS NULL "
                    + "OR NOT EXISTS (SELECT 1 FROM adopter a WHERE a.id = u.adopter_id) "
                    + "OR EXISTS (SELECT 1 FROM users o WHERE o.adopter_id = u.adopter_id AND o.id < u.id)) "
                    + "ORDER BY u.id";
            try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(find)) {
                while (rs.next()) {
                    userIds.add(rs.getInt(1));
                    names.add(rs.getString(2));
                }
            }
            for (int i = 0; i < userIds.size(); i++) {
                int adopterId = accountAdopterId(userIds.get(i), names.get(i));
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE users SET adopter_id = ? WHERE id = ?")) {
                    ps.setInt(1, adopterId);
                    ps.setInt(2, userIds.get(i));
                    ps.executeUpdate();
                }
                repaired++;
            }
            connection.commit();
        } catch (SQLException | RuntimeException failure) {
            connection.rollback();
            throw failure;
        } finally {
            connection.setAutoCommit(previous);
        }
        if (repaired > 0) {
            LOGGER.info("Adopter link audit repaired " + repaired + " account(s)");
        }
        return repaired;
    }

    private int accountAdopterId(int userId, String username) throws SQLException {
        String email = "account-" + userId + "@pawtrack.invalid";
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id FROM adopter WHERE lower(email) = lower(?) "
                        + "AND id NOT IN (SELECT adopter_id FROM users WHERE adopter_id IS NOT NULL AND id <> ?)")) {
            ps.setString(1, email);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO adopter (name, email, phone, created_at) VALUES (?, ?, '', ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, java.time.LocalDate.now().toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Could not create adopter profile for account " + userId);
    }

    /** One adopter profile per account; best effort so an unexpected legacy state cannot block startup. */
    private void enforceOneAccountPerAdopter() {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS users_one_account_per_adopter "
                    + "ON users(adopter_id) WHERE adopter_id IS NOT NULL");
        } catch (SQLException e) {
            LOGGER.log(java.util.logging.Level.WARNING,
                    "Could not enforce one account per adopter; run the link audit", e);
        }
    }

    private void seedDefaultUsers() throws SQLException {
	        if (userCount() > 0) {
	            return;
	        }
	        insertUser(DEFAULT_ADMIN_USER, DEFAULT_ADMIN_PASS, "STAFF", null);
	        insertUser(DEFAULT_USER, DEFAULT_USER_PASS, "ADOPTER", null);
	        LOGGER.info("Seeded default users");
	    }
	
	    private int userCount() throws SQLException {
	        try (Statement st = connection.createStatement();
	             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
	            return rs.next() ? rs.getInt(1) : 0;
	        }
	    }
	
	    private void insertUser(String username, String password, String role, Integer adopterId) throws SQLException {
	        String salt = PasswordHasher.generateSalt();
	        String hash = PasswordHasher.hash(password, salt);
	        String sql = "INSERT INTO users (username, password_hash, password_salt, role, adopter_id) VALUES (?, ?, ?, ?, ?)";
	        try (PreparedStatement ps = connection.prepareStatement(sql)) {
	            ps.setString(1, username);
	            ps.setString(2, hash);
	            ps.setString(3, salt);
	            ps.setString(4, role);
	            if (adopterId == null) {
	                ps.setNull(5, java.sql.Types.INTEGER);
	            } else {
	                ps.setInt(5, adopterId);
	            }
	            ps.executeUpdate();
	        }
	    }
	    
	    public void beginTransaction() throws SQLException {
	        connection.setAutoCommit(false);
	    }
	
	    public void commit() throws SQLException {
	        connection.commit();
	        connection.setAutoCommit(true);
	    }
	
	    public void rollback() {
	        try {
	            if (!connection.getAutoCommit()) {
	                connection.rollback();
	                connection.setAutoCommit(true);
	            }
	        } catch (SQLException e) {
	            LOGGER.log(java.util.logging.Level.SEVERE, "Database rollback failed", e);
	        }
	    }
	
	    @Override
	    public void close() throws SQLException {
	        if (!connection.isClosed()) {
	            connection.close();
	        }
	    }
	}
