package edu.cnu.mdi.graphics;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.UIScale;

import edu.cnu.mdi.log.Log;

/**
 * Centralized image/icon loading for MDI using classpath resources.
 * <p>
 * This manager is designed for applications packaged as a JAR where all icons and
 * images live under {@code src/main/resources} and are loaded from the classpath
 * (e.g. {@code "icons/zoom_in.svg"}).
 * </p>
 *
 * <h2>FlatLaf considerations</h2>
 * <ul>
 *   <li><b>SVG UI icons:</b> Use {@link FlatSVGIcon}. FlatLaf will handle HiDPI scaling
 *       correctly and icons remain crisp at all scales.</li>
 *   <li><b>Raster UI icons:</b> When an icon is requested at a "logical" size (e.g. 16),
 *       this class scales to device pixels via {@link UIScale#scale(int)}.</li>
 *   <li><b>Do not eagerly initialize icons in static initializers.</b> Look-and-feel setup
 *       can affect scaling; this class caches lazily.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 * <p>
 * All caches are thread-safe. Icon creation may occur off the EDT, but icons are
 * typically requested during UI construction on the EDT.
 * </p>
 *
 * <h2>Resource paths</h2>
 * <p>
 * Paths are resolved via {@link ClassLoader#getResourceAsStream(String)} and should
 * be relative (no leading slash). For example, if the file is at
 * {@code src/main/resources/icons/zoom_in.svg}, use {@code "icons/zoom_in.svg"}.
 * </p>
 */
public final class ImageManager {

    /** Singleton instance. */
    private static volatile ImageManager instance;

    /** Cache of original raster images (unscaled) keyed by resource path. */
    private final ConcurrentHashMap<String, BufferedImage> rasterCache = new ConcurrentHashMap<>();

    /** Cache of icons (SVG or raster) keyed by "path@WxH". */
    private final ConcurrentHashMap<String, Icon> iconCache = new ConcurrentHashMap<>();

    private ImageManager() {
        // singleton
    }

