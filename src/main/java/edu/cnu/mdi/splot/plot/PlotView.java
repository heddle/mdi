package edu.cnu.mdi.splot.plot;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingConstants;
import javax.swing.ButtonGroup;

import edu.cnu.mdi.properties.PropertyUtils;
import edu.cnu.mdi.pseudo3D.Histo2DPanel;
import edu.cnu.mdi.splot.io.PlotFileFilter;
import edu.cnu.mdi.splot.io.PlotIO;
import edu.cnu.mdi.splot.io.RecentPlotFiles;
import edu.cnu.mdi.splot.io.RecentPlotsMenu;
import edu.cnu.mdi.transfer.FileDropHandler;
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

	// Card constants
	public static final String CARD_PLOT = "Plot";
	public static final String CARD_HISTO2D = "Histo2D";

	protected PlotCanvas _plotCanvas;
	protected PlotPanel _plotPanel;
	

	// CardLayout components
	private CardLayout _cardLayout;
	private JPanel _cardDeck;
	private JPanel _histoPanel;

	private File _currentPlotFile;
	private RecentPlotFiles _recentFiles;
	private RecentPlotsMenu _recentMenuHelper;
	private JMenu _recentPlotsMenu;
	// ... existing constants and fields ...
	private JMenu _viewMenu;
	private JRadioButtonMenuItem _plotMenuItem;
	private JMenuItem _histoMenuItem;

	public PlotView() {
		this("sPlot");
	}

	/**
	 * Create a PlotView
	 *
	 * @param title the view title
	 */
	public PlotView(Object... keyVals) {
		super(PropertyUtils.fromKeyValues(keyVals));

		// 1. Setup CardLayout deck
		_cardLayout = new CardLayout();
		_cardDeck = new JPanel(_cardLayout);

		// 2. Initialize cards
		_cardDeck.add(createPlotPanel(), CARD_PLOT);
		_cardDeck.add(createHistoPlaceholder(), CARD_HISTO2D);

		// 3. Add deck to the view (BorderLayout.CENTER by default)
		add(_cardDeck, BorderLayout.CENTER);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		Predicate<File> plotFilter = f -> {
			if (!f.isFile())
				return false;
			String name = f.getName().toLowerCase();
			return name.endsWith(".plot.json") || name.endsWith(".splot.json");
		};
		setFileFilter(plotFilter);
		addMenus();
		// Initial sync of the menu state
		updateViewMenuState();
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

		// Create View Menu
		_viewMenu = new JMenu("View");

		// Use radio buttons so the user knows which card is active
		ButtonGroup group = new ButtonGroup();

		_plotMenuItem = new JRadioButtonMenuItem("Standard Plot");
		_plotMenuItem.setSelected(true);
		_plotMenuItem.addActionListener(e -> showCard(CARD_PLOT));

		_histoMenuItem = new JRadioButtonMenuItem("2D Histogram");
		_histoMenuItem.addActionListener(e -> showCard(CARD_HISTO2D));

		group.add(_plotMenuItem);
		group.add(_histoMenuItem);

		_viewMenu.add(_plotMenuItem);
		_viewMenu.add(_histoMenuItem);

		BaseView.applyFocusFix(_viewMenu, this);
		menuBar.add(_viewMenu);

		revalidate();
		repaint();
	}

	/**
	 * Updates the enablement of the Histo2D option based on the PlotPanel's capabilities.
	 */
	private void updateViewMenuState() {
	    if (_histoMenuItem != null && _plotPanel != null) {
	        boolean canShowHisto = _plotPanel.holds2DHistogram();
	        _histoMenuItem.setEnabled(canShowHisto);
	        
	        // Only force a switch if we are CURRENTLY on the Histo card 
	        // and the new panel doesn't support it.
	        if (!canShowHisto && isHistoCardVisible()) {
	            showCard(CARD_PLOT);
	        }
	    }
	}

	/**
	 * Helper to check which card is currently at the front
	 */
	private boolean isHistoCardVisible() {
	    // CardLayout doesn't have a simple getVisibleCard() method, 
	    // so we check the internal state or the component visibility.
	    return _histoPanel.isVisible();
	}
	
	private JPanel createHistoPlaceholder() {
		_histoPanel = new JPanel(new BorderLayout());
		_histoPanel.setBackground(Color.DARK_GRAY);
		JLabel label = new JLabel("2D Histogram Placeholder", SwingConstants.CENTER);
		label.setForeground(Color.WHITE);
		_histoPanel.add(label, BorderLayout.CENTER);
		return _histoPanel;
	}

	
	/**
	 * Toggles between the Plot and Histo2D cards.
	 * 
	 * @param cardName Use PlotView.CARD_PLOT or PlotView.CARD_HISTO2D
	 */
	public void showCard(String cardName) {
		_cardLayout.show(_cardDeck, cardName);
	}

	// create the plot panel
	private PlotPanel createPlotPanel() {
		_plotCanvas = new PlotCanvas(null, "Empty Plot", "X Axis", "Y axis");
		_plotPanel = new PlotPanel(_plotCanvas);
		_plotCanvas.setTransferHandler(new FileDropHandler(this));

		return _plotPanel;
	}
	
	private void setHisto2DPanel() {
		if (_histoPanel != null) {
			_cardDeck.remove(_histoPanel);
		}
		
		if (_plotPanel.holds2DHistogram()) {
			PlotParameters params = _plotCanvas.getParameters();
			boolean logZ = reflectLogZ(params);
			_histoPanel = new Histo2DPanel(_plotCanvas.getPlotData().getHisto2DData(), params.getColorMap(), logZ);
			_plotCanvas.addPropertyChangeListener((Histo2DPanel)_histoPanel);
		} else {
			createHistoPlaceholder(); // empty placeholder
		}
	    _cardDeck.add(_histoPanel, CARD_HISTO2D);
	    
	}

	/**
	 * Updated to ensure every new plot starts on the "Plot" card.
	 */
	private void setPlotPanel(PlotPanel plotPanel) {
	    _cardDeck.remove(_plotPanel);
	    
	    _plotPanel = plotPanel;
	    _plotCanvas = plotPanel.getPlotCanvas();
	    _plotCanvas.setTransferHandler(new FileDropHandler(this));
	    
	    _cardDeck.add(_plotPanel, CARD_PLOT);
	    
	    // 1. Update menu enablement
	    updateViewMenuState();
	    
	    // 2. ALWAYS reset to the standard Plot card for new panels
	    showCard(CARD_PLOT);
	    
	    // 3. Ensure the radio button in the menu matches the reset
	    if (_plotMenuItem != null) {
	        _plotMenuItem.setSelected(true);
	    }
	    
	    setHisto2DPanel();
	    
	    _cardDeck.revalidate();
	    _cardDeck.repaint();
	}
	/**
	 * Switch to a different plot panel (and its canvas)
	 *
	 * @param plotPanel the new plot panel
	 */
	public void switchToPlotPanel(PlotPanel plotPanel) {
		Objects.requireNonNull(plotPanel, "PlotPanel cannot be null");
		_currentPlotFile = null;

		JMenu splotMenu = findMenu(getJMenuBar(), SplotEditMenu.MENU_TITLE);
		if (splotMenu != null) {
			getJMenuBar().remove(splotMenu);
			if (_plotCanvas != null)
				_plotCanvas.shutDown();
		}

		PlotCanvas plotCanvas = plotPanel.getPlotCanvas();
		plotCanvas.standUp();
		setPlotPanel(plotPanel);

		JMenu menu = new SplotEditMenu(plotCanvas);
		getJMenuBar().add(menu);

		revalidate();
		repaint();
	}

	// find a menu by name in a menubar
	private JMenu findMenu(JMenuBar menuBar, String targetName) {
		if (menuBar == null) {
			return null;
		}
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

	/**
	 * Handle files dropped on this view through drag and drop.
	 *
	 * @param files the dropped files.
	 */
	@Override
	public void filesDropped(List<File> files) {
		// just open the first valid plot file
		// files already filtered by FileDropHandler
		if (files == null || files.isEmpty()) {
			return;
		}
		File file = files.get(0);
		updateEnvironmentDataDirectory(file);
		openPlotFile(file);

	}

	// Get initial directory for file chooser from Environment
	private File getInitialChooserDirectory() {
		Environment env = Environment.getInstance();
		String dir = env.getDataDirectory();
		if (dir == null || dir.isBlank()) {
			return null;
		}
		File f = new File(dir);
		return (f.exists() && f.isDirectory()) ? f : null;
	}

	// Update Environment data directory based on chosen file/dir
	private void updateEnvironmentDataDirectory(File chosenFileOrDir) {
		if (chosenFileOrDir == null) {
			return;
		}

		File dir = chosenFileOrDir.isDirectory() ? chosenFileOrDir : chosenFileOrDir.getParentFile();
		if (dir != null && dir.exists() && dir.isDirectory()) {
			Environment.getInstance().setDataDirectory(dir.getAbsolutePath());
		}
	}

	// Open a plot file
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

	// Open the given plot file
	private void openPlotFile(File file) {
		if (file == null) {
			return;
		}

		try {
			// Load a fresh canvas on EDT (PlotIO does EDT-safe construction)
			PlotCanvas newCanvas = PlotIO.loadCanvas(file);

			// Cleanly retire old canvas
			if (_plotCanvas != null) {
				_plotCanvas.shutDown();
			}

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
			JOptionPane.showMessageDialog(this, ex.getMessage(), "Open Plot Failed", JOptionPane.ERROR_MESSAGE);

			// If it failed, drop it from recents to avoid a "dead" entry
			if (_recentFiles != null) {
				_recentFiles.remove(file);
				rebuildRecentMenu();
			}
		}
	}

	// Save the current plot
	private void doSave(boolean forceSaveAs) {

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
			int ok = JOptionPane.showConfirmDialog(this, "Overwrite existing file?\n" + target.getAbsolutePath(),
					"Confirm Save", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
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
			JOptionPane.showMessageDialog(this, ex.getMessage(), "Save Plot Failed", JOptionPane.ERROR_MESSAGE);
		}
	}

	// rebuild the recent files menu
	private void rebuildRecentMenu() {
		if (_recentPlotsMenu != null && _recentMenuHelper != null) {
			_recentMenuHelper.rebuild(_recentPlotsMenu);
		}
	}

	/**
	 * Best-effort reflection helper to detect whether the current plot parameters
	 * have log-Z enabled for 2D histograms/heatmaps.
	 * <p>
	 * This keeps the pseudo-3D card loosely coupled to PlotParameters API details.
	 */
	private static boolean reflectLogZ(Object plotParameters) {
		if (plotParameters == null) {
			return false;
		}
		final String[] methodNames = {
				"isLogZ",
				"getLogZ",
				"isLogZEnabled",
				"getLogZEnabled",
				"isLogz",
				"getLogz"
		};
		for (String mname : methodNames) {
			try {
				var m = plotParameters.getClass().getMethod(mname);
				Object val = m.invoke(plotParameters);
				if (val instanceof Boolean b) {
					return b.booleanValue();
				}
			} catch (Exception ignore) {
				// try next
			}
		}
		// Some implementations store it as a field instead of an accessor.
		final String[] fieldNames = { "logZ", "_logZ", "logz", "_logz" };
		for (String fname : fieldNames) {
			try {
				var f = plotParameters.getClass().getDeclaredField(fname);
				f.setAccessible(true);
				Object val = f.get(plotParameters);
				if (val instanceof Boolean b) {
					return b.booleanValue();
				}
			} catch (Exception ignore) {
			}
		}
		return false;
	}
}