package edu.cnu.mdi.splot.plot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import edu.cnu.mdi.feedback.FeedbackPane;
import edu.cnu.mdi.graphics.toolbar.BaseToolBar;
import edu.cnu.mdi.util.PrintUtils;

@SuppressWarnings("serial")
public class PlotPanel extends JPanel implements PropertyChangeListener {

	// the underlying plot canvas
	private PlotCanvas _canvas;

	// title related
	private JLabel _titleLabel;

	// feedback pane
	private FeedbackPane _feedbackPane;

	// axes labels
	private JLabel _xLabel;
	private JLabel _yLabel;

	// how adorned
	public static int VERYBARE = -2;
	public static int BARE = 1;
	public static int STANDARD = 0;

	// toolbar
	protected BaseToolBar _toolbar;

	// decorations level
	protected int _decorations;

	/**
	 * Create a plot panel for a single xy dataset and a toolbar
	 *
	 * @param dataSet   the data set
	 * @param plotTitle the title of the plot
	 */
	public PlotPanel(PlotCanvas canvas) {
		this(canvas, STANDARD);
	}

	/**
	 * Create a plot panel for a single xy dataset
	 *
	 * @param dataSet     the data set
	 * @param plotTitle   the title of the plot
	 * @param decorations (stripped down panel?)
	 */
	public PlotPanel(PlotCanvas canvas, int decorations) {
		_canvas = canvas;
		_canvas.setParent(this);
		_decorations = decorations;

		setLayout(new BorderLayout(0, 0));

		_canvas.addPropertyChangeListener(this);
		add(_canvas, BorderLayout.CENTER);

		addSouth();
		addNorth();
		addWest();
	}

	@Override
	public Insets getInsets() {
		Insets def = super.getInsets();
		return new Insets(def.top + 2, def.left + 2, def.bottom + 2, def.right + 2);
	}

	// add the south component
	private void addSouth() {
		// south panel for x axis and status
		JPanel spanel = new JPanel();

		spanel.setLayout(new BorderLayout());
		spanel.setOpaque(true);
		spanel.setBackground(Color.white);
		PlotParameters parameters = _canvas.getParameters();

		// axes labels
		_xLabel = makeJLabel(parameters.getXLabel(), parameters.getAxesFont(), SwingConstants.CENTER, Color.white, null,
				false);
		_xLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		spanel.add(_xLabel, BorderLayout.CENTER);

		// status label
		if (_decorations == STANDARD) {
			_feedbackPane = new FeedbackPane();
			FontMetrics fm = getFontMetrics(getFont());
			int h = 3 * fm.getHeight() + 4;
			Dimension d = new Dimension(super.getPreferredSize().width, h);
			_feedbackPane.getViewport().setPreferredSize(d);
			spanel.add(_feedbackPane.getViewport(), BorderLayout.SOUTH);
		}

		add(spanel, BorderLayout.SOUTH);
	}

	// add the north component
	private void addNorth() {
		PlotParameters parameters = _canvas.getParameters();

		PlotToolHandler toolHandler = new PlotToolHandler(this);
		_toolbar = toolHandler.getToolBar();
		_canvas.setToolBar(_toolbar);
		_toolbar.setHandler(toolHandler);


		JPanel npanel = new JPanel();
		npanel.setOpaque(true);
		npanel.setBackground(Color.white);
		npanel.setLayout(new BorderLayout());

		// title label
		_titleLabel = makeJLabel(_canvas.getParameters().getPlotTitle(), parameters.getTitleFont(),
				SwingConstants.CENTER, Color.white, null, false);
		_titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		_toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);

