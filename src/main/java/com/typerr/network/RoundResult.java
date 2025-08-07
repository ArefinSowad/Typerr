package com.typerr.network;

import com.typerr.statics.GameMode;
import com.typerr.TestSession;

/**
 * Data model representing the results of a completed typing test round in multiplayer games.
 * 
 * <p>This class encapsulates all performance metrics and metadata for a single player's
 * typing test session, including speed measurements (WPM), accuracy statistics, character
 * counts, completion status, and game configuration details. It serves as a transfer
 * object for network communication between game clients and servers.</p>
 * 
 * <p>The class supports JSON serialization/deserialization for network transmission
 * and provides multiple constructors for different use cases:</p>
 * <ul>
 *   <li>Default constructor for JSON deserialization</li>
 *   <li>Constructor accepting {@link TestSession.TestResult} for easy conversion</li>
 *   <li>Constructor accepting individual values for maximum flexibility</li>
 * </ul>
 * 
 * <p>Key metrics captured:</p>
 * <ul>
 *   <li>Words per minute (WPM) - typing speed measurement</li>
 *   <li>Accuracy percentage - correctness of typed characters</li>
 *   <li>Character counts - total characters typed and errors made</li>
 *   <li>Completion time - duration of the typing test</li>
 *   <li>Game context - mode, difficulty, and target values</li>
 * </ul>
 * 
 * @author ArefinSowad
 * @version 1.0
 * @since 1.0
 * @see TestSession.TestResult
 * @see GameMode
 */
public class RoundResult {
    /** Unique identifier for the player who completed this round. */
    private String playerId;
    
    /** Words per minute (WPM) achieved during the typing test. */
    private int wpm;
    
    /** Accuracy percentage (0-100) representing correct vs. total characters typed. */
    private int accuracy;
    
    /** Total number of characters typed during the test (correct + incorrect). */
    private long totalCharacters;
    
    /** Number of incorrectly typed characters (errors/mistakes). */
    private long errorCount;
    
    /** Total time taken to complete the test, in seconds. */
    private double completionTimeSeconds;
    
    /** Whether the test was completed successfully or abandoned early. */
    private boolean completed;
    
    /** The game mode used for this test (TIME, WORDS, etc.). */
    private GameMode gameMode;
    
    /** The target value for the game mode (e.g., 60 seconds, 25 words). */
    private int gameValue;

    /**
     * Default constructor for JSON deserialization and framework use.
     * Creates an empty RoundResult with default values.
     */
    public RoundResult() {}

    /**
     * Creates a RoundResult from a completed test session and metadata.
     * 
     * <p>This constructor is the primary way to create RoundResult objects
     * from actual typing test sessions. It automatically extracts performance
     * metrics from the TestResult and combines them with game metadata.</p>
     * 
     * @param playerId the unique identifier for the player
     * @param testResult the completed test session results containing performance metrics
     * @param completionTimeSeconds the total time taken for the test in seconds
     * @param completed whether the test was completed successfully
     * @param gameMode the game mode used (TIME, WORDS, etc.)
     * @param gameValue the target value for the game mode
     * @throws IllegalArgumentException if any required parameter is null
     */
    public RoundResult(String playerId, TestSession.TestResult testResult,
                      double completionTimeSeconds, boolean completed,
                      GameMode gameMode, int gameValue) {
        this.playerId = playerId;
        this.wpm = testResult.netWPM();
        this.accuracy = testResult.accuracy();
        this.totalCharacters = testResult.correctChars() + testResult.incorrectChars();
        this.errorCount = testResult.incorrectChars();
        this.completionTimeSeconds = completionTimeSeconds;
        this.completed = completed;
        this.gameMode = gameMode;
        this.gameValue = gameValue;
    }

    /**
     * Creates a RoundResult with explicit values for all fields.
     * 
     * <p>This constructor provides maximum flexibility for creating RoundResult
     * objects when the data comes from external sources or manual calculations
     * rather than from a TestSession.</p>
     * 
     * @param playerId the unique identifier for the player
     * @param wpm the words per minute achieved
     * @param accuracy the accuracy percentage (0-100)
     * @param totalCharacters the total number of characters typed
     * @param errorCount the number of incorrectly typed characters
     * @param completionTimeSeconds the total time taken in seconds
     * @param completed whether the test was completed successfully
     * @param gameMode the game mode used
     * @param gameValue the target value for the game mode
     */
    public RoundResult(String playerId, int wpm, int accuracy, long totalCharacters,
                      long errorCount, double completionTimeSeconds, boolean completed,
                      GameMode gameMode, int gameValue) {
        this.playerId = playerId;
        this.wpm = wpm;
        this.accuracy = accuracy;
        this.totalCharacters = totalCharacters;
        this.errorCount = errorCount;
        this.completionTimeSeconds = completionTimeSeconds;
        this.completed = completed;
        this.gameMode = gameMode;
        this.gameValue = gameValue;
    }

