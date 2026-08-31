package edu.cnu.mdi.pseudo3D;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.splot.pdata.Histo2DData;
import edu.cnu.mdi.ui.colors.ScientificColorMap;

class Histogram2DTest {

    @Test
    void noHoveredBinByDefault() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Histogram2D histogram = new Histogram2D(data(3, 3), ScientificColorMap.VIRIDIS);
            assertEquals(-1, histogram.getHoveredI());
            assertEquals(-1, histogram.getHoveredJ());
        });
    }

    @Test
    void setLogZTogglesIsLogZAndIsIdempotentWhenUnchanged() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Histogram2D histogram = new Histogram2D(data(3, 3), ScientificColorMap.VIRIDIS);
            boolean initial = histogram.isLogZ();

            histogram.setLogZ(!initial);
            assertEquals(!initial, histogram.isLogZ());

            // Setting to the same value again must be a safe no-op.
            assertDoesNotThrow(() -> histogram.setLogZ(!initial));
            assertEquals(!initial, histogram.isLogZ());

            histogram.setLogZ(initial);
            assertEquals(initial, histogram.isLogZ());
        });
    }

    @Test
    void changingDataDimensionsRebuildsRenderingBuffers() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Histogram2D histogram = new Histogram2D(data(3, 3), ScientificColorMap.VIRIDIS);
            histogram.setSize(300, 240);
            histogram.setData(data(1, 1));

            BufferedImage image = new BufferedImage(300, 240, BufferedImage.TYPE_INT_ARGB);
            var graphics = image.createGraphics();
            try {
                assertDoesNotThrow(() -> histogram.paint(graphics));
            } finally {
                graphics.dispose();
            }
        });
    }

    private static Histo2DData data(int nx, int ny) {
        Histo2DData data = new Histo2DData("test", 0, 1, nx, 0, 1, ny);
        data.fill(0.5, 0.5);
        return data;
    }
}
