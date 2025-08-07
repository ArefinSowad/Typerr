package com.typerr.statics;

/**
 * Application-wide constants and configuration values for the Typerr typing test application.
 * 
 * <p>This utility class contains all constant values used throughout the application,
 * including UI dimensions, timing parameters, file paths, database configuration,
 * and performance calculation constants. The class is designed to be immutable
 * and cannot be instantiated.</p>
 * 
 * <p>Constants are organized into logical groups:</p>
 * <ul>
 *   <li>Application and UI settings (window dimensions, fonts, animations)</li>
 *   <li>Typing test parameters (word pool multipliers, accuracy calculations)</li>
 *   <li>Resource file paths (words, icons, stylesheets)</li>
 *   <li>Database configuration (paths, versioning)</li>
 *   <li>Chart and statistics settings (data limits, time ranges)</li>
 * </ul>
 * 
 * @author ArefinSowad
 * @version 1.0
 * @since 1.0
 */
public final class Constants {

    // ========== APPLICATION SETTINGS ==========
    
    /** The display name of the application. */
    public static final String APP_NAME = "Typerr";

    /** Default width of the application window in pixels. */
    public static final int DEFAULT_WINDOW_WIDTH = 950;
    
    /** Default height of the application window in pixels. */
    public static final int DEFAULT_WINDOW_HEIGHT = 600;
    
    /** Font family used for displaying words during typing tests. */
    public static final String WORD_FONT_FAMILY = "Roboto Mono";

    // ========== UI ANIMATION AND TIMING ==========
    
    /** Interval in milliseconds for the typing cursor blink animation. */
    public static final int CARET_BLINK_INTERVAL = 500;
    
    /** Duration in milliseconds for fade transition animations. */
    public static final int FADE_TRANSITION_DURATION = 150;

    // ========== TYPING TEST CONFIGURATION ==========
    
    /** 
     * Multiplier for word pool size relative to the target word count.
     * Ensures sufficient words are available for longer tests.
     */
    public static final int DEFAULT_WORD_POOL_MULTIPLIER = 3;
    
    /** 
     * Minimum elapsed time in minutes to prevent division by zero in WPM calculations.
     * Used as a safety threshold for very short typing sessions.
     */
    public static final double MIN_ELAPSED_TIME_MINUTES = 1e-4;
    
    /** 
     * Standard divisor for calculating words per minute.
     * Based on the standard definition of 5 characters = 1 word.
     */
    public static final int WORDS_PER_MINUTE_DIVISOR = 5;
    
    /** Default accuracy percentage for perfect typing (100%). */
    public static final int DEFAULT_ACCURACY_PERCENTAGE = 100;

    /** Maximum number of words to render simultaneously in the UI for performance. */
    public static final int MAX_WORDS_RENDERED = 25;

    // ========== RESOURCE PATHS ==========
    
    /** Path to the application icon resource file. */
    public static final String ICON_PATH = "/images/icon.png";

    /** Path to the default word list file containing common words. */
    public static final String DEFAULT_WORD_FILE = "/Words/words.txt";
    
    /** Path to the easy difficulty word list file. */
    public static final String EASY_WORD_FILE = "/Words/words_easy.txt";
    
    /** Path to the medium difficulty word list file. */
    public static final String MEDIUM_WORD_FILE = "/Words/words_medium.txt";
    
    /** Path to the hard difficulty word list file. */
    public static final String HARD_WORD_FILE = "/Words/words_hard.txt";

    // ========== DATABASE CONFIGURATION ==========
    
    /** Primary database file path for storing user statistics and preferences. */
    public static final String DATABASE_PATH = "typerr.db";
    
    /** Backup database file path for data recovery purposes. */
    public static final String DATABASE_BACKUP_PATH = "typerr_backup.db";
    
    /** Current database schema version for migration compatibility. */
    public static final int DATABASE_VERSION = 1;

    // ========== CHARTS AND STATISTICS ==========
    
    /** Maximum number of data points to display in performance charts. */
    public static final int CHART_MAX_DATA_POINTS = 100;
    
    /** Number of days to include in performance history analysis. */
    public static final int PERFORMANCE_HISTORY_DAYS = 30;

    /**
     * Private constructor to prevent instantiation of this utility class.
     * 
     * @throws UnsupportedOperationException always, as this class should never be instantiated
     */
    private Constants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }
}