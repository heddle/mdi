package edu.cnu.mdi.splot.example;

import java.awt.Color;

import org.apache.commons.math3.distribution.NormalDistribution;

import edu.cnu.mdi.graphics.style.IStyled;
import edu.cnu.mdi.splot.fit.CurveDrawingMethod;
import edu.cnu.mdi.splot.pdata.HistoCurve;
import edu.cnu.mdi.splot.pdata.HistoData;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.plot.PlotCanvas;
import edu.cnu.mdi.splot.plot.PlotChangeType;
import edu.cnu.mdi.splot.plot.PlotParameters;

@SuppressWarnings("serial")
public class GrowingHisto extends AExample {

	private static Thread sourceThread;

	public GrowingHisto(boolean headless) {
		super(headless);
		double mu = 50.0;
		double sig = 10.0;
		NormalDistribution normDev = new NormalDistribution(mu, sig);
		addData(getPlotCanvas(), 100000, 100, normDev);
	}

	@Override
	protected PlotData createPlotData() throws PlotDataException {
		HistoData h1 = new HistoData("Histo 1", 0.0, 100.0, 50);
		return new PlotData(h1);
	}

	@Override
	protected String getXAxisLabel() {
		return "some measured value";
	}

	@Override
	protected String getYAxisLabel() {
		return "Counts";
	}

	@Override
	protected String getPlotTitle() {
		return "Growing Histogram Thread Test";
	}

	@Override
	public void fillData() {
		// no op, data added by background tread
	}

	@Override
	public void setParameters() {
		HistoCurve hc = (HistoCurve) canvas.getPlotData().getCurve(0);
		IStyled style = hc.getStyle();
		style.setFillColor(new Color(196, 196, 196, 64));
		style.setBorderColor(Color.black);

		// basic example, not fitting
		hc.setCurveMethod(CurveDrawingMethod.GAUSSIAN);
		PlotParameters params = canvas.getParameters();
		params.setMinExponentY(6);
		params.setNumDecimalY(0);
	}

	private static void addData(final PlotCanvas canvas, final long maxCount, final int increment,
			NormalDistribution normDev) {
		final HistoCurve hc = (HistoCurve) canvas.getPlotData().getCurve(0);

		final double[] x = new double[increment];

		Runnable runner = new Runnable() {
			@Override
			public void run() {
				int count = 0;
				while (count < maxCount) {
					count += increment;
					for (int i = 0; i < increment; i++) {
						x[i] = normDev.sample();
					}
					hc.addAll(x);
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};

		sourceThread = new Thread(runner);
		sourceThread.start();

	}

	@Override
	public void plotChanged(PlotChangeType type) {
		switch (type) {
		case SHUTDOWN:
			if (sourceThread != null && sourceThread.isAlive()) {
				sourceThread.interrupt();
			}
			break;
		}
	}

	public static void main(String arg[]) {

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				GrowingHisto example = new GrowingHisto(false);
				example.setVisible(true);
			}
		});

	}
}
