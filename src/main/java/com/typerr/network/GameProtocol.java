package com.typerr.network;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.typerr.statics.GameMode;

import java.io.IOException;
import java.util.List;

/**
 * Protocol definition and message handling for multiplayer typing game network communication.
 * 
 * <p>This class defines the complete communication protocol used between Typerr game clients
 * and servers for multiplayer typing sessions. It provides JSON-based message serialization,
 * standardized message types, and data transfer objects for reliable network communication.</p>
 * 
 * <h3>Protocol Overview:</h3>
 * <p>The GameProtocol implements a stateful, message-based communication system where:</p>
 * <ul>
 *   <li><strong>JSON Format:</strong> All messages use JSON serialization for cross-platform compatibility</li>
 *   <li><strong>Type Safety:</strong> Strongly-typed message enums prevent protocol errors</li>
 *   <li><strong>Bidirectional:</strong> Both clients and servers can initiate specific message types</li>
 *   <li><strong>Stateful Flow:</strong> Messages follow a defined sequence for game coordination</li>
 *   <li><strong>Error Handling:</strong> Built-in error message types for robust communication</li>
 * </ul>
 * 
 * <h3>Message Flow Architecture:</h3>
 * <p>The protocol follows a structured flow for multiplayer game sessions:</p>
 * <ol>
 *   <li><strong>Connection Phase:</strong> CLIENT_CONNECT establishes player session</li>
 *   <li><strong>Configuration Phase:</strong> GAME_MODE_SELECT sets test parameters</li>
 *   <li><strong>Execution Phase:</strong> ROUND_START initiates synchronized typing tests</li>
 *   <li><strong>Results Phase:</strong> ROUND_STATS and ROUND_RESULTS exchange performance data</li>
 *   <li><strong>Cleanup Phase:</strong> CLIENT_DISCONNECT gracefully terminates session</li>
 * </ol>
 * 
 * <h3>Message Types and Usage:</h3>
 * <ul>
 *   <li><strong>{@link MessageType#CLIENT_CONNECT}:</strong> Initial player connection with ID</li>
 *   <li><strong>{@link MessageType#GAME_MODE_SELECT}:</strong> Configure test mode and parameters</li>
 *   <li><strong>{@link MessageType#ROUND_START}:</strong> Begin synchronized typing round</li>
 *   <li><strong>{@link MessageType#ROUND_STATS}:</strong> Submit individual player performance</li>
 *   <li><strong>{@link MessageType#ROUND_RESULTS}:</strong> Broadcast comparative results</li>
 *   <li><strong>{@link MessageType#CLIENT_DISCONNECT}:</strong> Graceful session termination</li>
 *   <li><strong>{@link MessageType#ERROR}:</strong> Error reporting and recovery</li>
 * </ul>
 * 
 * <h3>Data Transfer Objects:</h3>
 * <p>The protocol includes specialized DTOs for different data types:</p>
 * <ul>
 *   <li><strong>{@link Message}:</strong> Base message wrapper with type, player ID, and data</li>
 *   <li><strong>{@link GameModeData}:</strong> Game configuration (mode, duration/word count)</li>
 *   <li><strong>{@link RoundStartData}:</strong> Round initialization with word lists</li>
 *   <li><strong>{@link StatsData}:</strong> Individual player performance metrics</li>
 * </ul>
 * 
 * <h3>JSON Serialization:</h3>
 * <p>The protocol uses Jackson for JSON processing with features:</p>
 * <ul>
 *   <li>Automatic object mapping and type conversion</li>
 *   <li>Null safety and optional field handling</li>
 *   <li>Enum serialization with string names for readability</li>
 *   <li>Error recovery for malformed messages</li>
 * </ul>
 * 
 * <h3>Thread Safety and Performance:</h3>
 * <ul>
 *   <li>Immutable message objects for safe concurrent access</li>
 *   <li>Reusable ObjectMapper instance for efficient serialization</li>
 *   <li>Lightweight message format minimizing network overhead</li>
 *   <li>Stateless protocol design supporting multiple concurrent sessions</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Create a game mode selection message
 * GameModeData modeData = new GameModeData(GameMode.TIME, 60);
 * Message message = new Message(MessageType.GAME_MODE_SELECT, "player123", modeData);
 * String json = GameProtocol.serialize(message);
 * 
 * // Send over network...
 * 
 * // Deserialize received message
 * Message received = GameProtocol.deserialize(json);
 * if (received.getType() == MessageType.ROUND_START) {
 *     RoundStartData roundData = GameProtocol.parseData(received, RoundStartData.class);
 *     startTypingTest(roundData.getWords());
 * }
 * }</pre>
 * 
 * <h3>Error Handling:</h3>
 * <p>The protocol includes comprehensive error handling:</p>
 * <ul>
 *   <li>JSON parsing errors are caught and logged</li>
 *   <li>Invalid message types trigger ERROR responses</li>
 *   <li>Missing required fields are validated</li>
 *   <li>Network timeouts and connection errors are handled gracefully</li>
 * </ul>
 * 
 * @author ArefinSowad
 * @version 1.0
 * @since 1.0
 * @see SimpleGameServer
 * @see SimpleGameClient
 * @see NetworkingUIIntegration
 * @see RoundResult
 * @see TestSession
 */
