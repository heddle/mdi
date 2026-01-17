package edu.cnu.mdi.view.demo;

import java.awt.Point;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.toolbar.AOneShotButton;
import edu.cnu.mdi.graphics.toolbar.BaseToolBar;
import edu.cnu.mdi.util.Environment;

@SuppressWarnings("serial")
public class GridButton extends AOneShotButton {
	
	public static final String TOOL_ID = "GRID_BUTTON";

	// the parent view being controlled
	private NetworkLayoutDemoView view;

	public GridButton(NetworkLayoutDemoView view) {
		super(view.getContainer().getComponent(), view.getToolBar());
		
		this.view = view;
		String iconPath = Environment.MDI_RESOURCE_PATH + "images/svg/grid.svg";
		String toolTip = "Snap to Grid";
		
		BaseToolBar toolBar = (BaseToolBar) view.getToolBar();
		toolBar.configureButton(this, iconPath, toolTip);
		toolBar.addButton(TOOL_ID, this);
	}

	@Override
	public void performAction() {
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
