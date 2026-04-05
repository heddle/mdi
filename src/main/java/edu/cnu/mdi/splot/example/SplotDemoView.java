package edu.cnu.mdi.splot.example;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import edu.cnu.mdi.properties.PropertyUtils;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.plot.BarPlot;
import edu.cnu.mdi.splot.plot.PlotView;
import edu.cnu.mdi.view.BaseView;

public class SplotDemoView {
	
	// This is a static method that creates and returns a PlotView configured 
	// with a menu of demo plots.
	public static PlotView createDemoView() {
		final PlotView view = new PlotView(PropertyUtils.TITLE, "Demo Plots", PropertyUtils.FRACTION, 0.7,
				PropertyUtils.ASPECT, 1.2, PropertyUtils.VISIBLE, true);

		// add the examples menu and call "hack" to fix focus issues
		JMenu examplesMenu = new JMenu("Gallery");
		BaseView.applyFocusFix(examplesMenu, view);
		view.getJMenuBar().add(examplesMenu, 1); // after File menu

		JMenuItem gaussianItem = new JMenuItem("Gaussian Fit");
		JMenuItem anotherGaussianItem = new JMenuItem("Another Gaussian");
		JMenuItem logItem = new JMenuItem("Log-log Plot");
		JMenuItem erfcItem = new JMenuItem("Erfc Fit");
		JMenuItem erfItem = new JMenuItem("Erf Fit");
		JMenuItem histoItem = new JMenuItem("Histogram");
		JMenuItem growingHistoItem = new JMenuItem("Growing Histogram");
		JMenuItem heatmapItem = new JMenuItem("Heatmap");
		JMenuItem lineItem = new JMenuItem("Straight Line Fit");
		JMenuItem stripItem = new JMenuItem("Memory Use Strip Chart");
		JMenuItem threeGaussiansItem = new JMenuItem("Three Gaussians");
		JMenuItem twoHistoItem = new JMenuItem("Two Histograms");
		JMenuItem twoLines = new JMenuItem("Two Lines with Errors");
		JMenuItem scatterItem = new JMenuItem("Scatter Example");
		JMenuItem barItem = new JMenuItem("Barplot Example");

		gaussianItem.addActionListener(e -> {
			Gaussian example = new Gaussian(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		anotherGaussianItem.addActionListener(e -> {
			AnotherGaussian example = new AnotherGaussian(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		logItem.addActionListener(e -> {
			CubicLogLog example = new CubicLogLog(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		erfcItem.addActionListener(e -> {
			ErfcTest example = new ErfcTest(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		erfItem.addActionListener(e -> {
			ErfTest example = new ErfTest(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		histoItem.addActionListener(e -> {
			Histo example = new Histo(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		growingHistoItem.addActionListener(e -> {
			GrowingHisto example = new GrowingHisto(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		heatmapItem.addActionListener(e -> {
			Heatmap example = new Heatmap(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		lineItem.addActionListener(e -> {
			StraightLine example = new StraightLine(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		stripItem.addActionListener(e -> {
			StripChart example = new StripChart(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		threeGaussiansItem.addActionListener(e -> {
			ThreeGaussians example = new ThreeGaussians(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		twoHistoItem.addActionListener(e -> {
			TwoHisto example = new TwoHisto(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		twoLines.addActionListener(e -> {
			TwoLinesWithErrors example = new TwoLinesWithErrors(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		scatterItem.addActionListener(e -> {
			Scatter example = new Scatter(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		barItem.addActionListener(e -> {
			try {
				view.switchToPlotPanel(BarPlot.demoBarPlot());
			} catch (PlotDataException e1) {
				e1.printStackTrace();
			}

		});

		examplesMenu.add(gaussianItem);
		examplesMenu.add(anotherGaussianItem);
		examplesMenu.add(logItem);
		examplesMenu.add(erfcItem);
		examplesMenu.add(erfItem);
		examplesMenu.add(histoItem);
		examplesMenu.add(growingHistoItem);
		examplesMenu.add(heatmapItem);
		examplesMenu.add(lineItem);
		examplesMenu.add(stripItem);
		examplesMenu.add(threeGaussiansItem);
		examplesMenu.add(twoHistoItem);
		examplesMenu.add(twoLines);
		examplesMenu.add(scatterItem);
		examplesMenu.add(barItem);
		return view;		
	}

}
