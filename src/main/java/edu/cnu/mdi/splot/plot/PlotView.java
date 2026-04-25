package edu.cnu.mdi.splot.plot;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.prefs.Preferences;

import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingConstants;

import edu.cnu.mdi.pseudo3D.Histo2DPanel;
import edu.cnu.mdi.splot.io.PlotFileFilter;
import edu.cnu.mdi.splot.io.PlotIO;
import edu.cnu.mdi.splot.io.RecentPlotFiles;
import edu.cnu.mdi.splot.io.RecentPlotsMenu;
import edu.cnu.mdi.util.Environment;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.AbstractViewInfo;
import edu.cnu.mdi.view.BaseView;

/**
 * Predefined view that displays a plot loaded from an splot JSON file.
 *
 * <p>{@code PlotView} hosts a {@link CardLayout} deck with two cards:</p>
 * <ul>
 *   <li>{@link #CARD_PLOT} — the standard {@link PlotPanel} / {@link PlotCanvas}
 *       rendering.</li>
 *   <li>{@link #CARD_HISTO2D} — an optional 2-D histogram panel, enabled only
 *       when the loaded data contains a {@code Histo2D} dataset.</li>
 * </ul>
 *
 * <h2>File drag-and-drop</h2>
 * <p>Drop support for {@code .plot.json} and {@code .splot.json} files is
 * activated in the constructor via
 * {@link BaseView#enableFileDrop(java.util.function.Predicate)}.
 * Subclasses that need to accept additional file types should call
 * {@code enableFileDrop} with a broader predicate and extend
 * {@link #filesDropped} accordingly.</p>
 *
 * <h2>Plot lifecycle</h2>
 * <p>All panel swaps (open, drag-and-drop, external programmatic switch) flow
 * through {@link #setPlotPanel}, which is the single choke-point responsible
 * for keeping the canvas reference, the edit menu, and the View menu
 * in sync.</p>
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

	/**
	 * Create a {@code PlotView} with the default title {@code "sPlot"}.
	 */
	public PlotView() {
		this("sPlot");
	}

	/**
	 * Create a {@code PlotView} from alternating key/value property pairs.
	 *
	 * <p>The view is built in four steps:</p>
	 * <ol>
	 *   <li>A {@link CardLayout} deck is created holding the standard plot card
	 *       ({@link #CARD_PLOT}) and the 2-D histogram card
	 *       ({@link #CARD_HISTO2D}).</li>
	 *   <li>File drag-and-drop is enabled for {@code .plot.json} and
	 *       {@code .splot.json} files via {@link #enableFileDrop(Predicate)}.
	 *       The handler is installed once on all appropriate surfaces (canvas,
	 *       viewport, and frame); it does <em>not</em> need to be re-installed
	 *       when the plot panel is replaced by {@link #setPlotPanel}.</li>
	 *   <li>Plot and edit menus are added to the menu bar.</li>
	 *   <li>The View menu state is synchronised with the current panel's
	 *       capabilities.</li>
	 * </ol>
	 *
	 * @param keyVals alternating {@link PropertyUtils} key/value pairs
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

		// Enable drop for splot plot files.  enableFileDrop installs the handler
		// on the canvas, viewport, and view frame, so it covers the full card
		// deck regardless of which card is currently visible.  There is no need
		// to re-install the handler when the plot panel is swapped.
		enableFileDrop(f -> f.isFile()
				&& (f.getName().toLowerCase().endsWith(".plot.json")
						|| f.getName().toLowerCase().endsWith(".splot.json")));

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
	 *  Get an information object describing this view,
	 *  used in the UI and for help.
	 */
	@Override
	public AbstractViewInfo getViewInfo() {
		return new PlotViewInfo();
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

	/**
	 * Create the initial plot panel and its canvas.
	 *
	 * <p>No {@link edu.cnu.mdi.transfer.FileDropHandler} is installed here.
	 * Drop support for the canvas is handled once, centrally, by the
	 * {@link #enableFileDrop(Predicate)} call in the constructor.  When the
	 * canvas is replaced later (via {@link #setPlotPanel}), the view-frame
	 * surface installed by {@code enableFileDrop} continues to receive drops
	 * without any re-wiring.</p>
	 *
	 * @return the newly created {@link PlotPanel}
	 */
	private PlotPanel createPlotPanel() {
		_plotCanvas = new PlotCanvas(null, "Empty Plot", "X Axis", "Y axis");
		_plotPanel = new PlotPanel(_plotCanvas);

		_plotPanel.getToolBar().addInfoButton();
		return _plotPanel;
	}

	// set up the 2D histogram panel
	private void setHisto2DPanel() {
		if (_histoPanel != null) {
			_cardDeck.remove(_histoPanel);
		}

		if (_plotPanel.holds2DHistogram()) {
			PlotParameters params = _plotCanvas.getParameters();
			boolean logZ = params.isLogZ();
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
	/**
	 * Replace the current plot panel with a new one and update all dependent state.
	 *
	 * <p>This method is the single choke-point through which every panel swap
	 * flows, whether triggered by opening a file, an external call to
	 * {@link #switchToPlotPanel}, or any future code path.</p>
	 *
	 * <p>The following updates are performed atomically before the card deck is
	 * repainted:</p>
	 * <ol>
	 *   <li>The old panel is removed from the deck.</li>
	 *   <li>{@link #_plotPanel} and {@link #_plotCanvas} are updated.</li>
	 *   <li>The View menu is re-evaluated for 2-D histogram capability.</li>
	 *   <li>The deck is reset to the {@link #CARD_PLOT} card so that the user
	 *       always starts on the standard plot after a load.</li>
	 *   <li>The 2-D histogram panel ({@link #CARD_HISTO2D}) is rebuilt for the
	 *       new canvas.</li>
	 * </ol>
	 *
	 * <p><strong>No {@link edu.cnu.mdi.transfer.FileDropHandler} is installed
	 * here.</strong>  Drop support was established once by
	 * {@link #enableFileDrop(Predicate)} in the constructor; the view-frame
	 * surface it installed remains active regardless of how many times the
	 * canvas is swapped.</p>
	 *
	 * @param plotPanel the new panel to install; must not be {@code null}
	 */
	private void setPlotPanel(PlotPanel plotPanel) {
		_cardDeck.remove(_plotPanel);

		_plotPanel = plotPanel;
		_plotPanel.getToolBar().addInfoButton();
		_plotCanvas = plotPanel.getPlotCanvas();

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
			if (_plotCanvas != null) {
				_plotCanvas.shutDown();
			}
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
	 * Handle plot files dropped on this view through drag-and-drop.
	 *
	 * <p>Only the first file in the list is used; the filter set by
	 * {@link #enableFileDrop(Predicate)} in the constructor already guarantees
	 * every entry ends with {@code .plot.json} or {@code .splot.json}, so no
	 * second format check is required here.</p>
	 *
	 * <p>The parent directory of the dropped file is recorded in
	 * {@link edu.cnu.mdi.util.Environment} so that the next "Open" dialog opens
	 * in the same location.</p>
	 *
	 * @param files the accepted dropped files; never {@code null}, never empty
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

}