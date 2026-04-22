package edu.cnu.mdi.container;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.swing.JComponent;

import edu.cnu.mdi.feedback.FeedbackControl;
import edu.cnu.mdi.feedback.FeedbackPane;
import edu.cnu.mdi.graphics.GraphicsUtils;
import edu.cnu.mdi.graphics.drawable.IDrawable;
import edu.cnu.mdi.graphics.toolbar.AToolBar;
import edu.cnu.mdi.graphics.toolbar.BaseToolBar;
import edu.cnu.mdi.graphics.world.WorldPolygon;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.ItemChangeListener;
import edu.cnu.mdi.item.ItemChangeType;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.log.Log;
import edu.cnu.mdi.transfer.FileDropHandler;
import edu.cnu.mdi.view.BaseView;

/**
 * Primary 2D drawing container for MDI.
 * <p>
 * A container holds a world-coordinate viewport and a z-stack of
 * {@link Layer}s. Each layer holds a collection of {@link AItem}s.
 *
 * <h2>Layer architecture</h2>
 * This container defines three standard layers:
 * <ul>
 *   <li><b>Connections</b> (protected; drawn first / bottom)</li>
 *   <li><b>Content</b> (default user layer; typical items go here)</li>
 *   <li><b>Annotations</b> (protected; drawn last / top)</li>
 * </ul>
 *
 * <h3>Important implementation note</h3>
 * {@link Layer}'s constructor auto-registers the new layer with the container
 * via {@link IContainer#addLayer(Layer)}. During this container's constructor,
 * the protected layer fields are not yet assigned when the first
 * {@code addLayer} calls occur.
 * <p>
 * Therefore, after constructing the layers we explicitly remove the protected
 * layers from {@link #_layers} and re-add the default user layer if needed.
 * This guarantees that:
 * <ul>
 *   <li>{@link #_layers} contains only user layers</li>
 *   <li>protected layers are still drawn and hit-tested in their fixed
 *       positions</li>
 * </ul>
 *
 * <h2>Visibility and locking</h2>
 * Container-wide operations that involve user interaction (hit testing,
 * selection, deletion, enclosed selection) respect:
 * <ul>
 *   <li>{@link Layer#isVisible()} — invisible layers are ignored for hit
 *       testing and enclosure</li>
 *   <li>{@link Layer#isLocked()} — locked layers are treated as
 *       non-interactive and ignored</li>
 * </ul>
 *
 * <h2>Tool handler extensibility</h2>
 * Subclasses that need a specialised {@link BaseToolHandler} should override
 * {@link #createToolHandler()} rather than overriding {@link #setToolBar}.
 * This keeps the base class free of {@code instanceof} checks against specific
 * subclasses.
 */
@SuppressWarnings("serial")
public class BaseContainer extends JComponent implements IContainer, ItemChangeListener {

	/**
	 * The user-managed z-layers (does NOT include protected layers).
	 * <p>
	 * Ordering convention: index 0 is back (drawn earlier), last index is front.
	 */
	protected List<Layer> _layers = new ArrayList<>();

	/** Optional toolbar for this container. */
	protected AToolBar _toolBar;

	/** Optional feedback(mouse-over) pane. */
	protected FeedbackPane _feedbackPane;

	/** Optional drawable invoked after all z layers are drawn. */
	protected IDrawable _afterDraw;

	/** Optional drawable invoked before user layers are drawn. */
	protected IDrawable _beforeDraw;

	/** The view that holds this container (may be null for viewless container). */
	protected BaseView _view;

	/** World coordinate viewport. */
	protected Rectangle2D.Double _worldSystem;

	/** Default/original world system. */
	protected Rectangle2D.Double _defaultWorldSystem;

	/** Previous world system (used to undo last zoom). */
	protected Rectangle2D.Double _previousWorldSystem;

