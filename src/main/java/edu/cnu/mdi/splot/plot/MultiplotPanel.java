package edu.cnu.mdi.splot.plot;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.EventQueue;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;

import edu.cnu.mdi.splot.io.PlotFileFilter;
import edu.cnu.mdi.splot.io.PlotIO;
import edu.cnu.mdi.util.Environment;

/**
 * A Swing panel that hosts multiple {@link PlotPanel}s, showing exactly one at a time.
 * <p>
 * This component is intended for "diagnostic docks" in simulation views (e.g., a network
 * decluttering view) where you want a small gallery of predefined plots rather than
 * arbitrary user-loaded plots.
 * </p>
 *
 * <h2>UI model</h2>
 * <ul>
 * <li>A {@code Gallery} menu lists available plots; selecting an entry makes it active.</li>
 * <li>An optional {@code File} menu provides {@code Save} for the <em>active</em> plot.</li>
 * <li>The plot-specific {@code Edit Plot} menu (a {@link SplotEditMenu}) is rebuilt when
 *     the active plot changes.</li>
 * </ul>
 *
 * <h2>Implementation notes</h2>
 * <ul>
 * <li>Uses a {@link CardLayout} so plots are added once and then switched by card name,
 *     avoiding remove/add churn.</li>
 * <li>All UI mutations are executed on the Swing EDT. If a caller invokes methods from
 *     a non-EDT thread (e.g., a simulation thread), the work is safely marshaled to the EDT.</li>
 * <li>The first plot added is automatically selected and displayed.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 * <p>
 * Public methods that mutate the UI will run on the EDT. If called off-EDT, they are
 * queued via {@link SwingUtilities#invokeLater(Runnable)}.
 * </p>
 *
 * @author heddle
 */
@SuppressWarnings("serial")
public class MultiplotPanel extends JPanel {

    /** Menu bar shown at the top of the panel. */
    private final JMenuBar menuBar = new JMenuBar();

    /** Optional File menu; may be null if not requested. */
    private JMenu fileMenu;

    /** Gallery menu (always present). */
    private final JMenu galleryMenu = new JMenu("Gallery");

    /** The per-plot edit menu; present only when a plot is active. */
    private JMenu splotMenu;

    /** File->Save item (enabled only when a plot is active). */
    private JMenuItem saveItem;

    /** Center panel that hosts plot cards. */
    private final JPanel cardPanel = new JPanel();

    /** Card layout controller. */
    private final CardLayout cardLayout = new CardLayout();

    /** Placeholder card shown when there is no active plot. */
    private static final String EMPTY_CARD = "__EMPTY__";

    /** Entries in insertion order. */
    private final List<PlotEntry> entries = new ArrayList<>();

    /** Radio selection group for gallery menu items. */
    private final ButtonGroup galleryGroup = new ButtonGroup();

    /** Current active plot entry (null means no active plot). */
    private PlotEntry active;

    /**
     * Create a multi-plot panel.
     *
     * @param includeFileMenu if true, include a {@code File} menu with {@code Save}
     */
    public MultiplotPanel(boolean includeFileMenu) {
        super(new BorderLayout(4, 4));

        // Menus
        add(menuBar, BorderLayout.NORTH);
        if (includeFileMenu) {
            createFileMenu();
        }
        menuBar.add(galleryMenu);

        // Cards
        cardPanel.setLayout(cardLayout);
        cardPanel.add(new JPanel(), EMPTY_CARD); // simple placeholder
        add(cardPanel, BorderLayout.CENTER);

        // Start empty
        showEmpty();
        updateSaveEnabled();
    }

    // ------------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------------

    /**
     * Add a plot panel to this multiplot panel.
     * <p>
     * The plot is registered in the Gallery menu and placed into the {@link CardLayout}.
     * If this is the first plot added, it becomes the active plot automatically.
     * </p>
     *
     * @param title title for the Gallery menu item (non-null)
     * @param pp    the plot panel to add (non-null)
     */
    public void addPlot(String title, PlotPanel pp) {
        Objects.requireNonNull(title, "title cannot be null");
        Objects.requireNonNull(pp, "PlotPanel cannot be null");

        runOnEDT(() -> doAddPlot(title, pp));
    }

