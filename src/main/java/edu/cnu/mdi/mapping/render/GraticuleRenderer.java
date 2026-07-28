package edu.cnu.mdi.mapping.render;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.mapping.container.MapContainer;
import edu.cnu.mdi.mapping.projection.IMapProjection;
import edu.cnu.mdi.mapping.theme.MapTheme;

/**
 * Renders a map outline and latitude/longitude graticule lines for a given
 * {@link IMapProjection}, with optional <em>zoom-adaptive</em> line spacing.
 *
 * <h2>Adaptive spacing</h2>
 * <p>
 * In adaptive mode (the default), the graticule step is chosen from the
 * geographic span currently visible in the viewport so that a roughly constant
 * number of lines is shown regardless of zoom. The ideal step is snapped to a
 * "nice" angular value drawn from {@link #NICE_STEPS_RAD} (…30°, 15°, 10°, 5°,
 * 2°, 1°, 30′, 15′, …) so that line positions and any labels remain on clean
 * round values as the user zooms.
 * </p>
 * <p>
 * The visible span is measured empirically: the device viewport corners and
 * edge midpoints are inverse-projected back to (λ, φ) via
 * {@link MapContainer#localToLatLon(Point, Point2D.Double)}. Because this uses
 * the projection's own inverse, it is correct for every projection — including
 * the azimuthal ones (orthographic, Lambert) where a viewport corner may fall
 * off the projected disk and invert to {@code NaN}. Such samples are skipped;
 * if too few valid samples remain (e.g. the whole globe is in view), the code
 * falls back to the projection's full {@link IMapProjection#getXYBounds()}
 * domain and the configured fixed step.
 * </p>
 *
 * <h2>Fixed spacing</h2>
 * <p>
 * When {@link #isAdaptive()} is {@code false}, the renderer behaves exactly as
 * before: it iterates the whole graticule using {@link #getLatitudeStepRad()}
 * and {@link #getLongitudeStepRad()}. Those setters remain the source of the
 * step in fixed mode and the <em>fallback</em> step in adaptive mode.
 * </p>
 *
 * <h2>Rendering</h2>
 * <p>
 * Actual drawing is still fully delegated to the projection's
 * {@link IMapProjection#drawLatitudeLine} /
 * {@link IMapProjection#drawLongitudeLine}, so graticule color, stroke, and
 * curve shape continue to match the projection and its {@link MapTheme}. This
 * class only decides <em>which</em> parallels and meridians to draw, and over
 * what latitude/longitude extent.
 * </p>
 */
public final class GraticuleRenderer {
	
	/**
	 * Optional coordinate-label color override.
	 *
	 * <p>
	 * When null, the active map theme supplies the label color.
	 * </p>
	 */
	private Color customLabelColor;

    /**
     * Target number of graticule lines to display along each axis. The
     * adaptive step is chosen so the visible span divided by the step is near
     * this value. Around 6–10 reads well without clutter.
     */
    private static final int TARGET_LINE_COUNT = 8;

    /**
     * Hard ceiling on iterations per axis. Guards against a degenerate visible
     * span (or a tiny step from an ill-conditioned inverse) producing an
     * unbounded loop. If a computed step would exceed this many lines over the
     * span, drawing for that axis is skipped rather than stalling the EDT.
     */
    private static final int MAX_LINES_PER_AXIS = 400;

    /** Inset (px) of edge labels from the viewport margin. */
    private static final int LABEL_MARGIN_PX = 4;

    /**
     * Minimum pixel gap between two labels on the same edge before the later
     * one is suppressed, preventing overlapping text when lines bunch up near
     * the poles or projection seams.
     */
    private static final int LABEL_MIN_GAP_PX = 28;

    /**
     * "Nice" angular steps in radians, descending. Mirrors a standard
     * map-tick ladder: 30°, 15°, 10°, 5°, 2°, 1°, then 30′, 15′, 10′, 5′, 2′,
     * 1′, then 30″, 15″, 10″, 5″, 2″, 1″. The adaptive selector picks the
     * smallest entry whose value is ≥ the ideal step, so the on-screen count
     * never exceeds the target.
     */
    private static final double[] NICE_STEPS_RAD = buildNiceSteps();

