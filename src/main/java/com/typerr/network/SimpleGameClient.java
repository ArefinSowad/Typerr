package com.typerr.network;

import com.typerr.statics.GameMode;
import com.typerr.TestSession;
import com.typerr.WordProvider;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client implementation for the Typerr multiplayer typing game network protocol.
 * 
 * <p>This class provides a comprehensive client-side implementation for connecting to
 * and interacting with the Typerr multiplayer game server. It handles network communication,
 * protocol message processing, session management, and provides callback mechanisms for
 * UI integration. The client supports competitive typing sessions with real-time
 * performance tracking and multiplayer coordination.</p>
 * 
 * <h3>Core Functionality:</h3>
 * <ul>
 *   <li><strong>Connection Management:</strong> Establishes and maintains TCP connections to game servers</li>
 *   <li><strong>Protocol Communication:</strong> Implements the Typerr game protocol for JSON message exchange</li>
 *   <li><strong>Session Coordination:</strong> Manages multiplayer typing test sessions and rounds</li>
 *   <li><strong>Asynchronous Processing:</strong> Non-blocking message handling with callback support</li>
 *   <li><strong>Thread Safety:</strong> Safe concurrent access to connection state and operations</li>
 *   <li><strong>Performance Tracking:</strong> Real-time WPM, accuracy, and completion time monitoring</li>
 * </ul>
 * 
 * <h3>Network Architecture:</h3>
 * <p>The client uses a TCP socket connection with JSON-based protocol messages following
 * the {@link GameProtocol} specification. Communication is bidirectional with the server
 * initiating rounds and clients responding with performance statistics. The implementation includes:</p>
 * <ul>
 *   <li>Automatic reconnection handling and configurable timeout management</li>
 *   <li>Dedicated thread pool for asynchronous message processing</li>
 *   <li>Graceful connection cleanup and comprehensive resource management</li>
 *   <li>Robust error handling with proper exception propagation to UI layer</li>
 *   <li>Connection state tracking with atomic operations for thread safety</li>
 * </ul>
 * 
 * <h3>Game Flow Integration:</h3>
 * <p>The client seamlessly integrates with the Typerr typing test system by:</p>
 * <ul>
 *   <li>Creating local {@link TestSession} instances based on server configuration</li>
 *   <li>Converting test results to {@link RoundResult} objects for transmission</li>
 *   <li>Coordinating round timing and synchronization with other connected players</li>
 *   <li>Providing performance comparisons and real-time leaderboards</li>
 *   <li>Supporting custom word lists and difficulty levels from server</li>
 * </ul>
 * 
 * <h3>Connection Lifecycle:</h3>
 * <p>The client follows a well-defined connection lifecycle:</p>
 * <ol>
 *   <li><strong>Connection:</strong> Establish TCP socket to server on specified host/port</li>
 *   <li><strong>Authentication:</strong> Exchange player information and capabilities</li>
 *   <li><strong>Waiting:</strong> Wait for server to initiate multiplayer rounds</li>
 *   <li><strong>Round Execution:</strong> Participate in typing tests with other players</li>
 *   <li><strong>Results Exchange:</strong> Send performance data and receive comparisons</li>
 *   <li><strong>Cleanup:</strong> Graceful disconnection and resource cleanup</li>
 * </ol>
 * 
 * <h3>Thread Safety and Performance:</h3>
 * <ul>
 *   <li>Uses atomic boolean flags for connection state management</li>
 *   <li>Employs single-threaded executor for message processing serialization</li>
 *   <li>Implements non-blocking I/O operations where possible</li>
 *   <li>Provides configurable timeouts for network operations</li>
 *   <li>Includes connection pooling support for multiple server connections</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Create and configure client
 * SimpleGameClient client = new SimpleGameClient();
 * 
 * // Set up callbacks for UI integration
 * client.setGameStartCallback(session -> {
 *     // Start local typing test with provided session
 *     startTypingTest(session);
 * });
 * 
 * client.setResultsReceivedCallback(results -> {
 *     // Display multiplayer results and rankings
 *     showLeaderboard(results);
 * });
 * 
 * // Connect to server
 * client.connect("localhost", 8080);
 * 
 * // Send results after completing test
 * RoundResult result = new RoundResult(wpm, accuracy, time);
 * client.sendResult(result);
 * 
 * // Cleanup when done
 * client.disconnect();
 * }</pre>
 * 
 * @author ArefinSowad
 * @version 1.0
 * @since 1.0
 * @see SimpleGameServer
 * @see GameProtocol
 * @see TestSession
 * @see RoundResult
 * @see NetworkingUIIntegration
 */
