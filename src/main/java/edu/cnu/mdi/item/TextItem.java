package edu.cnu.mdi.item;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.SwingConstants;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.dialog.TextEditDialog;
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
 *     world focus → screen focus → pixel corners → world corners.
 *     This is required because font metrics are in pixels and the world
 *     coordinate system may have an entirely different scale.</li>
 *
 * <li>Rotation is handled in <b>screen space</b>. A world-space affine rotation
 *     only looks correct when scaleX == scaleY (square world units). With a
 *     non-square world (e.g. world=(0,0,1,1) on a non-square window) a
 *     world-space rotation shears the rectangle so it no longer matches the
 *     text drawn by {@link TextUtils#drawRotatedText}, which rotates in screen
 *     space. The fix: rotate the four screen corners, then convert to world.</li>
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
		TextUtils.drawRotatedText(g2, cp, getText(), _font, _style.getTextColor(),
				getAzimuth(), alignment);
	}

	// -----------------------------------------------------------------------
	// Rotation — overridden to work in screen space
	// -----------------------------------------------------------------------

	/**
	 * Override ROTATE so the bounding-box path is rebuilt via a screen-space
	 * rotation rather than a world-space affine rotation.
	 *
	 * <p>A world-space rotation is only correct when scaleX == scaleY. With a
	 * non-square world coordinate system the inherited world-space rotation
	 * shears the rectangle out of alignment with the text drawn by
	 * {@link TextUtils#drawRotatedText} (which rotates in screen space).
	 * By computing the new path corners in screen space we guarantee that the
	 * outline exactly matches the text at every azimuth.</p>
	 *
	 * <p>DRAG and RESIZE fall through to {@link PathBasedItem#modify()}, which
	 * operates correctly in world space for those two operations.</p>
	 */
	@Override
	public void modify() {
		if (_modification == null) return;

		if (_modification.getType() == ItemModification.ModificationType.ROTATE) {
			// Angle from screen-space mouse points (same as PathBasedItem).
			Point p1     = _modification.getStartMousePoint();
			Point vertex = _modification.getStartFocusPoint();
			Point p2     = _modification.getCurrentMousePoint();
			double angle = threePointAngle(p1, vertex, p2);
			angle = ((int) angle);   // preserve original integer-snap behaviour

			double newAzimuth = _modification.getStartAzimuth() + angle;
			setAzimuth(newAzimuth);

			// Rebuild path corners in screen space so the rectangle
			// matches what drawRotatedText draws regardless of world scale.
			setPath(getRotatedPoints(newAzimuth));

			geometryChanged();
			_modification.getContainer().refresh();
		} else {
			super.modify();   // DRAG and RESIZE handled by PathBasedItem
		}
	}

	// -----------------------------------------------------------------------
	// Geometry helpers
	// -----------------------------------------------------------------------

	/**
	 * Four unrotated world corners: world focus → screen centre →
	 * pixel half-extents → screen corners → world corners.
	 */
	private Point2D.Double[] getUnrotatedPoints() {
		IContainer container = getContainer();
		FontMetrics fm = container.getComponent().getFontMetrics(_font);
		Rectangle r = TextUtils.textBounds(getText(), fm, MARGIN, MARGIN, MARGIN, MARGIN, LINESIZE);

		Point sf = new Point();
		container.worldToLocal(sf, _focus);

		int hw = r.width  / 2;
		int hh = r.height / 2;

		// Screen corners: bl, br, tr, tl  (screen y-down: bottom = sf.y + hh)
		Point2D.Double wbl = toWorld(container, sf.x - hw, sf.y + hh);
		Point2D.Double wbr = toWorld(container, sf.x + hw, sf.y + hh);
		Point2D.Double wtr = toWorld(container, sf.x + hw, sf.y - hh);
		Point2D.Double wtl = toWorld(container, sf.x - hw, sf.y - hh);
		return new Point2D.Double[]{ wbl, wbr, wtr, wtl };
	}

	/**
	 * Four rotated world corners.  Rotation is done in screen space (CW for
	 * positive degrees, matching {@link TextUtils#drawRotatedText}) and the
	 * result is converted back to world coordinates.
	 *
	 * @param azimuthDegrees screen-space CW angle in degrees
	 */
	public Point2D.Double[] getRotatedPoints(double azimuthDegrees) {
		IContainer container = getContainer();
		FontMetrics fm = container.getComponent().getFontMetrics(_font);
		Rectangle r = TextUtils.textBounds(getText(), fm, MARGIN, MARGIN, MARGIN, MARGIN, LINESIZE);

		Point sf = new Point();
		container.worldToLocal(sf, _focus);

		int hw = r.width  / 2;
		int hh = r.height / 2;

		// Unrotated offsets from screen focus
		double[] ox = { -hw,  hw,  hw, -hw };
		double[] oy = {  hh,  hh, -hh, -hh };

		// Screen-space rotation matrix (CW positive = standard g2.rotate convention)
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
		getContainer().refresh();
		setPath(getRotatedPoints(getAzimuth()));
	}

	// -----------------------------------------------------------------------
	// Accessors
	// -----------------------------------------------------------------------

	public int getAlignment()              { return alignment; }
	public void setAlignment(int align)    { this.alignment = align; }

	public Font getFont()                  { return _font; }
	public void setFont(Font font) {
		_font = (font != null) ? font : _defaultFont;
	}

	public String getText() {
		return String.join("\n", _lines);
	}
	public void setText(String text) {
		text = UnicodeUtils.specialCharReplace(text);
		_lines = text.lines().toArray(String[]::new);
	}

	// -----------------------------------------------------------------------
	// Constructor helper (static — called before instance fields are set)
	// -----------------------------------------------------------------------

	private static Rectangle2D.Double getWorldRectangle(IContainer container,
			Point2D.Double location, Font font, String text) {

		FontMetrics fm = container.getComponent().getFontMetrics(font);
		Rectangle r = TextUtils.textBounds(text, fm, MARGIN, MARGIN, MARGIN, MARGIN, LINESIZE);

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