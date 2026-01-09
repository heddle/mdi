package edu.cnu.mdi.splot.plot;

import java.util.EventListener;

public interface PlotChangeListener extends EventListener {
	/**
	 * A plot change event occurred
	 * 
	 * @param event the event
	 */
	public void plotChanged(PlotChangeType event);
}
