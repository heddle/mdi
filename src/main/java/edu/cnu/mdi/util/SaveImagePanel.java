package edu.cnu.mdi.util;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 * The choices of the "Save Image" dialog: which area to capture and at what
 * size. The area choice appears only when there are two areas to choose from.
 */
@SuppressWarnings("serial")
final class SaveImagePanel extends JPanel {

	private static final int[] SCALES = { 1, 2, 4 };

	private final Component content;
	private final Component view;

	private final JRadioButton contentButton = new JRadioButton("Content only");
	private final JRadioButton viewButton = new JRadioButton("Entire view");
	private final JRadioButton[] scaleButtons = new JRadioButton[SCALES.length];

	/**
	 * @param content        the content area, or {@code null} if there is none
	 * @param view           the whole view, or {@code null} if there is none
	 * @param preferView     whether to start on the whole view
	 * @param preferredScale the size to start on
	 */
	SaveImagePanel(Component content, Component view, boolean preferView, int preferredScale) {
		this.content = hasSize(content) ? content : null;
		this.view = hasSize(view) ? view : null;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		if (hasAreaChoice()) {
			heading("Area", 0);
			ButtonGroup areas = new ButtonGroup();
			for (JRadioButton button : new JRadioButton[] { contentButton, viewButton }) {
				areas.add(button);
				add(button);
				button.addActionListener(e -> showPixelSizes());
			}
			(preferView ? viewButton : contentButton).setSelected(true);
		}

		heading("Size", hasAreaChoice() ? 8 : 0);
		ButtonGroup sizes = new ButtonGroup();
		for (int i = 0; i < SCALES.length; i++) {
			scaleButtons[i] = new JRadioButton();
			sizes.add(scaleButtons[i]);
			add(scaleButtons[i]);
			scaleButtons[i].setSelected(i == 0 || SCALES[i] == preferredScale);
		}
		showPixelSizes();
	}

	/** Whether both the content area and the whole view can be captured. */
	boolean hasAreaChoice() {
		return content != null && view != null;
	}

	/** The component to capture, or {@code null} if there is nothing to capture. */
	Component area() {
		if (hasAreaChoice()) {
			return viewButton.isSelected() ? view : content;
		}
		return content != null ? content : view;
	}

	/** The chosen size: pixels per component unit. */
	int scale() {
		for (int i = 0; i < SCALES.length; i++) {
			if (scaleButtons[i].isSelected()) {
				return SCALES[i];
			}
		}
		return 1;
	}

	private void showPixelSizes() {
		Component area = area();
		for (int i = 0; i < SCALES.length; i++) {
			String text = SCALES[i] + "×";
			if (area != null) {
				text += "  (" + area.getWidth() * SCALES[i] + " × " + area.getHeight() * SCALES[i]
						+ " pixels)";
			}
			scaleButtons[i].setText(text);
		}
	}

	private void heading(String text, int gap) {
		JLabel label = new JLabel(text);
		label.setBorder(BorderFactory.createEmptyBorder(gap, 0, 2, 0));
		add(label);
	}

	private static boolean hasSize(Component c) {
		return c != null && c.getWidth() > 0 && c.getHeight() > 0;
	}
}
