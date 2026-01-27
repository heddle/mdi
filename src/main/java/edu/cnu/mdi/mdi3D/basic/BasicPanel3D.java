package edu.cnu.mdi.mdi3D.basic;

import java.awt.Color;
import java.awt.Dimension;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLJPanel;

@SuppressWarnings("serial")
public class BasicPanel3D extends GLJPanel implements GLEventListener {

	// the preferred size
	private final Dimension _preferredSize;

	public BasicPanel3D(Dimension preferredSize) {
		_preferredSize = (preferredSize != null) ? preferredSize : new Dimension(600, 400);
		addGLEventListener(this);
	}

	@Override
	public Dimension getPreferredSize() {
		return _preferredSize;
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		System.out.println("Got a GLEventListener init event.");
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		System.out.println("Got a GLEventListener dispose event.");
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		float x1[] = { -0.5f, 0f, 0f };
		float y1[] = { -0.5f, 0.5f, 0.5f };
		float z1[] = { 0f, 0f, 0f };
		float x2[] = { 0.5f, -0.5f, 0.5f };
		float y2[] = { -0.5f, -0.5f, -0.5f };
		float z2[] = { 0f, 0f, 0f };
		BasicLineDrawing.drawLines(drawable, x1, y1, z1, x2, y2, z2, Color.yellow);
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		System.out.println("Got a GLEventListener reshape event.");
	}

}
