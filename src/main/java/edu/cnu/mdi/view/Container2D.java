package edu.cnu.mdi.view;

import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import edu.cnu.mdi.graphics.drawable.Drawable;

/**
 * A 2D drawing container with a world coordinate system and conversion
 * to pixel (screen) coordinates. Implementations are typically Swing
 * components that render one or more {@link Drawable}s.
 */
public interface Container2D {

    /**
     * Get the world bounds currently mapped into this container.
     *
     * @return the world bounds as a {@link Rectangle2D}.
     */
    Rectangle2D getWorldBounds();

    /**
     * Set the world bounds to display in this container.
     *
     * @param world the new world bounds. Must have positive width and height.
     */
    void setWorldBounds(Rectangle2D world);

    /**
     * Get the affine transform that maps world coordinates to screen
     * (pixel) coordinates.
     *
     * @return the world-to-screen transform.
     */
    AffineTransform getWorldToScreenTransform();

    /**
     * Get the affine transform that maps screen (pixel) coordinates
     * back to world coordinates.
     *
     * @return the screen-to-world transform.
     */
    AffineTransform getScreenToWorldTransform();

    /**
     * Convert a world point to a screen (pixel) point.
     *
     * @param world a point in world coordinates.
     * @return the corresponding point in pixel coordinates.
     */
    Point worldToScreen(Point2D world);

    /**
     * Convert a screen (pixel) point to a world point.
     *
     * @param screen a point in pixel coordinates.
     * @return the corresponding point in world coordinates.
     */
    Point2D screenToWorld(Point screen);

    /**
     * Add a drawable to this container.
     *
     * @param drawable the drawable to add.
     */
    void addDrawable(Drawable drawable);

    /**
     * Remove a drawable from this container.
     *
     * @param drawable the drawable to remove.
     */
    void removeDrawable(Drawable drawable);

    /**
     * Get the current drawables.
     * <p>
     * Callers should treat the returned list as read-only.
     *
     * @return a list of drawables in this container.
     */
    List<Drawable> getDrawables();

    /**
     * Mark the container (and optionally its drawables) as dirty.
     * Implementations should use this to force recomputation of
     * cached transforms or geometry on the next paint.
     *
     * @param dirty {@code true} if the container needs to recompute state.
     */
    void setDirty(boolean dirty);

    /**
     * Whether the container is currently considered dirty.
     *
     * @return {@code true} if internal cached state needs recomputation.
     */
    boolean isDirty();
}
