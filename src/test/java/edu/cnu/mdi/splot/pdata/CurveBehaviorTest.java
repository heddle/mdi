package edu.cnu.mdi.splot.pdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.splot.fit.CurveDrawingMethod;

class CurveBehaviorTest {

	@Test
	void stripChartScalesSubMillisecondUnitsWithoutInfinity() throws Exception {
		StripChartCurve[] holder = new StripChartCurve[1];
		SwingUtilities.invokeAndWait(() -> {
			holder[0] = new StripChartCurve("strip", 10, x -> x, 100);
			holder[0].add(2.5, 7.0);
			holder[0].setTimeUnit(TimeUnit.MICROSECONDS);
		});

		assertEquals(2500.0, holder[0].snapshot().x[0]);
		SwingUtilities.invokeAndWait(
				() -> holder[0].setTimeUnit(TimeUnit.NANOSECONDS));
		assertEquals(2_500_000.0, holder[0].snapshot().x[0]);
		holder[0].shutdown();
	}

	@Test
	void failedHistogramFitRemainsDirtyForRetry() throws Exception {
		HistoCurve[] holder = new HistoCurve[1];
		SwingUtilities.invokeAndWait(() -> {
			holder[0] = new HistoCurve("h", new HistoData("h", 0, 1, 10));
			holder[0].setCurveDrawingMethod(CurveDrawingMethod.GAUSSIAN);
			holder[0].doFit(true);
		});

		assertTrue(holder[0].isDirty());

		SwingUtilities.invokeAndWait(() -> {
			holder[0].setCurveDrawingMethod(CurveDrawingMethod.CONNECT);
			holder[0].doFit(true);
		});
		assertFalse(holder[0].isDirty());
	}
}
