package edu.cnu.mdi.graphics.toolbar;

import edu.cnu.mdi.graphics.rubberband.Rubberband;

/**
 * Minimal toolbar contract exposed to containers and non-UI helpers. Keeps
 * containers ignorant of Swing button classes and concrete toolbar types.
 */
public interface IContainerToolBar {

	/**
	 * Update enable/disable state (e.g. delete button) based on container
	 * selection.
	 */
	void updateButtonState();

	/** Update status text (e.g. mouse world position). */
	void setText(String text);

	/** Return to the configured default tool (typically pointer). */
	void resetDefaultSelection();

	// ------------------------------------------------------------
	// Convenience extension hooks (default no-ops)
	// ------------------------------------------------------------

	/**
	 * Register a tool and add it as a mutually-exclusive toggle button. Default
	 * implementation does nothing, so containers can use other toolbars.
	 */
	default ToolToggleButton addToolToggle(ITool tool, String iconFile, String tooltip) {
		return null;
	}

	/**
	 * Add a one-shot action button (not part of the toggle group). Default
	 * implementation does nothing.
	 */
	default IContainerToolBar addOneShot(ToolActionButton button) {
		return this;
	}

	/**
	 * Add spacing on the toolbar. Default no-op.
	 */
	default IContainerToolBar spacer(int px) {
		return this;
	}

	/**
	 * Set the rubber-band policy for box-zoom gestures.
	 * 
	 * @param policy the desired policy
	 */
	public void setBoxZoomRubberbandPolicy(Rubberband.Policy policy);
}
