package edu.cnu.mdi.util;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;

/**
 * Environment information and simple application preferences for the mdi
 * framework. This class acts as a central place to query basic system
 * properties (home directory, host name, screen DPI, etc.) and to persist
 * lightweight, application-specific preferences.
 * <p>
 * The class is implemented as a simple, lazily-initialized singleton.
 */
public final class Environment {

	//resourcde path prefix for MDI owned resources
	public static final String MDI_RESOURCE_PATH = "/edu/cnu/mdi/";

	// the logger
	private static final Logger LOGGER = Logger.getLogger(Environment.class.getName());

	/** The singleton instance. */
	private static Environment instance;

	// --- basic system properties ------------------------------------------------

	private final String homeDirectory;
	private final String currentWorkingDirectory;
	private final String userName;
	private final String osName;
	private final String tempDirectory;
	private final String classPath;
	private String dataDirectory;

	// cached host address
	private String hostAddress;

	// PNG image writer, if one is available
	private final ImageWriter pngWriter;

	// application name (derived lazily)
	private static String _applicationName;

	/**
	 * Private constructor for the singleton. Gathers system information and
	 * attempts to load any previously stored preferences.
	 */
	private Environment() {
		homeDirectory = getSystemProperty("user.home");
		currentWorkingDirectory = getSystemProperty("user.dir");
		userName = getSystemProperty("user.name");
		osName = getSystemProperty("os.name");
		tempDirectory = getSystemProperty("java.io.tmpdir");
		classPath = getSystemProperty("java.class.path");
		dataDirectory = getSystemProperty("user.home");

		// any PNG image writers?
		ImageWriter writer = null;
		try {
			var iterator = ImageIO.getImageWritersByFormatName("png");
			if (iterator != null && iterator.hasNext()) {
				writer = iterator.next();
			}
		} catch (Throwable t) {
			LOGGER.log(Level.WARNING, "Unable to locate PNG ImageWriter.", t);
		}
		pngWriter = writer;

	}

	// ------------------------------------------------------------------------
	// Singleton access
	// ------------------------------------------------------------------------

	/**
	 * Returns the singleton {@code Environment} instance.
	 *
	 * @return the shared {@code Environment} instance
	 */
	public static synchronized Environment getInstance() {
		if (instance == null) {
			instance = new Environment();
		}
		return instance;
	}

	// ------------------------------------------------------------------------
	// Basic properties
	// ------------------------------------------------------------------------

	// safe system property access
	private String getSystemProperty(String key) {
		try {
			return System.getProperty(key);
		} catch (SecurityException e) {
			LOGGER.log(Level.FINE, "Unable to read system property: " + key, e);
			return null;
		}
	}

	/*
	 * Get the JVM class path
	 * @return the class path
	 */
	public String getClassPath() {
		return classPath;
	}

	/**
	 * Returns the current working directory.
	 *
	 * @return the current working directory
	 */
	public String getCurrentWorkingDirectory() {
		return currentWorkingDirectory;
	}

	/**
	 * Returns the user's home directory.
	 *
	 * @return the home directory
	 */
	public String getHomeDirectory() {
		return homeDirectory;
	}

	/**
	 * Returns the operating system name.
	 *
	 * @return the OS name
	 */
	public String getOsName() {
		return osName;
	}

	/**
	 * Returns the data directory. this is where
	 * application data files (e.g., plots) were most recently stored
	 *
	 * @return the data directory
	 */
	public String getDataDirectory() {
		return dataDirectory;
	}

	/**
	 * Sets the data directory.This is where
	 * application data files (e.g., plots) were most recently stored.
	 *
	 * @param dataDirectory the data directory to set
	 */
	public void setDataDirectory(String dataDirectory) {
		this.dataDirectory = dataDirectory;
	}

	/**
	 * Returns a unique temporary directory.
	 *
	 * @return a unique temp directory
	 */
	public String getTempDirectory() {
		return tempDirectory;
	}

	/**
	 * Returns the user name of the current user.
	 *
	 * @return the user name
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * Returns the primary IPv4 address of the local host, if it can be determined.
	 * The result is cached after the first successful lookup.
	 *
	 * @return the host address or {@code null} if it cannot be resolved
	 */
	public synchronized String getHostAddress() {
		if (hostAddress != null) {
			return hostAddress;
		}
		try {
			InetAddress localhost = InetAddress.getLocalHost();
			hostAddress = localhost.getHostAddress();
		} catch (UnknownHostException e) {
			LOGGER.log(Level.WARNING, "Unable to determine local host address.", e);
		}
		return hostAddress;
	}

