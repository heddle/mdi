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
import javax.swing.UIManager;

@SuppressWarnings("serial")
public class ColorScaleBar extends JComponent {

    private static final int DEFAULT_BAR_HEIGHT = 18;
    private static final int HORIZONTAL_INSET = 10;
    private static final int VERTICAL_GAP = 4;

    private ScientificColorMap _map;
    private String[] labels = {"Min", "Max"};
    private int barHeight = DEFAULT_BAR_HEIGHT;

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
		setTickLabels(min, max);
    }

    /** Set labels at evenly spaced positions along the color bar. */
    public void setTickLabels(String... tickLabels) {
		Objects.requireNonNull(tickLabels, "tickLabels");
		if (tickLabels.length < 2) {
			throw new IllegalArgumentException("at least two tick labels are required");
		}
		labels = tickLabels.clone();
		for (String label : labels) {
			Objects.requireNonNull(label, "tickLabels contains null");
		}
        repaint();
    }

    /**
     * Set the painted gradient height. The component's preferred and minimum
     * heights are updated so layout managers cannot collapse the gradient.
     *
     * @param height gradient height in pixels; values below 2 are clamped
     */
    public void setBarHeight(int height) {
        barHeight = Math.max(2, height);
        updateComponentHeights();
        revalidate();
        repaint();
    }

    /** @return the requested gradient height in pixels */
    public int getBarHeight() {
        return barHeight;
    }

    private void updateComponentHeights() {
        java.awt.Font font = getFont();
        if (font == null) {
            font = UIManager.getFont("Label.font");
        }
        if (font == null) {
            font = new java.awt.Font(java.awt.Font.SANS_SERIF,
                    java.awt.Font.PLAIN, 11);
        }
        int labelHeight = getFontMetrics(font).getHeight();
        Insets insets = getInsets();
        int height = insets.top + 3 + barHeight + VERTICAL_GAP
                + labelHeight + 2 + insets.bottom;
        int width = Math.max(200, getPreferredSize().width);
        Dimension size = new Dimension(width, height);
        setPreferredSize(size);
        setMinimumSize(new Dimension(1, height));
    }

    @Override
    public void setBorder(javax.swing.border.Border border) {
        super.setBorder(border);
        if (barHeight > 0) {
            updateComponentHeights();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Insets insets = getInsets();
        int left = insets.left + HORIZONTAL_INSET;
        int right = getWidth() - insets.right - HORIZONTAL_INSET;
        int top = insets.top + 3;
        FontMetrics fm = g2d.getFontMetrics();
        int availableBottom = getHeight() - insets.bottom - fm.getHeight()
                - VERTICAL_GAP - 2;
        int paintedBarHeight = Math.min(barHeight,
                Math.max(2, availableBottom - top));
        int barBottom = top + paintedBarHeight;
        int labelY = barBottom + VERTICAL_GAP + fm.getAscent();

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
        g2d.drawRect(left, top, right - left, paintedBarHeight);

        // 3) Labels
        g2d.setColor(getForeground());
        int intervals = labels.length - 1;
        for (int i = 0; i < labels.length; i++) {
            String label = labels[i];
            int center = left + (int) Math.round(i * (right - left) / (double) intervals);
            int labelWidth = fm.stringWidth(label);
            int labelX = center - labelWidth / 2;
            if (i == 0) {
                labelX = left;
            } else if (i == intervals) {
                labelX = right - labelWidth;
            }
            g2d.drawString(label, labelX, labelY);
        }
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
