package edu.cnu.mdi.splot.plot;

import java.awt.Graphics;
import java.awt.Point;

import edu.cnu.mdi.graphics.GraphicsUtils;
import edu.cnu.mdi.graphics.style.Styled;

/**
 * Just a straight line to be drawn on the plot, e.g. a y = 0 line
 *
 * @author heddle
 *
 */
public abstract class PlotLine {

//	public Styled(Color fillColor, Color borderColor, Color fitLineColor, Color auxLineColor, LineStyle fitLineStyle,
//			LineStyle auxLineStyle, float fitLineWidth, float auxLineWidth, SymbolType symbolType, int symbolSize) {

	// default style
	protected static Styled _defaultStyle = new Styled();

	// the plot canvas
	protected PlotCanvas _canvas;

	// init style to default
	protected Styled _style = _defaultStyle;

	// work points
	protected Point p0 = new Point();
	protected Point p1 = new Point();
	protected Point.Double wp = new Point.Double();

	public PlotLine(PlotCanvas canvas) {
		_canvas = canvas;
	}

	/**
	 * Set the line drawing style
	 *
	 * @param style the new style
	 */
	public void setStyle(Styled style) {
		_style = style;
	}

	/**
	 * Get the line style
	 *
	 * @return the line style
	 */
	public Styled getStyle() {
		return _style;
	}

	/**
	 * Draw the line
	 *
	 * @param g the graphis context
	 */
	public void draw(Graphics g) {
		wp.setLocation(getX0(), getY0());
		
		boolean goodPoint;
		goodPoint = _canvas.worldToLocal(p0, wp);
		if (!goodPoint) {
			return;
		}
		wp.setLocation(getX1(), getY1());
		
		goodPoint =_canvas.worldToLocal(p1, wp);
		if (!goodPoint) {
			return;
		}

		GraphicsUtils.drawStyleLine(g, _style.getAuxLineColor(), _style.getAuxLineWidth(), _style.getAuxLineStyle(),
				p0.x, p0.y, p1.x, p1.y);
	}

	/**
	 * Get the starting x coordinate
	 *
	 * @return the starting x coordinate
	 */
	public abstract double getX0();

	/**
	 * Get the ending x coordinate
	 *
	 * @return the ending x coordinate
	 */
	public abstract double getX1();

	/**
	 * Get the starting y coordinate
	 *
	 * @return the starting y coordinate
	 */
	public abstract double getY0();

	/**
	 * Get the ending y coordinate
	 *
	 * @return the ending y coordinate
	 */
	public abstract double getY1();
	
	@Override
	public String toString() {
		return "PlotLine: (" + getX0() + ", " + getY0() + ") to (" + getX1() + ", " + getY1() + ")";	
	}

}
