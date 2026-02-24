package edu.cnu.mdi.view.demo;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * A simple control panel for the NetworkLayoutDemoView. 
 * This panel contains checkboxes that allow the user to toggle various display options in the view, such as showing a "snap to" grid. 
 * The checkboxes are aligned to the left and styled with a medium font for better readability.
 */
@SuppressWarnings("serial")
public class ControlPanel extends JPanel {

	// the parent view being controlled
	private NetworkLayoutDemoView view;

	/**
	 * Construct a control panel for the given view.
	 *
	 * @param view the view to control
	 */
	public ControlPanel(NetworkLayoutDemoView view) {
		this.view = view;
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		createCheckboxes(this);

	}

	// create any display checkboxes
	private void createCheckboxes(JPanel panel) {
		JCheckBox showGrid = createCheckBox("Show \"snap to\" grid", true);
		showGrid.addActionListener(e -> {
			GridDrawer gridDrawer = view.getGridDrawer();
			gridDrawer.setVisible(showGrid.isSelected());
			view.refresh();
		});
		
		JCheckBox showNames = createCheckBox("Show node names", view.showNames());
		showNames.addActionListener(e -> {
			view.setShowNames(showNames.isSelected());
			view.refresh();
		});

		panel.add(showGrid);
		panel.add(showNames);
		panel.add(Box.createVerticalStrut(12));

	}
	
	// helper method to create a left-aligned checkbox with the specified label and initial state
	private JCheckBox createCheckBox(String label, boolean initialState) {
		JCheckBox checkbox = new JCheckBox(label, initialState);
		checkbox.setFont(Fonts.mediumFont);
		checkbox.setHorizontalAlignment(SwingConstants.LEFT);
		leftAlign(checkbox);
		return checkbox;
	}

	/** Force BoxLayout children to left-align instead of centering. */
	private static void leftAlign(JComponent c) {
		c.setAlignmentX(Component.LEFT_ALIGNMENT);
	}

}
