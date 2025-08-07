package com.typerr.ui;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages keyboard shortcuts and hotkey functionality for the Typerr application.
 * 
 * <p>This class provides a centralized system for registering, handling, and managing
 * keyboard shortcuts throughout the application. It supports common application actions
 * such as restarting tests, switching modes, accessing settings, and toggling UI states.</p>
 * 
 * <p>The manager uses JavaFX's {@link KeyCombination} system to detect key combinations
 * and execute associated actions. It maintains a registry of shortcuts that can be
 * dynamically updated at runtime.</p>
 * 
 * <p>Default shortcuts registered include:</p>
 * <ul>
 *   <li><strong>Ctrl+R</strong> - Restart current typing test</li>
 *   <li><strong>Ctrl+M</strong> - Switch between test modes (time/words)</li>
 *   <li><strong>Ctrl+S</strong> - Toggle statistics and charts view</li>
 *   <li><strong>Ctrl+P</strong> - Toggle multiplayer mode</li>
 *   <li><strong>Ctrl+,</strong> - Open application settings</li>
 *   <li><strong>F11</strong> - Toggle fullscreen mode</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * KeyboardShortcutsManager shortcuts = new KeyboardShortcutsManager();
 * shortcuts.setRestartAction(() -> restartCurrentTest());
 * shortcuts.setSettingsAction(() -> openSettingsDialog());
 * 
 * // In your key event handler:
 * if (shortcuts.handleKeyEvent(event)) {
 *     return; // Shortcut was handled
 * }
 * }</pre>
 * 
 * @author ArefinSowad
 * @version 1.0
 * @since 1.0
 * @see KeyCombination
 * @see KeyEvent
 */
public class KeyboardShortcutsManager {

    /** 
     * Registry mapping key combinations to their associated actions.
     * Thread-safe access is not guaranteed; external synchronization required for concurrent use.
     */
    private final Map<KeyCombination, Runnable> shortcuts = new HashMap<>();

