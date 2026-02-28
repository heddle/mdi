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
 * Centralized hover management (tool-tip like behavior) for arbitrary Swing components.
 * <p>
 * - All callbacks happen on the EDT (mouse events + Swing Timer).
 * - Call {@link #registerComponent(Component, HoverListener)} once per component.
 * - Call {@link #unregisterComponent(Component)} when the component/view is being disposed.
 */
public final class HoverManager {

    private static final int DEFAULT_DELAY_MS = 600;

    private static final HoverManager instance = new HoverManager();

    /** Single Swing timer shared across all hover registrations. Fires on EDT. */
    private final Timer timer;

    /** Weak keys so the manager does not keep components alive; listeners must still be removed on dispose. */
    private final WeakHashMap<Component, Registration> registry = new WeakHashMap<>();

    /** Current pending task (if any). Accessed only on EDT. */
    private HoverTask activeTask;

    /** Hover delay in ms. */
    private int delayMs = DEFAULT_DELAY_MS;

    private HoverManager() {
        timer = new Timer(delayMs, this::handleTimerFired);
        timer.setRepeats(false);
    }

    public static HoverManager getInstance() {
        return instance;
    }

    /**
     * Set the hover delay. Must be called on EDT or before UI shows.
     */
    public void setDelayMs(int delayMs) {
        if (delayMs < 0) {
            throw new IllegalArgumentException("delayMs must be >= 0");
        }
        this.delayMs = delayMs;
        timer.setInitialDelay(delayMs);
    }

    public int getDelayMs() {
        return delayMs;
    }

    /**
     * Register a component for hover events.
     * <p>
     * If the component is already registered, this method is a no-op.
     */
    public void registerComponent(Component comp, HoverListener listener) {
        Objects.requireNonNull(comp, "comp");
        Objects.requireNonNull(listener, "listener");

        // If already registered, do nothing (keeps backward compatibility with your current behavior).
        if (registry.containsKey(comp)) {
            return;
        }

        // Mouse listener (also used as motion listener).
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
                handleExit(comp, listener);
            }
        };

        // Hierarchy listener to force exit if component is no longer showing.
        HierarchyListener hierarchyListener = (HierarchyEvent e) -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (!comp.isShowing()) {
                    handleExit(comp, listener);
                }
            }
        };

        comp.addMouseListener(mouseAdapter);
        comp.addMouseMotionListener(mouseAdapter);
        comp.addHierarchyListener(hierarchyListener);

        registry.put(comp, new Registration(mouseAdapter, hierarchyListener));
    }

    /**
     * Unregister a component and remove listeners installed by {@link #registerComponent(Component, HoverListener)}.
     * <p>
     * Safe to call multiple times.
     */
    public void unregisterComponent(Component comp) {
        if (comp == null) {
            return;
        }

        Registration reg = registry.remove(comp);
        if (reg == null) {
            // Not registered (or already GC'd from map); still attempt to cancel active task if it matches.
            if (activeTask != null && activeTask.component == comp) {
                cancelActiveTask();
            }
            return;
        }

        // Remove installed listeners.
        comp.removeMouseListener(reg.mouseAdapter);
        comp.removeMouseMotionListener(reg.mouseAdapter);
        comp.removeHierarchyListener(reg.hierarchyListener);

        // If this component is involved in the current pending/hovering task, cancel it cleanly.
        if (activeTask != null && activeTask.component == comp) {
            cancelActiveTask();
        }
    }

    // -----------------------------
    // Internal handlers (EDT only)
    // -----------------------------

    private void handleMove(Component comp, HoverListener listener, MouseEvent e) {
        if (!comp.isShowing()) {
            handleExit(comp, listener);
            return;
        }

        // Convert to component coords (MouseEvent already is), and reset the task.
        Point p = e.getPoint();
        resetTask(comp, listener, p);
    }

    private void handleExit(Component comp, HoverListener listener) {
        // If active task relates to this component, cancel it and send hoverDown if needed.
        if (activeTask != null && activeTask.component == comp) {
            if (activeTask.isHovering) {
                safeHoverDown(activeTask.listener, new HoverEvent(comp, activeTask.lastPoint));
            }
            cancelActiveTask();
        }
    }

    private void resetTask(Component comp, HoverListener listener, Point p) {
        // If we were hovering, first issue hoverDown (classic tooltip behavior).
        if (activeTask != null && activeTask.isHovering) {
            safeHoverDown(activeTask.listener, new HoverEvent(activeTask.component, activeTask.lastPoint));
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
        safeHoverUp(activeTask.listener, new HoverEvent(activeTask.component, activeTask.lastPoint));
    }

    private void cancelActiveTask() {
        timer.stop();
        activeTask = null;
    }

    private static void safeHoverUp(HoverListener listener, HoverEvent evt) {
        // Defensive: ensure hover callbacks always occur on EDT.
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

    private static final class HoverTask {
        final Component component;
        final HoverListener listener;
        final Point lastPoint;
        boolean isHovering;

        HoverTask(Component component, HoverListener listener, Point p) {
            this.component = component;
            this.listener = listener;
            this.lastPoint = (p == null) ? new Point(0, 0) : new Point(p); // defensive copy
        }
    }

    private static final class Registration {
        final MouseAdapter mouseAdapter;
        final HierarchyListener hierarchyListener;

        Registration(MouseAdapter mouseAdapter, HierarchyListener hierarchyListener) {
            this.mouseAdapter = mouseAdapter;
            this.hierarchyListener = hierarchyListener;
        }
    }
}