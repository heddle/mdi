package edu.cnu.mdi.mdi3D.adapter3D;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import edu.cnu.mdi.mdi3D.panel.Panel3D;

public class KeyAdapter3D implements KeyListener {

	// steps in rotation angle
	private static final float DTHETA = 2f; // degrees

	private Panel3D _panel3D;

	public KeyAdapter3D(Panel3D panel3D) {
		_panel3D = panel3D;
	}

	@Override
	public void keyTyped(KeyEvent e) {
		System.err.println("3D KEY TYPE");
	}

	@Override
	public void keyPressed(KeyEvent e) {
//		System.err.println("3D KEY PRESS " + e.getSource().getClass().getName());

		int keyCode = e.getKeyCode();
		boolean shifted = e.isShiftDown();

		handleVK(_panel3D, keyCode, shifted);
	}

	/**
	 * Respond to a key stroke or mimic a key stroke
	 *
	 * @param panel3D the owner panel
	 * @param keyCode the key code
	 * @param shifted whether it was shifted (e.g., capitalized)
	 */
	public static void handleVK(Panel3D panel3D, int keyCode, boolean shifted) {

		int factor = (shifted ? 3 : 1);
		float step = panel3D.getZStep();
		float ang = factor * DTHETA;

		if (keyCode == KeyEvent.VK_L) {
			panel3D.deltaX(-step);
			panel3D.refresh();
		} else if (keyCode == KeyEvent.VK_R) {
			panel3D.deltaX(step);
			panel3D.refresh();
		} else if (keyCode == KeyEvent.VK_U) {
			panel3D.deltaY(step);
			panel3D.refresh();
		} else if (keyCode == KeyEvent.VK_D) {
			panel3D.deltaY(-step);
			panel3D.refresh();
		} else if (keyCode == KeyEvent.VK_J) {
			panel3D.deltaZ(step);
			panel3D.refresh();
		} else if (keyCode == KeyEvent.VK_K) {
			panel3D.deltaZ(-step);
			panel3D.refresh();
		} else if (keyCode == KeyEvent.VK_X) {
			panel3D.rotateX(ang);
			panel3D.refresh();
		} else if (keyCode == KeyEvent.VK_Y) {
			panel3D.rotateY(ang);
			panel3D.refresh();
		} else if (keyCode == KeyEvent.VK_Z) {
			panel3D.rotateZ(ang);
			panel3D.refresh();
		} else if (keyCode == KeyEvent.VK_1) { // x out
			panel3D.loadIdentityMatrix();
			panel3D.rotateX(180f);
			panel3D.rotateY(90f);
			panel3D.refresh();
		} else if (keyCode == KeyEvent.VK_2) { // y out3
			panel3D.loadIdentityMatrix();
			panel3D.rotateZ(-90f);
			panel3D.rotateY(-90f);
			panel3D.refresh();
		} else if (keyCode == KeyEvent.VK_3) { // z out
			panel3D.loadIdentityMatrix();
			panel3D.refresh();
		} else if (keyCode == KeyEvent.VK_4) { // z in
			panel3D.loadIdentityMatrix();
			panel3D.rotateY(180f);
			panel3D.refresh();
		}

	}

	@Override
	public void keyReleased(KeyEvent e) {
		int keyCode = e.getKeyCode();

		if (keyCode == KeyEvent.VK_F5) {
			_panel3D.refresh();
		}
	}

}
