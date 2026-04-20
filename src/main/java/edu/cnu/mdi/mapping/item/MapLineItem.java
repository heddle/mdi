package edu.cnu.mdi.mapping.item;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.ItemModification;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.mapping.MapConstants;
import edu.cnu.mdi.mapping.container.MapContainer;
import edu.cnu.mdi.mapping.graphics.MapGraphics;
import edu.cnu.mdi.mapping.graphics.MapGraphics.ProjectedMapShape;

/**
 * A map-native line item defined by two geographic endpoints and rendered as a
 * seam-aware great-circle route.
 *
 * <h2>Conceptual model</h2>
 * <p>
 * Unlike {@code edu.cnu.mdi.item.LineItem}, which stores a straight segment in
 * ordinary world coordinates, {@code MapLineItem} stores its geometry in
 * geographic coordinates:
 * </p>
 * <ul>
 * <li>{@code _startLatLon} — geographic start point in radians
 * ({@code x = longitude, y = latitude})</li>
 * <li>{@code _endLatLon} — geographic end point in radians
 * ({@code x = longitude, y = latitude})</li>
 * </ul>
 * <p>
 * When drawn, the item is projected and sampled as a great-circle route using
 * {@link MapGraphics}. This gives the familiar "airline route" appearance on a
 * map: two endpoints chosen by a simple line gesture, but rendered as the
 * correct curved route in the active projection.
 * </p>
 *
 * <h2>Interaction model</h2>
 * <p>
 * This class is designed to work well with the existing line-creation and
 * pointer tools:
 * </p>
 * <ul>
 * <li><strong>Creation</strong> can use the ordinary straight line rubberband.
 * The tool should convert its two screen endpoints to geographic coordinates
 * via {@link MapContainer#localToLatLon} and construct a {@code MapLineItem}
 * from those lat/lon values.</li>
 * <li><strong>Selection handles</strong> remain the two projected endpoint
 * positions, even though the rendered route between them is curved.</li>
 * <li><strong>Dragging</strong> moves both endpoints by the geographic delta
 * implied by the mouse movement.</li>
 * <li><strong>Resizing</strong> moves one endpoint to the current geographic
 * cursor location.</li>
 * </ul>
 *
 * <h2>Geometry and caching</h2>
 * <p>
 * The projected route is cached as a {@link ProjectedMapShape} after drawing.
 * That cached shape is used for hit testing and bounds. The cache is cleared
 * whenever the item is marked dirty.
 * </p>
 *
 * <h2>Focus point</h2>
 * <p>
 * The item focus is defined as the geographic midpoint obtained by averaging
 * the two endpoint longitude and latitude values. This is a pragmatic editing
 * focus, not the exact midpoint along the great-circle arc.
 * </p>
 */
public class MapLineItem extends AMapItem {

	/**
	 * Geographic start endpoint in radians ({@code x = longitude, y = latitude}).
	 */
	protected Point2D.Double _startLatLon;

	/**
	 * Geographic end endpoint in radians ({@code x = longitude, y = latitude}).
	 */
	protected Point2D.Double _endLatLon;

	/**
	 * Cached projected route produced during the most recent draw.
	 *
	 * <p>
	 * This cache is invalidated whenever the item becomes dirty.
	 * </p>
	 */
	protected transient ProjectedMapShape _projectedShape;

	/**
	 * Creates a map-native great-circle line item.
	 *
	 * @param layer       the layer this item lives on
	 * @param startLatLon geographic start point in radians
	 * @param endLatLon   geographic end point in radians
	 */
	public MapLineItem(Layer layer, Point2D.Double startLatLon, Point2D.Double endLatLon) {
		this(layer, startLatLon, endLatLon, (Object[]) null);
	}

	/**
	 * Creates a map-native great-circle line item with optional item property
	 * key/value pairs.
	 *
	 * @param layer       the layer this item lives on
	 * @param startLatLon geographic start point in radians
	 * @param endLatLon   geographic end point in radians
	 * @param keyVals     optional item property key/value pairs
	 */
	public MapLineItem(Layer layer, Point2D.Double startLatLon, Point2D.Double endLatLon, Object... keyVals) {

		super(layer, keyVals);

		_startLatLon = copy(startLatLon);
		_endLatLon = copy(endLatLon);

		updateFocus();
	}

	/**
	 * Returns the geographic start endpoint.
	 *
	 * @return the start point in radians
	 */
	public Point2D.Double getStartLatLon() {
		return copy(_startLatLon);
	}

