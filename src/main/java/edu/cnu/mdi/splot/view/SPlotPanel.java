package edu.cnu.mdi.splot.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import edu.cnu.mdi.splot.model.Plot2D;
import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * Simple panel that hosts {@link SPlotCanvas} in the center and uses
 * NORTH/WEST/SOUTH for title/labels/status.
 */
@SuppressWarnings("serial")
public class SPlotPanel extends JPanel {

    private final Plot2D plot;
    private final SPlotCanvas canvas;
    private final PlotTheme theme;
    private Plot2DRenderer renderer;
    private static final Font statusFont = Fonts.mediumFont;

    private final JLabel statusLabel = new JLabel(" ", SwingConstants.LEFT);

    public SPlotPanel(Plot2D plot, Plot2DRenderer renderer) {
        this.plot = plot;
        this.renderer = renderer;
        this.canvas = new SPlotCanvas(plot, renderer);
        this.theme = renderer.getTheme();

        setLayout(new BorderLayout(0, 0));
        add(canvas, BorderLayout.CENTER);


        statusLabel.setFont(Fonts.smallFont);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(Color.black);
        statusLabel.setForeground(Color.cyan);
        statusLabel.setPreferredSize(new Dimension(10, getFontMetrics(statusFont).getHeight() + 6));
        add(statusLabel, BorderLayout.SOUTH);

 
        // Mouse feedback -> status
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                statusLabel.setText(" " + canvas.getLocationString());
            }
        });
    }
    
    /**
	 * Get the Plot2D model.
	 * 
	 * @return the Plot2D model.
	 */
    public Plot2D getPlot() {
		return plot;
	}
    
    /**
	 * Get the PlotTheme in use.
	 */
    public PlotTheme getTheme() {
		return theme;
	}
    
    public Plot2DRenderer getRenderer() {
    	return renderer;
    }

    public SPlotCanvas getCanvas() {
        return canvas;
    }
}
