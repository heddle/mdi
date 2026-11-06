package edu.cnu.mdi.container;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
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
import edu.cnu.mdi.item.ItemState;
import edu.cnu.mdi.item.TextItem;
import edu.cnu.mdi.undo.ItemEdits;
import edu.cnu.mdi.util.PrintUtils;
import edu.cnu.mdi.util.TakePicture;
import edu.cnu.mdi.view.BaseView;

/**
 * Default tool handler for BaseContainer. This class provides standard
 * implementations for all tools, which can be used as-is or overridden by
 * subclasses.
 *
 * <p>Dragging an already-selected item translates every selected item that is
 * draggable and belongs to an unlocked layer. Locked and non-draggable items
 * remain in place. Resize and rotate gestures continue to affect only the item
 * whose handle initiated the gesture.</p>
 *
 * @author heddle
 */
public class BaseToolHandler implements IToolHandler {

	// Zoom factor for each zoom in/out action
	protected static final double ZOOM_FACTOR = 0.8;

	// for panning
	// for panning
	protected BufferedImage base;
	protected BufferedImage buffer;

	// Container that owns this tool handler
	protected final BaseContainer container;

	// Primary item and all eligible items participating in the current gesture.
	protected AItem modifyItem;
	protected final List<AItem> modifyItems = new ArrayList<>();
	protected boolean modifying;

	// cached press point for the current drag gesture
	protected Point dragPressPoint;

	// state of the items in the current drag/resize/rotate gesture before it
	// changed anything, and the name its undo entry will have
	private List<ItemState> gestureBefore;
	private String gestureName;

	/**
	 * Constructor.
	 *
	 * @param container BaseContainer that owns this tool handler
	 */
	public BaseToolHandler(BaseContainer container) {
		Objects.requireNonNull(container, "container");
		this.container = container;
	}

	@Override
	public void info(GestureContext gc) {
		BaseView view = container.getView();
		if (view != null) {
			view.viewInfo();
		}
	}

