package edu.cnu.mdi.util;

import java.awt.Color;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.swing.UIManager;

/**
 * Environment information and simple application preferences for the mdi
 * framework. This class acts as a central place to query basic system
 * properties (home directory, host name, screen DPI, etc.) and to persist
 * lightweight, application-specific preferences.
 * <p>
 * The class is implemented as a simple, lazily-initialized singleton.
 */
public final class Environment {

    private static final Logger LOGGER = Logger.getLogger(Environment.class.getName());

    /** Separator used when storing simple string lists as a single value. */
    private static final String LIST_SEPARATOR = "$$";

    /** The singleton instance. */
    private static Environment instance;

    // --- basic system properties ------------------------------------------------

    private final String homeDirectory;
    private final String currentWorkingDirectory;
    private final String userName;
    private final String osName;
    private final String tempDirectory;
    private final String classPath;

    // cached host address
    private String hostAddress;

    // PNG image writer, if one is available
    private final ImageWriter pngWriter;

    // application name (derived lazily)
    private String applicationName;

    // preferences
    private final Properties properties;

    // dragging flag (used to let worker threads know about GUI drag operations)
    private volatile boolean dragging;

    // screen information
    private final float resolutionScaleFactor;
    private final int dotsPerInch;

    // common Swing panel background
    private static Color commonPanelBackgroundColor;

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

        // screen information
        int dpi = 96;
        float scaleFactor = 1.0f;
        try {
            dpi = Toolkit.getDefaultToolkit().getScreenResolution();
            double dpcm = dpi / 2.54;
            // 42.91 was empirically chosen in the original bCNU code
            scaleFactor = (float) (dpcm / 42.91);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to determine screen resolution.", e);
        }
        dotsPerInch = dpi;
        resolutionScaleFactor = scaleFactor;

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

        // load preferences (if any)
        properties = new Properties();
        File prefFile = getPreferencesFile();
        if (prefFile.exists() && prefFile.isFile()) {
            try (FileInputStream in = new FileInputStream(prefFile)) {
                properties.loadFromXML(in);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to load preferences from " + prefFile, e);
            }
        }

        if (commonPanelBackgroundColor == null) {
            commonPanelBackgroundColor = UIManager.getColor("Panel.background");
        }
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

    private String getSystemProperty(String key) {
        try {
            return System.getProperty(key);
        } catch (SecurityException e) {
            LOGGER.log(Level.FINE, "Unable to read system property: " + key, e);
            return null;
        }
    }

    public String getClassPath() {
        return classPath;
    }

    public String getCurrentWorkingDirectory() {
        return currentWorkingDirectory;
    }

    public String getHomeDirectory() {
        return homeDirectory;
    }

    public String getOsName() {
        return osName;
    }

    public String getTempDirectory() {
        return tempDirectory;
    }

    public String getUserName() {
        return userName;
    }

    /**
     * Returns the primary IPv4 address of the local host, if it can be
     * determined. The result is cached after the first successful lookup.
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

    public boolean isLinux() {
        return osName != null && osName.toLowerCase(Locale.ENGLISH).contains("linux");
    }

    public boolean isWindows() {
        return osName != null && osName.toLowerCase(Locale.ENGLISH).contains("windows");
    }

    public boolean isMac() {
        return osName != null && osName.toLowerCase(Locale.ENGLISH).startsWith("mac");
    }

    // ------------------------------------------------------------------------
    // Screen information / scaling
    // ------------------------------------------------------------------------

    /**
     * For scaling UI-related quantities such as font sizes. Logical sizes can
     * be multiplied by this factor to adapt to high-DPI displays.
     *
     * @return the resolution scale factor
     */
    public float getResolutionScaleFactor() {
        return resolutionScaleFactor;
    }

    /**
     * Returns the dots per inch (DPI) for the primary display.
     *
     * @return screen DPI
     */
    public int getDotsPerInch() {
        return dotsPerInch;
    }

    /**
     * Returns the dots per centimetre (DPCM) for the primary display.
     *
     * @return screen DPCM
     */
    public double getDotsPerCentimeter() {
        return dotsPerInch / 2.54;
    }

