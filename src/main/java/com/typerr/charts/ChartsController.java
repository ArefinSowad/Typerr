package com.typerr.charts;

import com.typerr.statics.Constants;
import com.typerr.TestSession;
import com.typerr.database.StatisticsService;
import javafx.geometry.Insets;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for creating and managing statistical charts and performance visualizations.
 * 
 * <p>This class provides static factory methods for creating comprehensive statistics views
 * that display user typing performance data through various chart types and summaries.
 * It integrates with the {@link TestSession.StatHistory} and {@link StatisticsService}
 * to retrieve and visualize historical performance data.</p>
 * 
 * <p>The controller creates a tabbed interface with three main sections:</p>
 * <ul>
 *   <li><strong>WPM Progress</strong> - Line chart showing words-per-minute improvement over time</li>
 *   <li><strong>Accuracy Progress</strong> - Line chart displaying accuracy trends and consistency</li>
 *   <li><strong>Performance Summary</strong> - Overall statistics and WPM distribution analysis</li>
 * </ul>
 * 
 * <p>Key features include:</p>
 * <ul>
 *   <li>Interactive charts with zoom and scroll capabilities</li>
 *   <li>Trend line calculations using linear regression</li>
 *   <li>Statistical summaries (averages, maximums, minimums)</li>
 *   <li>Performance distribution histograms</li>
 *   <li>Improvement tracking and progress indicators</li>
 * </ul>
 * 
 * <p>All charts are responsive and styled according to the application's CSS theme.
 * The visualizations automatically adapt to available data and provide meaningful
 * insights even with limited test history.</p>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * TabPane statisticsView = ChartsController.createStatisticsView();
 * Scene scene = new Scene(statisticsView, 800, 600);
 * stage.setScene(scene);
 * }</pre>
 * 
 * @author ArefinSowad
 * @version 1.0
 * @since 1.0
 * @see TestSession.StatHistory
 * @see StatisticsService
 */
public class ChartsController {

    /** Date and time formatter for chart labels and timestamps. */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd HH:mm");
    
    /** Default number of data points to display in charts, taken from application constants. */
    private static final int DEFAULT_DATA_POINTS = Constants.CHART_MAX_DATA_POINTS;

    /**
     * Creates the main statistics view with tabbed charts and performance summaries.
     * 
     * <p>This method constructs a complete {@link TabPane} containing all available
     * statistical visualizations. Each tab provides a different perspective on the
     * user's typing performance history and progress.</p>
     * 
     * <p>The returned TabPane includes:</p>
     * <ul>
     *   <li>WPM Progress tab with line chart and trend analysis</li>
     *   <li>Accuracy Progress tab with accuracy tracking over time</li>
     *   <li>Summary tab with overall statistics and distribution charts</li>
     * </ul>
     * 
     * <p>All tabs are non-closable and styled with appropriate CSS classes
     * for consistent theming throughout the application.</p>
     * 
     * @return a TabPane containing all statistics charts and summaries
     */
    public static TabPane createStatisticsView() {
        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("stats-tab-pane");

        // Create WPM progress tracking tab
        Tab wpmTab = new Tab("WPM Progress");
        wpmTab.setContent(createWPMProgressionChart());
        wpmTab.setClosable(false);

        // Create accuracy tracking tab
        Tab accuracyTab = new Tab("Accuracy Progress");
        accuracyTab.setContent(createAccuracyProgressionChart());
        accuracyTab.setClosable(false);

        // Create overall performance summary tab
        Tab summaryTab = new Tab("Summary");
        summaryTab.setContent(createPerformanceSummary());
        summaryTab.setClosable(false);

        tabPane.getTabs().addAll(wpmTab, accuracyTab, summaryTab);
        return tabPane;
    }

