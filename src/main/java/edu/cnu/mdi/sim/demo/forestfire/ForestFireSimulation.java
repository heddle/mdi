package edu.cnu.mdi.sim.demo.forestfire;

import java.util.Objects;
import java.util.Random;

import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.Simulation;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationEngine;

/**
 * Classic cellular-automaton style forest fire spread simulation.
 * <p>
 * Rules per step:
 * <ul>
 *   <li>BURNING -> EMPTY</li>
 *   <li>TREE -> BURNING with probability pSpread if any 4-neighbor is burning</li>
 *   <li>EMPTY stays EMPTY</li>
 * </ul>
 * </p>
 */
public class ForestFireSimulation implements Simulation {

    private SimulationEngine engine;
    private final Random random = new Random();

    private final ForestFireModel model;

    /** Current step index. */
    private int step;

    /** True if {@link #cancel(SimulationContext)} has been called. */
    private volatile boolean canceled;

    /**
     * Create a forest fire simulation with the given model.
     * @param model forest fire model
     */
    public ForestFireSimulation(ForestFireModel m) {
        this.model = Objects.requireNonNull(m, "model");
    }

    /** 
     * Get the model used by this simulation.
     * @return forest fire model
     */
    public ForestFireModel getModel() {
        return model;
    }

    /**
     * Attach the engine so this simulation can post messages/progress/refresh.
     *
     * @param engine engine executing this simulation (may be null)
     */
    public void setEngine(SimulationEngine engine) {
        this.engine = engine;
    }

    @Override
    public void init(SimulationContext ctx) throws Exception {
        step = 0;
        canceled = false;

        if (engine != null) {
            engine.postMessage("Forest fire initialized.");
            engine.postProgress(ProgressInfo.determinate(0.0, "0% of forest consumed"));
            engine.requestRefresh();
        }
    }

    @Override
    public boolean step(SimulationContext ctx) throws Exception {

        if (canceled || (ctx != null && ctx.isCancelRequested())) {
            return false;
        }

        final CellState[][] nextGrid = new CellState[model.width][model.height];

        boolean anyBurningNext = false;

        // First pass: apply rules
        for (int x = 0; x < model.width; x++) {
            for (int y = 0; y < model.height; y++) {

                // If cancel requested, exit promptly.
                if (canceled || (ctx != null && ctx.isCancelRequested())) {
                    return false;
                }

                CellState current = model.grid[x][y];

                if (current == CellState.BURNING) {
                    nextGrid[x][y] = CellState.EMPTY;
                }
                else if (current == CellState.TREE) {
                    if (isNeighborBurning(x, y) && random.nextDouble() < model.pSpread) {
                        nextGrid[x][y] = CellState.BURNING;
                        anyBurningNext = true;
                    } else {
                        nextGrid[x][y] = CellState.TREE;
                    }
                }
                else { // EMPTY
                    nextGrid[x][y] = CellState.EMPTY;
                }
            }
        }

        // Commit
        model.grid = nextGrid;

        // Compute fraction consumed = EMPTY / total.
        int emptyCount = 0;
        for (int x = 0; x < model.width; x++) {
            for (int y = 0; y < model.height; y++) {
                if (model.grid[x][y] == CellState.EMPTY) {
                    emptyCount++;
                }
            }
        }

        double fraction = (double) emptyCount / (double) model.totalCells;

        if (engine != null) {
            engine.postProgress(ProgressInfo.determinate(
                    fraction,
                    String.format("Step %d  —  %.1f%% of forest consumed", step, fraction * 100.0)
            ));

            // For this demo, refreshing every step is fine.
            engine.requestRefresh();
        }

        step++;
        return anyBurningNext; // stop automatically when fire dies out
    }

    @Override
    public void cancel(SimulationContext ctx) throws Exception {
        canceled = true;
        if (engine != null) {
            engine.postMessage("Cancel requested.");
        }
    }

    private boolean isNeighborBurning(int x, int y) {
        int[] dx = { 0, 0, 1, -1 };
        int[] dy = { 1, -1, 0, 0 };

        for (int i = 0; i < 4; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];

            if (nx >= 0 && nx < model.width && ny >= 0 && ny < model.height) {
                if (model.grid[nx][ny] == CellState.BURNING) {
                    return true;
                }
            }
        }
        return false;
    }
}
