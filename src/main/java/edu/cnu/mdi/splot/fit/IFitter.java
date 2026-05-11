package edu.cnu.mdi.splot.fit;

public interface IFitter {
	/**
	 * Fits a curve to the given data points, returning the fit result.
	 *
	 * @param x the x values of the data points
	 * @param y the y values of the data points
	 * @return the fit result containing the fitted curve and parameters
	 */
	FitResult fit(double[] x, double[] y);

	/**
	 * Fits a curve to the given data points with weights, returning the fit result.
	 *
	 * @param x the x values of the data points
	 * @param y the y values of the data points
	 * @param weights the weights for each data point
	 * @return the fit result containing the fitted curve and parameters
	 */
	FitResult fit(double[] x, double[] y, double[] weights);
}
