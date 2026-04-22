package edu.cnu.mdi.mapping.util;

import java.awt.geom.Point2D;

/**
 * Utility class for geographic coordinate conversions using the WGS84 ellipsoid.
 *
 * <p>Supports bidirectional conversion between:
 * <ul>
 *   <li>Radians (longitude, latitude) as {@link Point2D.Double} — the internal
 *       representation used by the mapping system</li>
 *   <li>UTM (Universal Transverse Mercator) as {@link UTMCoordinate}</li>
 *   <li>Decimal degrees as a convenience intermediate</li>
 * </ul>
 *
 * <p><b>Coordinate convention:</b> All {@code Point2D.Double} values used here
 * follow the mapping convention where {@code .x = longitude} and {@code .y = latitude},
 * both in <em>radians</em>.
 *
 * <p><b>Accuracy:</b> The UTM implementation uses the standard transverse Mercator
 * series expansion (Bowring/Snyder approach). Accuracy is better than 1 mm within
 * the standard UTM zone bounds (±3° from the central meridian). Results degrade
 * gracefully outside those bounds but are not intended for use beyond ±10°.
 *
 * <p><b>Coverage:</b> Valid for latitudes {@code -80°} to {@code +84°} (the defined
 * UTM coverage area). Polar regions require a separate UPS implementation.
 *
 * @see UTMCoordinate
 */
public class GeoUtils {

    // -------------------------------------------------------------------------
    // WGS84 ellipsoid constants
    // -------------------------------------------------------------------------

    /** Semi-major axis of the WGS84 ellipsoid (equatorial radius), in metres. */
    private static final double A = 6_378_137.0;

    /** Flattening of the WGS84 ellipsoid: {@code f = (a - b) / a}. */
    private static final double F = 1.0 / 298.257223563;

    /** Semi-minor axis: {@code b = a * (1 - f)}. */
    private static final double B = A * (1.0 - F);

    /** First eccentricity squared: {@code e² = 1 - (b/a)²}. */
    private static final double E2 = 1.0 - (B * B) / (A * A);

    /** Second eccentricity squared: {@code e'² = (a/b)² - 1}. */
    private static final double EP2 = (A * A) / (B * B) - 1.0;

    // -------------------------------------------------------------------------
    // UTM projection constants
    // -------------------------------------------------------------------------

    /** UTM central scale factor (k₀). */
    private static final double K0 = 0.9996;

    /** UTM false easting applied to every zone, in metres. */
    private static final double FALSE_EASTING = 500_000.0;

    /** UTM false northing applied in the southern hemisphere, in metres. */
    private static final double FALSE_NORTHING_SOUTH = 10_000_000.0;

    /** Latitude limit of the UTM system (84° N). */
    private static final double MAX_LAT_DEG = 84.0;

    /** Latitude limit of the UTM system (80° S). */
    private static final double MIN_LAT_DEG = -80.0;

    // Private constructor — static utility class, not meant to be instantiated.
    private GeoUtils() {}

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Converts a geographic position given in radians to a {@link UTMCoordinate}.
     *
     * @param latLonRad the position to convert; {@code .x} = longitude in radians,
     *                  {@code .y} = latitude in radians
     * @return the corresponding UTM coordinate on the WGS84 ellipsoid
     * @throws IllegalArgumentException if the latitude is outside the UTM-defined
     *                                  range of {@code -80°} to {@code +84°}
     */
    public static UTMCoordinate fromRadians(Point2D.Double latLonRad) {
        double latDeg = Math.toDegrees(latLonRad.y);
        double lonDeg = Math.toDegrees(latLonRad.x);
        return fromDecimalDegrees(latDeg, lonDeg);
    }

