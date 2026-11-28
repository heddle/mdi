package edu.cnu.mdi.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.splot.plot.PlotView;
import edu.cnu.mdi.util.PropertyUtils;

/** Which views offer "Save Image…" in their popup menu. */
class SaveImageMenuTest {

	private static List<String> items(BaseView view) {
		List<String> texts = new ArrayList<>();
		for (Component child : view.getViewPopupMenu().getComponents()) {
			if (child instanceof JMenuItem item) {
				texts.add(item.getText());
			}
		}
		return texts;
	}

	@Test
	void aDrawingViewAddsItAfterUndoAndRedo() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			BaseView view = new BaseView(PropertyUtils.TITLE, "Canvas", PropertyUtils.VISIBLE, false,
					PropertyUtils.WIDTH, 200, PropertyUtils.HEIGHT, 100,
					PropertyUtils.WORLDSYSTEM, new Rectangle2D.Double(0, 0, 2, 1));

			List<String> texts = items(view);

			assertEquals("Save Image…", texts.get(texts.size() - 1));
			assertTrue(texts.size() > 1, "undo and redo are still there");
		});
	}

	@Test
	void aPlotViewOffersOnlyTheImage() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			PlotView view = new PlotView(PropertyUtils.TITLE, "Plot", PropertyUtils.VISIBLE, false);

			assertEquals(List.of("Save Image…"), items(view));
		});
	}
}
