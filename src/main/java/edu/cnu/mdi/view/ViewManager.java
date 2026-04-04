package edu.cnu.mdi.view;

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
 * (typically {@link javax.swing.JInternalFrame}s) in an MDI application.
 * <p>
 * Responsibilities:
 * </p>
 * <ul>
 * <li>Maintain a collection of currently-registered views.</li>
 * <li>Maintain an optional "Views" {@link JMenu}: one menu item per view.</li>
 * <li>Notify {@link IViewListener}s when views are added/removed.</li>
 * <li>Track a {@link VirtualView} (if present) and optionally route newly
 * activated views to it.</li>
 * <li>Support lazy registration of views through {@link ViewConfiguration}.</li>
 * </ul>
 *
 * <h3>Threading</h3>
 * <p>
 * This class mutates Swing state (menu items, view visibility/icon state) and
 * therefore should be called from the Swing EDT. The implementation
 * defensively re-dispatches certain operations to the EDT when needed.
 * </p>
 *
 * <h3>Notes on API / Backward Compatibility</h3>
 * <p>
 * This class extends {@link Vector} to remain a drop-in replacement for older
 * code that iterates over {@code ViewManager.getInstance()} directly. To keep
 * internal bookkeeping consistent (listeners, menu items, disposing), this
 * class overrides several mutators (e.g. {@link #add(BaseView)},
 * {@link #remove(Object)}, {@link #clear()}). Prefer using
 * {@link #add(BaseView)} and {@link #remove(BaseView)} explicitly.
 * </p>
 */
@SuppressWarnings("serial")
public class ViewManager extends Vector<BaseView> {

	/** Singleton instance. */
	private static volatile ViewManager instance;

	/** List of registered view configurations, including lazy ones. */
	private final java.util.List<ViewConfiguration<?>> configs = new java.util.ArrayList<>();

	/** Optional menu whose contents reflect the registered views. */
	private JMenu viewMenu;

	/** Listener list for view add/remove notifications. */
	private EventListenerList listenerList;

	/** If present, the application's virtual desktop view. */
	private VirtualView virtualView;

	/**
	 * Tracks the menu item created for each view so it can be removed when the view is
	 * removed.
	 */
	private final Map<BaseView, JMenuItem> menuItems = new HashMap<>();

	/** Private constructor for singleton. */
	private ViewManager() {
		super(32);
	}

	/**
	 * Obtain the singleton instance.
	 *
	 * @return the global {@link ViewManager}
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
	 * When set, the menu is cleared and rebuilt from the current registered views.
	 * Lazy placeholders are then re-added for any configurations that have not yet
	 * been realized.
	 * </p>
	 *
	 * @param viewMenu the "Views" menu to maintain; may be {@code null}
	 */
	public void setViewMenu(JMenu viewMenu) {
		runOnEdt(() -> {
			this.viewMenu = viewMenu;
			rebuildViewMenu();
		});
	}

	/**
	 * Get the "Views" menu maintained by this manager.
	 * <p>
	 * This method never returns {@code null}. If no menu has been set explicitly via
	 * {@link #setViewMenu(JMenu)}, a default menu is lazily created.
	 * </p>
	 *
	 * @return a non-null Views menu
	 */
	public JMenu getViewMenu() {
		if (viewMenu == null) {
			viewMenu = new JMenu("Views");
			runOnEdt(this::rebuildViewMenu);
		}
		return viewMenu;
	}

	/**
	 * Register a view for control by this manager.
	 * <p>
	 * If a view menu is configured, a corresponding menu item is created. Registered
	 * listeners are notified via {@link IViewListener#viewAdded(BaseView)}. If the
	 * view is a {@link VirtualView}, it becomes the active virtual view.
	 * </p>
	 *
	 * @param view the view to add
	 * @return {@code true} if the view was newly added
	 */
	@Override
	public synchronized boolean add(BaseView view) {
		if ((view == null) || contains(view)) {
			return false;
		}

		boolean added = super.add(view);
		if (added) {

			if (view instanceof VirtualView) {
				virtualView = (VirtualView) view;
			}

			runOnEdt(() -> {
				ensureMenuItem(view);
				notifyListeners(view, true);
			});
		}
		return added;
	}

	/**
	 * Unregister a view.
	 *
	 * @param view the view to remove
	 * @return {@code true} if removed
	 */
	public synchronized boolean remove(BaseView view) {
		if ((view == null) || !contains(view)) {
			return false;
		}

		runOnEdt(() -> {
			removeMenuItem(view);
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
	 * Override {@link Vector#remove(Object)} so callers still go through the cleanup
	 * path.
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
	 * Remove all views through the normal cleanup path.
	 */
	@Override
	public synchronized void clear() {
		for (int i = size() - 1; i >= 0; i--) {
			BaseView v = get(i);
			remove(v);
		}
	}

	/**
	 * Ask the current {@link VirtualView} to activate the cell corresponding to the
	 * specified view.
	 *
	 * @param view the view to activate
	 */
	public void makeViewVisibleInVirtualWorld(BaseView view) {
		if ((virtualView != null) && (virtualView != view)) {
			virtualView.activateViewCell(view);
		}
	}

	/**
	 * Add a view lifecycle listener.
	 *
	 * @param listener listener to add
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
	 * @param listener listener to remove
	 */
	public void removeViewListener(IViewListener listener) {
		if ((listener == null) || (listenerList == null)) {
			return;
		}
		listenerList.remove(IViewListener.class, listener);
	}

	/**
	 * Add a view configuration. If the configuration is eager, the view is created
	 * immediately. If it is lazy, a placeholder menu entry is inserted and the view
	 * is created only when selected.
	 *
	 * @param config the configuration to add
	 */
	public void addConfiguration(ViewConfiguration<?> config) {
		if (config == null) {
			return;
		}

		configs.add(config);

		if (config.lazily) {
			runOnEdt(() -> addLazyMenuEntry(config));
		} else {
			config.getView();
		}
	}

	/**
	 * Notify listeners that a view was added or removed.
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
				} catch (Exception e) {
					Log.getInstance().warning("ViewManager: listener exception: " + e.getMessage());
				}
			}
		}
	}

	/**
	 * Ensure a normal menu item exists for the given realized view.
	 *
	 * @param view the view whose menu item should exist
	 */
	private void ensureMenuItem(BaseView view) {
		if ((viewMenu == null) || menuItems.containsKey(view)) {
			return;
		}

		JMenuItem mi = new JMenuItem(view.getTitle());

		ActionListener al = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
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
			}
		};

		mi.addActionListener(al);
		viewMenu.add(mi);
		menuItems.put(view, mi);
	}

	/**
	 * Remove the menu item for a realized view.
	 *
	 * @param view the view whose menu item should be removed
	 */
	private void removeMenuItem(BaseView view) {
		JMenuItem mi = menuItems.remove(view);
		if ((viewMenu != null) && (mi != null)) {
			viewMenu.remove(mi);
			viewMenu.revalidate();
			viewMenu.repaint();
		}
	}

	/**
	 * Rebuild the entire Views menu from realized views and unrealized lazy
	 * configurations.
	 */
	private void rebuildViewMenu() {
		menuItems.clear();

		if (viewMenu == null) {
			return;
		}

		viewMenu.removeAll();

		for (BaseView v : this) {
			ensureMenuItem(v);
		}

		for (ViewConfiguration<?> config : configs) {
			if (config.lazily && !config.isRealized()) {
				addLazyMenuEntry(config);
			}
		}

		viewMenu.revalidate();
		viewMenu.repaint();
	}

	/**
	 * Add a placeholder menu entry for a lazily-created view.
	 *
	 * @param config the lazy configuration
	 */
	private void addLazyMenuEntry(final ViewConfiguration<?> config) {
		if (viewMenu == null) {
			return;
		}

		final JMenuItem mi = new JMenuItem("Create " + config.getMenuTitle());
		mi.setFont(mi.getFont().deriveFont(java.awt.Font.ITALIC));

		mi.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				viewMenu.remove(mi);
				config.getView();
				viewMenu.revalidate();
				viewMenu.repaint();
			}
		});

		viewMenu.add(mi);
		config.menuIndex = viewMenu.getItemCount() - 1;
	}

	/**
	 * Run an action on the Swing EDT.
	 *
	 * @param r the action
	 */
	private static void runOnEdt(Runnable r) {
		if (SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			SwingUtilities.invokeLater(r);
		}
	}
}