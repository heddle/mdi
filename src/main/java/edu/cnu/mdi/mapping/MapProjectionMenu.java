package edu.cnu.mdi.mapping;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;

public class MapProjectionMenu extends JMenu {

	private EProjection currentProjection = EProjection.MOLLWEIDE; // Default selection
	private final ActionListener projectionChangeCallback;

	/**
	 * Constructor to create the "Map Projection" menu.
	 *
	 * @param callback The callback to handle projection changes.
	 */
	public MapProjectionMenu(ActionListener callback) {
		super("Map Projection");
		this.projectionChangeCallback = callback;
		createMenuItems();
	}

	/**
	 * Creates the "Map Projection" menu with radio button items.
	 */
	private void createMenuItems() {
		ButtonGroup buttonGroup = new ButtonGroup();

		for (EProjection projection : EProjection.values()) {
			// Create a radio button menu item for each projection
			JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(projection.getName());

			// Set the default selection
			if (projection == EProjection.MOLLWEIDE) {
				menuItem.setSelected(true);
			}

			// Add action listener to handle selection
			menuItem.addActionListener(e -> {
				currentProjection = projection;
				projectionChangeCallback
						.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, projection.name()));
			});

			// Add the radio button to the group and menu
			buttonGroup.add(menuItem);
			add(menuItem);
		}
	}

	/**
	 * Gets the currently selected projection.
	 *
	 * @return The currently selected projection.
	 */
	public EProjection getCurrentProjection() {
		return currentProjection;
	}

}
