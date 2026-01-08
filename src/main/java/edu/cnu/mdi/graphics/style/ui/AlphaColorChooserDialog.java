package edu.cnu.mdi.graphics.style.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;

public final class AlphaColorChooserDialog {

    private AlphaColorChooserDialog() {}

    public static Color show(Component parent, String title, Color initial) {
        Color init = (initial != null) ? initial : new Color(0,0,0,255);

        JColorChooser chooser = new JColorChooser(new Color(init.getRed(), init.getGreen(), init.getBlue()));
        JSlider alpha = new JSlider(0, 255, init.getAlpha());
        alpha.setPaintTicks(true);
        alpha.setMajorTickSpacing(51);
        alpha.setPaintLabels(true);

        JPanel panel = new JPanel(new BorderLayout(8,8));
        panel.add(chooser, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(8,8));
        south.add(new JLabel("Opacity"), BorderLayout.WEST);
        south.add(alpha, BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(parent, panel, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) {
			return null;
		}

        Color rgb = chooser.getColor();
        return new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), alpha.getValue());
    }
}
