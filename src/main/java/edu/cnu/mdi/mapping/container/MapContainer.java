package edu.cnu.mdi.mapping.container;

import java.awt.Point;
import java.awt.Window;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import edu.cnu.mdi.container.BaseContainer;
import edu.cnu.mdi.container.BaseToolHandler;
import edu.cnu.mdi.hover.HoverEvent;
import edu.cnu.mdi.hover.HoverInfoWindow;
import edu.cnu.mdi.hover.HoverListener;
import edu.cnu.mdi.hover.HoverManager;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.mapping.MapView2D;
import edu.cnu.mdi.mapping.item.MapMilSymbolItem;
import edu.cnu.mdi.mapping.milsym.MilSymbolDescriptor;
import edu.cnu.mdi.mapping.milsym.MilSymbolTransferable;
import edu.cnu.mdi.mapping.projection.EProjection;
import edu.cnu.mdi.mapping.projection.IMapProjection;
import edu.cnu.mdi.mapping.projection.LambertEqualAreaProjection;
import edu.cnu.mdi.mapping.projection.MercatorProjection;
import edu.cnu.mdi.mapping.projection.MollweideProjection;
import edu.cnu.mdi.mapping.projection.OrthographicProjection;
import edu.cnu.mdi.util.UnicodeUtils;

/**
 * Map-specific container supporting projection-aware recentering, geographic
 * coordinate conversion, country-name hover popups, and drag-and-drop placement
 * of military symbols.
 */
@SuppressWarnings("serial")
public class MapContainer extends BaseContainer implements HoverListener {

	/** Lazily-created popup window used for hover country names. */
	private HoverInfoWindow hoverWindow;

	/**
	 * Creates a map container with the given initial world coordinate system.
	 *
	 * @param worldSystem the initial world coordinate rectangle
	 */
	public MapContainer(Rectangle2D.Double worldSystem) {
		super(worldSystem);
		HoverManager.getInstance().registerComponent(getComponent(), this);
		installMilSymbolDropTarget();
	}

	/**
	 * Installs an AWT drop target that accepts military symbol payloads from the
	 * NATO palette and places a map symbol at the drop location.
	 */
	private void installMilSymbolDropTarget() {
		new DropTarget(getComponent(), DnDConstants.ACTION_COPY, new DropTargetAdapter() {
			@Override
			public void drop(DropTargetDropEvent event) {

				if (!event.isDataFlavorSupported(MilSymbolTransferable.FLAVOR)) {
					event.rejectDrop();
					return;
				}

				event.acceptDrop(DnDConstants.ACTION_COPY);
				try {
					Transferable t = event.getTransferable();
					MilSymbolDescriptor descriptor = (MilSymbolDescriptor) t
							.getTransferData(MilSymbolTransferable.FLAVOR);
					placeSymbol(descriptor, event.getLocation());
					event.dropComplete(true);
				} catch (Exception ex) {
					event.dropComplete(false);
				}
			}
		}, true);
	}

	/**
	 * Creates a military symbol item at the geographic location corresponding to
	 * the supplied screen-space point.
	 *
	 * @param descriptor  the symbol descriptor
	 * @param screenPoint the drop point in canvas coordinates
	 */
	void placeSymbol(MilSymbolDescriptor descriptor, Point screenPoint) {
		if (descriptor == null || screenPoint == null) {
			return;
		}

		Point2D.Double latLon = new Point2D.Double();
		localToLatLon(screenPoint, latLon);

		ImageIcon icon = descriptor.getIcon();
		Layer layer = getAnnotationLayer();
		new MapMilSymbolItem(layer, latLon, descriptor, icon);

		setDirty(true);
		refresh();
	}

