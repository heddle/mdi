package edu.cnu.mdi.ui.menu;

import java.util.Objects;

import javax.swing.JMenu;

/**
 * A menu registered under a stable ID with a deterministic relative order.
 *
 * <p>Lower order values appear before higher values among contributed menus.
 * Equal values retain registration order. Legacy menus added through
 * {@link MenuManager#addMenu(JMenu)} are not reordered, preserving historical
 * application behavior.</p>
 *
 * @param id stable lookup ID
 * @param menu Swing menu
 * @param order relative order among contributions
 */
public record MenuContribution(MenuId id, JMenu menu, int order) {
    /** Validates required contribution components. */
    public MenuContribution {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(menu, "menu");
        Objects.requireNonNull(menu.getText(), "menu text");
    }

    /**
     * Convenience factory.
     * @param id stable ID text
     * @param menu Swing menu
     * @param order relative order
     * @return validated contribution
     */
    public static MenuContribution of(String id, JMenu menu, int order) {
        return new MenuContribution(new MenuId(id), menu, order);
    }
}