    /**
     * Get the singleton instance.
     *
     * @return the singleton {@link ImageManager}
     */
    public static ImageManager getInstance() {
        if (instance == null) {
            synchronized (ImageManager.class) {
                if (instance == null) {
                    instance = new ImageManager();
                }
            }
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // UI Icon API (recommended)
    // -------------------------------------------------------------------------

    /**
     * Load a UI icon at a given logical size.
     * <p>
     * For SVG resources, returns a {@link FlatSVGIcon} of the requested size.
     * For raster resources (png/jpg/gif), loads the image and scales it to the
     * requested logical size, then wraps it in an {@link ImageIcon}.
     * </p>
     *
     * <p>
     * "Logical size" means the size in typical UI coordinates (e.g. 16, 20, 24).
     * Under HiDPI displays, FlatLaf uses a scale factor; for raster icons we convert
     * logical size to device pixels using {@link UIScale#scale(int)}.
     * </p>
     *
     * @param resourcePath classpath resource path (e.g. {@code "icons/zoom_in.svg"})
     * @param logicalW requested logical width (e.g. 16, 20, 24)
     * @param logicalH requested logical height
     * @return an {@link Icon}, or {@code null} if the resource cannot be found/read
     * @throws NullPointerException if {@code resourcePath} is null
     * @throws IllegalArgumentException if {@code logicalW <= 0} or {@code logicalH <= 0}
     */
    public Icon loadUiIcon(String resourcePath, int logicalW, int logicalH) {
        Objects.requireNonNull(resourcePath, "resourcePath");
        if (logicalW <= 0 || logicalH <= 0) {
            throw new IllegalArgumentException("logicalW and logicalH must be positive.");
        }

        final String key = resourcePath + "@" + logicalW + "x" + logicalH;
        return iconCache.computeIfAbsent(key, k -> createUiIcon(resourcePath, logicalW, logicalH));
    }

    /**
     * Convenience for square UI icons (e.g. 16x16, 20x20).
     *
     * @param resourcePath classpath resource path
     * @param logicalSize logical size in both dimensions
     * @return an {@link Icon}, or {@code null} if the resource cannot be found/read
     */
    public Icon loadUiIcon(String resourcePath, int logicalSize) {
        return loadUiIcon(resourcePath, logicalSize, logicalSize);
    }

    /**
     * Create (uncached) UI icon, choosing SVG vs raster handling.
     */
    private Icon createUiIcon(String resourcePath, int logicalW, int logicalH) {
        final String lower = resourcePath.toLowerCase();

        if (lower.endsWith(".svg")) {
            // FlatLaf-native SVG icon. Crisp at any scale.
            if (!resourceExists(resourcePath)) {
                Log.getInstance().warning("SVG icon resource not found: " + resourcePath);
                return null;
            }
            return new FlatSVGIcon(resourcePath, logicalW, logicalH);
        }

        // Raster icon: load original and scale to device pixels using UIScale.
        BufferedImage src = loadRaster(resourcePath);
        if (src == null) {
            return null;
        }

        int deviceW = UIScale.scale(logicalW);
        int deviceH = UIScale.scale(logicalH);

        Image scaled = src.getScaledInstance(deviceW, deviceH, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    // -------------------------------------------------------------------------
    // Raster image API (for non-UI uses)
    // -------------------------------------------------------------------------

    /**
     * Load a raster image (PNG/JPG/GIF/etc.) from the classpath as a {@link BufferedImage}.
     * <p>
     * This returns the original image at its native pixel dimensions. It is cached.
     * </p>
     *
     * @param resourcePath classpath resource path (e.g. {@code "images/background.png"})
     * @return the image, or {@code null} if the resource cannot be found/read
     * @throws NullPointerException if {@code resourcePath} is null
     */
    public BufferedImage loadRaster(String resourcePath) {
        Objects.requireNonNull(resourcePath, "resourcePath");
        return rasterCache.computeIfAbsent(resourcePath, this::readRasterUnchecked);
    }

    /**
     * Load an arbitrary image as an {@link Image}.
     * <p>
     * This is a thin convenience wrapper around {@link #loadRaster(String)} for code that
     * expects {@link Image} instead of {@link BufferedImage}.
     * </p>
     *
     * @param resourcePath classpath resource path
     * @return the image, or {@code null} if not found/read
     */
    public Image loadImage(String resourcePath) {
        BufferedImage bi = loadRaster(resourcePath);
        return bi;
    }

    /**
     * Load an {@link ImageIcon} without applying any HiDPI logical sizing.
     * <p>
     * Prefer {@link #loadUiIcon(String, int)} for toolbar/menu icons. This method is intended
     * for legacy usage where the caller expects the icon at its native pixel size.
     * </p>
     *
     * @param resourcePath classpath resource path
     * @return the image icon, or {@code null} if not found/read
     */
    public ImageIcon loadImageIcon(String resourcePath) {
        BufferedImage bi = loadRaster(resourcePath);
        return (bi == null) ? null : new ImageIcon(bi);
    }

    /**
     * Read a raster image from the classpath, returning null on failure.
     * This method is used by the cache and must not throw.
     */
    private BufferedImage readRasterUnchecked(String resourcePath) {
        try (InputStream in = getResourceAsStream(resourcePath)) {
            if (in == null) {
                Log.getInstance().warning("Raster resource not found: " + resourcePath);
                return null;
            }
            return ImageIO.read(in);
        } catch (IOException e) {
            Log.getInstance().exception(e);
            Log.getInstance().warning("Failed to read raster resource: " + resourcePath);
            return null;
        }
    }

    /**
     * Returns true if a resource exists on the classpath.
     */
    private boolean resourceExists(String resourcePath) {
        try (InputStream in = getResourceAsStream(resourcePath)) {
            return in != null;
        } catch (IOException ioe) {
            // Should not occur for ByteArray-backed streams; be safe.
            return false;
        }
    }

    /**
     * Get a resource as a stream from the current class loader.
     *
     * @param resourcePath classpath resource path
     * @return stream or null if not found
     */
    private InputStream getResourceAsStream(String resourcePath) {
        return getClass().getClassLoader().getResourceAsStream(resourcePath);
    }


    // -------------------------------------------------------------------------
    // Cache management
    // -------------------------------------------------------------------------

    /**
     * Clear all cached images and icons.
     * <p>
     * Useful if you support runtime theme changes and want to rebuild icons, or
     * when memory pressure suggests cache eviction.
     * </p>
     */
    public void clearCaches() {
        rasterCache.clear();
        iconCache.clear();
    }
}
