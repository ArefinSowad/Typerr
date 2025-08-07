package com.typerr.statics;

import com.typerr.SettingsManager;

/**
 * Enumeration representing the available visual themes for the Typerr application.
 * 
 * <p>This enum defines the supported UI themes that control the overall appearance
 * and color scheme of the typing test application. The theme selection affects all
 * visual components including backgrounds, text colors, button styles, and accent colors.</p>
 * 
 * <p>The theme system provides:</p>
 * <ul>
 *   <li><strong>Consistent Styling:</strong> Unified appearance across all UI components</li>
 *   <li><strong>User Preference:</strong> Persistent theme selection through {@link SettingsManager}</li>
 *   <li><strong>Accessibility:</strong> Light and dark modes for different lighting conditions</li>
 *   <li><strong>CSS Integration:</strong> Automatic stylesheet switching for JavaFX components</li>
 * </ul>
 * 
 * <p>Theme switching is handled by the {@link SettingsManager} which persists user
 * preferences and the UI layer which applies the appropriate CSS stylesheets.</p>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Get current theme
 * Theme currentTheme = SettingsManager.getInstance().getTheme();
 * 
 * // Switch to dark theme
 * SettingsManager.getInstance().setTheme(Theme.DARK);
 * 
 * // Apply theme to scene
 * if (theme == Theme.DARK) {
 *     scene.getStylesheets().add("dark-styles.css");
 * } else {
 *     scene.getStylesheets().add("styles.css");
 * }
 * }</pre>
 * 
 * @author ArefinSowad
 * @version 1.0
 * @since 1.0
 * @see SettingsManager#getTheme()
 * @see SettingsManager#setTheme(Theme)
 */
public enum Theme {
    
    /**
     * Light theme with bright backgrounds and dark text.
     * 
     * <p>The light theme provides a traditional, bright interface suitable for
     * well-lit environments and users who prefer high contrast between text
     * and background. This theme uses the main stylesheet (styles.css) and
     * features:</p>
     * <ul>
     *   <li>White and light gray backgrounds</li>
     *   <li>Dark text for optimal readability</li>
     *   <li>Blue accent colors for highlights and buttons</li>
     *   <li>Green indicators for correct text and success states</li>
     *   <li>Red indicators for errors and incorrect text</li>
     * </ul>
     */
    LIGHT,
    
    /**
     * Dark theme with dark backgrounds and light text.
     * 
     * <p>The dark theme provides a modern, low-light interface ideal for
     * dimly lit environments and users who prefer reduced eye strain during
     * extended typing sessions. This theme uses the dark stylesheet (dark-styles.css)
     * and features:</p>
     * <ul>
     *   <li>Dark gray and black backgrounds</li>
     *   <li>Light text for comfortable reading</li>
     *   <li>Yellow/gold accent colors for visibility</li>
     *   <li>Muted color indicators that are easier on the eyes</li>
     *   <li>Reduced brightness overall for extended use</li>
     * </ul>
     */
    DARK
}