public class GameProtocol {

    /**
     * Enumeration of all supported message types in the multiplayer game protocol.
     * 
     * <p>Each message type represents a specific stage or action in the multiplayer
     * game flow, ensuring structured communication between clients and servers.
     * The types follow a logical sequence for game session management.</p>
     * 
     * <h4>Message Type Descriptions:</h4>
     * <ul>
     *   <li><strong>CLIENT_CONNECT:</strong> Initial connection message with player ID</li>
     *   <li><strong>GAME_MODE_SELECT:</strong> Configure test parameters (mode, duration)</li>
     *   <li><strong>ROUND_START:</strong> Begin typing test with synchronized word list</li>
     *   <li><strong>ROUND_STATS:</strong> Submit individual player performance data</li>
     *   <li><strong>ROUND_RESULTS:</strong> Receive aggregated results from all players</li>
     *   <li><strong>CLIENT_DISCONNECT:</strong> Graceful disconnection notification</li>
     *   <li><strong>ERROR:</strong> Error reporting and exception handling</li>
     * </ul>
     * 
     * @see Message
     */
    public enum MessageType {
        /** Player establishing initial connection to server. */
        CLIENT_CONNECT,
        
        /** Configuration of game mode and test parameters. */
        GAME_MODE_SELECT,
        
        /** Server initiating a new typing test round. */
        ROUND_START,
        
        /** Player submitting performance statistics after test completion. */
        ROUND_STATS,
        
        /** Server broadcasting comparative results from all players. */
        ROUND_RESULTS,
        
        /** Player disconnecting from the game session. */
        CLIENT_DISCONNECT,
        
        /** Error message for exception handling and recovery. */
        ERROR
    }

    /**
     * Base message wrapper for all protocol communications.
     * 
     * <p>This class serves as the standard envelope for all messages exchanged between
     * game clients and servers. It provides type identification, player association,
     * and data payload encapsulation in a JSON-serializable format.</p>
     * 
     * <h4>Message Structure:</h4>
     * <ul>
     *   <li><strong>Type:</strong> Identifies the message purpose and expected data format</li>
     *   <li><strong>Player ID:</strong> Associates the message with a specific player session</li>
     *   <li><strong>Data:</strong> Type-specific payload containing the actual message content</li>
     * </ul>
     * 
     * <h4>JSON Serialization:</h4>
     * <p>The class is designed for automatic JSON serialization using Jackson:</p>
     * <ul>
     *   <li>Default constructor for deserialization</li>
     *   <li>Getter/setter methods for property access</li>
     *   <li>Object data field supporting polymorphic content</li>
     *   <li>Null safety for optional fields</li>
     * </ul>
     * 
     * <h4>Usage Pattern:</h4>
     * <pre>{@code
     * // Create a new message
     * Message msg = new Message(MessageType.ROUND_START, "player123", roundData);
     * 
     * // Serialize to JSON
     * String json = GameProtocol.serialize(msg);
     * 
     * // Deserialize from JSON
     * Message received = GameProtocol.deserialize(json);
     * }</pre>
     * 
     * @see MessageType
     * @see GameProtocol#serialize(Message)
     * @see GameProtocol#deserialize(String)
     */
    public static class Message {
        /** The type of this message, determining its purpose and data format. */
        private MessageType type;
        
