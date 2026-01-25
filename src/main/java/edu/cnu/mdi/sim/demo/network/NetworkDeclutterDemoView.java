package edu.cnu.mdi.sim.demo.network;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.Random;

import javax.swing.Icon;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.ImageManager;
import edu.cnu.mdi.graphics.drawable.DrawableAdapter;
import edu.cnu.mdi.sim.SimulationEngineConfig;
import edu.cnu.mdi.sim.ui.IconSimulationControlPanel;
import edu.cnu.mdi.sim.ui.SimulationView;
import edu.cnu.mdi.sim.ui.StandardSimIcons;
import edu.cnu.mdi.util.Environment;

/**
 * World-based network decluttering demo hosted in an MDI
 * {@link SimulationView}.
 * <p>
 * This version is Java-21 compatible: it does not rely on “flexible constructor
 * bodies”.
 * </p>
 */
@SuppressWarnings("serial")
public class NetworkDeclutterDemoView extends SimulationView {

	/** Model used by this view (world coords). */
	private final NetworkModel model;

	/** Simulation used by this view. */
	private final NetworkDeclutterSimulation sim;

	// Icons for servers, printers and clients
	private final Icon serverIcon;
	private final Icon clientIcon;
	private final Icon printerIcon;
	private final int iconSize = 28;
	private int iconRadiusPx;
	/**
	 * Create a network layout demo view.
	 *
	 * @param keyVals variable set of arguments.
	 */
	public NetworkDeclutterDemoView(Object... keyVals) {
		// must call super(...) first. We build the simulation via a static helper.
		super(
			    createSimulation(),
			    new SimulationEngineConfig(60, 250, 60, false),
			    true,
			    (SimulationView.ControlPanelFactory) () ->
			        new IconSimulationControlPanel(new StandardSimIcons(), false),
			    keyVals
			);		// Recover our concrete simulation type so we can access the model and attach
		// engine callbacks.

		this.sim = (NetworkDeclutterSimulation) getSimulationEngine().getSimulation();
		this.model = this.sim.getModel();

		// Let the simulation post progress/messages/refresh through this view’s engine.
		this.sim.setEngine(getSimulationEngine());

		// Load icons
		String resPath = Environment.MDI_RESOURCE_PATH;
		serverIcon = ImageManager.getInstance().loadUiIcon(resPath + "images/svg/server.svg", iconSize, iconSize);
		clientIcon = ImageManager.getInstance().loadUiIcon(resPath + "images/svg/workstation.svg", iconSize, iconSize);
		printerIcon = ImageManager.getInstance().loadUiIcon(resPath + "images/svg/printer.svg", iconSize, iconSize);
		
		iconRadiusPx = clientIcon.getIconWidth() / 2; // assuming square icons of same size

		setAfterDraw();
		setBeforeDraw();

		getContainer().scale(1.1); // initial zoom

		// Start engine thread (remains paused until Run unless you call startAndRun()).
		startSimulation();
	}

	// Helper to create the simulation instance
	private static NetworkDeclutterSimulation createSimulation() {
		NetworkModel m = NetworkModel.random(14, // servers
				100, // clients
				5, // printers
				new Random());

		return new NetworkDeclutterSimulation(m);
	}

	// Draw edges before nodes
	private void setBeforeDraw() {
		getContainer().setBeforeDraw(new DrawableAdapter() {
			@Override
			public void draw(Graphics2D g2, IContainer container) {
				Point pp0 = new Point();
				Point pp1 = new Point();


				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				for (NetworkModel.Edge e : model.edges) {
					Node node1 = e.node1;
					Node node2 = e.node2;
					
					// draw lnks to printers in red
					if (node2.type == Node.NodeType.PRINTER || node1.type == Node.NodeType.PRINTER) {
						g2.setColor(Color.red);
					} else {
						g2.setColor(Color.black);
					}

					getContainer().worldToLocal(pp0, node1.x, node1.y);
					getContainer().worldToLocal(pp1, node2.x, node2.y);

					g2.drawLine(pp0.x, pp0.y, pp1.x, pp1.y);
				}
			}
		});
	}

	// Draw nodes after edges
	private void setAfterDraw() {
		getContainer().setAfterDraw(new DrawableAdapter() {
			@Override
			public void draw(Graphics2D g2, IContainer container) {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				Point pp = new Point();
				for (Node n : model.servers) {
					getContainer().worldToLocal(pp, n.x, n.y);
					serverIcon.paintIcon(getContainer().getComponent(), g2, pp.x - iconRadiusPx, pp.y - iconRadiusPx);
				}
				for (Node n : model.clients) {
					getContainer().worldToLocal(pp, n.x, n.y);
					clientIcon.paintIcon(getContainer().getComponent(), g2, pp.x - iconRadiusPx, pp.y - iconRadiusPx);
				}
				for (Node n : model.printers) {
					getContainer().worldToLocal(pp, n.x, n.y);
					printerIcon.paintIcon(getContainer().getComponent(), g2, pp.x - iconRadiusPx, pp.y - iconRadiusPx);
				}
			}
		});
	}

}
