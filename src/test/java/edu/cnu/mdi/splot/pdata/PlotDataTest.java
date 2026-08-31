package edu.cnu.mdi.splot.pdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class PlotDataTest {

	@Test
	void rejectsNullCurveNames() {
		assertThrows(PlotDataException.class,
				() -> new PlotData(PlotDataType.XYXY, new String[] { null }, null));
	}

	@Test
	void findsCurvesAndForwardsCurveChanges() throws Exception {
		PlotData data = new PlotData(PlotDataType.XYXY, new String[] { "one" }, null);
		Curve curve = (Curve) data.getFirstCurve();
		assertSame(curve, data.getCurve("one"));
		assertEquals(null, data.getCurve(null));

		AtomicInteger events = new AtomicInteger();
		data.addDataChangeListener((source, changedCurve, type) -> {
			assertSame(data, source);
			assertSame(curve, changedCurve);
			assertEquals(CurveChangeType.DATA, type);
			events.incrementAndGet();
		});
		SwingUtilities.invokeAndWait(() -> curve.add(1, 2));
		assertEquals(1, events.get());
	}
}
