package edu.cnu.mdi.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

/**
 * Regression coverage for {@link VerticalFlowLayout#minimumLayoutSize(java.awt.Container)}.
 */
class VerticalFlowLayoutTest {

	private static JPanel sizedComponent(Dimension minimum, Dimension preferred) {
		JPanel panel = new JPanel() {
			@Override
			public Dimension getMinimumSize() {
				return minimum;
			}

			@Override
			public Dimension getPreferredSize() {
				return preferred;
			}
		};
		return panel;
	}

	@Test
	void minimumLayoutSizeReflectsComponentMinimumSizesNotTheParentsCurrentSize() {
		VerticalFlowLayout layout = new VerticalFlowLayout();
		layout.setExternalPadLeft(0);
		layout.setExternalPadRight(0);
		layout.setExternalPadTop(0);
		layout.setExternalPadBottom(0);
		layout.setInternalPadX(0);
		layout.setInternalPadY(0);
		layout.setVerticalGap(0);

		JPanel parent = new JPanel(layout);
		// The parent's current on-screen size (0x0, since it's never been
		// shown/packed) must not leak into the computed minimum size.
		parent.setSize(0, 0);

		parent.add(sizedComponent(new Dimension(10, 20), new Dimension(50, 60)));
		parent.add(sizedComponent(new Dimension(15, 25), new Dimension(50, 60)));

		Dimension min = layout.minimumLayoutSize(parent);

		assertEquals(15, min.width, "minimum width should be the widest component's minimum width");
		assertEquals(45, min.height, "minimum height should be the sum of the components' minimum heights");
	}

	@Test
	void minimumLayoutSizeIsNoLargerThanPreferredLayoutSize() {
		VerticalFlowLayout layout = new VerticalFlowLayout();
		JPanel parent = new JPanel(layout);

		parent.add(sizedComponent(new Dimension(10, 20), new Dimension(50, 60)));

		Dimension min = layout.minimumLayoutSize(parent);
		Dimension pref = layout.preferredLayoutSize(parent);

		assertTrue(min.width <= pref.width && min.height <= pref.height,
				"minimum size (" + min + ") should not exceed preferred size (" + pref + ")");
	}
}
