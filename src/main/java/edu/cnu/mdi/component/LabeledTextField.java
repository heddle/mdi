package edu.cnu.mdi.component;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class LabeledTextField extends JPanel {

	private JTextField _textField;
	private JLabel _jLabel;


	/**
	 * Create a labeled text field.
	 *
	 * @param label  the label to serve as a prompt.
	 * @param numcol the default number of columns in the text field.
	 * @param font the font for prompt and text field
	 */
	public LabeledTextField(String label, int numcol, Font font) {
		this(label, null, numcol, font);
	}

	/**
	 * Create a labeled text field.
	 *
	 * @param label  the label to serve as a prompt.
	 * @param numcol the default number of columns in the text field.
	 * @param font the font for prompt and text field
	 */
	public LabeledTextField(String label, String units, int numcol, Font font) {
		this(label, numcol);
		_textField.setFont(font);
		_jLabel.setFont(font);

		if (units != null) {
			JLabel unitLabel = new JLabel(units);
			unitLabel.setFont(font);
			add(unitLabel);
		}
	}


	/**
	 * Create a labeled text field.
	 *
	 * @param label  the label to serve as a prompt.
	 * @param numcol the default number of columns in the text field.
	 */
	public LabeledTextField(String label, int numcol) {

		setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
		_jLabel = new JLabel(label);
		add(_jLabel);
		_textField = new JTextField(numcol);
		add(_textField);
	}

	/**
	 * A Labeleled text field with the label given a fixed width to make things line up
	 * @param label the label
	 * @param labelWidth the preferred width
	 * @param numcol the number of columns
	 */
	public LabeledTextField(String label, int labelWidth, int numcol) {
		this(label, numcol);

		Dimension d = _jLabel.getPreferredSize();
		d.width = labelWidth;
		_jLabel.setPreferredSize(d);
	}

	/**
	 * Set the text in the text field.
	 *
	 * @param s the text to place in the text field.
	 */
	public void setText(String s) {
		_textField.setText(s);
	}

	/**
	 * Get the text from the text field.
	 *
	 * @return the text from the text field.
	 */
	public String getText() {
		return _textField.getText();
	}

	/**
	 * Get the underlying text field.
	 *
	 * @return the underlying text field.
	 */
	public JTextField getTextField() {
		return _textField;
	}


	/**
	 * Get the prompt label
	 * @return the prompt label
	 */
	public JLabel getPrompt() {
		return _jLabel;
	}

	@Override
	public void setEnabled(boolean enabled) {
		_textField.setEnabled(enabled);
	}
}
