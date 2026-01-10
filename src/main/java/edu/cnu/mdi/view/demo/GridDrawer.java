package edu.cnu.mdi.view.demo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.drawable.DrawableAdapter;

public class GridDrawer extends DrawableAdapter {

	private int gridSize;
	private Color gridColor;

	/**
	 * Create a grid drawer that draws a grid of the given size and color on the
	 * given container.
	 *
	 * @param container the container on which to draw the grid
	 * @param gridSize  the size of the grid cells in pixels
	 * @param gridColor the color of the grid lines
	 */
	public GridDrawer(IContainer container, int gridSize, Color gridColor) {
		super("SnapToGrid");
		this.gridSize = gridSize;
		this.gridColor = gridColor;

		// this will cause the drawing to occur before items are drawn.
		// alternatively, use setAfterDraw to draw on top of items.
		container.setBeforeDraw(this);
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
