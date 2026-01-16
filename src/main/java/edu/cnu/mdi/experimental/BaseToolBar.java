package edu.cnu.mdi.experimental;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Objects;
import java.util.function.BiConsumer;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import edu.cnu.mdi.graphics.ImageManager;
import edu.cnu.mdi.graphics.rubberband.Rubberband;
import edu.cnu.mdi.graphics.rubberband.Rubberband.Policy;
import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * Default MDI toolbar implementation for a canvas.
 * <p>
 * This toolbar supports a mix of:
 * </p>
 * <ul>
 *   <li><b>Mutually exclusive toggle tools</b> (pointer, pan, magnify, box zoom, etc.)</li>
 *   <li><b>One-shot actions</b> (zoom in/out, reset zoom, delete, style, camera, printer)</li>
 *   <li><b>Status field</b> (optional)</li>
 * </ul>
 *
 * <h2>Extensibility</h2>
 * <p>
 * Applications can add their own buttons after construction using the id-aware
 * methods from {@link AToolBar}:
 * </p>
 *
 * <pre>{@code
 * toolbar.addToggle("myTool", new JToggleButton("..."));
 * toolbar.addButton("myAction", new JButton("..."));
 *
 * toolbar.setButtonEnabled("myTool", false);
 * }</pre>
 *
 * <h2>Listener swapping</h2>
 * <p>
 * The active toggle tool is the only one that receives mouse events from the canvas.
 * This class swaps canvas mouse listeners when the active tool changes. It tracks
 * the currently installed listeners directly (O(1)) rather than scanning all toggles.
 * </p>
 *
 * @author heddle
 */
@SuppressWarnings("serial")
public class BaseToolBar extends AToolBar {

	/** Default minimum size in pixels for rubberbanded shapes. */
	public static final int DEFAULT_MIN_SIZE_PX = 4;

	/** Default button size. */
	public static final Dimension DEFAULT_BUTTON_SIZE = new Dimension(24, 24);

	/** Default icon size (w and h). */
	public static final int DEFAULT_ICON_SIZE = 20;

	/** Default magnify viewport size. */
	public static final Dimension DEFAULT_MAGNIFY_SIZE = new Dimension(100, 100);

	/** The canvas this toolbar is associated with. */
	private final Component canvas;

	/** Bits controlling which predefined buttons are added. */
	private final long bits;

	/** Handles the gestures and actions of the tools. */
	protected final IToolHandler handler;

	/** Policy to use for box zoom. */
	protected final Rubberband.Policy boxZoomPolicy;

	/** Policy to use for pointer tool. */
	protected final Rubberband.Policy pointerPolicy;

	// --- Active canvas listener tracking (O(1) swap) ---
	private MouseListener activeMouseListener;
	private MouseMotionListener activeMouseMotionListener;

	/**
	 * Creates a new horizontal toolbar associated with a canvas.
	 *
	 * @param canvas  the canvas component this toolbar is associated with
	 * @param handler the tool handler to notify of tool gestures
	 * @param bits    controls which predefined buttons are added to the toolbar.
	 *                See {@link ToolBits} for details.
	 */
	public BaseToolBar(Component canvas, IToolHandler handler, long bits) {
		this(canvas, handler, bits, HORIZONTAL, Policy.RECTANGLE, Policy.RECTANGLE_PRESERVE_ASPECT);
	}

	/**
	 * Creates a new horizontal toolbar associated with a canvas, specifying pointer and
	 * box-zoom rubberband policies.
	 *
	 * @param canvas         the canvas component this toolbar is associated with
	 * @param handler        the tool handler to notify of tool gestures
	 * @param bits           controls which predefined buttons are added to the toolbar
	 * @param pointerPolicy  the rubberband policy to use for the pointer tool
	 * @param boxZoomPolicy  the rubberband policy to use for the box zoom tool
	 */
	public BaseToolBar(Component canvas, IToolHandler handler, long bits,
			Rubberband.Policy pointerPolicy, Rubberband.Policy boxZoomPolicy) {
		this(canvas, handler, bits, HORIZONTAL, pointerPolicy, boxZoomPolicy);
	}

	/**
	 * Creates a new toolbar associated with a canvas.
	 *
	 * @param canvas         the canvas component this toolbar is associated with
	 * @param handler        the tool handler to notify of tool gestures
	 * @param bits           controls which predefined buttons are added to the toolbar
	 * @param orientation    toolbar orientation ({@link #HORIZONTAL} or {@link #VERTICAL})
	 * @param pointerPolicy  the rubberband policy to use for the pointer tool
	 * @param boxZoomPolicy  the rubberband policy to use for the box zoom tool
	 */
	public BaseToolBar(Component canvas, IToolHandler handler, long bits, int orientation,
			Rubberband.Policy pointerPolicy, Rubberband.Policy boxZoomPolicy) {

		super(orientation);

		this.canvas = Objects.requireNonNull(canvas, "Canvas component cannot be null");
		this.handler = Objects.requireNonNull(handler, "Tool handler cannot be null");

		this.bits = bits;
		this.pointerPolicy = Objects.requireNonNull(pointerPolicy, "pointerPolicy cannot be null");
		this.boxZoomPolicy = Objects.requireNonNull(boxZoomPolicy, "boxZoomPolicy cannot be null");

		addPredefinedButtons();
	}

