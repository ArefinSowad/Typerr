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

public class SimpleGameClient {

    private static final String DEFAULT_HOST = "localhost";

    private static final int DEFAULT_PORT = 8080;

    private final String host;

    private final int port;

    private final String playerId;

    private Socket socket;

    private BufferedReader reader;

    private PrintWriter writer;

    private final ExecutorService threadPool;

    private final AtomicBoolean connected = new AtomicBoolean(false);

    private TestSession currentSession;

    private String currentRoundId;

    private GameMode currentGameMode;

    private int currentGameValue;

    private RoundStartCallback roundStartCallback;

    private RoundResultsCallback roundResultsCallback;

    private ConnectionStatusCallback connectionStatusCallback;

    public interface RoundStartCallback {

        void onRoundStart(String roundId, GameMode mode, int value, List<String> words);
    }

    public interface RoundResultsCallback {

        void onRoundResults(String roundId, List<RoundResult> results);
    }

    public interface ConnectionStatusCallback {

        void onConnectionStatusChanged(boolean connected);
    }

    public SimpleGameClient(String playerId) {
        this(DEFAULT_HOST, DEFAULT_PORT, playerId);
    }

    public SimpleGameClient(String host, int port, String playerId) {
        this.host = host;
        this.port = port;
        this.playerId = playerId;
        this.threadPool = Executors.newCachedThreadPool();
    }

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

    public void disconnect() {
        if (!connected.get()) {
            return;
        }

        connected.set(false);

        try {
            sendMessage(GameProtocol.createClientDisconnect(playerId));
        } catch (Exception e) {

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

    public void selectGameMode(GameMode mode, int value) throws IOException {
        if (!connected.get()) {
            throw new IllegalStateException("Client is not connected");
        }

        sendMessage(GameProtocol.createGameModeSelect(playerId, mode, value));
        System.out.println("Selected game mode: " + mode + " / " + value);
    }

    public void sendRoundStats(RoundResult result) throws IOException {
        if (!connected.get()) {
            throw new IllegalStateException("Client is not connected");
        }

        sendMessage(GameProtocol.createRoundStats(playerId, result));
        System.out.println("Sent round stats: " + result);
    }

    public TestSession createTestSession() {
        if (currentGameMode == null) {
            throw new IllegalStateException("No active round");
        }

        WordProvider wordProvider = new WordProvider();
        currentSession = new TestSession(currentGameMode, currentGameValue, wordProvider);
        return currentSession;
    }

    public TestSession getCurrentSession() {
        return currentSession;
    }

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

    private void handleError(GameProtocol.Message message) {
        String errorMessage = (String) message.getData();
        System.err.println("Server error: " + errorMessage);
    }

    private void sendMessage(GameProtocol.Message message) throws IOException {
        if (!connected.get()) {
            throw new IllegalStateException("Client is not connected");
        }

        String json = GameProtocol.serialize(message);
        writer.println(json);
    }

    public void setRoundStartCallback(RoundStartCallback callback) {
        this.roundStartCallback = callback;
    }

    public void setRoundResultsCallback(RoundResultsCallback callback) {
        this.roundResultsCallback = callback;
    }

    public void setConnectionStatusCallback(ConnectionStatusCallback callback) {
        this.connectionStatusCallback = callback;
    }

    public boolean isConnected() {
        return connected.get();
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

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