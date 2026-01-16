package edu.cnu.mdi.experimental;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import edu.cnu.mdi.util.Environment;

/**
 * Bit flags controlling which pre-defined buttons appear on a {@link BaseToolBar}.
 * <p>
 * Values are written in octal.
 * </p>
 *
 * <h2>Stable IDs</h2>
 * <p>
 * Each predefined bit maps to a stable string id via {@link #getId(long)}. These
 * ids are intended for programmatic lookup through {@link AToolBar#getButton(String)}
 * and related methods:
 * </p>
 *
 * <pre>{@code
 * toolbar.setButtonEnabled(ToolBits.getId(ToolBits.DELETEBUTTON), hasSelection);
 * }</pre>
 *
 * <p>
 * Applications may add their own buttons with their own ids.
 * </p>
 */
public final class ToolBits {

	private ToolBits() {
	}

	/**
	 * Bit flags for each standard toolbar button.
	 * <p>
	 * The status field is not a button; it is a small mouse-over feedback area on
	 * the toolbar.
	 * </p>
	 */
	public static final long POINTERBUTTON   = 01L;
	public static final long ELLIPSEBUTTON   = 02L;
	public static final long TEXTBUTTON      = 04L;
	public static final long RECTANGLEBUTTON = 010L;
	public static final long POLYGONBUTTON   = 020L;
	public static final long LINEBUTTON      = 040L;
	public static final long STYLEBUTTON     = 0100L;
	public static final long DELETEBUTTON    = 0200L;
	public static final long CENTERBUTTON    = 0400L;
	public static final long UNDOZOOMBUTTON  = 01000L;
	public static final long RADARCBUTTON    = 02000L;
	public static final long POLYLINEBUTTON  = 04000L;
	public static final long MAGNIFYBUTTON   = 010000L;
	public static final long BOXZOOMBUTTON   = 020000L;
	public static final long PANBUTTON       = 040000L;
	public static final long CONNECTORBUTTON = 0100000L;
	public static final long ZOOMINBUTTON    = 0200000L;
	public static final long ZOOMOUTBUTTON   = 0400000L;
	public static final long RESETZOOMBUTTON = 01000000L;
	public static final long CAMERABUTTON    = 02000000L;
	public static final long PRINTERBUTTON   = 04000000L;
	public static final long STATUSFIELD     = 010000000L;

	/**
	 * Return a stable string id for a predefined toolbar bit.
	 * <p>
	 * These ids are intended for programmatic lookup through
	 * {@link AToolBar#getButton(String)} and related methods.
	 * </p>
	 * <p>
	 * For application-defined buttons, choose your own ids (e.g., "myCustomTool").
	 * </p>
	 *
	 * @param buttonBit a predefined bit constant such as {@link #POINTERBUTTON}.
	 * @return a stable id string (never null). Unknown bits map to {@code "bit_<value>"}.
	 */
	public static String getId(long buttonBit) {
		if (buttonBit == POINTERBUTTON)   return "pointer";
		if (buttonBit == ELLIPSEBUTTON)   return "ellipse";
		if (buttonBit == TEXTBUTTON)      return "text";
		if (buttonBit == RECTANGLEBUTTON) return "rectangle";
		if (buttonBit == POLYGONBUTTON)   return "polygon";
		if (buttonBit == LINEBUTTON)      return "line";
		if (buttonBit == STYLEBUTTON)     return "style";
		if (buttonBit == DELETEBUTTON)    return "delete";
		if (buttonBit == CENTERBUTTON)    return "center";
		if (buttonBit == UNDOZOOMBUTTON)  return "undoZoom";
		if (buttonBit == RADARCBUTTON)    return "radArc";
		if (buttonBit == POLYLINEBUTTON)  return "polyline";
		if (buttonBit == MAGNIFYBUTTON)   return "magnify";
		if (buttonBit == BOXZOOMBUTTON)   return "boxZoom";
		if (buttonBit == PANBUTTON)       return "pan";
		if (buttonBit == CONNECTORBUTTON) return "connector";
		if (buttonBit == ZOOMINBUTTON)    return "zoomIn";
		if (buttonBit == ZOOMOUTBUTTON)   return "zoomOut";
		if (buttonBit == RESETZOOMBUTTON) return "resetZoom";
		if (buttonBit == CAMERABUTTON)    return "camera";
		if (buttonBit == PRINTERBUTTON)   return "printer";
		if (buttonBit == STATUSFIELD)     return "status";
		return "bit_" + buttonBit;
	}

