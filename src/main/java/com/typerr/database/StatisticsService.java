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

public class StatisticsService {

    private final DatabaseManager dbManager;

    private final ObjectMapper objectMapper;

    public StatisticsService() {
        this.dbManager = DatabaseManager.getInstance();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

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

            updateLeaderboard(difficulty, "SPEED", result.netWPM(), conn);
            updateLeaderboard(difficulty, "ACCURACY", result.accuracy(), conn);

        } catch (SQLException e) {
            System.err.println("Failed to save test result: " + e.getMessage());
        }
    }

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

        Collections.reverse(dataPoints);
        return dataPoints;
    }

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

        Collections.reverse(dataPoints);
        return dataPoints;
    }

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

    public void exportDataToJSON(String filePath) throws IOException {
        Map<String, Object> exportData = new HashMap<>();

        exportData.put("testResults", getAllTestResults());
        exportData.put("leaderboards", getAllLeaderboardData());
        exportData.put("exportDate", LocalDateTime.now());

        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(filePath), exportData);
    }

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

    public record WPMDataPoint(
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime timestamp,
            int wpm
    ) {
    }

    public record AccuracyDataPoint(
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime timestamp,
            int accuracy
    ) {
    }

    public record LeaderboardEntry(
            int rank,
            String username,
            int score,
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime achievedAt
    ) {
    }
}