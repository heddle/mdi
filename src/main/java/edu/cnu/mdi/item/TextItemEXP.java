package edu.cnu.mdi.item;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D.Double;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.GraphicsUtils;
import edu.cnu.mdi.graphics.text.Snippet;
import edu.cnu.mdi.graphics.text.UnicodeSupport;
import edu.cnu.mdi.graphics.world.WorldGraphicsUtils;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.util.TextUtils;

/**
 * EXPERIMENTAL!!!!!!!!!!!!!!!!
 * A drawable item that renders text with a rectangular background/border.
 * <p>
 * Key invariants:
 * <ul>
 *   <li>{@link #_focus} is the <b>center</b> of the text box.</li>
 *   <li>When the item is being modified (drag/resize/rotate), we do <b>not</b>
 *       rebuild the path from font metrics. We rely on the affine transforms in
 *       {@link PathBasedItem#modify()} so the rectangle and handles stay
 *       lockstep with the mouse.</li>
 *   <li>The text baseline point is tracked as a {@link #_secondaryPoints}
 *       entry so it transforms together with the rectangle.</li>
 *   <li>When the modification ends, we “snap” back to a clean metrics-derived
 *       rectangle for stable serialization and consistent redraw.</li>
 * </ul>
 * </p>
 */
public class TextItemEXP extends RectangleItem {

	// makes the bounds bigger than the text by a bit
	private static final int MARGIN = 4;

	// twice the margin
	private static final int MARGIN2 = 2 * MARGIN;

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
	public TextItemEXP(Layer itemList, Point2D.Double location, Font font, String text, Color textColor,
			Color fillColor, Color lineColor) {
		super(itemList, new Rectangle2D.Double(location.x, location.y, 1, 1));
		setFont(font);
		setText(text);
		_textColor = textColor;
		_style.setFillColor(fillColor);
		_style.setLineColor(lineColor); 

		_focus = location;
		_resizePolicy = ResizePolicy.SCALEONLY;
		setPath(getPoints());
	}

	private Point2D.Double[] getPoints() {
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

		return new Dimension(width + MARGIN2, height + MARGIN2);
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
