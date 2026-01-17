package edu.cnu.mdi.graphics.toolbar;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

/**
 * A convenience {@link JToolBar} for toolbars that contain a mutually exclusive
 * set of {@link JToggleButton}s (i.e., a radio-button-like group), plus optional
 * one-shot buttons.
 * <p>
 * This class manages:
 * </p>
 * <ul>
 * <li>a primary {@link ButtonGroup} that enforces mutual exclusivity</li>
 * <li>an optional default toggle button (selected when the active toggle is
 * turned off)</li>
 * <li>a selection-change hook for subclasses</li>
 * <li>a registry of buttons by stable id for programmatic enable/disable</li>
 * </ul>
 *
 * <h2>Button registry</h2>
 * <p>
 * Toolbars often need to enable/disable specific tools depending on application
 * state (e.g., disable <em>Delete</em> when nothing is selected). Rather than
 * forcing applications to keep references to every button instance, this class
 * supports registering buttons under a stable string id and retrieving them
 * later:
 * </p>
 *
 * <pre>{@code
 * toolBar.setButtonEnabled("delete", hasSelection);
 * toolBar.getButton("pointer", JToggleButton.class).setSelected(true);
 * }</pre>
 *
 * <p>
 * For predefined buttons, you typically use {@link ToolBits#getId(long)} as
 * the stable id. For application-defined buttons, choose your own ids.
 * </p>
 *
 * <h2>Notes</h2>
 * <ul>
 * <li>You may still add non-toggle components (regular buttons, separators,
 * etc.) using {@link #add(Component)} from {@link JToolBar}.</li>
 * <li>Only toggles added via {@link #addToggle(JToggleButton)} or
 * {@link #addToggle(JToggleButton, boolean)} are managed by the primary
 * group.</li>
 * <li>The registry is populated by using the id-aware overloads
 * {@link #addToggle(String, JToggleButton)} / {@link #addButton(String, JButton)}.
 * If you call {@link #add(Component)} directly, you may register manually via
 * {@link #registerButton(String, AbstractButton)}.</li>
 * </ul>
 *
 * @author heddle
 */
@SuppressWarnings("serial")
public abstract class AToolBar extends JToolBar {

	/** Client property key used to store an id on a button. */
	public static final String CLIENTPROP_TOOL_ID = "toolId";

	/** Primary mutually-exclusive group for tool toggle buttons. */
	protected final ButtonGroup toggleGroup = new ButtonGroup();

	/** Optional default toggle button to activate when "no selection" occurs. */
	private JToggleButton defaultToggleButton;

	/** Status field to display messages (may be null). */
	protected JTextField statusField;

	/**
	 * Registry of toolbar buttons by stable id.
	 * <p>
	 * This includes both predefined buttons and application-added buttons.
	 * </p>
	 */
	private final Map<String, AbstractButton> buttonsById = new LinkedHashMap<>();

	/** Listener that watches selection changes on group-managed toggles. */
	private final ItemListener toggleSelectionListener = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (!(e.getItemSelectable() instanceof JToggleButton)) {
				return;
			}

			JToggleButton b = (JToggleButton) e.getItemSelectable();

