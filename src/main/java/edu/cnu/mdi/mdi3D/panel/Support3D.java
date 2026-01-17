package edu.cnu.mdi.mdi3D.panel;

import java.awt.Color;

import com.jogamp.graph.geom.SVertex;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLUquadric;
import com.jogamp.opengl.util.gl2.GLUT;

public class Support3D {

	public static GLUT glut = new GLUT();

	private static GLUquadric _quad;

	/**
	 * Draw a set of points
	 *
	 * @param drawable the OpenGL drawable
	 * @param coords   the vertices as [x, y, z, x, y, z, ...]
	 * @param color    the color
	 * @param size     the points size
	 */
	public static void drawPoints(GLAutoDrawable drawable, float coords[], Color color, float size, boolean circular) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glPointSize(size);

		// how many points?
		int np = coords.length / 3;

		if (circular) {
			gl.glEnable(GL2ES1.GL_POINT_SMOOTH);
		} else {
			gl.glDisable(GL2ES1.GL_POINT_SMOOTH);
		}
		gl.glBegin(GL.GL_POINTS);
		setColor(gl, color);

		for (int i = 0; i < np; i++) {
			int j = i * 3;
			gl.glVertex3f(coords[j], coords[j + 1], coords[j + 2]);
		}

		gl.glEnd();
	}

	/**
	 * Draw a set of points
	 *
	 * @param drawable the OpenGL drawable
	 * @param coords   the vertices as [x, y, z, x, y, z, ...]
	 * @param fill     the fill color
	 * @param frame    the frame color
	 * @param size     the points size
	 */
	public static void drawPoints(GLAutoDrawable drawable, float coords[], Color fill, Color frame, float size,
			boolean circular) {
		if (frame == null) {
			drawPoints(drawable, coords, fill, size, circular);
		} else {
			drawPoints(drawable, coords, frame, size, circular);
			drawPoints(drawable, coords, fill, size - 2, circular);
		}
	}

	/**
	 * Draw a single point using double coordinates
	 *
	 * @param drawable the OpenGL drawable
	 * @param x        the x coordinate
	 * @param y        the y coordinate
	 * @param z        the z coordinate
	 *
	 * @param color    the color
	 * @param size     the point's pixel size
	 */
	public static void drawPoint(GLAutoDrawable drawable, double x, double y, double z, Color color, float size,
			boolean circular) {
		drawPoint(drawable, (float) x, (float) y, (float) z, color, size, circular);
	}

	/**
	 * Draw a point using float coordinates
	 *
	 * @param drawable the OpenGL drawable
	 * @param x        the x coordinate
	 * @param y        the y coordinate
	 * @param z        the z coordinate
	 *
	 * @param color    the color
	 * @param size     the points size
	 * @param circular
	 */
	public static void drawPoint(GLAutoDrawable drawable, float x, float y, float z, Color color, float size,
			boolean circular) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glPointSize(size);

		setColor(gl, color);
		if (circular) {
			gl.glEnable(GL2ES1.GL_POINT_SMOOTH);
		} else {
			gl.glDisable(GL2ES1.GL_POINT_SMOOTH);
		}

		gl.glBegin(GL.GL_POINTS);
		gl.glVertex3f(x, y, z);
		gl.glEnd();
	}

	/**
	 * Draw a marker (a point) with an associated text label. The marker is drawn as
	 * in drawPoint, and then a text label is rendered centered below the marker
	 * with a 3 pixel gap.
	 *
	 * @param drawable    the OpenGL drawable
	 * @param x           x coordinate of the marker
	 * @param y           y coordinate of the marker
	 * @param z           z coordinate of the marker
	 * @param markerColor color of the marker
	 * @param markerSize  pixel size of the marker
	 * @param circular    whether the marker should be drawn with smooth (circular)
	 *                    edges
	 * @param label       the text to display below the marker
	 * @param fontSize    scaling factor for the stroke font (in OpenGL units)
	 * @param fontColor   color of the text label
	 */
	public static void drawMarker(GLAutoDrawable drawable, float x, float y, float z, Color markerColor,
			float markerSize, boolean circular, String label, float fontSize, Color fontColor) {
		GL2 gl = drawable.getGL().getGL2();

		// Draw the marker (reuse your drawPoint method)
		// drawPoint(drawable, x, y, z, markerColor, markerSize, circular);

		// Retrieve the current matrices and viewport to project the marker position.
		int[] viewport = new int[4];
		double[] modelview = new double[16];
		double[] projection = new double[16];
		gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
		gl.glGetDoublev(GLMatrixFunc.GL_MODELVIEW_MATRIX, modelview, 0);
		gl.glGetDoublev(GLMatrixFunc.GL_PROJECTION_MATRIX, projection, 0);

		// Use gluProject to map the marker's 3D position to window (screen)
		// coordinates.
		double[] winCoords = new double[3];
		// Note: We are using Panel3D.glu as in your drawTube method.
		Panel3D.glu.gluProject(x, y, z, modelview, 0, projection, 0, viewport, 0, winCoords, 0);

		// Compute the text width (in pixels) using GLUT stroke font widths.
		// The GLUT stroke font returns a width in its native coordinate system,
		// so we multiply by our chosen fontSize.
		float textWidth = 0;
		for (int i = 0; i < label.length(); i++) {
			textWidth += glut.glutStrokeWidth(GLUT.STROKE_ROMAN, label.charAt(i)) * fontSize;
		}

		// Determine the text position:
		// We want the text centered horizontally at the marker’s x coordinate
		// and placed just below the marker. Since the marker is drawn with a size in
		// pixels,
		// we subtract half the marker size and then a further 3 pixels from the
		// marker’s screen y.
		double textWinX = winCoords[0] - textWidth / 2.0;
		double textWinY = winCoords[1] - markerSize / 2.0 - 3.0;

		// --- Switch to 2D orthographic projection for drawing text ---
		// Save the current projection matrix.
		gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		// Set up an orthographic projection covering the entire window.
		gl.glOrtho(0, viewport[2], 0, viewport[3], -1, 1);

		// Save the current modelview matrix.
		gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();

		// Disable depth testing so the text is not hidden by other geometry.
		gl.glDisable(GL.GL_DEPTH_TEST);

		// Set the font color.
		setColor(gl, fontColor);

		// Position the text.
		gl.glPushMatrix();
		// Translate to the computed window coordinates.
		// (Remember that in an orthographic projection as set above, (0,0) is at the
		// bottom left.)
		gl.glTranslated(textWinX, textWinY, 0);
		// Scale the stroke font by fontSize.
		gl.glScalef(fontSize, fontSize, fontSize);

		// Draw each character of the label using the GLUT stroke font.
		for (int i = 0; i < label.length(); i++) {
			glut.glutStrokeCharacter(GLUT.STROKE_ROMAN, label.charAt(i));
		}
		gl.glPopMatrix();

		// Re-enable depth testing.
		gl.glEnable(GL.GL_DEPTH_TEST);

		// Restore the modelview matrix.
		gl.glPopMatrix();
		// Restore the projection matrix.
		gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		gl.glPopMatrix();
		// Return to modelview matrix mode.
		gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
	}

	/**
	 * Draw a point using float coordinates and a point sprite
	 *
	 * @param drawable the OpenGL drawable
	 * @param x        the x coordinate
	 * @param y        the y coordinate
	 * @param z        the z coordinate
	 *
	 * @param color    the color
	 * @param size     the points size
	 */
	public static void drawPoint(GLAutoDrawable drawable, float x, float y, float z, Color color, float size) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glPointSize(size);

		setColor(gl, color);
		gl.glEnable(GL2ES1.GL_POINT_SPRITE);
		gl.glBegin(GL.GL_POINTS);
		gl.glVertex3f(x, y, z);
		gl.glEnd();
	}

	/**
	 * Draw a wire sphere
	 *
	 * @param drawable the OpenGL drawable
	 * @param x        x center
	 * @param y        y center
	 * @param z        z enter
	 * @param radius   radius in physical units
	 * @param slices   number of slices
	 * @param stacks   number of strips
	 * @param color    color of wires
	 */
	public static void wireSphere(GLAutoDrawable drawable, float x, float y, float z, float radius, int slices,
			int stacks, Color color) {
		GL2 gl = drawable.getGL().getGL2();
		setColor(gl, color);
		gl.glPushMatrix();
		gl.glTranslatef(x, y, z);
		glut.glutWireSphere(radius, slices, stacks);
		gl.glPopMatrix();

	}

	/**
	 * Draw a solid sphere
	 *
	 * @param drawable the OpenGL drawable
	 * @param x        x center
	 * @param y        y center
	 * @param z        z enter
	 * @param radius   radius in physical units
	 * @param slices   number of slices
	 * @param stacks   number of strips
	 * @param color    color of wires
	 */
	public static void solidSphere(GLAutoDrawable drawable, float x, float y, float z, float radius, int slices,
			int stacks, Color color) {
		GL2 gl = drawable.getGL().getGL2();
		setColor(gl, color);
		gl.glPushMatrix();
		gl.glTranslatef(x, y, z);
		glut.glutSolidSphere(radius, slices, stacks);
		gl.glPopMatrix();

	}

	public static void solidShadedSphere(GLAutoDrawable drawable, float x, float y, float z, float radius, int slices,
			int stacks, Color color, boolean enableLighting) {
		GL2 gl = drawable.getGL().getGL2();

// Set color
		setColor(gl, color);

// Enable lighting if requested
		if (enableLighting) {
			gl.glEnable(GLLightingFunc.GL_LIGHTING);
			gl.glEnable(GLLightingFunc.GL_LIGHT0);

// Define light properties
			float[] lightPosition = { 1.0f, 1.0f, 1.0f, 0.0f }; // Directional light
			float[] lightDiffuse = { 1.0f, 1.0f, 1.0f, 1.0f };
			float[] lightSpecular = { 1.0f, 1.0f, 1.0f, 1.0f };

			gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_POSITION, lightPosition, 0);
			gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_DIFFUSE, lightDiffuse, 0);
			gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_SPECULAR, lightSpecular, 0);

