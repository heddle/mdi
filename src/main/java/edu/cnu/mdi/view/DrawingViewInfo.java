package edu.cnu.mdi.view;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes the {@link DrawingView} for the in-app help / info dialog.
 *
 * <p>Returned by {@link DrawingView#getViewInfo()} and displayed when the
 * user clicks the info button in the toolbar.</p>
 */
public class DrawingViewInfo extends AbstractViewInfo {

    // -------------------------------------------------------------------------
    // Mandatory content
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @return {@code "Drawing View"}
     */
    @Override
    public String getTitle() {
        return "Drawing View";
    }

    /**
     * {@inheritDoc}
     *
     * @return a short description of the view's purpose
     */
    @Override
    public String getPurpose() {
        return "A sandbox canvas for experimenting with MDI drawing and "
             + "interaction capabilities. Supports rubber-band shape creation, "
             + "zoom and pan navigation, interactive item editing, and image "
             + "drag-and-drop. Not intended as a full-featured drawing "
             + "application \u2014 its purpose is to demonstrate the tools and "
             + "features available to any MDI container-backed view.";
    }

    // -------------------------------------------------------------------------
    // Structured usage — steps first, then supplementary bullets
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Step-by-step instructions covering the main interaction modes.</p>
     */
    @Override
    public List<String> getUsageSteps() {
        return List.of(
                "Select a drawing tool from the toolbar (rectangle, oval, or "
              + "line), then click and drag on the canvas to draw the shape. "
              + "Release the mouse button to finish.",

                "Click any existing shape to select it. A selected shape shows "
              + "handles. Drag the shape to reposition it, or use the style "
              + "editor to change its fill color, border color, and stroke width.",

                "To zoom in on an area, select the rubber-band zoom tool and "
              + "drag a rectangle around the region of interest. Use the zoom "
              + "buttons or the mouse wheel to zoom in and out uniformly.",

                "Use the pan tool (or hold the middle mouse button) to scroll "
              + "the canvas in any direction.",

                "To insert an image, drag a PNG or JPEG file from the file "
              + "system and drop it onto the canvas. The image is placed on the "
              + "annotation layer and rendered above all drawn shapes.",

                "To delete selected items, press Delete or Backspace."
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Supplementary notes about drawing and navigation behaviour.</p>
     */
    @Override
    public List<String> getUsageBullets() {
        return List.of(
                "All coordinates are in a world coordinate system (unit square "
              + "by default). The toolbar's status area shows the current cursor "
              + "position in world coordinates as you move the mouse.",

                "The canvas can hold multiple shapes simultaneously. All shapes "
              + "on the annotation layer are drawn on top of shapes on the "
              + "default content layer.",

                "Image files are verified by attempting to decode them (not just "
              + "by extension), so mis-named files are rejected cleanly. Only the "
              + "first file in a multi-file drop is used.",

                "Zoom and pan are non-destructive \u2014 item positions are "
              + "stored in world coordinates and redrawn correctly at any zoom "
              + "level.",

                "Use \u201cRestore Default View\u201d from the right-click context "
              + "menu to reset the zoom and pan to the original full-canvas view."
        );
    }

    // -------------------------------------------------------------------------
    // Keyboard shortcuts
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Returns keyboard shortcuts in display order.</p>
     */
    @Override
    public Map<String, String> getKeyboardShortcuts() {
        Map<String, String> shortcuts = new LinkedHashMap<>();
        shortcuts.put("Delete / Backspace", "Delete all selected items");
        shortcuts.put("Mouse wheel",        "Zoom in / out at cursor");
        shortcuts.put("Middle mouse drag",  "Pan the canvas");
        return shortcuts;
    }

    // -------------------------------------------------------------------------
    // Technical notes
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Returns implementation notes for developers extending the view.</p>
     */
    @Override
    public String getTechnicalNotes() {
        return "DrawingView is a container-backed BaseView with toolbar bits: "
             + "STATUS | DRAWINGTOOLS | ZOOMTOOLS | PAN | INFO. "
             + "The canvas is a BaseContainer with a unit-square world system "
             + "(Rectangle2D.Double(0, 0, 1, 1)). "
             + "Drawn shapes are AItem subclasses placed on the container's "
             + "default content layer; dropped images are ImageItems placed on "
             + "the annotation layer (always rendered last). "
             + "File drag-and-drop uses ImageFilters.isActualImage (decodes the "
             + "file with ImageIO.read rather than checking the extension), "
             + "installed via BaseView.enableFileDrop. "
             + "To add new item types or tools, subclass BaseContainer or "
             + "BaseToolBar and supply them via the PropertyUtils.CONTAINERFACTORY "
             + "or PropertyUtils.TOOLBARBITS constructor properties.";
    }

    // -------------------------------------------------------------------------
    // Appearance
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Uses a neutral dark gray accent \u2014 appropriate for a general-purpose
     * drawing tool with no domain-specific color associations.</p>
     */
    @Override
    protected String getAccentColorHex() {
        return "#444444";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code "Drawing View \u2014 MDI Framework"}
     */
    @Override
    public String getFooter() {
        return "Drawing View \u2014 MDI Framework";
    }
}