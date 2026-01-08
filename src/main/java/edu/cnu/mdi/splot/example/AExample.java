package edu.cnu.mdi.splot.example;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatIntelliJLaf;

import edu.cnu.mdi.graphics.GraphicsUtils;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.plot.PlotCanvas;
import edu.cnu.mdi.splot.plot.PlotPanel;
import edu.cnu.mdi.splot.plot.SplotMenus;
import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * A template class for plot examples
 *
 * @author heddle
 *
 */
@SuppressWarnings("serial")
public abstract class AExample extends JFrame {

	// the plot canvas
	protected PlotCanvas canvas;

	// the menus and items
	protected SplotMenus menus;

	public AExample() {
		super("sPlot");
		UIInit();

		// set up what to do if the window is closed
		WindowAdapter windowAdapter = new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				System.exit(1);
			}
		};
		addWindowListener(windowAdapter);

		try {
			canvas = new PlotCanvas(createPlotData(), getPlotTitle(), getXAxisLabel(), getYAxisLabel());
		}
		catch (PlotDataException e) {
			e.printStackTrace();
			return;
		}

		// add the menu bar
		JMenuBar mb = new JMenuBar();
		setJMenuBar(mb);
		menus = new SplotMenus(canvas, mb, true);
		fillData();
		setParameters();
		final PlotPanel ppanel = new PlotPanel(canvas);

		ppanel.setPreferredSize(new Dimension(750, 700));

		add(ppanel, BorderLayout.CENTER);

		pack();
		GraphicsUtils.centerComponent(this);
	}

	   //initialize the FlatLaf UI
		private void UIInit() {
			FlatIntelliJLaf.setup();
			UIManager.put("Component.focusWidth", 1);
			UIManager.put("Component.arc", 6);
			UIManager.put("Button.arc", 6);
			UIManager.put("TabbedPane.showTabSeparators", true);
			Fonts.refresh();
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
	 * Create the plot data
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
