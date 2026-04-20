package edu.cnu.mdi.mapping.graphics;

import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A shape representing a projected map feature, consisting of one or more
 * paths and a flag indicating whether the shape is closed (i.e. a polygon).
 *
 * <p>Paths are stored as unmodifiable lists of {@link Path2D.Double} instances
 * to ensure immutability and thread safety. The {@code closed} flag indicates
 * whether the paths should be treated as closed polygons (e.g. for filling)
 * or as open polylines.</p>
 */
public class ProjectedMapShape {

	// Paths representing the shape. Each path is a sequence of connected line
	// segments. For polygons, the first and last points of each path are implicitly
	// connected, but the paths themselves are stored as open sequences of points.
    private final List<Path2D.Double> paths;
    
    // Flag indicating whether the shape is closed (i.e. a polygon) or open (i.e. a polyline).
    private final boolean closed;

    /**
	 * Constructs a new ProjectedMapShape with the given paths and closed flag.
	 *
	 * @param paths  list of paths representing the shape; if {@code null}, an empty list is used
	 * @param closed {@code true} if the shape is closed (polygon), {@code false} if open (polyline)
	 */
    public ProjectedMapShape(List<Path2D.Double> paths, boolean closed) {
        this.paths = (paths == null) ? Collections.emptyList()
                                     : Collections.unmodifiableList(new ArrayList<>(paths));
        this.closed = closed;
    }

    /**
     * Returns the list of paths representing this shape.
     * @return unmodifiable list of paths; never {@code null}
     */
    public List<Path2D.Double> getPaths() {
        return paths;
    }

    /**
	 * Returns whether this shape is closed (i.e. a polygon) or open (i.e. a polyline).
	 * @return {@code true} if the shape is closed, {@code false} if open
	 */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Returns whether this shape has no paths (i.e. is empty).
     * @return {@code true} if the shape has no paths, {@code false} otherwise
     */
    public boolean isEmpty() {
        return paths.isEmpty();
    }

     /**
	  * Computes and returns the bounding rectangle that fully contains all paths of this shape.
	  * @return bounding rectangle of the shape; if the shape is empty, returns a rectangle with zero width and height at the origin
	  */
    public Rectangle getBounds() {
        Rectangle r = null;
        for (Path2D.Double path : paths) {
            Rectangle pr = path.getBounds();
            if (r == null) {
                r = new Rectangle(pr);
            } else {
                r.add(pr);
            }
        }
        return (r == null) ? new Rectangle() : r;
    }
}