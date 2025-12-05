package edu.cnu.mdi.graphics.toolbar;

import java.awt.Point;
import java.awt.event.MouseEvent;

import edu.cnu.mdi.container.DrawingContainer;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.rubberband.IRubberbanded;
import edu.cnu.mdi.graphics.rubberband.Rubberband;
import edu.cnu.mdi.item.AItem;

/**
 * @author heddle
 */
@SuppressWarnings("serial")
public class PolygonButton extends ToolBarToggleButton implements IRubberbanded {

	/**
	 * Create a button for creating a polygon.
	 *
	 * @param container the owner container.
	 */
	public PolygonButton(IContainer container) {
		super(container, "images/polygon.gif", "Create a polygon");
	}

	/**
	 * The mouse has been pressed, start rubber banding.
	 *
	 * @param mouseEvent the causal mouse event.
	 */
	@Override
	public void mousePressed(MouseEvent mouseEvent) {
		if (rubberband == null) {
			rubberband = new Rubberband(container, this, Rubberband.Policy.POLYGON);
			rubberband.setActive(true);
			rubberband.startRubberbanding(mouseEvent.getPoint());
		}
	}

	/**
	 * Notification that rubber banding is finished.
	 */
	@Override
	public void doneRubberbanding() {
		Point pp[] = rubberband.getRubberbandVertices();
		rubberband = null;

		if ((pp == null) || (pp.length < 2)) {
			return;
		}

		DrawingContainer dc	= (DrawingContainer) container;

		AItem item = dc.createPolygonItem(container.getAnnotationList(), pp);
		if (item != null) {
			item.setRightClickable(true);
			item.setDraggable(true);
			item.setRotatable(true);
			item.setResizable(true);
			item.setDeletable(true);
			item.setLocked(false);
		}
		container.selectAllItems(false);
		container.getToolBar().resetDefaultSelection();
		container.refresh();

	}
}