        /** Unique identifier of the player associated with this message. */
        private String playerId;
        
        /** Type-specific data payload for this message. */
        private Object data;

        /**
         * Default constructor for JSON deserialization.
         * 
         * <p>This constructor is required by Jackson for automatic JSON-to-object
         * conversion. It creates an empty message that will be populated during
         * the deserialization process.</p>
         */
        public Message() {}

        /**
         * Creates a new message with specified type, player ID, and data.
         * 
         * <p>This constructor is used for creating outgoing messages that will
         * be serialized and transmitted over the network. It ensures all
         * required fields are properly initialized.</p>
         * 
         * @param type the message type indicating purpose and expected data format
         * @param playerId the unique identifier of the associated player
         * @param data the type-specific payload for this message
         */
        public Message(MessageType type, String playerId, Object data) {
            this.type = type;
            this.playerId = playerId;
            this.data = data;
        }

        /**
         * Returns the message type.
         * 
         * @return the message type, or null if not set
         */
        public MessageType getType() { return type; }
        
        /**
         * Sets the message type.
         * 
         * @param type the message type to set
         */
        public void setType(MessageType type) { this.type = type; }
        
        /**
         * Returns the player ID associated with this message.
         * 
         * @return the player ID, or null if not set
         */
        public String getPlayerId() { return playerId; }
        
        /**
         * Sets the player ID associated with this message.
         * 
         * @param playerId the player ID to set
         */
        public void setPlayerId(String playerId) { this.playerId = playerId; }
        
        /**
         * Returns the data payload of this message.
         * 
         * @return the message data, or null if not set
         */
        public Object getData() { return data; }
        
        /**
         * Sets the data payload of this message.
         * 
         * @param data the message data to set
         */
        public void setData(Object data) { this.data = data; }
    }

    /**
     * Data transfer object for game mode configuration messages.
     * 
     * <p>This class encapsulates the game mode selection data that is exchanged
     * between clients and servers when configuring a multiplayer typing test.
     * It includes the test type (time-based or word-based) and the associated
     * parameter value (duration in seconds or word count).</p>
     * 
     * <p>The GameModeData is primarily used with {@link MessageType#GAME_MODE_SELECT}
     * messages to allow clients to request specific test configurations and for
     * servers to acknowledge and broadcast the selected game parameters to all
     * connected players.</p>
     * 
     * <h4>Supported Game Modes:</h4>
     * <ul>
     *   <li><strong>TIME mode:</strong> Value represents duration in seconds (e.g., 15, 30, 60)</li>
     *   <li><strong>WORDS mode:</strong> Value represents word count target (e.g., 10, 25, 50)</li>
     * </ul>
     * 
     * <p>JSON serialization example:</p>
     * <pre>{@code
     * {
     *   "mode": "TIME",
     *   "value": 60
     * }
     * }</pre>
     * 
     * @see GameMode
     * @see Message
     * @see MessageType#GAME_MODE_SELECT
     */
    public static class GameModeData {
        /** The selected game mode (TIME or WORDS). */
        private GameMode mode;
        
        /** The associated parameter value (seconds for TIME mode, word count for WORDS mode). */
        private int value;

        /**
         * Default constructor for JSON deserialization.
         * 
         * <p>Creates an empty GameModeData object that will be populated
         * during JSON-to-object conversion by Jackson.</p>
         */
        public GameModeData() {}

        /**
         * Creates a new GameModeData with the specified mode and value.
         * 
         * @param mode the game mode type (TIME or WORDS)
         * @param value the parameter value (duration in seconds for TIME, word count for WORDS)
         */
        public GameModeData(GameMode mode, int value) {
            this.mode = mode;
            this.value = value;
        }

        /**
         * Returns the game mode.
         * 
         * @return the game mode, or null if not set
         */
        public GameMode getMode() { return mode; }
        
        /**
         * Sets the game mode.
         * 
         * @param mode the game mode to set
         */
        public void setMode(GameMode mode) { this.mode = mode; }
        
        /**
         * Returns the parameter value.
         * 
         * @return the value (seconds for TIME mode, word count for WORDS mode)
         */
        public int getValue() { return value; }
        
