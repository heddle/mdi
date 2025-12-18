package edu.cnu.mdi.view.demo;

import java.awt.Cursor;
import java.awt.event.MouseEvent;

import edu.cnu.mdi.graphics.toolbar.ITool;
import edu.cnu.mdi.graphics.toolbar.ToolContext;

public class PlaceDeviceTool implements ITool {
	
    private  EDeviceSymbol symbol;

    public PlaceDeviceTool(EDeviceSymbol symbol) {
        this.symbol = java.util.Objects.requireNonNull(symbol);
    }
    
    @Override
    public Cursor cursor(ToolContext ctx) {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    @Override
    public void mouseClicked(ToolContext ctx, MouseEvent e) {
        // Convert click to world coords if needed (depends on your ctx API)
        // Point2D world = ctx.container().localToWorld(e.getPoint());

        // Create + add your item at click location
        // ctx.container().addItem(new DeviceItem(symbol, world));
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
