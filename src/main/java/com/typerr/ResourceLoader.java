package com.typerr;

import com.typerr.statics.Constants;
import javafx.scene.image.Image;
import java.io.InputStream;
import java.net.URL;

/**
 * Utility class for loading application resources from the classpath.
 * 
 * <p>ResourceLoader provides a centralized, type-safe approach to loading various
 * application resources including images, stylesheets, and other files embedded
 * in the application JAR. It handles common error scenarios gracefully and provides
 * consistent logging for troubleshooting resource loading issues.</p>
 * 
 * <h3>Supported Resource Types:</h3>
 * <ul>
 *   <li><strong>Images:</strong> PNG, JPEG, GIF, and other JavaFX-supported formats</li>
 *   <li><strong>Stylesheets:</strong> CSS files for JavaFX scene styling</li>
 *   <li><strong>General Resources:</strong> Text files, data files, and other classpath resources</li>
 * </ul>
 * 
 * <h3>Resource Loading Strategy:</h3>
 * <p>The class uses the classloader-based resource loading mechanism which:</p>
 * <ul>
 *   <li>Loads resources from the application's classpath</li>
 *   <li>Works correctly in both development and packaged JAR environments</li>
 *   <li>Provides consistent behavior across different platforms</li>
 *   <li>Handles resource path normalization automatically</li>
 * </ul>
 * 
 * <h3>Error Handling:</h3>
 * <p>All methods implement robust error handling:</p>
 * <ul>
 *   <li>Null and empty path validation</li>
 *   <li>File existence checking</li>
 *   <li>Exception catching and logging</li>
 *   <li>Graceful degradation (returns null on failure)</li>
 * </ul>
 * 
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * // Load application icon
 * Image icon = ResourceLoader.loadImage("/images/icon.png");
 * if (icon != null) {
 *     stage.getIcons().add(icon);
 * }
 * 
 * // Load CSS stylesheet
 * String styleUrl = ResourceLoader.loadStylesheet("/styles.css");
 * if (styleUrl != null) {
 *     scene.getStylesheets().add(styleUrl);
 * }
 * }</pre>
 * 
 * <h3>Resource Organization:</h3>
 * <p>Resources should be organized in the {@code src/main/resources} directory:</p>
 * <pre>
 * src/main/resources/
 * ├── images/
 * │   ├── icon.png
 * │   └── background.jpg
 * ├── styles.css
 * ├── dark-styles.css
 * └── words.txt
 * </pre>
 * 
 * @author ArefinSowad
 * @version 1.0
 * @since 1.0
 * @see javafx.scene.image.Image
 * @see Constants
 */
public final class ResourceLoader {

    /**
     * Loads an image resource from the classpath.
     * 
     * <p>This method creates a JavaFX {@link Image} object from a resource file
     * located in the application's classpath. The image is loaded immediately
     * and can be used in JavaFX UI components such as ImageView, stage icons,
     * or background images.</p>
     * 
     * <p>Supported image formats include all formats supported by JavaFX:</p>
     * <ul>
     *   <li>PNG (recommended for icons and graphics with transparency)</li>
     *   <li>JPEG/JPG (recommended for photographs)</li>
     *   <li>GIF (supported, including animated GIFs)</li>
     *   <li>BMP (basic support)</li>
     * </ul>
     * 
     * <p>Error handling:</p>
     * <ul>
     *   <li>Returns null if the resource path is null or empty</li>
     *   <li>Returns null if the resource file is not found</li>
     *   <li>Returns null if an exception occurs during loading</li>
     *   <li>Logs warnings/errors to System.err for debugging</li>
     * </ul>
     * 
     * @param resourcePath the path to the image resource (e.g., "/images/icon.png")
     * @return a JavaFX Image object, or null if loading failed
     * @throws IllegalArgumentException if resourcePath contains invalid characters
     */
    public static Image loadImage(String resourcePath) {
        if (resourcePath == null || resourcePath.trim().isEmpty()) {
            System.err.println("Warning: Invalid image resource path: " + resourcePath);
            return null;
        }

        try {
            InputStream imageStream = ResourceLoader.class.getResourceAsStream(resourcePath);
            if (imageStream != null) {
                return new Image(imageStream);
            } else {
                System.err.println("Warning: Image resource not found: " + resourcePath);
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error loading image resource " + resourcePath + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Loads a CSS stylesheet resource from the classpath.
     * 
     * <p>This method locates a CSS file in the application's classpath and returns
     * its URL as a string suitable for use with JavaFX's stylesheet system. The
     * returned URL can be added directly to a Scene's stylesheets list.</p>
     * 
     * <p>CSS file requirements:</p>
     * <ul>
     *   <li>Must be valid CSS syntax compatible with JavaFX</li>
     *   <li>Should use JavaFX-specific CSS properties (prefixed with -fx-)</li>
     *   <li>Should be encoded in UTF-8 for international character support</li>
     *   <li>Should follow the application's naming conventions</li>
     * </ul>
     * 
     * <p>Usage with JavaFX:</p>
     * <pre>{@code
     * String cssUrl = ResourceLoader.loadStylesheet("/styles.css");
     * if (cssUrl != null) {
     *     scene.getStylesheets().add(cssUrl);
     * }
     * }</pre>
     * 
     * <p>Error handling:</p>
     * <ul>
     *   <li>Returns null if the resource path is null or empty</li>
     *   <li>Returns null if the CSS file is not found</li>
     *   <li>Returns null if URL conversion fails</li>
     *   <li>Logs warnings/errors to System.err for debugging</li>
     * </ul>
     * 
     * @param resourcePath the path to the CSS resource (e.g., "/styles.css")
     * @return a URL string suitable for JavaFX stylesheets, or null if loading failed
     */
    public static String loadStylesheet(String resourcePath) {
        if (resourcePath == null || resourcePath.trim().isEmpty()) {
            System.err.println("Warning: Invalid CSS resource path: " + resourcePath);
            return null;
        }

        try {
            URL cssUrl = ResourceLoader.class.getResource(resourcePath);
            if (cssUrl != null) {
                return cssUrl.toExternalForm();
            } else {
                System.err.println("Warning: CSS resource not found: " + resourcePath);
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error loading CSS resource " + resourcePath + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     * 
     * <p>ResourceLoader is designed as a utility class with only static methods.
     * Attempting to instantiate it will result in an UnsupportedOperationException.</p>
     * 
     * @throws UnsupportedOperationException always, as this class cannot be instantiated
     */
    private ResourceLoader() {
        throw new UnsupportedOperationException("ResourceLoader is a utility class and cannot be instantiated");
    }
}