package edu.cnu.mdi.experimental;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.Enumeration;
import java.util.Objects;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import edu.cnu.mdi.graphics.GraphicsUtils;
import edu.cnu.mdi.graphics.ImageManager;
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
	
	//for panning
	private BufferedImage base;
	private BufferedImage buffer;


		
	/**
	 * Creates a new horizontal toolbar associated with a canvas.
	 *
	 * @param canvas the canvas component this toolbar is associated with
	 * @param bits   controls which predefined buttons are added to the toolbar.
	 * See {@link AToolBar} and {@link ToolBarBits}  for details. You
	 * are not limited to these bits; you can always add your own buttons after
	 * creating the toolbar.
	 */
	public BaseToolBar(Component canvas, long bits) {
		this(canvas, bits, HORIZONTAL);
	}
	
	/**
	 * 
	 * Creates a new toolbar associated with a canvas.
	 *
	 * @param canvas      the canvas component this toolbar is associated with
	 * @param bits        controls which predefined buttons are added to the toolbar.
	 * See {@link AToolBar} and {@link ToolBarBits}  for details. You
	 * are not limited to these bits; you can always add your own buttons after
	 * 
	 * @param orientation the initial orientation -- it must be either
	 *                    <code>HORIZONTAL</code> or <code>VERTICAL</code>
	 */
	public BaseToolBar(Component canvas, long bits, int orientation) {
		super(orientation);
		this.canvas = canvas;
		this.bits = bits;
		Objects.requireNonNull(canvas, "Canvas component cannot be null");
		
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
		pointerButton = new ARubberbandButton(canvas, this, Policy.RECTANGLE, DEFAULT_MIN_SIZE_PX) {

				@Override
				public void handleRubberbanding(Rectangle bounds, Point[] vertices) {
					// select all items within bounds
					System.out.println("Pointer button rubberbanded: " + bounds);
				}
				
			};
			configureButton(pointerButton, ToolBarBits.POINTERBUTTON);
			addToggle(pointerButton);
			setDefaultToggleButton(pointerButton);
		} //end pointer button
		
		//box_zoom button
		if (ToolBarBits.hasBoxZoomButton(bits)) {
			boxZoomButton = new ARubberbandButton(canvas, this, Policy.RECTANGLE_PRESERVE_ASPECT, DEFAULT_MIN_SIZE_PX) {

				@Override
				public void handleRubberbanding(Rectangle bounds, Point[] vertices) {
					// zoom to rectangle
					System.out.println("Box zoom button rubberbanded: " + bounds);
					resetDefaultToggleButton();
				}
				
			};
			configureButton(boxZoomButton, ToolBarBits.BOXZOOMBUTTON);
			addToggle(boxZoomButton);
		}
		
		//pan button
		if (ToolBarBits.hasPanButton(bits)) {
			ADragButton panButton = new ADragButton(canvas, this) {
				@Override
				public void startDrag(Point start) {
					System.out.println("start dragging at " + start);
					base = GraphicsUtils.getComponentImage(canvas);
					buffer = GraphicsUtils.getComponentImageBuffer(canvas);
				}

				@Override
				public void updateDrag(Point start, Point previous, Point current) {
					System.out.println("dragging start " + start + " previous " + previous + " current " + current);
					int totalDx = current.x - start.x;
					int totalDy = current.y - start.y;

					Graphics gg = buffer.getGraphics();
					gg.setColor(canvas.getBackground());
					gg.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());
					gg.drawImage(base, totalDx, totalDy, canvas);
					gg.dispose();

					Graphics g = canvas.getGraphics();
					g.drawImage(buffer, 0, 0, canvas);
					g.dispose();
				}

				@Override
				public void doneDrag(Point start, Point end) {
					System.out.println("done dragging from " + start + " to " + end);
					base = null;
					buffer = null;
					canvas.repaint();
				}

			};
			configureButton(panButton, ToolBarBits.PANBUTTON);
			addToggle(panButton);
			
		}
		
		//magnify button
		if (ToolBarBits.hasMagnifyButton(bits)) {
			Dimension dimension = new Dimension(100, 100);
			AMoveButton magnifyButton = new AMoveButton(canvas, this, dimension) {

				@Override
				public void handleMove(Point p) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void doneMove(Point p) {
					// TODO Auto-generated method stub
					
				}
			};
			configureButton(magnifyButton, ToolBarBits.MAGNIFYBUTTON);
			addToggle(magnifyButton);
		}

		//center button
		if (ToolBarBits.hasCenterButton(bits)) {
			centerButton = new ASingleClickButton(canvas, this) {

				@Override
				public void handleCanvasClick(MouseEvent e) {
					// center view at click location
					System.out.println("Center button clicked at: " + e.getPoint());
					resetDefaultToggleButton();
				}
				
			};
			configureButton(centerButton, ToolBarBits.CENTERBUTTON);
			addToggle(centerButton);
		}
		

		//line button
		if (ToolBarBits.hasLineButton(bits)) {
			lineButton = new ARubberbandButton(canvas, this, Policy.LINE, DEFAULT_MIN_SIZE_PX) {

				@Override
				public void handleRubberbanding(Rectangle bounds, Point[] vertices) {
					// create line item
					System.out.println("Line button rubberbanded: " + vertices[0] + " to " + vertices[1]);
					resetDefaultToggleButton();
				}
				
			};
			configureButton(lineButton, ToolBarBits.LINEBUTTON);
			addToggle(lineButton);
		}
		
		//rectangle button
		if (ToolBarBits.hasRectangleButton(bits)) {
			rectangleButton = new ARubberbandButton(canvas, this, Policy.RECTANGLE, DEFAULT_MIN_SIZE_PX) {

				@Override
				public void handleRubberbanding(Rectangle bounds, Point[] vertices) {
					// create rectangle item
					System.out.println("Rectangle button rubberbanded: " + bounds);
					resetDefaultToggleButton();
				}
				
			};
			configureButton(rectangleButton, ToolBarBits.RECTANGLEBUTTON);
			addToggle(rectangleButton);
		}
		
		
		//ellipse button
		if (ToolBarBits.hasEllipseButton(bits)) {
			ellipseButton = new ARubberbandButton(canvas, this, Policy.OVAL, DEFAULT_MIN_SIZE_PX) {

				@Override
				public void handleRubberbanding(Rectangle bounds, Point[] vertices) {
					// create ellipse item
					System.out.println("Ellipse button rubberbanded: " + bounds);
					resetDefaultToggleButton();
				}
				
			};
			configureButton(ellipseButton, ToolBarBits.ELLIPSEBUTTON);
			addToggle(ellipseButton);
		}
		
		//radarc button
		if (ToolBarBits.hasRadArcButton(bits)) {
			radArcButton = new ARubberbandButton(canvas, this, Policy.RADARC, DEFAULT_MIN_SIZE_PX) {

				@Override
				public void handleRubberbanding(Rectangle bounds, Point[] vertices) {
					// create radarc item
					System.out.println("RadArc button rubberbanded: " + bounds);
					resetDefaultToggleButton();
				}
				
			};
			configureButton(radArcButton, ToolBarBits.RADARCBUTTON);
			addToggle(radArcButton);
		}
		
		//polygon button
		if (ToolBarBits.hasPolygonButton(bits)) {
			polygonButton = new ARubberbandButton(canvas, this, Policy.POLYGON, DEFAULT_MIN_SIZE_PX) {

				@Override
				public void handleRubberbanding(Rectangle bounds, Point[] vertices) {
					// create polygon item
					System.out.println("Polygon button rubberbanded, num vert " + vertices.length);
					resetDefaultToggleButton();
				}
				
			};
			configureButton(polygonButton, ToolBarBits.POLYGONBUTTON);
			addToggle(polygonButton);
		}
		
		//polyline button
		if (ToolBarBits.hasPolylineButton(bits)) {
			polylineButton = new ARubberbandButton(canvas, this, Policy.POLYLINE, DEFAULT_MIN_SIZE_PX) {

				@Override
				public void handleRubberbanding(Rectangle bounds, Point[] vertices) {
					// create polyline item
					System.out.println("Polyline button rubberbanded, num vert " + vertices.length);
					resetDefaultToggleButton();
				}
				
			};
			configureButton(polylineButton, ToolBarBits.POLYLINEBUTTON);
			addToggle(polylineButton);
		}
		
		//text button
		if (ToolBarBits.hasTextButton(bits)) {
			textButton = new ASingleClickButton(canvas, this) {

				@Override
				public void handleCanvasClick(MouseEvent e) {
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
				public void handleRubberbanding(Rectangle bounds, Point[] vertices) {
					// create connector item
					System.out.println("Connector button rubberbanded: " + vertices[0] + " to " + vertices[1]);
					resetDefaultToggleButton();
				}
				
				@Override
				public boolean approvePoint(Point p) {
					return true;
				}
				
			};
			configureButton(connectorButton, ToolBarBits.CONNECTORBUTTON);
			addToggle(connectorButton);
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
