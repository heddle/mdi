package edu.cnu.mdi.graphics.toolbar;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

public interface IToolHandler {
	
	/**
	 * Hit test at the given point on the canvas.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas  JComponent on which the hit test is occurring
	 * @param p       Point to hit test
	 * @return Object that was hit, or null if nothing was hit
	 */
	public Object hitTest(AToolBar toolBar, Component canvas, Point p);

	/**
	 * Handle pointer button rubberbanding with the given bounds.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the rubberbanding is occurring
	 * @param bounds Rectangle defining the rubberband area
	 */
	public void pointerRubberbanding(AToolBar toolBar, Component canvas, Rectangle bounds);
	
	/**
	 * Handle pointer click on an object at the given point.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the click is occurring
	 * @param p      Point where the click occurred
	 * @param obj    Object that was clicked or null if none
	 * @param e      MouseEvent that triggered the click
	 */
	public void pointerClick(AToolBar toolBar, Component canvas, Point p, Object obj, MouseEvent e);
	
	/**
	 * Handle pointer double click on an object at the given point.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the double click is occurring
	 * @param p      Point where the double click occurred
	 * @param obj    Object that was double clicked or null if none
	 * @param e      MouseEvent that triggered the click
	 */
	public void pointerDoubleClick(AToolBar toolBar, Component canvas, Point p, Object obj, MouseEvent e);
	
	
	/**
	 * Handle beginning of a drag operation on an object.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the drag is occurring
	 * @param obj    Object being dragged
	 * @param pressPoint Point where the drag started (this should be the 
	 * initial mouse press point used to check modification triggers)
	 * @param e MouseEvent that triggered the drag
	 */
	public void beginDragObject(AToolBar toolBar, Component canvas, Object obj, Point pressPoint, MouseEvent e);
	
	public void dragObjectBy(AToolBar toolBar, Component canvas, Object obj, int dx, int dy, MouseEvent e);
	
	public void endDragObject(AToolBar toolBar, Component canvas, Object obj, MouseEvent e);
	
	public boolean doNotDrag(AToolBar toolBar, Component canvas, Object obj, MouseEvent e);
	
	/**
	 * Handle box zoom rubberbanding with the given bounds.
	 * 
	 * @param gc     GestureContext for the rubberband gesture
	 * @param bounds Rectangle defining the rubberband area
	 */
	public void boxZoomRubberbanding(GestureContext gc, Rectangle bounds);

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
	 * @param e      MouseEvent that triggered the start
	 */
	public void magnifyStartMove(AToolBar toolBar, Component canvas, Point start, MouseEvent e);
	
	/**
	 * Handle an update during a magnify move operation.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the move is occurring
	 * @param start  Overall tarting point of the move (not the previous point)
	 * @param p      Current point during the move
	 * @param e      MouseEvent that triggered the update
	 */
	public void magnifyUpdateMove(AToolBar toolBar, Component canvas, Point start, Point p, MouseEvent e);
	
	/**
	 * Handle the end of a magnify move operation.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the move is occurring
	 * @param start  Overall starting point of the move (not the previous point)
	 * @param end    Ending point of the move
	 * @param e      MouseEvent that ended the move
	 */
	public void magnifyDoneMove(AToolBar toolBar, Component canvas, Point start, Point end, MouseEvent e);
	
	/**
	 * Recenter the canvas at the given point.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the recentering is occurring
	 * @param center Point to recenter the canvas at
	 */	
	public void recenter(AToolBar toolBar, Component canvas, Point center);
	
	/**
	 * Zoom in on the canvas.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the zooming is occurring
	 */
	public void zoomIn(AToolBar toolBar, Component canvas);
	
	/**
	 * Zoom out on the canvas.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the zooming is occurring
	 */
	public void zoomOut(AToolBar toolBar, Component canvas);
	
	/**
	 * Undo the last zoom operation on the canvas.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the zooming is occurring
	 */
	public void undoZoom(AToolBar toolBar, Component canvas);
	
	/**
	 * Reset the zoom on the canvas to the default state.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the zooming is occurring
	 */
	public void resetZoom(AToolBar toolBar, Component canvas);
	
	/**
	 * Edit the style of the selected objects on the canvas.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the style editing is occurring
	 */
	public void styleEdit(AToolBar toolBar, Component canvas);
	
	/**
	 * Delete the selected objects on the canvas.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the deletion is occurring
	 */
	public void delete(AToolBar toolBar, Component canvas);
	
	/**
	 * Capture an image of the canvas in a png file.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent from which the image is captured
	 */
	public void captureImage(AToolBar toolBar, Component canvas);
	
	/**
	 * Print the canvas.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent to print
	 */
	public void print(AToolBar toolBar, Component canvas);
	
	/**
	 * Create a connection between two points.
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas  JComponent on which the connection is occurring
	 * @param start Starting point of the connection
	 * @param end   Ending point of the connection
	 */
	public void createConnection(AToolBar toolBar, Component canvas, Point start, Point end);

	/**
	 * Approve or reject a connection point.
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas  JComponent on which the connection is occurring
	 * @param p Point to approve or reject
	 * @return true to approve, false to reject
	 */
	public boolean approveConnectionPoint(AToolBar toolBar, Component canvas, Point p);
	
	/**
	 * Create a rectangle with the given bounds.
	 * @param gc GestureContext for the rectangle creation gesture
	 * @param bounds Rectangle defining the bounds of the new rectangle
	 */
	public void createRectangle(GestureContext gc, Rectangle bounds);
	
	/**
	 * Create an ellipse with the given bounds.
	 * @param gc GestureContext for the ellipse creation gesture
	 * @param bounds Rectangle defining the bounds of the new ellipse
	 */
	public void createEllipse(GestureContext gc, Rectangle bounds);

	/**
	 * Create a radarc with the given bounds and vertices.
	 * @param gc GestureContext for the radarc creation gesture
	 * @param vertices Array of Points defining the vertices of the radarc.
	 * The first point is the center, the second point defines the radius,
	 * and the third point defines the arc angle.
	 */
	public void createRadArc(GestureContext gc, Point[] vertices);
}
