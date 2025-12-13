package edu.cnu.mdi.graphics.toolbar;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.swing.ImageIcon;

import edu.cnu.mdi.graphics.ImageManager;

/**
 * Creates and caches {@link Cursor} instances for tools.
 * <p>
 * Swing/AWT custom cursors have platform-dependent limitations (notably maximum
 * cursor size). This manager:
 * </p>
 * <ul>
 *   <li>loads cursor images via {@link ImageManager}</li>
 *   <li>clamps hotspot coordinates</li>
 *   <li>scales images down to the platform "best cursor size" when necessary</li>
 *   <li>caches the resulting {@link Cursor} objects for reuse</li>
 * </ul>
 * <p>
 * If a custom cursor cannot be created (missing image, unsupported size, etc.),
 * {@link Cursor#getDefaultCursor()} is returned.
 * </p>
 *
 * @author heddle
 */
public final class CursorManager {

    /** Cache of generated cursors keyed by (resourcePath, hotX, hotY). */
    private final ConcurrentMap<Key, Cursor> cache = new ConcurrentHashMap<>();

    /** Toolkit used to create cursors. */
    private final Toolkit toolkit = Toolkit.getDefaultToolkit();

    /**
     * Get a custom cursor created from an image resource and hotspot.
     * <p>
     * Results are cached, so repeated calls with the same arguments are cheap.
     * </p>
     *
     * @param resourcePath classpath/resource path understood by {@link ImageManager}
     *                     (e.g. {@code "images/pointercursor.gif"}). Must not be null.
     * @param hotX         hotspot x in pixels (will be clamped into valid range).
     * @param hotY         hotspot y in pixels (will be clamped into valid range).
     *
     * @return a cached/created custom cursor, or the default cursor if creation fails.
     */
    public Cursor custom(String resourcePath, int hotX, int hotY) {
        Objects.requireNonNull(resourcePath, "resourcePath must not be null");
        return cache.computeIfAbsent(new Key(resourcePath, hotX, hotY), this::createCursor);
    }

    /**
     * Convenience: load a cursor with hotspot at (0,0).
     *
     * @param resourcePath cursor image resource path.
     * @return a cached/created custom cursor, or default cursor on failure.
     */
    public Cursor custom(String resourcePath) {
        return custom(resourcePath, 0, 0);
    }

    /**
     * Clear the internal cursor cache.
     * <p>
     * Normally you never need this, but it can be useful during development
     * if cursor resources are changing.
     * </p>
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Create a cursor for a cache key.
     */
    private Cursor createCursor(Key k) {
        try {
            Image img = loadImage(k.resourcePath);
            if (img == null) {
                return Cursor.getDefaultCursor();
            }

            int w = img.getWidth(null);
            int h = img.getHeight(null);
            if (w <= 0 || h <= 0) {
                return Cursor.getDefaultCursor();
            }

            // Platform constraint: best cursor size. (0,0) => custom cursors not supported.
            Dimension best = toolkit.getBestCursorSize(w, h);
            if (best == null || best.width <= 0 || best.height <= 0) {
                return Cursor.getDefaultCursor();
            }

            Image use = img;

            // If image exceeds platform best size, scale down.
            if (w > best.width || h > best.height) {
                use = img.getScaledInstance(best.width, best.height, Image.SCALE_SMOOTH);
                w = best.width;
                h = best.height;
            } else {
                // Some platforms prefer exact best size; if it differs, leave image as-is.
                // Scaling up usually looks bad, so we do not scale up.
            }

            // Clamp hotspot.
            int hx = clamp(k.hotX, 0, Math.max(0, w - 1));
            int hy = clamp(k.hotY, 0, Math.max(0, h - 1));

            return toolkit.createCustomCursor(use, new Point(hx, hy), k.resourcePath);

        } catch (Exception ex) {
            return Cursor.getDefaultCursor();
        }
    }

    /**
     * Load an image for the cursor.
     */
    private Image loadImage(String resourcePath) {
        ImageIcon icon = ImageManager.getInstance().loadImageIcon(resourcePath);
        return (icon == null) ? null : icon.getImage();
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /**
     * Cache key for cursor creation.
     */
    private record Key(String resourcePath, int hotX, int hotY) { }
}
