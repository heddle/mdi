package edu.cnu.mdi.splot.plot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Rectangle2D;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

/**
 * Regression coverage for {@link PlotCanvas#setWorldSystem()}, the
 * coordinate-transform pipeline's bounds computation — previously entirely
 * untested despite being the mathematical core of the plotting subsystem.
 */
class PlotCanvasWorldSystemTest {

	private static PlotCanvas canvasWith(double[] x, double[] y) throws Exception {
		PlotCanvas[] holder = new PlotCanvas[1];
		SwingUtilities.invokeAndWait(() -> {
			PlotPanel panel = PlotBuilders.xy("test").axes("x", "y").series("s", x, y).build();
			holder[0] = panel.getPlotCanvas();
		});
		return holder[0];
	}

	@Test
	void useDataLimitsGivesTheExactDataBoundsOnALinearAxis() throws Exception {
		PlotCanvas canvas = canvasWith(
				new double[] { 1.3, 2.7, 8.9 },
				new double[] { 5.0, 1.0, 9.0 });

		SwingUtilities.invokeAndWait(() -> {
			canvas.getParameters().setXLimitsMethod(LimitsMethod.USEDATALIMITS);
			canvas.getParameters().setYLimitsMethod(LimitsMethod.USEDATALIMITS);
			canvas.setWorldSystem();
		});

		Rectangle2D.Double world = canvas.getWorld();
		assertEquals(1.3, world.getMinX(), 1.0e-9);
		assertEquals(8.9, world.getMaxX(), 1.0e-9);
		assertEquals(1.0, world.getMinY(), 1.0e-9);
		assertEquals(9.0, world.getMaxY(), 1.0e-9);
	}

	@Test
	void algorithmicLimitsRoundsBoundsOutwardBeyondTheExactData() throws Exception {
		PlotCanvas canvas = canvasWith(
				new double[] { 1.3, 2.7, 8.9 },
				new double[] { 5.0, 1.0, 9.0 });

		SwingUtilities.invokeAndWait(() -> {
			canvas.getParameters().setXLimitsMethod(LimitsMethod.ALGORITHMICLIMITS);
			canvas.getParameters().setYLimitsMethod(LimitsMethod.ALGORITHMICLIMITS);
			canvas.setWorldSystem();
		});

		Rectangle2D.Double world = canvas.getWorld();
		// "Nice" bounds must fully enclose the exact data range, and (for this
		// deliberately non-round data) must not just equal it -- that's the
		// whole point of ALGORITHMICLIMITS vs. USEDATALIMITS.
		assertTrue(world.getMinX() <= 1.3, "nice min must not exceed the data min");
		assertTrue(world.getMaxX() >= 8.9, "nice max must not be less than the data max");
		assertTrue(world.getMinX() < 1.3 || world.getMaxX() > 8.9,
				"nice bounds should round outward from non-round data, not just match it");
	}

	@Test
	void manualLimitsUseTheExplicitRangeRegardlessOfData() throws Exception {
		PlotCanvas canvas = canvasWith(
				new double[] { 1.0, 2.0, 3.0 },
				new double[] { 1.0, 2.0, 3.0 });

		SwingUtilities.invokeAndWait(() -> {
			canvas.getParameters().setXRange(-50.0, 50.0);
			canvas.getParameters().setYRange(-100.0, 100.0);
		});

		Rectangle2D.Double world = canvas.getWorld();
		assertEquals(-50.0, world.getMinX(), 1.0e-9);
		assertEquals(50.0, world.getMaxX(), 1.0e-9);
		assertEquals(-100.0, world.getMinY(), 1.0e-9);
		assertEquals(100.0, world.getMaxY(), 1.0e-9);
	}

	@Test
	void logAxisBoundsIgnoreNonPositiveDataPoints() throws Exception {
		// x includes a non-positive value that must be excluded from the
		// positive-only bounds computed for a log axis.
		PlotCanvas canvas = canvasWith(
				new double[] { -5.0, 2.0, 20.0 },
				new double[] { 1.0, 2.0, 3.0 });

		SwingUtilities.invokeAndWait(() -> {
			canvas.getParameters().setXScale(PlotParameters.AxisScale.LOG10);
			canvas.getParameters().setXLimitsMethod(LimitsMethod.USEDATALIMITS);
			canvas.setWorldSystem();
		});

		// When log is active, the world system stores log10 of the bounds,
		// rounded outward to whole decades: floor(log10(min)) / ceil(log10(max)).
		// If the excluded -5.0 had leaked in, log10 of a negative number is
		// NaN and setWorldSystem() would have fallen back to the 0..1 default
		// frame instead of these decade bounds.
		Rectangle2D.Double world = canvas.getWorld();
		assertEquals(Math.floor(Math.log10(2.0)), world.getMinX(), 1.0e-9,
				"the negative x value must be excluded from the log-axis bounds");
		assertEquals(Math.ceil(Math.log10(20.0)), world.getMaxX(), 1.0e-9);
	}
}
