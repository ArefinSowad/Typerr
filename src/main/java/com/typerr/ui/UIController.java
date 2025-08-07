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

/**
 * Enhanced UI controller for managing complex view transitions and interactions in the Typerr application.
 * 
 * <p>The UIController provides advanced user interface management capabilities
 * including smooth view transitions, statistics visualization, settings management,
 * and multiplayer integration. It serves as the central coordinator for the application's
 * sophisticated user interface elements.</p>
 * 
 * <h3>Core Functionality:</h3>
 * <ul>
 *   <li><strong>View Management:</strong> Smooth transitions between application views</li>
 *   <li><strong>Animation System:</strong> Fade transitions for professional UI feel</li>
 *   <li><strong>Statistics Integration:</strong> Charts and performance analytics display</li>
 *   <li><strong>Settings Interface:</strong> Comprehensive application configuration</li>
 *   <li><strong>Multiplayer Support:</strong> Integration with networking components</li>
 * </ul>
 * 
 * <h3>Animation Features:</h3>
 * <p>The controller implements smooth fade transitions for all view changes:</p>
 * <ul>
 *   <li>150ms fade-out duration for departing views</li>
 *   <li>150ms fade-in duration for incoming views</li>
 *   <li>Automatic focus management for accessibility</li>
 *   <li>Non-blocking transitions using JavaFX timeline</li>
 * </ul>
 * 
 * <h3>Supported Views:</h3>
 * <ul>
 *   <li><strong>Statistics View:</strong> Performance charts and analytics</li>
 *   <li><strong>Settings View:</strong> Application preferences and configuration</li>
 *   <li><strong>Multiplayer View:</strong> Network game setup and management</li>
 * </ul>
 * 
 * <h3>Accessibility Features:</h3>
 * <ul>
 *   <li>Keyboard navigation support with proper focus management</li>
 *   <li>Screen reader compatibility through semantic structure</li>
 *   <li>High contrast support via CSS styling</li>
 *   <li>Keyboard shortcuts integration</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Create controller with root pane
 * StackPane root = new StackPane();
 * UIController controller = new UIController(root);
 * 
 * // Switch to statistics view with smooth transition
 * Node statsView = controller.createStatisticsView(() -> {
 *     // Handle back button action
 *     controller.switchView(mainMenuView);
 * });
 * controller.switchView(statsView);
 * 
 * // Create settings view with theme toggle
 * Node settingsView = controller.createSettingsView(
 *     () -> controller.switchView(mainMenuView),
 *     () -> themeManager.toggleTheme(),
 *     () -> themeManager.getCurrentTheme().name()
 * );
 * }</pre>
 * 
 * @author ArefinSowad
 * @version 1.0
 * @since 1.0
 * @see ChartsController
 * @see NetworkingUIIntegration
 * @see KeyboardShortcutsManager
 */
public class UIController {

    /** The root container pane that holds all application views. */
    private final StackPane rootPane;
    
    /** Reference to the currently displayed view node. */
    private Node currentView;
    
    /** Flag indicating whether the statistics view is currently visible. */
    private boolean isStatsVisible = false;

    /** Action callback for theme toggle functionality. */
    private Runnable themeToggleAction;
    
    /** Supplier function for getting the current theme name. */
    private java.util.function.Supplier<String> getCurrentTheme;

    /** Networking UI integration component for multiplayer features. */
    private NetworkingUIIntegration networkingUI;
    
    /** Action callback for handling back navigation from multiplayer view. */
    private Runnable multiplayerBackAction;

    /**
     * Constructs a new UIController with the specified root pane.
     * 
     * <p>Initializes the controller and sets the current view to the first child
     * of the root pane if one exists, or null if the pane is empty.</p>
     * 
     * @param rootPane the StackPane that will serve as the root container for all views
     * @throws IllegalArgumentException if rootPane is null
     */
    public UIController(StackPane rootPane) {
        this.rootPane = rootPane;
        this.currentView = rootPane.getChildren().isEmpty() ? null : rootPane.getChildren().get(0);
    }

    /**
     * Switches to a new view with default transition settings.
     * 
     * <p>This is a convenience method that calls {@link #switchView(Node, boolean)}
     * with forceClear set to false.</p>
     * 
     * @param newView the JavaFX Node to display as the new view
     */
    public void switchView(Node newView) {
        switchView(newView, false);
    }

    /**
     * Switches to a new view with smooth fade transition animation.
     * 
     * <p>This method performs an animated transition from the current view to the
     * specified new view. The transition includes fade-out of the current view
     * followed by fade-in of the new view, with automatic focus management.</p>
     * 
     * <p>Transition process:</p>
     * <ol>
     *   <li>If no current view exists, immediately display new view with fade-in</li>
     *   <li>Otherwise, fade out current view over 150ms</li>
     *   <li>Replace current view with new view in the scene graph</li>
     *   <li>Fade in new view over 150ms</li>
     *   <li>Set focus to the new view for keyboard accessibility</li>
     * </ol>
     * 
     * @param newView the JavaFX Node to display as the new view
     * @param forceClear if true, immediately clears the root pane (currently unused)
     */
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

    /**
     * Creates a comprehensive statistics view with charts and performance analytics.
     * 
     * <p>This method constructs a complete statistics interface containing tabbed
     * charts, performance trends, and detailed analytics. The view includes a
     * back button for navigation and is optimized for scrolling through large
     * amounts of statistical data.</p>
     * 
     * <p>View components:</p>
     * <ul>
     *   <li>Header with back navigation button</li>
     *   <li>Tabbed chart interface from {@link ChartsController}</li>
     *   <li>Scrollable container with enhanced scroll behavior</li>
     *   <li>Proper styling and accessibility support</li>
     * </ul>
     * 
     * <p>The view automatically tracks visibility state and provides smooth
     * scrolling with 4x speed enhancement for better user experience.</p>
     * 
     * @param backAction the action to execute when the back button is pressed
     * @return a ScrollPane containing the complete statistics interface
     */
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

