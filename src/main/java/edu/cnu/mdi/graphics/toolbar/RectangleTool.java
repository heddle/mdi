package edu.cnu.mdi.graphics.toolbar;

import java.awt.Rectangle;

import edu.cnu.mdi.container.IAnnotationSupport;
import edu.cnu.mdi.graphics.rubberband.Rubberband;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.Layer;

/**
 * Tool that creates a {@link edu.cnu.mdi.item.RectangleItem} by rubber-banding
 * a rectangle on a {@link DrawingContainer}.
 * <p>
 * This is the tool-based replacement for the legacy {@code RectangleButton}.
 * Behavior matches legacy:
 * </p>
 * <ul>
 *   <li>Uses {@link Rubberband.Policy#RECTANGLE}.</li>
 *   <li>Ignores tiny gestures (width/height &lt; 3 px).</li>
 *   <li>Creates the rectangle on the container's annotation list.</li>
 *   <li>Configures created items as right-clickable, draggable, rotatable,
 *       resizable, deletable, unlocked.</li>
 *   <li>Clears selection, resets to default tool, refreshes the container.</li>
 * </ul>
 *
 * @author heddle
 */
public class RectangleTool extends AbstractRubberbandTool {

    /** Tool id used by {@link ToolController}. */
    public static final String ID = "rectangle";

    /** Legacy minimum gesture size in pixels. */
    private static final int MIN_SIZE_PX = 3;

    /**
     * Create a rectangle tool with the legacy minimum size threshold.
     */
    public RectangleTool() {
        super(MIN_SIZE_PX);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String toolTip() {
        return "Create a rectangle";
    }

    @Override
    protected Rubberband.Policy rubberbandPolicy() {
        return Rubberband.Policy.RECTANGLE;
    }

    @Override
    protected AItem createItem(IAnnotationSupport owner, Layer layer, Rectangle bounds) {
        return owner.createRectangleItem(layer, bounds);
    }

    @Override
    protected void configureItem(AItem item) {
        // Match legacy RectangleButton defaults.
        item.setRightClickable(true);
        item.setDraggable(true);
        item.setRotatable(true);
        item.setResizable(true);
        item.setDeletable(true);
        item.setLocked(false);
    }
}
