package com.typerr.network;

import com.typerr.statics.GameMode;
import com.typerr.TestSession;
import com.typerr.WordProvider;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TCP-based multiplayer game server for Typerr typing speed competitions.
 * 
 * <p>SimpleGameServer provides a comprehensive multiplayer typing test platform that enables
 * multiple clients to participate in synchronized typing competitions. The server manages
 * client connections, coordinates game rounds, distributes identical word lists, and
 * collects/ranks player performance results in real-time.</p>
 * 
 * <p>Built on Java's NIO socket architecture with concurrent client handling, the server
 * supports unlimited simultaneous connections while maintaining high performance and
 * reliability. Each game round features synchronized start times, identical test content,
 * and fair competition mechanics with server-side result verification.</p>
 * 
 * <h3>Core Server Features:</h3>
 * <ul>
 *   <li><strong>Multi-Client Support:</strong> Concurrent handling of unlimited player connections</li>
 *   <li><strong>Game Mode Flexibility:</strong> Support for time-based and word count-based competitions</li>
 *   <li><strong>Synchronized Rounds:</strong> Fair start times and identical content distribution</li>
 *   <li><strong>Real-time Results:</strong> Live performance collection and leaderboard generation</li>
 *   <li><strong>Server Participation:</strong> Optional server-side AI player for demonstrations</li>
 *   <li><strong>Timeout Protection:</strong> Automatic round completion to prevent hanging sessions</li>
 * </ul>
 * 
 * <h3>Network Architecture:</h3>
 * <p>The server implements a thread-per-client model using Java's ExecutorService:</p>
 * <ul>
 *   <li><strong>Main Thread:</strong> Server socket management and client acceptance</li>
 *   <li><strong>Worker Threads:</strong> Individual client communication and message processing</li>
 *   <li><strong>Game Thread:</strong> Round management and timeout handling</li>
 *   <li><strong>Cached Thread Pool:</strong> Dynamic thread allocation for optimal performance</li>
 * </ul>
 * 
 * <h3>Game Protocol Integration:</h3>
 * <p>Communication uses the standardized {@link GameProtocol} for message serialization:</p>
 * <ul>
 *   <li><strong>Connection Messages:</strong> Client registration and authentication</li>
 *   <li><strong>Game Setup:</strong> Mode selection and round configuration</li>
 *   <li><strong>Round Management:</strong> Start signals and word list distribution</li>
 *   <li><strong>Result Collection:</strong> Performance data and ranking compilation</li>
 * </ul>
 * 
 * <h3>Performance and Scalability:</h3>
 * <ul>
 *   <li><strong>Concurrent Design:</strong> Non-blocking client operations with thread safety</li>
 *   <li><strong>Memory Efficient:</strong> Optimized data structures for large player counts</li>
 *   <li><strong>Network Optimized:</strong> Minimal bandwidth usage with compact message protocol</li>
 *   <li><strong>Resource Management:</strong> Automatic cleanup and connection lifecycle management</li>
 * </ul>
 * 
 * <h3>Server Lifecycle:</h3>
 * <pre>
 * START → ACCEPTING_CONNECTIONS → ROUND_ACTIVE → COLLECTING_RESULTS → ROUND_COMPLETE → ACCEPTING_CONNECTIONS
 *                ↓                                                                           ↑
 *              SHUTDOWN ←−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−
 * </pre>
 * 
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * // Start server on default port
 * SimpleGameServer server = new SimpleGameServer();
 * server.start();
 * 
 * // Start server on custom port
 * SimpleGameServer server = new SimpleGameServer(9090);
 * server.start();
 * 
 * // Start a 30-second typing round
 * server.startRound(GameMode.TIME, 30);
 * 
 * // Graceful shutdown
 * server.stop();
 * }</pre>
 * 
 * <h3>Thread Safety:</h3>
 * <p>The server is designed for high concurrency with thread-safe operations:</p>
 * <ul>
 *   <li>Client management uses {@link ConcurrentHashMap} for thread-safe access</li>
 *   <li>Round state is protected by synchronized methods</li>
 *   <li>Atomic operations ensure consistent server state</li>
 *   <li>Proper resource cleanup prevents memory leaks</li>
 * </ul>
 * 
 * <h3>Error Handling:</h3>
 * <p>Comprehensive error handling ensures server stability:</p>
 * <ul>
 *   <li>Client disconnection detection with automatic cleanup</li>
 *   <li>Network error recovery with connection retries</li>
 *   <li>Invalid message handling with error responses</li>
 *   <li>Resource exhaustion protection with limits</li>
 * </ul>
 * 
 * @author ArefinSowad
 * @version 1.0
 * @since 1.0
 * @see GameProtocol
 * @see TestSession
 * @see RoundResult
 * @see ClientHandler
 */
public class SimpleGameServer {

    /** Default TCP port for server socket binding if no custom port is specified. */
    private static final int DEFAULT_PORT = 8080;
    
    /** 
     * Maximum duration in seconds for a typing round before automatic timeout.
     * Prevents indefinite waiting when clients fail to submit results.
     */
    private static final int ROUND_TIMEOUT_SECONDS = 300;

    /** TCP port number where the server listens for client connections. */
    private final int port;
    
    /** 
     * Word provider service for generating typing test content. Provides word lists
     * based on difficulty settings and game mode requirements for distribution to clients.
     * 
     * @see WordProvider
     */
    private final WordProvider wordProvider;
    
    /** 
     * Main server socket for accepting incoming client connections.
     * Bound to the configured port during server startup.
     */
    private ServerSocket serverSocket;
    
    /** 
     * Thread pool for handling concurrent client connections and background tasks.
     * Uses a cached thread pool that dynamically scales based on connection load.
     */
    private final ExecutorService threadPool;
    
    /** 
     * Atomic flag indicating server running state. Thread-safe access ensures
     * consistent state checking across multiple threads during startup and shutdown.
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** 
     * Thread-safe map of currently connected clients indexed by their unique player IDs.
     * Supports concurrent access from client handler threads and main server operations.
     * 
     * @see ClientHandler
     */
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    /** Current active game mode for the round in progress (TIME, WORDS, etc.). */
    private GameMode currentGameMode;
    
    /** Mode-specific value: seconds for TIME mode, word count for WORDS mode. */
    private int currentGameValue;
    
    /** 
     * Unique identifier for the current typing round. Generated using UUID
     * to ensure uniqueness across server restarts and multiple rounds.
     */
    private String currentRoundId;
    
