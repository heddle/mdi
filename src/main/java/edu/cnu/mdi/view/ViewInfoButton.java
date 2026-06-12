package edu.cnu.mdi.view;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;

import edu.cnu.mdi.graphics.ImageManager;
import edu.cnu.mdi.graphics.toolbar.BaseToolBar;
import edu.cnu.mdi.graphics.toolbar.ToolBits;

/**
 * Small button that displays information about a view.
 *
 * <p>
 * This button is useful for views that do not have a toolbar, or for views
 * whose toolbar does not include the standard information tool. The action is
 * deliberately simple: clicking the button delegates to
 * {@link BaseView#viewInfo()}.
 * </p>
 */
@SuppressWarnings("serial")
public class ViewInfoButton extends JButton {

    /** Icon displayed in the view information button. */
    protected static final Icon infoIcon;

    static {
        String path = ToolBits.getResourcePath(ToolBits.INFO);
        infoIcon = ImageManager.getInstance().loadUiIcon(
                path,
                BaseToolBar.DEFAULT_ICON_SIZE,
                BaseToolBar.DEFAULT_ICON_SIZE);
    }

    /**
     * Construct a button that shows information for the supplied view.
     *
     * @param view the view whose information dialog should be displayed
     */
    public ViewInfoButton(BaseView view) {
        setIcon(infoIcon);
        setToolTipText("View information");
        setFocusable(false);
        setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
        addActionListener(e -> view.viewInfo());
    }
}