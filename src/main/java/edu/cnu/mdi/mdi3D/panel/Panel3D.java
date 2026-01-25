package edu.cnu.mdi.mdi3D.panel;

import java.awt.BorderLayout;
import java.util.Vector;

import javax.swing.JComponent;
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
import edu.cnu.mdi.mdi3D.item3D.Item3D;

@SuppressWarnings("serial")
public class Panel3D extends JPanel implements GLEventListener {

	// background default color used for r, g and b
	public static final float BGFEFAULT = 0.9804f;

	// alpha cutoff for opaque vs transparent
	private static final int OPAQUE_ALPHA_CUTOFF = 250;

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


	// Quaternion orientation (this IS the truth)
	private final Quat _orientation = new Quat(); // identity by default

	// scratch matrix (column-major for OpenGL)
	private final float[] _rotMat = new float[16];

	// the list of 3D items to be drawn
	protected Vector<Item3D> _itemList = new Vector<>();

	// listen for mouse events
	protected MouseAdapter3D _mouseAdapter;

	// listen for key events
	protected KeyAdapter3D _keyAdapter;

	protected String _rendererStr;

	private boolean _skipLastStage = false;

	// the openGL version and renderer strings
	protected String _versionStr;

	public Panel3D(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		this(angleX, angleY, angleZ, xDist, yDist, zDist, BGFEFAULT, BGFEFAULT, BGFEFAULT, false);
	}

	public Panel3D(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist,
			float bgRed, float bgGreen, float bgBlue, boolean skipLastStage) {

		_skipLastStage = skipLastStage;

		_xdist = xDist;
		_ydist = yDist;
		_zdist = zDist;

		_bgRed = bgRed;
		_bgGreen = bgGreen;
		_bgBlue = bgBlue;

		setLayout(new BorderLayout(0, 0));

		GLProfile glprofile;
		if (GLProfile.isAvailable(GLProfile.GL2)) {
			glprofile = GLProfile.get(GLProfile.GL2);
		} else {
			glprofile = GLProfile.getMaxFixedFunc(true);
		}

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

		add(gljpanel, BorderLayout.CENTER);

		new KeyBindings3D(this);

		_mouseAdapter = new MouseAdapter3D(this);
		gljpanel.addMouseListener(_mouseAdapter);
		gljpanel.addMouseMotionListener(_mouseAdapter);
		gljpanel.addMouseWheelListener(_mouseAdapter);

		// Set initial orientation using the same semantics as before:
		// reset then apply rotateX/Y/Z in that order.
		loadIdentityMatrix();
		rotateX(angleX);
		rotateY(angleY);
		rotateZ(angleZ);

		createInitialItems();

	}

	public void createInitialItems() {
		// default empty implementation
	}

	private void safeAdd(JComponent c, String placement) {
		if (c != null) {
			add(c, placement);
		}
	}

	private JComponent addNorth() { return null; }
	private JComponent addSouth() { return null; }
	private JComponent addEast()  { return null; }
	private JComponent addWest()  { return null; }

	public GLJPanel getGLJPanel() {
		return gljpanel;
	}

	public void setScale(float xscale, float yscale, float zscale) {
		_xscale = xscale;
		_yscale = yscale;
		_zscale = zscale;
	}

	/**
	 * TRUE axis-angle rotation composition (restores arcball behavior).
	 * The angle is in radians (as produced by MouseAdapter3D).
	 */
	public void rotate(Vector3f axis, float angleRadians) {
		if (axis == null) {
			return;
		}
		float len = axis.length();
		if (len < 1e-6f) {
			return;
		}

		// normalize axis
		float ax = axis.x / len;
		float ay = axis.y / len;
		float az = axis.z / len;

		Quat dq = Quat.fromAxisAngle(ax, ay, az, angleRadians);

		// Compose: newOrientation = dq * orientation
		// (left-multiply matches typical "rotate object in world" feel for arcball)
		synchronized (_orientation) {
			_orientation.set(dq.mul(_orientation));
			_orientation.normalizeInPlace();
		}

		refresh();
	}

	public void rotateX(float angleDeg) {
		float rad = (float) Math.toRadians(angleDeg);
		Quat dq = Quat.fromAxisAngle(1f, 0f, 0f, rad);

		synchronized (_orientation) {
			_orientation.set(dq.mul(_orientation));
			_orientation.normalizeInPlace();
		}

		refresh();
	}

	public void rotateY(float angleDeg) {
		float rad = (float) Math.toRadians(angleDeg);
		Quat dq = Quat.fromAxisAngle(0f, 1f, 0f, rad);

		synchronized (_orientation) {
			_orientation.set(dq.mul(_orientation));
			_orientation.normalizeInPlace();
		}

		refresh();
	}

