package edu.cnu.mdi.splot.plot;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;

import edu.cnu.mdi.splot.pdata.Histo2DData;
import edu.cnu.mdi.ui.colors.ScientificColorMap;

public final class HeatmapDrawer {

    private HeatmapDrawer() {}

    /** Draws a heatmap representation of the given 2D histogram data
	 * onto the given graphics context, using the given canvas for
	 * coordinate transforms, and the given options for color scale
	 * and log-Z setting.
	 * @param g  graphics context on which to draw
	 * @param canvas  plot canvas for coordinate transforms
	 * @param h2d  2D histogram data to draw
	 * @param opt  heatmap drawing options (if null, defaults used)
	 */
    public static void drawHeatmap(Graphics g, PlotCanvas canvas, Histo2DData h2d) {

        if (g == null || canvas == null || h2d == null) {
            return;
        }
        

        final Graphics2D g2 = (Graphics2D) g;
        final Color[] scale =canvas.getParameters().getColorMap().scale();
        final boolean logZ = canvas.getParameters().isLogZ();
        
        // Snapshot for consistent paint
        final double[][] bins = h2d.snapshotBins();

        final double zMax = Histo2DData.maxZ(bins);
        if (zMax <= 0) {
            return;
        }

        // log10(v+1) mapping 
        final double logMax = logZ ? Math.log10(zMax + 1.0) : 0.0;
        final double denom = logZ ? logMax : 1.0;
        
        final int nx = h2d.nx();
        final int ny = h2d.ny();

        final double dx = h2d.xBinWidth();
        final double dy = h2d.yBinWidth();

        final double xmin = h2d.xMin();
        final double ymin = h2d.yMin();

        for (int ix = 0; ix < nx; ix++) {
            final double x0 = xmin + ix * dx;
            final double x1 = x0 + dx;

            for (int iy = 0; iy < ny; iy++) {
            	final double v = bins[ix][iy];
            	if (!(v >= 0) || !Double.isFinite(v)) {
            	    continue;
            	}
            	
                final double y0 = ymin + iy * dy;
                final double y1 = y0 + dy;

                double t;
                if (!logZ) {
                    t = (zMax <= 0) ? 0.0 : (v / zMax);     // v=0 -> t=0 (paint it!)
                } else {
                    t = (denom <= 0) ? 0.0 : (Math.log10(v + 1.0) / denom); // v=0 -> t=0
                }

                if (!Double.isFinite(t)) {
                    continue;
                }

                t = Math.max(0.0, Math.min(1.0, t));

                final Color c = ScientificColorMap.interpolate(scale, t);

                Point p0 = canvas.dataToScreen(x0, y0);
                Point p1 = canvas.dataToScreen(x1, y1);

                int x = Math.min(p0.x, p1.x);
                int y = Math.min(p0.y, p1.y);
                int w = Math.max(1, Math.abs(p1.x - p0.x));
                int h = Math.max(1, Math.abs(p1.y - p0.y));

                // optional anti-seam
                g2.setColor(c);
                g2.fillRect(x, y, w + 1, h + 1);
             }
        }
    }

}
