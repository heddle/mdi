package edu.cnu.mdi.view.demo.layout;

import java.awt.event.MouseEvent;

import edu.cnu.mdi.graphics.toolbar.ASingleClickButton;
import edu.cnu.mdi.graphics.toolbar.BaseToolBar;
import edu.cnu.mdi.item.Layer;

@SuppressWarnings("serial")
public class DeviceButton extends ASingleClickButton {

	// the parent view being controlled
	private NetworkLayoutDemoView view;


	// the layer to which the created device items will be added
	private Layer deviceLayer;
	
	// the symbol to be created when this button is clicked
	private EDeviceSymbol symbol;

	/**
	 * Constructor
	 * @param view the parent view being controlled
	 * @param symbol the symbol to be created when this button is clicked
	 */
	public DeviceButton(NetworkLayoutDemoView view, EDeviceSymbol symbol) {
		super(view.getIContainer().getComponent(), view.getToolBar());
		this.view = view;
		this.deviceLayer = view.getDeviceLayer();
		this.symbol = symbol;

		BaseToolBar toolBar = (BaseToolBar) view.getToolBar();
		toolBar.configureButton(this, symbol.iconPath, symbol.toolTip);
		toolBar.addToggle(symbol.name(), this);

	}

	/**
	 * When the button is clicked, create a new device item at the clicked location
	 * and refresh the view.
	 */
	@Override
	public void canvasClick(MouseEvent e) {
		DeviceItem.createDeviceItem(deviceLayer, e.getPoint(), symbol);
		view.getIContainer().refresh();
		view.getToolBar().resetDefaultToggleButton();
	}

}
