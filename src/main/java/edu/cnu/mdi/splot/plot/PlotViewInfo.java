package edu.cnu.mdi.splot.plot;

import java.util.List;

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
		       "with support for saving (plot file or PNG), printing, zooming, panning, and interactive feedback. "
		       + "The plot can be customized with titles and axis labels. "
		       +" There is extensive editing support through the edit menu.";
	}

	@Override
	public List<String> getUsageBullets() {
		
		return List.of(
				"Use the Toolbar to zoom, pan, and reset the view.",
				"Hover over data points to see details in the feedback pane.",
				"Use the Edit menu to add titles, axis labels, and customize the plot appearance.",
				"To save a plot, use the File menu. You can save as a plot file (for later editing) or as a PNG image.",
				"To open a saved plot file, simply drag-and-drop it into the plot area."
				);
	}
	
	@Override
	protected boolean isPurposeHtml() {
	    return true;
	}

}
