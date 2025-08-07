package com.typerr.statics;

import com.typerr.TestSession;

/**
 * Enumeration representing the available game modes for typing tests in the Typerr application.
 * 
 * <p>This enum defines the core game mechanics that determine how typing tests are structured
 * and when they conclude. Each game mode provides a different approach to measuring typing
 * performance, allowing users to focus on speed, accuracy, or endurance based on their
 * preferences and training goals.</p>
 * 
 * <p>Game modes are fundamental to the typing test experience and affect:</p>
 * <ul>
 *   <li><strong>Test Duration:</strong> How long the test runs or what triggers completion</li>
 *   <li><strong>Performance Metrics:</strong> Which statistics are most relevant</li>
 *   <li><strong>User Experience:</strong> The pacing and pressure during tests</li>
 *   <li><strong>Progress Tracking:</strong> How improvement is measured over time</li>
 * </ul>
 * 
 * <p>The game mode is selected at the start of each test and cannot be changed during
 * an active session. Different modes may use different word sets and have varying
 * difficulty characteristics.</p>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Create a 60-second time-based test
 * GameMode mode = GameMode.TIME;
 * int testValue = 60; // 60 seconds
 * TestSession session = new TestSession(mode, testValue, wordList);
 * 
 * // Create a 25-word test
 * GameMode mode = GameMode.WORDS;
 * int testValue = 25; // 25 words
 * TestSession session = new TestSession(mode, testValue, wordList);
 * }</pre>
 * 
 * @author ArefinSowad
 * @version 1.0
 * @since 1.0
 * @see TestSession
 * @see com.typerr.network.RoundResult
 */
public enum GameMode {
    
    /**
     * Time-based typing test mode with fixed duration.
     * 
     * <p>In TIME mode, the typing test runs for a predetermined number of seconds,
     * and the goal is to type as many words as possible accurately within the time limit.
     * This mode is ideal for measuring sustained typing speed and is commonly used
     * for standardized typing assessments.</p>
     * 
     * <p>Characteristics of TIME mode:</p>
     * <ul>
     *   <li><strong>Fixed Duration:</strong> Test ends when timer reaches zero</li>
     *   <li><strong>Unlimited Words:</strong> Word list cycles if user types quickly</li>
     *   <li><strong>Speed Focus:</strong> Encourages maximum words per minute (WPM)</li>
     *   <li><strong>Time Pressure:</strong> Creates urgency that can improve performance</li>
     *   <li><strong>Consistent Comparison:</strong> Results are directly comparable across sessions</li>
     * </ul>
     * 
     * <p>Common time values: 15, 30, 60, 120, or 300 seconds.</p>
     * 
     * <p>Performance metrics in TIME mode emphasize:</p>
     * <ul>
     *   <li>Words per minute (WPM) - primary metric</li>
     *   <li>Accuracy percentage - balancing speed with correctness</li>
     *   <li>Character count - total typing output</li>
     *   <li>Consistency - maintaining speed throughout the duration</li>
     * </ul>
     */
    TIME,
    
    /**
     * Word-count-based typing test mode with fixed word target.
     * 
     * <p>In WORDS mode, the typing test continues until the user successfully types
     * a predetermined number of words, regardless of how long it takes. This mode
     * focuses on accuracy and completion rather than pure speed, making it ideal
     * for careful practice and precision training.</p>
     * 
     * <p>Characteristics of WORDS mode:</p>
     * <ul>
     *   <li><strong>Fixed Word Count:</strong> Test ends when target word count is reached</li>
     *   <li><strong>Variable Duration:</strong> Time depends on user's typing speed</li>
     *   <li><strong>Accuracy Focus:</strong> Encourages careful, precise typing</li>
     *   <li><strong>Completion Goal:</strong> Clear endpoint motivates users to finish</li>
     *   <li><strong>Self-Paced:</strong> Users can take time to ensure accuracy</li>
     * </ul>
     * 
     * <p>Common word counts: 10, 25, 50, 100, or 250 words.</p>
     * 
     * <p>Performance metrics in WORDS mode emphasize:</p>
     * <ul>
     *   <li>Accuracy percentage - primary metric</li>
     *   <li>Completion time - secondary speed measurement</li>
     *   <li>Words per minute - calculated from total time</li>
     *   <li>Error rate - mistakes per word typed</li>
     * </ul>
     */
    WORDS
}
