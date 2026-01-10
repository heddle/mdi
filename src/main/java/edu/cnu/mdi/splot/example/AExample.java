package edu.cnu.mdi.splot.example;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatIntelliJLaf;

import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.plot.PlotCanvas;
import edu.cnu.mdi.splot.plot.PlotChangeListener;
import edu.cnu.mdi.splot.plot.PlotChangeType;
import edu.cnu.mdi.splot.plot.PlotPanel;
import edu.cnu.mdi.splot.plot.SplotEditMenu;
import edu.cnu.mdi.swing.WindowPlacement;
import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * A template class for plot examples
 *
 * @author heddle
 *
 */
@SuppressWarnings("serial")
public abstract class AExample extends JFrame implements PlotChangeListener {

	// the plot canvas
	protected PlotCanvas canvas;

	// the plot panel
	protected PlotPanel plotPanel;

	// the menus and items
	protected SplotEditMenu menu;

	protected JMenuBar menuBar;

	protected boolean headless = false;

	public AExample() {
		this(false);
	}

	public AExample(boolean headless) {
		super("sPlot");
		this.headless = headless;
		UIInit();
		dataSetup();
		if (headless) {
			return;
		}

		// set up what to do if the window is closed
		WindowAdapter windowAdapter = new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				System.exit(1);
			}
		};
		addWindowListener(windowAdapter);

		// add the menu bar
		setJMenuBar(menuBar);
		add(plotPanel, BorderLayout.CENTER);
		pack();
		WindowPlacement.centerComponent(this);
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

		menuBar = new JMenuBar();
		menu = new SplotEditMenu(canvas);
		menuBar.add(menu);

		plotPanel.setPreferredSize(new Dimension(750, 700));

	}

	// initialize the FlatLaf UI
	private void UIInit() {
		FlatIntelliJLaf.setup();
		UIManager.put("Component.focusWidth", 1);
		UIManager.put("Component.arc", 6);
		UIManager.put("Button.arc", 6);
		UIManager.put("TabbedPane.showTabSeparators", true);
		Fonts.refresh();
	}

	@Override
	public void plotChanged(PlotChangeType type) {
		switch (type) {
		case SHUTDOWN:
			break;

		case STOODUP:
			break;
		}
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
