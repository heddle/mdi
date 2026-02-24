package edu.cnu.mdi.item;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Objects;

import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.feedback.IFeedbackProvider;
import edu.cnu.mdi.graphics.ImageManager;
import edu.cnu.mdi.graphics.drawable.IDrawable;
import edu.cnu.mdi.graphics.style.IStyled;
import edu.cnu.mdi.graphics.style.Styled;
import edu.cnu.mdi.item.ItemModification.ModificationType;
import edu.cnu.mdi.ui.colors.X11Colors;
import edu.cnu.mdi.util.Environment;
import edu.cnu.mdi.view.BaseView;

/**
 * This is the base class for custom items that are rendered on an AContainer.
 *
 * @author heddle
 *
 */
public abstract class AItem implements IDrawable, IFeedbackProvider {

	// used for drawing a the focus point
	protected static final Color _FOCUSFILL = new Color(128, 128, 128, 128);

	// used for selecting
	protected static final int SPSIZE = 10;

	// used for selecting
	protected static final int SPSIZE2 = SPSIZE / 2;

	// used for rotating
	protected static final int RPSIZE = 14;

	// used for rotating
	protected static final int RPSIZE2 = RPSIZE / 2;

	// icon for rotation
	protected static Icon rotateIcon =
			ImageManager.getInstance().loadUiIcon(Environment.MDI_RESOURCE_PATH + "images/svg/rotate.svg", 16);

	/**
	 * The path is used by some items (not point or line based items). NOTE: it is
	 * world coordinate based.
	 */
	protected Path2D.Double _path;

	/**
	 * The line is used by line based items
	 */
	protected Line2D.Double _line;

	/**
	 * Optionaly secondary points (such as internal points)
	 */
	protected Point2D.Double _secondaryPoints[];

	/**
	 * The focus of the item--often it is the center.
	 */
	protected Point2D.Double _focus;

	/**
	 * What layer the item is on.
	 */
	protected Layer _layer;

	/**
	 * The style for this item.
	 */
	protected IStyled _style = new Styled();

	/**
	 * Resize policy (assuming this is resizable)
	 */
	protected ResizePolicy _resizePolicy = ResizePolicy.NORMAL;

	/**
	 * Visibility flag for this item.
	 */
	protected boolean _visible = true;

	/**
	 * Controls whether the item can be dragged.
	 */
	protected boolean _draggable = false;

	/**
	 * Controls whether the item can be rotated.
	 */
	protected boolean _rotatable = false;

	/**
	 * Controls whether the item responds to a righjt click.
	 */
	protected boolean _rightClickable = false;

	/**
	 * Controls whether the item can be selected.
	 */
	protected boolean _selectable = true;

	/**
	 * Controls whether the item can be connected to other items.
	 */
	protected boolean connectable = false;

	/**
	 * Controls whether the item responds to a double click.
	 */
	protected boolean doubleClickable = false;

	/**
	 * Controls whether the item is locked-which takes precedence over other flags.
	 * A locked item cannot be dragged, rotated, resized, or deleted--regardless of
	 * the values of those flags.
	 */
	protected boolean _locked = true;

	/**
	 * Controls whether the item can be resized.
	 */
	private boolean _resizable = false;

	/**
	 * Controls whether the item can be deleted.
	 */
	protected boolean _deletable = false;

	/**
	 * Flag indicating whether the item is selected.
	 */
	protected boolean _selected = false;

	/**
	 * Flag indicating whether the item is enabled. Objects that are not enabled are
	 * inert and might be drawn "ghosted."
	 */
	protected boolean _enabled = true;

	/**
	 * Flag indicating whether the item is dirty. If the item is dirty, the next
	 * time it is drawn it must be drawn from scratch. Many items are drawn from
	 * scratch anyway--but some complicated items may be caching data for quick
	 * redraw.
	 */
	protected boolean _dirty = true;

	// polygon from last draw. Some items will not use this
	protected Polygon _lastDrawnPolygon;

	/**
	 * Used to modify items (drag, resize, rotate)
	 */
	protected ItemModification _modification;

