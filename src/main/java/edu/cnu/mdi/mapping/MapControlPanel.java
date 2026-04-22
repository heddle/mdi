package edu.cnu.mdi.mapping;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

import edu.cnu.mdi.component.CommonBorder;
import edu.cnu.mdi.component.EnumComboBox;
import edu.cnu.mdi.component.RangeSlider;
import edu.cnu.mdi.mapping.projection.EProjection;
import edu.cnu.mdi.mapping.projection.IMapProjection;
import edu.cnu.mdi.mapping.render.CityPointRenderer;
import edu.cnu.mdi.mapping.theme.MapTheme;
import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * Side-panel control widget for {@link MapView2D}.
 *
 * <p>Provides interactive controls for:
 * <ul>
 *   <li><b>Projection</b> — an {@link EnumComboBox} that switches the active
 *       {@link EProjection}.</li>
 *   <li><b>City label visibility</b> — a checkbox that toggles city name
 *       labels via {@link CityPointRenderer#setDrawLabels(boolean)}.</li>
 *   <li><b>Minimum population</b> — a {@link RangeSlider} that filters cities
 *       below a given population threshold via
 *       {@link CityPointRenderer#setMinPopulation(long)}.</li>
 *   <li><b>Map theme</b> — radio buttons selecting between the built-in
 *       {@link MapTheme} presets (Light, Dark, Blue).</li>
 * </ul>
 *
 * <h2>Coupling</h2>
 * <p>This panel holds a direct reference to a {@link MapView2D} and calls
 * its public API in response to user interaction. The coupling is intentional
 * and kept minimal: the panel only calls well-defined accessors on the view
 * ({@link MapView2D#getCityRenderer()},
 * {@link MapView2D#getMapProjection()},
 * {@link MapView2D#setProjection(EProjection)},
 * {@link MapView2D#refresh()}).</p>
 *
 * <h2>Slider maximum</h2>
 * <p>The maximum value of the population slider is
 * {@link MapConstants#MAX_POP_SLIDER_VALUE}. Previously the same constant was
 * duplicated in both this class and {@link MapView2D}; it is now sourced from
 * a single location to prevent the two values from diverging.</p>
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

    /** Whether city name labels are visible. Tracks the checkbox state. */
    private boolean showNames = true;

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
        createCheckboxes(this);
        createMinPopRangeSlider(this);
        createThemeSelector(this);
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

        EnumComboBox<EProjection> projCombo = EProjection.createComboBox();
        projCombo.addActionListener(e -> {
            EProjection selected = projCombo.getSelectedEnum();
            mapView.setProjection(selected);
        });
        projCombo.setFont(font);
        leftAlign(projCombo);

        panel.add(projCombo);
        panel.add(Box.createVerticalStrut(6));
    }

    /**
     * Adds the "Show city names" checkbox to {@code panel}.
     *
     * <p>Toggling the checkbox calls
     * {@link CityPointRenderer#setDrawLabels(boolean)} and triggers a
     * repaint.</p>
     *
     * @param panel the panel to add the checkbox to
     */
    private void createCheckboxes(JPanel panel) {
        JCheckBox showCityNamesCheckBox = new JCheckBox("Show city names", true);
        showCityNamesCheckBox.setFont(font);
        showCityNamesCheckBox.setHorizontalAlignment(SwingConstants.LEFT);
        showCityNamesCheckBox.addActionListener(e -> {
            showNames = showCityNamesCheckBox.isSelected();
            updateCityLabelVisibility();
        });
        leftAlign(showCityNamesCheckBox);
        panel.add(showCityNamesCheckBox);
        panel.add(Box.createVerticalStrut(6));
    }

    /**
     * Adds the minimum-population range slider to {@code panel}.
     *
     * <p>The slider range is [0, {@link MapConstants#MAX_POP_SLIDER_VALUE}]
     * and the initial value is half that maximum. Changing the slider value
     * calls {@link CityPointRenderer#setMinPopulation(long)} and triggers a
     * repaint.</p>
     *
     * @param panel the panel to add the slider to
     */
    private void createMinPopRangeSlider(JPanel panel) {
        int max = MapConstants.MAX_POP_SLIDER_VALUE;  // single source of truth
        RangeSlider minPopSlider = new RangeSlider(0, max, max / 2, true);
        minPopSlider.setOnChange(this::updateMinPopulationFilter);
        minPopSlider.setBorder(new CommonBorder("Minimum Population"));
        leftAlign(minPopSlider);
        panel.add(minPopSlider);
        panel.add(Box.createVerticalStrut(6));
       
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
            if      (lightThemeButton.isSelected()) currentTheme = lightTheme;
            else if (darkThemeButton.isSelected())  currentTheme = darkTheme;
            else if (blueThemeButton.isSelected())  currentTheme = blueTheme;
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
     * Applies a new minimum population filter to the city renderer and
     * triggers a repaint.
     *
     * @param pop the new minimum population value from the slider
     */
    private void updateMinPopulationFilter(int pop) {
        CityPointRenderer renderer = mapView.getCityRenderer();
        if (renderer != null) {
            renderer.setMinPopulation(pop);
            mapView.refresh();
        }
    }

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

    /**
     * Applies the current label-visibility setting to the city renderer and
     * triggers a repaint.
     */
    private void updateCityLabelVisibility() {
        CityPointRenderer renderer = mapView.getCityRenderer();
        if (renderer != null) {
            renderer.setDrawLabels(showNames);
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
