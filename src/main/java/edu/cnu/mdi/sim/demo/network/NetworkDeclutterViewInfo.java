package edu.cnu.mdi.sim.demo.network;

import edu.cnu.mdi.view.AbstractViewInfo;

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
		public String getUsage() {
			return "<ul>" +
					"<li>Use the <b>Toolbar</b> to start, pause, and reset the simulation.</li>" +
					"<li>Adjust the <b>Number of Servers</b>, <b>Number of Clients</b>, and <b>Number of Printers</b> sliders to generate different network configurations.</li>" +
					"<li>Observe how the force-directed layout algorithm declutters the network while maintaining connectivity.</li>" +
					"</ul>";
	}
	
    @Override
    public String getTechnicalNotes() {
        return "Handbook of Graph Drawing and Visualization (Discrete Mathematics and Its Applications), Chapter 12: \"Force-Directed Drawing Algorithms\", Roberto Tamassia ed.";
    }

}
