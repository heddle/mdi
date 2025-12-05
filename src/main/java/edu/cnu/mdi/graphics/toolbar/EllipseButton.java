package edu.cnu.mdi.graphics.toolbar;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import edu.cnu.mdi.container.DrawingContainer;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.rubberband.IRubberbanded;
import edu.cnu.mdi.graphics.rubberband.Rubberband;
import edu.cnu.mdi.item.AItem;

@SuppressWarnings("serial")
public class EllipseButton extends ToolBarToggleButton implements IRubberbanded {

	/**
	 * Create a button for creating ellipses by rubberbanding.
	 *
	 * @param container the container using this button.
	 */
	public EllipseButton(IContainer container) {
		super(container, "images/ellipse.gif", "Create an ellipse");
	}

	@Override
	public void mousePressed(MouseEvent mouseEvent) {
		if (rubberband == null) {
			rubberband = new Rubberband(container, this, Rubberband.Policy.OVAL);
			rubberband.setActive(true);
			rubberband.startRubberbanding(mouseEvent.getPoint());
		}
	}

	/**
	 * Notification that rubber banding is finished.
	 */
	@Override
	public void doneRubberbanding() {
		Rectangle b = rubberband.getRubberbandBounds();
		rubberband = null;
		// create an ellipse item

		if ((b.width < 3) || (b.height < 3)) {
			return;
		}

		DrawingContainer dc	= (DrawingContainer) container;

		AItem item = dc.createEllipseItem(container.getAnnotationList(), b);
		if (item != null) {
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
