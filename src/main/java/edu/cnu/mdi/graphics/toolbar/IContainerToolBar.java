package edu.cnu.mdi.graphics.toolbar;

/**
 * Minimal toolbar contract exposed to containers and non-UI helpers.
 * Keeps containers ignorant of Swing button classes and concrete toolbar types.
 */
public interface IContainerToolBar {

    /** Update enable/disable state (e.g. delete button) based on container selection. */
    void updateButtonState();

    /** Update status text (e.g. mouse world position). */
    void setText(String text);

    /** Return to the configured default tool (typically pointer). */
    void resetDefaultSelection();
}