	/**
	 * Default user content layer (drawn between connection and annotation layers).
	 * Most developer-created items go here by default.
	 */
	protected Layer _defaultLayer;

	/**
	 * Annotation layer (protected; always drawn last).
	 */
	protected Layer _annotationLayer;

	/**
	 * Connection layer (protected; always drawn first).
	 */
	protected Layer _connectionLayer;

	/**
	 * Feedback control for mouse-over feedback providers.
	 */
	protected final FeedbackControl _feedbackControl;

	/** Transform local(screen) -> world. */
	protected AffineTransform localToWorld;

	/** Transform world -> local(screen). */
	protected AffineTransform worldToLocal;

	// Tool handler for this container
	protected BaseToolHandler toolHandler;

	/**
	 * Constructor.
	 *
	 * @param worldSystem the default world system (viewport)
	 */
	public BaseContainer(Rectangle2D.Double worldSystem) {

		_feedbackControl = new FeedbackControl(this);

		resetWorldSystem(worldSystem);

		// Create layers. NOTE: Layer constructor auto-calls container.addLayer(this).
		_connectionLayer = new Layer(this, "Connections");
		_defaultLayer = new Layer(this, "Content");
		_annotationLayer = new Layer(this, "Annotations");

		// IMPORTANT: Because addLayer() was called before the fields were assigned,
		// the protected layers may have accidentally been added to _layers. Fix it now.
		_layers.remove(_connectionLayer);
		_layers.remove(_annotationLayer);

		// Ensure the default layer is present as a user layer
		if (!_layers.contains(_defaultLayer)) {
			_layers.add(_defaultLayer);
		}

		// listen for resize events
		ComponentAdapter componentAdapter = new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent ce) {
				setDirty(true);
				repaint();
				setAffineTransforms();
			}
		};

		addComponentListener(componentAdapter);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setView(BaseView view) {
		_view = view;
		// Initialize the file drop handler
		setTransferHandler(new FileDropHandler(_view));
	}

	/**
	 * Share the model with another container (used for magnification windows).
	 * <p>
	 * This is not a deep copy: layers and items are shared by reference.
	 *
	 * @param sContainer the source container
	 */
	public void shareModel(BaseContainer sContainer) {
		if (sContainer == null) {
			return;
		}
		_annotationLayer = sContainer._annotationLayer;
		_connectionLayer = sContainer._connectionLayer;
		_defaultLayer = sContainer._defaultLayer;
		_layers = sContainer._layers;
		_afterDraw = sContainer._afterDraw;
		_beforeDraw = sContainer._beforeDraw;
		setBackground(sContainer.getBackground());
		setForeground(sContainer.getForeground());
	}

	/**
	 * Get the default user content layer.
	 *
	 * @return the default user content layer (never null after construction)
	 */
	@Override
	public Layer getDefaultLayer() {
		return _defaultLayer;
	}

	/**
	 * Determine whether a layer is one of the protected layers.
	 *
	 * @param layer layer to test
	 * @return true if layer is the connection or annotation layer
	 */
	public boolean isProtectedLayer(Layer layer) {
		return (layer == _connectionLayer) || (layer == _annotationLayer);
	}

	/**
	 * Get all layers in draw order (bottom -> top), including protected layers.
	 * <p>
	 * Order:
	 * <ol>
	 * <li>Connections</li>
	 * <li>User layers</li>
	 * <li>Annotations</li>
	 * </ol>
	 *
	 * @return immutable snapshot list in draw order (never null)
	 */
	public List<Layer> getAllLayers() {
		ArrayList<Layer> all = new ArrayList<>(_layers.size() + 2);
		if (_connectionLayer != null) {
			all.add(_connectionLayer);
		}
		if (_layers != null) {
			all.addAll(_layers);
		}
		if (_annotationLayer != null) {
			all.add(_annotationLayer);
		}
		return Collections.unmodifiableList(all);
	}

	/**
	 * Get all layers in hit-test order (top -> bottom), including protected layers.
	 * <p>
	 * Order:
	 * <ol>
	 * <li>Annotations</li>
	 * <li>User layers (top -> bottom)</li>
	 * <li>Connections</li>
	 * </ol>
	 *
	 * @return immutable snapshot list in hit-test order (never null)
	 */
	public List<Layer> getAllLayersForHitTesting() {
		ArrayList<Layer> all = new ArrayList<>(_layers.size() + 2);

		if (_annotationLayer != null) {
			all.add(_annotationLayer);
		}

		if (_layers != null) {
			for (int i = _layers.size() - 1; i >= 0; i--) {
				all.add(_layers.get(i));
			}
		}

		if (_connectionLayer != null) {
			all.add(_connectionLayer);
		}

		return Collections.unmodifiableList(all);
	}

	/**
	 * Clip graphics context to container bounds.
	 *
	 * @param g graphics context
	 */
	public void clipBounds(Graphics g) {
		Rectangle b = getBounds();
		g.setClip(0, 0, b.width, b.height);
	}

	/**
	 * Get the approximate zoom factor based on the current and default world
	 * systems.
	 *
	 * @return the approximate zoom factor. Numbers > 1 mean zoomed in; < 1 mean
	 *         zoomed out.
	 */
	@Override
	public double approximateZoomFactor() {
		if (_worldSystem == null || _defaultWorldSystem == null) {
			return 1.0;
		}

		double scaleX = _defaultWorldSystem.width / _worldSystem.width;
		double scaleY = _defaultWorldSystem.height / _worldSystem.height;

		return (scaleX + scaleY) / 2.0;
	}

	/**
	 * Override paint command. Draw all layers.
	 * <p>
	 * Rendering order:
	 * <ol>
	 * <li>background</li>
	 * <li>connection layer</li>
	 * <li>before-draw</li>
	 * <li>user layers</li>
	 * <li>after-draw</li>
	 * <li>annotation layer</li>
	 * </ol>
	 *
	 * @param g graphics context
	 */
	@Override
	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;

		if ((_view != null) && !_view.isViewVisible()) {
			return;
		}

		super.paintComponent(g2);

		clipBounds(g2);

		Rectangle b = getBounds();

		setAffineTransforms();
		if (localToWorld == null) {
			return;
		}

		// background
		g2.setColor(getBackground());
		g2.fillRect(0, 0, b.width, b.height);

		// before-draw is drawn before layers, for non-item
		// drawing such as grids or maps
		if (_beforeDraw != null) {
			_beforeDraw.draw(g2, this);
		}

		// connection layer first ) (typically lines between items)
		if (_connectionLayer != null) {
			_connectionLayer.draw(g2, this);
		}

		// user layers including the default (content) layer
		for (Layer layer : _layers) {
			layer.draw(g2, this);
		}

		// after-draw is drawn after layers, for overlays
		if (_afterDraw != null) {
			_afterDraw.draw(g2, this);
		}

		// annotation layer last
		if (_annotationLayer != null) {
			_annotationLayer.draw(g2, this);
		}

		// always clean after drawing
		setDirty(false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addLayer(Layer layer) {
		Objects.requireNonNull(layer, "layer");

		// During construction the _annotationLayer/_connectionLayer fields are not yet
		// assigned, so these guards can never fire then — they only matter for
		// post-construction callers who should not pass a protected layer here.
		if (layer == _annotationLayer || layer == _connectionLayer) {
			Log.getInstance().warning("addLayer() called with a protected layer ('"
					+ layer.getName() + "'). Protected layers are managed internally "
					+ "and must not be passed to addLayer(). Call ignored.");
			return;
		}

		_layers.remove(layer); // deduplicate: remove if already present, then re-add at top
		_layers.add(layer);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Layer getAnnotationLayer() {
		return _annotationLayer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Layer getConnectionLayer() {
		return _connectionLayer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Layer getLayerByName(String name) {
		Objects.requireNonNull(name, "Layer name cannot be null");
		for (Layer layer : _layers) {
			if (layer.getName().equals(name)) {
				return layer;
			}
		}
		return null;
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
		if (worldToLocal == null || wp == null || pp == null) {
			return;
		}
		worldToLocal.transform(wp, pp);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void worldToLocal(Rectangle r, Rectangle.Double wr) {
		// accommodates world with x decreasing to the right
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

		// accommodates world with x decreasing to the right
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
		Point2D.Double wp = new Point2D.Double();
		localToWorld(pp, wp);
		recenter(_worldSystem, wp);
		setDirty(true);
		refresh();
	}

	/**
	 * Recenter a world rectangle about a new center point.
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
	public void resetWorldSystem(Rectangle2D.Double worldSystem) {
		_worldSystem = worldSystem;
		_defaultWorldSystem = copy(worldSystem);
		_previousWorldSystem = copy(worldSystem);
		setDirty(true);
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
	 * Scale a world rectangle about its center.
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
	 * {@inheritDoc}
	 */
	@Override
	public void setDirty(boolean dirty) {

		setAffineTransforms();

		// mark dirty across ALL layers, including protected layers
		for (Layer layer : getAllLayers()) {
			layer.setDirty(dirty);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Includes protected layers and respects visibility/locking.
	 */
	@Override
	public AItem getItemAtPoint(Point pp) {
		if (pp == null) {
			return null;
		}

		for (Layer layer : getAllLayersForHitTesting()) {
			if (!layer.isVisible() || layer.isLocked()) {
				continue;
			}
			AItem item = layer.getItemAtPoint(this, pp);
			if (item != null) {
				return item;
			}
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Includes protected layers and respects visibility/locking.
	 */
	@Override
	public ArrayList<AItem> getEnclosedItems(Rectangle rect) {

		if (rect == null) {
			return null;
		}

		ArrayList<AItem> items = new ArrayList<>();
		for (Layer layer : getAllLayers()) {
			if (!layer.isVisible() || layer.isLocked()) {
				continue;
			}
			layer.addEnclosedItems(this, items, rect);
		}
		return items;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Includes protected layers and returns items in top -> bottom order.
	 */
	@Override
	public ArrayList<AItem> getItemsAtPoint(Point lp) {
		ArrayList<AItem> items = new ArrayList<>(25);

		if (lp == null) {
			return items;
		}

		for (Layer layer : getAllLayersForHitTesting()) {
			if (!layer.isVisible() || layer.isLocked()) {
				continue;
			}
			layer.addItemsAtPoint(items, this, lp);
		}

		return items;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Includes protected layers. Does not require visibility, so selections on
	 * hidden layers still count (if your tools allow that state).
	 */
	@Override
	public boolean anySelectedItems() {
		for (Layer layer : getAllLayers()) {
			if (layer.anySelected()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Includes protected layers and respects visibility/locking.
	 */
	@Override
	public void deleteSelectedItems() {
		for (Layer layer : getAllLayers()) {
			if (!layer.isVisible() || layer.isLocked()) {
				continue;
			}
			layer.deleteSelectedItems(this);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Includes protected layers. Uses {@link Layer#getSelectedItems()} which may
	 * itself honor layer locking depending on your Layer implementation.
	 */
	@Override
	public List<AItem> getSelectedItems() {
		ArrayList<AItem> selectedItems = new ArrayList<>();

		for (Layer layer : getAllLayers()) {
			selectedItems.addAll(layer.getSelectedItems());
		}

		return selectedItems;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Includes protected layers and respects locking.
	 */
	@Override
	public void selectAllItems(boolean select) {
		for (Layer layer : getAllLayers()) {
			if (layer.isLocked()) {
				continue;
			}
			layer.selectAllItems(select);
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
	 * {@inheritDoc}
	 */
	@Override
	public AToolBar getToolBar() {
		return _toolBar;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Installs the toolbar and creates the tool handler via
	 * {@link #createToolHandler()}, which subclasses may override to supply a
	 * different handler without requiring an {@code instanceof} branch here.</p>
	 */
	@Override
	public void setToolBar(AToolBar toolBar) {
		_toolBar = toolBar;
		if (_toolBar != null) {
			toolHandler = createToolHandler();
			((BaseToolBar) _toolBar).setHandler(toolHandler);
		}
	}

	/**
	 * Factory method that creates the {@link BaseToolHandler} appropriate for
	 * this container.
	 *
	 * <p>The default implementation returns a plain {@link BaseToolHandler}.
	 * Subclasses that require specialised tool behaviour (e.g.
	 * {@code MapContainer}) should override this method to return their own
	 * handler rather than relying on an {@code instanceof} check in
	 * {@link #setToolBar}.</p>
	 *
	 * @return a new tool handler bound to this container; never {@code null}
	 */
	protected BaseToolHandler createToolHandler() {
		return new BaseToolHandler(this);
	}

	/**
	 * Check and set the toolbar state (e.g., button enable/disable)
	 */
	public void setToolBarState() {
		if (_toolBar != null) {
			if (_toolBar instanceof BaseToolBar) {
				((BaseToolBar) _toolBar).setDeleteEnabled(anySelectedItems());
			}
		}
	}

	/**
	 * Convert the mouse event location to a world point.
	 *
	 * @param me mouse event
	 * @return world location, or null
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
	 * {@inheritDoc}
	 */
	@Override
	public void feedbackTrigger(MouseEvent mouseEvent, boolean dragging) {
		Point2D.Double wp = getLocation(mouseEvent);
		Point pp = mouseEvent.getPoint();

		// Update toolbar text (if present)
		if (_toolBar != null) {
			String statusText = String.format("Local: (%d, %d) World: (%.2f, %.2f)", pp.x, pp.y, wp.x, wp.y);
			_toolBar.updateStatusText(statusText);
		}

		// Update feedback (if present)
		if (_feedbackControl != null) {
			_feedbackControl.updateFeedback(mouseEvent, wp, dragging);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BaseView getView() {
		return _view;
	}

	/**
	 * Called when files are dropped onto this container's canvas.
	 *
	 * <p>The default implementation logs each dropped filename and then
	 * delegates to {@link BaseView#filesDropped(List)} if a view is attached.
	 * Subclasses may override this to handle files differently before or
	 * instead of passing them to the view.</p>
	 *
	 * @param files the validated files that were dropped; never {@code null}
	 */
	protected void onFilesDropped(List<File> files) {
		for (File f : files) {
			Log.getInstance().info("File dropped: " + f.getName());
		}
		if (_view != null) {
			_view.filesDropped(files);
		}
	}

	/**
	 * Responds to item change events fired by layers owned by this container.
	 *
	 * <p>The base implementation:
	 * <ul>
	 *   <li>Updates the toolbar state (e.g. enables/disables the delete button)
	 *       when an item is selected or deselected.</li>
	 *   <li>Marks the affected item dirty on any change, triggering a repaint
	 *       on the next {@link #refresh()} call.</li>
	 * </ul>
	 * All other change types are no-ops in the base class. Subclasses may
	 * override to add behaviour for specific types such as {@code MOVED} or
	 * {@code DELETED}.</p>
	 *
	 * @param layer the layer containing the changed item
	 * @param item  the affected item; may be {@code null} for layer-level events
	 * @param type  the type of change
	 */
	@Override
	public void itemChanged(Layer layer, AItem item, ItemChangeType type) {
		switch (type) {
		case SELECTED:
		case DESELECTED:
			setToolBarState();
			break;

		case ADDED:
		case DELETED:
		case DOUBLECLICKED:
		case HIDDEN:
		case MODIFIED:
		case MOVED:
		case RESIZED:
		case ROTATED:
		case SHOWN:
			// Intentional no-op in the base class. Subclasses may override.
			break;
		}

		// Conservative default: mark the item dirty so it is redrawn.
		if (item != null) {
			item.setDirty(true);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setFeedbackPane(FeedbackPane feedbackPane) {
		_feedbackPane = feedbackPane;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FeedbackPane getFeedbackPane() {
		return _feedbackPane;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FeedbackControl getFeedbackControl() {
		return _feedbackControl;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Component getComponent() {
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setAfterDraw(IDrawable afterDraw) {
		_afterDraw = afterDraw;
	}

	/**
	 * @return after-draw drawable (may be null)
	 */
	public IDrawable getAfterDraw() {
		return _afterDraw;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setBeforeDraw(IDrawable beforeDraw) {
		_beforeDraw = beforeDraw;
	}

	/**
	 * @return before-draw drawable (may be null)
	 */
	public IDrawable getBeforeDraw() {
		return _beforeDraw;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Rectangle2D.Double getWorldSystem() {
		return _worldSystem;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setWorldSystem(Rectangle2D.Double wr) {
		_worldSystem = new Rectangle2D.Double(wr.x, wr.y, wr.width, wr.height);
	}

	/**
	 * Compute transforms for world <-> local conversion.
	 */
	protected void setAffineTransforms() {
		// Use getWidth()/getHeight() rather than getBounds() here.
		//
		// getBounds() returns the component's rectangle in its *parent's*
		// coordinate space (bounds.x/y = offset within parent). But
		// paintComponent() receives a Graphics2D already translated to the
		// component's own origin — (0,0) in that context is always the
		// component's top-left corner regardless of parent position.
		//
		// The old code subtracted bounds.x/bounds.y in the affine translate,
		// double-correcting for the parent offset and corrupting the world↔screen
		// mapping whenever the container was not at (0,0) in its parent — e.g.
		// after adding a WEST panel that shifts the canvas rightward.
		int w = getWidth();
		int h = getHeight();

		if (w < 1 || h < 1) {
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

		double scaleX = _worldSystem.width / w;
		double scaleY = _worldSystem.height / h;

		// Map pixel (0,0) → world top-left; y-axis flipped because screen y
		// increases downward while world y increases upward.
		localToWorld = AffineTransform.getTranslateInstance(_worldSystem.getMinX(), _worldSystem.getMaxY());
		localToWorld.concatenate(AffineTransform.getScaleInstance(scaleX, -scaleY));

		try {
			worldToLocal = localToWorld.createInverse();
		} catch (NoninvertibleTransformException e) {
			// The guards above (non-zero bounds, non-zero world extents) make this
			// branch unreachable in practice. If it somehow fires, leave both
			// transforms null so callers get a clean no-op rather than stale data.
			localToWorld = null;
			worldToLocal = null;
			Log.getInstance().error("Non-invertible affine transform in setAffineTransforms: " + e.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
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
	 * {@inheritDoc}
	 */
	@Override
	public void worldToLocal(Polygon polygon, WorldPolygon worldPolygon) {
		Point pp = new Point();
		for (int i = 0; i < worldPolygon.npoints; ++i) {
			worldToLocal(pp, worldPolygon.xpoints[i], worldPolygon.ypoints[i]);
			polygon.addPoint(pp.x, pp.y);
		}
	}

	/**
	 * Copy helper.
	 */
	private Rectangle2D.Double copy(Rectangle2D.Double wr) {
		return new Rectangle2D.Double(wr.x, wr.y, wr.width, wr.height);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BufferedImage getImage() {
		BufferedImage image = GraphicsUtils.getComponentImageBuffer(this);
		GraphicsUtils.paintComponentOnImage(this, image);
		return image;
	}

	@Override
	public void prepareForExit() {
		// no op
	}

}