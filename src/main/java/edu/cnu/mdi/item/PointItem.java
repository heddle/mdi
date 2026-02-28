package edu.cnu.mdi.item;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.SymbolDraw;
import edu.cnu.mdi.graphics.style.SymbolType;

/**
 * Basic point item
 *
 * @author heddle
 *
 */
public class PointItem extends AItem {


	// the alignment values
	private int _xAlignment = SwingUtilities.CENTER;
	private int _yAlignment = SwingUtilities.CENTER;

	// some point items will display an icon instead of a symbol. If so, this is the icon to display.
	protected ImageIcon icon;

	/**
	 * Constructor for a basic point item.
	 *
	 * @param layer the layer this item is on.
	 */
	public PointItem(Layer layer) {
		super(layer);
		_focus = new Point2D.Double(Double.NaN, Double.NaN);
	}

	/**
	 * Constructor for a basic point item.
	 *
	 * @param layer the layer this item is on.
	 * @param icon     an icon to draw at the point
	 */
	public PointItem(Layer layer, ImageIcon icon) {
		super(layer);
		this.icon = icon;
		_focus = new Point2D.Double(Double.NaN, Double.NaN);
	}

	/**
	 * Constructor for a basic point item.
	 *
	 * @param layer the layer this item is on.
	 * @param location the location for the point.
	 */
	public PointItem(Layer layer, Point2D.Double location) {
		this(layer, location, (Object[])null);
	}
	
	/**
	 * Constructor for a basic point item.
	 *
	 * @param layer the layer this item is on.
	 * @param location the location for the point.
	 * @param keyVals optional key-value pairs to set on the item. For example, you could
	 * use this to set the display name, style, or other properties at construction time. 
	 * The keys should be from the set of standard keys defined in PropertyUtils, or any 
	 * custom keys recognized by this item or its style.
	 */
	public PointItem(Layer layer, Point2D.Double location, Object... keyVals) {
		super(layer, keyVals);
		_focus = new Point2D.Double(location.x, location.y);
	}


	/**
	 * Custom drawer for the item.
	 *
	 * @param g         the graphics context.
	 * @param container the graphical container being rendered.
	 */
	@Override
	public void drawItem(Graphics g, IContainer container) {

		// draw icon?
		if (icon != null) {
			Point p = getFocusPoint(container);
			int w = icon.getIconWidth();
			int h = icon.getIconHeight();

			int x = p.x;
			int y = p.y;

			switch (_xAlignment) {
			case SwingUtilities.LEFT:
				break;
			case SwingUtilities.CENTER:
				x -= w / 2;
				break;
			case SwingUtilities.RIGHT:
				x -= w;
				break;
			}

			switch (_yAlignment) {
			case SwingUtilities.TOP:
				break;
			case SwingUtilities.CENTER:
				y -= h / 2;
				break;
			case SwingUtilities.BOTTOM:
				y -= h;
				break;
			}

			g.drawImage(icon.getImage(), x, y, container.getComponent());
		} else {
			// draw symbol?
			if (_style.getSymbolType() != SymbolType.NOSYMBOL) {
				Rectangle r = getBounds(container);
				int xc = r.x + r.width / 2;
				int yc = r.y + r.height / 2;
				SymbolDraw.drawSymbol(g, xc, yc, _style);
			}
		}
	}

	/**
	 * Checks whether the item should be drawn. This is an additional check, beyond
	 * the simple visibility flag check. For example, it might check whether the
	 * item intersects the area being drawn.
	 *
	 * @param g         the graphics context.
	 * @param container the graphical container being rendered.
	 * @return <code>true</code> if the item passes any and all tests, and should be
	 *         drwan.
	 */
	@Override
	public boolean shouldDraw(Graphics g, IContainer container) {
		Rectangle r = getBounds(container);
		Rectangle b = container.getComponent().getBounds();
		b.x = 0;
		b.y = 0;
		return b.intersects(r);
	}