// Material properties
			float[] matAmbient = { 0.2f, 0.2f, 0.2f, 1.0f };
			float[] matDiffuse = { color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1.0f };
			float[] matSpecular = { 1.0f, 1.0f, 1.0f, 1.0f };
			float[] matShininess = { 50.0f }; // Shininess factor

			gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_AMBIENT, matAmbient, 0);
			gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_DIFFUSE, matDiffuse, 0);
			gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_SPECULAR, matSpecular, 0);
			gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_SHININESS, matShininess, 0);
		}

// Draw sphere
		gl.glPushMatrix();
		gl.glTranslatef(x, y, z);
		glut.glutSolidSphere(radius, slices, stacks);
		gl.glPopMatrix();

// Disable lighting after drawing
		if (enableLighting) {
			gl.glDisable(GLLightingFunc.GL_LIGHTING);
		}
	}

	/**
	 * Draws a spherical shell with a given inner and outer radius.
	 *
	 * @param drawable    the OpenGL drawable
	 * @param cx          x center
	 * @param cy          y center
	 * @param cz          z center
	 * @param innerRadius the inner radius of the shell
	 * @param outerRadius the outer radius of the shell
	 * @param slices      number of subdivisions around the Z axis (similar to
	 *                    longitude)
	 * @param stacks      number of subdivisions along the Z axis (similar to
	 *                    latitude)
	 * @param color       the color of the shell
	 */
	public static void solidSphereShell(GLAutoDrawable drawable, float cx, float cy, float cz, float innerRadius,
			float outerRadius, int slices, int stacks, Color color) {
		GL2 gl = drawable.getGL().getGL2();
		setColor(gl, color);
		gl.glPushMatrix();
		gl.glTranslatef(cx, cy, cz);

		// Draw outer surface (with outward facing normals)
		drawSphereSurface(gl, outerRadius, slices, stacks, false);

		// Draw inner surface (with inward facing normals)
		drawSphereSurface(gl, innerRadius, slices, stacks, true);

		// Connect the two surfaces by drawing side quads along each horizontal band.
		// This creates the "thickness" between the outer and inner spheres.
		for (int i = 0; i < stacks; i++) {
			float theta1 = (float) (i * Math.PI / stacks);
			float theta2 = (float) ((i + 1) * Math.PI / stacks);
			gl.glBegin(GL2.GL_QUAD_STRIP);
			for (int j = 0; j <= slices; j++) {
				float phi = (float) (j * 2 * Math.PI / slices);
				float sinTheta1 = (float) Math.sin(theta1);
				float cosTheta1 = (float) Math.cos(theta1);
				float sinTheta2 = (float) Math.sin(theta2);
				float cosTheta2 = (float) Math.cos(theta2);
				float sinPhi = (float) Math.sin(phi);
				float cosPhi = (float) Math.cos(phi);

				// Outer vertices
				float xOuter1 = outerRadius * sinTheta1 * cosPhi;
				float yOuter1 = outerRadius * cosTheta1;
				float zOuter1 = outerRadius * sinTheta1 * sinPhi;

				float xOuter2 = outerRadius * sinTheta2 * cosPhi;
				float yOuter2 = outerRadius * cosTheta2;
				float zOuter2 = outerRadius * sinTheta2 * sinPhi;

				// Inner vertices
				float xInner1 = innerRadius * sinTheta1 * cosPhi;
				float yInner1 = innerRadius * cosTheta1;
				float zInner1 = innerRadius * sinTheta1 * sinPhi;

				float xInner2 = innerRadius * sinTheta2 * cosPhi;
				float yInner2 = innerRadius * cosTheta2;
				float zInner2 = innerRadius * sinTheta2 * sinPhi;

				// Create a quad strip between outer and inner surfaces.
				// For each band, we connect the corresponding outer and inner vertices.
				gl.glVertex3f(xOuter1, yOuter1, zOuter1);
				gl.glVertex3f(xInner1, yInner1, zInner1);
				gl.glVertex3f(xOuter2, yOuter2, zOuter2);
				gl.glVertex3f(xInner2, yInner2, zInner2);
			}
			gl.glEnd();
		}

		gl.glPopMatrix();
	}

	/**
	 * Draws the surface of a sphere.
	 *
	 * @param gl            the GL2 context
	 * @param radius        the radius of the sphere
	 * @param slices        number of subdivisions around the Z axis
	 * @param stacks        number of subdivisions along the Z axis
	 * @param invertNormals if true, normals are inverted (useful for inner
	 *                      surfaces)
	 */
	private static void drawSphereSurface(GL2 gl, float radius, int slices, int stacks, boolean invertNormals) {
		for (int i = 0; i < stacks; i++) {
			float theta1 = (float) (i * Math.PI / stacks);
			float theta2 = (float) ((i + 1) * Math.PI / stacks);
			gl.glBegin(GL2.GL_QUAD_STRIP);
			for (int j = 0; j <= slices; j++) {
				float phi = (float) (j * 2 * Math.PI / slices);
				float sinTheta1 = (float) Math.sin(theta1);
				float cosTheta1 = (float) Math.cos(theta1);
				float sinTheta2 = (float) Math.sin(theta2);
				float cosTheta2 = (float) Math.cos(theta2);
				float sinPhi = (float) Math.sin(phi);
				float cosPhi = (float) Math.cos(phi);

				// Compute positions
				float x1 = radius * sinTheta1 * cosPhi;
				float y1 = radius * cosTheta1;
				float z1 = radius * sinTheta1 * sinPhi;
				float x2 = radius * sinTheta2 * cosPhi;
				float y2 = radius * cosTheta2;
				float z2 = radius * sinTheta2 * sinPhi;

				// Compute normals (invert if necessary)
				if (invertNormals) {
					gl.glNormal3f(-x1 / radius, -y1 / radius, -z1 / radius);
					gl.glVertex3f(x1, y1, z1);
					gl.glNormal3f(-x2 / radius, -y2 / radius, -z2 / radius);
					gl.glVertex3f(x2, y2, z2);
				} else {
					gl.glNormal3f(x1 / radius, y1 / radius, z1 / radius);
					gl.glVertex3f(x1, y1, z1);
					gl.glNormal3f(x2 / radius, y2 / radius, z2 / radius);
					gl.glVertex3f(x2, y2, z2);
				}
			}
			gl.glEnd();
		}
	}

	/**
	 * Draw a rectangular solid
	 *
	 * @param drawable
	 * @param xc
	 * @param yc
	 * @param zc
	 * @param xw
	 * @param yw
	 * @param zw
	 * @param fc        * @param lc
	 * @param lineWidth
	 * @param frame
	 */
	public static void drawRectangularSolid(GLAutoDrawable drawable, float xc, float yc, float zc, float xw, float yw,
			float zw, Color fc, float lineWidth, boolean frame) {
		drawRectangularSolid(drawable, xc, yc, zc, xw, yw, zw, fc, null, lineWidth, frame);
	}

	/**
	 * Draw a rectangular solid
	 *
	 * @param drawable
	 * @param xc
	 * @param yc
	 * @param zc
	 * @param xw
	 * @param yw
	 * @param zw
	 * @param fc
	 * @param lc
	 * @param lineWidth
	 * @param frame
	 */
	public static void drawRectangularSolid(GLAutoDrawable drawable, float xc, float yc, float zc, float xw, float yw,
			float zw, Color fc, Color lc, float lineWidth, boolean frame) {
		GL2 gl = drawable.getGL().getGL2();

		float xm = xc - xw / 2;
		float xp = xc + xw / 2;
		float ym = yc - yw / 2;
		float yp = yc + yw / 2;
		float zm = zc - zw / 2;
		float zp = zc + zw / 2;

		Support3D.setColor(gl, fc);
		gl.glBegin(GL2ES3.GL_QUADS);
		gl.glVertex3f(xm, ym, zp);
		gl.glVertex3f(xm, yp, zp);
		gl.glVertex3f(xp, yp, zp);
		gl.glVertex3f(xp, ym, zp);
		gl.glEnd();

		gl.glBegin(GL2ES3.GL_QUADS);
		gl.glVertex3f(xm, ym, zm);
		gl.glVertex3f(xm, yp, zm);
		gl.glVertex3f(xp, yp, zm);
		gl.glVertex3f(xp, ym, zm);
		gl.glEnd();

		gl.glBegin(GL2ES3.GL_QUADS);
		gl.glVertex3f(xm, yp, zm);
		gl.glVertex3f(xm, yp, zp);
		gl.glVertex3f(xp, yp, zp);
		gl.glVertex3f(xp, yp, zm);
		gl.glEnd();

		gl.glBegin(GL2ES3.GL_QUADS);
		gl.glVertex3f(xm, ym, zm);
		gl.glVertex3f(xm, ym, zp);
		gl.glVertex3f(xp, ym, zp);
		gl.glVertex3f(xp, ym, zm);
		gl.glEnd();

		gl.glBegin(GL2ES3.GL_QUADS);
		gl.glVertex3f(xp, yp, zm);
		gl.glVertex3f(xp, yp, zp);
		gl.glVertex3f(xp, ym, zp);
		gl.glVertex3f(xp, ym, zm);
		gl.glEnd();

		gl.glBegin(GL2ES3.GL_QUADS);
		gl.glVertex3f(xm, yp, zm);
		gl.glVertex3f(xm, yp, zp);
		gl.glVertex3f(xm, ym, zp);
		gl.glVertex3f(xm, ym, zm);
		gl.glEnd();

		if (frame) {

			if (lc == null) {
				lc = fc.darker();
			}
			Support3D.setColor(gl, lc);

			gl.glBegin(GL.GL_LINE_STRIP);
			gl.glVertex3f(xm, yp, zm);
			gl.glVertex3f(xm, yp, zp);
			gl.glVertex3f(xm, ym, zp);
			gl.glVertex3f(xm, ym, zm);
			gl.glVertex3f(xm, yp, zm);
			gl.glEnd();

			gl.glBegin(GL.GL_LINE_STRIP);
			gl.glVertex3f(xm, ym, zm);
			gl.glVertex3f(xm, yp, zm);
			gl.glVertex3f(xp, yp, zm);
			gl.glVertex3f(xp, ym, zm);
			gl.glVertex3f(xm, ym, zm);
			gl.glEnd();

			gl.glBegin(GL.GL_LINE_STRIP);
			gl.glVertex3f(xm, yp, zm);
			gl.glVertex3f(xm, yp, zp);
			gl.glVertex3f(xp, yp, zp);
			gl.glVertex3f(xp, yp, zm);
			gl.glVertex3f(xm, yp, zm);
			gl.glEnd();

			gl.glBegin(GL.GL_LINE_STRIP);
			gl.glVertex3f(xm, ym, zm);
			gl.glVertex3f(xm, ym, zp);
			gl.glVertex3f(xp, ym, zp);
			gl.glVertex3f(xp, ym, zm);
			gl.glVertex3f(xm, ym, zm);
			gl.glEnd();

			gl.glBegin(GL.GL_LINE_STRIP);
			gl.glVertex3f(xp, yp, zm);
			gl.glVertex3f(xp, yp, zp);
			gl.glVertex3f(xp, ym, zp);
			gl.glVertex3f(xp, ym, zm);
			gl.glVertex3f(xp, yp, zm);
			gl.glEnd();

			gl.glBegin(GL.GL_LINE_STRIP);
			gl.glVertex3f(xm, yp, zm);
			gl.glVertex3f(xm, yp, zp);
			gl.glVertex3f(xm, ym, zp);
			gl.glVertex3f(xm, ym, zm);
			gl.glVertex3f(xm, yp, zm);
			gl.glEnd();
		}
		gl.glLineWidth(1f);

	}

	/**
	 * @param drawable  the openGL drawable
	 * @param coords    the coordinate array
	 * @param color     the color
	 * @param lineWidth the line width
	 * @param frame     if <code>true</code> frame in slightly darker color
	 */
	public static void drawQuads(GLAutoDrawable drawable, float coords[], Color color, float lineWidth, boolean frame) {

		drawQuads(drawable, coords, color, (frame ? color.darker() : null), lineWidth);
	}

	/**
	 * @param drawable  the openGL drawable
	 * @param coords    the coordinate array
	 * @param color     the color
	 * @param lineWidth the line width
	 * @param frame     if <code>true</code> frame in slightly darker color
	 */
	public static void drawQuads(GLAutoDrawable drawable, float coords[], Color color, Color lineColor,
			float lineWidth) {

		GL2 gl = drawable.getGL().getGL2();
		gl.glLineWidth(lineWidth);

		gl.glBegin(GL2ES3.GL_QUADS);
		setColor(gl, color);

		int numPoints = coords.length / 3;

		for (int i = 0; i < numPoints; i++) {
			int j = 3 * i;
			gl.glVertex3f(coords[j], coords[j + 1], coords[j + 2]);
		}

		gl.glEnd();

		if (lineColor != null) {

			// a quad has four vertices therefor 12 points
			int numQuad = coords.length / 12;
			for (int i = 0; i < numQuad; i++) {
				gl.glBegin(GL.GL_LINE_STRIP);
				setColor(gl, lineColor);

				int j = i * 12;

				gl.glVertex3f(coords[j++], coords[j++], coords[j++]);
				gl.glVertex3f(coords[j++], coords[j++], coords[j++]);
				gl.glVertex3f(coords[j++], coords[j++], coords[j++]);
				gl.glVertex3f(coords[j++], coords[j++], coords[j++]);

				j = i * 12;
				gl.glVertex3f(coords[j++], coords[j++], coords[j++]);

				gl.glEnd();
			}
		}

		gl.glLineWidth(1f);

	}

	/**
	 * @param drawable  the openGL drawable
	 * @param coords    the coordinate array
	 * @param index1    index into first vertex
	 * @param index2    index into second vertex
	 * @param index3    index into third vertex
	 * @param index4    index into fourth vertex
	 * @param color     the color
	 * @param lineWidth the line width
	 * @param frame     if <code>true</code> frame in slightly darker color
	 */
	public static void drawQuad(GLAutoDrawable drawable, float coords[], int index1, int index2, int index3, int index4,
			Color color, float lineWidth, boolean frame) {

		int i1 = 3 * index1;
		int i2 = 3 * index2;
		int i3 = 3 * index3;
		int i4 = 3 * index4;

		GL2 gl = drawable.getGL().getGL2();
		gl.glLineWidth(lineWidth);

		gl.glBegin(GL2ES3.GL_QUADS);
		setColor(gl, color);
		gl.glVertex3f(coords[i1], coords[i1 + 1], coords[i1 + 2]);
		gl.glVertex3f(coords[i2], coords[i2 + 1], coords[i2 + 2]);
		gl.glVertex3f(coords[i3], coords[i3 + 1], coords[i3 + 2]);
		gl.glVertex3f(coords[i4], coords[i4 + 1], coords[i4 + 2]);

		gl.glEnd();

		if (frame) {
			gl.glBegin(GL.GL_LINE_STRIP);
			setColor(gl, color.darker());

			gl.glVertex3f(coords[i1], coords[i1 + 1], coords[i1 + 2]);
			gl.glVertex3f(coords[i2], coords[i2 + 1], coords[i2 + 2]);
			gl.glVertex3f(coords[i3], coords[i3 + 1], coords[i3 + 2]);
			gl.glVertex3f(coords[i4], coords[i4 + 1], coords[i4 + 2]);
			gl.glVertex3f(coords[i1], coords[i1 + 1], coords[i1 + 2]);
			gl.glEnd();
		}

		gl.glLineWidth(1f);

	}

	/**
	 *
	 * @param drawable  the OpenGL drawable
	 * @param coords    the triangle as [x1, y1, ..., y3, z3]
	 * @param color     the color
	 * @param lineWidth the line width
	 * @param frame     if <code>true</code> frame in slightly darker color
	 * @param lineWidth
	 */
	public static void drawTriangles(GLAutoDrawable drawable, float coords[], Color color, float lineWidth,
			boolean frame) {
		int numTriangle = coords.length / 9;
		for (int i = 0; i < numTriangle; i++) {
			int j = 3 * i;
			drawTriangle(drawable, coords, j, j + 1, j + 2, color, lineWidth, frame);
		}
	}

	/**
	 * Break one triangle into smaller triangles
	 *
	 * @param coords the triangle as [x1, y1, ..., y3, z3]
	 * @param level  [1..] number of times called recursively. If level is n, get
	 *               4^n triangles
	 * @return all the triangles in a coordinate array
	 */
	public static float[] triangulateTriangle(float coords[], int level) {
		if (level < 1) {
			return coords;
		}
		float tricoords[] = oneToFourTriangle(coords);

		for (int lev = 2; lev <= level; lev++) {
			int numtri = tricoords.length / 9;
			int numNewTri = 4 * numtri;
			float[] tri[] = new float[numtri][];
			float allTris[] = new float[9 * numNewTri];
			for (int i = 0; i < numtri; i++) {
				tri[i] = oneToFourTriangle(tricoords, i);
				System.arraycopy(tri[i], 0, allTris, 36 * i, 36);
			}
			tricoords = allTris;

		}
		return tricoords;
	}

	/**
	 * Break one triangle into four by connecting the midpoints
	 *
	 * @param coords the triangle as [x1, y1, ..., y3, z3] starting at index
	 * @param index  to first vertex, where coords is assume to contain a list of
	 *               triangles each one requiring 9 numbers
	 * @return all four triangles in a coordinate array
	 */
	public static float[] oneToFourTriangle(float coords[], int index) {
		Vector3f p[] = new Vector3f[6];

		int j = 3 * index;

		p[0] = new Vector3f(coords, j + 0);
		p[1] = new Vector3f(coords, j + 1);
		p[2] = new Vector3f(coords, j + 2);

		p[3] = Vector3f.midpoint(p[0], p[1]);
		p[4] = Vector3f.midpoint(p[1], p[2]);
		p[5] = Vector3f.midpoint(p[2], p[0]);

		float coords4[] = new float[36];

		fillCoords(coords4, 0, p[0], p[3], p[5]);
		fillCoords(coords4, 1, p[1], p[3], p[4]);
		fillCoords(coords4, 2, p[3], p[4], p[5]);
		fillCoords(coords4, 3, p[2], p[4], p[5]);
		// System.err.println("Found Triangles");
		return coords4;
	}

	/**
	 * Break one triangle into four by connecting the midpoints
	 *
	 * @param coords the triangle as [x1, y1, ..., y3, z3]
	 * @return all four triangles in a coordinate array
	 */
	public static float[] oneToFourTriangle(float coords[]) {
		return oneToFourTriangle(coords, 0);
	}

	// create a coords array by appending 3D points
	private static void fillCoords(float coords[], int index, Vector3f... p) {

		int size = 3 * p.length;
		int i = size * index;

		for (Vector3f v3f : p) {
			coords[i++] = v3f.x;
			coords[i++] = v3f.y;
			coords[i++] = v3f.z;
		}
	}

	/**
	 * Draw a triangle from a coordinate array
	 *
	 * @param drawable  the OpenGL drawable
	 * @param coords    a set of points
	 * @param index1    "three index" of start of first corner, which will be the
	 *                  next three entries in the coords array
	 * @param index2    "three index" of start of second corner, which will be the
	 *                  next three entries in the coords array
	 * @param index3    "three index" of start of third corner, which will be the
	 *                  next three entries in the coords array
	 * @param color     the color the fill color
	 * @param lineWidth the line width in pixels (if framed)
	 * @param frame     if <code>true</code> frame in slightly darker color
	 * @param lineWidth
	 */
	public static void drawTriangle(GLAutoDrawable drawable, float coords[], int index1, int index2, int index3,
			Color color, float lineWidth, boolean frame) {

		int i1 = 3 * index1;
		int i2 = 3 * index2;
		int i3 = 3 * index3;

		GL2 gl = drawable.getGL().getGL2();
		gl.glLineWidth(lineWidth);

		gl.glBegin(GL.GL_TRIANGLES);
		setColor(gl, color);
		gl.glVertex3f(coords[i1], coords[i1 + 1], coords[i1 + 2]);
		gl.glVertex3f(coords[i2], coords[i2 + 1], coords[i2 + 2]);
		gl.glVertex3f(coords[i3], coords[i3 + 1], coords[i3 + 2]);

		gl.glEnd();

		if (frame) {
			gl.glBegin(GL.GL_LINE_STRIP);
			setColor(gl, color.darker());

			gl.glVertex3f(coords[i1], coords[i1 + 1], coords[i1 + 2]);
			gl.glVertex3f(coords[i2], coords[i2 + 1], coords[i2 + 2]);
			gl.glVertex3f(coords[i3], coords[i3 + 1], coords[i3 + 2]);
			gl.glVertex3f(coords[i1], coords[i1 + 1], coords[i1 + 2]);
			gl.glEnd();
		}
		gl.glLineWidth(1f);
	}

	/**
	 * Draw a cone
	 *
	 * @param drawable the OpenGL drawable
	 * @param x1       x coordinate of center of base
	 * @param y1       y coordinate of center of base
	 * @param z1       z coordinate of center of base
	 * @param x2       x coordinate of tip
	 * @param y2       y coordinate of tip
	 * @param z2       z coordinate of tip
	 * @param radius   radius of base
	 * @param color    color of cone
	 */
	public static void drawCone(GLAutoDrawable drawable, float x1, float y1, float z1, float x2, float y2, float z2,
			float radius, Color color) {

		float vx = x2 - x1;
		float vy = y2 - y1;
		float vz = z2 - z1;
		if (Math.abs(vz) < 1.0e-5) {
			vz = 0.0001f;
		}

		float v = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
		float ax = (float) (57.2957795 * Math.acos(vz / v));
		if (vz < 0.0) {
			ax = -ax;
		}
		float rx = -vy * vz;
		float ry = vx * vz;

		GL2 gl = drawable.getGL().getGL2();
		setColor(gl, color);

		gl.glPushMatrix();
		// draw the cylinder body
		gl.glTranslatef(x1, y1, z1);
		gl.glRotatef(ax, rx, ry, 0f);

		glut.glutSolidCone(radius, v, 20, 20);

		gl.glPopMatrix();
	}

	/**
	 * Draw a 3D tube
	 *
	 * @param drawable the OpenGL drawable
	 * @param x1       x coordinate of one end
	 * @param y1       y coordinate of one end
	 * @param z1       z coordinate of one end
	 * @param x2       x coordinate of other end
	 * @param y2       y coordinate of other end
	 * @param z2       z coordinate of other end
	 * @param radius   the radius of the tube
	 * @param color    the color of the tube
	 */
	public static void drawTube(GLAutoDrawable drawable, float x1, float y1, float z1, float x2, float y2, float z2,
			float radius, Color color) {

		if (_quad == null) {
			_quad = Panel3D.glu.gluNewQuadric();
		}

		float vx = x2 - x1;
		float vy = y2 - y1;
		float vz = z2 - z1;
		if (Math.abs(vz) < 1.0e-5) {
			vz = 0.0001f;
		}

		float v = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
		float ax = (float) (57.2957795 * Math.acos(vz / v));
		if (vz < 0.0) {
			ax = -ax;
		}
		float rx = -vy * vz;
		float ry = vx * vz;

		GL2 gl = drawable.getGL().getGL2();
		setColor(gl, color);

		gl.glPushMatrix();
		// draw the cylinder body
		gl.glTranslatef(x1, y1, z1);
		gl.glRotatef(ax, rx, ry, 0f);
		// gluQuadricOrientation(quadric,GLU_OUTSIDE);
		Panel3D.glu.gluCylinder(_quad, radius, radius, v, 50, 1);

		gl.glPopMatrix();
	}

	/**
	 * @param drawable  the OpenGL drawable
	 * @param x1        x coordinate of start
	 * @param y1        y coordinate of start
	 * @param z1        z coordinate of start
	 * @param ux        x component of unit vector direction
	 * @param uy        y component of unit vector direction
	 * @param uz        z component of unit vector direction
	 * @param length    the length of the line
	 * @param color     the color
	 * @param lineWidth the line width
	 */
	public static void drawLine(GLAutoDrawable drawable, float x1, float y1, float z1, float ux, float uy, float uz,
			float length, Color color, float lineWidth) {

		float x2 = x1 + length * ux;
		float y2 = y1 + length * uy;
		float z2 = z1 + length * uz;

		drawLine(drawable, x1, y1, z1, x2, y2, z2, color, lineWidth);
	}

	/**
	 * Draw a 3D line
	 *
	 * @param drawable  the OpenGL drawable
	 * @param x1        x coordinate of one end
	 * @param y1        y coordinate of one end
	 * @param z1        z coordinate of one end
	 * @param x2        x coordinate of other end
	 * @param y2        y coordinate of other end
	 * @param z2        z coordinate of other end
	 * @param color     the color
	 * @param lineWidth the line width in pixels
	 */
	public static void drawLine(GLAutoDrawable drawable, float x1, float y1, float z1, float x2, float y2, float z2,
			Color color, float lineWidth) {

		GL2 gl = drawable.getGL().getGL2();
		gl.glLineWidth(lineWidth);

		gl.glBegin(GL.GL_LINES);
		setColor(gl, color);
		gl.glVertex3f(x1, y1, z1);
		gl.glVertex3f(x2, y2, z2);
		gl.glEnd();
		gl.glLineWidth(1f);
	}

	/**
	 * Draw a 3D line (convert double args to float)
	 *
	 * @param drawable  the OpenGL drawable
	 * @param x1        x coordinate of one end
	 * @param y1        y coordinate of one end
	 * @param z1        z coordinate of one end
	 * @param x2        x coordinate of other end
	 * @param y2        y coordinate of other end
	 * @param z2        z coordinate of other end
	 * @param color     the color
	 * @param lineWidth the line width in pixels
	 */
	public static void drawLine(GLAutoDrawable drawable, double x1, double y1, double z1, double x2, double y2,
			double z2, Color color, float lineWidth) {
		drawLine(drawable, (float) x1, (float) y1, (float) z1, (float) x2, (float) y2, (float) z2, color, lineWidth);
	}

	/**
	 * Draw a 3D line
	 *
	 * @param drawable  the OpenGL drawable
	 * @param p0        one end point as [x, y, z]
	 * @param p1        other end point as [x, y, z]
	 * @param color     the color
	 * @param lineWidth the line width in pixels
	 */
	public static void drawLine(GLAutoDrawable drawable, float[] p0, float[] p1, Color color, float lineWidth) {

		drawLine(drawable, p0[0], p0[1], p0[2], p1[0], p1[1], p1[2], color, lineWidth);
	}

	/**
	 * Draw a 3D line
	 *
	 * @param drawable  the OpenGL drawable
	 * @param coords    the line as [x1, y1, z1, x2, y2, z2]
	 * @param color     the color
	 * @param lineWidth the line width in pixels
	 */
	public static void drawLine(GLAutoDrawable drawable, float[] coords, Color color, float lineWidth) {

		drawLine(drawable, coords[0], coords[1], coords[2], coords[3], coords[4], coords[5], color, lineWidth);
	}

	/**
	 * Draw a polyline
	 *
	 * @param drawable  the OpenGL drawable
	 * @param coords    the vertices as [x, y, z, x, y, z, ...]
	 * @param color     the color
	 * @param lineWidth the line width
	 */
	public static void drawPolyLine(GLAutoDrawable drawable, float[] coords, Color color, float lineWidth) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glLineWidth(lineWidth);

		int np = coords.length / 3;

		gl.glBegin(GL.GL_LINE_STRIP);
		setColor(gl, color);

		for (int i = 0; i < np; i++) {
			int j = i * 3;
			gl.glVertex3f(coords[j], coords[j + 1], coords[j + 2]);
		}
		gl.glEnd();
		gl.glLineWidth(1f);
	}

	/**
	 * Draw a two color 3D line
	 *
	 * @param drawable  the OpenGL drawable
	 * @param gl        the gl context
	 * @param x1        x coordinate of one end
	 * @param y1        y coordinate of one end
	 * @param z1        z coordinate of one end
	 * @param x2        x coordinate of other end
	 * @param y2        y coordinate of other end
	 * @param z2        z coordinate of other end
	 * @param color1    one color
	 * @param color2    other color
	 * @param lineWidth the line width in pixels
	 */
	public static void drawLine(GLAutoDrawable drawable, float x1, float y1, float z1, float x2, float y2, float z2,
			Color color1, Color color2, float lineWidth) {

		GL2 gl = drawable.getGL().getGL2();
		gl.glEnable(GL2.GL_LINE_STIPPLE);
		gl.glLineWidth(lineWidth);

		if (color1 != null) {
			gl.glLineStipple(1, (short) 0x00FF); /* dashed */
			gl.glBegin(GL.GL_LINES);
			setColor(gl, color1);
			gl.glVertex3f(x1, y1, z1);
			gl.glVertex3f(x2, y2, z2);
			gl.glEnd();
		}
		if (color2 != null) {
			gl.glLineStipple(1, (short) 0xFF00); /* dashed */
			gl.glBegin(GL.GL_LINES);
			setColor(gl, color2);
			gl.glVertex3f(x1, y1, z1);
			gl.glVertex3f(x2, y2, z2);
			gl.glEnd();
		}

		gl.glDisable(GL2.GL_LINE_STIPPLE);
		gl.glLineWidth(1f);

	}

	/**
	 * Draw a two color polyline
	 *
	 * @param drawable  the OpenGL drawable
	 * @param coords    the vertices as [x, y, z, x, y, z, ...]
	 * @param color1    one color
	 * @param color2    other color
	 * @param lineWidth the line width in pixels
	 */
	public static void drawPolyLine(GLAutoDrawable drawable, float[] coords, Color color1, Color color2,
			float lineWidth) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glLineWidth(lineWidth);
		gl.glEnable(GL2.GL_LINE_STIPPLE);

		int np = coords.length / 3;

		if (color1 != null) {
			gl.glLineStipple(1, (short) 0x00FF); /* dashed */
			gl.glBegin(GL.GL_LINE_STRIP);
			setColor(gl, color1);

			for (int i = 0; i < np; i++) {
				int j = i * 3;
				gl.glVertex3f(coords[j], coords[j + 1], coords[j + 2]);
			}
			gl.glEnd();
		}
		if (color2 != null) {
			gl.glLineStipple(1, (short) 0xFF00); /* dashed */
			gl.glBegin(GL.GL_LINE_STRIP);
			setColor(gl, color2);

			for (int i = 0; i < np; i++) {
				int j = i * 3;
				gl.glVertex3f(coords[j], coords[j + 1], coords[j + 2]);
			}
			gl.glEnd();
		}

		gl.glDisable(GL2.GL_LINE_STIPPLE);
		gl.glLineWidth(1f);

	}

	/**
	 * Draw and fill a spherical polygon
	 *
	 * @param drawable  the OpenGL drawable
	 * @param radius    the sphere radius
	 * @param coords    the vertices as [theta phi, theta phi, …] in radians
	 * @param lineColor the line color
	 * @param fillColor the fill color
	 * @param lineWidth the line width in pixels
	 */
	public static void drawSphericalPolygon(GLAutoDrawable drawable, float radius, float[] coords, Color lineColor,
			Color fillColor, float lineWidth) {
		GL2 gl = drawable.getGL().getGL2();

		int numPoints = coords.length / 2;
		float[] cartesianCoords = new float[numPoints * 3];

		// Convert spherical to Cartesian coordinates
		for (int i = 0; i < numPoints; i++) {
			float theta = coords[2 * i];
			float phi = coords[2 * i + 1];

			cartesianCoords[3 * i] = radius * (float) (Math.sin(theta) * Math.cos(phi));
			cartesianCoords[3 * i + 1] = radius * (float) (Math.sin(theta) * Math.sin(phi));
			cartesianCoords[3 * i + 2] = radius * (float) Math.cos(theta);
		}

		// Fill the polygon
		if (fillColor != null) {
			gl.glBegin(GL2.GL_POLYGON);
			Support3D.setColor(gl, fillColor);
			for (int i = 0; i < numPoints; i++) {
				gl.glVertex3f(cartesianCoords[3 * i], cartesianCoords[3 * i + 1], cartesianCoords[3 * i + 2]);
			}
			gl.glEnd();
		}

		// Draw the border
		if (lineColor != null) {
			gl.glLineWidth(lineWidth);
			gl.glBegin(GL.GL_LINE_LOOP);
			Support3D.setColor(gl, lineColor);
			for (int i = 0; i < numPoints; i++) {
				gl.glVertex3f(cartesianCoords[3 * i], cartesianCoords[3 * i + 1], cartesianCoords[3 * i + 2]);
			}
			gl.glEnd();
		}

		gl.glLineWidth(1.0f); // Reset line width to default
	}

	/**
	 * Set a color based on an awt color
	 *
	 * @param gl    the graphics context
	 * @param color the awt color
	 */
	public static void setColor(GL2 gl, Color color) {
		float r = color.getRed() / 255f;
		float g = color.getGreen() / 255f;
		float b = color.getBlue() / 255f;
		float a = color.getAlpha() / 255f;
		gl.glColor4f(r, g, b, a);
	}

	/**
	 * Get a simple vertex
	 *
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @return a vertex
	 */
	public static SVertex vertex(float x, float y, float z) {
		return new SVertex(x, y, z, false);
	}

	/**
	 * Convenience method to convert a variable list of floats into a float array.
	 *
	 * @param v the variable length list of floats
	 * @return the corresponding array
	 */
	public static float[] toArray(float... v) {
		return v;
	}

}
