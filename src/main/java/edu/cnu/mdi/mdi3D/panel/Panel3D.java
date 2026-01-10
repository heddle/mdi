package edu.cnu.mdi.mdi3D.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;

import edu.cnu.mdi.mdi3D.adapter3D.KeyAdapter3D;
import edu.cnu.mdi.mdi3D.adapter3D.KeyBindings3D;
import edu.cnu.mdi.mdi3D.adapter3D.MouseAdapter3D;
import edu.cnu.mdi.mdi3D.item3D.Axes3D;
import edu.cnu.mdi.mdi3D.item3D.Cube;
import edu.cnu.mdi.mdi3D.item3D.Cylinder;
import edu.cnu.mdi.mdi3D.item3D.Item3D;
import edu.cnu.mdi.mdi3D.item3D.PointSet3D;
import edu.cnu.mdi.mdi3D.item3D.Triangle3D;

@SuppressWarnings("serial")
public class Panel3D extends JPanel implements GLEventListener {
	protected float[] rotationMatrix = new float[16];

	// background default color used for r, g and b
	public static final float BGFEFAULT = 0.9804f;

	// the actual components of the background
	private float _bgRed = BGFEFAULT;
	private float _bgGreen = BGFEFAULT;
	private float _bgBlue = BGFEFAULT;

	public float _xscale = 1.0f;
	public float _yscale = 1.0f;
	public float _zscale = 1.0f;

	protected GLProfile glprofile;
	protected GLCapabilities glcapabilities;
	protected final GLJPanel gljpanel;
	public static GLU glu; // glu utilities

	// distance in front of the screen
	private float _zdist;

	// x and y translation
	private float _xdist;
	private float _ydist;

	// the list of 3D items to be drawn
	protected Vector<Item3D> _itemList = new Vector<>();

	// listen for mouse events
	protected MouseAdapter3D _mouseAdapter;

	// listen for key events
	protected KeyAdapter3D _keyAdapter;

	protected String _rendererStr;

	private boolean _skipLastStage = false;

	/*
	 * The panel that holds the 3D objects
	 *
	 * @param angleX the initial x rotation angle in degrees
	 *
	 * @param angleY the initial y rotation angle in degrees
	 *
	 * @param angleZ the initial z rotation angle in degrees
	 *
	 * @param xdist move viewpoint left/right
	 *
	 * @param ydist move viewpoint up/down
	 *
	 * @param zdist the initial viewer z distance should be negative
	 */
	public Panel3D(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		this(angleX, angleY, angleZ, xDist, yDist, zDist, BGFEFAULT, BGFEFAULT, BGFEFAULT, false);
	}

	/*
	 * The panel that holds the 3D objects
	 *
	 * @param angleX the initial x rotation angle in degrees
	 *
	 * @param angleY the initial y rotation angle in degrees
	 *
	 * @param angleZ the initial z rotation angle in degrees
	 *
	 * @param xdist move viewpoint left/right
	 *
	 * @param ydist move viewpoint up/down
	 *
	 * @param zdist the initial viewer z distance should be negative
	 */
	public Panel3D(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist, float bgRed,
			float bgGreen, float bgBlue, boolean skipLastStage) {

		_skipLastStage = skipLastStage;
		loadIdentityMatrix();

		rotateX(angleX);
		rotateY(angleY);
		rotateZ(angleZ);

		_xdist = xDist;
		_ydist = yDist;
		_zdist = zDist;

		_bgRed = bgRed;
		_bgGreen = bgGreen;
		_bgBlue = bgBlue;

		setLayout(new BorderLayout(0, 0));

		glprofile = GLProfile.getDefault();

		glcapabilities = new GLCapabilities(glprofile);
		glcapabilities.setRedBits(8);
		glcapabilities.setBlueBits(8);
		glcapabilities.setGreenBits(8);
		glcapabilities.setAlphaBits(8);
		glcapabilities.setDepthBits(32);

		gljpanel = new GLJPanel(glcapabilities);
		gljpanel.addGLEventListener(this);

		safeAdd(addNorth(), BorderLayout.NORTH);
		safeAdd(addSouth(), BorderLayout.SOUTH);
		safeAdd(addEast(), BorderLayout.EAST);
		safeAdd(addWest(), BorderLayout.WEST);

		// GLJPanel in the center
		add(gljpanel, BorderLayout.CENTER);

		new KeyBindings3D(this);

		_mouseAdapter = new MouseAdapter3D(this);
		gljpanel.addMouseListener(_mouseAdapter);
		gljpanel.addMouseMotionListener(_mouseAdapter);
		gljpanel.addMouseWheelListener(_mouseAdapter);

		createInitialItems();
	}

	// the openGL version and renderer strings
	protected String _versionStr;

	/**
	 * Create the initial items
	 */
	public void createInitialItems() {
		// default empty implementation
	}

