package edu.cnu.mdi.graphics.toolbar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.Icon;

import org.junit.jupiter.api.Test;

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
