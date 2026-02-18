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
		JCheckBox showGrid = new JCheckBox("Show \"snap to\" grid", true);
		showGrid.setFont(Fonts.mediumFont);
		showGrid.setHorizontalAlignment(SwingConstants.LEFT);

		showGrid.addActionListener(e -> {
			GridDrawer gridDrawer = view.getGridDrawer();
			gridDrawer.setVisible(showGrid.isSelected());
			view.refresh();
		});

		leftAlign(showGrid);
		panel.add(showGrid);
		panel.add(Box.createVerticalStrut(12));

	}

	/** Force BoxLayout children to left-align instead of centering. */
	private static void leftAlign(JComponent c) {
		c.setAlignmentX(Component.LEFT_ALIGNMENT);
	}

}
