package edu.cnu.mdi.splot.plot;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collection;

import edu.cnu.mdi.graphics.GraphicsUtils;
import edu.cnu.mdi.graphics.SymbolDraw;
import edu.cnu.mdi.graphics.style.Styled;
import edu.cnu.mdi.graphics.style.SymbolType;
import edu.cnu.mdi.graphics.text.UnicodeUtils;
import edu.cnu.mdi.splot.fit.CurveDrawingMethod;
import edu.cnu.mdi.splot.pdata.ACurve;
import edu.cnu.mdi.splot.pdata.Histo2DData;
import edu.cnu.mdi.splot.pdata.HistoCurve;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataType;

@SuppressWarnings("serial")
public class Legend extends DraggableRectangle {

	// the owner plot panel
	private PlotCanvas _canvas;

	// the plot parameters
	private PlotParameters _params;

	// gap before text
	private final int HGAP = 8;

	// extra v gap
	private final int VGAP = 6;

	private int _maxStringWidth;
	private int _extra;

	private int _numVisCurves;

	/**
	 * Create a Legend rectangle
	 *
	 * @param canvas the parent plot canvas
	 */
	public Legend(PlotCanvas canvas) {
		_canvas = canvas;
		_params = canvas.getParameters();
	}

	/**
	 * Draw the legend
	 *
	 * @param g the graphics context
	 */
	public void draw(Graphics g) {
		PlotData plotData = _canvas.getPlotData();

		//as always, Histo2D has no curves to show so
		//we put some useful stats instead
		if (plotData.getType() == PlotDataType.H2D) {
			Histo2DData h2d = plotData.getHisto2DData();
			if (h2d != null) {
				histo2DDraw(g, h2d);
			}
			return;
		}

		// are there any visible curves?
		if ((plotData == null) || plotData.getVisibleCurves().isEmpty()) {
			return;
		}

		_numVisCurves = plotData.getVisibleCurves().size();

		width = getLegendWidth();
		height = getLegendHeight();

		g.setColor(_params.getTextBackground());
		g.fillRect(x, y, width, height);

		if (_params.isLegendBorder()) {
			g.setColor(_params.getTextBorderColor());
			g.drawRect(x, y, width, height);
		}

		int yi = y + VGAP;

		Collection<ACurve> curves = plotData.getVisibleCurves();
		for (ACurve curve : curves) {
			if (curve.isVisible()) {
				yi = drawCurveLegendInfo(g, yi, curve);
			}
		}
	}

	private void histo2DDraw(Graphics g, Histo2DData h2d) {
		FontMetrics fm = _canvas.getFontMetrics(_params.getTextFont());

		ArrayList<String> strings = new ArrayList<>();
		long totalEntries = h2d.getTotalCount();
		if (totalEntries <  10000) {
			strings.add(String.format("Total Entries: %d", totalEntries));
		} else  {
			strings.add(String.format("Total Entries: %.2e", (double)totalEntries));
		}

		//empty bins, occupancy
		long totalBins = h2d.nx() * h2d.ny();
		long emptyBins = h2d.getEmptyBinCount();
		double occupancy = 100.0 * (totalBins - emptyBins) / totalBins;
		strings.add(String.format("Bins: %d ( %.2f%% occupied) ", totalBins, occupancy));

		//count range
		double maxCount = h2d.maxBin();
		double minCount = ((emptyBins > 0) ? 0.0 : h2d.minNonZero());

		boolean logZ = _params.isLogZ();
		if (logZ) {
			double logMin = Math.log10(minCount + 1.0);
			double logMax = Math.log10(maxCount + 1.0);
			strings.add(String.format("%s(Z): [%.1f - %.2f]", UnicodeUtils.LOG10, logMin, logMax));
		} else {
			strings.add(String.format("Z: [%.0f - %.0f]", minCount,
					maxCount));
		}

		// mean
		strings.add(String.format("<Z>: %.1f (occupied bins)", h2d.meanBin()));

		//determine required width and height
		width = 0;
		height = 0;
		for (String s : strings) {
			width = Math.max(width, 2*HGAP + fm.stringWidth(s));
			height += fm.getHeight();
		}

		g.setColor(_params.getTextBackground());
		g.fillRect(x, y, width, height);

		if (_params.isLegendBorder()) {
			g.setColor(_params.getTextBorderColor());
			g.drawRect(x, y, width, height);
		}

		g.setFont(_params.getTextFont());
		g.setColor(_params.getTextForeground());

		//draw the strings
		int ys = y;
		for (String s : strings) {
			g.drawString(s, x + HGAP, ys + fm.getAscent());
			ys += fm.getHeight();
		}

	}

