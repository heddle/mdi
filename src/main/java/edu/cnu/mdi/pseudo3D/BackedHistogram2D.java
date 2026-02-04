package edu.cnu.mdi.pseudo3D;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

import javax.swing.JComponent;
import javax.swing.JFrame;

import edu.cnu.mdi.splot.pdata.Histo2DData;
import edu.cnu.mdi.ui.colors.ScientificColorMap;

/**
 * A lightweight pseudo-3D renderer for a {@link Histo2DData} 2D histogram.
 * <p>
 * This component draws each histogram bin as a 3D-looking pillar (top + side faces)
 * using pure Java2D (no JOGL). A painter's algorithm (depth sort) is used to
 * render bins from far-to-near so occlusion looks correct.
 * <p>
 * Interaction:
 * <ul>
 *   <li>Left-drag: rotate (about X and Y)</li>
 *   <li>Mouse wheel: zoom</li>
 *   <li>Move mouse: hover highlight (top face hit-test)</li>
 * </ul>
 * <p>
 * Performance notes:
 * <ul>
 *   <li>No per-bar allocations during painting (no {@code new int[]} per bin).</li>
 *   <li>Depth array and index array are primitive and reused.</li>
 *   <li>Depth-sort is recomputed only when rotation/zoom/size changes (not on mouse move).</li>
 *   <li>Trigonometric values are cached per frame.</li>
 * </ul>
 *
 * <h3>Drop-in behavior</h3>
 * This is intended as a drop-in replacement for the prototype {@code BackedHistogram2D}
 * you shared: same package/class name, same constructor signature, same mouse gestures,
 * and it still directly reads {@code data._bins}.
 */
@SuppressWarnings("serial")
public class BackedHistogram2D extends JComponent {

    // ---- Backing data ----
    private Histo2DData data;
    private ScientificColorMap colorMap;

    // Histogram geometry
    private int nx; // number of bins in x direction
    private int ny; // number of bins in y direction
    private double[][] bins;

    // ---- Interaction state ----
    private Point lastMouse;
    private final Point mousePos = new Point(0, 0);

    private double rotX = 0.5;
    private double rotY = 0.5;
    private double zoomMultiplier = 1.0; // 1.0 = no zoom

    // ---- Hover state ----
    private int hoveredI = -1;
    private int hoveredJ = -1;

    // ---- Reused polygon buffers (no allocations in paint) ----
    private final int[] px = new int[4];
    private final int[] py = new int[4];

    // ---- Performance caches ----
    /** Flattened indices for bins, sorted far-to-near. */
    private final int[] indices;
    /** Depth value per flattened bin index (same length as {@link #indices}). */
    private final double[] depth;

    // Cached view parameters (used to decide when to recompute depth/sort)
    private int lastW = -1;
    private int lastH = -1;
    private double lastRotX = Double.NaN;
    private double lastRotY = Double.NaN;
    private double lastZoomMultiplier = Double.NaN;
    private double cachedZoom = 1.0;
    private int cachedCx = 0;
    private int cachedCy = 0;

    // Cached trig values for projections (updated when view cache is updated)
    private double sinX, cosX, sinY, cosY;

    // Cached data scaling
    private double invMaxBin = 1.0;
    private double heightScale = 1.0;

    /**
     * Create a pseudo-3D histogram view for the given data.
     *
     * @param data     histogram data (must not be {@code null})
     * @param colorMap colormap used to color pillars by bin content (must not be {@code null})
     */
    public BackedHistogram2D(Histo2DData data, ScientificColorMap colorMap) {
        this.data = Objects.requireNonNull(data, "data");
        this.colorMap = Objects.requireNonNull(colorMap, "colorMap");

        this.nx = data.nx();
        this.ny = data.ny();
        this.bins = data._bins;

        int n = nx * ny;
        this.indices = new int[n];
        this.depth = new double[n];
        for (int i = 0; i < n; i++) {
            indices[i] = i;
        }

        refreshDataScale();
        mouseListening();
    }

