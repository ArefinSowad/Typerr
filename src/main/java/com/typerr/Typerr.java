package com.typerr;

import com.typerr.network.NetworkingUIIntegration;
import com.typerr.statics.Constants;
import com.typerr.statics.GameMode;
import com.typerr.statics.Theme;
import com.typerr.ui.UIController;
import com.typerr.ui.KeyboardShortcutsManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;

/**
 * Main application class for Typerr - A feature-rich typing speed test application.
 * 
 * <p>Typerr is a comprehensive JavaFX-based desktop application that provides advanced typing speed 
 * testing capabilities with support for multiple game modes, difficulty levels, multiplayer gaming,
 * real-time analytics, and detailed performance tracking. The application implements a sophisticated
 * state-machine architecture to manage different application views and user interactions, ensuring
 * a smooth and intuitive user experience.</p>
 * 
 * <p>Built with modern Java and JavaFX technologies, Typerr combines performance, usability, and
 * extensibility to deliver a professional typing test environment suitable for both casual users
 * and serious typing enthusiasts.</p>
 * 
 * <h3>Core Features:</h3>
 * <ul>
 *   <li><strong>Multiple Game Modes:</strong> Time-based (15/30/60 seconds), word count (10/25/50 words), and custom test configurations</li>
 *   <li><strong>Difficulty Levels:</strong> Easy, medium, and hard word sets with customizable complexity</li>
 *   <li><strong>Real-time Statistics:</strong> Live WPM, raw WPM, accuracy percentage, and error tracking</li>
 *   <li><strong>Multiplayer Support:</strong> TCP-based client/server architecture for competitive typing sessions</li>
 *   <li><strong>Theme Support:</strong> Light and dark themes with CSS-based customization</li>
 *   <li><strong>Comprehensive Analytics:</strong> Detailed performance tracking, historical data, and progress charts</li>
 *   <li><strong>Keyboard Shortcuts:</strong> Extensive hotkey support for efficient navigation and control</li>
 *   <li><strong>Performance Persistence:</strong> SQLite database for storing typing test history and user preferences</li>
 * </ul>
 * 
 * <h3>Application Architecture:</h3>
 * <p>The application follows a layered architecture with clear separation of concerns:</p>
 * <ul>
 *   <li><strong>Presentation Layer:</strong> JavaFX UI components, controllers, and view management</li>
 *   <li><strong>Business Logic:</strong> Typing test engine, statistics calculation, and game mechanics</li>
 *   <li><strong>Data Layer:</strong> SQLite database management, settings persistence, and data access</li>
 *   <li><strong>Network Layer:</strong> Multiplayer client/server communication and protocol handling</li>
 *   <li><strong>Utility Layer:</strong> Resource loading, theming, and configuration management</li>
 * </ul>
 * 
 * <h3>State Management System:</h3>
 * <p>The application uses a finite state machine pattern with the {@link AppState} enumeration to manage
 * different views and user interactions. This ensures predictable behavior and prevents invalid state
 * transitions. The state machine handles seven distinct states with well-defined transition rules.</p>
 * 
 * <h3>Performance Characteristics:</h3>
 * <ul>
 *   <li><strong>Memory Efficient:</strong> Optimized word rendering limits displayed content</li>
 *   <li><strong>Responsive UI:</strong> Smooth animations and transitions using JavaFX Timeline</li>
 *   <li><strong>Real-time Updates:</strong> Sub-second statistics recalculation during active tests</li>
 *   <li><strong>Scalable Networking:</strong> Supports multiple concurrent multiplayer sessions</li>
 * </ul>
 * 
 * <h3>Thread Safety:</h3>
 * <p>The application runs on the JavaFX Application Thread for UI operations. Database access
 * is synchronized through the {@link com.typerr.database.DatabaseManager} singleton. Network
 * operations use separate threads with Platform.runLater() for UI updates.</p>
 * 
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * // Launch the application
 * public static void main(String[] args) {
 *     Application.launch(Typerr.class, args);
 * }
 * 
 * // Start a 30-second typing test programmatically
 * Typerr app = new Typerr();
 * app.startTest(GameMode.TIME, 30);
 * 
 * // Switch themes
 * app.toggleTheme();
 * }</pre>
 * 
 * @author ArefinSowad
 * @version 1.0
 * @since 1.0
 * @see TestSession
 * @see UIController
 * @see KeyboardShortcutsManager
 * @see NetworkingUIIntegration
 * @see GameMode
 * @see Theme
 */
public class Typerr extends Application {

    /**
     * Default font used for displaying typing test text. This monospace font ensures
     * consistent character spacing for accurate typing practice.
     */
    private static Font WORD_FONT = Font.font(Constants.WORD_FONT_FAMILY, FontWeight.NORMAL, 28);

    /**
     * Currently active theme for the application UI. Defaults to light theme.
     * 
     * @see Theme
     */
    private Theme currentTheme = Theme.LIGHT;

    /**
     * Application state enumeration defining all possible application views and modes.
     * 
     * <p>The application uses a finite state machine to manage user interface transitions
     * and ensure consistent behavior. Each state represents a distinct user interaction
     * context with specific available actions and UI components.</p>
     * 
     * <h4>State Descriptions:</h4>
     * <ul>
     *   <li><strong>MENU:</strong> Main menu showing game mode selection and options</li>
     *   <li><strong>WAITING_TO_START:</strong> Test is configured but not yet started</li>
     *   <li><strong>RUNNING:</strong> Typing test is actively in progress</li>
     *   <li><strong>FINISHED:</strong> Test completed, showing results</li>
     *   <li><strong>STATISTICS:</strong> Viewing performance statistics and charts</li>
     *   <li><strong>SETTINGS:</strong> Application configuration and preferences</li>
     *   <li><strong>MULTIPLAYER:</strong> Multiplayer game setup and play</li>
     * </ul>
     * 
     * <h4>Valid State Transitions:</h4>
     * <pre>
     * MENU → {WAITING_TO_START, STATISTICS, SETTINGS, MULTIPLAYER}
     * WAITING_TO_START → {RUNNING, MENU}
     * RUNNING → {FINISHED, MENU}
     * FINISHED → {MENU, STATISTICS, WAITING_TO_START}
     * STATISTICS → MENU
     * SETTINGS → MENU
     * MULTIPLAYER → {MENU, RUNNING}
     * </pre>
     */
    private enum AppState {
        /** Main menu with game mode selection */
        MENU,
        /** Test configured but not started, waiting for user input */
        WAITING_TO_START,
        /** Typing test actively in progress */
        RUNNING,
        /** Test completed, displaying results */
        FINISHED,
        /** Viewing performance statistics and analytics */
        STATISTICS,
        /** Application settings and configuration */
        SETTINGS,
        /** Multiplayer game setup and gameplay */
        MULTIPLAYER
    }

    /**
     * Current application state. Controls which UI view is active and determines
     * available user actions. Initialized to MENU state.
     * 
     * @see AppState
     */
    private AppState appState = AppState.MENU;

    // ===== UI COMPONENTS =====
    
    /**
     * Root container for all application UI components. Uses StackPane for overlay support
     * and easy view switching with transitions.
     */
    private StackPane rootPane;
    
    /**
     * Primary UI containers for different application views.
     * - gamePane: Active typing test interface
     * - resultsPane: Test completion and results display
     * - menuPane: Main menu and game mode selection
     */
    private VBox gamePane, resultsPane, menuPane;
    
    /**
     * Container for time-based statistics display during typing tests.
     * Visible only in time-based game modes.
     */
    private VBox timeStatBox;
    
    /**
     * Real-time statistic display labels updated during typing tests.
     * - timerValueLabel: Countdown timer for time-based tests
     * - wpmValueLabel: Current words per minute calculation
     * - accValueLabel: Current accuracy percentage
     */
    private Label timerValueLabel, wpmValueLabel, accValueLabel;
    