	// ------------------------------------------------------------------------
	// OS helpers
	// ------------------------------------------------------------------------

	/**
	 * Returns {@code true} if the current operating system is a Linux variant.
	 *
	 * @return {@code true} if running on Linux
	 */
	public boolean isLinux() {
		return osName != null && osName.toLowerCase(Locale.ENGLISH).contains("linux");
	}

	/**
	 * Returns {@code true} if the current operating system is a Windows variant.
	 *
	 * @return {@code true} if running on Windows
	 */
	public boolean isWindows() {
		return osName != null && osName.toLowerCase(Locale.ENGLISH).contains("windows");
	}

	/**
	 * Returns {@code true} if the current operating system is a macOS variant.
	 *
	 * @return {@code true} if running on macOS
	 */
	public boolean isMac() {
		return osName != null && osName.toLowerCase(Locale.ENGLISH).startsWith("mac");
	}

	// ------------------------------------------------------------------------
	// Screen information / scaling
	// ------------------------------------------------------------------------

	/**
	 * Returns the display scale factor for the default screen device based on the
	 * default affine transform.
	 *
	 * @return the display scale factor (typically 1.0, 1.25, 1.5, 2.0, ...)
	 */
	public static double getDisplayScaleFactor() {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		GraphicsConfiguration gc = gd.getDefaultConfiguration();
		AffineTransform transform = gc.getDefaultTransform();
		return transform.getScaleX();
	}

	/**
	 * Returns the available graphics devices (monitors).
	 *
	 * @return array of graphics devices
	 */
	public GraphicsDevice[] getGraphicsDevices() {
		GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
		return g.getScreenDevices();
	}

	// ------------------------------------------------------------------------
	// PNG writer
	// ------------------------------------------------------------------------

	/**
	 * Returns an ImageIO PNG image writer, or {@code null} if none is available.
	 *
	 * @return PNG image writer, or {@code null}
	 */
	public ImageWriter getPngWriter() {
		return pngWriter;
	}

	// ------------------------------------------------------------------------
	// Application name and configuration
	// ------------------------------------------------------------------------

	/**
	 * Returns the application name. This is used to derive configuration and
	 * preference file names.
	 *
	 * @return the application name, or {@code null} if not set
	 */
	public static String getApplicationName() {
		return _applicationName;
	}

	/**
	 * Sets the application name. This is used to derive configuration and
	 * preference file names. Set by basemdiapplication then never changed
	 *
	 * @param applicationName the application name to set
	 */
	public static void setApplicationName(String applicationName) {
	    if (applicationName == null) {
	        return;
	    }
	    applicationName = applicationName.trim();
	    if (applicationName.isEmpty()) {
	        return;
	    }

	    // Only block changes if we already have a non-empty name.
	    if (_applicationName == null || _applicationName.trim().isEmpty()) {
	        _applicationName = applicationName;
	    }
	}

	/**
	 * Returns the configuration file used by the application. The file name is
	 * derived from the application name with a {@code .xml} extension. On Unix-like
	 * systems the file is written as a "dot" file in the user's home directory
	 * (e.g. {@code ~/.myapp.xml}); on Windows the leading dot is omitted.
	 *
	 * @return configuration file, or {@code null} if the application name is
	 *         unknown
	 */
	public File getConfigurationFile() {
	    String aname = getApplicationName();
	    if ((homeDirectory == null) || (aname == null)) {
	        return null;
	    }
	    aname = aname.trim();
	    if (aname.isEmpty()) {
	        return null;
	    }

	    final boolean windows = isWindows();

	    // Sanitize to something safe for filenames (optional but smart)
	    // e.g. strip spaces and path separators
	    aname = aname.replaceAll("[\\\\/\\s:]+", "_");

	    final String baseName = windows ? (aname + ".xml") : ("." + aname + ".xml");
	    try {
	        return new File(homeDirectory, baseName);
	    } catch (Exception e) {
	        LOGGER.log(Level.WARNING, "Could not create configuration file.", e);
	        return null;
	    }
	}


	// ------------------------------------------------------------------------
	// Utility methods
	// ------------------------------------------------------------------------

	/**
	 * Returns a short, human-readable summary string containing the user name,
	 * operating system and current working directory.
	 */
	public String summaryString() {
		return " [" + userName + "] [" + osName + "] [" + currentWorkingDirectory + "]";
	}

	/**
	 * Splits the JVM class path into individual entries.
	 *
	 * @return array of class path entries (never {@code null})
	 */
	public String[] splitClassPath() {
		return splitPath(classPath);
	}