	// reference rotation angle in degrees.
	private double _azimuth = 0.0;

	/**
	 * The name of the item.
	 */
	protected String _displayName = "no name";

	// used for select points
	private static final Color _selectFill = Color.white;

	// used for select points
	private static final Color _rotateFill = X11Colors.getX11Color("yellow", 64);

	// used for select points
	private static final Color _selectLine = Color.black;

	// the popup menu for this item
	protected JPopupMenu _popupMenu;

	/**
	 * Create an item on a specific layer.
	 *
	 * @param layer the layer it is on.
	 */
	public AItem(Layer layer) {
		_layer = layer;
		_layer.add(this);

		_layer.getContainer().getFeedbackControl().addFeedbackProvider(this);
	}
	
	/**
	 * Draw the item.
	 *
	 * @param g         the graphics context.
	 * @param container the graphical container being rendered.
	 */
	@Override
	public void draw(Graphics2D g, IContainer container) {

		if (_visible) {

			if (shouldDraw(g, container)) {

				// special clip?
				Shape oldClip = g.getClip();
				BaseView bview = container.getView();

				if (bview != null) {
					Shape clip = bview.getSpecialClip();
					if (clip != null) {
						g.setClip(clip);
					}
				}

				Stroke oldStroke = g.getStroke();
				drawItem(g, container);
				setDirty(false);
				g.setStroke(oldStroke);

				g.setClip(oldClip);
			}

			drawSelections(g, container);
		}
	}

	/**
	 * Helper to get the view this item is on.
	 *
	 * @return the view this item is on.
	 */
	public BaseView getView() {
		IContainer container = getContainer();
		if (container != null) {
			return container.getView();
		}
		return null;
	}


	/**
	 * Draws any selection or rotation rectangles
	 *
	 * @param g         the graphics context.
	 * @param container the graphical container being rendered.
	 */
	public void drawSelections(Graphics2D g, IContainer container) {

		if (!isSelected()) {
			return;
		}

		Point selectPoints[] = getSelectionPoints(container);
		if (selectPoints != null) {
			for (Point p : selectPoints) {
				g.setColor(_selectFill);
				g.fillOval(p.x - SPSIZE2, p.y - SPSIZE2, SPSIZE, SPSIZE);
				g.setColor(_selectLine);
				g.drawOval(p.x - SPSIZE2, p.y - SPSIZE2, SPSIZE, SPSIZE);
			}
		}

		if (isRotatable()) {
			Point rp = getRotatePoint(container);

			if ((rp != null) && (rotateIcon != null)) {
				g.setColor(_rotateFill);
				g.fillOval(rp.x - RPSIZE2, rp.y - RPSIZE2, RPSIZE, RPSIZE);
				rotateIcon.paintIcon(container.getComponent(), g, rp.x - rotateIcon.getIconHeight() / 2,
						rp.y - rotateIcon.getIconHeight() / 2);
			}
		}

		// draw the focus
		focusFill(g, container);
	}

	// draw the focus for selected items
	protected void focusFill(Graphics g, IContainer container) {
		Point pp = getFocusPoint(container);
		if (pp != null) {
			g.setColor(_FOCUSFILL);
			g.fillRect(pp.x - 3, pp.y - 3, 6, 6);
		}
	}

	/**
	 * Check whether this item is marked as visible.
	 *
	 * @return <code>true</code> is this item is marked as visible.
	 */
	@Override
	public boolean isVisible() {
		return _visible;
	}

	/**
	 * Sets the visibility flag.
	 *
	 * @param visible the new value of the flag
	 */
	@Override
	public void setVisible(boolean visible) {
		_visible = visible;
	}

	/**
	 * Check whether the item can be dragged.
	 *
	 * @return <code>true</code> if the item can be dragged.
	 */
	public boolean isDraggable() {
		if (_locked) {
			return false;
		}

		return _draggable;
	}

	/**
	 * Set whether the item can be dragged.
	 *
	 * @param draggable if <code>true</code>, the item can be dragged.
	 */
	public void setDraggable(boolean draggable) {
		_draggable = draggable;
	}

