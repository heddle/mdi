package edu.cnu.mdi.ui.menu;

import java.awt.Color;
import java.util.Hashtable;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

public class MenuManager {

	// Singleton object
	private static MenuManager instance;

	// file menu
	private static JMenu _fileMenu;

	/**
	 * The BaseMDIApplication being managed.
	 */
	private JMenuBar _menuBar;

	// keep track of the menus added
	private Hashtable<String, JMenu> _menus = new Hashtable<>(41);

	/**
	 * private constructor for singleton.
	 *
	 * @param menuBar the main menubar
	 */
	private MenuManager(JMenuBar menuBar) {
		_menuBar = menuBar;
	}

	/**
	 * Public access for the singleton.
	 *
	 * @param menubar the main menu bar
	 * @return the menu manager for the one and only BaseMDIApplication.
	 */
	public static MenuManager createMenuManager(JMenuBar menubar) {
		if (instance == null) {
			instance = new MenuManager(menubar);
		}
		return instance;
	}

	/**
	 * This one is used after the menu manager is created. Then you can add menus to
	 * the main frame without a reference to it.
	 *
	 * @return the menu manager for the one and only BaseMDIApplication.
	 */
	public static MenuManager getInstance() {
		return instance;
	}

	/**
	 * Add a menu to the main menu bar.
	 *
	 * @param menu the menu to add.
	 */
	public void addMenu(JMenu menu) {
		if (_menuBar != null) {
			_menuBar.add(menu);
		}
		// put into the menu hash
		_menus.put(menu.getText(), menu);

		// seems to be necessary on some linus platforms
		menu.setForeground(Color.black);
	}

	/**
	 * Get a menu based on its name.
	 *
	 * @param text the name of the menu, e.g., "File".
	 * @return the menu, if it finds it.
	 */
	public JMenu getMenu(String text) {
		return _menus.get(text);
	}

	/**
	 * Get the file menu.
	 *
	 * @return the file menu, if it has been set.
	 */
	public JMenu getFileMenu() {
		if (_fileMenu == null) {
			_fileMenu = getMenu(FileMenu.MENU_LABEL);
		}
		return _fileMenu;
	}

	/**
	 * Remove an unwanted menu from the menu bar
	 *
	 * @param menu the menu to remove
	 */
	public void removeMenu(JMenu menu) {
		_menuBar.remove(menu);
	}

	/**
	 * Set the file menu
	 *
	 * @param menu the file menu
	 */
	public static void setFileMenu(JMenu menu) {
		_fileMenu = menu;
	}



	/**
	 * Singleton objects cannot be cloned, so we override clone to throw a
	 * CloneNotSupportedException.
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

}
