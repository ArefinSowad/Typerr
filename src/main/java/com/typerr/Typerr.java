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

public class Typerr extends Application {

    private static Font WORD_FONT = Font.font(Constants.WORD_FONT_FAMILY, FontWeight.NORMAL, 28);

    private Theme currentTheme = Theme.LIGHT;

    private enum AppState {

        MENU,

        WAITING_TO_START,

        RUNNING,

        FINISHED,

        STATISTICS,

        SETTINGS,

        MULTIPLAYER
    }

    private AppState appState = AppState.MENU;

    private StackPane rootPane;

    private VBox gamePane, resultsPane, menuPane;

    private VBox timeStatBox;

    private Label timerValueLabel, wpmValueLabel, accValueLabel;

    private TextFlow wordDisplayArea;

    private UIController uiController;

    private KeyboardShortcutsManager shortcutsManager;

    private Stage primaryStage;

    private NetworkingUIIntegration networkingUI;

    private Timeline gameTimer, caretTimeline;

    private int timeRemaining;

    private final WordProvider wordProvider = new WordProvider();

    private TestSession currentSession;

    private SettingsManager settingsManager = SettingsManager.getInstance();

    private Scene scene;

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

    private void setupKeyboardShortcuts() {
        shortcutsManager.setRestartAction(this::handleRestartShortcut);
        shortcutsManager.setModeSwitchAction(this::handleModeSwitchShortcut);
        shortcutsManager.setStatsAction(this::handleStatsShortcut);
        shortcutsManager.setSettingsAction(this::handleSettingsShortcut);
        shortcutsManager.setFullscreenAction(this::handleFullscreenShortcut);
        shortcutsManager.setMultiplayerAction(this::handleMultiplayerShortcut);
    }

    private void handleRestartShortcut() {
        if (appState == AppState.RUNNING || appState == AppState.WAITING_TO_START || appState == AppState.FINISHED) {
            restartCurrentTest();
        }
    }

    private void handleModeSwitchShortcut() {
        if (appState == AppState.MENU) {
            return;
        }

        stopAllTimers();
        appState = AppState.MENU;
        uiController.switchView(menuPane);
    }

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

    private void handleFullscreenShortcut() {
        primaryStage.setFullScreen(!primaryStage.isFullScreen());
    }

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