    private static double[] buildNiceSteps() {
        double deg = Math.PI / 180.0;
        double min = deg / 60.0;
        double sec = min / 60.0;
        double[] base = { 30, 15, 10, 5, 2, 1 };
        double[] out = new double[base.length * 3];
        int i = 0;
        for (double b : base) out[i++] = b * deg; // degrees
        for (double b : base) out[i++] = b * min; // arcminutes
        for (double b : base) out[i++] = b * sec; // arcseconds
        return out;
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** The projection to which graticule drawing is delegated. */
    private final IMapProjection projection;

    /** Fixed-mode step / adaptive fallback step for parallels, in radians. */
    private double latitudeStepRad = Math.toRadians(15.0);

    /** Fixed-mode step / adaptive fallback step for meridians, in radians. */
    private double longitudeStepRad = Math.toRadians(15.0);

    /** Whether the map outline is drawn before the graticule lines. */
    private boolean drawOutline = true;

    /** Whether spacing adapts to the current zoom/viewport. Default true. */
    private boolean adaptive = true;

    /** Whether coordinate labels are drawn along the viewport edges. Default true. */
    private boolean drawLabels = true;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a graticule renderer backed by the given projection.
     *
     * @param projection the projection to use for outline and line drawing;
     *                   must not be {@code null}
     * @throws IllegalArgumentException if {@code projection} is {@code null}
     */
    public GraticuleRenderer(IMapProjection projection) {
        if (projection == null) {
            throw new IllegalArgumentException("projection must not be null");
        }
        this.projection = projection;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the associated projection.
     *
     * @return the projection used by this renderer; never {@code null}
     */
    public IMapProjection getProjection() { return projection; }

    /**
     * Returns the latitude step size in radians (fixed mode / adaptive
     * fallback).
     *
     * @return latitude step in radians (&gt; 0)
     */
    public double getLatitudeStepRad() { return latitudeStepRad; }

    /**
     * Sets the latitude step size in radians.
     *
     * @param latitudeStepRad step size in radians; must be &gt; 0
     * @throws IllegalArgumentException if {@code latitudeStepRad} &le; 0
     */
    public void setLatitudeStepRad(double latitudeStepRad) {
        if (latitudeStepRad <= 0.0) {
            throw new IllegalArgumentException("latitudeStepRad must be > 0");
        }
        this.latitudeStepRad = latitudeStepRad;
    }

    /**
     * Convenience method that sets the latitude step size in degrees.
     *
     * @param latitudeStepDeg step size in degrees; must be &gt; 0
     * @throws IllegalArgumentException if {@code latitudeStepDeg} &le; 0
     */
    public void setLatitudeStepDeg(double latitudeStepDeg) {
        setLatitudeStepRad(Math.toRadians(latitudeStepDeg));
    }

    /**
     * Returns the longitude step size in radians (fixed mode / adaptive
     * fallback).
     *
     * @return longitude step in radians (&gt; 0)
     */
    public double getLongitudeStepRad() { return longitudeStepRad; }

    /**
     * Sets the longitude step size in radians.
     *
     * @param longitudeStepRad step size in radians; must be &gt; 0
     * @throws IllegalArgumentException if {@code longitudeStepRad} &le; 0
     */
    public void setLongitudeStepRad(double longitudeStepRad) {
        if (longitudeStepRad <= 0.0) {
            throw new IllegalArgumentException("longitudeStepRad must be > 0");
        }
        this.longitudeStepRad = longitudeStepRad;
    }

    /**
     * Convenience method that sets the longitude step size in degrees.
     *
     * @param longitudeStepDeg step size in degrees; must be &gt; 0
     * @throws IllegalArgumentException if {@code longitudeStepDeg} &le; 0
     */
    public void setLongitudeStepDeg(double longitudeStepDeg) {
        setLongitudeStepRad(Math.toRadians(longitudeStepDeg));
    }

    /**
     * Returns whether the map outline is drawn before the graticule lines.
     *
     * @return {@code true} if the outline will be drawn
     */
    public boolean isDrawOutline() { return drawOutline; }

    /**
     * Sets whether the map outline is drawn before the graticule lines.
     *
     * @param drawOutline {@code true} to draw the outline; {@code false} to
     *                    suppress it
     */
    public void setDrawOutline(boolean drawOutline) { this.drawOutline = drawOutline; }

    /**
     * Returns whether graticule spacing adapts to the current zoom level.
     *
     * @return {@code true} if adaptive spacing is enabled
     */
    public boolean isAdaptive() { return adaptive; }

    /**
     * Enables or disables zoom-adaptive spacing. When disabled, the fixed
     * {@link #getLatitudeStepRad()} / {@link #getLongitudeStepRad()} are used
     * across the full graticule, reproducing the original behavior.
     *
     * @param adaptive {@code true} to adapt spacing to zoom; {@code false} for
     *                 fixed spacing
     */
    public void setAdaptive(boolean adaptive) { this.adaptive = adaptive; }

    /**
     * Returns whether coordinate labels are drawn along the viewport edges.
     *
     * @return {@code true} if edge labels are enabled
     */
    public boolean isDrawLabels() { return drawLabels; }

    /**
     * Enables or disables coordinate labels along the viewport edges (e.g.
     * "39°N", "132°E"). Labels are placed where each parallel/meridian enters
     * the viewport and are only available in adaptive mode (they require the
     * inverse + viewport that {@link MapContainer} supplies).
     *
     * @param drawLabels {@code true} to draw edge labels
     */
    public void setDrawLabels(boolean drawLabels) { this.drawLabels = drawLabels; }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    /**
     * Renders the map outline (if enabled) followed by all graticule lines.
     *
     * <p>In fixed mode, parallels are drawn for φ ∈ [-π/2, +π/2] and meridians
     * for λ ∈ [-π, +π] at the configured step. In adaptive mode, the visible
     * (λ, φ) extent and a nice step are computed from the viewport, and only
     * lines intersecting that extent are drawn (padded by one step so lines
     * never pop in at the edges).</p>
     *
     * @param g2        graphics context to draw into; must not be {@code null}
     * @param container container providing the world-to-local transform and,
     *                  when adaptive, the inverse and viewport; must not be
     *                  {@code null}
     */
    public void render(Graphics2D g2, IContainer container) {
        if (drawOutline) {
            projection.drawMapOutline(g2, container);
        }

        GeoExtent extent = adaptive ? computeVisibleExtent(container) : null;
        if (extent == null) {
            renderFixed(g2, container);
        } else {
            renderAdaptive(g2, container, extent);
        }
    }

    // -------------------------------------------------------------------------
    // Fixed-step rendering (original behavior)
    // -------------------------------------------------------------------------

    private void renderFixed(Graphics2D g2, IContainer container) {
        double latMin = -Math.PI / 2.0;
        double latMax =  Math.PI / 2.0;
        for (double phi = latMin; phi <= latMax + 1e-9; phi += latitudeStepRad) {
            projection.drawLatitudeLine(g2, container, phi);
        }
        double lonMin = -Math.PI;
        double lonMax =  Math.PI;
        for (double lambda = lonMin; lambda <= lonMax + 1e-9; lambda += longitudeStepRad) {
            projection.drawLongitudeLine(g2, container, lambda);
        }
    }

    // -------------------------------------------------------------------------
    // Adaptive rendering
    // -------------------------------------------------------------------------

    private void renderAdaptive(Graphics2D g2, IContainer container, GeoExtent e) {
        double latSpan = e.latMax - e.latMin;
        double lonSpan = e.lonMax - e.lonMin;

        double latStep = niceStep(latSpan / TARGET_LINE_COUNT, latitudeStepRad);
        double lonStep = niceStep(lonSpan / TARGET_LINE_COUNT, longitudeStepRad);

        // Parallels: snap the lower bound down to a multiple of the step, pad
        // by one step on each side, clamp to the valid latitude range.
        double latLo = Math.max(-Math.PI / 2.0, floorToStep(e.latMin, latStep) - latStep);
        double latHi = Math.min( Math.PI / 2.0, e.latMax + latStep);
        if (countLines(latLo, latHi, latStep) <= MAX_LINES_PER_AXIS) {
            for (double phi = latLo; phi <= latHi + 1e-9; phi += latStep) {
                projection.drawLatitudeLine(g2, container, phi);
            }
        }

        // Meridians: do NOT clamp to [-π, π] here, because the visible window
        // can straddle the seam (e.g. lonMin = 170°, lonMax = -170°). The
        // extent computation already widened the window across the seam, so we
        // iterate the raw [lonMin, lonMax] band and let drawLongitudeLine /
        // wrapLongitude resolve each meridian to its canonical position.
        double lonLo = floorToStep(e.lonMin, lonStep) - lonStep;
        double lonHi = e.lonMax + lonStep;
        if (countLines(lonLo, lonHi, lonStep) <= MAX_LINES_PER_AXIS) {
            for (double lambda = lonLo; lambda <= lonHi + 1e-9; lambda += lonStep) {
                projection.drawLongitudeLine(g2, container, projection.wrapLongitude(lambda));
            }
        }

        if (drawLabels && container instanceof MapContainer) {
            drawEdgeLabels((MapContainer) container, g2, e,
                    latLo, latHi, latStep, lonLo, lonHi, lonStep);
        }
    }

    // -------------------------------------------------------------------------
    // Visible-extent computation
    // -------------------------------------------------------------------------

    /**
     * Inverse-projects a ring of viewport sample points to find the visible
     * geographic extent. Returns {@code null} (→ caller falls back to fixed
     * spacing over the full domain) when the container cannot supply an
     * inverse + viewport, or when too few samples invert validly to trust the
     * extent (typically means most of the disk/globe is on screen).
     */
    private GeoExtent computeVisibleExtent(IContainer container) {
        if (!(container instanceof MapContainer)) {
            return null; // no inverse available through this container type
        }
        MapContainer mc = (MapContainer) container;
        Component comp = mc.getComponent();
        if (comp == null) {
            return null;
        }
        int w = comp.getWidth();
        int h = comp.getHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }

        // A 3x3 grid of sample points (corners, edge mids, center). More than
        // four samples makes the extent robust when only part of the viewport
        // covers the projected domain (azimuthal projections).
        int[][] frac = {
            {0, 0}, {1, 0}, {2, 0},
            {0, 1}, {1, 1}, {2, 1},
            {0, 2}, {1, 2}, {2, 2}
        };

        double latMin = Double.POSITIVE_INFINITY, latMax = Double.NEGATIVE_INFINITY;
        double lonMin = Double.POSITIVE_INFINITY, lonMax = Double.NEGATIVE_INFINITY;
        Point2D.Double ll = new Point2D.Double();
        Point p = new Point();
        int valid = 0;
        boolean seamWrap = false;
        double prevLon = Double.NaN;

        for (int[] f : frac) {
            p.x = (int) Math.round(f[0] * (w / 2.0));
            p.y = (int) Math.round(f[1] * (h / 2.0));
            mc.localToLatLon(p, ll);
            double lon = ll.x, lat = ll.y;
            if (Double.isNaN(lon) || Double.isNaN(lat)
                    || Double.isInfinite(lon) || Double.isInfinite(lat)) {
                continue; // sample fell outside the projected domain
            }
            valid++;
            if (lat < latMin) latMin = lat;
            if (lat > latMax) latMax = lat;
            if (lon < lonMin) lonMin = lon;
            if (lon > lonMax) lonMax = lon;
            if (!Double.isNaN(prevLon) && Math.abs(lon - prevLon) > Math.PI) {
                seamWrap = true; // adjacent samples jumped >180° → straddles seam
            }
            prevLon = lon;
        }

        // Need enough valid corners to trust the window; otherwise the whole
        // globe/disk is effectively visible and fixed spacing is appropriate.
        if (valid < 6) {
            return null;
        }

        // If the window straddles the antimeridian seam, the naive min/max
        // spans nearly the whole globe. Re-express longitudes in [0, 2π) so the
        // band is contiguous, then keep the narrower of the two interpretations.
        if (seamWrap && (lonMax - lonMin) > Math.PI) {
            double lo = Double.POSITIVE_INFINITY, hi = Double.NEGATIVE_INFINITY;
            for (int[] f : frac) {
                p.x = (int) Math.round(f[0] * (w / 2.0));
                p.y = (int) Math.round(f[1] * (h / 2.0));
                mc.localToLatLon(p, ll);
                if (Double.isNaN(ll.x)) continue;
                double lon = ll.x < 0 ? ll.x + 2 * Math.PI : ll.x;
                if (lon < lo) lo = lon;
                if (lon > hi) hi = lon;
            }
            if (hi - lo < lonMax - lonMin) {
                lonMin = lo;
                lonMax = hi; // may exceed π; meridian loop wraps each value
            }
        }

        // Degenerate guard: a near-zero span (extreme zoom) still needs a
        // drawable window. Give it at least the smallest nice step.
        double minSpan = NICE_STEPS_RAD[NICE_STEPS_RAD.length - 1];
        if (latMax - latMin < minSpan) { latMax += minSpan; latMin -= minSpan; }
        if (lonMax - lonMin < minSpan) { lonMax += minSpan; lonMin -= minSpan; }

        return new GeoExtent(latMin, latMax, lonMin, lonMax);
    }

    // -------------------------------------------------------------------------
    // Nice-number helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the smallest entry of {@link #NICE_STEPS_RAD} that is ≥
     * {@code ideal}. If {@code ideal} is larger than the coarsest nice step,
     * returns {@code fallback} (so heavily zoomed-out views keep their
     * configured coarse spacing instead of collapsing to a single line).
     */
    private static double niceStep(double ideal, double fallback) {
        if (ideal <= 0 || Double.isNaN(ideal)) {
            return fallback;
        }
        double chosen = Double.NaN;
        for (double s : NICE_STEPS_RAD) { // descending
            if (s >= ideal) {
                chosen = s;
            } else {
                break;
            }
        }
        return Double.isNaN(chosen) ? fallback : chosen;
    }

    private static double floorToStep(double value, double step) {
        return Math.floor(value / step) * step;
    }

    private static int countLines(double lo, double hi, double step) {
        return (int) Math.floor((hi - lo) / step) + 1;
    }

    // -------------------------------------------------------------------------
    // Edge labels
    // -------------------------------------------------------------------------

    /**
     * Draws coordinate labels where each parallel/meridian enters the viewport.
     *
     * <p>For a parallel, longitude is walked across the visible band and each
     * sample is forward-projected; the first sample landing inside the viewport
     * marks the entry point, and a "39°N" label is placed clamped to the left
     * margin at that y. Meridians are handled symmetrically along the bottom
     * margin. Because the actual projected curve is sampled (not assumed
     * straight), this is correct for curved graticules — Mollweide arcs,
     * orthographic meridians fanning from the pole, etc.</p>
     */
    private void drawEdgeLabels(MapContainer mc, Graphics2D g2, GeoExtent e,
                                double latLo, double latHi, double latStep,
                                double lonLo, double lonHi, double lonStep) {
        Component comp = mc.getComponent();
        if (comp == null) return;
        Rectangle view = new Rectangle(0, 0, comp.getWidth(), comp.getHeight());
        if (view.width <= 0 || view.height <= 0) return;

        Color oldColor = g2.getColor();
        Font oldFont = g2.getFont();

        g2.setColor(getLabelColor());
        g2.setFont(oldFont.deriveFont(Font.PLAIN, 11f));
        FontMetrics fm = g2.getFontMetrics();
        int ascent = fm.getAscent();

        int latDecimals = stepDecimals(latStep);
        int lonDecimals = stepDecimals(lonStep);

        // Parallels → labels on the left edge, ordered top-to-bottom.
        List<Integer> usedY = new ArrayList<>();
        for (double phi = latLo; phi <= latHi + 1e-9; phi += latStep) {
            Point entry = lineEntryAlongLongitude(mc, phi, e.lonMin, e.lonMax, view);
            if (entry == null) continue;
            if (tooClose(usedY, entry.y)) continue;
            usedY.add(entry.y);
            String text = formatLat(phi, latDecimals);
            int y = clamp(entry.y, ascent + LABEL_MARGIN_PX, view.height - LABEL_MARGIN_PX);
            g2.drawString(text, LABEL_MARGIN_PX, y);
        }

        // Meridians → labels on the bottom edge, ordered left-to-right.
        List<Integer> usedX = new ArrayList<>();
        for (double lambda = lonLo; lambda <= lonHi + 1e-9; lambda += lonStep) {
            double lon = projection.wrapLongitude(lambda);
            Point entry = lineEntryAlongLatitude(mc, lon, e.latMin, e.latMax, view);
            if (entry == null) continue;
            if (tooClose(usedX, entry.x)) continue;
            usedX.add(entry.x);
            String text = formatLon(lon, lonDecimals);
            int w = fm.stringWidth(text);
            int x = clamp(entry.x - w / 2, LABEL_MARGIN_PX, view.width - w - LABEL_MARGIN_PX);
            g2.drawString(text, x, view.height - LABEL_MARGIN_PX);
        }

        g2.setColor(oldColor);
        g2.setFont(oldFont);
    }
    
    /**
     * Copies user-configurable settings from another graticule renderer.
     *
     * <p>
     * The projection itself is deliberately not copied.
     * </p>
     *
     * @param source source renderer
     */
    public void copyStyleFrom(GraticuleRenderer source) {
        if (source == null) {
            return;
        }

        latitudeStepRad = source.latitudeStepRad;
        longitudeStepRad = source.longitudeStepRad;

        drawOutline = source.drawOutline;
        adaptive = source.adaptive;
        drawLabels = source.drawLabels;

        customLabelColor = source.customLabelColor;
    }

    /**
     * Walks longitude across [lonMin, lonMax] at the given fixed latitude,
     * forward-projecting each sample, and returns the first screen point that
     * falls inside {@code view} — i.e. where this parallel enters the viewport.
     * Returns {@code null} if the parallel never crosses the viewport (e.g. it
     * is on the far side of an orthographic globe).
     */
    private Point lineEntryAlongLongitude(MapContainer mc, double lat,
                                          double lonMin, double lonMax, Rectangle view) {
        int samples = 96;
        Point2D.Double ll = new Point2D.Double();
        Point sp = new Point();
        for (int i = 0; i <= samples; i++) {
            double lon = lonMin + (lonMax - lonMin) * (i / (double) samples);
            ll.x = projection.wrapLongitude(lon);
            ll.y = lat;
            if (!projection.isPointVisible(ll)) continue;
            mc.latLonToLocal(sp, ll);
            if (view.contains(sp.x, sp.y)) {
                return new Point(sp.x, sp.y);
            }
        }
        return null;
    }
    
    /**
     * Returns the effective coordinate-label color.
     *
     * @return custom color when set; otherwise the theme label or graticule color
     */
    public Color getLabelColor() {
        if (customLabelColor != null) {
            return customLabelColor;
        }

        MapTheme theme = projection.getTheme();

        Color color = theme.getLabelColor();
        return (color != null)
                ? color
                : theme.getGraticuleColor();
    }

    /**
     * Sets a custom coordinate-label color.
     *
     * @param color custom color, or null to use the theme
     */
    public void setLabelColor(Color color) {
        customLabelColor = color;
    }

    /**
     * Restores theme-controlled coordinate-label coloring.
     */
    public void useThemeLabelColor() {
        customLabelColor = null;
    }

    /**
     * Walks latitude across [latMin, latMax] at the given fixed longitude and
     * returns the first screen point inside {@code view} — where this meridian
     * enters the viewport. Returns {@code null} if it never crosses.
     */
    private Point lineEntryAlongLatitude(MapContainer mc, double lon,
                                         double latMin, double latMax, Rectangle view) {
        int samples = 96;
        Point2D.Double ll = new Point2D.Double();
        Point sp = new Point();
        for (int i = 0; i <= samples; i++) {
            double lat = latMin + (latMax - latMin) * (i / (double) samples);
            ll.x = lon;
            ll.y = lat;
            if (!projection.isPointVisible(ll)) continue;
            mc.latLonToLocal(sp, ll);
            if (view.contains(sp.x, sp.y)) {
                return new Point(sp.x, sp.y);
            }
        }
        return null;
    }

    private boolean tooClose(List<Integer> used, int v) {
        for (int u : used) {
            if (Math.abs(u - v) < LABEL_MIN_GAP_PX) return true;
        }
        return false;
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    // -------------------------------------------------------------------------
    // Coordinate formatting
    // -------------------------------------------------------------------------

    /**
     * Chooses how many fractional degree-digits a label needs given the step.
     * Steps ≥ 1° → whole degrees; ≥ 1′ → 2 decimals; finer → 4 decimals. (We
     * format in decimal degrees rather than D°M′S″ for compactness; switch to
     * {@link #formatDms} below if you prefer sexagesimal.)
     */
    private static int stepDecimals(double stepRad) {
        double deg = Math.toDegrees(stepRad);
        if (deg >= 1.0) return 0;
        if (deg >= 1.0 / 60.0) return 2;
        return 4;
    }

    private static String formatLat(double latRad, int decimals) {
        double deg = Math.toDegrees(latRad);
        char hemi = deg >= 0 ? 'N' : 'S';
        return fmtDeg(Math.abs(deg), decimals) + "\u00B0" + hemi;
    }

    private static String formatLon(double lonRad, int decimals) {
        double deg = Math.toDegrees(lonRad);
        char hemi = deg >= 0 ? 'E' : 'W';
        return fmtDeg(Math.abs(deg), decimals) + "\u00B0" + hemi;
    }

    private static String fmtDeg(double absDeg, int decimals) {
        if (decimals == 0) {
            return Long.toString(Math.round(absDeg));
        }
        return String.format("%." + decimals + "f", absDeg);
    }

    /**
     * Optional sexagesimal formatter (e.g. {@code 39°12'30"N}). Not used by
     * default; call it in place of {@link #fmtDeg} if you prefer D°M′S″ over
     * decimal degrees at fine zoom.
     *
     * @param absDeg absolute degrees (non-negative)
     * @param hemi   hemisphere character (N/S/E/W)
     * @return a D°M′S″ formatted string
     */
    @SuppressWarnings("unused")
    private static String formatDms(double absDeg, char hemi) {
        int d = (int) Math.floor(absDeg);
        double remMin = (absDeg - d) * 60.0;
        int m = (int) Math.floor(remMin);
        int s = (int) Math.round((remMin - m) * 60.0);
        if (s == 60) { s = 0; m++; }
        if (m == 60) { m = 0; d++; }
        return String.format("%d\u00B0%02d'%02d\"%c", d, m, s, hemi);
    }

    // -------------------------------------------------------------------------
    // Small value type
    // -------------------------------------------------------------------------

    /** Visible geographic window in radians; {@code lon} may exceed π for seam-straddling views. */
    private static final class GeoExtent {
        final double latMin, latMax, lonMin, lonMax;
        GeoExtent(double latMin, double latMax, double lonMin, double lonMax) {
            this.latMin = latMin; this.latMax = latMax;
            this.lonMin = lonMin; this.lonMax = lonMax;
        }
    }
}