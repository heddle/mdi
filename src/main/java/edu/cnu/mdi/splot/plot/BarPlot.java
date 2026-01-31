package edu.cnu.mdi.splot.plot;

import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Objects;

import javax.swing.JFrame;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatIntelliJLaf;

import edu.cnu.mdi.splot.fit.CurveDrawingMethod;
import edu.cnu.mdi.splot.pdata.ACurve;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.pdata.PlotDataType;
import edu.cnu.mdi.ui.colors.ScientificColorMap;
import edu.cnu.mdi.ui.fonts.Fonts;

public class BarPlot {
		
	
	/**
	 * Create a simple bar plot. Actually creates an XY plot to mimic a bar plot.
	 * 
	 * @param title      the plot title
	 * @param values     the bar values
	 * @param categories the bar categories
	 * @param xLabel     the x axis label
	 * @param yLabel     the y axis label
	 * @return the plot data
	 * @throws PlotDataException if there is a problem creating the plot data
	 */
	public static PlotPanel createBarPlot(String title, 
			double[] values, 
			String[] categories, 
			String xLabel, 
			String yLabel) throws PlotDataException {
		
		Objects.requireNonNull(values, "Values array cannot be null");
		Objects.requireNonNull(categories, "Categories array cannot be null");
		if (values.length != categories.length) {
			throw new IllegalArgumentException("Values and categories arrays must have the same length");
		}
		if (values.length == 0) {
			throw new IllegalArgumentException("Values and categories arrays cannot be empty");
		}
				
		PlotData plotData = new PlotData(PlotDataType.XYXY, categories, null);
		PlotCanvas canvas = new PlotCanvas(plotData, title, xLabel, yLabel);
		PlotPanel plotPanel = new PlotPanel(canvas);

		List<ACurve> curves = plotData.getCurves();
		assignStyles(canvas, curves, values);
		forceBehavior(canvas, curves.size());
		return plotPanel;
	}
	
	// Assign styles to the curves and plot to mimic bars
	private static void assignStyles(PlotCanvas canvas, List<ACurve> curves, double[] values) {
		ScientificColorMap palette = ScientificColorMap.VIRIDIS;
	

		double u = 1.0/(3*curves.size() + 1);
		for (int i = 0; i < curves.size(); i++) {
			ACurve acurve = curves.get(i);
			Curve curve = (Curve) acurve;
			double x = u * (3*i + 1);
			Color color = palette.colorAt(x);
			curve.getStyle().setLineColor(color.darker());
			curve.getStyle().setFillColor(color);
			curve.getStyle().setLineWidth(1f);
			curve.getStyle().setSymbolType(null); // no symbols
			curve.setCurveDrawingMethod(CurveDrawingMethod.CONNECT);
			
			curve.add(x, 0.0);
			curve.add(x, values[i]);
			curve.add(x+2*u, values[i]);
			curve.add(x+2*u, 0.0);
		}
		
	}
	
	private static void forceBehavior(PlotCanvas canvas, int numCurves) {
		PlotParameters params = canvas.getParameters();
		params.addRenderHint(RenderHint.BARPLOT);
		params.setXScale(PlotParameters.AxisScale.LINEAR);
		params.setYScale(PlotParameters.AxisScale.LINEAR);
		params.includeXZero(true);
		params.includeYZero(true);
		params.setLogZ(false);
		params.addPlotLine(new HorizontalLine(canvas, 0));
		params.setLegendDrawing(false);
		
		PlotTicks ticks = canvas.getPlotTicks();
		ticks.setNumMajorTickY(4);
		ticks.setNumMinorTickY(0);
		ticks.setNumMajorTickX(numCurves);
		ticks.setNumMinorTickX(0);
	
		Rectangle2D.Double world = canvas.getDataWorld();
		//add horizontal line at every integer y value
		for (int y = -1; y <= (int) world.getMaxY() + 1; y++) {
			params.addPlotLine(new HorizontalLine(canvas, y));
		}
	}
	
	public static PlotPanel demoBarPlot() throws PlotDataException {
		String title = "Sample Bar Plot";
		double[] values = { 1.0, 2.5, 3.0, 4.5, -2.0, 0.4, 2.6};
		String[] categories = { "Category A", "Category B", 
				"Category C", "Category D", "Category E",
				"Category F", "Category G"};
		String xLabel = "Categories";
		String yLabel = "Values";
		
		return createBarPlot(title, values, categories, xLabel, yLabel);
	}
	
	// initialize the FlatLaf UI
	private static void UIInit() {
		FlatIntelliJLaf.setup();
		UIManager.put("Component.focusWidth", 1);
		UIManager.put("Component.arc", 6);
		UIManager.put("Button.arc", 6);
		UIManager.put("TabbedPane.showTabSeparators", true);
		Fonts.refresh();
	}

	
	public static void main(String[] args) throws PlotDataException {
		
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				UIInit();

				JFrame frame = new JFrame("Bar Plot Example");
				// set up what to do if the window is closed
				WindowAdapter windowAdapter = new WindowAdapter() {
					@Override
					public void windowClosing(WindowEvent event) {
						System.exit(1);
					}
				};
				frame.addWindowListener(windowAdapter);
				frame.setSize(800, 600);
				
				try {
					PlotPanel barPlot = demoBarPlot();
					frame.getContentPane().add(barPlot);
					frame.setVisible(true);
				} catch (PlotDataException e) {
					e.printStackTrace();
				}
				

				
			}
		});

		
		
		
		
		// Display or save the plot as needed
	}
	
}
