package edu.cnu.mdi.graphics.toolbar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.border.EmptyBorder;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.swing.SwingSizingUtils;

class BaseToolBarSizingTest {

    @Test
    void defaultSizeIsUsedWithoutAnIcon() {
        assertEquals(new Dimension(24, 24), BaseToolBar.buttonSizeForIcon(null));
    }

    @Test
    void scaledIconCannotBeCompressed() {
        Icon scaledIcon = new FixedIcon(36, 30);

        assertEquals(new Dimension(42, 36),
                BaseToolBar.buttonSizeForIcon(scaledIcon));
    }

	@Test
	void compactToolbarHeightContainsButtonAndBorderInsets() {
		JToolBar toolbar = new JToolBar();
		toolbar.setBorder(new EmptyBorder(2, 0, 3, 0));
		JButton button = new JButton();
		button.setPreferredSize(new Dimension(24, 24));
		button.setMinimumSize(new Dimension(24, 24));
		toolbar.add(button);

		assertEquals(29, SwingSizingUtils.requiredHorizontalToolbarHeight(toolbar));
	}

    private record FixedIcon(int width, int height) implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }
    }
}
