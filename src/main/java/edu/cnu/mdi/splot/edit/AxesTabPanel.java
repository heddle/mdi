package edu.cnu.mdi.splot.edit;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import edu.cnu.mdi.component.CommonBorder;
import edu.cnu.mdi.splot.plot.PlotCanvas;
import edu.cnu.mdi.splot.plot.PlotParameters;

/**
 * Tab for axis behavior: limits, scaling (linear/log), include-zero, tick formatting.
 */
@SuppressWarnings("serial")
public class AxesTabPanel extends JPanel {

    private final PlotParameters _params;

	private final AxesLimitsPanel _limitsPanel;

	private final JCheckBox _includeXZero;
	private final JCheckBox _includeYZero;

	private final JCheckBox _reverseX;
	private final JCheckBox _reverseY;

	private final JCheckBox _logX;
	private final JCheckBox _logY;

	private final JSpinner _decX;
	private final JSpinner _expX;
	private final JSpinner _decY;
	private final JSpinner _expY;

	// snapshot for deciding if world system must be recomputed
	private final boolean _xZero0, _yZero0;
	private final boolean _xLog0, _yLog0;

    public AxesTabPanel(PlotCanvas canvas) {
        _params = canvas.getParameters();

		setBorder(new CommonBorder("Axes"));
		setLayout(new GridBagLayout());

		_xZero0 = _params.includeXZero();
		_yZero0 = _params.includeYZero();

        boolean _xReverse0 = _params.isReverseXaxis();
        boolean _yReverse0 = _params.isReverseYaxis();

		_xLog0 = (_params.getXScale() == PlotParameters.AxisScale.LOG10);
		_yLog0 = (_params.getYScale() == PlotParameters.AxisScale.LOG10);

        int _decX0 = _params.getNumDecimalX();
        int _expX0 = _params.getMinExponentX();
        int _decY0 = _params.getNumDecimalY();
        int _expY0 = _params.getMinExponentY();

		_limitsPanel = new AxesLimitsPanel(canvas);

		_includeXZero = new JCheckBox("Include X=0 in auto limits", _xZero0);
		_includeYZero = new JCheckBox("Include Y=0 in auto limits", _yZero0);

		_reverseX = new JCheckBox("Reverse X axis", _xReverse0);
		_reverseY = new JCheckBox("Reverse Y axis", _yReverse0);

		_logX = new JCheckBox("Log X (base 10)", _xLog0);
		_logY = new JCheckBox("Log Y (base 10)", _yLog0);

		// When log is selected, include-zero is meaningless/invalid.
		_logX.addActionListener(e -> updateForLogSelection());
		_logY.addActionListener(e -> updateForLogSelection());

		_decX = new JSpinner(new SpinnerNumberModel(_decX0, 0, 10, 1));
		_expX = new JSpinner(new SpinnerNumberModel(_expX0, 0, 12, 1));
		_decY = new JSpinner(new SpinnerNumberModel(_decY0, 0, 10, 1));
		_expY = new JSpinner(new SpinnerNumberModel(_expY0, 0, 12, 1));

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 6, 4, 6);
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;

		// Limits editor
		c.gridwidth = 2;
		add(_limitsPanel, c);

		c.gridwidth = 1;

		// Row: include-zero + reverse (X)
		c.gridy++;
		c.gridx = 0;
		add(_includeXZero, c);
		c.gridx = 1;
		add(_reverseX, c);

		// Row: include-zero + reverse (Y)
		c.gridy++;
		c.gridx = 0;
		add(_includeYZero, c);
		c.gridx = 1;
		add(_reverseY, c);

		// Row: log toggles
		c.gridy++;
		c.gridx = 0;
		add(_logX, c);
		c.gridx = 1;
		add(_logY, c);

		// Tick formatting block
		c.gridy++;
		c.gridx = 0;
		add(new JLabel("X ticks: decimals"), c);
		c.gridx = 1;
		add(_decX, c);

		c.gridy++;
		c.gridx = 0;
		add(new JLabel("X ticks: sci exponent threshold"), c);
		c.gridx = 1;
		add(_expX, c);

		c.gridy++;
		c.gridx = 0;
		add(new JLabel("Y ticks: decimals"), c);
		c.gridx = 1;
		add(_decY, c);

		c.gridy++;
		c.gridx = 0;
		add(new JLabel("Y ticks: sci exponent threshold"), c);
		c.gridx = 1;
		add(_expY, c);

		// initial enforcement
		updateForLogSelection();
	}

	private void updateForLogSelection() {
		boolean xlog = _logX.isSelected();
		boolean ylog = _logY.isSelected();

		// log axes cannot include zero (and really shouldn't auto-include 0 anyway)
		_includeXZero.setEnabled(!xlog);
		if (xlog) {
			_includeXZero.setSelected(false);
		}

		_includeYZero.setEnabled(!ylog);
		if (ylog) {
			_includeYZero.setSelected(false);
		}
	}

	/**
	 * Apply changes.
	 *
	 * @return true if changes require recomputing the world system
	 */
	public boolean apply() {

		// Apply limits/methods first (may set manual ranges)
		_limitsPanel.apply();

		// Apply scale before include-zero, since include-zero is disabled in log mode.
		_params.setXScale(_logX.isSelected() ? PlotParameters.AxisScale.LOG10 : PlotParameters.AxisScale.LINEAR);
		_params.setYScale(_logY.isSelected() ? PlotParameters.AxisScale.LOG10 : PlotParameters.AxisScale.LINEAR);

		_params.includeXZero(_includeXZero.isSelected());
		_params.includeYZero(_includeYZero.isSelected());

		_params.setReverseXaxis(_reverseX.isSelected());
		_params.setReverseYaxis(_reverseY.isSelected());

		_params.setNumDecimalX(((Number) _decX.getValue()).intValue());
		_params.setMinExponentX(((Number) _expX.getValue()).intValue());
		_params.setNumDecimalY(((Number) _decY.getValue()).intValue());
		_params.setMinExponentY(((Number) _expY.getValue()).intValue());

		// Recompute world system if:
		//  - include-zero toggled (linear only)
		//  - log scale toggled (either axis)
		boolean includeZeroChanged = (_xZero0 != _includeXZero.isSelected()) || (_yZero0 != _includeYZero.isSelected());
		boolean scaleChanged = (_xLog0 != _logX.isSelected()) || (_yLog0 != _logY.isSelected());

		return includeZeroChanged || scaleChanged;
	}
}
