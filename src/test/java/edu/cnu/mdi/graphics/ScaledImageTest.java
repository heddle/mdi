package edu.cnu.mdi.graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.container.BaseContainer;
import edu.cnu.mdi.item.RectangleItem;

/** {@link GraphicsUtils#getComponentImage(java.awt.Component, int)}. */
class ScaledImageTest {

	/** Blocks of scale x scale pixels that are not one color: real detail. */
	private static int detailedBlocks(BufferedImage image, int scale) {
		int count = 0;
		for (int y = 0; y + scale <= image.getHeight(); y += scale) {
			for (int x = 0; x + scale <= image.getWidth(); x += scale) {
				boolean mixed = false;
				for (int dy = 0; dy < scale; dy++) {
					for (int dx = 0; dx < scale; dx++) {
						mixed |= image.getRGB(x + dx, y + dy) != image.getRGB(x, y);
					}
				}
				if (mixed) {
					count++;
				}
			}
		}
		return count;
	}

	@SuppressWarnings("serial")
	private static JComponent diagonalLine() {
		JComponent line = new JComponent() {
			@Override
			protected void paintComponent(Graphics g) {
				g.setColor(Color.BLACK);
				g.drawLine(0, 0, 39, 29);
			}
		};
		line.setSize(40, 30);
		return line;
	}

	@Test
	void paintsAgainAtTheLargerSizeInsteadOfEnlargingPixels() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			BufferedImage image = GraphicsUtils.getComponentImage(diagonalLine(), 4);

			assertEquals(160, image.getWidth());
			assertEquals(120, image.getHeight());
			assertTrue(detailedBlocks(image, 4) > 20, "an enlarged copy would have no detail within a block");
			assertEquals(Color.WHITE.getRGB(), image.getRGB(150, 5), "unpainted areas are white");
		});
	}

	@Test
	void drawsContainerItemsAtTheLargerSize() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			BaseContainer container = new BaseContainer(new Rectangle2D.Double(0, 0, 10, 10));
			container.setBounds(0, 0, 100, 100);
			container.setBackground(Color.WHITE);
			RectangleItem item = new RectangleItem(container.getDefaultLayer(),
					new Rectangle2D.Double(2, 2, 6, 6));
			item.getStyle().setFillColor(Color.RED);
			container.setDirty(true);

			BufferedImage image = GraphicsUtils.getComponentImage(container, 3);

			assertEquals(300, image.getWidth());
			assertEquals(Color.RED.getRGB(), image.getRGB(150, 150), "the item is in the middle");
			assertEquals(Color.WHITE.getRGB(), image.getRGB(30, 30), "and does not fill the canvas");
		});
	}

	@Test
	void aScrolledContainerStaysInsideItsViewport() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			BaseContainer container = new BaseContainer(new Rectangle2D.Double(0, 0, 10, 10));
			container.setBackground(Color.RED);
			container.setPreferredSize(new java.awt.Dimension(200, 200));
			JViewport viewport = new JViewport();
			viewport.setView(container);
			JPanel frame = new JPanel(null);
			frame.setBackground(Color.WHITE);
			frame.add(viewport);
			frame.setSize(100, 100);
			viewport.setBounds(0, 0, 50, 50);
			viewport.doLayout();
			container.setDirty(true);

			BufferedImage image = GraphicsUtils.getComponentImage(frame, 1);

			assertEquals(Color.RED.getRGB(), image.getRGB(25, 25));
			assertEquals(Color.WHITE.getRGB(), image.getRGB(75, 75), "outside the viewport");
		});
	}

	@Test
	void leavesDoubleBufferingAsItWas() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JPanel panel = new JPanel();
			panel.setBounds(new Rectangle(0, 0, 10, 10));
			RepaintManager manager = RepaintManager.currentManager(panel);
			boolean before = manager.isDoubleBufferingEnabled();

			GraphicsUtils.getComponentImage(panel, 2);

			assertEquals(before, manager.isDoubleBufferingEnabled());
		});
	}

	@Test
	void rejectsNothingToDrawAndScalesBelowOne() {
		assertNull(GraphicsUtils.getComponentImage(null, 2));
		assertNull(GraphicsUtils.getComponentImage(new JPanel(), 2));
		assertThrows(IllegalArgumentException.class, () -> GraphicsUtils.getComponentImage(new JPanel(), 0));
	}
}
