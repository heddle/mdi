package edu.cnu.mdi.item;

import java.awt.Point;
import java.awt.geom.Point2D;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.world.WorldGraphicsUtils;
import edu.cnu.mdi.util.Point2DSupport;

public class RadArcItem extends PolygonItem {

	private Point2D.Double _center;
	private Point2D.Double _leg; // endpoint of first radius leg (defines radius + direction)

	/** Arc opening angle COUNTERCLOCKWISE in degrees, in [0, 360). */
	private double _arcAngle;

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

	    int idx = _modification.getSelectIndex(); // 0=leg, 1=arc end
	    Point2D.Double wp = _modification.getCurrentWorldPoint();
	    if (wp == null) {
	        return;
	    }

	    if (idx == 0) {
	        _leg = (Point2D.Double) wp.clone();
	    } else {
	        // Compute minor signed measurement (-180,180]
	        double signedNow = WorldGraphicsUtils.signedSweepDeg(_center, _leg, wp);

	        // Unwrap to keep continuity and allow >180 during drag
	        _arcAngle = WorldGraphicsUtils.unwrapSweepDeg(_arcAngle, signedNow);
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

		int mid = 1 + (wp.length - 2) / 2;
		Point p = new Point();
		container.worldToLocal(p, wp[mid]);
		return p;
	}
}
