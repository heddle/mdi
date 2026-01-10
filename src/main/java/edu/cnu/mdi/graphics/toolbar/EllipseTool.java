package edu.cnu.mdi.graphics.toolbar;

import java.awt.Rectangle;

import edu.cnu.mdi.graphics.rubberband.Rubberband;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.Layer;

/**
 * Tool that creates an {@link edu.cnu.mdi.item.EllipseItem} by rubber-banding
 * an oval on the canvas.
 *
 * @author heddle
 */
public class EllipseTool extends AbstractRubberbandTool {

	/** Stable tool id used for registration and selection. */
	public static final String ID = "ellipse";

	/** Minimum pixel size required for creation. */
	private static final int MIN_SIZE = 3;

	/**
	 * Create the ellipse tool.
	 */
	public EllipseTool() {
		super(MIN_SIZE);
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public String toolTip() {
		return "Create an ellipse";
	}

	@Override
	protected Rubberband.Policy rubberbandPolicy() {
		return Rubberband.Policy.OVAL;
	}

	@Override
	protected AItem createItem(Layer layer, Rectangle bounds) {
		return DrawingToolSupport.createEllipseItem(layer, bounds);
	}

	@Override
	protected void configureItem(AItem item) {
		item.setRightClickable(true);
		item.setDraggable(true);
		item.setRotatable(true);
		item.setResizable(true);
		item.setDeletable(true);
		item.setLocked(false);
	}
}
