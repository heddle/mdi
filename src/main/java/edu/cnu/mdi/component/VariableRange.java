package edu.cnu.mdi.component;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.util.Random;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class VariableRange extends JPanel {

	//shared random number generator
	private static Random _rand = new Random();

	//to make the prompts uniform width
	private int _measureWidth;

	//the limit value text fields
	private JTextField _minValue;
	private JTextField _maxValue;

	//cache last good values for bad entry recovery
	private double _lastGoodMin;
	private double _lastGoodMax;



	public VariableRange(String prompt, String units, String measureString, Font font, double minVal, double maxVal) {
		setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));

		_lastGoodMin = minVal;
		_lastGoodMax = maxVal;


		FontMetrics fm = this.getFontMetrics(font);
		_measureWidth = Math.max(20, fm.stringWidth(measureString));

		add(createPrompt(prompt, font));

		_minValue = new JTextField(valStr(minVal), 7);
		_maxValue = new JTextField(valStr(maxVal), 7);

		_minValue.setFont(font);
		_maxValue.setFont(font);


		add (_minValue);
		add (makeLabel(" to ", font));
		add (_maxValue);
		add (makeLabel(units, font));

	}

	//create the prompt as wide as the measure width
	private JLabel createPrompt(String prompt, Font font) {
		JLabel label;

		label = new JLabel(prompt, SwingConstants.RIGHT) {

			@Override
			public Dimension getPreferredSize() {
				Dimension d = super.getPreferredSize();
				d.width = _measureWidth;
				return d;
			}
		};

		label.setFont(font);

		return label;
	}

	private JLabel makeLabel(String s, Font font) {
		JLabel label = new JLabel(s);
		label.setFont(font);
		return label;
	}


	private String valStr(double v) {
		String s = String.format("%-9.3f", v);
		return s.trim();
	}

	//get the min value, watch for bad input
	private double getMinValue() {
		try {
			double v = Double.parseDouble(_minValue.getText());
			_lastGoodMin = v;
			return v;
		}
		catch (Exception e) {
			_minValue.setText(valStr(_lastGoodMin));
			return _lastGoodMin;
		}
	}

	//get the max value, watch for bad input
	private double getMaxValue() {
		try {
			double v = Double.parseDouble(_maxValue.getText());
			_lastGoodMax = v;
			return v;
		}
		catch (Exception e) {
			_maxValue.setText(valStr(_lastGoodMax));
			return _lastGoodMax;
		}
	}

	/**
	 * Get a random number corresponding to the range
	 * @return a random number corresponding to the range
	 */
	public double nextRandom() {
		double minV = getMinValue();
		double maxV = getMaxValue();

		if (Math.abs(minV - maxV) < 1.0e-16) {
			return minV;
		}

		return minV + (maxV - minV) * _rand.nextDouble();
	}


}
