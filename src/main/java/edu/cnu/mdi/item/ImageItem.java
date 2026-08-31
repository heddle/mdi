package edu.cnu.mdi.item;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.awt.image.BufferedImage;
import java.util.Objects;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.world.WorldGraphicsUtils;

/**
 * A {@link RectangleItem} that displays a {@link BufferedImage} scaled to its
 * bounds, with no fill or outline drawn for the rectangle itself.
 *
 * <p>All standard interactions are enabled by default (draggable, selectable,
 * resizable, rotatable, deletable, right-clickable, unlocked). If no explicit
 * bounds are given, the item is sized and positioned to the image's natural
 * dimensions, centered in the container.</p>
 */
public class ImageItem extends RectangleItem {

	private BufferedImage image;

	/**
	 * Create a world image object.
	 *
	 * @param layer the z layer this item is on.
	 * @param wr    the initial bounds of the item. If null, the image size is used.
	 * @param image the image to display.
	 */
	public ImageItem(Layer layer, Double wr, BufferedImage image) {
		super(layer, wr == null ? createDefaultBounds(layer.getContainer(), image) : wr);
		Objects.requireNonNull(image, "Image cannot be null");
		this.image = image;
		this.setRightClickable(true);
		this.setDraggable(true);
		this.setSelectable(true);
		this.setResizable(true);
		this.setRotatable(true);
		this.setDeletable(true);
		this.setLocked(false);
		getStyle().setFillColor(null);
		getStyle().setLineColor(null);
	}

	/**
	 * Custom drawer for the item.
	 *
	 * @param g2         the graphics context.
	 * @param container the graphical container being rendered.
	 */
	@Override
	public void drawItem(Graphics2D g2, IContainer container) {
		// Keep this so _lastDrawnPolygon gets updated for selection handles.
		super.drawItem(g2, container);

		if (image == null || _path == null) {
			return;
		}

		// Get the actual (possibly rotated) rectangle corners in WORLD coords.
		Point2D.Double[] wpoly = WorldGraphicsUtils.pathToWorldPolygon(_path);
		if (wpoly == null || wpoly.length < 4) {
			return;
		}

		WorldGraphicsUtils.drawImageOnQuad(g2, image, wpoly, container);
	}

	// get the bounds of the image in world coordinates, centered in the container
	private static Rectangle2D.Double createDefaultBounds(IContainer container, BufferedImage image) {
		Objects.requireNonNull(container, "container cannot be null");
		Objects.requireNonNull(image, "image cannot be null");

		Rectangle cb = container.getComponent().getBounds();

		int cw = Math.max(1, cb.width);
		int ch = Math.max(1, cb.height);

		// Center in screen coordinates.
		int cx = cw / 2;
		int cy = ch / 2;

		// Use the image's native pixel size as the initial displayed size.
		// This preserves readability. Large images may extend beyond the view,
		// but that is better than silently downsampling them into illegibility.
		int iw = Math.max(1, image.getWidth());
		int ih = Math.max(1, image.getHeight());

		int left   = cx - iw / 2;
		int right  = left + iw;
		int top    = cy - ih / 2;
		int bottom = top + ih;

		Point2D.Double wtl = new Point2D.Double();
		Point2D.Double wbr = new Point2D.Double();

		container.localToWorld(new java.awt.Point(left,  top),    wtl);
		container.localToWorld(new java.awt.Point(right, bottom), wbr);

		double wx = Math.min(wtl.x, wbr.x);
		double wy = Math.min(wtl.y, wbr.y);
		double ww = Math.abs(wbr.x - wtl.x);
		double wh = Math.abs(wbr.y - wtl.y);

		return new Rectangle2D.Double(wx, wy, ww, wh);
	}
}
