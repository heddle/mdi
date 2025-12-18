package edu.cnu.mdi.graphics;

import java.awt.Component;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;

import javax.swing.ImageIcon;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;

import edu.cnu.mdi.log.Log;

/**
 * Centralized manager for loading and caching images and image icons.
 * <p>
 * This class is implemented as a singleton and maintains in-memory caches for
 * both {@link Image} and {@link ImageIcon} instances. Images are loaded from
 * either the local file system or the application class path using the current
 * class loader.
 * </p>
 *
 * <p>
 * Logging is performed via {@link Log}:
 * <ul>
 *   <li>{@code Log.getInstance().info(...)} is used when an image or icon is
 *       successfully loaded (not just retrieved from cache).</li>
 *   <li>{@code Log.getInstance().warning(...)} is used when loading fails or
 *       when a requested resource cannot be found.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Note: The caches used by this class are not synchronized beyond the inherent
 * synchronization of {@link Hashtable}. If more sophisticated concurrency
 * control is required, this implementation can be adapted to use alternative
 * collections.
 * </p>
 *
 * @author heddle
 */
public final class ImageManager {

	/**
	 * The singleton instance of the {@link ImageManager}.
	 */
	private static volatile ImageManager _instance;

	/**
	 * Icon used for application dialogs. Initialized lazily via the singleton.
	 */
	private static ImageIcon _dialogIcon = getInstance().loadImageIcon("images/mdiicon.png");

	/**
	 * A memory-only cache for {@link Image} objects, keyed by their source
	 * {@link URL}.
	 */
	private final Hashtable<URL, Image> _imageCache = new Hashtable<>(137);

	/**
	 * A memory-only cache for {@link ImageIcon} objects, keyed by the image path
	 * (relative to either the file system or the class path).
	 */
	private final Hashtable<String, ImageIcon> _iconCache = new Hashtable<>(193);

	/**
	 * Private constructor for the singleton instance.
	 */
	private ImageManager() {
		// no-op
	}

	/**
	 * Provides access to the {@link ImageManager} singleton.
	 *
	 * @return the singleton {@link ImageManager} instance
	 */
	public static ImageManager getInstance() {
		if (_instance == null) {
			synchronized (ImageManager.class) {
				if (_instance == null) {
					try {
						_instance = new ImageManager();
					} catch (Exception e) {
						Log.getInstance().exception(e);
					}
				}
			}
		}
		return _instance;
	}

	/**
	 * Loads an {@link Image} from the class path using the given file name and an
	 * optional {@link Component} as the image observer.
	 * <p>
	 * The image is cached by its resolved {@link URL}. Subsequent calls with the
	 * same resource path will return the cached image.
	 * </p>
	 *
	 * @param imageFileName the image file name, relative to the class path
	 * @param component     a component to serve as the {@link java.awt.image.ImageObserver};
	 *                      may be {@code null}
	 * @return the loaded {@link Image}, or {@code null} if the image could not be
	 *         found or loaded
	 */
	public Image loadImage(String imageFileName, Component component) {
		if (imageFileName == null) {
			Log.getInstance().warning("Requested image with null file name.");
			return null;
		}

		URL imageURL = getClass().getClassLoader().getResource(imageFileName);
		if (imageURL == null) {
			Log.getInstance().warning("Image resource not found on classpath: " + imageFileName);
			return null;
		}

		// Try cache first
		Image image = _imageCache.get(imageURL);
		if (image != null) {
			return image;
		}

		// Load from URL and cache
		image = loadImageFromURL(imageURL, component);
		if (image != null) {
			_imageCache.put(imageURL, image);
			Log.getInstance().info("Image loaded and cached from URL: " + imageURL);
		} else {
			Log.getInstance().warning("Failed to load image from URL: " + imageURL);
		}

		return image;
	}

