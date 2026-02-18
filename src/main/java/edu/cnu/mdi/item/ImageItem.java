package edu.cnu.mdi.item;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.awt.image.BufferedImage;
import java.util.Objects;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.world.WorldGraphicsUtils;

public class ImageItem extends RectangleItem {

	private BufferedImage image;

	/**
	 * Create a world image object.
	 *
	 * @param layer the list this item is on.
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
	 * @param g         the graphics context.
	 * @param container the graphical container being rendered.
	 */
	@Override
	public void drawItem(Graphics g, IContainer container) {
		// Keep this so _lastDrawnPolygon gets updated for selection handles.
		super.drawItem(g, container);

		if (image == null || _path == null) {
			return;
		}

		// Get the actual (possibly rotated) rectangle corners in WORLD coords.
		Point2D.Double[] wpoly = WorldGraphicsUtils.pathToWorldPolygon(_path);
		if (wpoly == null || wpoly.length < 4) {
			return;
		}

		WorldGraphicsUtils.drawImageOnQuad(g, image, wpoly, container);
	}

	private static Rectangle2D.Double createDefaultBounds(IContainer container, BufferedImage image) {
		Rectangle r = image.getRaster().getBounds();
		Rectangle2D.Double wr = container.getWorldSystem();
		// Center the image in the world system
		// and make it not larger that 80% of the world system
		// preserving aspect ratio
		double width = r.width;
		double height = r.height;
		double maxWidth = 0.8 * wr.width;
		double maxHeight = 0.8 * wr.height;
		double scaleX = width / maxWidth;
		double scaleY = height / maxHeight;
		double scale = Math.max(scaleX, scaleY);

		if (scale > 1.0) {
			width = width / scale;
			height = height / scale;
		}
		double x = wr.x + (wr.width - width) / 2;
		double y = wr.y + (wr.height - height) / 2;
		Rectangle2D.Double wbounds = new Rectangle2D.Double(x, y, width, height);
		return wbounds;
	}
}
