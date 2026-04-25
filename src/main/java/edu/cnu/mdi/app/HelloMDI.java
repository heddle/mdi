package edu.cnu.mdi.app;

import java.awt.Color;
import java.awt.geom.Rectangle2D;

import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.DrawingView;

/**
 * Simple "Hello World" demo for the MDI creates a single drawing view and
 * demonstrates the use of the PropertyUtils key-value system to configure
 * views. Also adds
 */

@SuppressWarnings("serial")
public class HelloMDI extends BaseMDIApplication {

	/** Singleton instance of the demo app. */
	private static HelloMDI INSTANCE;

	/**
	 * Constructor.
	 *
	 * @param keyVals key-value pairs for configuring the Hello MDI application
	 */
	private HelloMDI(Object... keyVals) {
		super(keyVals);

		// Create internal views.
		addInitialViews();
	}

	/**
	 * Public access to the singleton.
	 *
	 * @return the singleton main application frame
	 */
	public static HelloMDI getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new HelloMDI(PropertyUtils.TITLE, "Hello MDI", 
					PropertyUtils.FRACTION, 0.8);
		}
		return INSTANCE;
	}

	// This method creates and registers the initial set of views.
	//For this demo, only one view is created.
	private void addInitialViews() {
		// specify (bitwise) what will be on the toolbar
		long toolBits = ToolBits.INFO | ToolBits.STATUS | ToolBits.DRAWINGTOOLS | ToolBits.ZOOMTOOLS | ToolBits.PAN;

		new DrawingView(
				PropertyUtils.FRACTION, 0.8,
				PropertyUtils.TOOLBARBITS, toolBits, 
				PropertyUtils.TITLE, "Drawing View");
	}

	/**
	 * Main method to launch the application
	 * 
	 * @param args command-line arguments (not used)
	 */
	public static void main(String[] args) {
		BaseMDIApplication.launch(HelloMDI::getInstance);
	}
}
