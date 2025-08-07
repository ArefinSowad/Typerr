package com.typerr;

import com.typerr.statics.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class WordProvider {

    public enum Difficulty {

        EASY,

        MEDIUM,

        HARD
    }

    private static final String DEFAULT_WORD_FILE = Constants.DEFAULT_WORD_FILE;

    private static final String EASY_WORD_FILE    = Constants.EASY_WORD_FILE;

    private static final String MEDIUM_WORD_FILE  = Constants.MEDIUM_WORD_FILE;

    private static final String HARD_WORD_FILE    = Constants.HARD_WORD_FILE;

    private final Map<Difficulty, List<String>> wordsByDifficulty = new EnumMap<>(Difficulty.class);

    private Difficulty currentDifficulty = Difficulty.MEDIUM;

    private final Random random = new Random();

    public WordProvider() {
        List<String> defaultWords = loadWordsFromResource(DEFAULT_WORD_FILE);

        wordsByDifficulty.put(Difficulty.EASY,
                loadWordsFromResource(EASY_WORD_FILE, defaultWords));

        wordsByDifficulty.put(Difficulty.MEDIUM,
                loadWordsFromResource(MEDIUM_WORD_FILE, defaultWords));

        wordsByDifficulty.put(Difficulty.HARD,
                loadWordsFromResource(HARD_WORD_FILE, defaultWords));
    }

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

    public void setDifficulty(Difficulty difficulty) {
        this.currentDifficulty = Objects.requireNonNullElse(difficulty, Difficulty.MEDIUM);
    }

    public Difficulty getCurrentDifficulty() {
        return currentDifficulty;
    }

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

    private List<String> loadWordsFromResource(String resourcePath, List<String> fallback) {
        List<String> loaded = loadWordsFromResource(resourcePath);
        return loaded.isEmpty() ? fallback : loaded;
    }
}
