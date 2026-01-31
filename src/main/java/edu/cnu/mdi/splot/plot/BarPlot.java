package edu.cnu.mdi.splot.plot;

import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Objects;

import javax.swing.JFrame;

import edu.cnu.mdi.graphics.style.SymbolType;
import edu.cnu.mdi.splot.fit.CurveDrawingMethod;
import edu.cnu.mdi.splot.pdata.ACurve;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.pdata.PlotDataType;
import edu.cnu.mdi.splot.plot.PlotParameters.RenderHint;
import edu.cnu.mdi.ui.colors.ScientificColorMap;

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
		return plotPanel;
	}
	
	// Assign styles to the curves and plot to mimic bars
	private static void assignStyles(PlotCanvas canvas, List<ACurve> curves, double[] values) {
		ScientificColorMap palette = ScientificColorMap.VIRIDIS;
		
		PlotParameters params = canvas.getParameters();
		params.addRenderHint(RenderHint.BARPLOT);

		for (int i = 0; i < curves.size(); i++) {
			ACurve acurve = curves.get(i);
			Curve curve = (Curve) acurve;
			double x = (double) (i+1) / (curves.size() + 2);
			Color color = palette.colorAt(x);
			curve.getStyle().setLineColor(color);
			curve.getStyle().setFillColor(color);
			curve.getStyle().setLineWidth(40f);
			curve.getStyle().setSymbolType(null); // no symbols
			curve.setCurveDrawingMethod(CurveDrawingMethod.CONNECT);
			
			curve.add(x, 0.0);
			curve.add(x, values[i]);
		}
	}
	
	public static void main(String[] args) throws PlotDataException {
		
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
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
				String title = "Sample Bar Plot";
				double[] values = { 1.0, 2.5, 3.0, 4.5, 2.0 };
				String[] categories = { "A", "B", "C", "D", "E" };
				String xLabel = "Categories";
				String yLabel = "Values";
				
				try {
					PlotPanel barPlot = createBarPlot(title, values, categories, xLabel, yLabel);

					frame.getContentPane().add(barPlot);
				} catch (PlotDataException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				
				frame.setVisible(true);
			}
		});

		
		
		
		
		// Display or save the plot as needed
	}
	
}
