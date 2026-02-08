package edu.cnu.mdi.pseudo3D;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;

import edu.cnu.mdi.splot.pdata.Histo2DData;
import edu.cnu.mdi.splot.plot.PlotCanvas;
import edu.cnu.mdi.ui.colors.ScientificColorMap;

/**
 * Simple container panel for the pseudo-3D 2D-histogram renderer.
 * <p>
 * This panel exists primarily so PlotView can swap it in via CardLayout.
 */
@SuppressWarnings("serial")
public class Histo2DPanel extends JPanel implements PropertyChangeListener {

	private Histogram2D hist;
 
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
}
