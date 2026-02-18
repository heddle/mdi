package edu.cnu.mdi.graphics.toolbar;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Callback interface for toolbar tools.
 * <p>
 * This interface is intentionally "gesture-first": wherever a tool is driven by
 * a pointer gesture (press/drag/release, rubberbanding, hover move), the callback
 * receives a {@link GestureContext}. This keeps signatures uniform and makes it
 * easy to extend tools without proliferating parameter combinations.
 * </p>
 *
 * <h2>Hit testing</h2>
 * <p>
 * The handler provides hit-testing so applications can map canvas points to
 * domain objects (items, nodes, handles, etc.).
 * </p>
 *
 * <h2>One-shot actions</h2>
 * <p>
 * Even one-shot actions (zoom in/out, print, delete...) receive a {@link GestureContext}
 * so handlers always have access to toolbar, canvas, and a representative point.
 * </p>
 */
public interface IToolHandler {

    // --------------------------------------------------------------------
    // Hit testing
    // --------------------------------------------------------------------

    /**
     * Hit test at a point on the canvas.
     *
     * @param gc gesture context providing toolbar/canvas (non-null)
     * @param p  point to test in canvas coordinates (non-null)
     * @return hit object or null
     */
    Object hitTest(GestureContext gc, Point p);

    // --------------------------------------------------------------------
    // Pointer tool (click + rubberband select + drag-move)
    // --------------------------------------------------------------------

    /**
     * Pointer rubberband feedback (typically multi-select or region selection).
     *
     * @param gc     gesture context (non-null)
     * @param bounds rubberband bounds (non-null)
     */
    void pointerRubberbanding(GestureContext gc, Rectangle bounds);

    /**
     * Pointer click (single click).
     *
     * @param gc gesture context (non-null). {@code gc.getTarget()} may be null.
     */
    void pointerClick(GestureContext gc);

    /**
     * Pointer double-click.
     *
     * @param gc gesture context (non-null). {@code gc.getTarget()} may be null.
     */
    void pointerDoubleClick(GestureContext gc);

    /**
     * Begin dragging an object/selection.
     *
     * @param gc gesture context (non-null). {@code gc.getTarget()} is the drag target.
     */
    void beginDragObject(GestureContext gc);

    /**
     * Drag the object/selection by a pixel delta.
     *
     * @param gc gesture context (non-null)
     * @param dx delta x since last update
     * @param dy delta y since last update
     */
    void dragObjectBy(GestureContext gc, int dx, int dy);

    /**
     * End dragging an object/selection.
     *
     * @param gc gesture context (non-null)
     */
    void endDragObject(GestureContext gc);

    /**
     * Optional veto before starting a drag.
     *
     * @param gc gesture context (non-null)
     * @return true to veto drag start, false to allow
     */
    boolean doNotDrag(GestureContext gc);

    // --------------------------------------------------------------------
    // Box zoom
    // --------------------------------------------------------------------

    /**
     * Box-zoom rubberband completion (or continuous feedback depending on tool).
     *
     * @param gc     gesture context (non-null)
     * @param bounds rubberband bounds (non-null)
     */
    void boxZoomRubberbanding(GestureContext gc, Rectangle bounds);

    // --------------------------------------------------------------------
    // Pan (press-drag-release)
    // --------------------------------------------------------------------

    /** @param gc gesture context (non-null) */
    void panStartDrag(GestureContext gc);

    /** @param gc gesture context (non-null) */
    void panUpdateDrag(GestureContext gc);

    /** @param gc gesture context (non-null) */
    void panDoneDrag(GestureContext gc);

    // --------------------------------------------------------------------
    // Magnify (hover move)
    // --------------------------------------------------------------------

    /** @param gc gesture context (non-null) */
    void magnifyStartMove(GestureContext gc);

    /** @param gc gesture context (non-null) */
    void magnifyUpdateMove(GestureContext gc);

    /** @param gc gesture context (non-null) */
    void magnifyDoneMove(GestureContext gc);

    // --------------------------------------------------------------------
    // Recenter
    // --------------------------------------------------------------------

    /** @param gc gesture context (non-null) */
    void recenter(GestureContext gc);

    // --------------------------------------------------------------------
    // Zoom / view actions (one-shot)
    // --------------------------------------------------------------------

    /** @param gc context (non-null) */
    void zoomIn(GestureContext gc);

    /** @param gc context (non-null) */
    void zoomOut(GestureContext gc);

    /** @param gc context (non-null) */
    void undoZoom(GestureContext gc);

    /** @param gc context (non-null) */
    void resetZoom(GestureContext gc);

    // --------------------------------------------------------------------
    // Style / delete / capture / print (one-shot)
    // --------------------------------------------------------------------

    /** @param gc context (non-null) */
    void styleEdit(GestureContext gc);

    /** @param gc context (non-null) */
    void delete(GestureContext gc);

    /** @param gc context (non-null) */
    void info(GestureContext gc);

    /** @param gc context (non-null) */
    void captureImage(GestureContext gc);

    /** @param gc context (non-null) */
    void print(GestureContext gc);

    // --------------------------------------------------------------------
    // Connections
    // --------------------------------------------------------------------

    /**
     * Create a connection between two points.
     *
     * @param gc    gesture context (non-null)
     * @param start start point (non-null)
     * @param end   end point (non-null)
     */
    void createConnection(GestureContext gc, Point start, Point end);

    /**
     * Approve or reject a connection point.
     *
     * @param gc gesture context (non-null)
     * @param p  point to approve/reject (non-null)
     * @return true to approve
     */
    boolean approveConnectionPoint(GestureContext gc, Point p);

    // --------------------------------------------------------------------
    // Shape creation via rubberband tools
    // --------------------------------------------------------------------

    /**
     * Create a rectangle.
     * @param gc gesture context (non-null)
     * @param bounds rectangle bounds (non-null)
     */
    void createRectangle(GestureContext gc, Rectangle bounds);

    /**
     * Create an ellipse.
     * @param gc gesture context (non-null)
     * @param bounds ellipse bounds (non-null)
     */
    void createEllipse(GestureContext gc, Rectangle bounds);

    /**
     * Create a radial arc.
     * <p>
     * The expected interpretation of vertices is tool-defined; for a typical
     * radial arc gesture:
     * </p>
     * <ul>
     *   <li>vertices[0] = center</li>
     *   <li>vertices[1] = radius leg endpoint</li>
     *   <li>vertices[2] = angle-defining point</li>
     * </ul>
     *
     * @param gc       gesture context (non-null)
     * @param vertices gesture vertices (non-null)
     */
    void createRadArc(GestureContext gc, Point[] vertices);

    /**
     * Create a polygon.
     * @param gc gesture context (non-null)
     * @param vertices polygon vertices (non-null)
     */
    void createPolygon(GestureContext gc, Point[] vertices);

    /**
	 * Create a polyline.
	 * @param gc gesture context (non-null)
	 * @param vertices polyline vertices (non-null)
	 */
    void createPolyline(GestureContext gc, Point[] vertices);

    /**
	 * Create a line between two points.
	 *
	 * @param gc gesture context (non-null)
	 * @param start start point
	 * @param end end point
	 */
    void createLine(GestureContext gc, Point start, Point end);

    /**
     * Create a text item at the given location.
     *
     * @param gc
     * @param location
     */
    void createTextItem(Point location);
}
