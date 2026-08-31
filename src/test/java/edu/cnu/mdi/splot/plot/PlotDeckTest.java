package edu.cnu.mdi.splot.plot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JMenu;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.splot.pdata.PlotData;

class PlotDeckTest {

    @Test
    void suppliesEditMenuForOnePlotAndGalleryOnlyForSeveral() throws Exception {
        AtomicReference<PlotDeck> reference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            PlotDeck deck = new PlotDeck("First", plot());
            reference.set(deck);
            assertEquals(1, deck.getPlotCount());
            assertFalse(menu(deck, "Gallery").isVisible());
            assertTrue(hasVisibleEditMenu(deck));

            deck.add("Second", plot());
            assertEquals(2, deck.getPlotCount());
            assertTrue(menu(deck, "Gallery").isVisible());
        });

        SwingUtilities.invokeAndWait(() -> reference.get().removeAllPlots());
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(0, reference.get().getPlotCount());
            assertFalse(menu(reference.get(), "Gallery").isVisible());
        });
    }

    private static PlotPanel plot() {
        return new PlotPanel(new PlotCanvas(PlotData.emptyData(), "test", "x", "y"));
    }

    private static JMenu menu(PlotDeck deck, String text) {
        for (int index = 0; index < deck.getMenuBar().getMenuCount(); index++) {
            JMenu menu = deck.getMenuBar().getMenu(index);
            if (menu != null && text.equals(menu.getText())) return menu;
        }
        throw new AssertionError("Missing menu " + text);
    }

    private static boolean hasVisibleEditMenu(PlotDeck deck) {
        for (int index = 0; index < deck.getMenuBar().getMenuCount(); index++) {
            JMenu menu = deck.getMenuBar().getMenu(index);
            if (menu != null && menu.isVisible() && menu.getText().contains("Edit")) return true;
        }
        return false;
    }
}
