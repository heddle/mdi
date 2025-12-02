package edu.cnu.mdi.ui.menu;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

/**
 * Manages the Swing menu bar for a single application window.
 *
 * This is intentionally lightweight and instance-based:
 * each top-level window has its own MenuManager.
 */
public final class MenuManager {

    private final JMenuBar menuBar;
    // preserve insertion order for consistent menu ordering
    private final Map<String, JMenu> menusById = new LinkedHashMap<>();

    /**
     * Create a MenuManager bound to a specific JMenuBar.
     *
     * @param menuBar the menu bar to manage (must not be null)
     */
    public MenuManager(JMenuBar menuBar) {
        this.menuBar = Objects.requireNonNull(menuBar, "menuBar must not be null");
    }

    /**
     * Register a top-level menu by an ID and display text.
     *
     * @param id       stable ID used for lookups (e.g. "file", "view", "help")
     * @param text     visible label (e.g. "File", "&File", "Fichier")
     * @param position index at which to add the menu, or -1 to append
     * @return the created JMenu
     */
    public JMenu addMenu(String id, String text, int position) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(text, "text must not be null");

        JMenu menu = new JMenu(text);

        if (position < 0 || position > menuBar.getMenuCount()) {
            menuBar.add(menu);
        } else {
            menuBar.add(menu, position);
        }

        menusById.put(id, menu);
        return menu;
    }

    /**
     * Convenience overload: append the menu at the end.
     */
    public JMenu addMenu(String id, String text) {
        return addMenu(id, text, -1);
    }

    /**
     * Look up a registered top-level menu by ID.
     *
     * @param id the menu ID
     * @return the JMenu, or null if not found
     */
    public JMenu getMenu(String id) {
        return menusById.get(id);
    }

    /**
     * Remove a top-level menu by ID.
     *
     * @param id the menu ID to remove
     */
    public void removeMenu(String id) {
        JMenu menu = menusById.remove(id);
        if (menu != null) {
            menuBar.remove(menu);
            menuBar.revalidate();
            menuBar.repaint();
        }
    }

    /**
     * Add a simple menu item.
     *
     * @param menuId     ID of the parent menu
     * @param label      visible text
     * @param mnemonic   optional mnemonic (use '\0' for none)
     * @param accelerator optional accelerator (may be null)
     * @param listener   optional ActionListener
     * @return the created JMenuItem, or null if the menu ID is unknown
     */
    public JMenuItem addMenuItem(
            String menuId,
            String label,
            char mnemonic,
            KeyStroke accelerator,
            ActionListener listener) {

        JMenu menu = menusById.get(menuId);
        if (menu == null) {
            return null; // or throw IllegalArgumentException
        }

        JMenuItem item = new JMenuItem(label);
        if (mnemonic != '\0') {
            item.setMnemonic(mnemonic);
        }
        if (accelerator != null) {
            item.setAccelerator(accelerator);
        }
        if (listener != null) {
            item.addActionListener(listener);
        }
        menu.add(item);
        return item;
    }

    /**
     * Add a checkbox menu item.
     *
     * @param menuId   ID of the parent menu
     * @param label    visible text
     * @param selected initial selection state
     * @param listener optional ItemListener
     * @return the created JCheckBoxMenuItem, or null if the menu ID is unknown
     */
    public JCheckBoxMenuItem addCheckBoxItem(
            String menuId,
            String label,
            boolean selected,
            ItemListener listener) {

        JMenu menu = menusById.get(menuId);
        if (menu == null) {
            return null; // or throw IllegalArgumentException
        }

        JCheckBoxMenuItem item = new JCheckBoxMenuItem(label, selected);
        if (listener != null) {
            item.addItemListener(listener);
        }
        menu.add(item);
        return item;
    }

    /**
     * Access to the underlying JMenuBar.
     */
    public JMenuBar getMenuBar() {
        return menuBar;
    }
}
