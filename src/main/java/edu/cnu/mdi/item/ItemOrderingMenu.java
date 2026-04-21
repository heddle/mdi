package edu.cnu.mdi.item;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * A {@link JMenu} that exposes the four z-order operations for a single
 * {@link AItem}: move to front, move to back, move forward one step, and move
 * backward one step.
 *
 * <h2>Usage</h2>
 * <p>
 * Obtain a configured menu via the factory method
 * {@link #forItem(AItem, boolean)}, which creates a fresh {@link JMenu}
 * instance bound to the given item.  The returned menu can be added to any
 * popup or main menu.
 * </p>
 * <pre>{@code
 * JPopupMenu popup = new JPopupMenu();
 * popup.add(ItemOrderingMenu.forItem(myItem, true));
 * }</pre>
 *
 * <h2>Thread safety</h2>
 * <p>
 * Each menu instance carries its own {@code targetItem} reference rather than
 * sharing a static field.  Multiple views can therefore display ordering menus
 * simultaneously without racing over a shared {@code hotItem}.
 * </p>
 */
@SuppressWarnings("serial")
public class ItemOrderingMenu extends JMenu implements ActionListener {

    // -----------------------------------------------------------------------
    // Z-order action indices (internal)
    // -----------------------------------------------------------------------

    private static final int BRINGTOFRONT = 0;
    private static final int SENDTOBACK   = 1;
    private static final int BRINGFORWARD = 2;
    private static final int SENDBACKWARD = 3;

    /**
     * Unformatted label templates.  The single format argument ({@code {0}})
     * is replaced with the layer name at menu-construction time.
     */
    private static final String[] LABEL_TEMPLATES = {
        "Move to Front of Layer {0}",
        "Move to Back of Layer {0}",
        "Move Forward in Layer {0}",
        "Move Backward in Layer {0}"
    };

    // -----------------------------------------------------------------------
    // Per-instance state (replaces the former static mutable fields)
    // -----------------------------------------------------------------------

    /**
     * The item whose z-order this menu controls.  Set once at construction;
     * never {@code null}.
     */
    private final AItem targetItem;

    /** The four action menu items, in {@link #BRINGTOFRONT}…{@link #SENDBACKWARD} order. */
    private final JMenuItem[] menuItems = new JMenuItem[4];

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    /**
     * Create an ordering menu bound to a specific item.
     *
     * <p>This constructor is private; use the factory method
     * {@link #forItem(AItem, boolean)} to obtain a configured instance.</p>
     *
     * @param item the item whose z-order this menu will control; must not be
     *             {@code null}
     */
    private ItemOrderingMenu(AItem item) {
        super("Item Ordering");
        this.targetItem = java.util.Objects.requireNonNull(item, "item");
        buildMenuItems(item);
    }

    /**
     * Build and add the four z-order {@link JMenuItem}s.
     *
     * @param item the target item (used to get the layer name for labels)
     */
    private void buildMenuItems(AItem item) {
        Layer layer    = item.getLayer();
        String layerName = (layer != null && layer.getName() != null)
                           ? layer.getName() : "?";
        Object[] fmtArgs = { layerName };

        for (int i = 0; i < menuItems.length; i++) {
            menuItems[i] = new JMenuItem(MessageFormat.format(LABEL_TEMPLATES[i], fmtArgs));
            menuItems[i].addActionListener(this);
            add(menuItems[i]);
        }
    }

    // -----------------------------------------------------------------------
    // Factory
    // -----------------------------------------------------------------------

    /**
     * Create and return a fully configured {@link ItemOrderingMenu} for the
     * given item.
     *
     * <p>Each call returns a <em>new</em> menu instance, ensuring that
     * simultaneous menus in different views do not share mutable state.</p>
     *
     * @param item           the item whose z-order the menu will control;
     *                       must not be {@code null}
     * @param insertItemName unused parameter retained for API compatibility
     *                       with the previous implementation; the layer name
     *                       is always embedded in the menu labels
     * @return a new, ready-to-use {@link JMenu}; never {@code null}
     * @throws NullPointerException if {@code item} is {@code null}
     */
    public static JMenu forItem(AItem item, boolean insertItemName) {
        return new ItemOrderingMenu(item);
    }

    /**
     * Convenience overload — equivalent to {@link #forItem(AItem, boolean)
     * forItem(item, true)}.
     *
     * @param item the item whose z-order the menu will control; must not be
     *             {@code null}
     * @return a new, ready-to-use {@link JMenu}
     */
    public static JMenu forItem(AItem item) {
        return new ItemOrderingMenu(item);
    }

    /**
     * Legacy factory method retained for source compatibility.
     *
     * <p><b>Deprecated.</b> The old API stored the target item in a static
     * field, which is unsafe in multi-view MDI applications because a
     * right-click in one view can overwrite the field before the menu action
     * fires in another view. Use {@link #forItem(AItem, boolean)} instead,
     * which creates a dedicated menu instance per invocation.</p>
     *
     * @param item           the item whose z-order the menu will control
     * @param insertItemName unused; see {@link #forItem(AItem, boolean)}
     * @return a new ordering menu
     * @deprecated use {@link #forItem(AItem, boolean)}
     */
    @Deprecated
    public static JMenu getItemOrderingMenu(AItem item, boolean insertItemName) {
        return forItem(item, insertItemName);
    }

    // -----------------------------------------------------------------------
    // ActionListener
    // -----------------------------------------------------------------------

    /**
     * Handle a z-order menu-item selection.
     *
     * <p>The action is applied to {@link #targetItem} using the layer it
     * belongs to at the time of the click. If the layer is {@code null}
     * (e.g. the item was deleted between right-click and menu selection) the
     * method returns silently.</p>
     *
     * @param e the action event from one of the four menu items
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        Layer layer = targetItem.getLayer();
        if (layer == null) return;   // item removed between right-click and selection

        Object src = e.getSource();
        if      (src == menuItems[BRINGTOFRONT]) layer.sendToFront(targetItem);
        else if (src == menuItems[SENDTOBACK])   layer.sendToBack(targetItem);
        else if (src == menuItems[BRINGFORWARD]) layer.sendForward(targetItem);
        else if (src == menuItems[SENDBACKWARD]) layer.sendBackward(targetItem);

        layer.getContainer().refresh();
    }
}