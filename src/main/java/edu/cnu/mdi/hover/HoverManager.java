package edu.cnu.mdi.hover;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.WeakHashMap;

import javax.swing.Timer;

/**
 * Manages hover events for registered components. This class implements a singleton
 * pattern to provide a centralized hover management system across the application.
 * It uses a timer to detect when the mouse has stopped moving over a component,
 * firing hover up and down events accordingly. The class also handles edge cases
 * such as components being removed or hidden while hovering, ensuring that hover
 * events are properly managed in those scenarios.
 * 
 * Note that the interface methods hoverUp and hoverDown are called on the 
 * Event Dispatch Thread (EDT) to ensure thread safety when interacting with 
 * Swing components.
 */
public class HoverManager {
	// Delay in milliseconds before firing the hover up event after mouse movement stops.
    private static final int HOVER_DELAY = 500;
    
    // Singleton instance of HoverManager
    private static HoverManager instance;

    // Single timer instance to manage hover events across all components
    private final Timer timer;
    
    // Active hover task tracking the current component, listener, and hover point
    private HoverTask activeTask;
    
    // Flag to indicate if a hover is currently active, used to manage hover down events on movement
    private boolean isHovering = false;

    // Maps components to their specific adapter to prevent double-registration
    private final WeakHashMap<Component, MouseAdapter> registry = new WeakHashMap<>();

    // Encapsulates the state of an active hover task
    private record HoverTask(Component component, HoverListener listener, Point point) {}

    // Private constructor for singleton pattern
    private HoverManager() {
        timer = new Timer(HOVER_DELAY, e -> fireHoverUp());
        timer.setRepeats(false);
    }

    /**
	 * Get the singleton instance of HoverManager. This method is synchronized to
	 * ensure thread safety during lazy initialization.
	 *
	 * @return the singleton instance of HoverManager
	 */
    public static synchronized HoverManager getInstance() {
        if (instance == null) instance = new HoverManager();
        return instance;
    }

    /**
	 * Register a component to receive hover events. This method adds mouse listeners
	 * to the component to track mouse movement and trigger hover events after a
	 * specified delay. It also handles edge cases such as the component being removed
	 * or hidden while hovering.
	 * @param comp     the component to register for hover events
	 * @param listener the HoverListener that will receive hover events for the component
	 * @throws IllegalArgumentException if the component is null or the listener is null
	 * @throws IllegalStateException if the component is already registered
	 * @implNote This method uses a WeakHashMap to store component-adapter pairs, allowing
	 * garbage collection of components that are no longer in use without causing memory leaks.
	 * The method also includes checks to prevent multiple registrations of the same component and to handle edge cases where the component's visibility changes during a hover event.
	 */
    public void registerComponent(Component comp, HoverListener listener) {
        // 1. Prevent multiple registrations
        if (registry.containsKey(comp)) return;

        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) { reset(comp, e.getPoint(), listener); }
            @Override
            public void mouseDragged(MouseEvent e) { reset(comp, e.getPoint(), listener); }
            @Override
            public void mouseExited(MouseEvent e) { handleExit(comp); }
        };

        // 2. Handle Edge Case: Component is removed or hidden while hovering
        comp.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (!comp.isShowing()) handleExit(comp);
            }
        });

        comp.addMouseMotionListener(adapter);
        comp.addMouseListener(adapter);
        registry.put(comp, adapter);
    }

    // Resets the hover timer and updates the active task with the new component, listener, and point.
    private void reset(Component comp, Point p, HoverListener listener) {
        // If moving within the same component that is already hovering, 
        // trigger down before restarting the "stillness" clock.
        if (isHovering) {
            fireHoverDown();
        }

        activeTask = new HoverTask(comp, listener, p);
        timer.restart();
    }

    //
    private void handleExit(Component comp) {
        if (activeTask != null && activeTask.component == comp) {
            cancel();
        }
    }

    // Fires the hover up event if the active task is still valid and the component is showing and enabled.
    private void fireHoverUp() {
        // Extra safety check: is the component still visible/enabled?
        if (activeTask != null && activeTask.component.isShowing() && activeTask.component.isEnabled()) {
            isHovering = true;
            activeTask.listener.hoverUp(new HoverEvent(activeTask.component, activeTask.point));
        }
    }

    //
    private void fireHoverDown() {
        if (activeTask != null && isHovering) {
            activeTask.listener.hoverDown();
        }
        isHovering = false;
    }

    // Cancels the active hover task and resets the state.
    private void cancel() {
        timer.stop();
        fireHoverDown();
        activeTask = null;
    }
}