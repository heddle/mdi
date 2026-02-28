package edu.cnu.mdi.mapping;

import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D.Double;

import javax.swing.SwingUtilities;

import edu.cnu.mdi.container.BaseContainer;
import edu.cnu.mdi.graphics.text.UnicodeUtils;
import edu.cnu.mdi.hover.HoverEvent;
import edu.cnu.mdi.hover.HoverInfoWindow;
import edu.cnu.mdi.hover.HoverListener;
import edu.cnu.mdi.hover.HoverManager;

@SuppressWarnings("serial")
public class MapContainer extends BaseContainer implements HoverListener {
	
	// Optional hover info window for displaying country name
	private HoverInfoWindow hoverWindow;

	/**
	 * Create a map container with the specified world system scale. The world
	 * system scale determines how many world units correspond to one pixel in the
	 * view. A larger scale means that more of the world is visible at once (zoomed
	 * out), while a smaller scale means that less of the world is visible (zoomed
	 * in). The constructor also disables standard panning behavior, which may be
	 * handled differently in a mapping context.
	 *
	 * @param worldSystem the initial world system scale for the map container
	 */
	public MapContainer(Double worldSystem) {
		super(worldSystem);
		setStandardPanning(false);
		
		//register for hover events
		HoverManager.getInstance().registerComponent(getComponent(), this);	
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
			String latLon = String.format("%.2f%s %s, %.2f%s %s", Math.abs(Math.toDegrees(ll.y)), UnicodeUtils.DEGREE,
					(ll.y >= 0) ? "N" : "S", Math.abs(Math.toDegrees(ll.x)), UnicodeUtils.DEGREE,
					(ll.x >= 0) ? "E" : "W");

			// Update toolbar text (if present)
			_toolBar.updateStatusText(latLon);
		}

	}

	// Get the MapView2D
	private MapView2D getMapView2D() {
		return (MapView2D) getView();
	}
	

	@Override
	public void hoverUp(HoverEvent he) {
		// Convert local mouse Point to screen coordinates
		Point p = he.getLocation();
		String countryName = getMapView2D().getCountryAtPoint(p, this);
		if (countryName == null) {
			return; // No country at this point, so don't show hover info
		}
		
		SwingUtilities.convertPointToScreen(p, he.getSource());

		getHoverWindow().showMessage(countryName, p);
	}

	@Override
	public void hoverDown(HoverEvent he) {
		getHoverWindow().hideMessage();
	}
	
	// Lazily create the hover window when needed
	private HoverInfoWindow getHoverWindow() {
	    if (hoverWindow == null) {
	        Window ownerWin = SwingUtilities.getWindowAncestor(getComponent());
	        if (ownerWin == null) {
	            // Not yet realized; try again later.
	            return null;
	        }
	        hoverWindow = new HoverInfoWindow(ownerWin);
	    }
	    return hoverWindow;
	}
	
	// Clean up hover resources when container is closed
	protected void prepareForExit() {
		HoverManager.getInstance().unregisterComponent(getComponent());
		
		if (hoverWindow != null) {
			HoverManager.getInstance().unregisterComponent(hoverWindow);
			hoverWindow.dispose();
			hoverWindow = null;
		}
	}

}
