package edu.cnu.mdi.item;

import java.awt.Point;
import java.awt.geom.Point2D;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.world.WorldGraphicsUtils;
import edu.cnu.mdi.util.Point2DSupport;

public class RadArcItem extends PolygonItem {

	private Point2D.Double _center;
	private Point2D.Double _leg; // endpoint of first radius leg (defines radius + direction)
	private double _arcAngle; // signed, degrees

	public RadArcItem(Layer layer, Point2D.Double wpc, Point2D.Double wp1, double arcAngle) {
		super(layer, null);
		_center = (Point2D.Double) wpc.clone();
		_leg = (Point2D.Double) wp1.clone();
		_arcAngle = arcAngle;
		rebuildPath();
	}

	private void rebuildPath() {
		_path = WorldGraphicsUtils.worldPolygonToPath(WorldGraphicsUtils.getRadArcPoints(_center, _leg, _arcAngle));
		setFocus(_center);
		setAzimuth(Point2DSupport.azimuth(_center, _leg) - _arcAngle / 2.0);
	}

	@Override
	protected void updateFocus() {
		setFocus(_center);
	}

	@Override
	protected void reshape() {
		if (_modification == null) {
			return;
		}

		int idx = _modification.getSelectIndex(); // 0=leg handle, 1=arc-end handle (matches your selection points)
		Point2D.Double wp = _modification.getCurrentWorldPoint();
		if (wp == null) {
			return;
		}

		if (idx == 0) {
			// Dragging the first leg endpoint: change radius and direction
			_leg = (Point2D.Double) wp.clone();
		} else {
			// Dragging arc end: change arc angle, keeping the first leg fixed
			Point2D.Double v1 = Point2DSupport.pointDelta(_center, _leg);
			Point2D.Double v2 = Point2DSupport.pointDelta(_center, wp);

			double dot = v1.x * v2.x + v1.y * v2.y;
			double cross = Point2DSupport.cross(v1, v2);
			_arcAngle = Math.toDegrees(Math.atan2(cross, dot)); // signed (-180,180]
			// If you want allow >180 sweeps, keep your unwrap logic from earlier here.
		}

		rebuildPath();
	}

	@Override
	public Point[] getSelectionPoints(IContainer container) {
		if (_path == null || container == null) {
			return null;
		}

		Point2D.Double[] wp = WorldGraphicsUtils.pathToWorldPolygon(_path);
		if (wp == null || wp.length < 3) {
			return null;
		}

		// Two handles: first leg endpoint and last arc endpoint
		Point[] pp = new Point[2];
		pp[0] = new Point();
		container.worldToLocal(pp[0], wp[1]);
		pp[1] = new Point();
		container.worldToLocal(pp[1], wp[wp.length - 1]);
		return pp;
	}

	@Override
	public Point getRotatePoint(IContainer container) {
		if (!isRotatable() || _path == null || container == null) {
			return null;
		}

		Point2D.Double[] wp = WorldGraphicsUtils.pathToWorldPolygon(_path);
		if (wp == null || wp.length < 3) {
			return null;
		}

		int mid = 1 + (wp.length - 2) / 2; // roughly halfway along the arc boundary
		Point p = new Point();
		container.worldToLocal(p, wp[mid]);
		return p;
	}

}