    /**
     * Set the backing data.
     * <p>
     * This is optional but handy if you reuse the component for new histograms.
     *
     * @param data new data (must not be {@code null})
     */
    public void setData(Histo2DData data) {
        this.data = Objects.requireNonNull(data, "data");
        this.nx = data.nx();
        this.ny = data.ny();
        this.bins = data._bins;
        refreshDataScale();
        invalidateViewCache();
        repaint();
    }

    /**
     * Set the colormap.
     *
     * @param colorMap new colormap (must not be {@code null})
     */
    public void setColorMap(ScientificColorMap colorMap) {
        this.colorMap = Objects.requireNonNull(colorMap, "colorMap");
        repaint();
    }

    /**
     * @return last hovered x-bin index, or -1 if none
     */
    public int getHoveredI() {
        return hoveredI;
    }

    /**
     * @return last hovered y-bin index, or -1 if none
     */
    public int getHoveredJ() {
        return hoveredJ;
    }

    // Add the mouse listeners to the canvas to handle rotation and zooming
    private void mouseListening() {
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMouse = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                rotY += (e.getX() - lastMouse.x) * 0.01;
                rotX += (e.getY() - lastMouse.y) * 0.01;
                lastMouse = e.getPoint();
                // Rotation changes depth ordering.
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                // Hover should NOT trigger a resort.
                mousePos.setLocation(e.getPoint());
                repaint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                zoomMultiplier *= (e.getPreciseWheelRotation() > 0) ? 0.9 : 1.1;
                repaint();
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
        addMouseWheelListener(ma);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        updateViewCacheIfNeeded();

        // 1) Draw axes behind everything
        drawAxes(g2, cachedCx, cachedCy, cachedZoom);

        // 2) Hover hit-test (nearest-to-farthest), then draw bars.
        //    This avoids scanning all bins for hover when the pointer is over a visible top face.
        int hoveredFlat = findHoveredBinFlatIndex(cachedCx, cachedCy, cachedZoom);
        if (hoveredFlat >= 0) {
            hoveredI = hoveredFlat / ny;
            hoveredJ = hoveredFlat % ny;
        } else {
            hoveredI = -1;
            hoveredJ = -1;
        }

        for (int idx : indices) {
            int i = idx / ny;
            int j = idx % ny;
            renderBar(g2, cachedCx, cachedCy, i, j, bins[i][j], cachedZoom, idx == hoveredFlat);
        }
    }

    /**
     * Determine which (if any) bin is under the mouse pointer.
     * <p>
     * We scan in <em>reverse draw order</em> (nearest-to-farthest) and stop at the first hit.
     * This mirrors what the user sees: the closest visible top face should win.
     *
     * @param cx   canvas center x
     * @param cy   canvas center y
     * @param zoom zoom scale
     * @return flattened bin index ({@code i*ny + j}) or {@code -1} if none
     */
    private int findHoveredBinFlatIndex(int cx, int cy, double zoom) {
        final int mx = mousePos.x;
        final int my = mousePos.y;

        // Quick reject: no need to work if mouse isn't inside the component.
        if (mx < 0 || my < 0 || mx >= getWidth() || my >= getHeight()) {
            return -1;
        }

        for (int k = indices.length - 1; k >= 0; k--) {
            int flat = indices[k];
            int i = flat / ny;
            int j = flat % ny;
            if (hitTestTopFace(cx, cy, i, j, bins[i][j], zoom, mx, my)) {
                return flat;
            }
        }
        return -1;
    }

