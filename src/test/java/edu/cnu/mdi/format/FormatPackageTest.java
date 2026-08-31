package edu.cnu.mdi.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

class FormatPackageTest {

    @Test
    void dateFormatsHaveTheirDocumentedDetail() {
        assertTrue(DateString.dateStringSS(0).matches(".*\\d{1,2}:\\d{2}:\\d{2} [AP]M"));
        assertTrue(DateString.dateStringShort(0).matches("\\d{1,2}:\\d{2}:\\d{2}"));
    }

    @Test
    void doubleFormattingRejectsInvalidPrecision() {
        assertThrows(IllegalArgumentException.class,
                () -> DoubleFormat.doubleFormat(1.2, -1));
        assertEquals("1.20", DoubleFormat.doubleFormat(1.2, 2));
    }

    @Test
    void thresholdOverloadUsesScientificNotationAtAndAboveTheMinimumExponent() {
        // 500000.0 has order-of-magnitude exponent 5 (log10 truncated).
        assertTrue(DoubleFormat.doubleFormat(500000.0, 2, 6).indexOf('E') < 0,
                "exponent (5) below minExponent (6) must stay fixed-point");
        assertEquals("500000.00", DoubleFormat.doubleFormat(500000.0, 2, 6));

        // At the boundary (exponent == minExponent), scientific notation wins.
        assertTrue(DoubleFormat.doubleFormat(500000.0, 2, 5).indexOf('E') >= 0,
                "exponent (5) at minExponent (5) must switch to scientific notation");
    }

    @Test
    void thresholdOverloadTreatsSmallMagnitudesSymmetrically() {
        // 0.0003 remaps to the same effective exponent (4) as a large value
        // of similar "extremeness", so small values also trigger scientific
        // notation once they cross minExponent.
        assertTrue(DoubleFormat.doubleFormat(0.0003, 2, 5).indexOf('E') < 0,
                "remapped exponent (4) below minExponent (5) must stay fixed-point");
        assertTrue(DoubleFormat.doubleFormat(0.0003, 2, 4).indexOf('E') >= 0,
                "remapped exponent (4) at minExponent (4) must switch to scientific notation");
    }

    @Test
    void cachedFormattersAreSafeAcrossThreads() throws Exception {
        var executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<String>> work = new ArrayList<>();
            for (int i = 0; i < 500; i++) {
                final double value = i + 0.25;
                work.add(() -> DoubleFormat.doubleFormat(value, 2));
            }
            var results = executor.invokeAll(work);
            for (int i = 0; i < results.size(); i++) {
                assertEquals(DoubleFormat.doubleFormat(i + 0.25, 2), results.get(i).get());
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
