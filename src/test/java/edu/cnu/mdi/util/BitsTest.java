package edu.cnu.mdi.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BitsTest {

    @Test
    void setClearToggleAndCheckMaskWorkTogether() {
        long bits = 0b0010;
        bits = Bits.set(bits, 0b0100);
        assertEquals(0b0110, bits);
        assertTrue(Bits.check(bits, 0b0010));

        bits = Bits.clear(bits, 0b0010);
        assertEquals(0b0100, bits);

        bits = Bits.toggle(bits, 0b1100);
        assertEquals(0b1000, bits);
    }

    @Test
    void indexedOperationsAndQueriesHandleEdges() {
        long bits = 0L;
        bits = Bits.setAt(bits, 0);
        bits = Bits.setAt(bits, 63);

        assertTrue(Bits.checkAt(bits, 0));
        assertTrue(Bits.checkAt(bits, 63));
        assertEquals(0, Bits.firstSet(bits));
        assertEquals(63, Bits.highestSet(bits));
        assertEquals(2, Bits.count(bits));

        bits = Bits.clearAt(bits, 0);
        assertFalse(Bits.checkAt(bits, 0));

        bits = Bits.toggleAt(bits, 63);
        assertTrue(Bits.isEmpty(bits));
    }

    @Test
    void maskHandlesCornerCases() {
        long bits = 0xFFFF_FFFF_FFFF_FFFFL;

        assertEquals(0L, Bits.mask(bits, 0));
        assertEquals(0b1111L, Bits.mask(bits, 4));
        assertEquals(bits, Bits.mask(bits, 64));
        assertEquals(bits, Bits.mask(bits, 100));
        assertEquals(0L, Bits.unsetAll());
    }
}