    /**
     * Ensure cached view parameters (zoom/cx/cy/trig) and the depth-sorted indices
     * are up to date.
     */
    private void updateViewCacheIfNeeded() {
        int w = getWidth();
        int h = getHeight();

        boolean viewChanged = (w != lastW)
                || (h != lastH)
                || (rotX != lastRotX)
                || (rotY != lastRotY)
                || (zoomMultiplier != lastZoomMultiplier);

        if (!viewChanged) {
            return;
        }

        lastW = w;
        lastH = h;
        lastRotX = rotX;
        lastRotY = rotY;
        lastZoomMultiplier = zoomMultiplier;

        // Auto scale
        double baseScale = Math.min(w, h) / (Math.max(nx, ny) * 1.5);
        cachedZoom = baseScale * zoomMultiplier;
        cachedCx = w / 2;
        cachedCy = h / 2;

        // Cache trig values
        sinX = Math.sin(rotX);
        cosX = Math.cos(rotX);
        sinY = Math.sin(rotY);
        cosY = Math.cos(rotY);

        // Recompute depth and sort far-to-near.
        computeDepths(cachedZoom);
        sortIndicesByDepthDesc();
    }

    private void invalidateViewCache() {
        lastW = -1;
        lastH = -1;
        lastRotX = Double.NaN;
        lastRotY = Double.NaN;
        lastZoomMultiplier = Double.NaN;
    }

    /** Update cached scale factors derived from histogram content. */
    private void refreshDataScale() {
        double max = data.maxBin();
        if (max <= 0) {
            invMaxBin = 1.0;
        } else {
            invMaxBin = 1.0 / max;
        }
        heightScale = ((nx + ny) / 5.0) * invMaxBin;
    }

    private void computeDepths(double zoom) {
        // Depth = (-x*sinY + (y*cosY)*sinX) * zoom
        // where x,y are centered bin coordinates.
        int n = indices.length;
        for (int idx = 0; idx < n; idx++) {
            double x = (idx / ny) - nx / 2.0;
            double y = (idx % ny) - ny / 2.0;
            depth[idx] = (-x * sinY + (y * cosY) * sinX) * zoom;
        }
    }

    /**
     * Sort {@link #indices} so that bars are drawn from far-to-near (descending depth).
     * <p>
     * Uses an in-place quicksort on primitive arrays to avoid boxing overhead.
     */
    private void sortIndicesByDepthDesc() {
        quicksortByDepthDesc(indices, 0, indices.length - 1);
    }

    private void quicksortByDepthDesc(int[] a, int lo, int hi) {
        // Classic in-place quicksort; for 10k items this is fine and avoids allocations.
        while (lo < hi) {
            int i = lo;
            int j = hi;
            double pivot = depth[a[(lo + hi) >>> 1]];
            while (i <= j) {
                while (depth[a[i]] > pivot) {
                    i++;
                }
                while (depth[a[j]] < pivot) {
                    j--;
                }
                if (i <= j) {
                    int tmp = a[i];
                    a[i] = a[j];
                    a[j] = tmp;
                    i++;
                    j--;
                }
            }
            // Recurse into smaller partition first to keep stack shallow.
            if ((j - lo) < (hi - i)) {
                if (lo < j) {
                    quicksortByDepthDesc(a, lo, j);
                }
                lo = i;
            } else {
                if (i < hi) {
                    quicksortByDepthDesc(a, i, hi);
                }
                hi = j;
            }
        }
    }

