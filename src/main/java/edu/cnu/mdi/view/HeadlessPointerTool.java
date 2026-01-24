package edu.cnu.mdi.view;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Objects;

import javax.swing.SwingUtilities;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.ItemModification;

/**
 * A minimal, "headless" pointer tool that enables item dragging (and other item
 * modifications supported by {@link AItem}) on views that do not have a visible
 * {@link edu.cnu.mdi.graphics.toolbar.AToolBar}.
 * <p>
 * In the toolbar-driven workflow, a pointer tool (e.g. PointerButton) is typically
 * responsible for installing mouse listeners and driving {@link AItem} modification
 * state (start/modify/stop). This class provides the same essential behavior without
 * requiring a toolbar UI.
 * </p>
 *
 * <h2>Behavior</h2>
 * <ul>
 *   <li>On mouse press, hit-tests for an {@link AItem}.</li>
 *   <li>If the item is draggable, the tool arms a potential drag.</li>
 *   <li>Once the mouse has moved beyond a configurable pixel threshold, the tool
 *       creates an {@link ItemModification}, calls {@link AItem#startModification()},
 *       and then repeatedly calls {@link AItem#modify()} as the mouse moves.</li>
 *   <li>On mouse release, if a modification was started, the tool calls
 *       {@link AItem#stopModification()}.</li>
 * </ul>
 *
 * <p>
 * This tool intentionally does <em>not</em> attempt to manage selection state or
 * multi-item operations; it is meant for "mini-map"/virtual views such as
 * {@code VirtualView}, where the primary goal is simple item dragging.
 * </p>
 */
public class HeadlessPointerTool implements MouseListener, MouseMotionListener {

	/** Default drag threshold (pixels) before promoting a press into a drag. */
	public static final int DEFAULT_DRAG_THRESHOLD_PX = 5;

	/** The container used for hit testing and coordinate conversions. */
	private final IContainer _container;

	/** The Swing component that receives mouse events. */
	private final Component _component;

	/** Drag threshold (px) to promote press into a drag gesture. */
	private int _dragThresholdPx = DEFAULT_DRAG_THRESHOLD_PX;

	/** Point where the press occurred (screen coords). */
	private Point _pressPt;

	/** Last mouse point processed during an active drag (screen coords). */
	private Point _lastPt;

	/** Item under cursor at press time (candidate for dragging). */
	private AItem _hitItem;

	/**
	 * True if a modification has been started (i.e., {@link ItemModification} created
	 * and {@link AItem#startModification()} has been called).
	 */
	private boolean _modifying;

	/**
	 * Create and immediately install a headless pointer tool on the view's canvas.
	 *
	 * @param view the view whose container/canvas should receive pointer behavior.
	 * @throws NullPointerException if {@code view}, its container, or its component is null.
	 */
	public HeadlessPointerTool(BaseView view) {
		Objects.requireNonNull(view, "view");

		_container = Objects.requireNonNull(view.getContainer(), "container");
		_component = Objects.requireNonNull(_container.getComponent(), "component");

		_component.addMouseListener(this);
		_component.addMouseMotionListener(this);
	}

	/**
	 * Remove listeners from the component. Call this if the view/canvas is being
	 * disposed or replaced, to avoid leaks and duplicate listeners.
	 */
	public void dispose() {
		_component.removeMouseListener(this);
		_component.removeMouseMotionListener(this);
		resetState();
	}

	/**
	 * Set the drag threshold in pixels used to promote a press into a drag.
	 *
	 * @param px threshold in pixels; values less than 0 are treated as 0.
	 */
	public void setDragThresholdPx(int px) {
		_dragThresholdPx = Math.max(0, px);
	}

	/**
	 * @return current drag threshold in pixels.
	 */
	public int getDragThresholdPx() {
		return _dragThresholdPx;
	}

	// ------------------------------------------------------------
	// MouseListener
	// ------------------------------------------------------------

	@Override
	public void mousePressed(MouseEvent e) {

		// Typically only left-button should start drag modifications.
		if (!SwingUtilities.isLeftMouseButton(e)) {
			return;
		}

		_hitItem = _container.getItemAtPoint(e.getPoint());
		if ((_hitItem == null) || !_hitItem.isDraggable() || !_hitItem.isTrackable()) {
			resetState();
			return;
		}

		_pressPt = e.getPoint();
		_lastPt = _pressPt;
		_modifying = false;
	}

	@Override
	public void mouseReleased(MouseEvent e) {

		if (!SwingUtilities.isLeftMouseButton(e)) {
			// If user releases a different button, ignore.
			return;
		}

		// Only stop modification if we actually started one.
		if (_modifying && (_hitItem != null) && (_hitItem.getItemModification() != null)) {
			_hitItem.stopModification();
		}

		resetState();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// No-op by design (selection/click actions are view-specific).
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// No-op
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// No-op
	}

	// ------------------------------------------------------------
	// MouseMotionListener
	// ------------------------------------------------------------

	@Override
	public void mouseDragged(MouseEvent e) {

		if (!SwingUtilities.isLeftMouseButton(e)) {
			return;
		}

		if ((_hitItem == null) || (_pressPt == null) || (_lastPt == null)) {
			return;
		}

		// If we haven't begun modifying yet, require crossing the threshold.
		final Point p = e.getPoint();
		if (!_modifying && !pastThreshold(_pressPt, p, _dragThresholdPx)) {
			return;
		}

		final int dx = p.x - _lastPt.x;
		final int dy = p.y - _lastPt.y;
		if ((dx == 0) && (dy == 0)) {
			return;
		}

		dragBy(dx, dy, e);
		_lastPt = p;

		// Lightweight repaint; containers sometimes prefer refresh(), but repaint is safe.
		_component.repaint();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		// No-op
	}

	// ------------------------------------------------------------
	// Core behavior
	// ------------------------------------------------------------

	/**
	 * Apply an incremental drag delta to the hit item by driving the standard
	 * {@link AItem} modification lifecycle.
	 */
	private void dragBy(int dx, int dy, MouseEvent e) {

		// Safety: item may have become untrackable (e.g., disabled/locked) mid-gesture.
		if ((_hitItem == null) || !_hitItem.isTrackable() || !_hitItem.isDraggable()) {
			resetState();
			return;
		}

		if (!_modifying) {
			_modifying = true;

			// Construct modification using the original press point and current point.
			final Point press = new Point(_pressPt);
			final Point current = e.getPoint();

			ItemModification mod = new ItemModification(
					_hitItem,
					_container,
					press,
					current,
					e.isShiftDown(),
					e.isControlDown()
			);

			_hitItem.setModification(mod);
			_hitItem.startModification();
		}

		// Update current point and apply modification.
		ItemModification mod = _hitItem.getItemModification();
		if (mod != null) {
			mod.setCurrentMousePoint(e.getPoint());
		}
		_hitItem.modify();
	}

	/**
	 * Reset transient gesture state.
	 */
	private void resetState() {
		_pressPt = null;
		_lastPt = null;
		_hitItem = null;
		_modifying = false;
	}

	/**
	 * Returns true if the distance between a and b is at least threshold pixels.
	 */
	private static boolean pastThreshold(Point a, Point b, int thresholdPx) {
		if ((a == null) || (b == null) || (thresholdPx <= 0)) {
			// Treat <=0 threshold as "always past threshold" once drag is attempted.
			return thresholdPx <= 0 && (a != null) && (b != null);
		}
		int dx = b.x - a.x;
		int dy = b.y - a.y;
		int thr = thresholdPx;
		return (dx * dx + dy * dy) >= (thr * thr);
	}
}
