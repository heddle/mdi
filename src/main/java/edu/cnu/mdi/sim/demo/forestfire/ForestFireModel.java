package edu.cnu.mdi.sim.demo.forestfire;

import java.util.Objects;
import java.util.Random;

/**
 * Simple model for the forest fire simulation. Cells are indexed as {@code grid[x][y]}.
 */
public final class ForestFireModel {

    /** Grid width in cells. */
    public final int width;

    /** Grid height in cells. */
    public final int height;

    /** Total number of cells ({@code width * height}). */
    public final int totalCells;

    /** Cell grid indexed as {@code grid[x][y]}. */
    public CellState[][] grid;

    /** Probability (0..1) that fire spreads from a burning neighbor to a tree. */
    public final double pSpread;

    public ForestFireModel(int width, int height, double pSpread) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be > 0");
        }
        if (!(pSpread >= 0.0 && pSpread <= 1.0)) {
            throw new IllegalArgumentException("pSpread must be in [0, 1]");
        }

        this.width = width;
        this.height = height;
        this.totalCells = width * height;
        this.pSpread = pSpread;

        // IMPORTANT: if we index as grid[x][y], allocate [width][height].
        this.grid = new CellState[width][height];

        // Initialize all cells as trees
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid[x][y] = CellState.TREE;
            }
        }
    }

    /**
     * Create a new random model with a single burning cell.
     */
    public static ForestFireModel random(int width, int height, double pSpread, Random rng) {
        Objects.requireNonNull(rng, "rng");
        ForestFireModel model = new ForestFireModel(width, height, pSpread);

        int startX = rng.nextInt(width);
        int startY = rng.nextInt(height);
        model.grid[startX][startY] = CellState.BURNING;

        return model;
    }
}