	/**
	 * Check whether the item can be deleted.
	 *
	 * @return <code>true</code> if the item can be deleted.
	 */
	public boolean isDeletable() {
		if (_locked) {
			return false;
		}
		return _deletable;
	}

	/**
	 * Set whether the item can be deleted.
	 *
	 * @param deletable if <code>true</code>, the item can be deleted.
	 */
	public void setDeletable(boolean deletable) {
		_deletable = deletable;
	}

	/**
	 * Check whether the item can be resized.
	 *
	 * @return <code>true</code> if the item can be resized.
	 */
	public boolean isResizable() {
		if (_locked) {
			return false;
		}
		return _resizable;
	}

	/**
	 * Set whether the item can be resized.
	 *
	 * @param resizable if <code>true</code>, the item can be resized.
	 */
	public void setResizable(boolean resizable) {
		_resizable = resizable;
	}

	/**
	 * Check whether the item can be rotated.
	 *
	 * @return <code>true</code> if the item can be rotated.
	 */
	public boolean isRotatable() {
		if (_locked) {
			return false;
		}
		return _rotatable;
	}

	/**
	 * /** Set whether the item can be rotated.
	 *
	 * @param rotatable if <code>true</code>, the item can be rotated.
	 */
	public void setRotatable(boolean rotatable) {
		_rotatable = rotatable;
	}

	/**
	 * Check whether the item can be connected to other items.
	 *
	 * @return <code>true</code> if the item can be connected to other items.
	 */
	public boolean isConnectable() {
		return connectable;
	}

	/**
	 * Set whether the item can be connected to other items.
	 *
	 * @param connectable if <code>true</code>, the item can be connected to other
	 *                    items.
	 */
	public void setConnectable(boolean connectable) {
		this.connectable = connectable;
	}

	/**
	 * Check whether the item responds to a double click.
	 *
	 * @return <code>true</code> if the item responds to a double click.
	 */
	public boolean isDoubleClickable() {
		return doubleClickable;
	}

	/**
	 * Set whether the item responds to a double click.
	 *
	 * @param doubleClickable if <code>true</code>, the item responds to a double
	 *                        click.
	 */
	public void setDoubleClickable(boolean doubleClickable) {
		this.doubleClickable = doubleClickable;
	}

	/**
	 * Check whether the item can be selected.
	 *
	 * @return <code>true</code> if the item can be selected.
	 */
	public boolean isSelectable() {
		return _selectable;
	}

	/**
	 * Set whether the item can be selected.
	 *
	 * @param selectable if <code>true</code>, the item can be selected.
	 */
	public void setSelectable(boolean selectable) {
		this._selectable = selectable;
	}

	/**
	 * Check whether the item is locked, which takes precedence over other flags. A
	 * locked item cannot be dragged, rotated, resized, or deleted--regardless of
	 * the values of those flags.
	 *
	 * @return <code>true</code> if the item is locked.
	 */
	public boolean isLocked() {
		return _locked;
	}

	/**
	 * Set whether the item is locked.
	 *
	 * @param locked if <code>true</code>, the item is set to locked.
	 */
	public void setLocked(boolean locked) {
		_locked = locked;
	}

	/**
	 * Check whether this item is marked as selected.
	 *
	 * @return <code>true</code> is this item is marked as selected.
	 */
	public boolean isSelected() {
		return _selected;
	}

	/**
	 * Sets whether this item is marked as selected.
	 *
	 * @param selected the new value of the flag.
	 */
	public void setSelected(boolean selected) {
		_selected = selected;
		_layer.getContainer().refresh();
	}

	/**
	 * Check whether this item is marked as enabled. If the item is enabled, it can
	 * be selected, otherwise it is inert.
	 *
	 * @return <code>true</code> is this item is marked as enabled.
	 */
	@Override
	public boolean isEnabled() {
		return _enabled;
	}

