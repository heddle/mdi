package edu.cnu.mdi.graphics.drawable;

import java.awt.Graphics;
import java.awt.Graphics2D;

import edu.cnu.mdi.view.Container2D;

/**
 * A drawable object that can render itself into a {@link Container2D}.
 * <p>
 * Implementations are typically small, focused drawing helpers such as
 * overlays, annotations, or extra user-supplied graphics that are
 * rendered on top of a view's main content.
 */
public interface Drawable {

    /**
     * Render this drawable.
     *
     * @param g2        the 2D graphics context to draw with. The caller is
     *                  responsible for restoring any graphics state (transform,
     *                  clip, composite, etc.) if needed.
     * @param container the container being rendered into; may be used to
     *                  query coordinate transforms, world bounds, etc.
     */
    void draw(Graphics2D g2, Container2D container);
    
    /**
     * Render this drawable.
     *
     * @param g         the oldD graphics context to draw with. The caller is
     *                  responsible for restoring any graphics state (transform,
     *                  clip, composite, etc.) if needed.
     * @param container the container being rendered into; may be used to
     *                  query coordinate transforms, world bounds, etc.
     */
	default void draw(Graphics g, Container2D container) {
		draw((Graphics2D) g, container);
	}
   

    /**
     * Mark this drawable as "dirty".
     * <p>
     * Implementations can use this to clear cached pixel-based data
     * (e.g., precomputed polygons) so they will be recomputed on the next draw.
     * The default implementation does nothing.
     *
     * @param dirty {@code true} if cached data should be considered invalid.
     */
    default void setDirty(boolean dirty) {
        // default: no-op
    }

    /**
     * Called before this drawable is removed from a list or registry.
     * <p>
     * Implementations can use this to release resources or unregister listeners.
     * The default implementation does nothing.
     */
    default void prepareForRemoval() {
        // default: no-op
    }

    /**
     * Whether this drawable is currently visible.
     * <p>
     * Callers should respect this flag and skip drawing when it returns {@code false}.
     * Default is {@code true}.
     *
     * @return {@code true} if the object should be rendered; {@code false} otherwise.
     */
    default boolean isVisible() {
        return true;
    }

    /**
     * Set whether this drawable should be visible.
     * <p>
     * The default implementation ignores the flag. Implementations that
     * track visibility should override this together with {@link #isVisible()}.
     *
     * @param visible the new visibility state.
     */
    default void setVisible(boolean visible) {
        // default: no-op
    }

    /**
     * Whether this drawable is currently enabled.
     * <p>
     * "Enabled" can be used by callers to decide, for example, whether to
     * process interaction or feedback for this drawable. Default is {@code true}.
     *
     * @return {@code true} if the object is enabled; {@code false} otherwise.
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Set whether this drawable is enabled.
     * <p>
     * The default implementation ignores the flag. Implementations that
     * track enabled state should override this together with {@link #isEnabled()}.
     *
     * @param enabled the new enabled state.
     */
    default void setEnabled(boolean enabled) {
        // default: no-op
    }

    /**
     * Get a human-readable name for this drawable.
     * <p>
     * The name can be used in visibility tables, debug logs, or UI lists.
     * The default implementation returns this class's simple name.
     *
     * @return the name of the drawable.
     */
    default String getName() {
        return getClass().getSimpleName();
    }
}