	/**
	 * Returns the geographic end endpoint.
	 *
	 * @return the end point in radians
	 */
	public Point2D.Double getEndLatLon() {
		return copy(_endLatLon);
	}

	/**
	 * Sets the geographic start endpoint.
	 *
	 * @param startLatLon the new start point in radians
	 */
	public void setStartLatLon(Point2D.Double startLatLon) {
		_startLatLon = copy(startLatLon);
		geometryChanged();
	}

	/**
	 * Sets the geographic end endpoint.
	 *
	 * @param endLatLon the new end point in radians
	 */
	public void setEndLatLon(Point2D.Double endLatLon) {
		_endLatLon = copy(endLatLon);
		geometryChanged();
	}

	/**
	 * Draws this item as a seam-aware great-circle route.
	 *
	 * <p>
	 * The rendered path is built through {@link MapGraphics}. The resulting
	 * projected shape is cached for later bounds and hit-testing use.
	 * </p>
	 *
	 * @param g2        the graphics context
	 * @param container the rendering container
	 */
	@Override
	public void drawItem(Graphics2D g2, IContainer container) {
		if (!(container instanceof MapContainer mapContainer)) {
			return;
		}

		Point2D.Double[] endpoints = { copy(_startLatLon), copy(_endLatLon) };

		_projectedShape = MapGraphics.buildProjectedPolyline(mapContainer, endpoints);
		_lastDrawnPolygon = null; // this item uses projected path hit-testing, not polygon caching

		MapGraphics.drawMapLine(g2, mapContainer, _startLatLon, _endLatLon, getStyleSafe().getLineColor(),
				getStyleSafe().getLineWidth(), getStyleSafe().getLineStyle());
	}

	/**
	 * Determines whether this item should be drawn by checking its projected pixel
	 * bounds against the visible component bounds.
	 *
	 * @param g2        the graphics context
	 * @param container the rendering container
	 * @return {@code true} if the route intersects the visible area
	 */
	@Override
	public boolean shouldDraw(Graphics2D g2, IContainer container) {
		Rectangle r = getBounds(container);
		if (r == null) {
			return false;
		}

		Rectangle b = container.getComponent().getBounds();
		b.x = 0;
		b.y = 0;
		return b.intersects(r);
	}

	/**
	 * Tests whether the rendered route or its selection handles contain the given
	 * screen point.
	 *
	 * <p>
	 * Handle hit-testing is checked first so endpoint handles remain easy to
	 * select. If no handle is hit, the method tests whether the point lies within a
	 * modest pixel tolerance of the rendered great-circle route.
	 * </p>
	 *
	 * @param container   the rendering container
	 * @param screenPoint the device-space point to test
	 * @return {@code true} if the point is near the route or on a handle
	 */
	@Override
	public boolean contains(IContainer container, Point screenPoint) {
		if (inASelectRect(container, screenPoint)) {
			return true;
		}

		ensureProjectedShape(container);
		return MapGraphics.isPointNear(_projectedShape, screenPoint, 6.0);
	}

	/**
	 * Returns the device-space bounds of the currently projected route.
	 *
	 * @param container the rendering container
	 * @return the projected pixel bounds, or {@code null} if unavailable
	 */
	@Override
	public Rectangle getBounds(IContainer container) {
		ensureProjectedShape(container);
		Rectangle r = MapGraphics.getBounds(_projectedShape);
		return (r == null || r.isEmpty()) ? null : r;
	}

	/**
	 * Returns the selection handle locations for this item.
	 *
	 * <p>
	 * Unlike a polyline, a map line exposes only its two geographic endpoints as
	 * handles, even though the rendered route between them is curved.
	 * </p>
	 *
	 * @param container the rendering container
	 * @return the two endpoint handle positions in device coordinates
	 */
	@Override
	public Point[] getSelectionPoints(IContainer container) {
		if (!(container instanceof MapContainer mapContainer)) {
			return null;
		}

		Point p0 = projectEndpoint(mapContainer, _startLatLon);
		Point p1 = projectEndpoint(mapContainer, _endLatLon);

		if (p0 == null || p1 == null) {
			return null;
		}

		return new Point[] { p0, p1 };
	}

	/**
	 * Begins an interactive modification.
	 *
	 * <p>
	 * The default classification logic from {@link AItem#startModification()}
	 * already works well for this class: endpoint handles imply resize, otherwise
	 * the gesture is treated as a drag.
	 * </p>
	 */
	@Override
	public void startModification() {
		super.startModification();
		if (_modification != null) {
			_modification.setUserObject(new MapLineStartState(_startLatLon, _endLatLon));
		}
	}