    /**
     * Render a single bar and return true if it's hovered.
     * <p>
     * This method handles both rendering and hover detection for efficiency.
     *
     * @param g2   graphics context
     * @param cx   canvas center x
     * @param cy   canvas center y
     * @param i    x bin index
     * @param j    y bin index
     * @param h    bin value
     * @param zoom zoom scale
     * @return true if the mouse is hovering over the top face of this bar
     */
    private void renderBar(Graphics2D g2, int cx, int cy, int i, int j, double h, double zoom, boolean isHovered) {
        // Bar footprint in "data grid" coordinates.
        final double x0 = i - nx / 2.0;
        final double y0 = j - ny / 2.0;
        final double x1 = x0 + 0.85;
        final double y1 = y0 + 0.85;

        // Scaled height.
        final double sH = h * heightScale;

        // Precompute some rotated values for speed.
        // projX: cx + (x*cosY + y*sinY)*zoom
        // projY: cy + ((-x*sinY + y*cosY)*cosX - z*sinX)*zoom

        // Top points
        final int tx0 = projX(x0, y0, cx, zoom);
        final int ty0 = projY(x0, y0, sH, cy, zoom);

        final int tx1 = projX(x1, y0, cx, zoom);
        final int ty1 = projY(x1, y0, sH, cy, zoom);

        final int tx2 = projX(x1, y1, cx, zoom);
        final int ty2 = projY(x1, y1, sH, cy, zoom);

        final int tx3 = projX(x0, y1, cx, zoom);
        final int ty3 = projY(x0, y1, sH, cy, zoom);

        // Bottom points (only need two for the visible side quad used here)
        final int bx0 = projX(x0, y0, cx, zoom);
        final int by0 = projY(x0, y0, 0.0, cy, zoom);

        final int bx1 = projX(x1, y0, cx, zoom);
        final int by1 = projY(x1, y0, 0.0, cy, zoom);

        float t = (float) (h * invMaxBin);
        if (t < 0f) {
            t = 0f;
        } else if (t > 1f) {
            t = 1f;
        }
        final Color mapColor = colorMap.colorAt(t);
        final Color base = isHovered ? Color.WHITE : mapColor;

        // Side face
        g2.setColor(base.darker());
        fillQ(g2, tx0, ty0, tx1, ty1, bx1, by1, bx0, by0);

        // Top face
        g2.setColor(base);
        fillQ(g2, tx0, ty0, tx1, ty1, tx2, ty2, tx3, ty3);

        // no return
    }

    /**
     * Hit-test the top face of a bar.
     * <p>
     * This is used by {@link #findHoveredBinFlatIndex(int, int, double)} to locate
     * the closest visible bar under the cursor. It intentionally does <em>not</em>
     * allocate temporary arrays.
     */
    private boolean hitTestTopFace(int cx, int cy, int i, int j, double h, double zoom, int mx, int my) {
        final double x0 = i - nx / 2.0;
        final double y0 = j - ny / 2.0;
        final double x1 = x0 + 0.85;
        final double y1 = y0 + 0.85;
        final double sH = h * heightScale;

        final int tx0 = projX(x0, y0, cx, zoom);
        final int ty0 = projY(x0, y0, sH, cy, zoom);
        final int tx1 = projX(x1, y0, cx, zoom);
        final int ty1 = projY(x1, y0, sH, cy, zoom);
        final int tx2 = projX(x1, y1, cx, zoom);
        final int ty2 = projY(x1, y1, sH, cy, zoom);
        final int tx3 = projX(x0, y1, cx, zoom);
        final int ty3 = projY(x0, y1, sH, cy, zoom);

        return contains(mx, my, tx0, ty0, tx1, ty1, tx2, ty2, tx3, ty3);
    }

    /**
     * Draw 3D axes (X red, Y green, Z cyan) for orientation reference.
     */
    private void drawAxes(Graphics2D g2, int cx, int cy, double zoom) {
        int ox = projX(-nx / 2.0, -ny / 2.0, cx, zoom);
        int oy = projY(-nx / 2.0, -ny / 2.0, 0.0, cy, zoom);
        g2.setStroke(new BasicStroke(2f));

        g2.setColor(Color.RED);
        g2.drawLine(ox, oy,
                projX(nx / 2.0 + 1, -ny / 2.0, cx, zoom),
                projY(nx / 2.0 + 1, -ny / 2.0, 0.0, cy, zoom));

        g2.setColor(Color.GREEN);
        g2.drawLine(ox, oy,
                projX(-nx / 2.0, ny / 2.0 + 1, cx, zoom),
                projY(-nx / 2.0, ny / 2.0 + 1, 0.0, cy, zoom));

        g2.setColor(Color.CYAN);
        g2.drawLine(ox, oy,
                projX(-nx / 2.0, -ny / 2.0, cx, zoom),
                projY(-nx / 2.0, -ny / 2.0, (nx + ny) / 4.0, cy, zoom));

        // Labels + ticks (kept lightweight; uses reflection so this class remains a drop-in
        // even if Histo2DData's accessor names change).
        drawAxisLabelsAndTicks(g2, cx, cy, zoom);
    }

