package edu.cnu.mdi.app;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DemoAppLayoutTest {

    @Test
    void compactLayoutIsLimitedToSmallKnownDesktopWidths() {
        assertTrue(DemoApp.usesCompactVirtualLayout(1279));
        assertFalse(DemoApp.usesCompactVirtualLayout(1280));
        assertFalse(DemoApp.usesCompactVirtualLayout(0));
    }
}
