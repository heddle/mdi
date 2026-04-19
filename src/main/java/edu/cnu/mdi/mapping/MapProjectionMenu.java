package edu.cnu.mdi.mapping;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;

/**
 * A {@link JMenu} that presents all available {@link EProjection} values as
 * mutually exclusive radio-button items.
 *
 * <h2>Default selection</h2>
 * <p>The initially selected projection is {@link MapConstants#DEFAULT_PROJECTION}.
 * Previously this class hardcoded {@link EProjection#MOLLWEIDE} as the
 * default, which disagreed with {@link MapView2D}'s actual default of
 * {@link EProjection#MERCATOR}. Both now share the constant so they can never
 * diverge silently.</p>
 *
 * <h2>Callback</h2>
 * <p>When the user selects a new projection an {@link ActionEvent} is fired on
 * the supplied {@link ActionListener} with the command string set to the
 * enum's {@link Enum#name()} (e.g. {@code "MERCATOR"}).</p>
 */
@SuppressWarnings("serial")
public class MapProjectionMenu extends JMenu {

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /**
     * The currently selected projection, kept in sync with the radio-button
     * selection. Initialized to {@link MapConstants#DEFAULT_PROJECTION} so it
     * matches the initial menu selection.
     */
    private EProjection currentProjection = MapConstants.DEFAULT_PROJECTION;

    /** Callback invoked whenever the user picks a different projection. */
    private final ActionListener projectionChangeCallback;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates the "Map Projection" menu populated with one radio-button item
     * per {@link EProjection} value.
     *
     * @param callback listener notified when the projection changes;
     *                 must not be {@code null}
     */
    public MapProjectionMenu(ActionListener callback) {
        super("Map Projection");
        this.projectionChangeCallback = callback;
        createMenuItems();
    }

    // -------------------------------------------------------------------------
    // Private setup
    // -------------------------------------------------------------------------

    /**
     * Populates the menu with a {@link JRadioButtonMenuItem} for each
     * {@link EProjection} value, grouped so exactly one is selected at a time.
     *
     * <p>The item corresponding to {@link MapConstants#DEFAULT_PROJECTION} is
     * pre-selected.</p>
     */
    private void createMenuItems() {
        ButtonGroup buttonGroup = new ButtonGroup();

        for (EProjection projection : EProjection.values()) {
            JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(projection.getName());

            // Pre-select the application-wide default projection.
            if (projection == MapConstants.DEFAULT_PROJECTION) {
                menuItem.setSelected(true);
            }

            menuItem.addActionListener(e -> {
                currentProjection = projection;
                projectionChangeCallback.actionPerformed(
                        new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                                        projection.name()));
            });

            buttonGroup.add(menuItem);
            add(menuItem);
        }
    }

    // -------------------------------------------------------------------------
    // Accessor
    // -------------------------------------------------------------------------

    /**
     * Returns the currently selected {@link EProjection}.
     *
     * @return the selected projection; never {@code null}
     */
    public EProjection getCurrentProjection() { return currentProjection; }
}
