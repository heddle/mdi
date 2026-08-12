package edu.cnu.mdi.component;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** A text field accompanied by a prompt and optional units label. */
public class LabeledTextField extends JPanel {

	private final JTextField textField;
	private final JLabel prompt;

	public LabeledTextField(String label, int numColumns, Font font) {
		this(label, null, numColumns, font);
	}

	public LabeledTextField(String label, String units, int numColumns, Font font) {
		this(label, numColumns);
		textField.setFont(font);
		prompt.setFont(font);
		if (units != null) {
			JLabel unitLabel = new JLabel(units);
			unitLabel.setFont(font);
			add(unitLabel);
		}
	}

	public LabeledTextField(String label, int numColumns) {
		setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
		prompt = new JLabel(label);
		add(prompt);
		textField = new JTextField(numColumns);
		add(textField);
	}

	public LabeledTextField(String label, int labelWidth, int numColumns) {
		this(label, numColumns);
		Dimension size = prompt.getPreferredSize();
		size.width = labelWidth;
		prompt.setPreferredSize(size);
	}

	public void setText(String text) {
		textField.setText(text);
	}

	public String getText() {
		return textField.getText();
	}

	public JTextField getTextField() {
		return textField;
	}

	public JLabel getPrompt() {
		return prompt;
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		textField.setEnabled(enabled);
		prompt.setEnabled(enabled);
	}
}
