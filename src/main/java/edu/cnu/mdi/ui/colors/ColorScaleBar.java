package edu.cnu.mdi.ui.colors;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("serial")
public class ColorScaleBar extends JComponent {

    private ScientificColorMap _map;
    private String minLabel = "Min";
    private String maxLabel = "Max";

    public ColorScaleBar(ScientificColorMap map) {
        _map = map;
        setPreferredSize(new Dimension(200, 50));
    }

    /** Backward-compatible constructor if needed elsewhere. */
    public ColorScaleBar(Color[] scale) {
        this(ScientificColorMap.VIRIDIS); // default placeholder map
        // If someone uses this ctor, we’ll draw using the scale via interpolate:
        _map = null;
        _fallbackScale = scale;
        setPreferredSize(new Dimension(200, 50));
    }

    // Only used by the fallback ctor
    private Color[] _fallbackScale;

    public void setColorMap(ScientificColorMap map) {
        _map = map;
        _fallbackScale = null;
        repaint();
    }

    /** Backward-compatible setter if needed. */
    public void setScale(Color[] scale) {
        _map = null;
        _fallbackScale = scale;
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

        // 1) Gradient bar
        for (int x = padding; x < width - padding; x++) {
            double value = (double) (x - padding) / (width - 2.0 * padding);
            Color c;
            if (_map != null) {
                c = _map.colorAt(value);
            } else {
                c = ScientificColorMap.interpolate(_fallbackScale, value);
            }
            g2d.setColor(c);
            g2d.drawLine(x, 5, x, 5 + barHeight);
        }

        // 2) Outline
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawRect(padding, 5, width - 2 * padding, barHeight);

        // 3) Labels
        g2d.setColor(getForeground());
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(minLabel, padding, 15 + barHeight + fm.getAscent());

        int maxLabelWidth = fm.stringWidth(maxLabel);
        g2d.drawString(maxLabel, width - padding - maxLabelWidth, 15 + barHeight + fm.getAscent());
    }
}