	/**
	 * Sets whether this item is marked as enabled.
	 *
	 * @param enabled the new value of the flag. If the item is enabled, it can be
	 *                selected, otherwise it is inert.
	 */
	@Override
	public void setEnabled(boolean enabled) {
		_enabled = enabled;
	}

	/**
	 * Check whether this item is marked as dirty. If the item is dirty, the next
	 * time it is drawn it must be drawn from scratch. Many items are drawn from
	 * scratch anyway--but some complicated items may be caching data for quick
	 * redraw.
	 *
	 * @return <code>true</code> is this item is marked as dirty.
	 */
	public boolean isDirty() {
		return _dirty;
	}

	/**
	 * Sets whether this item is marked as dirty.
	 *
	 * @param dirty the new value of the flag. If the item is dirty, the next time
	 *              it is drawn it must be drawn from scratch. Many items are drawn
	 *              from scratch anyway--but some complicated items may be caching
	 *              data for quick redraw.
	 */
	@Override
	public void setDirty(boolean dirty) {
		_dirty = dirty;
		if (dirty) {
			_lastDrawnPolygon = null;
		}
	}

	/**
	 * Convenience routine to see if this item should be ignored.
	 *
	 * @return <code>true</code> if the item should be ignored in terms of dragging,
	 *         ro
	 */
	public boolean isTrackable() {
		if (_locked || !_enabled) {
			return false;
		}

		return _rotatable || _draggable || _resizable;
	}

	/**
	 * Custom drawer for the item.
	 *
	 * @param g         the graphics context.
	 * @param container the graphical container being rendered.
	 */
	public abstract void drawItem(Graphics g, IContainer container);

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
	public abstract boolean shouldDraw(Graphics g, IContainer container);

	/**
	 * Check whether the (rendered) item contains the given screen point.
	 *
	 * @param container   the graphical container rendering the item.
	 * @param screenPoint a pixel location.
	 * @return <code>true</code> if the item, as rendered on the given container,
	 *         contains the given screen point.
	 */
	public boolean contains(IContainer container, Point screenPoint) {
		// do we have a cached polygon?

		if (_lastDrawnPolygon != null) {
			if (_lastDrawnPolygon.contains(screenPoint)) {
				return true;
			}
		} else { // try simple bounds
			Rectangle r = getBounds(container);
			if ((r != null) && r.contains(screenPoint)) {
				return true;
			}
		}
		return inASelectRect(container, screenPoint);
	}

	// Are we in any select rect?
	protected boolean inASelectRect(IContainer container, Point screenPoint) {

		// still have to consider rotate and select points
		// last hope
		if ((inSelectPoint(container, screenPoint, false) >= 0) || inRotatePoint(container, screenPoint)) {
			return true;
		}

		return false;
	}

	public IStyled getStyleSafe() {
		if (_style == null) {
			_style = new Styled();
		}
		return _style;
	}

	/**
	 * Get the drawing style for this item. Through this object you can set the fill
	 * color, line style, etc.
	 *
	 * @return the style for this item.
	 */
	public IStyled getStyle() {
		return _style;
	}

	/**
	 * Set the drawing style for this item.
	 *
	 * @param style the style to set.
	 */
	public void setStyle(IStyled style) {
		this._style = style;
	}

	/**
	 * Translate the item in world coordinates.
	 *
	 * @param dx the delta x in world coordinates.
	 * @param dy the delta y in world coordinates.
	 */
	public abstract void translateWorld(double dx, double dy);

	/**
	 * Translate the item in local (pixel) coordinates.
	 *
	 * @param dx the delta x in local coordinates.
	 * @param dy the delta y in local coordinates.
	 */
	public void translateLocal(int dx, int dy) {

		if (dx == 0 && dy == 0) {
			return;
		}

		IContainer container = getContainer();
		if (container == null) {
			return;
		}
		// convert local deltas to world deltas
		Point2D.Double w0 = new Point2D.Double();
		Point2D.Double w1 = new Point2D.Double();
		container.localToWorld(new Point(0, 0), w0);
		container.localToWorld(new Point(dx, dy), w1);
		double dxWorld = w1.x - w0.x;
		double dyWorld = w1.y - w0.y;
		translateWorld(dxWorld, dyWorld);
	}

