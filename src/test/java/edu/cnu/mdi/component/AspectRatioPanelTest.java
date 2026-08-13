package edu.cnu.mdi.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Dimension;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

class AspectRatioPanelTest {

	@Test
	void centersSquareContentInWideArea() {
		JPanel content = new JPanel();
		content.setPreferredSize(new Dimension(800, 600));
		AspectRatioPanel host = new AspectRatioPanel(content, 1.0);
		host.setSize(900, 600);
		host.doLayout();
		assertEquals(150, content.getX());
		assertEquals(0, content.getY());
		assertEquals(600, content.getWidth());
		assertEquals(600, content.getHeight());
	}

	@Test
	void centersWideContentInTallArea() {
		JPanel content = new JPanel();
		AspectRatioPanel host = new AspectRatioPanel(content, 2.0);
		host.setSize(500, 400);
		host.doLayout();
		assertEquals(0, content.getX());
		assertEquals(75, content.getY());
		assertEquals(500, content.getWidth());
		assertEquals(250, content.getHeight());
	}

	@Test
	void rejectsInvalidInputs() {
		assertThrows(IllegalArgumentException.class, () -> new AspectRatioPanel(null, 1.0));
		assertThrows(IllegalArgumentException.class,
				() -> new AspectRatioPanel(new JPanel(), 0.0));
	}
}
