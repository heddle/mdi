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
