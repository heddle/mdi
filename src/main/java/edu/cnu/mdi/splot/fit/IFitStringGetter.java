package edu.cnu.mdi.splot.fit;

/** Supplies human-readable model and parameter labels for fit results. */
public interface IFitStringGetter {

	/** @return model name suitable for a legend or report */
	public String modelName();

	/** @return mathematical function form suitable for display */
	public String functionForm();

	/**
	 * Return the display name of a fitted parameter.
	 * @param index zero-based parameter index
	 * @return parameter name
	 */
	public String parameterName(int index);
}
