package edu.cnu.mdi.splot.pdata;

/** Checked exception raised when a plot dataset cannot be constructed. */
@SuppressWarnings("serial")
public class PlotDataException extends Exception {

	/**
	 * Create an exception with an explanatory message.
	 * @param message failure description
	 */
	public PlotDataException(String message) {
		super(message);
	}
}
