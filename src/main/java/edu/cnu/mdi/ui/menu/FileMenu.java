package edu.cnu.mdi.ui.menu;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import edu.cnu.mdi.desktop.Desktop;

/**
 * The global File menu common to all MDI applications.
 *
 * <p>
 * Provides actions for saving/deleting the view configuration and quitting the
 * application. All accelerator key masks avoid deprecated AWT APIs.
 * </p>
 *
 * @author heddle
 */
@SuppressWarnings("serial")
public class FileMenu extends JMenu {

	/** Label used for the File menu. */
	public static final String MENU_LABEL = "File";

	public FileMenu() {
		this(true);
	}

	/**
	 * Creates the File menu.
	 *
	 * @param includeConfigurationItems whether to include save/delete configuration
	 *                                  actions
	 */
	public FileMenu(boolean includeConfigurationItems) {
		super(MENU_LABEL);
		MenuManager.setFileMenu(this);

		if (includeConfigurationItems) {
			addSaveConfigurationItem();
			addDeleteConfigurationItem();
		}
		addSeparator();
		addQuitItem();
	}

	/**
	 * Determine platform-appropriate shortcut key (Command on macOS, Control
	 * elsewhere).
	 */
	private static int shortcutKeyMask() {
		String os = System.getProperty("os.name").toLowerCase();
		return os.contains("mac") ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK;
	}

	/** Add the "Save View Configuration" menu item. */
	private void addSaveConfigurationItem() {
		JMenuItem item = new JMenuItem("Save View Configuration...");
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutKeyMask()));
		item.addActionListener(e -> Desktop.getInstance().writeConfigurationFile());
		add(item);
	}

	/** Add the "Delete View Configuration" menu item. */
	private void addDeleteConfigurationItem() {
		JMenuItem item = new JMenuItem("Delete View Configuration");
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, shortcutKeyMask()));
		item.addActionListener(e -> Desktop.getInstance().deleteConfigurationFile());
		add(item);
	}

	/** Add the "Quit" menu item. */
	private void addQuitItem() {
		JMenuItem item = new JMenuItem("Quit");
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, shortcutKeyMask()));
		item.addActionListener(e -> System.exit(0));
		add(item);
	}
}
