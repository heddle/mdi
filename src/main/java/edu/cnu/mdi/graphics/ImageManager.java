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
 * This manager is designed for applications packaged as a JAR where icons/images
 * live under {@code src/main/resources} and are loaded from the classpath. This
 * works the same way when MDI is used as a Maven dependency: resources packaged
 * inside the {@code mdi-<version>.jar} remain accessible via the classpath.
 * </p>
 *
 * <h2>Resource path convention</h2>
 * <p>
 * Resources are resolved using {@link Class#getResourceAsStream(String)} anchored
 * to {@code ImageManager.class}. This class accepts resource paths either:
 * </p>
 * <ul>
 *   <li><b>Absolute:</b> starting with {@code "/"} (recommended), e.g.
 *       {@code "/edu/cnu/mdi/icons/zoom_in.svg"}</li>
 *   <li><b>Classpath-relative:</b> without a leading {@code "/"}, e.g.
 *       {@code "edu/cnu/mdi/icons/zoom_in.svg"} or {@code "icons/zoom_in.svg"}.
 *       In these cases, this class will automatically prepend {@code "/"} so the
 *       lookup is still absolute in the classpath.</li>
 * </ul>
 *
 * <p>
 * If you want {@code ImageManager} to remain agnostic about where resources live,
 * your calling code can keep prepending whatever prefix you like (e.g.
 * {@code "/edu/cnu/mdi/"}). This class will load MDI’s icons properly from the
 * MDI dependency JAR, and also load application-provided icons, as long as the
 * resource path is unique on the classpath.
 * </p>
 *
 * <h2>FlatLaf considerations</h2>
 * <ul>
 *   <li><b>SVG UI icons:</b> Uses {@link FlatSVGIcon}. FlatLaf handles HiDPI
 *       scaling correctly and SVG icons remain crisp at all scales.</li>
 *   <li><b>Raster UI icons:</b> When an icon is requested at a "logical" size
 *       (e.g. 16), raster icons are scaled to device pixels via
 *       {@link UIScale#scale(int)}.</li>
 *   <li><b>Avoid eager static initialization.</b> Look-and-feel setup can affect
 *       scaling; this class caches lazily.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 * <p>
 * All caches are thread-safe. Icon creation may occur off the EDT, but icons are
 * typically requested during UI construction on the EDT.
 * </p>
 */
public final class ImageManager {

	/**
	 * Optional convenience prefix for MDI’s own resources.
	 * <p>
	 * This class does not automatically apply this prefix; it is provided so callers
	 * can build MDI resource paths consistently.
	 * </p>
	 */
	public static final String MDI_RESOURCE_PATH = "/edu/cnu/mdi";

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
	 * For SVG resources, returns a {@link FlatSVGIcon} of the requested size. For
	 * raster resources (png/jpg/gif), loads the image and scales it to the requested
	 * logical size, then wraps it in an {@link ImageIcon}.
	 * </p>
	 *
	 * <p>
	 * "Logical size" means the size in typical UI coordinates (e.g. 16, 20, 24).
	 * Under HiDPI displays, FlatLaf uses a scale factor; for raster icons we convert
	 * logical size to device pixels using {@link UIScale#scale(int)}.
	 * </p>
	 *
	 * @param resourcePath classpath resource path (absolute preferred), e.g.
	 *                     {@code "/edu/cnu/mdi/icons/zoom_in.svg"} or
	 *                     {@code "edu/cnu/mdi/icons/zoom_in.svg"}
	 * @param logicalW     requested logical width (e.g. 16, 20, 24)
	 * @param logicalH     requested logical height
	 * @return an {@link Icon}, or {@code null} if the resource cannot be found/read
	 * @throws NullPointerException     if {@code resourcePath} is null
	 * @throws IllegalArgumentException if {@code logicalW <= 0} or {@code logicalH <= 0}
	 */
	public Icon loadUiIcon(String resourcePath, int logicalW, int logicalH) {
		Objects.requireNonNull(resourcePath, "resourcePath");
		if (logicalW <= 0 || logicalH <= 0) {
			throw new IllegalArgumentException("logicalW and logicalH must be positive.");
		}

		final String normalized = normalizeResourcePath(resourcePath);
		final String key = normalized + "@" + logicalW + "x" + logicalH;

		return iconCache.computeIfAbsent(key, k -> createUiIcon(normalized, logicalW, logicalH));
	}

	/**
	 * Convenience for square UI icons (e.g. 16x16, 20x20).
	 *
	 * @param resourcePath classpath resource path
	 * @param logicalSize  logical size in both dimensions
	 * @return an {@link Icon}, or {@code null} if the resource cannot be found/read
	 * @throws NullPointerException     if {@code resourcePath} is null
	 * @throws IllegalArgumentException if {@code logicalSize <= 0}
	 */
	public Icon loadUiIcon(String resourcePath, int logicalSize) {
		return loadUiIcon(resourcePath, logicalSize, logicalSize);
	}

	/**
	 * Create (uncached) UI icon, choosing SVG vs raster handling.
	 *
	 * @param normalizedResourcePath normalized absolute resource path (leading '/')
	 */
	private Icon createUiIcon(String normalizedResourcePath, int logicalW, int logicalH) {
		final String lower = normalizedResourcePath.toLowerCase();

		if (lower.endsWith(".svg")) {
		    if (!resourceExists(normalizedResourcePath)) {
		        Log.getInstance().warning("SVG icon resource not found: " + normalizedResourcePath);
		        return null;
		    }

		    // Use Option B resource resolution (absolute classpath via Class.getResource*),
		    // then let FlatSVGIcon read from a concrete URL and derive the requested size.
		    java.net.URL url = ImageManager.class.getResource(normalizedResourcePath);
		    if (url == null) {
		        Log.getInstance().warning("SVG icon resource not found (URL): " + normalizedResourcePath);
		        return null;
		    }

		    return new FlatSVGIcon(url).derive(logicalW, logicalH);
		}

		// Raster icon: load original and scale to device pixels using UIScale.
		BufferedImage src = loadRaster(normalizedResourcePath);
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
	 * Load a raster image (PNG/JPG/GIF/etc.) from the classpath as a
	 * {@link BufferedImage}.
	 * <p>
	 * This returns the original image at its native pixel dimensions. It is cached.
	 * </p>
	 *
	 * @param resourcePath classpath resource path (absolute preferred), e.g.
	 *                     {@code "/edu/cnu/mdi/images/background.png"}
	 * @return the image, or {@code null} if the resource cannot be found/read
	 * @throws NullPointerException if {@code resourcePath} is null
	 */
	public BufferedImage loadRaster(String resourcePath) {
		Objects.requireNonNull(resourcePath, "resourcePath");
		final String normalized = normalizeResourcePath(resourcePath);
		return rasterCache.computeIfAbsent(normalized, this::readRasterUnchecked);
	}

	/**
	 * Load an arbitrary image as an {@link Image}.
	 * <p>
	 * Thin convenience wrapper around {@link #loadRaster(String)} for code that expects
	 * {@link Image} instead of {@link BufferedImage}.
	 * </p>
	 *
	 * @param resourcePath classpath resource path
	 * @return the image, or {@code null} if not found/read
	 */
	public Image loadImage(String resourcePath) {
		return loadRaster(resourcePath);
	}

	/**
	 * Load an {@link ImageIcon} without applying any HiDPI logical sizing.
	 * <p>
	 * Prefer {@link #loadUiIcon(String, int)} for toolbar/menu icons. This method is
	 * intended for legacy usage where the caller expects the icon at its native pixel
	 * size.
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
	 * Read a raster image from the classpath, returning {@code null} on failure.
	 * <p>
	 * This method is used by the cache and must not throw.
	 * </p>
	 *
	 * @param normalizedResourcePath normalized absolute resource path (leading '/')
	 * @return the decoded image, or {@code null} if missing or unreadable
	 */
	private BufferedImage readRasterUnchecked(String normalizedResourcePath) {
		try (InputStream in = getResourceAsStream(normalizedResourcePath)) {
			if (in == null) {
				Log.getInstance().warning("Raster resource not found: " + normalizedResourcePath);
				return null;
			}
			return ImageIO.read(in);
		} catch (IOException e) {
			Log.getInstance().exception(e);
			Log.getInstance().warning("Failed to read raster resource: " + normalizedResourcePath);
			return null;
		}
	}

	/**
	 * Returns {@code true} if a resource exists on the classpath.
	 *
	 * @param resourcePath classpath resource path (absolute or relative)
	 * @return {@code true} if the resource can be opened; {@code false} otherwise
	 */
	private boolean resourceExists(String resourcePath) {
		final String normalized = normalizeResourcePath(resourcePath);
		try (InputStream in = getResourceAsStream(normalized)) {
			return in != null;
		} catch (IOException e) {
			// Extremely unlikely, but be defensive.
			return false;
		}
	}

	/**
	 * Normalize resource paths so lookups are absolute in the classpath.
	 * <p>
	 * With {@link Class#getResourceAsStream(String)}, a leading {@code "/"} means
	 * "absolute from the classpath root". Without it, the lookup becomes relative
	 * to this class's package, which is rarely what we want in a library.
	 * </p>
	 *
	 * @param resourcePath raw resource path
	 * @return an absolute resource path beginning with {@code "/"}
	 */
	private static String normalizeResourcePath(String resourcePath) {
		String rp = Objects.requireNonNull(resourcePath, "resourcePath").trim();
		if (rp.isEmpty()) {
			return "/";
		}
		return rp.startsWith("/") ? rp : ("/" + rp);
	}

	/**
	 * Get a resource as a stream using {@link Class#getResourceAsStream(String)}.
	 * <p>
	 * This method expects an <b>absolute</b> classpath resource name (leading
	 * {@code "/"}). Use {@link #normalizeResourcePath(String)} for user-provided
	 * paths.
	 * </p>
	 *
	 * @param normalizedResourcePath absolute classpath resource path (leading '/')
	 * @return stream or {@code null} if not found
	 */
	private InputStream getResourceAsStream(String normalizedResourcePath) {
		// Option B: use Class.getResourceAsStream so leading "/" is supported.
		return ImageManager.class.getResourceAsStream(normalizedResourcePath);
	}

	// -------------------------------------------------------------------------
	// Cache management
	// -------------------------------------------------------------------------

	/**
	 * Clear all cached images and icons.
	 * <p>
	 * Useful if you support runtime theme changes and want to rebuild icons, or when
	 * memory pressure suggests cache eviction.
	 * </p>
	 */
	public void clearCaches() {
		rasterCache.clear();
		iconCache.clear();
	}
}
