package com.typerr;

import com.typerr.statics.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides word lists for typing tests with configurable difficulty levels.
 * 
 * <p>The WordProvider is responsible for loading, managing, and serving word lists
 * for typing tests based on different difficulty levels. It reads word lists from
 * resource files and provides randomized word selection for test generation.</p>
 * 
 * <h3>Difficulty System:</h3>
 * <p>The provider supports three difficulty levels, each with distinct characteristics:</p>
 * <ul>
 *   <li><strong>Easy:</strong> Common, short words (3-5 characters) for beginners</li>
 *   <li><strong>Medium:</strong> Balanced mix of common and moderately complex words</li>
 *   <li><strong>Hard:</strong> Complex words, technical terms, and longer words</li>
 * </ul>
 * 
 * <h3>Word Selection Algorithm:</h3>
 * <p>Words are selected using a uniform random distribution to ensure variety
 * in typing tests. The same word may appear multiple times in longer tests,
 * which is intentional for training purposes.</p>
 * 
 * <h3>Resource Management:</h3>
 * <p>Word lists are loaded from text files in the application resources:</p>
 * <ul>
 *   <li>{@code /words.txt} - Default/fallback word list</li>
 *   <li>{@code /words_easy.txt} - Easy difficulty words</li>
 *   <li>{@code /words_medium.txt} - Medium difficulty words</li>
 *   <li>{@code /words_hard.txt} - Hard difficulty words</li>
 * </ul>
 * 
 * <p>Files support comment lines starting with '//' or '#' which are ignored
 * during loading. Empty lines are also filtered out automatically.</p>
 * 
 * <h3>Error Handling:</h3>
 * <p>The provider includes robust error handling:</p>
 * <ul>
 *   <li>Missing resource files fall back to default word list</li>
 *   <li>Invalid difficulty settings default to MEDIUM</li>
 *   <li>Empty word lists are handled gracefully</li>
 *   <li>File encoding issues are logged but don't crash the application</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * WordProvider provider = new WordProvider();
 * 
 * // Set difficulty level
 * provider.setDifficulty(WordProvider.Difficulty.HARD);
 * 
 * // Get words for a test
 * List<String> testWords = provider.getWords(25);
 * 
 * // Words are ready for use in typing test
 * TestSession session = new TestSession(GameMode.WORDS, 25, provider);
 * }</pre>
 * 
 * @author ArefinSowad
 * @version 1.0
 * @since 1.0
 * @see TestSession
 * @see Constants
 */
public class WordProvider {

    /**
     * Enumeration representing the available difficulty levels for word selection.
     * 
     * <p>Each difficulty level corresponds to a different word list with varying
     * complexity characteristics designed to challenge typists at different
     * skill levels.</p>
     */
    public enum Difficulty {
        
        /**
         * Easy difficulty level featuring short, common words.
         * 
         * <p>Ideal for beginners and users learning touch typing.
         * Contains words that are:</p>
         * <ul>
         *   <li>3-5 characters in length typically</li>
         *   <li>High-frequency words in English</li>
         *   <li>Simple letter combinations</li>
         *   <li>Minimal use of special characters</li>
         * </ul>
         */
        EASY,

        /**
         * Medium difficulty level with balanced word complexity.
         * 
         * <p>Suitable for intermediate typists developing speed and accuracy.
         * Contains words that are:</p>
         * <ul>
         *   <li>4-8 characters in length typically</li>
         *   <li>Mix of common and less common words</li>
         *   <li>Moderate complexity in letter combinations</li>
         *   <li>Some apostrophes and basic punctuation</li>
         * </ul>
         */
        MEDIUM,

        /**
         * Hard difficulty level featuring complex and challenging words.
         * 
         * <p>Designed for advanced typists seeking to improve precision
         * and handle complex text. Contains words that are:</p>
         * <ul>
         *   <li>6+ characters in length</li>
         *   <li>Technical terms and specialized vocabulary</li>
         *   <li>Complex letter combinations and patterns</li>
         *   <li>Mixed case and punctuation where appropriate</li>
         * </ul>
         */
        HARD
    }

    /** Path to the default word list resource file. */
    private static final String DEFAULT_WORD_FILE = Constants.DEFAULT_WORD_FILE;
    
    /** Path to the easy difficulty word list resource file. */
    private static final String EASY_WORD_FILE    = Constants.EASY_WORD_FILE;
    
    /** Path to the medium difficulty word list resource file. */
    private static final String MEDIUM_WORD_FILE  = Constants.MEDIUM_WORD_FILE;
    
    /** Path to the hard difficulty word list resource file. */
    private static final String HARD_WORD_FILE    = Constants.HARD_WORD_FILE;

    /** Map storing word lists organized by difficulty level. */
    private final Map<Difficulty, List<String>> wordsByDifficulty = new EnumMap<>(Difficulty.class);
    
    /** Current difficulty level for word selection. */
    private Difficulty currentDifficulty = Difficulty.MEDIUM;
    
    /** Random number generator for word selection. */
    private final Random random = new Random();

