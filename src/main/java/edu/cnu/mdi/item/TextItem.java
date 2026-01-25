package edu.cnu.mdi.item;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.SwingConstants;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.dialog.TextEditDialog;
import edu.cnu.mdi.graphics.text.UnicodeUtils;
import edu.cnu.mdi.swing.WindowPlacement;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.util.TextUtils;

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

	private static final float LINESIZE = 1.0f;

	// default font
	private static final Font _defaultFont = Fonts.plainFontDelta(0);

	// font for rendering
	private Font _font = _defaultFont;

	// when text is multi-line, holds the lines
	private String _lines[];

	// the text alignment
	private int alignment = SwingConstants.LEFT;

	/**
	 *
	 * @param itemList  the list which will hold the item.
	 * @param location  the location of the lower left
	 * @param font      the font to use.
	 * @param text      the text to display.
	 * @param fillColor the text background color.
	 * @param lineColor the border color
	 * @param textColor the text foreground color.
	 */
	public TextItem(Layer itemList, Point2D.Double location, Font font, String text,
			Color lineColor, Color fillColor, Color textColor) {
		super(itemList, getWorldRectangle(itemList.getContainer(), location, font, text));
		setFont(font);
		setText(text);
		_style.setFillColor(fillColor);
		_style.setLineColor(lineColor);
		_style.setTextColor(textColor == null ? Color.BLACK : textColor);

		_focus = location;
		_resizePolicy = ResizePolicy.SCALEONLY;
		setPath(getUnrotatedPoints());
	}

	@Override
	public void drawItem(Graphics g, IContainer container) {
		super.drawItem(g, container);

		Point cp = new Point();
		container.worldToLocal(cp, _focus);
		TextUtils.drawRotatedText(g, cp, getText(), _font, _style.getTextColor(),
				getAzimuth(), alignment);
	}

	/**
	 * Get the text alignment.
	 *
	 * @return the alignment (SwingConstants.LEFT, CENTER, RIGHT)
	 */
	public int getAlignment() {
		return alignment;
	}

	/**
	 * Set the text alignment.
	 *
	 * @param alignment the alignment (SwingConstants.LEFT, CENTER, RIGHT)
	 */
	public void setAlignment(int alignment) {
		this.alignment = alignment;
	}


	// get the unrotated corner points of the text box
	private Point2D.Double[] getUnrotatedPoints() {
		FontMetrics fm = getContainer().getComponent().getFontMetrics(_font);
		Rectangle r = TextUtils.textBounds(getText(), fm, MARGIN, MARGIN, MARGIN, MARGIN, LINESIZE);
		Dimension size = new Dimension(r.width, r.height);
		Point2D.Double bl = new Point2D.Double(_focus.x - size.width / 2, _focus.y - size.height / 2);
		Point2D.Double br = new Point2D.Double(_focus.x + size.width / 2, _focus.y - size.height / 2);
		Point2D.Double tr = new Point2D.Double(_focus.x + size.width / 2, _focus.y + size.height / 2);
		Point2D.Double tl = new Point2D.Double(_focus.x - size.width / 2, _focus.y + size.height / 2);
		return new Point2D.Double[] { bl, br, tr, tl };
	}

	public Point2D.Double[] getRotatedPoints(double azimuthDegrees) {
	    // 1. Get the unrotated corners
	    Point2D.Double[] points = getUnrotatedPoints();

	    // 2. Create the rotation transform
	    // Note: Use negative degrees for counter-clockwise rotation in standard AWT
	    double angleInRadians = Math.toRadians(-azimuthDegrees);
	    AffineTransform at = AffineTransform.getRotateInstance(angleInRadians, _focus.x, _focus.y);

	    // 3. Transform the points
	    Point2D.Double[] rotatedPoints = new Point2D.Double[points.length];
	    at.transform(points, 0, rotatedPoints, 0, points.length);

	    return rotatedPoints;
	}


	/**
	 * Edit the text item.
	 */
	public void edit() {
		TextEditDialog dialog = new TextEditDialog(this);
		WindowPlacement.centerComponent(dialog);
		dialog.setVisible(true);

		if (dialog.isCancelled()) {
			return;
		}
		dialog.updateTextItem(this);
		this.getContainer().refresh();
		setPath(getRotatedPoints(getAzimuth()));
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
	 * @return the text as a single line
	 */
	public String getText() {
		return String.join("\n", _lines);
	}

	/**
	 * @param text the text to set
	 */
	public void setText(String text) {
		text = UnicodeUtils.specialCharReplace(text);
		_lines = text.lines().toArray(String[]::new);
	}


	// get the world rectangle bounds
	private static Rectangle2D.Double getWorldRectangle(IContainer container,
			Point2D.Double location, Font font,
			String text) {

		FontMetrics fm = container.getComponent().getFontMetrics(font);
		Rectangle r = TextUtils.textBounds(text, fm, MARGIN, MARGIN, MARGIN, MARGIN, LINESIZE);
		r.x += (int) location.x - r.width / 2;
		r.y += (int) location.y - r.height / 2;
		Rectangle2D.Double wr = new Rectangle2D.Double();
		container.localToWorld(r, wr);
		return wr;
	}

}
