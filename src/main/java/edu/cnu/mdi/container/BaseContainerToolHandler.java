package edu.cnu.mdi.container;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.SwingUtilities;

import edu.cnu.mdi.component.MagnifyWindow;
import edu.cnu.mdi.experimental.AToolBar;
import edu.cnu.mdi.experimental.DefaultToolHandler;
import edu.cnu.mdi.graphics.style.IStyled;
import edu.cnu.mdi.graphics.style.Styled;
import edu.cnu.mdi.graphics.style.ui.StyleEditorDialog;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.view.BaseView;

public class BaseContainerToolHandler extends DefaultToolHandler {
	
	// Zoom factor for each zoom in/out action
	private static final double ZOOM_FACTOR = 0.8;
	
	private BaseContainer container;
	
	public BaseContainerToolHandler(BaseContainer container) {
		Objects.requireNonNull(container, "container");
		this.container = container;
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
			selectItemsFromClick(item, e);
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
	public void boxZoomRubberbanding(AToolBar toolBar, Component canvas, Rectangle bounds) {
		container.rubberBanded(bounds);
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
