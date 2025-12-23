package edu.cnu.mdi.graphics.toolbar;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JTextField;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.util.Bits;

/**
 * Standard toolbar for an {@link IContainer} that manages a mutually exclusive
 * set of interaction tools (pan, pointer, zoom-box, magnify, draw shapes, etc.)
 * and routes mouse events from the container's canvas to the currently active tool.
 * <p>
 * Popup triggers are handled centrally on both press and release (platform dependent)
 * using {@link PopupTriggerSupport}. Popup events are not forwarded to tools.
 * </p>
 *
 * @author heddle
 */
@SuppressWarnings("serial")
public class BaseToolBar extends CommonToolBar implements MouseListener, MouseMotionListener, IContainerToolBar {

    /** The container being controlled by this toolbar. */
    private final IContainer container;

    /** Context passed to all tools. */
    private final ToolContext toolContext;

    /** Controller that registers tools and tracks the active tool. */
    private final ToolController toolController;

    /** Optional status text field shown on the toolbar. */
    private JTextField statusLine;

    /** Temporary override tool used for gestures like middle-click box zoom. */
    private ITool temporarilyOverriddenTool;

    /** Tool id -> toggle button map (keeps UI synced with controller). */
    private final Map<String, ToolToggleButton> toolButtons = new LinkedHashMap<>();

    /** Convenience reference for delete button (so we can enable/disable it). */
    private ToolActionButton deleteButton;

    /** Convenience reference for style button (so we can enable/disable it). */
    private ToolActionButton styleButton;

    /**
     * Create a toolbar with the full default tool set.
     *
     * @param container the container this toolbar controls (must not be null).
     */
    public BaseToolBar(IContainer container) {
        this(container, ToolBarBits.EVERYTHING);
    }

    /**
     * Create a toolbar using a bit mask to control which tools/widgets are added.
     *
     * @param container the container this toolbar controls (must not be null).
     * @param bits      bit mask controlling which tools/widgets are added.
     */
    public BaseToolBar(IContainer container, long bits) {
        super("ToolBar");
        this.container = Objects.requireNonNull(container, "container must not be null");
        this.container.setToolBar(this);

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        this.toolContext = new ToolContext(this.container, this);
        this.toolController = new ToolController(toolContext);

        // Keep Swing toggle selection synchronized with ToolController selection.
        hookControllerSelectionToButtons();

        buildToolsAndButtons(bits);
        attachMouseRouting();

        setBorder(BorderFactory.createEtchedBorder());
        setFloatable(false);

        // Ensure a default tool is active at startup.
        resetDefaultSelection();
    }

    /**
	 * Get the tool controller for this toolbar.
	 * @return the tool controller.
	 */
    public ToolController getToolController() {
        return toolController;
    }

    /**
	 * Get the tool context passed to all tools.
	 *
	 * @return the tool context.
	 */
    public ToolContext getToolContext() {
        return toolContext;
    }

    /**
     * Attach mouse listeners to the container canvas so this toolbar can route events.
     */
    private void attachMouseRouting() {
        Component c = container.getComponent();
        if (c != null) {
            c.addMouseListener(this);
            c.addMouseMotionListener(this);
        }
    }

    /**
     * Bridge ToolController selection changes -> Swing toggle selected state.
     */
    private void hookControllerSelectionToButtons() {
        toolController.addSelectionListener(activeTool -> {
            for (ToolToggleButton b : toolButtons.values()) {
                boolean shouldBeSelected =
                        (activeTool != null) && b.toolId().equals(activeTool.id());

                if (b.isSelected() != shouldBeSelected) {
                    b.setSelectedProgrammatically(shouldBeSelected);
                }
            }
            updateButtonState();
        });
    }

    /**
	 * Add a spacer of the given width to the toolbar.
	 * @param width the empty space width in pixels.
	 */
    public void addToolBarSpacer(int width) {
		add(Box.createHorizontalStrut(width));
	}

