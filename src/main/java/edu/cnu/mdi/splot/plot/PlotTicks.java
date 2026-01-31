package edu.cnu.mdi.splot.plot;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.List;

import edu.cnu.mdi.graphics.GraphicsUtils;
import edu.cnu.mdi.graphics.text.UnicodeUtils;
import edu.cnu.mdi.splot.pdata.ACurve;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.HistoCurve;
import edu.cnu.mdi.splot.pdata.HistoData;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.ui.fonts.Fonts;

public class PlotTicks {

	private int majorTickLen = 6;
	private int minorTickLen = 2;
	private int numMajorTickX = 4; // interior ticks
	private int numMinorTickX = 4; // interior ticks
	private int numMajorTickY = 4; // interior ticks
	private int numMinorTickY = 4; // interior ticks

	// draw 1-based bin instead of value
	private boolean drawBinValue;

	private Color _tickColor = Color.black;

	private Font _tickFont = Fonts.plainFontDelta(-1);

	// plot owner
	private PlotCanvas _plotCanvas;

	// work points
	private Point _pp = new Point();
	private Point2D.Double _wp = new Point2D.Double();

	public PlotTicks(PlotCanvas plotCanvas) {
		_plotCanvas = plotCanvas;
	}

	public void setTickFont(Font font) {
		_tickFont = font;
	}

	/**
	 * Sets whether we want to draw the bin values on the x axis. Only relevant for
	 * histograms
	 *
	 * @param drawBinVal the value
	 */
	public void setDrawBinValue(boolean drawBinVal) {
		if (_plotCanvas.getPlotData().isHistogramData()) {
			drawBinValue = drawBinVal;
		}
	}

	/**
	 * Draw the plot ticks
	 *
	 * @param g the Graphics context
	 */
	public void draw(Graphics g) {
		Rectangle activeBounds = _plotCanvas.getActiveBounds();
		if (activeBounds == null) {
			return;
		}

		Rectangle.Double world = _plotCanvas.getDataWorld();
		if (world == null) {
			return;
		}

		g.setColor(_tickColor);
		g.setFont(_tickFont);

		double xmin = world.x;
		double ymin = world.y;
		double xmax = world.getMaxX();
		double ymax = world.getMaxY();

		boolean xLog = _plotCanvas.isXLogActive();
		boolean yLog = _plotCanvas.isYLogActive();

		// Major ticks
		if (xLog) {
			drawLogXTicks(g, xmin, xmax, world.getCenterY(), activeBounds);
		} else {
			if (_plotCanvas.isBarPlot()) {
				List<ACurve> curves = _plotCanvas.getPlotData().getCurves();
				drawBarPlotCatagories(g, curves, xmin, xmax, world.getCenterY(), majorTickLen, curves.size(), activeBounds);
			} 
			else {
				drawXTicks(g, xmin, xmax, world.getCenterY(), majorTickLen, numMajorTickX, activeBounds, true);
			}
		}

		if (yLog) {
			drawLogYTicks(g, ymin, ymax, world.getCenterX(), activeBounds);
		} else {
			drawYTicks(g, ymin, ymax, world.getCenterX(), majorTickLen, numMajorTickY, activeBounds, true);
		}

		// Minor ticks for linear only (log draws its own minors)
		if (!xLog) {
			double delx = (xmax - xmin) / (numMajorTickX + 1);
			for (int i = 0; i <= numMajorTickX; i++) {
				double xxmin = xmin + i * delx;
				drawXTicks(g, xxmin, xxmin + delx, world.getCenterY(), minorTickLen, numMinorTickX, activeBounds, false);
			}
		}

		if (!yLog) {
			double dely = (ymax - ymin) / (numMajorTickY + 1);
			for (int i = 0; i <= numMajorTickY; i++) {
				double yymin = ymin + i * dely;
				drawYTicks(g, yymin, yymin + dely, world.getCenterX(), minorTickLen, numMinorTickY, activeBounds, false);
			}
		}

	}

