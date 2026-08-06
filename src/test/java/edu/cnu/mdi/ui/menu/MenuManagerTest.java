package edu.cnu.mdi.ui.menu;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.junit.jupiter.api.Test;

class MenuManagerTest {

    @Test
    void menuRegistryTracksRemovalAndRejectsInvalidMenus() {
        MenuManager manager = MenuManager.createMenuManager(new JMenuBar());
        JMenu menu = new JMenu("Test-support-menu");

        manager.addMenu(menu);
        assertSame(menu, manager.getMenu("Test-support-menu"));
        manager.removeMenu(menu);
        assertNull(manager.getMenu("Test-support-menu"));
        assertThrows(NullPointerException.class, () -> manager.addMenu(null));
    }

    @Test
    void contributionsUseStableIdsAndRelativeOrder() {
        MenuManager manager = MenuManager.createMenuManager(new JMenuBar());
        JMenu late = new JMenu("Late contribution");
        JMenu early = new JMenu("Early contribution");
        MenuId lateId = new MenuId("test.late");
        MenuId earlyId = new MenuId("test.early");

        manager.addContribution(new MenuContribution(lateId, late, 200));
        manager.addContribution(new MenuContribution(earlyId, early, 100));

        assertSame(late, manager.getMenu(lateId));
        assertSame(early, manager.getMenu(earlyId));
        assertSame(early, manager.getMenu("Early contribution"));
        JMenuBar bar = (JMenuBar) early.getParent();
        assertTrue(bar.getComponentZOrder(early) < bar.getComponentZOrder(late));
        assertThrows(IllegalArgumentException.class,
                () -> manager.addContribution(
                        new MenuContribution(earlyId, new JMenu("Duplicate"), 300)));

        manager.removeMenu(early);
        manager.removeMenu(late);
        assertNull(manager.getMenu(earlyId));
        assertNull(manager.getMenu(lateId));
    }
}