    private void handleKeyPress(KeyEvent event) {

        if (shortcutsManager.handleKeyEvent(event)) {
            return;
        }

        if (event.getCode() == KeyCode.ESCAPE) {
            if (appState != AppState.MENU) {
                stopAllTimers();
                appState = AppState.MENU;
                uiController.switchView(menuPane);
                return;
            }
        }

        switch (appState) {
            case MENU -> {

                if (event.getCode() == KeyCode.TAB) {
                    event.consume();
                }
            }
            case STATISTICS, SETTINGS, MULTIPLAYER -> {

            }
            case WAITING_TO_START -> {

                if (event.getCode() == KeyCode.TAB) {
                    restartCurrentTest();
                    event.consume();
                    return;
                }

                if (event.getCode().isLetterKey()) {
                    startGame();
                    processKeystroke(event);
                }
            }
            case RUNNING -> {

                if (event.getCode() == KeyCode.TAB) {
                    restartCurrentTest();
                    event.consume();
                    return;
                }

                processKeystroke(event);
            }
            case FINISHED -> {

                if (event.getCode() == KeyCode.TAB) {
                    restartCurrentTest();
                    event.consume();
                    return;
                }

                if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.ESCAPE) {
                    appState = AppState.MENU;
                    uiController.switchView(menuPane);
                }
            }
        }
    }

    private void restartCurrentTest() {
        if (currentSession != null) {

            stopAllTimers();

            startTest(currentSession.getMode(), currentSession.getValue());
        }
    }

    private void startGame() {

        appState = AppState.RUNNING;

        currentSession.start();

        if (currentSession.getMode() == GameMode.TIME && gameTimer != null) {
            gameTimer.play();
        }
    }

    private void processKeystroke(KeyEvent event) {

        TestSession.Word currentWord = currentSession.getCurrentWord();
        String typed = currentWord.getTypedWord();

        if (event.getCode() == KeyCode.SPACE) {

            if (typed.length() > 0) {

                boolean isCorrect = typed.equals(currentWord.getTargetWord());

                currentSession.advanceToNextWord();

                if (currentSession.isFinished()) {
                    endGame();
                }
            }

            event.consume();
        } else if (event.getCode() == KeyCode.BACK_SPACE) {

            if (typed.length() > 0) {
                currentWord.setTypedWord(typed.substring(0, typed.length() - 1));
            }
        } else if (event.getCode().isLetterKey() && event.getText() != null && !event.getText().isEmpty()) {

            String character = event.getText();

            if (!event.isControlDown() && !event.isAltDown()) {
                currentWord.setTypedWord(typed + character);
            }
        }

        updateWordDisplay();
    }

    private void endGame() {

        appState = AppState.FINISHED;

        stopAllTimers();

        if (!currentSession.getCurrentWord().isCompleted()) {
            currentSession.getCurrentWord().complete();
        }

        currentSession.end();

        if (networkingUI != null && networkingUI.isConnectedAsClient()) {
            TestSession.TestResult result = currentSession.calculateCurrentStats(true);
            double completionTime = (System.currentTimeMillis() - currentSession.getStartTime()) / 1000.0;
            networkingUI.sendRoundStats(result, completionTime, currentSession.getMode(), currentSession.getValue());
        }

        showResults();
    }

    private void stopAllTimers() {

        if (gameTimer != null) {
            gameTimer.stop();
            gameTimer = null;
        }

        if (caretTimeline != null) {
            caretTimeline.stop();
        }
    }

    private void updateWordDisplay() {

        if (wordDisplayArea == null || currentSession == null) return;

        wordDisplayArea.getChildren().clear();

        int currentIndex = currentSession.getCurrentWordIndex();
        List<TestSession.Word> words = currentSession.getWordList();

        int renderCutoff = Math.min(words.size(), currentIndex + Constants.MAX_WORDS_RENDERED);

        for (int i = 0; i < renderCutoff; i++) {

            wordDisplayArea.getChildren().add(createWordNode(words.get(i), i == currentIndex));

            wordDisplayArea.getChildren().add(new Text("  "));
        }

        updateStats();
    }

    private Node createWordNode(TestSession.Word word, boolean isCurrent) {

        TextFlow wordFlow = new TextFlow();
        String target = word.getTargetWord();
        String typed = word.getTypedWord();

        for (int i = 0; i < target.length(); i++) {

            Text charText = new Text(String.valueOf(target.charAt(i)));
            charText.setFont(WORD_FONT);

            if (i < typed.length()) {

                boolean isCorrect = Character.toLowerCase(typed.charAt(i)) == Character.toLowerCase(target.charAt(i));
                charText.getStyleClass().add(isCorrect ? "text-correct" : "text-incorrect");
            } else if (isCurrent && i == typed.length()) {

                charText.getStyleClass().add("caret");
            } else {

                charText.getStyleClass().add("text-default");
            }

            wordFlow.getChildren().add(charText);
        }

        if (typed.length() > target.length()) {

            Text extra = new Text(typed.substring(target.length()));
            extra.setFont(WORD_FONT);
            extra.getStyleClass().addAll("text-incorrect", "text-extra");
            wordFlow.getChildren().add(extra);
        }

        if (isCurrent && typed.length() >= target.length()) {

            Text spaceCaret = new Text(" ");
            spaceCaret.setFont(WORD_FONT);
            spaceCaret.getStyleClass().add("caret");
            wordFlow.getChildren().add(spaceCaret);
        }

        return wordFlow;
    }

    private void updateStats() {

        if (appState != AppState.RUNNING || currentSession.getStartTime() == 0) return;

        if (currentSession.getMode() == GameMode.TIME) {
            timerValueLabel.setText(String.valueOf(timeRemaining));
        }

        TestSession.TestResult partialResult = currentSession.calculateCurrentStats(false);

        wpmValueLabel.setText(String.valueOf(partialResult.netWPM()));
        accValueLabel.setText(partialResult.accuracy() + "%");
    }

    private void showResults() {

        TestSession.TestResult result = currentSession.calculateCurrentStats(true);

        resultsPane.getChildren().clear();

        String testInfoText = String.format("test: %s / %d / %s",
                currentSession.getMode().name().toLowerCase(),
                currentSession.getValue(),
                wordProvider.getCurrentDifficulty().name().toLowerCase());
        Label testInfoLabel = createResultLabel(testInfoText, "results-info");

        Label wpmLabel = new Label("wpm");
        wpmLabel.getStyleClass().add("results-label-title");
        Label wpmValue = new Label(String.valueOf(result.netWPM()));
        wpmValue.getStyleClass().add("results-label-value");
        VBox wpmBox = new VBox(wpmLabel, wpmValue);
        wpmBox.setAlignment(Pos.CENTER);

        Label accLabel = new Label("acc");
        accLabel.getStyleClass().add("results-label-title");
        Label accValue = new Label(result.accuracy() + "%");
        accValue.getStyleClass().add("results-label-value");
        VBox accBox = new VBox(accLabel, accValue);
        accBox.setAlignment(Pos.CENTER);

        HBox mainStats = new HBox(40, wpmBox, accBox);
        mainStats.setAlignment(Pos.CENTER);

        String secondaryStatsText = String.format("raw: %d | chars: %d/%d/%d",
                result.rawWPM(),
                result.correctChars(),
                result.incorrectChars(),
                (result.correctChars() + result.incorrectChars()));
        Label secondaryStatsLabel = createResultLabel(secondaryStatsText, "results-info");

        Button backButton = new Button("↩");
        backButton.getStyleClass().addAll("nav-button", "back-button");
        backButton.setOnAction(e -> {

            appState = AppState.MENU;
            uiController.switchView(menuPane);
        });

        Button restartButton = new Button("⟳");
        restartButton.getStyleClass().addAll("nav-button", "restart-button");
        restartButton.setOnAction(e -> restartCurrentTest());

        HBox buttonRow = new HBox(10, backButton, restartButton);
        buttonRow.setAlignment(Pos.CENTER);

        resultsPane.getChildren().addAll(
                testInfoLabel,
                mainStats,
                secondaryStatsLabel,
                buttonRow
        );

        uiController.switchView(resultsPane);
    }

    private void toggleTheme() {

        Theme newTheme = (currentTheme == Theme.DARK) ? Theme.LIGHT : Theme.DARK;
        currentTheme = newTheme;

        settingsManager.setTheme(newTheme);

        applyTheme(newTheme);
    }

    private void applyTheme(Theme theme) {

        scene.getStylesheets().clear();

        rootPane.getStyleClass().removeAll("light-theme", "dark-theme");

        if (theme == Theme.DARK) {

            String darkStylesheet = ResourceLoader.loadStylesheet("/CSS Styles/dark-styles.css");
            if (darkStylesheet != null) {
                scene.getStylesheets().add(darkStylesheet);
            }

            rootPane.getStyleClass().add("dark-theme");
        } else {

            String lightStylesheet = ResourceLoader.loadStylesheet("/CSS Styles/styles.css");
            if (lightStylesheet != null) {
                scene.getStylesheets().add(lightStylesheet);
            }

            rootPane.getStyleClass().add("light-theme");
        }
    }

    private VBox createMenuView() {

        Label title = new Label("Typerr");
        title.getStyleClass().add("menu-title");

        HBox timeModes = new HBox(
                createModeButton("15 Seconds", GameMode.TIME, 15),
                createModeButton("30 Seconds", GameMode.TIME, 30),
                createModeButton("60 Seconds", GameMode.TIME, 60)
        );
        timeModes.getStyleClass().add("menu-group");

        HBox wordModes = new HBox(
                createModeButton("10 Words", GameMode.WORDS, 10),
                createModeButton("25 Words", GameMode.WORDS, 25),
                createModeButton("50 Words", GameMode.WORDS, 50)
        );
        wordModes.getStyleClass().add("menu-group");

        HBox difficultyBox = new HBox(10);
        difficultyBox.setAlignment(Pos.CENTER);

        for (WordProvider.Difficulty difficulty : WordProvider.Difficulty.values()) {
            Button diffButton = new Button(difficulty.name());
            diffButton.getStyleClass().add("mode-button");

            if (difficulty == wordProvider.getCurrentDifficulty()) {
                diffButton.getStyleClass().add("selected-difficulty-button");
            }

            diffButton.setOnAction(e -> {
                wordProvider.setDifficulty(difficulty);
                updateDifficultyButtons(difficultyBox, difficulty);
            });
            difficultyBox.getChildren().add(diffButton);
        }

        HBox navButtons = new HBox(10);
        navButtons.setAlignment(Pos.CENTER);

        Button multiplayerButton = new Button("🌐");
        multiplayerButton.getStyleClass().add("nav-button");
        multiplayerButton.setOnAction(e -> handleMultiplayerShortcut());

        Button statsButton = new Button("📊");
        statsButton.getStyleClass().add("nav-button");
        statsButton.setOnAction(e -> handleStatsShortcut());

        Button settingsButton = new Button("⚙");
        settingsButton.getStyleClass().add("nav-button");
        settingsButton.setOnAction(e -> handleSettingsShortcut());

        navButtons.getChildren().addAll(multiplayerButton, statsButton, settingsButton);

        VBox statsBox = new VBox(5);
        statsBox.setAlignment(Pos.CENTER);

        TestSession.TestResult bestResult = TestSession.StatHistory.getBestResult();
        if (bestResult != null) {
            Label bestWpmLabel = new Label("Best WPM: " + bestResult.netWPM());
            bestWpmLabel.getStyleClass().add("stat-title");
            statsBox.getChildren().add(bestWpmLabel);
        }

        int totalTests = TestSession.StatHistory.getTotalTestCount();
        if (totalTests > 0) {
            Label totalLabel = new Label("Tests Completed: " + totalTests);
            totalLabel.getStyleClass().add("stat-title");
            statsBox.getChildren().add(totalLabel);
        }

        VBox menu = new VBox(20,
                title,
                new Label("Select Mode:"),
                timeModes,
                wordModes,
                new Label("Select Difficulty:"),
                difficultyBox,
                navButtons,
                statsBox);
        menu.getStyleClass().add("menu-pane");
        return menu;
    }

    private void updateDifficultyButtons(HBox difficultyBox, WordProvider.Difficulty selectedDifficulty) {

        for (Node node : difficultyBox.getChildren()) {
            if (node instanceof Button button) {

                button.getStyleClass().remove("selected-difficulty-button");

                if (button.getText().equals(selectedDifficulty.name())) {
                    button.getStyleClass().add("selected-difficulty-button");
                }
            }
        }
    }

    private Button createModeButton(String text, GameMode mode, int value) {
        Button b = new Button(text);
        b.getStyleClass().add("mode-button");

        b.setOnAction(e -> startTest(mode, value));
        return b;
    }

    private VBox createGamePane() {

        VBox pane = new VBox(40);
        pane.setPadding(new Insets(50));
        pane.setAlignment(Pos.CENTER);

        HBox statsBar = createStatsBar();

        wordDisplayArea = new TextFlow();
        VBox wordContainer = new VBox(wordDisplayArea);
        wordContainer.getStyleClass().add("word-container");

        pane.getChildren().addAll(statsBar, wordContainer);
        return pane;
    }

    private HBox createStatsBar() {

        wpmValueLabel = new Label("0");
        accValueLabel = new Label("100%");
        timerValueLabel = new Label("0");

        VBox wpmBox = createStatBox("WPM", wpmValueLabel);
        VBox accBox = createStatBox("ACCURACY", accValueLabel);
        timeStatBox = createStatBox("TIME", timerValueLabel);

        HBox statsBar = new HBox(wpmBox, accBox, timeStatBox);
        statsBar.getStyleClass().add("stats-bar");
        return statsBar;
    }

    private VBox createStatBox(String title, Label valueLabel) {

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");

        valueLabel.getStyleClass().add("stat-value");

        VBox statBox = new VBox(titleLabel, valueLabel);
        statBox.getStyleClass().add("stat-box");
        return statBox;
    }

    private VBox createResultsPane() {
        VBox pane = new VBox();
        pane.getStyleClass().add("results-pane");
        return pane;
    }

    private Label createResultLabel(String text, String styleClass) {
        Label l = new Label(text);
        l.getStyleClass().add(styleClass);
        return l;
    }

    private void setupGameTimer() {

        if (gameTimer != null) gameTimer.stop();

        if (currentSession.getMode() == GameMode.TIME) {

            gameTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {

                timeRemaining--;

                updateStats();

                if (timeRemaining <= 0) {
                    endGame();
                }
            }));

            gameTimer.setCycleCount(currentSession.getValue());
        }
    }

    private void setupCaretTimeline() {

        if (caretTimeline != null) {
            caretTimeline.stop();
        }

        caretTimeline = new Timeline(new KeyFrame(Duration.millis(Constants.CARET_BLINK_INTERVAL), e -> {

            if (wordDisplayArea != null && (appState == AppState.WAITING_TO_START || appState == AppState.RUNNING)) {

                wordDisplayArea.getChildren().stream()
                        .filter(node -> node.getStyleClass().contains("caret"))
                        .forEach(node -> {

                            if (node.getParent() != null) {
                                node.setVisible(!node.isVisible());
                            }
                        });
            }
        }));

        caretTimeline.setCycleCount(Timeline.INDEFINITE);
        caretTimeline.play();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void handleMultiplayerGameStart(TestSession session) {

        currentSession = session;

        gamePane = createGamePane();

        appState = AppState.WAITING_TO_START;

        if (session.getMode() == GameMode.TIME) {
            timeRemaining = session.getValue();
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

    @Override
    public void stop() throws Exception {

        stopAllTimers();

        if (networkingUI != null) {
            networkingUI.cleanup();
        }

        super.stop();
    }
}