			// Only respond to selection events; deselection is handled by the newly-selected
			// button's selection event OR by resetDefaultToggleButton().
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
	 * Create a toolbar with the given orientation.
	 *
	 * @param orientation {@link SwingConstants#HORIZONTAL} or
	 *                    {@link SwingConstants#VERTICAL}.
	 */
	public AToolBar(int orientation) {
		super(orientation);
		setFloatable(false);
		putClientProperty("JToolBar.isRollover", true);
	}

	// ------------------------------------------------------------------------
	// Registry API
	// ------------------------------------------------------------------------

	/**
	 * Register a toolbar button under a stable id.
	 * <p>
	 * This is used by the id-aware add methods. It is also safe for subclasses to
	 * call directly if they add components via {@link #add(Component)}.
	 * </p>
	 * <p>
	 * For debugging and tooling, this method also sets:
	 * </p>
	 * <ul>
	 * <li>{@link AbstractButton#setName(String)} to the id</li>
	 * <li>{@code button.putClientProperty("toolId", id)}</li>
	 * </ul>
	 *
	 * @param id     stable identifier (non-blank).
	 * @param button the button to register.
	 * @return the same button (for chaining).
	 * @throws NullPointerException     if id or button is null.
	 * @throws IllegalArgumentException if id is blank or already used by a
	 *                                  different button.
	 */
	protected <T extends AbstractButton> T registerButton(String id, T button) {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(button, "button");

		final String key = id.trim();
		if (key.isEmpty()) {
			throw new IllegalArgumentException("Button id cannot be blank.");
		}

		final AbstractButton existing = buttonsById.get(key);
		if (existing != null && existing != button) {
			throw new IllegalArgumentException("Duplicate button id: '" + key + "'");
		}

		buttonsById.put(key, button);

		// Helpful metadata for debugging / UI inspection:
		button.setName(key);
		button.putClientProperty(CLIENTPROP_TOOL_ID, key);
		return button;
	}

	/**
	 * Remove a registered button from the registry by id.
	 * <p>
	 * This does not remove the button from the Swing component hierarchy. If you
	 * dynamically remove tools, call this in addition to {@link #remove(Component)}.
	 * </p>
	 *
	 * @param id the id to remove (null/blank are ignored).
	 * @return the removed button, or {@code null} if none was registered.
	 */
	public AbstractButton removeButton(String id) {
		if (id == null) {
			return null;
		}
		String key = id.trim();
		if (key.isEmpty()) {
			return null;
		}
		return buttonsById.remove(key);
	}

	/**
	 * Look up a registered toolbar button by id.
	 *
	 * @param id the id used when adding/registering the button.
	 * @return the button, or {@code null} if not found.
	 */
	public AbstractButton getButton(String id) {
		if (id == null) {
			return null;
		}
		String key = id.trim();
		if (key.isEmpty()) {
			return null;
		}
		return buttonsById.get(key);
	}

	/**
	 * Look up a registered toolbar button by id and expected type.
	 *
	 * @param id   the id used when adding/registering the button.
	 * @param type expected type.
	 * @param <T>  type parameter.
	 * @return the typed button, or {@code null} if not found or wrong type.
	 */
	public <T extends AbstractButton> T getButton(String id, Class<T> type) {
		Objects.requireNonNull(type, "type");
		final AbstractButton b = getButton(id);
		return (b != null && type.isInstance(b)) ? type.cast(b) : null;
	}

	/**
	 * Look up a registered toolbar button by id, throwing if missing.
	 * <p>
	 * This is handy in application wiring code where missing tools should be a
	 * programming error rather than silently ignored.
	 * </p>
	 *
	 * @param id   registered id (non-blank).
	 * @param type expected type.
	 * @param <T>  type parameter.
	 * @return the typed button (never null).
	 * @throws IllegalStateException if no button exists under that id, or the type is wrong.
	 */
	public <T extends AbstractButton> T requireButton(String id, Class<T> type) {
		Objects.requireNonNull(type, "type");
		AbstractButton b = getButton(id);
		if (b == null) {
			throw new IllegalStateException("No toolbar button registered with id '" + id + "'");
		}
		if (!type.isInstance(b)) {
			throw new IllegalStateException("Toolbar button '" + id + "' is " + b.getClass().getName()
					+ " but expected " + type.getName());
		}
		return type.cast(b);
	}

	/**
	 * Enable or disable a registered button by id.
	 *
	 * @param id      button id.
	 * @param enabled new enabled state.
	 * @return {@code true} if the button was found and updated, {@code false} if
	 *         no button is registered under that id.
	 */
	public boolean setButtonEnabled(String id, boolean enabled) {
		final AbstractButton b = getButton(id);
		if (b == null) {
			return false;
		}
		b.setEnabled(enabled);
		return true;
	}

	/**
	 * Try to read the registered tool id from a button.
	 *
	 * @param button a button that may have been registered.
	 * @return the id string, or {@code null} if not present.
	 */
	public static String getToolId(AbstractButton button) {
		if (button == null) {
			return null;
		}
		Object v = button.getClientProperty(CLIENTPROP_TOOL_ID);
		return (v instanceof String s && !s.isBlank()) ? s : null;
	}

	/**
	 * @return an unmodifiable view of the current button registry.
	 */
	public Map<String, AbstractButton> getButtonRegistryView() {
		return Collections.unmodifiableMap(buttonsById);
	}

	// ------------------------------------------------------------------------
	// Add helpers (id-aware)
	// ------------------------------------------------------------------------

	/**
	 * Add a button to the toolbar and register it under the given id.
	 *
	 * @param id     stable identifier for later retrieval (non-blank).
	 * @param button the button to add.
	 * @return the added button (for chaining).
	 * @throws NullPointerException     if id or button is null.
	 * @throws IllegalArgumentException if id is blank or already used.
	 */
	public JButton addButton(String id, JButton button) {
		Objects.requireNonNull(button, "button");
		registerButton(id, button);
		super.add(button);
		return button;
	}

	/**
	 * Add a toggle to the toolbar and register it under the given id.
	 *
	 * @param id           stable identifier for later retrieval (non-blank).
	 * @param toggleButton the toggle to add.
	 * @return the added toggle (for chaining).
	 */
	public JToggleButton addToggle(String id, JToggleButton toggleButton) {
		return addToggle(id, toggleButton, true);
	}

	/**
	 * Add a toggle button to the toolbar, optionally adding it to the primary
	 * mutually-exclusive group.
	 *
	 * @param toggleButton the toggle to add.
	 * @param toGroup      if true, add to the primary mutually-exclusive group.
	 * @return the added toggle.
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
	 * Add a toggle button to the toolbar, optionally adding it to the primary
	 * mutually-exclusive group, and register it under the given id.
	 *
	 * @param id           stable identifier for later retrieval (non-blank).
	 * @param toggleButton the toggle to add.
	 * @param toGroup      if true, add to the primary mutually-exclusive group.
	 * @return the added toggle (for chaining).
	 * @throws NullPointerException     if id or toggleButton is null.
	 * @throws IllegalArgumentException if id is blank or already used.
	 */
	public JToggleButton addToggle(String id, JToggleButton toggleButton, boolean toGroup) {
		Objects.requireNonNull(toggleButton, "toggleButton");
		registerButton(id, toggleButton);
		return addToggle(toggleButton, toGroup);
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
	 * @throws IllegalArgumentException if the button is not null but not contained
	 *                                  in this toolbar (it should be added first).
	 */
	public void setDefaultToggleButton(JToggleButton defaultToggleButton) {
		if (defaultToggleButton != null) {
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
		resetDefaultToggleButton();
	}

	/**
	 * Programmatically select the default toggle button.
	 * <p>
	 * If another toggle is currently active, it will be deselected by the
	 * {@link ButtonGroup}. If no default is set, this method does nothing.
	 * </p>
	 */
	public void resetDefaultToggleButton() {
		if (defaultToggleButton == null) {
			return;
		}

		defaultToggleButton.setSelected(true);

		// Ensure subclass hook runs even if selection state is unchanged.
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

	/**
	 * Set the status text shown on the toolbar.
	 *
	 * @param text the status text to set.
	 */
	public void updateStatusText(String text) {
		if (statusField != null) {
			statusField.setText(text);
		}
	}
	
	public void spacer(int pixels) {
		if (getOrientation() == SwingConstants.HORIZONTAL) {
			add(Box.createHorizontalStrut(pixels));
		} else {
			add(Box.createVerticalStrut(pixels));
		}
	}	
}
