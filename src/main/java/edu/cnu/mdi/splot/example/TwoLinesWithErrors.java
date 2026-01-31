package edu.cnu.mdi.splot.example;

import java.util.Collection;

import edu.cnu.mdi.graphics.style.SymbolType;
import edu.cnu.mdi.splot.fit.CurveDrawingMethod;
import edu.cnu.mdi.splot.fit.Evaluator;
import edu.cnu.mdi.splot.pdata.ACurve;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.FitVectors;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.pdata.PlotDataType;
import edu.cnu.mdi.splot.plot.PlotParameters;

@SuppressWarnings("serial")
public class TwoLinesWithErrors extends AExample {

	public TwoLinesWithErrors(boolean headless) {
		super(headless);
	}

	@Override
	protected PlotData createPlotData() throws PlotDataException {
		String[] curveNames = { "Line 1", "Line 2" };
		int[] fitOrders = { 1, 1 }; // linear fits
		return new PlotData(PlotDataType.XYEXYE, curveNames, fitOrders);
	}

	@Override
	protected String getXAxisLabel() {
		return "<html>x data  X<SUB>M</SUB><SUP>2</SUP>";
	}

	@Override
	protected String getYAxisLabel() {
		return "<html>y data  Y<SUB>Q</SUB><SUP>2</SUP>";
	}

	@Override
	protected String getPlotTitle() {
		return "<html>Sample Plot X<SUP>2</SUP> vs. Q<SUP>2</SUP>";
	}

	// test data for line 1
	private FitVectors line1Data() {
		final double m = 3.3; // slope
		final double b = -0.4; // intercept
		int n = 40;

		Evaluator evaluator = new Evaluator() {
			@Override
			public double value(double x) {
				return m * x + b;
			}
		};

		// test data
		return FitVectors.testData(evaluator, 0.0, 10.0, n, 10, 20);
	}

	// test data for line 2
	private FitVectors line2Data() {
		final double m = -1.7; // slope
		final double b = 8; // intercept
		int n = 20;

		Evaluator evaluator = new Evaluator() {
			@Override
			public double value(double x) {
				return m * x + b;
			}
		};

		// test data
		return FitVectors.testData(evaluator, 0.0, 10.0, n, 6, 11);
	}

	@Override
	public void fillData() {
		FitVectors fv1 = line1Data();
		FitVectors fv2 = line2Data();
		Curve curve1 = (Curve) canvas.getPlotData().getCurve(0);
		Curve curve2 = (Curve) canvas.getPlotData().getCurve(1);

		for (int i = 0; i < fv1.x.length; i++) {
			double e = 1.0 / Math.sqrt(1.0e-12 + fv1.w[i]);
			curve1.add(fv1.x[i], fv1.y[i], e);
		}

		for (int i = 0; i < fv2.x.length; i++) {
			double e = 1.0 / Math.sqrt(1.0e-12 + fv2.w[i]);
			curve2.add(fv2.x[i], fv2.y[i], e);
		}

	}

	@Override
	public void setParameters() {
		PlotData plotData = canvas.getPlotData();
		Collection<ACurve> curves = plotData.getCurves();
		for (ACurve dc : curves) {
			dc.setCurveDrawingMethod(CurveDrawingMethod.POLYNOMIAL);
		}
		
		Curve curve1 = (Curve) plotData.getCurve(0);
		curve1.getStyle().setFillColor(java.awt.Color.BLUE);
		curve1.getStyle().setBorderColor(java.awt.Color.DARK_GRAY);
		curve1.getStyle().setLineColor(java.awt.Color.BLACK);
		curve1.getStyle().setSymbolType(SymbolType.CIRCLE);
		
		Curve curve2 = (Curve) plotData.getCurve(1);
		curve2.getStyle().setFillColor(java.awt.Color.RED);
		curve2.getStyle().setBorderColor(java.awt.Color.DARK_GRAY);
		curve2.getStyle().setLineColor(java.awt.Color.BLACK);
		curve2.getStyle().setSymbolType(SymbolType.SQUARE);
		

		// many options controlled via plot parameters
		PlotParameters params = canvas.getParameters();
		params.includeXZero(true);
		params.includeYZero(true);
	}

	public static void main(String arg[]) {

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				TwoLinesWithErrors example = new TwoLinesWithErrors(false);
				example.setVisible(true);
			}
		});

	}

}
