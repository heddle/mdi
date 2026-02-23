package edu.cnu.mdi.splot.plot;

import edu.cnu.mdi.view.AbstractViewInfo;

/**
 *  View info for the plot view. This provides metadata about the view, including its title, purpose, and usage instructions.
 */
public class PlotViewInfo extends AbstractViewInfo {

	@Override
	public String getTitle() {
		return "Plot View";
	}

	@Override
	public String getPurpose() {
		return "This view is designed to display 2D plots of datasets. It provides a simple interface for "
				+ "visualizing data points, " +
		       "with support for zooming, panning, and interactive feedback. "
		       + "The plot can be customized with titles and axis labels. "
		       +" There is extensive editing support through the edit menu.";
	}

	@Override
	public String getUsage() {
		return "This view displays a 2D plot of the provided dataset. " +
		       "Use the toolbar to zoom, pan, and reset the view. " +
		       "Hover over data points to see details in the feedback pane."
			       + "There is a built in curve fitting capability.";
	}
	
	@Override
	protected boolean isPurposeHtml() {
	    return true;
	}

}
