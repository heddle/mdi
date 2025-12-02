package edu.cnu.mdi.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;
import javax.swing.event.EventListenerList;

/**
 * Simple Swing-based timer that periodically notifies registered
 * {@link IPing} listeners.
 * <p>
 * Internally this class uses a {@link javax.swing.Timer}, so all callbacks
 * occur on the Swing Event Dispatch Thread (EDT). This makes it suitable
 * for driving lightweight UI updates, animation steps, or regular
 * housekeeping work in an MDI application.
 * </p>
 *
 * <p>
 * Example:
 * </p>
 *
 * <pre>{@code
 * Ping ping = new Ping(500); // fire every 500 ms
 * ping.addPingListener(() -> {
 *     // e.g. repaint a status component
 * });
 *
 * // Later, when you no longer need it:
 * ping.stop();       // or ping.close();
 * }</pre>
 */
public class Ping implements AutoCloseable {

    /**
     * The Swing timer responsible for firing ping events.
     */
    private final Timer timer;

    /**
     * Collection of registered {@link IPing} listeners.
     */
    private EventListenerList listeners;

    /**
     * Creates a new {@code Ping} that fires at the specified interval
     * and starts immediately.
     *
     * @param delayInMillis the delay between pings in milliseconds;
     *                      must be positive
     *
     * @throws IllegalArgumentException if {@code delayInMillis <= 0}
     */
    public Ping(int delayInMillis) {
        this(delayInMillis, true);
    }

    /**
     * Creates a new {@code Ping} that fires at the specified interval.
     *
     * @param delayInMillis     the delay between pings in milliseconds;
     *                          must be positive
     * @param startImmediately  if {@code true}, the timer is started
     *                          as soon as the {@code Ping} is created;
     *                          otherwise it remains stopped until
     *                          {@link #start()} is called
     *
     * @throws IllegalArgumentException if {@code delayInMillis <= 0}
     */
    public Ping(int delayInMillis, boolean startImmediately) {
        if (delayInMillis <= 0) {
            throw new IllegalArgumentException("delayInMillis must be > 0");
        }

        listeners = new EventListenerList();

        ActionListener taskPerformer = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                notifyListeners();
            }
        };

        timer = new Timer(delayInMillis, taskPerformer);
        timer.setRepeats(true);

        if (startImmediately) {
            timer.start();
        }
    }

    /**
     * Starts the underlying timer if it is not already running.
     * <p>
     * All subsequent ticks will result in calls to {@link IPing#ping()}
     * for each registered listener until {@link #stop()} or
     * {@link #close()} is called.
     * </p>
     */
    public void start() {
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    /**
     * Stops the underlying timer. No further {@link IPing#ping()} callbacks
     * will occur until {@link #start()} is called again.
     */
    public void stop() {
        if (timer.isRunning()) {
            timer.stop();
        }
    }

    /**
     * Returns {@code true} if the underlying timer is currently running.
     *
     * @return {@code true} if the timer is running; {@code false} otherwise
     */
    public boolean isRunning() {
        return timer.isRunning();
    }

    /**
     * Returns the current delay between timer firings, in milliseconds.
     *
     * @return the delay between pings in milliseconds
     */
    public int getDelay() {
        return timer.getDelay();
    }

    /**
     * Sets the delay between timer firings, in milliseconds.
     *
     * @param delayInMillis the new delay between pings; must be positive
     *
     * @throws IllegalArgumentException if {@code delayInMillis <= 0}
     */
    public void setDelay(int delayInMillis) {
        if (delayInMillis <= 0) {
            throw new IllegalArgumentException("delayInMillis must be > 0");
        }
        timer.setDelay(delayInMillis);
    }

    /**
     * Adds a ping listener that will be notified on each timer firing.
     *
     * @param listener the ping listener to add; ignored if {@code null}
     */
    public void addPingListener(IPing listener) {
        if (listener == null) {
            return;
        }
        if (listeners == null) {
            listeners = new EventListenerList();
        }
        listeners.add(IPing.class, listener);
    }

    /**
     * Removes a previously registered ping listener.
     *
     * @param listener the ping listener to remove; ignored if {@code null}
     */
    public void removePingListener(IPing listener) {
        if (listener == null || listeners == null) {
            return;
        }
        listeners.remove(IPing.class, listener);
    }

    /**
     * Notifies all registered {@link IPing} listeners that a timer tick
     * has occurred.
     * <p>
     * This method is invoked on the Swing Event Dispatch Thread (EDT)
     * by the internal {@link javax.swing.Timer}.
     * </p>
     */
    private void notifyListeners() {
        if (listeners == null) {
            return;
        }

        // Guaranteed to return a non-null array
        Object[] l = listeners.getListenerList();

        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = l.length - 2; i >= 0; i -= 2) {
            if (l[i] == IPing.class) {
                ((IPing) l[i + 1]).ping();
            }
        }
    }

    /**
     * Stops the timer and releases any resources held by this {@code Ping}.
     * <p>
     * This method simply calls {@link #stop()} and is provided so that
     * {@code Ping} can be used with try-with-resources:
     * </p>
     *
     * <pre>{@code
     * try (Ping ping = new Ping(250)) {
     *     ping.addPingListener(...);
     *     // use ping
     * }
     * // timer automatically stopped here
     * }</pre>
     */
    @Override
    public void close() {
        stop();
    }
}
