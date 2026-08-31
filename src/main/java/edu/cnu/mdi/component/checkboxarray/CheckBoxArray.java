package edu.cnu.mdi.component.checkboxarray;

import java.awt.Color;
import java.awt.Component;
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

	/**
	 * Create an empty check box array with the given column layout.
	 *
	 * @param numColumns number of columns; must be positive
	 * @param hgap       horizontal gap in pixels between columns
	 * @param vgap       vertical gap in pixels between check boxes within a
	 *                   column; negative values are clamped to zero
	 * @throws IllegalArgumentException if {@code numColumns} is not positive
	 */
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
			column.setAlignmentY(Component.TOP_ALIGNMENT);
			subPanels[i] = column;
			box.add(column);
			if (i < numColumns - 1) {
				box.add(Box.createHorizontalStrut(hgap));
			}
		}
		add(box);
	}

	/**
	 * Create a check box array pre-populated with one unchecked, enabled,
	 * ungrouped check box per label, in black text with no item listener.
	 *
	 * @param numColumns number of columns; must be positive
	 * @param hgap       horizontal gap in pixels between columns
	 * @param vgap       vertical gap in pixels between check boxes within a
	 *                   column; negative values are clamped to zero
	 * @param labels     labels of the check boxes to add, one per box
	 * @throws IllegalArgumentException if {@code numColumns} is not positive
	 */
	public CheckBoxArray(int numColumns, int hgap, int vgap, String... labels) {
		this(numColumns, hgap, vgap);
		for (String label : labels) {
			add(label, false, true, null, Color.black);
		}
	}

	/**
	 * Add an ungrouped check box, placed in the next column in round-robin
	 * order (see {@link #add(String, boolean, boolean, String, ItemListener,
	 * Color)}).
	 *
	 * @param label         the check box's label and lookup key
	 * @param initialState  initial checked state
	 * @param enabled       initial enabled state
	 * @param itemListener  listener notified of check/uncheck events, or
	 *                      {@code null} for none
	 * @param textColor     label text color
	 * @return the new check box's data holder
	 */
	public CheckBoxData add(String label, boolean initialState, boolean enabled, ItemListener itemListener,
			Color textColor) {
		return add(label, initialState, enabled, null, itemListener, textColor);
	}

	/**
	 * Conditionally add an ungrouped check box, gated by a bitmask test.
	 * <p>
	 * The check box is added only when {@code (bits & mask) == mask} (i.e.
	 * every bit in {@code mask} is set in {@code bits|}); otherwise nothing is
	 * added and this method returns {@code null} without error.
	 * </p>
	 *
	 * @param label         the check box's label and lookup key
	 * @param initialState  initial checked state
	 * @param enabled       initial enabled state
	 * @param bits          the feature-flag bits to test
	 * @param mask          the bits that must all be set in {@code bits} for
	 *                      the check box to be added
	 * @param itemListener  listener notified of check/uncheck events, or
	 *                      {@code null} for none
	 * @param textColor     label text color
	 * @return the new check box's data holder, or {@code null} if
	 *         {@code (bits & mask) != mask}
	 */
	public CheckBoxData add(String label, boolean initialState, boolean enabled, long bits, long mask,
			ItemListener itemListener, Color textColor) {
		return (bits & mask) == mask
				? add(label, initialState, enabled, null, itemListener, textColor)
				: null;
	}

	/**
	 * Add a check box, optionally as part of a mutually-exclusive
	 * (radio-button-like) group.
	 * <p>
	 * Check boxes are placed into columns in round-robin order: each call
	 * places its box in the next column after the previous call's, wrapping
	 * back to the first column after the last.
	 * </p>
	 *
	 * @param label           the check box's label and lookup key (used by
	 *                        {@link #getButton}, {@link #isSelected}, etc.)
	 * @param initialState    initial checked state
	 * @param enabled         initial enabled state
	 * @param buttonGroupName name of the mutually-exclusive group to join, or
	 *                        {@code null} for an ungrouped (independent)
	 *                        check box
	 * @param itemListener    listener notified of check/uncheck events, or
	 *                        {@code null} for none
	 * @param textColor       label text color
	 * @return the new check box's data holder
	 */
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

	/**
	 * @param buttonGroupName name of a group previously used with
	 *                        {@link #add(String, boolean, boolean, String,
	 *                        ItemListener, Color)}
	 * @return the currently-selected button in that group, or {@code null} if
	 *         the group doesn't exist or has no selection
	 */
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

	/**
	 * Set a check box's checked state; a no-op if no box has this label.
	 *
	 * @param label    the check box's label
	 * @param selected the new checked state
	 */
	public void setSelected(String label, boolean selected) {
		AbstractButton button = buttons.get(label);
		if (button != null) {
			button.setSelected(selected);
		}
	}

	/**
	 * @param label the check box's label
	 * @return {@code true} if a box with this label exists and is checked
	 */
	public boolean isSelected(String label) {
		AbstractButton button = buttons.get(label);
		return button != null && button.isSelected();
	}

	/**
	 * @param label the check box's label
	 * @return {@code true} if a box with this label exists and is enabled
	 */
	public boolean isEnabled(String label) {
		AbstractButton button = buttons.get(label);
		return button != null && button.isEnabled();
	}

	/**
	 * Set a check box's enabled state; a no-op if no box has this label.
	 *
	 * @param label   the check box's label
	 * @param enabled the new enabled state
	 */
	public void setEnabled(String label, boolean enabled) {
		AbstractButton button = buttons.get(label);
		if (button != null) {
			button.setEnabled(enabled);
		}
	}

	/**
	 * @param label the check box's label
	 * @return the button with this label, or {@code null} if none exists
	 */
	public AbstractButton getButton(String label) {
		return buttons.get(label);
	}
}
