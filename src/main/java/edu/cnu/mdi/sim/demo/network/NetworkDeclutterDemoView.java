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
import edu.cnu.mdi.sim.ui.SimulationView;

/**
 * World-based network decluttering demo hosted in an MDI {@link SimulationView}.
 * <p>
 * This version is Java-21 compatible: it does not rely on “flexible constructor bodies”.
 * </p>
 */
@SuppressWarnings("serial")
public class NetworkDeclutterDemoView extends SimulationView {

    /** Model used by this view (world coords). */
    private final NetworkModel model;

    /** Simulation used by this view. */
    private final NetworkDeclutterSimulation sim;

    // Icons for servers and clients
    private final Icon serverIcon;
    private final Icon clientIcon;
    private int iconRadiusPx;


    /**
	 * Create a network layout demo view.
	 *
	 * @param keyVals variable set of arguments.
	 */
    public NetworkDeclutterDemoView(Object... keyVals) {
        // Java 21: must call super(...) first. We build the simulation via a static helper.
        super(createSimulation(), new SimulationEngineConfig(33, 250, 20, false), true, keyVals);

        // Recover our concrete simulation type so we can access the model and attach engine callbacks.
        this.sim = (NetworkDeclutterSimulation) getSimulationEngine().getSimulation();
        this.model = this.sim.getModel();

        // Let the simulation post progress/messages/refresh through this view’s engine.
        this.sim.setEngine(getSimulationEngine());

        // Load icons
        serverIcon = ImageManager.getInstance().loadUiIcon("images/svg/server.svg", 32, 32);
		clientIcon = ImageManager.getInstance().loadUiIcon("images/svg/workstation.svg", 32, 32);
		iconRadiusPx = clientIcon.getIconWidth() / 2; // assuming square icons of same size

		setAfterDraw();
		setBeforeDraw();

		getContainer().scale(1.1); // initial zoom


        // Start engine thread (remains paused until Run unless you call startAndRun()).
        startSimulation();
    }

    private static NetworkDeclutterSimulation createSimulation() {
        NetworkModel m = NetworkModel.random(
                14,      // servers
                100,     // clients
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

				g2.setColor(Color.black);

				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				for (NetworkModel.Edge e : model.edges) {
					NetworkModel.Node client = model.nodes.get(e.clientIndex);
					NetworkModel.Node server = model.nodes.get(e.serverIndex);

					getContainer().worldToLocal(pp0, client.x, client.y);
					getContainer().worldToLocal(pp1, server.x, server.y);

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
				for (NetworkModel.Node n : model.nodes) {
					Icon icon = (n.type == NetworkModel.NodeType.SERVER) ? serverIcon : clientIcon;
					getContainer().worldToLocal(pp, n.x, n.y);
					icon.paintIcon(getContainer().getComponent(), g2, pp.x - iconRadiusPx, pp.y - iconRadiusPx);
				}
			}
		});
	}

}
