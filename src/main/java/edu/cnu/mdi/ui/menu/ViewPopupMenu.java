package edu.cnu.mdi.ui.menu;

import java.util.Objects;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import edu.cnu.mdi.view.BaseView;

@SuppressWarnings("serial")
public class ViewPopupMenu extends JPopupMenu {

	/** Lazily created submenu containing view-specific zoom destinations. */
	private JMenu quickZoomMenu;

    /**
	 * The view's popup menu
	 * @param view
	 */
	public ViewPopupMenu(BaseView view) {
		super("Options");
		setLightWeightPopupEnabled(false);
        // base view owner

    }

	/**
	 * Adds a view-specific zoom destination to the {@code Quick Zoom} submenu.
	 *
	 * <p>The submenu and its preceding separator are created only when the first
	 * destination is registered. This keeps views without quick zooms unchanged
	 * and visually distinguishes navigation shortcuts from ordinary view
	 * commands.</p>
	 *
	 * @param item menu item that performs the zoom; must not be {@code null}
	 */
	public void addQuickZoom(JMenuItem item) {
		Objects.requireNonNull(item, "item");
		if (quickZoomMenu == null) {
			addSeparator();
			quickZoomMenu = new JMenu("Quick Zoom");
			add(quickZoomMenu);
		}
		quickZoomMenu.add(item);
	}

}
