package edu.cnu.mdi.mapping.render;

import java.awt.Point;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.mapping.shapefile.ShapeFeatureRenderer;

/**
 * Implemented by map renderers that support mouse-over hit testing.
 *
 * <p>Any layer that wants to contribute text to the feedback panel or the
 * hover popup when the user moves the cursor over a feature should implement
 * this interface and return a formatted string describing the feature under
 * the cursor.</p>
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>{@link #pick(Point, IContainer)} must never throw; it should return
 *       {@code null} silently if no feature is hit or if the pick cache has
 *       not yet been populated by a render call.</li>
 *   <li>The returned string may contain multiple lines separated by
 *       {@code '\n'} if the caller splits and displays them individually, but
 *       a single line is the common case.</li>
 *   <li>Implementations are expected to use a cached version of the projected
 *       geometry built during the most recent
 *       {@link ShapeFeatureRenderer#render} call, so picking is O(n) in the
 *       number of cached features and does not require re-projecting
 *       coordinates.</li>
 * </ul>
 *
 * <h2>Render-before-pick dependency</h2>
 * <p>Because picking uses the cached projected geometry, {@code pick} must be
 * called <em>after</em> at least one render call. In practice this is always
 * satisfied because mouse-over events cannot fire before the component is
 * painted. Callers should still null-check the return value.</p>
 *
 * <h2>Current implementors</h2>
 * <ul>
 *   <li>{@link ShapeFeatureRenderer} — picks polygon, polyline, and point
 *       features from arbitrary shapefile layers and returns the values of
 *       the configured tooltip fields.</li>
 * </ul>
 */
public interface IPickable {

    /**
     * Returns a human-readable description of the feature under the given
     * mouse position, or {@code null} if no feature is hit.
     *
     * <p>The description is typically one line suitable for the feedback
     * panel (e.g. {@code "Lake Superior"} or
     * {@code "Amazon River  length: 6400 km"}).</p>
     *
     * @param mouseLocal the cursor position in the container's local
     *                   (screen) coordinate space; must not be {@code null}
     * @param container  the container providing coordinate transforms;
     *                   must not be {@code null}
     * @return a non-empty description string, or {@code null} if no feature
     *         was hit
     */
    String pick(Point mouseLocal, IContainer container);
}