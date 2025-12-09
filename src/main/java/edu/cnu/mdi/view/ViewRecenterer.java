package edu.cnu.mdi.view;

import java.awt.Point;

public interface ViewRecenterer {

	/**
	 * Custom recentering of the view to the specified point.
	 * @param pp probably a mouse click
	 */
	public void recenterView(Point pp);
}
