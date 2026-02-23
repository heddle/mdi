package edu.cnu.mdi.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.feedback.IFeedbackProvider;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.item.ImageItem;
import edu.cnu.mdi.properties.PropertyUtils;
import edu.cnu.mdi.swing.WindowPlacement;
import edu.cnu.mdi.transfer.ImageFilters;

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
	public DrawingView(Object... keyVals) {
		super(PropertyUtils.fromKeyValues(keyVals));
		getContainer().getFeedbackControl().addFeedbackProvider(this);
		setFileFilter(ImageFilters.isActualImage);
		initFeedback();
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
		long toolBits = ToolBits.STATUS | ToolBits.DRAWINGTOOLS | ToolBits.ZOOMTOOLS | ToolBits.PAN;
		view = new DrawingView(PropertyUtils.WORLDSYSTEM, new Rectangle2D.Double(0.0, 0.0, width, height),
				PropertyUtils.WIDTH, width, // container width, not total view width
				PropertyUtils.HEIGHT, height, // container height, not total view width
				PropertyUtils.TOOLBARBITS, toolBits, PropertyUtils.VISIBLE, true,
				PropertyUtils.BACKGROUND, Color.white, PropertyUtils.TITLE,
				"Drawing View ", PropertyUtils.STANDARDVIEWDECORATIONS, true,
				PropertyUtils.INFOBUTTON, true);

		view.pack();
		return view;
	}
	
	/**
	 *  Get an information object describing this view,
	 *  used in the UI and for help.
	 */
	@Override
	public AbstractViewInfo getViewInfo() {
		return new DrawingViewInfo();
	}


	/**
	 * Handle files dropped on this view through drag and drop.
	 *
	 * @param files the dropped files.
	 */
	@Override
	public void filesDropped(List<File> files) {
		if (files == null || files.isEmpty()) {
			return;
		}
		File file = files.get(0);
		try {
			BufferedImage img = ImageIO.read(file);
			IContainer container = getContainer();
			new ImageItem(container.getAnnotationLayer(), null, img);
			refresh();
		} catch (IOException e) {
			System.err.println("Error reading image file: " + e.getMessage());
		}
	}


}
