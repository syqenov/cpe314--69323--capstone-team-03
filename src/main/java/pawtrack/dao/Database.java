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
	        seedDefaultUsers();
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
	                        + "UNIQUE(animal_id, adopter_id))",
	                "CREATE TABLE IF NOT EXISTS users ("
	                        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
	                        + "username TEXT NOT NULL UNIQUE,"
	                        + "password_hash TEXT NOT NULL,"
	                        + "password_salt TEXT NOT NULL,"
	                        + "role TEXT NOT NULL CHECK (role IN ('STAFF','ADOPTER')),"
	                        + "adopter_id INTEGER REFERENCES adopter(id))"
	        };
	        try (Statement statement = connection.createStatement()) {
	            for (String sql : ddl) {
	                statement.execute(sql);
	            }
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