		// if verybare, tool bar is hidden
		if (_decorations != VERYBARE) {
			npanel.add(_toolbar, BorderLayout.NORTH);
		}
		npanel.add(_titleLabel, BorderLayout.CENTER);
		add(npanel, BorderLayout.NORTH);
	}

	// add the west component
	private void addWest() {
		PlotParameters parameters = _canvas.getParameters();
		_yLabel = makeRotatedLabel(_canvas.getParameters().getYLabel(), parameters.getAxesFont(), Color.white, null);
		add(_yLabel, BorderLayout.WEST);
	}

	// convenience function for making a label
	private JLabel makeJLabel(String text, Font font, int alignment, Color bg, Color fg, boolean excludeFromPrint) {

		JLabel lab = null;
		if (excludeFromPrint) {
			lab = new JLabel(text != null ? text : " ") {
				@Override
				public void paint(Graphics g) {
					// exclude from print
					if (!PrintUtils.isPrinting()) {
						super.paint(g);
					}
				}
			};
		} else {
			lab = new JLabel(text != null ? text : " ");
		}
		lab.setFont(font);
		lab.setOpaque(true);
		if (bg != null) {
			lab.setBackground(bg);
		}
		if (fg != null) {
			lab.setForeground(fg);
		}
		lab.setHorizontalAlignment(alignment);
		lab.setVerticalAlignment(SwingConstants.CENTER);
		return lab;
	}

	// convenience function for making a rotated label for y axis label
	private JLabel makeRotatedLabel(String text, Font font, Color bg, Color fg) {
		JLabel lab = new JLabel(text);
		lab.setFont(font);
		lab.setOpaque(true);
		lab.setUI(new VerticalLabelUI());
		lab.setHorizontalAlignment(SwingConstants.CENTER);

		if (bg != null) {
			lab.setBackground(bg);
		}
		if (fg != null) {
			lab.setForeground(bg);
		}
		return lab;
	}

	/**
	 * Get the feedback pane
	 *
	 * @return the feedback pane
	 */
	public FeedbackPane getFeedbackPane() {
		return _feedbackPane;
	}

	/**
	 * Get the toolbar
	 *
	 * @return the toolbar
	 */
	public BaseToolBar getToolBar() {
		return _toolbar;
	}

	/**
	 * Get the underlying plot canvas
	 *
	 * @return the plot canvas
	 */
	public PlotCanvas getPlotCanvas() {
		return _canvas;
	}

	/**
	 * Get the plot parameters
	 *
	 * @return the plot parameters
	 */
	public PlotParameters getParameters() {
		return _canvas.getParameters();
	}

	public void setColor(Color bg) {
		super.setBackground(bg);
		_canvas.setBackground(bg);
		_titleLabel.setBackground(bg);
		_xLabel.setBackground(bg);
		_yLabel.setBackground(bg);
	}

	/**
	 * Does this panel hold a 2D histogram?
	 *
	 * @return true if it holds a 2D histogram
	 */
	public boolean holds2DHistogram() {
		return _canvas.getPlotData().isHisto2DData();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (PlotCanvas.TITLETEXTCHANGE.equals(evt.getPropertyName())) {
			_titleLabel.setText((String) evt.getNewValue());
			_titleLabel.repaint();
		} else if (PlotCanvas.XLABELTEXTCHANGE.equals(evt.getPropertyName())) {
			_xLabel.setText((String) evt.getNewValue());
			_xLabel.repaint();
		} else if (PlotCanvas.YLABELTEXTCHANGE.equals(evt.getPropertyName())) {
			_yLabel.setText((String) evt.getNewValue());
			_yLabel.repaint();
		} else if (PlotCanvas.TITLEFONTCHANGE.equals(evt.getPropertyName())) {
			Font font = (Font) evt.getNewValue();
			_titleLabel.setFont(font);
		} else if (PlotCanvas.AXESFONTCHANGE.equals(evt.getPropertyName())) {
			Font font = (Font) evt.getNewValue();
			_xLabel.setFont(font);
			_yLabel.setFont(font);
		} else if (PlotCanvas.STATUSFONTCHANGE.equals(evt.getPropertyName())) {
        }

	}


}
