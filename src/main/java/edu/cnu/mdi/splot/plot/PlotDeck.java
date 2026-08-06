package edu.cnu.mdi.splot.plot;

import java.util.Objects;

/**
 * A uniform menu-bearing host for one or more {@link PlotPanel}s.
 *
 * <p>{@code PlotDeck} removes the application-level distinction between a
 * single plot and a gallery. With one plot it shows the plot's standard edit
 * menu and, optionally, the File menu while suppressing the redundant Gallery
 * menu. As soon as a second plot is added, Gallery appears automatically and
 * provides card selection. Removing plots updates that behavior in reverse.</p>
 *
 * <p>The deck inherits {@link MultiplotPanel}'s EDT marshalling, active-canvas
 * lifecycle, Save behavior, and menu rebuilding. Consequently a plot hosted in
 * a deck receives the same menus whether an application has one diagnostic or
 * several.</p>
 */
@SuppressWarnings("serial")
public final class PlotDeck extends MultiplotPanel {

    /** Creates an empty deck with a File/Save menu. */
    public PlotDeck() {
        this(true);
    }

    /**
     * Creates an empty deck.
     *
     * @param includeFileMenu whether to include File/Save for the active plot
     */
    public PlotDeck(boolean includeFileMenu) {
        super(includeFileMenu, false);
    }

    /**
     * Creates a deck containing one plot and a File/Save menu.
     *
     * @param title logical plot title; used if Gallery later becomes visible
     * @param plot plot to host
     */
    public PlotDeck(String title, PlotPanel plot) {
        this(true);
        addPlot(title, plot);
    }

    /**
     * Adds a plot and returns this deck for fluent construction.
     *
     * @param title logical plot title
     * @param plot plot panel to add
     * @return this deck
     * @throws NullPointerException if either argument is null
     */
    public PlotDeck add(String title, PlotPanel plot) {
        addPlot(Objects.requireNonNull(title, "title"), Objects.requireNonNull(plot, "plot"));
        return this;
    }
}
