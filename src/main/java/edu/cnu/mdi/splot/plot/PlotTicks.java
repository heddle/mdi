package edu.cnu.mdi.splot.plot;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.List;

import edu.cnu.mdi.graphics.GraphicsUtils;
import edu.cnu.mdi.splot.pdata.ACurve;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.HistoCurve;
import edu.cnu.mdi.splot.pdata.HistoData;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.util.UnicodeUtils;

/**
 * Draws the tick marks and tick labels on a {@link PlotCanvas}.
 *
 * <h2>Tick layout</h2>
 * <p>
 * Ticks are drawn on both the top and bottom edges of the active plot area
 * for the X axis, and on both the left and right edges for the Y axis, so
 * that the plot is framed on all four sides. Major ticks carry value labels;
 * minor ticks do not.
 * </p>
 * <p>
 * The number of interior major and minor ticks is configurable via the
 * {@code setNum*} methods. Log-scale axes draw one major tick per decade
 * plus minor ticks at 2–9 × the decade value; the {@code numMajorTick*}
 * setting is not used in log mode.
 * </p>
 *
 * <h2>Histogram mode</h2>
 * <p>
 * When the plot contains histogram data and {@link #setDrawBinValue(boolean)}
 * has been called with {@code true}, the X-axis labels show 1-based bin
 * indices rather than raw data values. This mode is automatically suppressed
 * if the underlying data is not a histogram.
 * </p>
 *
 * <h2>Bar plot mode</h2>
 * <p>
 * When the plot is a bar plot, the X axis shows the name of each data curve
 * centred on its bar group rather than numeric tick values.
 * </p>
 *
 * <h2>Graphics context</h2>
 * <p>
 * All drawing methods accept {@link Graphics2D} directly. No casts from
 * {@link java.awt.Graphics} are performed in this class. Callers that receive
 * a {@link java.awt.Graphics} from AWT (e.g. inside
 * {@link java.awt.Component#paintComponent}) should cast once at the call
 * site:
 * </p>
 * <pre>
 * &#64;Override
 * protected void paintComponent(Graphics g) {
 *     super.paintComponent(g);
 *     plotTicks.draw((Graphics2D) g);
 * }
 * </pre>
 */
public class PlotTicks {

    // -----------------------------------------------------------------------
    // Configuration
    // -----------------------------------------------------------------------

    /** Length of major ticks in pixels. */
    private int majorTickLen = 6;

    /** Length of minor ticks in pixels. */
    private int minorTickLen = 2;

    /** Number of interior major ticks on the X axis (linear mode). */
    private int numMajorTickX = 4;

    /** Number of interior minor ticks per major interval on the X axis. */
    private int numMinorTickX = 4;

    /** Number of interior major ticks on the Y axis (linear mode). */
    private int numMajorTickY = 4;

    /** Number of interior minor ticks per major interval on the Y axis. */
    private int numMinorTickY = 4;

    /**
     * If {@code true} and the data is a histogram, X-axis labels show the
     * 1-based bin index rather than the raw data value.
     */
    private boolean drawBinValue;

    /** Colour used for tick marks and tick labels. */
    private Color _tickColor = Color.black;

    /** Font used for tick labels. */
    private Font _tickFont = Fonts.plainFontDelta(-1);

    // -----------------------------------------------------------------------
    // Internal state
    // -----------------------------------------------------------------------

    /** The {@link PlotCanvas} this instance serves. */
    private final PlotCanvas _plotCanvas;

    /**
     * Reusable screen-coordinate point, avoiding per-tick allocation on the
     * drawing hot path.
     */
    private final Point _pp = new Point();

    /**
     * Reusable world-coordinate point, avoiding per-tick allocation on the
     * drawing hot path.
     */
    private final Point2D.Double _wp = new Point2D.Double();

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    /**
     * Construct a {@code PlotTicks} instance for the given canvas.
     *
     * @param plotCanvas the canvas this instance will draw ticks on; must not
     *                   be {@code null}
     */
    public PlotTicks(PlotCanvas plotCanvas) {
        _plotCanvas = plotCanvas;
    }

