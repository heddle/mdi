package edu.cnu.mdi.graphics.toolbar;

public class ToolBarBits {
	public static final long ELLIPSEBUTTON      = 01;
	public static final long TEXTBUTTON         = 02;
	public static final long RECTANGLEBUTTON    = 04;
	public static final long POLYGONBUTTON      = 010L;
	public static final long LINEBUTTON         = 020L;
	public static final long RANGEBUTTON        = 040L;
	public static final long DELETEBUTTON       = 0100L;
	public static final long TEXTFIELD          = 0200L;
	public static final long CENTERBUTTON       = 0400L;
	public static final long CONTROLPANELBUTTON = 01000L; // toggle control panel
	public static final long RADARCBUTTON       = 02000L;
	public static final long POLYLINEBUTTON     = 04000L;
	public static final long MAGNIFYBUTTON      = 010000L;
	public static final long NOZOOM             = 020000L;
	public static final long PANBUTTON          = 040000L;
	public static final long UNDOZOOMBUTTON     = 0100000L;

	// used to eliminate some basic buttons

	public static final long DRAWING = ELLIPSEBUTTON + TEXTBUTTON + RECTANGLEBUTTON + POLYGONBUTTON + LINEBUTTON
			+ RADARCBUTTON + POLYLINEBUTTON;

	public static final long EVERYTHING = 07777777777 & ~NOZOOM;
	public static final long STANDARD = EVERYTHING & ~CONTROLPANELBUTTON;

	public static final long NODRAWING = EVERYTHING & ~DRAWING;

	// only the text button
	public static final long TEXTDRAWING = STANDARD & ~DRAWING + TEXTBUTTON & ~RANGEBUTTON;

	// nobuttons!
	public static final long NOTHING = 01777777777777777777777L;
}
