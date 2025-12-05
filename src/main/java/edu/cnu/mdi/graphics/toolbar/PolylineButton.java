/**
 *
 */
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
 *
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
@SuppressWarnings("serial")
public class PolylineButton extends ToolBarToggleButton implements IRubberbanded {

	/**
	 * Create a button for creating a polygon.
	 *
	 * @param container the owner container.
	 */
	public PolylineButton(IContainer container) {
		super(container, "images/polyline.gif", "Create a polyline");
	}

	/**
	 * The mouse has been pressed, start rubber banding.
	 *
	 * @param mouseEvent the causal mouse event.
	 */
	@Override
	public void mousePressed(MouseEvent mouseEvent) {
		if (rubberband == null) {
			rubberband = new Rubberband(container, this, Rubberband.Policy.POLYLINE);
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

		AItem item = dc.createPolylineItem(container.getAnnotationList(), pp);
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
