package edu.cnu.mdi.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.EventListenerList;

import edu.cnu.mdi.log.Log;

/**
 * Manages all the views, or internal frames.
 *
 * @author heddle
 *
 */
@SuppressWarnings("serial")
public class ViewManager extends Vector<BaseView> {

	// singleton instance
	private static ViewManager instance;

	// the view menu
	private JMenu _viewMenu;

	// List of view change listeners
	private EventListenerList _listenerList;

	// Virtual view is present
	private VirtualView _virtualView;

	/**
	 * Private constructor used to create singleton.
	 */
	private ViewManager() {
		_viewMenu = new JMenu("Views");
	}

	/**
	 * Make the view visible and change to its viretual panel
	 *
	 * @param view the view
	 * @param vis  whether it is visible
	 */
	public void setVisible(BaseView view, boolean vis) {
		view.setVisible(vis);
		if (vis) {
			if (!(view instanceof VirtualView)) {
				makeViewVisibleInVirtualWorld(view);
			}
		}
	}

	/**
	 * Add (register) a view for control by this manager.
	 *
	 * @param view the View to add.
	 * @return <code>true</code>, per Collection guidelines.
	 */
	@Override
	public boolean add(final BaseView view) {
		boolean result = super.add(view);
		notifyListeners(view, true);

		JMenuItem mi = new JMenuItem(view.getTitle());
		ActionListener al = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (view.isIcon()) {
					try {
						view.setIcon(false);
					} catch (PropertyVetoException e1) {
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

		Log.getInstance().info("ViewManager: added view: " + view.getTitle());

		if (view instanceof VirtualView) {
			_virtualView = (VirtualView) view;
		}
		return result;
	}

	/**
	 * Removes (unregisters) a view.
	 *
	 * @param view the View to remove.
	 * @return <code>true</code> if this ViewManager contained the specified view.
	 */
	public boolean remove(BaseView view) {
		if (view != null) {
			Log.getInstance().info("ViewManager: removed view: " + view.getTitle());

			notifyListeners(view, false);
			boolean removed = super.remove(view);
			view.dispose();
			return removed;
		}
		return false;
	}


	public void makeViewVisibleInVirtualWorld(BaseView view) {
		if ((_virtualView != null) && (_virtualView != view)) {
			_virtualView.activateViewCell(view);
		}
	}

	/**
	 * Obtain the singleton.
	 *
	 * @return the singleton ViewManager object.
	 */
	public static ViewManager getInstance() {
		if (instance == null) {
			instance = new ViewManager();
		}
		return instance;
	}

	/**
	 * Gets the view menu whose state is maintained by the ViewManager.
	 *
	 * @return the view menu.
	 */
	public JMenu getViewMenu() {
		return _viewMenu;
	}

	// notify listeners of a change in the views
	private void notifyListeners(BaseView view, boolean added) {

		if (_listenerList == null) {
			return;
		}

		// Guaranteed to return a non-null array
		Object[] listeners = _listenerList.getListenerList();

		// This weird loop is the bullet proof way of notifying all listeners.
		for (int i = 0; i < listeners.length; i += 2) {
			if (listeners[i] == IViewListener.class) {
				IViewListener listener = (IViewListener) listeners[i + 1];
				if (added) {
					listener.viewAdded(view);
				} else {
					listener.viewRemoved(view);
				}
			}

		}
	}

	/**
	 * Add a data change listener
	 *
	 * @param listener the listener to add
	 */
	public void addViewListener(IViewListener listener) {

		if (_listenerList == null) {
			_listenerList = new EventListenerList();
		}

		// avoid adding duplicates
		_listenerList.remove(IViewListener.class, listener);
		_listenerList.add(IViewListener.class, listener);
	}

	/**
	 * Remove a ViewListener.
	 *
	 * @param listener the listener to remove.
	 */

	public void removeViewListener(IViewListener listener) {

		if ((listener == null) || (_listenerList == null)) {
			return;
		}

		_listenerList.remove(IViewListener.class, listener);
	}

}
