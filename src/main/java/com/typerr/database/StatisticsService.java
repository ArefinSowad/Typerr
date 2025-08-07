package com.typerr.database;

import com.typerr.statics.GameMode;
import com.typerr.TestSession;
import com.typerr.WordProvider;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service class for managing typing test statistics, leaderboards, and data persistence.
 * 
 * <p>This service provides comprehensive functionality for storing, retrieving, and analyzing
 * typing test performance data. It integrates with the {@link DatabaseManager} to handle
 * all database operations related to test results, leaderboards, and user statistics.</p>
 * 
 * <p>Key responsibilities include:</p>
 * <ul>
 *   <li>Saving test results with detailed performance metrics</li>
 *   <li>Maintaining leaderboards for different difficulties and categories</li>
 *   <li>Providing data for performance analysis and chart generation</li>
 *   <li>Exporting user data for backup and migration purposes</li>
 *   <li>Retrieving historical performance trends and statistics</li>
 * </ul>
 * 
 * <p>The service supports multiple data views:</p>
 * <ul>
 *   <li><strong>WPM Progression</strong> - Historical typing speed trends</li>
 *   <li><strong>Accuracy Progression</strong> - Typing accuracy improvement over time</li>
 *   <li><strong>Leaderboards</strong> - Best performances categorized by difficulty and metric</li>
 *   <li><strong>Overall Statistics</strong> - Aggregate performance summaries</li>
 * </ul>
 * 
 * <p>Data is automatically timestamped and can be exported to JSON format for portability.
 * The service uses Jackson for JSON serialization with proper time formatting.</p>
 * 
 * <p>Thread safety: This class is not thread-safe. External synchronization is required
 * for concurrent access from multiple threads.</p>
 * 
 * @author ArefinSowad
 * @version 1.0
 * @since 1.0
 * @see DatabaseManager
 * @see TestSession.TestResult
 * @see WordProvider.Difficulty
 */
public class StatisticsService {

    /** Database manager instance for handling database connections and operations. */
    private final DatabaseManager dbManager;
    
    /** JSON object mapper configured for time serialization and data export. */
    private final ObjectMapper objectMapper;

    /**
     * Creates a new StatisticsService instance with database and JSON support.
     * 
     * <p>The constructor initializes the database manager and configures JSON serialization
     * with proper Java 8 time support for accurate timestamp handling.</p>
     */
    public StatisticsService() {
        this.dbManager = DatabaseManager.getInstance();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Saves a completed test result to the database with full performance metrics.
     * 
     * <p>This method stores comprehensive test data including WPM calculations, accuracy
     * percentages, character counts, and test metadata. It also automatically updates
     * relevant leaderboards for both speed and accuracy categories.</p>
     * 
     * <p>The saved data includes:</p>
     * <ul>
     *   <li>Game configuration (mode, target value, difficulty)</li>
     *   <li>Performance metrics (net WPM, raw WPM, accuracy)</li>
     *   <li>Character statistics (correct and incorrect counts)</li>
     *   <li>Test duration and completion timestamp</li>
     * </ul>
     * 
     * @param result the test result containing performance metrics
     * @param mode the game mode used (TIME, WORDS, etc.)
     * @param value the target value for the game mode (e.g., 60 seconds, 25 words)
     * @param difficulty the word difficulty level used in the test
     * @param durationSeconds the total test duration in seconds
     * @throws IllegalArgumentException if any parameter is null
     */
    public void saveTestResult(TestSession.TestResult result, GameMode mode, int value,
                               WordProvider.Difficulty difficulty, double durationSeconds) {
        try (Connection conn = dbManager.getConnection()) {
            String sql = """
                    INSERT INTO test_results (game_mode, game_value, difficulty, net_wpm, raw_wpm,
                                            accuracy, correct_chars, incorrect_chars, test_duration_seconds)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, mode.name());
                pstmt.setInt(2, value);
                pstmt.setString(3, difficulty.name());
                pstmt.setInt(4, result.netWPM());
                pstmt.setInt(5, result.rawWPM());
                pstmt.setInt(6, result.accuracy());
                pstmt.setLong(7, result.correctChars());
                pstmt.setLong(8, result.incorrectChars());
                pstmt.setDouble(9, durationSeconds);
                pstmt.executeUpdate();
            }

            // Update leaderboards with new performance data
            updateLeaderboard(difficulty, "SPEED", result.netWPM(), conn);
            updateLeaderboard(difficulty, "ACCURACY", result.accuracy(), conn);

        } catch (SQLException e) {
            System.err.println("Failed to save test result: " + e.getMessage());
        }
    }

    /**
     * Updates the leaderboard with a new score if it represents an improvement.
     * 
     * <p>This method uses an SQL UPSERT operation to either insert a new leaderboard
     * entry or update an existing one if the new score is better than the current
     * best score for the user, difficulty, and category combination.</p>
     * 
     * @param difficulty the word difficulty level
     * @param category the leaderboard category ("SPEED" or "ACCURACY")
     * @param score the score to potentially add to the leaderboard
     * @param conn the database connection to use
     * @throws SQLException if a database error occurs
     */
    private void updateLeaderboard(WordProvider.Difficulty difficulty, String category,
                                   int score, Connection conn) throws SQLException {
        String sql = """
                INSERT INTO leaderboards (user_id, difficulty, category, best_score)
                VALUES (1, ?, ?, ?)
                ON CONFLICT(user_id, difficulty, category) DO UPDATE SET
                    best_score = MAX(best_score, ?),
                    achieved_at = CURRENT_TIMESTAMP
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, difficulty.name());
            pstmt.setString(2, category);
            pstmt.setInt(3, score);
            pstmt.setInt(4, score);
            pstmt.executeUpdate();
        }
    }