    /**
     * Build/register tools and create their buttons.
     */
    protected void buildToolsAndButtons(long bits) {

        ITool pointer   = createPointerTool(); // REQUIRED default tool
        ITool boxZoom   = (!Bits.check(bits, ToolBarBits.NOZOOM)) ? createBoxZoomTool() : null;

        ITool pan       = Bits.check(bits, ToolBarBits.PANBUTTON)      ? createPanTool()      : null;
        ITool magnify   = Bits.check(bits, ToolBarBits.MAGNIFYBUTTON)  ? createMagnifyTool()  : null;
        ITool ellipse   = Bits.check(bits, ToolBarBits.ELLIPSEBUTTON)  ? new EllipseTool()    : null;
        ITool center    = Bits.check(bits, ToolBarBits.CENTERBUTTON)   ? createCenterTool()   : null;
        ITool line      = Bits.check(bits, ToolBarBits.LINEBUTTON)     ? new LineTool()       : null;
        ITool connector = Bits.check(bits, ToolBarBits.CONNECTORBUTTON)? new ConnectorTool()  : null;
        ITool polygon   = Bits.check(bits, ToolBarBits.POLYGONBUTTON)  ? new PolygonTool()    : null;
        ITool polyline  = Bits.check(bits, ToolBarBits.POLYLINEBUTTON) ? new PolylineTool()   : null;
        ITool rectangle = Bits.check(bits, ToolBarBits.RECTANGLEBUTTON)? new RectangleTool()  : null;
        ITool radArc    = Bits.check(bits, ToolBarBits.RADARCBUTTON)   ? new RadArcTool()     : null;
        ITool text      = Bits.check(bits, ToolBarBits.TEXTBUTTON)     ? new TextTool() : null;

        // Register tools
        register(pointer);
        register(boxZoom);
        register(pan);
        register(magnify);
        register(center);
        register(line);
        register(rectangle);
        register(ellipse);
        register(polygon);
        register(polyline);
        register(radArc);
        register(text);
        register(connector);

        // Default tool: pointer.
        toolController.setDefaultTool(pointer.id());

        // Buttons (order is respected)
        addToolButton(pointer, "images/svg/pointer.svg", pointer.toolTip());

        if (boxZoom != null) {
            addToolButton(boxZoom, "images/svg/box_zoom.svg", boxZoom.toolTip());
        }
        if (pan != null) {
            addToolButton(pan, "images/svg/pan.svg", pan.toolTip());
        }
        if (magnify != null) {
            addToolButton(magnify, "images/svg/magnify.svg", magnify.toolTip());
        }
         if (center != null) {
            addToolButton(center, "images/svg/center.svg", center.toolTip());
        }
        if (line != null) {
            addToolButton(line, "images/svg/line.svg", line.toolTip());
        }
     	if (rectangle != null) {
    	    addToolButton(rectangle, "images/svg/rectangle.svg", rectangle.toolTip());
    	}
        if (ellipse != null) {
 			addToolButton(ellipse, "images/svg/ellipse.svg", ellipse.toolTip());
 		}
        if (polygon != null) {
            addToolButton(polygon, "images/svg/polygon.svg", polygon.toolTip());
        }
    	if (polyline != null) {
			addToolButton(polyline, "images/svg/polyline.svg", polyline.toolTip());
		}
		if (radArc != null) {
			addToolButton(radArc, "images/svg/radarc.svg", radArc.toolTip());
		}
		if (connector != null) {
			addToolButton(connector, "images/svg/connect.svg", connector.toolTip());
		}
		if (text != null) {
			addToolButton(text, "images/svg/text.svg", text.toolTip());
		}
		if (Bits.check(bits, ToolBarBits.UNDOZOOMBUTTON)) {
		    addActionButton(new UndoZoomButton(toolContext));
		}


		addActionButton(new WorldButton(toolContext));   // if you have a bit for it, gate it
		addActionButton(new ZoomInButton(toolContext));
		addActionButton(new ZoomOutButton(toolContext));
		addActionButton(new RefreshButton(toolContext));

		if (Bits.check(bits, ToolBarBits.STYLEBUTTON)) {
			styleButton = new EditStyleButton(toolContext);
			styleButton.setEnabled(false); // initially disabled
		    addActionButton(styleButton);
		}

		if (Bits.check(bits, ToolBarBits.DELETEBUTTON)) {
			//common practice to add some space before delete
			addToolBarSpacer(8);
			deleteButton = new DeleteButton(toolContext);
			deleteButton.setEnabled(false); // initially disabled
		    addActionButton(deleteButton);
		}

        if (Bits.check(bits, ToolBarBits.STATUSFIELD)) {
            createStatusTextField();
            add(statusLine);
        }
    }

