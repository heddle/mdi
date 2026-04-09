package edu.cnu.mdi.view;

import javax.swing.SwingUtilities;

/**
 * Describes everything needed to register a {@link BaseView} with the
 * {@link ViewManager}, including optional deferred (lazy) creation and
 * virtual-desktop placement.
 *
 * <h2>Eager vs. lazy creation</h2>
 * <p>
 * When {@link #lazily} is {@code false} the view is created immediately when
 * {@link ViewManager#addConfiguration(ViewConfiguration)} is called. When
 * {@code true} a placeholder entry appears in the Views menu (rendered in
 * italics to signal "not yet open") and the view is created only when the
 * user selects that item.
 * </p>
 *
 * <h2>Virtual-desktop placement</h2>
 * <p>
 * {@link #column}, {@link #dh}, {@link #dv}, and {@link #constraint} are
 * forwarded to {@link VirtualView#moveTo(BaseView, int, int, int, int)} once
 * the view has been realized and made visible.
 * </p>
 *
 * <h2>Menu-item ordering</h2>
 * <p>
 * Menu-item positioning is now managed entirely by {@link ViewManager}.
 * {@code ViewConfiguration} no longer carries a {@code menuIndex} field;
 * doing so coupled two unrelated concerns and was fragile when other items
 * or separators were inserted between configuration registration and view
 * realization.
 * </p>
 *
 * @param <T> the concrete {@link BaseView} subtype produced by this
 *            configuration
 */
public class ViewConfiguration<T extends BaseView> {

    // -----------------------------------------------------------------------
    // Identity / creation
    // -----------------------------------------------------------------------

    /** Factory invoked to create the view instance. */
    private final ViewFactory<T> factory;

    /**
     * The title shown in the Views menu.
     * <p>
     * For unrealized lazy views this label appears in italics. Once the view
     * is realized the normal (non-italic) menu item uses the view's own title,
     * so the two will typically match.
     * </p>
     */
    private final String menuTitle;

    /**
     * If {@code true} the view is created only when the user first requests it
     * from the Views menu; otherwise it is created immediately.
     */
    public final boolean lazily;

    // -----------------------------------------------------------------------
    // Virtual-desktop placement
    // -----------------------------------------------------------------------

    /** Target virtual desktop column for the initial placement. */
    public final int column;

    /** Additional horizontal pixel offset applied after the constraint. */
    public final int dh;

    /** Additional vertical pixel offset applied after the constraint. */
    public final int dv;

    /**
     * Placement constraint such as {@link VirtualView#CENTER},
     * {@link VirtualView#UPPERLEFT}, etc.
     */
    public final int constraint;

    // -----------------------------------------------------------------------
    // Realized instance
    // -----------------------------------------------------------------------

    /** The realized view, or {@code null} until {@link #getView()} is called. */
    private T view;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    /**
     * Full constructor.
     *
     * @param menuTitle  label shown in the Views menu; must not be blank
     * @param factory    factory that creates the view; must not be {@code null}
     * @param lazily     {@code true} to defer creation until first use
     * @param column     target virtual desktop column
     * @param dh         additional horizontal pixel offset
     * @param dv         additional vertical pixel offset
     * @param constraint placement constraint (e.g. {@link VirtualView#CENTER})
     * @throws IllegalArgumentException if {@code menuTitle} is blank or
     *                                  {@code factory} is {@code null}
     */
    public ViewConfiguration(String menuTitle, ViewFactory<T> factory,
            boolean lazily, int column, int dh, int dv, int constraint) {

        if (menuTitle == null || menuTitle.isBlank()) {
            throw new IllegalArgumentException("menuTitle must not be null or blank.");
        }
        if (factory == null) {
            throw new IllegalArgumentException("factory must not be null.");
        }

        this.menuTitle  = menuTitle;
        this.factory    = factory;
        this.lazily     = lazily;
        this.column     = column;
        this.dh         = dh;
        this.dv         = dv;
        this.constraint = constraint;
    }

    // -----------------------------------------------------------------------
    // Convenience factories
    // -----------------------------------------------------------------------

    /**
     * Convenience factory for a <em>lazily</em>-created view configuration.
     * The view is not constructed until the user selects it from the Views menu.
     *
     * @param <T>        the concrete view type
     * @param menuTitle  the menu label
     * @param factory    the view factory
     * @param column     target virtual column
     * @param dh         additional horizontal pixel offset
     * @param dv         additional vertical pixel offset
     * @param constraint placement constraint
     * @return a new lazy {@code ViewConfiguration}
     */
    public static <T extends BaseView> ViewConfiguration<T> lazy(
            String menuTitle, ViewFactory<T> factory,
            int column, int dh, int dv, int constraint) {
        return new ViewConfiguration<>(menuTitle, factory, true, column, dh, dv, constraint);
    }