    /**
     * Draw simple tick marks and numeric labels for the X and Y axes.
     * <p>
     * Labels are derived from the histogram's configured X/Y min/max if available.
     * If not, we fall back to bin indices.
     */
    private void drawAxisLabelsAndTicks(Graphics2D g2, int cx, int cy, double zoom) {
        final double xMin = reflectRangeValue(data,
                new String[] { "xMin", "xmin", "_xmin", "minX", "getXMin" },
                0.0);
        final double xMax = reflectRangeValue(data,
                new String[] { "xMax", "xmax", "_xmax", "maxX", "getXMax" },
                (double) nx);
        final double yMin = reflectRangeValue(data,
                new String[] { "yMin", "ymin", "_ymin", "minY", "getYMin" },
                0.0);
        final double yMax = reflectRangeValue(data,
                new String[] { "yMax", "ymax", "_ymax", "maxY", "getYMax" },
                (double) ny);

        // If we failed to detect real-world ranges, label in terms of bin indices.
        final boolean useBinLabels = (xMax == (double) nx && xMin == 0.0 && yMax == (double) ny && yMin == 0.0);

        final int ticks = 5;
        g2.setStroke(new BasicStroke(1f));
        FontMetrics fm = g2.getFontMetrics();

        // Origin is at the lower-left of the grid (in our model)
        final double x0 = -nx / 2.0;
        final double y0 = -ny / 2.0;

        // --- X axis ticks along (x, y0, z=0) ---
        g2.setColor(Color.DARK_GRAY);
        for (int t = 0; t < ticks; t++) {
            double a = (ticks == 1) ? 0.0 : (t / (double) (ticks - 1));
            double gx = x0 + a * nx;
            int sx = projX(gx, y0, cx, zoom);
            int sy = projY(gx, y0, 0.0, cy, zoom);

            // Screen-space tick mark (small vertical line)
            g2.drawLine(sx, sy, sx, sy + 4);

            double val = useBinLabels ? (a * (nx - 1)) : (xMin + a * (xMax - xMin));
            String lab = formatTick(val);
            int sw = fm.stringWidth(lab);
            g2.drawString(lab, sx - sw / 2, sy + fm.getAscent() + 6);
        }

        // X label near end
        {
            int ex = projX(x0 + nx + 1.0, y0, cx, zoom);
            int ey = projY(x0 + nx + 1.0, y0, 0.0, cy, zoom);
            g2.setColor(Color.RED.darker());
            g2.drawString("X", ex + 2, ey + fm.getAscent() + 2);
        }

        // --- Y axis ticks along (x0, y, z=0) ---
        g2.setColor(Color.DARK_GRAY);
        for (int t = 0; t < ticks; t++) {
            double a = (ticks == 1) ? 0.0 : (t / (double) (ticks - 1));
            double gy = y0 + a * ny;
            int sx = projX(x0, gy, cx, zoom);
            int sy = projY(x0, gy, 0.0, cy, zoom);

            // Tick mark (small horizontal line)
            g2.drawLine(sx, sy, sx - 4, sy);

            double val = useBinLabels ? (a * (ny - 1)) : (yMin + a * (yMax - yMin));
            String lab = formatTick(val);
            int sw = fm.stringWidth(lab);
            g2.drawString(lab, sx - sw - 6, sy + fm.getAscent() / 2);
        }

        // Y label near end
        {
            int ex = projX(x0, y0 + ny + 1.0, cx, zoom);
            int ey = projY(x0, y0 + ny + 1.0, 0.0, cy, zoom);
            g2.setColor(Color.GREEN.darker());
            g2.drawString("Y", ex - fm.stringWidth("Y") - 2, ey - 2);
        }
    }

