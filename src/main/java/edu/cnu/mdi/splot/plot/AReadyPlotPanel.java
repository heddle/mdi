package edu.cnu.mdi.splot.plot;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;

import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;

public abstract class AReadyPlotPanel extends JPanel implements PlotChangeListener {

	// the plot canvas
	protected PlotCanvas canvas;

	// the plot panel
	protected PlotPanel plotPanel;


	public AReadyPlotPanel() {
		super();
	}
	
	protected void dataSetup() {
		try {
			canvas = new PlotCanvas(createPlotData(), getPlotTitle(), getXAxisLabel(), getYAxisLabel());
			canvas.addPlotChangeListener(this);
		} catch (PlotDataException e) {
			e.printStackTrace();
			return;
		}

		fillData();
		setParameters();
		plotPanel = new PlotPanel(canvas);
		plotPanel.setPreferredSize(new Dimension(300, 500));
		add(plotPanel, BorderLayout.CENTER);

	}
	
	/**
	 * Get the plot canvas
	 *
	 * @return the plot canvas
	 */
	public PlotCanvas getPlotCanvas() {
		return canvas;
	}

	/**
	 * Get the plot panel
	 *
	 * @return the plot panel
	 */
	public PlotPanel getPlotPanel() {
		return plotPanel;
	}

	/**
	 * Create the plot data
	 *
	 * @return the plot data
	 * @throws PlotDataException
	 */
	protected abstract PlotData createPlotData() throws PlotDataException;

	/** get the x axis label */
	protected abstract String getXAxisLabel();

	/** get the y axis label */
	protected abstract String getYAxisLabel();

	/** get the plot title */
	protected abstract String getPlotTitle();

	// fill the plot data
	public abstract void fillData();

	// set the preferences
	public abstract void setParameters();
}
