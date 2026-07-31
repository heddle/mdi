package edu.cnu.mdi.splot.plot;

/** Plot annotation spanning the visible y range at a fixed data-space x value. */
public class VerticalLine extends PlotLine {

	// the x value of the vertical line
	private double _x;

	/**
	 * Create a vertical annotation.
	 * @param canvas owning canvas
	 * @param x fixed data-space x value
	 */
	public VerticalLine(PlotCanvas canvas, double x) {
		super(canvas);
		_x = x;
	}

	@Override
	public double getX0() {
		return _x;
	}

	@Override
	public double getX1() {
		return _x;
	}

	@Override
	public double getY0() {
		return _canvas.getDataWorld().getMinY();
	}

	@Override
	public double getY1() {
		return _canvas.getDataWorld().getMaxY();
	}

}