    protected void addActionButton(ToolActionButton b) {
        if (b != null) {
            add(b, false);
        }
    }

	/**
	 * Register a tool with the controller (if non-null).
	 */

    private void register(ITool tool) {
        if (tool != null) {
            toolController.register(tool);
        }
    }
    /**
     * Create and add a toggle button for a tool and register it for UI sync.
     *
     * @return the created button, or null.
     */
    protected ToolToggleButton addToolButton(ITool tool, String iconFile, String tooltip) {
        if (tool == null) {
            return null;
        }

        ToolToggleButton b = new ToolToggleButton(toolController, tool.id(), iconFile, tooltip, 24, 24);
        add(b, true);

        toolButtons.put(tool.id(), b);

        // Make the UI default toggle match the controller default tool.
        if ("pointer".equals(tool.id())) {
            setDefaultToggleButton(b);
        }

        return b;
    }

    /**
	 * Create the status text field shown on the toolbar.
	 */
    protected void createStatusTextField() {
        statusLine = new JTextField(" ");
        statusLine.setFont(Fonts.tweenFont);
        statusLine.setEditable(false);
        statusLine.setBackground(Color.black);
        statusLine.setForeground(Color.cyan);

        FontMetrics fm = getFontMetrics(statusLine.getFont());
        Dimension d = statusLine.getPreferredSize();
        d.width = fm.stringWidth(" ( 9999.99999 , 9999.99999 ) XXXXXXXXXXX");
        statusLine.setPreferredSize(d);
        statusLine.setMaximumSize(d);
    }

    /**
	 * Set the text shown in the toolbar status text field (if any).
	 *
	 * @param text the text to show (null shows empty string).
	 */
    @Override
	public void setText(String text) {
        if (statusLine != null) {
            statusLine.setText((text == null) ? "" : text);
        }
    }

    public JTextField getTextField() {
        return statusLine;
    }

    public ITool getActiveTool() {
        return toolController.getActive();
    }

    @Override
    public void resetDefaultSelection() {
        super.resetDefaultSelection();
        toolController.resetToDefault();
    }

    protected boolean isCanvasEnabled() {
        Component c = container.getComponent();
        return (c != null) && c.isEnabled();
    }

    private ITool activeToolOrNull() {
        return (temporarilyOverriddenTool != null) ? temporarilyOverriddenTool : toolController.getActive();
    }

    // ============================================================
    // Mouse routing with popup integration
    // ============================================================

    @Override
    public void mouseClicked(MouseEvent e) {
        if (!isCanvasEnabled()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        // Popups are handled on press/release; if one slips through, ignore.
        if (PopupTriggerSupport.isPopupTrigger(e)) {
            return;
        }

        ITool tool = activeToolOrNull();
        if (tool == null) {
            return;
        }

        boolean mb1 = (e.getButton() == MouseEvent.BUTTON1) && !e.isControlDown();
        if (!mb1) {
            return;
        }

        if (e.getClickCount() == 1) {
            tool.mouseClicked(toolContext, e);
        } else {
            tool.mouseDoubleClicked(toolContext, e);
        }
    }


	@Override
	public void mouseExited(MouseEvent e) {
		ITool tool = activeToolOrNull();
		if (tool != null) {
			tool.mouseExited(toolContext, e);
		}
		Component canvas = container.getComponent();
		if (canvas != null) {
			canvas.setCursor(Cursor.getDefaultCursor());
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	    ITool tool = activeToolOrNull();
	    if (tool != null) {
	        toolController.applyCursorNow();
	        tool.mouseEntered(toolContext, e);
	    }
	    container.locationUpdate(e, false);
	}

	@Override
	public void mousePressed(MouseEvent e) {
	    if (!isCanvasEnabled()) {
	        return;
	    }

	    if (PopupTriggerSupport.isPopupTrigger(e)) {
	        PopupTriggerSupport.showPopup(container, e);
	        return;
	    }

	    if (e.getButton() == MouseEvent.BUTTON2) {
	        ITool boxZoom = toolController.get("boxZoom");
	        if (boxZoom != null) {
	            temporarilyOverriddenTool = boxZoom;
	        }
	    }

	    ITool tool = activeToolOrNull();
	    if (tool != null) {
	        tool.mousePressed(toolContext, e);
	    }

	    // Update immediately so click location shows up right away
	    container.locationUpdate(e, false);
	}

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!isCanvasEnabled()) {
            temporarilyOverriddenTool = null;
            return;
        }

        // Popup trigger can be on release (macOS).
        if (PopupTriggerSupport.isPopupTrigger(e)) {
            temporarilyOverriddenTool = null;
            PopupTriggerSupport.showPopup(container, e);
            return;
        }

        ITool tool = activeToolOrNull();
        if (tool != null) {
            tool.mouseReleased(toolContext, e);
        }

        temporarilyOverriddenTool = null;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!isCanvasEnabled()) {
            return;
        }