	// draw histogram bin values
	private void drawBinValues(Graphics g, double xmin, double xmax, double yc, int ticklen, int numtick, Rectangle ab,
			boolean drawVal) {

		PlotData plotData = _plotCanvas.getPlotData();
		if (!plotData.isHistogramData()) {
			return;
		}

		HistoCurve hc = (HistoCurve) (plotData.getCurve(0));
		HistoData hd = hc.getHistoData();
		if (hd == null) {
			return;
		}

		FontMetrics fm = _plotCanvas.getFontMetrics(_tickFont);
		double delx = (xmax - xmin) / (numtick + 1);

		int t = ab.y;
		int b = t + ab.height;
		int sb = b + fm.getHeight();

		for (int i = 1; i <= (numtick + 1); i++) {
			double value = xmin + (i - 0.5) * delx;
			_wp.setLocation(value, yc);
			_plotCanvas.dataToScreen(_pp, _wp);
			g.drawLine(_pp.x, b, _pp.x, b - ticklen);
			g.drawLine(_pp.x, t, _pp.x, t + ticklen);

			if (drawVal) {
				int bin = hd.getBin(value) + 1;
				String valStr = "" + bin;
				int sw = fm.stringWidth(valStr);
				g.drawString(valStr, _pp.x - sw / 2, sb);

			} // draw val
		} // for

	}
	
	private void drawLogXTicks(Graphics g, double xmin, double xmax, double yc, Rectangle ab) {
		if (xmin <= 0 || xmax <= 0) {
			return;
		}

		FontMetrics fm = _plotCanvas.getFontMetrics(_tickFont);

		int t = ab.y;
		int b = t + ab.height;
		int sb = b + fm.getHeight();

		int n0 = (int) Math.ceil(Math.log10(xmin));
		int n1 = (int) Math.floor(Math.log10(xmax));

		Point _pp = this._pp;
		Point2D.Double _wp = this._wp;

		for (int n = n0; n <= n1; n++) {
			double decade = Math.pow(10.0, n);

			// Major tick
			_wp.setLocation(decade, yc);
			_plotCanvas.dataToScreen(_pp, _wp);
			g.drawLine(_pp.x, b, _pp.x, b - majorTickLen);
			g.drawLine(_pp.x, t, _pp.x, t + majorTickLen);

			// Label
			String label = "10" + UnicodeUtils.getSuperscript(n, n < 0);

			int sw = fm.stringWidth(label);
			g.drawString(label, _pp.x - sw / 2, sb);

			// Minor ticks (2..9)
			for (int m = 2; m <= 9; m++) {
				double v = m * decade;
				if (v < xmin || v > xmax) {
					continue;
				}
				_wp.setLocation(v, yc);
				_plotCanvas.dataToScreen(_pp, _wp);
				g.drawLine(_pp.x, b, _pp.x, b - minorTickLen);
				g.drawLine(_pp.x, t, _pp.x, t + minorTickLen);
			}
		}
	}

	private void drawLogYTicks(Graphics g, double ymin, double ymax, double xc, Rectangle ab) {
		if (ymin <= 0 || ymax <= 0) {
			return;
		}

		FontMetrics fm = _plotCanvas.getFontMetrics(_tickFont);

		int l = ab.x;
		int r = l + ab.width;
		int sb = l - 6; // a little left of axis

		int n0 = (int) Math.ceil(Math.log10(ymin));
		int n1 = (int) Math.floor(Math.log10(ymax));

		Point _pp = this._pp;
		Point2D.Double _wp = this._wp;

		for (int n = n0; n <= n1; n++) {
			double decade = Math.pow(10.0, n);

			// Major tick
			_wp.setLocation(xc, decade);
			_plotCanvas.dataToScreen(_pp, _wp);
			g.drawLine(l, _pp.y, l + majorTickLen, _pp.y);
			g.drawLine(r, _pp.y, r - majorTickLen, _pp.y);

			// Label (rotated like your linear y labels)
			String label = "10" + UnicodeUtils.getSuperscript(n, n < 0);
			int sw = fm.stringWidth(label);
			GraphicsUtils.drawRotatedText((Graphics2D) g, label, _tickFont, sb, _pp.y + sw / 2, 0, 0, -90);

			// Minor ticks (2..9)
			for (int m = 2; m <= 9; m++) {
				double v = m * decade;
				if (v < ymin || v > ymax) {
					continue;
				}
				_wp.setLocation(xc, v);
				_plotCanvas.dataToScreen(_pp, _wp);
				g.drawLine(l, _pp.y, l + minorTickLen, _pp.y);
				g.drawLine(r, _pp.y, r - minorTickLen, _pp.y);
			}
		}
	}