    /**
     * Returns the display scale factor for the default screen device based on
     * the default affine transform.
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

    public GraphicsDevice[] getGraphicsDevices() {
        GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
        return g.getScreenDevices();
    }

    // ------------------------------------------------------------------------
    // PNG writer
    // ------------------------------------------------------------------------

    public ImageWriter getPngWriter() {
        return pngWriter;
    }

    // ------------------------------------------------------------------------
    // Application name and configuration
    // ------------------------------------------------------------------------

    /**
     * Attempts to infer a simple application name. By default this is the
     * simple name of the class that appears at the bottom of the stack trace
     * for thread id 1, converted to lower case. If this cannot be determined,
     * {@code null} is returned.
     *
     * @return the inferred application name, or {@code null} if unavailable
     */
    public synchronized String getApplicationName() {
        if (applicationName != null) {
            return applicationName;
        }

        try {
            ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            ThreadInfo info = bean.getThreadInfo(1, Integer.MAX_VALUE);
            if (info != null) {
                StackTraceElement[] stackTrace = info.getStackTrace();
                if (stackTrace != null && stackTrace.length > 0) {
                    String fqcn = stackTrace[stackTrace.length - 1].getClassName();
                    int index = fqcn.lastIndexOf('.');
                    applicationName = (index >= 0 ? fqcn.substring(index + 1) : fqcn)
                            .toLowerCase(Locale.ENGLISH);
                    LOGGER.info("Application name: " + applicationName);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not determine application name.", e);
            applicationName = null;
        }

        return applicationName;
    }

    /**
     * Returns the configuration file used by the application. The file name is
     * derived from the application name with a {@code .xml} extension. On
     * Unix-like systems the file is written as a "dot" file in the user's home
     * directory (e.g. {@code ~/.myapp.xml}); on Windows the leading dot is
     * omitted.
     *
     * @return configuration file, or {@code null} if the application name is
     *         unknown
     */
    public File getConfigurationFile() {
        String aname = getApplicationName();
        if (aname == null || homeDirectory == null) {
            return null;
        }

        boolean windows = isWindows();
        String baseName = windows ? aname + ".xml" : "." + aname + ".xml";
        try {
            return new File(homeDirectory, baseName);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not create configuration file.", e);
            return null;
        }
    }

    /**
     * Returns the file used to store {@link #getProperties() preferences}. The
     * file is created in the user's home directory and uses an XML
     * {@link java.util.Properties} format.
     */
    private File getPreferencesFile() {
        String aname = getApplicationName();
        if (aname == null || homeDirectory == null) {
            // fall back to a generic name
            return new File("mdi.preferences.xml");
        }
        return new File(homeDirectory, "." + aname + ".pref.xml");
    }

    // ------------------------------------------------------------------------
    // Preferences
    // ------------------------------------------------------------------------

    /**
     * Returns the raw preferences backing store. The returned instance is
     * live; modifications are not written to disk until
     * {@link #flushPreferences()} is called.
     *
     * @return the properties used as preferences
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Returns the preference associated with the given key, or
     * {@code null} if no such preference exists.
     */
    public String getPreference(String key) {
        return (key == null ? null : properties.getProperty(key));
    }

    /**
     * Stores a single preference value and immediately writes the preferences
     * file to disk.
     *
     * @param key   the preference key (must not be {@code null})
     * @param value the value to store (must not be {@code null})
     */
    public void savePreference(String key, String value) {
        if (key == null || value == null) {
            return;
        }
        properties.setProperty(key, value);
        flushPreferences();
    }

    /**
     * Saves a list of string values under the given key. The list is stored as
     * a single, separator-encoded string. An empty or {@code null} list
     * clears the preference.
     *
     * @param key    the preference key
     * @param values the values to store
     */
    public void savePreferenceList(String key, List<String> values) {
        if (key == null) {
            return;
        }
        if (values == null || values.isEmpty()) {
            properties.remove(key);
            flushPreferences();
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            sb.append(values.get(i));
            if (i < values.size() - 1) {
                sb.append(LIST_SEPARATOR);
            }
        }
        savePreference(key, sb.toString());
    }

    /**
     * Returns the preference identified by {@code key} as a list of strings.
     * The value is split using the list separator.
     *
     * @param key the preference key
     * @return a list of values, or {@code null} if the preference does not
     *         exist
     */
    public List<String> getPreferenceList(String key) {
        String raw = getPreference(key);
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String[] tokens = raw.split(java.util.regex.Pattern.quote(LIST_SEPARATOR));
        List<String> result = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            if (!token.isEmpty()) {
                result.add(token);
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * Writes the current preferences to disk. This is automatically invoked by
     * the {@code savePreference} methods.
     */
    public synchronized void flushPreferences() {
        File file = getPreferencesFile();
        if (file == null) {
            return;
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            properties.storeToXML(out, "mdi preferences");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to write preferences to " + file, e);
        }
    }

    // ------------------------------------------------------------------------
    // Dragging flag
    // ------------------------------------------------------------------------

    /**
     * Returns {@code true} while an interactive drag operation is in progress.
     * Long-running background tasks may choose to suspend UI updates while
     * dragging is active.
     */
    public boolean isDragging() {
        return dragging;
    }

    /**
     * Sets the dragging flag.
     *
     * @param dragging {@code true} when an interactive drag operation is in
     *                 progress
     */
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    // ------------------------------------------------------------------------
    // Panel background
    // ------------------------------------------------------------------------

    /**
     * Returns a "common" Swing panel background color used by mdi. By default,
     * this is initialized from {@code UIManager.getColor("Panel.background")}.
     */
    public static Color getCommonPanelBackground() {
        return commonPanelBackgroundColor;
    }

    /**
     * Overrides the common panel background color used by mdi.
     *
     * @param color new background color, may be {@code null}
     */
    public static void setCommonPanelBackground(Color color) {
        commonPanelBackgroundColor = color;
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
     * Produces a simple memory usage report as a string. A full garbage
     * collection is requested before the numbers are computed.
     *
     * @param message optional message to prefix in the report (may be
     *                {@code null})
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
     * Returns a multi-line description of the environment, including paths,
     * display information and memory usage.
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

        sb.append("Dots per Inch: ").append(dotsPerInch).append('\n');
        sb.append("Dots per Centimeter: ")
          .append(String.format(Locale.US, "%.2f", getDotsPerCentimeter()))
          .append('\n');
        sb.append("Resolution Scale Factor: ")
          .append(String.format(Locale.US, "%.2f", resolutionScaleFactor))
          .append('\n');
        sb.append("PNG Writer: ").append(pngWriter == null ? "none" : pngWriter).append('\n');

        sb.append("Monitors:\n");
        GraphicsDevice[] devices = getGraphicsDevices();
        if (devices != null) {
            for (GraphicsDevice device : devices) {
                Rectangle bounds = device.getDefaultConfiguration().getBounds();
                int width = device.getDisplayMode().getWidth();
                int height = device.getDisplayMode().getHeight();
                sb.append("   [W, H] = [").append(width).append(", ").append(height)
                  .append("] bounds: ").append(bounds).append('\n');
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
        return (dotIndex != -1)
                ? Integer.parseInt(version.substring(0, dotIndex))
                : Integer.parseInt(version);
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
        Arrays.stream(stackTrace)
              .filter(element -> element.getClassName().startsWith(startsWith))
              .forEach(System.err::println);
    }

    // ------------------------------------------------------------------------
    // Look & Feel helpers
    // ------------------------------------------------------------------------

    /**
     * Initialize the Swing look &amp; feel. This first tries to install the
     * system look &amp; feel, and falls back to the cross-platform look
     * &amp; feel on failure.
     */
    public static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            LOGGER.info("Set Look and Feel: " + UIManager.getLookAndFeel());
        } catch (Exception first) {
            LOGGER.log(Level.FINE, "System Look and Feel failed, falling back to cross platform.", first);
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                LOGGER.info("Set Look and Feel: " + UIManager.getLookAndFeel());
            } catch (Exception second) {
                LOGGER.log(Level.WARNING, "Unable to set any preferred Look and Feel.", second);
            }
        }
    }

    /**
     * Logs the installed Swing look &amp; feels and the system / cross-platform
     * defaults to {@code System.out}.
     */
    public static void listLookAndFeels() {
        var lookAndFeels = UIManager.getInstalledLookAndFeels();
        System.out.println("Installed Look & Feels:");
        for (var info : lookAndFeels) {
            System.out.println("  " + info.getName() + " : " + info.getClassName());
        }

        System.out.println("System Look and Feel: " + UIManager.getSystemLookAndFeelClassName());
        System.out.println("Cross Platform Look and Feel: " + UIManager.getCrossPlatformLookAndFeelClassName());
    }

    /**
     * Simple test harness that prints the current environment and installed
     * look &amp; feels.
     */
    public static void main(String[] args) {
        Environment env = Environment.getInstance();
        System.out.println(env);
        listLookAndFeels();
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