        ITool tool = activeToolOrNull();
        if (tool != null) {
            tool.mouseDragged(toolContext, e);
        }

        // KEEP FEEDBACK ALIVE while dragging
        container.locationUpdate(e, true);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        ITool tool = activeToolOrNull();
        if (tool != null) {
            tool.mouseMoved(toolContext, e);
        }

        // KEEP FEEDBACK ALIVE while moving
        container.locationUpdate(e, false);
    }

    /**
     * Update enable/disable state for toolbar widgets whose availability depends on
     * container state (typically selection).
     */
    @Override
	public void updateButtonState() {
        boolean anySelected = container.anySelectedItems();
        if (deleteButton != null) {
            deleteButton.setEnabled(anySelected);
        }
        if (styleButton != null) {
			styleButton.setEnabled(anySelected);
		}
    }


    // ============================================================
    // Factory hooks for creating tools
    // ============================================================

	protected ITool createPointerTool() {
		return new PointerTool();
	}

    protected ITool createBoxZoomTool() {
    	return new BoxZoomTool();
    }

    protected ITool createCenterTool() {
    	return new CenterTool();
    }

    protected ITool createPanTool() {
        return new PanTool(new PreviewImagePanBehavior());
    }

	protected ITool createMagnifyTool() {
		return new MagnifyTool();
	}

	/**
	 * Register a tool with this toolbar's controller.
	 *
	 * @param tool the tool to register (ignored if null)
	 * @return this toolbar (for chaining)
	 */
	public BaseToolBar addTool(ITool tool) {
	    if (tool != null) {
	        toolController.register(tool);
	    }
	    return this;
	}

	@Override
	public ToolToggleButton addToolToggle(ITool tool, String iconFile, String tooltip) {
	    return addTool(tool, iconFile, tooltip); // uses your existing convenience
	}

	@Override
	public IContainerToolBar addOneShot(ToolActionButton button) {
	    addAction(button); // uses your existing convenience
	    return this;
	}

	@Override
	public IContainerToolBar spacer(int px) {
	    addToolBarSpacer(px);
	    return this;
	}


	/**
	 * Register a tool and add a toggle button for it.
	 * <p>
	 * This is the main convenience method for applications extending the toolbar.
	 * </p>
	 *
	 * @param tool     the tool to register (ignored if null)
	 * @param iconFile icon resource path
	 * @param tooltip  tooltip text
	 * @return the created toggle button, or null if tool was null
	 */
	public ToolToggleButton addTool(ITool tool, String iconFile, String tooltip) {
	    if (tool == null) {
	        return null;
	    }
	    toolController.register(tool);
	    return addToolButton(tool, iconFile, tooltip); // your existing method
	}

	/**
	 * Add a one-shot action button.
	 *
	 * @param button the button (ignored if null)
	 * @return this toolbar (for chaining)
	 */
	public BaseToolBar addAction(ToolActionButton button) {
		button.setToolContext(toolContext);
	    addActionButton(button); // your existing method
	    return this;
	}

}
