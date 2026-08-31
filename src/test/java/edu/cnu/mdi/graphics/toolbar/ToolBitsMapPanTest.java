package edu.cnu.mdi.graphics.toolbar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ToolBitsMapPanTest {

    @Test
    void mapPanHasDistinctIdentityButReusesPanArtwork() {
        assertNotEquals(ToolBits.PAN, ToolBits.MAPPAN);
        assertEquals("mapPan", ToolBits.getId(ToolBits.MAPPAN));
        assertEquals(ToolBits.getResourcePath(ToolBits.PAN),
                ToolBits.getResourcePath(ToolBits.MAPPAN));
        assertEquals("Pan the map by dragging",
                ToolBits.getToolTip(ToolBits.MAPPAN));
    }

    @Test
    void standardMapToolSetIncludesOnlyProjectionAwarePan() {
        assertTrue((ToolBits.MAPTOOLS & ToolBits.MAPPAN) != 0L);
        assertEquals(0L, ToolBits.MAPTOOLS & ToolBits.PAN);
    }
}
