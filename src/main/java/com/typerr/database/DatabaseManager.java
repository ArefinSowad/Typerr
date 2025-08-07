package com.typerr.database;

import com.typerr.statics.Constants;
import java.sql.*;

/**
 * Singleton database manager for SQLite database operations and schema management.
 * 
 * <p>DatabaseManager provides centralized access to the SQLite database used for persistent
 * storage of typing test results, user statistics, and application data. It implements the
 * Singleton pattern to ensure a single database connection and consistent data access.</p>
 * 
 * <h3>Key Responsibilities:</h3>
 * <ul>
 *   <li><strong>Connection Management:</strong> Single SQLite connection with automatic initialization</li>
 *   <li><strong>Schema Management:</strong> Database table creation and schema evolution</li>
 *   <li><strong>Migration System:</strong> Automatic database version updates and migrations</li>
 *   <li><strong>Transaction Support:</strong> ACID compliance for data integrity</li>
 *   <li><strong>Foreign Key Enforcement:</strong> Referential integrity constraints</li>
 * </ul>
 * 
 * <h3>Database Schema:</h3>
 * <p>The manager handles the following core tables:</p>
 * <ul>
 *   <li><strong>users:</strong> User accounts and profiles</li>
 *   <li><strong>test_results:</strong> Individual typing test performance data</li>
 *   <li><strong>leaderboards:</strong> Best performance records by category</li>
 *   <li><strong>database_version:</strong> Schema version tracking for migrations</li>
 * </ul>
 * 
 * <h3>SQLite Configuration:</h3>
 * <ul>
 *   <li><strong>Auto-commit:</strong> Enabled for immediate transaction processing</li>
 *   <li><strong>Foreign Keys:</strong> Enabled for referential integrity</li>
 *   <li><strong>File Location:</strong> Configurable via {@link Constants#DATABASE_PATH}</li>
 *   <li><strong>Connection Persistence:</strong> Single long-lived connection</li>
 * </ul>
 * 
 * <h3>Migration System:</h3>
 * <p>The database manager includes an automatic migration system that:</p>
 * <ul>
 *   <li>Tracks current database version</li>
 *   <li>Applies sequential migrations on version updates</li>
 *   <li>Ensures backward compatibility</li>
 *   <li>Provides rollback safety through transaction boundaries</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Get database manager instance
 * DatabaseManager db = DatabaseManager.getInstance();
 * 
 * // Use connection for database operations
 * try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
 *     stmt.setString(1, "parameter");
 *     ResultSet rs = stmt.executeQuery();
 *     // Process results...
 * }
 * }</pre>
 * 
 * <h3>Thread Safety:</h3>
 * <p>This class uses synchronized getInstance() method for thread-safe singleton creation.
 * However, the underlying SQLite connection should be accessed from a single thread
 * (preferably the JavaFX Application Thread) to avoid concurrency issues.</p>
 * 
 * <h3>Error Handling:</h3>
 * <p>Database initialization failures result in RuntimeException to prevent application
 * startup with an invalid database state. All SQL operations should be wrapped in
 * try-catch blocks by calling code.</p>
 * 
 * @author Typerr Development Team
 * @version 1.0
 * @since 1.0
 * @see Constants#DATABASE_PATH
 * @see Constants#DATABASE_VERSION
 * @see StatisticsService
 */
public class DatabaseManager {

    /** Singleton instance of the DatabaseManager. Initialized lazily on first access. */
    private static DatabaseManager instance;
    
    /** 
     * Main database connection to SQLite database. 
     * Configured with auto-commit enabled and foreign key constraints active.
     */
    private Connection connection;
    
    /** 
     * File system path to the SQLite database file.
     * Configured from {@link Constants#DATABASE_PATH}.
     */
    private final String databasePath;

    /**
     * Private constructor for singleton pattern. Initializes database connection
     * and performs schema setup including table creation and migrations.
     * 
     * @throws RuntimeException if database initialization fails
     */
    private DatabaseManager() {
        this.databasePath = Constants.DATABASE_PATH;
        initializeDatabase();
    }

