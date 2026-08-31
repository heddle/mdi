package edu.cnu.mdi.item;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.SwingConstants;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.dialog.TextEditDialog;
import edu.cnu.mdi.graphics.world.WorldGraphicsUtils;
import edu.cnu.mdi.swing.WindowPlacement;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.util.TextUtils;
import edu.cnu.mdi.util.UnicodeUtils;

/**
 * A drawable item that renders text with a rectangular background/border.
 * <p>
 * Key invariants:
 * </p>
 * <ul>
 * <li>{@link #_focus} is the world-coordinate center of the text box.</li>
 *
 * <li>Path corners are computed via a <b>screen-space round-trip</b>:
 *     world focus &rarr; screen focus &rarr; pixel corners &rarr; world corners.
 *     This is required because font metrics are in pixels and the world
 *     coordinate system may have an entirely different scale.</li>
 *
 * <li>Rotation is handled in <b>screen space</b>. A world-space affine rotation
 *     only looks correct when scaleX == scaleY. With a non-square world
 *     coordinate system, a world-space rotation shears the rectangle so it no
 *     longer matches the text drawn by {@link TextUtils#drawRotatedText}, which
 *     rotates in screen space.</li>
 *
 * <li>The stored font is the logical/base font. At draw time, a temporary
 *     derived font is chosen to fit the current screen-space text box. This
 *     lets text scale visually with zooming and item resizing without mutating
 *     the stored font.</li>
 * </ul>
 */
public class TextItem extends RectangleItem implements ITextEditable {

	private static final int   MARGIN   = 4;
	private static final float LINESIZE = 1.0f;

	private static final Font _defaultFont = Fonts.plainFontDelta(0);

	private Font     _font      = _defaultFont;
	private String[] _lines;
	private int      alignment  = SwingConstants.LEFT;

	/**
	 * @param itemList  layer to add this item to
	 * @param location  world-coordinate center of the text box
	 * @param font      font to use
	 * @param text      text to display
	 * @param lineColor border color
	 * @param fillColor background color
	 * @param textColor text foreground color
	 */
	public TextItem(Layer itemList, Point2D.Double location, Font font, String text,
			Color lineColor, Color fillColor, Color textColor) {
		super(itemList, getWorldRectangle(itemList.getContainer(), location, font, text));

		setFont(font);
		setText(text);

		_style.setFillColor(fillColor);
		_style.setLineColor(lineColor);
		_style.setTextColor(textColor == null ? Color.BLACK : textColor);

		_focus = new Point2D.Double(location.x, location.y);
		_resizePolicy = ResizePolicy.SCALEONLY;

		setPath(getUnrotatedPoints());
	}

	// -----------------------------------------------------------------------
	// Drawing
	// -----------------------------------------------------------------------

	@Override
	public void drawItem(Graphics2D g2, IContainer container) {
		super.drawItem(g2, container);   // draws filled/stroked rectangle path

		Point cp = new Point();
		container.worldToLocal(cp, _focus);

		Font drawFont = getFittedFont(g2, container);

		TextUtils.drawRotatedText(g2, cp, getText(), drawFont, _style.getTextColor(),
				getAzimuth(), alignment);
	}

	/**
	 * Get the font that best fits the current screen-space size of the text box.
	 * This is a draw-time font only; it does not mutate the stored base font.
	 */
	private Font getFittedFont(Graphics2D g2, IContainer container) {
		int[] size = getPathScreenSize(container, _path);
		int boxWidth = Math.max(1, size[0]);
		int boxHeight = Math.max(1, size[1]);

		float lo = 1.0f;
		float hi = Math.max(2.0f, _font.getSize2D());

		// If the base font fits, allow growth. This handles zoom-in and
		// enlarged/resized text boxes.
		while (hi < 2048.0f && fontFits(g2, _font.deriveFont(hi), boxWidth, boxHeight)) {
			lo = hi;
			hi *= 2.0f;
		}

		// Binary search for the largest fitting size.
		for (int i = 0; i < 12; i++) {
			float mid = 0.5f * (lo + hi);
			Font trial = _font.deriveFont(mid);

			if (fontFits(g2, trial, boxWidth, boxHeight)) {
				lo = mid;
			} else {
				hi = mid;
			}
		}

		return _font.deriveFont(Math.max(1.0f, lo));
	}

	/**
	 * Check whether a font fits inside the current box. The same margins used
	 * to create the original text rectangle are included here.
	 */
	private boolean fontFits(Graphics2D g2, Font font, int boxWidth, int boxHeight) {
		FontMetrics fm = g2.getFontMetrics(font);
		Rectangle r = TextUtils.textBounds(getText(), fm,
				MARGIN, MARGIN, MARGIN, MARGIN, LINESIZE);

		return (r.width <= boxWidth) && (r.height <= boxHeight);
	}

