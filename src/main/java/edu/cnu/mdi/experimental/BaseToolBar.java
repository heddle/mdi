package edu.cnu.mdi.experimental;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Enumeration;
import java.util.Objects;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import edu.cnu.mdi.graphics.ImageManager;
import edu.cnu.mdi.graphics.rubberband.Rubberband;
import edu.cnu.mdi.graphics.rubberband.Rubberband.Policy;
import edu.cnu.mdi.ui.fonts.Fonts;



@SuppressWarnings("serial")
public class BaseToolBar extends AToolBar {

	/** default minimum size in pixels for rubberbanded shapes */
	public static final int DEFAULT_MIN_SIZE_PX = 4;

	/** default button size */
	public static final Dimension DEFAULT_BUTTON_SIZE = new Dimension(24, 24);

	/** default icon size (w and h)*/
	public static final int DEFAULT_ICON_SIZE = 20;

	// the canvas this toolbar is associated with
	private Component canvas;

	// the bits controlling which predefined buttons are added
	private long bits;

	// predefined rubberbanding toggle buttons
	protected ARubberbandButton pointerButton;
	protected ARubberbandButton boxZoomButton;
	protected ARubberbandButton lineButton;
	protected ARubberbandButton rectangleButton;
	protected ARubberbandButton ellipseButton;
	protected ARubberbandButton polygonButton;
	protected ARubberbandButton polylineButton;
	protected ARubberbandButton radArcButton;
	protected ARubberbandButton connectorButton;

	//predefined click on canvas buttons
	protected ASingleClickButton centerButton;
	protected ASingleClickButton textButton;
	
	//buttons that use dragging
	protected ADragButton panButton;
	
	//buttons that track movement
	protected AMoveButton magnifyButton;
	
	//one shot buttons
	protected AOneShotButton zoomInButton;
	protected AOneShotButton zoomOutButton;

	//handles the gestures and actions of the tools
	protected IToolHandler handler;
	
	//what policy to use for box zoom
	protected Rubberband.Policy boxZoomPolicy;
	
	//what policy to use for pointer
	protected Rubberband.Policy pointerPolicy;

	/**
	 * Creates a new horizontal toolbar associated with a canvas.
	 *
	 * @param canvas the canvas component this toolbar is associated with
	 * @param handler   the tool handler to notify of tool gestures
	 * @param bits   controls which predefined buttons are added to the toolbar.
	 * See {@link AToolBar} and {@link ToolBarBits}  for details. You
	 * are not limited to these bits; you can always add your own buttons after
	 * creating the toolbar.
	 */
	public BaseToolBar(Component canvas, IToolHandler handler, long bits) {
		this(canvas, handler, bits, HORIZONTAL, 
				Policy.RECTANGLE,  Policy.RECTANGLE_PRESERVE_ASPECT);
	}
	
	public BaseToolBar(Component canvas, IToolHandler handler, long bits, 
			Rubberband.Policy pointerPolicy, Rubberband.Policy boxZoomPolicy) {
		this(canvas, handler, bits, HORIZONTAL, pointerPolicy, boxZoomPolicy);
	}


	/**
	 *
	 * Creates a new toolbar associated with a canvas.
	 *
	 * @param canvas      the canvas component this toolbar is associated with
	 * @param handler   the tool handler to notify of tool gestures
	 * @param bits        controls which predefined buttons are added to the toolbar.
	 * See {@link AToolBar} and {@link ToolBarBits}  for details. You
	 * are not limited to these bits; you can always add your own buttons after
	 *
	 * @param orientation the initial orientation -- it must be either
	 *                    <code>HORIZONTAL</code> or <code>VERTICAL</code>
	 * @param pointerPolicy   the rubberband policy to use for the pointer tool
	 * @param boxZoomPolicy   the rubberband policy to use for the box zoom tool
	 */
	public BaseToolBar(Component canvas, IToolHandler handler, long bits, int orientation,  
			Rubberband.Policy pointerPolicy, Rubberband.Policy boxZoomPolicy) {
		super(orientation);
		this.canvas = canvas;
		this.bits = bits;
		this.handler = handler;
		this.pointerPolicy = pointerPolicy;
		this.boxZoomPolicy = boxZoomPolicy;
		Objects.requireNonNull(canvas, "Canvas component cannot be null");
		Objects.requireNonNull(handler, "Tool handler cannot be null");

		addPredefinedButtons();
	}


