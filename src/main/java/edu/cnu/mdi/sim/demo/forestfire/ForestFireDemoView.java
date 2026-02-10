package edu.cnu.mdi.sim.demo.forestfire;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.Random;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.drawable.DrawableAdapter;
import edu.cnu.mdi.sim.SimulationEngine;
import edu.cnu.mdi.sim.SimulationEngineConfig;
import edu.cnu.mdi.sim.ui.SimulationView;

@SuppressWarnings("serial")
public class ForestFireDemoView extends SimulationView implements IForestFireResettable {

    public static final int DEFAULT_GRID_WIDTH = 200;
    public static final int DEFAULT_GRID_HEIGHT = 200;

    // critical-ish around ~0.59 for percolation-like behavior on large grids
    public static final double DEFAULT_P_SPREAD = 0.60;

    private ForestFireModel model;
    private ForestFireSimulation sim;

    public ForestFireDemoView(Object... keyVals) {
        super(createSimulation(DEFAULT_GRID_WIDTH, DEFAULT_GRID_HEIGHT, DEFAULT_P_SPREAD),
                new SimulationEngineConfig(60, 250, 60, false),
                true,
                (SimulationView.ControlPanelFactory) ForestFireControlPanel::new,
                keyVals);

        this.sim = (ForestFireSimulation) getSimulationEngine().getSimulation();
        this.model = sim.getModel();

        // allow sim to post messages/progress/refresh
        sim.setEngine(getSimulationEngine());

        installGridDrawing();

        // Use a simple world where each cell is a 1x1 square.
        getContainer().scale(10.0); // initial zoom so cells are visible

        pack();
        startSimulation(); // engine thread starts; remains READY until Run
    }

    private static ForestFireSimulation createSimulation(int w, int h, double pSpread) {
        ForestFireModel m = ForestFireModel.random(w, h, pSpread, new Random());
        return new ForestFireSimulation(m);
    }

    private void installGridDrawing() {
        getContainer().setBeforeDraw(new DrawableAdapter() {
            @Override
            public void draw(Graphics2D g2, IContainer container) {

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

                // We draw in LOCAL pixel space by converting world coords.
                Point p0 = new Point();
                Point p1 = new Point();

                // cell (x,y) occupies world square [x,x+1] x [y,y+1]
                for (int x = 0; x < model.width; x++) {
                    for (int y = 0; y < model.height; y++) {
                        CellState s = model.grid[x][y];

                        switch (s) {
                        case TREE:
                            g2.setColor(java.awt.Color.GREEN.darker());
                            break;
                        case BURNING:
                            g2.setColor(java.awt.Color.RED);
                            break;
                        case EMPTY:
                        default:
                            g2.setColor(java.awt.Color.DARK_GRAY);
                            break;
                        }

                        container.worldToLocal(p0, x, y);
                        container.worldToLocal(p1, x + 1.0, y + 1.0);

                        int left = Math.min(p0.x, p1.x);
                        int top = Math.min(p0.y, p1.y);
                        int w = Math.abs(p1.x - p0.x);
                        int h = Math.abs(p1.y - p0.y);

                        // Fill cell
                        g2.fillRect(left, top, Math.max(1, w), Math.max(1, h));
                    }
                }
            }
        });
    }

    @Override
    public void requestReset(int width, int height, double pSpread) {

        requestEngineReset(
                () -> createSimulation(width, height, pSpread),

                (SimulationEngine newEngine) -> {
                    ForestFireSimulation newSim = (ForestFireSimulation) newEngine.getSimulation();
                    this.sim = newSim;
                    this.model = newSim.getModel();
                    this.sim.setEngine(newEngine);

                    // drawing closure references `model` field, so we’re fine.
                },

                true,  // autoStart
                true   // refresh immediately
        );
    }
}
