package edu.cnu.mdi.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.feedback.IFeedbackProvider;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.item.ImageItem;
import edu.cnu.mdi.swing.WindowPlacement;
import edu.cnu.mdi.transfer.ImageFilters;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * A simple view used to test the tool bar.
 *
 * @author heddle
 */
@SuppressWarnings("serial")
public class DrawingView extends BaseView implements IFeedbackProvider {

	/**
	 * Construct a {@code DrawingView} from alternating key/value property pairs.
	 *
	 * <p>File drag-and-drop for image files is enabled automatically via
	 * {@link #enableFileDrop(java.util.function.Predicate)}.  The filter used
	 * is {@link ImageFilters#isActualImage}, which verifies a file is a
	 * readable image by attempting to decode it — slower than an extension
	 * check but immune to mis-named files.</p>
	 *
	 * @param keyVals alternating {@link edu.cnu.mdi.util.PropertyUtils}
	 *                key/value pairs
	 */
	public DrawingView(Object... keyVals) {
		super(PropertyUtils.fromKeyValues(keyVals));
		enableFileDrop(ImageFilters.isActualImage);
		initFeedback();
	}

	/**
	 * Convenience method for creating a Drawing View with a square canvas.
	 * <p>
	 * The BaseView constructor calls pack() and desktop.add(), both of which
	 * affect frame sizing. setVisible(true) is deferred via invokeLater.
	 * We defer our chrome measurement and setSize() to a second invokeLater,
	 * which runs after setVisible has completed, giving us the true realized
	 * component sizes.
	 * </p>
	 */
	public static DrawingView createDrawingView() {

		Dimension d = WindowPlacement.screenFraction(0.4);
		final int width = d.width;
		final int height = d.height + 100;

		long toolBits = ToolBits.STATUS | ToolBits.DRAWINGTOOLS
				| ToolBits.ZOOMTOOLS | ToolBits.PAN | ToolBits.INFO;

		DrawingView view = new DrawingView(
				PropertyUtils.WORLDSYSTEM, new Rectangle2D.Double(0.0, 0.0, 1.0, 1.0),
				PropertyUtils.WIDTH,       width,
				PropertyUtils.HEIGHT,      height,
				PropertyUtils.TOOLBARBITS, toolBits,
				PropertyUtils.VISIBLE,     true,
				PropertyUtils.BACKGROUND,  Color.white,
				PropertyUtils.TITLE,       "Drawing View");

		// BaseView defers setVisible via its own invokeLater. We queue our
		// resize AFTER that by nesting a second invokeLater — it is guaranteed
		// to run after the first one has completed, so the frame is fully
		// realized and component sizes are the ground truth.
		SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() -> {
			Dimension frameSize     = view.getSize();
			Dimension containerSize = view.getIContainer().getComponent().getSize();
			int chromeW = frameSize.width  - containerSize.width;
			int chromeH = frameSize.height - containerSize.height;
			view.setSize(width + chromeW, height + chromeH);
		}));

		return view;
	}

	@Override
	public AbstractViewInfo getViewInfo() {
		return new DrawingViewInfo();
	}

	/**
	 * Handle image files dropped onto this view.
	 *
	 * <p>Only the first file in the list is used; multi-file drops are accepted
	 * by the framework but silently truncated here because a drawing view has a
	 * single canvas and loading several images simultaneously would require an
	 * explicit layout policy that does not yet exist.</p>
	 *
	 * <p>The dropped image is placed on the annotation layer so it is always
	 * rendered above other items.  If the file cannot be decoded
	 * ({@link ImageIO#read} returns {@code null} or throws), an error is
	 * logged to {@code System.err} and the view is left unchanged.</p>
	 *
	 * <p>This method is called on the EDT by {@link edu.cnu.mdi.transfer.FileDropHandler}
	 * after the filter set in the constructor ({@link edu.cnu.mdi.transfer.ImageFilters#isActualImage})
	 * has already accepted the file, so no second format check is needed
	 * here.</p>
	 *
	 * @param files the accepted dropped files; never {@code null}, never empty
	 */
	@Override
	public void filesDropped(List<File> files) {
		if (files == null || files.isEmpty()) return;
		File file = files.get(0);
		try {
			BufferedImage img = ImageIO.read(file);
			IContainer container = getIContainer();
			new ImageItem(container.getAnnotationLayer(), null, img);
			refresh();
		} catch (IOException e) {
			System.err.println("Error reading image file: " + e.getMessage());
		}
	}
}