	/**
	 *
	 * Adds predefined buttons to the toolbar based on the provided bits.
	 * The order is based on common usage patterns.
	 *
	 */
	private void addPredefinedButtons() {
		//pointer button
		if (ToolBarBits.hasPointerButton(bits)) {
		pointerButton = new ARubberbandButton(canvas, this, pointerPolicy, DEFAULT_MIN_SIZE_PX) {

				@Override
				public void rubberbanding(Rectangle bounds, Point[] vertices) {
					handler.pointerRubberbanding(BaseToolBar.this, canvas, bounds);
				}

				@Override
				public void simplePress(Point p) {
				}

			};
			configureButton(pointerButton, ToolBarBits.POINTERBUTTON);
			addToggle(pointerButton);
			//pointer, if present, is the default button
			setDefaultToggleButton(pointerButton);
		} //end pointer button

		//box_zoom button
		if (ToolBarBits.hasBoxZoomButton(bits)) {
			boxZoomButton = new ARubberbandButton(canvas, this, boxZoomPolicy, DEFAULT_MIN_SIZE_PX) {

				@Override
				public void rubberbanding(Rectangle bounds, Point[] vertices) {
					handler.boxZoomRubberbanding(BaseToolBar.this, canvas, bounds);
				}
				
				@Override
				public void simplePress(Point p) {
				}

			};
			configureButton(boxZoomButton, ToolBarBits.BOXZOOMBUTTON);
			addToggle(boxZoomButton);
		}
		
		//zoom in button
		if (ToolBarBits.hasZoomInButton(bits)) {
			zoomInButton = new AOneShotButton(canvas, this) {

				@Override
				public void performAction() {
					handler.zoomIn(BaseToolBar.this, canvas);
				}
			};
			configureButton(zoomInButton, ToolBarBits.ZOOMINBUTTON);
			addButton(zoomInButton);
		}
		
		//zoom out button
		if (ToolBarBits.hasZoomOutButton(bits)) {
			zoomOutButton = new AOneShotButton(canvas, this) {

				@Override
				public void performAction() {
					handler.zoomOut(BaseToolBar.this, canvas);
				}
			};
			configureButton(zoomOutButton, ToolBarBits.ZOOMOUTBUTTON);
			addButton(zoomOutButton);
		}
		
		//undo zoom button
		if (ToolBarBits.hasUndoZoomButton(bits)) {
			AOneShotButton undoZoomButton = new AOneShotButton(canvas, this) {

				@Override
				public void performAction() {
					handler.undoZoom(BaseToolBar.this, canvas);
				}
			};
			configureButton(undoZoomButton, ToolBarBits.UNDOZOOMBUTTON);
			addButton(undoZoomButton);
		}

		//reset zoom button
		if (ToolBarBits.hasResetZoomButton(bits)) {
			AOneShotButton resetZoomButton = new AOneShotButton(canvas, this) {

				@Override
				public void performAction() {
					handler.resetZoom(BaseToolBar.this, canvas);
				}
			};
			configureButton(resetZoomButton, ToolBarBits.RESETZOOMBUTTON);
			addButton(resetZoomButton);
		}
		//pan button
		if (ToolBarBits.hasPanButton(bits)) {
			panButton = new ADragButton(canvas, this) {
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
					handler.panDoneDrag(BaseToolBar.this, canvas, start,  end);
				}
			};
			configureButton(panButton, ToolBarBits.PANBUTTON);
			addToggle(panButton);

		}

		// magnify button
		if (ToolBarBits.hasMagnifyButton(bits)) {
			Dimension dimension = new Dimension(100, 100);
			magnifyButton = new AMoveButton(canvas, this, dimension) {

				@Override
				public void startMove(Point start) {
					handler.magnifyStartMove(BaseToolBar.this, canvas, start);
				}

				@Override
				public void updateMove(Point start, Point p) {
					handler.magnifyUpdateMove(BaseToolBar.this, canvas, start, p);
				}

				@Override
				public void doneMove(Point start, Point p) {
					handler.magnifyDoneMove(BaseToolBar.this, canvas, start, p);
				}
			};
			configureButton(magnifyButton, ToolBarBits.MAGNIFYBUTTON);
			addToggle(magnifyButton);
		}

		//center button
		if (ToolBarBits.hasCenterButton(bits)) {
			centerButton = new ASingleClickButton(canvas, this) {

				@Override
				public void canvasClick(MouseEvent e) {
					handler.recenter(BaseToolBar.this, canvas, e.getPoint());
				}

			};
			configureButton(centerButton, ToolBarBits.CENTERBUTTON);
			addToggle(centerButton);
		}