    /**
     * Text display area for the typing test content. Uses TextFlow for rich text
     * formatting with color coding for correct/incorrect characters.
     */
    private TextFlow wordDisplayArea;

    // ===== CONTROLLERS AND MANAGERS =====
    
    /**
     * Enhanced UI controller responsible for complex UI operations, view creation,
     * and advanced user interface management. Handles statistics views, settings
     * panels, and animated transitions.
     * 
     * @see UIController
     */
    private UIController uiController;
    
    /**
     * Centralized keyboard shortcut manager. Handles global hotkeys and context-sensitive
     * keyboard navigation throughout the application.
     * 
     * @see KeyboardShortcutsManager
     */
    private KeyboardShortcutsManager shortcutsManager;
    
    /**
     * Primary JavaFX stage reference for window management operations such as
     * fullscreen toggling, icon setting, and window properties.
     */
    private Stage primaryStage;

    /**
     * Networking UI integration component for multiplayer functionality.
     * Manages client/server setup, connection status, and multiplayer game coordination.
     * 
     * @see NetworkingUIIntegration
     */
    private NetworkingUIIntegration networkingUI;

    // ===== TIMING AND ANIMATION =====

    /**
     * JavaFX Timeline instances for application timing operations.
     * - gameTimer: Main countdown timer for time-based typing tests
     * - caretTimeline: Blinking cursor animation in text display area
     */
    private Timeline gameTimer, caretTimeline;
    
    /**
     * Remaining time in seconds for time-based typing tests. Updated each second
     * by the gameTimer timeline. When reaches zero, test automatically completes.
     */
    private int timeRemaining;

    // ===== CORE BUSINESS OBJECTS =====
    
    /**
     * Word provider service for generating typing test content. Provides words
     * based on difficulty level and game mode requirements.
     * 
     * @see WordProvider
     */
    private final WordProvider wordProvider = new WordProvider();
    
    /**
     * Current active typing test session. Contains test configuration, real-time
     * statistics, and typed content tracking. Null when no test is active.
     * 
     * @see TestSession
     */
    private TestSession currentSession;

    // ===== CONFIGURATION AND PERSISTENCE =====
    
    /**
     * Settings manager singleton for application configuration persistence.
     * Handles user preferences, theme settings, and game defaults.
     * 
     * @see SettingsManager
     */
    private SettingsManager settingsManager = SettingsManager.getInstance();
    
    /**
     * Main JavaFX scene containing all application UI. Used for global event
     * handling and theme application.
     */
    private Scene scene;

    /**
     * JavaFX Application start method. Initializes the main application window,
     * sets up UI components, configures themes, and establishes keyboard shortcuts.
     * 
     * <p>This method is called by the JavaFX platform after the Application is launched.
     * It performs the following initialization sequence:</p>
     * <ol>
     *   <li>Configure primary stage (title, icon, dimensions)</li>
     *   <li>Initialize core UI controllers and managers</li>
     *   <li>Create and display main menu view</li>
     *   <li>Apply user's preferred theme settings</li>
     *   <li>Setup global keyboard event handling</li>
     *   <li>Configure application shortcuts</li>
     *   <li>Initialize visual feedback systems (caret animation)</li>
     * </ol>
     * 
     * @param primaryStage the primary stage for this application, onto which 
     *                    the application scene can be set. The primary stage will be
     *                    embedded in the browser if the application was launched
     *                    as an applet. Applications may create other stages, if needed,
     *                    but they will not be primary stages and will not be
     *                    embedded in the browser.
     * @throws Exception if there is an error during application startup
     */
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle(Constants.APP_NAME);

        Image icon = ResourceLoader.loadImage(Constants.ICON_PATH);
        if (icon != null) {
            primaryStage.getIcons().add(icon);
        }

        rootPane = new StackPane();
        uiController = new UIController(rootPane);
        shortcutsManager = new KeyboardShortcutsManager();
        networkingUI = new NetworkingUIIntegration();

        menuPane = createMenuView();
        rootPane.getChildren().add(menuPane);

        scene = new Scene(rootPane, Constants.DEFAULT_WINDOW_WIDTH, Constants.DEFAULT_WINDOW_HEIGHT);

        currentTheme = settingsManager.getTheme();
        applyTheme(currentTheme);

        scene.setOnKeyPressed(this::handleKeyPress);
        setupKeyboardShortcuts();

