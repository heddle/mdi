package edu.cnu.mdi.sim.demo.forestfire;

import edu.cnu.mdi.sim.*;
import java.util.Random;

public class ForestFireSimulation implements Simulation {

    private SimulationEngine engine;
    private final Random random = new Random();
    
    private ForestFireModel model;
    
	// -------------------------------------------------------------------------
	// Bookkeeping / framework integration
	// -------------------------------------------------------------------------

	/** Current step index. */
	private int step;

	/** True if {@link #cancel(SimulationContext)} has been called. */
	private volatile boolean canceled;


    /**
     * Constructor for the ForestFireSimulation.
     * @param engine The simulation engine to post messages and progress updates to.
     * @param m The initial model of the forest fire simulation, containing the grid and its dimensions.
     */
    public ForestFireSimulation(ForestFireModel m) {
        this.model = m;
    }

	/**
	 * Attach the engine so this simulation can post messages/progress/refresh.
	 * <p>
	 * This is optional. If unset, the simulation still runs but does not emit UI
	 * hints beyond what the engine itself already provides.
	 * </p>
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
			engine.postMessage("Network generated. Relaxing layout…");
			engine.postProgress(ProgressInfo.indeterminate("Relaxing…"));
			engine.requestRefresh();
		}
	}

    /**
	 * Perform one step of the simulation. This method is called repeatedly by the engine until it returns false.
	 * @param ctx The simulation context, which can be used to check for cancellation and track progress.
	 * @return true if the simulation should continue, or false to stop the engine automatically.
	 */
    @Override
    public boolean step(SimulationContext ctx) throws Exception {
        CellState[][] nextGrid = new CellState[model.width][model.height];
        boolean fireExists = false;
        int burntCount = 0;

        for (int x = 0; x < model.width; x++) {
            for (int y = 0; y < model.height; y++) {
                CellState current = model.grid[x][y];

                if (current == CellState.BURNING) {
                    nextGrid[x][y] = CellState.EMPTY;
                } else if (current == CellState.TREE) {
                    if (isNeighborBurning(x, y) && random.nextDouble() < model.pSpread) {
                        nextGrid[x][y] = CellState.BURNING;
                        fireExists = true;
                    } else {
                        nextGrid[x][y] = CellState.TREE;
                    }
                } else {
                    nextGrid[x][y] = CellState.EMPTY;
                    burntCount++;
                }
            }
        }

        model.grid = nextGrid;

        // Calculate progress and use the factory method for ProgressInfo
        double fraction = (double) burntCount / model.totalCells;
        ProgressInfo progress = ProgressInfo.determinate(
            fraction, 
            String.format("%.1f%% of forest consumed", fraction * 100)
        );
        
        // Post the update to the engine
        engine.postProgress(progress);

        return fireExists; // Step returns false to stop the engine automatically
    }

    // Helper method to check if any neighboring cell is burning
    private boolean isNeighborBurning(int x, int y) {
        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};
        for (int i = 0; i < 4; i++) {
            int nx = x + dx[i], ny = y + dy[i];
            if (nx >= 0 && nx < model.width && ny >= 0 && ny < model.height && 
            		model.grid[nx][ny] == CellState.BURNING) {
                return true;
            }
        }
        return false;
    }

    /**
	 * Get the current state of the grid. This method can be used by the UI to
	 * render the forest.
	 * 
	 * @return A 2D array representing the state of each cell in the grid.
	 */
	public CellState[][] getGrid() {
		return model.grid;
	}

}