	/**
	 * Loads an {@link ImageIcon} from either the local file system or the class
	 * path.
	 * <p>
	 * The loading process is:
	 * <ol>
	 *   <li>Check the icon cache.</li>
	 *   <li>Attempt to load from a local file with the given path.</li>
	 *   <li>Attempt to load via the application class loader using the given path
	 *       as a class path resource.</li>
	 * </ol>
	 * Successfully loaded icons are cached and logged.
	 * </p>
	 *
	 * @param imageFileName the image file name; may be a path in the file system
	 *                      or relative to the class path
	 * @return the loaded {@link ImageIcon}, or {@code null} if loading failed
	 */
	public ImageIcon loadImageIcon(String imageFileName) {
		if (imageFileName == null) {
			Log.getInstance().warning("Requested ImageIcon with null file name.");
			return null;
		}

		// Try the icon cache first
		ImageIcon icon = _iconCache.get(imageFileName);
		if (icon != null) {
			return icon;
		}

		// Try from local file system
		File file = new File(imageFileName);
		if (file.exists() && file.canRead()) {
			icon = new ImageIcon(imageFileName);
			if (icon.getIconWidth() > 0) {
				_iconCache.put(imageFileName, icon);
				Log.getInstance().info("ImageIcon loaded from file system: " + file.getAbsolutePath());
				return icon;
			} else {
				Log.getInstance().warning("Failed to load ImageIcon from file system: " + file.getAbsolutePath());
				icon = null;
			}
		}

		// Try from class path
		URL imageURL = getClass().getClassLoader().getResource(imageFileName);
		if (imageURL != null) {
			icon = new ImageIcon(imageURL);
			if (icon.getIconWidth() > 0) {
				_iconCache.put(imageFileName, icon);
				Log.getInstance().info("ImageIcon loaded from classpath resource: " + imageURL);
				return icon;
			} else {
				Log.getInstance().warning("Failed to load ImageIcon from classpath resource: " + imageURL);
				icon = null;
			}
		} else {
			Log.getInstance().warning("ImageIcon resource not found on classpath: " + imageFileName);
		}

		return icon;
	}

	/**
	 * Loads an SVG {@link ImageIcon} from the class path or file system, scaling
	 * it to the specified width and height.
	 *
	 * @param imageFileName the SVG image file name; may be a path in the file
	 *                      system or relative to the class path
	 * @param width         the desired icon width
	 * @param height        the desired icon height
	 * @return the loaded and scaled {@link ImageIcon}, or {@code null} if loading
	 *         failed
	 */
	public ImageIcon loadImageIcon(String imageFileName, int width, int height) {
	    if (imageFileName == null) {
	        Log.getInstance().warning("Requested ImageIcon with null file name.");
	        return null;
	    }

	    boolean isSvg = imageFileName.toLowerCase().endsWith(".svg");
	    String cacheKey = isSvg ? (imageFileName + "@" + width + "x" + height) : imageFileName;

	    ImageIcon cached = _iconCache.get(cacheKey);
	    if (cached != null) {
	        return cached;
	    }

	    // SVG path
	    if (isSvg) {
	        ImageIcon icon = loadSvgIcon(imageFileName, width, height);
	        if (icon != null) {
	            _iconCache.put(cacheKey, icon);
	            Log.getInstance().info("SVG ImageIcon loaded and cached: " + cacheKey);
	        }
	        return icon;
	    }

	    // Fallback to existing raster logic:
	    ImageIcon icon = loadImageIcon(imageFileName);
	    if (icon != null) {
	        // loadImageIcon(imageFileName) already caches by imageFileName in your current code
	        // so nothing extra needed here
	        return icon;
	    }
	    return null;
	}
	
	private ImageIcon loadSvgIcon(String imageFileName, int width, int height) {
	    // 1) Try file system
	    File file = new File(imageFileName);
	    if (file.exists() && file.canRead()) {
	        try (InputStream in = java.nio.file.Files.newInputStream(file.toPath())) {
	            BufferedImage img = renderSvgToBufferedImage(in, width, height);
	            if (img != null) {
	                return new ImageIcon(img);
	            }
	        } catch (Exception e) {
	            Log.getInstance().exception(e);
	            Log.getInstance().warning("Failed to load SVG from file system: " + file.getAbsolutePath());
	        }
	    }

	    // 2) Try classpath
	    URL url = getClass().getClassLoader().getResource(imageFileName);
	    if (url == null) {
	        Log.getInstance().warning("SVG resource not found on classpath: " + imageFileName);
	        return null;
	    }

	    try (InputStream in = url.openStream()) {
	        BufferedImage img = renderSvgToBufferedImage(in, width, height);
	        if (img != null) {
	            return new ImageIcon(img);
	        }
	    } catch (Exception e) {
	        Log.getInstance().exception(e);
	        Log.getInstance().warning("Failed to load SVG from classpath: " + url);
	    }

	    return null;
	}
	