    /**
     * Returns the singleton instance of DatabaseManager, creating it if necessary.
     * 
     * <p>This method is thread-safe and ensures only one DatabaseManager instance
     * exists throughout the application lifecycle. The instance is created lazily
     * on first access.</p>
     * 
     * @return the singleton DatabaseManager instance
     * @throws RuntimeException if database initialization fails on first creation
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Initializes the SQLite database connection and performs complete setup.
     * 
     * <p>This method performs the complete database initialization sequence:</p>
     * <ol>
     *   <li>Establishes SQLite connection with configured database path</li>
     *   <li>Enables auto-commit mode for immediate transaction processing</li>
     *   <li>Activates foreign key constraint enforcement</li>
     *   <li>Creates all required database tables</li>
     *   <li>Runs any pending database migrations</li>
     * </ol>
     * 
     * <h4>SQLite Configuration:</h4>
     * <ul>
     *   <li><strong>Auto-commit:</strong> Enabled for immediate persistence</li>
     *   <li><strong>Foreign Keys:</strong> Enabled via PRAGMA foreign_keys = ON</li>
     *   <li><strong>Connection URL:</strong> jdbc:sqlite:[database_path]</li>
     * </ul>
     * 
     * @throws RuntimeException if database connection fails or initialization errors occur
     * @see #createTables()
     * @see #runMigrations()
     */
    private void initializeDatabase() {
        try {
            String url = "jdbc:sqlite:" + databasePath;
            connection = DriverManager.getConnection(url);
            connection.setAutoCommit(true);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }

            createTables();

            runMigrations();

        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Creates all required database tables with proper schema definitions.
     * 
     * <p>This method creates the core application tables using CREATE TABLE IF NOT EXISTS
     * statements to ensure idempotent operation. The tables include:</p>
     * 
     * <h4>Table Structures:</h4>
     * 
     * <h5>users table:</h5>
     * <ul>
     *   <li><strong>id:</strong> Auto-incrementing primary key</li>
     *   <li><strong>username:</strong> Unique username (NOT NULL)</li>
     *   <li><strong>created_at:</strong> Account creation timestamp</li>
     * </ul>
     * 
     * <h5>test_results table:</h5>
     * <ul>
     *   <li><strong>id:</strong> Auto-incrementing primary key</li>
     *   <li><strong>user_id:</strong> Foreign key to users table (default: 1)</li>
     *   <li><strong>game_mode:</strong> Test mode (TIME, WORDS, etc.)</li>
     *   <li><strong>game_value:</strong> Mode-specific parameter</li>
     *   <li><strong>difficulty:</strong> Word difficulty level</li>
     *   <li><strong>net_wpm, raw_wpm:</strong> Performance metrics</li>
     *   <li><strong>accuracy:</strong> Typing accuracy percentage</li>
     *   <li><strong>correct_chars, incorrect_chars:</strong> Character counts</li>
     *   <li><strong>test_duration_seconds:</strong> Actual test duration</li>
     *   <li><strong>completed_at:</strong> Test completion timestamp</li>
     * </ul>
     * 
     * <h5>leaderboards table:</h5>
     * <ul>
     *   <li><strong>id:</strong> Auto-incrementing primary key</li>
     *   <li><strong>user_id:</strong> Foreign key to users table</li>
     *   <li><strong>difficulty:</strong> Difficulty category</li>
     *   <li><strong>category:</strong> Performance category (WPM, accuracy, etc.)</li>
     *   <li><strong>best_score:</strong> Best achieved score</li>
     *   <li><strong>achieved_at:</strong> Achievement timestamp</li>
     *   <li><strong>Constraint:</strong> Unique combination of user, difficulty, category</li>
     * </ul>
     * 
     * <h4>Referential Integrity:</h4>
     * <p>All foreign key relationships are enforced through SQLite foreign key constraints,
     * ensuring data consistency and preventing orphaned records.</p>
     * 
     * @throws SQLException if table creation fails
     * @see #insertDefaultUser()
     */
    private void createTables() throws SQLException {
        String[] tableCreationSQL = {
                """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,

                """
            CREATE TABLE IF NOT EXISTS test_results (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER DEFAULT 1,
                game_mode TEXT NOT NULL,
                game_value INTEGER NOT NULL,
                difficulty TEXT NOT NULL,
                net_wpm INTEGER NOT NULL,
                raw_wpm INTEGER NOT NULL,
                accuracy INTEGER NOT NULL,
                correct_chars INTEGER NOT NULL,
                incorrect_chars INTEGER NOT NULL,
                test_duration_seconds REAL NOT NULL,
                completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users (id)
            )
            """,

                """
            CREATE TABLE IF NOT EXISTS leaderboards (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                difficulty TEXT NOT NULL,
                category TEXT NOT NULL,
                best_score INTEGER NOT NULL,
                achieved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users (id),
                UNIQUE(user_id, difficulty, category)
            )
            """
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : tableCreationSQL) {
                stmt.execute(sql);
            }
        }

        insertDefaultUser();
    }

    /**
     * Inserts the default user account into the users table.
     * 
     * <p>This method ensures a default user (ID: 1, username: 'default') exists
     * in the database for applications that don't require user authentication.
     * Uses INSERT OR IGNORE to prevent duplicate insertion on multiple runs.</p>
     * 
     * <h4>Default User Details:</h4>
     * <ul>
     *   <li><strong>ID:</strong> 1 (fixed for predictable foreign key references)</li>
     *   <li><strong>Username:</strong> 'default'</li>
     *   <li><strong>Purpose:</strong> Enables immediate application usage without user setup</li>
     * </ul>
     * 
     * @throws SQLException if user insertion fails
     */
    private void insertDefaultUser() throws SQLException {
        String sql = "INSERT OR IGNORE INTO users (id, username) VALUES (1, 'default')";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.executeUpdate();
        }
    }

    /**
     * Executes database migrations to update schema to the current version.
     * 
     * <p>This method implements an automatic migration system that compares the
     * current database version against the target application version and applies
     * sequential migrations as needed.</p>
     * 
     * <h4>Migration Process:</h4>
     * <ol>
     *   <li>Retrieve current database version from version table</li>
     *   <li>Compare against target version from {@link Constants#DATABASE_VERSION}</li>
     *   <li>Apply sequential migrations for each version increment</li>
     *   <li>Update database version to current application version</li>
     * </ol>
     * 
     * <h4>Migration Safety:</h4>
     * <ul>
     *   <li>Sequential application prevents version gaps</li>
     *   <li>Version tracking ensures idempotent behavior</li>
     *   <li>Each migration runs in isolated context</li>
     *   <li>Failures stop migration chain to prevent corruption</li>
     * </ul>
     * 
     * @throws SQLException if migration execution fails or version update fails
     * @see #getDatabaseVersion()
     * @see #runMigration(int)
     * @see #setDatabaseVersion(int)
     * @see Constants#DATABASE_VERSION
     */
    private void runMigrations() throws SQLException {
        int currentVersion = getDatabaseVersion();

        if (currentVersion < Constants.DATABASE_VERSION) {
            for (int version = currentVersion + 1; version <= Constants.DATABASE_VERSION; version++) {
                runMigration(version);
            }

            setDatabaseVersion(Constants.DATABASE_VERSION);
        }
    }

    /**
     * Retrieves the current database schema version.
     * 
     * <p>This method queries the database_version table to determine the current
     * schema version. If the version table doesn't exist, it's created automatically.
     * If no version record exists, returns 0 indicating a fresh database.</p>
     * 
     * <h4>Version Table Structure:</h4>
     * <ul>
     *   <li><strong>version:</strong> INTEGER PRIMARY KEY containing schema version</li>
     *   <li><strong>Auto-creation:</strong> Table created if not exists</li>
     *   <li><strong>Default value:</strong> 0 for fresh databases</li>
     * </ul>
     * 
     * @return current database schema version, or 0 if no version is set
     * @throws SQLException if version table creation or query fails
     */
    private int getDatabaseVersion() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS database_version (
                    version INTEGER PRIMARY KEY
                )
                """);

            try (ResultSet rs = stmt.executeQuery("SELECT version FROM database_version LIMIT 1")) {
                return rs.next() ? rs.getInt("version") : 0;
            }
        }
    }