		//line button
		if (ToolBarBits.hasLineButton(bits)) {
			lineButton = new ARubberbandButton(canvas, this, Policy.LINE, DEFAULT_MIN_SIZE_PX) {

				@Override
				public void rubberbanding(Rectangle bounds, Point[] vertices) {
					// create line item
					System.out.println("Line button rubberbanded: " + vertices[0] + " to " + vertices[1]);
					resetDefaultToggleButton();
				}

				@Override
				public void simplePress(Point p) {
				}

			};
			configureButton(lineButton, ToolBarBits.LINEBUTTON);
			addToggle(lineButton);
		}

		//rectangle button
		if (ToolBarBits.hasRectangleButton(bits)) {
			rectangleButton = new ARubberbandButton(canvas, this, Policy.RECTANGLE, DEFAULT_MIN_SIZE_PX) {

				@Override
				public void rubberbanding(Rectangle bounds, Point[] vertices) {
					// create rectangle item
					System.out.println("Rectangle button rubberbanded: " + bounds);
					resetDefaultToggleButton();
				}
				
				@Override
				public void simplePress(Point p) {
				}

			};
			configureButton(rectangleButton, ToolBarBits.RECTANGLEBUTTON);
			addToggle(rectangleButton);
		}


		//ellipse button
		if (ToolBarBits.hasEllipseButton(bits)) {
			ellipseButton = new ARubberbandButton(canvas, this, Policy.OVAL, DEFAULT_MIN_SIZE_PX) {

				@Override
				public void rubberbanding(Rectangle bounds, Point[] vertices) {
					// create ellipse item
					System.out.println("Ellipse button rubberbanded: " + bounds);
					resetDefaultToggleButton();
				}
				
				@Override
				public void simplePress(Point p) {
				}

			};
			configureButton(ellipseButton, ToolBarBits.ELLIPSEBUTTON);
			addToggle(ellipseButton);
		}

		//radarc button
		if (ToolBarBits.hasRadArcButton(bits)) {
			radArcButton = new ARubberbandButton(canvas, this, Policy.RADARC, DEFAULT_MIN_SIZE_PX) {

				@Override
				public void rubberbanding(Rectangle bounds, Point[] vertices) {
					// create radarc item
					System.out.println("RadArc button rubberbanded: " + bounds);
					resetDefaultToggleButton();
				}
				
				@Override
				public void simplePress(Point p) {
				}

			};
			configureButton(radArcButton, ToolBarBits.RADARCBUTTON);
			addToggle(radArcButton);
		}

		//polygon button
		if (ToolBarBits.hasPolygonButton(bits)) {
			polygonButton = new ARubberbandButton(canvas, this, Policy.POLYGON, DEFAULT_MIN_SIZE_PX) {

				@Override
				public void rubberbanding(Rectangle bounds, Point[] vertices) {
					// create polygon item
					System.out.println("Polygon button rubberbanded, num vert " + vertices.length);
					resetDefaultToggleButton();
				}
				
				@Override
				public void simplePress(Point p) {
				}

			};
			configureButton(polygonButton, ToolBarBits.POLYGONBUTTON);
			addToggle(polygonButton);
		}

		//polyline button
		if (ToolBarBits.hasPolylineButton(bits)) {
			polylineButton = new ARubberbandButton(canvas, this, Policy.POLYLINE, DEFAULT_MIN_SIZE_PX) {

				@Override
				public void rubberbanding(Rectangle bounds, Point[] vertices) {
					// create polyline item
					System.out.println("Polyline button rubberbanded, num vert " + vertices.length);
					resetDefaultToggleButton();
				}
				
				@Override
				public void simplePress(Point p) {
				}

			};
			configureButton(polylineButton, ToolBarBits.POLYLINEBUTTON);
			addToggle(polylineButton);
		}

		//text button
		if (ToolBarBits.hasTextButton(bits)) {
			textButton = new ASingleClickButton(canvas, this) {

				@Override
				public void canvasClick(MouseEvent e) {
					// create text item at click location
					System.out.println("Text button clicked at: " + e.getPoint());
					resetDefaultToggleButton();
				}

			};
			configureButton(textButton, ToolBarBits.TEXTBUTTON);
			addToggle(textButton);
		}

		//connector button
		if (ToolBarBits.hasConnectorButton(bits)) {
			connectorButton = new ARubberbandButton(canvas, this, Policy.LINE, DEFAULT_MIN_SIZE_PX) {

				@Override
				public void rubberbanding(Rectangle bounds, Point[] vertices) {
					// create connector item
					System.out.println("Connector button rubberbanded: " + vertices[0] + " to " + vertices[1]);
					resetDefaultToggleButton();
				}

				@Override
				public void simplePress(Point p) {
				}
				
				@Override
				public boolean approvePoint(Point p) {
					return true;
				}

			};
			configureButton(connectorButton, ToolBarBits.CONNECTORBUTTON);
			addToggle(connectorButton);
		}
		
