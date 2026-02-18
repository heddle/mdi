package edu.cnu.mdi.graphics.rubberband;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.Objects;

import edu.cnu.mdi.graphics.GraphicsUtils;

/**
 * Base class for all rubberband gestures.
 *
 * Owns:
 * - lifecycle (start/end/cancel)
 * - image buffering + overlay drawing
 * - shared points (start/current)
 * - shared poly storage (poly/tempPoly) for click-collected gestures
 *
 * Subclasses own:
 * - interaction semantics (drag vs click)
 * - shape drawing
 * - bounds + vertices + validity rules (if not area-based)
 */
public abstract class ARubberband implements MouseListener, MouseMotionListener {

	public static enum Policy {
		NONE,
		RECTANGLE,
		RECTANGLE_PRESERVE_ASPECT,
		POLYGON,
		POLYLINE,
		OVAL,
		LINE,
		RADARC,
		XONLY,
		YONLY,
		TWO_CLICK_LINE
	}

	private static boolean veryFirst = true;

	protected Color fillColor = new Color(64, 64, 128, 64);
	protected Color highlightColor1 = Color.black;
	protected Color highlightColor2 = Color.lightGray;

	protected final Policy policy;

	protected final Point startPt = new Point();
	protected final Point currentPt = new Point();

	protected final Component component;
	protected final IRubberbanded rubberbanded;

	protected boolean started = false;
	protected boolean active = false;

	/** Buffered image of the component at the start of the gesture, used for clean rubberband redraws. */
	protected BufferedImage backgroundImage;
	protected BufferedImage image;

	/** Final poly result after completion (poly policies). */
	protected Polygon poly;

	/** Working poly during collection/preview (click policies). */
	protected Polygon tempPoly;

	/**
	 * Constructor does not activate; caller must call setActive(true) to enable listening.
	 * @param component target component for mouse events and drawing
	 * @param rubberbanded callback for completion notification
	 * @param policy rubberband policy (behavior/drawing rules)
	 */
	protected ARubberband(Component component, IRubberbanded rubberbanded, Policy policy) {
		Objects.requireNonNull(component, "component");
		Objects.requireNonNull(rubberbanded, "rubberbanded");
		Objects.requireNonNull(policy, "policy");

		this.component = component;
		this.rubberbanded = rubberbanded;
		this.policy = policy;

		component.addMouseListener(this);
		component.addMouseMotionListener(this);
	}

	/** For toolbars that construct the rubberband after the press event. */
	public final void begin(Point anchorPt) {
		startRubberbanding(anchorPt);
	}

	/**
	 * Active means listening and rubberbanding in response to mouse events.
	 * @return true if this rubberband is active and responding to mouse events,
	 * false if inactive or completed (waiting for toolbar to read results).
	 */
	public final boolean isActive() {
		return active;
	}

	/**
	 * Activate or deactivate this rubberband. Deactivated rubberbands ignore mouse events and do not draw.
	 * @param b true to activate, false to deactivate. Deactivation is not the same as completion;
	 * it simply ignores mouse events and stops drawing until reactivated or completed.
	 */
	public final void setActive(boolean b) {
		active = b;
	}

	/**
	 * True if this rubberband collects points on click rather than drag. Used
	 * by toolbars to determine when to call isGestureValid() and getRubberbandVertices().
	 */
	public boolean isClickBased() {
		return false;
	}

	/**
	 * Validity rules depend on the policy; default is area-based (bounds must exceed min size).
	 * Click-based policies should override with their own rules.
	 * @param minSizePx minimum size in pixels for validity; interpretation depends on policy (e.g. max dimension for polygons).
	 * @return true if the current gesture state is valid for completion, false if it should be rejected (e.g. too small).
	 */
	public boolean isGestureValid(int minSizePx) {
		Rectangle b = getRubberbandBounds();
		return (b != null) && (b.width >= minSizePx) && (b.height >= minSizePx);
	}

	/**
	 * Get the anchor point where the gesture started. For click-based policies, this may be the first click
	 * or a synthetic point; for drag-based policies, this is the initial mouse press location.
	 * @return the start point of the gesture, which may be used as a reference for drawing and bounds calculations.
	 */
	public final Point getStart() {
		return startPt;
	}

	/**
	 * Get the current point of the gesture, which may be updated during dragging or clicking. For click-based policies,
	 * this may be the most recent click location or a synthetic point; for drag-based policies, this is the current
	 * mouse location during dragging.
	 * @return the current point of the gesture, which may be used as a reference for drawing and bounds calculations.
	 */
	public final Point getCurrent() {
		return currentPt;
	}

	/**
	 * Set the fill color for this rubberband. Used for the main shape fill; subclasses may also use it for borders or highlights.
	 * @param color the fill color to use when drawing this rubberband; may be null for no fill.
	 */
	public final void setFillColor(Color color) {
		fillColor = color;
	}

	/**
	 * Primary highlight color, used for borders or 1-color gradients.
	 */
	public final void setHighlightColor1(Color c) {
		highlightColor1 = c;
	}

	/**
	 * Secondary highlight color, used for 2-color gradients or borders.
	 */
	public final void setHighlightColor2(Color c) {
		highlightColor2 = c;
	}

	/**
	 * Cancel the rubberbanding operation. Discards results and releases resources immediately. The toolbar will not call
	 * doneRubberbanding() after this, so the rubberband should not rely on that for cleanup.
     */
	public void cancel() {
		detachListeners();

		image = null;
		backgroundImage = null;
		poly = null;
		tempPoly = null;

		active = false;
		started = false;

		component.repaint();
	}

