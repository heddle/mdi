package edu.cnu.mdi.mapping.milsym;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NatoIconPickerSizingTest {

    @Test
    void measuredContentCanGrowAColumnPastItsBaseline() {
        assertEquals(102, NatoIconPicker.measuredColumnWidth(90, 75));
    }

    @Test
    void compactBaselineIsRetainedForShortContent() {
        assertEquals(36, NatoIconPicker.measuredColumnWidth(10, 36));
    }
}