	// draw the info for the given curve
	private int drawCurveLegendInfo(Graphics g, int y, ACurve curve) {
		FontMetrics fm = _canvas.getFontMetrics(_params.getTextFont());
		g.setFont(_params.getTextFont());
		g.setColor(_params.getTextForeground());

		Styled style = curve.getStyle();
		CurveDrawingMethod cdm = curve.getCurveDrawingMethod();
		int space = verticalSpaceNeeded(curve);
		int yc = y + space / 2;

		String legStr = curve.name();
		if (legStr == null) {
			legStr = "";
		}

		if (curve.isHistogram()) {
			HistoCurve hc = (HistoCurve) curve;
			legStr += (" " + hc.getHistoData().statStr());
		}

		g.drawString(legStr, x + width - _maxStringWidth - HGAP, yc + fm.getHeight() / 2);

		if ((_numVisCurves > 0) && cdm != CurveDrawingMethod.NONE) {
			GraphicsUtils.drawStyleLine(g, style.getLineColor(), style.getLineWidth(), style.getLineStyle(), x + HGAP,
					yc, x + HGAP + _params.getLegendLineLength(), yc);
		}

		// draw symbol if any but only for xy curves
		if (curve.isXYCurve()) {
			SymbolDraw.drawSymbol(g, x + HGAP + _extra / 2, yc, curve.getStyle());
		}

		return y + space + VGAP;
	}

	// get required width of the legend
	private int getLegendWidth() {
		FontMetrics fm = _canvas.getFontMetrics(_params.getTextFont());
		PlotData plotData = _canvas.getPlotData();

		_maxStringWidth = 0;
		_extra = 0;

		Collection<ACurve> curves = plotData.getVisibleCurves();
		for (ACurve curve : curves) {
			if (curve.isVisible()) {

				CurveDrawingMethod cdm = curve.getCurveDrawingMethod();
				if ((_numVisCurves > 0) && cdm != CurveDrawingMethod.NONE) {
					_extra = _params.getLegendLineLength();
				} else {
					Styled style = curve.getStyle();
					if (style.getSymbolType() != SymbolType.NOSYMBOL) {
						_extra = Math.max(_extra, style.getSymbolSize());
					}
				}

				String legStr = curve.name();
				if (curve.isHistogram()) {
					HistoCurve hc = (HistoCurve) curve;
					legStr += (" " + hc.getHistoData().statStr());
				}

				int sw = fm.stringWidth(legStr);

				_maxStringWidth = Math.max(_maxStringWidth, sw);
			}

		}

		return _extra + _maxStringWidth + 3 * HGAP;
	}

	// get required height of the legend based
	// on the currently visible curves
	private int getLegendHeight() {
		PlotData plotData = _canvas.getPlotData();

		int height = VGAP;

		Collection<ACurve> curves = plotData.getVisibleCurves();
		for (ACurve curve : curves) {
			if (curve.isVisible()) {
				height += (VGAP + verticalSpaceNeeded(curve));
			}
		}

		return height;
	}

	// get the vertical space needed for a curve based on symbol
	// size and font height
	private int verticalSpaceNeeded(ACurve curve) {
		FontMetrics fm = _canvas.getFontMetrics(_params.getTextFont());
		return Math.max(fm.getHeight(), curve.getStyle().getSymbolSize());
	}

}
