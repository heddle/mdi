package edu.cnu.mdi.ui.menu;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
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

	/** Menus registered through the stable contribution API. */
	private final Hashtable<MenuId, MenuContribution> contributions = new Hashtable<>();

	/** Contribution registration order, used as the tie breaker for equal orders. */
	private final List<MenuContribution> contributionOrder = new ArrayList<>();

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
	 * Add a menu under a stable ID and order it relative to other contributions.
	 *
	 * <p>IDs must be unique within this manager. Registration also populates the
	 * legacy text lookup, so old and new consumers can share the same menu.
	 * Legacy menus already on the bar are never reordered.</p>
	 *
	 * @param contribution contribution to register
	 * @throws NullPointerException if {@code contribution} is null
	 * @throws IllegalArgumentException if its ID is already registered
	 */
	public void addContribution(MenuContribution contribution) {
		Objects.requireNonNull(contribution, "contribution");
		if (contributions.containsKey(contribution.id())) {
			throw new IllegalArgumentException("Duplicate menu ID: " + contribution.id().value());
		}
		int insertionIndex = _menuBar.getMenuCount();
		for (MenuContribution existing : contributionOrder) {
			if (existing.order() > contribution.order()) {
				insertionIndex = _menuBar.getComponentZOrder(existing.menu());
				break;
			}
		}
		_menuBar.add(contribution.menu(), Math.max(0, insertionIndex));
		contributions.put(contribution.id(), contribution);
		int listIndex = 0;
		while (listIndex < contributionOrder.size()
				&& contributionOrder.get(listIndex).order() <= contribution.order()) {
			listIndex++;
		}
		contributionOrder.add(listIndex, contribution);
		_menus.put(contribution.menu().getText(), contribution.menu());
	}

	/**
	 * Gets a contributed menu without depending on its display label.
	 *
	 * @param id stable menu ID
	 * @return registered menu, or {@code null}
	 */
	public JMenu getMenu(MenuId id) {
		MenuContribution contribution = contributions.get(id);
		return contribution == null ? null : contribution.menu();
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
		MenuContribution contribution = contributionOrder.stream()
				.filter(item -> item.menu() == menu).findFirst().orElse(null);
		if (contribution != null) {
			contributionOrder.remove(contribution);
			contributions.remove(contribution.id());
		}
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
