package edu.cnu.mdi.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;

import org.junit.jupiter.api.Test;

class SwingSizingUtilsTest {

    @Test
    void preferredWidthTreatsTheNumberAsABaseline() {
        JLabel narrow = sizedLabel(180, 20);
        JLabel wide = sizedLabel(310, 20);

        assertEquals(310, SwingSizingUtils.preferredWidth(230, narrow, wide));
        assertEquals(230, SwingSizingUtils.preferredWidth(230, narrow));
    }

    @Test
    void preferredSizeCanGrowInEitherDimension() {
        JLabel component = sizedLabel(220, 90);

        assertEquals(new Dimension(220, 100),
                SwingSizingUtils.preferredSizeAtLeast(component, 150, 100));
    }

    @Test
    void iconSizeIncludesMarginsAndMinimums() {
        Icon icon = new FixedIcon(36, 30);
        assertEquals(new Dimension(42, 36), SwingSizingUtils.iconButtonSize(
                icon, new Insets(3, 3, 3, 3), new Dimension(24, 24)));
    }

    @Test
    void availableWidthUsesThePositionInsideAVirtualColumn() {
        JDesktopPane desktop = new JDesktopPane();
        desktop.setSize(1000, 700);
        JInternalFrame frame = new JInternalFrame();
        desktop.add(frame);

        frame.setLocation(6250, 0);
        assertEquals(750, SwingSizingUtils.availableDesktopWidth(frame));

        frame.setLocation(-750, 0);
        assertEquals(750, SwingSizingUtils.availableDesktopWidth(frame));
    }

    private static JLabel sizedLabel(int width, int height) {
        JLabel label = new JLabel();
        label.setPreferredSize(new Dimension(width, height));
        return label;
    }

    private record FixedIcon(int width, int height) implements Icon {
        @Override public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) { }
        @Override public int getIconWidth() { return width; }
        @Override public int getIconHeight() { return height; }
    }
}
