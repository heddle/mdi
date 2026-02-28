package edu.cnu.mdi.view.demo;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.ImageManager;
import edu.cnu.mdi.graphics.connection.ConnectionManager;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.item.PointItem;
import edu.cnu.mdi.properties.PropertyUtils;
import edu.cnu.mdi.ui.fonts.Fonts;

public class DeviceItem extends PointItem {
	
	// default icon size in pixels. Will change with zooming.
	public static final int DEVICESIZE = 48;

	// The device symbol represented by this item
	private EDeviceSymbol symbol;

	// instance number for this device
	private int instanceNumber;

	// map to keep track of device counts by symbol
	private static final Map<EDeviceSymbol, Integer> countMap = new HashMap<>();


	/**
	 * Create a device item with the given symbol and world location.
	 * @param layer the z layer to add the device to
	 * @param location the world location for the device
	 * @param symbol the device symbol to use
	 */
	public DeviceItem(Layer layer, Point2D.Double location, EDeviceSymbol symbol) {
		super(layer, location,
				PropertyUtils.RIGHTCLICKABLE, true,
				PropertyUtils.DRAGGABLE, true,
				PropertyUtils.ROTATABLE, false,
				PropertyUtils.RESIZABLE, false,
				PropertyUtils.DELETABLE, true,
				PropertyUtils.LOCKED, false,
				PropertyUtils.CONNECTABLE, true);
		
		this.symbol = symbol;
		incrementMapCount(symbol);
		this.instanceNumber = getMapCount(symbol);
		setDisplayName(symbol + " (" + instanceNumber + ")");

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
	
	@Override
	public void drawItem(Graphics g2, IContainer container) {
		int size = (int) Math.round(DEVICESIZE * container.approximateZoomFactor());
		icon = (ImageIcon) ImageManager.getInstance().loadUiIcon(symbol.iconPath, size, size); // ensure image manager is
		super.drawItem(g2, container);
		
		if (showName()) {
			Rectangle pxBounds = getBounds(container);
			int x = pxBounds.x + (pxBounds.width - size) / 2;
			int y = pxBounds.y + (pxBounds.height - size) / 2;
			drawDisplayName(g2, container, getDisplayName(),
					x, y, size, container.approximateZoomFactor());
		}
	}
	

	//draw the display name of the device, centered and underneath
	private void drawDisplayName(Graphics g2, IContainer container, String name,
			int xc, int yc, int size, double approxZoomFactor) {
		
		//use font delta to size the font from the basline font size, so it 
		// scales with zoom level
		
		int baseDelta = -2;
		int fontDelta = (int) Math.round(baseDelta + 4 * (approxZoomFactor - 1)); // adjust the multiplier as needed
		Font font = Fonts.plainFontDelta(fontDelta);
		g2.setFont(font);
		// measure the name to center it
		int nameWidth = g2.getFontMetrics().stringWidth(name);
		int x = xc + (size - nameWidth) / 2;
		int y = yc + size + g2.getFontMetrics().getAscent(); 
		g2.setColor(Color.black);
		g2.drawString(name, x, y);
				
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
		Point2D.Double wp = new Point2D.Double();
		container.localToWorld(p, wp);
		DeviceItem device = new DeviceItem(layer, wp, symbol);
		return device;
	}

	// see whether name should be displayed
	private boolean showName() {
		NetworkLayoutDemoView view = (NetworkLayoutDemoView) getView();
		return view.showNames();
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