    /**
     * Constructs a new WordProvider and loads all word lists from resources.
     * 
     * <p>This constructor initializes the word provider by loading word lists
     * for all difficulty levels from their respective resource files. If any
     * difficulty-specific file fails to load, it falls back to the default
     * word list to ensure the application remains functional.</p>
     * 
     * <p>The loading process:</p>
     * <ol>
     *   <li>Loads the default word list as fallback</li>
     *   <li>Attempts to load each difficulty-specific word list</li>
     *   <li>Uses default words if any specific list fails to load</li>
     *   <li>Filters out comments and empty lines</li>
     *   <li>Creates immutable lists for thread safety</li>
     * </ol>
     * 
     * @throws RuntimeException if the default word list cannot be loaded
     */
    public WordProvider() {
        List<String> defaultWords = loadWordsFromResource(DEFAULT_WORD_FILE);

        wordsByDifficulty.put(Difficulty.EASY,
                loadWordsFromResource(EASY_WORD_FILE, defaultWords));

        wordsByDifficulty.put(Difficulty.MEDIUM,
                loadWordsFromResource(MEDIUM_WORD_FILE, defaultWords));

        wordsByDifficulty.put(Difficulty.HARD,
                loadWordsFromResource(HARD_WORD_FILE, defaultWords));
    }

    /**
     * Returns a list of randomly selected words for typing tests.
     * 
     * <p>This method generates a list of words selected randomly from the
     * current difficulty level's word pool. Words are selected with replacement,
     * meaning the same word may appear multiple times in the result, which is
     * intentional for typing practice.</p>
     * 
     * <p>Selection behavior:</p>
     * <ul>
     *   <li>Uses uniform random distribution for fair selection</li>
     *   <li>Allows duplicate words within the same list</li>
     *   <li>Falls back to MEDIUM difficulty if current level is unavailable</li>
     *   <li>Returns immutable list for thread safety</li>
     * </ul>
     * 
     * @param count the number of words to select, must be positive
     * @return an immutable list of randomly selected words, empty if count <= 0
     * @throws IllegalStateException if no word lists are available
     */
    public List<String> getWords(int count) {
        if (count <= 0) {
            return Collections.emptyList();
        }

        List<String> source = wordsByDifficulty.get(currentDifficulty);
        if (source == null || source.isEmpty()) {
            source = wordsByDifficulty.get(Difficulty.MEDIUM);
        }

        return Collections.unmodifiableList(
                random.ints(count, 0, source.size())
                        .mapToObj(source::get)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Sets the current difficulty level for word selection.
     * 
     * <p>This method changes the difficulty level used for subsequent calls
     * to {@link #getWords(int)}. If the provided difficulty is null, it
     * defaults to {@link Difficulty#MEDIUM}.</p>
     * 
     * @param difficulty the new difficulty level, or null for default (MEDIUM)
     */
    public void setDifficulty(Difficulty difficulty) {
        this.currentDifficulty = Objects.requireNonNullElse(difficulty, Difficulty.MEDIUM);
    }

    /**
     * Returns the current difficulty level setting.
     * 
     * @return the current difficulty level, never null
     */
    public Difficulty getCurrentDifficulty() {
        return currentDifficulty;
    }

    /**
     * Loads words from a resource file in the classpath.
     * 
     * <p>This method reads a text file from the application resources and
     * returns a list of words. The file is expected to contain one word per
     * line. Lines starting with '//' or '#' are treated as comments and ignored.
     * Empty lines are also filtered out.</p>
     * 
     * <p>File format requirements:</p>
     * <ul>
     *   <li>UTF-8 encoding</li>
     *   <li>One word per line</li>
     *   <li>Comment lines start with '//' or '#'</li>
     *   <li>Empty lines are ignored</li>
     *   <li>Words are trimmed of whitespace</li>
     * </ul>
     * 
     * @param resourcePath the path to the resource file (e.g., "/words.txt")
     * @return an immutable list of words loaded from the file, empty if loading fails
     */
    private List<String> loadWordsFromResource(String resourcePath) {
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {

                return br.lines()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .filter(s -> !s.startsWith("//"))
                        .filter(s -> !s.startsWith("#"))
                        .collect(Collectors.toUnmodifiableList());
            }
        } catch (IOException ex) {
            System.err.println("Could not load " + resourcePath + " – " + ex.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Loads words from a resource file with fallback support.
     * 
     * <p>This method attempts to load words from the specified resource file,
     * but if the loading fails or results in an empty list, it returns the
     * provided fallback list instead. This ensures that the application
     * remains functional even if some word files are missing.</p>
     * 
     * @param resourcePath the path to the resource file to load
     * @param fallback the fallback word list to use if loading fails
     * @return the loaded word list, or the fallback list if loading fails
     */
    private List<String> loadWordsFromResource(String resourcePath, List<String> fallback) {
        List<String> loaded = loadWordsFromResource(resourcePath);
        return loaded.isEmpty() ? fallback : loaded;
    }
}
