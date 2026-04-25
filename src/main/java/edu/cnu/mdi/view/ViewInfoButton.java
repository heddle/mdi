package edu.cnu.mdi.view;

import javax.swing.Icon;
import javax.swing.JButton;

import edu.cnu.mdi.graphics.ImageManager;
import edu.cnu.mdi.graphics.toolbar.BaseToolBar;
import edu.cnu.mdi.graphics.toolbar.ToolBits;

/**
 * A button that displays information about the view when clicked.
 * This is provide for views that do not have a toolbar or room for a
 * dedicated info button in their toolbar.
 */

@SuppressWarnings("serial")
public class ViewInfoButton extends JButton {
	
	   /** Icon displayed in the floating info button. */
    protected static final Icon infoIcon;

    static {
        String path = ToolBits.getResourcePath(ToolBits.INFO);
        infoIcon = ImageManager.getInstance().loadUiIcon(
                path,
                BaseToolBar.DEFAULT_ICON_SIZE,
                BaseToolBar.DEFAULT_ICON_SIZE);
    }

    /**
     * Constructs a ViewInfoButton that will call the view's viewInfo() method when clicked.
     * @param view the view whose information will be displayed when the button is clicked
     */
    public ViewInfoButton(BaseView view) {
		setIcon(infoIcon);
		setToolTipText("View information");
		addActionListener(e -> view.viewInfo());
	}

}
