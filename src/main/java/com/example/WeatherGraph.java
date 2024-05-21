package com.example;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class WeatherGraph {

    public static File createTemperatureChart(double[] temperatures, String[] times) throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (int i = 0; i < temperatures.length; i++) {
            dataset.addValue(temperatures[i], "Temperature", times[i]);
        }

        JFreeChart lineChart = ChartFactory.createLineChart(
                "Temperature Over Time",
                "Time",
                "Temperature (°C)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        // Customize the chart appearance
        CategoryPlot plot = lineChart.getCategoryPlot();
        plot.setBackgroundPaint(new Color(240, 248, 255));  // Alice blue background
        plot.setDomainGridlinePaint(new Color(173, 216, 230));  // Light blue gridlines
        plot.setRangeGridlinePaint(new Color(173, 216, 230));

        // Customize the renderer for a darker blue line
        LineAndShapeRenderer renderer = new LineAndShapeRenderer();
        renderer.setSeriesPaint(0, new Color(25, 25, 112));  // Midnight blue
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));  // Thicker line
        plot.setRenderer(renderer);

        // Customize the domain axis (X axis)
        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);  // Rotate labels 45 degrees
        domainAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        domainAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
        domainAxis.setTickLabelPaint(new Color(25, 25, 112));  // Midnight blue
        domainAxis.setLabelPaint(new Color(25, 25, 112));

        // Customize the range axis (Y axis)
        plot.getRangeAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        plot.getRangeAxis().setLabelFont(new Font("SansSerif", Font.BOLD, 12));
        plot.getRangeAxis().setTickLabelPaint(new Color(25, 25, 112));  // Midnight blue
        plot.getRangeAxis().setLabelPaint(new Color(25, 25, 112));

        // Customize the title
        lineChart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 16));
        lineChart.getTitle().setPaint(new Color(25, 25, 112));  // Midnight blue
        lineChart.setBackgroundPaint(Color.WHITE);

        File chartFile = new File("TemperatureChart.jpeg");
        ChartUtils.saveChartAsJPEG(chartFile, lineChart, 800, 600);

        return chartFile;
    }
}
