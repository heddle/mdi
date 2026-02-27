package edu.cnu.mdi.view.demo;

import edu.cnu.mdi.util.Environment;

/**
 * Enumeration of device symbols, each with an associated icon path and tooltip.
 */
public enum EDeviceSymbol {
	CLOUD("images/svg/cloud.svg", "cloud"), FIREWALL("images/svg/firewall.svg", "firewall"),
	LAPTOP("images/svg/laptop.svg", "laptop"), PRINTER("images/svg/printer.svg", "printer"),
	ROUTER("images/svg/router.svg", "router"), SERVER("images/svg/server.svg", "server"),
	SWITCH("images/svg/switch.svg", "switch"), WIRELESS("images/svg/wireless.svg", "wireless"),
	WORKSTATION("images/svg/workstation.svg", "workstation");

	public final String iconPath;
	public final String toolTip;

	/**
	 * Constructor for EDeviceSymbol enum constants.
	 *
	 * @param iconPath the relative path to the icon image for this device symbol
	 * @param toolTip  the tooltip text to display for this device symbol
	 */
	EDeviceSymbol(String iconPath, String toolTip) {
		this.iconPath = Environment.MDI_RESOURCE_PATH + iconPath;
		this.toolTip = toolTip;
	}
}
