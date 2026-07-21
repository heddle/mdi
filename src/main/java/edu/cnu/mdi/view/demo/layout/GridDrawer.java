package edu.cnu.mdi.view.demo.layout;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.drawable.DrawableAdapter;

public class GridDrawer extends DrawableAdapter {

	// grid cell size in pixels
	private int gridSize;
	
	// grid line color
	private Color gridColor;

	/**
	 * Create a grid drawer that draws a grid of the given size and color on the
	 * given container.
	 *
	 * @param gridSize  the size of the grid cells in pixels
	 * @param gridColor the color of the grid lines
	 */
	public GridDrawer(int gridSize, Color gridColor) {
		super("SnapToGrid");
		this.gridSize = gridSize;
		this.gridColor = gridColor;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Draw a "snap to" grid overlay.
	 */
	@Override
	public void draw(Graphics2D g, IContainer container) {

		// "show snap grid" checkbox on control panel toggles visibility
		if (!isVisible()) {
			return;
		}

		Rectangle bounds = container.getComponent().getBounds();

		g.setColor(gridColor);
		// draw vertical grid lines
		for (int dx = 0; dx <= bounds.width; dx += gridSize) {
			g.drawLine(dx, 0, dx, bounds.height);
		}
		// draw horizontal grid lines
		for (int dy = 0; dy <= bounds.height; dy += gridSize) {
			g.drawLine(0, dy, bounds.width, dy);
		}
	}

	/**
	 * Get the grid size.
	 */
	public int getGridSize() {
		return gridSize;
	}

}