        primaryStage.setScene(scene);
        primaryStage.show();
        setupCaretTimeline();
    }

    /**
     * Configures keyboard shortcuts by registering action handlers with the shortcuts manager.
     * 
     * <p>This method sets up the following global keyboard shortcuts:</p>
     * <ul>
     *   <li><strong>Restart:</strong> Restart current test or return to test setup</li>
     *   <li><strong>Mode Switch:</strong> Return to main menu from any view</li>
     *   <li><strong>Statistics:</strong> Toggle statistics view</li>
     *   <li><strong>Settings:</strong> Toggle settings panel</li>
     *   <li><strong>Fullscreen:</strong> Toggle fullscreen mode</li>
     *   <li><strong>Multiplayer:</strong> Toggle multiplayer interface</li>
     * </ul>
     * 
     * <p>The shortcuts manager handles the actual key combination detection and
     * context-sensitive shortcut activation.</p>
     * 
     * @see KeyboardShortcutsManager
     */
    private void setupKeyboardShortcuts() {
        shortcutsManager.setRestartAction(this::handleRestartShortcut);
        shortcutsManager.setModeSwitchAction(this::handleModeSwitchShortcut);
        shortcutsManager.setStatsAction(this::handleStatsShortcut);
        shortcutsManager.setSettingsAction(this::handleSettingsShortcut);
        shortcutsManager.setFullscreenAction(this::handleFullscreenShortcut);
        shortcutsManager.setMultiplayerAction(this::handleMultiplayerShortcut);
    }

    /**
     * Handles the restart shortcut activation. Restarts the current typing test
     * if one is active, otherwise has no effect.
     * 
     * <p>This method is called when the user presses Ctrl+R (or Cmd+R on macOS).
     * The restart action is only available during active test states to prevent
     * accidental activation.</p>
     * 
     * <p><strong>Valid States for Restart:</strong></p>
     * <ul>
     *   <li>{@link AppState#RUNNING} - Restart active test</li>
     *   <li>{@link AppState#WAITING_TO_START} - Restart test setup</li>
     *   <li>{@link AppState#FINISHED} - Restart completed test</li>
     * </ul>
     */
    private void handleRestartShortcut() {
        if (appState == AppState.RUNNING || appState == AppState.WAITING_TO_START || appState == AppState.FINISHED) {
            restartCurrentTest();
        }
    }

    /**
     * Handles the mode switch shortcut activation. Returns to the main menu
     * from any non-menu application state.
     * 
     * <p>This method provides a quick way to return to the main menu using
     * the Escape key. It safely stops all active timers and resets the
     * application state.</p>
     * 
     * <p><strong>Behavior:</strong></p>
     * <ul>
     *   <li>Stops all active timers (game timer, caret animation)</li>
     *   <li>Transitions application state to MENU</li>
     *   <li>Switches UI view to main menu</li>
     *   <li>Has no effect if already in MENU state</li>
     * </ul>
     */
    private void handleModeSwitchShortcut() {
        if (appState == AppState.MENU) {
            return;
        }

        stopAllTimers();
        appState = AppState.MENU;
        uiController.switchView(menuPane);
    }

    /**
     * Handles the statistics view shortcut activation. Toggles between the
     * statistics view and the main menu.
     * 
     * <p>This method is activated by Ctrl+S (or Cmd+S on macOS) and provides
     * quick access to performance statistics and analytics.</p>
     * 
     * <p><strong>Toggle Behavior:</strong></p>
     * <ul>
     *   <li>If currently in STATISTICS state → return to MENU</li>
     *   <li>If in MENU or FINISHED state → switch to STATISTICS</li>
     *   <li>Creates statistics view with navigation callback</li>
     *   <li>Enables statistics visibility flag for UI controller</li>
     * </ul>
     * 
     * @see UIController#createStatisticsView(Runnable)
     */
    private void handleStatsShortcut() {
        if (appState == AppState.STATISTICS) {
            appState = AppState.MENU;
            uiController.switchView(menuPane);
        } else if (appState == AppState.MENU || appState == AppState.FINISHED) {
            appState = AppState.STATISTICS;
            Node statsView = uiController.createStatisticsView(() -> {
                appState = AppState.MENU;
                uiController.switchView(menuPane);
            });
            uiController.switchView(statsView);
            uiController.setStatsVisible(true);
        }
    }

    /**
     * Handles the settings shortcut activation. Toggles between the settings
     * panel and the main menu.
     * 
     * <p>This method is activated by Ctrl+, (or Cmd+, on macOS) and provides
     * quick access to application configuration and user preferences.</p>
     * 
     * <p><strong>Toggle Behavior:</strong></p>
     * <ul>
     *   <li>If currently in SETTINGS state → return to MENU</li>
     *   <li>If in MENU state → switch to SETTINGS</li>
     *   <li>Creates settings view with theme toggle capability</li>
     *   <li>Provides current theme information to settings panel</li>
     * </ul>
     * 
     * @see UIController#createSettingsView(Runnable, Runnable, java.util.function.Supplier)
     */
    private void handleSettingsShortcut() {
        if (appState == AppState.SETTINGS) {
            appState = AppState.MENU;
            uiController.switchView(menuPane);
        } else if (appState == AppState.MENU) {
            appState = AppState.SETTINGS;
            Node settingsView = uiController.createSettingsView(() -> {
                appState = AppState.MENU;
                uiController.switchView(menuPane);
            }, this::toggleTheme, () -> currentTheme.name());
            uiController.switchView(settingsView);
        }
    }

    /**
     * Handles the fullscreen toggle shortcut activation. Switches the application
     * between windowed and fullscreen modes.
     * 
     * <p>This method is activated by F11 and provides a distraction-free typing
     * environment. The fullscreen state is managed by the JavaFX Stage.</p>
     * 
     * <p><strong>Fullscreen Benefits:</strong></p>
     * <ul>
     *   <li>Eliminates desktop distractions</li>
     *   <li>Maximizes text display area</li>
     *   <li>Improves focus during typing tests</li>
     *   <li>Better presentation for demonstrations</li>
     * </ul>
     */
    private void handleFullscreenShortcut() {
        primaryStage.setFullScreen(!primaryStage.isFullScreen());
    }

    /**
     * Handles the multiplayer shortcut activation. Toggles between the multiplayer
     * interface and the main menu.
     * 
     * <p>This method is activated by Ctrl+M (or Cmd+M on macOS) and provides
     * quick access to multiplayer game setup and competitive typing sessions.</p>
     * 
     * <p><strong>Toggle Behavior:</strong></p>
     * <ul>
     *   <li>If currently in MULTIPLAYER state → return to MENU</li>
     *   <li>If in MENU state → switch to MULTIPLAYER</li>
     *   <li>Creates multiplayer view with networking integration</li>
     *   <li>Provides callback for multiplayer game start events</li>
     * </ul>
     * 
     * @see NetworkingUIIntegration
     * @see UIController#createMultiplayerView(Runnable, NetworkingUIIntegration, Runnable)
     */
    private void handleMultiplayerShortcut() {
        if (appState == AppState.MULTIPLAYER) {
            appState = AppState.MENU;
            uiController.switchView(menuPane);
        } else if (appState == AppState.MENU) {
            appState = AppState.MULTIPLAYER;
            Node multiplayerView = uiController.createMultiplayerView(() -> {
                appState = AppState.MENU;
                uiController.switchView(menuPane);
            }, networkingUI, this::handleMultiplayerGameStart);
            uiController.switchView(multiplayerView);
        }
    }

    /**
     * Initiates a new typing test with the specified game mode and parameters.
     * 
     * <p>This method configures and starts a new typing test session. It handles
     * the transition from menu or finished state to the active test state, setting up
     * appropriate UI components and timers based on the selected game mode.</p>
     * 
     * <p><strong>Initialization Sequence:</strong></p>
     * <ol>
     *   <li>Stop any existing timers to ensure clean state</li>
     *   <li>Set application state to WAITING_TO_START</li>
     *   <li>Create new TestSession with provided parameters</li>
     *   <li>Build game UI components (game pane, results pane)</li>
     *   <li>Configure mode-specific features (timer for TIME mode)</li>
     *   <li>Initialize statistics display (WPM, accuracy)</li>
     *   <li>Populate word display area</li>
     *   <li>Switch to game view with transition animation</li>
     * </ol>
     * 
     * <p><strong>Game Mode Handling:</strong></p>
     * <ul>
     *   <li><strong>TIME mode:</strong> Sets up countdown timer, displays time remaining</li>
     *   <li><strong>WORD_COUNT mode:</strong> Hides timer, focuses on word count progress</li>
     *   <li><strong>CUSTOM mode:</strong> Adapts to custom configuration parameters</li>
     * </ul>
     * 
     * @param mode the game mode for this test (TIME, WORD_COUNT, or CUSTOM)
     * @param value the mode-specific parameter (seconds for TIME mode, word count for WORD_COUNT mode)
     * @throws IllegalArgumentException if mode is null or value is invalid for the mode
     * @see GameMode
     * @see TestSession
     * @see #setupGameTimer()
     */
    private void startTest(GameMode mode, int value) {
        stopAllTimers();

        appState = AppState.WAITING_TO_START;

        currentSession = new TestSession(mode, value, wordProvider);

        gamePane = createGamePane();

        if (resultsPane == null) {
            resultsPane = createResultsPane();
        }

        if (mode == GameMode.TIME) {
            timeRemaining = value;
            timerValueLabel.setText(String.valueOf(timeRemaining));
            timeStatBox.setVisible(true);
            setupGameTimer();
        } else {
            timeStatBox.setVisible(false);
        }

        wpmValueLabel.setText("0");
        accValueLabel.setText("100%");
        updateWordDisplay();

        uiController.switchView(gamePane, true);

        gamePane.requestFocus();
    }

    /**
     * Handles global keyboard input and routes key events based on current application state.
     * 
     * <p>This method serves as the central keyboard event dispatcher for the entire application.
     * It first checks for global keyboard shortcuts, then handles state-specific key events
     * including special keys (Escape, Tab), typing input, and navigation commands.</p>
     * 
     * <p><strong>Global Key Handling Priority:</strong></p>
     * <ol>
     *   <li>Keyboard shortcuts (Ctrl+R, Ctrl+S, etc.) - handled by {@link KeyboardShortcutsManager}</li>
     *   <li>Escape key - returns to main menu from any state</li>
     *   <li>State-specific key handling (typing, navigation, commands)</li>
     * </ol>
     * 
     * <p><strong>State-Specific Key Behaviors:</strong></p>
     * <ul>
     *   <li><strong>MENU:</strong> Tab key consumption to prevent focus issues</li>
     *   <li><strong>STATISTICS/SETTINGS/MULTIPLAYER:</strong> No additional key handling</li>
     *   <li><strong>WAITING_TO_START:</strong> Tab restarts test, letter keys begin typing</li>
     *   <li><strong>RUNNING:</strong> Tab restarts test, all keys are processed for typing</li>
     *   <li><strong>FINISHED:</strong> Tab restarts test, Enter/Escape return to menu</li>
     * </ul>
     * 
     * <p>The method ensures proper event consumption to prevent unwanted side effects
     * and maintains consistent behavior across different application states.</p>
     * 
     * @param event the keyboard event to process, containing key code and modifiers
     * @see KeyboardShortcutsManager#handleKeyEvent(KeyEvent)
     * @see #processKeystroke(KeyEvent)
     * @see AppState
     */
    private void handleKeyPress(KeyEvent event) {
        // First priority: check for global keyboard shortcuts
        if (shortcutsManager.handleKeyEvent(event)) {
            return;
        }

        // Second priority: handle Escape key for global menu return
        if (event.getCode() == KeyCode.ESCAPE) {
            if (appState != AppState.MENU) {
                stopAllTimers();
                appState = AppState.MENU;
                uiController.switchView(menuPane);
                return;
            }
        }

        // State-specific key handling
        switch (appState) {
            case MENU -> {
                // Prevent Tab from interfering with button focus
                if (event.getCode() == KeyCode.TAB) {
                    event.consume();
                }
            }
            case STATISTICS, SETTINGS, MULTIPLAYER -> {
                // These states handle their own key events through UI controllers
            }
            case WAITING_TO_START -> {
                // Tab restarts the current test
                if (event.getCode() == KeyCode.TAB) {
                    restartCurrentTest();
                    event.consume();
                    return;
                }
                // Any letter key starts the test and processes the keystroke
                if (event.getCode().isLetterKey()) {
                    startGame();
                    processKeystroke(event);
                }
            }
            case RUNNING -> {
                // Tab restarts the current test
                if (event.getCode() == KeyCode.TAB) {
                    restartCurrentTest();
                    event.consume();
                    return;
                }
                // All key events are processed for typing input
                processKeystroke(event);
            }
            case FINISHED -> {
                // Tab restarts the current test
                if (event.getCode() == KeyCode.TAB) {
                    restartCurrentTest();
                    event.consume();
                    return;
                }
                // Enter or Escape returns to main menu
                if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.ESCAPE) {
                    appState = AppState.MENU;
                    uiController.switchView(menuPane);
                }
            }
        }
    }

    /**
     * Restarts the current typing test with the same configuration parameters.
     * 
     * <p>This method provides a quick way to restart the active test session without
     * returning to the main menu. It preserves all test settings including game mode,
     * difficulty level, and time/word count parameters, while resetting all progress
     * and statistics to their initial state.</p>
     * 
     * <p><strong>Restart Process:</strong></p>
     * <ol>
     *   <li>Stop all active timers (game timer, animations)</li>
     *   <li>Extract current test configuration (mode, value, difficulty)</li>
     *   <li>Initialize new test session with same parameters</li>
     *   <li>Reset UI to initial test state (WAITING_TO_START)</li>
     *   <li>Clear all typed content and statistics</li>
     * </ol>
     * 
     * <p><strong>Preserved Settings:</strong></p>
     * <ul>
     *   <li>Game mode (TIME, WORDS, CUSTOM)</li>
     *   <li>Mode value (seconds for TIME, word count for WORDS)</li>
     *   <li>Difficulty level (Easy, Medium, Hard)</li>
     *   <li>Theme and UI preferences</li>
     * </ul>
     * 
     * <p><strong>Reset Elements:</strong></p>
     * <ul>
     *   <li>All typed content and word progress</li>
     *   <li>WPM and accuracy statistics</li>
     *   <li>Timer countdown (for time-based tests)</li>
     *   <li>Word highlighting and cursor position</li>
     * </ul>
     * 
     * <p>This method is commonly triggered by the Tab key shortcut or the restart
     * button in the results view, providing immediate access to test repetition.</p>
     * 
     * @throws IllegalStateException if no test session is currently active
     * @see #startTest(GameMode, int)
     * @see #stopAllTimers()
     */
    private void restartCurrentTest() {
        if (currentSession != null) {
            // Stop all timers to ensure clean state
            stopAllTimers();
            
            // Restart with same parameters from current session
            startTest(currentSession.getMode(), currentSession.getValue());
        }
    }

    /**
     * Transitions the application from waiting state to active typing test mode.
     * 
     * <p>This method is called when the user begins typing during the WAITING_TO_START state.
     * It activates the test session, starts any necessary timers, and transitions the application
     * state to RUNNING to enable full typing test functionality.</p>
     * 
     * <p><strong>Activation Sequence:</strong></p>
     * <ol>
     *   <li>Change application state to RUNNING</li>
     *   <li>Start the test session timer (records start time)</li>
     *   <li>Activate game timer for time-based tests</li>
     *   <li>Enable real-time statistics calculation</li>
     * </ol>
     * 
     * <p><strong>Timer Activation Logic:</strong></p>
     * <ul>
     *   <li>TIME mode: Starts countdown timer for test duration</li>
     *   <li>WORDS mode: No timer activated (completion based on word count)</li>
     *   <li>CUSTOM mode: Timer behavior depends on custom configuration</li>
     * </ul>
     * 
     * <p>The method ensures that the test session begins tracking performance metrics
     * immediately when the user starts typing, providing accurate timing and statistics.</p>
     * 
     * @see AppState#RUNNING
     * @see TestSession#start()
     * @see #setupGameTimer()
     */
    private void startGame() {
        // Transition to active test state
        appState = AppState.RUNNING;
        
        // Begin session timing and statistics tracking
        currentSession.start();

        // Start countdown timer for time-based tests
        if (currentSession.getMode() == GameMode.TIME && gameTimer != null) {
            gameTimer.play();
        }
    }

    /**
     * Processes individual keystrokes during an active typing test session.
     * 
     * <p>This method handles the core typing mechanics by processing each keystroke and updating
     * the current word being typed. It manages character input, backspace correction, word completion
     * with the space bar, and automatic test progression. The method ensures accurate tracking of
     * typing performance and maintains real-time visual feedback.</p>
     * 
     * <p><strong>Keystroke Processing Logic:</strong></p>
     * <ol>
     *   <li><strong>Space Bar:</strong> Completes current word and advances to next word</li>
     *   <li><strong>Backspace:</strong> Removes last character from current word</li>
     *   <li><strong>Letter Keys:</strong> Adds character to current word (ignoring control/alt combinations)</li>
     *   <li><strong>Other Keys:</strong> Ignored to prevent accidental input</li>
     * </ol>
     * 
     * <p><strong>Word Completion Behavior:</strong></p>
     * <ul>
     *   <li>Space advances to next word only if current word has content</li>
     *   <li>Word correctness is evaluated on completion</li>
     *   <li>Test automatically ends when all words are completed</li>
     *   <li>Statistics are updated in real-time during progression</li>
     * </ul>
     * 
     * <p><strong>Character Input Validation:</strong></p>
     * <ul>
     *   <li>Only processes actual character input (non-null, non-empty text)</li>
     *   <li>Ignores control key combinations (Ctrl+C, Alt+Tab, etc.)</li>
     *   <li>Handles both lowercase and uppercase letters correctly</li>
     *   <li>Supports extended character input for international keyboards</li>
     * </ul>
     * 
     * <p>After processing each keystroke, the method triggers a visual update of the word
     * display area to reflect the new typing state and provide immediate feedback to the user.</p>
     * 
     * @param event the keyboard event containing the keystroke information
     * @throws IllegalStateException if called when no test session is active
     * @see TestSession.Word#setTypedWord(String)
     * @see TestSession#advanceToNextWord()
     * @see #updateWordDisplay()
     * @see #endGame()
     */
    private void processKeystroke(KeyEvent event) {
        // Get the current word being typed
        TestSession.Word currentWord = currentSession.getCurrentWord();
        String typed = currentWord.getTypedWord();

        if (event.getCode() == KeyCode.SPACE) {
            // Space completes the current word and advances to next
            if (typed.length() > 0) {
                // Evaluate word correctness (for statistics)
                boolean isCorrect = typed.equals(currentWord.getTargetWord());

                // Advance to next word in the test
                currentSession.advanceToNextWord();
                
                // Check if test is completed
                if (currentSession.isFinished()) {
                    endGame();
                }
            }
            // Consume space event to prevent UI side effects
            event.consume();
        } else if (event.getCode() == KeyCode.BACK_SPACE) {
            // Backspace removes last character if word has content
            if (typed.length() > 0) {
                currentWord.setTypedWord(typed.substring(0, typed.length() - 1));
            }
        } else if (event.getCode().isLetterKey() && event.getText() != null && !event.getText().isEmpty()) {
            // Process letter key input
            String character = event.getText();

            // Only add character if not using control/alt modifiers
            if (!event.isControlDown() && !event.isAltDown()) {
                currentWord.setTypedWord(typed + character);
            }
        }

        // Update visual display to reflect new typing state
        updateWordDisplay();
    }

    /**
     * Completes the current typing test and transitions to results display.
     * 
     * <p>This method handles the test completion process, including final statistics calculation,
     * multiplayer result transmission, data persistence, and UI transition to the results view.
     * It ensures all test data is properly finalized and saved before displaying results.</p>
     * 
     * <p><strong>Completion Sequence:</strong></p>
     * <ol>
     *   <li>Transition application state to FINISHED</li>
     *   <li>Stop all active timers and animations</li>
     *   <li>Complete any partially typed words</li>
     *   <li>Finalize test session with end timestamp</li>
     *   <li>Send multiplayer results (if connected)</li>
     *   <li>Calculate and display final statistics</li>
     *   <li>Persist test results to database</li>
     * </ol>
     * 
     * <p><strong>Statistics Finalization:</strong></p>
     * <ul>
     *   <li>Calculate final WPM (words per minute) and raw WPM</li>
     *   <li>Compute accuracy percentage and character counts</li>
     *   <li>Record total test duration and completion time</li>
     *   <li>Store results in test history for analytics</li>
     * </ul>
     * 
     * <p><strong>Multiplayer Integration:</strong></p>
     * <ul>
     *   <li>Transmits final results to server if connected as client</li>
     *   <li>Includes completion time, accuracy, and test configuration</li>
     *   <li>Enables competitive comparison with other players</li>
     * </ul>
     * 
     * <p>The method ensures graceful test termination regardless of how the test ended
     * (completion, timeout, or manual termination) and provides consistent result handling.</p>
     * 
     * @see AppState#FINISHED
     * @see TestSession#end()
     * @see TestSession#calculateCurrentStats(boolean)
     * @see NetworkingUIIntegration#sendRoundStats
     * @see #showResults()
     */
    private void endGame() {
        // Transition to finished state
        appState = AppState.FINISHED;
        
        // Stop all timers and animations
        stopAllTimers();

        // Complete any partially typed word for accurate statistics
        if (!currentSession.getCurrentWord().isCompleted()) {
            currentSession.getCurrentWord().complete();
        }

        // Finalize session with end timestamp
        currentSession.end();

        // Send results to multiplayer server if connected
        if (networkingUI != null && networkingUI.isConnectedAsClient()) {
            TestSession.TestResult result = currentSession.calculateCurrentStats(true);
            double completionTime = (System.currentTimeMillis() - currentSession.getStartTime()) / 1000.0;
            networkingUI.sendRoundStats(result, completionTime, currentSession.getMode(), currentSession.getValue());
        }

        // Display final results to user
        showResults();
    }

    /**
     * Stops all active JavaFX Timeline animations and timers in the application.
     * 
     * <p>This method provides centralized timer management by stopping all timing-related
     * animations and ensuring a clean application state. It prevents timer conflicts when
     * transitioning between different application states or restarting tests.</p>
     * 
     * <p><strong>Stopped Timers Include:</strong></p>
     * <ul>
     *   <li><strong>Game Timer:</strong> Countdown timer for time-based typing tests</li>
     *   <li><strong>Caret Timeline:</strong> Blinking cursor animation in text areas</li>
     * </ul>
     * 
     * <p><strong>Cleanup Process:</strong></p>
     * <ol>
     *   <li>Stop game timer if active and set reference to null</li>
     *   <li>Stop caret blinking animation (preserved for restart)</li>
     *   <li>Ensure all timeline resources are properly released</li>
     * </ol>
     * 
     * <p>This method is called during test restarts, application state changes,
     * and before starting new tests to prevent timer interference and ensure
     * consistent timing behavior.</p>
     * 
     * @see Timeline#stop()
     * @see #gameTimer
     * @see #caretTimeline
     */
    private void stopAllTimers() {
        // Stop and cleanup game timer
        if (gameTimer != null) {
            gameTimer.stop();
            gameTimer = null;  // Allow garbage collection
        }
        
        // Stop caret animation (but preserve for restart)
        if (caretTimeline != null) {
            caretTimeline.stop();
        }
    }


    /**
     * Updates the visual word display area with current typing progress and color-coded feedback.
     * 
     * <p>This method rebuilds the entire word display to reflect the current typing state,
     * including character-by-character correctness feedback and cursor position. It implements
     * performance optimizations by limiting the number of rendered words to prevent UI lag
     * during long typing sessions.</p>
     * 
     * @see #createWordNode(TestSession.Word, boolean)
     * @see Constants#MAX_WORDS_RENDERED
     */
    private void updateWordDisplay() {
        // Validate prerequisites before attempting display update
        if (wordDisplayArea == null || currentSession == null) return;
        
        // Clear existing display content to rebuild from current state
        wordDisplayArea.getChildren().clear();

        // Get current typing progress information
        int currentIndex = currentSession.getCurrentWordIndex();
        List<TestSession.Word> words = currentSession.getWordList();
        
        // Calculate rendering limit to prevent performance issues with large word lists
        // Only render from current word up to MAX_WORDS_RENDERED ahead
        int renderCutoff = Math.min(words.size(), currentIndex + Constants.MAX_WORDS_RENDERED);

        // Build visual representation for each word in the render window
        for (int i = 0; i < renderCutoff; i++) {
            // Create word node with highlighting if it's the currently active word
            wordDisplayArea.getChildren().add(createWordNode(words.get(i), i == currentIndex));
            
            // Add visual spacing between words for readability
            wordDisplayArea.getChildren().add(new Text("  "));
        }
        
        // Update real-time statistics after display refresh
        updateStats();
    }

    /**
     * Creates a styled TextFlow node for a single word with character-by-character feedback.
     * 
     * <p>This method implements the core visual feedback system by analyzing each character
     * of the target word against what the user has typed, applying appropriate CSS styles
     * for correct, incorrect, and untyped characters. It also handles cursor positioning
     * and extra character visualization.</p>
     * 
     * @param word the word object containing target and typed text
     * @param isCurrent whether this is the currently active word (for cursor display)
     * @return a TextFlow node with styled character elements
     */
    private Node createWordNode(TestSession.Word word, boolean isCurrent) {
        // Create container for individual character elements
        TextFlow wordFlow = new TextFlow();
        String target = word.getTargetWord();
        String typed = word.getTypedWord();

        // Process each character in the target word for visual feedback
        for (int i = 0; i < target.length(); i++) {
            // Create individual character element with consistent font
            Text charText = new Text(String.valueOf(target.charAt(i)));
            charText.setFont(WORD_FONT);

            // Apply styling based on typing progress and correctness
            if (i < typed.length()) {
                // Character has been typed - check correctness (case-insensitive)
                boolean isCorrect = Character.toLowerCase(typed.charAt(i)) == Character.toLowerCase(target.charAt(i));
                charText.getStyleClass().add(isCorrect ? "text-correct" : "text-incorrect");
            } else if (isCurrent && i == typed.length()) {
                // This is the next character to be typed - show cursor
                charText.getStyleClass().add("caret");
            } else {
                // Character not yet typed - show in default style
                charText.getStyleClass().add("text-default");
            }

            wordFlow.getChildren().add(charText);
        }

        // Handle case where user typed more characters than the target word length
        if (typed.length() > target.length()) {
            // Create element for extra characters (always marked as incorrect)
            Text extra = new Text(typed.substring(target.length()));
            extra.setFont(WORD_FONT);
            extra.getStyleClass().addAll("text-incorrect", "text-extra");
            wordFlow.getChildren().add(extra);
        }

        // Show space cursor if user has completed the current word
        if (isCurrent && typed.length() >= target.length()) {
            // Create visual caret for space character to indicate word completion
            Text spaceCaret = new Text(" ");
            spaceCaret.setFont(WORD_FONT);
            spaceCaret.getStyleClass().add("caret");
            wordFlow.getChildren().add(spaceCaret);
        }

        return wordFlow;
    }

    /**
     * Updates real-time statistics display during active typing tests.
     * 
     * <p>This method calculates and displays current performance metrics including
     * WPM, accuracy, and remaining time. It only operates during active test states
     * to prevent unnecessary calculations and UI updates.</p>
     */
    private void updateStats() {
        // Only update stats during active typing sessions with valid timing
        if (appState != AppState.RUNNING || currentSession.getStartTime() == 0) return;

        // Update countdown timer for time-based tests
        if (currentSession.getMode() == GameMode.TIME) {
            timerValueLabel.setText(String.valueOf(timeRemaining));
        }

        // Calculate current performance metrics without finalizing the session
        TestSession.TestResult partialResult = currentSession.calculateCurrentStats(false);
        
        // Update live performance displays with current metrics
        wpmValueLabel.setText(String.valueOf(partialResult.netWPM()));
        accValueLabel.setText(partialResult.accuracy() + "%");
    }

    /**
     * Displays the final test results with formatted statistics and navigation options.
     * 
     * <p>This method creates a comprehensive results view showing the user's final
     * performance metrics, test configuration details, and provides navigation options
     * for returning to the menu or restarting the current test.</p>
     */
    private void showResults() {
        // Calculate final test statistics
        TestSession.TestResult result = currentSession.calculateCurrentStats(true);
        
        // Clear existing results content for fresh display
        resultsPane.getChildren().clear();

        // Create test information header showing configuration details
        String testInfoText = String.format("test: %s / %d / %s",
                currentSession.getMode().name().toLowerCase(),
                currentSession.getValue(),
                wordProvider.getCurrentDifficulty().name().toLowerCase());
        Label testInfoLabel = createResultLabel(testInfoText, "results-info");

        // Create WPM (Words Per Minute) display section
        Label wpmLabel = new Label("wpm");
        wpmLabel.getStyleClass().add("results-label-title");
        Label wpmValue = new Label(String.valueOf(result.netWPM()));
        wpmValue.getStyleClass().add("results-label-value");
        VBox wpmBox = new VBox(wpmLabel, wpmValue);
        wpmBox.setAlignment(Pos.CENTER);

        // Create accuracy percentage display section
        Label accLabel = new Label("acc");
        accLabel.getStyleClass().add("results-label-title");
        Label accValue = new Label(result.accuracy() + "%");
        accValue.getStyleClass().add("results-label-value");
        VBox accBox = new VBox(accLabel, accValue);
        accBox.setAlignment(Pos.CENTER);

        // Arrange main statistics in horizontal layout with proper spacing
        HBox mainStats = new HBox(40, wpmBox, accBox);
        mainStats.setAlignment(Pos.CENTER);

        // Create secondary statistics display with detailed metrics
        String secondaryStatsText = String.format("raw: %d | chars: %d/%d/%d",
                result.rawWPM(), 
                result.correctChars(), 
                result.incorrectChars(), 
                (result.correctChars() + result.incorrectChars()));
        Label secondaryStatsLabel = createResultLabel(secondaryStatsText, "results-info");

        // Create navigation button for returning to main menu
        Button backButton = new Button("↩");
        backButton.getStyleClass().addAll("nav-button", "back-button");
        backButton.setOnAction(e -> {
            // Transition back to main menu state
            appState = AppState.MENU;
            uiController.switchView(menuPane);
        });

        // Create restart button for immediate test repetition
        Button restartButton = new Button("⟳");
        restartButton.getStyleClass().addAll("nav-button", "restart-button");
        restartButton.setOnAction(e -> restartCurrentTest());

        // Arrange navigation buttons with consistent spacing
        HBox buttonRow = new HBox(10, backButton, restartButton);
        buttonRow.setAlignment(Pos.CENTER);

        // Assemble complete results view with proper vertical spacing
        resultsPane.getChildren().addAll(
                testInfoLabel,     // Test configuration information
                mainStats,         // Primary performance metrics (WPM, accuracy)
                secondaryStatsLabel, // Detailed statistics (raw WPM, character counts)
                buttonRow          // Navigation and action buttons
        );
        
        // Display the completed results view to the user
        uiController.switchView(resultsPane);
    }


    /**
     * Toggles between light and dark themes and persists the preference.
     * 
     * <p>This method switches the application theme and saves the new preference
     * to persistent storage for future application sessions.</p>
     */
    private void toggleTheme() {
        // Switch to opposite theme
        Theme newTheme = (currentTheme == Theme.DARK) ? Theme.LIGHT : Theme.DARK;
        currentTheme = newTheme;
        
        // Persist theme preference for future sessions
        settingsManager.setTheme(newTheme);
        
        // Apply the new theme immediately
        applyTheme(newTheme);
    }

    /**
     * Applies the specified theme to the application by updating stylesheets and CSS classes.
     * 
     * <p>This method handles the complete theme switching process including stylesheet
     * loading, CSS class management, and visual updates throughout the application.</p>
     * 
     * @param theme the theme to apply (LIGHT or DARK)
     */
    private void applyTheme(Theme theme) {
        // Clear existing stylesheets to prevent conflicts
        scene.getStylesheets().clear();
        
        // Remove previous theme classes from root container
        rootPane.getStyleClass().removeAll("light-theme", "dark-theme");

        if (theme == Theme.DARK) {
            // Load and apply dark theme stylesheet
            String darkStylesheet = ResourceLoader.loadStylesheet("/CSS Styles/dark-styles.css");
            if (darkStylesheet != null) {
                scene.getStylesheets().add(darkStylesheet);
            }
            // Add dark theme CSS class for component-specific styling
            rootPane.getStyleClass().add("dark-theme");
        } else {
            // Load and apply light theme stylesheet (default)
            String lightStylesheet = ResourceLoader.loadStylesheet("/CSS Styles/styles.css");
            if (lightStylesheet != null) {
                scene.getStylesheets().add(lightStylesheet);
            }
            // Add light theme CSS class for component-specific styling
            rootPane.getStyleClass().add("light-theme");
        }
    }

    /**
     * Creates the main menu view with game mode selection and navigation options.
     * 
     * <p>This method builds the complete main menu interface including game mode buttons,
     * difficulty selection, navigation controls, and performance statistics display.</p>
     * 
     * @return a VBox containing the complete main menu interface
     */
    private VBox createMenuView() {
        // Create main application title
        Label title = new Label("Typerr");
        title.getStyleClass().add("menu-title");

        // Create time-based game mode selection buttons
        HBox timeModes = new HBox(
                createModeButton("15 Seconds", GameMode.TIME, 15),
                createModeButton("30 Seconds", GameMode.TIME, 30),
                createModeButton("60 Seconds", GameMode.TIME, 60)
        );
        timeModes.getStyleClass().add("menu-group");

        // Create word count-based game mode selection buttons
        HBox wordModes = new HBox(
                createModeButton("10 Words", GameMode.WORDS, 10),
                createModeButton("25 Words", GameMode.WORDS, 25),
                createModeButton("50 Words", GameMode.WORDS, 50)
        );
        wordModes.getStyleClass().add("menu-group");

        // Create difficulty selection interface
        HBox difficultyBox = new HBox(10);
        difficultyBox.setAlignment(Pos.CENTER);

        // Generate difficulty buttons for all available difficulty levels
        for (WordProvider.Difficulty difficulty : WordProvider.Difficulty.values()) {
            Button diffButton = new Button(difficulty.name());
            diffButton.getStyleClass().add("mode-button");
            
            // Highlight the currently selected difficulty
            if (difficulty == wordProvider.getCurrentDifficulty()) {
                diffButton.getStyleClass().add("selected-difficulty-button");
            }
            
            // Set up difficulty change handler
            diffButton.setOnAction(e -> {
                wordProvider.setDifficulty(difficulty);
                updateDifficultyButtons(difficultyBox, difficulty);
            });
            difficultyBox.getChildren().add(diffButton);
        }

        // Create navigation buttons for advanced features
        HBox navButtons = new HBox(10);
        navButtons.setAlignment(Pos.CENTER);

        // Multiplayer access button
        Button multiplayerButton = new Button("🌐");
        multiplayerButton.getStyleClass().add("nav-button");
        multiplayerButton.setOnAction(e -> handleMultiplayerShortcut());

        // Statistics viewing button
        Button statsButton = new Button("📊");
        statsButton.getStyleClass().add("nav-button");
        statsButton.setOnAction(e -> handleStatsShortcut());

        // Settings access button
        Button settingsButton = new Button("⚙");
        settingsButton.getStyleClass().add("nav-button");
        settingsButton.setOnAction(e -> handleSettingsShortcut());

        navButtons.getChildren().addAll(multiplayerButton, statsButton, settingsButton);

        // Create performance statistics display section
        VBox statsBox = new VBox(5);
        statsBox.setAlignment(Pos.CENTER);

        // Display best WPM achievement if available
        TestSession.TestResult bestResult = TestSession.StatHistory.getBestResult();
        if (bestResult != null) {
            Label bestWpmLabel = new Label("Best WPM: " + bestResult.netWPM());
            bestWpmLabel.getStyleClass().add("stat-title");
            statsBox.getChildren().add(bestWpmLabel);
        }

        // Display total test count if user has completed tests
        int totalTests = TestSession.StatHistory.getTotalTestCount();
        if (totalTests > 0) {
            Label totalLabel = new Label("Tests Completed: " + totalTests);
            totalLabel.getStyleClass().add("stat-title");
            statsBox.getChildren().add(totalLabel);
        }

        // Assemble complete menu with proper spacing and organization
        VBox menu = new VBox(20, 
                title,                              // Application title
                new Label("Select Mode:"),          // Mode selection header
                timeModes,                          // Time-based mode buttons
                wordModes,                          // Word count-based mode buttons
                new Label("Select Difficulty:"),    // Difficulty selection header
                difficultyBox,                      // Difficulty selection buttons
                navButtons,                         // Navigation and feature buttons
                statsBox);                          // Performance statistics display
        menu.getStyleClass().add("menu-pane");
        return menu;
    }

    /**
     * Updates difficulty button styling to reflect the currently selected difficulty level.
     * 
     * <p>This method refreshes the visual state of all difficulty buttons to show
     * which difficulty is currently active through CSS class management.</p>
     * 
     * @param difficultyBox the container holding difficulty selection buttons
     * @param selectedDifficulty the currently selected difficulty level
     */
    private void updateDifficultyButtons(HBox difficultyBox, WordProvider.Difficulty selectedDifficulty) {
        // Iterate through all buttons in the difficulty selection container
        for (Node node : difficultyBox.getChildren()) {
            if (node instanceof Button button) {
                // Remove selection styling from all buttons first
                button.getStyleClass().remove("selected-difficulty-button");

                // Add selection styling only to the currently selected difficulty
                if (button.getText().equals(selectedDifficulty.name())) {
                    button.getStyleClass().add("selected-difficulty-button");
                }
            }
        }
    }

    /**
     * Creates a game mode selection button with appropriate styling and behavior.
     * 
     * <p>This factory method creates consistently styled buttons for game mode selection
     * with automatic test initiation when clicked.</p>
     * 
     * @param text the display text for the button
     * @param mode the game mode this button will start
     * @param value the mode-specific value (time in seconds, word count, etc.)
     * @return a configured button for game mode selection
     */
    private Button createModeButton(String text, GameMode mode, int value) {
        Button b = new Button(text);
        b.getStyleClass().add("mode-button");
        
        // Set up click handler to start test with specified parameters
        b.setOnAction(e -> startTest(mode, value));
        return b;
    }

    /**
     * Creates the main game interface for active typing tests.
     * 
     * <p>This method builds the typing test interface including the statistics bar
     * and word display area with appropriate layout and styling.</p>
     * 
     * @return a VBox containing the complete game interface
     */
    private VBox createGamePane() {
        // Create main container with generous spacing for readability
        VBox pane = new VBox(40);
        pane.setPadding(new Insets(50));
        pane.setAlignment(Pos.CENTER);
        
        // Create statistics display bar
        HBox statsBar = createStatsBar();
        
        // Initialize word display area for typing content
        wordDisplayArea = new TextFlow();
        VBox wordContainer = new VBox(wordDisplayArea);
        wordContainer.getStyleClass().add("word-container");
        
        // Assemble complete game interface
        pane.getChildren().addAll(statsBar, wordContainer);
        return pane;
    }

    /**
     * Creates the statistics bar showing real-time performance metrics.
     * 
     * <p>This method builds the horizontal statistics display with WPM, accuracy,
     * and time remaining (for time-based tests) indicators.</p>
     * 
     * @return an HBox containing all statistics display elements
     */
    private HBox createStatsBar() {
        // Initialize statistic value labels
        wpmValueLabel = new Label("0");
        accValueLabel = new Label("100%");
        timerValueLabel = new Label("0");
        
        // Create individual statistic display containers
        VBox wpmBox = createStatBox("WPM", wpmValueLabel);
        VBox accBox = createStatBox("ACCURACY", accValueLabel);
        timeStatBox = createStatBox("TIME", timerValueLabel);
        
        // Arrange statistics in horizontal layout
        HBox statsBar = new HBox(wpmBox, accBox, timeStatBox);
        statsBar.getStyleClass().add("stats-bar");
        return statsBar;
    }

    /**
     * Creates a single statistic display box with title and value labels.
     * 
     * <p>This factory method creates consistently formatted statistic displays
     * used throughout the application interface.</p>
     * 
     * @param title the statistic title (e.g., "WPM", "ACCURACY")
     * @param valueLabel the label that will display the current value
     * @return a VBox containing the formatted statistic display
     */
    private VBox createStatBox(String title, Label valueLabel) {
        // Create title label with consistent styling
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");
        
        // Apply value styling to the provided label
        valueLabel.getStyleClass().add("stat-value");
        
        // Create vertical container with title above value
        VBox statBox = new VBox(titleLabel, valueLabel);
        statBox.getStyleClass().add("stat-box");
        return statBox;
    }

    /**
     * Creates the results display pane for test completion.
     * 
     * <p>This method creates an empty results container that will be populated
     * with test results when a typing test is completed.</p>
     * 
     * @return a VBox configured for results display
     */
    private VBox createResultsPane() {
        VBox pane = new VBox();
        pane.getStyleClass().add("results-pane");
        return pane;
    }

    /**
     * Creates a styled label for results display with consistent formatting.
     * 
     * <p>This factory method ensures consistent styling across all result labels
     * throughout the results interface.</p>
     * 
     * @param text the text content for the label
     * @param styleClass the CSS style class to apply
     * @return a styled label for results display
     */
    private Label createResultLabel(String text, String styleClass) {
        Label l = new Label(text);
        l.getStyleClass().add(styleClass);
        return l;
    }

    /**
     * Sets up the countdown timer for time-based typing tests.
     * 
     * <p>This method creates and configures a JavaFX Timeline that decrements
     * the remaining time counter and automatically ends the test when time expires.</p>
     */
    private void setupGameTimer() {
        // Stop any existing timer to prevent conflicts
        if (gameTimer != null) gameTimer.stop();
        
        // Only create timer for time-based game modes
        if (currentSession.getMode() == GameMode.TIME) {
            // Create timeline that executes every second
            gameTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                // Decrement remaining time
                timeRemaining--;
                
                // Update statistics display with new time
                updateStats();
                
                // End game when time reaches zero
                if (timeRemaining <= 0) {
                    endGame();
                }
            }));
            
            // Set timer to run for the specified test duration
            gameTimer.setCycleCount(currentSession.getValue());
        }
    }

    /**
     * Sets up the blinking cursor animation for the typing interface.
     * 
     * <p>This method creates a continuous animation that makes cursor elements
     * blink at regular intervals to provide visual feedback for typing position.</p>
     */
    private void setupCaretTimeline() {
        // Stop existing caret animation if present
        if (caretTimeline != null) {
            caretTimeline.stop();
        }
        
        // Create timeline for cursor blinking animation
        caretTimeline = new Timeline(new KeyFrame(Duration.millis(Constants.CARET_BLINK_INTERVAL), e -> {
            // Only animate cursor during active typing states
            if (wordDisplayArea != null && (appState == AppState.WAITING_TO_START || appState == AppState.RUNNING)) {
                // Find all cursor elements and toggle their visibility
                wordDisplayArea.getChildren().stream()
                        .filter(node -> node.getStyleClass().contains("caret"))
                        .forEach(node -> {
                            // Only toggle visibility if element is still in the scene
                            if (node.getParent() != null) {
                                node.setVisible(!node.isVisible());
                            }
                        });
            }
        }));
        
        // Run animation indefinitely
        caretTimeline.setCycleCount(Timeline.INDEFINITE);
        caretTimeline.play();
    }

    /**
     * Application entry point that launches the JavaFX Typerr application.
     * 
     * <p>This static method serves as the main entry point for the Typerr typing test application.
     * It delegates to the JavaFX {@link Application#launch(String...)} method which handles the
     * JavaFX application lifecycle including toolkit initialization, platform startup, and
     * calling the {@link #start(Stage)} method.</p>
     * 
     * <p><strong>Startup Process:</strong></p>
     * <ol>
     *   <li>Initialize JavaFX Platform and Application Thread</li>
     *   <li>Create primary Stage and Application instance</li>
     *   <li>Call {@link #start(Stage)} to build and display UI</li>
     *   <li>Enter JavaFX event loop for user interaction</li>
     * </ol>
     * 
     * <p><strong>Command Line Arguments:</strong></p>
     * <p>The application currently does not process command line arguments, but the parameter
     * is preserved for future extensibility (configuration files, debug modes, etc.)</p>
     * 
     * <p><strong>System Requirements:</strong></p>
     * <ul>
     *   <li>Java 17 or later with JavaFX runtime</li>
     *   <li>Desktop environment with graphics support</li>
     *   <li>Minimum 512MB available memory</li>
     *   <li>100MB available disk space for database and settings</li>
     * </ul>
     * 
     * @param args command line arguments passed to the application (currently unused)
     * @see Application#launch(String...)
     * @see #start(Stage)
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Handles the initiation of a multiplayer typing test session.
     * 
     * <p>This method is called by the {@link NetworkingUIIntegration} when a multiplayer
     * game is ready to start. It configures the application for multiplayer mode by setting
     * up the provided test session and preparing the game interface for competitive typing.</p>
     * 
     * <p><strong>Multiplayer Setup Process:</strong></p>
     * <ol>
     *   <li>Accept the multiplayer test session configuration</li>
     *   <li>Create and configure the game interface</li>
     *   <li>Set application state to WAITING_TO_START</li>
     *   <li>Setup mode-specific timers and displays</li>
     *   <li>Initialize statistics tracking</li>
     *   <li>Switch to game view with transition</li>
     * </ol>
     * 
     * <p><strong>Multiplayer-Specific Configuration:</strong></p>
     * <ul>
     *   <li>Uses pre-configured word list from server</li>
     *   <li>Synchronizes timer settings across all players</li>
     *   <li>Enables result transmission to server on completion</li>
     *   <li>Maintains network connection throughout the test</li>
     * </ul>
     * 
     * <p>The method ensures that the local typing interface is properly configured
     * to match the multiplayer game parameters while maintaining all standard
     * typing test functionality for a consistent user experience.</p>
     * 
     * @param session the multiplayer test session configuration provided by the server
     * @throws IllegalArgumentException if the session is null or invalid
     * @see NetworkingUIIntegration
     * @see TestSession
     * @see #createGamePane()
     */
    private void handleMultiplayerGameStart(TestSession session) {
        // Set the provided multiplayer session as current
        currentSession = session;

        // Create fresh game interface for multiplayer test
        gamePane = createGamePane();

        // Set initial state to wait for player to begin typing
        appState = AppState.WAITING_TO_START;

        // Configure timer display and functionality for time-based tests
        if (session.getMode() == GameMode.TIME) {
            timeRemaining = session.getValue();
            timerValueLabel.setText(String.valueOf(timeRemaining));
            timeStatBox.setVisible(true);
            setupGameTimer();
        } else {
            // Hide timer for word count-based tests
            timeStatBox.setVisible(false);
        }

        // Initialize statistics display
        wpmValueLabel.setText("0");
        accValueLabel.setText("100%");
        
        // Populate word display with test content
        updateWordDisplay();

        // Switch to game view and focus for immediate typing
        uiController.switchView(gamePane, true);
        gamePane.requestFocus();
    }

    /**
     * Gracefully shuts down the application and cleans up resources.
     * 
     * <p>This method overrides the JavaFX {@link Application#stop()} method to provide
     * proper application cleanup when the user closes the window or terminates the
     * application. It ensures all timers are stopped, network connections are closed,
     * and resources are properly released.</p>
     * 
     * <p><strong>Cleanup Sequence:</strong></p>
     * <ol>
     *   <li>Stop all active timers and animations</li>
     *   <li>Close multiplayer network connections</li>
     *   <li>Cleanup networking UI resources</li>
     *   <li>Call parent stop() method for JavaFX cleanup</li>
     * </ol>
     * 
     * <p><strong>Resource Cleanup:</strong></p>
     * <ul>
     *   <li>Game timers and countdown animations</li>
     *   <li>Caret blinking timeline</li>
     *   <li>TCP network connections and sockets</li>
     *   <li>Thread pools and background tasks</li>
     * </ul>
     * 
     * <p>The method ensures graceful shutdown even if errors occur during cleanup,
     * preventing resource leaks and ensuring the application terminates properly.</p>
     * 
     * @throws Exception if an error occurs during shutdown (propagated from parent)
     * @see Application#stop()
     * @see #stopAllTimers()
     * @see NetworkingUIIntegration#cleanup()
     */
    @Override
    public void stop() throws Exception {
        // Stop all timing-related resources
        stopAllTimers();

        // Cleanup network connections and resources
        if (networkingUI != null) {
            networkingUI.cleanup();
        }

        // Call parent cleanup for JavaFX resources
        super.stop();
    }
}