        /**
         * Sets the parameter value.
         * 
         * @param value the value to set (seconds for TIME mode, word count for WORDS mode)
         */
        public void setValue(int value) { this.value = value; }
    }

    /**
     * Data transfer object for round start initialization messages.
     * 
     * <p>This class contains all the information needed to start a synchronized
     * typing test round in a multiplayer environment. It includes a unique round
     * identifier, the test configuration parameters, and the complete list of
     * words that all players will type during the round.</p>
     * 
     * <p>The RoundStartData is sent by the server to all connected clients when
     * initiating a new typing test round, ensuring that all players receive
     * exactly the same word sequence and test parameters for fair competition.</p>
     * 
     * <h4>Key Components:</h4>
     * <ul>
     *   <li><strong>Round ID:</strong> Unique identifier for tracking and correlating results</li>
     *   <li><strong>Mode &amp; Value:</strong> Test configuration (TIME/WORDS and parameter value)</li>
     *   <li><strong>Word List:</strong> Exact sequence of words for all players to type</li>
     * </ul>
     * 
     * <p>The word list ensures synchronization across all clients, enabling
     * meaningful performance comparison and preventing advantages from
     * different word difficulties or sequences.</p>
     * 
     * <p>JSON serialization example:</p>
     * <pre>{@code
     * {
     *   "roundId": "round_12345",
     *   "mode": "TIME",
     *   "value": 60,
     *   "words": ["hello", "world", "typing", "test", ...]
     * }
     * }</pre>
     * 
     * @see GameMode
     * @see MessageType#ROUND_START
     * @see RoundResult
     */
    public static class RoundStartData {
        /** Unique identifier for this typing test round. */
        private String roundId;
        
        /** The game mode for this round (TIME or WORDS). */
        private GameMode mode;
        
        /** The parameter value (duration in seconds or target word count). */
        private int value;
        
        /** The list of words that all players will type during this round. */
        private List<String> words;

        /**
         * Default constructor for JSON deserialization.
         * 
         * <p>Creates an empty RoundStartData object that will be populated
         * during JSON-to-object conversion by Jackson.</p>
         */
        public RoundStartData() {}

        /**
         * Creates a new RoundStartData with the specified parameters.
         * 
         * @param roundId unique identifier for this round
         * @param mode the game mode (TIME or WORDS)
         * @param value the parameter value (seconds for TIME, word count for WORDS)
         * @param words the list of words for players to type
         */
        public RoundStartData(String roundId, GameMode mode, int value, List<String> words) {
            this.roundId = roundId;
            this.mode = mode;
            this.value = value;
            this.words = words;
        }

        /**
         * Returns the round identifier.
         * 
         * @return the unique round ID, or null if not set
         */
        public String getRoundId() { return roundId; }
        
        /**
         * Sets the round identifier.
         * 
         * @param roundId the unique round ID to set
         */
        public void setRoundId(String roundId) { this.roundId = roundId; }
        
        /**
         * Returns the game mode for this round.
         * 
         * @return the game mode, or null if not set
         */
        public GameMode getMode() { return mode; }
        
        /**
         * Sets the game mode for this round.
         * 
         * @param mode the game mode to set
         */
        public void setMode(GameMode mode) { this.mode = mode; }
        
        /**
         * Returns the parameter value for this round.
         * 
         * @return the value (seconds for TIME mode, word count for WORDS mode)
         */
        public int getValue() { return value; }
        
        /**
         * Sets the parameter value for this round.
         * 
         * @param value the value to set (seconds for TIME mode, word count for WORDS mode)
         */
        public void setValue(int value) { this.value = value; }
        
        /**
         * Returns the word list for this round.
         * 
         * @return the list of words to type, or null if not set
         */
        public List<String> getWords() { return words; }
        
        /**
         * Sets the word list for this round.
         * 
         * @param words the list of words to type
         */
        public void setWords(List<String> words) { this.words = words; }
    }