    /**
     * Remove a plot panel from this multiplot panel.
     * <p>
     * If the removed plot was active, another plot is selected if available; otherwise
     * the panel becomes empty.
     * </p>
     *
     * @param pp the plot panel to remove (non-null)
     */
    public void removePlot(PlotPanel pp) {
        Objects.requireNonNull(pp, "PlotPanel cannot be null");
        runOnEDT(() -> doRemovePlot(pp));
    }

    /**
     * Remove all plots from this multiplot panel and show an empty placeholder.
     * <p>
     * This removes the plots from the UI (it does not call {@code clearData()}).
     * </p>
     */
    public void removeAllPlots() {
        runOnEDT(this::doRemoveAllPlots);
    }

    /**
     * Clear all data on all curves for all plots.
     * <p>
     * Use with caution: this wipes the data from every plot canvas.
     * </p>
     */
    public void clearAllPlots() {
        runOnEDT(() -> {
            for (PlotEntry e : entries) {
                PlotCanvas c = e.plotPanel.getPlotCanvas();
                if (c != null) {
                    c.clearData();
                }
            }
        });
    }

    /**
     * Get the active plot panel, or {@code null} if none.
     *
     * @return the active plot panel or null
     */
    public PlotPanel getActivePlotPanel() {
        if (EventQueue.isDispatchThread()) {
            return (active == null) ? null : active.plotPanel;
        }
        // best-effort snapshot; callers should treat as informational
        PlotEntry a = active;
        return (a == null) ? null : a.plotPanel;
    }

    // ------------------------------------------------------------------------
    // Internal implementation (EDT only)
    // ------------------------------------------------------------------------

    private void doAddPlot(String title, PlotPanel pp) {
        requireEDT();

        // Create a stable unique card key
        String key = UUID.randomUUID().toString();

        // Add to cards
        cardPanel.add(pp, key);

        // Add to Gallery as a radio item (shows selection state)
        JRadioButtonMenuItem mi = new JRadioButtonMenuItem(title);
        galleryGroup.add(mi);
        galleryMenu.add(mi);

        PlotEntry entry = new PlotEntry(title, key, pp, mi);
        entries.add(entry);

        mi.addActionListener(e -> setActiveEntry(entry));

        // Auto-select first plot
        if (active == null) {
            mi.setSelected(true);
            setActiveEntry(entry);
        } else {
            // keep current selection; ensure layout is valid
            cardPanel.revalidate();
            cardPanel.repaint();
        }
    }

    private void doRemovePlot(PlotPanel pp) {
        requireEDT();

        PlotEntry victim = null;
        for (PlotEntry e : entries) {
            if (e.plotPanel == pp) {
                victim = e;
                break;
            }
        }
        if (victim == null) {
            return; // not present
        }

        // If removing active, shut it down first.
        boolean wasActive = (active == victim);
        if (wasActive) {
            shutdownActiveCanvasAndMenu();
        }

        // Remove from UI: menu + card
        galleryMenu.remove(victim.menuItem);
        galleryGroup.remove(victim.menuItem);
        cardPanel.remove(victim.plotPanel);
        entries.remove(victim);

        // Choose a new active plot if needed
        if (wasActive) {
            active = null;
            if (!entries.isEmpty()) {
                PlotEntry next = entries.get(0);
                next.menuItem.setSelected(true);
                setActiveEntry(next);
            } else {
                showEmpty();
            }
        }

        galleryMenu.revalidate();
        galleryMenu.repaint();
        cardPanel.revalidate();
        cardPanel.repaint();

        updateSaveEnabled();
    }

    private void doRemoveAllPlots() {
        requireEDT();

        shutdownActiveCanvasAndMenu();

        // Remove entries
        for (PlotEntry e : entries) {
            galleryMenu.remove(e.menuItem);
            galleryGroup.remove(e.menuItem);
            cardPanel.remove(e.plotPanel);
        }
        entries.clear();
        active = null;

        showEmpty();
        updateSaveEnabled();
    }

