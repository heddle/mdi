package edu.cnu.mdi.mapping;

import java.awt.Component;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

import edu.cnu.mdi.component.CommonBorder;
import edu.cnu.mdi.component.EnumComboBox;
import edu.cnu.mdi.component.RangeSlider;
import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * A panel for map controls.
 */
@SuppressWarnings("serial")
public class MapControlPanel extends JPanel {
	// max slider value for minimum population
	private static final int MAX_POP_SLIDER_VALUE = 2_000_000;
	
	// current map theme and available themes
	private MapTheme _darkTheme = MapTheme.dark();
	private MapTheme _lightTheme = MapTheme.light();
	private MapTheme _blueTheme = MapTheme.blue();
	private MapTheme _currentTheme = _lightTheme;


	private JRadioButton _lightThemeButton;
	private JRadioButton _darkThemeButton;
	private JRadioButton _blueThemeButton;
	
	private boolean showNames = true;
	
	// Reference to the map view being controlled
	private MapView2D mapView;

	
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
	
	//create a radio button for theme selection
	private JRadioButton createThemeButton(String label, ButtonGroup bg, ActionListener al, boolean selected) {
		JRadioButton themeButton = new JRadioButton();
		themeButton.setSelected(selected);
		themeButton.setFont(Fonts.mediumFont);
		themeButton.setText(label);
		bg.add(themeButton);
		themeButton.addActionListener(al);
		return themeButton;
	}
	
	//create the projection selection combo box
	private void createProjectionCombo(JPanel panel) {
		JLabel projLabel = new JLabel("Projection");
		projLabel.setFont(Fonts.mediumFont);
		leftAlign(projLabel);

		EnumComboBox<EProjection> projCombo = EProjection.createComboBox();

		projCombo.addActionListener(e -> {
			EProjection selected = projCombo.getSelectedEnum();
			mapView.setProjection(selected);
		});

		leftAlign(projCombo);

		panel.add(projLabel);
		panel.add(Box.createVerticalStrut(4));
		panel.add(projCombo);
		panel.add(Box.createVerticalStrut(12));

	}
	
	//create any display checkboxes
	private void createCheckboxes(JPanel panel) {
		JCheckBox showCityNamesCheckBox = new JCheckBox("Show city names", true);
		showCityNamesCheckBox.setFont(Fonts.mediumFont);
		showCityNamesCheckBox.setHorizontalAlignment(SwingConstants.LEFT);
		
		showCityNamesCheckBox.addActionListener(e -> {
			showNames = showCityNamesCheckBox.isSelected();
			updateCityLabelVisibility();
		});
		
		leftAlign(showCityNamesCheckBox);
		panel.add(showCityNamesCheckBox);
		panel.add(Box.createVerticalStrut(12));
	}
	
	//create the minimum population slider
	private void createMinPopRangeSlider(JPanel panel) {
		RangeSlider minPopSlider = new RangeSlider(0, MAX_POP_SLIDER_VALUE, MAX_POP_SLIDER_VALUE / 2, true);
		minPopSlider.setOnChange(this::updateMinPopulationFilter);
		minPopSlider.setBorder(new CommonBorder("Minimum Population"));
		leftAlign(minPopSlider);
		panel.add(minPopSlider);
		panel.add(Box.createVerticalStrut(12));
	}

	/** Force BoxLayout children to left-align instead of centering. */
	private static void leftAlign(JComponent c) {
		c.setAlignmentX(Component.LEFT_ALIGNMENT);
	}
	
	private void createThemeSelector(JPanel panel) {
		ButtonGroup themeGroup = new ButtonGroup();
		ActionListener themeListener = e -> {
			if (_lightThemeButton.isSelected()) {
				_currentTheme = _lightTheme;
			} else if (_darkThemeButton.isSelected()) {
				_currentTheme = _darkTheme;
			} else if (_blueThemeButton.isSelected()) {
				_currentTheme = _blueTheme;
			}
			updateTheme();
		};

		_lightThemeButton = createThemeButton("Light", themeGroup, themeListener, true);
		_darkThemeButton = createThemeButton("Dark", themeGroup, themeListener, false);
		_blueThemeButton = createThemeButton("Blue", themeGroup, themeListener, false);

		leftAlign(_lightThemeButton);
		leftAlign(_darkThemeButton);
		leftAlign(_blueThemeButton);		
		
		
		JPanel subPanel = new JPanel();
		subPanel.setAlignmentX(Component.LEFT_ALIGNMENT);


		subPanel.add(_lightThemeButton);
		subPanel.add(_darkThemeButton);
		subPanel.add(_blueThemeButton);
		subPanel.add(Box.createVerticalStrut(8));
		subPanel.setBorder(new CommonBorder("Map Theme"));
		panel.add(subPanel);

	}
	/**
	 * Update the minimum population filter for displayed cities and refresh the
	 * view. The population value is provided directly by the slider.
	 *
	 * @param pop minimum population (inclusive) for city display.
	 */
	private void updateMinPopulationFilter(int pop) {
		if (mapView.getCityRenderer() != null) {
			long minPop = pop;
			mapView.getCityRenderer().setMinPopulation(minPop);
			mapView.refresh();
		}
	}
	
	/**
	 * Update the map theme to light or dark and refresh the display.
	 *
	 */
	private void updateTheme() {
		if (mapView.getMapProjection() != null) {
			mapView.getMapProjection().setTheme(_currentTheme);
			mapView.refresh();
		}
	}
	
	/**
	 * Update whether city names (labels) are drawn.
	 */
	private void updateCityLabelVisibility() {
		if (mapView.getCityRenderer() != null) {
			// Adjust to match your CityPointRenderer API
			mapView.getCityRenderer().setDrawLabels(showNames);
			mapView.refresh();
		}
	}
	
	/**
	 * Get the current map theme.
	 * 
	 * @return the current MapTheme
	 */
	public MapTheme getCurrentTheme() {
		return _currentTheme;
	}


}
