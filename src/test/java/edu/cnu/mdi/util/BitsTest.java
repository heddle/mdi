package edu.cnu.mdi.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Bits}.
 *
 * <p>These tests cover the core bit operations: mask-based checks, setting,
 * clearing, toggling, index-based operations, bit position queries, counting,
 * emptiness checks, and width-based masking.</p>
 */
public class BitsTest {

    /**
     * Verifies that {@link Bits#check(long, long)} returns {@code true} only
     * when all bits in the mask are present in the word.
     */
    @Test
    void check_returnsTrueOnlyWhenAllMaskBitsAreSet() {
        long bits = 0b10110L; // 22 decimal

        assertTrue(Bits.check(bits, 0b00010L), "Bit 1 should be set");
        assertTrue(Bits.check(bits, 0b00100L), "Bit 2 should be set");
        assertTrue(Bits.check(bits, 0b10110L), "All bits of mask should be set");

        assertFalse(Bits.check(bits, 0b01000L), "Bit 3 should not be set");
        assertFalse(Bits.check(bits, 0b11110L), "Mask requiring additional bits should fail");
    }

    /**
     * Tests {@link Bits#set(long, long)} to ensure the specified bits are set.
     */
    @Test
    void set_setsMaskBits() {
        long bits = 0b0001L;
        long result = Bits.set(bits, 0b0110L);

        assertEquals(0b0111L, result, "Bits 1 and 2 should be added to existing bit 0");
    }

    /**
     * Tests {@link Bits#clear(long, long)} to ensure the specified bits are cleared.
     */
    @Test
    void clear_clearsMaskBits() {
        long bits = 0b1111L;
        long result = Bits.clear(bits, 0b0110L);

        assertEquals(0b1001L, result, "Bits 1 and 2 should be cleared");
    }

    /**
     * Tests {@link Bits#toggle(long, long)} to ensure the specified bits are flipped.
     */
    @Test
    void toggle_flipsMaskBits() {
        long bits = 0b1010L;
        long result = Bits.toggle(bits, 0b0110L); // flip bits 1 and 2

        assertEquals(0b1100L, result, "Bits 1 and 2 should be toggled");
    }

    /**
     * Verifies that {@link Bits#setAt(long, int)} sets a single bit at the
     * specified index.
     */
    @Test
    void setAt_setsBitAtIndex() {
        long bits = 0L;
        long result = Bits.setAt(bits, 3);

        assertEquals(0b1000L, result, "Bit 3 should be set");
        assertTrue(Bits.checkAt(result, 3), "checkAt should confirm bit 3 is set");
    }

    /**
     * Verifies that {@link Bits#clearAt(long, int)} clears a single bit at the
     * specified index.
     */
    @Test
    void clearAt_clearsBitAtIndex() {
        long bits = 0b1111L;
        long result = Bits.clearAt(bits, 2);

        assertEquals(0b1011L, result, "Bit 2 should be cleared");
        assertFalse(Bits.checkAt(result, 2), "checkAt should confirm bit 2 is cleared");
    }

    /**
     * Verifies that {@link Bits#toggleAt(long, int)} flips the bit at the given index.
     */
    @Test
    void toggleAt_flipsBitAtIndex() {
        long bits = 0b0100L;
        long result = Bits.toggleAt(bits, 2);

        assertEquals(0b0000L, result, "Bit 2 should be toggled off");
        result = Bits.toggleAt(result, 2);
        assertEquals(0b0100L, result, "Bit 2 should be toggled back on");
    }

    /**
     * Tests {@link Bits#checkAt(long, int)} for both set and unset bits.
     */
    @Test
    void checkAt_reportsSingleBitCorrectly() {
        long bits = 0b1001L;

        assertTrue(Bits.checkAt(bits, 0), "Bit 0 should be set");
        assertFalse(Bits.checkAt(bits, 1), "Bit 1 should not be set");
        assertFalse(Bits.checkAt(bits, 2), "Bit 2 should not be set");
        assertTrue(Bits.checkAt(bits, 3), "Bit 3 should be set");
    }