    /**
     * Creates a new keyboard shortcuts manager with default shortcuts registered.
     * 
     * <p>The constructor initializes the manager with placeholder actions for all
     * standard application shortcuts. These should be replaced with actual
     * functionality using the appropriate setter methods.</p>
     * 
     * <p>Default shortcuts registered (with empty actions):</p>
     * <ul>
     *   <li>Ctrl+R - Restart action</li>
     *   <li>Ctrl+M - Mode switch action</li>
     *   <li>Ctrl+S - Statistics toggle action</li>
     *   <li>Ctrl+, - Settings action</li>
     *   <li>Ctrl+P - Multiplayer action</li>
     *   <li>F11 - Fullscreen toggle action</li>
     * </ul>
     */
    public KeyboardShortcutsManager() {
        // Register default shortcuts with empty actions (to be set later)
        registerShortcut(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN), () -> {});
        registerShortcut(new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN), () -> {});
        registerShortcut(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), () -> {});
        registerShortcut(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.CONTROL_DOWN), () -> {});
        registerShortcut(new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN), () -> {});
        registerShortcut(new KeyCodeCombination(KeyCode.F11), () -> {});
    }

    /**
     * Registers a keyboard shortcut with its associated action.
     * 
     * <p>If a shortcut with the same key combination already exists, it will be
     * replaced with the new action. This allows for dynamic reconfiguration
     * of shortcuts at runtime.</p>
     * 
     * @param combination the key combination that triggers the action
     * @param action the action to execute when the key combination is detected
     * @throws IllegalArgumentException if combination or action is null
     */
    public void registerShortcut(KeyCombination combination, Runnable action) {
        shortcuts.put(combination, action);
    }

    /**
     * Handles a key event and executes the associated action if a matching shortcut is found.
     * 
     * <p>This method should be called from key event handlers throughout the application.
     * If a matching shortcut is found, the event is consumed to prevent further processing.</p>
     * 
     * <p>The method iterates through all registered shortcuts and checks for matches
     * using the {@link KeyCombination#match(KeyEvent)} method.</p>
     * 
     * @param event the key event to process
     * @return true if a matching shortcut was found and executed, false otherwise
     * @throws IllegalArgumentException if event is null
     */
    public boolean handleKeyEvent(KeyEvent event) {
        for (Map.Entry<KeyCombination, Runnable> entry : shortcuts.entrySet()) {
            if (entry.getKey().match(event)) {
                entry.getValue().run();
                event.consume();
                return true;
            }
        }
        return false;
    }

    // ========== SHORTCUT SETTER METHODS ==========

    /**
     * Sets the action for the restart shortcut (Ctrl+R).
     * 
     * <p>This action is typically used to restart the current typing test,
     * resetting the timer, word list, and progress indicators.</p>
     * 
     * @param action the action to execute when Ctrl+R is pressed
     */
    public void setRestartAction(Runnable action) {
        registerShortcut(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN), action);
    }

    /**
     * Sets the action for the mode switch shortcut (Ctrl+M).
     * 
     * <p>This action is typically used to cycle through available test modes
     * such as time-based tests, word count tests, or custom challenges.</p>
     * 
     * @param action the action to execute when Ctrl+M is pressed
     */
    public void setModeSwitchAction(Runnable action) {
        registerShortcut(new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN), action);
    }

    /**
     * Sets the action for the statistics toggle shortcut (Ctrl+S).
     * 
     * <p>This action is typically used to show or hide the statistics panel,
     * charts, and performance analysis views.</p>
     * 
     * @param action the action to execute when Ctrl+S is pressed
     */
    public void setStatsAction(Runnable action) {
        registerShortcut(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), action);
    }

    /**
     * Sets the action for the settings shortcut (Ctrl+,).
     * 
     * <p>This action is typically used to open the application settings dialog
     * where users can configure themes, preferences, and other options.</p>
     * 
     * @param action the action to execute when Ctrl+, is pressed
     */
    public void setSettingsAction(Runnable action) {
        registerShortcut(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.CONTROL_DOWN), action);
    }

    /**
     * Sets the action for the fullscreen toggle shortcut (F11).
     * 
     * <p>This action is typically used to toggle between windowed and fullscreen
     * modes for distraction-free typing practice.</p>
     * 
     * @param action the action to execute when F11 is pressed
     */
    public void setFullscreenAction(Runnable action) {
        registerShortcut(new KeyCodeCombination(KeyCode.F11), action);
    }

    /**
     * Sets the action for the multiplayer toggle shortcut (Ctrl+P).
     * 
     * <p>This action is typically used to toggle multiplayer mode, open
     * networking dialogs, or switch between single and multiplayer interfaces.</p>
     * 
     * @param action the action to execute when Ctrl+P is pressed
     */
    public void setMultiplayerAction(Runnable action) {
        registerShortcut(new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN), action);
    }

    /**
     * Returns a formatted description of all available keyboard shortcuts.
     * 
     * <p>This method provides a user-friendly reference for all keyboard shortcuts
     * available in the application. The returned string is suitable for display
     * in help dialogs, tooltips, or documentation.</p>
     * 
     * <p>The description includes both registered shortcuts managed by this class
     * and other common application shortcuts that may be handled elsewhere.</p>
     * 
     * @return a multi-line string describing all available keyboard shortcuts
     */
    public String getShortcutsDescription() {
        return """
            Keyboard Shortcuts:
            • Ctrl+R - Restart current test
            • Ctrl+M - Switch between test modes
            • Ctrl+S - Toggle statistics view
            • Ctrl+P - Toggle multiplayer mode
            • Ctrl+, - Open settings
            • F11 - Toggle fullscreen mode
            • Esc - Return to main menu
            • Tab - Restart test
            • Space/Enter - Navigate through results
            """;
    }
}