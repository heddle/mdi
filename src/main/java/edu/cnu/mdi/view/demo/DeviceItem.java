package edu.cnu.mdi.view.demo;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.ImageManager;
import edu.cnu.mdi.graphics.connection.ConnectionManager;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.item.RectangleItem;

public class DeviceItem extends RectangleItem {
	// icon size in pixels
	public static final int DEVICESIZE = 48;

	// The device symbol represented by this item
	private EDeviceSymbol symbol;

	// instance number for this device
	private int instanceNumber;

	// map to keep track of device counts by symbol
	private static final Map<EDeviceSymbol, Integer> countMap = new HashMap<>();

	public DeviceItem(Layer layer, Double wr, EDeviceSymbol symbol) {
		super(layer, wr);
		this.symbol = symbol;
		incrementMapCount(symbol);
		this.instanceNumber = getMapCount(symbol);
		setDisplayName(symbol + " (" + instanceNumber + ")");

		// configure the device item
		setRightClickable(true);
		setDraggable(true);
		setRotatable(false);
		setResizable(false);
		setDeletable(true);
		setLocked(false);
		setConnectable(true);
	}

	// Increment the count for the given device symbol in the map
	private void incrementMapCount(EDeviceSymbol symbol) {
		int count = getMapCount(symbol);
		countMap.put(symbol, count + 1);
	}

	// Get the count for the given device symbol from the map
	private int getMapCount(EDeviceSymbol symbol) {
		return countMap.getOrDefault(symbol, 0);
	}

	/**
	 * Draw the device icon centered in the item's bounds.
	 */
	@Override
	public void drawItem(Graphics g2, IContainer container) {
		Rectangle pxBounds = getBounds(container);

		// calculate icon size (and item bounds) based on approximate zoom factor

		int size = (int) Math.round(DEVICESIZE * container.approximateZoomFactor());
		// get the icon for the device
		Icon icon = ImageManager.getInstance().loadUiIcon(symbol.iconPath, size, size); // ensure image manager is
																						// initialized

		if (icon != null) {
			int x = pxBounds.x + (pxBounds.width - size) / 2;
			int y = pxBounds.y + (pxBounds.height - size) / 2;

			// Paint the Icon directly (works for FlatSVGIcon, ImageIcon, etc.)
			// Prefer a real component as the paint context.
			java.awt.Component c = (container != null) ? container.getComponent() : null;
			icon.paintIcon(c, g2, x, y);
			return;
		}

		super.drawItem(g2, container);
	}

	/**
	 * Create a device item centered at given point in local container coordinates
	 *
	 * @param layer  the z layer to add the device to
	 * @param p      the point in local screen coordinates
	 * @param symbol the device symbol to use
	 * @return the created device item
	 */
	public static DeviceItem createDeviceItem(Layer layer, Point p, EDeviceSymbol symbol) {
		IContainer container = layer.getContainer();
		Rectangle2D.Double wr = createDeviceBounds(container, p);
		DeviceItem device = new DeviceItem(layer, wr, symbol);
		return device;
	}

	// Create world rectangle for device centered at given point to serve as world
	// bounds
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
		feedbackStrings.add("$yellow$" + getDisplayName());

		// list items connected to this device
		Set<AItem> connectedItems = ConnectionManager.getInstance().getConnectedItems(this);
		if (!connectedItems.isEmpty()) {
			feedbackStrings.add("Connected to:");
			for (AItem item : connectedItems) {
				feedbackStrings.add(" - " + item.getDisplayName());
			}
		}

	}

}