    /**
     * Verifies that {@link Bits#firstSet(long)} returns the index of the least
     * significant set bit, or -1 if no bits are set.
     */
    @Test
    void firstSet_returnsIndexOfLowestSetBitOrMinusOne() {
        assertEquals(-1, Bits.firstSet(0L), "Zero should yield -1");

        assertEquals(0, Bits.firstSet(0b0001L), "LSB set at position 0");
        assertEquals(1, Bits.firstSet(0b0010L), "Single bit at position 1");
        assertEquals(2, Bits.firstSet(0b0100L | 0b1000L), "Lowest of bits 2 and 3 is 2");
    }

    /**
     * Verifies that {@link Bits#highestSet(long)} returns the index of the most
     * significant set bit, or -1 if no bits are set.
     */
    @Test
    void highestSet_returnsIndexOfHighestSetBitOrMinusOne() {
        assertEquals(-1, Bits.highestSet(0L), "Zero should yield -1");

        assertEquals(0, Bits.highestSet(0b0001L), "Highest bit is 0");
        assertEquals(3, Bits.highestSet(0b1000L), "Highest bit is 3");
        assertEquals(5, Bits.highestSet(0b0010_0000L | 0b0000_0100L), "Highest of bits 2 and 5 is 5");
    }

    /**
     * Tests {@link Bits#count(long)} for various values, including none, one,
     * and multiple bits set.
     */
    @Test
    void count_returnsNumberOfSetBits() {
        assertEquals(0, Bits.count(0L), "Zero should have zero set bits");
        assertEquals(1, Bits.count(0b1000L), "One bit set");
        assertEquals(4, Bits.count(0b1010_0011L), "Four bits set");
        assertEquals(64, Bits.count(~0L), "All bits set in ~0L");
    }

    /**
     * Verifies that {@link Bits#isEmpty(long)} returns true only when the value
     * is zero.
     */
    @Test
    void isEmpty_detectsZeroValueCorrectly() {
        assertTrue(Bits.isEmpty(0L), "Zero should be empty");
        assertFalse(Bits.isEmpty(1L), "Non-zero should not be empty");
        assertFalse(Bits.isEmpty(0b1000L), "Non-zero should not be empty");
    }

    /**
     * Verifies that {@link Bits#unsetAll()} returns zero.
     */
    @Test
    void unsetAll_returnsZero() {
        assertEquals(0L, Bits.unsetAll(), "unsetAll should always return zero");
    }

    /**
     * Tests {@link Bits#mask(long, int)} for various width values, including
     * widths less than zero, between zero and 64, and widths greater than or
     * equal to 64.
     */
    @Test
    void mask_limitsBitsToRequestedWidth() {
        long bits = 0b1111_1111L; // 8 bits set

        // width <= 0 should return zero
        assertEquals(0L, Bits.mask(bits, 0), "Width 0 should yield 0");
        assertEquals(0L, Bits.mask(bits, -5), "Negative width should yield 0");

        // width between 1 and 64 retains low-order bits
        assertEquals(0b0000_1111L, Bits.mask(bits, 4), "Width 4 should keep only 4 LSBs");
        assertEquals(0b1111_1111L, Bits.mask(bits, 8), "Width 8 should keep all original 8 bits");

        // width >= 64 returns bits unchanged
        assertEquals(bits, Bits.mask(bits, 64), "Width 64 should keep all bits");
        assertEquals(bits, Bits.mask(bits, 100), "Width above 64 should keep all bits");
    }

    /**
     * Tests combined behavior: sets several bits, then uses firstSet, highestSet,
     * and count to verify a plausible internal state.
     */
    @Test
    void combinedOperations_yieldConsistentState() {
        long bits = 0L;
        bits = Bits.setAt(bits, 0);
        bits = Bits.setAt(bits, 5);
        bits = Bits.setAt(bits, 10);

        assertTrue(Bits.checkAt(bits, 0), "Bit 0 should be set");
        assertTrue(Bits.checkAt(bits, 5), "Bit 5 should be set");
        assertTrue(Bits.checkAt(bits, 10), "Bit 10 should be set");

        assertEquals(0, Bits.firstSet(bits), "Lowest set bit should be 0");
        assertEquals(10, Bits.highestSet(bits), "Highest set bit should be 10");
        assertEquals(3, Bits.count(bits), "Exactly three bits should be set");

        bits = Bits.clearAt(bits, 0);
        assertEquals(5, Bits.firstSet(bits), "After clearing bit 0, lowest should be 5");
        assertEquals(2, Bits.count(bits), "Two bits should remain set");
    }
}
