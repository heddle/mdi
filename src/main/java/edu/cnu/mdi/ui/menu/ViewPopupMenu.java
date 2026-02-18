package edu.cnu.mdi.ui.menu;

import javax.swing.JPopupMenu;

import edu.cnu.mdi.view.BaseView;

@SuppressWarnings("serial")
public class ViewPopupMenu extends JPopupMenu {

    /**
	 * The view's popup menu
	 * @param view
	 */
	public ViewPopupMenu(BaseView view) {
		super("Options");
		setLightWeightPopupEnabled(false);
        // base view owner

    }

}