	/**
	 * Set the name of the item.
	 *
	 * @param name the name of the item.
	 */
	public void setDisplayName(String name) {
		_displayName = name;
	}

	/**
	 * Return the name of the item.
	 *
	 * @return the name of the item.
	 */
	public String getDisplayName() {
		return _displayName;
	}

	/**
	 * Equality check.
	 *
	 * @return <code>true</code> if objects are equal.
	 */
	@Override
	public boolean equals(Object o) {

		if ((o != null) && (o instanceof AItem)) {
			return (this == o);
		}
		return false;
	}

	/**
	 * Add any appropriate feedback strings panel. Default implementation returns
	 * the item's name.
	 *
	 * @param container       the Base container.
	 * @param pp              the mouse location.
	 * @param wp              the corresponding world point.
	 * @param feedbackStrings the List of feedback strings to add to.
	 */
	@Override
	public void getFeedbackStrings(IContainer container, Point pp, Point2D.Double wp, List<String> feedbackStrings) {
	}

	/**
	 * Check if the given rectangle completely encloses the item. This is used for
	 * rubber band selection. The default implementation should work for rectangular
	 * objects. More complicated objects (polygons) should overwrite.
	 *
	 * @param container the rendering container.
	 * @param r         the bounds
	 * @return <code>true</code> if the bounds (r) completely enclose the item.
	 */
	public boolean enclosed(IContainer container, Rectangle r) {

		if (_lastDrawnPolygon != null) {
			return r.contains(_lastDrawnPolygon.getBounds());
		}

		Rectangle myBounds = getBounds(container);
		if (myBounds == null) {
			return false;
		}
		return r.contains(myBounds);
	}

	/**
	 * get the bounding rectangle of the item.
	 *
	 * @param container the container being rendered
	 * @return the box around the item.
	 */
	public Rectangle getBounds(IContainer container) {
		return null;
	}

	/**
	 * Get the world bounding rectangle of the item.
	 *
	 * @return the world box containing the item.
	 */
	public abstract Rectangle2D.Double getWorldBounds();

	/**
	 * A modification such as a drag, resize or rotate has begun.
	 */
	public void startModification() {

		if (_modification == null) {
			return;
		}

		IContainer container = _modification.getContainer();
		Point smp = _modification.getStartMousePoint();

		// Defensive default
		_modification.setType(ModificationType.DRAG);

		// 1) Rotation has highest priority
		if (inRotatePoint(container, smp)) {
			_modification.setType(ModificationType.ROTATE);
			return;
		}

		// 2) Resize (only if resizable and selected)
		int index = inSelectPoint(container, smp, true);
		if (index >= 0) {
			_modification.setSelectIndex(index);
			_modification.setType(ModificationType.RESIZE);
			return;
		}
		// 3) Otherwise it's a drag
		_modification.setType(ModificationType.DRAG);
	}

	/**
	 * A modification such as a drag, resize or rotate is continuing.
	 */
	public abstract void modify();

	/**
	 * A modification such as a drag, resize or rotate has ended.
	 */
	public void stopModification() {
		if (_modification == null) {
			return;
		}
		switch (_modification.getType()) {
		case DRAG:
			_layer.notifyItemChangeListeners(this, ItemChangeType.MOVED);
			break;

		case ROTATE:
			_layer.notifyItemChangeListeners(this, ItemChangeType.ROTATED);
			break;

		case RESIZE:
			_layer.notifyItemChangeListeners(this, ItemChangeType.RESIZED);
			break;
		}
		_modification = null;
	}

	/**
	 * This gets the focus of the item. Fot pointlike items it will be the location.
	 * For polygonal items it might be the centroid.
	 *
	 * @return the focus of the item.
	 */
	public Point2D.Double getFocus() {
		return _focus;
	}

	/**
	 * This should be overridden by items to do something sensible. They should set
	 * their focus point which for simple items may their location and for
	 * complicated items may be where their new centroid should be.
	 *
	 * @param wp the new focus.
	 */
	public void setFocus(Point2D.Double wp) {
		_focus = wp;
	}

