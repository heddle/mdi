package edu.cnu.mdi.app;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import edu.cnu.mdi.container.CreationSupport;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.properties.PropertyUtils;
import edu.cnu.mdi.swing.WindowPlacement;
import edu.cnu.mdi.view.DrawingView;

/**
 * Simple "Hello World" demo for the MDI creates a single drawing view and
 * demonstrates the use of the PropertyUtils key-value system to configure views. Also adds
 */

@SuppressWarnings("serial")
public class HelloMDI extends BaseMDIApplication {

	private final DrawingView drawingView; //only view

	public HelloMDI(Object... keyVals) {
		super(keyVals);
		// set to a fraction of screen
		Dimension d = WindowPlacement.screenFraction(0.4);
		
		// specify (bitwise) what will be on the toolbar
		long toolBits = ToolBits.STATUS | ToolBits.DRAWINGTOOLS | ToolBits.ZOOMTOOLS | ToolBits.PAN;
		
		drawingView = new DrawingView(
				PropertyUtils.WORLDSYSTEM, new Rectangle2D.Double(0, 0, d.width, d.height),
				PropertyUtils.WIDTH, d.width,
				PropertyUtils.HEIGHT, d.height,
				PropertyUtils.TOOLBARBITS, toolBits,
				PropertyUtils.VISIBLE, true,
				PropertyUtils.BACKGROUND, Color.white,
				PropertyUtils.INFOBUTTON, true,
				PropertyUtils.TITLE,"Drawing View");
	}
	
	/**
	 * Runs once after the outer frame is showing and Swing layout has stabilized.
	 */
	@Override
	protected void onVirtualDesktopReady() {
		// desktop ready, safe to apply default placements and add content.
		drawingView.center();

		//lets add some initial content to the drawing view
		Layer layer = drawingView.getContainer().getAnnotationLayer();
		CreationSupport.createRectangleItem(layer, new Rectangle(50, 50, 100, 100));
	}

	/**
	 * Main method to launch the application
	 * @param args command-line arguments (not used)
	 */
	public static void main(String[] args) {
	    BaseMDIApplication.launch(() ->
	        new HelloMDI(
	            PropertyUtils.TITLE, "Hello MDI",
	            PropertyUtils.FRACTION, 0.8
	        )
	    );
	}
}