    /**
     * Switch the UI to the given plot entry, rebuilding the edit menu and managing
     * plot canvas lifecycle.
     *
     * @param entry entry to activate (non-null)
     */
    private void setActiveEntry(PlotEntry entry) {
        requireEDT();
        Objects.requireNonNull(entry, "entry cannot be null");

        if (active == entry) {
            return;
        }

        // Shut down old canvas/menu
        shutdownActiveCanvasAndMenu();

        // Show new card
        active = entry;
        cardLayout.show(cardPanel, entry.cardKey);

        // Stand up new canvas + install edit menu
        PlotCanvas canvas = entry.plotPanel.getPlotCanvas();
        if (canvas != null) {
            canvas.standUp();
            splotMenu = new SplotEditMenu(canvas);
            menuBar.add(splotMenu);
        } else {
            splotMenu = null;
        }

        menuBar.revalidate();
        menuBar.repaint();

        updateSaveEnabled();
    }

    private void showEmpty() {
        requireEDT();
        cardLayout.show(cardPanel, EMPTY_CARD);
        menuBar.revalidate();
        menuBar.repaint();
    }

    private void shutdownActiveCanvasAndMenu() {
        requireEDT();

        // Shut down old canvas if any
        if (active != null) {
            PlotCanvas old = active.plotPanel.getPlotCanvas();
            if (old != null) {
                old.shutDown();
            }
        }

        // Remove old edit menu if present
        if (splotMenu != null) {
            menuBar.remove(splotMenu);
            splotMenu = null;
        }
    }

    // ------------------------------------------------------------------------
    // File menu + save
    // ------------------------------------------------------------------------

    private void createFileMenu() {
        requireEDTIfPossible();

        fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        saveItem = new JMenuItem("Save");
        saveItem.addActionListener(e -> doSaveAsActivePlot());
        fileMenu.add(saveItem);
    }

    /**
     * Save the active plot (Save As... behavior).
     * <p>
     * This method shows a {@link JFileChooser} and writes the active {@link PlotCanvas}
     * via {@link PlotIO#save(PlotCanvas, File)}.
     * </p>
     */
    private void doSaveAsActivePlot() {
        requireEDT();

        PlotCanvas canvas = (active == null) ? null : active.plotPanel.getPlotCanvas();
        if (canvas == null) {
            JOptionPane.showMessageDialog(this, "No active plot to save.", "Save Plot Failed",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser(getInitialChooserDirectory());
        fc.setFileFilter(new PlotFileFilter());
        fc.setAcceptAllFileFilterUsed(true);

        int res = fc.showSaveDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File target = PlotFileFilter.ensurePlotExtension(fc.getSelectedFile());

        // Confirm overwrite if needed
        if (target.exists()) {
            int ok = JOptionPane.showConfirmDialog(this,
                    "Overwrite existing file?\n" + target.getAbsolutePath(),
                    "Confirm Save", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) {
                return;
            }
        }

        try {
            PlotIO.save(canvas, target);
            updateEnvironmentDataDirectory(target); // update once, after success
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Save Plot Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateSaveEnabled() {
        requireEDT();
        if (saveItem != null) {
            saveItem.setEnabled(active != null && active.plotPanel != null && active.plotPanel.getPlotCanvas() != null);
        }
    }

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
        if (chosenFileOrDir == null) {
            return;
        }
        File dir = chosenFileOrDir.isDirectory() ? chosenFileOrDir : chosenFileOrDir.getParentFile();
        if (dir != null && dir.exists() && dir.isDirectory()) {
            Environment.getInstance().setDataDirectory(dir.getAbsolutePath());
        }
    }

    // ------------------------------------------------------------------------
    // EDT helpers
    // ------------------------------------------------------------------------

    private static void runOnEDT(Runnable r) {
        if (EventQueue.isDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    private static void requireEDT() {
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException("Must be called on the Swing EDT");
        }
    }

    /**
     * Best-effort EDT assert for constructors (Swing sometimes constructs off-EDT in tests).
     * This does not throw; it exists only to keep intent obvious.
     */
    private static void requireEDTIfPossible() {
        // no-op by design
    }

    // ------------------------------------------------------------------------
    // Internal entry model
    // ------------------------------------------------------------------------

    /**
     * Internal record tying together a plot, its card key, and its menu item.
     */
    private static final class PlotEntry {
        final String title;
        final String cardKey;
        final PlotPanel plotPanel;
        final JRadioButtonMenuItem menuItem;

        PlotEntry(String title, String cardKey, PlotPanel plotPanel, JRadioButtonMenuItem menuItem) {
            this.title = title;
            this.cardKey = cardKey;
            this.plotPanel = plotPanel;
            this.menuItem = menuItem;
        }
    }
}
