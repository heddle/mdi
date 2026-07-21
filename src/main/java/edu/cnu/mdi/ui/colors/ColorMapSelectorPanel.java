package edu.cnu.mdi.ui.colors;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * A panel that combines a {@link JComboBox} for selecting a
 * {@link ScientificColorMap} with a {@link ColorScaleBar} that previews the
 * currently selected map.
 *
 * <h2>Layout</h2>
 * <p>
 * Components are arranged left-to-right in a {@link FlowLayout}: the combo box
 * appears first, followed immediately by the scale bar preview. The scale bar
 * defaults to 100&times;20 pixels but can be resized via
 * {@link #setScaleBarSize(Dimension)}.
 * </p>
 *
 * <h2>Selection</h2>
 * <p>
 * The selected map can be read at any time with {@link #getSelectedColorMap()},
 * and can be changed programmatically with {@link #setSelectedColorMap}. The
 * combo box and scale bar are always kept in sync.
 * </p>
 *
 * <h2>Change notification</h2>
 * <p>
 * Callers that need to react to selection changes should register a
 * {@link ColorMapChangeListener} via {@link #addColorMapChangeListener}. The
 * listener is fired on the Swing EDT whenever the selected map changes, whether
 * the change originates from user interaction or from a programmatic call to
 * {@link #setSelectedColorMap}.
 * </p>
 *
 * <h2>Thread safety</h2>
 * <p>
 * {@link #setSelectedColorMap} is safe to call from any thread; it marshals to
 * the EDT automatically. All other methods must be called on the EDT.
 * </p>
 *
 * @see ScientificColorMap
 * @see ColorScaleBar
 */
@SuppressWarnings("serial")
public class ColorMapSelectorPanel extends JPanel {

    // ----------------------------------------------------------------
    // Listener interface
    // ----------------------------------------------------------------

    /**
     * Callback interface notified when the selected {@link ScientificColorMap}
     * changes in a {@link ColorMapSelectorPanel}.
     */
    public interface ColorMapChangeListener {

        /**
         * Invoked on the Swing EDT whenever the selected color map changes.
         *
         * @param source the panel where the change occurred; never {@code null}
         * @param map    the newly selected color map; never {@code null}
         */
        void colorMapChanged(ColorMapSelectorPanel source, ScientificColorMap map);
    }

    // ----------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------

    /** Combo box listing all available {@link ScientificColorMap} values. */
    private final JComboBox<ScientificColorMap> _combo;

    /** Live preview bar that reflects the currently selected map. */
    private final ColorScaleBar _scaleBar;

    /** Registered change listeners; iterated on the EDT only. */
    private final List<ColorMapChangeListener> _listeners = new ArrayList<>();

    // ----------------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------------

    /**
     * Creates a {@code ColorMapSelectorPanel} with the given initial selection.
     *
     * @param initialMap the map to select on construction; must not be {@code null}
     * @throws IllegalArgumentException if {@code initialMap} is {@code null}
     */
    public ColorMapSelectorPanel(ScientificColorMap initialMap) {
    	this(null, initialMap);
    }
    
    
    /**
     * Creates a {@code ColorMapSelectorPanel} with the given initial selection.
     *
     * @param listener   an optional listener to register for selection changes; may be {@code null}
     * @param initialMap the map to select on construction; must not be {@code null}
     * @throws IllegalArgumentException if {@code initialMap} is {@code null}
     */
    public ColorMapSelectorPanel(ColorMapChangeListener listener, ScientificColorMap initialMap) {
        super(new FlowLayout(FlowLayout.LEFT, 4, 0));

        if (initialMap == null) {
            throw new IllegalArgumentException("initialMap must not be null");
        }

        _combo = new JComboBox<>(ScientificColorMap.values());
        _combo.setSelectedItem(initialMap);
        add(_combo);

        _scaleBar = new ColorScaleBar(initialMap);
        _scaleBar.setPreferredSize(new Dimension(100, 20));
        add(_scaleBar);

        // ActionListener is always fired on the EDT, so no marshalling needed here.
        _combo.addActionListener(e -> {
            ScientificColorMap map = (ScientificColorMap) _combo.getSelectedItem();
            if (map != null) {
                _scaleBar.setColorMap(map);
                fireColorMapChanged(map);
            }
        });
        
        if (listener != null) {
			addColorMapChangeListener(listener);
		}
    }


    // ----------------------------------------------------------------
    // Public API — selection
    // ----------------------------------------------------------------

    /**
     * Returns the currently selected color map.
     *
     * <p>This is always consistent with what the combo box displays and what the
     * scale bar previews.</p>
     *
     * @return the selected {@link ScientificColorMap}; never {@code null}
     */
    public ScientificColorMap getSelectedColorMap() {
        return (ScientificColorMap) _combo.getSelectedItem();
    }

    /**
     * Programmatically selects the given color map, updating both the combo box
     * and the scale bar preview, and notifying any registered
     * {@link ColorMapChangeListener}s.
     *
     * <p>This method is safe to call from any thread. If called off the EDT it
     * marshals the update via {@link SwingUtilities#invokeLater}.</p>
     *
     * <p>If {@code map} is already the selected item this method is a no-op.</p>
     *
     * @param map the map to select; must not be {@code null}
     * @throws IllegalArgumentException if {@code map} is {@code null}
     */
    public void setSelectedColorMap(ScientificColorMap map) {
        if (map == null) {
            throw new IllegalArgumentException("map must not be null");
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setSelectedColorMap(map));
            return;
        }
        if (map == getSelectedColorMap()) {
            return;
        }
        // Setting the selected item fires the ActionListener, which updates
        // the scale bar and notifies listeners — no duplication needed here.
        _combo.setSelectedItem(map);
    }

    // ----------------------------------------------------------------
    // Public API — scale bar
    // ----------------------------------------------------------------

    /**
     * Sets the preferred size of the color scale bar preview.
     *
     * <p>Call {@link #revalidate()} and {@link #repaint()} on the panel after
     * this method if it is already displayed.</p>
     *
     * @param size the new preferred size; must not be {@code null}
     */
    public void setScaleBarSize(Dimension size) {
        if (size == null) {
            throw new IllegalArgumentException("size must not be null");
        }
        _scaleBar.setPreferredSize(size);
    }

    // ----------------------------------------------------------------
    // Public API — listeners
    // ----------------------------------------------------------------

    /**
     * Registers a listener to be notified when the selected color map changes.
     *
     * <p>Adding the same listener instance more than once will result in it being
     * notified multiple times per event.</p>
     *
     * @param listener the listener to add; must not be {@code null}
     */
    public void addColorMapChangeListener(ColorMapChangeListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        _listeners.add(listener);
    }

    /**
     * Removes a previously registered listener.
     *
     * <p>If the listener was registered more than once, only the first occurrence
     * is removed. If it was never registered, this method does nothing.</p>
     *
     * @param listener the listener to remove; must not be {@code null}
     */
    public void removeColorMapChangeListener(ColorMapChangeListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        _listeners.remove(listener);
    }

    // ----------------------------------------------------------------
    // Overrides
    // ----------------------------------------------------------------

    /**
     * Enables or disables the panel and its child components (the combo box and
     * scale bar). When disabled, the user cannot interact with either.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        _combo.setEnabled(enabled);
        _scaleBar.setEnabled(enabled);
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    /**
     * Notifies all registered {@link ColorMapChangeListener}s of a selection
     * change. Must be called on the EDT.
     *
     * @param map the newly selected map; never {@code null}
     */
    private void fireColorMapChanged(ScientificColorMap map) {
        for (ColorMapChangeListener listener : new ArrayList<>(_listeners)) {
            listener.colorMapChanged(this, map);
        }
    }
}