package edu.cnu.mdi.graphics.toolbar.tool;

import java.awt.Point;

import edu.cnu.mdi.graphics.rubberband.Rubberband;
import edu.cnu.mdi.graphics.toolbar.DrawingToolSupport;
import edu.cnu.mdi.graphics.toolbar.ToolController;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.Layer;

/**
 * Tool that creates a {@link edu.cnu.mdi.item.RadArcItem} by rubber-banding a
 * 3-point "radius-arc" gesture on a {@link DrawingToolSupport}.
 * <p>
 * This replaces the legacy {@code RadArcButton}. The RADARC rubberband policy
 * yields exactly three screen points:
 * </p>
 * <ol>
 * <li>center</li>
 * <li>radius/start-angle point (defines radius and start angle)</li>
 * <li>opening-angle point (relative to point 2, defines arc angle)</li>
 * </ol>
 * <p>
 * The arc angle is computed using the angle between the two radius vectors and
 * a 2D cross product to determine sign, matching the legacy logic.
 * </p>
 *
 * <p>
 * Post-creation behavior (clear selection, reset to default tool, refresh) is
 * handled by {@link AbstractVertexRubberbandTool}.
 * </p>
 *
 * @author heddle
 */
public class RadArcTool extends AbstractVertexRubberbandTool {

	/** Tool id used by {@link ToolController}. */
	public static final String ID = "radArc";

	/**
	 * Minimum radius in pixels required to accept the gesture (legacy: < 3 abort).
	 */
	private static final int MIN_RADIUS_PX = 3;

	/** Construct the tool (requires exactly 3 vertices). */
	public RadArcTool() {
		super(3);
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public String toolTip() {
		return "Create a radius-arc";
	}

	@Override
	protected Rubberband.Policy rubberbandPolicy() {
		return Rubberband.Policy.RADARC;
	}

	@Override
	protected AItem createItem(Layer layer, Point[] pp) {

		// Defensive (base guarantees length>=3, but RADARC expects exactly 3)
		if (pp == null || pp.length != 3) {
			return null;
		}

		// Unpack points (screen coords)
		double xc = pp[0].x;
		double yc = pp[0].y;

		double x1 = pp[1].x;
		double y1 = pp[1].y;

		double x2 = pp[2].x;
		double y2 = pp[2].y;

		// Vectors from center
		double dx1 = x1 - xc;
		double dy1 = y1 - yc;
		double dx2 = x2 - xc;
		double dy2 = y2 - yc;

		double r1 = Math.sqrt(dx1 * dx1 + dy1 * dy1);
		double r2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);

		// Legacy guard
		if (r1 < 0.99 || r2 < 0.99) {
			return null;
		}

		// Angle between radius vectors (clamped for numeric safety)
		double cos = (dx1 * dx2 + dy1 * dy2) / (r1 * r2);
		cos = Math.max(-1.0, Math.min(1.0, cos));
		double aAngle = Math.acos(cos);

		// Sign using 2D cross product (matches legacy sign logic)
		if ((dx1 * dy2 - dx2 * dy1) > 0.0) {
			aAngle = -aAngle;
		}

		int arcAngleDeg = (int) Math.toDegrees(aAngle);
		int pixrad = (int) r1;

		if (pixrad < MIN_RADIUS_PX) {
			return null;
		}

		return DrawingToolSupport.createRadArcItem(layer, pp[0], pp[1], arcAngleDeg);
	}

	@Override
	protected void configureItem(AItem item) {
		item.setRightClickable(true);
		item.setDraggable(true);
		item.setRotatable(true);
		item.setResizable(true);
		item.setDeletable(true);
		item.setLocked(false);
	}
}
