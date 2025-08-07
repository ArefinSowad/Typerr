package com.typerr.statics;

public final class Constants {

    public static final String APP_NAME = "Typerr";

    public static final int DEFAULT_WINDOW_WIDTH = 950;

    public static final int DEFAULT_WINDOW_HEIGHT = 600;

    public static final String WORD_FONT_FAMILY = "Roboto Mono";

    public static final int CARET_BLINK_INTERVAL = 500;

    public static final int FADE_TRANSITION_DURATION = 150;

    public static final int DEFAULT_WORD_POOL_MULTIPLIER = 3;

    public static final double MIN_ELAPSED_TIME_MINUTES = 1e-4;

    public static final int WORDS_PER_MINUTE_DIVISOR = 5;

    public static final int DEFAULT_ACCURACY_PERCENTAGE = 100;

    public static final int MAX_WORDS_RENDERED = 25;

    public static final String ICON_PATH = "/images/icon.png";

    public static final String DEFAULT_WORD_FILE = "/Words/words.txt";

    public static final String EASY_WORD_FILE = "/Words/words_easy.txt";

    public static final String MEDIUM_WORD_FILE = "/Words/words_medium.txt";

    public static final String HARD_WORD_FILE = "/Words/words_hard.txt";

    public static final String DATABASE_PATH = "typerr.db";

    public static final String DATABASE_BACKUP_PATH = "typerr_backup.db";

    public static final int DATABASE_VERSION = 1;

    public static final int CHART_MAX_DATA_POINTS = 100;

    public static final int PERFORMANCE_HISTORY_DAYS = 30;

    private Constants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }
}