	public void loadIdentityMatrix() {
		for (int i = 0; i < 16; i++) {
			rotationMatrix[i] = (i % 5 == 0) ? 1.0f : 0.0f; // Set diagonal elements to 1
		}
	}

	// add a component in the specified place if not null
	private void safeAdd(JComponent c, String placement) {
		if (c != null) {
			add(c, placement);
		}
	}

	// add the component in the north
	private JComponent addNorth() {
		return null;
	}

	// add the component in the south
	private JComponent addSouth() {
		return null;
	}

	// add the component in the north
	private JComponent addEast() {
		return null;
	}

	// add the component in the north
	private JComponent addWest() {
		return null;
	}

	/**
	 * Get the opengl panel
	 *
	 * @return the opengl panel
	 */
	public GLJPanel getGLJPanel() {
		return gljpanel;
	}

	public void setScale(float xscale, float yscale, float zscale) {
		_xscale = xscale;
		_yscale = yscale;
		_zscale = zscale;
	}

	public void rotate(Vector3f axis, float angle) {
		// Normalize the axis
		float length = (float) Math.sqrt(axis.x * axis.x + axis.y * axis.y + axis.z * axis.z);
		if (length == 0.0f) {
			return;
		}

		float x = axis.x / length;
		float y = axis.y / length;
		float z = axis.z / length;

		// Compute rotation matrix components
		float c = (float) Math.cos(angle);
		float s = (float) Math.sin(angle);
		float t = 1.0f - c;

		float[] rotation = new float[16];
		rotation[0] = t * x * x + c;
		rotation[1] = t * x * y - s * z;
		rotation[2] = t * x * z + s * y;
		rotation[3] = 0;

		rotation[4] = t * x * y + s * z;
		rotation[5] = t * y * y + c;
		rotation[6] = t * y * z - s * x;
		rotation[7] = 0;

		rotation[8] = t * x * z - s * y;
		rotation[9] = t * y * z + s * x;
		rotation[10] = t * z * z + c;
		rotation[11] = 0;

		rotation[12] = 0;
		rotation[13] = 0;
		rotation[14] = 0;
		rotation[15] = 1;

		// Multiply the current matrix with the rotation matrix
		multiplyMatrix(rotation);
		refresh();
	}

