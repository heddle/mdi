package edu.cnu.mdi.item;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.text.Snippet;
import edu.cnu.mdi.graphics.text.UnicodeSupport;
import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * A drawable item that renders text with a rectangular background/border.
 * <p>
 * Key invariants:
 * <ul>
 * <li>{@link #_focus} is the <b>center</b> of the text box.</li>
 * <li>When the item is being modified (drag/resize/rotate), we do <b>not</b>
 * rebuild the path from font metrics. We rely on the affine transforms in
 * {@link PathBasedItem#modify()} so the rectangle and handles stay lockstep
 * with the mouse.</li>
 * <li>The text baseline point is tracked as a {@link #_secondaryPoints} entry
 * so it transforms together with the rectangle.</li>
 * <li>When the modification ends, we “snap” back to a clean metrics-derived
 * rectangle for stable serialization and consistent redraw.</li>
 * </ul>
 * </p>
 */
public class TextItem extends RectangleItem {

	// makes the bounds bigger than the text by a bit
	private static final int MARGIN = 4;

	// default font
	private static final Font _defaultFont = Fonts.plainFontDelta(0);

	// font for rendering
	private Font _font = _defaultFont;

	// text being rendered
	private String _text;

	// text foreground
	private Color _textColor = Color.black;

	/**
	 *
	 * @param itemList  the list which will hold the item.
	 * @param location  the location of the lower left
	 * @param font      the font to use.
	 * @param text      the text to display.
	 * @param textColor the text foreground color.
	 * @param fillColor the text background color.
	 * @param lineColor the border color
	 */
	public TextItem(Layer itemList, Point2D.Double location, Font font, String text, Color textColor, Color fillColor,
			Color lineColor) {
		super(itemList, new Rectangle2D.Double(location.x, location.y, 1, 1));
		setFont(font);
		setText(text);
		_textColor = textColor;
		_style.setFillColor(fillColor);
		_style.setLineColor(lineColor);

		_focus = location;
		_resizePolicy = ResizePolicy.SCALEONLY;
		setPath(getUnrotatedPoints());
	}

	@Override
	public void drawItem(Graphics g, IContainer container) {
		super.drawItem(g, container);

		Point cp = new Point();
		container.worldToLocal(cp, _focus);
		FontMetrics fm = container.getComponent().getFontMetrics(_font);

		Dimension size = getSize();
		Font fitFont = justFitSize(_text, fm, size.width - 2 * MARGIN, size.height - 2 * MARGIN);
		drawRotatedText(g, cp, _text, fitFont, _textColor, getAzimuth());
	}

	// get the size of the rectangle from the last drawn polygon
	private Dimension getSize() {
		int x0 = _lastDrawnPolygon.xpoints[0];
		int x1 = _lastDrawnPolygon.xpoints[1];
		int x2 = _lastDrawnPolygon.xpoints[2];

		int y0 = _lastDrawnPolygon.ypoints[0];
		int y1 = _lastDrawnPolygon.ypoints[1];
		int y2 = _lastDrawnPolygon.ypoints[2];

		double delx = x2 - x1;
		double dely = y2 - y1;
		int h = (int) Math.sqrt(delx * delx + dely * dely);

		delx = x0 - x1;
		dely = y0 - y1;
		int w = (int) Math.sqrt(delx * delx + dely * dely);

		return new Dimension(w, h);
	}

	/**
	 * Determine the maximum font size that will fit the given string within the
	 * specified width and height.
	 *
	 * @param s  the string to fit
	 * @param fm the FontMetrics of the base font
	 * @param w  the width of the box
	 * @param h  the height of the box
	 * @return a Font object at the maximum size that fits within the box
	 */
	protected Font justFitSize(String s, FontMetrics fm, int w, int h) {
		Font baseFont = fm.getFont();
		// Get the FontRenderContext from the existing metrics to ensure accurate
		// scaling
		FontRenderContext frc = fm.getFontRenderContext();

		int low = 1;
		int high = 1000; // Define a reasonable maximum font size

		Font testFont = baseFont;

		while (low <= high) {
			int mid = (low + high) / 2;
			testFont = baseFont.deriveFont((float) mid);

			// Measure string bounds at this size
			Rectangle2D bounds = testFont.getStringBounds(s, frc);

			// Check if it fits within the box dimensions
			if (bounds.getWidth() <= w && bounds.getHeight() <= h) {
				low = mid + 1;
			} else {
				high = mid - 1; // Too big, try a smaller size
			}
		}
		return testFont;
	}

	public void drawRotatedText(Graphics g, Point cp, String s, Font font, Color tcolor, double theta) {
		Graphics2D g2d = (Graphics2D) g.create();

		// 1. Set font and color
		g2d.setFont(font);
		g2d.setColor(tcolor);

		// 2. Enable antialiasing for smoother text
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// 3. Calculate text dimensions for centering
		FontMetrics fm = g2d.getFontMetrics();
		Rectangle2D rect = fm.getStringBounds(s, g2d);
		double textWidth = rect.getWidth();
		double textHeight = rect.getHeight();

		// 4. Apply transformations
		// Translate to the center point cp
		g2d.translate(cp.x, cp.y);
		// Rotate by theta degrees (convert to radians)
		g2d.rotate(Math.toRadians(theta));

		// 5. Draw string centered on the origin (which is now cp)
		// Horizontal center: -textWidth / 2
		// Vertical center: (textHeight / 2) - ascent to account for the baseline
		float x = (float) (-textWidth / 2);
		float y = (float) (fm.getAscent() - textHeight / 2);

		g2d.drawString(s, x, y);

		// 6. Dispose the graphics copy
		g2d.dispose();
	}

	// get the unrotated corner points of the text box
	private Point2D.Double[] getUnrotatedPoints() {
		Dimension size = sizeText(getContainer().getComponent(), _font, _text);
		Point2D.Double bl = new Point2D.Double(_focus.x - size.width / 2, _focus.y - size.height / 2);
		Point2D.Double br = new Point2D.Double(_focus.x + size.width / 2, _focus.y - size.height / 2);
		Point2D.Double tr = new Point2D.Double(_focus.x + size.width / 2, _focus.y + size.height / 2);
		Point2D.Double tl = new Point2D.Double(_focus.x - size.width / 2, _focus.y + size.height / 2);
		return new Point2D.Double[] { bl, br, tr, tl };
	}

	/**
	 * Get the size of the text, including an invisible border of thickness MARGIN
	 * all around.
	 *
	 * @return the size of the text, including an invisible border of thickness
	 *         MARGIN all around.
	 */
	public static Dimension sizeText(Component component, Font font, String text) {
		if (text == null) {
			return null;
		}

		int width = 0;
		int height = 0;
		ArrayList<Snippet> snippets = Snippet.getSnippets(font, text, component);
		for (Snippet s : snippets) {
			Dimension size = s.size(component);
			width = Math.max(width, s.getDeltaX() + size.width);
			height = Math.max(height, s.getDeltaY() + size.height);
		}

		return new Dimension(width + 2 * MARGIN, height + 2 * MARGIN);
	}

	/**
	 * @return the font
	 */
	public Font getFont() {
		return _font;
	}

	/**
	 * @param font the font to set
	 */
	public void setFont(Font font) {
		_font = font;
		if (_font == null) {
			_font = _defaultFont;
		}
	}

	/**
	 * @return the text
	 */
	public String getText() {
		return _text;
	}

	/**
	 * @param text the text to set
	 */
	public void setText(String text) {
		_text = UnicodeSupport.specialCharReplace(text);
		_text = text;
	}

	/**
	 * Get the text foreground color
	 *
	 * @return the text foreground color
	 */
	public Color getTextColor() {
		return _textColor;
	}

	/**
	 * Set the text foreground color
	 *
	 * @param textForeground the text foreground color to set
	 */
	public void setTextColor(Color textForeground) {
		_textColor = textForeground;
	}

	// get the world rectangle bounds
	private static Rectangle2D.Double getWorldRectangle(IContainer container, Point2D.Double location, Font font,
			String text) {

		Dimension size = sizeText(container.getComponent(), font, text);
		FontMetrics fm = container.getComponent().getFontMetrics(font);
		Point p = new Point();
		container.worldToLocal(p, location);
		Rectangle r = new Rectangle(p.x - MARGIN, p.y - fm.getAscent() - MARGIN, size.width, size.height);
		Rectangle2D.Double wr = new Rectangle2D.Double();
		container.localToWorld(r, wr);
		return wr;
	}

}
