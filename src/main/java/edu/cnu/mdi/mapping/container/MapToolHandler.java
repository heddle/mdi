package edu.cnu.mdi.mapping.container;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;

import edu.cnu.mdi.container.BaseContainer;
import edu.cnu.mdi.container.BaseToolHandler;
import edu.cnu.mdi.graphics.style.IStyled;
import edu.cnu.mdi.graphics.toolbar.GestureContext;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.mapping.item.MapLineItem;

public class MapToolHandler extends BaseToolHandler {
	
	MapContainer container;

	public MapToolHandler(BaseContainer container) {
		super(container);
		this.container = (MapContainer) container;
	}
	
	@Override
	public void createLine(GestureContext gc, Point start, Point end) {
		Point2D.Double ll1 = new Point2D.Double();
		Point2D.Double ll2 = new Point2D.Double();
		container.localToLatLon(start, ll1);
		container.localToLatLon(end, ll2);
		AItem item = new MapLineItem(container.getAnnotationLayer(), ll1, ll2);
		
		defaultConfigureItem(item);
		
		item.setDisplayName("GC line of length");
		IStyled style;
		style = item.getStyle();
		style.setLineColor(Color.red);
		style.setLineWidth(2.0f);
		container.setDirty(true);
		container.refresh();
	}

	// Apply default configuration to the given item.
	private static void defaultConfigureItem(AItem item) {
		item.setRightClickable(true);
		item.setDraggable(true);
		item.setSelectable(true);
		item.setResizable(true);
		item.setRotatable(true);
		item.setDeletable(true);
		item.setLocked(false);
	}


}
