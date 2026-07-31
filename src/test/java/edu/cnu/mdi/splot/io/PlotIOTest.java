package edu.cnu.mdi.splot.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.cnu.mdi.splot.fit.CurveDrawingMethod;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataType;
import edu.cnu.mdi.splot.plot.PlotCanvas;

class PlotIOTest {

	@TempDir
	Path tempDirectory;

	@Test
	void xyPlotRoundTripsThroughUtf8Json() throws Exception {
		PlotCanvas[] source = new PlotCanvas[1];
		SwingUtilities.invokeAndWait(() -> {
			try {
				PlotData data = new PlotData(PlotDataType.XYEXYE,
						new String[] { "μ signal" }, new int[] { 3 });
				Curve curve = (Curve) data.getFirstCurve();
				curve.addAll(new double[] { 1, 2 }, new double[] { 3, 4 },
						new double[] { 0.1, 0.2 });
				curve.setCurveDrawingMethod(CurveDrawingMethod.POLYNOMIAL);
				source[0] = new PlotCanvas(data, "Δ plot", "x", "y");
			} catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		});

		Path file = tempDirectory.resolve("plot.json");
		PlotIO.save(source[0], file.toFile());
		PlotCanvas restored = PlotIO.loadCanvas(file.toFile());
		Curve restoredCurve = (Curve) restored.getPlotData().getFirstCurve();

		assertEquals("Δ plot", restored.getParameters().getPlotTitle());
		assertEquals("μ signal", restoredCurve.name());
		assertEquals(3, restoredCurve.getFitOrder());
		assertEquals(CurveDrawingMethod.POLYNOMIAL,
				restoredCurve.getCurveDrawingMethod());
		assertArrayEquals(new double[] { 1, 2 }, restoredCurve.snapshot().x);
		assertArrayEquals(new double[] { 3, 4 }, restoredCurve.snapshot().y);
		assertArrayEquals(new double[] { 0.1, 0.2 }, restoredCurve.snapshot().e);
	}

	@Test
	void emptyOrNullJsonIsRejected() throws Exception {
		Path empty = tempDirectory.resolve("empty.json");
		Files.writeString(empty, "null");
		assertThrows(java.io.IOException.class,
				() -> PlotIO.loadSpec(empty.toFile()));
	}
}
