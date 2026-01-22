package edu.cnu.mdi.splot.edit;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import edu.cnu.mdi.component.CommonBorder;
import edu.cnu.mdi.component.FontChoosePanel;
import edu.cnu.mdi.splot.plot.PlotCanvas;
import edu.cnu.mdi.splot.plot.PlotParameters;
import edu.cnu.mdi.ui.colors.ColorLabel;

/**
 * Tab for legend settings.
 */
@SuppressWarnings("serial")
public class LegendTabPanel extends JPanel {

	private final PlotParameters _params;

	private JCheckBox _draw;
	private JCheckBox _border;
	private JSpinner _lineLen;

	private final FontChoosePanel _font;

	private ColorLabel _fill;
	private ColorLabel _text;
	private ColorLabel _stroke;

	public LegendTabPanel(PlotCanvas canvas) {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		_params = canvas.getParameters();

		_draw = new JCheckBox("Draw legend", _params.isLegendDrawn());
		_border = new JCheckBox("Draw legend border", _params.isLegendBorder());
		_lineLen = new JSpinner(new SpinnerNumberModel(_params.getLegendLineLength(), 10, 500, 5));

		JPanel legendDrawingPanel = legendDrawingPanel();
		_font = new FontChoosePanel("Legend font", _params.getTextFont());
		JPanel colors = colorPanel();

		legendDrawingPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		_font.setAlignmentX(Component.LEFT_ALIGNMENT);
		colors.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		add(legendDrawingPanel);
		add(_font);
		add(colors);

	}

	private JPanel legendDrawingPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		_draw = new JCheckBox("Draw legend", _params.isLegendDrawn());
		_border = new JCheckBox("Draw legend border", _params.isLegendBorder());

		_draw.setAlignmentX(Component.LEFT_ALIGNMENT);
		_border.setAlignmentX(Component.LEFT_ALIGNMENT);

		JPanel lineLenPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
		lineLenPanel.add(new javax.swing.JLabel("Legend line length"));

		_lineLen = new JSpinner(new SpinnerNumberModel(_params.getLegendLineLength(), 10, 500, 5));
		lineLenPanel.add(_lineLen);

		lineLenPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		panel.add(_draw);
		panel.add(_border);
		panel.add(lineLenPanel);

		panel.setBorder(new CommonBorder("Legend drawing options"));
		return panel;

	}

	/**
	 * Create the color selection panel.
	 *
	 * @return the color selection panel.
	 */
	private JPanel colorPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
		_fill = new ColorLabel((src, col) -> {
		}, _params.getTextBackground(), getFont(), "Fill");
		_text = new ColorLabel((src, col) -> {
		}, _params.getTextForeground(), getFont(), "Text");
		_stroke = new ColorLabel((src, col) -> {
		}, _params.getTextBorderColor(), getFont(), "Border");
		panel.add(_fill);
		panel.add(_text);
		panel.add(_stroke);
		panel.setBorder(new CommonBorder("Legend text colors"));
		return panel;
	}

	/**
	 * Apply the settings to the plot parameters.
	 */
	public void apply() {
		_params.setLegendDrawing(_draw.isSelected());
		_params.setLegendBorder(_border.isSelected());
		_params.setLegendLineLength(((Number) _lineLen.getValue()).intValue());

		_params.setTextFont(_font.getSelectedFont());
		_params.setTextBackground(_fill.getColor());
		_params.setTextForeground(_text.getColor());
		_params.setTextBorderColor(_stroke.getColor());
	}
}