	/**
	 * Map the button bit to the corresponding icon path <em>relative</em> to the
	 * MDI resource root.
	 * <p>
	 * For buttons without an icon (e.g. {@link #STATUSFIELD}), the value is the
	 * empty string.
	 * </p>
	 */
	private static final Map<Long, String> BUTTON_ICON_MAP;
	static {
		Map<Long, String> m = new HashMap<>();
		m.put(POINTERBUTTON,    "images/svg/pointer.svg");
		m.put(ELLIPSEBUTTON,    "images/svg/ellipse.svg");
		m.put(TEXTBUTTON,       "images/svg/text.svg");
		m.put(RECTANGLEBUTTON,  "images/svg/rectangle.svg");
		m.put(POLYGONBUTTON,    "images/svg/polygon.svg");
		m.put(LINEBUTTON,       "images/svg/line.svg");
		m.put(STYLEBUTTON,      "images/svg/colorwheel.svg");
		m.put(DELETEBUTTON,     "images/svg/delete.svg");
		m.put(CENTERBUTTON,     "images/svg/center.svg");
		m.put(UNDOZOOMBUTTON,   "images/svg/undo_zoom.svg");
		m.put(POLYLINEBUTTON,   "images/svg/polyline.svg");
		m.put(BOXZOOMBUTTON,    "images/svg/box_zoom.svg");
		m.put(PANBUTTON,        "images/svg/pan.svg");
		m.put(CONNECTORBUTTON,  "images/svg/connect.svg");
		m.put(ZOOMINBUTTON,     "images/svg/zoom_in.svg");
		m.put(ZOOMOUTBUTTON,    "images/svg/zoom_out.svg");
		m.put(RESETZOOMBUTTON,  "images/svg/reset_zoom.svg");
		m.put(CAMERABUTTON,     "images/svg/camera.svg");
		m.put(PRINTERBUTTON,    "images/svg/printer.svg");
		m.put(RADARCBUTTON,     "images/svg/radarc.svg");
		m.put(MAGNIFYBUTTON,    "images/svg/magnify.svg");
		m.put(STATUSFIELD,      ""); // No icon for status field
		BUTTON_ICON_MAP = Collections.unmodifiableMap(m);
	}

	/**
	 * Map the button bit to the corresponding tooltip text.
	 */
	private static final Map<Long, String> BUTTON_TOOLTIP_MAP;
	static {
		Map<Long, String> m = new HashMap<>();
		m.put(POINTERBUTTON,    "Make single or bulk selection");
		m.put(ELLIPSEBUTTON,    "Create an ellipse item");
		m.put(TEXTBUTTON,       "Create a text item");
		m.put(RECTANGLEBUTTON,  "Create a rectangle item");
		m.put(POLYGONBUTTON,    "Create a polygon item");
		m.put(LINEBUTTON,       "Create a line item");
		m.put(STYLEBUTTON,      "Edit style of selected item(s)");
		m.put(DELETEBUTTON,     "Delete selected item(s)");
		m.put(CENTERBUTTON,     "Recenter the view at clicked location");
		m.put(UNDOZOOMBUTTON,   "Undo last zoom action");
		m.put(POLYLINEBUTTON,   "Create a polyline item");
		m.put(BOXZOOMBUTTON,    "Rubber-band zoom to area");
		m.put(PANBUTTON,        "Pan the view by dragging");
		m.put(CONNECTORBUTTON,  "Connect two item with a connector line");
		m.put(ZOOMINBUTTON,     "Zoom in by a fixed amount");
		m.put(ZOOMOUTBUTTON,    "Zoom out by a fixed amount");
		m.put(RESETZOOMBUTTON,  "Restore zoom to default");
		m.put(CAMERABUTTON,     "Snapshot of the current canvas as a png image");
		m.put(PRINTERBUTTON,    "Print the current canvas");
		m.put(RADARCBUTTON,     "Create a radarc item");
		m.put(MAGNIFYBUTTON,    "Magnify area under mouse cursor");
		m.put(STATUSFIELD,      "");
		BUTTON_TOOLTIP_MAP = Collections.unmodifiableMap(m);
	}

	/*
	 * Note that in the predefined sets of buttons to include on a toolbar,
	 * the order of the bits does NOT determine order of the buttons on the toolbar.
	 */
	public static final long ANNOTATIONTOOLS = POINTERBUTTON | ELLIPSEBUTTON | TEXTBUTTON | RECTANGLEBUTTON | POLYGONBUTTON | LINEBUTTON
			| POLYLINEBUTTON | STYLEBUTTON | DELETEBUTTON;

	public static final long DRAWINGTOOLS = ANNOTATIONTOOLS | RADARCBUTTON;

	public static final long ZOOMTOOLS = BOXZOOMBUTTON | UNDOZOOMBUTTON | ZOOMINBUTTON | ZOOMOUTBUTTON
			| RESETZOOMBUTTON;

	public static final long PICVIEWSTOOLS = CAMERABUTTON | PRINTERBUTTON;