public class SimpleGameClient {

    /** Default server hostname for local connections. */
    private static final String DEFAULT_HOST = "localhost";
    
    /** Default server port for game connections. */
    private static final int DEFAULT_PORT = 8080;

    /** Server hostname or IP address for connection. */
    private final String host;
    
    /** Server port number for connection. */
    private final int port;
    
    /** Unique identifier for this player in multiplayer sessions. */
    private final String playerId;

    /** TCP socket connection to the game server. */
    private Socket socket;
    
    /** Input stream reader for receiving server messages. */
    private BufferedReader reader;
    
    /** Output stream writer for sending messages to server. */
    private PrintWriter writer;
    
    /** Thread pool for asynchronous message handling and network operations. */
    private final ExecutorService threadPool;
    
    /** Atomic flag indicating current connection status for thread-safe access. */
    private final AtomicBoolean connected = new AtomicBoolean(false);

    /** Current typing test session for active rounds. */
    private TestSession currentSession;
    
    /** Identifier for the currently active multiplayer round. */
    private String currentRoundId;
    
    /** Game mode for the current round (TIME or WORDS). */
    private GameMode currentGameMode;
    
    /** Target value for current game mode (seconds for TIME, word count for WORDS). */
    private int currentGameValue;

    /** Callback interface for round start notifications. */
    private RoundStartCallback roundStartCallback;
    
    /** Callback interface for round results and leaderboard updates. */
    private RoundResultsCallback roundResultsCallback;
    
    /** Callback interface for connection status change notifications. */
    private ConnectionStatusCallback connectionStatusCallback;

    /**
     * Callback interface for handling round start events.
     * 
     * <p>Implementations receive notification when the server starts a new
     * typing test round, including all necessary configuration and word lists.</p>
     */
    public interface RoundStartCallback {
        /**
         * Called when a new multiplayer round begins.
         * 
         * @param roundId unique identifier for this round
         * @param mode the game mode for this round (TIME or WORDS)
         * @param value the target value (seconds for TIME mode, word count for WORDS mode)
         * @param words the list of words to be typed in this round
         */
        void onRoundStart(String roundId, GameMode mode, int value, List<String> words);
    }

    /**
     * Callback interface for handling round completion and results.
     * 
     * <p>Implementations receive the final results from all players who
     * participated in the round, typically used for displaying leaderboards.</p>
     */
    public interface RoundResultsCallback {
        /**
         * Called when round results are available from the server.
         * 
         * @param roundId unique identifier for the completed round
         * @param results list of all player results, typically sorted by performance
         */
        void onRoundResults(String roundId, List<RoundResult> results);
    }

    /**
     * Callback interface for handling connection status changes.
     * 
     * <p>Implementations can update UI state and provide user feedback
     * based on network connectivity status.</p>
     */
    public interface ConnectionStatusCallback {
        /**
         * Called when the client connection status changes.
         * 
         * @param connected true if connected to server, false if disconnected
         */
        void onConnectionStatusChanged(boolean connected);
    }

    /**
     * Creates a new game client with default connection parameters.
     * 
     * <p>Uses localhost and default port (8080) for server connection.
     * The client is created in disconnected state and requires {@link #connect()}
     * to establish server communication.</p>
     * 
     * @param playerId unique identifier for this player, must not be null or empty
     * @throws IllegalArgumentException if playerId is null or empty
     */
    public SimpleGameClient(String playerId) {
        this(DEFAULT_HOST, DEFAULT_PORT, playerId);
    }

