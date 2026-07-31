package edu.cnu.mdi.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

class SwingGraphicsUtilsTest {

    @Test
    void createsCorrectBufferTypesAndDimensions() {
        JPanel panel = new JPanel();
        panel.setSize(20, 10);

        BufferedImage opaque = SwingGraphicsUtils.createComponentImageBuffer(panel);
        BufferedImage translucent = SwingGraphicsUtils.createComponentTranslucentImageBuffer(panel);

        assertEquals(20, opaque.getWidth());
        assertEquals(10, opaque.getHeight());
        assertEquals(BufferedImage.TYPE_INT_RGB, opaque.getType());
        assertEquals(BufferedImage.TYPE_INT_ARGB, translucent.getType());
    }

    @Test
    void invalidComponentsReturnNullAndPaintingWorks() {
        assertNull(SwingGraphicsUtils.getComponentImage(null));
        assertNull(SwingGraphicsUtils.getComponentImage(new JPanel()));

        JPanel panel = new JPanel();
        panel.setBackground(Color.RED);
        panel.setSize(8, 8);
        BufferedImage image = SwingGraphicsUtils.getComponentImage(panel);
        assertEquals(Color.RED.getRGB(), image.getRGB(4, 4));
    }
}
