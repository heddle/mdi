package edu.cnu.mdi.experimental;

import java.util.HashMap;

import edu.cnu.mdi.util.Environment;

/**
 * Bit flags controlling which pre-defined toggle buttons appear on a {@link BaseToolBar}.
 * <p>
 * Values are written in octal.
 * </p>
 */
public final class ToolBarBits {

	private ToolBarBits() {
	}

	/** Bit flags for each standard toolbar button.
	 * The status field is not button but a small
	 * mouse-over feedback area on the toolbar */
	
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
	public static final long RESETZOOMBUTTON = 00100000L;
	public static final long CAMERABUTTON    = 00200000L;
	public static final long PRINTERBUTTON   = 00400000L;
	public static final long STATUSFIELD     = 010000000L;
	
	/** Map the button to the corresponding icon image file */
	public static final HashMap<Long, String> BUTTON_ICON_MAP = new HashMap<>() {{
		put(POINTERBUTTON,    "images/svg/pointer.svg");
		put(ELLIPSEBUTTON,    "images/svg/ellipse.svg");
		put(TEXTBUTTON,       "images/svg/text.svg");
		put(RECTANGLEBUTTON,  "images/svg/rectangle.svg");
		put(POLYGONBUTTON,    "images/svg/polygon.svg");
		put(LINEBUTTON,       "images/svg/line.svg");
		put(STYLEBUTTON,      "images/svg/colorwheel.svg");
		put(DELETEBUTTON,     "images/svg/delete.svg");
		put(CENTERBUTTON,     "images/svg/center.svg");
		put(UNDOZOOMBUTTON,   "images/svg/undo_zoom.svg");
		put(POLYLINEBUTTON,   "images/svg/polyline.svg");
		put(BOXZOOMBUTTON,    "images/svg/box_zoom.svg");
		put(PANBUTTON,        "images/svg/pan.svg");
		put(CONNECTORBUTTON,  "images/svg/connector.svg");
		put(ZOOMINBUTTON,     "images/svg/zoom_in.svg");
		put(ZOOMOUTBUTTON,    "images/svg/zoom_out.svg");
		put(RESETZOOMBUTTON,  "images/svg/reset_zoom.svg");
		put(CAMERABUTTON,     "images/svg/camera.svg");
		put(PRINTERBUTTON,    "images/svg/printer.svg");
		put(RADARCBUTTON,     "images/svg/radarc.svg");
		put(MAGNIFYBUTTON,    "images/svg/magnify.svg");
		put(STATUSFIELD,      ""); // No icon for status field
	}};

	/*
	 * Note that is the predefined sets of buttons to include on a toolbar,
	 * the order of the bits does NOT determine order of the buttons on the toolbar.
	 */
	/** All standard annotation tool buttons */
	public static final long ANNOTATIONTOOLS = POINTERBUTTON | ELLIPSEBUTTON | TEXTBUTTON | RECTANGLEBUTTON | POLYGONBUTTON | LINEBUTTON
			| POLYLINEBUTTON | STYLEBUTTON | DELETEBUTTON;
	
	/** All standard drawing tool buttons */
	public static final long DRAWINGTOOLS = ANNOTATIONTOOLS | RADARCBUTTON;
	
	/** All standard zoom tool buttons */
	public static final long ZOOMTOOLS = BOXZOOMBUTTON | UNDOZOOMBUTTON | ZOOMINBUTTON | ZOOMOUTBUTTON
			| RESETZOOMBUTTON;
	
	/** All standard picture view tool buttons */
	public static final long PICVIEWSTOOLS = CAMERABUTTON | PRINTERBUTTON;

	/** All standard navigation tool buttons */
	public static final long NAVIGATIONTOOLS = POINTERBUTTON | ZOOMTOOLS | PANBUTTON | CENTERBUTTON;

	/** All standard plot tool buttons for splot panels*/
	public static final long PLOTTOOLS = POINTERBUTTON | ZOOMTOOLS | PICVIEWSTOOLS;
	
