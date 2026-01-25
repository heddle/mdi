package edu.cnu.mdi.mdi3D.item3D;

import java.awt.Color;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;

public class Cube extends Item3D {

	// x coordinate of center of cube
	private float _xc;

	// y coordinate of center of cube
	private float _yc;

	// z coordinate of center of cube
	private float _zc;

	// half length
	private float _halfLength;

	// frame?
	protected boolean _frame;

	public Cube(Panel3D panel3D, float xc, float yc, float zc, float length, Color color) {
		this(panel3D, xc, yc, zc, length, color, false);
	}

	public Cube(Panel3D panel3D, float xc, float yc, float zc, float length, Color color, boolean frame) {
		super(panel3D);
		_xc = xc;
		_yc = yc;
		_zc = zc;
		_frame = frame;
		_halfLength = length / 2;
		setFillColor(color);
	}

	@Override
	public void draw(GLAutoDrawable drawable) {
		float w = 2 * _halfLength;
		Support3D.drawRectangularSolid(drawable, _xc, _yc, _zc, w, w, w, getFillColor(), 1, _frame);
	}

	@Override
	public float[] getSortPoint() {
		return new float[] { _xc, _yc, _zc };
	}


}
