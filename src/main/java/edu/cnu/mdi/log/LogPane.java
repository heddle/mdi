package edu.cnu.mdi.log;

import java.awt.Color;
import java.awt.Dimension;
import java.util.EnumMap;

import javax.swing.SwingUtilities;
import javax.swing.text.SimpleAttributeSet;

import edu.cnu.mdi.component.TextPaneScrollPane;
import edu.cnu.mdi.ui.colors.X11Colors;

/**
 * A color-coded log display hosted inside the application's {@code LogView}.
 *
 * <h2>Overview</h2>
 * <p>
 * {@code LogPane} extends {@link TextPaneScrollPane} and registers itself as
 * an {@link ILogListener} with the {@link Log} singleton at construction
 * time. Each log level is rendered in a distinct color and font so messages
 * can be scanned at a glance:
 * </p>
 * <ul>
 *   <li><b>INFO</b> — black, sans-serif</li>
 *   <li><b>CONFIG</b> — blue, sans-serif</li>
 *   <li><b>WARNING</b> — orange-red, monospaced, italic</li>
 *   <li><b>ERROR</b> — red, sans-serif, italic</li>
 *   <li><b>EXCEPTION</b> — red, monospaced, italic (smaller; stack traces
 *       are verbose)</li>
 * </ul>
 *
 * <h2>Threading</h2>
 * <p>
 * Log messages may arrive from any thread. All Swing text-pane mutations are
 * dispatched to the EDT via {@link SwingUtilities#invokeLater} so this class
 * is safe to use from background threads.
 * </p>
 *
 * <h2>Lifecycle</h2>
 * <p>
 * The listener registered in the constructor is held by the {@link Log}
 * singleton for the lifetime of the application. If the {@code LogPane} is
 * ever removed from the UI it will continue to receive log messages; call
 * {@link #detach()} to unregister the listener.
 * </p>
 */
@SuppressWarnings("serial")
public class LogPane extends TextPaneScrollPane {

    // -----------------------------------------------------------------------
    // Style constants
    // -----------------------------------------------------------------------

    /** Font size for INFO messages. */
    private static final int INFO_FONT_SIZE      = 12;

    /** Font size for CONFIG messages. */
    private static final int CONFIG_FONT_SIZE    = 12;

    /** Font size for WARNING messages. */
    private static final int WARNING_FONT_SIZE   = 11;

    /** Font size for ERROR messages. */
    private static final int ERROR_FONT_SIZE     = 11;

    /**
     * Font size for EXCEPTION messages.
     * <p>
     * Slightly smaller than ERROR because stack traces are verbose and a
     * compact rendering makes them easier to scan.
     * </p>
     */
    private static final int EXCEPTION_FONT_SIZE = 10;

    /**
     * Per-level text styles, initialized once at class load.
     * <p>
     * <strong>Note:</strong> each {@link Log.Level} must have exactly one
     * entry. Using an {@link EnumMap} guarantees this at a glance and
     * provides O(1) lookup.
     * </p>
     */
    private static final EnumMap<Log.Level, SimpleAttributeSet> STYLES;

    static {
        STYLES = new EnumMap<>(Log.Level.class);
        STYLES.put(Log.Level.INFO,
                createStyle(Color.black,
                        "sansserif", INFO_FONT_SIZE, false, false));
        STYLES.put(Log.Level.CONFIG,
                createStyle(Color.blue,
                        "sansserif", CONFIG_FONT_SIZE, false, false));
        STYLES.put(Log.Level.WARNING,
                createStyle(X11Colors.getX11Color("orange red"),
                        "monospaced", WARNING_FONT_SIZE, false, true));
        STYLES.put(Log.Level.ERROR,
                createStyle(Color.red,
                        "sansserif", ERROR_FONT_SIZE, false, true));
        // Fixed: was accidentally overwriting the ERROR entry with ERROR key.
        STYLES.put(Log.Level.EXCEPTION,
                createStyle(Color.red,
                        "monospaced", EXCEPTION_FONT_SIZE, false, true));
    }

    // -----------------------------------------------------------------------
    // Instance state
    // -----------------------------------------------------------------------

    /**
     * The listener registered with {@link Log}.
     * <p>
     * Kept as a field so it can be unregistered via {@link #detach()}.
     * </p>
     */
    private final ILogListener logListener;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    /**
     * Construct a {@code LogPane} and register it with the {@link Log}
     * singleton.
     * <p>
     * The pane begins receiving messages immediately after construction.
     * Call {@link #detach()} if you need to stop receiving messages without
     * disposing the component.
     * </p>
     */
    public LogPane() {
        setPreferredSize(new Dimension(800, 400));

        // Build a listener using default methods — only override levels that
        // need non-default (i.e. all of them here, but concisely via lambdas).
        logListener = new ILogListener() {
            @Override public void info     (String m) { appendOnEdt(Log.Level.INFO,      m); }
            @Override public void config   (String m) { appendOnEdt(Log.Level.CONFIG,    m); }
            @Override public void warning  (String m) { appendOnEdt(Log.Level.WARNING,   m); }
            @Override public void error    (String m) { appendOnEdt(Log.Level.ERROR,     m); }
            @Override public void exception(String m) { appendOnEdt(Log.Level.EXCEPTION, m); }
        };

        Log.getInstance().addLogListener(logListener);
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Unregister this pane's listener from the {@link Log} singleton.
     * <p>
     * After this call the pane stops receiving new log messages but retains
     * whatever content it has already displayed. The component itself remains
     * usable (e.g. the user can still scroll and read existing messages).
     * </p>
     */
    public void detach() {
        Log.getInstance().removeLogListener(logListener);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Append {@code message} at {@code level} on the Swing EDT.
     * <p>
     * If the calling thread is already the EDT the append happens
     * synchronously via a direct {@code invokeLater} queue entry; either
     * way Swing's single-threaded rule is respected.
     * </p>
     *
     * @param level   the log level whose style should be applied
     * @param message the raw message text
     */
    private void appendOnEdt(Log.Level level, String message) {
        SwingUtilities.invokeLater(() -> appendStyled(level, message));
    }

    /**
     * Append {@code message} to the text pane using the style registered for
     * {@code level}.
     * <p>
     * A trailing newline is appended if the message does not already end with
     * one, so each message occupies its own line regardless of how the caller
     * formatted the string.
     * </p>
     * <p>
     * For {@link Log.Level#ERROR} and {@link Log.Level#EXCEPTION} messages a
     * timestamp prefix is also prepended by the underlying
     * {@link TextPaneScrollPane#append(String, SimpleAttributeSet, boolean)}
     * call.
     * </p>
     *
     * @param level   the log level
     * @param message the message text
     */
    private void appendStyled(Log.Level level, String message) {
        boolean writeTime = (level == Log.Level.ERROR)
                         || (level == Log.Level.EXCEPTION);
        String text = (message == null) ? "\n"
                    : message.endsWith("\n") ? message
                    : message + "\n";
        append(text, STYLES.get(level), writeTime);
    }
}