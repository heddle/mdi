package edu.cnu.mdi.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.feedback.IFeedbackProvider;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.properties.PropertySupport;
import edu.cnu.mdi.swing.WindowPlacement;

/**
 * A simple view used to test the tool bar.
 *
 * @author heddle
 *
 */
@SuppressWarnings("serial")
public class DrawingView extends BaseView implements IFeedbackProvider {

	/**
	 * Create a drawing view
	 *
	 * @param keyVals variable set of arguments.
	 */
	private DrawingView(Object... keyVals) {
		super(PropertySupport.fromKeyValues(keyVals));
		getContainer().getFeedbackControl().addFeedbackProvider(this);
	}

	/**
	 * Convenience method for creating a Drawing View.
	 *
	 * @return a new DrawingView object
	 */
	public static DrawingView createDrawingView() {
		DrawingView view = null;

		// set to a fraction of screen
		Dimension d = WindowPlacement.screenFraction(0.4);

		int width = d.width;
		int height = d.height;

		// create the view
		long toolBits = ToolBits.DRAWINGTOOLS | ToolBits.ZOOMTOOLS | ToolBits.PAN;
		view = new DrawingView(PropertySupport.WORLDSYSTEM, new Rectangle2D.Double(0.0, 0.0, width, height),
				PropertySupport.WIDTH, width, // container width, not total view width
				PropertySupport.HEIGHT, height, // container height, not total view width
				PropertySupport.TOOLBARBITS, toolBits, PropertySupport.VISIBLE, true,
				PropertySupport.PROPNAME, "DRAWING", PropertySupport.BACKGROUND, Color.white, PropertySupport.TITLE,
				"Drawing View ", PropertySupport.STANDARDVIEWDECORATIONS, true);

		view.pack();
		return view;
	}

	/**
	 * Some view specific feedback. Should always call super.getFeedbackStrings
	 * first.
	 *
	 * @param container   the base container for the view.
	 * @param screenPoint the pixel point
	 * @param worldPoint  the corresponding world location.
	 */
	@Override
	public void getFeedbackStrings(IContainer container, Point pp, Point2D.Double wp, List<String> feedbackStrings) {

	}

}
