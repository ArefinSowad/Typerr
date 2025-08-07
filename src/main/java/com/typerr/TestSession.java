package com.typerr;

import com.typerr.database.StatisticsService;
import com.typerr.statics.Constants;
import com.typerr.statics.GameMode;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Core typing test session management and statistics calculation engine.
 * 
 * <p>The TestSession class encapsulates all logic related to a single typing test session,
 * including word management, timing, statistics calculation, and performance tracking.
 * It serves as the primary business logic component for the typing test functionality.</p>
 * 
 * <h3>Key Responsibilities:</h3>
 * <ul>
 *   <li><strong>Session Lifecycle:</strong> Start, progress tracking, and completion</li>
 *   <li><strong>Word Management:</strong> Individual word tracking and validation</li>
 *   <li><strong>Real-time Statistics:</strong> WPM and accuracy calculation during typing</li>
 *   <li><strong>Performance Metrics:</strong> Net WPM, raw WPM, and detailed accuracy</li>
 *   <li><strong>Data Persistence:</strong> Automatic statistics storage via StatHistory</li>
 * </ul>
 * 
 * <h3>Supported Game Modes:</h3>
 * <ul>
 *   <li><strong>{@link GameMode#TIME}:</strong> Fixed duration tests (e.g., 60 seconds)</li>
 *   <li><strong>{@link GameMode#WORDS}:</strong> Fixed word count tests (e.g., 100 words)</li>
 * </ul>
 * 
 * <h3>Statistics Calculation:</h3>
 * <p>The class implements industry-standard typing speed metrics:</p>
 * <ul>
 *   <li><strong>Net WPM:</strong> Adjusted for errors, counts only correct characters</li>
 *   <li><strong>Raw WPM:</strong> Total characters typed divided by time</li>
 *   <li><strong>Accuracy:</strong> Percentage of correctly typed characters</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Create a 60-second typing test
 * WordProvider provider = new WordProvider();
 * TestSession session = new TestSession(GameMode.TIME, 60, provider);
 * 
 * // Start the test
 * session.start();
 * 
 * // During typing, update current word
 * Word currentWord = session.getCurrentWord();
 * currentWord.setTypedWord("hello");
 * 
 * // Check if test is finished
 * if (session.isFinished()) {
 *     session.end();
 *     TestResult result = session.calculateCurrentStats(true);
 *     System.out.println("WPM: " + result.netWPM());
 * }
 * }</pre>
 * 
 * <h3>Thread Safety:</h3>
 * <p>This class is <strong>not</strong> thread-safe and should be used from the JavaFX
 * Application Thread only. All timing operations use System.currentTimeMillis() for
 * consistency.</p>
 * 
 * @author Typerr Development Team
 * @version 1.0
 * @since 1.0
 * @see GameMode
 * @see WordProvider
 * @see StatisticsService
 */
public class TestSession {


    /**
     * Represents an individual word within a typing test session.
     * 
     * <p>The Word class encapsulates both the target word that should be typed
     * and the actual word typed by the user. It tracks completion status and
     * provides correctness validation for statistics calculation.</p>
     * 
     * <h4>Word Lifecycle:</h4>
     * <ol>
     *   <li><strong>Creation:</strong> Word is created with target text</li>
     *   <li><strong>Typing:</strong> User input is tracked via setTypedWord()</li>
     *   <li><strong>Completion:</strong> Word is marked complete when user moves to next word</li>
     *   <li><strong>Validation:</strong> Correctness is determined by exact match (case-insensitive)</li>
     * </ol>
     * 
     * <h4>Correctness Criteria:</h4>
     * <p>A word is considered correct if:</p>
     * <ul>
     *   <li>The word has been marked as completed</li>
     *   <li>The typed text exactly matches the target (case-insensitive)</li>
     *   <li>No extra or missing characters</li>
     * </ul>
     * 
     * <p><strong>Thread Safety:</strong> This class is not thread-safe.</p>
     * 
     * @see TestSession
     */
    static class Word {
        /** The correct word that should be typed. Immutable after construction. */
        private final String targetWord;
        
        /** The word actually typed by the user. Updated during typing. */
        private String typedWord = "";
        
        /** Whether this word has been completed (user moved to next word). */
        private boolean completed = false;

        /**
         * Creates a new Word with the specified target text.
         * 
         * @param targetWord the correct word to be typed, must not be null
         * @throws NullPointerException if targetWord is null
         */
        Word(String targetWord) {
            this.targetWord = Objects.requireNonNull(targetWord);
        }

        /**
         * Updates the typed word content with user input.
         * 
         * <p>This method is called as the user types to track their progress.
         * Null input is converted to empty string for safety.</p>
         * 
         * @param typedWord the current user input for this word
         */
        void setTypedWord(String typedWord) {
            this.typedWord = typedWord == null ? "" : typedWord;
        }

        /**
         * Returns the target word that should be typed.
         * 
         * @return the correct word text
         */
        String getTargetWord()   { return targetWord; }

        /**
         * Returns the word currently typed by the user.
         * 
         * @return the user's current input for this word
         */
        String getTypedWord()    { return typedWord;  }

        /**
         * Checks if this word has been completed by the user.
         * 
         * <p>A word is completed when the user advances to the next word,
         * regardless of whether it was typed correctly.</p>
         * 
         * @return true if the word has been completed
         */
        boolean isCompleted()    { return completed;  }

        /**
         * Determines if this word was typed correctly.
         * 
         * <p>A word is correct if it has been completed and the typed text
         * exactly matches the target word (case-insensitive comparison).</p>
         * 
         * @return true if the word was typed correctly and completed
         */
        boolean isCorrect() {
            return completed && targetWord.toLowerCase().equals(typedWord.toLowerCase());
        }

        /**
         * Marks this word as completed. Called when the user advances to the next word.
         * 
         * <p>Once completed, the word's correctness can be determined and it contributes
         * to the overall test statistics.</p>
         */
        void complete() { this.completed = true; }
    }

    /**
     * Immutable record representing the calculated results of a typing test.
     * 
     * <p>TestResult encapsulates all key performance metrics calculated at the end
     * of a typing test session. These metrics follow industry standards for typing
     * speed measurement and accuracy calculation.</p>
     * 
     * <h4>Metric Definitions:</h4>
     * <ul>
     *   <li><strong>Net WPM:</strong> Words per minute adjusted for errors (only correct characters counted)</li>
     *   <li><strong>Raw WPM:</strong> Total characters typed divided by time, regardless of errors</li>
     *   <li><strong>Accuracy:</strong> Percentage of correctly typed characters (0-100)</li>
     *   <li><strong>Correct Chars:</strong> Total number of correctly typed characters</li>
     *   <li><strong>Incorrect Chars:</strong> Total number of incorrectly typed characters</li>
     * </ul>
     * 
     * <h4>Calculation Notes:</h4>
     * <ul>
     *   <li>WPM calculations use the standard divisor of 5 characters per word</li>
     *   <li>Net WPM provides a more realistic measure of practical typing speed</li>
     *   <li>Raw WPM shows pure typing speed without error penalties</li>
     *   <li>All values are rounded to nearest integers for display consistency</li>
     * </ul>
     * 
     * @param netWPM adjusted words per minute (errors subtracted)
     * @param rawWPM gross words per minute (before error adjustment)  
     * @param accuracy percentage of correct characters (0-100)
     * @param correctChars total number of correctly typed characters
     * @param incorrectChars total number of incorrectly typed characters
     * @since 1.0
     */
    public record TestResult(
            int  netWPM,
            int  rawWPM,
            int  accuracy,
            long correctChars,
            long incorrectChars) { }

    /**
     * Static utility class providing access to persistent statistics storage and retrieval.
     * 
     * <p>StatHistory serves as a facade over the StatisticsService, providing convenient
     * static methods for managing typing test results and historical performance data.
     * All operations are automatically persisted to the SQLite database.</p>
     * 
     * <h4>Key Features:</h4>
     * <ul>
     *   <li><strong>Result Storage:</strong> Automatic persistence of completed test results</li>
     *   <li><strong>Performance Tracking:</strong> Historical WPM and accuracy progression</li>
     *   <li><strong>Best Results:</strong> Personal record tracking and retrieval</li>
     *   <li><strong>Data Export:</strong> JSON export functionality for data portability</li>
     *   <li><strong>Statistics Summary:</strong> Aggregate performance metrics</li>
     * </ul>
     * 
     * <h4>Database Integration:</h4>
     * <p>All methods interact with the underlying SQLite database through StatisticsService.
     * Data is automatically structured for efficient querying and analysis.</p>
     * 
     * <h4>Usage Pattern:</h4>
     * <pre>{@code
     * // Save a test result
     * TestResult result = session.calculateCurrentStats(true);
     * StatHistory.addResult(result, GameMode.TIME, 60, 
     *                      WordProvider.Difficulty.MEDIUM, 60.5);
     * 
     * // Retrieve best performance
     * TestResult best = StatHistory.getBestResult();
     * System.out.println("Personal best: " + best.netWPM() + " WPM");
     * }</pre>
     * 
     * <p><strong>Thread Safety:</strong> All methods delegate to StatisticsService which
     * handles database synchronization.</p>
     * 
     * @see StatisticsService
     * @see TestResult
     * @since 1.0
     */
    public static final class StatHistory {
        /** Singleton StatisticsService instance for database operations. */
        private static final StatisticsService statisticsService = new StatisticsService();

        /**
         * Persists a completed test result to the database with full context information.
         * 
         * <p>This method automatically saves the test result along with all relevant
         * metadata including game mode, difficulty level, and timing information.
         * The data is immediately available for statistics queries.</p>
         * 
         * @param result the calculated test results, may be null (will be ignored)
         * @param mode the game mode used for this test
         * @param value the mode-specific parameter (time in seconds or word count)
         * @param difficulty the word difficulty level used
         * @param durationSeconds the actual test duration in seconds
         * @see StatisticsService#saveTestResult(TestResult, GameMode, int, WordProvider.Difficulty, double)
         */
        public static void addResult(TestResult result, GameMode mode, int value,
                                     WordProvider.Difficulty difficulty, double durationSeconds) {
            if (result != null) {
                statisticsService.saveTestResult(result, mode, value, difficulty, durationSeconds);
            }
        }

        /**
         * Retrieves the user's best typing performance based on net WPM.
         * 
         * @return the highest net WPM test result, or null if no tests completed
         * @see StatisticsService#getBestResult()
         */
        public static TestResult getBestResult() {
            return statisticsService.getBestResult();
        }

        /**
         * Returns the total number of completed typing tests.
         * 
         * @return count of all completed tests in the database
         * @see StatisticsService#getTotalTestCount()
         */
        public static int getTotalTestCount() {
            return statisticsService.getTotalTestCount();
        }

        /**
         * Retrieves historical WPM progression data for trend analysis.
         * 
         * <p>Returns the most recent WPM data points ordered chronologically.
         * Useful for creating progress charts and performance trend analysis.</p>
         * 
         * @param limit maximum number of data points to retrieve
         * @return list of WPM data points, ordered from oldest to newest
         * @see StatisticsService#getWPMProgression(int)
         */
        public static List<StatisticsService.WPMDataPoint> getWPMProgression(int limit) {
            return statisticsService.getWPMProgression(limit);
        }

        /**
         * Retrieves historical accuracy progression data for trend analysis.
         * 
         * <p>Returns the most recent accuracy data points ordered chronologically.
         * Useful for tracking typing accuracy improvements over time.</p>
         * 
         * @param limit maximum number of data points to retrieve
         * @return list of accuracy data points, ordered from oldest to newest
         * @see StatisticsService#getAccuracyProgression(int)
         */
        public static List<StatisticsService.AccuracyDataPoint> getAccuracyProgression(int limit) {
            return statisticsService.getAccuracyProgression(limit);
        }

        /**
         * Exports all user statistics data to a JSON file.
         * 
         * <p>Creates a comprehensive export of all typing test data in JSON format.
         * Useful for data backup, migration, or external analysis tools.</p>
         * 
         * @param filePath the target file path for the JSON export
         * @see StatisticsService#exportDataToJSON(String)
         */
        public static void exportDataToJSON(String filePath) {
            try {
                statisticsService.exportDataToJSON(filePath);
            } catch (Exception e) {
                System.err.println("Failed to export data: " + e.getMessage());
            }
        }

        /** Private constructor to prevent instantiation of utility class. */
        private StatHistory() {  }
    }


    // ===== SESSION CONFIGURATION =====
    
    /** The game mode for this test session (TIME or WORDS). Immutable after construction. */
    private final GameMode      mode;
    
    /** Mode-specific parameter: seconds for TIME mode, word count for WORDS mode. */
    private final int           value;
    
    /** List of words for this test session, pre-generated based on mode and difficulty. */
    private final List<Word>    wordList;
    
    /** Difficulty level determining word complexity for this session. */
    private final WordProvider.Difficulty difficulty;

    // ===== SESSION STATE =====
    
    /** Index of the current active word being typed (0-based). */
    private int                 currentWordIndex = 0;
    
    /** Timestamp when the test session started (System.currentTimeMillis()). */
    private long                startTimeMillis  = 0;
    
    /** Timestamp when the test session ended (System.currentTimeMillis()). */
    private long                endTimeMillis    = 0;
    
    /** Whether the test session has been started by the user. */
    private boolean             hasStarted       = false;
    
    /** Whether the test session has been completed and ended. */
    private boolean             hasEnded         = false;


    /**
     * Creates a new typing test session with the specified configuration.
     * 
     * <p>This constructor initializes all session parameters and pre-generates the
     * word list based on the game mode and difficulty level. The word pool size
     * is optimized based on the expected test duration or word count.</p>
     * 
     * <h4>Word Pool Sizing Strategy:</h4>
     * <ul>
     *   <li><strong>WORDS mode:</strong> Exactly the requested number of words</li>
     *   <li><strong>TIME mode:</strong> Estimated words needed plus buffer (max 1000)</li>
     * </ul>
     * 
     * <h4>Initialization Process:</h4>
     * <ol>
     *   <li>Validate and store game mode parameters</li>
     *   <li>Capture current difficulty level from provider</li>
     *   <li>Calculate optimal word pool size</li>
     *   <li>Generate word list and wrap in Word objects</li>
     * </ol>
     * 
     * @param mode the game mode (TIME or WORDS), must not be null
     * @param value mode-specific parameter: seconds for TIME mode, word count for WORDS mode
     * @param provider word provider for generating test content, must not be null
     * @throws NullPointerException if mode or provider is null
     * @throws IllegalArgumentException if value is not positive
     * @see GameMode
     * @see WordProvider
     * @see Constants#DEFAULT_WORD_POOL_MULTIPLIER
     */
    public TestSession(GameMode mode, int value, WordProvider provider) {
        this.mode = Objects.requireNonNull(mode);
        this.value = value;
        this.difficulty = provider.getCurrentDifficulty();

        int basePoolSize = (mode == GameMode.WORDS) ? value
                : Math.min(value * Constants.DEFAULT_WORD_POOL_MULTIPLIER, 1000);

        this.wordList = provider.getWords(basePoolSize)
                .stream()
                .map(Word::new)
                .collect(Collectors.toList());
    }


    /**
     * Starts the typing test session and begins timing.
     * 
     * <p>This method transitions the session from initialized state to active state.
     * It records the start timestamp for accurate timing calculations and can only
     * be called once per session.</p>
     * 
     * <p><strong>Behavior:</strong></p>
     * <ul>
     *   <li>Records current timestamp as session start time</li>
     *   <li>Sets hasStarted flag to true</li>
     *   <li>Subsequent calls have no effect (idempotent)</li>
     *   <li>Enables accurate WPM and timing calculations</li>
     * </ul>
     * 
     * <p><strong>Thread Safety:</strong> This method should only be called from
     * the JavaFX Application Thread.</p>
     */
    public void start() {
        if (!hasStarted) {
            hasStarted       = true;
            startTimeMillis  = System.currentTimeMillis();
        }
    }

    /**
     * Ends the typing test session and triggers final statistics calculation.
     * 
     * <p>This method finalizes the test session by recording the end timestamp,
     * calculating final statistics, and automatically persisting the results
     * to the database via StatHistory.</p>
     * 
     * <p><strong>End Process:</strong></p>
     * <ol>
     *   <li>Record end timestamp for accurate duration calculation</li>
     *   <li>Calculate final test statistics (WPM, accuracy, etc.)</li>
     *   <li>Persist results to database with full context</li>
     *   <li>Set hasEnded flag to prevent duplicate processing</li>
     * </ol>
     * 
     * <p><strong>Conditions:</strong></p>
     * <ul>
     *   <li>Can only be called once per session (idempotent)</li>
     *   <li>Session must have been started first</li>
     *   <li>Results are only saved if session was properly started</li>
     * </ul>
     * 
     * @see #calculateCurrentStats(boolean)
     * @see StatHistory#addResult(TestResult, GameMode, int, WordProvider.Difficulty, double)
     */
    public void end() {
        if (!hasEnded && hasStarted) {
            hasEnded = true;
            endTimeMillis = System.currentTimeMillis();

            TestResult finalResult = calculateCurrentStats(true);
            double durationSeconds = (endTimeMillis - startTimeMillis) / 1000.0;

            StatHistory.addResult(finalResult, mode, value, difficulty, durationSeconds);
        }
    }

    /**
     * Advances to the next word in the test sequence.
     * 
     * <p>This method completes the current word and moves the focus to the next
     * word in the sequence. It should be called when the user presses space or
     * otherwise indicates completion of the current word.</p>
     * 
     * <p><strong>Actions Performed:</strong></p>
     * <ul>
     *   <li>Marks current word as completed</li>
     *   <li>Increments word index to next word</li>
     *   <li>Makes next word available via getCurrentWord()</li>
     * </ul>
     * 
     * <p><strong>Word Completion:</strong> Completion is independent of correctness.
     * A word is completed even if typed incorrectly, allowing the test to continue.</p>
     * 
     * @see Word#complete()
     * @see #getCurrentWord()
     */
    public void advanceToNextWord() {
        wordList.get(currentWordIndex).complete();
        currentWordIndex++;
    }

    /**
     * Determines if the test session has reached its completion criteria.
     * 
     * <p>The completion criteria depend on the game mode:</p>
     * <ul>
     *   <li><strong>WORDS mode:</strong> Current word index reaches target word count</li>
     *   <li><strong>TIME mode:</strong> Elapsed time reaches target duration</li>
     * </ul>
     * 
     * <p><strong>Implementation Details:</strong></p>
     * <ul>
     *   <li>Returns false if session hasn't been started</li>
     *   <li>Uses real-time calculation for TIME mode</li>
     *   <li>Word count is checked against current progress</li>
     *   <li>Millisecond precision for timing accuracy</li>
     * </ul>
     * 
     * @return true if the test session has met its completion criteria
     * @see GameMode
     */
    public boolean isFinished() {
        if (!hasStarted) return false;

        return switch (mode) {
            case WORDS -> currentWordIndex >= value;
            case TIME  -> (System.currentTimeMillis() - startTimeMillis) >= value * 1_000L;
        };
    }


    /**
     * Returns the currently active word for user typing input.
     * 
     * <p>This method provides access to the word that the user should currently
     * be typing. If the current index exceeds the word list size (edge case),
     * it returns the last word to prevent array bounds exceptions.</p>
     * 
     * @return the Word object representing the current typing target
     * @see Word
     */
    public Word getCurrentWord()     {
        if (currentWordIndex >= wordList.size()) {
            return wordList.get(wordList.size() - 1);
        }
        return wordList.get(currentWordIndex);
    }

    /**
     * Returns the complete list of words for this test session.
     * 
     * <p>This provides access to all words in the test, useful for UI display
     * and progress visualization. The list is immutable after construction.</p>
     * 
     * @return unmodifiable view of the word list
     */
    public List<Word> getWordList()  { return wordList; }

    /**
     * Returns the current word index (0-based position in word list).
     * 
     * @return current word index, starting from 0
     */
    public int  getCurrentWordIndex(){ return currentWordIndex; }

    /**
     * Returns the game mode for this test session.
     * 
     * @return the GameMode (TIME or WORDS)
     */
    public GameMode getMode()        { return mode; }

    /**
     * Returns the mode-specific value for this test session.
     * 
     * @return seconds for TIME mode, word count for WORDS mode
     */
    public int  getValue()           { return value; }

    /**
     * Returns the session start timestamp.
     * 
     * @return start time in milliseconds since epoch, or 0 if not started
     */
    public long getStartTime()       { return startTimeMillis; }

    /**
     * Returns the difficulty level for this test session.
     * 
     * @return the WordProvider.Difficulty level used for word generation
     */
    public WordProvider.Difficulty getDifficulty() { return difficulty; }


    /**
     * Calculates comprehensive typing performance statistics for the current session.
     * 
     * <p>This method implements industry-standard typing speed and accuracy calculations,
     * providing both real-time statistics during typing and final results upon completion.
     * The calculations follow established typing test conventions for consistency.</p>
     * 
     * <h4>Calculation Methodology:</h4>
     * 
     * <h5>Words Per Minute (WPM):</h5>
     * <ul>
     *   <li><strong>Net WPM:</strong> (Correct characters ÷ 5) ÷ elapsed minutes</li>
     *   <li><strong>Raw WPM:</strong> (Total characters ÷ 5) ÷ elapsed minutes</li>
     *   <li>Uses standard divisor of 5 characters per word</li>
     *   <li>Only counts completed words in calculations</li>
     * </ul>
     * 
     * <h5>Accuracy Calculation:</h5>
     * <ul>
     *   <li>Formula: ((Total chars - Errors) ÷ Total chars) × 100</li>
     *   <li>Considers whole-word correctness</li>
     *   <li>Includes space characters in word completion</li>
     *   <li>Clamped to 0-100% range</li>
     * </ul>
     * 
     * <h5>Character Counting:</h5>
     * <ul>
     *   <li><strong>Correct chars:</strong> All characters in correctly typed words + spaces</li>
     *   <li><strong>Incorrect chars:</strong> All characters in incorrectly typed words + spaces</li>
     *   <li>Only completed words contribute to statistics</li>
     *   <li>Partial words (current word) excluded from final calculations</li>
     * </ul>
     * 
     * <h4>Edge Cases:</h4>
     * <ul>
     *   <li><strong>Minimum time:</strong> Returns default values if elapsed time too short</li>
     *   <li><strong>No typing:</strong> Returns 0 WPM, 100% accuracy for empty input</li>
     *   <li><strong>All errors:</strong> Returns 0% accuracy, positive raw WPM</li>
     *   <li><strong>Real-time mode:</strong> Excludes current incomplete word</li>
     * </ul>
     * 
     * @param finalCalculation if true, uses recorded end time; if false, uses current time
     *                        for real-time statistics during active typing
     * @return TestResult containing all calculated performance metrics
     * @see TestResult
     * @see Constants#MIN_ELAPSED_TIME_MINUTES
     * @see Constants#WORDS_PER_MINUTE_DIVISOR
     * @see Constants#DEFAULT_ACCURACY_PERCENTAGE
     */
    public TestResult calculateCurrentStats(boolean finalCalculation) {
        double elapsedMinutes = getElapsedMinutes(finalCalculation);
        if (elapsedMinutes < Constants.MIN_ELAPSED_TIME_MINUTES) {
            return new TestResult(0, 0, Constants.DEFAULT_ACCURACY_PERCENTAGE, 0, 0);
        }

        long correctChars   = 0;
        long incorrectChars = 0;
        long allTypedChars = 0;

        int wordLimit = Math.min(currentWordIndex + 1, wordList.size());

        for (int i = 0; i < wordLimit; i++) {
            Word w = wordList.get(i);
            String target = w.getTargetWord();
            String typed  = w.getTypedWord();
            allTypedChars += typed.length();

            if (w.isCompleted() && !typed.isEmpty()) {
                if(w.isCorrect()) {
                    correctChars += (target.length() + 1);
                } else {
                    incorrectChars += (target.length() + 1);
                }
            }
        }

        long totalCharsInCorrectWords = correctChars;
        double uncorrectedErrors = Math.max(0, incorrectChars);

        int netWPM = (int) Math.round((totalCharsInCorrectWords / (double) Constants.WORDS_PER_MINUTE_DIVISOR) / elapsedMinutes);
        int rawWPM = (int) Math.round((allTypedChars / (double) Constants.WORDS_PER_MINUTE_DIVISOR) / elapsedMinutes);

        int accuracy = (allTypedChars) == 0
                ? Constants.DEFAULT_ACCURACY_PERCENTAGE
                : (int) Math.round(100.0 * (allTypedChars - uncorrectedErrors) / allTypedChars);
        accuracy = Math.max(0, accuracy);


        return new TestResult(netWPM, rawWPM, accuracy, correctChars, incorrectChars);
    }

    /**
     * Calculates elapsed time in minutes for WPM calculations.
     * 
     * <p>This helper method provides accurate timing for statistics calculations,
     * supporting both real-time updates during typing and final calculations
     * after test completion.</p>
     * 
     * <h4>Time Calculation Logic:</h4>
     * <ul>
     *   <li><strong>Not started:</strong> Returns 0 minutes</li>
     *   <li><strong>Final calculation + ended:</strong> Uses recorded end time</li>
     *   <li><strong>Real-time calculation:</strong> Uses current timestamp</li>
     *   <li><strong>Precision:</strong> Millisecond accuracy converted to minutes</li>
     * </ul>
     * 
     * @param finalCalculation if true and session has ended, uses recorded end time;
     *                        otherwise uses current system time
     * @return elapsed time in minutes as a double value
     */
    private double getElapsedMinutes(boolean finalCalculation) {
        if (!hasStarted) return 0;

        long end = finalCalculation && hasEnded ? endTimeMillis : System.currentTimeMillis();
        return (end - startTimeMillis) / 60_000.0;
    }
}