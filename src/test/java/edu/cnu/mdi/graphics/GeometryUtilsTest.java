package edu.cnu.mdi.graphics;

import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GeometryUtils}.  
 *
 * <p>This test suite verifies:</p>
 * <ul>
 *     <li>Corner extraction from rectangles</li>
 *     <li>Rectangle construction from two points (including null handling)</li>
 *     <li>Aspect-ratio-preserving rectangle adjustment</li>
 *     <li>Point-on-line hit-testing with default and explicit tolerances</li>
 *     <li>Special cases such as tiny segments and boundary conditions</li>
 * </ul>
 *
 * <p>All tests use JUnit 5 Jupiter API.</p>
 */
public class GeometryUtilsTest {

    /**
     * Ensures {@link GeometryUtils#rectangleToPoints(Rectangle)} returns
     * the four corners in a consistent clockwise order starting from top-left.
     */
    @Test
    void rectangleToPoints_returnsFourCornersInOrder() {
        Rectangle rect = new Rectangle(10, 20, 30, 40);
        Point[] points = GeometryUtils.rectangleToPoints(rect);

        assertNotNull(points, "Points array should not be null");
        assertEquals(4, points.length, "Rectangle should have 4 corner points");

        // (x, y, w, h) = (10, 20, 30, 40) → corners:
        // (10, 20), (40, 20), (40, 60), (10, 60)
        assertEquals(new Point(10, 20), points[0], "Top-left corner mismatch");
        assertEquals(new Point(40, 20), points[1], "Top-right corner mismatch");
        assertEquals(new Point(40, 60), points[2], "Bottom-right corner mismatch");
        assertEquals(new Point(10, 60), points[3], "Bottom-left corner mismatch");
    }

    /**
     * Verifies that constructing a rectangle from two non-null points works
     * regardless of the order in which the points are supplied.
     */
    @Test
    void rectangleFromPoints_bothNonNull_orderIndependent() {
        Point p1 = new Point(10, 20);
        Point p2 = new Point(40, 60);

        Rectangle rect1 = GeometryUtils.rectangleFromPoints(p1, p2);
        Rectangle rect2 = GeometryUtils.rectangleFromPoints(p2, p1);

        assertNotNull(rect1);
        assertNotNull(rect2);

        assertEquals(10, rect1.x);
        assertEquals(20, rect1.y);
        assertEquals(30, rect1.width);
        assertEquals(40, rect1.height);

        // Should be symmetric with respect to point order
        assertEquals(rect1, rect2);
    }

    /**
     * Tests that when one point is null, the method returns a zero-sized
     * rectangle located at the non-null point.
     */
    @Test
    void rectangleFromPoints_oneNull_returnsZeroSizeRectangleAtOtherPoint() {
        Point p = new Point(5, 7);

        Rectangle rect1 = GeometryUtils.rectangleFromPoints(p, null);
        Rectangle rect2 = GeometryUtils.rectangleFromPoints(null, p);

        assertEquals(new Rectangle(5, 7, 0, 0), rect1);
        assertEquals(new Rectangle(5, 7, 0, 0), rect2);
    }

    /**
     * Confirms that passing two null points results in a null rectangle.
     */
    @Test
    void rectangleFromPoints_bothNull_returnsNull() {
        assertNull(GeometryUtils.rectangleFromPoints(null, null));
    }

    /**
     * Tests aspect-ratio-preserving adjustment when the reference rectangle
     * is taller than it is wide. Drag direction should be preserved.
     */
    @Test
    void rectangleARFixedAdjust_preservesAspectRatio_whenReferenceIsTaller() {
        // Reference rectangle: width 100, height 200 → aspect = 0.5
        Rectangle ref = new Rectangle(0, 0, 100, 200);
        Point p0 = new Point(10, 10);
        Point p = new Point(50, 90); // arbitrary drag point

        GeometryUtils.rectangleARFixedAdjust(ref, p0, p);

        Rectangle adjusted = GeometryUtils.rectangleFromPoints(p0, p);
        double width = adjusted.getWidth();
        double height = adjusted.getHeight();

        assertTrue(height > 0, "Height should be positive after adjustment");
        double aspect = width / height;

        assertEquals(0.5, aspect, 0.05, "Aspect ratio should be close to reference");
    }

    /**
     * Tests aspect-ratio-preserving adjustment when the reference rectangle
     * is wider than it is tall.
     */
    @Test
    void rectangleARFixedAdjust_preservesAspectRatio_whenReferenceIsWider() {
        // Reference rectangle: width 200, height 100 → aspect = 2.0
        Rectangle ref = new Rectangle(0, 0, 200, 100);
        Point p0 = new Point(10, 10);
        Point p = new Point(70, 50); // arbitrary drag point

        GeometryUtils.rectangleARFixedAdjust(ref, p0, p);

        Rectangle adjusted = GeometryUtils.rectangleFromPoints(p0, p);
        double width = adjusted.getWidth();
        double height = adjusted.getHeight();

        assertTrue(width > 0, "Width should be positive after adjustment");
        double aspect = width / height;

        assertEquals(2.0, aspect, 0.05, "Aspect ratio should be close to reference");
    }