	public static final long NAVIGATIONTOOLS = POINTERBUTTON | ZOOMTOOLS | PANBUTTON | CENTERBUTTON;

	public static final long PLOTTOOLS = POINTERBUTTON | ZOOMTOOLS | PICVIEWSTOOLS & ~UNDOZOOMBUTTON;

	public static final long EVERYTHING = 01777777777777777777777L;

	/**
	 * Get the icon path relative to the MDI resource root for a predefined button.
	 *
	 * @param buttonBit the button bit flag.
	 * @return relative icon path, or {@code null} if no icon is defined for this bit.
	 */
	public static String getIconRelativePath(long buttonBit) {
		String rel = BUTTON_ICON_MAP.get(buttonBit);
		if (rel == null || rel.isBlank()) {
			return null;
		}
		return rel;
	}

	/**
	 * Get the full resource path for the icon corresponding to the given button bit.
	 * <p>
	 * If the button has no icon (e.g. {@link #STATUSFIELD}), this method returns
	 * {@code null}.
	 * </p>
	 *
	 * @param buttonBit the button bit flag.
	 * @return full resource path, or {@code null} if no icon is defined.
	 */
	public static String getResourcePath(long buttonBit) {
		String rel = getIconRelativePath(buttonBit);
		return (rel == null) ? null : (Environment.MDI_RESOURCE_PATH + rel);
	}

	/**
	 * Get the tooltip string for a predefined button bit.
	 *
	 * @param buttonBit the button bit flag.
	 * @return tooltip string, or {@code null} if none is defined.
	 */
	public static String getToolTip(long buttonBit) {
		return BUTTON_TOOLTIP_MAP.get(buttonBit);
	}

	private static boolean hasBit(long buttonBit, long toolbarBits) {
		return (toolbarBits & buttonBit) == buttonBit;
	}

	// --- checks used by BaseToolBar ---

	protected static boolean hasPointerButton(long toolbarBits)   { return hasBit(POINTERBUTTON, toolbarBits); }
	protected static boolean hasPanButton(long toolbarBits)       { return hasBit(PANBUTTON, toolbarBits); }
	protected static boolean hasResetZoomButton(long toolbarBits) { return hasBit(RESETZOOMBUTTON, toolbarBits); }
	protected static boolean hasUndoZoomButton(long toolbarBits)  { return hasBit(UNDOZOOMBUTTON, toolbarBits); }
	protected static boolean hasCenterButton(long toolbarBits)    { return hasBit(CENTERBUTTON, toolbarBits); }
	protected static boolean hasPrinterButton(long toolbarBits)   { return hasBit(PRINTERBUTTON, toolbarBits); }
	protected static boolean hasCameraButton(long toolbarBits)    { return hasBit(CAMERABUTTON, toolbarBits); }
	protected static boolean hasDeleteButton(long toolbarBits)    { return hasBit(DELETEBUTTON, toolbarBits); }
	protected static boolean hasStyleButton(long toolbarBits)     { return hasBit(STYLEBUTTON, toolbarBits); }
	protected static boolean hasEllipseButton(long toolbarBits)   { return hasBit(ELLIPSEBUTTON, toolbarBits); }
	protected static boolean hasTextButton(long toolbarBits)      { return hasBit(TEXTBUTTON, toolbarBits); }
	protected static boolean hasRectangleButton(long toolbarBits) { return hasBit(RECTANGLEBUTTON, toolbarBits); }
	protected static boolean hasPolygonButton(long toolbarBits)   { return hasBit(POLYGONBUTTON, toolbarBits); }
	protected static boolean hasLineButton(long toolbarBits)      { return hasBit(LINEBUTTON, toolbarBits); }
	protected static boolean hasPolylineButton(long toolbarBits)  { return hasBit(POLYLINEBUTTON, toolbarBits); }
	protected static boolean hasRadArcButton(long toolbarBits)    { return hasBit(RADARCBUTTON, toolbarBits); }
	protected static boolean hasMagnifyButton(long toolbarBits)   { return hasBit(MAGNIFYBUTTON, toolbarBits); }
	protected static boolean hasConnectorButton(long toolbarBits) { return hasBit(CONNECTORBUTTON, toolbarBits); }
	protected static boolean hasStatusField(long toolbarBits)     { return hasBit(STATUSFIELD, toolbarBits); }
	protected static boolean hasZoomInButton(long toolbarBits)    { return hasBit(ZOOMINBUTTON, toolbarBits); }
	protected static boolean hasZoomOutButton(long toolbarBits)   { return hasBit(ZOOMOUTBUTTON, toolbarBits); }
	protected static boolean hasBoxZoomButton(long toolbarBits)   { return hasBit(BOXZOOMBUTTON, toolbarBits); }
}
