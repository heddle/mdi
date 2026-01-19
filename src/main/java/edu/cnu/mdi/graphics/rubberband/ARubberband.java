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

	protected BufferedImage backgroundImage;
	protected BufferedImage image;

	/** Final poly result after completion (poly policies). */
	protected Polygon poly;

	/** Working poly during collection/preview (click policies). */
	protected Polygon tempPoly;

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

	public final boolean isActive() {
		return active;
	}

	public final void setActive(boolean b) {
		active = b;
	}

	/** True for click-collection rubberbands (toolbars forward the creation press). */
	public boolean isClickBased() {
		return false;
	}

	/**
	 * Completion validity test used by toolbars.
	 * Default: "area gesture" semantics.
	 */
	public boolean isGestureValid(int minSizePx) {
		Rectangle b = getRubberbandBounds();
		return (b != null) && (b.width >= minSizePx) && (b.height >= minSizePx);
	}

	public final Point getStart() {
		return startPt;
	}

	public final Point getCurrent() {
		return currentPt;
	}

	public final void setFillColor(Color color) {
		fillColor = color;
	}

	public final void setHighlightColor1(Color c) {
		highlightColor1 = c;
	}

	public final void setHighlightColor2(Color c) {
		highlightColor2 = c;
	}

	/** Cancel without notifying callback; clears results. */
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

	private void detachListeners() {
		component.removeMouseListener(this);
		component.removeMouseMotionListener(this);
	}

	// -------------------- Drawing pipeline --------------------

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

	/** Prevent adding the same point consecutively. */
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
		if (cp == null) return;
		Rectangle b = component.getBounds();
		cp.x = Math.max(1, Math.min(b.x + b.width - 1, cp.x));
		cp.y = Math.max(1, Math.min(b.y + b.height - 1, cp.y));
	}

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

	protected abstract void draw(Graphics2D g);

	public abstract Rectangle getRubberbandBounds();

	/** Default vertices are bounds corners; subclasses override for line/poly. */
	protected Point[] computeVertices() {
		Rectangle r = getRubberbandBounds();
		if (r == null) return null;
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
