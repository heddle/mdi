package edu.cnu.mdi.container;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.JComponent;

import edu.cnu.mdi.feedback.FeedbackControl;
import edu.cnu.mdi.feedback.FeedbackPane;
import edu.cnu.mdi.graphics.GraphicsUtils;
import edu.cnu.mdi.graphics.drawable.DrawableChangeType;
import edu.cnu.mdi.graphics.drawable.DrawableList;
import edu.cnu.mdi.graphics.drawable.IDrawable;
import edu.cnu.mdi.graphics.drawable.IDrawableListener;
import edu.cnu.mdi.graphics.toolbar.BaseToolBar;
import edu.cnu.mdi.graphics.toolbar.ToolBarToggleButton;
import edu.cnu.mdi.graphics.world.WorldPolygon;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.ItemList;
import edu.cnu.mdi.item.YouAreHereItem;
import edu.cnu.mdi.log.Log;
import edu.cnu.mdi.util.Point2DSupport;
import edu.cnu.mdi.view.BaseView;
import edu.cnu.mdi.view.ViewRecenterer;


/**
 * This the primary basic container. It contains a list of ItemList objects
 * list of items.)
 *
 * @author heddle
 *
 */

@SuppressWarnings("serial")
public class BaseContainer extends JComponent
		implements IContainer, MouseListener, MouseMotionListener, MouseWheelListener, IDrawableListener {
	
    
	/**
	 * A collection of item list. This is the container's model.
	 */
	protected DrawableList _itemLists = new DrawableList("ItemLists");

	/**
	 * Keeps track of current mouse position
	 */
	private Point _currentMousePoint;

	/**
	 * Each container may or may not have a tool bar.
	 */
	protected BaseToolBar _toolBar;

	/**
	 * The optional feedback pane.
	 */
	protected FeedbackPane _feedbackPane;

	// location of last mouse event
	protected MouseEvent _lastLocationMouseEvent;

	/**
	 * This optional drawable is called after the lists are drawn.
	 */
	protected IDrawable _afterDraw;

	/**
	 * This optional drawable is called before the lists are drawn.
	 */
	protected IDrawable _beforeDraw;

	/**
	 * Option drawer for magnification window rather than just simple magnification
	 */
	protected IDrawable _magDraw;

	/**
	 * The view that holds this container (might be null for viewless container).
	 */
	protected BaseView _view;

	// used for things like a YouAreHereItem reference point
	private ItemList _glassList;

	/**
	 * The world coordinate system,
	 */
	protected Rectangle2D.Double _worldSystem;

	/**
	 * Original, default world system.
	 */
	protected Rectangle2D.Double _defaultWorldSystem;

	/**
	 * Previous world system, for undoing the last zoom.
	 */
	protected Rectangle2D.Double _previousWorldSystem;

	/**
	 * The annotation list. Every container has one.
	 */
	protected ItemList _annotationList;

	// A map of lists added by users.
	private Hashtable<String, ItemList> _userItemLists = new Hashtable<>(47);

	/**
	 * Controls the feedback for the container. You can add and remove feedback
	 * providers to this object.
	 */
	protected FeedbackControl _feedbackControl;

	/**
	 * Optional anchor item.
	 */
	protected YouAreHereItem _youAreHereItem;

	// for world to local transformations (and vice versa)

	private int _lMargin = 0;
	private int _tMargin = 0;
	private int _rMargin = 0;
	private int _bMargin = 0;
	protected AffineTransform localToWorld;
	protected AffineTransform worldToLocal;

	/**
	 * Constructor for a container that does not live in a view. It might live on a
	 * panel, for example
	 *
	 * @param worldSystem the default world system.
	 */
	public BaseContainer(Rectangle2D.Double worldSystem) {
		this(null, worldSystem);
	}

	/**
	 * Constructor
	 *
	 * @param view        Every container lives on one view. This is the view, which
	 *                    is an internal frame, that owns this container.
	 * @param worldSystem the default world system.
	 */
	public BaseContainer(BaseView view, Rectangle2D.Double worldSystem) {
		_view = view;
		_feedbackControl = new FeedbackControl(this);

		resetWorldSystem(worldSystem);

		// create the annotation list. (not added to userlist hash)
		_annotationList = new ItemList(this, "Annotations");
		addItemList(_annotationList);

		ComponentAdapter componentAdapter = new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent ce) {
				setDirty(true);
				repaint();
				setAffineTransforms();
			}
		};

		addComponentListener(componentAdapter);
		addMouseListener(this);
		addMouseMotionListener(this);

	}
	
	/**
	 * Reset the world system to a new value.
	 * Resets the default and previous world systems as well.
	 * @param worldSystem the new world system.
	 */
	public void resetWorldSystem(Rectangle2D.Double worldSystem) {
		_worldSystem = worldSystem;
		_defaultWorldSystem = copy(worldSystem);
		_previousWorldSystem = copy(worldSystem);
		setDirty(true);
	}

	/**
	 * Share the model of another view. Note, this is not a copy, either view can
	 * modify the items. This is primarily used for magnification windows.
	 *
	 * @param sContainer the source container
	 */
	public void shareModel(BaseContainer sContainer) {
		_itemLists = sContainer._itemLists;
		_afterDraw = sContainer._afterDraw;
		_beforeDraw = sContainer._beforeDraw;
		_userItemLists = sContainer._userItemLists;
		setBackground(sContainer.getBackground());
		setForeground(sContainer.getForeground());
	}

	public void clipBounds(Graphics g) {
		Rectangle b = getBounds();
		g.setClip(0, 0, b.width, b.height);
	}

	
	/**
	 * Override the paint command. Draw all the lists.
	 *
	 * @param g the graphics context.
	 */
	@Override
	public void paintComponent(Graphics g) {
		
		if ((_view != null) && !_view.isViewVisible()) {
			return;
		}
		
	    super.paintComponent(g);	

		clipBounds(g);

		Rectangle b = getBounds();

		setAffineTransforms();
		if (localToWorld == null) {
			return;
		}

		// normal drawing
		g.setColor(getBackground());
		g.fillRect(0, 0, b.width, b.height);

		// any before lists drawing?
		if (_beforeDraw != null) {
			_beforeDraw.draw(g, this);
		}

		// draw the lists
		if (_itemLists != null) {
			_itemLists.draw(g, this);
		}

		// any post lists drawing?
		if (_afterDraw != null) {
			_afterDraw.draw(g, this);
		}

		setDirty(false);

	}

	/**
	 * {@inheritDoc}
     */
	@Override
	public ItemList addItemList(String name) {
		if (name == null) {
			return null;
		}
		ItemList itemList = _userItemLists.get(name);
		if (itemList != null) {
			Log.getInstance().warning("Asked to add an ItemList: " + name + " which already exists.");
		} else {
			itemList = new ItemList(this, name);
			_userItemLists.put(name, itemList);
			addItemList(itemList);
		}
		return itemList;
	}

	/**
	 * {@inheritDoc}
     */
	@Override
	public void addItemList(ItemList itemList) {

		_itemLists.add(itemList);
		if (itemList != _annotationList) {
			_itemLists.sendToFront(_annotationList);
		}
		itemList.addDrawableListener(this);
	}

	/**
	 * {@inheritDoc}
     */
	@Override
	public ItemList getAnnotationList() {
		return _annotationList;
	}

	/**
	 * {@inheritDoc}
     */
	@Override
	public ItemList getItemList(String name) {
		ItemList itemList = _userItemLists.get(name);
		if (itemList == null) {
			Log.getInstance().warning("Requested nonexistent item list: " + name);
		}
		return itemList;
	}

	/**
	 * {@inheritDoc}
     */
	@Override
	public void removeItemList(ItemList itemList) {
		if (itemList != null) {
			_itemLists.remove(itemList);
			// also remove from hash
			if (_userItemLists.contains(itemList)) {
				_userItemLists.remove(itemList.getName());
			}
		}
	}


	/**
	 * {@inheritDoc}
     */
	@Override
	public void localToWorld(Point pp, Point2D.Double wp) {
		if (localToWorld != null) {
			localToWorld.transform(pp, wp);
		}
	}

	/**
	 * {@inheritDoc}
     */
	@Override
	public void worldToLocal(Point pp, Point2D.Double wp) {
		if (worldToLocal != null) {
			try {
				worldToLocal.transform(wp, pp);
			} catch (NullPointerException npe) {
				npe.printStackTrace();
			}
		}
	}

	/**
	 * {@inheritDoc}
     */
	@Override
	public void worldToLocal(Rectangle r, Rectangle.Double wr) {
		// New version to accommodate world with x decreasing right
		Point2D.Double wp0 = new Point2D.Double(wr.getMinX(), wr.getMinY());
		Point2D.Double wp1 = new Point2D.Double(wr.getMaxX(), wr.getMaxY());
		Point p0 = new Point();
		Point p1 = new Point();
		worldToLocal(p0, wp0);
		worldToLocal(p1, wp1);

		int x = Math.min(p0.x, p1.x);
		int y = Math.min(p0.y, p1.y);
		int w = Math.abs(p1.x - p0.x);
		int h = Math.abs(p1.y - p0.y);
		r.setBounds(x, y, w, h);
	}

	/**
	 * {@inheritDoc}
     */
	@Override
	public void localToWorld(Rectangle r, Rectangle.Double wr) {
		Point p0 = new Point(r.x, r.y);
		Point p1 = new Point(r.x + r.width, r.y + r.height);
		Point2D.Double wp0 = new Point2D.Double();
		Point2D.Double wp1 = new Point2D.Double();
		localToWorld(p0, wp0);
		localToWorld(p1, wp1);

		// New version to accommodate world with x decreasing right
		double x = wp0.x;
		double y = wp1.y;
		double w = wp1.x - wp0.x;
		double h = wp0.y - wp1.y;
		wr.setFrame(x, y, w, h);

	}

	/**
	 * {@inheritDoc}
     */
	@Override
	public void worldToLocal(Point pp, double wx, double wy) {
		worldToLocal(pp, new Point2D.Double(wx, wy));
	}

	/**
	 * {@inheritDoc}
     */
	@Override
	public void pan(int dh, int dv) {

		Rectangle r = getBounds();
		int xc = r.width / 2;
		int yc = r.height / 2;

		xc -= dh;
		yc -= dv;

		Point p = new Point(xc, yc);
		recenter(p);
	}

	/**
	 * {@inheritDoc}
     */
    @Override
    public void recenter(Point pp) {
    	
		if ((_view != null) && (_view instanceof ViewRecenterer)) {
			((ViewRecenterer) _view).recenterView(pp);
			return;
		}
        Point2D.Double wp = new Point2D.Double();
        localToWorld(pp, wp);
        recenter(_worldSystem, wp);
        setDirty(true);
        refresh();
    }

	/**
	 * {@inheritDoc}
     */
	private void recenter(Rectangle2D.Double wr, Point2D.Double newCenter) {
		wr.x = newCenter.x - wr.width / 2.0;
		wr.y = newCenter.y - wr.height / 2.0;
	}

	/**
	 * {@inheritDoc}
     */
	@Override
	public void prepareToZoom() {
		_previousWorldSystem = copy(_worldSystem);
	}

	/**
	 * {@inheritDoc}
     */
	@Override
	public void restoreDefaultWorld() {
		_worldSystem = copy(_defaultWorldSystem);
		setDirty(true);
		refresh();
	}

	/**
	 * {@inheritDoc}
     */
	@Override
	public void refresh() {
		if ((_view != null) && !_view.isViewVisible()) {
			return;
		}

		repaint();

	}

	/**
	 * {@inheritDoc}
     */
	@Override
	public void scale(double scaleFactor) {
		prepareToZoom();
		scale(_worldSystem, scaleFactor);
		setDirty(true);
		refresh();
	}

	/**
	 * {@inheritDoc}
     */
	private void scale(Rectangle2D.Double wr, double scale) {
		double xc = wr.getCenterX();
		double yc = wr.getCenterY();
		wr.width *= scale;
		wr.height *= scale;
		wr.x = xc - wr.width / 2.0;
		wr.y = yc - wr.height / 2.0;
	}

	/**
	 * {@inheritDoc}
     */
	@Override
	public void undoLastZoom() {
		Rectangle2D.Double temp = _worldSystem;
		_worldSystem = copy(_previousWorldSystem);
		_previousWorldSystem = temp;
		setDirty(true);
		refresh();
	}

	/**
	 * {@inheritDoc}
     */
	@Override
	public void rubberBanded(Rectangle b) {
		// if too small, don't zoom
		if ((b.width < 10) || (b.height < 10)) {
			return;
		}
		localToWorld(b, _worldSystem);
		setDirty(true);
		refresh();
	}

	/**
	 * Convenience method for setting the dirty flag for all items on all item lists.
	 * Things that make a container dirty:
	 * <ol>
	 * <li>container was resized
	 * <li>zooming
	 * <li>undo zooming
	 * <li>scaling
	 * <li>restoring default world
	 * <li>panning
	 * <li>recenter
	 * </ol>
	 *
	 * @param dirty the new value of the dirty flag.
	 */
	@Override
	public void setDirty(boolean dirty) {

		setAffineTransforms();

		if (_itemLists != null) {
			for (IDrawable list : _itemLists) {
				list.setDirty(dirty);
			}
		}
	}

	/**
	 * Find an item, if any, at the point.
	 *
	 * @param pp The pixel point in question.
	 * @return the topmost satisfying item, or null.
	 */
	@Override
	public AItem getItemAtPoint(Point pp) {
		if (_itemLists == null) {
			return null;
		}

		for (int i = _itemLists.size() - 1; i >= 0; i--) {
			ItemList itemList = ((ItemList) _itemLists.get(i));
			AItem item = itemList.getItemAtPoint(this, pp);
			if (item != null) {
				return item;
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
     */
	@Override
	public ArrayList<AItem> getEnclosedItems(Rectangle rect) {

		if (rect == null) {
			return null;
		}

		ArrayList<AItem> items = new ArrayList<>(25);
		for (IDrawable drawable : _itemLists) {
			((ItemList) drawable).addEnclosedItems(this, items, rect);
		}
		return items;
	}

	/**
	 * {@inheritDoc}
     */
	@Override
	public ArrayList<AItem> getItemsAtPoint(Point lp) {
		ArrayList<AItem> items = new ArrayList<>(25);

		if (_itemLists != null) {
			for (int i = _itemLists.size() - 1; i >= 0; i--) {
				ItemList itemList = ((ItemList) _itemLists.get(i));
				itemList.addItemsAtPoint(items, this, lp);
			}
		}

		return items;
	}

	/**
	 * {@inheritDoc}
     */
	@Override
	public boolean anySelectedItems() {
		if (_itemLists != null) {
			for (IDrawable drawable : _itemLists) {
				ItemList itemList = (ItemList) drawable;
				if (itemList.anySelected()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
     */
	@Override
	public void deleteSelectedItems(IContainer container) {
		if (_itemLists != null) {
			for (IDrawable drawable : _itemLists) {
				ItemList itemList = (ItemList) drawable;
				itemList.deleteSelectedItems(container);
			}
		}
	}

	/**
	 * {@inheritDoc}
     */
	@Override
	public void selectAllItems(boolean select) {
		if (_itemLists != null) {
			for (IDrawable drawable : _itemLists) {
				ItemList itemList = (ItemList) drawable;
				itemList.selectAllItems(select);
			}
		}
	}


	/**
	 * {@inheritDoc}
     */
	@Override
	public void zoom(final double xmin, final double xmax, final double ymin, final double ymax) {
		prepareToZoom();
		_worldSystem = new Rectangle2D.Double(xmin, ymin, xmax - xmin, ymax - ymin);
		setDirty(true);
		refresh();
	}


	/**
	 * Get this container's tool bar.
	 *
	 * @return this container's tool bar, or <code>null</code>.
	 */
	@Override
	public BaseToolBar getToolBar() {
		return _toolBar;
	}

	/**
	 * Set this container's tool bar.
	 *
	 * @param toolBar the new toolbar.
	 */
	@Override
	public void setToolBar(BaseToolBar toolBar) {
		_toolBar = toolBar;
	}

	/**
	 * The mouse has been clicked.
	 *
	 * @param mouseEvent the causal event.
	 */
	@Override
	public void mouseClicked(MouseEvent mouseEvent) {

		if (!isEnabled()) {
			Toolkit.getDefaultToolkit().beep();
			return;
		}
	}

	/**
	 * The mouse has entered the container.
	 *
	 * @param mouseEvent the causal event.
	 */
	@Override
	public void mouseEntered(MouseEvent mouseEvent) {
		_currentMousePoint = mouseEvent.getPoint();

		ToolBarToggleButton mtb = getActiveButton();
		if (mtb != null) {
			setCursor(mtb.canvasCursor());
		}
	}

	/**
	 * The mouse has exited the container.
	 *
	 * @param mouseEvent the causal event.
	 */
	@Override
	public void mouseExited(MouseEvent mouseEvent) {
		_currentMousePoint = null;
	}

	/**
	 * The mouse was pressed in the container.
	 *
	 * @param mouseEvent the causal event.
	 */
	@Override
	public void mousePressed(MouseEvent mouseEvent) {
	}

	/**
	 * The mouse was released in the container.
	 *
	 * @param mouseEvent the causal event.
	 */
	@Override
	public void mouseReleased(MouseEvent mouseEvent) {
		if (_toolBar != null) {
			_toolBar.checkButtonState();
		}
	}

	/**
	 * The mouse was dragged in the container.
	 *
	 * @param mouseEvent the causal event.
	 */
	@Override
	public void mouseDragged(MouseEvent mouseEvent) {
		if (!isEnabled()) {
			return;
		}
		locationUpdate(mouseEvent, true);
	}

	/**
	 * The mouse has moved in the container.
	 *
	 * @param mouseEvent the causal event.
	 */
	@Override
	public void mouseMoved(MouseEvent mouseEvent) {
		if (!isEnabled()) {
			return;
		}
		_currentMousePoint = mouseEvent.getPoint();
		locationUpdate(mouseEvent, false);
	}

	/**
	 * Convert the mouse event location to a world point.
	 *
	 * @param me the mouse event
	 * @return the world location of the mouse click
	 */
	protected Point2D.Double getLocation(MouseEvent me) {
		if (me == null) {
			return null;
		}

		Point2D.Double wp = new Point2D.Double();
		localToWorld(me.getPoint(), wp);
		return wp;
	}

	/**
	 * Gets the current mouse position.
	 *
	 * @return the current mouse position.
	 */
	public Point getCurrentMousePoint() {
		return _currentMousePoint;
	}

	/**
	 * Get the active button on the toolbar, if there is a toolbar.
	 *
	 * @return the active toggle button.
	 */
	@Override
	public ToolBarToggleButton getActiveButton() {
		BaseToolBar tb = getToolBar();
		if (tb == null) {
			return null;
		} else {
			return tb.getActiveButton();
		}
	}

	/**
	 * Convenience method to update the location string in the toolbar.
	 *
	 * @param mouseEvent the causal event.
	 * @param dragging   <code>true</code> if we are dragging
	 */
	@Override
	public void locationUpdate(MouseEvent mouseEvent, boolean dragging) {

		_lastLocationMouseEvent = mouseEvent;
		ToolBarToggleButton mtb = getActiveButton();
		Point2D.Double wp = null;
		wp = getLocation(mouseEvent);

		if (mtb == null) {
			if (_feedbackControl != null) {
				_feedbackControl.updateFeedback(mouseEvent, wp, dragging);
			}
			return;
		}

		if (mtb == _toolBar.getPointerButton()) { // pointer active
			getToolBar().setText(Point2DSupport.toString(wp));
			if (_feedbackControl != null) {
				_feedbackControl.updateFeedback(mouseEvent, wp, dragging);
			}
		} else if (mtb == _toolBar.getPanButton()) { // pan active
			// do nothing
		} else { // default case
			wp = getLocation(mouseEvent);
			getToolBar().setText(Point2DSupport.toString(wp));
			if (_feedbackControl != null) {
				_feedbackControl.updateFeedback(mouseEvent, wp, dragging);
			}
		}
	}

	/**
	 * Force a redo of the feedback even though the mouse didn't move. This is
	 * useful, for example, when control-N'ing events.
	 */
	@Override
	public void redoFeedback() {
		if (_lastLocationMouseEvent != null) {
			locationUpdate(_lastLocationMouseEvent, false);
		}
	}

	/**
	 * Get the view (internal frame) that holds this container.
	 *
	 * @return the view (internal frame) that holds this container.
	 */
	@Override
	public BaseView getView() {
		return _view;
	}

	/**
	 * An item has changed.
	 *
	 * @param list     the ItemList it was on
	 * @param drawable the drawable (item) that changed.
	 * @param type     the type of the change.
	 */
	@Override
	public void drawableChanged(DrawableList list, IDrawable drawable, DrawableChangeType type) {

		AItem item = (drawable == null) ? null : (AItem) drawable;

		switch (type) {
		case ADDED:
			break;

		case DESELECTED:
			break;

		case DOUBLECLICKED:
			break;

		case HIDDEN:
				break;

		case MODIFIED:
			break;

		case MOVED:
			break;

		case REMOVED:
			if (item == _youAreHereItem) {
				_youAreHereItem = null;
			}
			break;

		case RESIZED:
			break;

		case ROTATED:
			break;

		case SELECTED:
			break;

		case SHOWN:
			break;

		case LISTCLEARED:
			break;

		case LISTHIDDEN:
			break;

		case LISTSHOWN:
			break;
		}

		// for now, lets not quibble
		if (item != null) {
			item.setDirty(true);
		}
	}

	/**
	 * Sets the feedback pane. This is an optional alternative to a HUD.
	 *
	 * @param feedbackPane the feedback pane.
	 */
	@Override
	public void setFeedbackPane(FeedbackPane feedbackPane) {
		_feedbackPane = feedbackPane;
	}

	/**
	 * Get the optional feedback pane.
	 *
	 * @return the feedbackPane
	 */
	@Override
	public FeedbackPane getFeedbackPane() {
		return _feedbackPane;
	}

	/**
	 * Return the object that controls the container's feedback. You can and and
	 * remove feedback providers using this object.
	 *
	 * @return the object that controls the container's feedback.
	 */
	@Override
	public FeedbackControl getFeedbackControl() {
		return _feedbackControl;
	}

	/**
	 * Get the optional YouAreHereItem
	 *
	 * @return the youAreHereItem
	 */
	@Override
	public YouAreHereItem getYouAreHereItem() {
		return _youAreHereItem;
	}

	/**
	 * Set the optional YouAreHereItem.
	 *
	 * @param youAreHereItem the youAreHereItem to set
	 */
	@Override
	public void setYouAreHereItem(YouAreHereItem youAreHereItem) {
		_youAreHereItem = youAreHereItem;
	}

	/**
	 * This is sometimes used as needed (i.e., not created until requested). That
	 * will generally make it the topmost view--so it is good for things like a
	 * reference point (YouAreHereItem).
	 *
	 * @return the glass list.
	 */
	@Override
	public ItemList getGlassList() {
		if (_glassList == null) {
			_glassList = new ItemList(this, "Glass List");
			_itemLists.add(_glassList);
		}
		return _glassList;
	}

	/**
	 * Get the underlying component, which is me.
	 *
	 * @return the underlying component, which is me.
	 */
	@Override
	public Component getComponent() {
		return this;
	}

	/**
	 * Set the after-draw drawable for this container.
	 *
	 * @param afterDraw the new after-draw drawable.
	 */
	@Override
	public void setAfterDraw(IDrawable afterDraw) {
		_afterDraw = afterDraw;
	}

	/**
	 * get the after drawer
	 *
	 * @return the after drawer
	 */
	public IDrawable getAfterDraw() {
		return _afterDraw;
	}

	/**
	 * Set the before-draw drawable.
	 *
	 * @param beforeDraw the new before-draw drawable.
	 */
	@Override
	public void setBeforeDraw(IDrawable beforeDraw) {
		_beforeDraw = beforeDraw;
	}

	/**
	 * get the before drawer
	 *
	 * @return the before drawer
	 */
	public IDrawable getBeforeDraw() {
		return _beforeDraw;
	}

	/**
	 * Set the optional magnification drawer
	 *
	 * @param mdraw the optional magnification drawer
	 */
	public void setMagnificationDraw(IDrawable mdraw) {
		_magDraw = mdraw;
	}

	/**
	 * Get the optional magnification drawer
	 *
	 * @return the optional magnification drawer
	 */
	public IDrawable getMagnificationDraw() {
		return _magDraw;
	}


	/**
	 * Get a location string for a point
	 *
	 * @param wp the world point in question
	 * @return a location string for a point
	 */
	@Override
	public String getLocationString(Point2D.Double wp) {
		return Point2DSupport.toString(wp);
	}


	/**
	 * Create a Point2D.Double or subclass thereof that is appropriate for this
	 * container.
	 *
	 * @return a Point2D.Double or subclass thereof that is appropriate for this
	 *         container.
	 */
	@Override
	public Point2D.Double getWorldPoint() {
		return new Point2D.Double();
	}

	/**
	 * Get the current world system
	 *
	 * @return the world system
	 */
	@Override
	public Rectangle2D.Double getWorldSystem() {
		return _worldSystem;
	}

	/**
	 * Set the world system (does not cause redraw)
	 *
	 * @param wr the new world system
	 */
	@Override
	public void setWorldSystem(Rectangle2D.Double wr) {
		_worldSystem = new Rectangle2D.Double(wr.x, wr.y, wr.width, wr.height);
	}

	// Get the transforms for world to local and vice versa
	protected void setAffineTransforms() {
		Rectangle bounds = getInsetRectangle();

		if ((bounds == null) || (bounds.width < 1) || (bounds.height < 1)) {
			localToWorld = null;
			worldToLocal = null;
			return;
		}

		if ((_worldSystem == null) || (Math.abs(_worldSystem.width) < 1.0e-12)
				|| (Math.abs(_worldSystem.height) < 1.0e-12)) {
			localToWorld = null;
			worldToLocal = null;
			return;
		}

		double scaleX = _worldSystem.width / bounds.width;
		double scaleY = _worldSystem.height / bounds.height;

		localToWorld = AffineTransform.getTranslateInstance(_worldSystem.getMinX(), _worldSystem.getMaxY());
		localToWorld.concatenate(AffineTransform.getScaleInstance(scaleX, -scaleY));
		localToWorld.concatenate(AffineTransform.getTranslateInstance(-bounds.x, -bounds.y));

		try {
			worldToLocal = localToWorld.createInverse();
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Convert a pixel based polygon to a world based polygon.
	 *
	 * @param polygon      the pixel based polygon
	 * @param worldPolygon the world based polygon
	 */
	@Override
	public void localToWorld(Polygon polygon, WorldPolygon worldPolygon) {
		Point2D.Double wp = new Point2D.Double();
		Point pp = new Point();
		for (int i = 0; i < polygon.npoints; ++i) {
			pp.setLocation(polygon.xpoints[i], polygon.ypoints[i]);
			localToWorld(pp, wp);
			worldPolygon.addPoint(wp.x, wp.y);
		}
	}

	/**
	 * Convert a world based polygon to a pixel based polygon.
	 *
	 * @param polygon      the pixel based polygon
	 * @param worldPolygon the world based polygon
	 */
	@Override
	public void worldToLocal(Polygon polygon, WorldPolygon worldPolygon) {
		Point pp = new Point();
		for (int i = 0; i < worldPolygon.npoints; ++i) {
			worldToLocal(pp, worldPolygon.xpoints[i], worldPolygon.ypoints[i]);
			polygon.addPoint(pp.x, pp.y);
		}
	}

	@Override
	public void setView(BaseView view) {
		_view = view;
	}

	/**
	 * The mouse scroll wheel has been moved.
	 *
	 * @param mouseEvent the causal event.
	 */
	@Override
	public void mouseWheelMoved(MouseWheelEvent mouseEvent) {
	}

	/**
	 * Obtain the inset rectangle. Insets are the inert region around the
	 * container's active area. Often there are no insets. Sometimes they are used
	 * so that text can be written in the inset area, such as for plot view.
	 *
	 * @return the inset rectangle.
	 */
	@Override
	public Rectangle getInsetRectangle() {
		Rectangle b = getComponent().getBounds();
		if (b == null) {
			return null;
		}

		// ignore b.x and b.y as usual
		int left = _lMargin;
		int top = _tMargin;
		int right = b.width - _rMargin;
		int bottom = b.height - _bMargin;

		Rectangle screenRect = new Rectangle(left, top, right - left, bottom - top);
		return screenRect;

	}

	/**
	 * Set the left margin
	 *
	 * @param lMargin the left margin
	 */
	@Override
	public void setLeftMargin(int lMargin) {
		_lMargin = lMargin;
	}

	/**
	 * Set the top margin
	 *
	 * @param tMargin the top margin
	 */
	@Override
	public void setTopMargin(int tMargin) {
		_tMargin = tMargin;
	}

	/**
	 * Set the right margin
	 *
	 * @param rMargin the right margin
	 */
	@Override
	public void setRightMargin(int rMargin) {
		_rMargin = rMargin;
	}

	/**
	 * Set the bottom margin
	 *
	 * @param bMargin the bottom margin
	 */
	@Override
	public void setBottomMargin(int bMargin) {
		_bMargin = bMargin;
	}

	// copier
	private Rectangle2D.Double copy(Rectangle2D.Double wr) {
		return new Rectangle2D.Double(wr.x, wr.y, wr.width, wr.height);
	}

	/**
	 * The active toolbar button changed.
	 *
	 * @param activeButton the new active button.
	 */
	@Override
	public void activeToolBarButtonChanged(ToolBarToggleButton activeButton) {
	}

	/**
	 * Get the background image.
	 *
	 * @return the fully painted background image.
	 */
	@Override
	public BufferedImage getImage() {
		BufferedImage image = GraphicsUtils.getComponentImageBuffer(this);
		GraphicsUtils.paintComponentOnImage(this, image);
		return image;

	}
}