	/**
	 * Continues an interactive modification.
	 *
	 * <p>
	 * Version 1 behavior:
	 * </p>
	 * <ul>
	 * <li><strong>Drag:</strong> move both geographic endpoints by the raw
	 * longitude and latitude delta between the start and current mouse
	 * positions.</li>
	 * <li><strong>Resize:</strong> replace the selected endpoint with the current
	 * geographic cursor location.</li>
	 * <li><strong>Rotate:</strong> not supported; ignored.</li>
	 * </ul>
	 *
	 * <p>
	 * This is intentionally simple and pairs naturally with ordinary straight
	 * rubberband creation. More sophisticated geodesic edit tools can come later.
	 * </p>
	 */
	@Override
	public void modify() {
		if (_modification == null) {
			return;
		}

		if (!(_modification.getContainer() instanceof MapContainer mapContainer)) {
			return;
		}

		Point startMouse = _modification.getStartMousePoint();
		Point currentMouse = _modification.getCurrentMousePoint();

		Point2D.Double startLL = localToLatLon(mapContainer, startMouse);
		Point2D.Double currentLL = localToLatLon(mapContainer, currentMouse);

		if (startLL == null || currentLL == null) {
			return;
		}

		ItemModification.ModificationType type = _modification.getType();

		switch (type) {

		case DRAG:
			if (!isDraggable()) {
				return;
			}

			MapLineStartState state = (MapLineStartState) _modification.getUserObject();

			double dLon = currentLL.x - startLL.x;
			double dLat = currentLL.y - startLL.y;

			_startLatLon.x = state.start.x + dLon;
			_startLatLon.y = state.start.y + dLat;
			_endLatLon = MapGraphics.greatCircleEndPoint(_startLatLon, state.azimuth, state.length);
			break;

		case RESIZE:
			int index = _modification.getSelectIndex();
			if (index == 0) {
				_startLatLon = copy(currentLL);
			} else {
				_endLatLon = copy(currentLL);
			}
			break;

		case ROTATE:
			return;
		}

		geometryChanged();
		_modification.getContainer().refresh();
	}

	/**
	 * Returns an approximate world-space bounding rectangle for this item.
	 *
	 * <p>
	 * Since the true geometry is geographic rather than ordinary world geometry,
	 * this method returns a simple longitude-latitude bounding box. It is mainly
	 * provided to satisfy the abstract {@link AItem} contract and should not be
	 * interpreted as the projected pixel bounds.
	 * </p>
	 *
	 * @return a longitude-latitude bounding rectangle in radians
	 */
	@Override
	public Rectangle2D.Double getWorldBounds() {
		if (_startLatLon == null || _endLatLon == null) {
			return null;
		}

		double xmin = Math.min(_startLatLon.x, _endLatLon.x);
		double xmax = Math.max(_startLatLon.x, _endLatLon.x);
		double ymin = Math.min(_startLatLon.y, _endLatLon.y);
		double ymax = Math.max(_startLatLon.y, _endLatLon.y);

		return new Rectangle2D.Double(xmin, ymin, xmax - xmin, ymax - ymin);
	}

	/**
	 * Translates the item in "world" units.
	 *
	 * <p>
	 * For a map-native item, this means applying raw longitude and latitude offsets
	 * in radians to both endpoints. This is a pragmatic version-1 translation
	 * model.
	 * </p>
	 *
	 * @param dx longitude offset in radians
	 * @param dy latitude offset in radians
	 */
	@Override
	public void translateWorld(double dx, double dy) {
		if (_startLatLon == null || _endLatLon == null) {
			return;
		}

		if (Math.abs(dx) < 1.0e-12 && Math.abs(dy) < 1.0e-12) {
			return;
		}

		_startLatLon.x += dx;
		_startLatLon.y += dy;
		_endLatLon.x += dx;
		_endLatLon.y += dy;

		geometryChanged();
	}

