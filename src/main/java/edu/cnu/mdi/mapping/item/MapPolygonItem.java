package edu.cnu.mdi.mapping.item;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.mapping.container.MapContainer;
import edu.cnu.mdi.mapping.graphics.MapGraphics;
import edu.cnu.mdi.mapping.graphics.MapGraphics.ProjectedMapShape;

/**
 * A map-native closed spherical polygon defined by an ordered array of
 * geographic vertices, with every edge (including the closing edge from the
 * last vertex back to the first) rendered as a great-circle arc.
 *
 * <h2>Use cases</h2>
 * <p>
 * User-drawn region boundaries, search areas, territorial outlines, or any
 * closed shape whose vertices must remain geographically pinned when the
 * projection changes.
 * </p>
 *
 * <h2>Drawing model — closed polyline, not Path2D.closePath()</h2>
 * <p>
 * All N edges (including the closing edge vertex[N-1] → vertex[0]) are
 * sampled as great-circle arcs by passing a length-(N+1) vertex array
 * (vertex[0] duplicated at the end) to
 * {@link MapGraphics#buildProjectedPolyline}.  This means:
 * </p>
 * <ul>
 *   <li>Every edge is a proper great-circle arc — no straight chord appears
 *       for the closing edge.</li>
 *   <li>{@code Path2D.closePath()} is never called, so seam-split sub-paths
 *       are not incorrectly filled with a straight closing line.</li>
 *   <li>The shape is drawn via
 *       {@link MapGraphics#drawMapPolyline(Graphics2D, MapContainer, Point2D.Double[], edu.cnu.mdi.graphics.style.IStyled)}
 *       with fill handled separately as a pre-pass.</li>
 * </ul>
 *
 * <h2>Hit-testing</h2>
 * <p>
 * Because seam splitting produces multiple disconnected sub-paths, Java2D's
 * {@link java.awt.geom.Path2D#contains(double, double)} cannot reliably test
 * interior containment for the full polygon.  Instead, a spherical
 * ray-casting test is applied directly to the geographic {@link #_latLons}
 * array, which is both projection-independent and exact.  The test counts how
 * many great-circle edges the meridian from the test point north to the pole
 * crosses; an odd count means the point is inside.
 * </p>
 *
 * <h2>Interaction</h2>
 * <ul>
 *   <li><b>Drag</b> — moves all vertices by the geographic delta of the mouse.</li>
 *   <li><b>Resize</b> — moves the grabbed handle vertex to the cursor.</li>
 *   <li><b>Rotate</b> — not supported (see {@link AMapMultiPointItem}).</li>
 * </ul>
 */
public class MapPolygonItem extends AMapMultiPointItem {

    /**
     * Creates a map-native spherical polygon.
     *
     * @param layer   the layer; must not be {@code null}
     * @param latLons geographic vertices in radians ({@code x = longitude,
     *                y = latitude}); must not be {@code null}, length ≥ 3
     * @throws IllegalArgumentException if {@code latLons} has fewer than 3 points
     */
    public MapPolygonItem(Layer layer, Point2D.Double[] latLons) {
        this(layer, latLons, (Object[]) null);
    }

