package edu.cnu.mdi.graphics.toolbar;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.GraphicsUtils;


/**
 * @author heddle
 *
 */
@SuppressWarnings("serial")
public class PanButton extends ToolBarToggleButton {

	private int pan_x = -999999;
	private int pan_y;
	private int startx = -999999;
	private int starty;

	private BufferedImage baseimage = null;
	private BufferedImage image = null;

	private static final int PANMINDEL = 4;

	/**
	 * Create a button for panning the container.
	 *
	 * @param container the owner container.
	 */
	public PanButton(IContainer container) {
		super(container, "images/pan.gif", "Pan the view");
	}

	@Override
	public void mouseDragged(MouseEvent mouseEvent) {

		if (startx < -32000) {
			startx = mouseEvent.getX();
			starty = mouseEvent.getY();
			pan_x = startx;
			pan_y = starty;

			baseimage = GraphicsUtils.getComponentImage(container.getComponent());
			image = GraphicsUtils.getComponentImageBuffer(container.getComponent());
			return;
		}

		update(mouseEvent, container);
	}

	/**
	 * Updates the results of a pan.
	 *
	 * @param mouseEvent the new current location.
	 * @param container  the container.
	 */
	private void update(MouseEvent mouseEvent, IContainer container) {

		// obtain the change since the last pan.
		int delx = mouseEvent.getX() - pan_x;
		int dely = mouseEvent.getY() - pan_y;

		if ((Math.abs(delx) > PANMINDEL) || (Math.abs(dely) > PANMINDEL)) {

			pan_x = mouseEvent.getX();
			pan_y = mouseEvent.getY();
			boolean standardPanning = container.isStandardPanning();

			if (standardPanning) {
				int total_dx = standardPanning ? mouseEvent.getX() - startx : 0;
				int total_dy = standardPanning ? mouseEvent.getY() - starty : 0;

				Graphics gg = image.getGraphics();
				gg.setColor(container.getComponent().getBackground());
				gg.fillRect(0, 0, image.getWidth(), image.getHeight());
				gg.drawImage(baseimage, total_dx, total_dy, container.getComponent());
				gg.dispose();

				Graphics g = container.getComponent().getGraphics();

				g.drawImage(image, 0, 0, container.getComponent());

				g.dispose();
			}

		}
	}

	/**
	 * Local pan method
	 *
	 * @param mouseEvent the causal event.
	 */
	private void pan(MouseEvent mouseEvent) {
		if (startx < -99999) {
			return;
		}

		int delx = mouseEvent.getX() - startx;
		int dely = mouseEvent.getY() - starty;

		if ((Math.abs(delx) > PANMINDEL) || (Math.abs(dely) > PANMINDEL)) {
			container.pan(delx, dely);
			pan_x = mouseEvent.getX();
			pan_y = mouseEvent.getY();
		}
	}

	/**
	 * Get the appropriate cursor for this tool.
	 *
	 * @return The cursor appropriate when the mouse is in the map canvas.
	 */

	@Override
	public Cursor canvasCursor() {
		return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseReleased(MouseEvent mouseEvent) {
		if (mouseEvent.getButton() != 1) {
			return;
		}

		pan(mouseEvent);
		startx = -999999;
	}

}