		//style button
		if (ToolBarBits.hasStyleButton(bits)) {
			AOneShotButton styleButton = new AOneShotButton(canvas, this) {

				@Override
				public void performAction() {
					handler.styleEdit(BaseToolBar.this, canvas);
				}
			};
			configureButton(styleButton, ToolBarBits.STYLEBUTTON);
			addButton(styleButton);
		}
		
		//delete button
		if (ToolBarBits.hasDeleteButton(bits)) {
			AOneShotButton deleteButton = new AOneShotButton(canvas, this) {

				@Override
				public void performAction() {
					handler.delete(BaseToolBar.this, canvas);
				}
			};
			configureButton(deleteButton, ToolBarBits.DELETEBUTTON);
			addButton(deleteButton);
		}
		
		//camera button
		if (ToolBarBits.hasCameraButton(bits)) {
			AOneShotButton cameraButton = new AOneShotButton(canvas, this) {

				@Override
				public void performAction() {
					handler.captureImage(BaseToolBar.this, canvas);
				}
			};
			configureButton(cameraButton, ToolBarBits.CAMERABUTTON);
			addButton(cameraButton);
		}
		
		//printer button
		if (ToolBarBits.hasPrinterButton(bits)) {
			AOneShotButton printerButton = new AOneShotButton(canvas, this) {

				@Override
				public void performAction() {
					handler.print(BaseToolBar.this, canvas);
				}
			};
			configureButton(printerButton, ToolBarBits.PRINTERBUTTON);
			addButton(printerButton);
		}

		//status field
		if (ToolBarBits.hasStatusField(bits)) {
			statusField = createStatusTextField();
			addSeparator();
			add(Box.createHorizontalGlue());
			statusField.setMaximumSize(new Dimension(Integer.MAX_VALUE,
					statusField.getPreferredSize().height));
			add(statusField);
		}

	}

	// configure a button based on the provided bit
	private void configureButton(AbstractButton button, long bit) {
		String path = ToolBarBits.getResourcePath(bit);
		String tip = ToolBarBits.getToolTip(bit);

		button.setFocusPainted(false);
		button.setToolTipText(tip);

		if (path != null) {
			Icon icon = ImageManager.getInstance().loadUiIcon(path, DEFAULT_ICON_SIZE, DEFAULT_ICON_SIZE);
			if (icon != null) {
				button.setIcon(icon);
			} else {
				button.setText("?");
			}

		}

		setPreferredSize(DEFAULT_BUTTON_SIZE);
		setMinimumSize(DEFAULT_BUTTON_SIZE);
		setMaximumSize(DEFAULT_BUTTON_SIZE);

	}


	@Override
	protected void activeToggleButtonChanged(JToggleButton newlyActive) {
		Enumeration<AbstractButton> elements = toggleGroup.getElements();
		while (elements.hasMoreElements()) {
		    AbstractButton button = elements.nextElement();
		    // Use the button (e.g., check if it's a JToggleButton)
		    if (button instanceof MouseListener) {
		    	canvas.removeMouseListener((MouseListener) button);
		    }
		    if (button instanceof MouseMotionListener) {
		    	canvas.removeMouseMotionListener((MouseMotionListener) button);
		    }
		}

		if (newlyActive instanceof MouseListener) {
			canvas.addMouseListener((MouseListener) newlyActive);
		}
		if (newlyActive instanceof MouseMotionListener) {
			canvas.addMouseMotionListener((MouseMotionListener) newlyActive);
		}
	}
	
	/**
	 * Check if the pointer tool is currently active.
	 * @return true if the pointer tool is active, false otherwise
	 */
	public boolean isPointerActive() {
		return pointerButton != null && pointerButton.isSelected();
	}


	/**
	 * Create the status text field shown on the toolbar.
	 * If used, it will clam all empty space on the right of toolbar.
	 * It will only be added if the orientation is horizontal.
	 * @return the status text field
	 */
	protected JTextField createStatusTextField() {
		JTextField status = new JTextField(" ");
		status.setFont(Fonts.tweenFont);
		status.setEditable(false);
		status.setBackground(Color.black);
		status.setForeground(Color.cyan);
		status.setFocusable(false); // key fix
		status.setRequestFocusEnabled(false); // extra;
		status.setOpaque(true);
		return status;
	}



}
