package edu.cnu.mdi.splot.plot;

import java.awt.Point;

/** Common interaction state for draggable plot overlays. */
public interface Draggable {

	/** @param p screen point @return whether the overlay contains the point */
	public boolean contains(Point p);

	/** @return whether a press has primed a possible drag */
	public boolean isDraggingPrimed();

	/** @return whether a drag is currently active */
	public boolean isDragging();

	/** @param primed whether a possible drag is primed */
	public void setDraggingPrimed(boolean primed);

	/** @param dragging whether a drag is active */
	public void setDragging(boolean dragging);

	/** @param p current screen point, or {@code null} */
	public void setCurrentPoint(Point p);

	/** @return defensive copy of the current screen point, or {@code null} */
	public Point getCurrentPoint();
}
