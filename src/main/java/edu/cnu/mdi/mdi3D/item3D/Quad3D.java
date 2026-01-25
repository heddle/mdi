package edu.cnu.mdi.mdi3D.item3D;

import java.awt.Color;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;

public class Quad3D extends Item3D {

	// the corner coordinates as [x1, y1, z1, ..., x4, y4, z4]
	protected float[] _coords;

	// frame?
	protected boolean _frame;

	/**
	 * Create a quad from an array of coordinates
	 *
	 * @param panel3d   the owner panel
	 * @param coords    the coordinates [x1, y1, ..., y3, z3]
	 * @param color     the quad color
	 * @param lineWidth the line width
	 * @param frame     frame the quad
	 */
	public Quad3D(Panel3D panel3d, float coords[], Color color, float lineWidth, boolean frame) {
		super(panel3d);
		_coords = coords;
		_frame = frame;

		setFillColor(color);
		setLineWidth(lineWidth);
	}

	/**
	 * Create a quad from 12 explicit coordinates
	 *
	 * @param panel3d   the owner panel
	 * @param x1
	 * @param y1
	 * @param z1
	 * @param x2
	 * @param y2
	 * @param z2
	 * @param x3
	 * @param y3
	 * @param z3
	 * @param x4
	 * @param y4
	 * @param z4
	 *
	 * @param color     the quad color
	 * @param lineWidth the line width
	 * @param frame     frame the quad
	 */
	public Quad3D(Panel3D panel3d, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3,
			float z3, float x4, float y4, float z4, Color color, float lineWidth, boolean frame) {
		super(panel3d);
		_coords = new float[12];
		_coords[0] = x1;
		_coords[1] = y1;
		_coords[2] = z1;
		_coords[3] = x2;
		_coords[4] = y2;
		_coords[5] = z2;
		_coords[6] = x3;
		_coords[7] = y3;
		_coords[8] = z3;
		_coords[9] = x4;
		_coords[10] = y4;
		_coords[11] = z4;

		_frame = frame;

		setFillColor(color);

		setLineWidth(lineWidth);
	}

	/**
	 * Create a quad to serve as a constant z plane
	 *
	 * @param panel3d
	 * @param z
	 * @param size
	 * @param color
	 * @param lineWidth
	 * @param frame
	 * @return
	 */
	public static Quad3D constantZQuad(Panel3D panel3d, float z, float size, Color color, float lineWidth,
			boolean frame) {
		float coords[] = new float[12];

		coords[0] = -size;
		coords[1] = -size;
		coords[2] = z;
		coords[3] = -size;
		coords[4] = size;
		coords[5] = z;
		coords[6] = size;
		coords[7] = size;
		coords[8] = z;
		coords[9] = size;
		coords[10] = -size;
		coords[11] = z;

		return new Quad3D(panel3d, coords, color, lineWidth, frame);
	}

	@Override
	public void draw(GLAutoDrawable drawable) {
		Support3D.drawQuads(drawable, _coords, getFillColor(), getLineWidth(), _frame);
	}

	public float[] getCentroid() {
		float cx = 0;
		float cy = 0;
		float cz = 0;
		for (int i = 0; i < 4; i++) {
			cx += _coords[3 * i];
			cy += _coords[3 * i + 1];
			cz += _coords[3 * i + 2];
		}
		return new float[] { cx / 4, cy / 4, cz / 4 };
	}

	@Override
	public float[] getSortPoint() {
		return getCentroid();
	}

}
