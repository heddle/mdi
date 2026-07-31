package edu.cnu.mdi.mapping.shapefile;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Clips periodic geographic polygon rings to a longitude/latitude rectangle.
 *
 * <p>The input longitudes may be wrapped to the usual {@code (-π, π]} range.
 * The ring is first unwrapped so consecutive vertices remain on the same
 * continuous branch, then shifted copies are clipped against the requested
 * longitude interval. This preserves polygons that cross a movable map seam.</p>
 */
final class GeographicPolygonClipper {

    private static final double TWO_PI = 2.0 * Math.PI;
    private static final double EPSILON = 1.0e-12;

    private GeographicPolygonClipper() {}

    /**
     * Clips a periodic ring to the supplied geographic rectangle.
     *
     * @param ring   closed or open polygon ring in longitude/latitude radians
     * @param minLon minimum unwrapped longitude
     * @param maxLon maximum unwrapped longitude
     * @param minLat minimum latitude
     * @param maxLat maximum latitude
     * @return clipped polygon pieces, each containing at least three vertices
     */
    static List<List<Point2D.Double>> clipPeriodicRing(
            List<Point2D.Double> ring,
            double minLon,
            double maxLon,
            double minLat,
            double maxLat) {

        if (ring.size() < 3) return Collections.emptyList();

        List<Point2D.Double> unwrapped = unwrap(ring);
        if (unwrapped.size() < 3) return Collections.emptyList();

        double ringMinLon = Double.POSITIVE_INFINITY;
        double ringMaxLon = Double.NEGATIVE_INFINITY;
        for (Point2D.Double point : unwrapped) {
            ringMinLon = Math.min(ringMinLon, point.x);
            ringMaxLon = Math.max(ringMaxLon, point.x);
        }

        int firstShift = (int) Math.ceil((minLon - ringMaxLon) / TWO_PI);
        int lastShift  = (int) Math.floor((maxLon - ringMinLon) / TWO_PI);
        List<List<Point2D.Double>> result = new ArrayList<>();

        for (int shiftIndex = firstShift; shiftIndex <= lastShift; shiftIndex++) {
            double shift = shiftIndex * TWO_PI;
            List<Point2D.Double> shifted = new ArrayList<>(unwrapped.size());
            for (Point2D.Double point : unwrapped) {
                shifted.add(new Point2D.Double(point.x + shift, point.y));
            }

            List<Point2D.Double> clipped = clipVertical(shifted, minLon, true);
            clipped = clipVertical(clipped, maxLon, false);
            clipped = clipHorizontal(clipped, minLat, true);
            clipped = clipHorizontal(clipped, maxLat, false);
            clipped = removeAdjacentDuplicates(clipped);

            if (clipped.size() >= 3) {
                result.add(Collections.unmodifiableList(clipped));
            }
        }

        return Collections.unmodifiableList(result);
    }

    private static List<Point2D.Double> unwrap(List<Point2D.Double> ring) {
        int limit = ring.size();
        if (limit > 1 && samePoint(ring.get(0), ring.get(limit - 1))) limit--;
        if (limit == 0) return Collections.emptyList();

        List<Point2D.Double> result = new ArrayList<>(limit);
        Point2D.Double first = ring.get(0);
        double previousLon = first.x;
        result.add(new Point2D.Double(previousLon, first.y));

        for (int i = 1; i < limit; i++) {
            Point2D.Double point = ring.get(i);
            double lon = point.x;
            while (lon - previousLon > Math.PI) lon -= TWO_PI;
            while (lon - previousLon < -Math.PI) lon += TWO_PI;
            result.add(new Point2D.Double(lon, point.y));
            previousLon = lon;
        }
        return result;
    }

    private static List<Point2D.Double> clipVertical(
            List<Point2D.Double> input, double boundary, boolean keepGreater) {
        return clip(input,
                point -> keepGreater ? point.x >= boundary : point.x <= boundary,
                (start, end) -> {
                    double t = (boundary - start.x) / (end.x - start.x);
                    return new Point2D.Double(boundary,
                            start.y + t * (end.y - start.y));
                });
    }

    private static List<Point2D.Double> clipHorizontal(
            List<Point2D.Double> input, double boundary, boolean keepGreater) {
        return clip(input,
                point -> keepGreater ? point.y >= boundary : point.y <= boundary,
                (start, end) -> {
                    double t = (boundary - start.y) / (end.y - start.y);
                    return new Point2D.Double(
                            start.x + t * (end.x - start.x), boundary);
                });
    }

    private static List<Point2D.Double> clip(
            List<Point2D.Double> input,
            InsideTest inside,
            Intersection intersection) {
        if (input.isEmpty()) return Collections.emptyList();

        List<Point2D.Double> output = new ArrayList<>();
        Point2D.Double start = input.get(input.size() - 1);
        boolean startInside = inside.test(start);

        for (Point2D.Double end : input) {
            boolean endInside = inside.test(end);
            if (endInside) {
                if (!startInside) output.add(intersection.of(start, end));
                output.add(end);
            } else if (startInside) {
                output.add(intersection.of(start, end));
            }
            start = end;
            startInside = endInside;
        }
        return output;
    }

    private static List<Point2D.Double> removeAdjacentDuplicates(
            List<Point2D.Double> input) {
        if (input.isEmpty()) return input;
        List<Point2D.Double> result = new ArrayList<>(input.size());
        for (Point2D.Double point : input) {
            if (result.isEmpty() || !samePoint(result.get(result.size() - 1), point)) {
                result.add(point);
            }
        }
        if (result.size() > 1 && samePoint(result.get(0), result.get(result.size() - 1))) {
            result.remove(result.size() - 1);
        }
        return result;
    }

    private static boolean samePoint(Point2D.Double first, Point2D.Double second) {
        return Math.abs(first.x - second.x) <= EPSILON
                && Math.abs(first.y - second.y) <= EPSILON;
    }

    @FunctionalInterface
    private interface InsideTest {
        boolean test(Point2D.Double point);
    }

    @FunctionalInterface
    private interface Intersection {
        Point2D.Double of(Point2D.Double start, Point2D.Double end);
    }
}
