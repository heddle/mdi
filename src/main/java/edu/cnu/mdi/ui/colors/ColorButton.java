package edu.cnu.mdi.ui.colors;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;

/**
 * A button that displays a color and allows the user to change it
 *
 * @author heddle
 *
 */
@SuppressWarnings("serial")
public class ColorButton extends JButton {

	private Color color;

	public ColorButton(String label, Color initial) {
		super(label);
		this.color = initial;
		setFocusPainted(false);
		updateSwatch();
		addActionListener(e -> chooseColor());
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color c) {
		color = c;
		updateSwatch();
	}

	// Updates the button icon to show the current color
	private void updateSwatch() {
		if (color == null) {
			setIcon(null);
			setText("x");
			return;
		}
		setIcon(new ImageIcon(makeSwatch(color, 28, 14)));
	}

	private void chooseColor() {
		java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(ColorButton.this);
		Color chosen = ColorDialog.showDialog(owner, color, true, true);
		setColor(chosen);
	}

	private static Image makeSwatch(Color c, int w, int h) {
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = img.createGraphics();
		// checker background
		int s = 4;
		for (int y = 0; y < h; y += s) {
			for (int x = 0; x < w; x += s) {
				boolean dark = ((x / s) + (y / s)) % 2 == 0;
				g2.setColor(dark ? new Color(200, 200, 200) : new Color(240, 240, 240));
				g2.fillRect(x, y, s, s);
			}
		}
		g2.setColor(c);
		g2.fillRect(0, 0, w, h);
		g2.setColor(Color.black);
		g2.drawRect(0, 0, w - 1, h - 1);
		g2.dispose();
		return img;
	}

}
