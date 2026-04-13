package edu.cnu.mdi.splot.example;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.plot.BarPlot;
import edu.cnu.mdi.splot.plot.PlotView;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.BaseView;

/**
 * Factory that creates the splot demo {@link PlotView} with a Gallery menu
 * of all available plot examples.
 *
 * <p>The Gallery menu is grouped into two sections separated by a divider:</p>
 * <ul>
 *   <li><b>Classic examples</b> &mdash; the original set of demos.</li>
 *   <li><b>New fit types</b> &mdash; exponential decay, power law, and
 *       Lorentzian, added as the final feature-complete additions to MDI.</li>
 * </ul>
 */
public class SplotDemoView {

	/**
	 * Creates and returns a {@link PlotView} pre-configured with the full
	 * Gallery menu.
	 *
	 * @return the configured view (already set visible)
	 */
	public static PlotView createDemoView() {
		final PlotView view = new PlotView(
				PropertyUtils.TITLE,   "Demo Plots",
				PropertyUtils.FRACTION, 0.7,
				PropertyUtils.ASPECT,   1.2,
				PropertyUtils.VISIBLE,  true);

		// Gallery menu — added after the File menu.
		JMenu examplesMenu = new JMenu("Gallery");
		BaseView.applyFocusFix(examplesMenu, view);
		view.getJMenuBar().add(examplesMenu, 1);

		// ---------------------------------------------------------------
		// Classic examples
		// ---------------------------------------------------------------
		JMenuItem gaussianItem       = new JMenuItem("Gaussian Fit");
		JMenuItem anotherGaussianItem = new JMenuItem("Another Gaussian");
		JMenuItem logItem            = new JMenuItem("Log-log Plot");
		JMenuItem erfcItem           = new JMenuItem("Erfc Fit");
		JMenuItem erfItem            = new JMenuItem("Erf Fit");
		JMenuItem histoItem          = new JMenuItem("Histogram");
		JMenuItem growingHistoItem   = new JMenuItem("Growing Histogram");
		JMenuItem heatmapItem        = new JMenuItem("Heatmap");
		JMenuItem lineItem           = new JMenuItem("Straight Line Fit");
		JMenuItem stripItem          = new JMenuItem("Memory Use Strip Chart");
		JMenuItem threeGaussiansItem = new JMenuItem("Three Gaussians");
		JMenuItem twoHistoItem       = new JMenuItem("Two Histograms");
		JMenuItem twoLines           = new JMenuItem("Two Lines with Errors");
		JMenuItem scatterItem        = new JMenuItem("Scatter Example");
		JMenuItem barItem            = new JMenuItem("Barplot Example");

		// ---------------------------------------------------------------
		// New fit types
		// ---------------------------------------------------------------
		JMenuItem expDecayItem  = new JMenuItem("Exponential Decay Fit");
		JMenuItem powerLawItem  = new JMenuItem("Power Law Fit");
		JMenuItem lorentzItem   = new JMenuItem("Lorentzian Fit");

		// ---------------------------------------------------------------
		// Action listeners — classic
		// ---------------------------------------------------------------
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

		// ---------------------------------------------------------------
		// Action listeners — new fit types
		// ---------------------------------------------------------------
		expDecayItem.addActionListener(e -> {
			ExponentialDecayExample example = new ExponentialDecayExample(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		powerLawItem.addActionListener(e -> {
			PowerLawExample example = new PowerLawExample(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		lorentzItem.addActionListener(e -> {
			LorentzianExample example = new LorentzianExample(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		// ---------------------------------------------------------------
		// Build the menu
		// ---------------------------------------------------------------
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

		// Separator before the three new fit types.
		examplesMenu.add(new JSeparator());

		examplesMenu.add(expDecayItem);
		examplesMenu.add(powerLawItem);
		examplesMenu.add(lorentzItem);

		return view;
	}
}