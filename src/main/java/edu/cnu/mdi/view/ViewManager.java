package edu.cnu.mdi.view;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import edu.cnu.mdi.log.Log;

/**
 * Singleton registry and UI coordinator for all {@link BaseView} instances
 * in an MDI application.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Maintain the ordered collection of currently-registered views.</li>
 *   <li>Maintain the "Views" {@link JMenu}: one item per registered view,
 *       plus italic placeholder items for lazy configurations whose views
 *       have not yet been created.</li>
 *   <li>Notify {@link IViewListener}s when views are added or removed.</li>
 *   <li>Track the active {@link VirtualView} (if any) and route view
 *       activation requests to it.</li>
 *   <li>Support lazy registration of views through
 *       {@link ViewConfiguration}.</li>
 * </ul>
 *
 * <h2>Views menu conventions</h2>
 * <p>
 * Each realized view has a normal-weight menu item labelled with the view's
 * title. Unrealized lazy configurations have an italic item labelled with
 * the configuration's {@link ViewConfiguration#getMenuTitle() menu title}
 * (no "Create " prefix — the italic style alone signals "not yet open").
 * When a lazy view is realized its italic placeholder is replaced in-place by
 * the normal item so that menu ordering is preserved.
 * </p>
 *
 * <h2>Threading</h2>
 * <p>
 * All Swing state mutations (menu items, view visibility) are dispatched to
 * the EDT. The collection itself is {@code synchronized} on {@code this} for
 * the add/remove operations inherited from {@link Vector}.
 * </p>
 *
 * <h2>Backward compatibility</h2>
 * <p>
 * This class extends {@link Vector} so that existing code iterating over
 * {@code ViewManager.getInstance()} directly continues to compile without
 * change. Prefer the typed {@link #add(BaseView)} and
 * {@link #remove(BaseView)} methods over the raw {@link Vector} mutators.
 * </p>
 */
@SuppressWarnings("serial")
public class ViewManager extends Vector<BaseView> {

    // -----------------------------------------------------------------------
    // Singleton
    // -----------------------------------------------------------------------

    /** The single application-wide instance. */
    private static volatile ViewManager instance;

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /**
     * All registered view configurations (both eager and lazy), in
     * registration order.
     */
    private final java.util.List<ViewConfiguration<?>> configs =
            new java.util.ArrayList<>();

    /**
     * The "Views" menu whose contents mirror the set of registered views.
     * May be {@code null} until first accessed or explicitly set.
     */
    private JMenu viewMenu;

    /** Listeners notified when views are added or removed. */
    private EventListenerList listenerList;

    /**
     * The active virtual desktop view, or {@code null} if none has been
     * registered.
     */
    private VirtualView virtualView;

    /**
     * Maps each realized {@link BaseView} to the menu item that represents it.
     * Used to remove the item cleanly when the view is unregistered.
     */
    private final Map<BaseView, JMenuItem> menuItems = new HashMap<>();

    /**
     * Maps each unrealized lazy {@link ViewConfiguration} to its italic
     * placeholder menu item. When the view is realized the placeholder is
     * replaced in-place by the normal item, preserving menu order.
     * The entry is removed once the view is realized.
     */
    private final Map<ViewConfiguration<?>, JMenuItem> lazyMenuItems =
            new HashMap<>();

    // -----------------------------------------------------------------------
    // Construction / singleton access
    // -----------------------------------------------------------------------

    /** Private constructor: use {@link #getInstance()}. */
    private ViewManager() {
        super(32);
    }

    /**
     * Returns the singleton {@code ViewManager} instance, creating it on
     * first call.
     *
     * @return the global {@code ViewManager}
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

    // -----------------------------------------------------------------------
    // Views menu
    // -----------------------------------------------------------------------

    /**
     * Replace the managed Views menu.
     * <p>
     * The new menu is immediately populated from the current set of registered
     * views and unrealized lazy configurations.
     * </p>
     *
     * @param viewMenu the menu to manage; {@code null} clears the reference
     */
    public void setViewMenu(JMenu viewMenu) {
        runOnEdt(() -> {
            this.viewMenu = viewMenu;
            rebuildViewMenu();
        });
    }

