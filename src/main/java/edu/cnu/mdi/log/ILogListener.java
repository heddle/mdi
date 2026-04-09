package edu.cnu.mdi.log;

import java.util.EventListener;

/**
 * Listener interface for receiving messages from the {@link Log} singleton.
 *
 * <h2>Usage</h2>
 * <p>
 * Implement this interface and register the implementation with
 * {@link Log#addLogListener(ILogListener)} to receive log messages.
 * All five methods have default no-op implementations, so an implementor
 * need only override the levels it cares about:
 * </p>
 * <pre>
 * Log.getInstance().addLogListener(new ILogListener() {
 *     &#64;Override
 *     public void warning(String message) {
 *         System.err.println("WARN: " + message);
 *     }
 * });
 * </pre>
 *
 * <h2>Threading</h2>
 * <p>
 * Methods on this interface may be called from any thread. Implementations
 * that update Swing components must dispatch to the EDT themselves.
 * </p>
 *
 * <h2>Backward compatibility</h2>
 * <p>
 * All methods carry default no-op implementations. Existing implementations
 * that override all five methods continue to compile and behave identically.
 * New implementations may override any subset.
 * </p>
 */
public interface ILogListener extends EventListener {

    /**
     * Receives an informational message.
     * <p>
     * Info messages describe normal application events (startup progress,
     * successful operations, state transitions). The default implementation
     * is a no-op.
     * </p>
     *
     * @param message the informational message; never {@code null}
     */
    default void info(String message) {}

    /**
     * Receives a configuration message.
     * <p>
     * Config messages describe how the application has been set up: loaded
     * resources, resolved file paths, applied settings. They are typically
     * shown in a distinct color in the log UI. The default implementation
     * is a no-op.
     * </p>
     *
     * @param message the configuration message; never {@code null}
     */
    default void config(String message) {}

    /**
     * Receives a warning message.
     * <p>
     * Warnings indicate a recoverable problem or an unexpected condition that
     * did not prevent the application from continuing. The default
     * implementation is a no-op.
     * </p>
     *
     * @param message the warning message; never {@code null}
     */
    default void warning(String message) {}

    /**
     * Receives an error message.
     * <p>
     * Errors indicate a significant failure. The application may be able to
     * continue, but the operation that triggered the error did not complete
     * successfully. The default implementation is a no-op.
     * </p>
     *
     * @param message the error message; never {@code null}
     */
    default void error(String message) {}

    /**
     * Receives an exception message.
     * <p>
     * Exception messages are produced when a {@link Throwable} is passed to
     * {@link Log#exception(Throwable)}. The message contains the full stack
     * trace as a string. The default implementation is a no-op.
     * </p>
     *
     * @param message the formatted stack trace; never {@code null}
     */
    default void exception(String message) {}
}