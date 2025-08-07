package com.typerr;

import com.typerr.statics.Constants;
import javafx.scene.image.Image;
import java.io.InputStream;
import java.net.URL;

public final class ResourceLoader {

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

    private ResourceLoader() {
        throw new UnsupportedOperationException("ResourceLoader is a utility class and cannot be instantiated");
    }
}