package edu.cnu.mdi.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JToolBar;

/** Scale-aware helpers for deriving Swing sizes from rendered content. */
public final class SwingSizingUtils {

    private SwingSizingUtils() {
        throw new AssertionError("No SwingSizingUtils instances");
    }

    /** Return the greatest preferred width among a baseline and components. */
    public static int preferredWidth(int minimumWidth, Component... components) {
        int width = Math.max(1, minimumWidth);
        if (components != null) {
            for (Component component : components) {
                if (component != null) {
                    width = Math.max(width, component.getPreferredSize().width);
                }
            }
        }
        return width;
    }

    /** Return a component's preferred size enlarged to the given baselines. */
    public static Dimension preferredSizeAtLeast(Component component,
            int minimumWidth, int minimumHeight) {
        Dimension preferred = (component == null) ? new Dimension() : component.getPreferredSize();
        return new Dimension(Math.max(Math.max(1, minimumWidth), preferred.width),
                Math.max(Math.max(1, minimumHeight), preferred.height));
    }

    /** Return rendered font height plus total vertical padding. */
    public static int fontHeight(Component component, Font font, int verticalPadding) {
        if (component == null) {
            return Math.max(1, verticalPadding);
        }
        Font measuredFont = (font == null) ? component.getFont() : font;
        FontMetrics metrics = component.getFontMetrics(measuredFont);
        return Math.max(1, metrics.getHeight() + Math.max(0, verticalPadding));
    }

    /** Return the rendered width of text plus total horizontal padding. */
    public static int textWidth(Component component, String text, int horizontalPadding) {
        if (component == null) {
            return Math.max(1, horizontalPadding);
        }
        String safeText = (text == null) ? "" : text;
        return Math.max(1, component.getFontMetrics(component.getFont()).stringWidth(safeText)
                + Math.max(0, horizontalPadding));
    }

    /** Return a conservative rendered width for a number of text columns. */
    public static int textColumnWidth(Component component, int columns,
            int minimumWidth, int horizontalPadding) {
        String sample = "M".repeat(Math.max(1, columns));
        return Math.max(Math.max(1, minimumWidth),
                textWidth(component, sample, horizontalPadding));
    }

    /** Return an icon-safe size including margins and a minimum baseline. */
    public static Dimension iconButtonSize(Icon icon, Insets margins, Dimension minimum) {
        Insets safeMargins = (margins == null) ? new Insets(0, 0, 0, 0) : margins;
        Dimension safeMinimum = (minimum == null) ? new Dimension(1, 1) : minimum;
        int iconWidth = (icon == null) ? 0 : Math.max(0, icon.getIconWidth());
        int iconHeight = (icon == null) ? 0 : Math.max(0, icon.getIconHeight());
        return new Dimension(
                Math.max(1, Math.max(safeMinimum.width,
                        iconWidth + safeMargins.left + safeMargins.right)),
                Math.max(1, Math.max(safeMinimum.height,
                        iconHeight + safeMargins.top + safeMargins.bottom)));
    }

	/**
	 * Return the minimum height that can contain every visible component in a
	 * horizontal toolbar, including the toolbar's border insets.
	 *
	 * @param toolbar toolbar to measure, or {@code null}
	 * @return minimum safe height, at least one pixel
	 */
	public static int requiredHorizontalToolbarHeight(JToolBar toolbar) {
		if (toolbar == null) {
			return 1;
		}
		int contentHeight = 0;
		for (Component component : toolbar.getComponents()) {
			if (component.isVisible()) {
				contentHeight = Math.max(contentHeight,
						Math.max(component.getMinimumSize().height,
								component.getPreferredSize().height));
			}
		}
		Insets insets = toolbar.getInsets();
		return Math.max(1, contentHeight + insets.top + insets.bottom);
	}

    /**
     * Return usable width to the right of an internal frame within its virtual
     * desktop column.
     *
     * <p>MDI implements virtual columns by offsetting frame X coordinates by
     * whole desktop widths. Consequently, an off-screen frame can have a very
     * large positive or negative X coordinate. Reducing that coordinate modulo
     * the desktop width recovers its column-local position.</p>
     */
    public static int availableDesktopWidth(JInternalFrame frame) {
        if (frame == null) {
            return Integer.MAX_VALUE;
        }
        JDesktopPane desktop = frame.getDesktopPane();
        if (desktop == null || desktop.getWidth() <= 0) {
            return Integer.MAX_VALUE;
        }
        int columnLocalX = Math.floorMod(frame.getX(), desktop.getWidth());
        return Math.max(1, desktop.getWidth() - columnLocalX);
    }
}
