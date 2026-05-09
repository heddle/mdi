package edu.cnu.mdi.hover;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.WeakHashMap;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Centralized hover management (tool-tip-like behavior) for arbitrary Swing
 * components.
 *
 * <h2>Threading</h2>
 * <p>All callbacks ({@link HoverListener#hoverUp} and
 * {@link HoverListener#hoverDown}) are guaranteed to fire on the Swing EDT.
 * Mouse events are already delivered on the EDT; the shared {@link Timer} also
 * fires on the EDT, so no explicit dispatch is needed in the normal path. The
 * {@code safeHoverUp}/{@code safeHoverDown} helpers defensively re-dispatch
 * for any caller that somehow bypasses the EDT.</p>
 *
 * <h2>Usage</h2>
 * <ol>
 *   <li>Call {@link #registerComponent(Component, HoverListener)} once per
 *       component.</li>
 *   <li>Call {@link #unregisterComponent(Component)} when the component or
 *       view is being disposed.</li>
 * </ol>
 *
 * <h2>lastPoint semantics</h2>
 * <p>
 * The {@link HoverEvent} passed to {@link HoverListener#hoverDown} always
 * contains the <em>most recent mouse position</em> seen for the component:
 * </p>
 * <ul>
 *   <li>For timer-driven expirations (the user moves the cursor while a hover
 *       is active), {@code lastPoint} is updated on every
 *       {@code mouseMoved}/{@code mouseDragged} event, so the position is
 *       current.</li>
 *   <li>For boundary exits ({@code mouseExited}), the actual exit point from
 *       the {@link MouseEvent} is forwarded directly.</li>
 *   <li>For programmatic exits (component hidden via a
 *       {@link HierarchyEvent}, or {@link #unregisterComponent} called while
 *       hovering), no mouse position is available; the last recorded position
 *       is used as a reasonable fallback.</li>
 * </ul>
 */
public final class HoverManager {

    private static final int DEFAULT_DELAY_MS = 600;

    private static final HoverManager instance = new HoverManager();

    /**
     * Single Swing {@link Timer} shared across all hover registrations.
     *
     * <p>A single timer is sufficient because at most one component can be
     * under the cursor at any time. The timer fires on the EDT.</p>
     */
    private final Timer timer;

    /**
     * Weak keys so the manager does not keep components alive.
     *
     * <p>Listeners must still be removed explicitly via
     * {@link #unregisterComponent(Component)} on dispose — the weak reference
     * only limits the blast radius of forgetting to do so.</p>
     */
    private final WeakHashMap<Component, Registration> registry =
            new WeakHashMap<>();

    /**
     * The currently pending or active hover task, if any.
     *
     * <p>Accessed only on the EDT.</p>
     */
    private HoverTask activeTask;

    /** Hover delay in milliseconds. */
    private int delayMs = DEFAULT_DELAY_MS;

    private HoverManager() {
        timer = new Timer(delayMs, this::handleTimerFired);
        timer.setRepeats(false);
    }

    /**
     * Returns the singleton {@code HoverManager}.
     *
     * @return the global instance; never {@code null}
     */
    public static HoverManager getInstance() {
        return instance;
    }

    /**
     * Set the hover delay.
     *
     * <p>Must be called on the EDT or before the UI is shown.</p>
     *
     * @param delayMs delay in milliseconds; must be &ge; 0
     * @throws IllegalArgumentException if {@code delayMs} is negative
     */
    public void setDelayMs(int delayMs) {
        if (delayMs < 0) {
            throw new IllegalArgumentException("delayMs must be >= 0");
        }
        this.delayMs = delayMs;
        timer.setInitialDelay(delayMs);
    }

    /**
     * Returns the current hover delay in milliseconds.
     *
     * @return hover delay; always &ge; 0
     */
    public int getDelayMs() {
        return delayMs;
    }

    /**
     * Register a component for hover events.
     *
     * <p>If the component is already registered, this method is a no-op.</p>
     *
     * @param comp     the component to watch; must not be {@code null}
     * @param listener the listener to notify; must not be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public void registerComponent(Component comp, HoverListener listener) {
        Objects.requireNonNull(comp, "comp");
        Objects.requireNonNull(listener, "listener");

        if (registry.containsKey(comp)) {
            return;
        }

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleMove(comp, listener, e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                handleMove(comp, listener, e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                handleMove(comp, listener, e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Forward the actual exit point so hoverDown receives the real
                // cursor position rather than the last hover position.
                handleExit(comp, listener, e.getPoint());
            }
        };

        // Hierarchy listener forces an exit when the component is hidden (e.g.
        // its parent view becomes invisible). No mouse position is available
        // in this case, so null is passed and the last known point is used.
        HierarchyListener hierarchyListener = (HierarchyEvent e) -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (!comp.isShowing()) {
                    handleExit(comp, listener, null);
                }
            }
        };

        comp.addMouseListener(mouseAdapter);
        comp.addMouseMotionListener(mouseAdapter);
        comp.addHierarchyListener(hierarchyListener);

        registry.put(comp, new Registration(mouseAdapter, hierarchyListener));
    }

    /**
     * Unregister a component and remove all listeners installed by
     * {@link #registerComponent(Component, HoverListener)}.
     *
     * <p>If a hover is currently active for this component,
     * {@link HoverListener#hoverDown} is <em>not</em> fired — the component
     * is being torn down and no further callbacks are appropriate. Safe to
     * call multiple times.</p>
     *
     * @param comp the component to unregister; ignored if {@code null}
     */
    public void unregisterComponent(Component comp) {
        if (comp == null) {
            return;
        }

        Registration reg = registry.remove(comp);
        if (reg == null) {
            // Not registered (or already GC'd from the weak map); still cancel
            // any pending task for this component to keep state consistent.
            if (activeTask != null && activeTask.component == comp) {
                cancelActiveTask();
            }
            return;
        }

        comp.removeMouseListener(reg.mouseAdapter);
        comp.removeMouseMotionListener(reg.mouseAdapter);
        comp.removeHierarchyListener(reg.hierarchyListener);

        // Cancel cleanly without firing hoverDown: the component is gone.
        if (activeTask != null && activeTask.component == comp) {
            cancelActiveTask();
        }
    }

    // -------------------------------------------------------------------------
    // Internal handlers (EDT only)
    // -------------------------------------------------------------------------

    /**
     * Processes a mouse-move, mouse-enter, or mouse-drag event.
     *
     * <p>Updates {@link HoverTask#lastPoint} on every call so that the point
     * passed to {@link HoverListener#hoverDown} is always the most recent
     * cursor position, regardless of how the hover ends.</p>
     *
     * @param comp     the source component
     * @param listener the registered listener for that component
     * @param e        the triggering mouse event
     */
    private void handleMove(Component comp, HoverListener listener, MouseEvent e) {
        if (!comp.isShowing()) {
            handleExit(comp, listener, e.getPoint());
            return;
        }

        Point p = e.getPoint();

        // Keep lastPoint current even when a hover is already active, so that
        // the hoverDown event fired by the next resetTask (or a later exit)
        // reflects where the cursor actually is.
        if (activeTask != null && activeTask.component == comp) {
            activeTask.lastPoint = new Point(p);
        }

        resetTask(comp, listener, p);
    }

    /**
     * Called when the mouse leaves a registered component's bounds, or when
     * the component stops showing.
     *
     * <p>If a hover gesture is currently active for this component,
     * {@link HoverListener#hoverDown} is fired with a {@link HoverEvent}
     * containing {@code exitPoint}. When {@code exitPoint} is {@code null}
     * (programmatic exit) the last known position is used as a fallback.</p>
     *
     * @param comp      the component being exited
     * @param listener  the listener registered for that component
     * @param exitPoint the component-local cursor position at exit time, or
     *                  {@code null} if no position is available (e.g. a
     *                  visibility change driven by code rather than by the
     *                  user)
     */
    private void handleExit(Component comp, HoverListener listener,
            Point exitPoint) {
        if (activeTask == null || activeTask.component != comp) {
            return;
        }

        if (activeTask.isHovering) {
            // Prefer the actual exit point; fall back to the last known position
            // when no real mouse position is available (programmatic exit).
            Point reportedPoint = (exitPoint != null)
                    ? exitPoint
                    : activeTask.lastPoint;
            activeTask.lastPoint = reportedPoint;
            safeHoverDown(activeTask.listener,
                    new HoverEvent(comp, reportedPoint));
        }
        cancelActiveTask();
    }

    private void resetTask(Component comp, HoverListener listener, Point p) {
        // If a hover was already active, dismiss it before starting a new one.
        if (activeTask != null && activeTask.isHovering) {
            safeHoverDown(activeTask.listener,
                    new HoverEvent(activeTask.component, activeTask.lastPoint));
        }

        activeTask = new HoverTask(comp, listener, p);
        timer.setInitialDelay(delayMs);
        timer.restart();
    }

    private void handleTimerFired(ActionEvent ignored) {
        if (activeTask == null) {
            return;
        }
        if (!activeTask.component.isShowing()) {
            cancelActiveTask();
            return;
        }

        activeTask.isHovering = true;
        safeHoverUp(activeTask.listener,
                new HoverEvent(activeTask.component, activeTask.lastPoint));
    }

    private void cancelActiveTask() {
        timer.stop();
        activeTask = null;
    }

    private static void safeHoverUp(HoverListener listener, HoverEvent evt) {
        if (SwingUtilities.isEventDispatchThread()) {
            listener.hoverUp(evt);
        } else {
            SwingUtilities.invokeLater(() -> listener.hoverUp(evt));
        }
    }

    private static void safeHoverDown(HoverListener listener, HoverEvent evt) {
        if (SwingUtilities.isEventDispatchThread()) {
            listener.hoverDown(evt);
        } else {
            SwingUtilities.invokeLater(() -> listener.hoverDown(evt));
        }
    }

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    /**
     * Transient state for the pending or active hover gesture on a single
     * component.
     *
     * <p>lastPoint semantics</p>
     * <p>{@code lastPoint} is <em>mutable</em> and is updated by
     * {@link HoverManager#handleMove} on every mouse event so that the point
     * passed to {@link HoverListener#hoverDown} reflects where the cursor
     * actually was when the hover ended:
     * </p>
     * <ul>
     *   <li>For cursor movements: updated continuously.</li>
     *   <li>For boundary exits ({@code mouseExited}): the actual exit point is
     *       written by {@link HoverManager#handleExit} immediately before the
     *       callback fires.</li>
     *   <li>For programmatic exits (visibility change, unregister): the last
     *       recorded point is used as a reasonable fallback.</li>
     * </ul>
     */
    private static final class HoverTask {
        final Component     component;
        final HoverListener listener;
        /** Mutable: kept current by handleMove and handleExit. */
        Point   lastPoint;
        boolean isHovering;

        HoverTask(Component component, HoverListener listener, Point p) {
            this.component = component;
            this.listener  = listener;
            this.lastPoint = (p == null) ? new Point(0, 0) : new Point(p);
        }
    }

    private static final class Registration {
        final MouseAdapter      mouseAdapter;
        final HierarchyListener hierarchyListener;

        Registration(MouseAdapter mouseAdapter,
                HierarchyListener hierarchyListener) {
            this.mouseAdapter      = mouseAdapter;
            this.hierarchyListener = hierarchyListener;
        }
    }
}