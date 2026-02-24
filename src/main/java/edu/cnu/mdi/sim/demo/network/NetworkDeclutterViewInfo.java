package edu.cnu.mdi.sim.demo.network;

import java.util.List;

import edu.cnu.mdi.view.AbstractViewInfo;

/**
 * View info for the Network Decluttering Demo. This provides metadata about the
 * view, including its title, purpose, and usage instructions. It is accessed
 * through the info button on "Network Decluttering Demo" view.
 */
public class NetworkDeclutterViewInfo extends AbstractViewInfo {

	@Override
	public String getTitle() {
		return "Network Decluttering Demo";
	}

	@Override
	public String getPurpose() {
		return "The purpose of this view is to demonstrate the MDI similation package"
				+ "using a world-based network decluttering algorithm. It simulates a network of servers, clients and printers, applying a force-directed layout algorithm to reduce visual clutter while preserving the overall structure of the network.";
	}

	@Override
	public List<String> getUsageBullets() {
		return List.of("Use the Toolbar to start, pause, and reset the simulation.",
				"Adjust the Number of Servers, Number of Clients, and Number of Printers sliders to generate different network configurations.",
				"Observe how the force-directed layout algorithm declutters the network while maintaining connectivity.");
	}

	@Override
	public String getTechnicalNotes() {
		return "Handbook of Graph Drawing and Visualization (Discrete Mathematics and Its Applications), Chapter 12: \"Force-Directed Drawing Algorithms\", Roberto Tamassia ed.";
	}

}
