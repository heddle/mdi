package edu.cnu.mdi.splot.plot;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JMenuBar;
import javax.swing.JPanel;

import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;

/**
 * An abstract plot panel that is ready to use once the data is filled
 * and the parameters are set.
 * 
 * @author heddle  
 * 
 */
public abstract class AReadyPlotPanel extends JPanel implements PlotChangeListener {

	// the plot canvas
	protected PlotCanvas canvas;

	// the plot panel
	protected PlotPanel plotPanel;

	// the menus and items
	protected SplotEditMenu menu;

	// the menu bar
	protected JMenuBar menuBar;
	
	private final boolean includeMenu;

	public AReadyPlotPanel(boolean includeMenu) {
		super();
		this.includeMenu = includeMenu;
		setLayout(new BorderLayout());
		setBackground(java.awt.Color.white);
		setOpaque(true);
	}
	
	@Override
	public Insets getInsets() {
		Insets def = super.getInsets();
		return new Insets(def.top + 2, def.left + 2, def.bottom + 2, def.right + 2);
	}

	
	/**
	 * Set up the data structures
	 */
	protected void dataSetup() {
		try {
			canvas = new PlotCanvas(createPlotData(), getPlotTitle(), getXAxisLabel(), getYAxisLabel());
			canvas.addPlotChangeListener(this);
		} catch (PlotDataException e) {
			e.printStackTrace();
			return;
		}
		if (includeMenu) {
			menu = new SplotEditMenu(canvas);
			menuBar = new JMenuBar();
			menuBar.add(menu);
			add(menuBar, BorderLayout.NORTH);
		}
		
		//set the plot parameters
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
	 * Clear all data from the plot in anticipation of new data
	 */
	public abstract void clearData();

	/**
	 * Create the plot data
	 *
	 * @return the plot data
	 * @throws PlotDataException
	 */
	protected abstract PlotData createPlotData() throws PlotDataException;

	/** Get the x axis label
	 * @return the x axis label
	 */
	protected abstract String getXAxisLabel();

	/** Get the y axis label 
	 * @return the y axis label
	 */
	protected abstract String getYAxisLabel();

	/** Get the plot title
	 * @return the plot title 
	 */
	protected abstract String getPlotTitle();

	/**
	 * Set the plot parameters
	 */
	public abstract void setParameters();
}
