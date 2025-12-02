package edu.cnu.mdi.swing.view;


/**
 * Keys used for view-related properties in the Swing MDI framework.
 *
 * These are intentionally limited to UI / layout / styling concepts.
 * Domain-specific keys (e.g. detector crate/slot/channel) should live
 * in their own modules, not here.
 */
public interface ViewPropertyKeys {

    String BACKGROUND              = "BACKGROUND";
    String BACKGROUND_IMAGE        = "BACKGROUNDIMAGE";

    String TOP_MARGIN              = "TOPMARGIN";
    String BOTTOM_MARGIN           = "BOTTOMMARGIN";
    String LEFT_MARGIN             = "LEFTMARGIN";
    String RIGHT_MARGIN            = "RIGHTMARGIN";

    String LEFT                    = "LEFT";
    String TOP                     = "TOP";
    String WIDTH                   = "WIDTH";
    String HEIGHT                  = "HEIGHT";

    String VISIBLE                 = "VISIBLE";
    String SCROLLABLE              = "SCROLLABLE";

    String TOOLBAR                 = "TOOLBAR";
    String TOOLBAR_BITS            = "TOOLBARBITS";
    String STANDARD_VIEW_DECOR     = "STANDARDVIEWDECORATIONS";

    String CLOSABLE                = "CLOSABLE";
    String ICONIFIABLE             = "ICONIFIABLE";
    String RESIZABLE               = "RESIZABLE";
    String MAXIMIZE                = "MAXIMIZE";
    String MAXIMIZABLE             = "MAXIMIZABLE";
    String DRAGGABLE               = "DRAGGABLE";
    String ROTATABLE               = "ROTATABLE";

    String FONT                    = "FONT";
    String TITLE                   = "TITLE";
    String PROP_NAME               = "PROPNAME";

    String WORLDSYSTEM             = "WORLDSYSTEM";
    String FRACTION                = "FRACTION";

    String SPLIT_WEST_COMPONENT    = "SPLITWESTCOMPONENT";

    String SYMBOL                  = "SYMBOL";
    String SYMBOL_SIZE             = "SYMBOLSIZE";

    String TEXT_COLOR              = "TEXTCOLOR";
    String FILL_COLOR              = "FILLCOLOR";
    String LINE_COLOR              = "LINECOLOR";
    String LINE_STYLE              = "LINESTYLE";
    String LINE_WIDTH              = "LINEWIDTH";

    String USER_DATA               = "USERDATA";

    // You can add LOCKED here if it is a UI concern in mdi:
    String LOCKED                  = "LOCKED";
}
