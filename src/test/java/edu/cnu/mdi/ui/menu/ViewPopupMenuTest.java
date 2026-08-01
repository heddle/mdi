package edu.cnu.mdi.ui.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.junit.jupiter.api.Test;

class ViewPopupMenuTest {

    @Test
    void createsOneSeparatedQuickZoomSubmenuLazily() {
        ViewPopupMenu popup = new ViewPopupMenu(null);
        assertEquals(0, popup.getComponentCount());

        JMenuItem global = new JMenuItem("Global");
        JMenuItem theater = new JMenuItem("Theater");
        popup.addQuickZoom(global);
        popup.addQuickZoom(theater);

        assertEquals(2, popup.getComponentCount());
        assertInstanceOf(JPopupMenu.Separator.class, popup.getComponent(0));
        JMenu submenu = assertInstanceOf(JMenu.class, popup.getComponent(1));
        assertEquals("Quick Zoom", submenu.getText());
        assertEquals(2, submenu.getItemCount());
        assertEquals(global, submenu.getItem(0));
        assertEquals(theater, submenu.getItem(1));
    }

    @Test
    void rejectsNullQuickZoomItem() {
        ViewPopupMenu popup = new ViewPopupMenu(null);
        assertThrows(NullPointerException.class, () -> popup.addQuickZoom(null));
        assertEquals(0, popup.getComponentCount());
    }
}