    /**
     * Returns the managed Views menu, creating a default one lazily if none
     * has been set via {@link #setViewMenu(JMenu)}.
     * <p>
     * This method never returns {@code null}.
     * </p>
     *
     * @return the Views menu
     */
    public JMenu getViewMenu() {
        if (viewMenu == null) {
            viewMenu = new JMenu("Views");
            runOnEdt(this::rebuildViewMenu);
        }
        return viewMenu;
    }

    // -----------------------------------------------------------------------
    // View registration
    // -----------------------------------------------------------------------

    /**
     * Register a view with this manager.
     * <p>
     * A corresponding menu item is added to the Views menu. Registered
     * {@link IViewListener}s are notified. If the view is a
     * {@link VirtualView} it becomes the active virtual desktop.
     * </p>
     *
     * @param view the view to register; ignored if {@code null} or already
     *             registered
     * @return {@code true} if the view was newly added
     */
    @Override
    public synchronized boolean add(BaseView view) {
        if (view == null || contains(view)) {
            return false;
        }

        boolean added = super.add(view);
        if (added) {
            if (view instanceof VirtualView) {
                virtualView = (VirtualView) view;
            }
            runOnEdt(() -> {
                ensureRealizedMenuItem(view);
                notifyListeners(view, true);
            });
        }
        return added;
    }

    /**
     * Unregister a view.
     * <p>
     * Its menu item is removed, registered listeners are notified, and the
     * view is disposed on the EDT.
     * </p>
     *
     * @param view the view to remove; ignored if {@code null} or not registered
     * @return {@code true} if the view was removed
     */
    public synchronized boolean remove(BaseView view) {
        if (view == null || !contains(view)) {
            return false;
        }

        runOnEdt(() -> {
            removeRealizedMenuItem(view);
            notifyListeners(view, false);
        });

        boolean removed = super.remove(view);

        if (removed) {
            if (view == virtualView) {
                virtualView = null;
            }
            runOnEdt(view::dispose);
        }
        return removed;
    }

    /**
     * Overrides {@link Vector#remove(Object)} so removal of a {@link BaseView}
     * always goes through the full cleanup path in {@link #remove(BaseView)}.
     *
     * @param o the object to remove
     * @return {@code true} if removed
     */
    @Override
    public synchronized boolean remove(Object o) {
        if (o instanceof BaseView) {
            return remove((BaseView) o);
        }
        return false;
    }

    /**
     * Removes all registered views through the standard cleanup path.
     */
    @Override
    public synchronized void clear() {
        for (int i = size() - 1; i >= 0; i--) {
            remove(get(i));
        }
    }

    // -----------------------------------------------------------------------
    // View configuration (lazy / eager)
    // -----------------------------------------------------------------------

    /**
     * Register a {@link ViewConfiguration}.
     * <p>
     * If the configuration is eager ({@link ViewConfiguration#lazily} is
     * {@code false}) the view is created immediately by calling
     * {@link ViewConfiguration#getView()}. If it is lazy, an italic
     * placeholder item is inserted into the Views menu; the view is created
     * only when the user selects that item.
     * </p>
     *
     * @param config the configuration to register; ignored if {@code null}
     */
    public void addConfiguration(ViewConfiguration<?> config) {
        if (config == null) {
            return;
        }

        configs.add(config);

        if (config.lazily) {
            runOnEdt(() -> insertLazyPlaceholder(config));
        } else {
            config.getView();
        }
    }

    // -----------------------------------------------------------------------
    // Virtual-desktop routing
    // -----------------------------------------------------------------------

    /**
     * Ask the active {@link VirtualView} to scroll so that the column
     * containing the given view becomes visible.
     * <p>
     * This is a no-op if no virtual view is registered, or if {@code view}
     * is itself the virtual view.
     * </p>
     *
     * @param view the view to make visible in the virtual desktop
     */
    public void makeViewVisibleInVirtualWorld(BaseView view) {
        if (virtualView != null && virtualView != view) {
            virtualView.activateViewCell(view);
        }
    }

    // -----------------------------------------------------------------------
    // Listeners
    // -----------------------------------------------------------------------