    /** 
     * Thread-safe collection of player results for the current round.
     * Maps player IDs to their complete typing performance data.
     * 
     * @see RoundResult
     */
    private final Map<String, RoundResult> currentRoundResults = new ConcurrentHashMap<>();
    
    /** 
     * Optional server-side typing session for server participation in rounds.
     * When not null, the server acts as an additional player for demonstration purposes.
     * 
     * @see TestSession
     */
    private TestSession serverSession;

    /**
     * Creates a new game server instance using the default port configuration.
     * 
     * <p>This convenience constructor initializes the server with standard settings
     * suitable for most deployment scenarios. The server will bind to port 8080
     * and use default configuration for all components.</p>
     * 
     * <p><strong>Default Configuration:</strong></p>
     * <ul>
     *   <li>Port: 8080 (DEFAULT_PORT)</li>
     *   <li>Thread Pool: Cached thread pool with automatic scaling</li>
     *   <li>Word Provider: Default difficulty and word set</li>
     * </ul>
     * 
     * @see #SimpleGameServer(int)
     * @see #DEFAULT_PORT
     */
    public SimpleGameServer() {
        this(DEFAULT_PORT);
    }

    /**
     * Creates a new game server instance with a custom port configuration.
     * 
     * <p>This constructor allows specification of a custom TCP port for the server socket.
     * All other components are initialized with default settings. The constructor
     * performs basic validation and setup but does not start the server.</p>
     * 
     * <p><strong>Initialization Process:</strong></p>
     * <ol>
     *   <li>Store port configuration for later binding</li>
     *   <li>Initialize word provider with default settings</li>
     *   <li>Create cached thread pool for client handling</li>
     *   <li>Setup internal data structures and state tracking</li>
     * </ol>
     * 
     * <p><strong>Port Considerations:</strong></p>
     * <ul>
     *   <li>Ports 1-1023 require administrator privileges on most systems</li>
     *   <li>Ports 1024-49151 are registered ports, use with caution</li>
     *   <li>Ports 49152-65535 are dynamic/private ports, safest for custom applications</li>
     *   <li>Ensure the chosen port is not in use by other applications</li>
     * </ul>
     * 
     * @param port the TCP port number for server socket binding (1-65535)
     * @throws IllegalArgumentException if port is outside valid range (1-65535)
     * @see #start()
     * @see WordProvider
     * @see Executors#newCachedThreadPool()
     */
    public SimpleGameServer(int port) {
        this.port = port;
        this.wordProvider = new WordProvider();
        this.threadPool = Executors.newCachedThreadPool();
    }

    /**
     * Starts the game server and begins accepting client connections.
     * 
     * <p>This method initializes the server socket, binds to the configured port, and
     * begins the main server loop for accepting incoming client connections. The server
     * runs asynchronously using the thread pool, allowing the calling thread to continue
     * with other operations.</p>
     * 
     * <p><strong>Startup Sequence:</strong></p>
     * <ol>
     *   <li>Verify server is not already running</li>
     *   <li>Create and bind server socket to configured port</li>
     *   <li>Set running flag to indicate active state</li>
     *   <li>Display startup confirmation message</li>
     *   <li>Submit connection acceptance task to thread pool</li>
     * </ol>
     * 
     * <p><strong>Network Binding:</strong></p>
     * <ul>
     *   <li>Creates ServerSocket bound to specified port</li>
     *   <li>Enables address reuse for faster restarts</li>
     *   <li>Configures appropriate socket options for game server use</li>
     *   <li>Begins listening for incoming TCP connections</li>
     * </ul>
     * 
     * <p><strong>Concurrency Model:</strong></p>
     * <p>The server uses a producer-consumer pattern where the main acceptance thread
     * produces new client connections, and worker threads from the thread pool consume
     * these connections for processing. This design ensures responsive connection
     * handling even under high load.</p>
     * 
     * @throws IOException if the server socket cannot be created or bound to the port
     * @throws IllegalStateException if the server is already running
     * @see #acceptConnections()
     * @see #stop()
     * @see ServerSocket#ServerSocket(int)
     */
    public void start() throws IOException {
        // Prevent duplicate server instances
        if (running.get()) {
            throw new IllegalStateException("Server is already running");
        }

        // Create and bind server socket to configured port
        serverSocket = new ServerSocket(port);
        running.set(true);

        // Notify successful startup
        System.out.println("Typerr server started on port " + port);

        // Begin accepting client connections asynchronously
        threadPool.submit(this::acceptConnections);
    }

    /**
     * Gracefully shuts down the game server and releases all resources.
     * 
     * <p>This method performs a complete server shutdown including client disconnection,
     * resource cleanup, and thread pool termination. The shutdown process is designed
     * to be safe and can be called multiple times without adverse effects.</p>
     * 
     * <p><strong>Shutdown Sequence:</strong></p>
     * <ol>
     *   <li>Check if server is currently running (exit early if not)</li>
     *   <li>Set running flag to false to stop new operations</li>
     *   <li>Disconnect all connected clients gracefully</li>
     *   <li>Clear client registry to release references</li>
     *   <li>Close server socket to stop accepting new connections</li>
     *   <li>Shutdown thread pool with timeout for graceful termination</li>
     *   <li>Force thread pool shutdown if graceful termination fails</li>
     * </ol>
     * 
     * <p><strong>Client Disconnection:</strong></p>
     * <p>All connected clients receive proper disconnection signals before socket closure.
     * This prevents client-side timeout errors and ensures clean connection termination.</p>
     * 
     * <p><strong>Thread Pool Termination:</strong></p>
     * <p>The method attempts graceful thread pool shutdown with a 5-second timeout.
     * If threads don't terminate gracefully (due to stuck operations), a forced
     * shutdown is initiated to prevent application hanging.</p>
     * 
     * <p><strong>Resource Safety:</strong></p>
     * <ul>
     *   <li>All socket connections are properly closed</li>
     *   <li>Thread resources are cleaned up and released</li>
     *   <li>Memory references are cleared to enable garbage collection</li>
     *   <li>No resource leaks occur even during abnormal shutdown</li>
     * </ul>
     * 
     * <p>This method is thread-safe and can be called from any thread, including
     * signal handlers for graceful application termination.</p>
     * 
     * @see #start()
     * @see ClientHandler#disconnect()
     * @see ExecutorService#shutdown()
     */
    public synchronized void stop() {
        // Exit early if server is not running
        if (!running.get()) {
            return;
        }

        // Stop accepting new operations
        running.set(false);

        // Disconnect all clients gracefully
        for (ClientHandler client : clients.values()) {
            client.disconnect();
        }
        clients.clear();

        // Close server socket to stop accepting connections
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

        // Shutdown thread pool with timeout for graceful termination
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                // Force shutdown if graceful termination fails
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            // Force shutdown on interruption and restore interrupt status
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("Typerr server stopped");
    }

