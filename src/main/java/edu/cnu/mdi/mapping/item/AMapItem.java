package edu.cnu.mdi.mapping.item;

import java.awt.Point;
import java.awt.geom.Point2D;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.mapping.container.MapContainer;

/**
 * Abstract base class for map-native items whose geometry is stored in
 * geographic coordinates (longitude/latitude in radians) rather than in the
 * container's affine-transform world coordinate system.
 *
 * <h2>Coordinate convention</h2>
 * <p>
 * All geographic points follow the MDI mapping convention: a
 * {@link Point2D.Double} where {@code x = λ} (longitude) and
 * {@code y = φ} (latitude), both in radians. Longitude is wrapped to the
 * half-open range {@code (-π, π]}.
 * </p>
 *
 * <h2>Focus semantics</h2>
 * <p>
 * The {@link #_focus} field inherited from {@link AItem} is repurposed to hold
 * a geographic point rather than a world (projection-space) point. Subclasses
 * are responsible for keeping {@code _focus} updated (typically in
 * {@link #updateFocus()}) to a meaningful geographic reference point such as
 * the arc midpoint or the polygon centroid.
 * </p>
 *
 * <h2>Screen-space focus</h2>
 * <p>
 * {@link #getFocusPoint(IContainer)} is overridden to convert the geographic
 * focus through the active map projection rather than through the container's
 * plain affine transform. If the container is not a {@link MapContainer}, or
 * if the geographic focus projects to a non-finite point (e.g. the far
 * hemisphere on an orthographic projection), the method returns {@code null}.
 * </p>
 *
 * <h2>Subclass responsibilities</h2>
 * <ul>
 *   <li>Store geometry in geographic coordinates (radians).</li>
 *   <li>Override {@link #updateFocus()} to keep {@link #_focus} current.</li>
 *   <li>Override {@link #drawItem}, {@link #shouldDraw}, {@link #contains},
 *       {@link #getBounds}, and {@link #translateWorld} to operate in
 *       geographic space.</li>
 * </ul>
 */
public abstract class AMapItem extends AItem {

    /**
     * Constructs a new map item on the given layer.
     *
     * @param layer   the layer this item belongs to; must not be {@code null}
     * @param keyVals optional alternating {@link edu.cnu.mdi.util.PropertyUtils}
     *                key/value pairs applied to behavior flags and style
     */
    public AMapItem(Layer layer, Object... keyVals) {
        super(layer, keyVals);
    }

    /**
     * Returns the screen-coordinate pixel position of this item's geographic
     * focus point.
     *
     * <p>The conversion path is: geographic focus → projection-space (x, y)
     * via the active {@link edu.cnu.mdi.mapping.projection.IMapProjection} →
     * device pixels via {@link MapContainer#worldToLocal}.</p>
     *
     * <p>Returns {@code null} if:
     * <ul>
     *   <li>{@code container} is not a {@link MapContainer},</li>
     *   <li>the geographic focus is {@code null}, or</li>
     *   <li>the focus projects to a non-finite coordinate (e.g. behind the
     *       globe in an orthographic projection).</li>
     * </ul>
     * </p>
     *
     * @param container the container rendering this item
     * @return the device-space focus position, or {@code null}
     */
    @Override
    public Point getFocusPoint(IContainer container) {
        if (!(container instanceof MapContainer mc)) return null;
        Point2D.Double latlon = getFocus();
        if (latlon == null) return null;

        // Check the projection result for finiteness before converting to pixels.
        // A non-finite result means the point is off-map (e.g. the far hemisphere
        // on an orthographic projection) and should not be drawn.
        if (!(mc.getView() instanceof edu.cnu.mdi.mapping.MapView2D mv)) return null;
        Point2D.Double xy = new Point2D.Double();
        mv.getProjection().latLonToXY(latlon, xy);
        if (!Double.isFinite(xy.x) || !Double.isFinite(xy.y)) return null;

        Point pp = new Point();
        mc.latLonToLocal(pp, latlon);
        return pp;
    }

    /**
     * Hook called when geometry changes and the geographic focus should be
     * recomputed.
     *
     * <p>The default implementation is a no-op. Subclasses that maintain a
     * derived focus (e.g. the geodesic midpoint of a great-circle arc, or the
     * geographic centroid of a polygon) should override this to recompute
     * {@link #_focus} from the current geometry.</p>
     */
    @Override
    protected void updateFocus() {
        // default: nothing to do
    }
}