	// ------------------------------------------------------------------------
	// Small helpers (standardized add/configure, factories)
	// ------------------------------------------------------------------------

	/** Configure and add a standard toggle tool. */
	private <T extends JToggleButton> T addStdToggle(long bit, T b) {
		configureButton(b, bit);
		addToggle(ToolBits.getId(bit), b);
		return b;
	}

	/** Configure and add a standard one-shot button. */
	private <T extends JButton> T addStdButton(long bit, T b) {
		configureButton(b, bit);
		addButton(ToolBits.getId(bit), b);
		return b;
	}

	/**
	 * Create and add a one-shot action button.
	 *
	 * @param bit    predefined tool bit
	 * @param action action to execute when pressed
	 * @return the created button
	 */
	private JButton oneShot(long bit, Runnable action) {
		Objects.requireNonNull(action, "action");
		JButton b = new AOneShotButton(canvas, this) {
			@Override
			public void performAction() {
				action.run();
			}
		};
		return addStdButton(bit, b);
	}

	/**
	 * Create and add a rubberband tool.
	 *
	 * @param bit      predefined tool bit
	 * @param policy   rubberband policy
	 * @param callback callback invoked on completed rubberband gesture
	 * @return the created toggle tool
	 */
	private JToggleButton rubberbandTool(long bit, Rubberband.Policy policy,
			BiConsumer<Rectangle, Point[]> callback) {

		Objects.requireNonNull(policy, "policy");
		Objects.requireNonNull(callback, "callback");

		JToggleButton b = new ARubberbandButton(canvas, this, policy, DEFAULT_MIN_SIZE_PX) {
			@Override
			public void rubberbanding(Rectangle bounds, Point[] vertices) {
				callback.accept(bounds, vertices);
			}
		};
		return addStdToggle(bit, b);
	}

	// ------------------------------------------------------------------------
	// Predefined buttons
	// ------------------------------------------------------------------------

