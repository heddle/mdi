package edu.cnu.mdi.experimental;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Enumeration;
import java.util.Objects;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

/**
 * A convenience {@link JToolBar} for toolbars that contain a mutually exclusive
 * set of {@link JToggleButton}s (i.e., a radio-button-like group).
 * <p>
 * This class manages:
 * </p>
 * <ul>
 * <li>a primary {@link ButtonGroup} that enforces mutual exclusivity</li>
 * <li>an optional default toggle button (selected when the active toggle is
 * turned off)</li>
 * <li>a selection-change hook for subclasses</li>
 * </ul>
 *
 * <h2>Typical usage</h2>
 * 
 * <pre>{@code
 * BaseToolBar tb = new BaseToolBar();
 * tb.addToggle(pointerButton, true);
 * tb.addToggle(panButton, true);
 * tb.setDefaultToggleButton(pointerButton);
 * tb.resetDefaultSelection(); // activates default
 * }</pre>
 *
 * <h2>Notes</h2>
 * <ul>
 * <li>You may still add non-toggle components (regular buttons, separators,
 * etc.) using {@link #add(Component)} from {@link JToolBar}.</li>
 * <li>Only toggles added via {@link #addToggle(JToggleButton)} or
 * {@link #addToggle(JToggleButton, boolean)} are managed by the primary
 * group.</li>
 * </ul>
 *
 * @author heddle
 */
@SuppressWarnings("serial")
public abstract class AToolBar extends JToolBar {

	/** Primary mutually-exclusive group for tool toggle buttons. */
	private final ButtonGroup toggleGroup = new ButtonGroup();

	/** Optional default toggle button to activate when "no selection" occurs. */
	private JToggleButton defaultToggleButton;

	/** Listener that watches selection changes on group-managed toggles. */
	private final ItemListener toggleSelectionListener = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (!(e.getItemSelectable() instanceof JToggleButton)) {
				return;
			}

			JToggleButton b = (JToggleButton) e.getItemSelectable();

			// Only respond to selection events; deselection is handled by the
			// newly-selected button's selection event OR by resetDefaultSelection().
			if (e.getStateChange() == ItemEvent.SELECTED) {
				activeToggleButtonChanged(b);
			}
		}
	};

	/**
	 * Create a toolbar with horizontal orientation.
	 */
	public AToolBar() {
		this(SwingConstants.HORIZONTAL);
	}

	/**
	 * Create a toolbar with the given name and orientation.
	 *
	 * @param name        the toolbar name (used when undocked).
	 * @param orientation {@link SwingConstants#HORIZONTAL} or
	 *                    {@link SwingConstants#VERTICAL}.
	 */
	public AToolBar(int orientation) {
		super(orientation);
		setFloatable(false);
		putClientProperty("JToolBar.isRollover", true);

	}

	/**
	 * Add a toggle button to the toolbar and to the primary mutually-exclusive
	 * group.
	 * <p>
	 * This is the common case for tool-selection toggles.
	 * </p>
	 *
	 * @param toggleButton the toggle to add (ignored if null).
	 * @return the added toggle (for chaining), or null if the argument was null.
	 */
	public JToggleButton addToggle(JToggleButton toggleButton) {
		Objects.requireNonNull(toggleButton, "Toggle button cannot be null.");
		return addToggle(toggleButton, true);
	}

	/**
	 * Add a toggle button to the toolbar, optionally adding it to the primary
	 * mutually-exclusive group.
	 * <p>
	 * If {@code toGroup} is true, the toggle is:
	 * </p>
	 * <ul>
	 * <li>added to the {@link ButtonGroup} so it becomes mutually exclusive</li>
	 * <li>registered with a listener so selection changes trigger
	 * {@link #activeToggleButtonChanged(JToggleButton)}</li>
	 * </ul>
	 *
	 * @param toggleButton the toggle to add (ignored if null).
	 * @param toGroup      if true, add to the primary mutually-exclusive group.
	 * @return the added toggle (for chaining), or null if the argument was null.
	 */
	public JToggleButton addToggle(JToggleButton toggleButton, boolean toGroup) {
		Objects.requireNonNull(toggleButton, "Toggle button cannot be null.");

		super.add(toggleButton);

		if (toGroup) {
			toggleGroup.add(toggleButton);

			// Avoid double-registration if callers add/remove/re-add.
			toggleButton.removeItemListener(toggleSelectionListener);
			toggleButton.addItemListener(toggleSelectionListener);
		}

		return toggleButton;
	}

	/**
	 * Remove a toggle button from the toolbar and from the primary group.
	 *
	 * @param toggleButton the toggle to remove (ignored if null).
	 */
	public void removeToggle(JToggleButton toggleButton) {
		if (toggleButton == null) {
			return;
		}
		super.remove(toggleButton);

		toggleGroup.remove(toggleButton);
		toggleButton.removeItemListener(toggleSelectionListener);

		// If we removed the default toggle, clear default to avoid surprises.
		if (toggleButton == defaultToggleButton) {
			defaultToggleButton = null;
		}
	}

	/**
	 * Called when the active toggle button in the primary group changes.
	 * <p>
	 * Subclasses override this to react to a tool selection change.
	 * </p>
	 *
	 * @param newlyActive the button that just became selected (never null).
	 */
	protected abstract void activeToggleButtonChanged(JToggleButton newlyActive);

	/**
	 * Get the default toggle button (if any).
	 * <p>
	 * The default is typically the pointer tool. It may be reselected if the user
	 * "turns off" the active toggle or when {@link #resetDefaultSelection()} is
	 * called.
	 * </p>
	 *
	 * @return the default toggle button, or null.
	 */
	public JToggleButton getDefaultToggleButton() {
		return defaultToggleButton;
	}

	/**
	 * Set the default toggle button.
	 *
	 * @param defaultToggleButton the default toggle (may be null).
	 * @throws IllegalArgumentException if the button is not null but not
	 * contained in this toolbar. It should be added to the toolbar first.
	 */
	public void setDefaultToggleButton(JToggleButton defaultToggleButton) {
		if (defaultToggleButton != null) {
			// Defensive: ensure it is actually part of this toolbar.
			boolean found = false;
			for (Component c : getComponents()) {
				if (c == defaultToggleButton) {
					found = true;
					break;
				}
			}
			if (!found) {
				throw new IllegalArgumentException("Default toggle button must be added to the toolbar first.");
			}
		}
		this.defaultToggleButton = defaultToggleButton;
	}

	/**
	 * Programmatically select the default toggle button.
	 * <p>
	 * If another toggle is currently active, it will be deselected by the
	 * {@link ButtonGroup}. If no default is set, this method does nothing.
	 * </p>
	 */
	public void resetDefaultToggleButton
	() {
		if (defaultToggleButton == null) {
			return;
		}

		// Selecting directly avoids extra ActionEvent semantics; the ButtonGroup
		// will enforce exclusivity.
		defaultToggleButton.setSelected(true);

		// Ensure subclass hook runs even if selection state is unchanged.
		// (If already selected, ItemListener may not fire.)
		activeToggleButtonChanged(defaultToggleButton);
	}

	/**
	 * Return the currently selected toggle button from the primary group.
	 *
	 * @return the selected toggle, or null if none is selected.
	 */
	public JToggleButton getActiveButton() {
		for (Enumeration<AbstractButton> e = toggleGroup.getElements(); e.hasMoreElements();) {
			AbstractButton ab = e.nextElement();
			if (ab instanceof JToggleButton && ab.isSelected()) {
				return (JToggleButton) ab;
			}
		}
		return null;
	}

}