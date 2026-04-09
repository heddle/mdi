package edu.cnu.mdi.log;

import java.io.PrintStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * An {@link ILogListener} that writes log messages to the standard console
 * streams.
 *
 * <h2>Output streams</h2>
 * <p>
 * {@link Log.Level#INFO} and {@link Log.Level#CONFIG} messages are written to
 * {@code System.out}. {@link Log.Level#WARNING}, {@link Log.Level#ERROR}, and
 * {@link Log.Level#EXCEPTION} messages are written to {@code System.err} so
 * that they appear in red in most terminal emulators and are captured
 * separately by process supervisors and IDEs that distinguish the two streams.
 * </p>
 *
 * <h2>Format</h2>
 * <p>
 * Each line is prefixed with the current wall-clock time (HH:mm:ss) and a
 * fixed-width level tag:
 * </p>
 * <pre>
 * 09:14:02 [INFO   ] Application started.
 * 09:14:02 [CONFIG ] Loaded 195 countries from GeoJSON resource.
 * 09:14:05 [WARNING] Configuration file not found; using defaults.
 * 09:14:07 [ERROR  ] Failed to open data file: foo.dat
 * 09:14:07 [EXCEPT ] java.io.FileNotFoundException: foo.dat ...
 * </pre>
 * <p>
 * Multi-line messages (such as exception stack traces) are indented on
 * continuation lines so the level tag visually anchors the start of each
 * entry.
 * </p>
 *
 * <h2>Registration</h2>
 * <pre>
 * Log.getInstance().addLogListener(new ConsoleLogger());
 * </pre>
 *
 * <h2>Selective levels</h2>
 * <p>
 * To suppress a level, subclass {@code ConsoleLogger} and override the
 * corresponding method as a no-op, or construct a targeted anonymous
 * {@link ILogListener} directly.
 * </p>
 *
 * <h2>Thread safety</h2>
 * <p>
 * Each {@link #format(Log.Level, String)} call produces a single
 * {@link PrintStream#println(String)} which is atomic on all standard JVM
 * implementations. Interleaving between threads is therefore limited to
 * whole messages rather than partial lines.
 * </p>
 */
public class ConsoleLogger implements ILogListener {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /** Formatter for the HH:mm:ss timestamp prefix. */
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Indentation applied to continuation lines of a multi-line message.
     * <p>
     * Width matches {@code "HH:mm:ss [XXXXXXX] "} so stack-trace lines
     * align with the message text of the first line.
     * </p>
     */
    private static final String CONTINUATION_INDENT = "         " + "           ";
    //                                                  timestamp   level-tag+space

    // -----------------------------------------------------------------------
    // ILogListener
    // -----------------------------------------------------------------------

    /**
     * Writes an {@link Log.Level#INFO} message to {@code System.out}.
     *
     * @param message the message; never {@code null}
     */
    @Override
    public void info(String message) {
        System.out.println(format(Log.Level.INFO, message));
    }

    /**
     * Writes a {@link Log.Level#CONFIG} message to {@code System.out}.
     *
     * @param message the message; never {@code null}
     */
    @Override
    public void config(String message) {
        System.out.println(format(Log.Level.CONFIG, message));
    }

    /**
     * Writes a {@link Log.Level#WARNING} message to {@code System.err}.
     *
     * @param message the message; never {@code null}
     */
    @Override
    public void warning(String message) {
        System.err.println(format(Log.Level.WARNING, message));
    }

    /**
     * Writes a {@link Log.Level#ERROR} message to {@code System.err}.
     *
     * @param message the message; never {@code null}
     */
    @Override
    public void error(String message) {
        System.err.println(format(Log.Level.ERROR, message));
    }

    /**
     * Writes a {@link Log.Level#EXCEPTION} message (stack trace) to
     * {@code System.err}.
     *
     * @param message the formatted stack trace; never {@code null}
     */
    @Override
    public void exception(String message) {
        System.err.println(format(Log.Level.EXCEPTION, message));
    }

    // -----------------------------------------------------------------------
    // Formatting
    // -----------------------------------------------------------------------

    /**
     * Format a log message as a single string ready for printing.
     * <p>
     * The first line receives the timestamp and level tag. Any subsequent
     * lines (common in stack traces) are indented to align with the message
     * text of the first line, keeping the output readable without repeating
     * the prefix on every line.
     * </p>
     *
     * @param level   the log level
     * @param message the raw message text
     * @return the formatted, ready-to-print string
     */
    protected String format(Log.Level level, String message) {
        String timestamp = LocalTime.now().format(TIME_FMT);
        String tag       = levelTag(level);
        String prefix    = timestamp + " " + tag + " ";

        // Strip a trailing newline if present — println adds its own.
        String text = message.endsWith("\n")
                ? message.substring(0, message.length() - 1)
                : message;

        // Indent continuation lines of multi-line messages.
        String indented = text.replace("\n", "\n" + CONTINUATION_INDENT);

        return prefix + indented;
    }

    /**
     * Returns the fixed-width, bracketed level tag for {@code level}.
     * <p>
     * All tags are padded to the same width so message text starts in the
     * same column regardless of level:
     * </p>
     * <pre>
     * [INFO   ]
     * [CONFIG ]
     * [WARNING]
     * [ERROR  ]
     * [EXCEPT ]
     * </pre>
     *
     * @param level the log level
     * @return the bracketed tag string
     */
    private static String levelTag(Log.Level level) {
        return switch (level) {
            case INFO      -> "[INFO   ]";
            case CONFIG    -> "[CONFIG ]";
            case WARNING   -> "[WARNING]";
            case ERROR     -> "[ERROR  ]";
            case EXCEPTION -> "[EXCEPT ]";
        };
    }
}