	/**
	 * get the bounding rect in pixels.
	 *
	 * @param container the container being rendered
	 * @return the box around the active part of the image.
	 */
	@Override
	public Rectangle getBounds(IContainer container) {

		if (icon != null) {
			Point p = new Point();
			container.worldToLocal(p, _focus);

			int w = icon.getIconWidth();
			int h = icon.getIconHeight();

			int x = p.x;
			int y = p.y;

			switch (_xAlignment) {
			case SwingUtilities.LEFT:
				break;
			case SwingUtilities.CENTER:
				x -= w / 2;
				break;
			case SwingUtilities.RIGHT:
				x -= w;
				break;
			}

			switch (_yAlignment) {
			case SwingUtilities.TOP:
				break;
			case SwingUtilities.CENTER:
				y -= h / 2;
				break;
			case SwingUtilities.BOTTOM:
				y -= h;
				break;
			}

			return new Rectangle(x, y, w, h);
		}

		Point p = new Point();
		container.worldToLocal(p, _focus);
		int size = _style.getSymbolSize();
		int size2 = size / 2;
        return new Rectangle(p.x - size2, p.y - size2, size, size);
	}

	/**
	 * A modification (can only be a drag for a point item) has occurred.
	 */
	@Override
	public void startModification() {
		_modification.setStartFocus(getFocus());
		_modification.setStartFocusPoint(getFocusPoint(_modification.getContainer()));
	}

	/**
	 * A modification such as a drag, resize or rotate is continuing.
	 */
	@Override
	public void modify() {
		Point startFocusPoint = _modification.getStartFocusPoint();
		if (startFocusPoint != null) {
			// compute the total mouse delta
			Point startMouse = _modification.getStartMousePoint();
			Point currentMouse = _modification.getCurrentMousePoint();
			int dx = currentMouse.x - startMouse.x;
			int dy = currentMouse.y - startMouse.y;

			Point newFocusPoint = new Point(startFocusPoint.x + dx, startFocusPoint.y + dy);
			Point2D.Double wp = new Point2D.Double();
			_modification.getContainer().localToWorld(newFocusPoint, wp);
			setFocus(wp);
			_modification.getContainer().refresh();
		}
	}

	/**
	 * Set the current location.
	 *
	 * @param currentLocation the new location to set
	 */
	@Override
	public void setFocus(Point2D.Double currentLocation) {
		if (currentLocation == null) {
			_focus.x = Double.NaN;
			_focus.y = Double.NaN;
		} else {
			_focus.x = currentLocation.x;
			_focus.y = currentLocation.y;
		}
	}

	/**
	 * Get the world bounding rectangle of the item.
	 *
	 * @return the world box containing the item. For a point item, which has no
	 *         extent, this is <code>null</code>.
	 */
	@Override
	public Rectangle2D.Double getWorldBounds() {
		return null;
	}

	/**
	 * Get the horizontal alignment for the image icon
	 *
	 * @return the horizontal alignment for the image icon
	 */
	public int getAlignmentH() {
		return _xAlignment;
	}

	/**
	 * Set the horizontal alignment for the image icon
	 *
	 * @param xAlignment the xAlignment to set
	 */
	public void setAlignmentH(int xAlignment) {
		this._xAlignment = xAlignment;
	}

	/**
	 * Get the vertical alignment for the image icon
	 *
	 * @return the vertical alignment for the image icon
	 */
	public int getAlignmentV() {
		return _yAlignment;
	}

	/**
	 * Set the vertical alignment for the image icon
	 *
	 * @param yAlignment the yAlignment to set
	 */
	public void setAlignmentV(int yAlignment) {
		this._yAlignment = yAlignment;
	}

	@Override
	public void translateWorld(double dx, double dy) {
		if (_focus == null) {
			_focus = new Point2D.Double(Double.NaN, Double.NaN);
		}
		// If not yet placed, treat as no-op.
		if (Double.isNaN(_focus.x) || Double.isNaN(_focus.y) || (Math.abs(dx) < 1.0e-12 && Math.abs(dy) < 1.0e-12)) {
			return;
		}
		_focus.x += dx;
		_focus.y += dy;
		// PointItem doesn't maintain a Path2D, but it does participate in selection,
		// caching, and redraw logic. Mark dirty so any cached draw info is invalidated.
		setDirty(true);
	}

}
