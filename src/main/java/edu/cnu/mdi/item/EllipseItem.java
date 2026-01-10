package edu.cnu.mdi.item;

import java.awt.Point;
import java.awt.geom.Point2D;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.world.WorldGraphicsUtils;

/**
 * An ellipse item represented internally as a {@link java.awt.geom.Path2D}
 * polygonal approximation.
 * <p>
 * Key invariants:
 * <ul>
 * <li>The ellipse center is the item's focus (world coordinates).</li>
 * <li>Width/height are stored as full extents in world units.</li>
 * <li>Selection handles are the four cardinal points of the ellipse in its
 * rotated frame (top/right/bottom/left).</li>
 * </ul>
 * </p>
 * <p>
 * Resize correctness after rotation depends on using the same azimuth
 * convention everywhere. In this codebase, azimuth behaves like a compass
 * bearing: <b>0° = north (+y), 90° = east (+x)</b>, increasing clockwise. That
 * convention is used by
 * {@link WorldGraphicsUtils#project(Point2D.Double, double, double, Point2D.Double)}.
 * </p>
 */
public class EllipseItem extends PolygonItem {

	/** Default number of segments used to approximate the ellipse path. */
	private static final int DEFAULT_SEGMENTS = 36;

	/** Lower bound on width/height to avoid degeneracy. */
	private static final double MIN_SIZE = 1.0e-9;

	/** Full width in world units. */
	private double w;

	/** Full height in world units. */
	private double h;

	/**
	 * Create an ellipse.
	 *
	 * @param layer   owning layer.
	 * @param w       full width in world units (clamped to {@link #MIN_SIZE}).
	 * @param h       full height in world units (clamped to {@link #MIN_SIZE}).
	 * @param azimuth rotation in degrees, using the application's compass/bearing
	 *                convention.
	 * @param center  ellipse center in world coordinates (copied). If {@code null},
	 *                the ellipse path is null until focus is set and
	 *                {@link #rebuildPath()} is called.
	 */
	public EllipseItem(Layer layer, double w, double h, double azimuth, Point2D.Double center) {
		super(layer, null);
		this.w = Math.max(MIN_SIZE, w);
		this.h = Math.max(MIN_SIZE, h);
		setAzimuth(azimuth);
		setFocus(center == null ? null : (Point2D.Double) center.clone());
		rebuildPath();
	}

	/**
	 * Rebuild the polygonal path approximation from the current size/azimuth and
	 * focus (center).
	 * <p>
	 * Some superclasses compute focus from path centroid; for ellipses we define
	 * focus == center, so we restore focus after setting the path.
	 * </p>
	 */
	private void rebuildPath() {
		Point2D.Double center = getFocus();
		if (center == null) {
			_path = null;
			return;
		}

		setPath(WorldGraphicsUtils.getEllipsePoints(w, h, getAzimuth(), DEFAULT_SEGMENTS, center));

		// Ensure focus remains exactly the center (not a computed centroid).
		setFocus(center);
	}

	/**
	 * Ellipse-specific reshape: update width/height based on which selection handle
	 * is dragged.
	 * <p>
	 * Handles are:
	 * <ul>
	 * <li>0 = top</li>
	 * <li>1 = right</li>
	 * <li>2 = bottom</li>
	 * <li>3 = left</li>
	 * </ul>
	 * </p>
	 * <p>
	 * Important implementation details for stable dragging after rotation:
	 * <ul>
	 * <li>Use the <b>start</b> focus as the center for the entire drag to avoid
	 * drift.</li>
	 * <li>Project into ellipse-local coordinates using the same <b>bearing
	 * azimuth</b> convention as {@link WorldGraphicsUtils#project}.</li>
	 * </ul>
	 * </p>
	 */
	@Override
	protected void reshape() {
		if (_modification == null) {
			return;
		}

		// Use start focus (world) as stable center during the entire resize.
		Point2D.Double center = _modification.getStartFocus();
		if (center == null) {
			center = getFocus();
		}
		if (center == null) {
			return;
		}

		int j = _modification.getSelectIndex(); // 0..3
		Point2D.Double wp = _modification.getCurrentWorldPoint();
		if (wp == null) {
			return;
		}

		// World delta from center to current mouse world point.
		double dx = wp.x - center.x;
		double dy = wp.y - center.y;

		/*
		 * Convert world delta into ellipse-local coordinates using bearing convention:
		 *
		 * Bearing azimuth β: unit vector at bearing β is (sinβ, cosβ) in (x,y).
		 *
		 * In our handle placement: localY axis (semi-height b) points along bearing
		 * azimuth (top/bottom), localX axis (semi-width a) points along bearing azimuth
		 * + 90° (right/left).
		 */
		double beta = Math.toRadians(getAzimuth());
		double sinB = Math.sin(beta);
		double cosB = Math.cos(beta);

		// Projection onto local axes:
		// localY along β: (sinβ, cosβ)
		double localY = dx * sinB + dy * cosB;

		// localX along β+90: (cosβ, -sinβ)
		double localX = dx * cosB - dy * sinB;

		// Update width/height based on the active handle.
		if (j == 0 || j == 2) {
			h = Math.max(MIN_SIZE, 2.0 * Math.abs(localY));
		} else {
			w = Math.max(MIN_SIZE, 2.0 * Math.abs(localX));
		}

		// Ensure the ellipse center stays at the resize center.
		setFocus((Point2D.Double) center.clone());

		rebuildPath();
	}

	/**
	 * Return the four selection handles in local (screen) coordinates.
	 *
	 * @param container the container for world→local conversion.
	 * @return a 4-element array of local points, or {@code null} if unavailable.
	 */
	@Override
	public Point[] getSelectionPoints(IContainer container) {
		if (container == null) {
			return null;
		}

		Point2D.Double center = getFocus();
		if (center == null) {
			return null;
		}

		double a = w / 2.0; // semi-width
		double b = h / 2.0; // semi-height

		Point2D.Double top = new Point2D.Double();
		Point2D.Double right = new Point2D.Double();
		Point2D.Double bottom = new Point2D.Double();
		Point2D.Double left = new Point2D.Double();

		double az = getAzimuth();
		WorldGraphicsUtils.project(center, b, 0.0 + az, top);
		WorldGraphicsUtils.project(center, a, 90.0 + az, right);
		WorldGraphicsUtils.project(center, b, 180.0 + az, bottom);
		WorldGraphicsUtils.project(center, a, 270.0 + az, left);

		Point[] pp = new Point[4];
		pp[0] = new Point();
		container.worldToLocal(pp[0], top);
		pp[1] = new Point();
		container.worldToLocal(pp[1], right);
		pp[2] = new Point();
		container.worldToLocal(pp[2], bottom);
		pp[3] = new Point();
		container.worldToLocal(pp[3], left);
		return pp;
	}

	/**
	 * Programmatically set ellipse size (full extents), clamped to
	 * {@link #MIN_SIZE}.
	 *
	 * @param w new full width in world units.
	 * @param h new full height in world units.
	 */
	public void setSize(double w, double h) {
		this.w = Math.max(MIN_SIZE, w);
		this.h = Math.max(MIN_SIZE, h);
		rebuildPath();
	}

	/** @return full width in world units. */
	public double getWidthWorld() {
		return w;
	}

	/** @return full height in world units. */
	public double getHeightWorld() {
		return h;
	}
}