	public void rotateZ(float angleDeg) {
		float rad = (float) Math.toRadians(angleDeg);
		Quat dq = Quat.fromAxisAngle(0f, 0f, 1f, rad);

		synchronized (_orientation) {
			_orientation.set(dq.mul(_orientation));
			_orientation.normalizeInPlace();
		}

		refresh();
	}

	/**
	 * Historically this reset the internal rotation matrix to identity.
	 * Now it resets the quaternion orientation to identity (and zeroes the angle fields).
	 */
	public void loadIdentityMatrix() {
		synchronized (_orientation) {
			_orientation.setIdentity();
		}
	}

	@Override
	public void display(GLAutoDrawable drawable) {


		final GL2 gl = drawable.getGL().getGL2();

		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDepthFunc(GL.GL_LEQUAL);

		gl.glDisable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

		gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
		gl.glLoadIdentity();

		gl.glTranslatef(_xdist, _ydist, _zdist);
		gl.glScalef(_xscale, _yscale, _zscale);

		// Apply quaternion orientation as a column-major matrix
		synchronized (_orientation) {
			_orientation.toColumnMajorMatrix(_rotMat);
		}
		gl.glMultMatrixf(_rotMat, 0);

		// Snapshot items
		final java.util.List<Item3D> snapshot;
		synchronized (_itemList) {
			snapshot = new java.util.ArrayList<>(_itemList);
		}

		final java.util.List<Item3D> opaque = new java.util.ArrayList<>(snapshot.size());
		final java.util.List<Item3D> transparent = new java.util.ArrayList<>(snapshot.size());

		for (Item3D item : snapshot) {
			if (item != null && item.isVisible()) {
				(isTransparent(item) ? transparent : opaque).add(item);
			}
		}


		gl.glPushMatrix();
		beforeDraw(drawable);

		// PASS 1: OPAQUE
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDepthFunc(GL.GL_LEQUAL);
		gl.glDepthMask(true);
		gl.glDisable(GL.GL_BLEND);
		for (Item3D item : opaque) {
			item.drawItem(drawable);
		}

		// PASS 2: TRANSPARENT
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDepthFunc(GL.GL_LEQUAL);
		gl.glDepthMask(false);
		gl.glEnable(GL.GL_BLEND);
		// Sort transparent items back-to-front to improve blending correctness
		sortTransparentBackToFront(transparent);

		for (Item3D item : transparent) {
			item.drawItem(drawable);
		}

		gl.glDepthMask(true);
		gl.glDisable(GL.GL_BLEND);

		afterDraw(drawable);
		gl.glPopMatrix();

		if (_skipLastStage) {
			return;
		}

		gl.glLoadIdentity();
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		System.err.println("called dispose");
	}

	public void beforeDraw(GLAutoDrawable drawable) { }
	public void afterDraw(GLAutoDrawable drawable)  { }

