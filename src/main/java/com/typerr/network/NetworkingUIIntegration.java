package com.typerr.network;

import com.typerr.statics.GameMode;
import com.typerr.TestSession;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * UI integration layer for multiplayer networking functionality in the Typerr application.
 * 
 * <p>This class provides a complete JavaFX-based user interface for managing multiplayer
 * typing test sessions, including both server hosting and client connection capabilities.
 * It serves as the bridge between the networking components ({@link SimpleGameServer}
 * and {@link SimpleGameClient}) and the main application UI.</p>
 * 
 * <h3>Core Functionality:</h3>
 * <ul>
 *   <li><strong>Server Management:</strong> Start/stop local game servers with configurable ports</li>
 *   <li><strong>Client Connection:</strong> Connect to remote servers with customizable settings</li>
 *   <li><strong>Game Coordination:</strong> Initiate multiplayer rounds with various game modes</li>
 *   <li><strong>Real-time Status:</strong> Live updates on connection status and game progress</li>
 *   <li><strong>Player Management:</strong> Display connected players and session information</li>
 * </ul>
 * 
 * <h3>UI Architecture:</h3>
 * <p>The integration provides a structured interface with distinct sections:</p>
 * <ul>
 *   <li><strong>Server Setup:</strong> Controls for hosting local game servers</li>
 *   <li><strong>Client Connection:</strong> Interface for joining remote servers</li>
 *   <li><strong>Game Controls:</strong> Round initiation and game mode selection</li>
 *   <li><strong>Player Status:</strong> Real-time display of connected players and game phase</li>
 * </ul>
 * 
 * <h3>Event Handling:</h3>
 * <p>The class integrates with the application's typing test system through callback mechanisms:</p>
 * <ul>
 *   <li>Automatic test session creation when rounds begin</li>
 *   <li>Results transmission to servers upon completion</li>
 *   <li>Real-time UI updates based on network events</li>
 *   <li>Error handling and user feedback for network issues</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Create networking UI integration
 * NetworkingUIIntegration networking = new NetworkingUIIntegration();
 * 
 * // Set up callbacks for main application
 * networking.setOnTestSessionCreated(session -> {
 *     mainUI.startTypingTest(session);
 * });
 * 
 * networking.setOnRoundResultsReceived(results -> {
 *     mainUI.showMultiplayerResults(results);
 * });
 * 
 * // Create and display networking panel
 * VBox networkingPanel = networking.createNetworkingPanel();
 * scene.setRoot(networkingPanel);
 * }</pre>
 * 
 * @author ArefinSowad
 * @version 1.0
 * @since 1.0
 * @see SimpleGameServer
 * @see SimpleGameClient
 * @see RoundResult
 * @see TestSession
 */
public class NetworkingUIIntegration {

    /** The local game server instance when hosting multiplayer sessions. */
    private SimpleGameServer server;
    
    /** The game client instance when connecting to remote servers. */
    private SimpleGameClient client;
    
    /** Flag indicating whether this instance is currently acting as a server. */
    private boolean isServer = false;

    /** Callback invoked when a new test session is created for multiplayer rounds. */
    private Consumer<TestSession> onTestSessionCreated;
    
    /** Callback invoked when round results are received from the server. */
    private Consumer<List<RoundResult>> onRoundResultsReceived;

    /** UI label displaying the current number of connected players. */
    private Label playerCountLabel;
    
    /** UI container for listing individual connected players. */
    private VBox playerList;
    
    /** UI label showing the current game/round status. */
    private Label gameStatusLabel;
    
    /** UI label displaying the current game phase (waiting, in progress, results). */
    private Label gamePhaseLabel;
    
    /** UI label showing server status (running, stopped, errors). */
    private Label serverStatusLabel;
    
    /** UI label showing client connection status. */
    private Label clientStatusLabel;

