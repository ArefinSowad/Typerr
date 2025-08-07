package com.typerr.database;

import com.typerr.statics.Constants;
import java.sql.*;

public class DatabaseManager {

    private static DatabaseManager instance;

    private Connection connection;

    private final String databasePath;

    private DatabaseManager() {
        this.databasePath = Constants.DATABASE_PATH;
        initializeDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

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

    private void insertDefaultUser() throws SQLException {
        String sql = "INSERT OR IGNORE INTO users (id, username) VALUES (1, 'default')";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.executeUpdate();
        }
    }

    private void runMigrations() throws SQLException {
        int currentVersion = getDatabaseVersion();

        if (currentVersion < Constants.DATABASE_VERSION) {
            for (int version = currentVersion + 1; version <= Constants.DATABASE_VERSION; version++) {
                runMigration(version);
            }

            setDatabaseVersion(Constants.DATABASE_VERSION);
        }
    }

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

    private void setDatabaseVersion(int version) throws SQLException {
        String sql = "INSERT OR REPLACE INTO database_version (version) VALUES (?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, version);
            pstmt.executeUpdate();
        }
    }

    private void runMigration(int version) throws SQLException {
        System.out.println("Running migration version " + version);
    }

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

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }

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