    /**
     * Add a view lifecycle listener.
     * <p>
     * Duplicate registrations are silently ignored.
     * </p>
     *
     * @param listener the listener to add; ignored if {@code null}
     */
    public void addViewListener(IViewListener listener) {
        if (listener == null) {
            return;
        }
        if (listenerList == null) {
            listenerList = new EventListenerList();
        }
        listenerList.remove(IViewListener.class, listener);
        listenerList.add(IViewListener.class, listener);
    }

    /**
     * Remove a view lifecycle listener.
     *
     * @param listener the listener to remove; ignored if {@code null}
     */
    public void removeViewListener(IViewListener listener) {
        if (listener == null || listenerList == null) {
            return;
        }
        listenerList.remove(IViewListener.class, listener);
    }

    // -----------------------------------------------------------------------
    // Private — menu management
    // -----------------------------------------------------------------------

    /**
     * Ensure a normal (non-italic) menu item exists for the given realized
     * view.
     * <p>
     * This method is called from {@link #add(BaseView)}, which fires during
     * the {@link BaseView} constructor. At that point a lazy
     * {@link ViewConfiguration} has not yet stored the view reference (the
     * assignment happens after {@code factory.create()} returns), so
     * {@link #findConfigForView} cannot locate the matching placeholder here.
     * Placeholder replacement is therefore handled separately in
     * {@link #insertLazyPlaceholder}, whose action listener performs the swap
     * synchronously <em>after</em> realization completes.  This method simply
     * appends a new item if none exists yet.
     * </p>
     *
     * @param view the newly realized view
     */
    private void ensureRealizedMenuItem(BaseView view) {
        if (viewMenu == null || menuItems.containsKey(view)) {
            return;
        }

        JMenuItem mi = new JMenuItem(view.getTitle());
        mi.addActionListener(makeShowViewListener(view));
        viewMenu.add(mi);
        menuItems.put(view, mi);
        viewMenu.revalidate();
        viewMenu.repaint();
    }

    /**
     * Replace a lazy placeholder with the realized view's menu item
     * in-place, preserving menu order.
     * <p>
     * Called by the placeholder's action listener <em>after</em>
     * {@link ViewConfiguration#getView()} has returned, so the view is
     * guaranteed to be registered and its normal menu item already appended
     * by {@link #ensureRealizedMenuItem}.  This method removes that appended
     * item and re-inserts it at the position previously occupied by the
     * placeholder.
     * </p>
     *
     * @param config      the lazy configuration that was just realized
     * @param placeholder the italic placeholder item to replace
     */
    private void swapPlaceholderForRealItem(ViewConfiguration<?> config,
            JMenuItem placeholder) {
        if (viewMenu == null) {
            return;
        }

        // Record the placeholder's position before removing it.
        int pos = indexOfComponent(placeholder);
        viewMenu.remove(placeholder);
        lazyMenuItems.remove(config);

        // The normal item was appended by ensureRealizedMenuItem() during
        // add(); move it to the placeholder's former position.
        BaseView view = config.getView();
        JMenuItem realItem = (view != null) ? menuItems.get(view) : null;
        if (realItem != null) {
            viewMenu.remove(realItem);
            int insertAt = (pos >= 0)
                    ? Math.min(pos, viewMenu.getItemCount())
                    : viewMenu.getItemCount();
            viewMenu.insert(realItem, insertAt);
        }

        viewMenu.revalidate();
        viewMenu.repaint();
    }

    /**
     * Remove the menu item associated with a realized view.
     *
     * @param view the view whose menu item should be removed
     */
    private void removeRealizedMenuItem(BaseView view) {
        JMenuItem mi = menuItems.remove(view);
        if (viewMenu != null && mi != null) {
            viewMenu.remove(mi);
            viewMenu.revalidate();
            viewMenu.repaint();
        }
    }

