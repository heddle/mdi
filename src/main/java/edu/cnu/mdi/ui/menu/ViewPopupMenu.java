package edu.cnu.mdi.ui.menu;

import javax.swing.JPopupMenu;

import edu.cnu.mdi.view.BaseView;

public class ViewPopupMenu extends JPopupMenu {

	// base view owner
	private BaseView _view;

	/**
	 * The view's popup menu
	 * @param view
	 */
	public ViewPopupMenu(BaseView view) {
		super("Options");
		setLightWeightPopupEnabled(false);
		_view = view;

	}

}
