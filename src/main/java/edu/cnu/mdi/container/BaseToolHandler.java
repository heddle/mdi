package edu.cnu.mdi.container;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import edu.cnu.mdi.component.MagnifyWindow;
import edu.cnu.mdi.graphics.GraphicsUtils;
import edu.cnu.mdi.graphics.connection.ConnectionManager;
import edu.cnu.mdi.graphics.style.IStyled;
import edu.cnu.mdi.graphics.style.Styled;
import edu.cnu.mdi.graphics.style.ui.StyleEditorDialog;
import edu.cnu.mdi.graphics.toolbar.GestureContext;
import edu.cnu.mdi.graphics.toolbar.IToolHandler;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.ItemModification;
import edu.cnu.mdi.item.TextItem;
import edu.cnu.mdi.util.PrintUtils;
import edu.cnu.mdi.util.TakePicture;
import edu.cnu.mdi.view.BaseView;

public class BaseToolHandler implements IToolHandler  {

	// Zoom factor for each zoom in/out action
	private static final double ZOOM_FACTOR = 0.8;

	// for panning
	//for panning
	private BufferedImage base;
	private BufferedImage buffer;

	// Container that owns this tool handler
	private final BaseContainer container;

	//for modifying items
	private AItem modifyItem;
	private boolean modifying;

	// cached press point for the current drag gesture
	private Point dragPressPoint;



	/**
	 * Constructor.
	 *
	 * @param container BaseContainer that owns this tool handler
	 */
	public BaseToolHandler(BaseContainer container) {
		Objects.requireNonNull(container, "container");
		this.container = container;

		//TODO: TEST TO CONFIRM THIS ISN'T NEEDED ANYMORE
//		MouseMotionListener mml = new MouseMotionListener() {
//			@Override
//			public void mouseMoved(MouseEvent e) {
//				container.feedbackTrigger(e, false);
//			}
//
//			@Override
//			public void mouseDragged(MouseEvent e) {
//				container.feedbackTrigger(e, true);
//			}
//		};
//		container.getComponent().addMouseMotionListener(mml);
	}



	/**
	 * Hit test at the given point on the canvas.
	 *
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas  JComponent on which the hit test is occurring
	 * @param p       Point to hit test
	 * @return Object that was hit, or null if nothing was hit
	 */
	@Override
	public Object hitTest(GestureContext gc, Point p) {
		return container.getItemAtPoint(p);
	}

