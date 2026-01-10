package edu.cnu.mdi.graphics.toolbar;

import java.awt.Cursor;
import java.awt.Point;

import edu.cnu.mdi.graphics.rubberband.Rubberband;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.Layer;

/**
 * Tool that creates a polygon item by vertex rubber-banding.
 * <p>
 * Drop-in replacement for the legacy {@code PolygonButton}.
 * </p>
 * <ul>
 * <li>Press starts {@link Rubberband.Policy#POLYGON} vertex capture.</li>
 * <li>Completion creates a polygon item in the container's annotation
 * list.</li>
 * <li>After creation: clears selection, resets to default tool, refreshes.</li>
 * </ul>
 *
 * @author heddle
 */
public class PolygonTool extends AbstractVertexRubberbandTool {

	/** Tool id used by {@link ToolController} for registration/selection. */
	public static final String ID = "polygon";

	/**
	 * Create a polygon tool.
	 * <p>
	 * Polygons require at least 2 vertices (matches the legacy check).
	 * </p>
	 */
	public PolygonTool() {
		super(2);
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public String toolTip() {
		return "Create a polygon";
	}

	@Override
	protected Cursor activeCursor() {
		return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
	}

	@Override
	protected Rubberband.Policy rubberbandPolicy() {
		return Rubberband.Policy.POLYGON;
	}

	@Override
	protected AItem createItem(Layer layer, Point[] vertices) {
		return DrawingToolSupport.createPolygonItem(layer, vertices);
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