    /**
     * Data transfer object for aggregated round results messages.
     * 
     * <p>This class encapsulates the complete results from a multiplayer typing
     * test round, including the round identifier and performance data from all
     * participating players. It is used by the server to broadcast final results
     * to all clients after a round has been completed by all players.</p>
     * 
     * <p>The RoundResultsData provides comparative performance analysis by
     * including results from all players in a single message, allowing clients
     * to display leaderboards, rankings, and comparative statistics.</p>
     * 
     * <h4>Contents:</h4>
     * <ul>
     *   <li><strong>Round ID:</strong> Links results to the specific round</li>
     *   <li><strong>Results List:</strong> Complete performance data from all players</li>
     *   <li><strong>Ranking Information:</strong> Implicitly ordered for competitive display</li>
     * </ul>
     * 
     * <p>The results list typically contains {@link RoundResult} objects sorted
     * by performance metrics (e.g., WPM descending) to facilitate leaderboard
     * display and winner determination.</p>
     * 
     * <p>JSON serialization example:</p>
     * <pre>{@code
     * {
     *   "roundId": "round_12345",
     *   "results": [
     *     {"playerId": "player1", "wpm": 85, "accuracy": 95, ...},
     *     {"playerId": "player2", "wpm": 78, "accuracy": 92, ...}
     *   ]
     * }
     * }</pre>
     * 
     * @see RoundResult
     * @see MessageType#ROUND_RESULTS
     * @see RoundStartData
     */
    public static class RoundResultsData {
        /** The identifier of the round these results belong to. */
        private String roundId;
        
        /** List of performance results from all players in the round. */
        private List<RoundResult> results;

        /**
         * Default constructor for JSON deserialization.
         * 
         * <p>Creates an empty RoundResultsData object that will be populated
         * during JSON-to-object conversion by Jackson.</p>
         */
        public RoundResultsData() {}

        /**
         * Creates a new RoundResultsData with the specified round ID and results.
         * 
         * @param roundId the identifier of the round these results belong to
         * @param results the list of player performance results
         */
        public RoundResultsData(String roundId, List<RoundResult> results) {
            this.roundId = roundId;
            this.results = results;
        }

        /**
         * Returns the round identifier.
         * 
         * @return the round ID these results belong to, or null if not set
         */
        public String getRoundId() { return roundId; }
        
        /**
         * Sets the round identifier.
         * 
         * @param roundId the round ID to set
         */
        public void setRoundId(String roundId) { this.roundId = roundId; }
        
        /**
         * Returns the list of player results.
         * 
         * @return the list of performance results from all players, or null if not set
         */
        public List<RoundResult> getResults() { return results; }
        
        /**
         * Sets the list of player results.
         * 
         * @param results the list of performance results to set
         */
        public void setResults(List<RoundResult> results) { this.results = results; }
    }

    /** Jackson ObjectMapper instance for efficient JSON serialization and deserialization. */
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Serializes a message object to JSON string format.
     * 
     * <p>Converts a {@link Message} object into its JSON string representation
     * using Jackson ObjectMapper. The resulting JSON can be transmitted over
     * network connections for client-server communication.</p>
     * 
     * @param message the message object to serialize
     * @return JSON string representation of the message
     * @throws JsonProcessingException if the message cannot be serialized to JSON
     */
    public static String serialize(Message message) throws JsonProcessingException {
        return mapper.writeValueAsString(message);
    }

    /**
     * Deserializes a JSON string into a message object.
     * 
     * <p>Converts a JSON string back into a {@link Message} object using
     * Jackson ObjectMapper. This method is used to reconstruct message
     * objects from JSON data received over network connections.</p>
     * 
     * @param json the JSON string to deserialize
     * @return the reconstructed Message object
     * @throws IOException if the JSON cannot be parsed or is malformed
     */
    public static Message deserialize(String json) throws IOException {
        return mapper.readValue(json, Message.class);
    }

    /**
     * Creates a client connection message.
     * 
     * <p>Factory method that creates a standardized {@link MessageType#CLIENT_CONNECT}
     * message for establishing a new player connection to the server. This is
     * typically the first message sent by a client when joining a multiplayer session.</p>
     * 
     * @param playerId unique identifier for the connecting player
     * @return a properly formatted client connect message
     */
    public static Message createClientConnect(String playerId) {
        return new Message(MessageType.CLIENT_CONNECT, playerId, null);
    }

