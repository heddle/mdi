package edu.cnu.mdi.mapping;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;

import edu.cnu.mdi.container.BaseContainer;

public class MapContainer extends BaseContainer {
	
	public MapContainer(Double worldSystem) {
		super(worldSystem);
	}


	/**
	 * {@inheritDoc}
     */
	@Override
	public void pan(int dh, int dv) {

		Rectangle r = getBounds();
		int xc = r.width / 2;
		int yc = r.height / 2;

		xc -= dh;
		yc -= dv;

		Point p = new Point(xc, yc);
		recenter(p);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void recenter(Point pp) {
		
		IMapProjection mp = getMapView2D().getProjection();
		EProjection proj = mp.getProjection();
		Point2D.Double wp = new Point2D.Double();
		Point2D.Double ll = new Point2D.Double();
		localToWorld(pp, wp);
		mp.latLonFromXY(ll, wp);

		switch (proj) {
		case MERCATOR:
			((MercatorProjection)mp).setCentralLongitude(ll.x);
			getMapView2D().invalidate();
			setDirty(true);
			refresh();
			break;
			
		case MOLLWEIDE:
			break;
			
		case ORTHOGRAPHIC:
			break;
			
		case LAMBERT_EQUAL_AREA:
			break;
		}
		

	}

	/**
	 * {@inheritDoc}
     */
	private void recenter(Rectangle2D.Double wr, Point2D.Double newCenter) {
		wr.x = newCenter.x - wr.width / 2.0;
		wr.y = newCenter.y - wr.height / 2.0;
	}
	
	private MapView2D getMapView2D() {
		return (MapView2D) getView();
	}

}