	/**
	 * Handle pointer click on an object at the given point.
	 *
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the click is occurring
	 * @param p      Point where the click occurred
	 * @param obj    Object that was clicked or null if none
	 * @param e      MouseEvent that triggered the click
	 */
	@Override
	public void pointerClick(GestureContext gc) {
		AItem item = container.getItemAtPoint(gc.getPressPoint());
		MouseEvent e = gc.getSourceEvent();
		if (item != null && item.isEnabled() && item.isTrackable()) {
			if (item.isRightClickable() && (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e))) {
				// Show popup menu
				JPopupMenu menu = item.getPopupMenu();
				if (menu != null) {
					menu.show(gc.getCanvas(), e.getX(), e.getY());
				}
				return;
			}
			selectItemsFromClick(item, e);
		}
		else {
			// Clicked on empty space: deselect all
			container.selectAllItems(false);
			container.refresh();
		}
		container.setToolBarState();
	}

	/**
	 * Handle pointer double click on an object at the given point.
	 *
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the double click is occurring
	 * @param p      Point where the double click occurred
	 * @param obj    Object that was double clicked or null if none
	 * @param e      MouseEvent that triggered the click
	 */
	@Override
	public void pointerDoubleClick(GestureContext gc) {
		MouseEvent e = gc.getSourceEvent();
		AItem item = container.getItemAtPoint(e.getPoint());
		if (item != null && item.isEnabled() && item.isDoubleClickable()) {
			item.doubleClicked(e);
			return;
		}
	}

	@Override
	public void pointerRubberbanding(GestureContext gc, Rectangle bounds) {
		container.selectAllItems(false);

		ArrayList<AItem> enclosed = container.getEnclosedItems(bounds);
		if (enclosed != null) {
			for (AItem item : enclosed) {
				if (item != null && !item.isLocked()) {
					item.getLayer().selectItem(item, true);
				}
			}
		}

		container.setToolBarState();
		container.refresh();
	}

	@Override
	public void beginDragObject(GestureContext gc) {

		modifyItem = null;
		modifying = false;

	    // Cache a defensive copy (Point is mutable)
	    dragPressPoint = (gc.getPressPoint() == null) ? null : new Point(gc.getPressPoint());
	}

	@Override
	public void dragObjectBy(GestureContext gc, int dx, int dy) {

		MouseEvent e = gc.getSourceEvent();
		Object obj = gc.getTarget();
	    if (!(obj instanceof AItem)) {
	        return;
	    }

	    AItem item = (AItem) obj;
	    if (!item.isTrackable()) {
	        return;
	    }

	    if (!modifying) {
	        modifyItem = item;
	        modifying = true;

	        Point current = e.getPoint();

	        // Prefer explicit press point from beginDragObject; fall back to offset trick.
	        Point press = (dragPressPoint != null)
	                ? new Point(dragPressPoint)
	                : new Point(current.x - dx, current.y - dy);

	        ItemModification mod = new ItemModification(
	                modifyItem,
	                container,
	                press,
	                current,
	                e.isShiftDown(),
	                e.isControlDown()
	        );

	        modifyItem.setModification(mod);
	        modifyItem.startModification();
	    }

	    if (modifyItem != null) {
	        ItemModification mod = modifyItem.getItemModification();
	        if (mod != null) {
	            mod.setCurrentMousePoint(e.getPoint());
	        }
	        modifyItem.modify();
	    }
	}


	@Override
	public void endDragObject(GestureContext gc) {

	    if (modifyItem != null) {
	        modifyItem.stopModification();
	        // stopModification already nulls _modification in your AItem; this line is redundant:
	        // modifyItem.setModification(null);
	    }

	    modifyItem = null;
	    modifying = false;
	    dragPressPoint = null;
	}


	@Override
	public void boxZoomRubberbanding(GestureContext gc, Rectangle bounds) {
		container.rubberBanded(bounds);
	}


	@Override
	public void panStartDrag(GestureContext gc) {
		base = GraphicsUtils.getComponentImage(gc.getCanvas());
		buffer = GraphicsUtils.getComponentImageBuffer(gc.getCanvas());
	}

	@Override
	public void panUpdateDrag(GestureContext gc) {

		Point start = gc.getPressPoint();
		Point current = gc.getCurrentPoint();

		int totalDx = current.x - start.x;
		int totalDy = current.y - start.y;

		Graphics gg = buffer.getGraphics();
		gg.setColor(gc.getCanvas().getBackground());
		gg.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());
		gg.drawImage(base, totalDx, totalDy, gc.getCanvas());
		gg.dispose();

		Graphics g = gc.getCanvas().getGraphics();
		g.drawImage(buffer, 0, 0, 	gc.getCanvas());
		g.dispose();
	}

	@Override
	public void panDoneDrag(GestureContext gc) {
		Point start = gc.getPressPoint();
		Point end = gc.getCurrentPoint();
		int dx = end.x - start.x;
		int dy = end.y - start.y;
		container.pan(dx, dy);
		base = null;
		buffer = null;
		container.refresh();
	}

	@Override
	public void magnifyStartMove(GestureContext gc) {
		MouseEvent e = gc.getRecentEvent();
		BaseView view = container.getView();
		if (view != null) {
			view.handleMagnify(e);
		}
	}

	@Override
	public void magnifyUpdateMove(GestureContext gc) {
		MouseEvent e = gc.getRecentEvent();
		BaseView view = container.getView();
		if (view != null) {
			view.handleMagnify(e);
		}
	}

	@Override
	public void magnifyDoneMove(GestureContext gc) {
		MagnifyWindow.closeMagnifyWindow();
		gc.getToolBar().resetDefaultToggleButton();
	}

	@Override
	public void recenter(GestureContext gc) {
		container.prepareToZoom();
		container.recenter(gc.getCurrentPoint());
		container.refresh();
	}

	@Override
	public void zoomIn(GestureContext gc) {
		container.scale(ZOOM_FACTOR);
	}

	@Override
	public void zoomOut(GestureContext gc) {
		container.scale(1.0 / ZOOM_FACTOR);
	}

	@Override
	public void undoZoom(GestureContext gc) {
		container.undoLastZoom();
	}

	@Override
	public void resetZoom(GestureContext gc) {
		container.restoreDefaultWorld();
	}

	@Override
	public void styleEdit(GestureContext gc) {
		List<AItem> selected = container.getSelectedItems();

		if (selected == null || selected.isEmpty()) {
			java.awt.Toolkit.getDefaultToolkit().beep();
			return;
		}

		// text items are special - cannot style multiple at once
		// have their own editor

		if (selected.size() == 1 && selected.get(0) instanceof TextItem) {
			TextItem item = (TextItem) selected.get(0);
			item.edit();
			return;
		}

		// seed from first selected item
		IStyled seed = selected.get(0).getStyleSafe();
		Styled edited = StyleEditorDialog.edit(container.getComponent(), seed, false);
		if (edited == null) {
			return;
		}

		for (AItem item : selected) {
			item.setStyle(edited.copy()); // avoid shared mutable style objects
			item.setDirty(true);
		}

		container.refresh();
	}

	@Override
	public void delete(GestureContext gc) {
		container.deleteSelectedItems();
		gc.getToolBar().resetDefaultToggleButton();
		container.refresh();
	}

	@Override
	public void createConnection(GestureContext gc, Point start, Point end) {
		AItem item1 = container.getItemAtPoint(start);
		AItem item2 = container.getItemAtPoint(end);
		if (ConnectionManager.getInstance().canConnect(item1, item2)) {
			ConnectionManager.getInstance().connect(container.getConnectionLayer(), item1, item2);
			container.refresh();

		}
	}

	@Override
	public boolean approveConnectionPoint(GestureContext gc, Point p) {
		AItem item = container.getItemAtPoint(p);
		if (item != null) {
			boolean enabled = item.isEnabled();
            boolean connectable = item.isConnectable();
			return enabled && connectable;
		}
		return false;
	}

	@Override
	public void createRectangle(GestureContext gc, Rectangle bounds) {
		CreationSupport.createRectangleItem(container.getAnnotationLayer(), bounds);
	}

	@Override
	public void createEllipse(GestureContext gc, Rectangle bounds) {
		CreationSupport.createEllipseItem(container.getAnnotationLayer(), bounds);
	}

	@Override
	public void createPolygon(GestureContext gc, Point[] vertices) {
		CreationSupport.createPolygonItem(container.getAnnotationLayer(), vertices);

	}

	@Override
	public void createPolyline(GestureContext gc, Point[] vertices) {
		CreationSupport.createPolylineItem(container.getAnnotationLayer(), vertices);
	}

	@Override
	public void createLine(GestureContext gc, Point start, Point end) {
		CreationSupport.createLineItem(container.getAnnotationLayer(), start, end);
	}


	@Override
	public void createRadArc(GestureContext gc, Point[] pp) {

	    if (pp == null || pp.length != 3) {
	        return;
	    }

	    // If RubberRadArc provided an unwrapped signed sweep, use it directly.
	    Double sweep = (gc != null) ? gc.getRubberbandAngleDeg() : null;

	    if (sweep == null) {
	        // Fallback only (should be rare): compute minor signed angle
	        // NOTE: pp are screen points; CreationSupport likely converts to world.
	        // Keep your existing behavior if needed.
	        return;
	    }

	    CreationSupport.createRadArcItem(container.getAnnotationLayer(), pp[0], pp[1], sweep);
	}

	@Override
	public void createTextItem(Point location) {
		CreationSupport.createTextItem(container.getAnnotationLayer(), location);
		container.refresh();
	}


	@Override
	public void captureImage(GestureContext gc) {
		TakePicture.takePicture(gc.getCanvas());
	}

	@Override
	public void print(GestureContext gc) {
		PrintUtils.printComponent(gc.getCanvas());
	}

	/**
	 * Select items based on a click.
	 *
	 * @param item clicked item (must not be null).
	 * @param e    mouse event.
	 */
	private void selectItemsFromClick(AItem item, MouseEvent e) {

		// Only left-click participates in selection changes.
		// Clicking a locked or already-selected item: do nothing.
		if (!SwingUtilities.isLeftMouseButton(e) || item.isLocked() || item.isSelected()) {
			return;
		}

		// If Ctrl not held, deselect all first.
		if (!e.isControlDown()) {
			container.selectAllItems(false);
		}

		// Select the clicked item.
		item.getLayer().selectItem(item, true);

		container.setDirty(true);
		container.refresh();
	}


	@Override
	public boolean doNotDrag(GestureContext gc) {
		return false;
	}




}