	/**
	 * Adds predefined buttons to the toolbar based on the provided bits.
	 * The order is based on common usage patterns.
	 */
	private void addPredefinedButtons() {

		// Pointer tool (selection + move + rubberband multi-select)
		if (ToolBits.hasPointerButton(bits)) {
			JToggleButton pointer = new ASelectionRubberbandButton(canvas, this, pointerPolicy, DEFAULT_MIN_SIZE_PX) {

				@Override
				public void rubberbanding(Rectangle bounds, Point[] vertices) {
					handler.pointerRubberbanding(BaseToolBar.this, canvas, bounds);
				}

				@Override
				protected Object hitTest(Point p) {
					return handler.hitTest(BaseToolBar.this, canvas, p);
				}

				@Override
				protected void clickObject(Object obj, MouseEvent e) {
					handler.pointerClick(BaseToolBar.this, canvas, e.getPoint(), obj, e);
				}

				@Override
				protected void doubleClickObject(Object obj, MouseEvent e) {
					handler.pointerDoubleClick(BaseToolBar.this, canvas, e.getPoint(), obj, e);
				}
			};

			addStdToggle(ToolBits.POINTERBUTTON, pointer);
			setDefaultToggleButton(pointer); // pointer is the default tool if present
		}

		// Box zoom tool (toggle)
		if (ToolBits.hasBoxZoomButton(bits)) {
			rubberbandTool(ToolBits.BOXZOOMBUTTON, boxZoomPolicy,
					(bounds, vertices) -> handler.boxZoomRubberbanding(this, canvas, bounds));
		}

		// Zoom in/out/undo/reset (one-shot)
		if (ToolBits.hasZoomInButton(bits)) {
			oneShot(ToolBits.ZOOMINBUTTON, () -> handler.zoomIn(this, canvas));
		}
		if (ToolBits.hasZoomOutButton(bits)) {
			oneShot(ToolBits.ZOOMOUTBUTTON, () -> handler.zoomOut(this, canvas));
		}
		if (ToolBits.hasUndoZoomButton(bits)) {
			oneShot(ToolBits.UNDOZOOMBUTTON, () -> handler.undoZoom(this, canvas));
		}
		if (ToolBits.hasResetZoomButton(bits)) {
			oneShot(ToolBits.RESETZOOMBUTTON, () -> handler.resetZoom(this, canvas));
		}

		// Pan tool (toggle + drag)
		if (ToolBits.hasPanButton(bits)) {
			JToggleButton pan = new ADragButton(canvas, this) {
				@Override
				public void startDrag(Point start) {
					handler.panStartDrag(BaseToolBar.this, canvas, start);
				}

				@Override
				public void updateDrag(Point start, Point previous, Point current) {
					handler.panUpdateDrag(BaseToolBar.this, canvas, start, previous, current);
				}

				@Override
				public void doneDrag(Point start, Point end) {
					handler.panDoneDrag(BaseToolBar.this, canvas, start, end);
				}
			};
			addStdToggle(ToolBits.PANBUTTON, pan);
		}

		// Magnify tool (toggle + move tracking)
		if (ToolBits.hasMagnifyButton(bits)) {
			JToggleButton magnify = new AMoveButton(canvas, this, DEFAULT_MAGNIFY_SIZE) {
				@Override
				public void startMove(Point start, MouseEvent e) {
					handler.magnifyStartMove(BaseToolBar.this, canvas, start, e);
				}

				@Override
				public void updateMove(Point start, Point p, MouseEvent e) {
					handler.magnifyUpdateMove(BaseToolBar.this, canvas, start, p, e);
				}

				@Override
				public void doneMove(Point start, Point p, MouseEvent e) {
					handler.magnifyDoneMove(BaseToolBar.this, canvas, start, p, e);
				}
			};
			addStdToggle(ToolBits.MAGNIFYBUTTON, magnify);
		}

		// Center tool (toggle + single click)
		if (ToolBits.hasCenterButton(bits)) {
			JToggleButton center = new ASingleClickButton(canvas, this) {
				@Override
				public void canvasClick(MouseEvent e) {
					handler.recenter(BaseToolBar.this, canvas, e.getPoint());
				}
			};
			addStdToggle(ToolBits.CENTERBUTTON, center);
		}

		// Drawing tools (rubberband)
		if (ToolBits.hasLineButton(bits)) {
			rubberbandTool(ToolBits.LINEBUTTON, Policy.LINE, (bounds, vertices) -> resetDefaultToggleButton());
		}
		if (ToolBits.hasRectangleButton(bits)) {
			rubberbandTool(ToolBits.RECTANGLEBUTTON, Policy.RECTANGLE, (bounds, vertices) -> resetDefaultToggleButton());
		}
		if (ToolBits.hasEllipseButton(bits)) {
			rubberbandTool(ToolBits.ELLIPSEBUTTON, Policy.OVAL, (bounds, vertices) -> resetDefaultToggleButton());
		}
		if (ToolBits.hasRadArcButton(bits)) {
			rubberbandTool(ToolBits.RADARCBUTTON, Policy.RADARC, (bounds, vertices) -> resetDefaultToggleButton());
		}
		if (ToolBits.hasPolygonButton(bits)) {
			rubberbandTool(ToolBits.POLYGONBUTTON, Policy.POLYGON, (bounds, vertices) -> resetDefaultToggleButton());
		}
		if (ToolBits.hasPolylineButton(bits)) {
			rubberbandTool(ToolBits.POLYLINEBUTTON, Policy.POLYLINE, (bounds, vertices) -> resetDefaultToggleButton());
		}

		// Text tool (toggle + single click)
		if (ToolBits.hasTextButton(bits)) {
			JToggleButton text = new ASingleClickButton(canvas, this) {
				@Override
				public void canvasClick(MouseEvent e) {
					// create text item at click location
					resetDefaultToggleButton();
				}
			};
			addStdToggle(ToolBits.TEXTBUTTON, text);
		}

		// Connector tool (rubberband line + point-approval hook)
		if (ToolBits.hasConnectorButton(bits)) {
			JToggleButton connector = new ARubberbandButton(canvas, this, Policy.LINE, DEFAULT_MIN_SIZE_PX) {

				@Override
				public void rubberbanding(Rectangle bounds, Point[] vertices) {
					// create connector item
					resetDefaultToggleButton();
				}

				@Override
				public boolean approvePoint(Point p) {
					return true;
				}
			};
			addStdToggle(ToolBits.CONNECTORBUTTON, connector);
		}

		// Style + Delete + Camera + Printer (one-shot)
		if (ToolBits.hasStyleButton(bits)) {
			oneShot(ToolBits.STYLEBUTTON, () -> handler.styleEdit(this, canvas));
		}
		if (ToolBits.hasDeleteButton(bits)) {
			oneShot(ToolBits.DELETEBUTTON, () -> handler.delete(this, canvas));
		}
		if (ToolBits.hasCameraButton(bits)) {
			oneShot(ToolBits.CAMERABUTTON, () -> handler.captureImage(this, canvas));
		}
		if (ToolBits.hasPrinterButton(bits)) {
			oneShot(ToolBits.PRINTERBUTTON, () -> handler.print(this, canvas));
		}

		// Status field (optional)
		if (ToolBits.hasStatusField(bits)) {
			statusField = createStatusTextField();
			addSeparator();

			if (getOrientation() == HORIZONTAL) {
				add(Box.createHorizontalGlue());
				statusField.setMaximumSize(new Dimension(Integer.MAX_VALUE, statusField.getPreferredSize().height));
			} else {
				add(Box.createVerticalGlue());
				statusField.setMaximumSize(new Dimension(statusField.getPreferredSize().width, Integer.MAX_VALUE));
			}

			add(statusField);
		}
	}