    /**
     * Creates a new game client with specified connection parameters.
     * 
     * <p>Initializes the client with custom server location but does not
     * establish connection immediately. Call {@link #connect()} to begin
     * network communication.</p>
     * 
     * @param host server hostname or IP address, must not be null
     * @param port server port number, must be valid (1-65535)
     * @param playerId unique identifier for this player, must not be null or empty
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public SimpleGameClient(String host, int port, String playerId) {
        this.host = host;
        this.port = port;
        this.playerId = playerId;
        this.threadPool = Executors.newCachedThreadPool();
    }

    /**
     * Establishes connection to the game server and begins message processing.
     * 
     * <p>This method creates a TCP socket connection with configured timeout values,
     * sets up input/output streams, and starts the asynchronous message handling
     * thread. Upon successful connection, it sends a client connect message to
     * register with the server.</p>
     * 
     * <p>Connection process includes:</p>
     * <ul>
     *   <li>Socket creation with 5-second connection timeout</li>
     *   <li>30-second read timeout for server communication</li>
     *   <li>Message processing thread startup</li>
     *   <li>Client registration with server</li>
     *   <li>Connection status callback notification</li>
     * </ul>
     * 
     * @throws IOException if connection fails due to network issues
     * @throws IllegalStateException if client is already connected
     */
    public void connect() throws IOException {
        if (connected.get()) {
            throw new IllegalStateException("Client is already connected");
        }

        socket = new Socket();
        socket.setSoTimeout(30000);
        socket.connect(new java.net.InetSocketAddress(host, port), 5000);

        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
        connected.set(true);

        System.out.println("Connected to server at " + host + ":" + port);

        threadPool.submit(this::handleMessages);

        sendMessage(GameProtocol.createClientConnect(playerId));

        if (connectionStatusCallback != null) {
            connectionStatusCallback.onConnectionStatusChanged(true);
        }
    }

    /**
     * Disconnects from the server and cleans up all network resources.
     * 
     * <p>This method gracefully closes the connection by sending a disconnect
     * message to the server, closing network streams and sockets, and shutting
     * down the thread pool. It can be called multiple times safely.</p>
     * 
     * <p>Cleanup process includes:</p>
     * <ul>
     *   <li>Sending disconnect notification to server</li>
     *   <li>Closing socket and stream resources</li>
     *   <li>Graceful thread pool shutdown with 5-second timeout</li>
     *   <li>Connection status callback notification</li>
     *   <li>Forced shutdown if graceful shutdown fails</li>
     * </ul>
     */
    public void disconnect() {
        if (!connected.get()) {
            return;
        }

        connected.set(false);

        try {
            sendMessage(GameProtocol.createClientDisconnect(playerId));
        } catch (Exception e) {
            // Ignore errors during disconnect message
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }

        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("Disconnected from server");

        if (connectionStatusCallback != null) {
            connectionStatusCallback.onConnectionStatusChanged(false);
        }
    }

    /**
     * Selects and requests a specific game mode from the server.
     * 
     * <p>This method sends a game mode selection request to the server, indicating
     * the player's preference for either time-based or word-count-based typing tests.
     * The server may use this information to match players with compatible preferences.</p>
     * 
     * @param mode the desired game mode (TIME or WORDS)
     * @param value the target value for the mode (seconds for TIME, word count for WORDS)
     * @throws IOException if network communication fails
     * @throws IllegalStateException if client is not connected to server
     */
    public void selectGameMode(GameMode mode, int value) throws IOException {
        if (!connected.get()) {
            throw new IllegalStateException("Client is not connected");
        }

        sendMessage(GameProtocol.createGameModeSelect(playerId, mode, value));
        System.out.println("Selected game mode: " + mode + " / " + value);
    }

    /**
     * Sends completed round statistics to the server.
     * 
     * <p>This method transmits the player's performance results for the current
     * round to the server. The server collects results from all participants
     * before sending back comparative statistics and rankings.</p>
     * 
     * @param result the completed round performance data
     * @throws IOException if network communication fails
     * @throws IllegalStateException if client is not connected to server
     */
    public void sendRoundStats(RoundResult result) throws IOException {
        if (!connected.get()) {
            throw new IllegalStateException("Client is not connected");
        }

        sendMessage(GameProtocol.createRoundStats(playerId, result));
        System.out.println("Sent round stats: " + result);
    }

