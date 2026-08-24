package edu.cnu.mdi.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JLabel;

import org.junit.jupiter.api.Test;

class MapControlPanelTest {

    @Test
    void sidePanelWidthGrowsWithScaledControls() {
        assertEquals(310, MapView2D.preferredSidePanelWidth(230,
                new java.awt.Dimension(310, 80)));
        assertEquals(230, MapView2D.preferredSidePanelWidth(230,
                new java.awt.Dimension(180, 80)));
    }

    @Test
    void addControlUsesDedicatedOrderedExtensionArea() {
        // No view interaction occurs while controls are registered.
        MapControlPanel panel = new MapControlPanel(null);
        JLabel first = new JLabel("First");
        JLabel second = new JLabel("Second");

        panel.addControl(first);
        panel.addControl(second);

        Container host = first.getParent();
        assertNotSame(panel, host);
        assertSame(host, second.getParent());
        assertEquals(Component.LEFT_ALIGNMENT, first.getAlignmentX());
        assertEquals(4, host.getComponentCount());
        assertSame(first, host.getComponent(1));
        assertSame(second, host.getComponent(3));
    }

    @Test
    void addingSameControlTwiceIsIdempotent() {
        MapControlPanel panel = new MapControlPanel(null);
        JLabel control = new JLabel("Once");

        panel.addControl(control);
        Container host = control.getParent();
        panel.addControl(control);

        assertEquals(2, host.getComponentCount());
        assertSame(control, host.getComponent(1));
    }

    @Test
    void addControlRejectsNull() {
        MapControlPanel panel = new MapControlPanel(null);
        assertThrows(NullPointerException.class, () -> panel.addControl(null));
    }
}
