package com.typerr.ui;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.util.HashMap;
import java.util.Map;

public class KeyboardShortcutsManager {

    private final Map<KeyCombination, Runnable> shortcuts = new HashMap<>();

    public KeyboardShortcutsManager() {

        registerShortcut(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN), () -> {});
        registerShortcut(new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN), () -> {});
        registerShortcut(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), () -> {});
        registerShortcut(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.CONTROL_DOWN), () -> {});
        registerShortcut(new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN), () -> {});
        registerShortcut(new KeyCodeCombination(KeyCode.F11), () -> {});
    }

    public void registerShortcut(KeyCombination combination, Runnable action) {
        shortcuts.put(combination, action);
    }

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

    public void setRestartAction(Runnable action) {
        registerShortcut(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN), action);
    }

    public void setModeSwitchAction(Runnable action) {
        registerShortcut(new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN), action);
    }

    public void setStatsAction(Runnable action) {
        registerShortcut(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), action);
    }

    public void setSettingsAction(Runnable action) {
        registerShortcut(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.CONTROL_DOWN), action);
    }

    public void setFullscreenAction(Runnable action) {
        registerShortcut(new KeyCodeCombination(KeyCode.F11), action);
    }

    public void setMultiplayerAction(Runnable action) {
        registerShortcut(new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN), action);
    }

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