    /**
     * Retrieves the total number of completed typing tests for the current user.
     * 
     * @return the total test count, or 0 if no tests exist or an error occurs
     */
    public int getTotalTestCount() {
        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT COUNT(*) FROM test_results WHERE user_id = 1";
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.err.println("Failed to get total test count: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Retrieves the best overall test result based on highest net WPM.
     * 
     * <p>This method finds the test result with the highest net words-per-minute
     * score across all difficulty levels and game modes.</p>
     * 
     * @return the best test result, or null if no results exist or an error occurs
     */
    public TestSession.TestResult getBestResult() {
        try (Connection conn = dbManager.getConnection()) {
            String sql = """
                    SELECT net_wpm, raw_wpm, accuracy, correct_chars, incorrect_chars
                    FROM test_results WHERE user_id = 1
                    ORDER BY net_wpm DESC LIMIT 1
                    """;

            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new TestSession.TestResult(
                            rs.getInt("net_wpm"),
                            rs.getInt("raw_wpm"),
                            rs.getInt("accuracy"),
                            rs.getLong("correct_chars"),
                            rs.getLong("incorrect_chars")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to get best result: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves WPM progression data for chart visualization.
     * 
     * <p>This method returns a chronologically ordered list of WPM data points
     * that can be used to create progression charts and trend analysis. The data
     * is retrieved in reverse chronological order and then reversed to maintain
     * proper time sequence for visualization.</p>
     * 
     * @param limit the maximum number of data points to retrieve
     * @return a list of WPM data points ordered chronologically (oldest first)
     */
    public List<WPMDataPoint> getWPMProgression(int limit) {
        List<WPMDataPoint> dataPoints = new ArrayList<>();

        try (Connection conn = dbManager.getConnection()) {
            String sql = """
                    SELECT net_wpm, completed_at
                    FROM test_results WHERE user_id = 1
                    ORDER BY completed_at DESC LIMIT ?
                    """;

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, limit);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        dataPoints.add(new WPMDataPoint(
                                rs.getTimestamp("completed_at").toLocalDateTime(),
                                rs.getInt("net_wpm")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to get WPM progression: " + e.getMessage());
        }

        // Reverse to get chronological order (oldest first) for proper chart display
        Collections.reverse(dataPoints);
        return dataPoints;
    }

    /**
     * Retrieves accuracy progression data for chart visualization.
     * 
     * <p>This method returns a chronologically ordered list of accuracy data points
     * that can be used to create accuracy progression charts and consistency analysis.
     * Similar to WPM progression, data is retrieved in reverse order and then reversed
     * for proper chronological sequence.</p>
     * 
     * @param limit the maximum number of data points to retrieve
     * @return a list of accuracy data points ordered chronologically (oldest first)
     */
    public List<AccuracyDataPoint> getAccuracyProgression(int limit) {
        List<AccuracyDataPoint> dataPoints = new ArrayList<>();

        try (Connection conn = dbManager.getConnection()) {
            String sql = """
                    SELECT accuracy, completed_at
                    FROM test_results WHERE user_id = 1
                    ORDER BY completed_at DESC LIMIT ?
                    """;

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, limit);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        dataPoints.add(new AccuracyDataPoint(
                                rs.getTimestamp("completed_at").toLocalDateTime(),
                                rs.getInt("accuracy")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to get accuracy progression: " + e.getMessage());
        }

        // Reverse to get chronological order (oldest first) for proper chart display
        Collections.reverse(dataPoints);
        return dataPoints;
    }

    /**
     * Retrieves leaderboard entries for a specific difficulty and category.
     * 
     * <p>This method returns the top performers for a given difficulty level and
     * performance category (SPEED or ACCURACY). The results are ordered by best
     * score in descending order and limited to the top 10 entries.</p>
     * 
     * <p>Each leaderboard entry includes ranking, username, best score, and the
     * timestamp when the score was achieved.</p>
     * 
     * @param difficulty the word difficulty level to filter by
     * @param category the performance category ("SPEED" or "ACCURACY")
     * @return a list of leaderboard entries ordered by rank (best to worst)
     */
    public List<LeaderboardEntry> getLeaderboard(WordProvider.Difficulty difficulty, String category) {
        List<LeaderboardEntry> entries = new ArrayList<>();

        try (Connection conn = dbManager.getConnection()) {
            String sql = """
                    SELECT u.username, l.best_score, l.achieved_at
                    FROM leaderboards l
                    JOIN users u ON l.user_id = u.id
                    WHERE l.difficulty = ? AND l.category = ?
                    ORDER BY l.best_score DESC LIMIT 10
                    """;

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, difficulty.name());
                pstmt.setString(2, category);
                try (ResultSet rs = pstmt.executeQuery()) {
                    int rank = 1;
                    while (rs.next()) {
                        entries.add(new LeaderboardEntry(
                                rank++,
                                rs.getString("username"),
                                rs.getInt("best_score"),
                                rs.getTimestamp("achieved_at").toLocalDateTime()
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to get leaderboard: " + e.getMessage());
        }

        return entries;
    }

    /**
     * Exports all user data to a JSON file for backup or migration purposes.
     * 
     * <p>This method creates a comprehensive backup of the user's typing test data
     * in JSON format. The exported data includes all test results, leaderboard
     * entries, and metadata with proper formatting and timestamps.</p>
     * 
     * <p>The exported JSON structure contains:</p>
     * <ul>
     *   <li>testResults - Complete test history with all metrics</li>
     *   <li>leaderboards - Personal best scores by category and difficulty</li>
     *   <li>exportDate - Timestamp when the export was created</li>
     * </ul>
     * 
     * @param filePath the target file path for the JSON export
     * @throws IOException if file writing fails or path is invalid
     */
    public void exportDataToJSON(String filePath) throws IOException {
        Map<String, Object> exportData = new HashMap<>();

        // Gather all data for export
        exportData.put("testResults", getAllTestResults());
        exportData.put("leaderboards", getAllLeaderboardData());
        exportData.put("exportDate", LocalDateTime.now());

        // Write formatted JSON to file
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(filePath), exportData);
    }

    /**
     * Retrieves all test results for the current user for data export.
     * 
     * <p>This private method fetches complete test history and converts it to
     * a format suitable for JSON serialization. All relevant fields are included
     * to ensure data completeness for backup purposes.</p>
     * 
     * @return a list of maps containing all test result data
     */
    private List<Map<String, Object>> getAllTestResults() {
        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT * FROM test_results WHERE user_id = 1 ORDER BY completed_at";
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("gameMode", rs.getString("game_mode"));
                    result.put("gameValue", rs.getInt("game_value"));
                    result.put("difficulty", rs.getString("difficulty"));
                    result.put("netWPM", rs.getInt("net_wpm"));
                    result.put("rawWPM", rs.getInt("raw_wpm"));
                    result.put("accuracy", rs.getInt("accuracy"));
                    result.put("correctChars", rs.getLong("correct_chars"));
                    result.put("incorrectChars", rs.getLong("incorrect_chars"));
                    result.put("duration", rs.getDouble("test_duration_seconds"));
                    result.put("completedAt", rs.getTimestamp("completed_at").toLocalDateTime());
                    results.add(result);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to get all test results: " + e.getMessage());
        }

        return results;
    }

    /**
     * Retrieves all leaderboard data for the current user for data export.
     * 
     * <p>This private method fetches all personal best scores across different
     * difficulties and categories for inclusion in data exports.</p>
     * 
     * @return a list of maps containing all leaderboard data
     */
    private List<Map<String, Object>> getAllLeaderboardData() {
        List<Map<String, Object>> leaderboardData = new ArrayList<>();

        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT * FROM leaderboards WHERE user_id = 1";
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("difficulty", rs.getString("difficulty"));
                    data.put("category", rs.getString("category"));
                    data.put("bestScore", rs.getInt("best_score"));
                    data.put("achievedAt", rs.getTimestamp("achieved_at").toLocalDateTime());
                    leaderboardData.add(data);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to get leaderboard data: " + e.getMessage());
        }

        return leaderboardData;
    }

    // ========== DATA TRANSFER OBJECTS ==========

    /**
     * Immutable data record representing a WPM measurement at a specific time.
     * 
     * <p>This record is used to transfer WPM data for chart generation and trend analysis.
     * The timestamp is formatted for JSON serialization with a standard pattern.</p>
     * 
     * @param timestamp the date and time when the WPM was recorded
     * @param wpm the words per minute value achieved
     */
    public record WPMDataPoint(
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime timestamp,
            int wpm
    ) {
    }

    /**
     * Immutable data record representing an accuracy measurement at a specific time.
     * 
     * <p>This record is used to transfer accuracy data for chart generation and
     * consistency analysis. The timestamp is formatted for JSON serialization.</p>
     * 
     * @param timestamp the date and time when the accuracy was recorded
     * @param accuracy the accuracy percentage (0-100) achieved
     */
    public record AccuracyDataPoint(
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime timestamp,
            int accuracy
    ) {
    }

    /**
     * Immutable data record representing a leaderboard entry with ranking information.
     * 
     * <p>This record is used to display leaderboard data with complete ranking
     * information including position, user identification, score, and achievement date.</p>
     * 
     * @param rank the position in the leaderboard (1 = first place)
     * @param username the username of the player who achieved the score
     * @param score the best score achieved (WPM or accuracy percentage)
     * @param achievedAt the date and time when the score was first achieved
     */
    public record LeaderboardEntry(
            int rank,
            String username,
            int score,
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime achievedAt
    ) {
    }
}