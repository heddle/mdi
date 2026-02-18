package edu.cnu.mdi.graphics.toolbar;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Objects;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import edu.cnu.mdi.graphics.ImageManager;
import edu.cnu.mdi.graphics.rubberband.ARubberband;
import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * Default MDI toolbar implementation for a canvas.
 * <p>
 * This class supports a mix of:
 * </p>
 * <ul>
 * <li><b>Mutually exclusive toggle tools</b> (pointer, pan, magnify, box zoom,
 * drawing tools, connector, etc.)</li>
 * <li><b>One-shot actions</b> (zoom in/out, undo/reset zoom, delete, style,
 * camera, printer)</li>
 * <li><b>Status field</b> (optional)</li>
 * </ul>
 *
 * <h2>GestureContext-first</h2>
 * <p>
 * All interactions are dispatched to {@link IToolHandler} using
 * {@link GestureContext}. Tools that originate from mouse gestures
 * (press/drag/release, hover move, rubberband) create/update a
 * {@link GestureContext}. One-shot actions synthesize a context at the canvas
 * center with a {@code null} {@link MouseEvent}.
 * </p>
 *
 * <h2>Listener swapping</h2>
 * <p>
 * Only the active toggle tool receives mouse events from the canvas. This
 * toolbar swaps canvas listeners when the active toggle changes, tracking the
 * currently installed listeners directly for O(1) removal.
 * </p>
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
 * toolbar.setButtonEnabled("myAction", false);
 * }</pre>
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
	public static final int DEFAULT_ICON_SIZE = 18;

	/** Default magnify viewport size. */
	public static final Dimension DEFAULT_MAGNIFY_SIZE = new Dimension(100, 100);

	/** The canvas this toolbar is associated with. */
	protected final Component canvas;

	/** Bits controlling which predefined buttons are added. */
	protected final long bits;

	/** Handles the gestures and actions of the tools. */
	protected IToolHandler handler;

	/** Policy to use for box zoom. */
	protected final ARubberband.Policy boxZoomPolicy;

	/** Policy to use for pointer tool. */
	protected final ARubberband.Policy pointerPolicy;

	// --- Active canvas listener tracking (O(1) swap) ---
	private MouseListener activeMouseListener;
	private MouseMotionListener activeMouseMotionListener;

	/**
	 * Creates a new horizontal toolbar associated with a canvas using common
	 * defaults: pointer uses {@link ARubberband.Policy#RECTANGLE} and box zoom uses
	 * {@link ARubberband.Policy#RECTANGLE_PRESERVE_ASPECT}.
	 *
	 * @param canvas  the canvas component this toolbar is associated with
	 * @param handler the tool handler to notify of tool gestures
	 * @param bits    controls which predefined buttons are added to the toolbar
	 */
	public BaseToolBar(Component canvas, IToolHandler handler, long bits) {
		this(canvas, handler, bits, HORIZONTAL, ARubberband.Policy.RECTANGLE,
				ARubberband.Policy.RECTANGLE_PRESERVE_ASPECT);
	}

	/**
	 * Creates a new horizontal toolbar associated with a canvas, specifying pointer
	 * and box-zoom rubberband policies.
	 *
	 * @param canvas        the canvas component this toolbar is associated with
	 * @param handler       the tool handler to notify of tool gestures
	 * @param bits          controls which predefined buttons are added to the
	 *                      toolbar
	 * @param pointerPolicy the rubberband policy to use for the pointer tool
	 * @param boxZoomPolicy the rubberband policy to use for the box zoom tool
	 */
	public BaseToolBar(Component canvas, IToolHandler handler, long bits, ARubberband.Policy pointerPolicy,
			ARubberband.Policy boxZoomPolicy) {
		this(canvas, handler, bits, HORIZONTAL, pointerPolicy, boxZoomPolicy);
	}

	/**
	 * Creates a new toolbar associated with a canvas.
	 *
	 * @param canvas        the canvas component this toolbar is associated with
	 * @param handler       the tool handler to notify of tool gestures
	 * @param bits          controls which predefined buttons are added to the
	 *                      toolbar
	 * @param orientation   toolbar orientation ({@link #HORIZONTAL} or
	 *                      {@link #VERTICAL})
	 * @param pointerPolicy the rubberband policy to use for the pointer tool
	 * @param boxZoomPolicy the rubberband policy to use for the box zoom tool
	 */
	public BaseToolBar(Component canvas, IToolHandler handler, long bits, int orientation,
			ARubberband.Policy pointerPolicy, ARubberband.Policy boxZoomPolicy) {

		super(orientation);

		this.canvas = Objects.requireNonNull(canvas, "Canvas component cannot be null");

		this.bits = bits;
		this.pointerPolicy = Objects.requireNonNull(pointerPolicy, "pointerPolicy cannot be null");
		this.boxZoomPolicy = Objects.requireNonNull(boxZoomPolicy, "boxZoomPolicy cannot be null");

		addPredefinedButtons();
	}

	/**
	 * Replace the current tool handler.
	 *
	 * @param handler the new handler (non-null)
	 */
	public void setHandler(IToolHandler handler) {
		this.handler = Objects.requireNonNull(handler, "Tool handler cannot be null");
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
	 * Create a synthetic context for one-shot actions.
	 * <p>
	 * Uses the canvas center as a representative point and provides {@code null}
	 * for the {@link MouseEvent}.
	 * </p>
	 *
	 * @return a synthetic context suitable for one-shot actions
	 */
	protected GestureContext actionContext() {
		int cx = Math.max(0, canvas.getWidth() / 2);
		int cy = Math.max(0, canvas.getHeight() / 2);
		return new GestureContext(this, canvas, null, new Point(cx, cy), null);
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

	@FunctionalInterface
	private interface TriConsumer<A, B, C> {
		void accept(A a, B b, C c);
	}


	// ------------------------------------------------------------------------
	// Predefined buttons
	// ------------------------------------------------------------------------

	/**
	 * Add predefined buttons to the toolbar based on {@link #bits}.
	 * <p>
	 * The order is based on common usage patterns (selection/navigation first, then
	 * drawing/annotation, then utilities).
	 * </p>
	 */
	private void addPredefinedButtons() {

		// Pointer tool (selection + move + rubberband multi-select)
		if (ToolBits.hasPointerButton(bits)) {
			JToggleButton pointer = new APointerButton(canvas, this, pointerPolicy, DEFAULT_MIN_SIZE_PX) {

				@Override
				public void rubberbanding(Rectangle bounds, Point[] vertices) {
					GestureContext gc = new GestureContext(BaseToolBar.this, canvas, null,
							(vertices != null && vertices.length > 0) ? vertices[0] : bounds.getLocation(), null);
					handler.pointerRubberbanding(gc, bounds);
				}

				@Override
				protected Object hitTest(Point p) {
					GestureContext gc = new GestureContext(BaseToolBar.this, canvas, null, p, null);
					return handler.hitTest(gc, p);
				}

				@Override
				protected void clickObject(Object obj, MouseEvent e) {
					GestureContext gc = new GestureContext(BaseToolBar.this, canvas, obj, e.getPoint(), e);
					handler.pointerClick(gc);
				}

				@Override
				protected void doubleClickObject(Object obj, MouseEvent e) {
					GestureContext gc = new GestureContext(BaseToolBar.this, canvas, obj, e.getPoint(), e);
					handler.pointerDoubleClick(gc);
				}

				@Override
				protected void beginDragObject(Object obj, Point pressPoint, MouseEvent e) {
					GestureContext gc = new GestureContext(BaseToolBar.this, canvas, obj, pressPoint, e);
					handler.beginDragObject(gc);
				}

				@Override
				protected void dragObjectBy(Object object, int dx, int dy, MouseEvent e) {
					GestureContext gc = new GestureContext(BaseToolBar.this, canvas, object, e.getPoint(), e);
					handler.dragObjectBy(gc, dx, dy);
				}

				@Override
				protected void endDragObject(Object object, MouseEvent e) {
					GestureContext gc = new GestureContext(BaseToolBar.this, canvas, object, e.getPoint(), e);
					handler.endDragObject(gc);
				}

				@Override
				protected boolean doNotDrag(Object object, MouseEvent e) {
					GestureContext gc = new GestureContext(BaseToolBar.this, canvas, object, e.getPoint(), e);
					return handler.doNotDrag(gc);
				}
			};

			addStdToggle(ToolBits.POINTER, pointer);
			setDefaultToggleButton(pointer); // pointer is default if present
		}

		// Box zoom tool (toggle)
		if (ToolBits.hasBoxZoomButton(bits)) {
			JToggleButton boxZoom = new ARubberbandButton(canvas, this, boxZoomPolicy, DEFAULT_MIN_SIZE_PX) {
				@Override
				public void rubberbanding(GestureContext gc, Rectangle bounds, Point[] vertices) {
					handler.boxZoomRubberbanding(gc, bounds);
				}
			};
			addStdToggle(ToolBits.BOXZOOM, boxZoom);
		}

		// Zoom in/out/undo/reset (one-shot)
		if (ToolBits.hasZoomInButton(bits)) {
			oneShot(ToolBits.ZOOMIN, () -> handler.zoomIn(actionContext()));
		}
		if (ToolBits.hasZoomOutButton(bits)) {
			oneShot(ToolBits.ZOOMOUT, () -> handler.zoomOut(actionContext()));
		}
		if (ToolBits.hasUndoZoomButton(bits)) {
			oneShot(ToolBits.UNDOZOOM, () -> handler.undoZoom(actionContext()));
		}
		if (ToolBits.hasResetZoomButton(bits)) {
			oneShot(ToolBits.RESETZOOM, () -> handler.resetZoom(actionContext()));
		}

		// Pan tool (toggle + drag)
		if (ToolBits.hasPanButton(bits)) {
			JToggleButton pan = new ADragButton(canvas, this) {
				@Override
				public void startDrag(GestureContext gc) {
					handler.panStartDrag(gc);
				}

				@Override
				public void updateDrag(GestureContext gc) {
					handler.panUpdateDrag(gc);
				}

				@Override
				public void doneDrag(GestureContext gc) {
					handler.panDoneDrag(gc);
				}
			};
			addStdToggle(ToolBits.PAN, pan);
		}

		// Magnify tool (toggle + move tracking)
		if (ToolBits.hasMagnifyButton(bits)) {
			JToggleButton magnify = new AMoveButton(canvas, this) {
				@Override
				public void startMove(GestureContext gc) {
					handler.magnifyStartMove(gc);
				}

				@Override
				public void updateMove(GestureContext gc) {
					handler.magnifyUpdateMove(gc);
				}

				@Override
				public void doneMove(GestureContext gc) {
					handler.magnifyDoneMove(gc);
				}
			};
			addStdToggle(ToolBits.MAGNIFY, magnify);
		}

		// Center tool (toggle + single click)
		if (ToolBits.hasCenterButton(bits)) {
			JToggleButton center = new ASingleClickButton(canvas, this) {
				@Override
				public void canvasClick(MouseEvent e) {
					GestureContext gc = new GestureContext(BaseToolBar.this, canvas, null, e.getPoint(), e);
					handler.recenter(gc);
				}
			};
			addStdToggle(ToolBits.CENTER, center);
		}

		// Drawing tools (rubberband)
		if (ToolBits.hasLineButton(bits)) {

			JToggleButton line = new ARubberbandButton(canvas, this, ARubberband.Policy.LINE, DEFAULT_MIN_SIZE_PX) {
				@Override
				public void rubberbanding(GestureContext gc, Rectangle bounds, Point[] vertices) {
					if (vertices != null && vertices.length == 2) {
						handler.createLine(gc, vertices[0], vertices[1]);
					}
					resetDefaultToggleButton();
				}
			};
			addStdToggle(ToolBits.LINE, line);
		}

		if (ToolBits.hasRectangleButton(bits)) {
			JToggleButton rectangle = new ARubberbandButton(canvas, this, ARubberband.Policy.RECTANGLE,
					DEFAULT_MIN_SIZE_PX) {
				@Override
				public void rubberbanding(GestureContext gc, Rectangle bounds, Point[] vertices) {
					handler.createRectangle(gc, bounds);
					resetDefaultToggleButton();
				}
			};
			addStdToggle(ToolBits.RECTANGLE, rectangle);
		}

		if (ToolBits.hasEllipseButton(bits)) {
			JToggleButton ellipse = new ARubberbandButton(canvas, this, ARubberband.Policy.OVAL, DEFAULT_MIN_SIZE_PX) {
				@Override
				public void rubberbanding(GestureContext gc, Rectangle bounds, Point[] vertices) {
					handler.createEllipse(gc, bounds);
					resetDefaultToggleButton();
				}
			};
			addStdToggle(ToolBits.ELLIPSE, ellipse);
		}

		if (ToolBits.hasRadArcButton(bits)) {
			JToggleButton radArc = new ARubberbandButton(canvas, this, ARubberband.Policy.RADARC, DEFAULT_MIN_SIZE_PX) {
				@Override
				public void rubberbanding(GestureContext gc, Rectangle bounds, Point[] vertices) {
					handler.createRadArc(gc, vertices);
					resetDefaultToggleButton();
				}
			};
			addStdToggle(ToolBits.RADARC, radArc);
		}

		if (ToolBits.hasPolygonButton(bits)) {

			JToggleButton polygon = new ARubberbandButton(canvas, this, ARubberband.Policy.POLYGON,
					DEFAULT_MIN_SIZE_PX) {
				@Override
				public void rubberbanding(GestureContext gc, Rectangle bounds, Point[] vertices) {
					handler.createPolygon(gc, vertices);
					resetDefaultToggleButton();
				}
			};
			addStdToggle(ToolBits.POLYGON, polygon);
		}

		if (ToolBits.hasPolylineButton(bits)) {
			JToggleButton polyline = new ARubberbandButton(canvas, this, ARubberband.Policy.POLYLINE,
					DEFAULT_MIN_SIZE_PX) {
				@Override
				public void rubberbanding(GestureContext gc, Rectangle bounds, Point[] vertices) {
					handler.createPolyline(gc, vertices);
					resetDefaultToggleButton();
				}
			};
			addStdToggle(ToolBits.POLYLINE, polyline);
		}

		// Text tool (toggle + single click)
		if (ToolBits.hasTextButton(bits)) {
			JToggleButton text = new ASingleClickButton(canvas, this) {
				@Override
				public void canvasClick(MouseEvent e) {
					handler.createTextItem(e.getPoint());
					resetDefaultToggleButton();
				}
			};
			addStdToggle(ToolBits.TEXT, text);
		}

		// Connector tool (rubberband line + point-approval hook)
		if (ToolBits.hasConnectorButton(bits)) {
			JToggleButton connector = new ARubberbandButton(canvas, this, ARubberband.Policy.TWO_CLICK_LINE,
					DEFAULT_MIN_SIZE_PX) {

				@Override
				public void rubberbanding(GestureContext gc, Rectangle bounds, Point[] vertices) {
					if (vertices != null && vertices.length == 2) {
						handler.createConnection(gc, vertices[0], vertices[1]);
					}
					resetDefaultToggleButton();
				}

				@Override
				public boolean approvePoint(Point p) {
					// ARubberbandButton supplies a GestureContext for the overall gesture, but
					// approvePoint may be called between clicks; create a small synthetic one.
					GestureContext gc = new GestureContext(BaseToolBar.this, canvas, null, p, null);
					return handler.approveConnectionPoint(gc, p);
				}
			};
			addStdToggle(ToolBits.CONNECTOR, connector);
		}

		// Style + Delete + Camera + Printer (one-shot)
		if (ToolBits.hasStyleButton(bits)) {
			oneShot(ToolBits.STYLEB, () -> handler.styleEdit(actionContext()));
		}
		if (ToolBits.hasDeleteButton(bits)) {
			oneShot(ToolBits.DELETE, () -> handler.delete(actionContext()));
		}
		if (ToolBits.hasCameraButton(bits)) {
			oneShot(ToolBits.CAMERA, () -> handler.captureImage(actionContext()));
		}
		if (ToolBits.hasPrinterButton(bits)) {
			oneShot(ToolBits.PRINTER, () -> handler.print(actionContext()));
		}
		if (ToolBits.hasInfoButton(bits)) {
			oneShot(ToolBits.INFO, () -> handler.info(actionContext()));
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

	/**
	 * Add the info button if it is not already present.
	 * <em>Intended for use by applications that want to add the info button after
	 * construction.</em>
	 */
	public void addInfoButton() {
		if (!hasTool(ToolBits.getId(ToolBits.INFO))) {
			oneShot(ToolBits.INFO, () -> handler.info(actionContext()));
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
	}

	/**
	 * Configure a toolbar button based on a raw icon path and tooltip.
	 * <p>
	 * Intended for application-added buttons.
	 * </p>
	 *
	 * @param button   button to configure
	 * @param iconPath path to icon resource
	 * @param tip      tooltip text
	 */
	public void configureButton(AbstractButton button, String iconPath, String tip) {
		Objects.requireNonNull(button, "button");

		button.setFocusPainted(false);
		button.setToolTipText(tip);

		if (iconPath != null && !iconPath.isBlank()) {
			Icon icon = ImageManager.getInstance().loadUiIcon(iconPath, DEFAULT_ICON_SIZE, DEFAULT_ICON_SIZE);
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

	/**
	 * Called whenever the active toggle tool changes.
	 * <p>
	 * This swaps the canvas listeners so only the active tool receives events.
	 * </p>
	 */
	@Override
	protected void activeToggleButtonChanged(JToggleButton newlyActive) {

		// Remove previously installed listeners (if any).
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
	// Convenience API for applications (restored)
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
		return isToolActive(ToolBits.getId(ToolBits.POINTER));
	}

	/**
	 * Enable or disable tools/buttons by id.
	 * <p>
	 * Works for both predefined tools (via {@link ToolBits#getId(long)}) and
	 * application-added tools (via {@link #addToggle(String, JToggleButton)} and
	 * {@link #addButton(String, JButton)}).
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

	/**
	 * Check if a tool/button with the given id exists in the toolbar registry.
	 *
	 * @param toolId the id of the tool to check
	 * @return true if the tool exists, false otherwise
	 */
	public boolean hasTool(String toolId) {
		return getButton(toolId) != null;
	}

	/**
	 * Check if the delete tool/button exists in the toolbar.
	 *
	 * @return true if the delete tool exists, false otherwise
	 */
	public boolean hasDeleteTool() {
		return hasTool(ToolBits.getId(ToolBits.DELETE));
	}

	/**
	 * Invoke the delete action as if the delete button had been pressed.
	 * <p>
	 * If the delete button is not present or not enabled, this method does nothing.
	 * </p>
	 */
	public void invokeDelete() {
		JButton deleteButton = getButton(ToolBits.getId(ToolBits.DELETE), JButton.class);
		if (deleteButton != null && deleteButton.isEnabled()) {
			deleteButton.doClick();
		}
	}

	/**
	 * Create the status text field shown on the toolbar.
	 * <p>
	 * When enabled via {@link ToolBits#STATUS}, it will claim empty space at the
	 * end of the toolbar via glue.
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
