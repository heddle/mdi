package edu.cnu.mdi.splot.plot;

import java.awt.Rectangle;
import java.util.Objects;

import javax.swing.SwingUtilities;

import edu.cnu.mdi.graphics.rubberband.ARubberband;
import edu.cnu.mdi.graphics.toolbar.BaseToolBar;
import edu.cnu.mdi.graphics.toolbar.DefaultToolHandler;
import edu.cnu.mdi.graphics.toolbar.GestureContext;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.splot.pdata.PlotDataType;
import edu.cnu.mdi.view.BaseView;


public class PlotToolHandler extends DefaultToolHandler {

	private static final double ZOOM_FACTOR = 0.85;

	// Toolbar that owns this tool handler
	private BaseToolBar toolBar;

	/**
	 * Create a tool handler for the given plot panel.
	 *
	 * @param plotPanel Plot panel
	 */
	public PlotToolHandler(PlotPanel plotPanel) {
		Objects.requireNonNull(plotPanel, "plotPanel");
		PlotCanvas plotCanvas = plotPanel.getPlotCanvas();
		Objects.requireNonNull(plotCanvas, "plotCanvas");

		//what is on the toolbar depends on type
		long bits = 0;
		PlotDataType type = plotCanvas.getType();
		if (type != PlotDataType.STRIP) {
			bits = ToolBits.POINTER | ToolBits.PLOTTOOLS;
			bits = bits & ~ToolBits.UNDOZOOM; //no undo zoom for now
		}
		else {
			bits = ToolBits.POINTER | ToolBits.PICVIEWSTOOLS;
		}

		ARubberband.Policy pointerPolicy = ARubberband.Policy.NONE;
		//histograms have x only policy
		ARubberband.Policy boxZoomPolicy = (type == PlotDataType.H1D) ? ARubberband.Policy.XONLY : ARubberband.Policy.RECTANGLE_PRESERVE_ASPECT;
		toolBar = new BaseToolBar(plotCanvas, this, bits, pointerPolicy, boxZoomPolicy);
	}

	/**
	 * Get the toolbar associated with this tool handler.
	 *
	 * @return Toolbar
	 */
	public BaseToolBar getToolBar() {
		return toolBar;
	}
	
	@Override
	public void info(GestureContext gc) {
		BaseView view = (BaseView) SwingUtilities.getAncestorOfClass(BaseView.class, gc.getCanvas());
		if (view != null) {
			view.viewInfo();
		}
	}


	@Override
	public void boxZoomRubberbanding(GestureContext gc, Rectangle bounds) {
		PlotCanvas plotCanvas = (PlotCanvas) gc.getCanvas();
		plotCanvas.zoomToRect(bounds);
	}

	@Override
	public void zoomIn(GestureContext gc) {
		PlotCanvas plotCanvas = (PlotCanvas) gc.getCanvas();
		plotCanvas.scale(ZOOM_FACTOR);
	}

	@Override
	public void zoomOut(GestureContext gc) {
		PlotCanvas plotCanvas = (PlotCanvas) gc.getCanvas();
		plotCanvas.scale(1.0 / ZOOM_FACTOR);
	}

	@Override
	public void resetZoom(GestureContext gc) {
		PlotCanvas plotCanvas = (PlotCanvas) gc.getCanvas();
		plotCanvas.setWorldSystem();
		plotCanvas.repaint();
	}


}
