package edu.cnu.mdi.component.checkboxarray;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ItemListener;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;

import edu.cnu.mdi.graphics.GraphicsUtils;
import edu.cnu.mdi.ui.fonts.Fonts;

/** Data and convenience access for one button in a {@link CheckBoxArray}. */
public class CheckBoxData {

	private final AbstractButton checkBox;

	public CheckBoxData(String label, boolean initialState, boolean enabled, boolean radioStyle,
			ItemListener itemListener, Color textColor) {
		this(label, initialState, enabled, radioStyle, null, itemListener, textColor);
	}

	public CheckBoxData(String label, boolean initialState, boolean enabled, boolean radioStyle, Font font,
			ItemListener itemListener, Color textColor) {
		checkBox = radioStyle ? new JRadioButton(label) : new JCheckBox(label);
		GraphicsUtils.setSizeSmall(checkBox);
		checkBox.setFont(font != null ? font : Fonts.smallFont);
		if (textColor != null) {
			checkBox.setForeground(textColor);
		}
		checkBox.setSelected(initialState);
		checkBox.setEnabled(enabled);
		if (itemListener != null) {
			checkBox.addItemListener(itemListener);
		}
	}

	public String getText() {
		return checkBox.getText();
	}

	public boolean isSelected() {
		return checkBox.isSelected();
	}

	public void setSelected(boolean selected) {
		checkBox.setSelected(selected);
	}

	public void setEnabled(boolean enabled) {
		checkBox.setEnabled(enabled);
	}

	public AbstractButton getCheckBox() {
		return checkBox;
	}
}
