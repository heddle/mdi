package edu.cnu.mdi.ui.menu;

import java.util.Hashtable;
import java.util.Objects;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

/** Singleton registry for the application's menu bar and named menus. */
public final class MenuManager {

	// Singleton object
	private static MenuManager instance;

	// file menu
	private static JMenu _fileMenu;

	/**
	 * The BaseMDIApplication being managed.
	 */
	private final JMenuBar _menuBar;

	// keep track of the menus added
	private final Hashtable<String, JMenu> _menus = new Hashtable<>(41);

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
	public static synchronized MenuManager createMenuManager(JMenuBar menubar) {
		Objects.requireNonNull(menubar, "menubar");
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
		Objects.requireNonNull(menu, "menu");
		Objects.requireNonNull(menu.getText(), "menu text");
		if (_menuBar != null) {
			_menuBar.add(menu);
		}
		// put into the menu hash
		_menus.put(menu.getText(), menu);

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
		if (menu == null) {
			return;
		}
		_menuBar.remove(menu);
		_menus.remove(menu.getText(), menu);
		if (_fileMenu == menu) {
			_fileMenu = null;
		}
	}

	/**
	 * Set the file menu
	 *
	 * @param menu the file menu
	 */
	public static void setFileMenu(JMenu menu) {
		_fileMenu = menu;
	}




}