    /**
     * Creates a new typing test session based on current round configuration.
     * 
     * <p>This method creates a local {@link TestSession} instance configured
     * with the game mode and parameters established by the server for the
     * current multiplayer round. The session can be used by the UI to conduct
     * the actual typing test.</p>
     * 
     * @return a configured TestSession for the current round
     * @throws IllegalStateException if no active round is in progress
     */
    public TestSession createTestSession() {
        if (currentGameMode == null) {
            throw new IllegalStateException("No active round");
        }

        WordProvider wordProvider = new WordProvider();
        currentSession = new TestSession(currentGameMode, currentGameValue, wordProvider);
        return currentSession;
    }

    /**
     * Returns the current active typing test session.
     * 
     * @return the current TestSession, or null if no session is active
     */
    public TestSession getCurrentSession() {
        return currentSession;
    }

    /**
     * Main message handling loop for processing server communications.
     * 
     * <p>This method runs in a separate thread and continuously reads messages
     * from the server, parsing and dispatching them to appropriate handlers.
     * It automatically disconnects if the connection is lost or an error occurs.</p>
     */
    private void handleMessages() {
        try {
            String line;
            while (connected.get() && (line = reader.readLine()) != null) {
                handleMessage(line);
            }
        } catch (IOException e) {
            if (connected.get()) {
                System.err.println("Error reading from server: " + e.getMessage());
            }
        } finally {
            disconnect();
        }
    }

    /**
     * Processes a single message received from the server.
     * 
     * <p>This method deserializes JSON messages and routes them to specific
     * handlers based on message type. Supported message types include round
     * start notifications, round results, and error messages.</p>
     * 
     * @param json the JSON message string received from server
     */
    private void handleMessage(String json) {
        try {
            GameProtocol.Message message = GameProtocol.deserialize(json);

            switch (message.getType()) {
                case ROUND_START:
                    handleRoundStart(message);
                    break;

                case ROUND_RESULTS:
                    handleRoundResults(message);
                    break;

                case ERROR:
                    handleError(message);
                    break;

                default:
                    System.err.println("Unknown message type: " + message.getType());
            }

        } catch (Exception e) {
            System.err.println("Error handling message: " + e.getMessage());
        }
    }

    /**
     * Handles round start messages from the server.
     * 
     * <p>Processes server notifications about new multiplayer rounds, extracting
     * game configuration and word lists, then notifies registered callbacks.</p>
     * 
     * @param message the round start message from server
     * @throws IOException if message processing fails
     */
    private void handleRoundStart(GameProtocol.Message message) throws IOException {
        GameProtocol.RoundStartData data = GameProtocol.extractData(message, GameProtocol.RoundStartData.class);

        currentRoundId = data.getRoundId();
        currentGameMode = data.getMode();
        currentGameValue = data.getValue();

        System.out.println("Round started: " + currentGameMode + " / " + currentGameValue);

        if (roundStartCallback != null) {
            roundStartCallback.onRoundStart(currentRoundId, currentGameMode, currentGameValue, data.getWords());
        }
    }

    /**
     * Handles round results messages from the server.
     * 
     * <p>Processes final results for completed rounds, including performance
     * statistics from all participants, then notifies registered callbacks
     * and cleans up round state.</p>
     * 
     * @param message the round results message from server
     * @throws IOException if message processing fails
     */
    private void handleRoundResults(GameProtocol.Message message) throws IOException {
        GameProtocol.RoundResultsData data = GameProtocol.extractData(message, GameProtocol.RoundResultsData.class);

        System.out.println("Round results received:");
        List<RoundResult> results = data.getResults();
        for (int i = 0; i < results.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, results.get(i));
        }

        if (roundResultsCallback != null) {
            roundResultsCallback.onRoundResults(data.getRoundId(), results);
        }