	/**
	 * This gets the screen (pixel) version focus of the item. Fot pointlike items
	 * it will be the location. For polygobal items it might be the centroid.
	 *
	 * @return the focus of the item.
	 */
	public Point getFocusPoint(IContainer container) {
		Point2D.Double wp = getFocus();
		if (wp == null) {
			return null;
		}
		Point pp = new Point();
		container.worldToLocal(pp, wp);
		return pp;
	}

	/**
	 * Get the rotation point
	 *
	 * @param container the container bing rendered
	 * @return the rotation point where rotations are initiated
	 */
	public Point getRotatePoint(IContainer container) {
		return null;
	}

	/**
	 * Get the last drawn polygon.
	 *
	 * @return the last drawn polygon.
	 */
	public Polygon getLastDrawnPolygon() {
		return _lastDrawnPolygon;
	}

	/**
	 * Obtain the selection points used to indicate this item is selected.
	 *
	 * @return the selection points used to indicate this item is selected.
	 */
	public Point[] getSelectionPoints(IContainer container) {
		// if the item cached a last drawn polygon lets use it--it better be
		// right!

		if ((_lastDrawnPolygon != null) && (_lastDrawnPolygon.npoints > 1)) {
			Point pp[] = new Point[_lastDrawnPolygon.npoints];

			for (int i = 0; i < _lastDrawnPolygon.npoints; i++) {
				pp[i] = new Point(_lastDrawnPolygon.xpoints[i], _lastDrawnPolygon.ypoints[i]);
			}
			return pp;
		}

		// else just use the bounds
		Rectangle r = getBounds(container);
		if (r == null) {
			return null;
		} else {
			Point p[] = new Point[4];
			int bottom = r.y + r.height;
			int right = r.x + r.width;
			p[0] = new Point(r.x, r.y);
			p[1] = new Point(r.x, bottom);
			p[2] = new Point(right, bottom);
			p[3] = new Point(right, r.y);
			return p;
		}
	}

	/**
	 * Get the laywer this item is on.
	 *
	 * @return the layer this item is on.
	 */
	public Layer getLayer() {
		return _layer;
	}

	/**
	 * Get the modification record which will not be null while the item is being
	 * modified.
	 *
	 * @return the itemModification record.
	 */
	public ItemModification getItemModification() {
		return _modification;
	}

	/**
	 * Check whether the item will respond to a right click.
	 *
	 * @return the rightClickable flag.
	 */
	public boolean isRightClickable() {
		// turn off the list check--allow even disabled lists
		// to process right clicks
		return _rightClickable;
	}

	/**
	 * Set whether the item will respond to a right click.
	 *
	 * @param rightClickable the new rightClickable flag to set
	 */
	public void setRightClickable(boolean rightClickable) {
		_rightClickable = rightClickable;
	}

	/**
	 * Called when the item was double clicked. The default implementation is to do
	 * nothing.
	 *
	 * @param mouseEvent the causal event.
	 */
	public void doubleClicked(MouseEvent mouseEvent) {
	}

	/**
	 * Is the given point int a select rect?
	 *
	 * @param container   the container being rendered
	 * @param screenPoint the point in question
	 * @return the index of the select point, or -1 if not in one
	 */
	public int inSelectPoint(IContainer container, Point screenPoint, boolean checkResizable) {
		if ((checkResizable && !isResizable()) || !isSelected()) {
			return -1;
		}

		Point pp[] = getSelectionPoints(container);
		if (pp == null) {
			return -1;
		}

		int index = 0;
		for (Point lp : pp) {
			Rectangle r = new Rectangle(lp.x - SPSIZE2, lp.y - SPSIZE2, SPSIZE, SPSIZE);
			if (r.contains(screenPoint)) {
				return index;
			}
			index++;
		}

		return -1;
	}