	// draw bar plot category tick marks
	private void drawBarPlotCatagories(Graphics g, List<ACurve> curves, double xmin, double xmax, double yc
			, int ticklen, int numtick, Rectangle ab) {
		if (curves == null || curves.isEmpty()) {
			return;
		}

		g.setFont(Fonts.tweenFont);
		FontMetrics fm = g.getFontMetrics();
		int t = ab.y;
		int b = t + ab.height;
		int sb = b + fm.getHeight();
		
		for (ACurve acurve : curves) {
			Curve curve = (Curve) acurve;
			String name = acurve.name();
			Point2D.Double center = curve.getCentroid();
			_wp.setLocation(center.x, yc);
			_plotCanvas.dataToScreen(_pp, _wp);
			int sw = fm.stringWidth(name);
			g.drawString(name, _pp.x - sw / 2, sb);
		}
	}

	// draw x tick marks
	private void drawXTicks(Graphics g, double xmin, double xmax, double yc, int ticklen, int numtick, Rectangle ab,
			boolean drawVal) {
		if (numtick < 1) {
			return;
		}

		if (drawBinValue) {
			drawBinValues(g, xmin, xmax, yc, ticklen, numtick, ab, drawVal);
			return;
		}

		PlotParameters params = _plotCanvas.getParameters();
		FontMetrics fm = _plotCanvas.getFontMetrics(_tickFont);
		g.setFont(_tickFont);
		double delx = (xmax - xmin) / (numtick + 1);

		int t = ab.y;
		int b = t + ab.height;
		int sb = b + fm.getHeight();

		for (int i = 1; i <= numtick; i++) {
			double value = xmin + i * delx;
			_wp.setLocation(value, yc);
			_plotCanvas.dataToScreen(_pp, _wp);
			g.drawLine(_pp.x, b, _pp.x, b - ticklen);
			g.drawLine(_pp.x, t, _pp.x, t + ticklen);

			if (drawVal) {
				String valStr = DoubleFormat.doubleFormat(value, params.getNumDecimalX(), params.getMinExponentX());
				int sw = fm.stringWidth(valStr);
				g.drawString(valStr, _pp.x - sw / 2, sb);

			} // draw val
		} // for

	}

	// draw y tick marks
	private void drawYTicks(Graphics g, double ymin, double ymax, double xc, int ticklen, int numtick, Rectangle ab,
			boolean drawVal) {
		if (numtick < 1) {
			return;
		}

		PlotParameters params = _plotCanvas.getParameters();
		FontMetrics fm = _plotCanvas.getFontMetrics(_tickFont);
		g.setFont(_tickFont);
		double dely = (ymax - ymin) / (numtick + 1);

		int l = ab.x;
		int r = l + ab.width;
		int sb = l - 4;

		for (int i = 0; i <= (numtick + 1); i++) {
			if ((i > 0) && (i < numtick + 1)) {
				double value = ymin + i * dely;
				_wp.setLocation(xc, value);
				_plotCanvas.dataToScreen(_pp, _wp);
				g.drawLine(l, _pp.y, l + ticklen, _pp.y);
				g.drawLine(r, _pp.y, r - ticklen, _pp.y);
				if (drawVal) {
					String valstr = DoubleFormat.doubleFormat(value, params.getNumDecimalY(), params.getMinExponentY());
					int sw = fm.stringWidth(valstr);
					GraphicsUtils.drawRotatedText((Graphics2D) g, valstr, _tickFont, sb, _pp.y + sw / 2, 0, 0, -90);
				}
			}
		}

	}

	public int getNumMajorTickX() {
		return numMajorTickX;
	}

	public void setNumMajorTickX(int numMajorTickX) {
		this.numMajorTickX = numMajorTickX;
	}

	public int getNumMinorTickX() {
		return numMinorTickX;
	}

	public void setNumMinorTickX(int numMinorTickX) {
		this.numMinorTickX = numMinorTickX;
	}

	public int getNumMajorTickY() {
		return numMajorTickY;
	}

	public void setNumMajorTickY(int numMajorTickY) {
		this.numMajorTickY = numMajorTickY;
	}

	public int getNumMinorTickY() {
		return numMinorTickY;
	}

	public void setNumMinorTickY(int numMinorTickY) {
		this.numMinorTickY = numMinorTickY;
	}

	public int getMajorTickLen() {
		return majorTickLen;
	}

	public void setMajorTickLen(int majorTickLen) {
		this.majorTickLen = majorTickLen;
	}

	public int getMinorTickLen() {
		return minorTickLen;
	}

	public void setMinorTickLen(int minorTickLen) {
		this.minorTickLen = minorTickLen;
	}

}