	/**
	 * Get the full resource path for the icon corresponding to the given button bit.
	 * This only works for prepefined buttons with known icons in the MDI resources path.
	 * @param buttonBit the button bit flag.
	 * @return the full resource path for the icon.
	 */
	public static final String getMDIIconResourcePath(long buttonBit) {
		return Environment.MDI_RESOURCE_PATH + BUTTON_ICON_MAP.get(buttonBit);
	}
	
	/**
	 * Determine if the given button bit is included in the given toolbar bits.
	 * @param buttonBit the button bit flag.
	 * @param toolbarBits the toolbar bits flag.
	 * @return true if the button is included in the toolbar bits.
	 */
	private static boolean addButton(long buttonBit, long toolbarBits) {
		return (toolbarBits & buttonBit) == buttonBit;
	}
	
	/*
	 * Methods to check for each button in the toolbar bits
	 */
	
	protected static boolean hasPointerButton(long toolbarBits) {
		return addButton(POINTERBUTTON, toolbarBits);
	}

	protected static boolean hasPanButton(long toolbarBits) {
		return addButton(PANBUTTON, toolbarBits);
	}
	
	protected static boolean hasResetZoomButton(long toolbarBits) {
		return addButton(RESETZOOMBUTTON, toolbarBits);
	}
	
	protected static boolean hasUndoZoomButton(long toolbarBits) {
		return addButton(UNDOZOOMBUTTON, toolbarBits);
	}
	
	protected static boolean hasCenterButton(long toolbarBits) {
		return addButton(CENTERBUTTON, toolbarBits);
	}
	
	protected static boolean hasPrinterButton(long toolbarBits) {
		return addButton(PRINTERBUTTON, toolbarBits);
	}
	
	protected static boolean hasCameraButton(long toolbarBits) {
		return addButton(CAMERABUTTON, toolbarBits);
	}
	
	protected static boolean hasDeleteButton(long toolbarBits) {
		return addButton(DELETEBUTTON, toolbarBits);
	}
	
	protected static boolean hasStyleButton(long toolbarBits) {
		return addButton(STYLEBUTTON, toolbarBits);
	}
	
	protected static boolean hasEllipseButton(long toolbarBits) {
		return addButton(ELLIPSEBUTTON, toolbarBits);
	}
	
	protected static boolean hasTextButton(long toolbarBits) {
		return addButton(TEXTBUTTON, toolbarBits);
	}
	protected static boolean hasRectangleButton(long toolbarBits) {
		return addButton(RECTANGLEBUTTON, toolbarBits);
	}	
	
	protected static boolean hasPolygonButton(long toolbarBits) {
		return addButton(POLYGONBUTTON, toolbarBits);
	}
	protected static boolean hasLineButton(long toolbarBits) {
		return addButton(LINEBUTTON, toolbarBits);
	}
	protected static boolean hasPolylineButton(long toolbarBits) {
		return addButton(POLYLINEBUTTON, toolbarBits);
	}
	
	protected static boolean hasRadArcButton(long toolbarBits) {
		return addButton(RADARCBUTTON, toolbarBits);
	}
	
	protected static boolean hasMagnifyButton(long toolbarBits) {
		return addButton(MAGNIFYBUTTON, toolbarBits);
	}
	
	protected static boolean hasConnectorButton(long toolbarBits) {
		return addButton(CONNECTORBUTTON, toolbarBits);
	}
	
	protected static boolean hasStatusField(long toolbarBits) {
		return addButton(STATUSFIELD, toolbarBits);
	}
	
	protected static boolean hasZoomInButton(long toolbarBits) {
		return addButton(ZOOMINBUTTON, toolbarBits);
	}

	protected static boolean hasZoomOutButton(long toolbarBits) {
		return addButton(ZOOMOUTBUTTON, toolbarBits);
	}
	
	protected static boolean hasBoxZoomButton(long toolbarBits) {
		return addButton(BOXZOOMBUTTON, toolbarBits);
	}
	
	
}