	private static BufferedImage renderSvgToBufferedImage(InputStream svgStream, int width, int height)
	        throws TranscoderException {

	    if (svgStream == null) {
	        return null;
	    }
	    if (width <= 0 || height <= 0) {
	        throw new IllegalArgumentException("SVG render size must be > 0.");
	    }

	    TranscoderInput input = new TranscoderInput(svgStream);

	    // Batik needs a custom ImageTranscoder to capture the BufferedImage
	    class BufferedImageTranscoder extends ImageTranscoder {
	        private BufferedImage image;

	        @Override
	        public BufferedImage createImage(int w, int h) {
	            return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	        }

	        @Override
	        public void writeImage(BufferedImage img, TranscoderOutput out) {
	            this.image = img;
	        }

	        BufferedImage getImage() {
	            return image;
	        }
	    }

	    BufferedImageTranscoder t = new BufferedImageTranscoder();
	    t.addTranscodingHint(ImageTranscoder.KEY_WIDTH,  (float) width);
	    t.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, (float) height);

	    // Optional: help with crisp small icons
	    // t.addTranscodingHint(ImageTranscoder.KEY_AOI, new Rectangle2D.Float(0,0,width,height));

	    t.transcode(input, null);
	    return t.getImage();
	}



	/**
	 * Loads an {@link Image} from the provided {@link URL}.
	 * <p>
	 * A {@link MediaTracker} is used to ensure that the image is fully loaded
	 * before returning. If an error occurs while loading, a warning is logged and
	 * {@code null} is returned.
	 * </p>
	 *
	 * @param url the {@link URL} of the image; must not be {@code null}
	 * @param c   a component to use as the {@link java.awt.image.ImageObserver};
	 *            may be {@code null}, in which case a temporary component will be
	 *            created
	 * @return the loaded {@link Image}, or {@code null} if an error occurred
	 */
	public Image loadImageFromURL(URL url, Component c) {
		if (url == null) {
			Log.getInstance().warning("Attempted to load image from null URL.");
			return null;
		}

		Image image = Toolkit.getDefaultToolkit().getImage(url);
		Component observer = (c != null) ? c : new Component() {
			private static final long serialVersionUID = 1L;
		};

		try {
			MediaTracker tracker = new MediaTracker(observer);
			tracker.addImage(image, 0);
			tracker.waitForAll();

			if (tracker.isErrorAny()) {
				Log.getInstance().warning("Error while loading image from URL: " + url);
				return null;
			}

			Log.getInstance().info("Image successfully loaded from URL: " + url);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			Log.getInstance().warning("Image loading interrupted for URL: " + url);
			return null;
		} catch (Exception e) {
			Log.getInstance().exception(e);
			Log.getInstance().warning("Unexpected error while loading image from URL: " + url);
			return null;
		}

		return image;
	}

	/**
	 * Attempts to retrieve a cached {@link ImageIcon} by key.
	 *
	 * @param key the cache key
	 * @return the cached {@link ImageIcon}, or {@code null} if no icon is cached
	 *         under that key
	 */
	public ImageIcon get(String key) {
		return _iconCache.get(key);
	}

	/**
	 * Places an {@link ImageIcon} into the cache.
	 *
	 * @param key       the cache key to use; must not be {@code null}
	 * @param imageIcon the {@link ImageIcon} to cache; if {@code null}, the call
	 *                  is ignored
	 */
	public void put(String key, ImageIcon imageIcon) {
		if (key == null) {
			Log.getInstance().warning("Attempted to cache ImageIcon with null key.");
			return;
		}
		if (imageIcon != null) {
			_iconCache.put(key, imageIcon);
		}
	}

	/**
	 * Singleton objects cannot be cloned, so this method always throws a
	 * {@link CloneNotSupportedException}.
	 *
	 * @return never returns normally
	 * @throws CloneNotSupportedException always thrown to prevent cloning
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException("ImageManager is a singleton and cannot be cloned.");
	}

	/**
	 * Returns the icon used in application dialogs.
	 *
	 * @return the dialog {@link ImageIcon}, which may be {@code null} if loading
	 *         failed
	 */
	public static ImageIcon getDialogIcon() {
		return _dialogIcon;
	}

	/**
	 * Sets the icon used in application dialogs.
	 *
	 * @param icon the new dialog {@link ImageIcon}; may be {@code null}
	 */
	public static void setDialogIcon(ImageIcon icon) {
		_dialogIcon = icon;
	}

}