	/**
	 * Supplies mouse-over feedback for this route item.
	 *
	 * @param container       the rendering container
	 * @param pp              current mouse point in device coordinates
	 * @param wp              current mouse point in ordinary container world
	 *                        coordinates; not used here
	 * @param feedbackStrings feedback strings to populate
	 */
	@Override
	public void getFeedbackStrings(IContainer container, Point pp, Point2D.Double wp, List<String> feedbackStrings) {

		if (feedbackStrings == null) {
			return;
		}

		if (!contains(container, pp)) {
			return;
		}

		double length = MapConstants.RADIUS_EARTH_KM * MapGraphics.greatCircleLength(_startLatLon, _endLatLon);

		feedbackStrings.add("$yellow$" + getDisplayName() + " GC length: " + String.format("%.1f km", length));

		if (_startLatLon != null) {
			feedbackStrings.add(String.format("start lon %.2f°, lat %.2f°", Math.toDegrees(_startLatLon.x),
					Math.toDegrees(_startLatLon.y)));
		}

		if (_endLatLon != null) {
			feedbackStrings.add(String.format("end lon %.2f°, lat %.2f°", Math.toDegrees(_endLatLon.x),
					Math.toDegrees(_endLatLon.y)));
		}
	}

	/**
	 * Updates the item focus from the current geographic endpoints.
	 *
	 * <p>
	 * The focus is taken as the simple average of the two endpoints in lon/lat
	 * space. This is sufficient for selection display and dragging reference.
	 * </p>
	 */
	@Override
	protected void updateFocus() {
		double length = MapGraphics.greatCircleLength(_startLatLon, _endLatLon);
		double azimuth = MapGraphics.greatCircleAzimuth(_startLatLon, _endLatLon);
		_focus = MapGraphics.greatCircleEndPoint(_startLatLon, azimuth, length / 2);
	}

	/**
	 * Marks the item dirty and clears the cached projected route.
	 *
	 * @param dirty whether the item should be marked dirty
	 */
	@Override
	public void setDirty(boolean dirty) {
		super.setDirty(dirty);
		if (dirty) {
			_projectedShape = null;
		}
	}

	/**
	 * Prepares this item for removal.
	 */
	@Override
	public void prepareForRemoval() {
		_projectedShape = null;
		_startLatLon = null;
		_endLatLon = null;
		super.prepareForRemoval();
	}

	/**
	 * Ensures that the projected route cache exists.
	 *
	 * @param container the rendering container
	 */
	protected void ensureProjectedShape(IContainer container) {
		if (_projectedShape != null) {
			return;
		}

		if (!(container instanceof MapContainer mapContainer)) {
			return;
		}

		if (_startLatLon == null || _endLatLon == null) {
			return;
		}

		_projectedShape = MapGraphics.buildProjectedPolyline(mapContainer,
				new Point2D.Double[] { copy(_startLatLon), copy(_endLatLon) });
	}

	/**
	 * Projects a geographic endpoint into a device-space point.
	 *
	 * @param container the map container
	 * @param latLon    endpoint in radians
	 * @return the device-space point, or {@code null} if projection fails
	 */
	protected Point projectEndpoint(MapContainer container, Point2D.Double latLon) {
		if (container == null || latLon == null) {
			return null;
		}

		Point2D.Double xy = new Point2D.Double();
		((edu.cnu.mdi.mapping.MapView2D) container.getView()).getProjection().latLonToXY(latLon, xy);

		if (!Double.isFinite(xy.x) || !Double.isFinite(xy.y)) {
			return null;
		}

		Point p = new Point();
		container.worldToLocal(p, xy);
		return p;
	}

	/**
	 * Converts a device-space point to geographic longitude/latitude.
	 *
	 * @param container the map container
	 * @param p         the device-space point
	 * @return the geographic point in radians
	 */
	protected Point2D.Double localToLatLon(MapContainer container, Point p) {
		if (container == null || p == null) {
			return null;
		}

		Point2D.Double ll = new Point2D.Double();
		container.localToLatLon(p, ll);
		return ll;
	}

	/**
	 * Returns a defensive copy of a geographic point.
	 *
	 * @param p the source point
	 * @return a copied point, or {@code null} if the source is {@code null}
	 */
	protected static Point2D.Double copy(Point2D.Double p) {
		return (p == null) ? null : new Point2D.Double(p.x, p.y);
	}

	private static final class MapLineStartState {
		final public Point2D.Double start;
		final public double length; // great-circle length in radians
		final public double azimuth; // bearing from start to end in radians

		MapLineStartState(Point2D.Double start, Point2D.Double end) {
			this.start = copy(start);
			this.length = MapGraphics.greatCircleLength(start, end);
			this.azimuth = MapGraphics.greatCircleAzimuth(start, end);
		}
	}
}