    /**
     * Enumeration representing the current phase of a multiplayer game session.
     * 
     * <p>This enum tracks the progression of multiplayer rounds from initial
     * setup through completion, providing clear visual feedback to users about
     * the current state of the game session.</p>
     */
    private enum GamePhase {
        /** Initial state when waiting for players to join or ready up. */
        WAITING_FOR_PLAYERS("⏳ Waiting for players"),
        
        /** Active state when a typing round is currently in progress. */
        ROUND_IN_PROGRESS("🎯 Round in progress"),
        
        /** Completion state when results are available for viewing. */
        RESULTS_READY("✅ Results ready");

        /** Human-readable display text for this game phase. */
        private final String displayText;

        /**
         * Creates a game phase with associated display text.
         * 
         * @param displayText the text to display in the UI for this phase
         */
        GamePhase(String displayText) {
            this.displayText = displayText;
        }

        /**
         * Gets the display text for this game phase.
         * 
         * @return the human-readable display text
         */
        public String getDisplayText() {
            return displayText;
        }
    }

    /** The current game phase for UI state management. */
    private GamePhase currentPhase = GamePhase.WAITING_FOR_PLAYERS;

    /**
     * Creates the main networking panel with all multiplayer controls.
     * 
     * <p>This method constructs a comprehensive JavaFX interface containing
     * all necessary controls for multiplayer functionality. The panel includes
     * server setup, client connection, game controls, and status displays
     * organized in a logical, user-friendly layout.</p>
     * 
     * <p>The panel structure includes:</p>
     * <ul>
     *   <li>Title and branding elements</li>
     *   <li>Server hosting controls with port configuration</li>
     *   <li>Client connection interface with host/port/player ID inputs</li>
     *   <li>Game mode selection and round initiation controls</li>
     *   <li>Real-time status displays for all components</li>
     * </ul>
     * 
     * @return a VBox containing the complete networking interface
     */
    public VBox createNetworkingPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20, 50, 20, 50));
        panel.setAlignment(Pos.CENTER);
        panel.getStyleClass().add("menu-pane");

        Label titleLabel = new Label("🌐 Multiplayer Mode");
        titleLabel.getStyleClass().add("menu-title");

        VBox serverSection = createServerSection();

        VBox clientSection = createClientSection();

        VBox gameSection = createGameSection();

        VBox playerStatusSection = createPlayerStatusSection();

        panel.getChildren().addAll(
            titleLabel,
            serverSection,
            clientSection,
            gameSection
        );

        return panel;
    }

    /**
     * Creates the server setup section of the networking interface.
     * 
     * <p>This section provides controls for starting and stopping local game servers,
     * including port configuration and status monitoring. Users can host multiplayer
     * sessions that other players can join via the client connection interface.</p>
     * 
     * @return a VBox containing server setup controls and status display
     */
    private VBox createServerSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("menu-group");

        Label sectionTitle = new Label("Server Setup");
        sectionTitle.getStyleClass().add("section-title");

        HBox serverControls = new HBox(10);
        serverControls.setAlignment(Pos.CENTER);

        Label portLabel = new Label("Port:");
        portLabel.getStyleClass().add("stat-title");

        TextField serverPortField = new TextField("8080");
        serverPortField.setPrefWidth(80);
        serverPortField.getStyleClass().add("game-input");

        Button startServerButton = new Button("Start Server");
        startServerButton.getStyleClass().add("mode-button");

        serverStatusLabel = new Label("Server stopped");
        serverStatusLabel.getStyleClass().add("stat-value");

        startServerButton.setOnAction(e -> {
            if (server == null || !server.isRunning()) {
                try {
                    int port = Integer.parseInt(serverPortField.getText());
                    server = new SimpleGameServer(port);
                    server.start();
                    isServer = true;

                    startServerButton.setText("Stop Server");
                    startServerButton.getStyleClass().remove("mode-button");
                    startServerButton.getStyleClass().add("selected-difficulty-button");
                    serverStatusLabel.setText("🟢 Server running on port " + port);

                } catch (NumberFormatException ex) {
                    showInlineError(serverStatusLabel, "Invalid port number");
                } catch (IOException ex) {
                    showInlineError(serverStatusLabel, "Failed to start server: " + ex.getMessage());
                }
            } else {
                server.stop();
                server = null;
                isServer = false;

                startServerButton.setText("Start Server");
                startServerButton.getStyleClass().remove("selected-difficulty-button");
                startServerButton.getStyleClass().add("mode-button");
                serverStatusLabel.setText("🔴 Server stopped");
            }
        });

        serverControls.getChildren().addAll(portLabel, serverPortField, startServerButton);
        section.getChildren().addAll(sectionTitle, serverControls, serverStatusLabel);

        return section;
    }

    /**
     * Creates the client connection section of the networking interface.
     * 
     * <p>This section provides controls for connecting to remote game servers,
     * including host/port configuration, player ID customization, and connection
     * status monitoring. Users can join multiplayer sessions hosted by other players.</p>
     * 
     * @return a VBox containing client connection controls and status display
     */
    private VBox createClientSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("menu-group");

        Label sectionTitle = new Label("Client Connection");
        sectionTitle.getStyleClass().add("section-title");

        HBox clientControls = new HBox(10);
        clientControls.setAlignment(Pos.CENTER);

        Label hostLabel = new Label("Host:");
        hostLabel.getStyleClass().add("stat-title");
        TextField hostField = new TextField("localhost");
        hostField.setPrefWidth(100);
        hostField.getStyleClass().add("game-input");

        Label portLabel = new Label("Port:");
        portLabel.getStyleClass().add("stat-title");
        TextField portField = new TextField("8080");
        portField.setPrefWidth(80);
        portField.getStyleClass().add("game-input");

        Label playerLabel = new Label("Player ID:");
        playerLabel.getStyleClass().add("stat-title");
        TextField playerIdField = new TextField("player" + System.currentTimeMillis() % 1000);
        playerIdField.setPrefWidth(100);
        playerIdField.getStyleClass().add("game-input");

        Button connectButton = new Button("Connect");
        connectButton.getStyleClass().add("mode-button");

        clientStatusLabel = new Label("Disconnected");
        clientStatusLabel.getStyleClass().add("stat-value");

        connectButton.setOnAction(e -> {
            if (client == null || !client.isConnected()) {
                try {
                    String host = hostField.getText();
                    int port = Integer.parseInt(portField.getText());
                    String playerId = playerIdField.getText();

                    client = new SimpleGameClient(host, port, playerId);
                    setupClientCallbacks();
                    client.connect();

                    connectButton.setText("Disconnect");
                    connectButton.getStyleClass().remove("mode-button");
                    connectButton.getStyleClass().add("selected-difficulty-button");
                    clientStatusLabel.setText("🟢 Connected to " + host + ":" + port);

                } catch (NumberFormatException ex) {
                    showInlineError(clientStatusLabel, "Invalid port number");
                } catch (IOException ex) {
                    showInlineError(clientStatusLabel, "Failed to connect: " + ex.getMessage());
                }
            } else {
                client.disconnect();
                client = null;

                connectButton.setText("Connect");
                connectButton.getStyleClass().remove("selected-difficulty-button");
                connectButton.getStyleClass().add("mode-button");
                clientStatusLabel.setText("🔴 Disconnected");
            }
        });

        clientControls.getChildren().addAll(
            hostLabel, hostField,
            portLabel, portField,
            playerLabel, playerIdField,
            connectButton
        );
        section.getChildren().addAll(sectionTitle, clientControls, clientStatusLabel);

        return section;
    }

    /**
     * Creates the game controls section of the networking interface.
     * 
     * <p>This section provides controls for initiating multiplayer rounds,
     * including game mode selection (TIME/WORDS), value configuration, and
     * round start functionality. The controls adapt based on whether the user
     * is hosting a server or connected as a client.</p>
     * 
     * @return a VBox containing game control elements and status display
     */
    private VBox createGameSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("menu-group");

        Label sectionTitle = new Label("Game Controls");
        sectionTitle.getStyleClass().add("section-title");

        HBox gameControls = new HBox(10);
        gameControls.setAlignment(Pos.CENTER);

        Label modeLabel = new Label("Mode:");
        modeLabel.getStyleClass().add("stat-title");
        ComboBox<GameMode> gameModeCombo = new ComboBox<>();
        gameModeCombo.getItems().addAll(GameMode.values());
        gameModeCombo.setValue(GameMode.TIME);
        gameModeCombo.getStyleClass().add("game-input");

        Label valueLabel = new Label("Value:");
        valueLabel.getStyleClass().add("stat-title");
        TextField gameValueField = new TextField("30");
        gameValueField.setPrefWidth(60);
        gameValueField.getStyleClass().add("game-input");

        Button startGameButton = new Button("🎮 Start Round");
        startGameButton.getStyleClass().add("mode-button");

        gameStatusLabel = new Label("Ready to start");
        gameStatusLabel.getStyleClass().add("stat-value");

        startGameButton.setOnAction(e -> {
            if (client != null && client.isConnected()) {
                try {
                    GameMode mode = gameModeCombo.getValue();
                    int value = Integer.parseInt(gameValueField.getText());
                    client.selectGameMode(mode, value);
                    updateGameStatus("🎯 Round starting...");

                    Timeline roundStartTimeout = new Timeline(new KeyFrame(
                        Duration.seconds(10),
                        event -> {
                            if ("🎯 Round starting...".equals(gameStatusLabel.getText())) {
                                updateGameStatus("❌ Round failed to start");
                            }
                        }
                    ));
                    roundStartTimeout.play();

                } catch (NumberFormatException ex) {
                    showInlineError(gameStatusLabel, "Invalid game value");
                } catch (IOException ex) {
                    showInlineError(gameStatusLabel, "Failed to start game: " + ex.getMessage());
                }
            } else if (server != null && server.isRunning()) {
                try {
                    GameMode mode = gameModeCombo.getValue();
                    int value = Integer.parseInt(gameValueField.getText());
                    server.startRound(mode, value);
                    updateGameStatus("🎯 Round starting...");

                    Timeline serverRoundTimeout = new Timeline(new KeyFrame(
                        Duration.seconds(2),
                        event -> {
                            if ("🎯 Round starting...".equals(gameStatusLabel.getText())) {
                                updateGameStatus("✅ Round started");
                            }
                        }
                    ));
                    serverRoundTimeout.play();

                } catch (NumberFormatException ex) {
                    showInlineError(gameStatusLabel, "Invalid game value");
                }
            } else {
                showInlineError(gameStatusLabel, "Not connected to server or no server running");
            }
        });

        gameControls.getChildren().addAll(modeLabel, gameModeCombo, valueLabel, gameValueField, startGameButton);
        section.getChildren().addAll(sectionTitle, gameControls, gameStatusLabel);

        return section;
    }

    /**
     * Creates the player status section of the networking interface.
     * 
     * <p>This section displays real-time information about connected players,
     * current game phase, and session status. It provides visual feedback
     * about the multiplayer session state and participant information.</p>
     * 
     * @return a VBox containing player status displays and game phase information
     */
    private VBox createPlayerStatusSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("menu-group");

        Label sectionTitle = new Label("Player Status");
        sectionTitle.getStyleClass().add("section-title");

        gamePhaseLabel = new Label(currentPhase.getDisplayText());
        gamePhaseLabel.getStyleClass().add("stat-title");

        playerCountLabel = new Label("Players Connected: 0/4");
        playerCountLabel.getStyleClass().add("stat-title");

        playerList = new VBox(5);
        playerList.getStyleClass().add("player-list");

        Label noPlayersLabel = new Label("No players connected");
        noPlayersLabel.getStyleClass().add("stat-value");
        playerList.getChildren().add(noPlayersLabel);

        section.getChildren().addAll(sectionTitle, gamePhaseLabel, playerCountLabel, playerList);

        return section;
    }

    /**
     * Updates the current game phase and refreshes the UI display.
     * 
     * <p>This method changes the game phase state and ensures the UI reflects
     * the new state through thread-safe Platform.runLater execution.</p>
     * 
     * @param newPhase the new game phase to set
     */
    private void updateGamePhase(GamePhase newPhase) {
        currentPhase = newPhase;
        if (gamePhaseLabel != null) {
            Platform.runLater(() -> {
                gamePhaseLabel.setText(newPhase.getDisplayText());
            });
        }
    }

    /**
     * Updates the player status display with current connection information.
     * 
     * <p>This method refreshes the player count and individual player list
     * to reflect current multiplayer session participants. Updates are
     * performed on the JavaFX Application Thread for UI safety.</p>
     * 
     * @param connectedPlayers the number of currently connected players
     * @param maxPlayers the maximum number of players allowed
     * @param playerNames the list of connected player names/IDs
     */
    private void updatePlayerStatus(int connectedPlayers, int maxPlayers, List<String> playerNames) {
        Platform.runLater(() -> {
            if (playerCountLabel != null) {
                playerCountLabel.setText(String.format("Players Connected: %d/%d", connectedPlayers, maxPlayers));
            }

            if (playerList != null) {
                playerList.getChildren().clear();

                if (playerNames.isEmpty()) {
                    Label noPlayersLabel = new Label("No players connected");
                    noPlayersLabel.getStyleClass().add("stat-value");
                    playerList.getChildren().add(noPlayersLabel);
                } else {
                    for (String playerName : playerNames) {
                        Label playerLabel = new Label("👤 " + playerName);
                        playerLabel.getStyleClass().add("stat-value");
                        playerList.getChildren().add(playerLabel);
                    }
                }
            }
        });
    }

    /**
     * Displays an inline error message in the specified status label.
     * 
     * <p>This method provides user feedback for errors by updating the
     * status label with an error message and appropriate styling.</p>
     * 
     * @param statusLabel the label to display the error message in
     * @param message the error message to display
     */
    private void showInlineError(Label statusLabel, String message) {
        Platform.runLater(() -> {
            statusLabel.setText("❌ " + message);
            statusLabel.getStyleClass().removeAll("stat-value");
            statusLabel.getStyleClass().add("error-message");
        });
    }

    /**
     * Sets up callback handlers for client network events.
     * 
     * <p>This method configures the client with appropriate callbacks for
     * round start notifications, results handling, and connection status
     * changes. All callbacks include proper thread safety for UI updates.</p>
     */
    private void setupClientCallbacks() {
        if (client == null) return;

        client.setRoundStartCallback((roundId, mode, value, words) -> {
            Platform.runLater(() -> {
                updateGamePhase(GamePhase.ROUND_IN_PROGRESS);
                updateGameStatus("🎯 Round in progress");

                if (onTestSessionCreated != null) {
                    TestSession session = client.createTestSession();
                    onTestSessionCreated.accept(session);
                }
            });
        });

        client.setRoundResultsCallback((roundId, results) -> {
            Platform.runLater(() -> {
                updateGamePhase(GamePhase.RESULTS_READY);
                updateGameStatus("✅ Round completed");

                if (onRoundResultsReceived != null) {
                    onRoundResultsReceived.accept(results);
                } else {
                    showRoundResults(results);
                }
            });
        });

        client.setConnectionStatusCallback((connected) -> {
            Platform.runLater(() -> {
                if (connected) {
                    updateClientStatus("🟢 Connected to " + client.getHost() + ":" + client.getPort());
                } else {
                    updateClientStatus("🔴 Disconnected");
                    updateGameStatus("Connection lost");
                    updateGamePhase(GamePhase.WAITING_FOR_PLAYERS);
                }
            });
        });
    }

    /**
     * Displays round results in a modal dialog.
     * 
     * @deprecated This method is deprecated in favor of the callback-based
     *             result handling through {@link #setOnRoundResultsReceived(Consumer)}.
     *             Use the callback mechanism for better integration with the main UI.
     * 
     * @param results the list of round results to display
     */
    @Deprecated
    private void showRoundResults(List<RoundResult> results) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Round Results");
        alert.setHeaderText("Typing Round Complete");

        StringBuilder content = new StringBuilder("Results:\n\n");
        for (int i = 0; i < results.size(); i++) {
            RoundResult result = results.get(i);
            content.append(String.format("%d. %s - %d WPM, %d%% accuracy\n",
                i + 1, result.getPlayerId(), result.getWpm(), result.getAccuracy()));
        }

        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    /**
     * Displays an error message in a modal dialog.
     * 
     * @deprecated This method is deprecated in favor of inline error display
     *             using {@link #showInlineError(Label, String)} for better
     *             user experience and non-intrusive feedback.
     * 
     * @param message the error message to display
     */
    @Deprecated
    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Sends completed round statistics to the connected server.
     * 
     * <p>This method converts test session results into network-transmittable
     * format and sends them to the server for comparison with other players.
     * It handles network errors gracefully and provides user feedback.</p>
     * 
     * @param testResult the completed typing test results
     * @param completionTime the total time taken to complete the test
     * @param gameMode the game mode that was used for the test
     * @param gameValue the target value for the game mode
     */
    public void sendRoundStats(TestSession.TestResult testResult, double completionTime,
                              GameMode gameMode, int gameValue) {
        if (client != null && client.isConnected()) {
            try {
                RoundResult result = new RoundResult(
                    client.getPlayerId(), testResult, completionTime, true,
                    gameMode, gameValue
                );
                client.sendRoundStats(result);
            } catch (IOException e) {
                showAlert("Failed to send round stats: " + e.getMessage());
            }
        }
    }

    /**
     * Updates the client connection status display.
     * 
     * @param status the status message to display
     */
    private void updateClientStatus(String status) {
        if (clientStatusLabel != null) {
            clientStatusLabel.setText(status);
        }
    }

    /**
     * Updates the server status display.
     * 
     * @param status the status message to display
     */
    private void updateServerStatus(String status) {
        if (serverStatusLabel != null) {
            serverStatusLabel.setText(status);
        }
    }

    /**
     * Updates the game status display.
     * 
     * @param status the status message to display
     */
    private void updateGameStatus(String status) {
        if (gameStatusLabel != null) {
            gameStatusLabel.setText(status);
        }
    }

    /**
     * Performs cleanup of networking resources when the UI is closed.
     * 
     * <p>This method should be called when the networking UI is being disposed
     * to ensure proper cleanup of network connections and prevent resource leaks.</p>
     */
    public void cleanup() {
        if (client != null) {
            client.disconnect();
        }
        if (server != null) {
            server.stop();
        }
    }

    /**
     * Sets the callback for test session creation events.
     * 
     * <p>This callback is invoked when a multiplayer round begins and a new
     * test session needs to be created for the local player.</p>
     * 
     * @param callback the callback to invoke when test sessions are created,
     *                or null to remove the current callback
     */
    public void setOnTestSessionCreated(Consumer<TestSession> callback) {
        this.onTestSessionCreated = callback;
    }

    /**
     * Sets the callback for round results reception events.
     * 
     * <p>This callback is invoked when round results are received from the
     * server, typically containing performance data from all participants.</p>
     * 
     * @param callback the callback to invoke when results are received,
     *                or null to remove the current callback
     */
    public void setOnRoundResultsReceived(Consumer<List<RoundResult>> callback) {
        this.onRoundResultsReceived = callback;
    }

    /**
     * Checks if the current instance is connected to a server as a client.
     * 
     * @return true if connected as a client, false otherwise
     */
    public boolean isConnectedAsClient() {
        return client != null && client.isConnected();
    }

    /**
     * Checks if the current instance is running as a server.
     * 
     * @return true if running as a server, false otherwise
     */
    public boolean isRunningAsServer() {
        return server != null && server.isRunning();
    }
}