package edu.cnu.mdi.view.demo;

import java.awt.Cursor;
import java.awt.event.MouseEvent;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.toolbar.ITool;
import edu.cnu.mdi.graphics.toolbar.ToolContext;
import edu.cnu.mdi.item.Layer;

public class PlaceDeviceTool implements ITool {

	// The device symbol to place
    private  EDeviceSymbol symbol;

    private Layer deviceLayer;


    public PlaceDeviceTool(Layer deviceLayer, EDeviceSymbol symbol) {
    	this.deviceLayer = deviceLayer;
        this.symbol = java.util.Objects.requireNonNull(symbol);
    }

    @Override
    public Cursor cursor(ToolContext ctx) {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

	@Override
	public void mouseClicked(ToolContext ctx, MouseEvent e) {
		java.util.Objects.requireNonNull(ctx, "ctx");
		if (e.getButton() == MouseEvent.BUTTON1) {
			IContainer container = ctx.container();
			DeviceItem.createDeviceItem(deviceLayer, e.getPoint(), symbol);
			container.refresh();
		}

	}

	@Override
	public String id() {
		return symbol.name();
	}

	@Override
	public String toolTip() {
		return symbol.name();
	}

}
