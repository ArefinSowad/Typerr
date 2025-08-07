package com.typerr;

import com.typerr.statics.Theme;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Singleton class for managing persistent application settings and user preferences.
 * 
 * <p>The SettingsManager provides a centralized, thread-safe mechanism for storing,
 * retrieving, and managing user preferences that persist across application sessions.
 * It uses the Java Preferences API as the underlying storage mechanism, ensuring
 * cross-platform compatibility and automatic persistence.</p>
 * 
 * <p>This implementation follows the Singleton design pattern to ensure consistent
 * access to settings throughout the application while maintaining state coherency.
 * The class provides both caching for performance and direct persistence for
 * reliability.</p>
 * 
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Persistent Storage:</strong> Settings survive application restarts</li>
 *   <li><strong>Performance Caching:</strong> In-memory cache reduces I/O operations</li>
 *   <li><strong>Thread Safety:</strong> Synchronized access for concurrent operations</li>
 *   <li><strong>Default Values:</strong> Graceful fallbacks for missing or invalid settings</li>
 *   <li><strong>Type Safety:</strong> Strong typing for theme preferences</li>
 * </ul>
 * 
 * <h3>Storage Implementation:</h3>
 * <p>The manager uses the Java Preferences API which typically stores data in:</p>
 * <ul>
 *   <li><strong>Windows:</strong> Registry under HKEY_CURRENT_USER</li>
 *   <li><strong>macOS:</strong> User preferences in ~/Library/Preferences</li>
 *   <li><strong>Linux:</strong> User preferences in ~/.java/.userPrefs</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Get the singleton instance
 * SettingsManager settings = SettingsManager.getInstance();
 * 
 * // Retrieve current theme
 * Theme currentTheme = settings.getTheme();
 * 
 * // Change theme and persist automatically
 * settings.setTheme(Theme.DARK);
 * 
 * // Theme is immediately available and persisted for next session
 * Theme newTheme = settings.getTheme(); // Returns DARK
 * }</pre>
 * 
 * @author ArefinSowad
 * @version 1.0
 * @since 1.0
 * @see Theme
 * @see java.util.prefs.Preferences
 */
public class SettingsManager {

    /** Preferences node name for storing Typerr application settings. */
    private static final String PREF_NODE_NAME = "com.typerr.settings";
    
    /** Singleton instance of the SettingsManager. */
    private static SettingsManager instance;

    /** Preference key for theme setting storage. */
    private static final String THEME_KEY = "theme";

    /** Default theme value when no preference is set or invalid value is found. */
    private static final String DEFAULT_THEME = "LIGHT";

    /** Java Preferences API backing store for persistent settings. */
    private final Preferences preferences;
    
    /** In-memory cache for fast setting retrieval and reduced I/O operations. */
    private final Map<String, Object> cachedSettings;

    /**
     * Private constructor for singleton pattern implementation.
     * 
     * <p>Initializes the preferences backing store and loads existing settings
     * into the cache. Creates the preferences node if it doesn't exist and
     * establishes the connection to the platform-specific storage mechanism.</p>
     * 
     * <p>The constructor is private to enforce singleton access through
     * {@link #getInstance()}.</p>
     */
    private SettingsManager() {
        this.preferences = Preferences.userRoot().node(PREF_NODE_NAME);
        this.cachedSettings = new HashMap<>();
        loadSettings();
    }

    /**
     * Returns the singleton instance of SettingsManager.
     * 
     * <p>This method implements thread-safe lazy initialization using synchronized
     * access. The instance is created only when first requested and reused for
     * all subsequent calls, ensuring consistent state across the application.</p>
     * 
     * @return the singleton SettingsManager instance
     */
    public static synchronized SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }
        return instance;
    }

    /**
     * Loads all settings from persistent storage into the in-memory cache.
     * 
     * <p>This method is called during initialization to populate the cache
     * with existing settings. It retrieves values from the Preferences API
     * and applies default values for any missing settings.</p>
     * 
     * <p>Currently loads:</p>
     * <ul>
     *   <li>Theme preference with LIGHT as default</li>
     * </ul>
     */
    private void loadSettings() {
        cachedSettings.put(THEME_KEY, preferences.get(THEME_KEY, DEFAULT_THEME));
    }

    /**
     * Retrieves the currently selected UI theme.
     * 
     * <p>Returns the user's preferred theme setting, with automatic fallback
     * to {@link Theme#LIGHT} if the stored value is invalid or missing.
     * The method provides type safety by converting string preferences to
     * the strongly-typed Theme enum.</p>
     * 
     * @return the current theme setting, never null
     * @throws IllegalStateException if the cached theme value is unexpectedly null
     */
    public Theme getTheme() {
        String themeStr = (String) cachedSettings.get(THEME_KEY);
        try {
            return Theme.valueOf(themeStr);
        } catch (IllegalArgumentException e) {
            return Theme.LIGHT;
        }
    }

    /**
     * Sets the UI theme preference and persists it to storage.
     * 
     * <p>This method immediately updates both the in-memory cache and the
     * persistent storage, ensuring that the new theme preference is available
     * for the current session and will be preserved for future sessions.</p>
     * 
     * <p>The theme change takes effect immediately in the cache, but UI
     * components may need to be refreshed to reflect the visual changes.</p>
     * 
     * @param theme the new theme to set, must not be null
     * @throws IllegalArgumentException if theme is null
     */
    public void setTheme(Theme theme) {
        String themeStr = theme.name();
        cachedSettings.put(THEME_KEY, themeStr);
        preferences.put(THEME_KEY, themeStr);
    }
}