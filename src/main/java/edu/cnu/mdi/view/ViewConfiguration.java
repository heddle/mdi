package edu.cnu.mdi.view;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

/**
 * Holds the configuration needed to register a view with the
 * {@link ViewManager}, including optional lazy creation.
 * <p>
 * This class replaces the older reflection-based approach in which a view class
 * had to provide a static {@code construct(Object...)} method. Instead, the
 * configuration carries a {@link ViewFactory} that knows how to create the
 * view.
 * </p>
 *
 * @param <T> the concrete {@link BaseView} subtype
 */
public class ViewConfiguration<T extends BaseView> {

	/** Factory used to create the view instance. */
	private final ViewFactory<T> factory;

	/** Menu title used before the view is realized. */
	private final String menuTitle;

	/** If true, the view is created only when first requested. */
	public final boolean lazily;

	/** The virtual column in which the view should be placed. */
	public final int column;

	/** Horizontal offset from the placement constraint. */
	public final int dh;

	/** Vertical offset from the placement constraint. */
	public final int dv;

	/** Placement constraint such as {@link VirtualView#CENTER}. */
	public final int constraint;

	/** The realized view instance, or {@code null} until created. */
	private T view;

	/**
	 * Index of the lazy menu item in the Views menu when initially inserted.
	 */
	public int menuIndex = -1;

	/**
	 * Create a new view configuration.
	 *
	 * @param menuTitle  the title shown in the Views menu before realization
	 * @param factory    factory used to create the view instance
	 * @param lazily     if {@code true}, creation is deferred until first use
	 * @param column     target virtual desktop column
	 * @param dh         horizontal offset from the placement constraint
	 * @param dv         vertical offset from the placement constraint
	 * @param constraint placement constraint such as
	 *                   {@link VirtualView#CENTER}
	 */
	public ViewConfiguration(String menuTitle, ViewFactory<T> factory, boolean lazily, int column, int dh, int dv,
			int constraint) {
		if (menuTitle == null || menuTitle.isBlank()) {
			throw new IllegalArgumentException("menuTitle must not be null or blank.");
		}
		if (factory == null) {
			throw new IllegalArgumentException("factory must not be null.");
		}

		this.menuTitle = menuTitle;
		this.factory = factory;
		this.lazily = lazily;
		this.column = column;
		this.dh = dh;
		this.dv = dv;
		this.constraint = constraint;
	}

	/**
	 * Convenience factory for a lazily-created view configuration.
	 *
	 * @param <T>        the concrete view type
	 * @param menuTitle  the menu title
	 * @param factory    the view factory
	 * @param column     target virtual column
	 * @param dh         horizontal offset
	 * @param dv         vertical offset
	 * @param constraint placement constraint
	 * @return a lazy view configuration
	 */
	public static <T extends BaseView> ViewConfiguration<T> lazy(String menuTitle, ViewFactory<T> factory, int column,
			int dh, int dv, int constraint) {
		return new ViewConfiguration<>(menuTitle, factory, true, column, dh, dv, constraint);
	}

	/**
	 * Convenience factory for an eagerly-created view configuration.
	 *
	 * @param <T>        the concrete view type
	 * @param menuTitle  the menu title
	 * @param factory    the view factory
	 * @param column     target virtual column
	 * @param dh         horizontal offset
	 * @param dv         vertical offset
	 * @param constraint placement constraint
	 * @return an eager view configuration
	 */
	public static <T extends BaseView> ViewConfiguration<T> eager(String menuTitle, ViewFactory<T> factory, int column,
			int dh, int dv, int constraint) {
		return new ViewConfiguration<>(menuTitle, factory, false, column, dh, dv, constraint);
	}

	/**
	 * Get the menu title associated with this configuration.
	 *
	 * @return the menu title
	 */
	public String getMenuTitle() {
		return menuTitle;
	}

	/**
	 * Get whether this configuration has already realized its view.
	 *
	 * @return {@code true} if the view has been created
	 */
	public boolean isRealized() {
		return view != null;
	}

	/**
	 * Get the view instance, creating it if necessary.
	 *
	 * @return the realized view instance
	 */
	public T getView() {
		if (view == null) {
			realizeView();
		}
		return view;
	}

	/**
	 * Create the view, reposition the menu item if necessary, and place the view on
	 * the virtual desktop.
	 */
	private void realizeView() {
	    if (view != null) {
	        return;
	    }

	    view = factory.create();

	    if (view == null) {
	        throw new IllegalStateException("ViewFactory returned null for menu title: " + menuTitle);
	    }

	    repositionCreatedViewMenuItem();

	    // Schedule placement after setVisible(true) has been processed.
	    // BaseView queues setVisible via invokeLater; we must land after that.
	    SwingUtilities.invokeLater(() -> {
	        placeViewOnVirtualDesktop();
	        view.onFirstRealize();
	    });
	}
	/**
	 * Reposition the normal menu item that was automatically added when the
	 * {@link BaseView} was constructed, placing it where the lazy placeholder menu
	 * item originally appeared.
	 */
	private void repositionCreatedViewMenuItem() {
		if (menuIndex < 0) {
			return;
		}

		JMenu viewMenu = ViewManager.getInstance().getViewMenu();
		if (viewMenu == null) {
			return;
		}

		int itemCount = viewMenu.getItemCount();
		if (itemCount < 1) {
			return;
		}

		int lastIndex = itemCount - 1;
		JMenuItem createdItem = viewMenu.getItem(lastIndex);

		if (createdItem == null) {
			return;
		}

		viewMenu.remove(lastIndex);

		int insertIndex = Math.max(0, Math.min(menuIndex, viewMenu.getItemCount()));
		viewMenu.insert(createdItem, insertIndex);
		viewMenu.revalidate();
		viewMenu.repaint();
	}

	/**
	 * Place the realized view on the virtual desktop.
	 */
	private void placeViewOnVirtualDesktop() {
		VirtualView vv = VirtualView.getInstance();
		if (vv != null) {
			vv.gotoColumn(column);
			vv.moveTo(view, column, dh, dv, constraint);
		}
	}
}