	/**
	 * Hit test at the given point on the canvas.
	 *
	 * @param gc	  GestureContext providing context for the hit test
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
	 * @param gc GestureContext providing context for the click
	 */
	@Override
	public void pointerClick(GestureContext gc) {
		AItem item = container.getItemAtPoint(gc.getPressPoint());
		MouseEvent e = gc.getSourceEvent();
		if (item != null && item.isEnabled() && item.isSelectable()) {
			if (item.isRightClickable() && (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e))) {
				// Show popup menu
				JPopupMenu menu = item.getPopupMenu();
				if (menu != null) {
					menu.show(gc.getCanvas(), e.getX(), e.getY());
				}
				return;
			}
			selectItemsFromClick(item, e);
		} else {
			// Clicked on empty space: deselect all
			container.selectAllItems(false);
			container.refresh();
		}
		container.setToolBarState();
	}

	/**
	 * Handle pointer double click on an object at the given point.
	 *
	 * @param gc GestureContext providing context for the double click
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
		modifyItems.clear();
		modifying = false;
		gestureBefore = null;

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
			Point press = (dragPressPoint != null) ? new Point(dragPressPoint)
					: new Point(current.x - dx, current.y - dy);

			startItemModification(modifyItem, press, current, e, false);
			modifyItems.add(modifyItem);

			ItemModification primaryModification = modifyItem.getItemModification();
			if (modifyItem.isSelected() && modifyItem.isDraggable()
					&& primaryModification != null
					&& primaryModification.getType() == ItemModification.ModificationType.DRAG) {
				for (AItem selectedItem : container.getSelectedItems()) {
					if (selectedItem == modifyItem || !isEligibleGroupDragItem(selectedItem)) {
						continue;
					}
					startItemModification(selectedItem, press, current, e, true);
					modifyItems.add(selectedItem);
				}
			}

			// Remember how the items were before the gesture moves them, so
			// the whole gesture becomes one undo step.
			gestureBefore = ItemEdits.capture(modifyItems);
			gestureName = editName(primaryModification);
		}

		for (AItem itemBeingModified : modifyItems) {
			ItemModification mod = itemBeingModified.getItemModification();
			if (mod != null) {
				mod.setCurrentMousePoint(e.getPoint());
				itemBeingModified.modify();
			}
		}
	}

	/** The undo-menu name for a gesture, from what the primary item is doing. */
	private static String editName(ItemModification modification) {
		if (modification == null) {
			return "Edit";
		}
		return switch (modification.getType()) {
		case DRAG -> "Move";
		case RESIZE -> "Resize";
		case ROTATE -> "Rotate";
		};
	}

	/** Starts one item modification for the current pointer gesture. */
	private void startItemModification(AItem item, Point press, Point current,
			MouseEvent event, boolean forceDrag) {
		ItemModification modification = new ItemModification(item, container, press, current,
				event.isShiftDown(), event.isControlDown());
		item.setModification(modification);
		item.startModification();
		if (forceDrag) {
			modification.setType(ItemModification.ModificationType.DRAG);
		}
	}

	/** Returns whether a selected companion item may participate in a group drag. */
	private static boolean isEligibleGroupDragItem(AItem item) {
		return item != null && item.isDraggable() && item.isTrackable()
				&& item.getLayer() != null && !item.getLayer().isLocked();
	}

	@Override
	public void endDragObject(GestureContext gc) {

	    if (!modifyItems.isEmpty()) {
	        for (AItem item : modifyItems) {
	            if (!item.acceptModification(gc)) {
	                item.modificationRejected(gc);
	            }
	            item.stopModification();
	        }

	        // A rejected or zero-length gesture leaves the items as they were,
	        // which changedStates() leaves out, so only real changes are recorded.
	        if (gestureBefore != null) {
	            container.recordEdit(ItemEdits.changedStates(gestureName, container, modifyItems, gestureBefore));
	        }

	        container.setDirty(true);
	        container.refresh();
	    }

	    modifyItem = null;
	    modifyItems.clear();
	    modifying = false;
	    dragPressPoint = null;
	    gestureBefore = null;
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
		g.drawImage(buffer, 0, 0, gc.getCanvas());
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
		Point p = (gc != null) ? gc.getPressPoint() : null;
		if (p == null && gc != null && gc.getSourceEvent() != null) {
			p = gc.getSourceEvent().getPoint();
		}
		if (p == null) {
			return;
		}

		container.prepareToZoom();
		container.recenter(p);
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
		
		//some items may not be editable, so filter out non-editable items
		List<AItem> editableItems = new ArrayList<>();
		for (AItem item : selected) {
			if (item.isStyleEditable()) {
				editableItems.add(item);
			}
		}
		if (editableItems.isEmpty()) {
			java.awt.Toolkit.getDefaultToolkit().beep();
			return;
		}

		// text items are special - cannot style multiple at once
		// have their own editor

		if (editableItems.size() == 1 && editableItems.get(0) instanceof TextItem) {
			TextItem item = (TextItem) editableItems.get(0);
			List<ItemState> before = ItemEdits.capture(editableItems);
			item.edit();
			container.recordEdit(ItemEdits.changedStates("Edit Text", container, editableItems, before));
			return;
		}

		// seed from first selected item
		IStyled seed = editableItems.get(0).getStyleSafe();
		Styled edited = StyleEditorDialog.edit(container.getComponent(), seed, false);
		if (edited == null) {
			return;
		}

		List<ItemState> before = ItemEdits.capture(editableItems);
		for (AItem item : editableItems) {
			item.setStyle(edited.copy()); // avoid shared mutable style objects
			item.setDirty(true);
		}
		container.recordEdit(ItemEdits.changedStates("Style", container, editableItems, before));

		container.refresh();
	}

	@Override
	public void delete(GestureContext gc) {
		container.deleteSelectedItemsUndoably();
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
		recordCreated("Rectangle", CreationSupport.createRectangleItem(container.getAnnotationLayer(), bounds));
		container.setDirty(true);
		container.refresh();
	}

	@Override
	public void createEllipse(GestureContext gc, Rectangle bounds) {
		recordCreated("Ellipse", CreationSupport.createEllipseItem(container.getAnnotationLayer(), bounds));
		container.setDirty(true);
		container.refresh();
	}

	@Override
	public void createPolygon(GestureContext gc, Point[] vertices) {
		recordCreated("Polygon", CreationSupport.createPolygonItem(container.getAnnotationLayer(), vertices));
		container.setDirty(true);
		container.refresh();
	}

	@Override
	public void createPolyline(GestureContext gc, Point[] vertices) {
		recordCreated("Polyline", CreationSupport.createPolylineItem(container.getAnnotationLayer(), vertices));
		container.setDirty(true);
		container.refresh();
	}

	@Override
	public void createLine(GestureContext gc, Point start, Point end) {
		recordCreated("Line", CreationSupport.createLineItem(container.getAnnotationLayer(), start, end));
		container.setDirty(true);
		container.refresh();
	}

	@Override
	public void createRadArc(GestureContext gc, Point[] pp) {
	    if (pp == null || pp.length != 3) {
	        return;
	    }

	    recordCreated("Arc", CreationSupport.createRadArcItem(
	            container.getAnnotationLayer(),
	            pp[0], pp[1], pp[2]));

	    container.setDirty(true);
	    container.refresh();
	}
	@Override
	public void createTextItem(GestureContext gc, Point location) {
		recordCreated("Text", CreationSupport.createTextItem(container.getAnnotationLayer(), location));
		container.setDirty(true);
		container.refresh();
	}

	/**
	 * Records the creation of an item by one of the creation tools so it can be
	 * undone. Subclasses that create their own item types call this after the
	 * item has been created and configured.
	 *
	 * @param what a short name for the kind of item, such as "Line"; shown as
	 *             "Undo Create Line"
	 * @param item the new item; {@code null} (a cancelled creation) is ignored
	 */
	protected final void recordCreated(String what, AItem item) {
		if (item != null) {
			container.recordEdit(ItemEdits.created("Create " + what, container, item));
		}
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

	    // Plain click replaces the selection.
	    // Ctrl-click or Shift-click adds to the existing selection.
	    if (!e.isControlDown() && !e.isShiftDown()) {
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
