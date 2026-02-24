package edu.cnu.mdi.pseudo3D;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;

import edu.cnu.mdi.graphics.toolbar.BaseToolBar;
import edu.cnu.mdi.graphics.toolbar.DefaultToolHandler;
import edu.cnu.mdi.graphics.toolbar.GestureContext;
import edu.cnu.mdi.graphics.toolbar.IToolHandler;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.splot.pdata.Histo2DData;
import edu.cnu.mdi.splot.plot.PlotCanvas;
import edu.cnu.mdi.ui.colors.ScientificColorMap;
import edu.cnu.mdi.util.PrintUtils;
import edu.cnu.mdi.util.TakePicture;

/**
 * Simple container panel for the pseudo-3D 2D-histogram renderer.
 * <p>
 * This panel exists primarily so PlotView can swap it in via CardLayout.
 */
@SuppressWarnings("serial")
public class Histo2DPanel extends JPanel implements PropertyChangeListener {

	// the histogram renderer
	private Histogram2D hist;
	
	private BaseToolBar toolbar;
	
	private IToolHandler toolHandler;

    /**
     * Create a pseudo-3D histogram panel.
     *
     * @param data     backing histogram data
     * @param colorMap colormap for pillar coloring
     * @param logZ     if {@code true}, use log10(1+Z) mapping for height and color
     */
    public Histo2DPanel(Histo2DData data, ScientificColorMap colorMap, boolean logZ) {
        setLayout(new BorderLayout());
        hist = new Histogram2D(data, colorMap);
        hist.setLogZ(logZ);
        add(hist, BorderLayout.CENTER);
        
        createToolHandler();
        addToolbar();
    }

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (PlotCanvas.LOGZCHANGE.equals(evt.getPropertyName())) {
			boolean logZ = (boolean) evt.getNewValue();
			// Update the logZ setting of the histogram renderer
			hist.setLogZ(logZ);
		}
		else if (PlotCanvas.COLORMAPCHANGE.equals(evt.getPropertyName())) {
			ScientificColorMap colorMap = (ScientificColorMap) evt.getNewValue();
			// Update the colormap of the histogram renderer
			hist.setColorMap(colorMap);
		}
	}
	
	private void createToolHandler() {
		toolHandler = new DefaultToolHandler() {
			@Override
			public void captureImage(GestureContext gc) {
				if (hist == null) {
					return;
				}
				TakePicture.takePicture(hist);
			}

			@Override
			public void print(GestureContext gc) {
				if (hist == null) {
					return;
				}
				PrintUtils.printComponent(hist);
			}

		};
	}
	
	// TODO: add a toolbar with buttons for captureImage and print,
	// and any other relevant actions
	private void addToolbar() {
		long bits = ToolBits.CAMERA | ToolBits.PRINTER;
		toolbar = new BaseToolBar(hist, toolHandler, bits);
		add(toolbar, BorderLayout.NORTH);
	}
}
