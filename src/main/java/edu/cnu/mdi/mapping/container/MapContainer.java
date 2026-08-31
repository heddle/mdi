package edu.cnu.mdi.mapping.container;

import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.ImageIcon;
import javax.swing.TransferHandler;

import edu.cnu.mdi.container.BaseContainer;
import edu.cnu.mdi.container.BaseToolHandler;
import edu.cnu.mdi.hover.HoverManager;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.mapping.MapView2D;
import edu.cnu.mdi.mapping.item.MapMilSymbolItem;
import edu.cnu.mdi.mapping.milsym.MilSymbolDescriptor;
import edu.cnu.mdi.mapping.milsym.MilSymbolTransferable;
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
public class MapContainer extends BaseContainer {

	// Reusable point objects to avoid unnecessary allocations during coordinate conversions.
	Point2D.Double latLonPoint = new Point2D.Double();

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
	 * <p>
	 * This container <em>is</em> its own canvas ({@link #getComponent()} returns
	 * {@code this}), which is also the surface {@link MapView2D#enableFileDrop}
	 * installs its Swing {@link TransferHandler} on (e.g. for shapefile
	 * drag-and-drop). A raw AWT {@link DropTarget} on a component takes over
	 * that component's drop handling entirely, so a drop that isn't a military
	 * symbol is delegated to whatever {@code TransferHandler} is installed here
	 * — otherwise it would shadow the file-drop handler completely rather than
	 * merely not handling milsym payloads.
	 * </p>
	 * <p>
	 * A no-op in a headless environment: {@link DropTarget}'s constructor throws
	 * {@link java.awt.HeadlessException} there, and a headless JVM (e.g. a unit
	 * test) has no display to drop onto anyway.
	 * </p>
	 * <p>
	 * {@code dragEnter}/{@code dragOver} are overridden (not left as
	 * {@link DropTargetAdapter}'s no-op defaults) to explicitly {@code acceptDrag}
	 * whenever the drop would ultimately be handled — either as a milsym payload
	 * or by delegation below. A {@link java.awt.dnd.DropTargetListener} that never
	 * accepts during the drag shows a reject cursor for the whole gesture and, on
	 * at least some platforms, may never receive {@code drop} at all.
	 * </p>
	 */
	private void installMilSymbolDropTarget() {
		if (GraphicsEnvironment.isHeadless()) {
			return;
		}
		new DropTarget(getComponent(), DnDConstants.ACTION_COPY, new DropTargetAdapter() {

			@Override
			public void dragEnter(DropTargetDragEvent event) {
				acceptOrRejectDrag(event);
			}

			@Override
			public void dragOver(DropTargetDragEvent event) {
				acceptOrRejectDrag(event);
			}

			private void acceptOrRejectDrag(DropTargetDragEvent event) {
				boolean handleable = event.isDataFlavorSupported(MilSymbolTransferable.FLAVOR)
						|| canDelegateHandle(event.getTransferable());
				if (handleable) {
					event.acceptDrag(DnDConstants.ACTION_COPY);
				} else {
					event.rejectDrag();
				}
			}

			@Override
			public void drop(DropTargetDropEvent event) {

				if (event.isDataFlavorSupported(MilSymbolTransferable.FLAVOR)) {
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
					return;
				}

				// canDelegateHandle only checks flavor support (safe pre-accept); the
				// actual transfer data must not be read via delegateToInstalledTransferHandler
				// until AFTER acceptDrop() - reading it earlier fails with "No drop
				// current" once the real native Transferable is in play, not the
				// synthetic one this class's own tests use.
				if (canDelegateHandle(event.getTransferable())) {
					event.acceptDrop(DnDConstants.ACTION_COPY);
					boolean imported = delegateToInstalledTransferHandler(event.getTransferable());
					event.dropComplete(imported);
				} else {
					event.rejectDrop();
				}
			}
		}, true);
	}

	/**
	 * Whether whatever Swing {@link TransferHandler} is installed on this
	 * component would accept the given transferable — used during
	 * {@code dragEnter}/{@code dragOver} to decide the accept/reject cursor, and
	 * by {@link #delegateToInstalledTransferHandler} at actual drop time.
	 *
	 * @param transferable the (possibly still in-flight, during a drag) dragged data
	 * @return {@code true} if a {@code TransferHandler} is installed and its
	 *         {@code canImport} accepts this transferable
	 */
	boolean canDelegateHandle(Transferable transferable) {
		TransferHandler handler = getTransferHandler();
		return handler != null && handler.canImport(new TransferHandler.TransferSupport(this, transferable));
	}

	/**
	 * Hands a drop this container's raw milsym {@link DropTarget} doesn't
	 * recognize off to whatever Swing {@link TransferHandler} is installed on
	 * this same component, using the public {@link TransferHandler.TransferSupport}
	 * constructor built for exactly this "bridge a non-Swing drop source into a
	 * TransferHandler" case.
	 * <p>
	 * <strong>Must only be called after {@link DropTargetDropEvent#acceptDrop}</strong>
	 * (see {@link #drop}). {@code importData} reads the transferable's actual data
	 * ({@code getTransferData}), and the real, native drop {@code Transferable} AWT
	 * hands to {@code drop()} throws {@code "No drop current"} if that's attempted
	 * before the drop has been accepted — {@link #canDelegateHandle} (flavor-only,
	 * safe to call anytime) is what {@code dragEnter}/{@code dragOver}/the pre-accept
	 * check in {@code drop()} should use instead.
	 * </p>
	 *
	 * @param transferable the dropped data
	 * @return {@code true} if a {@code TransferHandler} was installed, accepted,
	 *         and successfully imported the data
	 */
	boolean delegateToInstalledTransferHandler(Transferable transferable) {
		TransferHandler handler = getTransferHandler();
		if (handler == null) {
			return false;
		}
		TransferHandler.TransferSupport support = new TransferHandler.TransferSupport(this, transferable);
		return handler.canImport(support) && handler.importData(support);
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

	/**
	 * Zooms to a geographic bounding box expressed in decimal degrees.
	 *
	 * <p>The corner coordinates are projected with the active map projection and
	 * used to replace the container's world-coordinate bounds.</p>
	 *
	 * @param minLat minimum latitude in degrees
	 * @param maxLat maximum latitude in degrees
	 * @param minLon minimum longitude in degrees
	 * @param maxLon maximum longitude in degrees
	 */
	public void zoomLatLon(double minLat, double maxLat, double minLon, double maxLon) {
			
		prepareToZoom();
		//convert bounds to world coordinates
		Point2D.Double ll = new Point2D.Double();
		getMapView2D().getProjection().latLonToXY(new Point2D.Double
				(Math.toRadians(minLon), Math.toRadians(minLat)), ll);
		
		double txmin = ll.x;
		double tymin = ll.y;
		getMapView2D().getProjection().latLonToXY(new Point2D.Double
				(Math.toRadians(maxLon), Math.toRadians(maxLat)), ll);
		double txmax = ll.x;
		double tymax = ll.y;
		
		double xmin = Math.min(txmin, txmax);
		double xmax = Math.max(txmin, txmax);
		double ymin = Math.min(tymin, tymax);
		double ymax = Math.max(tymin, tymax);
		
		_worldSystem = new Rectangle2D.Double(xmin, ymin, xmax - xmin, ymax - ymin);
		setDirty(true);
		refresh();
	}

	/**
	 * Recenters the active projection on the geographic location beneath a
	 * screen-space point.
	 *
	 * <p>Clicks that cannot be converted to finite geographic coordinates, or
	 * projections that do not support recentering, are ignored.</p>
	 *
	 * @param pp point in container coordinates; may be {@code null}
	 */
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
	 * Pans the map by changing its geographic center rather than translating a
	 * cached image.
	 *
	 * <p>The displacement is incremental: positive values mean the user dragged
	 * the map right or down. The geographic point opposite that displacement
	 * from the viewport center becomes the new projection center. If that point
	 * lies outside a bounded projection, the displacement is shortened to the
	 * furthest invertible point, so a drag never exposes empty space.</p>
	 *
	 * @param dx horizontal drag since the preceding event, in pixels
	 * @param dy vertical drag since the preceding event, in pixels
	 */
	void panMapBy(int dx, int dy) {
	    if ((dx == 0 && dy == 0) || _worldSystem == null) {
	        return;
	    }

	    java.awt.Component component = getComponent();
	    int width = component.getWidth();
	    int height = component.getHeight();
	    if (width <= 0 || height <= 0) {
	        return;
	    }

	    Point center = new Point(width / 2, height / 2);
	    Point requested = new Point(center.x - dx, center.y - dy);
	    Point target = nearestInvertiblePoint(center, requested);
	    if (target != null && !target.equals(center)) {
	        recenter(target);
	    }
	}

	/** Finds the furthest invertible point from {@code center} toward {@code end}. */
	private Point nearestInvertiblePoint(Point center, Point end) {
	    if (hasFiniteLatLon(end)) {
	        return end;
	    }

	    double low = 0.0;
	    double high = 1.0;
	    Point best = center;
	    for (int i = 0; i < 16; i++) {
	        double fraction = (low + high) / 2.0;
	        Point candidate = new Point(
	                (int) Math.round(center.x + fraction * (end.x - center.x)),
	                (int) Math.round(center.y + fraction * (end.y - center.y)));
	        if (hasFiniteLatLon(candidate)) {
	            best = candidate;
	            low = fraction;
	        } else {
	            high = fraction;
	        }
	    }
	    return best;
	}

	/** Returns whether a local point can be inverted by the active projection. */
	private boolean hasFiniteLatLon(Point point) {
	    Point2D.Double ll = new Point2D.Double();
	    localToLatLon(point, ll);
	    return Double.isFinite(ll.x) && Double.isFinite(ll.y);
	}

	/**
	 * Converts a screen-space point to geographic lon/lat in radians.
	 *
	 * @param pp screen-space point
	 * @param ll A geographic point is represented as a {@link Point2D.Double} where
     * {@code x = λ} (longitude) and {@code y = φ} (latitude).
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
	 * @param ll A geographic point is represented as a {@link Point2D.Double} where
     * {@code x = λ} (longitude) and {@code y = φ} (latitude) in radians.
	 */
	public void latLonToLocal(Point pp, Point2D.Double ll) {
		Point2D.Double wp = new Point2D.Double();
		getMapView2D().getProjection().latLonToXY(ll, wp);
		worldToLocal(pp, wp);
	}

	/**
	 * Converts projection world coordinates to geographic lon/lat.
	 *
	 * @param ll A geographic point is represented as a {@link Point2D.Double} where
     * {@code x = λ} (longitude) and {@code y = φ} (latitude).
	 * @param wp input world point
	 */
	public void worldToLatLon(Point2D.Double ll, Point2D.Double wp) {
		getMapView2D().getProjection().latLonFromXY(ll, wp);
	}

	/**
	 * Update the toolbar status text with the current mouse location.
	 *
	 * @param pp the current mouse location in local (screen) coordinates
	 * @param wp the current mouse location in world coordinates
	 */
	@Override
	public void updateStatusText(Point pp, Point2D.Double wp) {
	    if ((_toolBar == null) || !_toolBar.hasStatusField()) {
	        return;
	    }

	    worldToLatLon(latLonPoint, wp);
	    if (!Double.isFinite(latLonPoint.x)
	            || !Double.isFinite(latLonPoint.y)) {
	        _toolBar.updateStatusText("");
	        return;
	    }

	    double latDeg = Math.toDegrees(latLonPoint.y);
	    double lonDeg = Math.toDegrees(latLonPoint.x);

	    String latLon = String.format("%.2f%s %s, %.2f%s %s",
	            Math.abs(latDeg), UnicodeUtils.DEGREE, (latDeg >= 0.0) ? "N" : "S",
	            Math.abs(lonDeg), UnicodeUtils.DEGREE, (lonDeg >= 0.0) ? "E" : "W");

	    double elevation = getMapView2D().getElevation(latDeg, lonDeg);
	    if (!Double.isNaN(elevation)) {
	        boolean onLand = getMapView2D().onLand(pp, this);
	        latLon += "   " + formatElevationStatus(elevation, onLand);
	    }

	    _toolBar.updateStatusText(latLon);
	}
	
	/**
	 * Formats the elevation status string for display in the toolbar.
	 *
	 * @param elevationMeters the elevation in meters
	 * @param onLand          {@code true} if the point is on land, {@code false} if over water
	 * @return a formatted string representing the elevation or depth
	 */
	public static String formatElevationStatus(double elevationMeters, boolean onLand) {
	    long meters = Math.round(elevationMeters);

	    if ((meters < 0) && !onLand) {
	        return String.format("depth %,d m", Math.abs(meters));
	    }

	    return "elev " + formatSignedMeters(meters) + " m";
	}
	
	private static String formatSignedMeters(long meters) {
	    if (meters < 0) {
	        return "\u2212" + String.format("%,d", Math.abs(meters));
	    }

	    return String.format("%,d", meters);
	}

	/** {@inheritDoc} */
	@Override
	public void feedbackTrigger(MouseEvent mouseEvent, boolean dragging) {
		Point2D.Double wp = getLocation(mouseEvent);

		if (_feedbackControl != null) {
			_feedbackControl.updateFeedback(mouseEvent, wp, dragging);
		}
	}

	/** {@inheritDoc} */
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

}