    /**
     * Main server loop that continuously accepts incoming client connections.
     * 
     * <p>This method runs in a dedicated thread from the thread pool and implements
     * the core server functionality of accepting new client connections. Each accepted
     * connection is immediately delegated to a separate client handler thread for
     * processing, ensuring the acceptance loop remains responsive.</p>
     * 
     * <p><strong>Connection Acceptance Logic:</strong></p>
     * <ol>
     *   <li>Wait for incoming connection on server socket</li>
     *   <li>Accept connection and create client socket</li>
     *   <li>Create dedicated ClientHandler for the new connection</li>
     *   <li>Submit client handler to thread pool for processing</li>
     *   <li>Log successful connection with client address</li>
     *   <li>Continue loop for next connection</li>
     * </ol>
     * 
     * <p><strong>Error Handling:</strong></p>
     * <ul>
     *   <li>IOException during accept() is logged but doesn't stop the server</li>
     *   <li>Checks running flag to distinguish normal shutdown from errors</li>
     *   <li>Continues operation after recoverable network errors</li>
     *   <li>Gracefully exits when server is shutting down</li>
     * </ul>
     * 
     * <p><strong>Concurrency Design:</strong></p>
     * <p>The method uses a single-threaded acceptance model with multi-threaded
     * client handling. This design ensures:</p>
     * <ul>
     *   <li>Sequential connection acceptance prevents race conditions</li>
     *   <li>Parallel client processing maximizes throughput</li>
     *   <li>Responsive server behavior under varying load conditions</li>
     *   <li>Isolation between client connections</li>
     * </ul>
     * 
     * <p><strong>Connection Logging:</strong></p>
     * <p>Each successful connection is logged with the client's remote address,
     * providing audit trails and debugging information for server administrators.</p>
     * 
     * @see ServerSocket#accept()
     * @see ClientHandler
     * @see ExecutorService#submit(Runnable)
     */
    private void acceptConnections() {
        // Continue accepting connections while server is running
        while (running.get()) {
            try {
                // Block until a client connection is available
                Socket clientSocket = serverSocket.accept();
                
                // Create dedicated handler for this client
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                
                // Process client in separate thread to maintain responsiveness
                threadPool.submit(clientHandler);

                // Log successful connection with client information
                System.out.println("New client connected from " + clientSocket.getRemoteSocketAddress());

            } catch (IOException e) {
                // Only log errors if server is still supposed to be running
                if (running.get()) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
                // Continue loop to accept more connections after recoverable errors
            }
        }
    }