    /**
     * Convenience factory for an <em>eagerly</em>-created view configuration.
     * The view is constructed immediately when the configuration is registered.
     *
     * @param <T>        the concrete view type
     * @param menuTitle  the menu label
     * @param factory    the view factory
     * @param column     target virtual column
     * @param dh         additional horizontal pixel offset
     * @param dv         additional vertical pixel offset
     * @param constraint placement constraint
     * @return a new eager {@code ViewConfiguration}
     */
    public static <T extends BaseView> ViewConfiguration<T> eager(
            String menuTitle, ViewFactory<T> factory,
            int column, int dh, int dv, int constraint) {
        return new ViewConfiguration<>(menuTitle, factory, false, column, dh, dv, constraint);
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /**
     * Returns the menu title associated with this configuration.
     *
     * @return the menu title; never {@code null}
     */
    public String getMenuTitle() {
        return menuTitle;
    }

    /**
     * Returns {@code true} if the view has already been created.
     *
     * @return {@code true} if realized
     */
    public boolean isRealized() {
        return view != null;
    }

    /**
     * Returns the view instance, creating it on first call if necessary.
     * <p>
     * After creation, virtual-desktop placement and the
     * {@link BaseView#onFirstRealize()} lifecycle hook are scheduled on the
     * Swing EDT via {@code invokeLater} so that any pending
     * {@code setVisible(true)} call (which {@link BaseView} itself queues) has
     * already been processed before placement is attempted.
     * </p>
     *
     * @return the realized view; never {@code null}
     * @throws IllegalStateException if the factory returns {@code null}
     */
    public T getView() {
        if (view == null) {
            realizeView();
        }
        return view;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Create the view and schedule post-creation work on the EDT.
     * <p>
     * This method is intentionally not {@code synchronized}. It is always
     * called from the EDT (either directly from
     * {@link ViewManager#addConfiguration} or from a menu-item action
     * listener), so no additional locking is needed.
     * </p>
     * <p>
     * <strong>Saved-layout handling:</strong> lazy views are not yet frames
     * when {@link edu.cnu.mdi.desktop.Desktop#configureViews()} runs at
     * startup, so their persisted state is not applied there. Instead, this
     * method checks whether the {@link edu.cnu.mdi.desktop.Desktop} has a
     * loaded configuration covering this view. If it does, that state is
     * restored (with a column-offset correction applied) and the virtual
     * desktop navigates to the view's saved column. If no saved state exists,
     * the default {@link #placeViewOnVirtualDesktop()} placement runs instead.
     * </p>
     */
    private void realizeView() {
        if (view != null) {
            return;
        }

        view = factory.create();

        if (view == null) {
            throw new IllegalStateException(
                    "ViewFactory returned null for menu title: " + menuTitle);
        }

        // Schedule placement after any queued setVisible(true) has been
        // processed. BaseView defers its own setVisible via invokeLater, so
        // we must land after that call.
        SwingUtilities.invokeLater(() -> {
            int targetColumn = applySavedConfiguration();
            if (targetColumn < 0) {
                // No saved state — use the configured default placement.
                placeViewOnVirtualDesktop();
            } else {
                // Saved state was applied; still need to navigate to the
                // view's column so it is immediately visible.
                VirtualView vv = VirtualView.getInstance();
                if (vv != null) {
                    vv.gotoColumn(targetColumn);
                }
            }
            view.onFirstRealize();
        });
    }

    /**
     * Apply any persisted layout state for this view from the
     * {@link edu.cnu.mdi.desktop.Desktop}'s loaded configuration.
     *
     * <p>Returns the virtual-desktop column the view should be navigated to,
     * or {@code -1} if no saved state was found (in which case the caller
     * should fall back to default placement).</p>
     *
     * <p><strong>Column-offset correction:</strong> saved X coordinates are
     * stored normalised to column-0 (see
     * {@link edu.cnu.mdi.desktop.Desktop#collectViewProperties()}). At the
     * moment a lazy view is realized the virtual desktop may be showing
     * column N, meaning all existing views have already been shifted left by
     * {@code N * frameWidth} pixels. The restored view must receive the same
     * shift; otherwise it lands {@code N} columns to the right of its saved
     * position. This method applies that correction immediately after
     * {@link BaseView#setFromProperties} restores the bounds.</p>
     *
     * <p>The probe key is {@code prefix.x}: if that entry is absent the view
     * has no saved record and {@code -1} is returned.</p>
     *
     * @return the column to navigate to (≥ 0), or {@code -1} if no saved
     *         state exists for this view
     */
    private int applySavedConfiguration() {
        java.util.Properties saved =
                edu.cnu.mdi.desktop.Desktop.getInstance().getSavedProperties();
        if (saved == null || saved.isEmpty()) {
            return -1;
        }

        String probeKey = view.getPropertyName() + ".x";
        if (!saved.containsKey(probeKey)) {
            return -1;
        }

        // Restore saved position, size, visibility, and maximized state.
        view.setFromProperties(saved);

        // The saved X is column-0-relative. Shift the view left by the
        // current column offset so it sits at the same physical position as
        // all other views that were already shifted when the desktop navigated
        // away from column 0.
        VirtualView vv = VirtualView.getInstance();
        int colOffset = (vv != null) ? vv.getCurrentColumnPixelOffset() : 0;
        if (colOffset != 0) {
            view.offset(-colOffset, 0);
        }

        // Determine which column the view now occupies so the caller can
        // navigate to it.
        int targetColumn = (vv != null) ? vv.getViewColumn(view) : 0;
        return targetColumn;
    }

    /**
     * Place the realized view on the virtual desktop using this configuration's
     * column and constraint values, then navigate the virtual desktop to that
     * column so the view is immediately visible.
     * <p>
     * This is a no-op if no {@link VirtualView} is currently installed.
     * </p>
     */
    private void placeViewOnVirtualDesktop() {
        VirtualView vv = VirtualView.getInstance();
        if (vv != null) {
            vv.moveTo(view, column, dh, dv, constraint);
            vv.gotoColumn(column);
        }
    }
}