package edu.cnu.mdi.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.swing.event.EventListenerList;

/**
 * Application-wide logger for the MDI framework.
 *
 * <h2>Design</h2>
 * <p>
 * {@code Log} is a singleton that distributes log messages to zero or more
 * registered {@link ILogListener} implementations. It retains a small bounded
 * history and replays it to each newly registered listener, while all actual
 * output remains the responsibility of listeners. The
 * framework ships with three ready-made listeners:
 * </p>
 * <ul>
 *   <li>{@link LogPane} — displays messages in a color-coded Swing text
 *       pane inside the application's {@code LogView}.</li>
 *   <li>{@code ConsoleLogger} — writes to {@code System.out} /
 *       {@code System.err} with timestamps.</li>
 *   <li>{@code FileLogger} — appends to a rotating log file.</li>
 * </ul>
 * <p>
 * Applications register whichever listeners suit their deployment:
 * </p>
 * <pre>
 * Log.getInstance().addLogListener(new ConsoleLogger());
 * Log.getInstance().addLogListener(new FileLogger(Path.of("app.log")));
 * </pre>
 *
 * <h2>Log levels</h2>
 * <p>
 * Five levels are defined in order of increasing severity:
 * {@link Level#INFO}, {@link Level#CONFIG}, {@link Level#WARNING},
 * {@link Level#ERROR}, and {@link Level#EXCEPTION}. Each level dispatches
 * to the corresponding method on {@link ILogListener}.
 * </p>
 *
 * <h2>Thread safety</h2>
 * <p>
 * The singleton is initialized eagerly at class-load time, which is
 * inherently thread-safe under the Java memory model. The listener list is
 * managed by {@link EventListenerList}, whose iteration snapshot guarantees
 * safe concurrent reads. Listener add/remove operations are
 * {@code synchronized} on the instance. Message dispatch is unsynchronized
 * and therefore lock-free on the hot path.
 * </p>
 * <p>
 * Listeners themselves are responsible for any thread-safety requirements
 * they have. In particular, listeners that update Swing components must
 * dispatch to the EDT.
 * </p>
 */
public class Log {

    /** Maximum number of recent messages retained for newly registered listeners. */
    public static final int HISTORY_CAPACITY = 500;

    private record Entry(Level level, String message) {}

    // -----------------------------------------------------------------------
    // Log levels
    // -----------------------------------------------------------------------

    /**
     * Severity levels for log messages.
     * <p>
     * Each constant carries its own dispatch logic via an abstract
     * {@link #dispatch(ILogListener, String)} method, eliminating the need
     * for a {@code switch} statement in the notification loop.
     * </p>
     */
    public enum Level {

        /** Normal application events. */
        INFO {
            @Override
            public void dispatch(ILogListener listener, String message) {
                listener.info(message);
            }
        },

        /** Application configuration and setup events. */
        CONFIG {
            @Override
            public void dispatch(ILogListener listener, String message) {
                listener.config(message);
            }
        },

        /** Recoverable problems or unexpected conditions. */
        WARNING {
            @Override
            public void dispatch(ILogListener listener, String message) {
                listener.warning(message);
            }
        },

        /** Significant failures that may have prevented an operation. */
        ERROR {
            @Override
            public void dispatch(ILogListener listener, String message) {
                listener.error(message);
            }
        },

        /**
         * Unhandled exceptions.
         * <p>
         * The message contains the full stack trace produced by
         * {@link Log#exception(Throwable)}.
         * </p>
         */
        EXCEPTION {
            @Override
            public void dispatch(ILogListener listener, String message) {
                listener.exception(message);
            }
        };

        /**
         * Dispatch {@code message} to the appropriate method on
         * {@code listener} for this level.
         *
         * @param listener the target listener; never {@code null}
         * @param message  the log message; never {@code null}
         */
        public abstract void dispatch(ILogListener listener, String message);
    }

    // -----------------------------------------------------------------------
    // Singleton
    // -----------------------------------------------------------------------

    /**
     * The single instance, initialized eagerly at class-load time.
     * <p>
     * Eager initialisation is thread-safe under the Java memory model without
     * any explicit synchronization and avoids the double-checked locking
     * pitfalls of lazy initialisation.
     * </p>
     */
    private static final Log INSTANCE = new Log();

    /** Private constructor — use {@link #getInstance()}. */
    private Log() {}

    /**
     * Returns the singleton {@code Log} instance.
     *
     * @return the global logger; never {@code null}
     */
    public static Log getInstance() {
        return INSTANCE;
    }

    // -----------------------------------------------------------------------
    // Listener management
    // -----------------------------------------------------------------------

    /**
     * Thread-safe list of registered listeners.
     * <p>
     * {@link EventListenerList} is used because its
     * {@link EventListenerList#getListenerList()} returns an atomic snapshot
     * array, making iteration safe even if listeners are added or removed
     * concurrently.
     * </p>
     */
    private final EventListenerList listenerList = new EventListenerList();

