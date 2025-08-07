package com.typerr.network;

import com.typerr.statics.GameMode;
import com.typerr.TestSession;
import com.typerr.WordProvider;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimpleGameServer {

    private static final int DEFAULT_PORT = 8080;

    private static final int ROUND_TIMEOUT_SECONDS = 300;

    private final int port;

    private final WordProvider wordProvider;

    private ServerSocket serverSocket;

    private final ExecutorService threadPool;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    private GameMode currentGameMode;

    private int currentGameValue;

    private String currentRoundId;

    private final Map<String, RoundResult> currentRoundResults = new ConcurrentHashMap<>();

    private TestSession serverSession;

    public SimpleGameServer() {
        this(DEFAULT_PORT);
    }

    public SimpleGameServer(int port) {
        this.port = port;
        this.wordProvider = new WordProvider();
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void start() throws IOException {

        if (running.get()) {
            throw new IllegalStateException("Server is already running");
        }

        serverSocket = new ServerSocket(port);
        running.set(true);

        System.out.println("Typerr server started on port " + port);

        threadPool.submit(this::acceptConnections);
    }

    public synchronized void stop() {

        if (!running.get()) {
            return;
        }

        running.set(false);

        for (ClientHandler client : clients.values()) {
            client.disconnect();
        }
        clients.clear();

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
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

        System.out.println("Typerr server stopped");
    }

    private void acceptConnections() {

        while (running.get()) {
            try {

                Socket clientSocket = serverSocket.accept();

                ClientHandler clientHandler = new ClientHandler(clientSocket);

                threadPool.submit(clientHandler);

                System.out.println("New client connected from " + clientSocket.getRemoteSocketAddress());

            } catch (IOException e) {

                if (running.get()) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }

            }
        }
    }

    public synchronized void startRound(GameMode mode, int value) {

        if (clients.isEmpty()) {
            System.out.println("No clients connected. Cannot start round.");
            return;
        }

        currentGameMode = mode;
        currentGameValue = value;
        currentRoundId = UUID.randomUUID().toString();
        currentRoundResults.clear();

        serverSession = new TestSession(mode, value, wordProvider);

        List<String> words = wordProvider.getWords(
            mode == GameMode.WORDS ? value : value * 20
        );

        System.out.println("Starting round: " + mode + " / " + value);

        GameProtocol.Message roundStartMessage = GameProtocol.createRoundStart(
            currentRoundId, mode, value, words
        );
        broadcastMessage(roundStartMessage);

        serverSession.start();

        threadPool.submit(() -> {
            try {

                TimeUnit.SECONDS.sleep(ROUND_TIMEOUT_SECONDS);
                completeRound();
            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
            }
        });
    }

    private synchronized void handleRoundStats(String playerId, RoundResult result) {

        if (currentRoundId == null) {
            return;
        }

        currentRoundResults.put(playerId, result);

        System.out.println("Received stats from " + playerId + ": " + result);

        if (currentRoundResults.size() >= clients.size()) {

            completeRound();
        }
    }

    private synchronized void completeRound() {

        if (currentRoundId == null) {
            return;
        }

        RoundResult serverRoundResult = null;
        if (serverSession != null) {

            TestSession.TestResult serverResult = serverSession.calculateCurrentStats(true);
            double serverTime = (System.currentTimeMillis() - serverSession.getStartTime()) / 1000.0;

            serverRoundResult = new RoundResult(
                "server", serverResult, serverTime, serverSession.isFinished(),
                currentGameMode, currentGameValue
            );
        }

        List<RoundResult> playerResults = new ArrayList<>(currentRoundResults.values());
        playerResults.sort((a, b) -> Integer.compare(b.getWpm(), a.getWpm()));

        List<RoundResult> allResults = new ArrayList<>(playerResults);
        if (serverRoundResult != null) {
            allResults.add(serverRoundResult);

            allResults.sort((a, b) -> Integer.compare(b.getWpm(), a.getWpm()));
        }

        System.out.println("Round completed. Results:");
        for (int i = 0; i < allResults.size(); i++) {
            RoundResult result = allResults.get(i);

            String prefix = "server".equals(result.getPlayerId()) ? "[SERVER] " : "";
            System.out.printf("%d. %s%s%n", i + 1, prefix, result);
        }

        GameProtocol.Message resultsMessage = GameProtocol.createRoundResults(
            currentRoundId, playerResults
        );
        broadcastMessage(resultsMessage);

        currentRoundId = null;
        currentRoundResults.clear();
        serverSession = null;
    }

    private synchronized void broadcastMessage(GameProtocol.Message message) {

        String json;
        try {
            json = GameProtocol.serialize(message);
        } catch (Exception e) {

            System.err.println("Error serializing message: " + e.getMessage());
            return;
        }

        for (ClientHandler client : clients.values()) {
            client.sendMessage(json);
        }
    }

    private class ClientHandler implements Runnable {

        private final Socket socket;

        private final BufferedReader reader;

        private final PrintWriter writer;

        private String playerId;

        private volatile boolean connected = true;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream(), true);
        }

        @Override
        public void run() {
            try {
                String line;

                while (connected && (line = reader.readLine()) != null) {
                    handleMessage(line);
                }
            } catch (IOException e) {

                if (connected) {
                    System.err.println("Error reading from client: " + e.getMessage());
                }
            } finally {

                disconnect();
            }
        }

        private void handleMessage(String json) {
            try {

                GameProtocol.Message message = GameProtocol.deserialize(json);

                switch (message.getType()) {
                    case CLIENT_CONNECT:
                        handleClientConnect(message.getPlayerId());
                        break;

                    case GAME_MODE_SELECT:

                        GameProtocol.GameModeData modeData = GameProtocol.extractData(
                            message, GameProtocol.GameModeData.class
                        );
                        handleGameModeSelect(message.getPlayerId(), modeData.getMode(), modeData.getValue());
                        break;

                    case ROUND_STATS:

                        RoundResult result = GameProtocol.extractData(message, RoundResult.class);
                        SimpleGameServer.this.handleRoundStats(message.getPlayerId(), result);
                        break;

                    case CLIENT_DISCONNECT:
                        handleClientDisconnect(message.getPlayerId());
                        break;

                    default:

                        System.err.println("Unknown message type: " + message.getType());
                }

            } catch (Exception e) {

                System.err.println("Error handling message: " + e.getMessage());

                try {
                    sendMessage(GameProtocol.serialize(GameProtocol.createError("server",
                        "Error processing message: " + e.getMessage())));
                } catch (Exception serializeEx) {

                    System.err.println("Error sending error message: " + serializeEx.getMessage());
                }
            }
        }

        private void handleClientConnect(String clientId) {
            playerId = clientId;
            synchronized (SimpleGameServer.this) {
                clients.put(playerId, this);
            }
            System.out.println("Client " + playerId + " connected");
        }

        private void handleGameModeSelect(String clientId, GameMode mode, int value) {
            System.out.println("Client " + clientId + " selected mode: " + mode + " / " + value);

            threadPool.submit(() -> startRound(mode, value));
        }

        private void handleClientDisconnect(String clientId) {
            disconnect();
        }

        public void sendMessage(String json) {
            if (connected) {
                writer.println(json);
            }
        }

        public void disconnect() {

            if (!connected) {
                return;
            }

            connected = false;

            if (playerId != null) {
                synchronized (SimpleGameServer.this) {
                    clients.remove(playerId);
                }
                System.out.println("Client " + playerId + " disconnected");
            }

            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    public synchronized int getClientCount() {
        return clients.size();
    }

    public boolean isRunning() {
        return running.get();
    }

    public int getPort() {
        return port;
    }

    public static void main(String[] args) {

        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default port " + DEFAULT_PORT);
            }
        }

        SimpleGameServer server = new SimpleGameServer(port);

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        try {

            server.start();

            Scanner scanner = new Scanner(System.in);
            System.out.println("Server is running. Type 'quit' to stop.");

            while (server.isRunning()) {
                String input = scanner.nextLine();

                if ("quit".equalsIgnoreCase(input.trim())) {
                    break;
                }

                String[] parts = input.trim().split("\\s+");
                if (parts.length >= 3 && "start".equalsIgnoreCase(parts[0])) {
                    try {

                        GameMode mode = GameMode.valueOf(parts[1].toUpperCase());
                        int value = Integer.parseInt(parts[2]);
                        server.startRound(mode, value);
                    } catch (Exception e) {
                        System.err.println("Invalid command. Usage: start TIME|WORDS <value>");
                    }
                } else if ("status".equalsIgnoreCase(parts[0])) {

                    System.out.println("Connected clients: " + server.getClientCount());
                }

            }

        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        } finally {

            server.stop();
        }
    }
}