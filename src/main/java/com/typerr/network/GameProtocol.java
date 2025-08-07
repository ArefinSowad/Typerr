package com.typerr.network;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.typerr.statics.GameMode;

import java.io.IOException;
import java.util.List;

public class GameProtocol {

    public enum MessageType {

        CLIENT_CONNECT,

        GAME_MODE_SELECT,

        ROUND_START,

        ROUND_STATS,

        ROUND_RESULTS,

        CLIENT_DISCONNECT,

        ERROR
    }

    public static class Message {

        private MessageType type;

        private String playerId;

        private Object data;

        public Message() {}

        public Message(MessageType type, String playerId, Object data) {
            this.type = type;
            this.playerId = playerId;
            this.data = data;
        }

        public MessageType getType() { return type; }

        public void setType(MessageType type) { this.type = type; }

        public String getPlayerId() { return playerId; }

        public void setPlayerId(String playerId) { this.playerId = playerId; }

        public Object getData() { return data; }

        public void setData(Object data) { this.data = data; }
    }

    public static class GameModeData {

        private GameMode mode;

        private int value;

        public GameModeData() {}

        public GameModeData(GameMode mode, int value) {
            this.mode = mode;
            this.value = value;
        }

        public GameMode getMode() { return mode; }

        public void setMode(GameMode mode) { this.mode = mode; }

        public int getValue() { return value; }

        public void setValue(int value) { this.value = value; }
    }

    public static class RoundStartData {

        private String roundId;

        private GameMode mode;

        private int value;

        private List<String> words;

        public RoundStartData() {}

        public RoundStartData(String roundId, GameMode mode, int value, List<String> words) {
            this.roundId = roundId;
            this.mode = mode;
            this.value = value;
            this.words = words;
        }

        public String getRoundId() { return roundId; }

        public void setRoundId(String roundId) { this.roundId = roundId; }

        public GameMode getMode() { return mode; }

        public void setMode(GameMode mode) { this.mode = mode; }

        public int getValue() { return value; }

        public void setValue(int value) { this.value = value; }

        public List<String> getWords() { return words; }

        public void setWords(List<String> words) { this.words = words; }
    }

    public static class RoundResultsData {

        private String roundId;

        private List<RoundResult> results;

        public RoundResultsData() {}

        public RoundResultsData(String roundId, List<RoundResult> results) {
            this.roundId = roundId;
            this.results = results;
        }

        public String getRoundId() { return roundId; }

        public void setRoundId(String roundId) { this.roundId = roundId; }

        public List<RoundResult> getResults() { return results; }

        public void setResults(List<RoundResult> results) { this.results = results; }
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String serialize(Message message) throws JsonProcessingException {
        return mapper.writeValueAsString(message);
    }

    public static Message deserialize(String json) throws IOException {
        return mapper.readValue(json, Message.class);
    }

    public static Message createClientConnect(String playerId) {
        return new Message(MessageType.CLIENT_CONNECT, playerId, null);
    }

    public static Message createGameModeSelect(String playerId, GameMode mode, int value) {
        return new Message(MessageType.GAME_MODE_SELECT, playerId, new GameModeData(mode, value));
    }

    public static Message createRoundStart(String roundId, GameMode mode, int value, List<String> words) {
        return new Message(MessageType.ROUND_START, "server", new RoundStartData(roundId, mode, value, words));
    }

    public static Message createRoundStats(String playerId, RoundResult result) {
        return new Message(MessageType.ROUND_STATS, playerId, result);
    }

    public static Message createRoundResults(String roundId, List<RoundResult> results) {
        return new Message(MessageType.ROUND_RESULTS, "server", new RoundResultsData(roundId, results));
    }

    public static Message createClientDisconnect(String playerId) {
        return new Message(MessageType.CLIENT_DISCONNECT, playerId, null);
    }

    public static Message createError(String playerId, String errorMessage) {
        return new Message(MessageType.ERROR, playerId, errorMessage);
    }

    @SuppressWarnings("unchecked")
    public static <T> T extractData(Message message, Class<T> dataType) throws IOException {
        if (message.getData() == null) {
            return null;
        }

        if (message.getData() instanceof java.util.LinkedHashMap ||
            message.getData() instanceof JsonNode) {
            String json = mapper.writeValueAsString(message.getData());
            return mapper.readValue(json, dataType);
        }

        if (dataType.isInstance(message.getData())) {
            return (T) message.getData();
        }

        throw new IOException("Cannot extract data of type " + dataType.getSimpleName());
    }
}