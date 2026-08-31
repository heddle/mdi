package edu.cnu.mdi.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Dimension;
import java.awt.Insets;

import org.junit.jupiter.api.Test;

class VirtualViewTest {

    @Test
    void navigatorSizeIncludesActualTitleAndBorderMetrics() {
        Dimension size = VirtualView.navigatorFrameSize(
                320, 23, 34, new Insets(2, 3, 4, 5));

        assertEquals(new Dimension(328, 63), size);
    }

    @Test
    void navigatorAlwaysRetainsANonzeroCanvas() {
        Dimension size = VirtualView.navigatorFrameSize(0, 0, -1, null);

        assertEquals(new Dimension(1, 1), size);
    }
}