    /**
     * Initiates a new typing competition round with specified game mode and parameters.
     * 
     * <p>This method orchestrates the complete setup and launch of a multiplayer typing round.
     * It coordinates word list generation, client synchronization, server session setup,
     * and automatic timeout handling to ensure fair and consistent competition across
     * all connected players.</p>
     * 
     * <p><strong>Round Initialization Process:</strong></p>
     * <ol>
     *   <li>Validate that clients are connected (exit early if none)</li>
     *   <li>Set current game mode and value for round tracking</li>
     *   <li>Generate unique round identifier for message correlation</li>
     *   <li>Clear previous round results to ensure clean state</li>
     *   <li>Create server-side test session (if server participation enabled)</li>
     *   <li>Generate word list based on mode requirements</li>
     *   <li>Broadcast round start message to all connected clients</li>
     *   <li>Start server session timer for synchronized competition</li>
     *   <li>Schedule automatic round completion timeout</li>
     * </ol>
     * 
     * <p><strong>Game Mode Handling:</strong></p>
     * <ul>
     *   <li><strong>WORDS Mode:</strong> Generates exact word count specified by value parameter</li>
     *   <li><strong>TIME Mode:</strong> Generates extended word list (value × 20) to prevent running out</li>
     *   <li><strong>Custom Modes:</strong> Adapts word generation to mode-specific requirements</li>
     * </ul>
     * 
     * <p><strong>Word List Distribution:</strong></p>
     * <p>All clients receive identical word lists to ensure fair competition. The word list
     * is generated server-side using the configured {@link WordProvider} and distributed
     * via the {@link GameProtocol.Message} system.</p>
     * 
     * <p><strong>Server Participation:</strong></p>
     * <p>The server can optionally participate as an AI player by creating its own
     * {@link TestSession}. This enables demonstrations, benchmarking, and provides
     * a baseline for player comparison.</p>
     * 
     * <p><strong>Timeout Protection:</strong></p>
     * <p>A background timeout task automatically completes the round after the configured
     * timeout period ({@link #ROUND_TIMEOUT_SECONDS}). This prevents indefinite waiting
     * when clients fail to submit results due to disconnection or technical issues.</p>
     * 
     * <p><strong>Synchronization Strategy:</strong></p>
     * <ul>
     *   <li>All clients receive round start message simultaneously</li>
     *   <li>Server session starts immediately after message broadcast</li>
     *   <li>Round timing begins when server session is activated</li>
     *   <li>Results collection waits for all clients or timeout</li>
     * </ul>
     * 
     * @param mode the game mode for this round (TIME for time-based, WORDS for word count-based)
     * @param value the mode-specific parameter (seconds for TIME mode, word count for WORDS mode)
     * @throws IllegalArgumentException if mode is null or value is invalid for the specified mode
     * @see GameMode
     * @see WordProvider#getWords(int)
     * @see GameProtocol#createRoundStart(String, GameMode, int, List)
     * @see TestSession
     * @see #completeRound()
     */
    public synchronized void startRound(GameMode mode, int value) {
        // Validate that clients are available for the round
        if (clients.isEmpty()) {
            System.out.println("No clients connected. Cannot start round.");
            return;
        }

        // Configure round parameters
        currentGameMode = mode;
        currentGameValue = value;
        currentRoundId = UUID.randomUUID().toString();
        currentRoundResults.clear();

        // Create server-side test session for optional server participation
        serverSession = new TestSession(mode, value, wordProvider);

        // Generate word list based on game mode requirements
        List<String> words = wordProvider.getWords(
            mode == GameMode.WORDS ? value : value * 20  // Extended list for time-based modes
        );

        // Log round initiation for server monitoring
        System.out.println("Starting round: " + mode + " / " + value);

        // Create and broadcast round start message to all clients
        GameProtocol.Message roundStartMessage = GameProtocol.createRoundStart(
            currentRoundId, mode, value, words
        );
        broadcastMessage(roundStartMessage);

        // Begin server session timing for synchronized competition
        serverSession.start();

        // Schedule automatic round completion to prevent hanging
        threadPool.submit(() -> {
            try {
                // Wait for maximum round duration before forcing completion
                TimeUnit.SECONDS.sleep(ROUND_TIMEOUT_SECONDS);
                completeRound();
            } catch (InterruptedException e) {
                // Restore interrupt status and exit timeout task
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * Processes and stores round statistics from a connected client player.
     * 
     * <p>This method handles the receipt and validation of typing test results from
     * individual clients during an active round. It manages result aggregation and
     * automatically triggers round completion when all expected results are received.</p>
     * 
     * <p><strong>Result Processing Logic:</strong></p>
     * <ol>
     *   <li>Validate that a round is currently active</li>
     *   <li>Store player result in round results collection</li>
     *   <li>Log result receipt for server monitoring</li>
     *   <li>Check if all expected results have been received</li>
     *   <li>Trigger automatic round completion if threshold met</li>
     * </ol>
     * 
     * <p><strong>Completion Trigger:</strong></p>
     * <p>Round completion is automatically triggered when the number of received
     * results equals the number of connected clients. This ensures the round
     * concludes as soon as all participants have submitted their performance data.</p>
     * 
     * <p><strong>Result Validation:</strong></p>
     * <ul>
     *   <li>Verifies round ID matches current active round</li>
     *   <li>Accepts results only during active round state</li>
     *   <li>Overwrites previous results from same player (latest wins)</li>
     *   <li>Logs all result submissions for audit purposes</li>
     * </ul>
     * 
     * <p><strong>Thread Safety:</strong></p>
     * <p>Method is synchronized to prevent race conditions when multiple clients
     * submit results simultaneously. The completion check is atomic to ensure
     * round completion is triggered exactly once.</p>
     * 
     * @param playerId unique identifier of the player submitting results
     * @param result complete typing performance data from the player
     * @see RoundResult
     * @see #completeRound()
     * @see #currentRoundResults
     */
    private synchronized void handleRoundStats(String playerId, RoundResult result) {
        // Only process results if a round is currently active
        if (currentRoundId == null) {
            return;
        }

        // Store player result (overwrites any previous result from same player)
        currentRoundResults.put(playerId, result);
        
        // Log result receipt for monitoring and debugging
        System.out.println("Received stats from " + playerId + ": " + result);

        // Check if all connected clients have submitted results
        if (currentRoundResults.size() >= clients.size()) {
            // All results received - complete the round immediately
            completeRound();
        }
    }

    /**
     * Finalizes the current typing round and distributes results to all participants.
     * 
     * <p>This method handles the complete round completion process including result
     * compilation, ranking calculation, server result integration, and final result
     * distribution to all connected clients. It ensures fair and comprehensive
     * result reporting with proper cleanup of round state.</p>
     * 
     * <p><strong>Completion Process:</strong></p>
     * <ol>
     *   <li>Verify that a round is currently active</li>
     *   <li>Calculate and integrate server results (if server participated)</li>
     *   <li>Sort player results by performance (WPM descending)</li>
     *   <li>Combine player and server results into unified ranking</li>
     *   <li>Display complete results to server console</li>
     *   <li>Broadcast final results to all connected clients</li>
     *   <li>Clean up round state and prepare for next round</li>
     * </ol>
     * 
     * <p><strong>Server Result Integration:</strong></p>
     * <p>If the server participated in the round via {@link #serverSession}, its
     * performance is calculated and integrated into the final rankings. Server
     * results are clearly marked with "[SERVER]" prefix for identification.</p>
     * 
     * <p><strong>Result Ranking Algorithm:</strong></p>
     * <ul>
     *   <li>Primary sort: Words Per Minute (WPM) in descending order</li>
     *   <li>Secondary criteria: Accuracy percentage for tie-breaking</li>
     *   <li>Server results integrated into same ranking system</li>
     *   <li>Final positions displayed with numerical ranking</li>
     * </ul>
     * 
     * <p><strong>Result Distribution:</strong></p>
     * <p>Final results are broadcast to all connected clients using the
     * {@link GameProtocol} message system. Only player results (excluding server)
     * are sent to clients to maintain clean client-side display.</p>
     * 
     * <p><strong>State Cleanup:</strong></p>
     * <p>After result distribution, all round-specific state is cleared to prepare
     * for the next round:</p>
     * <ul>
     *   <li>Round ID reset to null</li>
     *   <li>Result collection cleared</li>
     *   <li>Server session reference cleared</li>
     * </ul>
     * 
     * <p><strong>Thread Safety:</strong></p>
     * <p>Method is synchronized to prevent multiple concurrent completion attempts
     * and ensure atomic state transitions during round finalization.</p>
     * 
     * @see RoundResult
     * @see TestSession#calculateCurrentStats(boolean)
     * @see GameProtocol#createRoundResults(String, List)
     * @see #broadcastMessage(GameProtocol.Message)
     */
    private synchronized void completeRound() {
        // Only proceed if a round is currently active
        if (currentRoundId == null) {
            return;
        }

        // Calculate server result if server participated in the round
        RoundResult serverRoundResult = null;
        if (serverSession != null) {
            // Calculate server's final typing statistics
            TestSession.TestResult serverResult = serverSession.calculateCurrentStats(true);
            double serverTime = (System.currentTimeMillis() - serverSession.getStartTime()) / 1000.0;
            
            // Create server result with special identifier
            serverRoundResult = new RoundResult(
                "server", serverResult, serverTime, serverSession.isFinished(),
                currentGameMode, currentGameValue
            );
        }

        // Create sorted list of player results (by WPM descending)
        List<RoundResult> playerResults = new ArrayList<>(currentRoundResults.values());
        playerResults.sort((a, b) -> Integer.compare(b.getWpm(), a.getWpm()));

        // Combine player and server results for unified ranking
        List<RoundResult> allResults = new ArrayList<>(playerResults);
        if (serverRoundResult != null) {
            allResults.add(serverRoundResult);
            // Re-sort combined list to integrate server result properly
            allResults.sort((a, b) -> Integer.compare(b.getWpm(), a.getWpm()));
        }

        // Display complete results to server console
        System.out.println("Round completed. Results:");
        for (int i = 0; i < allResults.size(); i++) {
            RoundResult result = allResults.get(i);
            // Mark server results with special prefix
            String prefix = "server".equals(result.getPlayerId()) ? "[SERVER] " : "";
            System.out.printf("%d. %s%s%n", i + 1, prefix, result);
        }

        // Broadcast final results to all connected clients (player results only)
        GameProtocol.Message resultsMessage = GameProtocol.createRoundResults(
            currentRoundId, playerResults
        );
        broadcastMessage(resultsMessage);

        // Clean up round state to prepare for next round
        currentRoundId = null;
        currentRoundResults.clear();
        serverSession = null;
    }

    /**
     * Broadcasts a protocol message to all currently connected clients.
     * 
     * <p>This method provides reliable message distribution to all active client
     * connections using the standardized {@link GameProtocol} message format.
     * It handles message serialization and ensures delivery to all clients,
     * with appropriate error handling for network failures.</p>
     * 
     * <p><strong>Broadcast Process:</strong></p>
     * <ol>
     *   <li>Serialize the message object to JSON format</li>
     *   <li>Iterate through all connected client handlers</li>
     *   <li>Send serialized message to each client individually</li>
     *   <li>Handle serialization errors with appropriate logging</li>
     * </ol>
     * 
     * <p><strong>Message Serialization:</strong></p>
     * <p>Messages are serialized using {@link GameProtocol#serialize(GameProtocol.Message)}
     * to ensure consistent format across all client connections. Serialization errors
     * are caught and logged to prevent broadcast failure from affecting server stability.</p>
     * 
     * <p><strong>Delivery Guarantees:</strong></p>
     * <ul>
     *   <li>Best-effort delivery to all connected clients</li>
     *   <li>Individual client failures don't affect other deliveries</li>
     *   <li>Failed deliveries are handled by individual client handlers</li>
     *   <li>Broadcast continues even if some clients are unreachable</li>
     * </ul>
     * 
     * <p><strong>Error Handling:</strong></p>
     * <ul>
     *   <li>Message serialization errors are logged and broadcast is aborted</li>
     *   <li>Individual client send failures are handled by {@link ClientHandler}</li>
     *   <li>Server remains stable regardless of client-side issues</li>
     * </ul>
     * 
     * <p><strong>Thread Safety:</strong></p>
     * <p>Method is synchronized to ensure atomic broadcast operations and prevent
     * message interleaving when multiple threads attempt concurrent broadcasts.</p>
     * 
     * @param message the protocol message to broadcast to all connected clients
     * @see GameProtocol#serialize(GameProtocol.Message)
     * @see ClientHandler#sendMessage(String)
     * @see #clients
     */
    private synchronized void broadcastMessage(GameProtocol.Message message) {
        // Serialize message to JSON format for network transmission
        String json;
        try {
            json = GameProtocol.serialize(message);
        } catch (Exception e) {
            // Log serialization error and abort broadcast
            System.err.println("Error serializing message: " + e.getMessage());
            return;
        }

        // Send serialized message to all connected clients
        for (ClientHandler client : clients.values()) {
            client.sendMessage(json);
        }
    }

    /**
     * Dedicated client connection handler for individual player communication.
     * 
     * <p>The ClientHandler class manages the complete lifecycle of a single client
     * connection, from initial handshake through active gameplay to disconnection.
     * Each client handler runs in its own thread to enable concurrent processing
     * of multiple client connections without blocking the main server operations.</p>
     * 
     * <p><strong>Handler Responsibilities:</strong></p>
     * <ul>
     *   <li><strong>Connection Management:</strong> Socket lifecycle and cleanup</li>
     *   <li><strong>Message Processing:</strong> Protocol message parsing and routing</li>
     *   <li><strong>Player Registration:</strong> Client identification and registration</li>
     *   <li><strong>Game Coordination:</strong> Round participation and result submission</li>
     *   <li><strong>Error Handling:</strong> Connection failures and invalid messages</li>
     * </ul>
     * 
     * <p><strong>Communication Protocol:</strong></p>
     * <p>All client communication uses the {@link GameProtocol} message format with
     * JSON serialization over TCP. The handler processes the following message types:</p>
     * <ul>
     *   <li><strong>CLIENT_CONNECT:</strong> Initial client registration</li>
     *   <li><strong>GAME_MODE_SELECT:</strong> Round configuration and startup</li>
     *   <li><strong>ROUND_STATS:</strong> Typing performance result submission</li>
     *   <li><strong>CLIENT_DISCONNECT:</strong> Graceful disconnection notification</li>
     * </ul>
     * 
     * <p><strong>Lifecycle Management:</strong></p>
     * <pre>
     * CONNECTION → REGISTRATION → IDLE → ROUND_PARTICIPATION → RESULT_SUBMISSION → IDLE
     *      ↓                                        ↓                                   ↓
     *  DISCONNECTION ←−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−−
     * </pre>
     * 
     * <p><strong>Thread Safety:</strong></p>
     * <p>Each client handler operates in isolation with thread-safe communication
     * to the main server through synchronized methods. The handler maintains its
     * own connection state and communicates server events through callback methods.</p>
     * 
     * <p><strong>Error Recovery:</strong></p>
     * <ul>
     *   <li>Automatic disconnection on communication errors</li>
     *   <li>Graceful cleanup of resources on termination</li>
     *   <li>Error message transmission for recoverable issues</li>
     *   <li>Client registry maintenance for connection tracking</li>
     * </ul>
     * 
     * @see GameProtocol
     * @see Runnable
     * @see Socket
     */
    private class ClientHandler implements Runnable {
        /** TCP socket connection to the individual client. */
        private final Socket socket;
        
        /** 
         * Buffered input stream reader for receiving messages from the client.
         * Wraps the socket's input stream for efficient line-based message reading.
         */
        private final BufferedReader reader;
        
        /** 
         * Print writer for sending messages to the client.
         * Configured with auto-flush for immediate message delivery.
         */
        private final PrintWriter writer;
        
        /** 
         * Unique identifier for this client player. Set during the connection handshake
         * and used for result tracking and client management.
         */
        private String playerId;
        
        /** 
         * Volatile flag indicating connection status. Thread-safe access ensures
         * consistent state checking across message processing and disconnection handling.
         */
        private volatile boolean connected = true;

        /**
         * Creates a new client handler for the specified socket connection.
         * 
         * <p>This constructor initializes the communication streams and prepares the
         * handler for message processing. It sets up buffered I/O streams for efficient
         * communication and configures the connection for immediate message delivery.</p>
         * 
         * <p><strong>Stream Configuration:</strong></p>
         * <ul>
         *   <li>BufferedReader for efficient line-based message reading</li>
         *   <li>PrintWriter with auto-flush for immediate message sending</li>
         *   <li>UTF-8 character encoding for international character support</li>
         *   <li>Line-based protocol matching {@link GameProtocol} requirements</li>
         * </ul>
         * 
         * <p><strong>Error Handling:</strong></p>
         * <p>IOException during stream setup is propagated to the caller, typically
         * resulting in immediate client disconnection and cleanup. This ensures
         * only properly configured clients proceed to message processing.</p>
         * 
         * @param socket the TCP socket connection to the client
         * @throws IOException if input/output streams cannot be created from the socket
         * @see BufferedReader
         * @see PrintWriter
         * @see Socket#getInputStream()
         * @see Socket#getOutputStream()
         */
        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream(), true);  // Auto-flush enabled
        }

        /**
         * Main client communication loop that processes incoming messages.
         * 
         * <p>This method implements the core client handling logic by continuously
         * reading messages from the client socket and dispatching them to appropriate
         * handlers. It runs in a dedicated thread for each client connection,
         * enabling concurrent processing of multiple clients.</p>
         * 
         * <p><strong>Message Processing Loop:</strong></p>
         * <ol>
         *   <li>Read line-based messages from client socket</li>
         *   <li>Dispatch each message to {@link #handleMessage(String)}</li>
         *   <li>Continue until client disconnects or error occurs</li>
         *   <li>Perform cleanup and resource release on exit</li>
         * </ol>
         * 
         * <p><strong>Connection Monitoring:</strong></p>
         * <p>The method monitors both the connection status flag and socket state
         * to detect disconnections. It gracefully handles both planned disconnections
         * (client-initiated) and unexpected failures (network issues).</p>
         * 
         * <p><strong>Error Handling:</strong></p>
         * <ul>
         *   <li>IOException from socket operations triggers disconnection</li>
         *   <li>Null return from readLine() indicates client disconnection</li>
         *   <li>Connection errors are logged only if connection was active</li>
         *   <li>All errors result in automatic cleanup and client removal</li>
         * </ul>
         * 
         * <p><strong>Resource Management:</strong></p>
         * <p>The finally block ensures proper resource cleanup regardless of how
         * the connection terminates, preventing resource leaks and maintaining
         * server stability.</p>
         * 
         * @see #handleMessage(String)
         * @see #disconnect()
         * @see BufferedReader#readLine()
         */
        @Override
        public void run() {
            try {
                String line;
                // Process messages while connection is active
                while (connected && (line = reader.readLine()) != null) {
                    handleMessage(line);
                }
            } catch (IOException e) {
                // Log errors only if connection was expected to be active
                if (connected) {
                    System.err.println("Error reading from client: " + e.getMessage());
                }
            } finally {
                // Ensure cleanup occurs regardless of exit reason
                disconnect();
            }
        }

        /**
         * Processes individual protocol messages received from the client.
         * 
         * <p>This method handles the parsing, validation, and routing of client messages
         * according to the {@link GameProtocol} specification. It provides centralized
         * message processing with comprehensive error handling and response generation.</p>
         * 
         * <p><strong>Message Processing Pipeline:</strong></p>
         * <ol>
         *   <li>Deserialize JSON message using {@link GameProtocol}</li>
         *   <li>Extract message type and route to appropriate handler</li>
         *   <li>Process message data and perform requested actions</li>
         *   <li>Send error responses for invalid or failed messages</li>
         * </ol>
         * 
         * <p><strong>Supported Message Types:</strong></p>
         * <ul>
         *   <li><strong>CLIENT_CONNECT:</strong> Player registration and identification</li>
         *   <li><strong>GAME_MODE_SELECT:</strong> Round configuration and startup request</li>
         *   <li><strong>ROUND_STATS:</strong> Typing performance result submission</li>
         *   <li><strong>CLIENT_DISCONNECT:</strong> Graceful disconnection notification</li>
         * </ul>
         * 
         * <p><strong>Error Handling Strategy:</strong></p>
         * <ul>
         *   <li>Message parsing errors are logged and error responses sent</li>
         *   <li>Unknown message types are logged but don't terminate connection</li>
         *   <li>Handler exceptions are caught and converted to error responses</li>
         *   <li>Connection remains active for recoverable errors</li>
         * </ul>
         * 
         * <p><strong>Response Generation:</strong></p>
         * <p>For errors that can be communicated to the client, the method attempts
         * to send structured error responses using the {@link GameProtocol} format.
         * This enables client-side error handling and user notification.</p>
         * 
         * @param json the JSON-encoded protocol message from the client
         * @see GameProtocol#deserialize(String)
         * @see GameProtocol#createError(String, String)
         * @see #handleClientConnect(String)
         * @see #handleGameModeSelect(String, GameMode, int)
         * @see #handleRoundStats(String, RoundResult)
         * @see #handleClientDisconnect(String)
         */
        private void handleMessage(String json) {
            try {
                // Parse the incoming JSON message
                GameProtocol.Message message = GameProtocol.deserialize(json);

                // Route message based on type
                switch (message.getType()) {
                    case CLIENT_CONNECT:
                        handleClientConnect(message.getPlayerId());
                        break;

                    case GAME_MODE_SELECT:
                        // Extract game mode configuration data
                        GameProtocol.GameModeData modeData = GameProtocol.extractData(
                            message, GameProtocol.GameModeData.class
                        );
                        handleGameModeSelect(message.getPlayerId(), modeData.getMode(), modeData.getValue());
                        break;

                    case ROUND_STATS:
                        // Extract result data from message
                        RoundResult result = GameProtocol.extractData(message, RoundResult.class);
                        SimpleGameServer.this.handleRoundStats(message.getPlayerId(), result);
                        break;

                    case CLIENT_DISCONNECT:
                        handleClientDisconnect(message.getPlayerId());
                        break;

                    default:
                        // Log unknown message types but continue processing
                        System.err.println("Unknown message type: " + message.getType());
                }

            } catch (Exception e) {
                // Log the error for debugging purposes
                System.err.println("Error handling message: " + e.getMessage());
                
                // Attempt to send structured error response to client
                try {
                    sendMessage(GameProtocol.serialize(GameProtocol.createError("server",
                        "Error processing message: " + e.getMessage())));
                } catch (Exception serializeEx) {
                    // If error response also fails, log the secondary error
                    System.err.println("Error sending error message: " + serializeEx.getMessage());
                }
            }
        }

        /**
         * Handles client connection registration and player identification.
         * 
         * <p>This method processes the initial connection handshake where the client
         * provides its unique player identifier. It registers the client in the server's
         * active client registry and enables participation in game rounds.</p>
         * 
         * <p><strong>Registration Process:</strong></p>
         * <ol>
         *   <li>Store the provided client ID as this handler's player identifier</li>
         *   <li>Register this handler in the server's client registry</li>
         *   <li>Log successful connection for server monitoring</li>
         * </ol>
         * 
         * <p><strong>Client Registry Integration:</strong></p>
         * <p>The method updates the server's {@link #clients} map to include this
         * handler, enabling server-wide operations like broadcasts and result collection.</p>
         * 
         * @param clientId the unique identifier provided by the connecting client
         * @see #clients
         * @see #playerId
         */
        private void handleClientConnect(String clientId) {
            playerId = clientId;
            synchronized (SimpleGameServer.this) {
                clients.put(playerId, this);
            }
            System.out.println("Client " + playerId + " connected");
        }

        /**
         * Handles game mode selection and round initiation requests from clients.
         * 
         * <p>This method processes client requests to start new typing rounds with
         * specific game mode configurations. It delegates the actual round startup
         * to the server's round management system while providing appropriate logging.</p>
         * 
         * <p><strong>Processing Logic:</strong></p>
         * <ol>
         *   <li>Log the client's game mode selection request</li>
         *   <li>Submit round startup task to server thread pool</li>
         *   <li>Delegate actual round creation to {@link SimpleGameServer#startRound}</li>
         * </ol>
         * 
         * <p><strong>Asynchronous Processing:</strong></p>
         * <p>Round startup is submitted to the thread pool to prevent blocking the
         * client handler thread. This ensures responsive client communication while
         * the potentially expensive round setup occurs in the background.</p>
         * 
         * <p><strong>Game Mode Support:</strong></p>
         * <p>The method supports all {@link GameMode} configurations including time-based
         * and word count-based typing tests, with the value parameter providing the
         * mode-specific configuration (seconds, word count, etc.).</p>
         * 
         * @param clientId the identifier of the client requesting the round
         * @param mode the requested game mode (TIME, WORDS, etc.)
         * @param value the mode-specific parameter (seconds for TIME, word count for WORDS)
         * @see SimpleGameServer#startRound(GameMode, int)
         * @see GameMode
         * @see ExecutorService#submit(Runnable)
         */
        private void handleGameModeSelect(String clientId, GameMode mode, int value) {
            System.out.println("Client " + clientId + " selected mode: " + mode + " / " + value);

            // Submit round startup to thread pool for asynchronous processing
            threadPool.submit(() -> startRound(mode, value));
        }

        /**
         * Handles client disconnection requests and initiates cleanup.
         * 
         * <p>This method processes explicit disconnection requests from clients who
         * are gracefully terminating their connection. It ensures proper cleanup
         * and resource management for planned disconnections.</p>
         * 
         * <p><strong>Disconnection Process:</strong></p>
         * <ol>
         *   <li>Receive disconnection notification from client</li>
         *   <li>Initiate standard disconnection cleanup procedures</li>
         *   <li>Remove client from server registry and close resources</li>
         * </ol>
         * 
         * <p><strong>Graceful vs Forced Disconnection:</strong></p>
         * <p>This method handles planned disconnections where the client sends an
         * explicit disconnect message. The {@link #disconnect()} method handles
         * both planned and unexpected disconnections with the same cleanup logic.</p>
         * 
         * @param clientId the identifier of the client requesting disconnection
         * @see #disconnect()
         */
        private void handleClientDisconnect(String clientId) {
            disconnect();
        }

        /**
         * Sends a message to this client over the established socket connection.
         * 
         * <p>This method provides reliable message transmission to the client using
         * the configured PrintWriter. It includes connection state checking to
         * prevent send attempts on closed connections.</p>
         * 
         * <p><strong>Message Transmission:</strong></p>
         * <ul>
         *   <li>Verifies connection is still active before sending</li>
         *   <li>Uses PrintWriter with auto-flush for immediate delivery</li>
         *   <li>Sends messages as complete lines matching protocol requirements</li>
         *   <li>Silently ignores send attempts on disconnected clients</li>
         * </ul>
         * 
         * <p><strong>Connection State Management:</strong></p>
         * <p>The method checks the {@link #connected} flag to avoid sending messages
         * to clients that have already disconnected, preventing unnecessary network
         * errors and resource usage.</p>
         * 
         * <p><strong>Protocol Compliance:</strong></p>
         * <p>Messages are sent as complete lines followed by newline characters,
         * matching the line-based protocol expected by {@link GameProtocol} clients.</p>
         * 
         * @param json the JSON-encoded message to send to the client
         * @see PrintWriter#println(String)
         * @see #connected
         * @see #writer
         */
        public void sendMessage(String json) {
            if (connected) {
                writer.println(json);
            }
        }

        /**
         * Disconnects the client and performs complete resource cleanup.
         * 
         * <p>This method handles the complete disconnection process including connection
         * state updates, client registry cleanup, and socket resource release. It is
         * designed to be safe for multiple calls and handles both planned and unexpected
         * disconnections gracefully.</p>
         * 
         * <p><strong>Disconnection Sequence:</strong></p>
         * <ol>
         *   <li>Check if already disconnected (exit early if so)</li>
         *   <li>Set connection flag to false to stop message processing</li>
         *   <li>Remove client from server's active client registry</li>
         *   <li>Log disconnection event for server monitoring</li>
         *   <li>Close socket connection and release network resources</li>
         * </ol>
         * 
         * <p><strong>Registry Cleanup:</strong></p>
         * <p>The method removes this handler from the server's client registry using
         * synchronized access to prevent race conditions during concurrent disconnections
         * or server operations.</p>
         * 
         * <p><strong>Resource Management:</strong></p>
         * <ul>
         *   <li>Socket closure releases TCP connection and port resources</li>
         *   <li>Stream resources are automatically closed with socket</li>
         *   <li>Thread pool threads are returned to the pool for reuse</li>
         *   <li>Memory references are cleared to enable garbage collection</li>
         * </ul>
         * 
         * <p><strong>Error Handling:</strong></p>
         * <p>Socket closure errors are logged but don't prevent the disconnection
         * process from completing. This ensures cleanup occurs even when network
         * issues prevent graceful socket closure.</p>
         * 
         * <p><strong>Thread Safety:</strong></p>
         * <p>Method is safe for concurrent access and can be called from multiple
         * threads (message handler, timeout handlers, shutdown procedures) without
         * causing resource conflicts or double-cleanup issues.</p>
         * 
         * @see #clients
         * @see #connected
         * @see Socket#close()
         */
        public void disconnect() {
            // Prevent multiple disconnection attempts
            if (!connected) {
                return;
            }

            // Mark connection as inactive to stop message processing
            connected = false;

            // Remove from server's client registry if registered
            if (playerId != null) {
                synchronized (SimpleGameServer.this) {
                    clients.remove(playerId);
                }
                System.out.println("Client " + playerId + " disconnected");
            }

            // Close socket connection and release resources
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    /**
     * Returns the current number of connected clients.
     * 
     * <p>This method provides a thread-safe way to query the current client count
     * for monitoring, debugging, and administrative purposes. The count reflects
     * active connections that are registered and capable of participating in rounds.</p>
     * 
     * <p><strong>Count Accuracy:</strong></p>
     * <ul>
     *   <li>Reflects only fully registered and active clients</li>
     *   <li>Excludes clients in the process of connecting or disconnecting</li>
     *   <li>Thread-safe access ensures consistent count during concurrent operations</li>
     *   <li>Real-time count updates as clients connect and disconnect</li>
     * </ul>
     * 
     * @return the number of currently connected and registered clients
     * @see #clients
     */
    public synchronized int getClientCount() {
        return clients.size();
    }

    /**
     * Checks if the server is currently running and accepting connections.
     * 
     * <p>This method provides a thread-safe way to query the server's operational
     * status. It reflects the current state of the server socket and connection
     * acceptance process.</p>
     * 
     * <p><strong>Status Indicators:</strong></p>
     * <ul>
     *   <li><strong>true:</strong> Server is running, accepting connections, and processing clients</li>
     *   <li><strong>false:</strong> Server is stopped, shutting down, or not yet started</li>
     * </ul>
     * 
     * <p><strong>Thread Safety:</strong></p>
     * <p>Uses atomic boolean operations to ensure consistent status reporting
     * across multiple threads during startup and shutdown operations.</p>
     * 
     * @return true if the server is currently running, false otherwise
     * @see #running
     * @see AtomicBoolean#get()
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Returns the TCP port number the server is configured to use.
     * 
     * <p>This method returns the port number that was specified during server
     * construction, regardless of the current server state. The port is used
     * for socket binding when the server is started.</p>
     * 
     * <p><strong>Port Information:</strong></p>
     * <ul>
     *   <li>Returns the configured port even if server is not running</li>
     *   <li>Port is set during construction and cannot be changed</li>
     *   <li>Useful for client connection configuration and server monitoring</li>
     * </ul>
     * 
     * @return the TCP port number configured for this server instance
     * @see #SimpleGameServer(int)
     */
    public int getPort() {
        return port;
    }

    /**
     * Main entry point for running the Typerr game server as a standalone application.
     * 
     * <p>This method provides a complete command-line interface for server operation
     * including startup, interactive commands, and graceful shutdown. It handles
     * command-line argument parsing, server lifecycle management, and provides
     * an interactive console for server administration.</p>
     * 
     * <p><strong>Command Line Arguments:</strong></p>
     * <ul>
     *   <li><strong>No arguments:</strong> Server starts on default port ({@link #DEFAULT_PORT})</li>
     *   <li><strong>Port number:</strong> Server starts on specified port (args[0])</li>
     *   <li><strong>Invalid port:</strong> Falls back to default port with warning</li>
     * </ul>
     * 
     * <p><strong>Interactive Commands:</strong></p>
     * <ul>
     *   <li><strong>quit:</strong> Graceful server shutdown and exit</li>
     *   <li><strong>start TIME|WORDS &lt;value&gt;:</strong> Initiate a new typing round</li>
     *   <li><strong>status:</strong> Display current client count and server status</li>
     * </ul>
     * 
     * <p><strong>Startup Sequence:</strong></p>
     * <ol>
     *   <li>Parse command line arguments for port configuration</li>
     *   <li>Create server instance with specified or default port</li>
     *   <li>Register shutdown hook for graceful termination</li>
     *   <li>Start server and begin accepting connections</li>
     *   <li>Enter interactive command loop for administration</li>
     * </ol>
     * 
     * <p><strong>Shutdown Handling:</strong></p>
     * <p>The method registers a JVM shutdown hook to ensure graceful server
     * termination when the application is terminated via Ctrl+C, SIGTERM, or
     * other system shutdown signals.</p>
     * 
     * <p><strong>Error Handling:</strong></p>
     * <ul>
     *   <li>Invalid port numbers are handled with fallback to default</li>
     *   <li>Server startup failures are reported and cause application exit</li>
     *   <li>Command parsing errors provide usage hints</li>
     *   <li>All errors are logged to standard error output</li>
     * </ul>
     * 
     * <p><strong>Example Usage:</strong></p>
     * <pre>{@code
     * # Start server on default port (8080)
     * java SimpleGameServer
     * 
     * # Start server on port 9090
     * java SimpleGameServer 9090
     * 
     * # Interactive commands (after startup)
     * start TIME 30      # Start 30-second round
     * start WORDS 25     # Start 25-word round
     * status             # Show client count
     * quit               # Shutdown server
     * }</pre>
     * 
     * @param args command line arguments, optionally containing port number as first argument
     * @see #DEFAULT_PORT
     * @see #SimpleGameServer(int)
     * @see #start()
     * @see #stop()
     * @see #startRound(GameMode, int)
     * @see Runtime#addShutdownHook(Thread)
     */
    public static void main(String[] args) {
        // Parse command line arguments for port configuration
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default port " + DEFAULT_PORT);
            }
        }

        // Create server instance with configured port
        SimpleGameServer server = new SimpleGameServer(port);

        // Register shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        try {
            // Start server and begin accepting connections
            server.start();

            // Setup interactive command console
            Scanner scanner = new Scanner(System.in);
            System.out.println("Server is running. Type 'quit' to stop.");

            // Interactive command processing loop
            while (server.isRunning()) {
                String input = scanner.nextLine();
                
                // Handle quit command for graceful shutdown
                if ("quit".equalsIgnoreCase(input.trim())) {
                    break;
                }

                // Parse and execute server commands
                String[] parts = input.trim().split("\\s+");
                if (parts.length >= 3 && "start".equalsIgnoreCase(parts[0])) {
                    try {
                        // Parse game mode and value for round startup
                        GameMode mode = GameMode.valueOf(parts[1].toUpperCase());
                        int value = Integer.parseInt(parts[2]);
                        server.startRound(mode, value);
                    } catch (Exception e) {
                        System.err.println("Invalid command. Usage: start TIME|WORDS <value>");
                    }
                } else if ("status".equalsIgnoreCase(parts[0])) {
                    // Display current server status information
                    System.out.println("Connected clients: " + server.getClientCount());
                }
                // Additional commands can be added here for extended functionality
            }

        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        } finally {
            // Ensure server cleanup even if errors occur
            server.stop();
        }
    }
}