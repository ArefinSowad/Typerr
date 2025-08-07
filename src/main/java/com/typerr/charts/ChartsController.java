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

public class ChartsController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd HH:mm");

    private static final int DEFAULT_DATA_POINTS = Constants.CHART_MAX_DATA_POINTS;

    public static TabPane createStatisticsView() {
        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("stats-tab-pane");

        Tab wpmTab = new Tab("WPM Progress");
        wpmTab.setContent(createWPMProgressionChart());
        wpmTab.setClosable(false);

        Tab accuracyTab = new Tab("Accuracy Progress");
        accuracyTab.setContent(createAccuracyProgressionChart());
        accuracyTab.setClosable(false);

        Tab summaryTab = new Tab("Summary");
        summaryTab.setContent(createPerformanceSummary());
        summaryTab.setClosable(false);

        tabPane.getTabs().addAll(wpmTab, accuracyTab, summaryTab);
        return tabPane;
    }

    private static ScrollPane createWPMProgressionChart() {

        List<StatisticsService.WPMDataPoint> dataPoints =
                TestSession.StatHistory.getWPMProgression(DEFAULT_DATA_POINTS);

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Test Number");
        yAxis.setLabel("Words Per Minute");

        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("WPM Progress Over Time");
        lineChart.setCreateSymbols(true);
        lineChart.setAnimated(true);

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Net WPM");

        for (int i = 0; i < dataPoints.size(); i++) {
            StatisticsService.WPMDataPoint point = dataPoints.get(i);
            series.getData().add(new XYChart.Data<>(i + 1, point.wpm()));
        }

        lineChart.getData().add(series);

        if (dataPoints.size() >= 5) {
            XYChart.Series<Number, Number> trendSeries = createTrendLine(dataPoints);
            lineChart.getData().add(trendSeries);
        }

        VBox container = new VBox(10);
        container.setPadding(new Insets(20));
        container.getChildren().add(lineChart);

        if (!dataPoints.isEmpty()) {
            VBox stats = createWPMStatistics(dataPoints);
            container.getChildren().add(stats);
        }

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);

        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            double speedFactor = 3.0;
            double deltaY = event.getDeltaY() * speedFactor;

            scrollPane.setVvalue(scrollPane.getVvalue() - (deltaY / container.getHeight() * 20));
            event.consume();
        });

        return scrollPane;
    }

    private static XYChart.Series<Number, Number> createTrendLine(List<StatisticsService.WPMDataPoint> dataPoints) {
        XYChart.Series<Number, Number> trendSeries = new XYChart.Series<>();
        trendSeries.setName("Trend");

        double n = dataPoints.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;

        for (int i = 0; i < dataPoints.size(); i++) {
            double x = i + 1;
            double y = dataPoints.get(i).wpm();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        trendSeries.getData().add(new XYChart.Data<>(1, intercept + slope));
        trendSeries.getData().add(new XYChart.Data<>(n, intercept + slope * n));

        return trendSeries;
    }

    private static VBox createWPMStatistics(List<StatisticsService.WPMDataPoint> dataPoints) {
        VBox stats = new VBox(5);
        stats.getStyleClass().add("stats-summary");

        double avgWPM = dataPoints.stream().mapToInt(StatisticsService.WPMDataPoint::wpm).average().orElse(0);
        int maxWPM = dataPoints.stream().mapToInt(StatisticsService.WPMDataPoint::wpm).max().orElse(0);
        int minWPM = dataPoints.stream().mapToInt(StatisticsService.WPMDataPoint::wpm).min().orElse(0);

        double improvement = 0;
        if (dataPoints.size() >= 2) {
            int first = dataPoints.get(0).wpm();
            int last = dataPoints.get(dataPoints.size() - 1).wpm();
            if (first > 0) {
                improvement = ((double) (last - first) / first) * 100;
            }
        }

        stats.getChildren().addAll(
                new Label(String.format("Average WPM: %.1f", avgWPM)),
                new Label("Max WPM: " + maxWPM),
                new Label("Min WPM: " + minWPM),
                new Label(String.format("Improvement: %+.1f%%", improvement))
        );

        return stats;
    }

    private static ScrollPane createAccuracyProgressionChart() {

        List<StatisticsService.AccuracyDataPoint> dataPoints =
                TestSession.StatHistory.getAccuracyProgression(DEFAULT_DATA_POINTS);

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis(0, 100, 5);
        xAxis.setLabel("Test Number");
        yAxis.setLabel("Accuracy (%)");

        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Accuracy Progress Over Time");
        lineChart.setCreateSymbols(true);
        lineChart.setAnimated(true);

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Accuracy");

        for (int i = 0; i < dataPoints.size(); i++) {
            StatisticsService.AccuracyDataPoint point = dataPoints.get(i);
            series.getData().add(new XYChart.Data<>(i + 1, point.accuracy()));
        }

        lineChart.getData().add(series);

        VBox container = new VBox(10);
        container.setPadding(new Insets(20));
        container.getChildren().add(lineChart);

        if (!dataPoints.isEmpty()) {
            VBox stats = createAccuracyStatistics(dataPoints);
            container.getChildren().add(stats);
        }

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);

        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            double speedFactor = 3.0;
            double deltaY = event.getDeltaY() * speedFactor;

            scrollPane.setVvalue(scrollPane.getVvalue() - (deltaY / container.getHeight() * 20));
            event.consume();
        });

        return scrollPane;
    }

    private static VBox createAccuracyStatistics(List<StatisticsService.AccuracyDataPoint> dataPoints) {
        VBox stats = new VBox(5);
        stats.getStyleClass().add("stats-summary");

        double avgAccuracy = dataPoints.stream().mapToInt(StatisticsService.AccuracyDataPoint::accuracy).average().orElse(0);
        int maxAccuracy = dataPoints.stream().mapToInt(StatisticsService.AccuracyDataPoint::accuracy).max().orElse(0);
        int minAccuracy = dataPoints.stream().mapToInt(StatisticsService.AccuracyDataPoint::accuracy).min().orElse(0);

        long perfectScores = dataPoints.stream().mapToInt(StatisticsService.AccuracyDataPoint::accuracy).filter(acc -> acc == 100).count();

        stats.getChildren().addAll(
                new Label(String.format("Average Accuracy: %.1f%%", avgAccuracy)),
                new Label("Max Accuracy: " + maxAccuracy + "%"),
                new Label("Min Accuracy: " + minAccuracy + "%"),
                new Label("Perfect Scores: " + perfectScores)
        );

        return stats;
    }

    private static ScrollPane createPerformanceSummary() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(20));

        VBox overallStats = new VBox(10);
        overallStats.getStyleClass().add("stats-section");

        Label sectionTitle = new Label("Overall Performance");
        sectionTitle.getStyleClass().add("section-title");
        overallStats.getChildren().add(sectionTitle);

        TestSession.TestResult bestResult = TestSession.StatHistory.getBestResult();
        int totalTests = TestSession.StatHistory.getTotalTestCount();

        if (bestResult != null) {

            overallStats.getChildren().addAll(
                    new Label("Best WPM: " + bestResult.netWPM()),
                    new Label("Best Accuracy: " + bestResult.accuracy() + "%"),
                    new Label("Total Tests: " + totalTests)
            );
        } else {

            overallStats.getChildren().add(new Label("No test data available yet."));
        }

        container.getChildren().add(overallStats);

        if (totalTests > 0) {
            VBox distributionSection = createPerformanceDistribution();
            container.getChildren().add(distributionSection);
        }

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);

        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            double speedFactor = 3.0;
            double deltaY = event.getDeltaY() * speedFactor;

            scrollPane.setVvalue(scrollPane.getVvalue() - (deltaY / container.getHeight() * 20));
            event.consume();
        });

        return scrollPane;
    }

    private static VBox createPerformanceDistribution() {
        VBox section = new VBox(10);
        section.getStyleClass().add("stats-section");

        Label title = new Label("WPM Distribution");
        title.getStyleClass().add("section-title");
        section.getChildren().add(title);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("WPM Range");
        yAxis.setLabel("Number of Tests");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Performance Distribution");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Tests");

        List<StatisticsService.WPMDataPoint> wpmData = TestSession.StatHistory.getWPMProgression(1000);

        String[] ranges = {"0-25", "26-50", "51-75", "76-100", "101-125", "126+"};
        int[] counts = new int[ranges.length];

        for (StatisticsService.WPMDataPoint point : wpmData) {
            int wpm = point.wpm();
            if (wpm <= 25) counts[0]++;
            else if (wpm <= 50) counts[1]++;
            else if (wpm <= 75) counts[2]++;
            else if (wpm <= 100) counts[3]++;
            else if (wpm <= 125) counts[4]++;
            else counts[5]++;
        }

        for (int i = 0; i < ranges.length; i++) {
            series.getData().add(new XYChart.Data<>(ranges[i], counts[i]));
        }

        barChart.getData().add(series);
        barChart.setLegendVisible(false);

        section.getChildren().add(barChart);
        return section;
    }
}