    /**
     * Verifies that {@link GeometryUtils#rectangleARFixed(Rectangle, Point, Point)}
     * correctly returns a new rectangle that matches the reference aspect ratio.
     */
    @Test
    void rectangleARFixed_returnsRectangleWithFixedAspect() {
        Rectangle ref = new Rectangle(0, 0, 160, 90); // 16:9
        Point p0 = new Point(20, 30);
        Point p = new Point(120, 130);

        Rectangle adjusted = GeometryUtils.rectangleARFixed(ref, p0, p);

        double width = adjusted.getWidth();
        double height = adjusted.getHeight();
        double aspect = width / height;
        double refAspect = ref.getWidth() / ref.getHeight();

        assertEquals(refAspect, aspect, 0.05, "Aspect ratio should match reference");
    }

    /**
     * Ensures that {@link GeometryUtils#pointOnLine(Point, Point, Point)}
     * rejects cases where one or more arguments are null.
     */
    @Test
    void pointOnLine_withPoints_returnsFalseForNulls() {
        assertFalse(GeometryUtils.pointOnLine(null, new Point(0, 0), new Point(10, 10)));
        assertFalse(GeometryUtils.pointOnLine(new Point(5, 5), null, new Point(10, 10)));
        assertFalse(GeometryUtils.pointOnLine(new Point(5, 5), new Point(0, 0), null));
    }

    /**
     * Tests detection of points on or near a horizontal line segment using
     * the default tolerance.
     */
    @Test
    void pointOnLine_withPoints_detectsHitNearCenter() {
        Point start = new Point(0, 0);
        Point end = new Point(10, 0);

        // On the line
        Point pOn = new Point(5, 0);
        assertTrue(GeometryUtils.pointOnLine(pOn, start, end), "Point on line should be a hit");

        // Slightly off the line but within default tolerance
        Point pNear = new Point(5, 2);
        assertTrue(GeometryUtils.pointOnLine(pNear, start, end), "Point near line should be a hit");
    }

    /**
     * Ensures that points far away from the segment are correctly rejected.
     */
    @Test
    void pointOnLine_withPoints_rejectsFarPoints() {
        Point start = new Point(0, 0);
        Point end = new Point(10, 0);

        Point pFar = new Point(5, 100);
        assertFalse(GeometryUtils.pointOnLine(pFar, start, end), "Point far from line should not be a hit");
    }

    /**
     * Tests hit-detection on a horizontal line segment where the X axis
     * is the dominant axis.
     */
    @Test
    void pointOnLine_withTolerance_usesMajorAxisX() {
        int sx = 0, sy = 0;
        int ex = 100, ey = 0;

        // Exact middle
        assertTrue(GeometryUtils.pointOnLine(50, 0, sx, sy, ex, ey, 0.1),
                "Exact point on horizontal segment should be a hit");

        // Slightly off but within tolerance
        assertTrue(GeometryUtils.pointOnLine(50, 1, sx, sy, ex, ey, 2.0),
                "Point within tolerance in y should be a hit");

        // Outside tolerance
        assertFalse(GeometryUtils.pointOnLine(50, 10, sx, sy, ex, ey, 2.0),
                "Point outside tolerance in y should not be a hit");
    }

    /**
     * Tests hit-detection on a vertical line segment where the Y axis
     * is the dominant axis.
     */
    @Test
    void pointOnLine_withTolerance_usesMajorAxisY() {
        int sx = 0, sy = 0;
        int ex = 0, ey = 100;

        // Exact middle
        assertTrue(GeometryUtils.pointOnLine(0, 50, sx, sy, ex, ey, 0.1),
                "Exact point on vertical segment should be a hit");

        // Slightly off but within tolerance
        assertTrue(GeometryUtils.pointOnLine(1, 50, sx, sy, ex, ey, 2.0),
                "Point within tolerance in x should be a hit");

        // Outside tolerance
        assertFalse(GeometryUtils.pointOnLine(10, 50, sx, sy, ex, ey, 2.0),
                "Point outside tolerance in x should not be a hit");
    }

    /**
     * Tests that extremely small segments (under 2px in both x and y)
     * are ignored by the line hit-test algorithm.
     */
    @Test
    void pointOnLine_rejectsVeryShortSegments() {
        assertFalse(GeometryUtils.pointOnLine(0, 0, 0, 0, 1, 1, 5.0),
                "Very short segments should not count as hits");
    }
}
