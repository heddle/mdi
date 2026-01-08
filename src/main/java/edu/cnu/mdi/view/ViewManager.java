package edu.cnu.mdi.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import edu.cnu.mdi.log.Log;

/**
 * Singleton registry and UI coordinator for all {@link BaseView} instances
 * (typically {@link javax.swing.JInternalFrame}s) in an MDI application.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Maintain a collection of currently-registered views.</li>
 *   <li>Maintain an optional "Views" {@link JMenu}: one menu item per view.</li>
 *   <li>Notify {@link IViewListener}s when views are added/removed.</li>
 *   <li>Track a {@link VirtualView} (if present) and optionally route newly
 *       activated views to it.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading</h3>
 * This class mutates Swing state (menu items, view visibility/icon state) and
 * therefore should be called from the Swing EDT. The implementation defensively
 * re-dispatches certain operations to the EDT when needed.
 *
 * <h3>Notes on API / Backward Compatibility</h3>
 * This class extends {@link Vector} to remain a drop-in replacement for older
 * code that iterates over {@code ViewManager.getInstance()} directly. To keep
 * internal bookkeeping consistent (listeners, menu items, disposing), this
 * class overrides several mutators (e.g. {@link #add(BaseView)},
 * {@link #remove(Object)}, {@link #clear()}). Prefer using
 * {@link #add(BaseView)} and {@link #remove(BaseView)} explicitly.
 */
@SuppressWarnings("serial")
public class ViewManager extends Vector<BaseView> {

    /** Singleton instance. */
    private static volatile ViewManager instance;

    /** Optional menu whose contents reflect the registered views. */
    private JMenu _viewMenu;

    /** Listener list for view add/remove notifications. */
    private EventListenerList _listenerList;

    /** If present, the application's virtual desktop view. */
    private VirtualView _virtualView;

    /**
     * Tracks the menu item created for each view so it can be removed when
     * the view is removed (prevents stale UI entries and listener retention).
     */
    private final Map<BaseView, JMenuItem> menuItems = new HashMap<>();

    /** Private constructor for singleton. */
    private ViewManager() {
        super(32);
    }

    /**
     * Obtain the singleton instance.
     *
     * @return the global {@link ViewManager}.
     */
    public static ViewManager getInstance() {
        if (instance == null) {
            synchronized (ViewManager.class) {
                if (instance == null) {
                    instance = new ViewManager();
                }
            }
        }
        return instance;
    }

    /**
     * Set the menu whose state is maintained by this manager.
     * <p>
     * When set, the menu is cleared and rebuilt from the current registered
     * views. When changed from one menu to another, any previous menu items are
     * dropped and recreated on the new menu.
     * </p>
     *
     * @param viewMenu the "Views" menu to maintain; may be {@code null}.
     */
    public void setViewMenu(JMenu viewMenu) {
        runOnEdt(() -> {
            _viewMenu = viewMenu;
            rebuildViewMenu();
        });
    }

    /**
     * Get the "Views" menu maintained by this manager.
     * <p>
     * This method never returns {@code null}. If no menu has been set explicitly
     * via {@link #setViewMenu(JMenu)}, a default menu is lazily created.
     * </p>
     *
     * @return a non-null Views menu.
     */
    public JMenu getViewMenu() {
        if (_viewMenu == null) {
            _viewMenu = new JMenu("Views");
            // If views already exist, populate the menu now.
            runOnEdt(this::rebuildViewMenu);
        }
        return _viewMenu;
    }

    /**
     * Make a view visible/invisible. If making visible and the view is not a
     * {@link VirtualView}, the manager will ask the current {@link VirtualView}
     * (if any) to activate the corresponding view cell.
     *
     * @param view the view to show/hide (must not be {@code null}).
     * @param vis  {@code true} to show; {@code false} to hide.
     */
    public void setVisible(BaseView view, boolean vis) {
        Objects.requireNonNull(view, "view must not be null");
        runOnEdt(() -> {
            view.setVisible(vis);
            if (vis && !(view instanceof VirtualView)) {
                makeViewVisibleInVirtualWorld(view);
            }
        });
    }

    /**
     * Register a view for control by this manager.
     * <p>
     * If a view menu is configured, a corresponding menu item is created.
     * Registered listeners are notified via {@link IViewListener#viewAdded(BaseView)}.
     * If the view is a {@link VirtualView}, it becomes the active virtual view.
     * </p>
     *
     * @param view the view to add (ignored if {@code null}).
     * @return {@code true} if the view was newly added; {@code false} if it was
     *         already present or {@code null}.
     */
    @Override
    public synchronized boolean add(BaseView view) {
        if ((view == null) || contains(view)) {
            return false;
        }

        boolean added = super.add(view);
        if (added) {
            Log.getInstance().info("ViewManager: added view: " + view.getTitle());

            if (view instanceof VirtualView) {
                _virtualView = (VirtualView) view;
            }

            // UI + notifications on EDT
            runOnEdt(() -> {
                ensureMenuItem(view);
                notifyListeners(view, true);
            });
        }
        return added;
    }