	// -----------------------------------------------------------------------
	// Rotation — overridden to work in screen space
	// -----------------------------------------------------------------------

	/**
	 * Override ROTATE so the bounding-box path is rebuilt via a screen-space
	 * rotation rather than a world-space affine rotation.
	 *
	 * <p>DRAG and RESIZE fall through to {@link PathBasedItem#modify()}.</p>
	 */
	@Override
	public void modify() {
		if (_modification == null) return;

		if (_modification.getType() == ItemModification.ModificationType.ROTATE) {
			IContainer container = _modification.getContainer();

			// Angle from screen-space mouse points, same as PathBasedItem.
			Point p1     = _modification.getStartMousePoint();
			Point vertex = _modification.getStartFocusPoint();
			Point p2     = _modification.getCurrentMousePoint();

			double angle = threePointAngle(p1, vertex, p2);
			angle = ((int) angle);   // preserve original integer-snap behaviour

			double newAzimuth = _modification.getStartAzimuth() + angle;
			setAzimuth(newAzimuth);

			// Preserve current/resized box dimensions while rotating.
			int[] size = getPathScreenSize(container, _modification.getStartPath());
			setPath(getRotatedPoints(newAzimuth, size[0], size[1]));

			geometryChanged();
			container.refresh();
		} else {
			super.modify();   // DRAG and RESIZE handled by PathBasedItem
		}
	}

	// -----------------------------------------------------------------------
	// Geometry helpers
	// -----------------------------------------------------------------------

	/**
	 * Four unrotated world corners: world focus &rarr; screen centre &rarr;
	 * pixel half-extents &rarr; screen corners &rarr; world corners.
	 */
	private Point2D.Double[] getUnrotatedPoints() {
		IContainer container = getContainer();
		FontMetrics fm = container.getComponent().getFontMetrics(_font);
		Rectangle r = TextUtils.textBounds(getText(), fm,
				MARGIN, MARGIN, MARGIN, MARGIN, LINESIZE);

		Point sf = new Point();
		container.worldToLocal(sf, _focus);

		int hw = r.width  / 2;
		int hh = r.height / 2;

		// Screen corners: bl, br, tr, tl  (screen y-down: bottom = sf.y + hh)
		Point2D.Double wbl = toWorld(container, sf.x - hw, sf.y + hh);
		Point2D.Double wbr = toWorld(container, sf.x + hw, sf.y + hh);
		Point2D.Double wtr = toWorld(container, sf.x + hw, sf.y - hh);
		Point2D.Double wtl = toWorld(container, sf.x - hw, sf.y - hh);

		return new Point2D.Double[] { wbl, wbr, wtr, wtl };
	}

	/**
	 * Four rotated world corners, using the natural text/font bounds.
	 *
	 * @param azimuthDegrees screen-space CW angle in degrees
	 */
	public Point2D.Double[] getRotatedPoints(double azimuthDegrees) {
		IContainer container = getContainer();
		FontMetrics fm = container.getComponent().getFontMetrics(_font);
		Rectangle r = TextUtils.textBounds(getText(), fm,
				MARGIN, MARGIN, MARGIN, MARGIN, LINESIZE);

		return getRotatedPoints(azimuthDegrees, r.width, r.height);
	}

	/**
	 * Four rotated world corners using explicit screen-space width/height.
	 * This is used during rotation so a resized text box keeps its resized
	 * dimensions instead of snapping back to the natural text bounds.
	 *
	 * @param azimuthDegrees screen-space CW angle in degrees
	 * @param width screen-space box width
	 * @param height screen-space box height
	 */
	private Point2D.Double[] getRotatedPoints(double azimuthDegrees, int width, int height) {
		IContainer container = getContainer();

		Point sf = new Point();
		container.worldToLocal(sf, _focus);

		int hw = Math.max(1, width  / 2);
		int hh = Math.max(1, height / 2);

		// Unrotated offsets from screen focus. Order: bl, br, tr, tl.
		double[] ox = { -hw,  hw,  hw, -hw };
		double[] oy = {  hh,  hh, -hh, -hh };

		// Screen-space rotation matrix. Positive is clockwise in screen coords,
		// matching the convention used by TextUtils.drawRotatedText.
		double rad = Math.toRadians(azimuthDegrees);
		double cos = Math.cos(rad);
		double sin = Math.sin(rad);

		Point2D.Double[] world = new Point2D.Double[4];
		for (int i = 0; i < 4; i++) {
			double rx = cos * ox[i] - sin * oy[i];
			double ry = sin * ox[i] + cos * oy[i];

			world[i] = toWorld(container,
					(int) Math.round(sf.x + rx),
					(int) Math.round(sf.y + ry));
		}

		return world;
	}

