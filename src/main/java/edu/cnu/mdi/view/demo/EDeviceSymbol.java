package edu.cnu.mdi.view.demo;

public enum EDeviceSymbol {
	CLOUD("images/svg/cloud.svg", "cloud"), FIREWALL("images/svg/firewall.svg", "firewall"),
	LAPTOP("images/svg/laptop.svg", "laptop"), PRINTER("images/svg/printer.svg", "printer"),
	ROUTER("images/svg/router.svg", "router"), SERVER("images/svg/server.svg", "server"),
	SWITCH("images/svg/switch.svg", "switch"), WIRELESS("images/svg/wireless.svg", "wireless"),
	WORKSTATION("images/svg/workstation.svg", "workstation");

	public final String iconPath;
	public final String toolTip;

	EDeviceSymbol(String iconPath, String toolTip) {
		this.iconPath = iconPath;
		this.toolTip = toolTip;
	}
}