    /**
     * Creates a scrollable chart showing WPM (Words Per Minute) progression over time.
     * 
     * <p>This method generates a comprehensive line chart that displays the user's
     * typing speed improvement over their test history. The chart includes:</p>
     * <ul>
     *   <li>Primary data series showing actual WPM values</li>
     *   <li>Trend line using linear regression (if 5+ data points exist)</li>
     *   <li>Statistical summary below the chart</li>
     *   <li>Custom scroll behavior for smooth navigation</li>
     * </ul>
     * 
     * <p>The chart automatically scales to accommodate the data range and provides
     * visual indicators for performance trends. If insufficient data is available,
     * appropriate fallbacks are provided.</p>
     * 
     * @return a ScrollPane containing the WPM progression chart and statistics
     */
    private static ScrollPane createWPMProgressionChart() {
        // Retrieve WPM data points from test history
        List<StatisticsService.WPMDataPoint> dataPoints =
                TestSession.StatHistory.getWPMProgression(DEFAULT_DATA_POINTS);

        // Configure chart axes
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Test Number");
        yAxis.setLabel("Words Per Minute");

        // Create and configure the line chart
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("WPM Progress Over Time");
        lineChart.setCreateSymbols(true);  // Show data point markers
        lineChart.setAnimated(true);       // Enable smooth animations

        // Create primary data series for WPM values
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Net WPM");

        // Populate chart with actual test data
        for (int i = 0; i < dataPoints.size(); i++) {
            StatisticsService.WPMDataPoint point = dataPoints.get(i);
            series.getData().add(new XYChart.Data<>(i + 1, point.wpm()));
        }

        lineChart.getData().add(series);

        // Add trend line if sufficient data points exist
        if (dataPoints.size() >= 5) {
            XYChart.Series<Number, Number> trendSeries = createTrendLine(dataPoints);
            lineChart.getData().add(trendSeries);
        }

        // Create container for chart and statistics
        VBox container = new VBox(10);
        container.setPadding(new Insets(20));
        container.getChildren().add(lineChart);

        // Add statistical summary if data is available
        if (!dataPoints.isEmpty()) {
            VBox stats = createWPMStatistics(dataPoints);
            container.getChildren().add(stats);
        }

        // Create scrollable container with custom scroll behavior
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);

        // Configure enhanced scroll behavior for better user experience
        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            double speedFactor = 3.0;
            double deltaY = event.getDeltaY() * speedFactor;