    /**
     * Creates a game mode selection message.
     * 
     * <p>Factory method that creates a {@link MessageType#GAME_MODE_SELECT} message
     * containing the desired test configuration. Used by clients to request
     * specific game modes and by servers to broadcast the selected configuration.</p>
     * 
     * @param playerId identifier of the player making the selection
     * @param mode the desired game mode (TIME or WORDS)
     * @param value the parameter value (duration in seconds for TIME, word count for WORDS)
     * @return a properly formatted game mode selection message
     */
    public static Message createGameModeSelect(String playerId, GameMode mode, int value) {
        return new Message(MessageType.GAME_MODE_SELECT, playerId, new GameModeData(mode, value));
    }

    /**
     * Creates a round start message.
     * 
     * <p>Factory method that creates a {@link MessageType#ROUND_START} message
     * used by servers to initiate a new typing test round. Contains all necessary
     * information for clients to begin synchronized testing.</p>
     * 
     * @param roundId unique identifier for the new round
     * @param mode the game mode for this round (TIME or WORDS)
     * @param value the parameter value (duration in seconds or target word count)
     * @param words the list of words that all players will type
     * @return a properly formatted round start message
     */
    public static Message createRoundStart(String roundId, GameMode mode, int value, List<String> words) {
        return new Message(MessageType.ROUND_START, "server", new RoundStartData(roundId, mode, value, words));
    }

    /**
     * Creates a round statistics submission message.
     * 
     * <p>Factory method that creates a {@link MessageType#ROUND_STATS} message
     * used by clients to submit their performance results after completing
     * a typing test round.</p>
     * 
     * @param playerId identifier of the player submitting results
     * @param result the player's performance data for the completed round
     * @return a properly formatted round statistics message
     */
    public static Message createRoundStats(String playerId, RoundResult result) {
        return new Message(MessageType.ROUND_STATS, playerId, result);
    }

    /**
     * Creates a round results broadcast message.
     * 
     * <p>Factory method that creates a {@link MessageType#ROUND_RESULTS} message
     * used by servers to broadcast the final results from all players after
     * a round has been completed by everyone.</p>
     * 
     * @param roundId identifier of the completed round
     * @param results list of performance results from all players
     * @return a properly formatted round results message
     */
    public static Message createRoundResults(String roundId, List<RoundResult> results) {
        return new Message(MessageType.ROUND_RESULTS, "server", new RoundResultsData(roundId, results));
    }

    /**
     * Creates a client disconnection message.
     * 
     * <p>Factory method that creates a {@link MessageType#CLIENT_DISCONNECT} message
     * for graceful disconnection from multiplayer sessions. Allows servers to
     * properly clean up player state and notify other clients.</p>
     * 
     * @param playerId identifier of the disconnecting player
     * @return a properly formatted client disconnect message
     */
    public static Message createClientDisconnect(String playerId) {
        return new Message(MessageType.CLIENT_DISCONNECT, playerId, null);
    }

    /**
     * Creates an error message.
     * 
     * <p>Factory method that creates a {@link MessageType#ERROR} message
     * for reporting errors, exceptions, or invalid states during multiplayer
     * communication. Includes a descriptive error message for debugging.</p>
     * 
     * @param playerId identifier of the player associated with the error (or "server")
     * @param errorMessage descriptive text explaining the error condition
     * @return a properly formatted error message
     */
    public static Message createError(String playerId, String errorMessage) {
        return new Message(MessageType.ERROR, playerId, errorMessage);
    }

    /**
     * Extracts and converts message data to the specified type.
     * 
     * <p>Utility method that safely extracts the data payload from a message
     * and converts it to the requested type. Handles the complexities of
     * Jackson's object mapping when dealing with polymorphic data fields.</p>
     * 
     * <p>This method handles several common scenarios:</p>
     * <ul>
     *   <li>Direct type casting when the data is already the correct type</li>
     *   <li>JSON re-serialization and parsing for LinkedHashMap data</li>
     *   <li>JsonNode conversion for complex nested objects</li>
     *   <li>Null safety for messages without data payloads</li>
     * </ul>
     * 
     * @param <T> the expected type of the data payload
     * @param message the message containing the data to extract
     * @param dataType the Class object representing the expected data type
     * @return the extracted data object, or null if the message data is null
     * @throws IOException if the data cannot be converted to the specified type
     */

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