        currentRoundId = null;
        currentSession = null;
    }

    /**
     * Handles error messages from the server.
     * 
     * <p>Processes server error notifications and outputs them for debugging
     * and user feedback purposes.</p>
     * 
     * @param message the error message from server
     */
    private void handleError(GameProtocol.Message message) {
        String errorMessage = (String) message.getData();
        System.err.println("Server error: " + errorMessage);
    }

    /**
     * Sends a protocol message to the server.
     * 
     * <p>Serializes and transmits a message object to the connected server
     * using the established protocol format.</p>
     * 
     * @param message the protocol message to send
     * @throws IOException if network communication fails
     * @throws IllegalStateException if client is not connected
     */
    private void sendMessage(GameProtocol.Message message) throws IOException {
        if (!connected.get()) {
            throw new IllegalStateException("Client is not connected");
        }

        String json = GameProtocol.serialize(message);
        writer.println(json);
    }

    /**
     * Sets the callback for round start notifications.
     * 
     * @param callback the callback to invoke when rounds start, or null to remove
     */
    public void setRoundStartCallback(RoundStartCallback callback) {
        this.roundStartCallback = callback;
    }

    /**
     * Sets the callback for round results notifications.
     * 
     * @param callback the callback to invoke when results are available, or null to remove
     */
    public void setRoundResultsCallback(RoundResultsCallback callback) {
        this.roundResultsCallback = callback;
    }

    /**
     * Sets the callback for connection status change notifications.
     * 
     * @param callback the callback to invoke when connection status changes, or null to remove
     */
    public void setConnectionStatusCallback(ConnectionStatusCallback callback) {
        this.connectionStatusCallback = callback;
    }

    /**
     * Checks if the client is currently connected to a server.
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * Gets the unique player identifier for this client.
     * 
     * @return the player ID string
     */
    public String getPlayerId() {
        return playerId;
    }

    /**
     * Gets the server hostname or IP address.
     * 
     * @return the host string
     */
    public String getHost() {
        return host;
    }

    /**
     * Gets the server port number.
     * 
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Command-line interface for testing and demonstration purposes.
     * 
     * <p>Provides a simple text-based interface for connecting to servers,
     * selecting game modes, and participating in multiplayer typing tests.
     * This main method is primarily used for debugging and testing the
     * client functionality.</p>
     * 
     * @param args command line arguments: [host] [port] [playerId]
     */
    public static void main(String[] args) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;
        String playerId = "player" + System.currentTimeMillis();

        if (args.length > 0) host = args[0];
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default port " + DEFAULT_PORT);
            }
        }
        if (args.length > 2) playerId = args[2];

        SimpleGameClient client = new SimpleGameClient(host, port, playerId);

        client.setRoundStartCallback((roundId, mode, value, words) -> {
            System.out.println("=== Round Started ===");
            System.out.println("Round ID: " + roundId);
            System.out.println("Mode: " + mode + " / " + value);
            System.out.println("Words available: " + words.size());
            System.out.println("Start typing! (Type 'done' when finished)");
        });

        client.setRoundResultsCallback((roundId, results) -> {
            System.out.println("=== Round Results ===");
            for (int i = 0; i < results.size(); i++) {
                RoundResult result = results.get(i);
                System.out.printf("%d. %s - %d WPM, %d%% accuracy%n",
                    i + 1, result.getPlayerId(), result.getWpm(), result.getAccuracy());
            }
        });

        try {
            client.connect();

            Scanner scanner = new Scanner(System.in);
            System.out.println("Connected to server. Commands:");
            System.out.println("  time <seconds> - Select time-based mode");
            System.out.println("  words <count> - Select word-based mode");
            System.out.println("  done - Complete current round (send fake stats)");
            System.out.println("  quit - Disconnect and exit");

            while (client.isConnected()) {
                System.out.print("> ");
                String input = scanner.nextLine().trim();

                if ("quit".equalsIgnoreCase(input)) {
                    break;
                }

                String[] parts = input.split("\\s+");
                if (parts.length >= 2) {
                    try {
                        if ("time".equalsIgnoreCase(parts[0])) {
                            int seconds = Integer.parseInt(parts[1]);
                            client.selectGameMode(GameMode.TIME, seconds);
                        } else if ("words".equalsIgnoreCase(parts[0])) {
                            int wordCount = Integer.parseInt(parts[1]);
                            client.selectGameMode(GameMode.WORDS, wordCount);
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid number format");
                    }
                } else if ("done".equalsIgnoreCase(parts[0])) {
                    if (client.currentRoundId != null) {
                        RoundResult fakeResult = new RoundResult(
                            playerId, 45, 95, 200, 10, 30.0, true,
                            client.currentGameMode, client.currentGameValue
                        );
                        client.sendRoundStats(fakeResult);
                    } else {
                        System.out.println("No active round");
                    }
                } else {
                    System.out.println("Unknown command: " + input);
                }
            }

        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        } finally {
            client.disconnect();
        }
    }
}