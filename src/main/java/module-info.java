/**
 * The Typerr application module definition.
 * 
 * <p>Typerr is a modern typing speed and accuracy testing application built with JavaFX.
 * This module defines the dependencies and package exports for the entire application,
 * including networking capabilities for multiplayer typing games, database functionality
 * for statistics tracking, and a rich user interface with charts and visualizations.</p>
 * 
 * <p>Key features provided by this module:</p>
 * <ul>
 *   <li>Typing speed tests with various game modes (time-based, word count)</li>
 *   <li>Real-time accuracy and WPM calculations</li>
 *   <li>Multiplayer networking support for competitive typing</li>
 *   <li>Statistics tracking and performance visualization</li>
 *   <li>Customizable themes and keyboard shortcuts</li>
 * </ul>
 * 
 * @author ArefinSowad
 * @version 1.0
 * @since 1.0
 */
module com.typerr {
    // JavaFX dependencies for UI components
    requires javafx.controls;
    
    // Java standard library modules
    requires java.prefs;    // For application preferences and settings
    requires java.sql;      // For SQLite database operations
    
    // Jackson JSON processing dependencies
    requires com.fasterxml.jackson.core;           // Core JSON processing
    requires com.fasterxml.jackson.databind;       // Object mapping and binding
    requires com.fasterxml.jackson.datatype.jsr310; // Java 8 time API support

    // Open packages to Jackson for JSON serialization/deserialization
    opens com.typerr.database to com.fasterxml.jackson.databind;
    opens com.typerr.network to com.fasterxml.jackson.databind;
    
    // Export public API packages
    exports com.typerr;          // Main application package
    exports com.typerr.database; // Database and statistics services
    exports com.typerr.charts;   // Chart and visualization components
    exports com.typerr.ui;       // User interface utilities and managers
    exports com.typerr.network;
    exports com.typerr.statics;  // Networking and multiplayer functionality
}