package edu.cnu.mdi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SmartDoubleFormatterTest {

    @Test
    void specialValuesAreReturnedAsJavaStrings() {
        assertEquals("NaN", SmartDoubleFormatter.doubleFormat(Double.NaN, 4));
        assertEquals("Infinity", SmartDoubleFormatter.doubleFormat(Double.POSITIVE_INFINITY, 4));
        assertEquals("-Infinity", SmartDoubleFormatter.doubleFormat(Double.NEGATIVE_INFINITY, 4));
    }

    @Test
    void compactNumbersPreferPlainString() {
        assertEquals("123.5", SmartDoubleFormatter.doubleFormat(123.456, 4));
    }

    @Test
    void veryLargeOrSmallValuesUseScientificNotationWhenPlainWouldBeLong() {
        String large = SmartDoubleFormatter.doubleFormat(1.23456789e20, 4);
        String small = SmartDoubleFormatter.doubleFormat(1.23456789e-20, 4);

        assertEquals("1.235E+20", large);
        assertEquals("1.235E-20", small);
    }
}
