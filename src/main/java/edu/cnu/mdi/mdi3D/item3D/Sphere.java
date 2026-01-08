package edu.cnu.mdi.mdi3D.item3D;

import java.awt.Color;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;

public class Sphere extends Item3D {

    private float _radius;
    private float _x;
    private float _y;
    private float _z;
    private Color _color;
    private int _slices = 100; // Default resolution
    private int _stacks = 100; // Default resolution
    private float[] _theta; // Polar angles [0, π]
    private float[] _phi;   // Azimuthal angles [-π, π]
    private Color _gridColor = Color.BLACK; // Default gridline color

    /**
     * Constructor for a Sphere item in 3D space.
     *
     * @param panel3D The parent 3D panel
     * @param x       X coordinate of the center
     * @param y       Y coordinate of the center
     * @param z       Z coordinate of the center
     * @param radius  Radius of the sphere
     * @param color   Color of the sphere
     */
    public Sphere(Panel3D panel3D, float x, float y, float z, float radius, Color color) {
        super(panel3D);
        _x = x;
        _y = y;
        _z = z;
        _radius = radius;
        _color = color;
    }

    /**
     * Set the resolution of the sphere.
     * Higher values for slices and stacks result in a smoother sphere.
     *
     * @param slices Number of slices (longitude divisions)
     * @param stacks Number of stacks (latitude divisions)
     */
    public void setResolution(int slices, int stacks) {
        _slices = Math.max(3, slices); // Ensure valid values
        _stacks = Math.max(2, stacks);
    }

    /**
     * Set the gridlines for the sphere.
     *
     * @param theta Array of polar angles [0, π]
     * @param phi   Array of azimuthal angles [-π, π]
     */
    public void setGridlines(float[] theta, float[] phi) {
        _theta = theta;
        _phi = phi;
    }

    /**
     * Set the color of the gridlines.
     *
     * @param gridColor The gridline color
     */
    public void setGridColor(Color gridColor) {
        _gridColor = gridColor;
    }

    @Override
    public void draw(GLAutoDrawable drawable) {
    	Support3D.prepareForOpaque(drawable);
        Support3D.solidSphere(drawable, _x, _y, _z, _radius, _slices, _stacks, _color);

        if (_theta != null || _phi != null) {
            drawGridlines(drawable);
        }
    	Support3D.prepareForTransparent(drawable);
   }

    /**
     * Draws the gridlines on the sphere based on provided theta and phi values.
     * Ensures hidden lines are not drawn.
     */
    private void drawGridlines(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        // Enable depth testing to hide hidden parts of gridlines
        gl.glEnable(GL.GL_DEPTH_TEST);

        // Apply polygon offset to avoid z-fighting
        gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
        gl.glPolygonOffset(-1.0f, -1.0f);

        if (_theta != null) {
            drawThetaLines(drawable);
        }
        if (_phi != null) {
            drawPhiLines(drawable);
        }

        // Disable polygon offset
        gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);
    }
    /**
     * Draws meridian (longitude) lines at the given azimuthal angles (phi).
     */
    private void drawPhiLines(GLAutoDrawable drawable) {
        for (float phi : _phi) {
            float[] coords = new float[(_slices + 1) * 3];

            for (int i = 0; i <= _slices; i++) {
                float theta = (float) (Math.PI * i / _slices);
                float x = _x + _radius * (float) (Math.sin(theta) * Math.cos(phi));
                float y = _y + _radius * (float) (Math.sin(theta) * Math.sin(phi));
                float z = _z + _radius * (float) Math.cos(theta);

                coords[3 * i] = x;
                coords[3 * i + 1] = y;
                coords[3 * i + 2] = z;
            }

            Support3D.drawPolyLine(drawable, coords, _gridColor, 1.5f);
        }
    }

    /**
     * Draws parallel (latitude) lines at the given polar angles (theta).
     */
    private void drawThetaLines(GLAutoDrawable drawable) {
        for (float theta : _theta) {
            float[] coords = new float[(_stacks + 2) * 3]; // +1 for closure, +1 for array index safety

            for (int i = 0; i <= _stacks; i++) {
                float phi = (float) (-Math.PI + 2 * Math.PI * i / _stacks);
                float x = _x + _radius * (float) (Math.sin(theta) * Math.cos(phi));
                float y = _y + _radius * (float) (Math.sin(theta) * Math.sin(phi));
                float z = _z + _radius * (float) Math.cos(theta);

                coords[3 * i] = x;
                coords[3 * i + 1] = y;
                coords[3 * i + 2] = z;
            }

            // Close the loop by explicitly connecting the last point to the first
            coords[3 * _stacks] = coords[0];
            coords[3 * _stacks + 1] = coords[1];
            coords[3 * _stacks + 2] = coords[2];

            Support3D.drawPolyLine(drawable, coords, _gridColor, 1.5f);
        }
    }
}
