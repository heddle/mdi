package edu.cnu.mdi.view.demo;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.util.List;

import javax.swing.ImageIcon;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.ImageManager;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.item.RectangleItem;

public class DeviceItem extends RectangleItem {
	// icon size in pixels
	public static final int DEVICESIZE = 48;
	
	// The device symbol represented by this item
	private EDeviceSymbol symbol;

	private ImageIcon icon;

	public DeviceItem(Layer layer, Double wr, EDeviceSymbol symbol) {
		super(layer, wr);
		this.symbol = symbol;

		//get the icon for the device
		icon = ImageManager.getInstance().loadImageIcon(symbol.iconPath, DEVICESIZE, DEVICESIZE); //ensure image manager is initialized

		// configure the device item
		setRightClickable(true);
		setDraggable(true);
		setRotatable(false);
		setResizable(false);
		setDeletable(true);
		setLocked(false);
		setConnectable(true);
	}

	/**
	 * Draw the device icon centered in the item's bounds
	 */
	@Override
	public void drawItem(Graphics2D g2, IContainer container) {
		Rectangle pxBounds = getBounds(container);
		if (icon != null) {
			int w = icon.getIconWidth();
			int h = icon.getIconHeight();
			int x = pxBounds.x + (pxBounds.width - w) / 2;
			int y = pxBounds.y + (pxBounds.height - h) / 2;
			g2.drawImage(icon.getImage(), x, y, null);
		} else {
			super.drawItem(g2, container);
		}
	}
/**
 * Create a device item centered at given point in local container coordinates
 * @param layer the z layer to add the device to
 * @param p the point in local screen coordinates
 * @param symbol the device symbol to use
 * @return the created device item
 */
	public static DeviceItem createDeviceItem(Layer layer, Point p, EDeviceSymbol symbol) {
		IContainer container = layer.getContainer();
		Rectangle2D.Double wr = createDeviceBounds(container, p);
		DeviceItem device = new DeviceItem(layer, wr, symbol);
		return device;
	}

	// Create world rectangle for device centered at given point to serve as world bounds
	private static Rectangle2D.Double createDeviceBounds(IContainer container, Point p) {
		Rectangle pxBounds = new Rectangle(p.x - DEVICESIZE / 2, p.y - DEVICESIZE / 2, DEVICESIZE, DEVICESIZE);
		Rectangle2D.Double wr = new Rectangle2D.Double();
		container.localToWorld(pxBounds, wr);
		return wr;
	}
	
	/**
	 * Add any appropriate feedback.
	 *
	 * @param container       the Base container.
	 * @param pp              the mouse location.
	 * @param wp              the corresponding world point.
	 * @param feedbackStrings the List of feedback strings to add to.
	 */
	@Override
	public void getFeedbackStrings(IContainer container, Point pp, Point2D.Double wp, List<String> feedbackStrings) {

		// check if the point is inside this item. No feedback if not contained.
		if (!contains(container, pp)) {
			return;
		}

		// add device type feedback in yellow (to show how)
		feedbackStrings.add("$yellow$Device: " + symbol);

	}


}