	/**
	 * See if we are in the rotate point
	 *
	 * @param container   the container being rendered
	 * @param screenPoint the point in question
	 * @return <code>true<code> if we are in the rotate rect.
	 */
	public boolean inRotatePoint(IContainer container, Point screenPoint) {
		if (!isRotatable() || !isSelected()) {
			return false;
		}
		Point p = getRotatePoint(container);

		if (p == null) {
			return false;
		}
		Rectangle r = new Rectangle(p.x - RPSIZE2, p.y - RPSIZE2, RPSIZE, RPSIZE);

		return r.contains(screenPoint);
	}

	/**
	 * Called only when a modification starts.
	 *
	 * @param itemModification the itemModification to set
	 */
	public void setModification(ItemModification itemModification) {
		_modification = itemModification;
	}

	/**
	 * Get this item's popup menu
	 *
	 * @return the item's popup menu
	 */
	public JPopupMenu getPopupMenu() {
		if (_popupMenu == null) {
			createPopupMenu();
		}
		return _popupMenu;
	}

	/**
	 * Create the basic popup menu for this item.
	 *
	 * @return the item's basic popup menu
	 */
	protected JPopupMenu createPopupMenu() {
		_popupMenu = new JPopupMenu();
		_popupMenu.add(ItemOrderingMenu.getItemOrderingMenu(this, true));

		final JCheckBoxMenuItem cbitem = new JCheckBoxMenuItem("Locked", isLocked());

		final AItem titem = this;
		ItemListener il = new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				titem.setLocked(cbitem.isSelected());
				if (titem.isLocked()) {
					titem.setSelected(false);

					IContainer cont = titem.getLayer().getContainer();
					if (cont != null) {
						titem.getContainer().refresh();
					}
				}
			}
		};
		cbitem.addItemListener(il);
		_popupMenu.add(cbitem);
		return _popupMenu;
	}

	/**
	 * Prepare and show the popup menu at the given point.
	 *
	 * @param pp the point to show the popup menu at.
	 */
	public void prepareForPopup(Point pp) {
		Objects.requireNonNull(pp, "Popup location cannot be null");
		IContainer container = getContainer();
		if (container == null) {
			return;
		}
		JPopupMenu menu = getPopupMenu();
		if (menu == null) {
			return;
		}
		menu.show(container.getComponent(), pp.x, pp.y);
	}


	/**
	 * Get the reference rotation angle in degrees.
	 *
	 * @return the azimuth
	 */
	public double getAzimuth() {
		return _azimuth;
	}

	/**
	 * Set the reference rotation angle in degrees.
	 *
	 * @param azimuth the azimuth to set in degrees
	 */
	public void setAzimuth(double azimuth) {
		_azimuth = azimuth;
		while (_azimuth > 180.0) {
			_azimuth -= 360.0;
		}
		while (_azimuth < -180.0) {
			_azimuth += 360.0;
		}
	}

	/**
	 * Called when the drawable is about to be removed from a layer.
	 */
	@Override
	public void prepareForRemoval() {

		_focus = null;
		_lastDrawnPolygon = null;
		_layer = null;
		_path = null;
		_secondaryPoints = null;
		_style = null;

	}

	/**
	 * @return the path
	 */
	public Path2D.Double getPath() {
		return _path;
	}

	/**
	 * @return the line
	 */
	public Line2D.Double getLine() {
		return _line;
	}

	/**
	 * Get the container this item lives on
	 *
	 * @return the container this item lives on.
	 */
	public IContainer getContainer() {
		return getLayer().getContainer();
	}

	/**
	 * @return the secondary points
	 */
	public Point2D.Double[] getSecondaryPoints() {
		return _secondaryPoints;
	}

	/**
	 * @return the resizePolicy
	 */
	public ResizePolicy getResizePolicy() {
		return _resizePolicy;
	}

	/**
	 * @param resizePolicy the resizePolicy to set
	 */
	public void setResizePolicy(ResizePolicy resizePolicy) {
		this._resizePolicy = resizePolicy;
	}

	protected void updateFocus() {
		// default implementation does nothing
	}

	public void geometryChanged() {
		updateFocus();
		setDirty(true);
	}

}
