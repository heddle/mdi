package edu.cnu.mdi.util;

import java.util.EventListener;

/**
 * Listener interface for receiving periodic {@link Ping} callbacks.
 * <p>
 * This is a very small, Swing-friendly timing mechanism: a {@link Ping} object
 * uses a {@code javax.swing.Timer} internally and notifies all registered
 * {@code IPing} listeners on the Swing Event Dispatch Thread (EDT) at a fixed
 * interval.
 * </p>
 *
 * <p>
 * Typical usage:
 * </p>
 *
 * <pre>{@code
 * Ping ping = new Ping(1000); // fire every second
 * ping.addPingListener(() -> {
 * 	// do something inexpensive on the EDT
 * });
 * }</pre>
 *
 * Implementations should return quickly, since all invocations occur on the EDT
 * and long-running work will block the GUI.
 */
@FunctionalInterface
public interface IPing extends EventListener {

	/**
	 * Called whenever the associated {@link Ping} timer fires.
	 * <p>
	 * This method is always invoked on the Swing Event Dispatch Thread (EDT).
	 * Implementations should perform only lightweight operations and delegate any
	 * long-running work to a background thread.
	 * </p>
	 */
	void ping();
}
