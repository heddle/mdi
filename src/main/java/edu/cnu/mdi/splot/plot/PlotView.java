package edu.cnu.mdi.splot.plot;

import java.util.Objects;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import edu.cnu.mdi.properties.PropertySupport;
import edu.cnu.mdi.splot.example.AExample;
import edu.cnu.mdi.view.BaseView;

/**
 * This is a predefined view used to display a plot from splot
 *
 * @author heddle
 *
 */
@SuppressWarnings("serial")
public class PlotView extends BaseView {

	// the owner canvas
	protected PlotCanvas _plotCanvas;

	// panel that holds the canvas
	protected PlotPanel _plotPanel;

	// the menu bar
	private JMenuBar _menuBar;

	public PlotView() {
		this("sPlot");
	}
	

	public PlotView(Object... keyVals) {
		super(PropertySupport.fromKeyValues(keyVals));
		add(createPlotPanel());
		addMenus();
		JMenuBar _menuBar = new JMenuBar();
		setJMenuBar(_menuBar);
	}

	// add the plot edit menus
	private void addMenus() {

	}

	// create the plot panel
	private PlotPanel createPlotPanel() {
		_plotCanvas = new PlotCanvas(null, "Empty Plot", "X Axis", "Y axis");
		_plotPanel = new PlotPanel(_plotCanvas);
		return _plotPanel;
	}
	
	public void setPlotPanel(PlotPanel plotPanel) {
		remove(_plotPanel);
		_plotPanel = plotPanel;
		_plotCanvas = plotPanel.getPlotCanvas();
		add(_plotPanel);
		revalidate();
		repaint();
	}
	
	/**
	 * Switch to a new example, replacing the current plot panel and menus
	 * This is used by the demo app
	 * @param example the example to switch to
	 */
	public void switchToExample(AExample example) {
		Objects.requireNonNull(example, "Example cannot be null");
		JMenu splotMenu = findMenu(getJMenuBar(), SplotEditMenu.MENU_TITLE);
		if (splotMenu != null) {
			getJMenuBar().remove(splotMenu);
			example.getPlotCanvas().shutDown();
		}
		PlotCanvas plotCanvas = example.getPlotCanvas();
		PlotPanel plotPanel = example.getPlotPanel();
		setPlotPanel(plotPanel);
		new SplotEditMenu(plotCanvas, getJMenuBar(),false);
		revalidate();
		repaint();
	}
	
	private JMenu findMenu(JMenuBar menuBar, String targetName) {
	    for (int i = 0; i < menuBar.getMenuCount(); i++) {
	        JMenu menu = menuBar.getMenu(i);
	        // Check either displayed text or internal component name
	        if (menu != null && targetName.equals(menu.getText())) {
	            return menu;
	        }
	    }
	    return null; // Not found
	}

	/**
	 * Get the plot canvas
	 *
	 * @return th plot canvas
	 */
	public PlotCanvas getPlotCanvas() {
		return _plotCanvas;
	}
}