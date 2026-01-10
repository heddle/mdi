package edu.cnu.mdi.container;

import java.awt.Component;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import edu.cnu.mdi.feedback.FeedbackControl;
import edu.cnu.mdi.feedback.FeedbackPane;
import edu.cnu.mdi.graphics.drawable.IDrawable;
import edu.cnu.mdi.graphics.toolbar.IContainerToolBar;
import edu.cnu.mdi.graphics.world.WorldPolygon;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.view.BaseView;

/**
 * A 2D drawing container with a world coordinate system and conversion to pixel
 * (screen) coordinates. Implementations are typically Swing components that
 * render one or more {@link IDrawable}s.
 */
public interface IContainer {

	/**
	 * The factor used for fixed zooms.
	 */
	public static final double FIXED_ZOOM_FACTOR = 0.85;

	/**
	 * Add a z layer for containing items rendered on this container..
	 *
	 * @param layer the layer to add.
	 */
	public void addLayer(Layer layer);

	/**
	 * Get the annotation layer for this container.
	 *
	 * @return the annotation layer for this container. All drawing tools draw on
	 *         the annotation layer, which is kept on top.
	 */
	public Layer getAnnotationLayer();

	/**
	 * Get the connection layer for this container.
	 *
	 * @return the connection layer for this container. Connection items are drawn
	 *         on first, i.e. connections are under other items.
	 */
	public Layer getConnectionLayer();

	/**
	 * Get the default user content layer for this container.
	 * 
	 * @return the default user content layer (never null after construction)
	 */
	public Layer getDefaultLayer();

	/**
	 * Gets an z layer by name. Do not use for the annotation layer or connection
	 * layer-- for that use getAnnotationLayer() or getConnectionLayer().
	 *
	 * @param name the name of the layer.
	 * @return the z layer, or <code>null</code>.
	 */
	public Layer getLayerByName(String name);

	/**
	 * This converts a screen or pixel point to a world point.
	 *
	 * @param pp contains the local (screen-pixel) point.
	 * @param wp will hold the resultant world point.
	 */
	public void localToWorld(Point pp, Point2D.Double wp);

	/**
	 * This converts a world point to a screen or pixel point.
	 *
	 * @param pp will hold the resultant local (screen-pixel) point.
	 * @param wp contains world point.
	 */
	public void worldToLocal(Point pp, Point2D.Double wp);

	/**
	 * This converts a world rectangle to a screen or pixel rectangle.
	 *
	 * @param r  will hold the resultant local (screen-pixel) rectangle.
	 * @param wr contains the world rectangle.
	 */
	public void worldToLocal(Rectangle r, Rectangle.Double wr);

	/**
	 * This converts a screen or local rectangle to a world rectangle.
	 *
	 * @param r  contains the local (screen-pixel) rectangle.
	 * @param wr will hold the resultant world rectangle.
	 */
	public void localToWorld(Rectangle r, Rectangle.Double wr);

	/**
	 * This converts a world polygon to a screen or pixel polygon.
	 *
	 * @param polygon      will hold the resultant local (screen-pixel) polygon.
	 * @param worldPolygon contains the world polygon.
	 */
	public void worldToLocal(Polygon polygon, WorldPolygon worldPolygon);

	/**
	 * This converts a screen or local polygon to a world polygon.
	 *
	 * @param polygon      contains the local (screen-pixel) polygon.
	 * @param worldPolygon will hold the resultant world polygon.
	 */
	public void localToWorld(Polygon polygon, WorldPolygon worldPolygon);

	/**
	 * This converts a world point to a screen or pixel point.
	 *
	 * @param pp will hold the resultant local (screen-pixel) point.
	 * @param wx the world x coordinate.
	 * @param wy the world y coordinate.
	 */
	public void worldToLocal(Point pp, double wx, double wy);

	/**
	 * Pan the container.
	 *
	 * @param dh the horizontal step in pixels.
	 * @param dv the vertical step in pixels.
	 */
	public void pan(int dh, int dv);

	/**
	 * Recenter the container at the point of a click.
	 *
	 * @param pp the point in question. It will be the new center.
	 */
	public void recenter(Point pp);

	/**
	 * Begin preparations for a zoom.
	 */
	public void prepareToZoom();

	/**
	 * Restore the default world. This gets us back to the original zoom level.
	 */
	public void restoreDefaultWorld();

	/**
	 * Reset the world system to a new value. Resets the default and previous world
	 * systems as well.
	 * 
	 * @param worldSystem the new world system.
	 */
	public void resetWorldSystem(Rectangle2D.Double worldSystem);

	/**
	 * Convenience routine to scale the container.
	 *
	 * @param scaleFactor the scale factor.
	 */
	public void scale(double scaleFactor);

	/**
	 * Undo that last zoom.
	 */
	public void undoLastZoom();

