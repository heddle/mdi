package edu.cnu.mdi.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ProgressInfoTest {

    @Test
    public void testDeterminateFractionIsAlwaysInRange() {
        assertEquals(0.0, ProgressInfo.determinate(-1.0, null).fraction);
        assertEquals(1.0, ProgressInfo.determinate(2.0, null).fraction);
        assertEquals(0.0, ProgressInfo.determinate(Double.NaN, null).fraction);
        assertTrue(Double.isFinite(
                ProgressInfo.determinate(Double.POSITIVE_INFINITY, null).fraction));
    }
}