	// ------------------------------------------------------------------------
	// Button configuration
	// ------------------------------------------------------------------------

	/**
	 * Configure a standard toolbar button based on the provided bit.
	 * <p>
	 * Sets tooltip, icon (if defined), and standard size.
	 * </p>
	 *
	 * @param button button to configure
	 * @param bit    predefined bit describing the button
	 */
	private void configureButton(AbstractButton button, long bit) {
		Objects.requireNonNull(button, "button");

		String path = ToolBits.getResourcePath(bit);
		String tip = ToolBits.getToolTip(bit);

		button.setFocusPainted(false);
		button.setToolTipText(tip);

		// Only attempt to load when there is a real resource path.
		if (path != null && !path.isBlank()) {
			Icon icon = ImageManager.getInstance().loadUiIcon(path, DEFAULT_ICON_SIZE, DEFAULT_ICON_SIZE);
			if (icon != null) {
				button.setIcon(icon);
			} else {
				button.setText("?");
			}
		}

		button.setPreferredSize(DEFAULT_BUTTON_SIZE);
		button.setMinimumSize(DEFAULT_BUTTON_SIZE);
		button.setMaximumSize(DEFAULT_BUTTON_SIZE);
	}

	// ------------------------------------------------------------------------
	// Tool activation (listener swapping)
	// ------------------------------------------------------------------------

	@Override
	protected void activeToggleButtonChanged(JToggleButton newlyActive) {

		// Remove the previously installed listeners (if any).
		if (activeMouseListener != null) {
			canvas.removeMouseListener(activeMouseListener);
			activeMouseListener = null;
		}
		if (activeMouseMotionListener != null) {
			canvas.removeMouseMotionListener(activeMouseMotionListener);
			activeMouseMotionListener = null;
		}

		// Install listeners from the new active tool (if it implements them).
		if (newlyActive instanceof MouseListener ml) {
			activeMouseListener = ml;
			canvas.addMouseListener(ml);
		}
		if (newlyActive instanceof MouseMotionListener mml) {
			activeMouseMotionListener = mml;
			canvas.addMouseMotionListener(mml);
		}
	}

	// ------------------------------------------------------------------------
	// Convenience API for applications
	// ------------------------------------------------------------------------

	/**
	 * Check if the tool with the given id is currently active.
	 *
	 * @param toolId the id of the tool to check (should correspond to a toggle)
	 * @return true if the tool is active, false otherwise
	 */
	public boolean isToolActive(String toolId) {
		JToggleButton b = getButton(toolId, JToggleButton.class);
		return b != null && b.isSelected();
	}

	/**
	 * Check if the pointer tool is currently active.
	 *
	 * @return true if the pointer tool is active, false otherwise
	 */
	public boolean isPointerActive() {
		return isToolActive(ToolBits.getId(ToolBits.POINTERBUTTON));
	}

	/**
	 * Enable or disable tools/buttons by id.
	 * <p>
	 * This works for predefined tools (see {@link ToolBits#getId(long)}) and
	 * application-added tools registered via {@link #addToggle(String, JToggleButton)}
	 * or {@link #addButton(String, JButton)}.
	 * </p>
	 *
	 * @param enabled new enabled state
	 * @param ids     tool ids to update (null/blank entries are ignored)
	 */
	public void setToolsEnabled(boolean enabled, String... ids) {
		if (ids == null) {
			return;
		}
		for (String id : ids) {
			if (id == null || id.isBlank()) {
				continue;
			}
			setButtonEnabled(id, enabled);
		}
	}

	// ------------------------------------------------------------------------
	// Status field
	// ------------------------------------------------------------------------

	/**
	 * Create the status text field shown on the toolbar.
	 * <p>
	 * When enabled, it will claim empty space at the end of the toolbar via glue.
	 * </p>
	 *
	 * @return the status text field
	 */
	protected JTextField createStatusTextField() {
		JTextField status = new JTextField(" ");
		status.setFont(Fonts.tweenFont);
		status.setEditable(false);
		status.setBackground(Color.black);
		status.setForeground(Color.cyan);
		status.setFocusable(false);
		status.setRequestFocusEnabled(false);
		status.setOpaque(true);
		return status;
	}
}