	private void multiplyMatrix(float[] other) {
		float[] result = new float[16];

		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 4; col++) {
				result[row * 4 + col] = 0;
				for (int k = 0; k < 4; k++) {
					result[row * 4 + col] += rotationMatrix[row * 4 + k] * other[k * 4 + col];
				}
			}
		}

		System.arraycopy(result, 0, rotationMatrix, 0, 16);
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		// Reassert the state you need on every frame
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDepthFunc(GL.GL_LEQUAL);

		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glDepthMask(true); // for opaque phase

		// Clear the buffers
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();

		// Translate and scale
		gl.glTranslatef(_xdist, _ydist, _zdist);
		gl.glScalef(_xscale, _yscale, _zscale);

		// Apply the rotation matrix
		gl.glMultMatrixf(rotationMatrix, 0);

		// Draw the 3D items
		gl.glPushMatrix();

		beforeDraw(drawable);
		for (Item3D item : _itemList) {
			if (item.isVisible()) {
				item.drawItem(drawable);
			}
		}
		afterDraw(drawable);

		gl.glPopMatrix();

		// ced might not like these lines
		if (_skipLastStage) {
			return;
		}
		gl.glDepthMask(true);
		gl.glDisable(GL.GL_BLEND);
		gl.glLoadIdentity();
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		System.err.println("called dispose");
	}

	/**
	 * Called before drawing the itemss.
	 */
	public void beforeDraw(GLAutoDrawable drawable) {
	}

	/**
	 * Called after drawing the items.
	 */
	public void afterDraw(GLAutoDrawable drawable) {
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		glu = GLU.createGLU();
		GL2 gl = drawable.getGL().getGL2();

		// version?
		_versionStr = gl.glGetString(GL.GL_VERSION);
		_rendererStr = gl.glGetString(GL.GL_RENDERER);

		float values[] = new float[2];
		gl.glGetFloatv(GL2GL3.GL_LINE_WIDTH_GRANULARITY, values, 0);

		gl.glGetFloatv(GL2GL3.GL_LINE_WIDTH_RANGE, values, 0);

		// Global settings.
		gl.glClearColor(_bgRed, _bgGreen, _bgBlue, 1f); // set background (clear) color
		gl.glClearDepth(1.0f); // set clear depth value to farthest
		gl.glEnable(GL.GL_DEPTH_TEST); // enables depth testing
		gl.glDepthFunc(GL.GL_LEQUAL); // the type of depth test to do
		// gl.glDepthFunc(GL.GL_LESS); // the type of depth test to do

		// best perspective correction
		gl.glHint(GL2ES1.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
		// blends colors, smoothes lighting
		gl.glShadeModel(GLLightingFunc.GL_FLAT);

		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL.GL_BLEND);
		gl.glEnable(GL2ES3.GL_COLOR);
		gl.glHint(GL2ES1.GL_POINT_SMOOTH_HINT, GL.GL_DONT_CARE);
		gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_DONT_CARE);

		gl.glEnable(GL3.GL_PROGRAM_POINT_SIZE);
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		GL2 gl = drawable.getGL().getGL2(); // get the OpenGL 2 graphics context

		if (height == 0) {
			height = 1; // prevent divide by zero
		}

		float aspect = (float) width / height;

		// Set the view port (display area) to cover the entire window
		gl.glViewport(0, 0, width, height);

		// Setup perspective projection, with aspect ratio matches viewport
		gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION); // choose projection matrix
		gl.glLoadIdentity(); // reset projection matrix

		// arguments are fovy, aspect, znear, zFar
		glu.gluPerspective(45.0, aspect, 0.1, 10000.0);

		// Enable the model-view transform
		gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
		gl.glLoadIdentity(); // reset
	}

	/**
	 * Change the x distance to move in or out
	 *
	 * @param dx the change in x
	 */
	public void deltaX(float dx) {
		_xdist += dx;
	}

	/**
	 * Change the y distance to move in or out
	 *
	 * @param dy the change in y
	 */
	public void deltaY(float dy) {
		_ydist += dy;
	}

	/**
	 * Change the z distance to move in or out
	 *
	 * @param dz the change in z
	 */
	public void deltaZ(float dz) {
		_zdist += dz;
	}

	/**
	 * Refresh the drawing
	 */
	public void refreshQueued() {
	}

	/**
	 * Refresh the drawing
	 */
	public void refresh() {

		if (gljpanel == null) {
			return;
		}

		gljpanel.display();

	}

	/**
	 * Add an item to the list. Note that this does not initiate a redraw.
	 *
	 * @param item the item to add.
	 */
	public void addItem(Item3D item) {
		if (item != null) {
			_itemList.remove(item);
			_itemList.add(item);
		}
	}

	/**
	 * Add an item to the list. Note that this does not initiate a redraw.
	 *
	 * @param item the item to add.
	 */
	public void addItem(int index, Item3D item) {
		if (item != null) {
			_itemList.remove(item);
			_itemList.add(index, item);
		}
	}

	/**
	 * Remove an item from the list. Note that this does not initiate a redraw.
	 *
	 * @param item the item to remove.
	 */
	public void removeItem(Item3D item) {
		if (item != null) {
			_itemList.remove(item);
			refresh();
		}
	}

	/**
	 * Clear all items from the list.
	 */
	public void clearItems() {
		_itemList.clear();
		refresh();
	}

	/**
	 * Conver GL coordinates to screen coordinates
	 *
	 * @param gl     graphics context
	 * @param objX   GL x coordinate
	 * @param objY   GL y coordinate
	 * @param objZ   GL z coordinate
	 * @param winPos should be float[3]. Will hold screen coords as floats as [x, y,
	 *               z]. Not sure what z is--ignore.
	 */
	public void project(GL2 gl, float objX, float objY, float objZ, float winPos[]) {

		int[] view = new int[4];
		gl.glGetIntegerv(GL.GL_VIEWPORT, view, 0);

		float[] model = new float[16];
		gl.glGetFloatv(GLMatrixFunc.GL_MODELVIEW_MATRIX, model, 0);

		float[] proj = new float[16];
		gl.glGetFloatv(GLMatrixFunc.GL_PROJECTION_MATRIX, proj, 0);

		glu.gluProject(objX, objY, objZ, model, 0, proj, 0, view, 0, winPos, 0);

	}

	/**
	 * This gets the z step used by the mouse and key adapters, to see how fast we
	 * move in or in in response to mouse wheel or up/down arrows. It should be
	 * overridden to give something sensible. like the scale/100;
	 *
	 * @return the z step (changes to zDist) for moving in and out
	 */
	public float getZStep() {
		return 0.1f;
	}

	/**
	 * Main program for testing. Put the panel on JFrame,
	 *
	 * @param arg
	 */
	public static void main(String arg[]) {
		final JFrame testFrame = new JFrame("bCNU 3D Panel Test");

		int n = 10000;
		if (arg.length > 0) {
			n = Integer.parseInt(arg[0]);
		}

		testFrame.setLayout(new BorderLayout(4, 4));

		final Panel3D p3d = createPanel3D();

		testFrame.add(p3d, BorderLayout.CENTER);

		// set up what to do if the window is closed
		WindowAdapter windowAdapter = new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				System.err.println("Done");
				System.exit(1);
			}
		};

		testFrame.addWindowListener(windowAdapter);
		testFrame.setBounds(200, 100, 900, 700);

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				testFrame.setVisible(true);
			}
		});

	}

	// for main program test
	private static Panel3D createPanel3D() {

		final float xymax = 600f;
		final float zmax = 600f;
		final float zmin = -100f;
		final float xdist = 0f;
		final float ydist = 0f;
		final float zdist = -2.75f * xymax;

		final float thetax = 45f;
		final float thetay = 45f;
		final float thetaz = 45f;

		return new Panel3D(thetax, thetay, thetaz, xdist, ydist, zdist) {
			@Override
			public void createInitialItems() {
				// coordinate axes

				Axes3D axes = new Axes3D(this, -xymax, xymax, -xymax, xymax, zmin, zmax, null, Color.darkGray, 1f, 7, 7,
						8, Color.black, Color.blue, new Font("SansSerif", Font.PLAIN, 11), 0);
				addItem(axes);

				// add some triangles

				// addItem(new Triangle3D(this,
				// 0f, 0f, 0f, 100f, 0f, -100f, 50f, 100, 100f, new Color(255,
				// 0, 0, 64), 2f, true));

				addItem(new Triangle3D(this, 500f, 0f, -200f, -500f, 500f, 0f, 0f, -100f, 500f,
						new Color(255, 0, 0, 64), 1f, true));

				addItem(new Triangle3D(this, 0f, 500f, 0f, -300f, -500f, 500f, 0f, -100f, 500f,
						new Color(0, 0, 255, 64), 2f, true));

				addItem(new Triangle3D(this, 0f, 0f, 500f, 0f, -400f, -500f, 500f, -100f, 500f,
						new Color(0, 255, 0, 64), 2f, true));

				addItem(new Cylinder(this, 0f, 0f, 0f, 300f, 300f, 300f, 50f, new Color(0, 255, 255, 128)));

				addItem(new Cube(this, 0f, 0f, 0f, 600, new Color(0, 0, 255, 32), true));

				// Cube cube = new Cube(this, 0.25f, 0.25f, 0.25f, 0.5f,
				// Color.yellow);
				// addItem(cube);

				// System.err.println("test with " + num + " lines.");
				// Line3D.lineItemTest(this, num);

				// Cube.cubeTest(this, 40000);

				// point set test
				int numPnt = 100;
				Color color = Color.orange;
				float pntSize = 10;
				float coords[] = new float[3 * numPnt];
				for (int i = 0; i < numPnt; i++) {
					int j = i * 3;
					float x = (float) (-xymax + 2 * xymax * Math.random());
					float y = (float) (-xymax + 2 * xymax * Math.random());
					float z = (float) (zmin + (zmax - zmin) * Math.random());
					coords[j] = x;
					coords[j + 1] = y;
					coords[j + 2] = z;
				}
				addItem(new PointSet3D(this, coords, color, pntSize, true));

			}

			/**
			 * This gets the z step used by the mouse and key adapters, to see how fast we
			 * move in or in in response to mouse wheel or up/down arrows. It should be
			 * overridden to give something sensible. like the scale/100;
			 *
			 * @return the z step (changes to zDist) for moving in and out
			 */
			@Override
			public float getZStep() {
				return (zmax - zmin) / 50f;
			}

		};
	}

	public void rotateX(float angle) {
		float[] rotation = createRotationX(angle);
		multiplyMatrix(rotation);
	}

	public void rotateY(float angle) {
		float[] rotation = createRotationY(angle);
		multiplyMatrix(rotation);
	}

	public void rotateZ(float angle) {
		float[] rotation = createRotationZ(angle);
		multiplyMatrix(rotation);
	}

	private float[] createRotationX(float angle) {
		float radians = (float) Math.toRadians(angle);
		float c = (float) Math.cos(radians);
		float s = (float) Math.sin(radians);

		return new float[] { 1, 0, 0, 0, 0, c, -s, 0, 0, s, c, 0, 0, 0, 0, 1 };
	}

	private float[] createRotationY(float angle) {
		float radians = (float) Math.toRadians(angle);
		float c = (float) Math.cos(radians);
		float s = (float) Math.sin(radians);

		return new float[] { c, 0, s, 0, 0, 1, 0, 0, -s, 0, c, 0, 0, 0, 0, 1 };
	}

	private float[] createRotationZ(float angle) {
		float radians = (float) Math.toRadians(angle);
		float c = (float) Math.cos(radians);
		float s = (float) Math.sin(radians);

		return new float[] { c, -s, 0, 0, s, c, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 };
	}

	/**
	 * 
	 * Print the panel. No default implementation.
	 * 
	 */

	public void print() {

	}

	/**
	 * 
	 * Snapshot of the panel. No default implementation.
	 * 
	 */

	public void snapshot() {

	}

}
