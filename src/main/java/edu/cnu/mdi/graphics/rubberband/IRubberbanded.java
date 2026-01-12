package edu.cnu.mdi.graphics.rubberband;

import java.awt.Point;

public interface IRubberbanded {

	/**
	 * This just signals that we are done rubber banding.
	 */
	public void doneRubberbanding();
	
	/**
	 * Approve or reject the given point as a valid rubberband endpoint.
	 * 
	 * @param p Point to evaluate
	 * @return true to approve, false to reject
	 */
	public default boolean approvePoint(Point p) {
		return true;
	}
}
