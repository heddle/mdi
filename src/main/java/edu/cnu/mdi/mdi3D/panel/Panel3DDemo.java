package edu.cnu.mdi.mdi3D.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import edu.cnu.mdi.mdi3D.item3D.Axes3D;
import edu.cnu.mdi.mdi3D.item3D.Cube;
import edu.cnu.mdi.mdi3D.item3D.Cylinder;
import edu.cnu.mdi.mdi3D.item3D.PointSet3D;
import edu.cnu.mdi.mdi3D.item3D.Triangle3D;

@SuppressWarnings("serial")
public class Panel3DDemo extends JFrame {

	public Panel3DDemo() {
		setTitle("3D Panel Demo");
		setLayout(new BorderLayout(4, 4));
		final Panel3D p3d = createPanel3D();
		add(p3d, BorderLayout.CENTER);

		WindowAdapter windowAdapter = new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				System.err.println("Done");
				System.exit(1);
			}
		};

		addWindowListener(windowAdapter);
		setBounds(200, 100, 900, 700);

	}

	// Create a Panel3D with some items just for demo
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
				Axes3D axes = new Axes3D(this, -xymax, xymax, -xymax, xymax, zmin, zmax, null, Color.darkGray, 1f, 7, 7,
						8, Color.black, Color.blue, new Font("SansSerif", Font.PLAIN, 11), 0);
				addItem(axes);

				addItem(new Triangle3D(this, 500f, 0f, -200f, -500f, 500f, 0f, 0f, -100f, 500f,
						new Color(255, 0, 0, 64), 1f, true));

				addItem(new Triangle3D(this, 0f, 500f, 0f, -300f, -500f, 500f, 0f, -100f, 500f,
						new Color(0, 0, 255, 64), 2f, true));

				addItem(new Triangle3D(this, 0f, 0f, 500f, 0f, -400f, -500f, 500f, -100f, 500f,
						new Color(0, 255, 0, 64), 2f, true));

				addItem(new Cylinder(this, 0f, 0f, 0f, 300f, 300f, 300f, 50f, new Color(0, 255, 255, 128)));
				addItem(new Cube(this, 0f, 0f, 0f, 600, new Color(0, 0, 255, 32), true));

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

			@Override
			public float getZStep() {
				return (zmax - zmin) / 50f;
			}
		};
	}

	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				Panel3DDemo demo = new Panel3DDemo();
				demo.setVisible(true);
			}
		});
	}

}
