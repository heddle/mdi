package edu.cnu.mdi.component.checkboxarray;

import java.awt.Color;
import java.awt.event.ItemListener;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;

/** Multi-column panel of check boxes and optional radio-button groups. */
public class CheckBoxArray extends JPanel {

	protected Hashtable<String, ButtonGroup> buttonGroups;
	protected final Hashtable<String, AbstractButton> buttons = new Hashtable<>(59);

	private int nextIndex;
	private final JPanel[] subPanels;
	private final int verticalGap;

	public CheckBoxArray(int numColumns, int hgap, int vgap) {
		if (numColumns < 1) {
			throw new IllegalArgumentException("numColumns must be positive");
		}
		Box box = Box.createHorizontalBox();
		box.setOpaque(true);
		verticalGap = Math.max(0, vgap);
		subPanels = new JPanel[numColumns];
		for (int i = 0; i < numColumns; i++) {
			JPanel column = new JPanel();
			column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
			subPanels[i] = column;
			box.add(column);
			if (i < numColumns - 1) {
				box.add(Box.createHorizontalStrut(hgap));
			}
		}
		add(box);
	}

	public CheckBoxArray(int numColumns, int hgap, int vgap, String... labels) {
		this(numColumns, hgap, vgap);
		for (String label : labels) {
			add(label, false, true, null, Color.black);
		}
	}

	public CheckBoxData add(String label, boolean initialState, boolean enabled, ItemListener itemListener,
			Color textColor) {
		return add(label, initialState, enabled, null, itemListener, textColor);
	}

	public CheckBoxData add(String label, boolean initialState, boolean enabled, long bits, long mask,
			ItemListener itemListener, Color textColor) {
		return (bits & mask) == mask
				? add(label, initialState, enabled, null, itemListener, textColor)
				: null;
	}

	public CheckBoxData add(String label, boolean initialState, boolean enabled, String buttonGroupName,
			ItemListener itemListener, Color textColor) {
		CheckBoxData data = new CheckBoxData(label, initialState, enabled, buttonGroupName != null,
				itemListener, textColor);
		AbstractButton button = data.getCheckBox();
		buttons.put(label, button);
		JPanel column = subPanels[nextIndex];
		if (column.getComponentCount() > 0 && verticalGap > 0) {
			column.add(Box.createVerticalStrut(verticalGap));
		}
		column.add(button);
		nextIndex = (nextIndex + 1) % subPanels.length;
		if (buttonGroupName != null) {
			ButtonGroup group = getOrCreate(buttonGroupName);
			group.add(button);
			if (initialState) {
				button.setSelected(true);
			}
		}
		return data;
	}

	public AbstractButton getActiveButton(String buttonGroupName) {
		ButtonGroup group = buttonGroups == null ? null : buttonGroups.get(buttonGroupName);
		if (group == null) {
			return null;
		}
		for (Enumeration<AbstractButton> buttons = group.getElements(); buttons.hasMoreElements();) {
			AbstractButton button = buttons.nextElement();
			if (button.isSelected()) {
				return button;
			}
		}
		return null;
	}

	private ButtonGroup getOrCreate(String name) {
		if (buttonGroups == null) {
			buttonGroups = new Hashtable<>(47);
		}
		return buttonGroups.computeIfAbsent(name, ignored -> new ButtonGroup());
	}

	public void setSelected(String label, boolean selected) {
		AbstractButton button = buttons.get(label);
		if (button != null) {
			button.setSelected(selected);
		}
	}

	public boolean isSelected(String label) {
		AbstractButton button = buttons.get(label);
		return button != null && button.isSelected();
	}

	public boolean isEnabled(String label) {
		AbstractButton button = buttons.get(label);
		return button != null && button.isEnabled();
	}

	public void setEnabled(String label, boolean enabled) {
		AbstractButton button = buttons.get(label);
		if (button != null) {
			button.setEnabled(enabled);
		}
	}

	public AbstractButton getButton(String label) {
		return buttons.get(label);
	}
}
