package edu.cnu.mdi.mdi3D.adapter3D;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.awt.GLJPanel;

import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.mdi3D.panel.Vector3f;

public class MouseAdapter3D implements MouseListener, MouseMotionListener, MouseWheelListener {

	protected int prevMouseX;
	protected int prevMouseY;

	protected Panel3D _panel3D;

	public MouseAdapter3D(Panel3D panel3D) {
		_panel3D = panel3D;
	}

	@Override
	public void mousePressed(MouseEvent e) {
		prevMouseX = e.getX();
		prevMouseY = e.getY();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseDragged(MouseEvent e) {

		final int x = e.getX();
		final int y = e.getY();
		int width = 0, height = 0;

		Object source = e.getSource();
		if (source instanceof GLJPanel) {
			GLJPanel window = (GLJPanel) source;
			width = window.getSurfaceWidth();
			height = window.getSurfaceHeight();
		} else if (source instanceof GLAutoDrawable) {
			GLAutoDrawable glad = (GLAutoDrawable) source;
			width = glad.getSurfaceWidth();
			height = glad.getSurfaceHeight();
		}

		// Normalize mouse coordinates to [-1, 1]
		float prevX = (2.0f * prevMouseX - width) / width;
		float prevY = (height - 2.0f * prevMouseY) / height;
		float currX = (2.0f * x - width) / width;
		float currY = (height - 2.0f * y) / height;

		// Map to the virtual arcball sphere
		Vector3f prevVec = mapToSphere(prevX, prevY);
		Vector3f currVec = mapToSphere(currX, currY);

		// Calculate rotation axis and angle
		Vector3f axis = prevVec.cross(currVec);
		float angle = (float) Math.acos(Math.min(1.0f, prevVec.dot(currVec)));

		if (axis.length() > 0.0001f) {
			axis.normalize();
			_panel3D.rotate(axis, angle);
		}

		prevMouseX = x;
		prevMouseY = y;

		_panel3D.refresh();

	}

	/**
	 * Maps 2D normalized device coordinates to a 3D point on the arcball sphere.
	 */
	private Vector3f mapToSphere(float x, float y) {
		float lengthSquared = x * x + y * y;
		if (lengthSquared <= 1.0f) {
			// Point lies on the sphere
			return new Vector3f(x, y, (float) Math.sqrt(1.0f - lengthSquared));
		} else {
			// Point is outside the sphere, normalize to the edge of the sphere
			float length = (float) Math.sqrt(lengthSquared);
			return new Vector3f(x / length, y / length, 0.0f);
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	private long lastWheelEventTime = 0;

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {

		long now = System.currentTimeMillis();
		long del = now - lastWheelEventTime;

		if (del < 100) {
			return;
		}

		lastWheelEventTime = now;

		int clicks = e.getWheelRotation();
		float maxZoomStep = _panel3D.getZStep() * 5; // Cap maximum zoom step
		float dz = _panel3D.getZStep() * clicks;

		// Clamp dz to avoid excessive zooming
		if (dz > maxZoomStep) {
			dz = maxZoomStep;
		} else if (dz < -maxZoomStep) {
			dz = -maxZoomStep;
		}

		_panel3D.deltaZ(dz);
		_panel3D.refresh();
	}
}