    /**
     * Insert an italic placeholder item for a lazy configuration.
     * <p>
     * The label is simply the configuration's
     * {@link ViewConfiguration#getMenuTitle() menu title} in italics. The
     * italic style is sufficient to communicate "not yet open"; no "Create "
     * prefix is added because it reads awkwardly alongside regular view names
     * and implies that a dialog will appear.
     * </p>
     *
     * @param config the lazy configuration
     */
    private void insertLazyPlaceholder(final ViewConfiguration<?> config) {
        if (viewMenu == null) {
            return;
        }

        final JMenuItem placeholder = new JMenuItem(config.getMenuTitle());
        placeholder.setFont(placeholder.getFont().deriveFont(Font.ITALIC));

        placeholder.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Realize the view. BaseView's constructor calls add(), which
                // appends a normal menu item. Once getView() returns we swap
                // that appended item into the placeholder's former position.
                config.getView();
                swapPlaceholderForRealItem(config, placeholder);
            }
        });

        viewMenu.add(placeholder);
        lazyMenuItems.put(config, placeholder);
        viewMenu.revalidate();
        viewMenu.repaint();
    }

    /**
     * Rebuild the entire Views menu from scratch.
     * <p>
     * Called when the menu reference changes via {@link #setViewMenu(JMenu)}.
     * Clears both tracking maps, re-adds items for all realized views, then
     * re-adds placeholders for any unrealized lazy configurations.
     * </p>
     */
    private void rebuildViewMenu() {
        menuItems.clear();
        lazyMenuItems.clear();

        if (viewMenu == null) {
            return;
        }

        viewMenu.removeAll();

        for (BaseView v : this) {
            ensureRealizedMenuItem(v);
        }

        for (ViewConfiguration<?> config : configs) {
            if (config.lazily && !config.isRealized()) {
                insertLazyPlaceholder(config);
            }
        }

        viewMenu.revalidate();
        viewMenu.repaint();
    }

    /**
     * Returns the zero-based index of a menu component within
     * {@link #viewMenu}, or {@code -1} if not found.
     *
     * @param item the component to locate
     * @return its index in the menu, or {@code -1}
     */
    private int indexOfComponent(JMenuItem item) {
        if (viewMenu == null || item == null) {
            return -1;
        }
        for (int i = 0; i < viewMenu.getMenuComponentCount(); i++) {
            if (viewMenu.getMenuComponent(i) == item) {
                return i;
            }
        }
        return -1;
    }

    // -----------------------------------------------------------------------
    // Private — action listener factory
    // -----------------------------------------------------------------------

    /**
     * Build the action listener that shows and focuses a view when its menu
     * item is selected.
     * <p>
     * If the view is iconified it is de-iconified first. For non-virtual views,
     * the virtual desktop is scrolled so the view's column becomes visible.
     * </p>
     *
     * @param view the view to show
     * @return the action listener
     */
    private ActionListener makeShowViewListener(BaseView view) {
        return e -> {
            if (view.isIcon()) {
                try {
                    view.setIcon(false);
                } catch (PropertyVetoException ignored) {
                }
            }
            view.setVisible(true);
            view.toFront();

            if (!(view instanceof VirtualView)) {
                makeViewVisibleInVirtualWorld(view);
            }
        };
    }

    // -----------------------------------------------------------------------
    // Private — listener notification
    // -----------------------------------------------------------------------

    /**
     * Notify all registered {@link IViewListener}s that a view was added or
     * removed. Exceptions thrown by individual listeners are caught and logged
     * so that one misbehaving listener cannot prevent others from being
     * notified.
     *
     * @param view  the affected view
     * @param added {@code true} if added, {@code false} if removed
     */
    private void notifyListeners(BaseView view, boolean added) {
        if (listenerList == null) {
            return;
        }
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == IViewListener.class) {
                IViewListener l = (IViewListener) listeners[i + 1];
                try {
                    if (added) {
                        l.viewAdded(view);
                    } else {
                        l.viewRemoved(view);
                    }
                } catch (Exception ex) {
                    Log.getInstance().warning(
                            "ViewManager: listener exception: " + ex.getMessage());
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Private — EDT helper
    // -----------------------------------------------------------------------

    /**
     * Execute {@code r} on the Swing EDT.
     * <p>
     * If the calling thread is already the EDT the runnable is executed
     * directly; otherwise it is queued via {@link SwingUtilities#invokeLater}.
     * </p>
     *
     * @param r the action to run on the EDT
     */
    private static void runOnEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }
}