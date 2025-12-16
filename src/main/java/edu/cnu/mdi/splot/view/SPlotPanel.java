package edu.cnu.mdi.splot.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import edu.cnu.mdi.splot.model.Plot2D;

/**
 * Simple panel that hosts {@link SPlotCanvas} in the center and uses
 * NORTH/WEST/SOUTH for title/labels/status.
 */
@SuppressWarnings("serial")
public class SPlotPanel extends JPanel {

    private final Plot2D plot;
    private final SPlotCanvas canvas;

    private final JLabel titleLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel xLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel yLabel = new JLabel(" ", SwingConstants.CENTER); // rotate later if you want
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.LEFT);

    public SPlotPanel(Plot2D plot, Plot2DRenderer renderer) {
        this.plot = plot;
        this.canvas = new SPlotCanvas(plot, renderer);

        setLayout(new BorderLayout(0, 0));
        add(canvas, BorderLayout.CENTER);

        // NORTH: title (and later: toolbar)
        titleLabel.setText(plot.getTitle());
        add(titleLabel, BorderLayout.NORTH);

        // SOUTH: x label + status
        JPanel south = new JPanel(new BorderLayout());
        xLabel.setText(plot.getXAxis().getDisplayLabel());
        south.add(xLabel, BorderLayout.NORTH);

        statusLabel.setPreferredSize(new Dimension(10, 24));
        south.add(statusLabel, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        // WEST: y label
        yLabel.setText(plot.getYAxis().getDisplayLabel());
        add(yLabel, BorderLayout.WEST);

        // Mouse feedback -> status
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                statusLabel.setText(canvas.getLocationString());
            }
        });
    }

    public SPlotCanvas getCanvas() {
        return canvas;
    }
}
