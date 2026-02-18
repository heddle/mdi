package edu.cnu.mdi.splot.example;

import edu.cnu.mdi.splot.pdata.Histo2DData;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;

@SuppressWarnings("serial")
public class Heatmap extends AExample {

	public Heatmap(boolean headless) {
		super(headless);
	}


	@Override
	protected PlotData createPlotData() throws PlotDataException {
		Histo2DData h2d = new Histo2DData("Heatmap 1", 0.0, 100.0, 50, 0.0, 100.0, 50);
		return new PlotData(h2d);
	}

	@Override
	protected String getXAxisLabel() {
		return "X Axis";
	}

	@Override
	protected String getYAxisLabel() {
		return "Y Axis";
	}

	@Override
	protected String getPlotTitle() {
		return "Sample 2D Histogram (Heatmap)";
	}

	@Override
	public void fillData() {

	    // however you get your H2D backing store:
	    // If you have it directly as a field, use that.
	    // Otherwise:
	    Histo2DData h2 = canvas.getPlotData().getHisto2DData();

	    final java.util.Random rand = new java.util.Random(12345);

	    final int N = 350_000;

	    for (int i = 0; i < N; i++) {

	        double x, y;
	        double r = rand.nextDouble();

	        if (r < 0.45) {
	            // Blob 1: tilted Gaussian near (30, 70)
	            double gx = rand.nextGaussian();
	            double gy = rand.nextGaussian();
	            x = 30 + 10 * gx;
	            y = 70 +  6 * gy + 0.35 * (x - 30);

	        } else if (r < 0.80) {
	            // Blob 2: compact Gaussian near (70, 30)
	            x = 70 + 6 * rand.nextGaussian();
	            y = 30 + 6 * rand.nextGaussian();

	        } else if (r < 0.98) {
	            // Ridge: y = 50 + 18*sin(2π x / 60) with modest noise
	            x = 100 * rand.nextDouble();
	            y = 50 + 18 * Math.sin((2.0 * Math.PI / 60.0) * x) + 2.5 * rand.nextGaussian();

	        } else {
	            // Light uniform background
	            x = 100 * rand.nextDouble();
	            y = 100 * rand.nextDouble();
	        }

	        // Clamp to [0,100] so we always contribute to bins
	        if (x < 0) {
				x = 0;
			} else if (x > 100) {
				x = 100;
			}

	        if (y < 0) {
				y = 0;
			} else if (y > 100) {
				y = 100;
			}

	        h2.fill(x, y); // or whatever your "increment bin for (x,y)" call is
	    }

	    canvas.repaint();
	}


	@Override
	public void setParameters() {
	}

	// ---------------------------------------------------------------
	public static void main(String arg[]) {

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				Heatmap example = new Heatmap(false);
				example.setVisible(true);
			}
		});

	}

}
