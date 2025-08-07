package com.typerr.network;

import com.typerr.statics.GameMode;
import com.typerr.TestSession;

public class RoundResult {

    private String playerId;

    private int wpm;

    private int accuracy;

    private long totalCharacters;

    private long errorCount;

    private double completionTimeSeconds;

    private boolean completed;

    private GameMode gameMode;

    private int gameValue;

    public RoundResult() {}

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

    public String getPlayerId() { return playerId; }

    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public int getWpm() { return wpm; }

    public void setWpm(int wpm) { this.wpm = wpm; }

    public int getAccuracy() { return accuracy; }

    public void setAccuracy(int accuracy) { this.accuracy = accuracy; }

    public long getTotalCharacters() { return totalCharacters; }

    public void setTotalCharacters(long totalCharacters) { this.totalCharacters = totalCharacters; }

    public long getErrorCount() { return errorCount; }

    public void setErrorCount(long errorCount) { this.errorCount = errorCount; }

    public double getCompletionTimeSeconds() { return completionTimeSeconds; }

    public void setCompletionTimeSeconds(double completionTimeSeconds) { this.completionTimeSeconds = completionTimeSeconds; }

    public boolean isCompleted() { return completed; }

    public void setCompleted(boolean completed) { this.completed = completed; }

    public GameMode getGameMode() { return gameMode; }

    public void setGameMode(GameMode gameMode) { this.gameMode = gameMode; }

    public int getGameValue() { return gameValue; }

    public void setGameValue(int gameValue) { this.gameValue = gameValue; }

    @Override
    public String toString() {
        return String.format("RoundResult{playerId='%s', wpm=%d, accuracy=%d%%, " +
                           "chars=%d, errors=%d, time=%.1fs, completed=%s}",
                           playerId, wpm, accuracy, totalCharacters, errorCount,
                           completionTimeSeconds, completed);
    }
}