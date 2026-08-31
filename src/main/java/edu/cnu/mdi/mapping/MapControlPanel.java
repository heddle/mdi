package edu.cnu.mdi.mapping;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.util.Objects;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import edu.cnu.mdi.component.CommonBorder;
import edu.cnu.mdi.mapping.projection.EProjection;
import edu.cnu.mdi.mapping.projection.IMapProjection;
import edu.cnu.mdi.mapping.projection.ProjectionFactory;
import edu.cnu.mdi.mapping.theme.MapTheme;
import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * Side-panel control widget for {@link MapView2D}.
 *
 * <p>Provides interactive controls for:
 * <ul>
 *   <li><b>Projection</b> — a factory-backed selector containing MDI's
 *       built-ins and projections registered through
 *       {@link #addProjection(String, Function)}.</li>
 *   <li><b>Map theme</b> — radio buttons selecting between the built-in
 *       {@link MapTheme} presets (Light, Dark, Blue).</li>
 *   <li><b>Application controls</b> — components registered through
 *       {@link #addControl(JComponent)} and displayed below the standard
 *       controls.</li>
 * </ul>
 *
 * <h2>Coupling</h2>
 * <p>This panel holds a direct reference to a {@link MapView2D} and calls
 * its public API in response to user interaction. The coupling is intentional
 * and kept minimal: the panel only calls well-defined accessors on the view
 * ({@link MapView2D#getMapProjection()},
 * {@link MapView2D#setProjection(IMapProjection)},
 * {@link MapView2D#refresh()}).</p>
 */
@SuppressWarnings("serial")
public class MapControlPanel extends JPanel {

    // -------------------------------------------------------------------------
    // Theme instances
    // -------------------------------------------------------------------------

    private final MapTheme darkTheme  = MapTheme.dark();
    private final MapTheme lightTheme = MapTheme.light();
    private final MapTheme blueTheme  = MapTheme.blue();

    private final Font font = Fonts.plainFontDelta(-2);

    /** The theme currently selected by the radio buttons. */
    private MapTheme currentTheme = lightTheme;

    // -------------------------------------------------------------------------
    // Widgets
    // -------------------------------------------------------------------------

    private JRadioButton lightThemeButton;
    private JRadioButton darkThemeButton;
    private JRadioButton blueThemeButton;

    /** Factory-backed projection selector, including application additions. */
    private JComboBox<ProjectionOption> projectionCombo;

    /** Dedicated vertical host for application-supplied controls. */
    private JPanel applicationControlHost;


    // -------------------------------------------------------------------------
    // View reference
    // -------------------------------------------------------------------------

    /**
     * The map view controlled by this panel.
     *
     * <p>This reference is used only to call well-defined public API methods;
     * the panel never accesses private or package-private state of the view
     * directly.</p>
     */
    private final MapView2D mapView;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a control panel bound to the given map view.
     *
     * @param mapView the view to control; must not be {@code null}
     */
    public MapControlPanel(MapView2D mapView) {
        this.mapView = mapView;
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        createProjectionCombo(this);
        createThemeSelector(this);
        createApplicationControlHost(this);
    }

    // -------------------------------------------------------------------------
    // Widget builders
    // -------------------------------------------------------------------------

    /**
     * Adds the projection selection combo box to {@code panel}.
     *
     * <p>Selecting a different projection calls
     * {@link MapView2D#setProjection(EProjection)}, which rebuilds the
     * projection, graticule, country, and city renderers and triggers a
     * repaint.</p>
     *
     * @param panel the panel to add the combo box to
     */
    private void createProjectionCombo(JPanel panel) {
        projectionCombo = new JComboBox<>();
        for (EProjection type : EProjection.values()) {
            addProjectionOption(type.getName(),
                    theme -> ProjectionFactory.create(type, theme, null));
        }
        projectionCombo.addActionListener(e -> {
            ProjectionOption selected =
                    (ProjectionOption) projectionCombo.getSelectedItem();
            if (selected != null) {
                IMapProjection projection = Objects.requireNonNull(
                        selected.factory().apply(currentTheme),
                        "Projection factory returned null for " + selected.name());
                mapView.setProjection(projection);
            }
        });
        projectionCombo.setFont(font);
        leftAlign(projectionCombo);

        panel.add(projectionCombo);
        panel.add(Box.createVerticalStrut(6));
    }

    /**
     * Adds an application-supplied projection to the standard selector.
     *
     * <p>The factory receives the currently selected theme whenever its entry
     * is chosen. Applications can therefore expose custom projections without
     * replacing the rest of the map control panel.</p>
     *
     * @param name display name; must not be blank or duplicate an existing name
     * @param factory factory receiving the active theme; must return a non-null
     *                projection
     * @throws IllegalArgumentException if the name is blank or already present
     */
    public void addProjection(String name,
            Function<MapTheme, ? extends IMapProjection> factory) {
        String normalizedName = Objects.requireNonNull(name, "name").trim();
        Objects.requireNonNull(factory, "factory");
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Projection name must not be blank");
        }
        for (int i = 0; i < projectionCombo.getItemCount(); i++) {
            if (projectionCombo.getItemAt(i).name().equals(normalizedName)) {
                throw new IllegalArgumentException(
                        "Projection already registered: " + normalizedName);
            }
        }
        addProjectionOption(normalizedName, factory);
    }

    private void addProjectionOption(String name,
            Function<MapTheme, ? extends IMapProjection> factory) {
        projectionCombo.addItem(new ProjectionOption(name, factory));
    }


    /**
     * Adds the theme selector (Light / Dark / Blue radio buttons) to
     * {@code panel}.
     *
     * <p>Selecting a radio button updates {@link #currentTheme} and calls
     * {@link IMapProjection#setTheme(MapTheme)} followed by a repaint.</p>
     *
     * @param panel the panel to add the theme selector to
     */
    private void createThemeSelector(JPanel panel) {
        ButtonGroup themeGroup = new ButtonGroup();

        ActionListener themeListener = e -> {
            if      (lightThemeButton.isSelected()) {
				currentTheme = lightTheme;
			} else if (darkThemeButton.isSelected()) {
				currentTheme = darkTheme;
			} else if (blueThemeButton.isSelected()) {
				currentTheme = blueTheme;
			}
            updateTheme();
        };

        lightThemeButton = createThemeButton("Light", themeGroup, themeListener, true);
        darkThemeButton  = createThemeButton("Dark",  themeGroup, themeListener, false);
        blueThemeButton  = createThemeButton("Blue",  themeGroup, themeListener, false);

        leftAlign(lightThemeButton);
        leftAlign(darkThemeButton);
        leftAlign(blueThemeButton);

        JPanel subPanel = new JPanel();
        subPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subPanel.add(lightThemeButton);
        subPanel.add(darkThemeButton);
        subPanel.add(blueThemeButton);
        subPanel.add(Box.createVerticalStrut(6));
        subPanel.setBorder(new CommonBorder("Map Theme"));
        panel.add(subPanel);
    }

    /** Creates the initially empty extension area below the standard controls. */
    private void createApplicationControlHost(JPanel panel) {
        applicationControlHost = new JPanel();
        applicationControlHost.setOpaque(false);
        applicationControlHost.setAlignmentX(Component.LEFT_ALIGNMENT);
        applicationControlHost.setLayout(
                new BoxLayout(applicationControlHost, BoxLayout.Y_AXIS));
        panel.add(applicationControlHost);
    }

    /**
     * Adds an application-supplied component below the standard projection and
     * theme controls.
     *
     * <p>Controls appear in registration order. This method supplies standard
     * vertical spacing, left alignment, layout validation, and repainting so
     * callers do not need to depend on this panel's Swing layout details. Adding
     * the same component more than once is an idempotent no-op.</p>
     *
     * <p>As with other Swing component mutations, callers should invoke this
     * method on the event-dispatch thread.</p>
     *
     * @param control component to add; must not be {@code null}
     * @throws NullPointerException if {@code control} is {@code null}
     */
    public void addControl(JComponent control) {
        Objects.requireNonNull(control, "control");
        if (control.getParent() == applicationControlHost) {
            return;
        }

        leftAlign(control);
        applicationControlHost.add(Box.createVerticalStrut(6));
        applicationControlHost.add(control);
        applicationControlHost.revalidate();
        applicationControlHost.repaint();
    }

    /**
     * Creates a themed radio button and adds it to the supplied group.
     *
     * @param label    button label text
     * @param bg       the button group this button belongs to
     * @param al       action listener notified on selection
     * @param selected {@code true} if the button should be pre-selected
     * @return the constructed {@link JRadioButton}
     */
    private JRadioButton createThemeButton(String label, ButtonGroup bg,
                                           ActionListener al, boolean selected) {
        JRadioButton button = new JRadioButton(label);
        button.setSelected(selected);
        button.setFont(font);
        bg.add(button);
        button.addActionListener(al);
        return button;
    }

    // -------------------------------------------------------------------------
    // Private update helpers
    // -------------------------------------------------------------------------

 

    /**
     * Applies the currently selected theme to the active projection and
     * triggers a repaint.
     */
    private void updateTheme() {
        IMapProjection proj = mapView.getMapProjection();
        if (proj != null) {
            proj.setTheme(currentTheme);
            mapView.refresh();
        }
    }

    // -------------------------------------------------------------------------
    // Public accessor
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link MapTheme} currently selected by the theme radio
     * buttons.
     *
     * <p>Called by {@link MapView2D#setProjection(EProjection)} so that a
     * newly constructed projection can be initialized with the theme the user
     * has already selected.</p>
     *
     * @return the currently active theme; never {@code null}
     */
    public MapTheme getCurrentTheme() { return currentTheme; }

    /** Factory-backed entry displayed by the projection selector. */
    private record ProjectionOption(String name,
            Function<MapTheme, ? extends IMapProjection> factory) {
        @Override
        public String toString() {
            return name;
        }
    }

    // -------------------------------------------------------------------------
    // Layout helpers
    // -------------------------------------------------------------------------

    /**
     * Forces a {@link JComponent} to left-align within a {@link BoxLayout}
     * parent. Without this, BoxLayout centres components horizontally.
     *
     * @param c the component to left-align
     */
    private static void leftAlign(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
    }
}
