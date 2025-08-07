package com.typerr.ui;

import com.typerr.TestSession;
import com.typerr.charts.ChartsController;
import com.typerr.network.NetworkingUIIntegration;
import com.typerr.network.RoundResult;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Consumer;

public class UIController {

    private final StackPane rootPane;

    private Node currentView;

    private boolean isStatsVisible = false;

    private Runnable themeToggleAction;

    private java.util.function.Supplier<String> getCurrentTheme;

    private NetworkingUIIntegration networkingUI;

    private Runnable multiplayerBackAction;

    public UIController(StackPane rootPane) {
        this.rootPane = rootPane;
        this.currentView = rootPane.getChildren().isEmpty() ? null : rootPane.getChildren().get(0);
    }

    public void switchView(Node newView) {
        switchView(newView, false);
    }

    public void switchView(Node newView, boolean forceClear) {
        if (currentView == null) {
            rootPane.getChildren().clear();
            rootPane.getChildren().add(newView);
            currentView = newView;

            newView.setOpacity(0);
            FadeTransition ftIn = new FadeTransition(Duration.millis(150), newView);
            ftIn.setToValue(1);
            ftIn.setOnFinished(e -> {
                javafx.application.Platform.runLater(() -> {
                    newView.requestFocus();
                    if (newView instanceof javafx.scene.Parent) {
                        ((javafx.scene.Parent) newView).getChildrenUnmodifiable()
                                .stream()
                                .filter(node -> node instanceof javafx.scene.control.Control)
                                .findFirst()
                                .ifPresent(javafx.scene.Node::requestFocus);
                    }
                });
            });
            ftIn.play();
            return;
        }

        FadeTransition ftOut = new FadeTransition(Duration.millis(150), currentView);
        ftOut.setToValue(0);
        ftOut.setOnFinished(e -> {
            rootPane.getChildren().clear();
            rootPane.getChildren().add(newView);
            newView.setOpacity(0);
            currentView = newView;

            FadeTransition ftIn = new FadeTransition(Duration.millis(150), newView);
            ftIn.setToValue(1);
            ftIn.setOnFinished(fadeInEvent -> {
                javafx.application.Platform.runLater(() -> {
                    newView.requestFocus();
                    if (newView instanceof javafx.scene.Parent) {
                        ((javafx.scene.Parent) newView).getChildrenUnmodifiable()
                                .stream()
                                .filter(node -> node instanceof javafx.scene.control.Control)
                                .findFirst()
                                .ifPresent(javafx.scene.Node::requestFocus);
                    }
                });
            });
            ftIn.play();
        });
        ftOut.play();
    }

