package edu.cnu.mdi.mapping;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D.Double;

import edu.cnu.mdi.container.BaseContainer;
import edu.cnu.mdi.graphics.text.UnicodeSupport;

@SuppressWarnings("serial")
public class MapContainer extends BaseContainer {

	public MapContainer(Double worldSystem) {
		super(worldSystem);
		setStandardPanning(false);
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
			((MercatorProjection) mp).setCentralLongitude(ll.x);
			break;

		case MOLLWEIDE:
			((MollweideProjection) mp).setCentralLongitude(ll.x);
			break;

		case ORTHOGRAPHIC:
			((OrthographicProjection) mp).setCenter(ll.x, ll.y);
			break;

		case LAMBERT_EQUAL_AREA:
			((LambertEqualAreaProjection) mp).setCenter(ll.x, ll.y);
			break;
		}

		getMapView2D().invalidate();
		setDirty(true);
		refresh();

	}

	/**
	 * Convert local point to lat/lon point
	 * 
	 * @param pp the local point
	 * @param ll the lat/lon point
	 */
	public void localToLatLon(Point pp, Point2D.Double ll) {
		Point2D.Double wp = new Point2D.Double();
		localToWorld(pp, wp);
		getMapView2D().getProjection().latLonFromXY(ll, wp);
	}

	/**
	 * Convert world point to lat/lon point
	 * 
	 * @param ll the lat/lon point
	 * @param wp the world point
	 */
	public void worldToLatLon(Point2D.Double ll, Point2D.Double wp) {
		getMapView2D().getProjection().latLonFromXY(ll, wp);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void feedbackTrigger(MouseEvent mouseEvent, boolean dragging) {
		Point2D.Double wp = getLocation(mouseEvent);

		if (_feedbackControl != null) {
			_feedbackControl.updateFeedback(mouseEvent, wp, dragging);
		}

		if (_toolBar != null) {
			Point2D.Double ll = new Point2D.Double();
			worldToLatLon(ll, wp);
			String latLon = String.format("%.2f%s %s, %.2f%s %s", Math.abs(Math.toDegrees(ll.y)), UnicodeSupport.DEGREE,
					(ll.y >= 0) ? "N" : "S", Math.abs(Math.toDegrees(ll.x)), UnicodeSupport.DEGREE,
					(ll.x >= 0) ? "E" : "W");

			// Update toolbar text (if present)
			_toolBar.updateStatusText(latLon);
		}

	}

	// Get the MapView2D
	private MapView2D getMapView2D() {
		return (MapView2D) getView();
	}

}