    /**
     * Converts a geographic position given in decimal degrees to a {@link UTMCoordinate}.
     *
     * @param latDeg latitude in decimal degrees, positive north ({@code -80} to {@code +84})
     * @param lonDeg longitude in decimal degrees, positive east ({@code -180} to {@code +180})
     * @return the corresponding UTM coordinate on the WGS84 ellipsoid
      */
    public static UTMCoordinate fromDecimalDegrees(double latDeg, double lonDeg) {
        if (latDeg < MIN_LAT_DEG || latDeg > MAX_LAT_DEG) {
        	return new UTMCoordinate(true); // Out of range: return a special coordinate indicating this
        }

        // Normalise longitude to [-180, 180)
        lonDeg = normalizeLongitude(lonDeg);

        double latRad = Math.toRadians(latDeg);
        double lonRad = Math.toRadians(lonDeg);

        // --- Zone number ---
        int zone = longitudeToZone(lonDeg, latDeg);

        // Central meridian of this zone
        double lonOriginRad = Math.toRadians(zoneTocentralMeridianDeg(zone));

        // --- Transverse Mercator projection (Snyder, 1987, §8) ---
        double sinLat = Math.sin(latRad);
        double cosLat = Math.cos(latRad);
        double tanLat = Math.tan(latRad);

        // Radius of curvature in the prime vertical
        double N = A / Math.sqrt(1.0 - E2 * sinLat * sinLat);

        double t  = tanLat * tanLat;
        double c  = EP2 * cosLat * cosLat;
        double aa = cosLat * (lonRad - lonOriginRad);

        // Meridional arc (M) using the standard series for WGS84
        double M = meridionalArc(latRad);

        // Easting (series expansion to sixth order in aa)
        double easting = K0 * N * (aa
                + aa * aa * aa / 6.0  * (1.0 - t + c)
                + aa * aa * aa * aa * aa / 120.0
                    * (5.0 - 18.0 * t + t * t + 72.0 * c - 58.0 * EP2))
                + FALSE_EASTING;

        // Northing
        double northing = K0 * (M + N * tanLat * (
                  aa * aa / 2.0
                + aa * aa * aa * aa / 24.0
                    * (5.0 - t + 9.0 * c + 4.0 * c * c)
                + aa * aa * aa * aa * aa * aa / 720.0
                    * (61.0 - 58.0 * t + t * t + 600.0 * c - 330.0 * EP2)));

        if (latDeg < 0.0) {
            northing += FALSE_NORTHING_SOUTH;  // Southern hemisphere: apply false northing
        }

        char letter = latitudeToZoneLetter(latDeg);
        return new UTMCoordinate(easting, northing, zone, letter);
    }

    /**
     * Converts a {@link UTMCoordinate} to a geographic position in radians.
     *
     * @param utm the UTM coordinate to convert
     * @return the corresponding geographic position; {@code .x} = longitude in radians,
     *         {@code .y} = latitude in radians
     * @throws IllegalArgumentException if the UTM zone number is not in the range
     *                                  {@code 1} to {@code 60}
     */
    public static Point2D.Double toRadians(UTMCoordinate utm) {
        double[] degResult = toDecimalDegrees(utm);
        return new Point2D.Double(Math.toRadians(degResult[1]), Math.toRadians(degResult[0]));
    }

    /**
     * Converts a {@link UTMCoordinate} to decimal degrees (latitude, longitude).
     *
     * @param utm the UTM coordinate to convert
     * @return a two-element array {@code [latitudeDeg, longitudeDeg]}
     * @throws IllegalArgumentException if the UTM zone number is not in the range
     *                                  {@code 1} to {@code 60}
     */
    public static double[] toDecimalDegrees(UTMCoordinate utm) {
        if (utm.zone < 1 || utm.zone > 60) {
            throw new IllegalArgumentException(
                    "UTM zone number must be between 1 and 60, got: " + utm.zone);
        }

        boolean northernHemisphere = utm.letter >= 'N';

        double x = utm.easting  - FALSE_EASTING;
        double y = utm.northing;
        if (!northernHemisphere) {
            y -= FALSE_NORTHING_SOUTH;
        }

        double lonOriginRad = Math.toRadians(zoneTocentralMeridianDeg(utm.zone));

        // --- Footpoint latitude (φ₁) via the meridional arc inverse ---
        double M = y / K0;
        double mu = M / (A * (1.0
                - E2 / 4.0
                - 3.0 * E2 * E2 / 64.0
                - 5.0 * E2 * E2 * E2 / 256.0));

        // Eccentricity-based series coefficients for inverse meridional arc
        double e1 = (1.0 - Math.sqrt(1.0 - E2)) / (1.0 + Math.sqrt(1.0 - E2));

        double phi1 = mu
                + (3.0 * e1 / 2.0       - 27.0 * e1 * e1 * e1 / 32.0) * Math.sin(2.0 * mu)
                + (21.0 * e1 * e1 / 16.0 - 55.0 * e1 * e1 * e1 * e1 / 32.0) * Math.sin(4.0 * mu)
                + (151.0 * e1 * e1 * e1 / 96.0) * Math.sin(6.0 * mu)
                + (1097.0 * e1 * e1 * e1 * e1 / 512.0) * Math.sin(8.0 * mu);

        // --- Compute latitude and longitude from the footpoint latitude ---
        double sinPhi1 = Math.sin(phi1);
        double cosPhi1 = Math.cos(phi1);
        double tanPhi1 = Math.tan(phi1);

        // Radius of curvature in the prime vertical at φ₁
        double N1 = A / Math.sqrt(1.0 - E2 * sinPhi1 * sinPhi1);
        // Radius of curvature in the meridian at φ₁
        double R1 = A * (1.0 - E2) / Math.pow(1.0 - E2 * sinPhi1 * sinPhi1, 1.5);

        double D   = x / (N1 * K0);
        double t1  = tanPhi1 * tanPhi1;
        double c1  = EP2 * cosPhi1 * cosPhi1;

        // Latitude (series in D)
        double latRad = phi1
                - N1 * tanPhi1 / R1 * (
                    D * D / 2.0
                  - D * D * D * D / 24.0
                    * (5.0 + 3.0 * t1 + 10.0 * c1 - 4.0 * c1 * c1 - 9.0 * EP2)
                  + D * D * D * D * D * D / 720.0
                    * (61.0 + 90.0 * t1 + 298.0 * c1 + 45.0 * t1 * t1
                       - 252.0 * EP2 - 3.0 * c1 * c1));

        // Longitude
        double lonRad = lonOriginRad + (
                    D
                  - D * D * D / 6.0  * (1.0 + 2.0 * t1 + c1)
                  + D * D * D * D * D / 120.0
                    * (5.0 - 2.0 * c1 + 28.0 * t1 - 3.0 * c1 * c1
                       + 8.0 * EP2 + 24.0 * t1 * t1))
                / cosPhi1;

        return new double[]{ Math.toDegrees(latRad), Math.toDegrees(lonRad) };
    }

