package edu.cnu.mdi.graphics.toolbar;

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
	public static final long POINTER   = 01L;
	public static final long ELLIPSE   = 02L;
	public static final long TEXT      = 04L;
	public static final long RECTANGLE = 010L;
	public static final long POLYGON   = 020L;
	public static final long LINE      = 040L;
	public static final long STYLEB     = 0100L;
	public static final long DELETE    = 0200L;
	public static final long CENTER    = 0400L;
	public static final long UNDOZOOM  = 01000L;
	public static final long RADARC    = 02000L;
	public static final long POLYLINE  = 04000L;
	public static final long MAGNIFY   = 010000L;
	public static final long BOXZOOM   = 020000L;
	public static final long PAN       = 040000L;
	public static final long CONNECTOR = 0100000L;
	public static final long ZOOMIN    = 0200000L;
	public static final long ZOOMOUT   = 0400000L;
	public static final long RESETZOOM = 01000000L;
	public static final long CAMERA    = 02000000L;
	public static final long PRINTER   = 04000000L;
	public static final long STATUS    = 010000000L;
	public static final long INFO      = 020000000L;

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
	 * @param buttonBit a predefined bit constant such as {@link #POINTER}.
	 * @return a stable id string (never null). Unknown bits map to {@code "bit_<value>"}.
	 */
	public static String getId(long buttonBit) {
		if (buttonBit == POINTER) {
			return "pointer";
		}
		if (buttonBit == ELLIPSE) {
			return "ellipse";
		}
		if (buttonBit == TEXT) {
			return "text";
		}
		if (buttonBit == RECTANGLE) {
			return "rectangle";
		}
		if (buttonBit == POLYGON) {
			return "polygon";
		}
		if (buttonBit == LINE) {
			return "line";
		}
		if (buttonBit == STYLEB) {
			return "style";
		}
		if (buttonBit == DELETE) {
			return "delete";
		}
		if (buttonBit == CENTER) {
			return "center";
		}
		if (buttonBit == UNDOZOOM) {
			return "undoZoom";
		}
		if (buttonBit == RADARC) {
			return "radArc";
		}
		if (buttonBit == POLYLINE) {
			return "polyline";
		}
		if (buttonBit == MAGNIFY) {
			return "magnify";
		}
		if (buttonBit == BOXZOOM) {
			return "boxZoom";
		}
		if (buttonBit == PAN) {
			return "pan";
		}
		if (buttonBit == CONNECTOR) {
			return "connector";
		}
		if (buttonBit == ZOOMIN) {
			return "zoomIn";
		}
		if (buttonBit == ZOOMOUT) {
			return "zoomOut";
		}
		if (buttonBit == RESETZOOM) {
			return "resetZoom";
		}
		if (buttonBit == CAMERA) {
			return "camera";
		}
		if (buttonBit == PRINTER) {
			return "printer";
		}
		if (buttonBit == STATUS) {
			return "status";
		}
		if (buttonBit == INFO) {
			return "info";
		}
		return "bit_" + buttonBit;
	}

	/**
	 * Map the button bit to the corresponding icon path <em>relative</em> to the
	 * MDI resource root.
	 * <p>
	 * For buttons without an icon (e.g. {@link #STATUS}), the value is the
	 * empty string.
	 * </p>
	 */
	private static final Map<Long, String> BUTTON_ICON_MAP;
	static {
		Map<Long, String> m = new HashMap<>();
		m.put(POINTER,    "images/svg/pointer.svg");
		m.put(ELLIPSE,    "images/svg/ellipse.svg");
		m.put(TEXT,       "images/svg/text.svg");
		m.put(RECTANGLE,  "images/svg/rectangle.svg");
		m.put(POLYGON,    "images/svg/polygon.svg");
		m.put(LINE,       "images/svg/line.svg");
		m.put(STYLEB,     "images/svg/colorwheel.svg");
		m.put(DELETE,     "images/svg/delete.svg");
		m.put(CENTER,     "images/svg/center.svg");
		m.put(UNDOZOOM,   "images/svg/undo_zoom.svg");
		m.put(POLYLINE,   "images/svg/polyline.svg");
		m.put(BOXZOOM,    "images/svg/box_zoom.svg");
		m.put(PAN,        "images/svg/pan.svg");
		m.put(CONNECTOR,  "images/svg/connect.svg");
		m.put(ZOOMIN,     "images/svg/zoom_in.svg");
		m.put(ZOOMOUT,    "images/svg/zoom_out.svg");
		m.put(RESETZOOM,  "images/svg/reset_zoom.svg");
		m.put(CAMERA,     "images/svg/camera.svg");
		m.put(PRINTER,    "images/svg/printer.svg");
		m.put(RADARC,     "images/svg/radarc.svg");
		m.put(MAGNIFY,    "images/svg/magnify.svg");
		m.put(INFO,       "images/svg/infocirc.svg");
		m.put(STATUS,      ""); // No icon for status field
		BUTTON_ICON_MAP = Collections.unmodifiableMap(m);
	}

	/**
	 * Map the button bit to the corresponding tooltip text.
	 */
	private static final Map<Long, String> BUTTON_TOOLTIP_MAP;
	static {
		Map<Long, String> m = new HashMap<>();
		m.put(POINTER,    "Make single or bulk selection");
		m.put(ELLIPSE,    "Create an ellipse item");
		m.put(TEXT,       "Create a text item");
		m.put(RECTANGLE,  "Create a rectangle item");
		m.put(POLYGON,    "Create a polygon item");
		m.put(LINE,       "Create a line item");
		m.put(STYLEB,     "Edit style of selected item(s)");
		m.put(DELETE,     "Delete selected item(s)");
		m.put(CENTER,     "Recenter the view at clicked location");
		m.put(UNDOZOOM,   "Undo last zoom action");
		m.put(POLYLINE,   "Create a polyline item");
		m.put(BOXZOOM,    "Rubber-band zoom to area");
		m.put(PAN,        "Pan the view by dragging");
		m.put(CONNECTOR,  "Connect two item with a connector line");
		m.put(ZOOMIN,     "Zoom in by a fixed amount");
		m.put(ZOOMOUT,    "Zoom out by a fixed amount");
		m.put(RESETZOOM,  "Restore zoom to default");
		m.put(CAMERA,     "Snapshot of the current canvas as a png image");
		m.put(PRINTER,    "Print the current canvas");
		m.put(RADARC,     "Create a radarc item");
		m.put(MAGNIFY,    "Magnify area under mouse cursor");
		m.put(INFO,       "Information about this view");
		m.put(STATUS,      "");
		BUTTON_TOOLTIP_MAP = Collections.unmodifiableMap(m);
	}

	/*
	 * Note that in the predefined sets of buttons to include on a toolbar,
	 * the order of the bits does NOT determine order of the buttons on the toolbar.
	 */
	public static final long ANNOTATIONTOOLS = POINTER | ELLIPSE | TEXT | RECTANGLE | POLYGON | LINE
			| POLYLINE | STYLEB | DELETE;

	public static final long DRAWINGTOOLS = ANNOTATIONTOOLS | RADARC;

	public static final long ZOOMTOOLS = BOXZOOM | UNDOZOOM | ZOOMIN | ZOOMOUT
			| RESETZOOM;

	public static final long PICVIEWSTOOLS = CAMERA | PRINTER;

	public static final long NAVIGATIONTOOLS = POINTER | ZOOMTOOLS | PAN | CENTER;

	public static final long PLOTTOOLS = POINTER | ZOOMTOOLS | PICVIEWSTOOLS & ~UNDOZOOM;

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
	 * If the button has no icon (e.g. {@link #STATUS}), this method returns
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

	protected static boolean hasPointerButton(long toolbarBits)   { return hasBit(POINTER, toolbarBits); }
	protected static boolean hasPanButton(long toolbarBits)       { return hasBit(PAN, toolbarBits); }
	protected static boolean hasResetZoomButton(long toolbarBits) { return hasBit(RESETZOOM, toolbarBits); }
	protected static boolean hasUndoZoomButton(long toolbarBits)  { return hasBit(UNDOZOOM, toolbarBits); }
	protected static boolean hasCenterButton(long toolbarBits)    { return hasBit(CENTER, toolbarBits); }
	protected static boolean hasPrinterButton(long toolbarBits)   { return hasBit(PRINTER, toolbarBits); }
	protected static boolean hasCameraButton(long toolbarBits)    { return hasBit(CAMERA, toolbarBits); }
	protected static boolean hasDeleteButton(long toolbarBits)    { return hasBit(DELETE, toolbarBits); }
	protected static boolean hasStyleButton(long toolbarBits)     { return hasBit(STYLEB, toolbarBits); }
	protected static boolean hasEllipseButton(long toolbarBits)   { return hasBit(ELLIPSE, toolbarBits); }
	protected static boolean hasTextButton(long toolbarBits)      { return hasBit(TEXT, toolbarBits); }
	protected static boolean hasRectangleButton(long toolbarBits) { return hasBit(RECTANGLE, toolbarBits); }
	protected static boolean hasPolygonButton(long toolbarBits)   { return hasBit(POLYGON, toolbarBits); }
	protected static boolean hasLineButton(long toolbarBits)      { return hasBit(LINE, toolbarBits); }
	protected static boolean hasPolylineButton(long toolbarBits)  { return hasBit(POLYLINE, toolbarBits); }
	protected static boolean hasRadArcButton(long toolbarBits)    { return hasBit(RADARC, toolbarBits); }
	protected static boolean hasMagnifyButton(long toolbarBits)   { return hasBit(MAGNIFY, toolbarBits); }
	protected static boolean hasConnectorButton(long toolbarBits) { return hasBit(CONNECTOR, toolbarBits); }
	protected static boolean hasStatusField(long toolbarBits)     { return hasBit(STATUS, toolbarBits); }
	protected static boolean hasZoomInButton(long toolbarBits)    { return hasBit(ZOOMIN, toolbarBits); }
	protected static boolean hasZoomOutButton(long toolbarBits)   { return hasBit(ZOOMOUT, toolbarBits); }
	protected static boolean hasInfoButton(long toolbarBits)      { return hasBit(INFO, toolbarBits); }
	protected static boolean hasBoxZoomButton(long toolbarBits)   { return hasBit(BOXZOOM, toolbarBits); }
}
