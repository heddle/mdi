package edu.cnu.mdi.splot.plot;

/** Plot annotation spanning the visible x range at a fixed data-space y value. */
public class HorizontalLine extends PlotLine {

	// the y value of the horizontal line
	private double _y;

	/**
	 * Create a horizontal annotation.
	 * @param canvas owning canvas
	 * @param y fixed data-space y value
	 */
	public HorizontalLine(PlotCanvas canvas, double y) {
		super(canvas);
		_y = y;
	}

	@Override
	public double getX0() {
		return _canvas.getDataWorld().getMinX();
	}

	@Override
	public double getX1() {
		return _canvas.getDataWorld().getMaxX();
	}

	@Override
	public double getY0() {
		return _y;
	}

	@Override
	public double getY1() {
		return _y;
	}

}