    // ========== GETTER AND SETTER METHODS ==========
    
    /**
     * Gets the unique player identifier.
     * 
     * @return the player ID string
     */
    public String getPlayerId() { return playerId; }
    
    /**
     * Sets the unique player identifier.
     * 
     * @param playerId the player ID to set
     */
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    /**
     * Gets the words per minute (WPM) achieved during the test.
     * 
     * @return the WPM value
     */
    public int getWpm() { return wpm; }
    
    /**
     * Sets the words per minute value.
     * 
     * @param wpm the WPM to set
     */
    public void setWpm(int wpm) { this.wpm = wpm; }

    /**
     * Gets the accuracy percentage (0-100).
     * 
     * @return the accuracy percentage
     */
    public int getAccuracy() { return accuracy; }
    
    /**
     * Sets the accuracy percentage.
     * 
     * @param accuracy the accuracy percentage to set (should be 0-100)
     */
    public void setAccuracy(int accuracy) { this.accuracy = accuracy; }

    /**
     * Gets the total number of characters typed during the test.
     * 
     * @return the total character count
     */
    public long getTotalCharacters() { return totalCharacters; }
    
    /**
     * Sets the total number of characters typed.
     * 
     * @param totalCharacters the total character count to set
     */
    public void setTotalCharacters(long totalCharacters) { this.totalCharacters = totalCharacters; }

    /**
     * Gets the number of incorrectly typed characters (errors).
     * 
     * @return the error count
     */
    public long getErrorCount() { return errorCount; }
    
    /**
     * Sets the number of incorrectly typed characters.
     * 
     * @param errorCount the error count to set
     */
    public void setErrorCount(long errorCount) { this.errorCount = errorCount; }

    /**
     * Gets the completion time in seconds.
     * 
     * @return the completion time in seconds
     */
    public double getCompletionTimeSeconds() { return completionTimeSeconds; }
    
    /**
     * Sets the completion time in seconds.
     * 
     * @param completionTimeSeconds the completion time to set
     */
    public void setCompletionTimeSeconds(double completionTimeSeconds) { this.completionTimeSeconds = completionTimeSeconds; }

    /**
     * Checks whether the test was completed successfully.
     * 
     * @return true if the test was completed, false if abandoned early
     */
    public boolean isCompleted() { return completed; }
    
    /**
     * Sets the completion status of the test.
     * 
     * @param completed true if completed successfully, false if abandoned
     */
    public void setCompleted(boolean completed) { this.completed = completed; }

    /**
     * Gets the game mode used for this test.
     * 
     * @return the game mode (TIME, WORDS, etc.)
     */
    public GameMode getGameMode() { return gameMode; }
    
    /**
     * Sets the game mode for this test.
     * 
     * @param gameMode the game mode to set
     */
    public void setGameMode(GameMode gameMode) { this.gameMode = gameMode; }

    /**
     * Gets the target value for the game mode.
     * 
     * @return the game value (e.g., 60 for 60-second test, 25 for 25-word test)
     */
    public int getGameValue() { return gameValue; }
    
    /**
     * Sets the target value for the game mode.
     * 
     * @param gameValue the game value to set
     */
    public void setGameValue(int gameValue) { this.gameValue = gameValue; }

    /**
     * Returns a human-readable string representation of this round result.
     * 
     * <p>The string includes key performance metrics and test metadata in a
     * concise format suitable for logging, debugging, and display purposes.</p>
     * 
     * @return a formatted string containing player ID, WPM, accuracy, character counts,
     *         completion time, and completion status
     */
    @Override
    public String toString() {
        return String.format("RoundResult{playerId='%s', wpm=%d, accuracy=%d%%, " +
                           "chars=%d, errors=%d, time=%.1fs, completed=%s}",
                           playerId, wpm, accuracy, totalCharacters, errorCount,
                           completionTimeSeconds, completed);
    }
}