package edu.cnu.mdi.splot.view;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Objects;

import edu.cnu.mdi.splot.model.CurveSnapshot;
import edu.cnu.mdi.splot.model.FitResult;
import edu.cnu.mdi.splot.model.Plot2D;

/**
 * Renders a {@link Plot2D} into a screen rectangle.
 * <p>
 * Features:
 * <ul>
 *   <li>Auto margins (left margin sized from y tick label widths)</li>
 *   <li>Frame, axes, ticks on all sides (configurable via {@link PlotTheme})</li>
 *   <li>Optional major/minor grid lines</li>
 *   <li>Curves as lines, points, or both (style decided by a provider)</li>
 *   <li>Y error bars (if curve snapshot has sigmaY)</li>
 *   <li>Optional fit overlay (if curve has a {@link FitResult})</li>
 *   <li>Legend with draggable placement (hit test uses cached legend rect)</li>
 * </ul>
 */
public class Plot2DRenderer {

    // Cached geometry for interactions (legend drag, canvas transforms)
    private Rectangle2D lastLegendRect;
    private Rectangle lastPlotArea;

    // Tick generation
    private final NiceTickGenerator tickGen = new NiceTickGenerator(6, 5);

    // Base margins (pixels)
    private int minLeftMargin = 60;
    private int rightMargin = 18;
    private int topMargin = 18;
    private int bottomMargin = 50;

    // Auto-margin behavior
    private boolean autoLeftMargin = true;

    // Tick lengths (pixels)
    private int tickLenMajor = 6;
    private int tickLenMinor = 3;

    // Optional features
    private boolean drawLegend = true;
    private boolean drawFitOverlay = true;
    private int fitSamples = 250;

    // Theme (colors/fonts/strokes/flags)
    private PlotTheme theme = new PlotTheme();

    // Legend placement and drag state
    private LegendPlacement legendPlacement = new LegendPlacement();
    private boolean legendDragging = false;
    private double dragOffsetPxX = 0.0;
    private double dragOffsetPxY = 0.0;

 
    // Default: sigmaY curves drawn as points (with error bars), otherwise lines.
    private CurveStyleProvider styleProvider = c -> c.hasSigmaY() ? CurveStyle.points(7) : CurveStyle.lines();

    // ---------------- Configuration ----------------

