package edu.cnu.mdi.sim.ui;

import javax.swing.JComponent;

/**
 * Factory for creating a simulation control panel component.
 * Demos can supply different factories to change the UI without subclassing views.
 */
@FunctionalInterface
public interface ControlPanelFactory {
	JComponent createControlPanel();
}
