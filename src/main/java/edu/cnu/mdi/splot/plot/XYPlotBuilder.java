package edu.cnu.mdi.splot.plot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import edu.cnu.mdi.splot.pdata.ACurve;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.pdata.PlotDataType;

/**
 * Fluent construction of ordinary MDI XY plots.
 *
 * <p>The builder is deliberately a convenience layer, not a second plotting
 * model. {@link #build()} creates the same {@link PlotData}, {@link Curve},
 * {@link PlotCanvas}, and {@link PlotPanel} objects that an application would
 * create directly. The resulting panel can therefore be placed in a
 * {@link PlotDeck}, and callers retain access to every existing sPlot API.</p>
 *
 * <p>Input arrays are defensively copied when a series is registered. A builder
 * may be reused; each build creates a fresh plot model and fresh curves.
 * Series style customizers receive the native {@link ACurve}, while plot
 * customizers receive the native {@link PlotParameters}. This keeps uncommon
 * or future MDI options accessible without continually expanding the builder.</p>
 *
 * <h2>Threading</h2>
 * <p>Build plots on Swing's event-dispatch thread when they will immediately
 * become part of a visible UI. The returned curves retain their normal
 * thread-safe data-appending behavior.</p>
 */
public final class XYPlotBuilder {

    private final String title;
    private String xLabel = "X Data";
    private String yLabel = "Y Data";
    private int decorations = PlotPanel.STANDARD;
    private final List<SeriesSpec> series = new ArrayList<>();
    private final List<Consumer<PlotParameters>> parameterCustomizers = new ArrayList<>();

    /**
     * Creates an empty XY plot definition.
     *
     * @param title plot title; must not be {@code null}
     */
    public XYPlotBuilder(String title) {
        this.title = Objects.requireNonNull(title, "title");
    }

    /**
     * Sets both axis labels.
     *
     * @param xLabel horizontal-axis label
     * @param yLabel vertical-axis label
     * @return this builder
     */
    public XYPlotBuilder axes(String xLabel, String yLabel) {
        this.xLabel = Objects.requireNonNull(xLabel, "xLabel");
        this.yLabel = Objects.requireNonNull(yLabel, "yLabel");
        return this;
    }

    /**
     * Sets the {@link PlotPanel} decoration level.
     *
     * @param decorations one of {@link PlotPanel#STANDARD}, {@link PlotPanel#BARE},
     *                    or {@link PlotPanel#VERYBARE}
     * @return this builder
     * @throws IllegalArgumentException for an unknown value
     */
    public XYPlotBuilder decorations(int decorations) {
        if (decorations != PlotPanel.STANDARD && decorations != PlotPanel.BARE
                && decorations != PlotPanel.VERYBARE) {
            throw new IllegalArgumentException("Unknown plot decoration level: " + decorations);
        }
        this.decorations = decorations;
        return this;
    }

    /**
     * Adds an uncustomized XY series.
     *
     * @param name legend name
     * @param x x coordinates
     * @param y y coordinates
     * @return this builder
     */
    public XYPlotBuilder series(String name, double[] x, double[] y) {
        return series(name, x, y, curve -> { });
    }

    /**
     * Adds an XY series with a native MDI curve customizer.
     *
     * <p>The customizer runs during each build after the copied data have been
     * installed and before the canvas is returned. Typical customizations set
     * line color, symbol, width, or drawing method.</p>
     *
     * @param name legend name
     * @param x x coordinates
     * @param y y coordinates
     * @param customizer curve style/drawing customizer
     * @return this builder
     * @throws IllegalArgumentException if array lengths differ
     */
    public XYPlotBuilder series(String name, double[] x, double[] y,
            Consumer<ACurve> customizer) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(x, "x");
        Objects.requireNonNull(y, "y");
        Objects.requireNonNull(customizer, "customizer");
        if (x.length != y.length) {
            throw new IllegalArgumentException("x and y lengths differ: " + x.length + " != " + y.length);
        }
        series.add(new SeriesSpec(name, x, y, customizer));
        return this;
    }

    /**
     * Adds a native plot-parameter customization applied during each build.
     * Multiple customizers run in registration order.
     *
     * @param customizer parameter customizer
     * @return this builder
     */
    public XYPlotBuilder configure(Consumer<PlotParameters> customizer) {
        parameterCustomizers.add(Objects.requireNonNull(customizer, "customizer"));
        return this;
    }

    /**
     * Creates a fresh native MDI plot panel.
     *
     * @return configured plot panel
     * @throws IllegalStateException if no series was registered or native plot
     *                               construction unexpectedly fails
     */
    public PlotPanel build() {
        if (series.isEmpty()) throw new IllegalStateException("At least one XY series is required");
        try {
            String[] names = series.stream().map(specification -> specification.name)
                    .toArray(String[]::new);
            PlotData data = new PlotData(PlotDataType.XYXY, names, null);
            for (int index = 0; index < series.size(); index++) {
                SeriesSpec specification = series.get(index);
                Curve curve = (Curve) data.getCurve(index);
                for (int point = 0; point < specification.x.length; point++) {
                    curve.xData().add(specification.x[point]);
                    curve.yData().add(specification.y[point]);
                }
                specification.customizer.accept(curve);
            }
            PlotCanvas canvas = new PlotCanvas(data, title, xLabel, yLabel);
            for (Consumer<PlotParameters> customizer : parameterCustomizers) {
                customizer.accept(canvas.getParameters());
            }
            return new PlotPanel(canvas, decorations);
        } catch (PlotDataException error) {
            throw new IllegalStateException("Could not create XY plot", error);
        }
    }

    private static final class SeriesSpec {
        private final String name;
        private final double[] x;
        private final double[] y;
        private final Consumer<ACurve> customizer;

        private SeriesSpec(String name, double[] x, double[] y, Consumer<ACurve> customizer) {
            this.name = name;
            this.x = Arrays.copyOf(x, x.length);
            this.y = Arrays.copyOf(y, y.length);
            this.customizer = customizer;
        }
    }
}