	@Override
	public void recenter(Point pp) {
	    if (pp == null) {
	        return;
	    }

	    MapView2D mapView = getMapView2D();
	    IMapProjection mp = mapView.getProjection();
	    if (mp == null) {
	        return;
	    }

	    // Convert the clicked screen point to geographic coordinates using the
	    // current transform and current projection.
	    Point2D.Double ll = new Point2D.Double();
	    localToLatLon(pp, ll);

	    // Ignore invalid clicks.
	    if (!Double.isFinite(ll.x) || !Double.isFinite(ll.y)) {
	        return;
	    }

	    boolean projectionChanged = false;

	    /*
	     * First give the projection itself a chance to handle recentering. This is
	     * the extension point for application-supplied projections whose
	     * getProjection() method returns null.
	     */
	    if (mp.supportsRecenter()) {
	        projectionChanged = mp.recenterOn(ll);
	    }

	    /*
	     * Backward-compatible handling for the built-in MDI projections. This keeps
	     * the old behavior intact and avoids forcing every existing projection class
	     * to override recenterOn immediately.
	     */
	    if (!projectionChanged && mp.getProjection() != null) {
	        switch (mp.getProjection()) {
	        case MERCATOR -> {
	            ((MercatorProjection) mp).setCentralLongitude(ll.x);
	            projectionChanged = true;
	        }

	        case MOLLWEIDE -> {
	            ((MollweideProjection) mp).setCentralLongitude(ll.x);
	            projectionChanged = true;
	        }

	        case ORTHOGRAPHIC -> {
	            ((OrthographicProjection) mp).setCenter(ll.x, ll.y);
	            projectionChanged = true;
	        }

	        case LAMBERT_EQUAL_AREA -> {
	            ((LambertEqualAreaProjection) mp).setCenter(ll.x, ll.y);
	            projectionChanged = true;
	        }
	        }
	    }

	    if (!projectionChanged) {
	        return;
	    }

	    // Re-project the same geographic point through the updated projection.
	    // This is the world-space point that must become the new viewport center.
	    Point2D.Double xy = new Point2D.Double();
	    mp.latLonToXY(ll, xy);

	    if (!Double.isFinite(xy.x) || !Double.isFinite(xy.y) || _worldSystem == null) {
	        return;
	    }

	    _worldSystem.x = xy.x - _worldSystem.width / 2.0;
	    _worldSystem.y = xy.y - _worldSystem.height / 2.0;

	    setDirty(true);
	    refresh();
	}

	/**
	 * Converts a screen-space point to geographic lon/lat in radians.
	 *
	 * @param pp screen-space point
	 * @param ll output geographic point
	 */
	public void localToLatLon(Point pp, Point2D.Double ll) {
		Point2D.Double wp = new Point2D.Double();
		localToWorld(pp, wp);
		getMapView2D().getProjection().latLonFromXY(ll, wp);
	}

	/**
	 * Converts geographic lon/lat in radians to screen-space coordinates.
	 *
	 * @param pp output screen-space point
	 * @param ll input geographic point
	 */
	public void latLonToLocal(Point pp, Point2D.Double ll) {
		Point2D.Double wp = new Point2D.Double();
		getMapView2D().getProjection().latLonToXY(ll, wp);
		worldToLocal(pp, wp);
	}

	/**
	 * Converts projection world coordinates to geographic lon/lat.
	 *
	 * @param ll output geographic point
	 * @param wp input world point
	 */
	public void worldToLatLon(Point2D.Double ll, Point2D.Double wp) {
		getMapView2D().getProjection().latLonFromXY(ll, wp);
	}

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
			_toolBar.updateStatusText(latLon);
		}
	}

	@Override
	public void hoverUp(HoverEvent he) {
		Point p = he.getLocation();
		String countryName = getMapView2D().getCountryAtPoint(p, this);
		if (countryName == null) {
			return;
		}

		HoverInfoWindow win = getHoverWindow();
		if (win == null) {
			return;
		}

		SwingUtilities.convertPointToScreen(p, he.getSource());
		win.showMessage(countryName, p);
	}

	@Override
	public void hoverDown(HoverEvent he) {
		if (hoverWindow != null) {
			hoverWindow.hideMessage();
		}
	}

	/**
	 * Releases hover resources held by this container.
	 */
	public void prepareForExit() {
		HoverManager.getInstance().unregisterComponent(getComponent());

		if (hoverWindow != null) {
			hoverWindow.hideMessage();
			hoverWindow.dispose();
			hoverWindow = null;
		}
	}

	@Override
	protected BaseToolHandler createToolHandler() {
		return new MapToolHandler(this);
	}

	/**
	 * Gets the owning map view.
	 *
	 * @return the parent map view
	 */
	private MapView2D getMapView2D() {
		return (MapView2D) getView();
	}

	/**
	 * Lazily creates the hover popup window.
	 *
	 * @return the hover popup, or {@code null} if the component is not yet realized
	 */
	private HoverInfoWindow getHoverWindow() {
		if (hoverWindow == null) {
			Window owner = SwingUtilities.getWindowAncestor(getComponent());
			if (owner == null) {
				return null;
			}
			hoverWindow = new HoverInfoWindow(owner);
		}
		return hoverWindow;
	}
}