	/**
	 * Normal completion. Preserves results long enough for the toolbar to read them
	 * in {@link IRubberbanded#doneRubberbanding()}.
	 */
	public void endRubberbanding(Point p) {

		if (p != null) {
			modifyCurrentPoint(p);
			currentPt.setLocation(p);
		}

		// Promote temp->final for click-collected gestures
		if (tempPoly != null) {
			poly = tempPoly;
			tempPoly = null;
		}

		detachListeners();

		// Clear transient drawing state, keep results.
		image = null;
		backgroundImage = null;

		active = false;
		started = false;

		rubberbanded.doneRubberbanding();
	}

	/// Remove listeners to stop receiving events and allow GC after completion or cancellation.
	private void detachListeners() {
		component.removeMouseListener(this);
		component.removeMouseMotionListener(this);
	}

	// -------------------- Drawing pipeline --------------------

	/**
	 * Update the current point and redraw the rubberband. For drag-based policies, this is called during dragging;
	 * for click-based policies, this may be called after each click or when the toolbar requests a preview update.
	 * @param newCurrentPoint
	 */
	protected final void setCurrent(Point newCurrentPoint) {

		currentPt.setLocation(newCurrentPoint);
		if (image == null) {
			return;
		}

		Graphics2D g2 = image.createGraphics();
		g2.setColor(fillColor);

		g2.drawImage(backgroundImage, 0, 0, component);
		draw(g2);
		g2.dispose();

		Graphics g = component.getGraphics();
		if (veryFirst) {
			veryFirst = false;
		} else {
			g.drawImage(image, 0, 0, component);
		}
		g.dispose();
	}

	/**
	 * Add a point to the given polygon if it's not a duplicate of the last point. Used by click-based policies to collect vertices.
	 * @param p the polygon to add the point to; must not be null
	 * @param x the x-coordinate of the point to add
	 * @param y the y-coordinate of the point to add
	 */
	protected final void addPoint(Polygon p, int x, int y) {
		int n = p.npoints;
		if (n > 0) {
			if (x == p.xpoints[n - 1] && y == p.ypoints[n - 1]) {
				return;
			}
		}
		p.addPoint(x, y);
	}

	/**
	 * Hook to modify the current point before using it.
	 * Default: clamp to component bounds (old Rubberband behavior).
	 */
	protected void modifyCurrentPoint(Point cp) {
		if (cp == null) {
			return;
		}
		Rectangle b = component.getBounds();
		cp.x = Math.max(1, Math.min(b.x + b.width - 1, cp.x));
		cp.y = Math.max(1, Math.min(b.y + b.height - 1, cp.y));
	}

	/**
	 * Initialize rubberbanding state and capture the component image for drawing. Called at the start of the gesture,
	 * either from mousePressed() for drag-based policies or from begin() for toolbar-initiated gestures.
	 * @param anchorPt the initial point of the gesture, which may be used as the start point and for initial drawing; may be null but is typically not.
	 */
	protected void startRubberbanding(Point anchorPt) {
		if (started) {
			return;
		}
		started = true;

		image = GraphicsUtils.getComponentImageBuffer(component);
		backgroundImage = GraphicsUtils.getComponentImage(component);

		startPt.setLocation(anchorPt);
		currentPt.setLocation(anchorPt);
	}

	// -------------------- Required subclass behavior --------------------

	/**
	 * Draw the rubberband shape based on the current state. Called during dragging or when the toolbar requests a preview update.
	 * Subclasses must implement this to define the visual appearance of the rubberband.
	 * @param g
	 */
	protected abstract void draw(Graphics2D g);

	/**
	 * Get the bounding rectangle of the rubberband shape based on the current state. Used for area-based validity checks and as a default for vertex calculations.
	 * Subclasses should override this if their shape is not well-represented by a bounding rectangle (e.g. lines, polygons).
	 * @return the bounding rectangle of the rubberband shape, or null if not applicable.
	 */
	public abstract Rectangle getRubberbandBounds();

	/**
	 * Get the vertices of the rubberband shape based on the current state. Used for click-based policies and by toolbars to read results.
	 * Default implementation returns the corners of the bounding rectangle, which is suitable for rectangles and ovals but not for lines or polygons.
	 * Subclasses should override this if their shape has specific vertex requirements.
	 * @return an array of points representing the vertices of the rubberband shape, or null if not applicable.
	 */
	protected Point[] computeVertices() {
		Rectangle r = getRubberbandBounds();
		if (r == null) {
			return null;
		}
		int left = r.x;
		int top = r.y;
		int right = left + r.width;
		int bottom = top + r.height;
		return new Point[] {
				new Point(left, bottom),
				new Point(left, top),
				new Point(right, top),
				new Point(right, bottom)
		};
	}

	/**
	 * Get the vertices of the rubberband shape for click-based policies. For drag-based policies, this returns the
	 * corners of the bounding rectangle by default.
	 * @return an array of points representing the vertices of the rubberband shape, or null if not applicable.
	 * For click-based policies, this should return the collected vertices; for drag-based policies, this returns
	 * the corners of the bounding rectangle by default.
	 */
	public final Point[] getRubberbandVertices() {
		return computeVertices();
	}

	// -------------------- Default no-op listener methods --------------------

	@Override public void mouseClicked(MouseEvent e) { }
	@Override public void mousePressed(MouseEvent e) { }
	@Override public void mouseReleased(MouseEvent e) { }
	@Override public void mouseEntered(MouseEvent e) { }
	@Override public void mouseExited(MouseEvent e) { }
	@Override public void mouseDragged(MouseEvent e) { }
	@Override public void mouseMoved(MouseEvent e) { }
}
