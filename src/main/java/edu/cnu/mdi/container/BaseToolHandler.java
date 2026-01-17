package edu.cnu.mdi.container;

import java.awt.Component;
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
import edu.cnu.mdi.graphics.toolbar.AToolBar;
import edu.cnu.mdi.graphics.toolbar.IToolHandler;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.ItemModification;
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
	private BaseContainer container;
	
	//for modifying items
	private AItem modifyItem;
	private boolean modifying;

	
	/**
	 * Constructor.
	 * 
	 * @param container BaseContainer that owns this tool handler
	 */
	public BaseToolHandler(BaseContainer container) {
		Objects.requireNonNull(container, "container");
		this.container = container;
		
		MouseMotionListener mml = new MouseMotionListener() {
			@Override
			public void mouseMoved(MouseEvent e) {
				container.feedbackTrigger(e, false);
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				container.feedbackTrigger(e, true);
			}
		};
		container.getComponent().addMouseMotionListener(mml);
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
	public Object hitTest(AToolBar toolBar, Component canvas, Point p) {
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
	public void pointerClick(AToolBar toolBar, Component canvas, Point p, Object obj, MouseEvent e) {
		AItem item = container.getItemAtPoint(e.getPoint());
		
		if (item != null && item.isEnabled() && item.isTrackable()) {
			if (item.isRightClickable() && (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e))) {
				// Show popup menu
				JPopupMenu menu = item.getPopupMenu();
				if (menu != null) {
					menu.show(canvas, e.getX(), e.getY());
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
	public void pointerDoubleClick(AToolBar toolBar, Component canvas, Point p, Object obj, MouseEvent e) {
		AItem item = container.getItemAtPoint(e.getPoint());
		if (item != null && item.isEnabled() && item.isDoubleClickable()) {
			item.doubleClicked(e);
			return;
		}
	}

	@Override
	public void pointerRubberbanding(AToolBar toolBar, Component canvas, Rectangle bounds) {
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
	public void beginDragObject(AToolBar toolBar, Component canvas, Object obj, MouseEvent e) {

		modifyItem = null;
		modifying = false;
		
		if (obj instanceof AItem) {
			AItem item = (AItem) obj;
			if (item.isEnabled() && !item.isLocked() && item.isDraggable()) {
			}
		}
	}

	@Override
	public void dragObjectBy(AToolBar toolBar, Component canvas, Object obj, int dx, int dy, MouseEvent e) {
			if (obj instanceof AItem) {
			AItem item = (AItem) obj;
			if (item.isTrackable()) {
				if (!modifying) {
					// first time through
					modifyItem = item;
					modifying = true;
					Point p = e.getPoint();
					ItemModification mod = new ItemModification(modifyItem, container, p, p,
							e.isShiftDown(), e.isControlDown());
					modifyItem.setModification(mod);
					modifyItem.startModification();
					
				} //not modifying
				if (modifying && modifyItem != null) {
					ItemModification mod = modifyItem.getItemModification();
					System.out.println("Mod type item: " + mod.getType());
					if (mod != null) {
						mod.setCurrentMousePoint(e.getPoint());
					}
					modifyItem.modify();
				}
			}
		} //instanceof AItem
	} // dragObjectBy

	@Override
	public void endDragObject(AToolBar toolBar, Component canvas, Object obj, MouseEvent e) {
		
		if (modifyItem != null) {
			modifyItem.stopModification();
			modifyItem.setModification(null);
		}
		modifyItem = null;
		modifying = false;
	}

	@Override
	public boolean doNotDrag(AToolBar toolBar, Component canvas, Object obj, MouseEvent e) {
		return false;
	}
	

	@Override
	public void boxZoomRubberbanding(AToolBar toolBar, Component canvas, Rectangle bounds) {
		container.rubberBanded(bounds);
	}


	@Override
	public void panStartDrag(AToolBar toolBar, Component canvas, Point start) {
		base = GraphicsUtils.getComponentImage(canvas);
		buffer = GraphicsUtils.getComponentImageBuffer(canvas);
	}

	@Override
	public void panUpdateDrag(AToolBar toolBar, Component canvas, Point start, Point previous, Point current) {
		int totalDx = current.x - start.x;
		int totalDy = current.y - start.y;

		Graphics gg = buffer.getGraphics();
		gg.setColor(canvas.getBackground());
		gg.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());
		gg.drawImage(base, totalDx, totalDy, canvas);
		gg.dispose();

		Graphics g = canvas.getGraphics();
		g.drawImage(buffer, 0, 0, canvas);
		g.dispose();
	}

	@Override
	public void panDoneDrag(AToolBar toolBar, Component canvas, Point start, Point end) {
		int dx = end.x - start.x;
		int dy = end.y - start.y;
		container.pan(dx, dy);
		base = null;
		buffer = null;
		canvas.repaint();
	}

	@Override
	public void magnifyStartMove(AToolBar toolBar, Component canvas, Point start, MouseEvent e) {
		BaseView view = container.getView();
		if (view != null) {
			view.handleMagnify(e);
		}
	}

	@Override
	public void magnifyUpdateMove(AToolBar toolBar, Component canvas, Point start, Point p, MouseEvent e) {
		BaseView view = container.getView();
		if (view != null) {
			view.handleMagnify(e);
		}
	}

	@Override
	public void magnifyDoneMove(AToolBar toolBar, Component canvas, Point start, Point end, MouseEvent e) {
		MagnifyWindow.closeMagnifyWindow();
		toolBar.resetDefaultToggleButton();
	}

	@Override
	public void recenter(AToolBar toolBar, Component canvas, Point center) {
		container.prepareToZoom();
		container.recenter(center);
		container.refresh();
	}

	@Override
	public void zoomIn(AToolBar toolBar, Component canvas) {
		container.scale(ZOOM_FACTOR);
	}

	@Override
	public void zoomOut(AToolBar toolBar, Component canvas) {
		container.scale(1.0 / ZOOM_FACTOR);
	}

	@Override
	public void undoZoom(AToolBar toolBar, Component canvas) {
		container.undoLastZoom();
	}

	@Override
	public void resetZoom(AToolBar toolBar, Component canvas) {
		container.restoreDefaultWorld();
	}

	@Override
	public void styleEdit(AToolBar toolBar, Component canvas) {
		List<AItem> selected = container.getSelectedItems();

		if (selected == null || selected.isEmpty()) {
			java.awt.Toolkit.getDefaultToolkit().beep();
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

		container.refresh();	}

	@Override
	public void delete(AToolBar toolBar, Component canvas) {
		container.deleteSelectedItems();
		toolBar.resetDefaultToggleButton();
		container.refresh();
	}
	
	@Override
	public void createConnection(AToolBar toolBar, Component canvas, Point start, Point end) {
		AItem item1 = container.getItemAtPoint(start);
		AItem item2 = container.getItemAtPoint(end);
		if (ConnectionManager.getInstance().canConnect(item1, item2)) {
			ConnectionManager.getInstance().connect(container.getConnectionLayer(), item1, item2);
			container.refresh();
			
		}
	}

	
	@Override
	public boolean approveConnectionPoint(AToolBar toolBar, Component canvas, Point p) {
		AItem item = container.getItemAtPoint(p);
		if (item != null) { 
			boolean enabled = item.isEnabled();
            boolean connectable = item.isConnectable();
			return enabled && connectable;
		}
		return false;
	}

	@Override
	public void createRectangle(AToolBar toolBar, Component canvas, Rectangle bounds) {
		CreationSupport.createRectangleItem(container.getAnnotationLayer(), bounds);
	}

	
	@Override
	public void captureImage(AToolBar toolBar, Component canvas) {
		TakePicture.takePicture(canvas);
	}

	
	@Override
	public void print(AToolBar toolBar, Component canvas) {
		PrintUtils.printComponent(canvas);
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
	

}

