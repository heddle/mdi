package edu.cnu.mdi.splot.edit;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JTextField;

import edu.cnu.mdi.component.CommonBorder;
import edu.cnu.mdi.component.FontChoosePanel;
import edu.cnu.mdi.splot.plot.PlotCanvas;
import edu.cnu.mdi.splot.plot.PlotParameters;

/**
 * Tab for plot title/labels and related fonts.
 */
@SuppressWarnings("serial")
public class LabelsTabPanel extends JPanel {

	private final PlotParameters _params;

	private final JTextField _titleTF;
	private final JTextField _xLabelTF;
	private final JTextField _yLabelTF;

	private FontChoosePanel _titleFont;
	private FontChoosePanel _axesFont;
	private FontChoosePanel _statusFont;

	public LabelsTabPanel(PlotCanvas canvas) {
		_params = canvas.getParameters();

		setBorder(new CommonBorder("Labels"));
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 6, 4, 6);
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;

		_titleTF = new JTextField(_params.getPlotTitle(), 40);
		_xLabelTF = new JTextField(_params.getXLabel(), 40);
		_yLabelTF = new JTextField(_params.getYLabel(), 40);

		int row = 0;

		c.gridx = 0;
		c.gridy = row;
		add(new javax.swing.JLabel("Plot title"), c);
		c.gridx = 1;
		c.gridy = row++;
		add(_titleTF, c);

		c.gridx = 0;
		c.gridy = row;
		add(new javax.swing.JLabel("X axis label"), c);
		c.gridx = 1;
		c.gridy = row++;
		add(_xLabelTF, c);

		c.gridx = 0;
		c.gridy = row;
		add(new javax.swing.JLabel("Y axis label"), c);
		c.gridx = 1;
		c.gridy = row++;
		add(_yLabelTF, c);

		c.gridx = 0;
		c.gridy = row;
		c.gridwidth = 3;

		JPanel fontPanel = createFontPanel();
		add(fontPanel, c);
	}

	// create the font selection panel
	private JPanel createFontPanel() {
		JPanel fontPanel = new JPanel();
		fontPanel.setLayout(new GridLayout(1, 3, 10, 10));
		_titleFont = new FontChoosePanel("Title font", _params.getTitleFont());
		_axesFont = new FontChoosePanel("Axes label font", _params.getAxesFont());
		_statusFont = new FontChoosePanel("Status font", _params.getStatusFont());

		fontPanel.add(_titleFont);
		fontPanel.add(_axesFont);
		fontPanel.add(_statusFont);
		return fontPanel;
	}
	public void apply() {
		_params.setPlotTitle(_titleTF.getText());
		_params.setXLabel(_xLabelTF.getText());
		_params.setYLabel(_yLabelTF.getText());

		_params.setTitleFont(_titleFont.getSelectedFont());
		_params.setAxesFont(_axesFont.getSelectedFont());
		_params.setStatusFont(_statusFont.getSelectedFont());
	}
}
