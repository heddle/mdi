package edu.cnu.mdi.graphics.toolbar;

import java.awt.Dimension;

import javax.swing.Icon;
import javax.swing.JToggleButton;

import edu.cnu.mdi.graphics.ImageManager;

/**
 * A toolbar toggle button that activates a registered {@link ITool}.
 * <p>
 * This button intentionally contains no tool behavior (no mouse logic, no
 * panning math, no rubber-banding). It simply informs the
 * {@link ToolController} which tool id should become active.
 * </p>
 * <p>
 * If the button is deselected (e.g., by clicking it while active), the
 * controller resets to its configured default tool.
 * </p>
 *
 * @author heddle
 */
@SuppressWarnings("serial")
public class ToolToggleButton extends JToggleButton {

	private final String toolId;
	private boolean programmatic;

	/**
	 * Create a new tool toggle button.
	 *
	 * @param controller the tool controller used to activate tools.
	 * @param toolId     the id of the tool to activate when selected.
	 * @param iconFile   image resource path for the button icon.
	 * @param tooltip    tooltip text for the button.
	 * @param w          preferred width in pixels.
	 * @param h          preferred height in pixels.
	 */
	public ToolToggleButton(ToolController controller, String toolId, String iconFile, String tooltip, int w, int h) {

		this.toolId = java.util.Objects.requireNonNull(toolId, "toolId");
		java.util.Objects.requireNonNull(controller, "controller");

		setFocusPainted(false);
		setToolTipText(tooltip);

		if (iconFile != null) {
			Icon icon = ImageManager.getInstance().loadUiIcon(iconFile, 20, 20);
			if (icon != null) {
				setIcon(icon);
			} else {
				setText("?");
			}

		}

		Dimension d = new Dimension(w, h);
		setPreferredSize(d);
		setMinimumSize(d);
		setMaximumSize(d);

		addActionListener(e -> {
			if (programmatic) {
				return;
			}
			if (isSelected()) {
				controller.select(toolId);
			} else {
				controller.resetToDefault();
			}
		});

	}

	public void setSelectedProgrammatically(boolean selected) {
		programmatic = true;
		try {
			setSelected(selected);
		} finally {
			programmatic = false;
		}
	}

	/**
	 * Get the tool id that this button activates.
	 *
	 * @return the tool id.
	 */
	public String toolId() {
		return toolId;
	}
}
