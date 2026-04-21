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
 * A map-native open polyline defined by an ordered array of geographic vertices,
 * with each consecutive segment rendered as a great-circle arc.
 *
 * <h2>Use cases</h2>
 * <p>
 * Flight routes, ship tracks, hiking trails, pipeline routes, or any sequence of
 * waypoints that should follow the surface of the Earth rather than a straight
 * line in projection space.
 * </p>
 *
 * <h2>Geometry</h2>
 * <p>
 * Each edge between adjacent vertices is sampled as a great-circle arc by
 * {@link MapGraphics}, which handles seam splitting automatically for global
 * projections.  The polyline is open: the last vertex is not connected back to
 * the first.  To create a closed shape use {@link MapPolygonItem}.
 * </p>
 *
 * <h2>Interaction</h2>
 * <ul>
 *   <li><b>Drag</b> — moves all vertices by the geographic delta of the mouse.</li>
 *   <li><b>Resize</b> — moves the grabbed handle vertex to the cursor.</li>
 *   <li><b>Rotate</b> — not supported (see {@link AMapMultiPointItem} class doc).</li>
 * </ul>
 *
 * <h2>Creation</h2>
 * <p>
 * Typically created by {@link edu.cnu.mdi.mapping.container.MapToolHandler} from
 * a polyline rubberband gesture:
 * </p>
 * <pre>{@code
 * Point2D.Double[] latLons = ...; // convert screen vertices to geographic
 * MapPolylineItem item = new MapPolylineItem(layer, latLons);
 * }</pre>
 */
public class MapPolylineItem extends AMapMultiPointItem {

    /**
     * Creates a map-native great-circle polyline.
     *
     * @param layer   the layer this item lives on; must not be {@code null}
     * @param latLons geographic vertices in radians ({@code x = longitude,
     *                y = latitude}); must not be {@code null}, length ≥ 2
     */
    public MapPolylineItem(Layer layer, Point2D.Double[] latLons) {
        this(layer, latLons, (Object[]) null);
    }

    /**
     * Creates a map-native great-circle polyline with optional property
     * key/value pairs.
     *
     * @param layer   the layer this item lives on; must not be {@code null}
     * @param latLons geographic vertices in radians; must not be {@code null},
     *                length ≥ 2
     * @param keyVals optional {@link edu.cnu.mdi.util.PropertyUtils} key/value pairs
     */
    public MapPolylineItem(Layer layer, Point2D.Double[] latLons, Object... keyVals) {
        super(layer, latLons, keyVals);
        getStyleSafe().setFillColor(null);
        setDisplayName("Polyline");
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    /**
     * Draws each edge as a seam-aware great-circle arc and caches the projected
     * shape for subsequent hit-testing and bounds queries.
     *
     * @param g2        the graphics context
     * @param container the rendering container; must be a {@link MapContainer}
     */
    @Override
    public void drawItem(Graphics2D g2, IContainer container) {
        if (!(container instanceof MapContainer mc)) return;
        _projectedShape = buildShape(mc);
        _lastDrawnPolygon = null;
        MapGraphics.drawMapPolyline(g2, mc, _latLons, getStyleSafe());
    }

    // -------------------------------------------------------------------------
    // Shape builder
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Builds an open (polyline) projected shape.</p>
     */
    @Override
    protected ProjectedMapShape buildShape(MapContainer mc) {
        return MapGraphics.buildProjectedPolyline(mc, _latLons);
    }
    
    @Override
    protected boolean containsPoint(ProjectedMapShape shape, Point screenPoint) {
        if (MapGraphics.isPointNear(shape, screenPoint, 6.0)) {
            return true;
        }

        if (!(getContainer() instanceof MapContainer mc)) {
            return false;
        }

        // Interior hit test as if the polyline were closed.
        ProjectedMapShape closedShape = MapGraphics.buildProjectedPolygon(mc, _latLons);
        return MapGraphics.contains(closedShape, screenPoint);
    }
}