	/**
	 * Screen-space width and height of a path-based text box.
	 * Assumes TextItem path point order: bl, br, tr, tl.
	 */
	private int[] getPathScreenSize(IContainer container, Path2D.Double path) {
		if (path == null) {
			return new int[] { 1, 1 };
		}

		Point2D.Double[] wpoly = WorldGraphicsUtils.pathToWorldPolygon(path);
		if (wpoly == null || wpoly.length < 4) {
			Rectangle b = getBounds(container);
			if (b == null) {
				return new int[] { 1, 1 };
			}
			return new int[] {
					Math.max(1, b.width),
					Math.max(1, b.height)
			};
		}

		Point p0 = new Point();
		Point p1 = new Point();
		Point p2 = new Point();

		container.worldToLocal(p0, wpoly[0]); // bl
		container.worldToLocal(p1, wpoly[1]); // br
		container.worldToLocal(p2, wpoly[2]); // tr

		int width  = (int) Math.round(p0.distance(p1));
		int height = (int) Math.round(p1.distance(p2));

		return new int[] {
				Math.max(1, width),
				Math.max(1, height)
		};
	}

	/** Convert a single screen point to world coordinates. */
	private static Point2D.Double toWorld(IContainer container, int sx, int sy) {
		Point2D.Double wp = new Point2D.Double();
		container.localToWorld(new Point(sx, sy), wp);
		return wp;
	}

	// -----------------------------------------------------------------------
	// Edit dialog
	// -----------------------------------------------------------------------

	/** Open the text-edit dialog and rebuild the path at the current azimuth. */
	public void edit() {
		TextEditDialog dialog = new TextEditDialog(this);
		WindowPlacement.centerComponent(dialog);
		dialog.setVisible(true);
		if (dialog.isCancelled()) return;

		dialog.updateTextItem(this);
		setPath(getRotatedPoints(getAzimuth()));
		geometryChanged();
		getContainer().refresh();
	}

	// -----------------------------------------------------------------------
	// Accessors
	// -----------------------------------------------------------------------

	public int getAlignment() {
		return alignment;
	}

	public void setAlignment(int align) {
		this.alignment = align;
	}

	/** @return the current font */
	public Font getFont() {
		return _font;
	}

	/**
	 * @param font the new font, or {@code null} to fall back to the default
	 *             font
	 */
	public void setFont(Font font) {
		_font = (font != null) ? font : _defaultFont;
	}

	/** @return the current text, with lines joined by {@code '\n'} */
	public String getText() {
		return String.join("\n", _lines);
	}

	/**
	 * Sets the displayed text, replacing any prior content.
	 * <p>
	 * LaTeX-like escapes are first rewritten to their Unicode equivalents via
	 * {@link UnicodeUtils#specialCharReplace(String)}, then the result is
	 * split into display lines on line terminators (via
	 * {@link String#lines()}).
	 * </p>
	 *
	 * @param text the new text; must not be {@code null}
	 * @throws NullPointerException if {@code text} is {@code null}
	 */
	public void setText(String text) {
		text = UnicodeUtils.specialCharReplace(text);
		_lines = text.lines().toArray(String[]::new);
	}

	// -----------------------------------------------------------------------
	// Constructor helper — static because called before instance fields are set
	// -----------------------------------------------------------------------

	private static Rectangle2D.Double getWorldRectangle(IContainer container,
			Point2D.Double location, Font font, String text) {

		FontMetrics fm = container.getComponent().getFontMetrics(font);
		Rectangle r = TextUtils.textBounds(text, fm,
				MARGIN, MARGIN, MARGIN, MARGIN, LINESIZE);

		Point sc = new Point();
		container.worldToLocal(sc, location);

		int hw = r.width  / 2;
		int hh = r.height / 2;

		Point2D.Double wtl = new Point2D.Double();
		Point2D.Double wbr = new Point2D.Double();

		container.localToWorld(new Point(sc.x - hw, sc.y - hh), wtl);
		container.localToWorld(new Point(sc.x + hw, sc.y + hh), wbr);

		double wx = Math.min(wtl.x, wbr.x);
		double wy = Math.min(wtl.y, wbr.y);
		double ww = Math.abs(wbr.x - wtl.x);
		double wh = Math.abs(wbr.y - wtl.y);

		return new Rectangle2D.Double(wx, wy, ww, wh);
	}
}