package edu.cnu.mdi.graphics.rubberband;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.Objects;

import edu.cnu.mdi.graphics.GraphicsUtils;

public abstract class ARubberband implements MouseListener, MouseMotionListener {
	/**
	 * Enum of possible rubber banding polices.
	 */
	public static enum Policy {
		NONE, 
		RECTANGLE, 
		RECTANGLE_PRESERVE_ASPECT, 
		POLYGON, 
		POLYLINE,
		OVAL, 
		LINE, 
		RADARC, 
		XONLY, 
		YONLY, 
		TWO_CLICK_LINE // like LINE but only needs approved clicks
	}
	
	// Needed by a hack that prevents a 1st time flash
	private static boolean veryFirst = true;

	// The default fill color. Should be fairly transparent.
	protected static final Color fillColor = new Color(255, 128, 128, 96);

	// One default color for highlighted drawing of boundary.
	protected Color highlightColor1 = Color.red;

	// Second default color for highlighted drawing of boundary.
	protected Color highlightColor2 = Color.yellow;

	// The default shape policy for rubberbanding
	protected final Policy policy;

	// The anchor or starting screen point.
	protected final Point startPt = new Point();

	// The current point during rubberbanding
	protected Point currentPt = new Point();

	// Component being rubber banded.
	protected final Component component;

	// Make sure we only start once
	protected boolean started = false;

	// used for component and background image
	protected BufferedImage backgroundImage;
	protected BufferedImage image;

	// Flag for whether rubberband is active
	protected boolean active = false;

	// Listener to notify when we are done.
	protected final IRubberbanded rubberbanded;

	/**
	 * Create a Rubberband
	 *
	 * @param component    the component being rubberbanded
	 * @param rubberbanded who gets notified when we are done.
	 * @param policy       the stretching shape policy.
	 */
	public ARubberband(Component component, IRubberbanded rubberbanded, Policy policy) {

		Objects.requireNonNull(component, "component");
		Objects.requireNonNull(rubberbanded, "rubberbanded");
		Objects.requireNonNull(policy, "policy");
		
		this.component = component;
		this.rubberbanded = rubberbanded;
		this.policy = policy;
		
		//add as listeners
		component.addMouseListener(this);
		component.addMouseMotionListener(this);
		

	}
	
	/**
	 * Start the rubber banding operation. Note: subclasses should call
	 * super.startRubberbanding(anchorPt) first.
	 *
	 * @param anchorPt the anchor (starting) screen point.
	 */
	protected void startRubberbanding(Point anchorPt) {
		if (started) {
			System.err.println("Rubberbanding already started!");
			return;
		}
		started = true;
		//get the necessary images
		// first image is simply big enough for component
		image = GraphicsUtils.getComponentImageBuffer(component);

		// this image holds background
		backgroundImage = GraphicsUtils.getComponentImage(component);
		
		startPt.setLocation(anchorPt);
		currentPt.setLocation(anchorPt);
	}
	
	/**
	 * Set the current point to the given point.
	 *
	 * @param newCurrentPoint the new current point.
	 */
	private void setCurrent(Point newCurrentPoint) {

		currentPt.setLocation(newCurrentPoint);
		if (image == null) {
			return;
		}

		// we are drawing on the bare image

		Graphics2D g2 = image.createGraphics();
		g2.setColor(fillColor);

		// copy the background image to the image
		g2.drawImage(backgroundImage, 0, 0, component);
		draw(g2);
		g2.dispose();

		Graphics g = component.getGraphics();
		// this causes that flash the first time i dinna ken why
		// hack with veryFirst prevents flash
		if (veryFirst) {
			veryFirst = false;
		} else {
			g.drawImage(image, 0, 0, component);
		}
		g.dispose();
	}
	
	/**
	 * Draw the rubber band shape
	 *
	 * @param g the graphics context to draw on.
	 */
	protected abstract void draw(Graphics2D g);
	
	/**
	 * Check whether this rubberband is active.
	 *
	 * @return <code>true</code> if active
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Get the starting screen point.
	 *
	 * @return the anchor (starting) screen point.
	 */
	public Point getStart() {
		return startPt;
	}

	/**
	 * Get the current screen point.
	 *
	 * @return The current screen point.
	 */
	public Point getCurrent() {
		return currentPt;
	}


}
