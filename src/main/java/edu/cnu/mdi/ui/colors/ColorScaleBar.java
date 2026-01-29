package edu.cnu.mdi.ui.colors;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("serial")
public class ColorScaleBar extends JComponent {
    private Color[] currentScale;
    private String minLabel = "Min";
    private String maxLabel = "Max";

    public ColorScaleBar(Color[] scale) {
        this.currentScale = scale;
        setPreferredSize(new Dimension(200, 50)); // Default size
    }

    public void setScale(Color[] scale) {
        this.currentScale = scale;
        repaint();
    }

    public void setLabels(String min, String max) {
        this.minLabel = min;
        this.maxLabel = max;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int barHeight = height / 2;
        int padding = 10;

        // 1. Draw the Gradient Bar
        // We iterate pixel by pixel to ensure the interpolation is perfectly smooth
        for (int x = padding; x < width - padding; x++) {
            double value = (double) (x - padding) / (width - 2 * padding);
            g2d.setColor(getInterpolatedColor(currentScale, value));
            g2d.drawLine(x, 5, x, 5 + barHeight);
        }

        // 2. Draw Outline
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawRect(padding, 5, width - 2 * padding, barHeight);

        // 3. Draw Labels
        g2d.setColor(getForeground());
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(minLabel, padding, 15 + barHeight + fm.getAscent());
        
        int maxLabelWidth = fm.stringWidth(maxLabel);
        g2d.drawString(maxLabel, width - padding - maxLabelWidth, 15 + barHeight + fm.getAscent());
    }

    // Helper method for the internal painting logic
    private Color getInterpolatedColor(Color[] scale, double value) {
        double pos = value * (scale.length - 1);
        int index = (int) pos;
        double fraction = pos - index;
        if (index >= scale.length - 1) return scale[scale.length - 1];
        Color c1 = scale[index];
        Color c2 = scale[index + 1];
        return new Color(
            (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * fraction),
            (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * fraction),
            (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * fraction)
        );
    }
}