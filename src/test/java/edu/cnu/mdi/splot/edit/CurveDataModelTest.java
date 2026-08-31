package edu.cnu.mdi.splot.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.splot.pdata.ACurve;
import edu.cnu.mdi.splot.pdata.PlotData;

class CurveDataModelTest {

	@Test
	void ownsItsRowsAndNotifiesForMutations() throws Exception {
		PlotData data = PlotData.emptyData();
		ACurve curve = data.getFirstCurve();
		CurveDataModel model = new CurveDataModel(data.getCurves());
		AtomicInteger changes = new AtomicInteger();
		model.addTableModelListener(event -> changes.incrementAndGet());

		SwingUtilities.invokeAndWait(() -> {
			model.setValueAt(false, 0, CurveDataModel.VIS_COLUMN);
			model.setValueAt("renamed", 0, CurveDataModel.NAME_COLUMN);
			model.remove(curve);
			model.add(curve);
			model.clear();
		});

		assertFalse(curve.isVisible());
		assertEquals("renamed", curve.name());
		assertEquals(0, model.getRowCount());
		assertEquals(5, changes.get());
		assertEquals(1, data.size(), "editing table rows must not mutate PlotData");
	}

	@Test
	void acceptsNullDataAsAnEmptyModel() {
		CurveDataModel model = new CurveDataModel((List<ACurve>) null);
		assertEquals(0, model.getRowCount());
	}
}
