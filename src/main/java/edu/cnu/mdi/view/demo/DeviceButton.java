package edu.cnu.mdi.view.demo;

import java.awt.event.MouseEvent;

import edu.cnu.mdi.graphics.toolbar.ASingleClickButton;
import edu.cnu.mdi.graphics.toolbar.BaseToolBar;
import edu.cnu.mdi.item.Layer;

@SuppressWarnings("serial")
public class DeviceButton extends ASingleClickButton {
	
	// the parent view being controlled
	private NetworkLayoutDemoView view;

	
	private Layer deviceLayer;
	private EDeviceSymbol symbol;
	
	public DeviceButton(NetworkLayoutDemoView view, EDeviceSymbol symbol) {
		super(view.getContainer().getComponent(), view.getToolBar());
		this.view = view;
		this.deviceLayer = view.getDeviceLayer();
		this.symbol = symbol;
		
		BaseToolBar toolBar = (BaseToolBar) view.getToolBar();
		toolBar.configureButton(this, symbol.iconPath, symbol.toolTip);
		toolBar.addToggle(symbol.name(), this);

	}

	@Override
	public void canvasClick(MouseEvent e) {
		DeviceItem.createDeviceItem(deviceLayer, e.getPoint(), symbol);
		view.getContainer().refresh();
		view.getToolBar().resetDefaultToggleButton();
	}

}
