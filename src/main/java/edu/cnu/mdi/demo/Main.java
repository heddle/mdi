package edu.cnu.mdi.demo;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import javax.swing.*;

public class Main implements GLEventListener {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("MDI JOGL Demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            GLProfile profile = GLProfile.get(GLProfile.GL2);
            GLCapabilities caps = new GLCapabilities(profile);
            GLCanvas canvas = new GLCanvas(caps);

            Main renderer = new Main();
            canvas.addGLEventListener(renderer);

            frame.add(canvas);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            FPSAnimator animator = new FPSAnimator(canvas, 60);
            animator.start();
        });
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        // Called once when OpenGL context is created
        GL2 gl = drawable.getGL().getGL2();
        System.out.println("OpenGL initialized: " + gl.glGetString(GL.GL_VERSION));
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        // Cleanup resources
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        // Render loop
        GL2 gl = drawable.getGL().getGL2();

        // Clear screen to dark gray
        gl.glClearColor(0.2f, 0.2f, 0.2f, 1f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        // Add drawing here...
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        // Handle window resizing
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
    }
}
