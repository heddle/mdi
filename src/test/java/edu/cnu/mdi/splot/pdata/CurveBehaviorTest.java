package edu.cnu.mdi.splot.pdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.splot.fit.CurveDrawingMethod;

class CurveBehaviorTest {

	/**
	 * Parks the event dispatch thread until the returned latch is released, so
	 * work queued meanwhile (such as a curve's deferred drain) cannot run.
	 * EDT events are processed in order, so anything queued after this call
	 * waits behind the parked task.
	 */
	private static CountDownLatch holdEdt() {
		CountDownLatch release = new CountDownLatch(1);
		SwingUtilities.invokeLater(() -> {
			try {
				release.await(10, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		return release;
	}

	/** Returns once every event queued on the EDT before this call has run. */
	private static void drainEdt() throws Exception {
		SwingUtilities.invokeAndWait(() -> { });
	}

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

	@Test
	void curveAddOffEdtIsDeferredAndCoalescedRatherThanAppliedImmediately() throws Exception {
		Curve[] holder = new Curve[1];
		SwingUtilities.invokeAndWait(() -> {
			try {
				holder[0] = new Curve("c", new DataColumn(), new DataColumn(), null);
			} catch (PlotDataException e) {
				throw new AssertionError(e);
			}
		});
		Curve curve = holder[0];

		// Off-EDT: must not throw, and must not apply synchronously. The EDT is
		// held so the queued drain cannot run before the assertion; without
		// this, an idle EDT could drain between add() and length().
		CountDownLatch release = holdEdt();
		try {
			assertDoesNotThrow(() -> curve.add(1.0, 2.0));
			assertEquals(0, curve.length(), "an off-EDT add must be deferred, not applied immediately");
		} finally {
			release.countDown();
		}

		// Drive the EDT queue until the deferred drain has run.
		drainEdt();

		assertEquals(1, curve.length());
		assertEquals(1.0, curve.xData().get(0));
		assertEquals(2.0, curve.yData().get(0));
	}

	@Test
	void curveAddAllOffEdtIsDeferredAndAppliesAllPointsInOneDrain() throws Exception {
		Curve[] holder = new Curve[1];
		SwingUtilities.invokeAndWait(() -> {
			try {
				holder[0] = new Curve("c", new DataColumn(), new DataColumn(), null);
			} catch (PlotDataException e) {
				throw new AssertionError(e);
			}
		});
		Curve curve = holder[0];

		// Hold the EDT so the queued drain cannot run before the assertion.
		CountDownLatch release = holdEdt();
		try {
			curve.addAll(new double[] { 1.0, 2.0, 3.0 }, new double[] { 4.0, 5.0, 6.0 });
			assertEquals(0, curve.length());
		} finally {
			release.countDown();
		}

		drainEdt();

		assertEquals(3, curve.length());
		assertEquals(3.0, curve.xData().get(2));
		assertEquals(6.0, curve.yData().get(2));
	}

	@Test
	void curveClearDataOffEdtDiscardsPendingPointsAndDefers() throws Exception {
		Curve[] holder = new Curve[1];
		SwingUtilities.invokeAndWait(() -> {
			try {
				holder[0] = new Curve("c", new DataColumn(), new DataColumn(), null);
				holder[0].add(1.0, 1.0);
			} catch (PlotDataException e) {
				throw new AssertionError(e);
			}
		});
		Curve curve = holder[0];
		assertEquals(1, curve.length());

		// Enqueue a point, then immediately clear -- the enqueued point must
		// never appear, even after the drain runs.
		curve.add(2.0, 2.0);
		curve.clearData();

		CountDownLatch latch = new CountDownLatch(1);
		SwingUtilities.invokeAndWait(() -> SwingUtilities.invokeLater(latch::countDown));
		latch.await(5, TimeUnit.SECONDS);

		assertEquals(0, curve.length(),
				"clearData() must discard both existing and not-yet-applied pending points");
	}

	@Test
	void histoCurveClearDataOffEdtDefersRatherThanThrows() throws Exception {
		HistoCurve[] holder = new HistoCurve[1];
		SwingUtilities.invokeAndWait(() -> {
			holder[0] = new HistoCurve("h", new HistoData("h", 0, 1, 10));
			holder[0].add(0.5);
			holder[0].add(0.6);
		});
		assertEquals(2L, holder[0].getHistoData().getTotalCount());

		// ACurve#clearData() documents that off-EDT callers are safe: the clear
		// is deferred to the EDT rather than throwing. HistoCurve must honor
		// that contract like every other ACurve implementation.
		assertDoesNotThrow(() -> holder[0].clearData());

		// The clear is deferred, so drive the EDT queue until it lands.
		CountDownLatch latch = new CountDownLatch(1);
		SwingUtilities.invokeAndWait(() -> SwingUtilities.invokeLater(latch::countDown));
		latch.await(5, TimeUnit.SECONDS);

		long[] totalCount = new long[1];
		SwingUtilities.invokeAndWait(() -> totalCount[0] = holder[0].getHistoData().getTotalCount());
		assertEquals(0L, totalCount[0]);
	}
}
