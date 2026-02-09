package edu.cnu.mdi.sim.demo.forestfire;

import java.util.Random;

public final class ForestFireModel {

	// The grid representing the forest, where each cell can be EMPTY, TREE, or
	// BURNING
	public final int width;
	public final int height;
	public  CellState[][] grid;
	public int totalCells;
	
	// Probability that fire spreads from a burning tree to an adjacent tree (0..1)
    public final double pSpread;


	public ForestFireModel(int width, int height, double pSpread) {
		this.width = width;
		this.height = height;
		this.pSpread = pSpread;
		this.grid = new CellState[height][width];

		// Initialize all cells as trees
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				grid[x][y] = CellState.TREE;
			}
		}
	}
	
	/**
	 * Helper method to create a random forest fire model with a single burning cell.
	 * @param width The width of the forest grid.
	 * @param height The height of the forest grid.
	 * @param pSpread The probability that fire spreads from a burning tree to an adjacent tree (0..1).
	 * @param rng A random number generator to select the initial burning cell.
	 * @return A new ForestFireModel instance with the specified dimensions and a random burning cell.
	 */
	public static ForestFireModel random(int width, int height, double pSpread, Random rng) {
		ForestFireModel model = new ForestFireModel(width, height, pSpread);
		
		// Randomly select a cell to start burning
		int startX = rng.nextInt(width);
		int startY = rng.nextInt(height);
		model.grid[startX][startY] = CellState.BURNING;
		
		return model;
	}
}