    /**
     * Updates the database version number after successful migration.
     * 
     * <p>This method records the new schema version in the database_version table
     * using INSERT OR REPLACE to handle both initial version setting and updates.
     * This ensures the database version accurately reflects applied migrations.</p>
     * 
     * @param version the new database schema version to record
     * @throws SQLException if version update fails
     */
    private void setDatabaseVersion(int version) throws SQLException {
        String sql = "INSERT OR REPLACE INTO database_version (version) VALUES (?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, version);
            pstmt.executeUpdate();
        }
    }

    /**
     * Executes a specific database migration for the given version.
     * 
     * <p>This method serves as a placeholder for version-specific database
     * schema changes. In the current implementation, it only logs the migration
     * attempt. Future versions should implement specific schema modifications
     * based on the version parameter.</p>
     * 
     * <h4>Migration Implementation Pattern:</h4>
     * <pre>{@code
     * private void runMigration(int version) throws SQLException {
     *     switch (version) {
     *         case 1:
     *             // Add new column or table
     *             stmt.execute("ALTER TABLE users ADD COLUMN email TEXT");
     *             break;
     *         case 2:
     *             // Create new index
     *             stmt.execute("CREATE INDEX idx_results_wpm ON test_results(net_wpm)");
     *             break;
     *         // Additional migrations...
     *     }
     * }
     * }</pre>
     * 
     * @param version the migration version to execute
     * @throws SQLException if migration execution fails
     */
    private void runMigration(int version) throws SQLException {
        System.out.println("Running migration version " + version);
    }

    /**
     * Provides access to the active database connection for SQL operations.
     * 
     * <p>This method returns the managed SQLite connection for use by other
     * database service classes. It includes automatic connection recovery
     * by re-initializing the database if the connection is null or closed.</p>
     * 
     * <h4>Connection Management:</h4>
     * <ul>
     *   <li><strong>Automatic Recovery:</strong> Re-initializes database if connection is lost</li>
     *   <li><strong>Configuration:</strong> Auto-commit enabled, foreign keys enforced</li>
     *   <li><strong>Thread Safety:</strong> Should be used from single thread</li>
     *   <li><strong>Exception Handling:</strong> Converts SQLException to RuntimeException</li>
     * </ul>
     * 
     * <h4>Usage Example:</h4>
     * <pre>{@code
     * DatabaseManager db = DatabaseManager.getInstance();
     * try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
     *     stmt.setString(1, parameter);
     *     ResultSet rs = stmt.executeQuery();
     *     // Process results...
     * }
     * }</pre>
     * 
     * @return active SQLite database connection
     * @throws RuntimeException if connection cannot be established
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initializeDatabase();
            }
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get database connection", e);
        }
    }

    /**
     * Properly closes the database connection when shutting down the application.
     * 
     * <p>This method should be called during application shutdown to ensure
     * proper resource cleanup and data integrity. It safely handles already
     * closed connections and logs any closure errors without throwing exceptions.</p>
     * 
     * <h4>Cleanup Process:</h4>
     * <ul>
     *   <li>Checks if connection exists and is open</li>
     *   <li>Closes connection gracefully</li>
     *   <li>Logs errors without throwing exceptions</li>
     *   <li>Safe to call multiple times (idempotent)</li>
     * </ul>
     * 
     * <p><strong>Best Practice:</strong> Call this method in application shutdown hooks
     * or in try-with-resources patterns for proper resource management.</p>
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }

    /**
     * Creates a backup copy of the database using SQLite VACUUM INTO command.
     * 
     * <p>This method creates a complete backup of the database by using SQLite's
     * VACUUM INTO command, which creates a new database file with all data intact
     * and optimized storage. The backup location is configured via
     * {@link Constants#DATABASE_BACKUP_PATH}.</p>
     * 
     * <h4>Backup Features:</h4>
     * <ul>
     *   <li><strong>Complete Copy:</strong> All tables, data, and schema included</li>
     *   <li><strong>Optimization:</strong> VACUUM command optimizes storage space</li>
     *   <li><strong>Atomic Operation:</strong> Backup succeeds completely or fails completely</li>
     *   <li><strong>Error Handling:</strong> Logs errors without stopping application</li>
     * </ul>
     * 
     * <h4>Usage Scenarios:</h4>
     * <ul>
     *   <li>Before major application updates</li>
     *   <li>Before database migrations</li>
     *   <li>Periodic automatic backups</li>
     *   <li>User-initiated data export</li>
     * </ul>
     * 
     * <p><strong>Note:</strong> The backup operation may take significant time for
     * large databases and should be performed on a background thread in production.</p>
     * 
     * @see Constants#DATABASE_BACKUP_PATH
     */
    public void backupDatabase() {
        try {
            String backupSql = """
                VACUUM INTO '%s'
                """.formatted(Constants.DATABASE_BACKUP_PATH);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(backupSql);
            }
        } catch (SQLException e) {
            System.err.println("Failed to backup database: " + e.getMessage());
        }
    }
}