    public Node createStatisticsView(Runnable backAction) {
        VBox container = new VBox(20);
        container.setPadding(new Insets(20));
        container.getStyleClass().add("stats-container");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(20);

        Button backButton = new Button("←");
        backButton.getStyleClass().addAll("nav-button", "back-button");
        backButton.setOnAction(e -> {
            isStatsVisible = false;
            backAction.run();
        });

        Label title = new Label("Statistics & Analytics");
        title.getStyleClass().add("stats-title");

        header.getChildren().addAll(backButton, title);

        TabPane chartsPane = ChartsController.createStatisticsView();
        chartsPane.setPrefHeight(600);

        container.getChildren().addAll(header, chartsPane);

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.getStyleClass().add("stats-scroll-pane");

        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            double speedFactor = 4.0;
            double deltaY = event.getDeltaY() * speedFactor;

            double currentVValue = scrollPane.getVvalue();
            double contentHeight = container.getBoundsInLocal().getHeight();
            double viewportHeight = scrollPane.getViewportBounds().getHeight();

            if (contentHeight > viewportHeight) {
                double scrollableHeight = contentHeight - viewportHeight;
                double scrollIncrement = deltaY / scrollableHeight;

                double newVValue = Math.max(0.0, Math.min(1.0, currentVValue - scrollIncrement));
                scrollPane.setVvalue(newVValue);
            }

            event.consume();
        });

        return scrollPane;
    }

    public Node createSettingsView(Runnable backAction) {
        return createSettingsView(backAction, null, null);
    }

    public Node createSettingsView(Runnable backAction, Runnable themeToggleAction, java.util.function.Supplier<String> getCurrentTheme) {
        this.themeToggleAction = themeToggleAction;
        this.getCurrentTheme = getCurrentTheme;
        VBox container = new VBox(20);
        container.setPadding(new Insets(20));
        container.getStyleClass().add("settings-container");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(20);

        Button backButton = new Button("←");
        backButton.getStyleClass().addAll("nav-button", "back-button");
        backButton.setOnAction(e -> backAction.run());

        Label title = new Label("Settings");
        title.getStyleClass().add("settings-title");

        header.getChildren().addAll(backButton, title);

        VBox settingsContent = new VBox(15);
        settingsContent.getStyleClass().add("settings-content");

        VBox themeSection = createSettingsSection("Appearance",
                "Theme and visual customization options");

        VBox typingSection = createSettingsSection("Typing",
                "Test preferences and difficulty settings");

        VBox shortcutsSection = createShortcutsSection();

        settingsContent.getChildren().addAll(themeSection, typingSection, shortcutsSection);

        container.getChildren().addAll(header, settingsContent);

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("settings-scroll-pane");

        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            double speedFactor = 4.0;
            double deltaY = event.getDeltaY() * speedFactor;

            double currentVValue = scrollPane.getVvalue();
            double contentHeight = container.getBoundsInLocal().getHeight();
            double viewportHeight = scrollPane.getViewportBounds().getHeight();

            if (contentHeight > viewportHeight) {
                double scrollableHeight = contentHeight - viewportHeight;
                double scrollIncrement = deltaY / scrollableHeight;

                double newVValue = Math.max(0.0, Math.min(1.0, currentVValue - scrollIncrement));
                scrollPane.setVvalue(newVValue);
            }

            event.consume();
        });

        return scrollPane;
    }

    private VBox createSettingsSection(String title, String description) {
        VBox section = new VBox(10);
        section.getStyleClass().add("settings-section");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("section-title");

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("section-description");

        section.getChildren().addAll(titleLabel, descLabel);

        if ("Appearance".equals(title)) {
            VBox appearanceControls = createAppearanceControls();
            section.getChildren().add(appearanceControls);
        } else if ("Typing".equals(title)) {
            VBox typingControls = createTypingControls();
            section.getChildren().add(typingControls);
        }

        return section;
    }

    private VBox createAppearanceControls() {
        VBox controls = new VBox(10);
        controls.getStyleClass().add("settings-controls");

        HBox themeBox = new HBox(10);
        themeBox.setAlignment(Pos.CENTER_LEFT);

        Label themeLabel = new Label("Theme:");
        themeLabel.getStyleClass().add("setting-label");

        Button themeToggle = new Button();
        updateThemeButtonText(themeToggle);
        themeToggle.getStyleClass().add("theme-toggle");
        themeToggle.setOnAction(e -> {
            toggleTheme();
            updateThemeButtonText(themeToggle);
        });

        themeBox.getChildren().addAll(themeLabel, themeToggle);
        controls.getChildren().add(themeBox);

        return controls;
    }

    private VBox createTypingControls() {
        VBox controls = new VBox(10);
        controls.getStyleClass().add("settings-controls");

        return controls;
    }

    private void updateThemeButtonText(Button themeButton) {
        if (getCurrentTheme != null) {
            String currentTheme = getCurrentTheme.get();
            boolean isDark = "DARK".equalsIgnoreCase(currentTheme);
            themeButton.setText(isDark ? "☀️ Light Mode" : "🌙 Dark Mode");
            themeButton.getStyleClass().removeAll("light-theme-button", "dark-theme-button");
            themeButton.getStyleClass().add(isDark ? "dark-theme-button" : "light-theme-button");
        } else {
            themeButton.setText("🌙 Toggle Theme");
        }
    }

    private void toggleTheme() {
        if (themeToggleAction != null) {
            themeToggleAction.run();
        }
    }

    private VBox createShortcutsSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("settings-section");

        Label title = new Label("Keyboard Shortcuts");
        title.getStyleClass().add("section-title");

        Label description = new Label("Available keyboard shortcuts for quick navigation");
        description.getStyleClass().add("section-description");

        VBox shortcutsList = new VBox(5);
        shortcutsList.getStyleClass().add("shortcuts-list");

        String[] shortcuts = {
                "Ctrl+R - Restart current test",
                "Ctrl+M - Switch between test modes",
                "Ctrl+S - Toggle statistics view",
                "Ctrl+P - Toggle multiplayer mode",
                "Ctrl+, - Open settings",
                "F11 - Toggle fullscreen mode",
                "Esc - Return to main menu",
                "Tab - Restart test",
                "Enter/Esc - Navigate through results"
        };

        for (String shortcut : shortcuts) {
            Label shortcutLabel = new Label(shortcut);
            shortcutLabel.getStyleClass().add("shortcut-item");
            shortcutsList.getChildren().add(shortcutLabel);
        }

        section.getChildren().addAll(title, description, shortcutsList);
        return section;
    }

    public Node createHelpView(Runnable backAction) {
        VBox container = new VBox(20);
        container.setPadding(new Insets(20));
        container.getStyleClass().add("help-container");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(20);

        Button backButton = new Button("←");
        backButton.getStyleClass().addAll("nav-button", "back-button");
        backButton.setOnAction(e -> backAction.run());

        Label title = new Label("Help & About");
        title.getStyleClass().add("help-title");

        header.getChildren().addAll(backButton, title);

        VBox helpContent = new VBox(15);
        helpContent.getStyleClass().add("help-content");

        Label appInfo = new Label("Typerr - Advanced Typing Test Application");
        appInfo.getStyleClass().add("app-info");

        Label description = new Label("""
                A comprehensive typing test application designed to help you improve
                your typing speed and accuracy. Features include multiple test modes,
                difficulty levels, detailed analytics, achievement tracking, and more.
                """);
        description.getStyleClass().add("app-description");
        description.setWrapText(true);

        helpContent.getChildren().addAll(appInfo, description);

        container.getChildren().addAll(header, helpContent);

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("help-scroll-pane");

        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            double speedFactor = 3.0;
            double deltaY = event.getDeltaY() * speedFactor;

            scrollPane.setVvalue(scrollPane.getVvalue() - (deltaY / container.getHeight() * 20));
            event.consume();
        });

        return scrollPane;
    }

    public Node createMultiplayerView(Runnable backAction, NetworkingUIIntegration networkingUI, Consumer<TestSession> onGameStart) {
        this.networkingUI = networkingUI;
        this.multiplayerBackAction = backAction;

        VBox container = new VBox(20);
        container.setPadding(new Insets(20));
        container.getStyleClass().add("multiplayer-container");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(20);

        Button backButton = new Button("←");
        backButton.getStyleClass().addAll("nav-button", "back-button");
        backButton.setOnAction(e -> backAction.run());

        Label title = new Label("Multiplayer Mode");
        title.getStyleClass().add("multiplayer-title");

        header.getChildren().addAll(backButton, title);

        networkingUI.setOnTestSessionCreated(session -> {
            onGameStart.accept(session);
        });

        networkingUI.setOnRoundResultsReceived(results -> {
            showMultiplayerResults(results);
        });

        VBox networkingPanel = networkingUI.createNetworkingPanel();

        container.getChildren().addAll(header, networkingPanel);

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.getStyleClass().add("multiplayer-scroll-pane");

        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            double speedFactor = 3.0;
            double deltaY = event.getDeltaY() * speedFactor;
            scrollPane.setVvalue(scrollPane.getVvalue() - (deltaY / container.getHeight() * 20));
            event.consume();
        });

        return scrollPane;
    }

    private void showMultiplayerResults(List<RoundResult> results) {
        javafx.application.Platform.runLater(() -> {
            Node resultsView = createMultiplayerResultsView(results, () -> {
                if (multiplayerBackAction != null) {
                    multiplayerBackAction.run();
                } else {
                    System.err.println("Warning: No back action available for multiplayer navigation");
                }
            });
            switchView(resultsView);
        });
    }

    private Node createMultiplayerResultsView(List<RoundResult> results, Runnable backAction) {
        VBox container = new VBox(25);
        container.setPadding(new Insets(40));
        container.setAlignment(Pos.CENTER);
        container.getStyleClass().add("results-pane");

        Label titleLabel = new Label("🏆 Multiplayer Round Complete");
        titleLabel.getStyleClass().add("menu-title");

        String testInfoText = "multiplayer round";
        if (!results.isEmpty()) {
            RoundResult firstResult = results.get(0);
            testInfoText = String.format("test: %s / %d / multiplayer",
                    firstResult.getGameMode().name().toLowerCase(),
                    firstResult.getGameValue());
        }
        Label testInfoLabel = new Label(testInfoText);
        testInfoLabel.getStyleClass().add("results-info");

        VBox rankingsSection = new VBox(15);
        rankingsSection.setAlignment(Pos.CENTER);

        Label rankingsTitle = new Label("Final Rankings");
        rankingsTitle.getStyleClass().add("section-title");

        VBox playerResults = new VBox(10);
        playerResults.setAlignment(Pos.CENTER);

        for (int i = 0; i < results.size(); i++) {
            RoundResult result = results.get(i);
            HBox playerRow = createPlayerResultRow(result, i + 1);
            playerResults.getChildren().add(playerRow);
        }

        rankingsSection.getChildren().addAll(rankingsTitle, playerResults);

        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(Pos.CENTER);

        Button backButton = new Button("↩ Back to Lobby");
        backButton.getStyleClass().addAll("nav-button", "back-button");
        backButton.setOnAction(e -> backAction.run());

        Button playAgainButton = new Button("🎮 Play Again");
        playAgainButton.getStyleClass().addAll("mode-button");
        playAgainButton.setOnAction(e -> {
            if (networkingUI != null && multiplayerBackAction != null) {
                Node multiplayerView = createMultiplayerView(multiplayerBackAction, networkingUI, session -> {});
                switchView(multiplayerView);
            } else {
                backAction.run();
            }
        });

        buttonRow.getChildren().addAll(backButton, playAgainButton);

        container.getChildren().addAll(
                titleLabel,
                testInfoLabel,
                rankingsSection,
                buttonRow
        );

        return container;
    }

    private HBox createPlayerResultRow(RoundResult result, int position) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("player-result-row");

        String medal = switch (position) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> "  ";
        };

        Label positionLabel = new Label(medal + " " + position + ".");
        positionLabel.getStyleClass().add("results-label-title");
        positionLabel.setPrefWidth(80);
        positionLabel.setMinWidth(80);

        Label nameLabel = new Label(result.getPlayerId());
        nameLabel.getStyleClass().add("stat-value");
        nameLabel.setPrefWidth(180);
        nameLabel.setMinWidth(180);

        Label wpmLabel = new Label(String.format("WPM: %d", result.getWpm()));
        wpmLabel.getStyleClass().add("multiplayer-wpm-value");
        wpmLabel.setPrefWidth(140);
        wpmLabel.setMinWidth(140);

        Label accLabel = new Label(String.format("Accuracy: %d%%", result.getAccuracy()));
        accLabel.getStyleClass().addAll("results-info");
        accLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        accLabel.setPrefWidth(120);
        accLabel.setMinWidth(120);

        row.getChildren().addAll(positionLabel, nameLabel, wpmLabel, accLabel);

        return row;
    }

    public void setStatsVisible(boolean visible) {
        this.isStatsVisible = visible;
    }
}
