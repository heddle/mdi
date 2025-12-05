package edu.cnu.mdi.graphics.toolbar;

import java.awt.event.ActionEvent;

import edu.cnu.mdi.container.IContainer;

/**
 * The world button restores the original zoom level.
 *
 * @author heddle
 *
 */
@SuppressWarnings("serial")
public class WorldButton extends ToolBarButton {

	/**
	 * Create the worlds button, which zooms bact to the original extent.
	 *
	 * @param container the container this button acts upon.
	 */
	public WorldButton(IContainer container) {
		super(container, "images/world.gif", "restore original zoom");
	}

	/**
	 * This is what I do if I am pressed
	 *
	 * @param e The causal event.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		container.restoreDefaultWorld();
		container.refresh();
		container.getToolBar().resetDefaultSelection();
	}
}
