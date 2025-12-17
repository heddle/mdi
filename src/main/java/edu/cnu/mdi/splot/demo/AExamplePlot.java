package edu.cnu.mdi.splot.demo;

import javax.swing.JComponent;

import edu.cnu.mdi.splot.model.Plot2D;
import edu.cnu.mdi.splot.view.Plot2DRenderer;
import edu.cnu.mdi.splot.view.PlotTheme;

/**
 * Base class for sample/demo plots.
 * <p>
 * Each example creates a {@link Plot2D} and configures a {@link Plot2DRenderer}.
 * The demo frame hosts the resulting component.
 * </p>
 */
public abstract class AExamplePlot {

    /** Unique id (stable; used for menus, persistence, etc.). */
    public abstract String getId();

    /** Human-readable name for menus. */
    public abstract String getDisplayName();

    /** Short description (optional; tooltips/status). */
    public String getDescription() {
        return "";
    }

    /**
     * Build the plot model for this example.
     */
    public abstract Plot2D buildPlot();

    /**
     * Configure renderer defaults for this example.
     * <p>
     * Override if you want different tick format, legend behavior, etc.
     * </p>
     */
    public Plot2DRenderer buildRenderer() {
        Plot2DRenderer r = new Plot2DRenderer();
        r.setTheme(new PlotTheme());
        r.setDrawLegend(true);
        return r;
    }

    /**
     * Optional: allow the example to provide a custom view component.
     * Default uses SPlotPanel.
     */
    public JComponent buildComponent() {
        Plot2D plot = buildPlot();
        Plot2DRenderer renderer = buildRenderer();
        return new edu.cnu.mdi.splot.view.SPlotPanel(plot, renderer);
    }

    @Override
    public final String toString() {
        return getDisplayName();
    }
}
