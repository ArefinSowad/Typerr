package com.typerr;

import com.typerr.database.StatisticsService;
import com.typerr.statics.Constants;
import com.typerr.statics.GameMode;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TestSession {

    static class Word {

        private final String targetWord;

        private String typedWord = "";

        private boolean completed = false;

        Word(String targetWord) {
            this.targetWord = Objects.requireNonNull(targetWord);
        }

        void setTypedWord(String typedWord) {
            this.typedWord = typedWord == null ? "" : typedWord;
        }

        String getTargetWord()   { return targetWord; }

        String getTypedWord()    { return typedWord;  }

        boolean isCompleted()    { return completed;  }

        boolean isCorrect() {
            return completed && targetWord.toLowerCase().equals(typedWord.toLowerCase());
        }

        void complete() { this.completed = true; }
    }

    public record TestResult(
            int  netWPM,
            int  rawWPM,
            int  accuracy,
            long correctChars,
            long incorrectChars) { }

    public static final class StatHistory {

        private static final StatisticsService statisticsService = new StatisticsService();

        public static void addResult(TestResult result, GameMode mode, int value,
                                     WordProvider.Difficulty difficulty, double durationSeconds) {
            if (result != null) {
                statisticsService.saveTestResult(result, mode, value, difficulty, durationSeconds);
            }
        }

        public static TestResult getBestResult() {
            return statisticsService.getBestResult();
        }

        public static int getTotalTestCount() {
            return statisticsService.getTotalTestCount();
        }

        public static List<StatisticsService.WPMDataPoint> getWPMProgression(int limit) {
            return statisticsService.getWPMProgression(limit);
        }

        public static List<StatisticsService.AccuracyDataPoint> getAccuracyProgression(int limit) {
            return statisticsService.getAccuracyProgression(limit);
        }

        public static void exportDataToJSON(String filePath) {
            try {
                statisticsService.exportDataToJSON(filePath);
            } catch (Exception e) {
                System.err.println("Failed to export data: " + e.getMessage());
            }
        }

        private StatHistory() {  }
    }

    private final GameMode      mode;

    private final int           value;

    private final List<Word>    wordList;

    private final WordProvider.Difficulty difficulty;

    private int                 currentWordIndex = 0;

    private long                startTimeMillis  = 0;

    private long                endTimeMillis    = 0;

    private boolean             hasStarted       = false;

    private boolean             hasEnded         = false;

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

    public void start() {
        if (!hasStarted) {
            hasStarted       = true;
            startTimeMillis  = System.currentTimeMillis();
        }
    }

    public void end() {
        if (!hasEnded && hasStarted) {
            hasEnded = true;
            endTimeMillis = System.currentTimeMillis();

            TestResult finalResult = calculateCurrentStats(true);
            double durationSeconds = (endTimeMillis - startTimeMillis) / 1000.0;

            StatHistory.addResult(finalResult, mode, value, difficulty, durationSeconds);
        }
    }

    public void advanceToNextWord() {
        wordList.get(currentWordIndex).complete();
        currentWordIndex++;
    }

    public boolean isFinished() {
        if (!hasStarted) return false;

        return switch (mode) {
            case WORDS -> currentWordIndex >= value;
            case TIME  -> (System.currentTimeMillis() - startTimeMillis) >= value * 1_000L;
        };
    }

    public Word getCurrentWord()     {
        if (currentWordIndex >= wordList.size()) {
            return wordList.get(wordList.size() - 1);
        }
        return wordList.get(currentWordIndex);
    }

    public List<Word> getWordList()  { return wordList; }

    public int  getCurrentWordIndex(){ return currentWordIndex; }

    public GameMode getMode()        { return mode; }

    public int  getValue()           { return value; }

    public long getStartTime()       { return startTimeMillis; }

    public WordProvider.Difficulty getDifficulty() { return difficulty; }

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

    private double getElapsedMinutes(boolean finalCalculation) {
        if (!hasStarted) return 0;

        long end = finalCalculation && hasEnded ? endTimeMillis : System.currentTimeMillis();
        return (end - startTimeMillis) / 60_000.0;
    }
}