	/**
	 * This is called when we have completed a rubber banding. pane.
	 *
	 * @param b The rubber band bounds.
	 */

	public void rubberBanded(Rectangle b);

	/**
	 * Find an item, if any, at the point.
	 *
	 * @param lp The pixel point in question.
	 * @return the topmost satisfying item, or null.
	 */
	public AItem getItemAtPoint(Point lp);

	/**
	 * Obtain a collection of all enclosed items across all lists.
	 *
	 * @param rect the rectangle in question.
	 * @return all items on all item lists enclosed by the rectangle.
	 */

	public ArrayList<AItem> getEnclosedItems(Rectangle rect);

	/**
	 * Find all items, if any, at the point.
	 *
	 * @param lp the pixel point in question.
	 * @return all items across all item lists that contain the given point. It may
	 *         be an empty vector, but it won't be <code>null</null>.
	 */
	public ArrayList<AItem> getItemsAtPoint(Point lp);

	/**
	 * Check whether at least one item on any item list is selected.
	 *
	 * @return <code>true</code> if at least one item on any item list is selected.
	 */
	public boolean anySelectedItems();

	/**
	 * Delete all selected items, across all item lists.
	 *
	 * @param container the container they lived on.
	 */
	public void deleteSelectedItems();

	/**
	 * Get all selected items, across all item lists.
	 *
	 * @param container the container they lived on.
	 */
	public List<AItem> getSelectedItems();

	/**
	 * Select or deselect all items, across all item lists.
	 *
	 * @param select the selection flag.
	 */
	public void selectAllItems(boolean select);

	/**
	 * Zooms to the specified area.
	 *
	 * @param xmin minimum x coordinate.
	 * @param xmax maximum x coordinate.
	 * @param ymin minimum y coordinate.
	 * @param ymax maximum y coordinate.
	 */
	public void zoom(final double xmin, final double xmax, final double ymin, final double ymax);

	/**
	 * Get this container's tool bar interface.
	 *
	 * @return this container's tool bar, or <code>null</code>.
	 */
	public IContainerToolBar getToolBar();

	/**
	 * Set this container's tool bar.
	 *
	 * @param toolBar the new toolbar interface.
	 */
	public void setToolBar(IContainerToolBar toolBar);

	/**
	 * Convenience method to update the location string in the toolbar.
	 *
	 * @param mouseEvent the causal event.
	 * @param dragging   <code>true</code> if we are dragging
	 */
	public void locationUpdate(MouseEvent mouseEvent, boolean dragging);

	/**
	 * Get the view (internal frame) that holds this container.
	 *
	 * @return the view (internal frame) that holds this container.
	 */
	public BaseView getView();

	/**
	 * Set the container's view.
	 *
	 * @param view the view to set.
	 */
	public void setView(BaseView view);

	/**
	 * Sets the feedback pane. This is an optional alternative to a HUD.
	 *
	 * @param feedbackPane the feedback pane.
	 */
	public void setFeedbackPane(FeedbackPane feedbackPane);

	/**
	 * Get the optional feedback pane.
	 *
	 * @return the feedbackPane
	 */
	public FeedbackPane getFeedbackPane();

	/**
	 * Return the object that controls the container's feedback. You can and and
	 * remove feedback providers using this object.
	 *
	 * @return the object that controls the container's feedback.
	 */
	public FeedbackControl getFeedbackControl();

	/**
	 * Convenience method for setting the dirty flag for all items on all item
	 * lists.
	 *
	 * @param dirty the new value of the dirty flag.
	 */
	public void setDirty(boolean dirty);

	/**
	 * Refresh the container.
	 */
	public void refresh();

	/**
	 * Get the underlying JComponent
	 *
	 * @return the underlying component
	 */
	public Component getComponent();

	/**
	 * Get the background image.
	 *
	 * @return the fully painted background image.
	 */
	public BufferedImage getImage();

	/**
	 * Set the after-draw drawable for this container.
	 *
	 * @param afterDraw the new after-draw drawable.
	 */
	public void setAfterDraw(IDrawable afterDraw);

	/**
	 * Set the before-draw drawable.
	 *
	 * @param beforeDraw the new before-draw drawable.
	 */
	public void setBeforeDraw(IDrawable beforeDraw);

	/**
	 * Get the current world system
	 *
	 * @return the world system
	 */
	public Rectangle2D.Double getWorldSystem();

	/**
	 * Set the world system (does not cause redraw)
	 *
	 * @param wr the new world system
	 */
	public void setWorldSystem(Rectangle2D.Double wr);

	public boolean isStandardPanning();

	public void setStandardPanning(boolean standardPanning);

	/**
	 * Get the approximate zoom factor based on the current and default world
	 * systems.
	 *
	 * @return the approximate zoom factor
	 */
	public double approximateZoomFactor();

}
