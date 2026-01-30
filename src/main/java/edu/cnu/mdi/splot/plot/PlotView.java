package edu.cnu.mdi.splot.plot;

import java.io.File;
import java.util.Objects;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import edu.cnu.mdi.properties.PropertyUtils;
import edu.cnu.mdi.splot.example.AExample;
import edu.cnu.mdi.splot.io.PlotFileFilter;
import edu.cnu.mdi.splot.io.PlotIO;
import edu.cnu.mdi.splot.io.RecentPlotFiles;
import edu.cnu.mdi.splot.io.RecentPlotsMenu;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.util.Environment;
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

	// current file (for Save vs Save As)
	private File _currentPlotFile;

	// recent files support
	private RecentPlotFiles _recentFiles;
	private RecentPlotsMenu _recentMenuHelper;
	private JMenu _recentPlotsMenu;

	public PlotView() {
		this("sPlot");
	}

	public PlotView(Object... keyVals) {
		super(PropertyUtils.fromKeyValues(keyVals));
		add(createPlotPanel());

		// IMPORTANT: create menu bar BEFORE adding menus
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		addMenus();
	}

	// add the plot IO menus + the splot edit menu
	private void addMenus() {

		JMenuBar menuBar = getJMenuBar();
		if (menuBar == null) {
			menuBar = new JMenuBar();
			setJMenuBar(menuBar);
		}

		// Preferences node for recent files (scoped to PlotView)
		Preferences prefs = Preferences.userNodeForPackage(getClass()).node("splot");
		_recentFiles = new RecentPlotFiles(prefs, 10);

		// File menu
		JMenu fileMenu = new JMenu("File");

		JMenuItem openItem = new JMenuItem("Open Plot\u2026");
		openItem.addActionListener(e -> doOpenPlot());
		fileMenu.add(openItem);

		_recentPlotsMenu = new JMenu("Recent Plots");
		_recentMenuHelper = new RecentPlotsMenu(_recentFiles, this::openPlotFile);
		_recentMenuHelper.rebuild(_recentPlotsMenu);
		fileMenu.add(_recentPlotsMenu);

		fileMenu.addSeparator();

		JMenuItem saveItem = new JMenuItem("Save");
		saveItem.addActionListener(e -> doSave(false));
		fileMenu.add(saveItem);

		JMenuItem saveAsItem = new JMenuItem("Save As\u2026");
		saveAsItem.addActionListener(e -> doSave(true));
		fileMenu.add(saveAsItem);

		// Remove any existing File menu, then add ours
		JMenu existingFile = findMenu(menuBar, "File");
		if (existingFile != null) {
			menuBar.remove(existingFile);
		}
		// add the file menu and call "hack" to fix focus issues
		BaseView.applyFocusFix(fileMenu, this);
		menuBar.add(fileMenu);

		// Ensure the splot edit menu exists (and is tied to current canvas)
		JMenu existingEdit = findMenu(menuBar, SplotEditMenu.MENU_TITLE);
		if (existingEdit != null) {
			menuBar.remove(existingEdit);
		}
		
		SplotEditMenu editMenu = new SplotEditMenu(_plotCanvas);
		// add the edit menu and call "hack" to fix focus issues
		BaseView.applyFocusFix(editMenu, this);
		menuBar.add(editMenu);

		revalidate();
		repaint();
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
	 * Switch to a new example, replacing the current plot panel and menus This is
	 * used by the demo app
	 *
	 * @param example the example to switch to
	 */
	public void switchToExample(AExample example) {
		Objects.requireNonNull(example, "Example cannot be null");

		// example switch means the current file is no longer authoritative
		_currentPlotFile = null;

		// remove the old edit menu and shutdown old canvas
		JMenu splotMenu = findMenu(getJMenuBar(), SplotEditMenu.MENU_TITLE);
		if (splotMenu != null) {
			getJMenuBar().remove(splotMenu);
			example.getPlotCanvas().shutDown(); // keeps your existing behavior
		}

		PlotCanvas plotCanvas = example.getPlotCanvas();
		plotCanvas.standUp();
		PlotPanel plotPanel = example.getPlotPanel();
		setPlotPanel(plotPanel);

		// re-add edit menu tied to new canvas (keep file menu as-is)
		JMenu menu = new SplotEditMenu(plotCanvas);
		getJMenuBar().add(menu);

		revalidate();
		repaint();
	}

	private JMenu findMenu(JMenuBar menuBar, String targetName) {
		if (menuBar == null) return null;
		for (int i = 0; i < menuBar.getMenuCount(); i++) {
			JMenu menu = menuBar.getMenu(i);
			if (menu != null && targetName.equals(menu.getText())) {
				return menu;
			}
		}
		return null;
	}

	/**
	 * Get the plot canvas
	 *
	 * @return th plot canvas
	 */
	public PlotCanvas getPlotCanvas() {
		return _plotCanvas;
	}

	// -----------------------------
	// File actions
	// -----------------------------

	private File getInitialChooserDirectory() {
		Environment env = Environment.getInstance();
		String dir = env.getDataDirectory();
		if (dir == null || dir.isBlank()) {
			return null;
		}
		File f = new File(dir);
		return (f.exists() && f.isDirectory()) ? f : null;
	}

	private void updateEnvironmentDataDirectory(File chosenFileOrDir) {
		if (chosenFileOrDir == null) return;

		File dir = chosenFileOrDir.isDirectory() ? chosenFileOrDir : chosenFileOrDir.getParentFile();
		if (dir != null && dir.exists() && dir.isDirectory()) {
			Environment.getInstance().setDataDirectory(dir.getAbsolutePath());
		}
	}

	private void doOpenPlot() {
		JFileChooser fc = new JFileChooser(getInitialChooserDirectory());
		fc.setFileFilter(new PlotFileFilter());
		fc.setAcceptAllFileFilterUsed(true);

		int res = fc.showOpenDialog(this);
		if (res != JFileChooser.APPROVE_OPTION) {
			return;
		}

		File selected = fc.getSelectedFile();
		updateEnvironmentDataDirectory(selected);
		openPlotFile(selected);
	}

	private void openPlotFile(File file) {
		if (file == null) return;

		try {
			// Load a fresh canvas on EDT (PlotIO does EDT-safe construction)
			PlotCanvas newCanvas = PlotIO.loadCanvas(file);

			// Cleanly retire old canvas
			if (_plotCanvas != null) {
				_plotCanvas.shutDown();
			}
			
			PlotData plotData = newCanvas.getPlotData();
			System.out.println("Loaded plot type = " + plotData.getType());
			System.out.println("Has curves = " + plotData.getCurves().size());
			System.out.println("Has H2D = " + (plotData.getHisto2DData() != null));


			// Stand up and install new plot
			newCanvas.standUp();
			PlotPanel newPanel = new PlotPanel(newCanvas);
			setPlotPanel(newPanel);

			// Update edit menu to point at the new canvas
			JMenuBar menuBar = getJMenuBar();
			if (menuBar != null) {
				JMenu existingEdit = findMenu(menuBar, SplotEditMenu.MENU_TITLE);
				if (existingEdit != null) {
					menuBar.remove(existingEdit);
				}
				menuBar.add(new SplotEditMenu(newCanvas));
			}

			// Track current file + recent list
			_currentPlotFile = file;
			updateEnvironmentDataDirectory(file);

			if (_recentFiles != null) {
				_recentFiles.add(file);
			}
			rebuildRecentMenu();

			revalidate();
			repaint();

		} catch (Exception ex) {
			JOptionPane.showMessageDialog(
					this,
					ex.getMessage(),
					"Open Plot Failed",
					JOptionPane.ERROR_MESSAGE);

			// If it failed, drop it from recents to avoid a "dead" entry
			if (_recentFiles != null) {
				_recentFiles.remove(file);
				rebuildRecentMenu();
			}
		}
	}

	private void doSave(boolean forceSaveAs) {
		System.out.println("SAVE plot type = " + _plotCanvas.getPlotData().getType());
		System.out.println("SAVE curves = " + _plotCanvas.getPlotData().getCurves().size());
		System.out.println("SAVE has H2D = " + (_plotCanvas.getPlotData().getHisto2DData() != null));

		File target = _currentPlotFile;

		if (forceSaveAs || target == null) {
			JFileChooser fc = new JFileChooser(getInitialChooserDirectory());
			fc.setFileFilter(new PlotFileFilter());
			fc.setAcceptAllFileFilterUsed(true);

			int res = fc.showSaveDialog(this);
			if (res != JFileChooser.APPROVE_OPTION) {
				return;
			}

			target = PlotFileFilter.ensurePlotExtension(fc.getSelectedFile());

			// keep Environment in sync with where the user navigated
			updateEnvironmentDataDirectory(target);
		}

		// Confirm overwrite if needed
		if (target.exists()) {
			int ok = JOptionPane.showConfirmDialog(
					this,
					"Overwrite existing file?\n" + target.getAbsolutePath(),
					"Confirm Save",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE);
			if (ok != JOptionPane.OK_OPTION) {
				return;
			}
		}

		try {
			PlotIO.save(_plotCanvas, target);

			_currentPlotFile = target;
			updateEnvironmentDataDirectory(target);

			if (_recentFiles != null) {
				_recentFiles.add(target);
			}
			rebuildRecentMenu();

		} catch (Exception ex) {
			JOptionPane.showMessageDialog(
					this,
					ex.getMessage(),
					"Save Plot Failed",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void rebuildRecentMenu() {
		if (_recentPlotsMenu != null && _recentMenuHelper != null) {
			_recentMenuHelper.rebuild(_recentPlotsMenu);
		}
	}
}
