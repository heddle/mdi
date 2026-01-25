package edu.cnu.mdi.mdi3D.item3D;

import java.awt.Color;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;

public class Cylinder extends Item3D {

	private float _radius;
	private float _x1;
	private float _y1;
	private float _z1;
	private float _x2;
	private float _y2;
	private float _z2;
	private Color _color;

	private boolean _extend;
	private float _extensionFactor = 2;

	/**
	 * Constructor for a Cylinder item in 3D space.
	 *
	 * @param panel3D The parent 3D panel
	 * @param x1      X coordinate of one end
	 * @param y1      Y coordinate of one end
	 * @param z1      Z coordinate of one end
	 * @param x2      X coordinate of the other end
	 * @param y2      Y coordinate of the other end
	 * @param z2      Z coordinate of the other end
	 * @param radius  Radius of the cylinder
	 * @param color   Color of the cylinder
	 */
	public Cylinder(Panel3D panel3D, float x1, float y1, float z1, float x2, float y2, float z2, float radius,
			Color color) {
		super(panel3D);
		_radius = radius;
		_x1 = x1;
		_y1 = y1;
		_z1 = z1;
		_x2 = x2;
		_y2 = y2;
		_z2 = z2;
		_color = color;
	}

	/**
	 * Set whether we draw the cylinder longer than its defining center line.
	 *
	 * @param extend if <code>true</code> draw the cylinder longer than its defining
	 *               center line.
	 */
	public void setExtend(boolean extend) {
		_extend = extend;
	}

	/**
	 * If drawing extended, what factor to extend by
	 *
	 * @param extensionFactor the extension factor
	 */
	public void setExtensionFactor(float extensionFactor) {
		_extensionFactor = extensionFactor;
	}

	public Cylinder(Panel3D panel3D, float data[], Color color) {
		this(panel3D, data[0], data[1], data[2], data[3], data[4], data[5], data[6], color);
	}

	@Override
	public void draw(GLAutoDrawable drawable) {
		if (_extend) {
			float dx = _x2 - _x1;
			float dy = _y2 - _y1;
			float dz = _z2 - _z1;

			float sm1 = _extensionFactor - 1;

			float x1 = _x1 - sm1 * dx;
			float x2 = _x1 + _extensionFactor * dx;

			float y1 = _y1 - sm1 * dy;
			float y2 = _y1 + _extensionFactor * dy;

			float z1 = _z1 - sm1 * dz;
			float z2 = _z1 + _extensionFactor * dz;
			Support3D.drawTube(drawable, x1, y1, z1, x2, y2, z2, _radius, _color);

		} else {
			Support3D.drawTube(drawable, _x1, _y1, _z1, _x2, _y2, _z2, _radius, _color);
		}
	}

	public void reset(float x1, float y1, float z1, float x2, float y2, float z2) {
		_x1 = x1;
		_y1 = y1;
		_z1 = z1;
		_x2 = x2;
		_y2 = y2;
		_z2 = z2;
	}

	@Override
	public float[] getSortPoint() {
		return new float[] { (_x1 + _x2) / 2, (_y1 + _y2) / 2, (_z1 + _z2) / 2 };
	}


}
