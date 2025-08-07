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

public class NetworkingUIIntegration {

    private SimpleGameServer server;

    private SimpleGameClient client;

    private boolean isServer = false;

    private Consumer<TestSession> onTestSessionCreated;

    private Consumer<List<RoundResult>> onRoundResultsReceived;

    private Label playerCountLabel;

    private VBox playerList;

    private Label gameStatusLabel;

    private Label gamePhaseLabel;

    private Label serverStatusLabel;

    private Label clientStatusLabel;

    private enum GamePhase {

        WAITING_FOR_PLAYERS("⏳ Waiting for players"),

        ROUND_IN_PROGRESS("🎯 Round in progress"),

        RESULTS_READY("✅ Results ready");

        private final String displayText;

        GamePhase(String displayText) {
            this.displayText = displayText;
        }

        public String getDisplayText() {
            return displayText;
        }
    }

    private GamePhase currentPhase = GamePhase.WAITING_FOR_PLAYERS;

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

    private void updateGamePhase(GamePhase newPhase) {
        currentPhase = newPhase;
        if (gamePhaseLabel != null) {
            Platform.runLater(() -> {
                gamePhaseLabel.setText(newPhase.getDisplayText());
            });
        }
    }

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

    private void showInlineError(Label statusLabel, String message) {
        Platform.runLater(() -> {
            statusLabel.setText("❌ " + message);
            statusLabel.getStyleClass().removeAll("stat-value");
            statusLabel.getStyleClass().add("error-message");
        });
    }

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

    private void updateClientStatus(String status) {
        if (clientStatusLabel != null) {
            clientStatusLabel.setText(status);
        }
    }

    private void updateServerStatus(String status) {
        if (serverStatusLabel != null) {
            serverStatusLabel.setText(status);
        }
    }

    private void updateGameStatus(String status) {
        if (gameStatusLabel != null) {
            gameStatusLabel.setText(status);
        }
    }

    public void cleanup() {
        if (client != null) {
            client.disconnect();
        }
        if (server != null) {
            server.stop();
        }
    }

    public void setOnTestSessionCreated(Consumer<TestSession> callback) {
        this.onTestSessionCreated = callback;
    }

    public void setOnRoundResultsReceived(Consumer<List<RoundResult>> callback) {
        this.onRoundResultsReceived = callback;
    }

    public boolean isConnectedAsClient() {
        return client != null && client.isConnected();
    }

    public boolean isRunningAsServer() {
        return server != null && server.isRunning();
    }
}