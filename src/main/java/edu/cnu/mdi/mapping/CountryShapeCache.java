package edu.cnu.mdi.mapping;

import java.awt.Point;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import edu.cnu.mdi.container.IContainer;

/**
 * Cache of projected country shapes in world (projection XY) coordinates.
 * <p>
 * Given a list of {@link GeoJsonCountryLoader.CountryFeature} objects defined
 * in geographic coordinates (lon/lat in degrees), this class builds and caches
 * a list of {@link CountryShape} instances that represent each country's
 * polygon in projection (XY) space.
 * </p>
 * <p>
 * The cache is keyed by {@link IMapProjection} instance; if the projection
 * changes, clients must call {@link #invalidate()} so that a subsequent
 * access will rebuild the cache with the new projection.
 * </p>
 */
public final class CountryShapeCache {

    /**
     * Immutable value type representing a single country's projected shape.
     * The path and bounds are expressed in world (projection XY) coordinates.
     */
    public static final class CountryShape {

        private final GeoJsonCountryLoader.CountryFeature feature;
        private final Path2D.Double worldPath;
        private final Rectangle2D worldBounds;

        CountryShape(GeoJsonCountryLoader.CountryFeature feature,
                     Path2D.Double worldPath) {
            this.feature = feature;
            this.worldPath = worldPath;
            this.worldBounds = worldPath.getBounds2D();
        }

        /** The underlying country feature. */
        public GeoJsonCountryLoader.CountryFeature getFeature() {
            return feature;
        }

        /** The projected country polygon in world (XY) coordinates. */
        public Path2D.Double getWorldPath() {
            return worldPath;
        }

        /** Bounding box of the world path (for fast reject). */
        public Rectangle2D getWorldBounds() {
            return worldBounds;
        }
    }

    private final List<GeoJsonCountryLoader.CountryFeature> countryFeatures;

    private List<CountryShape> cachedShapes = Collections.emptyList();
    private boolean cacheDirty = true;

    // Remember which projection the cache was built for
    private IMapProjection lastProjection;

    /**
     * Create a new cache for the given list of country features.
     *
     * @param countryFeatures the country features to cache; must not be null
     */
    public CountryShapeCache(List<GeoJsonCountryLoader.CountryFeature> countryFeatures) {
        this.countryFeatures = Objects.requireNonNull(countryFeatures, "countryFeatures");
    }

    /**
     * Mark the cache as out-of-date. The next call to
     * {@link #getShapes(IMapProjection)} or
     * {@link #pickCountry(Point, IContainer, IMapProjection)}
     * will rebuild the cache for the given projection.
     */
    public void invalidate() {
        cacheDirty = true;
        lastProjection = null;
    }

    /**
     * Return the list of cached shapes, rebuilding the cache if necessary
     * for the supplied projection.
     *
     * @param projection the projection to use for building the world shapes
     * @return immutable list of {@link CountryShape} instances
     */
    public List<CountryShape> getShapes(IMapProjection projection) {
        Objects.requireNonNull(projection, "projection");
        ensureCache(projection);
        return cachedShapes;
    }

    /**
     * Perform a hit-test on the cached country shapes using a mouse position
     * in local (component) coordinates.
     *
     * @param mouseLocal the mouse position in local/component coordinates
     * @param container  the container providing the local-to-world transform
     * @param projection the projection for which the cache should be valid
     * @return the first {@link GeoJsonCountryLoader.CountryFeature} whose
     *         projected polygon contains the mouse position, or {@code null}
     *         if no country was hit
     */
    public GeoJsonCountryLoader.CountryFeature pickCountry(Point mouseLocal,
                                                           IContainer container,
                                                           IMapProjection projection) {
        Objects.requireNonNull(mouseLocal, "mouseLocal");
        Objects.requireNonNull(container, "container");
        Objects.requireNonNull(projection, "projection");

        ensureCache(projection);
        if (cachedShapes.isEmpty()) {
            return null;
        }

        // Convert mouse (local) -> world coordinates
        Point2D.Double worldPoint = new Point2D.Double();
        container.localToWorld(mouseLocal, worldPoint);

        for (CountryShape cs : cachedShapes) {
            // Cheap bounding box reject
            if (!cs.getWorldBounds().contains(worldPoint)) {
                continue;
            }
            if (cs.getWorldPath().contains(worldPoint)) {
                return cs.getFeature();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    // Internal cache building
    // ------------------------------------------------------------------------

    private void ensureCache(IMapProjection projection) {
        if (!cacheDirty && projection == lastProjection && cachedShapes != null) {
            return;
        }
        rebuildCache(projection);
    }

    private void rebuildCache(IMapProjection projection) {
        List<CountryShape> shapes = new ArrayList<>(countryFeatures.size());

        Point2D.Double latLon = new Point2D.Double();
        Point2D.Double xy = new Point2D.Double();

        for (GeoJsonCountryLoader.CountryFeature feature : countryFeatures) {

            Path2D.Double worldPath = new Path2D.Double(Path2D.WIND_NON_ZERO);
            boolean hasAnyRingPoint = false;

            for (List<Point2D.Double> ring : feature.getPolygons()) {
                if (ring.isEmpty()) {
                    continue;
                }

                boolean ringHasPoint = false;

                for (Point2D.Double lonLatDeg : ring) {
                    double lonDeg = lonLatDeg.x;
                    double latDeg = lonLatDeg.y;

                    double lonRad = Math.toRadians(lonDeg);
                    double latRad = Math.toRadians(latDeg);

                    // lambda = longitude, phi = latitude
                    latLon.setLocation(lonRad, latRad);

                    projection.latLonToXY(latLon, xy);
                    if (!projection.isPointOnMap(xy)) {
                        continue;
                    }

                    double x = xy.x;
                    double y = xy.y;

                    if (!ringHasPoint) {
                        worldPath.moveTo(x, y);
                        ringHasPoint = true;
                        hasAnyRingPoint = true;
                    } else {
                        worldPath.lineTo(x, y);
                    }
                }

                if (ringHasPoint) {
                    worldPath.closePath();
                }
            }

            // Only add features that have at least one visible ring
            if (hasAnyRingPoint) {
                shapes.add(new CountryShape(feature, worldPath));
            }
        }

        this.cachedShapes = Collections.unmodifiableList(shapes);
        this.cacheDirty = false;
        this.lastProjection = projection;
    }
}
