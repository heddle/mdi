package edu.cnu.mdi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class PrintUtilsTest {

	@Test
	void validatesComponentAndPrintsOnlyTheFirstPage() throws Exception {
		assertThrows(NullPointerException.class, () -> new PrintUtils(null));
		boolean[] renderedOnEdt = new boolean[1];
		JPanel panel = new JPanel() {
			@Override
			public void printAll(Graphics graphics) {
				renderedOnEdt[0] = SwingUtilities.isEventDispatchThread();
				super.printAll(graphics);
			}
		};
		panel.setSize(200, 100);
		PrintUtils printable = new PrintUtils(panel);
		BufferedImage page = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = page.createGraphics();
		try {
			Paper paper = new Paper();
			paper.setSize(500, 500);
			paper.setImageableArea(20, 20, 460, 460);
			PageFormat format = new PageFormat();
			format.setPaper(paper);
			assertEquals(Printable.PAGE_EXISTS, printable.print(graphics, format, 0));
			assertEquals(Printable.NO_SUCH_PAGE, printable.print(graphics, format, 1));
			assertEquals(true, renderedOnEdt[0]);
		} finally {
			graphics.dispose();
		}
	}
}
