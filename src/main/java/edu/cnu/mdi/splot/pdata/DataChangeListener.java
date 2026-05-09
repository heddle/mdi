package edu.cnu.mdi.splot.pdata;

import java.util.EventListener;

/**
 * A listener for changes to the data. It is notified when a data set is added, removed, or changed.
 *
 * @author heddle
 *
 */
public interface DataChangeListener extends EventListener {

	/**
	 * A data set changed
	 *
	 * @param plotData the dataSet that changed
	 * @param curve    the curve that changed, or null if the change was to the data set as a whole
	 * @param type     the type of change
	 */
	public void dataSetChanged(PlotData plotData, ACurve curve, CurveChangeType type);
}