    // -----------------------------------------------------------------------
    // Configuration API
    // -----------------------------------------------------------------------

    /**
     * Override the font used for tick labels.
     *
     * @param font the replacement font; must not be {@code null}
     */
    public void setTickFont(Font font) {
        _tickFont = font;
    }

    /**
     * Enable or disable bin-index labels on the X axis.
     * <p>
     * When enabled, X-axis labels show the 1-based histogram bin index
     * instead of the raw data value. This setting is silently ignored if the
     * plot does not contain histogram data.
     * </p>
     *
     * @param drawBinVal {@code true} to show bin indices; {@code false} to
     *                   show raw values
     */
    public void setDrawBinValue(boolean drawBinVal) {
        if (_plotCanvas.getPlotData().isHistogramData()) {
            drawBinValue = drawBinVal;
        }
    }

    // -----------------------------------------------------------------------
    // Public drawing entry point
    // -----------------------------------------------------------------------

    /**
     * Draw all tick marks and tick labels onto the plot canvas.
     * <p>
     * This method draws major ticks (with labels) and then minor ticks
     * (without labels) for both axes. Log-scale axes draw their own minor
     * ticks (2–9 × decade) internally; the outer minor-tick loop is skipped
     * for log axes.
     * </p>
     * <p>
     * This method is a no-op if the active bounds or data world have not yet
     * been established on the canvas.
     * </p>
     *
     * @param g2 the graphics context
     */
    public void draw(Graphics2D g2) {
        Rectangle activeBounds = _plotCanvas.getActiveBounds();
        if (activeBounds == null) {
            return;
        }

        Rectangle.Double world = _plotCanvas.getDataWorld();
        if (world == null) {
            return;
        }

        g2.setColor(_tickColor);
        g2.setFont(_tickFont);

        double xmin = world.x;
        double ymin = world.y;
        double xmax = world.getMaxX();
        double ymax = world.getMaxY();

        boolean xLog = _plotCanvas.isXLogActive();
        boolean yLog = _plotCanvas.isYLogActive();

        // ---- Major ticks and labels ----------------------------------------

        if (xLog) {
            drawLogXTicks(g2, xmin, xmax, world.getCenterY(), activeBounds);
        } else if (_plotCanvas.isBarPlot()) {
            List<ACurve> curves = _plotCanvas.getPlotData().getCurves();
            drawBarPlotCategories(g2, curves, xmin, xmax,
                    world.getCenterY(), majorTickLen, curves.size(),
                    activeBounds);
        } else {
            drawXTicks(g2, xmin, xmax, world.getCenterY(),
                    majorTickLen, numMajorTickX, activeBounds, true);
        }

        if (yLog) {
            drawLogYTicks(g2, ymin, ymax, world.getCenterX(), activeBounds);
        } else {
            drawYTicks(g2, ymin, ymax, world.getCenterX(),
                    majorTickLen, numMajorTickY, activeBounds, true);
        }

        // ---- Minor ticks (linear axes only; log draws its own) -------------

        if (!xLog) {
            double delx = (xmax - xmin) / (numMajorTickX + 1);
            for (int i = 0; i <= numMajorTickX; i++) {
                double xxmin = xmin + i * delx;
                drawXTicks(g2, xxmin, xxmin + delx, world.getCenterY(),
                        minorTickLen, numMinorTickX, activeBounds, false);
            }
        }

        if (!yLog) {
            double dely = (ymax - ymin) / (numMajorTickY + 1);
            for (int i = 0; i <= numMajorTickY; i++) {
                double yymin = ymin + i * dely;
                drawYTicks(g2, yymin, yymin + dely, world.getCenterX(),
                        minorTickLen, numMinorTickY, activeBounds, false);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Private drawing helpers
    // -----------------------------------------------------------------------

    /**
     * Draw X-axis tick marks and, when {@code drawVal} is {@code true},
     * 1-based histogram bin-index labels.
     * <p>
     * This method is only called when {@link #drawBinValue} is {@code true}
     * and the underlying data is a histogram. Ticks are placed at the centres
     * of each bin rather than at evenly-spaced data values.
     * </p>
     *
     * @param g2      the graphics context
     * @param xmin    left bound of the tick range in data coordinates
     * @param xmax    right bound of the tick range in data coordinates
     * @param yc      Y data coordinate of the axis line (typically the centre
     *                of the data world)
     * @param ticklen tick length in pixels
     * @param numtick number of ticks to draw
     * @param ab      the active (plotted) bounds in screen coordinates
     * @param drawVal if {@code true}, draw bin-index labels below the ticks
     */
    private void drawBinValues(Graphics2D g2, double xmin, double xmax,
            double yc, int ticklen, int numtick, Rectangle ab,
            boolean drawVal) {

        PlotData plotData = _plotCanvas.getPlotData();
        if (!plotData.isHistogramData()) {
            return;
        }

        HistoCurve hc = (HistoCurve) plotData.getCurve(0);
        HistoData hd = hc.getHistoData();
        if (hd == null) {
            return;
        }

        FontMetrics fm = _plotCanvas.getFontMetrics(_tickFont);
        double delx = (xmax - xmin) / (numtick + 1);

        int t  = ab.y;
        int b  = t + ab.height;
        int sb = b + fm.getHeight();

        for (int i = 1; i <= (numtick + 1); i++) {
            double value = xmin + (i - 0.5) * delx;
            _wp.setLocation(value, yc);
            _plotCanvas.dataToScreen(_pp, _wp);
            g2.drawLine(_pp.x, b,          _pp.x, b - ticklen);
            g2.drawLine(_pp.x, t,          _pp.x, t + ticklen);

            if (drawVal) {
                int bin = hd.getBin(value) + 1;
                String valStr = String.valueOf(bin);
                int sw = fm.stringWidth(valStr);
                g2.drawString(valStr, _pp.x - sw / 2, sb);
            }
        }
    }

    /**
     * Draw logarithmic X-axis ticks and decade labels.
     * <p>
     * One major tick (with a superscript label of the form "10ⁿ") is drawn
     * at each power of ten within {@code [xmin, xmax]}. Minor ticks at
     * 2–9 × the decade value are drawn without labels.
     * </p>
     * <p>
     * This method is a no-op if either bound is non-positive.
     * </p>
     *
     * @param g2   the graphics context
     * @param xmin left bound of the axis in data coordinates (must be &gt; 0)
     * @param xmax right bound of the axis in data coordinates (must be &gt; 0)
     * @param yc   Y data coordinate of the axis line
     * @param ab   the active bounds in screen coordinates
     */
    private void drawLogXTicks(Graphics2D g2, double xmin, double xmax,
            double yc, Rectangle ab) {

        if (xmin <= 0 || xmax <= 0) {
            return;
        }

        FontMetrics fm = _plotCanvas.getFontMetrics(_tickFont);

        int t  = ab.y;
        int b  = t + ab.height;
        int sb = b + fm.getHeight();

        int n0 = (int) Math.ceil (Math.log10(xmin));
        int n1 = (int) Math.floor(Math.log10(xmax));

        for (int n = n0; n <= n1; n++) {
            double decade = Math.pow(10.0, n);

            // Major tick
            _wp.setLocation(decade, yc);
            _plotCanvas.dataToScreen(_pp, _wp);
            g2.drawLine(_pp.x, b, _pp.x, b - majorTickLen);
            g2.drawLine(_pp.x, t, _pp.x, t + majorTickLen);

            // Decade label (e.g. "10²")
            String label = "10" + UnicodeUtils.getSuperscript(n, n < 0);
            int sw = fm.stringWidth(label);
            g2.drawString(label, _pp.x - sw / 2, sb);

            // Minor ticks at 2×, 3×, … 9× the decade
            for (int m = 2; m <= 9; m++) {
                double v = m * decade;
                if (v < xmin || v > xmax) continue;
                _wp.setLocation(v, yc);
                _plotCanvas.dataToScreen(_pp, _wp);
                g2.drawLine(_pp.x, b, _pp.x, b - minorTickLen);
                g2.drawLine(_pp.x, t, _pp.x, t + minorTickLen);
            }
        }
    }

    /**
     * Draw logarithmic Y-axis ticks and rotated decade labels.
     * <p>
     * One major tick (with a rotated superscript label of the form "10ⁿ") is
     * drawn at each power of ten within {@code [ymin, ymax]}. Minor ticks at
     * 2–9 × the decade value are drawn without labels.
     * </p>
     * <p>
     * This method is a no-op if either bound is non-positive.
     * </p>
     *
     * @param g2   the graphics context
     * @param ymin bottom bound of the axis in data coordinates (must be &gt; 0)
     * @param ymax top bound of the axis in data coordinates (must be &gt; 0)
     * @param xc   X data coordinate of the axis line
     * @param ab   the active bounds in screen coordinates
     */
    private void drawLogYTicks(Graphics2D g2, double ymin, double ymax,
            double xc, Rectangle ab) {

        if (ymin <= 0 || ymax <= 0) {
            return;
        }

        FontMetrics fm = _plotCanvas.getFontMetrics(_tickFont);

        int l  = ab.x;
        int r  = l + ab.width;
        int sb = l - 6; // label position: a little left of the left axis edge

        int n0 = (int) Math.ceil (Math.log10(ymin));
        int n1 = (int) Math.floor(Math.log10(ymax));

        for (int n = n0; n <= n1; n++) {
            double decade = Math.pow(10.0, n);

            // Major tick
            _wp.setLocation(xc, decade);
            _plotCanvas.dataToScreen(_pp, _wp);
            g2.drawLine(l,          _pp.y, l + majorTickLen, _pp.y);
            g2.drawLine(r,          _pp.y, r - majorTickLen, _pp.y);

            // Rotated decade label
            String label = "10" + UnicodeUtils.getSuperscript(n, n < 0);
            int sw = fm.stringWidth(label);
            GraphicsUtils.drawRotatedText(g2, label, _tickFont,
                    sb, _pp.y + sw / 2, 0, 0, -90);

            // Minor ticks at 2×, 3×, … 9× the decade
            for (int m = 2; m <= 9; m++) {
                double v = m * decade;
                if (v < ymin || v > ymax) continue;
                _wp.setLocation(xc, v);
                _plotCanvas.dataToScreen(_pp, _wp);
                g2.drawLine(l, _pp.y, l + minorTickLen, _pp.y);
                g2.drawLine(r, _pp.y, r - minorTickLen, _pp.y);
            }
        }
    }

    /**
     * Draw bar-plot category labels on the X axis.
     * <p>
     * Instead of numeric ticks, each data curve contributes a centred name
     * label positioned at the horizontal centroid of its bar group. No tick
     * marks are drawn — only labels.
     * </p>
     * <p>
     * This method is a no-op if {@code curves} is {@code null} or empty.
     * </p>
     *
     * @param g2      the graphics context
     * @param curves  the list of data curves whose names are used as labels
     * @param xmin    left bound of the X axis in data coordinates (unused,
     *                retained for signature consistency)
     * @param xmax    right bound of the X axis in data coordinates (unused,
     *                retained for signature consistency)
     * @param yc      Y data coordinate of the axis line
     * @param ticklen tick length in pixels (unused, retained for signature
     *                consistency)
     * @param numtick number of ticks (unused, retained for signature
     *                consistency)
     * @param ab      the active bounds in screen coordinates
     */
    private void drawBarPlotCategories(Graphics2D g2, List<ACurve> curves,
            double xmin, double xmax, double yc,
            int ticklen, int numtick, Rectangle ab) {

        if (curves == null || curves.isEmpty()) {
            return;
        }

        g2.setFont(Fonts.tweenFont);
        FontMetrics fm = g2.getFontMetrics();

        int b  = ab.y + ab.height;
        int sb = b + fm.getHeight();

        for (ACurve acurve : curves) {
            Curve curve = (Curve) acurve;
            String name = acurve.name();
            Point2D.Double center = curve.getCentroid();
            _wp.setLocation(center.x, yc);
            _plotCanvas.dataToScreen(_pp, _wp);
            int sw = fm.stringWidth(name);
            g2.drawString(name, _pp.x - sw / 2, sb);
        }
    }

    /**
     * Draw linear X-axis tick marks and, optionally, value labels.
     * <p>
     * Ticks are drawn at {@code numtick} evenly-spaced positions within
     * {@code [xmin, xmax]}, on both the top and bottom edges of the active
     * plot area. When {@link #drawBinValue} is set, the call is forwarded to
     * {@link #drawBinValues} instead.
     * </p>
     *
     * @param g2      the graphics context
     * @param xmin    left bound of the tick range in data coordinates
     * @param xmax    right bound of the tick range in data coordinates
     * @param yc      Y data coordinate of the axis line
     * @param ticklen tick length in pixels
     * @param numtick number of ticks to place in the interior of
     *                {@code [xmin, xmax]}
     * @param ab      the active bounds in screen coordinates
     * @param drawVal if {@code true}, draw formatted value labels below the
     *                bottom ticks
     */
    private void drawXTicks(Graphics2D g2, double xmin, double xmax,
            double yc, int ticklen, int numtick, Rectangle ab,
            boolean drawVal) {

        if (numtick < 1) {
            return;
        }

        if (drawBinValue) {
            drawBinValues(g2, xmin, xmax, yc, ticklen, numtick, ab, drawVal);
            return;
        }

        PlotParameters params = _plotCanvas.getParameters();
        FontMetrics fm = _plotCanvas.getFontMetrics(_tickFont);
        g2.setFont(_tickFont);

        double delx = (xmax - xmin) / (numtick + 1);
        int t  = ab.y;
        int b  = t + ab.height;
        int sb = b + fm.getHeight();

        for (int i = 1; i <= numtick; i++) {
            double value = xmin + i * delx;
            _wp.setLocation(value, yc);
            _plotCanvas.dataToScreen(_pp, _wp);
            g2.drawLine(_pp.x, b, _pp.x, b - ticklen);
            g2.drawLine(_pp.x, t, _pp.x, t + ticklen);

            if (drawVal) {
                String valStr = DoubleFormat.doubleFormat(value,
                        params.getNumDecimalX(), params.getMinExponentX());
                int sw = fm.stringWidth(valStr);
                g2.drawString(valStr, _pp.x - sw / 2, sb);
            }
        }
    }

    /**
     * Draw linear Y-axis tick marks and, optionally, rotated value labels.
     * <p>
     * Ticks are drawn at {@code numtick} evenly-spaced positions within
     * {@code [ymin, ymax]}, on both the left and right edges of the active
     * plot area. Value labels are rotated −90° and drawn to the left of the
     * left edge.
     * </p>
     *
     * @param g2      the graphics context
     * @param ymin    bottom bound of the tick range in data coordinates
     * @param ymax    top bound of the tick range in data coordinates
     * @param xc      X data coordinate of the axis line
     * @param ticklen tick length in pixels
     * @param numtick number of ticks to place in the interior of
     *                {@code [ymin, ymax]}
     * @param ab      the active bounds in screen coordinates
     * @param drawVal if {@code true}, draw rotated formatted value labels to
     *                the left of the left-edge ticks
     */
    private void drawYTicks(Graphics2D g2, double ymin, double ymax,
            double xc, int ticklen, int numtick, Rectangle ab,
            boolean drawVal) {

        if (numtick < 1) {
            return;
        }

        PlotParameters params = _plotCanvas.getParameters();
        FontMetrics fm = _plotCanvas.getFontMetrics(_tickFont);
        g2.setFont(_tickFont);

        double dely = (ymax - ymin) / (numtick + 1);
        int l  = ab.x;
        int r  = l + ab.width;
        int sb = l - 4; // label position: a little left of the left axis edge

        for (int i = 1; i <= numtick; i++) {
            double value = ymin + i * dely;
            _wp.setLocation(xc, value);
            _plotCanvas.dataToScreen(_pp, _wp);
            g2.drawLine(l, _pp.y, l + ticklen, _pp.y);
            g2.drawLine(r, _pp.y, r - ticklen, _pp.y);

            if (drawVal) {
                String valStr = DoubleFormat.doubleFormat(value,
                        params.getNumDecimalY(), params.getMinExponentY());
                int sw = fm.stringWidth(valStr);
                GraphicsUtils.drawRotatedText(g2, valStr, _tickFont,
                        sb, _pp.y + sw / 2, 0, 0, -90);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /**
     * Returns the number of interior major ticks on the X axis (linear mode).
     *
     * @return number of major X ticks
     */
    public int getNumMajorTickX() { return numMajorTickX; }

    /**
     * Set the number of interior major ticks on the X axis (linear mode).
     *
     * @param numMajorTickX the desired count; must be ≥ 0
     */
    public void setNumMajorTickX(int numMajorTickX) {
        this.numMajorTickX = numMajorTickX;
    }

    /**
     * Returns the number of interior minor ticks per major interval on the
     * X axis.
     *
     * @return number of minor X ticks per interval
     */
    public int getNumMinorTickX() { return numMinorTickX; }

    /**
     * Set the number of interior minor ticks per major interval on the X axis.
     *
     * @param numMinorTickX the desired count; must be ≥ 0
     */
    public void setNumMinorTickX(int numMinorTickX) {
        this.numMinorTickX = numMinorTickX;
    }

    /**
     * Returns the number of interior major ticks on the Y axis (linear mode).
     *
     * @return number of major Y ticks
     */
    public int getNumMajorTickY() { return numMajorTickY; }

    /**
     * Set the number of interior major ticks on the Y axis (linear mode).
     *
     * @param numMajorTickY the desired count; must be ≥ 0
     */
    public void setNumMajorTickY(int numMajorTickY) {
        this.numMajorTickY = numMajorTickY;
    }

    /**
     * Returns the number of interior minor ticks per major interval on the
     * Y axis.
     *
     * @return number of minor Y ticks per interval
     */
    public int getNumMinorTickY() { return numMinorTickY; }

    /**
     * Set the number of interior minor ticks per major interval on the Y axis.
     *
     * @param numMinorTickY the desired count; must be ≥ 0
     */
    public void setNumMinorTickY(int numMinorTickY) {
        this.numMinorTickY = numMinorTickY;
    }

    /**
     * Returns the major tick length in pixels.
     *
     * @return major tick length
     */
    public int getMajorTickLen() { return majorTickLen; }

    /**
     * Set the major tick length in pixels.
     *
     * @param majorTickLen the desired length; must be ≥ 0
     */
    public void setMajorTickLen(int majorTickLen) {
        this.majorTickLen = majorTickLen;
    }

    /**
     * Returns the minor tick length in pixels.
     *
     * @return minor tick length
     */
    public int getMinorTickLen() { return minorTickLen; }

    /**
     * Set the minor tick length in pixels.
     *
     * @param minorTickLen the desired length; must be ≥ 0
     */
    public void setMinorTickLen(int minorTickLen) {
        this.minorTickLen = minorTickLen;
    }
}