    /**
     * Creates a settings view with default functionality.
     * 
     * <p>This is a convenience method that creates a settings view without
     * theme toggle functionality.</p>
     * 
     * @param backAction the action to execute when the back button is pressed
     * @return a Node containing the settings interface
     * @see #createSettingsView(Runnable, Runnable, java.util.function.Supplier)
     */
    public Node createSettingsView(Runnable backAction) {
        return createSettingsView(backAction, null, null);
    }

    /**
     * Creates a comprehensive settings view with theme management and preferences.
     * 
     * <p>This method constructs a complete settings interface with multiple
     * configuration sections including appearance settings, typing preferences,
     * and keyboard shortcuts. The view supports theme toggling and provides
     * organized sections for different types of settings.</p>
     * 
     * <p>Settings sections include:</p>
     * <ul>
     *   <li><strong>Appearance:</strong> Theme selection and visual preferences</li>
     *   <li><strong>Typing:</strong> Test difficulty and behavior settings</li>
     *   <li><strong>Shortcuts:</strong> Keyboard shortcut configuration and reference</li>
     * </ul>
     * 
     * <p>The view includes enhanced scrolling behavior and proper styling
     * for consistency with the rest of the application.</p>
     * 
     * @param backAction the action to execute when the back button is pressed
     * @param themeToggleAction the action to execute when theme toggle is activated, or null
     * @param getCurrentTheme supplier function for getting current theme name, or null
     * @return a ScrollPane containing the complete settings interface
     */
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

    /**
     * Creates a settings section with title, description, and optional controls.
     * 
     * <p>This helper method generates a standardized settings section layout
     * with consistent styling and spacing. It creates a VBox container with
     * a title label, description label, and dynamically adds type-specific
     * controls based on the section title.</p>
     * 
     * @param title the section title (e.g., "Appearance", "Typing")
     * @param description brief description of the section's purpose
     * @return a VBox containing the formatted settings section
     */
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

    /**
     * Creates appearance-related control elements for the settings view.
     * 
     * <p>Generates the theme toggle button and other appearance-related
     * controls for the settings interface. The theme button dynamically
     * updates its text and styling based on the current theme state.</p>
     * 
     * @return a VBox containing appearance control elements
     */
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

    /**
     * Creates typing-related control elements for the settings view.
     * 
     * <p>Placeholder method for future typing-specific controls such as
     * difficulty level selection, word set preferences, and typing behavior
     * settings. Currently returns an empty container for future expansion.</p>
     * 
     * @return a VBox container for typing control elements (currently empty)
     */
    private VBox createTypingControls() {
        VBox controls = new VBox(10);
        controls.getStyleClass().add("settings-controls");


        return controls;
    }

    /**
     * Updates the theme toggle button's text and styling based on current theme.
     * 
     * <p>This method dynamically updates the theme button to reflect the current
     * theme state and provide appropriate text for the next action. It uses
     * emoji icons and descriptive text to make the button's function clear
     * to users.</p>
     * 
     * @param themeButton the button to update with current theme information
     */
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

    /**
     * Executes the theme toggle action if one is configured.
     * 
     * <p>Helper method that safely invokes the theme toggle callback
     * if it has been set. This allows the UI controller to trigger
     * theme changes without direct coupling to theme management logic.</p>
     */
    private void toggleTheme() {
        if (themeToggleAction != null) {
            themeToggleAction.run();
        }
    }

    /**
     * Creates the keyboard shortcuts reference section for the settings view.
     * 
     * <p>Generates a comprehensive list of available keyboard shortcuts with
     * descriptions, formatted as a settings section. This provides users with
     * a convenient reference for all application hotkeys and navigation shortcuts.</p>
     * 
     * @return a VBox containing the keyboard shortcuts reference list
     */
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

    /**
     * Handles the display of multiplayer round results.
     * 
     * <p>This method is called when multiplayer round results are received
     * from the networking component. It creates a results view and transitions
     * to it using the Platform.runLater to ensure UI updates occur on the
     * JavaFX Application Thread.</p>
     * 
     * @param results the list of player performance results to display
     */
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

    /**
     * Creates a comprehensive multiplayer results view with rankings and player statistics.
     * 
     * <p>Constructs a complete results interface showing the final rankings from a
     * multiplayer typing test round. The view includes medal indicators for top
     * performers, detailed player statistics, and navigation options for continuing
     * or returning to the main menu.</p>
     * 
     * <p>The results are displayed in ranking order with visual indicators:</p>
     * <ul>
     *   <li>🥇 Gold medal for 1st place</li>
     *   <li>🥈 Silver medal for 2nd place</li>
     *   <li>🥉 Bronze medal for 3rd place</li>
     *   <li>Numbered positions for remaining players</li>
     * </ul>
     * 
     * @param results the list of player performance results, typically pre-sorted by performance
     * @param backAction the action to execute when returning to the previous view
     * @return a Node containing the complete multiplayer results interface
     */
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

    /**
     * Creates a formatted row displaying an individual player's performance results.
     * 
     * <p>Generates a horizontal layout showing a player's ranking, name, and
     * key performance metrics (WPM and accuracy) with appropriate styling
     * and medal indicators for top performers. The row uses fixed-width
     * columns for consistent alignment in the results table.</p>
     * 
     * @param result the player's performance data to display
     * @param position the player's ranking position (1-based)
     * @return an HBox containing the formatted player result row
     */
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