    /** Bounded, oldest-first history used to populate listeners created later. */
    private final Deque<Entry> history = new ArrayDeque<>(HISTORY_CAPACITY);

    /**
     * Register a log listener.
     * <p>
     * A new listener first receives the retained history in chronological
     * order, followed by subsequent messages. Registering the same listener
     * instance again is a no-op, preventing duplicate output and replay.
     * </p>
     *
     * @param listener the listener to add; ignored if {@code null}
     */
    public synchronized void addLogListener(ILogListener listener) {
        if (listener == null) {
            return;
        }

        for (ILogListener registered : listenerList.getListeners(ILogListener.class)) {
            if (registered == listener) {
                return;
            }
        }

        listenerList.add(ILogListener.class, listener);
        for (Entry entry : history) {
            dispatch(listener, entry.level(), entry.message());
        }
    }

    /**
     * Unregister a log listener.
     * <p>
     * If {@code listener} is not currently registered this is a no-op.
     * </p>
     *
     * @param listener the listener to remove; ignored if {@code null}
     */
    public synchronized void removeLogListener(ILogListener listener) {
        if (listener == null) {
            return;
        }
        listenerList.remove(ILogListener.class, listener);
    }

    // -----------------------------------------------------------------------
    // Logging methods
    // -----------------------------------------------------------------------

    /**
     * Send an informational message to all registered listeners.
     * <p>
     * Use for normal application events: startup progress, successful
     * operations, state transitions.
     * </p>
     *
     * @param message the message; ignored if {@code null}
     */
    public void info(String message) {
        notifyListeners(message, Level.INFO);
    }

    /**
     * Send a configuration message to all registered listeners.
     * <p>
     * Use for application setup events: loaded resources, resolved file
     * paths, applied settings.
     * </p>
     *
     * @param message the message; ignored if {@code null}
     */
    public void config(String message) {
        notifyListeners(message, Level.CONFIG);
    }

    /**
     * Send a warning message to all registered listeners.
     * <p>
     * Use for recoverable problems or unexpected conditions that did not
     * prevent the application from continuing.
     * </p>
     *
     * @param message the message; ignored if {@code null}
     */
    public void warning(String message) {
        notifyListeners(message, Level.WARNING);
    }

    /**
     * Send an error message to all registered listeners.
     * <p>
     * Use for significant failures. The application may be able to continue,
     * but the operation that triggered the error did not complete
     * successfully.
     * </p>
     *
     * @param message the message; ignored if {@code null}
     */
    public void error(String message) {
        notifyListeners(message, Level.ERROR);
    }

    /**
     * Send a {@link Throwable}'s full stack trace to all registered
     * listeners at {@link Level#EXCEPTION} severity.
     * <p>
     * The throwable is converted to a string before dispatch; listeners
     * receive a plain {@code String} and never see the original object.
     * </p>
     *
     * @param t the throwable to log; ignored if {@code null}
     */
    public void exception(Throwable t) {
        if (t == null) {
            return;
        }
        notifyListeners(throwableToString(t), Level.EXCEPTION);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Dispatch {@code message} at {@code level} to every registered listener.
     * <p>
     * {@link EventListenerList#getListenerList()} returns an atomic snapshot,
     * so this method does not need to hold a lock. Individual listener
     * exceptions are caught and printed to {@code System.err} so that one
     * misbehaving listener cannot prevent others from receiving the message.
     * </p>
     *
     * @param message the message to dispatch; ignored if {@code null}
     * @param level   the severity level
     */
    private void notifyListeners(String message, Level level) {
        if (message == null) {
            return;
        }

        Object[] listeners;
        synchronized (this) {
            if (history.size() == HISTORY_CAPACITY) {
                history.removeFirst();
            }
            history.addLast(new Entry(level, message));
            listeners = listenerList.getListenerList();
        }

        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ILogListener.class) {
                dispatch((ILogListener) listeners[i + 1], level, message);
            }
        }
    }

    private static void dispatch(ILogListener listener, Level level, String message) {
        try {
            level.dispatch(listener, message);
        } catch (Exception ex) {
            // Use System.err directly to avoid recursive logging.
            System.err.println("Log: listener threw exception: " + ex.getMessage());
        }
    }

    /** Clears retained messages for isolated package-level tests. */
    synchronized void clearHistoryForTesting() {
        history.clear();
    }

    /**
     * Convert a {@link Throwable} to a string containing its full stack trace.
     *
     * @param t the throwable; must not be {@code null}
     * @return the stack trace as a string
     */
    private static String throwableToString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * Singleton objects may not be cloned.
     *
     * @throws CloneNotSupportedException always
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
}
