package edu.cnu.mdi.splot.plot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Color;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.splot.pdata.Curve;

class XYPlotBuilderTest {

    @Test
    void buildsNativeStyledPlotFromDefensiveDataCopies() throws Exception {
        double[] x = { 1.0, 2.0, 3.0 };
        double[] y = { 4.0, 5.0, 6.0 };
        XYPlotBuilder builder = PlotBuilders.xy("Diagnostic").axes("p", "psi")
                .series("wave", x, y, curve -> curve.getStyle().setLineColor(Color.RED))
                .configure(parameters -> parameters.setXScale(PlotParameters.AxisScale.LOG10));
        x[0] = 99.0;
        y[0] = 88.0;

        SwingUtilities.invokeAndWait(() -> {
            PlotPanel first = builder.build();
            PlotPanel second = builder.build();
            Curve curve = (Curve) first.getPlotCanvas().getPlotData().getFirstCurve();
            assertEquals(1.0, curve.xData().get(0));
            assertEquals(4.0, curve.yData().get(0));
            assertEquals(Color.RED, curve.getStyle().getLineColor());
            assertEquals(PlotParameters.AxisScale.LOG10,
                    first.getParameters().getXScale());
            assertNotSame(first.getPlotCanvas().getPlotData(),
                    second.getPlotCanvas().getPlotData());
        });
    }

    @Test
    void validatesSeriesAndRequiredState() {
        assertThrows(IllegalArgumentException.class,
                () -> PlotBuilders.xy("bad").series("curve", new double[1], new double[2]));
        assertThrows(IllegalStateException.class, () -> PlotBuilders.xy("empty").build());
        assertThrows(IllegalArgumentException.class,
                () -> PlotBuilders.xy("bad decorations").decorations(42));
    }
}
