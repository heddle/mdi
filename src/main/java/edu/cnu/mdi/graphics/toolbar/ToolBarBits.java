package edu.cnu.mdi.graphics.toolbar;

/**
 * Bit flags controlling which tools/widgets appear on a {@link BaseToolBar}.
 * <p>
 * Values are written in octal to match the legacy code style.
 * </p>
 */
public final class ToolBarBits {

    private ToolBarBits() {}

    public static final long ELLIPSEBUTTON      = 01L;
    public static final long TEXTBUTTON         = 02L;
    public static final long RECTANGLEBUTTON    = 04L;
    public static final long POLYGONBUTTON      = 010L;
    public static final long LINEBUTTON         = 020L;
    public static final long STYLEBUTTON        = 040L;
    public static final long DELETEBUTTON       = 0100L;
    public static final long STATUSFIELD        = 0200L;
    public static final long CENTERBUTTON       = 0400L;
    public static final long UNDOZOOMBUTTON     = 01000L;
    public static final long RADARCBUTTON       = 02000L;
    public static final long POLYLINEBUTTON     = 04000L;
    public static final long MAGNIFYBUTTON      = 010000L;
    public static final long BOXZOOMBUTTON      = 020000L;
    public static final long PANBUTTON          = 040000L;
    public static final long CONNECTORBUTTON    = 0100000L;


    /** "Everything" means all known bits except the NOZOOM suppressor. */
    public static final long EVERYTHING = STYLEBUTTON | ELLIPSEBUTTON | TEXTBUTTON | RECTANGLEBUTTON | POLYGONBUTTON | LINEBUTTON
            | DELETEBUTTON | STATUSFIELD | CENTERBUTTON | UNDOZOOMBUTTON | RADARCBUTTON | POLYLINEBUTTON
            | MAGNIFYBUTTON | PANBUTTON | BOXZOOMBUTTON;

    public static final long DEFAULTS = BOXZOOMBUTTON | DELETEBUTTON | CENTERBUTTON | UNDOZOOMBUTTON | PANBUTTON;

    /** All drawing-shape tools. */
    public static final long DRAWING =
            EVERYTHING & ~MAGNIFYBUTTON;

    /** Default set (same as everything in your current intent). */
    public static final long STANDARD = EVERYTHING;

    /** Everything except drawing tools. */
    public static final long NODRAWING = EVERYTHING & ~DRAWING;

    /** Only the text drawing tool (plus whatever non-drawing widgets you keep in STANDARD). */
    public static final long TEXTDRAWING = (STANDARD & ~DRAWING) | TEXTBUTTON;

    /** No tools/widgets. */
    public static final long NOTHING = 0L;
}