    /**
     * Unregister (remove) a view.
     * <p>
     * Removes its menu item (if any), notifies listeners, and disposes the view.
     * If the removed view is the current {@link VirtualView}, the virtual-view
     * reference is cleared.
     * </p>
     *
     * @param view the view to remove (ignored if {@code null}).
     * @return {@code true} if the view was removed; {@code false} otherwise.
     */
    public synchronized boolean remove(BaseView view) {
        if ((view == null) || !contains(view)) {
            return false;
        }

        Log.getInstance().info("ViewManager: removed view: " + view.getTitle());

        // UI + notifications on EDT
        runOnEdt(() -> {
            removeMenuItem(view);
            notifyListeners(view, false);
        });

        boolean removed = super.remove(view);

        if (removed) {
            if (view == _virtualView) {
                _virtualView = null;
            }
            // dispose on EDT to be safe
            runOnEdt(view::dispose);
        }
        return removed;
    }

    /**
     * Override {@link Vector#remove(Object)} so callers who hold a reference as
     * {@code Object} still go through the cleanup path.
     *
     * @param o the object to remove.
     * @return {@code true} if removed.
     */
    @Override
    public synchronized boolean remove(Object o) {
        if (o instanceof BaseView) {
            return remove((BaseView) o);
        }
        return false;
    }

    /**
     * Override {@link Vector#clear()} to ensure all views are removed via the
     * same cleanup path (menu removal, listener notification, disposal).
     */
    @Override
    public synchronized void clear() {
        // Remove from the end to avoid shifting costs and to mimic typical UI close ordering.
        for (int i = size() - 1; i >= 0; i--) {
            BaseView v = get(i);
            remove(v);
        }
    }

    /**
     * Ask the current {@link VirtualView} (if present) to activate the
     * cell/window corresponding to the specified view.
     *
     * @param view a non-virtual view to activate.
     */
    public void makeViewVisibleInVirtualWorld(BaseView view) {
        if ((_virtualView != null) && (_virtualView != view)) {
            _virtualView.activateViewCell(view);
        }
    }

    /**
     * Add a view lifecycle listener. Duplicates are avoided.
     *
     * @param listener listener to add (ignored if {@code null}).
     */
    public void addViewListener(IViewListener listener) {
        if (listener == null) {
            return;
        }
        if (_listenerList == null) {
            _listenerList = new EventListenerList();
        }
        // Avoid duplicates
        _listenerList.remove(IViewListener.class, listener);
        _listenerList.add(IViewListener.class, listener);
    }

    /**
     * Remove a view lifecycle listener.
     *
     * @param listener listener to remove (ignored if {@code null}).
     */
    public void removeViewListener(IViewListener listener) {
        if ((listener == null) || (_listenerList == null)) {
            return;
        }
        _listenerList.remove(IViewListener.class, listener);
    }

    // ------------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------------

    /**
     * Notify listeners that a view was added or removed.
     */
    private void notifyListeners(BaseView view, boolean added) {
        if (_listenerList == null) {
            return;
        }
        Object[] listeners = _listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == IViewListener.class) {
                IViewListener l = (IViewListener) listeners[i + 1];
                try {
                    if (added) {
                        l.viewAdded(view);
                    } else {
                        l.viewRemoved(view);
                    }
                } catch (Exception e) {
                    // Listener errors shouldn't break the manager.
                    Log.getInstance().warning("ViewManager: listener exception: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Ensure a menu item exists for the given view if a view menu is configured.
     */
    private void ensureMenuItem(BaseView view) {
        if ((_viewMenu == null) || menuItems.containsKey(view)) {
            return;
        }

        JMenuItem mi = new JMenuItem(view.getTitle());

        // Action shows/activates the view.
        ActionListener al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Restore if iconified.
                if (view.isIcon()) {
                    try {
                        view.setIcon(false);
                    } catch (PropertyVetoException ignored) {
                        // If vetoed, proceed with best effort.
                    }
                }
                view.setVisible(true);
                view.toFront();

                if (!(view instanceof VirtualView)) {
                    makeViewVisibleInVirtualWorld(view);
                }
            }
        };

        mi.addActionListener(al);
        _viewMenu.add(mi);
        menuItems.put(view, mi);
    }

    /**
     * Remove the menu item (if any) for the given view.
     */
    private void removeMenuItem(BaseView view) {
        JMenuItem mi = menuItems.remove(view);
        if ((_viewMenu != null) && (mi != null)) {
            _viewMenu.remove(mi);
            _viewMenu.revalidate();
            _viewMenu.repaint();
        }
    }

    /**
     * Rebuild the entire view menu from currently registered views.
     * Clears any stale menu item bookkeeping.
     */
    private void rebuildViewMenu() {
        menuItems.clear();

        if (_viewMenu == null) {
            return;
        }

        _viewMenu.removeAll();
        for (BaseView v : this) {
            ensureMenuItem(v);
        }
        _viewMenu.revalidate();
        _viewMenu.repaint();
    }

    /**
     * Run an action on the Swing EDT. If already on the EDT, runs immediately.
     */
    private static void runOnEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }
}