    private String formatTick(double v) {
        double av = Math.abs(v);
        if (av >= 1000 || (av > 0 && av < 0.01)) {
            return String.format("%.2g", v);
        }
        if (av >= 10) {
            return String.format("%.0f", v);
        }
        if (av >= 1) {
            return String.format("%.2f", v);
        }
        return String.format("%.3f", v);
    }

    /**
     * Best-effort extraction of a range value from {@code Histo2DData} using reflection.
     * <p>
     * We try a small set of common accessor names as no-arg methods first, then fields.
     */
    private static double reflectRangeValue(Object obj, String[] names, double fallback) {
        for (String n : names) {
            // Method
            try {
                Method m = obj.getClass().getMethod(n);
                if (m.getReturnType() == double.class || m.getReturnType() == Double.class) {
                    Object v = m.invoke(obj);
                    if (v instanceof Number) {
                        return ((Number) v).doubleValue();
                    }
                }
            } catch (Throwable ignore) {
                // try next
            }
            // Field
            try {
                Field f = obj.getClass().getField(n);
                Object v = f.get(obj);
                if (v instanceof Number) {
                    return ((Number) v).doubleValue();
                }
            } catch (Throwable ignore) {
                // try next
            }
        }
        return fallback;
    }

    /**
     * Point-in-quad test (ray casting) with no allocations.
     */
    private boolean contains(int mx, int my, int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) {
        // Unrolled ray-cast for 4 vertices.
        boolean c = false;

        // Edge (x1,y1) -> (x2,y2)
        if (((y1 > my) != (y2 > my)) && (mx < (long) (x2 - x1) * (my - y1) / (y2 - y1) + x1)) {
            c = !c;
        }
        // Edge (x2,y2) -> (x3,y3)
        if (((y2 > my) != (y3 > my)) && (mx < (long) (x3 - x2) * (my - y2) / (y3 - y2) + x2)) {
            c = !c;
        }
        // Edge (x3,y3) -> (x4,y4)
        if (((y3 > my) != (y4 > my)) && (mx < (long) (x4 - x3) * (my - y3) / (y4 - y3) + x3)) {
            c = !c;
        }
        // Edge (x4,y4) -> (x1,y1)
        if (((y4 > my) != (y1 > my)) && (mx < (long) (x1 - x4) * (my - y4) / (y1 - y4) + x4)) {
            c = !c;
        }

        return c;
    }

    /**
     * Project x/y at z=0 to screen X. (z does not affect x in this simple model)
     */
    private int projX(double x, double y, int cx, double zoom) {
        return cx + (int) ((x * cosY + y * sinY) * zoom);
    }

    /**
     * Project x/y/z to screen Y.
     */
    private int projY(double x, double y, double z, int cy, double zoom) {
        double yRot = -x * sinY + y * cosY;
        return cy + (int) ((yRot * cosX - z * sinX) * zoom);
    }

    /**
     * Fill a quadrilateral defined by four vertices with the current color.
     */
    private void fillQ(Graphics2D g2, int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) {
        px[0] = x1;
        px[1] = x2;
        px[2] = x3;
        px[3] = x4;
        py[0] = y1;
        py[1] = y2;
        py[2] = y3;
        py[3] = y4;
        g2.fillPolygon(px, py, 4);
    }

    public static void main(String[] args) {
        JFrame f = new JFrame("2D Histogram in pseudo-3D");

        int nx = 50;
        int ny = 50;
        Histo2DData data = new Histo2DData("Test", 0, 1, nx, 0, 1, ny);

        for (int i = 0; i < nx; i++) {
            double x = ((double) i + 1.0e-6) / nx;
            for (int j = 0; j < ny; j++) {
                double y = ((double) j + 1.0e-6) / ny;
                double z = (Math.sin(i * 0.1) * Math.cos(j * 0.1) + 1.1) * 10; // some test data
                data.fill(x, y, z);
            }

        }

        BackedHistogram2D canvas = new BackedHistogram2D(data, ScientificColorMap.VIRIDIS);
        f.add(canvas);
        f.setSize(1000, 800);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }
}
