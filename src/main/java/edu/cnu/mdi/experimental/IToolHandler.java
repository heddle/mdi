package edu.cnu.mdi.experimental;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;

public interface IToolHandler {

	/**
	 * Handle pointer button rubberbanding with the given bounds.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the rubberbanding is occurring
	 * @param bounds Rectangle defining the rubberband area
	 */
	public void pointerRubberbanding(AToolBar toolBar, Component canvas, Rectangle bounds);

	/**
	 * Handle box zoom rubberbanding with the given bounds.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the rubberbanding is occurring
	 * @param bounds Rectangle defining the rubberband area
	 */
	public void boxZoomRubberbanding(AToolBar toolBar, Component canvas, Rectangle bounds);

	/**
	 * Handle the start of a pan drag operation.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the drag is occurring
	 * @param start Starting point of the drag
	 */
	public void panStartDrag(AToolBar toolBar, Component canvas, Point start);

	/**
	 * Handle an update during a pan drag operation.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas   JComponent on which the drag is occurring
	 * @param start    Starting point of the drag
	 * @param previous Previous point during the drag
	 * @param current  Current point during the drag
	 */
	public void panUpdateDrag(AToolBar toolBar, Component canvas, Point start, Point previous, Point current);
	
	/**
	 * Handle the end of a pan drag operation.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the drag is occurring
	 * @param start Starting point of the drag
	 * @param end   Ending point of the drag
	 */
	public void panDoneDrag(AToolBar toolBar, Component canvas, Point start, Point end);
	
	/**
	 * Handle the start of a magnify move operation.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the move is occurring
	 * @param p      Starting point of the move
	 */
	public void magnifyStartMove(AToolBar toolBar, Component canvas, Point start);
	
	/**
	 * Handle an update during a magnify move operation.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the move is occurring
	 * @param start  Overall tarting point of the move (not the previous point)
	 * @param p      Current point during the move
	 */
	public void magnifyUpdateMove(AToolBar toolBar, Component canvas, Point start, Point p);
	
	/**
	 * Handle the end of a magnify move operation.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the move is occurring
	 * @param start  Overall starting point of the move (not the previous point)
	 * @param end    Ending point of the move
	 */
	public void magnifyDoneMove(AToolBar toolBar, Component canvas, Point start, Point end);
	
	/**
	 * Recenter the canvas at the given point.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the recentering is occurring
	 * @param center Point to recenter the canvas at
	 */	
	public void recenter(AToolBar toolBar, Component canvas, Point center);
}