    // =========================================================================
    // Package-private helpers (accessible to tests in the same package)
    // =========================================================================

    /**
     * Returns the UTM zone number (1–60) for a given longitude and latitude.
     * Applies the special-case rules for Norway (zone 32V) and Svalbard (zones 31X–37X).
     *
     * @param lonDeg longitude in decimal degrees, already normalised to {@code [-180, 180)}
     * @param latDeg latitude in decimal degrees
     * @return the UTM zone number, {@code 1} to {@code 60}
     */
    static int longitudeToZone(double lonDeg, double latDeg) {
        int zone = (int) Math.floor((lonDeg + 180.0) / 6.0) + 1;

        // Norway special case (zone 32V covers 3°E–12°E at 56°N–64°N instead of the
        // normal 32 / 31 split), per the ITU GRS designation.
        if (latDeg >= 56.0 && latDeg < 64.0 && lonDeg >= 3.0 && lonDeg < 12.0) {
            zone = 32;
        }

        // Svalbard special cases (zone letter X: 72°N–84°N)
        if (latDeg >= 72.0 && latDeg < 84.0) {
            if      (lonDeg >= 0.0  && lonDeg < 9.0)  zone = 31;
            else if (lonDeg >= 9.0  && lonDeg < 21.0) zone = 33;
            else if (lonDeg >= 21.0 && lonDeg < 33.0) zone = 35;
            else if (lonDeg >= 33.0 && lonDeg < 42.0) zone = 37;
        }

        return zone;
    }

    /**
     * Returns the central meridian (in decimal degrees) for a given UTM zone number.
     *
     * @param zone the UTM zone number (1–60)
     * @return the central meridian in decimal degrees
     */
    static double zoneTocentralMeridianDeg(int zone) {
        return (zone - 1) * 6.0 - 180.0 + 3.0;
    }

    /**
     * Returns the UTM latitude band letter for a given latitude.
     * Letters 'I' and 'O' are intentionally skipped to avoid visual ambiguity.
     *
     * @param latDeg latitude in decimal degrees ({@code -80} to {@code +84})
     * @return the single-character UTM latitude band identifier
     */
    static char latitudeToZoneLetter(double latDeg) {
        // Bands are 8° each starting at -80°, except the last (X) which is 12° wide.
        final String BANDS = "CDEFGHJKLMNPQRSTUVWXX";
        int index = (int) Math.floor((latDeg + 80.0) / 8.0);
        index = Math.max(0, Math.min(index, BANDS.length() - 1));
        return BANDS.charAt(index);
    }

    // =========================================================================
    // Private computation helpers
    // =========================================================================

    /**
     * Computes the meridional arc distance {@code M} from the equator to a given
     * latitude, in metres, using the standard WGS84 series expansion.
     *
     * @param latRad latitude in radians
     * @return the meridional arc length in metres
     */
    private static double meridionalArc(double latRad) {
        return A * (
                (1.0 - E2 / 4.0 - 3.0 * E2 * E2 / 64.0 - 5.0 * E2 * E2 * E2 / 256.0)  * latRad
              - (3.0 * E2 / 8.0 + 3.0 * E2 * E2 / 32.0 + 45.0 * E2 * E2 * E2 / 1024.0) * Math.sin(2.0 * latRad)
              + (15.0 * E2 * E2 / 256.0 + 45.0 * E2 * E2 * E2 / 1024.0)                 * Math.sin(4.0 * latRad)
              - (35.0 * E2 * E2 * E2 / 3072.0)                                           * Math.sin(6.0 * latRad));
    }

    /**
     * Normalises a longitude value to the half-open interval {@code [-180, 180)}.
     *
     * @param lonDeg longitude in decimal degrees (any value)
     * @return the equivalent longitude in {@code [-180, 180)}
     */
    private static double normalizeLongitude(double lonDeg) {
        lonDeg = lonDeg % 360.0;
        if (lonDeg >= 180.0)  lonDeg -= 360.0;
        if (lonDeg < -180.0)  lonDeg += 360.0;
        return lonDeg;
    }
}