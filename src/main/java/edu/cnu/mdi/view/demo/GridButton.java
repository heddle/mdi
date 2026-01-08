package edu.cnu.mdi.view.demo;

import java.awt.Point;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.toolbar.ToolActionButton;
import edu.cnu.mdi.graphics.toolbar.ToolContext;

public class GridButton extends ToolActionButton {

	// the parent view being controlled
	private NetworkLayoutDemoView view;

	public GridButton(NetworkLayoutDemoView view) {
		super("images/svg/grid.svg", "Snap to Grid");
		this.view = view;
	}

	@Override
	protected void perform(ToolContext ctx) {
	    int gridSize = view.getGridDrawer().getGridSize();
	    IContainer container = view.getContainer();

	    for (DeviceItem device : view.getAllDevices()) {

	        Point focusPx = device.getFocusPoint(container);
	        if (focusPx == null) {
	            continue;
	        }

	        int snapX = Math.round((float) focusPx.x / gridSize) * gridSize;
	        int snapY = Math.round((float) focusPx.y / gridSize) * gridSize;

	        int dxPx = snapX - focusPx.x;
	        int dyPx = snapY - focusPx.y;

	        device.translateLocal(dxPx, dyPx);
	    }

	    container.refresh();
	}
	}