	@Override
	public void init(GLAutoDrawable drawable) {
		glu = GLU.createGLU();
		GL2 gl = drawable.getGL().getGL2();

		_versionStr = gl.glGetString(GL.GL_VERSION);
		_rendererStr = gl.glGetString(GL.GL_RENDERER);

		float values[] = new float[2];
		gl.glGetFloatv(GL2GL3.GL_LINE_WIDTH_GRANULARITY, values, 0);
		gl.glGetFloatv(GL2GL3.GL_LINE_WIDTH_RANGE, values, 0);

		gl.glClearColor(_bgRed, _bgGreen, _bgBlue, 1f);
		gl.glClearDepth(1.0f);
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDepthFunc(GL.GL_LEQUAL);

		gl.glHint(GL2ES1.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
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
		GL2 gl = drawable.getGL().getGL2();

		if (height == 0) {
			height = 1;
		}

		float aspect = (float) width / height;

		gl.glViewport(0, 0, width, height);

		gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		gl.glLoadIdentity();

		glu.gluPerspective(45.0, aspect, 0.1, 10000.0);

		gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	public void deltaX(float dx) { _xdist += dx; }
	public void deltaY(float dy) { _ydist += dy; }
	public void deltaZ(float dz) { _zdist += dz; }

	public void refreshQueued() { }

	public void refresh() {
		if (gljpanel == null) {
			return;
		}
		gljpanel.display();
	}

	/**
	 * Adds the given item to the panel.
	 */
	public void addItem(Item3D item) {
		if (item != null) {
			_itemList.remove(item);
			_itemList.add(item);
		}
	}

	/**
	 * Adds the given item at the specified index in the panel.
	 */
	public void addItem(int index, Item3D item) {
		if (item != null) {
			_itemList.remove(item);
			_itemList.add(index, item);
		}
	}

	/**
	 * Removes the given item from the panel.
	 */
	public void removeItem(Item3D item) {
		if (item != null) {
			_itemList.remove(item);
			refresh();
		}
	}

	/**
	 * Removes all items from the panel.
	 */
	public void clearItems() {
		_itemList.clear();
		refresh();
	}

	/**
	 * Projects the given object coordinates (objX, objY, objZ) to window coordinates.
	 * The result is stored in winPos[0] (x), winPos[1] (y), winPos[2] (z).
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

	public float getZStep() { return 0.1f; }

	private boolean isTransparent(Item3D item) {
		try {
			int fa = item.getFillAlpha();
			if (fa >= 0 && fa < OPAQUE_ALPHA_CUTOFF) {
				return true;
			}
		} catch (Exception ignored) { }

		try {
			int la = item.getLineAlpha();
			if (la >= 0 && la < OPAQUE_ALPHA_CUTOFF) {
				return true;
			}
		} catch (Exception ignored) { }

		return false;
	}

	// --------------------------------------------------------------------
	// Minimal quaternion implementation (no dependencies)
	// --------------------------------------------------------------------
	private static final class Quat {
		// w + xi + yj + zk
		float w = 1f, x = 0f, y = 0f, z = 0f;

		void setIdentity() { w = 1f; x = y = z = 0f; }

		void set(Quat q) { this.w = q.w; this.x = q.x; this.y = q.y; this.z = q.z; }

		static Quat fromAxisAngle(float ax, float ay, float az, float angleRad) {
			float half = 0.5f * angleRad;
			float s = (float) Math.sin(half);
			Quat q = new Quat();
			q.w = (float) Math.cos(half);
			q.x = ax * s;
			q.y = ay * s;
			q.z = az * s;
			return q;
		}

		Quat mul(Quat r) {
			// this * r
			Quat q = new Quat();
			q.w = this.w * r.w - this.x * r.x - this.y * r.y - this.z * r.z;
			q.x = this.w * r.x + this.x * r.w + this.y * r.z - this.z * r.y;
			q.y = this.w * r.y - this.x * r.z + this.y * r.w + this.z * r.x;
			q.z = this.w * r.z + this.x * r.y - this.y * r.x + this.z * r.w;
			return q;
		}

		// Normalize quaternion in place
		void normalizeInPlace() {
			float n = (float) Math.sqrt(w * w + x * x + y * y + z * z);
			if (n < 1e-12f) {
				setIdentity();
				return;
			}
			w /= n; x /= n; y /= n; z /= n;
		}

		/**
		 * Convert to a 4x4 column-major matrix for OpenGL.
		 */
		void toColumnMajorMatrix(float[] m) {
			// assumes normalized
			float xx = x * x, yy = y * y, zz = z * z;
			float xy = x * y, xz = x * z, yz = y * z;
			float wx = w * x, wy = w * y, wz = w * z;

			// Column-major (OpenGL)
			m[0]  = 1f - 2f * (yy + zz);
			m[1]  = 2f * (xy + wz);
			m[2]  = 2f * (xz - wy);
			m[3]  = 0f;

			m[4]  = 2f * (xy - wz);
			m[5]  = 1f - 2f * (xx + zz);
			m[6]  = 2f * (yz + wx);
			m[7]  = 0f;

			m[8]  = 2f * (xz + wy);
			m[9]  = 2f * (yz - wx);
			m[10] = 1f - 2f * (xx + yy);
			m[11] = 0f;

			m[12] = 0f;
			m[13] = 0f;
			m[14] = 0f;
			m[15] = 1f;
		}
	}

	private void sortTransparentBackToFront(java.util.List<Item3D> transparent) {

	    // Copy rotation matrix once (already computed for glMultMatrixf)
	    final float[] R = _rotMat;

	    // Cache camera translation as well (your view translate happens before rotation)
	    final float tx = _xdist;
	    final float ty = _ydist;
	    final float tz = _zdist;

	    // Cache scales
	    final float sx = _xscale, sy = _yscale, sz = _zscale;

	    java.util.Collections.sort(transparent, (a, b) -> {
	        float za = viewZ(a, R, sx, sy, sz, tx, ty, tz);
	        float zb = viewZ(b, R, sx, sy, sz, tx, ty, tz);
	        int c = Float.compare(za, zb);           // ascending: more negative first
	        if (c != 0) {
				return c;
			}
	        return Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
	    });	}

	/**
	 * Compute approximate view-space Z for an item using its sort point.
	 * We build a view transform equivalent to:
	 *   v = R * (S * p) + T
	 * and return v.z.
	 */
	private float viewZ(Item3D item, float[] R, float sx, float sy, float sz, float tx, float ty, float tz) {
	    float[] p = item.getSortPoint();

	    float x = p[0];
	    float y = p[1];
	    float z = p[2];

	    float zr = R[2] * x + R[6] * y + R[10] * z;

	    // Then scale (because your GL does Translate -> Scale -> Rotate)
	    zr *= sz;

	    // Then translate
	    zr += tz;

	    return zr;
	}

}
