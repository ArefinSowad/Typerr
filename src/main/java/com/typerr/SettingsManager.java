package com.typerr;

import com.typerr.statics.Theme;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

public class SettingsManager {

    private static final String PREF_NODE_NAME = "com.typerr.settings";

    private static SettingsManager instance;

    private static final String THEME_KEY = "theme";

    private static final String DEFAULT_THEME = "LIGHT";

    private final Preferences preferences;

    private final Map<String, Object> cachedSettings;

    private SettingsManager() {
        this.preferences = Preferences.userRoot().node(PREF_NODE_NAME);
        this.cachedSettings = new HashMap<>();
        loadSettings();
    }

    public static synchronized SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }
        return instance;
    }

    private void loadSettings() {
        cachedSettings.put(THEME_KEY, preferences.get(THEME_KEY, DEFAULT_THEME));
    }

    public Theme getTheme() {
        String themeStr = (String) cachedSettings.get(THEME_KEY);
        try {
            return Theme.valueOf(themeStr);
        } catch (IllegalArgumentException e) {
            return Theme.LIGHT;
        }
    }

    public void setTheme(Theme theme) {
        String themeStr = theme.name();
        cachedSettings.put(THEME_KEY, themeStr);
        preferences.put(THEME_KEY, themeStr);
    }
}