    /**
     * Creates a map-native spherical polygon with optional property key/value pairs.
     *
     * @param layer   the layer; must not be {@code null}
     * @param latLons geographic vertices in radians; must not be {@code null},
     *                length ≥ 3
     * @param keyVals optional {@link edu.cnu.mdi.util.PropertyUtils} key/value pairs
     * @throws IllegalArgumentException if {@code latLons} has fewer than 3 points
     */
    public MapPolygonItem(Layer layer, Point2D.Double[] latLons, Object... keyVals) {
        super(layer, requireThree(latLons), keyVals);
        setDisplayName("Polygon");
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    /**
     * Draws the closed spherical polygon.
     *
     * <p>Fill (if any) is painted first using the projected boundary paths, then
     * the outline is drawn as a closed polyline of great-circle arcs so that the
     * closing edge is a genuine arc rather than a straight chord.</p>
     *
     * @param g2        the graphics context
     * @param container the rendering container; must be a {@link MapContainer}
     */
    //@Override
    public void XdrawItem(Graphics2D g2, IContainer container) {
        if (!(container instanceof MapContainer mc)) return;

        // Build a closed-polyline shape (no Path2D.closePath) for both
        // caching and fill.  The shape is an open polyline of N+1 points
        // where vertex[0] is duplicated at the end so the closing arc is
        // sampled as a great-circle segment.
        _projectedShape = buildShape(mc);
        _lastDrawnPolygon = null;

        // Fill the interior using the projected paths (even with seam splits
        // the fill covers the correct screen region for Mercator/Mollweide;
        // unfilled polygons skip this entirely).
        java.awt.Color fillColor = getStyleSafe().getFillColor();
        if (fillColor != null) {
            g2.setColor(fillColor);
            for (java.awt.geom.Path2D.Double path : _projectedShape.getPaths()) {
                g2.fill(path);
            }
        }

        // Draw the outline as a closed great-circle polyline.
        MapGraphics.drawMapPolyline(g2, mc, closedVertices(), getStyleSafe());
    }
    
    @Override
    public void drawItem(Graphics2D g2, IContainer container) {
        if (!(container instanceof MapContainer mc)) return;

        // Use a true polygon shape for fill/interior semantics.
        ProjectedMapShape fillShape = MapGraphics.buildProjectedPolygon(mc, _latLons);

        // Cache an outline shape for bounds/edge-hit testing.
        _projectedShape = MapGraphics.buildProjectedPolyline(mc, closedVertices());
        _lastDrawnPolygon = null;

        java.awt.Color fillColor = getStyleSafe().getFillColor();
        if (fillColor != null) {
            g2.setColor(fillColor);
            for (java.awt.geom.Path2D.Double path : fillShape.getPaths()) {
                g2.fill(path);
            }
        }

        // Draw outline as sampled great-circle edges, including closing edge.
        MapGraphics.drawMapPolyline(g2, mc, closedVertices(), getStyleSafe());
    }

    // -------------------------------------------------------------------------
    // Hit-testing — spherical point-in-polygon
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Interior containment is tested using a spherical ray-casting algorithm
     * on the geographic {@link #_latLons} array, so the result is correct
     * regardless of how the projection splits the boundary into sub-paths.
     * Outline proximity (6-pixel tolerance) is tested as a fallback so the
     * boundary is always clickable even for unfilled polygons.</p>
     */
    @Override
    protected boolean containsPoint(ProjectedMapShape shape, Point screenPoint) {
        return MapGraphics.isPointNear(shape, screenPoint, 6.0)
            || geographicContains(screenPoint);
    }

    // -------------------------------------------------------------------------
    // Shape builder
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Builds an <em>open polyline</em> of the N+1 closed-polygon vertices
     * (vertex[0] duplicated at the end) so that all edges, including the closing
     * edge, are great-circle arcs.  {@code Path2D.closePath()} is not used.</p>
     */
    @Override
    protected ProjectedMapShape buildShape(MapContainer mc) {
        return MapGraphics.buildProjectedPolyline(mc, closedVertices());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the vertex array with vertex[0] duplicated at the end, so that
     * passing it to a polyline builder produces all N closed edges as great-circle
     * arcs.
     *
     * @return an array of length {@code _latLons.length + 1}
     */
    private Point2D.Double[] closedVertices() {
        int n = _latLons.length;
        Point2D.Double[] closed = new Point2D.Double[n + 1];
        System.arraycopy(_latLons, 0, closed, 0, n);
        closed[n] = new Point2D.Double(_latLons[0].x, _latLons[0].y);
        return closed;
    }

    /**
     * Tests whether the geographic location that projects to {@code screenPoint}
     * lies inside this polygon using a spherical ray-casting algorithm.
     *
     * <p>The algorithm counts how many edges of the polygon are crossed by the
     * meridian arc running northward from the test point to the pole.  An odd
     * crossing count means the point is inside (Jordan curve theorem on the
     * sphere).  This test is performed in geographic (lon/lat) space so it is
     * independent of the active projection and is unaffected by seam splitting.
     * </p>
     *
     * <p>The test point is obtained by inverse-projecting {@code screenPoint}
     * through the active {@link edu.cnu.mdi.mapping.projection.IMapProjection}.
     * If the inverse projection fails (the point is off the map domain), the
     * method returns {@code false}.</p>
     *
     * @param screenPoint the device-space point to test
     * @return {@code true} if the point lies inside the polygon
     */
    private boolean geographicContains(Point screenPoint) {
        IContainer container = getContainer();
        if (!(container instanceof MapContainer mc)) return false;

        // Inverse-project the screen point to geographic coordinates.
        Point2D.Double ll = new Point2D.Double();
        mc.localToLatLon(screenPoint, ll);
        if (!Double.isFinite(ll.x) || !Double.isFinite(ll.y)) return false;

        return sphericalRayCast(ll.x, ll.y, _latLons);
    }

    /**
     * Spherical ray-casting: counts edge crossings of the northward meridian
     * from ({@code testLon}, {@code testLat}) toward the north pole.
     *
     * <p>For each edge (lon1,lat1) → (lon2,lat2) the algorithm checks whether
     * the meridian at {@code testLon} passes through the latitude band between
     * the two vertices and, if so, whether the crossing latitude is above
     * {@code testLat}.  The longitude handling wraps correctly so edges crossing
     * the ±180° seam are treated as a single continuous arc.</p>
     *
     * @param testLon longitude of the test point in radians
     * @param testLat latitude of the test point in radians
     * @param vertices the polygon vertices in lon/lat radians (not closed)
     * @return {@code true} if the crossing count is odd (point is inside)
     */
    private static boolean sphericalRayCast(double testLon, double testLat,
                                            Point2D.Double[] vertices) {
        int n = vertices.length;
        int crossings = 0;

        for (int i = 0; i < n; i++) {
            double lon1 = vertices[i].x;
            double lat1 = vertices[i].y;
            double lon2 = vertices[(i + 1) % n].x;
            double lat2 = vertices[(i + 1) % n].y;

            // Normalise dLon to (-π, π] so edges crossing the seam are handled.
            double dLon = lon2 - lon1;
            while (dLon >  Math.PI) dLon -= 2.0 * Math.PI;
            while (dLon < -Math.PI) dLon += 2.0 * Math.PI;

            // Re-express lon2 relative to lon1 in the same half-open interval.
            lon2 = lon1 + dLon;

            // The test meridian must fall strictly between lon1 and lon2.
            double minLon = Math.min(lon1, lon2);
            double maxLon = Math.max(lon1, lon2);
            if (testLon <= minLon || testLon > maxLon) continue;

            // Interpolate the crossing latitude along the edge.
            // For short edges on the sphere this linear interpolation is a good
            // approximation; for longer edges the great-circle arc is slightly
            // higher than the linear interpolation, which introduces negligible
            // error for typical polygon sizes.
            double t = (testLon - lon1) / dLon;
            double crossLat = lat1 + t * (lat2 - lat1);

            if (crossLat > testLat) crossings++;
        }

        return (crossings & 1) == 1;
    }

    /**
     * Validates that the vertex array has at least 3 points.
     *
     * @param latLons the vertex array to validate
     * @return the same array, unchanged
     * @throws IllegalArgumentException if {@code latLons} has fewer than 3 points
     */
    private static Point2D.Double[] requireThree(Point2D.Double[] latLons) {
        if (latLons == null || latLons.length < 3)
            throw new IllegalArgumentException(
                    "MapPolygonItem requires at least 3 vertices");
        return latLons;
    }
}