            scrollPane.setVvalue(scrollPane.getVvalue() - (deltaY / container.getHeight() * 20));
            event.consume();
        });

        return scrollPane;
    }

    /**
     * Creates a trend line for WPM data using linear regression analysis.
     * 
     * <p>This method calculates a best-fit line through the WPM data points using
     * the least squares linear regression method. The trend line helps visualize
     * overall performance improvement or decline over time.</p>
     * 
     * <p>The calculation uses the standard linear regression formulas:</p>
     * <ul>
     *   <li>Slope = (n×Σ(xy) - Σ(x)×Σ(y)) / (n×Σ(x²) - (Σ(x))²)</li>
     *   <li>Intercept = (Σ(y) - slope×Σ(x)) / n</li>
     * </ul>
     * 
     * @param dataPoints the WPM data points to analyze (must contain at least 2 points)
     * @return a chart series representing the calculated trend line
     */
    private static XYChart.Series<Number, Number> createTrendLine(List<StatisticsService.WPMDataPoint> dataPoints) {
        XYChart.Series<Number, Number> trendSeries = new XYChart.Series<>();
        trendSeries.setName("Trend");

        double n = dataPoints.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;

        // Calculate sums for linear regression
        for (int i = 0; i < dataPoints.size(); i++) {
            double x = i + 1;  // Test number (1-based)
            double y = dataPoints.get(i).wpm();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }

        // Apply linear regression formulas
        double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        // Create trend line with start and end points
        trendSeries.getData().add(new XYChart.Data<>(1, intercept + slope));
        trendSeries.getData().add(new XYChart.Data<>(n, intercept + slope * n));

        return trendSeries;
    }

    /**
     * Creates a statistical summary panel for WPM performance data.
     * 
     * <p>This method generates a formatted summary containing key performance
     * metrics derived from the WPM data points. The summary includes descriptive
     * statistics and improvement indicators.</p>
     * 
     * <p>Calculated metrics include:</p>
     * <ul>
     *   <li>Average WPM across all tests</li>
     *   <li>Maximum WPM achieved</li>
     *   <li>Minimum WPM recorded</li>
     *   <li>Overall improvement percentage (first vs. last test)</li>
     * </ul>
     * 
     * @param dataPoints the WPM data points to analyze (must not be empty)
     * @return a VBox container with formatted statistical labels
     */
    private static VBox createWPMStatistics(List<StatisticsService.WPMDataPoint> dataPoints) {
        VBox stats = new VBox(5);
        stats.getStyleClass().add("stats-summary");

        // Calculate descriptive statistics
        double avgWPM = dataPoints.stream().mapToInt(StatisticsService.WPMDataPoint::wpm).average().orElse(0);
        int maxWPM = dataPoints.stream().mapToInt(StatisticsService.WPMDataPoint::wpm).max().orElse(0);
        int minWPM = dataPoints.stream().mapToInt(StatisticsService.WPMDataPoint::wpm).min().orElse(0);

        // Calculate improvement percentage (first test vs. last test)
        double improvement = 0;
        if (dataPoints.size() >= 2) {
            int first = dataPoints.get(0).wpm();
            int last = dataPoints.get(dataPoints.size() - 1).wpm();
            if (first > 0) {
                improvement = ((double) (last - first) / first) * 100;
            }
        }

        // Create formatted statistics labels
        stats.getChildren().addAll(
                new Label(String.format("Average WPM: %.1f", avgWPM)),
                new Label("Max WPM: " + maxWPM),
                new Label("Min WPM: " + minWPM),
                new Label(String.format("Improvement: %+.1f%%", improvement))
        );

        return stats;
    }

    /**
     * Creates a scrollable chart showing accuracy progression over time.
     * 
     * <p>This method generates a line chart that displays the user's typing accuracy
     * consistency and improvement over their test history. The chart focuses specifically
     * on accuracy metrics and includes:</p>
     * <ul>
     *   <li>Accuracy percentage data series (0-100% scale)</li>
     *   <li>Fixed Y-axis scale for consistent comparison</li>
     *   <li>Statistical summary for accuracy analysis</li>
     *   <li>Custom scroll behavior for navigation</li>
     * </ul>
     * 
     * <p>The accuracy chart uses a fixed scale (0-100%) with 5% increments to provide
     * consistent visual reference across different test sessions.</p>
     * 
     * @return a ScrollPane containing the accuracy progression chart and statistics
     */
    private static ScrollPane createAccuracyProgressionChart() {
        // Retrieve accuracy data points from test history
        List<StatisticsService.AccuracyDataPoint> dataPoints =
                TestSession.StatHistory.getAccuracyProgression(DEFAULT_DATA_POINTS);

        // Configure chart axes with fixed accuracy scale
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis(0, 100, 5);  // 0-100% with 5% increments
        xAxis.setLabel("Test Number");
        yAxis.setLabel("Accuracy (%)");

        // Create and configure the line chart
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Accuracy Progress Over Time");
        lineChart.setCreateSymbols(true);  // Show data point markers
        lineChart.setAnimated(true);       // Enable smooth animations

        // Create data series for accuracy values
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Accuracy");

        // Populate chart with accuracy data
        for (int i = 0; i < dataPoints.size(); i++) {
            StatisticsService.AccuracyDataPoint point = dataPoints.get(i);
            series.getData().add(new XYChart.Data<>(i + 1, point.accuracy()));
        }

        lineChart.getData().add(series);

        // Create container for chart and statistics
        VBox container = new VBox(10);
        container.setPadding(new Insets(20));
        container.getChildren().add(lineChart);

        // Add statistical summary if data is available
        if (!dataPoints.isEmpty()) {
            VBox stats = createAccuracyStatistics(dataPoints);
            container.getChildren().add(stats);
        }

        // Create scrollable container with custom scroll behavior
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);

        // Configure enhanced scroll behavior for better user experience
        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            double speedFactor = 3.0;
            double deltaY = event.getDeltaY() * speedFactor;

            scrollPane.setVvalue(scrollPane.getVvalue() - (deltaY / container.getHeight() * 20));
            event.consume();
        });

        return scrollPane;
    }

    /**
     * Creates a statistical summary panel for accuracy performance data.
     * 
     * <p>This method generates a formatted summary containing key accuracy metrics
     * derived from the accuracy data points. The summary provides insights into
     * typing consistency and precision.</p>
     * 
     * <p>Calculated metrics include:</p>
     * <ul>
     *   <li>Average accuracy percentage across all tests</li>
     *   <li>Maximum accuracy achieved</li>
     *   <li>Minimum accuracy recorded</li>
     *   <li>Count of perfect scores (100% accuracy)</li>
     * </ul>
     * 
     * @param dataPoints the accuracy data points to analyze (must not be empty)
     * @return a VBox container with formatted accuracy statistics labels
     */
    private static VBox createAccuracyStatistics(List<StatisticsService.AccuracyDataPoint> dataPoints) {
        VBox stats = new VBox(5);
        stats.getStyleClass().add("stats-summary");

        // Calculate descriptive accuracy statistics
        double avgAccuracy = dataPoints.stream().mapToInt(StatisticsService.AccuracyDataPoint::accuracy).average().orElse(0);
        int maxAccuracy = dataPoints.stream().mapToInt(StatisticsService.AccuracyDataPoint::accuracy).max().orElse(0);
        int minAccuracy = dataPoints.stream().mapToInt(StatisticsService.AccuracyDataPoint::accuracy).min().orElse(0);

        // Count perfect accuracy scores (100%)
        long perfectScores = dataPoints.stream().mapToInt(StatisticsService.AccuracyDataPoint::accuracy).filter(acc -> acc == 100).count();

        // Create formatted statistics labels
        stats.getChildren().addAll(
                new Label(String.format("Average Accuracy: %.1f%%", avgAccuracy)),
                new Label("Max Accuracy: " + maxAccuracy + "%"),
                new Label("Min Accuracy: " + minAccuracy + "%"),
                new Label("Perfect Scores: " + perfectScores)
        );

        return stats;
    }

    /**
     * Creates a comprehensive performance summary view with overall statistics and distribution analysis.
     * 
     * <p>This method generates a scrollable summary view that provides a high-level overview
     * of the user's typing performance. The summary includes both overall achievement metrics
     * and detailed performance distribution analysis.</p>
     * 
     * <p>The summary contains:</p>
     * <ul>
     *   <li>Overall performance section with best results and total test count</li>
     *   <li>WPM distribution histogram (if test data exists)</li>
     *   <li>Performance breakdown across different speed ranges</li>
     * </ul>
     * 
     * <p>If no test data is available, appropriate placeholder content is displayed
     * to guide new users.</p>
     * 
     * @return a ScrollPane containing the complete performance summary
     */
    private static ScrollPane createPerformanceSummary() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(20));

        // Create overall performance statistics section
        VBox overallStats = new VBox(10);
        overallStats.getStyleClass().add("stats-section");

        Label sectionTitle = new Label("Overall Performance");
        sectionTitle.getStyleClass().add("section-title");
        overallStats.getChildren().add(sectionTitle);

        // Retrieve best results and test count from history
        TestSession.TestResult bestResult = TestSession.StatHistory.getBestResult();
        int totalTests = TestSession.StatHistory.getTotalTestCount();

        if (bestResult != null) {
            // Display best achievements and total test count
            overallStats.getChildren().addAll(
                    new Label("Best WPM: " + bestResult.netWPM()),
                    new Label("Best Accuracy: " + bestResult.accuracy() + "%"),
                    new Label("Total Tests: " + totalTests)
            );
        } else {
            // Show placeholder for new users
            overallStats.getChildren().add(new Label("No test data available yet."));
        }

        container.getChildren().add(overallStats);

        // Add performance distribution section if test data exists
        if (totalTests > 0) {
            VBox distributionSection = createPerformanceDistribution();
            container.getChildren().add(distributionSection);
        }

        // Create scrollable container with custom scroll behavior
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);

        // Configure enhanced scroll behavior for better user experience
        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            double speedFactor = 3.0;
            double deltaY = event.getDeltaY() * speedFactor;

            scrollPane.setVvalue(scrollPane.getVvalue() - (deltaY / container.getHeight() * 20));
            event.consume();
        });

        return scrollPane;
    }

    /**
     * Creates a performance distribution histogram showing WPM ranges and frequencies.
     * 
     * <p>This method generates a bar chart that visualizes the distribution of typing
     * speeds across different WPM ranges. The chart helps users understand their
     * performance consistency and identify their most common typing speed ranges.</p>
     * 
     * <p>The distribution uses the following WPM ranges:</p>
     * <ul>
     *   <li>0-25 WPM - Beginner level</li>
     *   <li>26-50 WPM - Novice level</li>
     *   <li>51-75 WPM - Intermediate level</li>
     *   <li>76-100 WPM - Advanced level</li>
     *   <li>101-125 WPM - Expert level</li>
     *   <li>126+ WPM - Professional level</li>
     * </ul>
     * 
     * @return a VBox containing the WPM distribution bar chart
     */
    private static VBox createPerformanceDistribution() {
        VBox section = new VBox(10);
        section.getStyleClass().add("stats-section");

        Label title = new Label("WPM Distribution");
        title.getStyleClass().add("section-title");
        section.getChildren().add(title);

        // Configure bar chart axes
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("WPM Range");
        yAxis.setLabel("Number of Tests");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Performance Distribution");

        // Create data series for the histogram
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Tests");

        // Retrieve WPM data for distribution analysis (up to 1000 recent tests)
        List<StatisticsService.WPMDataPoint> wpmData = TestSession.StatHistory.getWPMProgression(1000);

        // Define WPM ranges and initialize counters
        String[] ranges = {"0-25", "26-50", "51-75", "76-100", "101-125", "126+"};
        int[] counts = new int[ranges.length];

        // Categorize each WPM value into appropriate range
        for (StatisticsService.WPMDataPoint point : wpmData) {
            int wpm = point.wpm();
            if (wpm <= 25) counts[0]++;
            else if (wpm <= 50) counts[1]++;
            else if (wpm <= 75) counts[2]++;
            else if (wpm <= 100) counts[3]++;
            else if (wpm <= 125) counts[4]++;
            else counts[5]++;  // 126+ WPM
        }

        // Populate bar chart with distribution data
        for (int i = 0; i < ranges.length; i++) {
            series.getData().add(new XYChart.Data<>(ranges[i], counts[i]));
        }

        barChart.getData().add(series);
        barChart.setLegendVisible(false);  // Hide legend for cleaner appearance

        section.getChildren().add(barChart);
        return section;
    }
}