    public void setTheme(PlotTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    public PlotTheme getTheme() {
        return theme;
    }

    public void setDrawLegend(boolean drawLegend) {
        this.drawLegend = drawLegend;
    }

    public void setDrawFitOverlay(boolean drawFitOverlay) {
        this.drawFitOverlay = drawFitOverlay;
    }

    /** If true, left margin is grown to fit y-axis labels. */
    public void setAutoLeftMargin(boolean autoLeftMargin) {
        this.autoLeftMargin = autoLeftMargin;
    }

    /** Minimum left margin in pixels (used even when auto-left-margin is enabled). */
    public void setMinLeftMargin(int minLeftMargin) {
        this.minLeftMargin = Math.max(10, minLeftMargin);
    }

    public void setFitSamples(int fitSamples) {
        this.fitSamples = Math.max(50, fitSamples);
    }

    public void setLegendPlacement(LegendPlacement placement) {
        this.legendPlacement = Objects.requireNonNull(placement, "placement");
    }

    public void setCurveStyleProvider(CurveStyleProvider provider) {
        this.styleProvider = (provider == null) ? (c -> c.hasSigmaY() ? CurveStyle.points(7) : CurveStyle.lines()) : provider;
    }

    // ---------------- Legend interaction API ----------------

    public boolean isLegendDragging() {
        return legendDragging;
    }

    public void endLegendDrag() {
        legendDragging = false;
    }

    public boolean legendHit(Point p) {
        return lastLegendRect != null && lastLegendRect.contains(p);
    }

    public void beginLegendDrag(Point mouse, LegendPlacement placement) {
        if (lastLegendRect == null || !lastLegendRect.contains(mouse)) {
            legendDragging = false;
            return;
        }
        dragOffsetPxX = mouse.x - lastLegendRect.getX();
        dragOffsetPxY = mouse.y - lastLegendRect.getY();
        legendDragging = true;
    }

    public void dragLegend(Point mouse, LegendPlacement placement) {
        if (!legendDragging || lastPlotArea == null || lastLegendRect == null) return;

        double nx = (mouse.x - dragOffsetPxX - lastPlotArea.x) / lastPlotArea.width;
        double ny = (mouse.y - dragOffsetPxY - lastPlotArea.y) / lastPlotArea.height;

        placement.anchorX = clamp(nx, 0.0, 1.0);
        placement.anchorY = clamp(ny, 0.0, 1.0);
    }

    /** Last plot area used during render; can be used by the canvas for transforms. */
    public Rectangle getLastPlotArea() {
        return lastPlotArea;
    }

    // ---------------- Rendering ----------------

    public void render(Graphics2D g, Plot2D plot, Rectangle2D screenBounds) {
        Objects.requireNonNull(g, "g");
        Objects.requireNonNull(plot, "plot");
        Objects.requireNonNull(screenBounds, "screenBounds");

        Rectangle2D.Double view = plot.getViewBounds();
        if (Double.isNaN(view.x) || Double.isNaN(view.y) || view.width <= 0 || view.height <= 0) {
            return;
        }

        // Build ticks first (needed for auto-left-margin)
        TickSet xTicks = tickGen.generate(view.x, view.x + view.width);
        TickSet yTicks = tickGen.generate(view.y, view.y + view.height);

        int effectiveLeft = computeLeftMarginPx(g, yTicks);
        Rectangle2D plotArea = getPlotAreaWithLeftMargin(screenBounds, effectiveLeft);

        lastPlotArea = new Rectangle(
                (int) Math.round(plotArea.getX()),
                (int) Math.round(plotArea.getY()),
                (int) Math.round(plotArea.getWidth()),
                (int) Math.round(plotArea.getHeight())
        );

        if (plotArea.getWidth() <= 5 || plotArea.getHeight() <= 5) return;

        // axes + grid + ticks + labels + frame
        drawAxesAndGrid(g, plot, plotArea, view, xTicks, yTicks);

        // Clip to plot area while drawing data
        Shape oldClip = g.getClip();
        g.setClip(plotArea);

        List<CurveSnapshot> snaps = plot.getCurves().stream().map(c -> c.snapshot()).toList();
        for (CurveSnapshot snap : snaps) {
            drawCurve(g, snap, view, plotArea);
            if (drawFitOverlay) {
                drawFit(g, snap.getFitResult(), view, plotArea);
            }
        }

        g.setClip(oldClip);

        if (drawLegend) {
            LegendModel legend = LegendModel.fromPlot(plot);
            drawLegend(g, legend, plotArea);
        }
    }

    /**
     * Fallback plot area computation using base margins (useful before first paint).
     * Prefer {@link #getLastPlotArea()} after at least one render.
     */
    public Rectangle getPlotArea(Rectangle componentBounds) {
        Rectangle2D pa = getPlotAreaWithLeftMargin(
                new Rectangle2D.Double(componentBounds.x, componentBounds.y, componentBounds.width, componentBounds.height),
                minLeftMargin
        );
        return new Rectangle((int) Math.round(pa.getX()),
                             (int) Math.round(pa.getY()),
                             (int) Math.round(pa.getWidth()),
                             (int) Math.round(pa.getHeight()));
    }

    // ---------------- Layout helpers ----------------

    private int computeLeftMarginPx(Graphics2D g, TickSet yTicks) {
        if (!autoLeftMargin) return minLeftMargin;

        Font oldFont = g.getFont();
        g.setFont(theme.getAxisFont());
        FontMetrics fm = g.getFontMetrics();

        int pad = theme.getTickLabelPadding();
        TickLabelFormatter fmt = theme.getTickLabelFormatter();

        int maxW = 0;
        for (Tick t : yTicks.getTicks()) {
            if (!t.isMajor()) continue;
            String lab = fmt.format(t.getValue());
            if (lab == null || lab.isEmpty()) continue;
            maxW = Math.max(maxW, fm.stringWidth(lab));
        }

        g.setFont(oldFont);

        // label width + tick length + padding + breathing room
        int needed = maxW + tickLenMajor + pad + 12;
        return Math.max(minLeftMargin, needed);
    }

    private Rectangle2D getPlotAreaWithLeftMargin(Rectangle2D screen, int left) {
        return new Rectangle2D.Double(
                screen.getX() + left,
                screen.getY() + topMargin,
                Math.max(0, screen.getWidth() - left - rightMargin),
                Math.max(0, screen.getHeight() - topMargin - bottomMargin)
        );
    }

    // ---------------- Axes / ticks / grid ----------------

    private void drawAxesAndGrid(Graphics2D g,
                                 Plot2D plot,
                                 Rectangle2D plotArea,
                                 Rectangle2D.Double view,
                                 TickSet xTicks,
                                 TickSet yTicks) {

        Stroke oldStroke = g.getStroke();
        Paint oldPaint = g.getPaint();
        Font oldFont = g.getFont();

        // grid first (behind data)
        if (theme.isDrawGrid()) {
            drawGrid(g, plotArea, view, xTicks, yTicks);
        }

        // frame
        if (theme.isDrawFrame()) {
            g.setPaint(theme.getAxisPaint());
            g.setStroke(theme.getFrameStroke());
            g.draw(new Rectangle2D.Double(plotArea.getX(), plotArea.getY(), plotArea.getWidth(), plotArea.getHeight()));
        }

        g.setFont(theme.getAxisFont());
        FontMetrics fm = g.getFontMetrics();

        boolean inward = theme.isInwardTicks();
        int pad = theme.getTickLabelPadding();
        TickLabelFormatter fmt = theme.getTickLabelFormatter();

        double x0 = plotArea.getX();
        double y0 = plotArea.getY();
        double x1 = plotArea.getMaxX();
        double y1 = plotArea.getMaxY();

        // ticks
        g.setPaint(theme.getTickPaint());
        g.setStroke(theme.getAxisStroke());

        // X ticks: bottom + optional top
        for (Tick t : xTicks.getTicks()) {
            double sx = mapX(t.getValue(), view, plotArea);
            int len = t.isMajor() ? tickLenMajor : tickLenMinor;

            // bottom tick
            double byA = y1;
            double byB = inward ? (y1 - len) : (y1 + len);
            g.draw(new Line2D.Double(sx, byA, sx, byB));

            // top tick
            if (theme.isDrawTopTicks()) {
                double tyA = y0;
                double tyB = inward ? (y0 + len) : (y0 - len);
                g.draw(new Line2D.Double(sx, tyA, sx, tyB));
            }

            // bottom labels (major only)
            if (t.isMajor()) {
                String lab = fmt.format(t.getValue());
                if (lab != null && !lab.isEmpty()) {
                    g.setPaint(theme.getLabelPaint());
                    int w = fm.stringWidth(lab);
                    g.drawString(lab,
                            (float) (sx - w / 2.0),
                            (float) (y1 + tickLenMajor + pad + fm.getAscent()));
                    g.setPaint(theme.getTickPaint());
                }
            }

            // top labels (optional)
            if (theme.isDrawTopTicks() && theme.isLabelTopTicks() && t.isMajor()) {
                String lab = fmt.format(t.getValue());
                if (lab != null && !lab.isEmpty()) {
                    g.setPaint(theme.getLabelPaint());
                    int w = fm.stringWidth(lab);
                    g.drawString(lab,
                            (float) (sx - w / 2.0),
                            (float) (y0 - pad));
                    g.setPaint(theme.getTickPaint());
                }
            }
        }

        // Y ticks: left + optional right
        for (Tick t : yTicks.getTicks()) {
            double sy = mapY(t.getValue(), view, plotArea);
            int len = t.isMajor() ? tickLenMajor : tickLenMinor;

            // left tick
            double lxA = x0;
            double lxB = inward ? (x0 + len) : (x0 - len);
            g.draw(new Line2D.Double(lxA, sy, lxB, sy));

            // right tick
            if (theme.isDrawRightTicks()) {
                double rxA = x1;
                double rxB = inward ? (x1 - len) : (x1 + len);
                g.draw(new Line2D.Double(rxA, sy, rxB, sy));
            }

            // left labels (major only)
            if (t.isMajor()) {
                String lab = fmt.format(t.getValue());
                if (lab != null && !lab.isEmpty()) {
                    g.setPaint(theme.getLabelPaint());
                    int w = fm.stringWidth(lab);
                    g.drawString(lab,
                            (float) (x0 - tickLenMajor - pad - w),
                            (float) (sy + fm.getAscent() / 2.5));
                    g.setPaint(theme.getTickPaint());
                }
            }

            // right labels (optional)
            if (theme.isDrawRightTicks() && theme.isLabelRightTicks() && t.isMajor()) {
                String lab = fmt.format(t.getValue());
                if (lab != null && !lab.isEmpty()) {
                    g.setPaint(theme.getLabelPaint());
                    g.drawString(lab,
                            (float) (x1 + tickLenMajor + pad),
                            (float) (sy + fm.getAscent() / 2.5));
                    g.setPaint(theme.getTickPaint());
                }
            }
        }

        // title (kept here; panel can also do it)
        if (!plot.getTitle().isEmpty()) {
            g.setFont(theme.getTitleFont());
            g.setPaint(theme.getLabelPaint());
            FontMetrics tfm = g.getFontMetrics();
            int w = tfm.stringWidth(plot.getTitle());
            g.drawString(plot.getTitle(),
                    (float) (plotArea.getCenterX() - w / 2.0),
                    (float) (plotArea.getY() - 6));
        }

        g.setFont(oldFont);
        g.setPaint(oldPaint);
        g.setStroke(oldStroke);
    }

    private void drawGrid(Graphics2D g,
                          Rectangle2D plotArea,
                          Rectangle2D.Double view,
                          TickSet xTicks,
                          TickSet yTicks) {

        Stroke oldStroke = g.getStroke();
        Paint oldPaint = g.getPaint();

        // Major grid
        g.setStroke(theme.getGridMajorStroke());
        g.setPaint(theme.getGridMajorPaint());
        for (Tick t : xTicks.getTicks()) {
            if (!t.isMajor()) continue;
            double sx = mapX(t.getValue(), view, plotArea);
            g.draw(new Line2D.Double(sx, plotArea.getY(), sx, plotArea.getMaxY()));
        }
        for (Tick t : yTicks.getTicks()) {
            if (!t.isMajor()) continue;
            double sy = mapY(t.getValue(), view, plotArea);
            g.draw(new Line2D.Double(plotArea.getX(), sy, plotArea.getMaxX(), sy));
        }

        // Minor grid
        if (theme.isDrawMinorGrid()) {
            g.setStroke(theme.getGridMinorStroke());
            g.setPaint(theme.getGridMinorPaint());
            for (Tick t : xTicks.getTicks()) {
                if (t.isMajor()) continue;
                double sx = mapX(t.getValue(), view, plotArea);
                g.draw(new Line2D.Double(sx, plotArea.getY(), sx, plotArea.getMaxY()));
            }
            for (Tick t : yTicks.getTicks()) {
                if (t.isMajor()) continue;
                double sy = mapY(t.getValue(), view, plotArea);
                g.draw(new Line2D.Double(plotArea.getX(), sy, plotArea.getMaxX(), sy));
            }
        }

        g.setPaint(oldPaint);
        g.setStroke(oldStroke);
    }

    // ---------------- Curves / error bars / fit ----------------

    private void drawCurve(Graphics2D g, CurveSnapshot c, Rectangle2D.Double view, Rectangle2D plotArea) {
        Stroke oldStroke = g.getStroke();
        Paint oldPaint = g.getPaint();

        // For now, use theme paints; you can later map per-curve paints if desired.
        g.setStroke(theme.getCurveStroke());
        g.setPaint(theme.getCurvePaint());

        CurveStyle style = styleProvider.styleFor(c);
        CurveDrawMode mode = style.getMode();
        int pt = style.getPointSize();

        boolean drawLines = (mode == CurveDrawMode.LINES || mode == CurveDrawMode.LINES_AND_POINTS);
        boolean drawPoints = (mode == CurveDrawMode.POINTS || mode == CurveDrawMode.LINES_AND_POINTS);

        int n = c.size();
        double lastSX = Double.NaN, lastSY = Double.NaN;
        boolean havePrev = false;

        for (int i = 0; i < n; i++) {
            if (!c.isValid(i)) {
                havePrev = false;
                continue;
            }

            double x = c.getX(i);
            double y = c.getY(i);
            if (Double.isNaN(x) || Double.isNaN(y)) {
                havePrev = false;
                continue;
            }

            double sx = mapX(x, view, plotArea);
            double sy = mapY(y, view, plotArea);

            // Lines
            if (drawLines) {
                if (havePrev) {
                    g.draw(new Line2D.Double(lastSX, lastSY, sx, sy));
                }
                lastSX = sx;
                lastSY = sy;
                havePrev = true;
            }

            // Error bars (Y only)
            if (c.hasSigmaY()) {
                double sY = c.getSigmaY(i);
                if (!Double.isNaN(sY) && sY > 0) {
                    double sy1 = mapY(y - sY, view, plotArea);
                    double sy2 = mapY(y + sY, view, plotArea);
                    g.draw(new Line2D.Double(sx, sy1, sx, sy2));
                    g.draw(new Line2D.Double(sx - 4, sy1, sx + 4, sy1));
                    g.draw(new Line2D.Double(sx - 4, sy2, sx + 4, sy2));
                }
            }

            // Points (filled square marker)
            if (drawPoints) {
                double r = pt / 2.0;
                g.fill(new Rectangle2D.Double(sx - r, sy - r, pt, pt));
            }
        }

        g.setPaint(oldPaint);
        g.setStroke(oldStroke);
    }

    private void drawFit(Graphics2D g, FitResult fit, Rectangle2D.Double view, Rectangle2D plotArea) {
        if (fit == null) return;

        double xMin = Math.max(view.x, fit.getXMin());
        double xMax = Math.min(view.x + view.width, fit.getXMax());
        if (!(xMax > xMin)) return;

        Stroke oldStroke = g.getStroke();
        Paint oldPaint = g.getPaint();

        g.setStroke(theme.getFitStroke());
        g.setPaint(theme.getFitPaint());

        double dx = (xMax - xMin) / (fitSamples - 1);

        double lastSX = Double.NaN, lastSY = Double.NaN;
        boolean havePrev = false;

        for (int i = 0; i < fitSamples; i++) {
            double x = xMin + i * dx;
            double y = fit.f(x);
            if (Double.isNaN(y) || Double.isInfinite(y)) {
                havePrev = false;
                continue;
            }
            double sx = mapX(x, view, plotArea);
            double sy = mapY(y, view, plotArea);

            if (havePrev) {
                g.draw(new Line2D.Double(lastSX, lastSY, sx, sy));
            }
            lastSX = sx;
            lastSY = sy;
            havePrev = true;
        }

        g.setPaint(oldPaint);
        g.setStroke(oldStroke);
    }

    // ---------------- Legend ----------------

    private void drawLegend(Graphics2D g, LegendModel legend, Rectangle2D plotArea) {
        Rectangle2D rect = computeLegendRect(g, legend, plotArea);
        if (rect == null) return;

        Font oldFont = g.getFont();
        Paint oldPaint = g.getPaint();

        g.setFont(theme.getLegendFont());

        if (theme.isFillLegendBackground()) {
            g.setPaint(theme.getLegendBackgroundPaint());
            g.fill(rect);
        }

        g.setPaint(theme.getLegendFramePaint());
        g.draw(rect);

        lastLegendRect = rect;

        FontMetrics fm = g.getFontMetrics();
        int lineH = fm.getHeight();
        int pad = 6;

        g.setPaint(theme.getLabelPaint());
        double tx = rect.getX() + pad;
        double ty = rect.getY() + pad + fm.getAscent();
        for (LegendEntry e : legend.getEntries()) {
            g.drawString(e.getSummary(), (float) tx, (float) ty);
            ty += lineH;
        }

        g.setFont(oldFont);
        g.setPaint(oldPaint);
    }

    private Rectangle2D computeLegendRect(Graphics2D g, LegendModel legend, Rectangle2D plotArea) {
        if (g == null) return null;

        Font oldFont = g.getFont();
        g.setFont(theme.getLegendFont());
        FontMetrics fm = g.getFontMetrics();

        int pad = 6;
        int lineH = fm.getHeight();

        int maxW = 0;
        for (LegendEntry e : legend.getEntries()) {
            maxW = Math.max(maxW, fm.stringWidth(e.getSummary()));
        }

        int w = maxW + 2 * pad;
        int h = legend.getEntries().size() * lineH + 2 * pad;

        // placement anchor normalized, interpreted as top-left corner
        double x = plotArea.getX() + legendPlacement.anchorX * plotArea.getWidth();
        double y = plotArea.getY() + legendPlacement.anchorY * plotArea.getHeight();

        x = clamp(x, plotArea.getX(), plotArea.getMaxX() - w);
        y = clamp(y, plotArea.getY(), plotArea.getMaxY() - h);

        g.setFont(oldFont);
        return new Rectangle2D.Double(x, y, w, h);
    }

    // ---------------- Mapping helpers ----------------

    private static double mapX(double x, Rectangle2D.Double view, Rectangle2D plotArea) {
        double t = (x - view.x) / view.width;
        return plotArea.getX() + t * plotArea.getWidth();
    }

    private static double mapY(double y, Rectangle2D.Double view, Rectangle2D plotArea) {
        double t = (y - view.y) / view.height;
        return plotArea.getMaxY() - t * plotArea.getHeight();
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
