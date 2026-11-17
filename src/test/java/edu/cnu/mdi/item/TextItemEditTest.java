package edu.cnu.mdi.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.container.BaseContainer;
import edu.cnu.mdi.graphics.world.WorldGraphicsUtils;

/**
 * How a {@link TextItem} box is rebuilt after the edit dialog: a box the user has
 * resized must not snap back to the text's natural size when only the colors
 * changed.
 */
class TextItemEditTest {

	private static <T> T onEdt(Supplier<T> body) {
		Object[] result = new Object[1];
		try {
			SwingUtilities.invokeAndWait(() -> result[0] = body.get());
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException re) throw re;
			if (cause instanceof Error err) throw err;
			throw new AssertionError(cause);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Interrupted while running Swing test", e);
		}
		@SuppressWarnings("unchecked")
		T value = (T) result[0];
		return value;
	}

	private static void onEdt(Runnable body) {
		onEdt(() -> {
			body.run();
			return null;
		});
	}

	private static BaseContainer newContainer() {
		return onEdt(() -> {
			BaseContainer c = new BaseContainer(new Rectangle2D.Double(0, 0, 10, 5));
			c.setBounds(0, 0, 800, 400);
			c.setDirty(true);
			return c;
		});
	}

	private static TextItem text(BaseContainer c, String s) {
		return new TextItem(c.getDefaultLayer(), new Point2D.Double(5, 2.5), new Font("SansSerif", Font.PLAIN, 14), s,
				Color.black, Color.white, Color.blue);
	}

	/** What the edit dialog does to the item before the box is rebuilt. */
	private static int[] natural(TextItem item) {
		Rectangle b = item.getBounds(item.getContainer());
		return new int[] { b.width, b.height };
	}

	/** Scales the box about its centre, as a resize drag does. */
	private static void resize(TextItem item, double factor) {
		Point2D.Double[] poly = WorldGraphicsUtils.pathToWorldPolygon(item.getPath());
		Point2D.Double f = item.getFocus();
		for (Point2D.Double p : poly) {
			p.x = f.x + (p.x - f.x) * factor;
			p.y = f.y + (p.y - f.y) * factor;
		}
		item.setPath(poly);
	}

	private static int[] size(TextItem item) {
		return natural(item);
	}

	@Test
	void aStyleOnlyEditKeepsAResizedBox() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			TextItem item = text(c, "hello");
			int[] naturalBefore = size(item);
			resize(item, 2.0);
			int[] resized = size(item);
			assertTrue(resized[0] > naturalBefore[0] + 20, "the box was enlarged");

			item.getStyleSafe().setFillColor(Color.yellow);
			item.rebuildBoxAfterEdit(resized, naturalBefore, false);

			assertEquals(resized[0], size(item)[0], 1);
			assertEquals(resized[1], size(item)[1], 1);
		});
	}

	@Test
	void aStyleOnlyEditKeepsAShrunkBoxToo() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			TextItem item = text(c, "hello");
			int[] naturalBefore = size(item);
			resize(item, 0.5);
			int[] shrunk = size(item);

			item.rebuildBoxAfterEdit(shrunk, naturalBefore, false);

			assertEquals(shrunk[0], size(item)[0], 1);
			assertEquals(shrunk[1], size(item)[1], 1);
		});
	}

	@Test
	void aBoxThatWasNeverResizedRefitsToChangedText() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			TextItem item = text(c, "hello");
			int[] naturalBefore = size(item);

			item.setText("hello there, this is much longer text");
			item.rebuildBoxAfterEdit(naturalBefore, naturalBefore, true);
			assertTrue(size(item)[0] > naturalBefore[0] + 20, "grew to fit the longer text");

			int[] longer = size(item);
			item.setText("hi");
			item.rebuildBoxAfterEdit(longer, longer, true);
			assertTrue(size(item)[0] < longer[0] - 20, "shrank to fit the shorter text");
		});
	}

	@Test
	void aResizedBoxKeepsItsSizeWhenTheTextChangesAndItStillFits() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			TextItem item = text(c, "hello there");
			int[] naturalBefore = size(item);
			resize(item, 2.0);
			int[] resized = size(item);

			item.setText("hi");
			item.rebuildBoxAfterEdit(resized, naturalBefore, true);

			assertEquals(resized[0], size(item)[0], 1);
			assertEquals(resized[1], size(item)[1], 1);
		});
	}

	@Test
	void aResizedBoxGrowsWhenTheNewTextWouldNotFit() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			TextItem item = text(c, "hi");
			int[] naturalBefore = size(item);
			resize(item, 1.5);
			int[] resized = size(item);

			item.setText("a considerably longer line of text than before");
			item.rebuildBoxAfterEdit(resized, naturalBefore, true);

			assertTrue(size(item)[0] > resized[0], "widened to hold the new text");
		});
	}

	@Test
	void theBoxStaysCenteredOnTheFocus() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			TextItem item = text(c, "hello");
			int[] naturalBefore = size(item);
			resize(item, 2.0);
			Point2D.Double focus = item.getFocus();

			item.rebuildBoxAfterEdit(size(item), naturalBefore, false);

			assertEquals(focus.x, item.getFocus().x, 1e-9);
			assertEquals(focus.y, item.getFocus().y, 1e-9);
		});
	}
}