	/**
	 * Splits an arbitrary path using the platform path separator.
	 *
	 * @param path path to split
	 * @return array of segments (never {@code null})
	 */
	public String[] splitPath(String path) {
		if (path == null || path.isEmpty()) {
			return new String[0];
		}
		return path.split(java.util.regex.Pattern.quote(File.pathSeparator));
	}

	/**
	 * Produces a simple memory usage report as a string. A full garbage collection
	 * is requested before the numbers are computed.
	 *
	 * @param message optional message to prefix in the report (may be {@code null})
	 * @return a formatted memory report
	 */
	public static String memoryReport(String message) {
		System.gc();
		System.gc();

		double total = Runtime.getRuntime().totalMemory() / 1048576.0;
		double free = Runtime.getRuntime().freeMemory() / 1048576.0;
		double used = total - free;
		DecimalFormat df = new DecimalFormat("0.0");

		StringBuilder sb = new StringBuilder(128);
		sb.append("==== Memory Report =====\n");
		if (message != null) {
			sb.append(message).append('\n');
		}
		sb.append("Total memory in JVM: ").append(df.format(total)).append(" MB\n");
		sb.append(" Free memory in JVM: ").append(df.format(free)).append(" MB\n");
		sb.append(" Used memory in JVM: ").append(df.format(used)).append(" MB\n");

		return sb.toString();
	}

	/**
	 * Returns a multi-line description of the environment, including paths, display
	 * information and memory usage.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(512);
		sb.append("Environment:\n");

		File cfg = getConfigurationFile();
		sb.append("Config File: ").append(cfg == null ? "null" : cfg.getAbsolutePath()).append('\n');
		sb.append("Host Address: ").append(getHostAddress()).append('\n');
		sb.append("User Name: ").append(userName).append('\n');
		sb.append("Temp Directory: ").append(tempDirectory).append('\n');
		sb.append("OS Name: ").append(osName).append('\n');
		sb.append("Home Directory: ").append(homeDirectory).append('\n');
		sb.append("Current Working Directory: ").append(currentWorkingDirectory).append('\n');
		sb.append("Class Path: ").append(classPath).append('\n');

		String[] tokens = splitClassPath();
		sb.append("Class Path Token Count: ").append(tokens.length).append('\n');
		for (int i = 0; i < tokens.length; i++) {
			sb.append("  Class Path Token [").append(i).append("] = ").append(tokens[i]).append('\n');
		}

		sb.append("PNG Writer: ").append(pngWriter == null ? "none" : pngWriter).append('\n');

		sb.append("Monitors:\n");
		GraphicsDevice[] devices = getGraphicsDevices();
		if (devices != null) {
			for (GraphicsDevice device : devices) {
				Rectangle bounds = device.getDefaultConfiguration().getBounds();
				int width = device.getDisplayMode().getWidth();
				int height = device.getDisplayMode().getHeight();
				sb.append("   [W, H] = [").append(width).append(", ").append(height).append("] bounds: ").append(bounds)
						.append('\n');
			}
		}

		sb.append('\n').append(memoryReport(null));
		return sb.toString();
	}

	/**
	 * Returns the major Java runtime version (e.g. 8, 11, 17).
	 */
	public static int getJavaMajorVersion() {
		String version = System.getProperty("java.version");
		if (version == null || version.isEmpty()) {
			return -1;
		}
		if (version.startsWith("1.")) {
			return Integer.parseInt(version.substring(2, 3));
		}
		int dotIndex = version.indexOf('.');
		return (dotIndex != -1) ? Integer.parseInt(version.substring(0, dotIndex)) : Integer.parseInt(version);
	}

	/**
	 * Print a filtered stack trace that only includes elements whose class name
	 * starts with the supplied prefix (for example, {@code "edu.cnu.mdi"}).
	 *
	 * @param startsWith package or class name prefix used to filter the trace
	 */
	public static void filteredTrace(String startsWith) {
		Exception e = new Exception();
		StackTraceElement[] stackTrace = e.getStackTrace();

		System.err.println("\nStack trace filtered on \"" + startsWith + "\"");
		Arrays.stream(stackTrace).filter(element -> element.getClassName().startsWith(startsWith))
				.forEach(System.err::println);
	}

	// ------------------------------------------------------------------------
	// Look & Feel helpers
	// ------------------------------------------------------------------------

	/**
	 * Simple test harness that prints the current environment and installed look
	 * &amp; feels.
	 */
	public static void main(String[] args) {
		Environment env = Environment.getInstance();
		System.out.println(env);
		System.out.println("Done.");
	}

	/**
	 * Singleton objects cannot be cloned; this method always throws
	 * {@link CloneNotSupportedException}.
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

}
