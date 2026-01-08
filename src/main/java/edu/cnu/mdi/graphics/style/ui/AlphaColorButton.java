package edu.cnu.mdi.graphics.style.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;

public class AlphaColorButton extends JButton {

    private Color color;

    public AlphaColorButton(String label, Color initial) {
        super(label);
        this.color = (initial != null) ? initial : new Color(0,0,0,255);
        setFocusPainted(false);
        updateSwatch();
        addActionListener(e -> chooseColor());
    }

    public Color getColor() { return color; }

    public void setColor(Color c) {
        color = (c != null) ? c : color;
        updateSwatch();
    }

    private void updateSwatch() {
        setIcon(new ImageIcon(makeSwatch(color, 28, 14)));
    }

    private void chooseColor() {
        Color chosen = AlphaColorChooserDialog.show(this, getText(), color);
        if (chosen != null) {
            setColor(chosen);
        }
    }

    private static Image makeSwatch(Color c, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        // checker background
        int s = 4;
        for (int y=0; y<h; y+=s) {
			for (int x=0; x<w; x+=s) {
			    boolean dark = ((x/s)+(y/s))%2==0;
			    g2.setColor(dark ? new Color(200,200,200) : new Color(240,240,240));
			    g2.fillRect(x,y,s,s);
			}
		}
        g2.setColor(c);
        g2.fillRect(0,0,w,h);
        g2.setColor(Color.black);
        g2.drawRect(0,0,w-1,h-1);
        g2.dispose();
        return img;
    }
}
