package edu.cnu.mdi.ui.colors;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Insets;
import java.util.Objects;

import javax.swing.JComponent;

@SuppressWarnings("serial")
public class ColorScaleBar extends JComponent {

    private ScientificColorMap _map;
    private String minLabel = "Min";
    private String maxLabel = "Max";

    public ColorScaleBar(ScientificColorMap map) {
		_map = Objects.requireNonNull(map, "map");
        setPreferredSize(new Dimension(200, 50));
    }

    /** Backward-compatible constructor if needed elsewhere. */
    public ColorScaleBar(Color[] scale) {
        this(ScientificColorMap.VIRIDIS); // default placeholder map
        // If someone uses this ctor, we’ll draw using the scale via interpolate:
        _map = null;
		_fallbackScale = validatedScale(scale);
        setPreferredSize(new Dimension(200, 50));
    }

    // Only used by the fallback ctor
    private Color[] _fallbackScale;

    public void setColorMap(ScientificColorMap map) {
		_map = Objects.requireNonNull(map, "map");
        _fallbackScale = null;
        repaint();
    }

    /** Backward-compatible setter if needed. */
    public void setScale(Color[] scale) {
        _map = null;
		_fallbackScale = validatedScale(scale);
        repaint();
    }

    public void setLabels(String min, String max) {
		this.minLabel = Objects.requireNonNull(min, "min");
		this.maxLabel = Objects.requireNonNull(max, "max");
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Insets insets = getInsets();
        int left = insets.left + 10;
        int right = getWidth() - insets.right - 10;
        int top = insets.top + 3;
        FontMetrics fm = g2d.getFontMetrics();
        int labelY = getHeight() - insets.bottom - 2;
        int barBottom = labelY - fm.getAscent() - 4;
        int barHeight = Math.max(2, barBottom - top);

		if (right <= left || barBottom <= top) {
			return;
		}

        // 1) Gradient bar
        for (int x = left; x < right; x++) {
            double value = (x - left) / (double) Math.max(1, right - left - 1);
            Color c;
            if (_map != null) {
                c = _map.colorAt(value);
            } else {
                c = ScientificColorMap.interpolate(_fallbackScale, value);
            }
            g2d.setColor(c);
            g2d.drawLine(x, top, x, barBottom);
        }

        // 2) Outline
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawRect(left, top, right - left, barHeight);

        // 3) Labels
        g2d.setColor(getForeground());
        g2d.drawString(minLabel, left, labelY);

        int maxLabelWidth = fm.stringWidth(maxLabel);
        g2d.drawString(maxLabel, right - maxLabelWidth, labelY);
    }

	private static Color[] validatedScale(Color[] scale) {
		Objects.requireNonNull(scale, "scale");
		if (scale.length == 0) {
			throw new IllegalArgumentException("scale must contain at least one color");
		}
		for (Color color : scale) {
			Objects.requireNonNull(color, "scale contains null color");
		}
		return scale.clone();
	}
}
