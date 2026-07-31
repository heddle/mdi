package edu.cnu.mdi.ui.menu;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
