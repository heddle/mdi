package edu.cnu.mdi.splot.plot;

import java.awt.Component;
import java.awt.Rectangle;
import java.util.Objects;

import edu.cnu.mdi.graphics.rubberband.Rubberband;
import edu.cnu.mdi.graphics.toolbar.AToolBar;
import edu.cnu.mdi.graphics.toolbar.BaseToolBar;
import edu.cnu.mdi.graphics.toolbar.DefaultToolHandler;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.splot.pdata.PlotDataType;


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
		
		Rubberband.Policy pointerPolicy = Rubberband.Policy.NONE;
		//histograms have x only policy
		Rubberband.Policy boxZoomPolicy = (type == PlotDataType.H1D) ? Rubberband.Policy.XONLY : Rubberband.Policy.RECTANGLE_PRESERVE_ASPECT;
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
	public void boxZoomRubberbanding(AToolBar toolBar, Component canvas, Rectangle bounds) {
		PlotCanvas plotCanvas = (PlotCanvas) canvas;
		plotCanvas.zoomToRect(bounds);
	}

	@Override
	public void zoomIn(AToolBar toolBar, Component canvas) {
		PlotCanvas plotCanvas = (PlotCanvas) canvas;
		plotCanvas.scale(ZOOM_FACTOR);
	}

	@Override
	public void zoomOut(AToolBar toolBar, Component canvas) {
		PlotCanvas plotCanvas = (PlotCanvas) canvas;
		plotCanvas.scale(1.0 / ZOOM_FACTOR);
	}

	@Override
	public void resetZoom(AToolBar toolBar, Component canvas) {
		PlotCanvas plotCanvas = (PlotCanvas) canvas;
		plotCanvas.setWorldSystem();
		plotCanvas.repaint();
	}


}
