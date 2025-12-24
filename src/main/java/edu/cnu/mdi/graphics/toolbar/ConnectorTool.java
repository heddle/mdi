package edu.cnu.mdi.graphics.toolbar;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.connection.ConnectionManager;
import edu.cnu.mdi.graphics.rubberband.IRubberbanded;
import edu.cnu.mdi.graphics.rubberband.Rubberband;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.ConnectorItem;
import edu.cnu.mdi.item.Layer;

/**
 * Tool that creates a "connection" between two connectable items.
 * <p>
 * Behavior:
 * <ul>
 * <li>First click must hit a clickable+connectable item -> tool becomes "armed"
 * and starts a {@link Rubberband.Policy#TWO_CLICK_LINE} rubberband from the
 * item's anchor.</li>
 * <li>While armed, rubberband follows mouse (handled by
 * {@link Rubberband}).</li>
 * <li>Clicks that do not hit a clickable+connectable item are ignored (tool
 * remains armed).</li>
 * <li>Second click on a different clickable+connectable item completes the
 * connection and prints "successful connection".</li>
 * <li>ESC cancels the in-progress connection.</li>
 * </ul>
 * </p>
 *
 * <p>
 * This tool intentionally does NOT extend {@code AbstractLineRubberbandTool}
 * because that base is designed for one-shot line gestures that complete on
 * mouse release and reset to default.
 * </p>
 */
public class ConnectorTool implements ITool, IRubberbanded {

	public static final String ID = "connector";

	/** Active rubberband session while armed; null when idle. */
	private Rubberband rubberband;

	/** Container that owns the current gesture; null when idle. */
	private IContainer owner;

	/**
	 * Controller (optional) for reset-to-default behavior if you choose to enable
	 * it.
	 */
	private ToolController controller;

	/** First selected endpoint. Null when idle. */
	private AItem first;
	
	/** Second selected endpoint. Null when idle */
	private AItem second;


	/** If true, reset to default after successful connection (one-shot). */
	private final boolean oneShot;

	public ConnectorTool() {
		this(false); // default: stay in connector tool after connecting
	}

	public ConnectorTool(boolean oneShot) {
		this.oneShot = oneShot;
	}

	@Override
	public Cursor cursor(ToolContext ctx) {
		return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
	}

	@Override
	public void onSelected(ToolContext ctx) {
		controller = safeController(ctx);
	}

	@Override
	public void onDeselected(ToolContext ctx) {
		cancel();
		owner = null;
		controller = null;
	}

	@Override
	public void mousePressed(ToolContext ctx, MouseEvent e) {
		if (ctx == null || e == null)
			return;
		if (e.getButton() != MouseEvent.BUTTON1)
			return;

		// Acquire container on every click (tool may be reused across containers)
		IContainer c = ctx.container();
		if (c == null)
			return;
		owner = c;

		// Find item under click; must be clickable + connectable
		AItem hit = pickClickableItem(owner, e.getPoint());
		if (hit == null || !hit.isConnectable()) {
			abortToDefault();
			return;
		}

		if (first == null) {
			// -----------------------------
			// First endpoint: arm tool
			// -----------------------------
			first = hit;
			Point firstAnchor = anchorFor(owner, hit, e.getPoint());

			rubberband = new Rubberband(owner, this, Rubberband.Policy.TWO_CLICK_LINE);
			rubberband.setActive(true);

			// Start the visual line at the anchor.
			rubberband.startRubberbanding(new Point(firstAnchor));

		} else {
			// -----------------------------
			// Second endpoint: finish
			// -----------------------------
			if (hit == first) {
				// ignore self-connection (change if you want to allow it)
				return;
			}
			
			second = hit;

			// End rubberband at the click point (or compute anchor for second item if
			// preferred)
			Point endPt = anchorFor(owner, hit, e.getPoint());

			// This will call doneRubberbanding(), which we implement below.
			if (rubberband != null) {
				rubberband.endRubberbanding(new Point(endPt));
			} else {
				// Defensive: if rubberband vanished, still treat as success.

				resetAfterSuccess();
			}
		}
	}

	@Override
	public void keyPressed(ToolContext ctx, KeyEvent e) {
		if (e != null && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			cancel();
		}
	}

	/**
	 * Called by {@link Rubberband} after
	 * {@link Rubberband#endRubberbanding(Point)}. For TWO_CLICK_LINE we treat that
	 * as a successful connection completion.
	 */
	@Override
	public void doneRubberbanding() {
		// If we got here, ConnectorTool requested endRubberbanding() (second valid
		// click).
		Layer connLayer = owner.getConnectionLayer(); // your new layer

		ConnectionManager.getInstance().connect(connLayer, first, second);
		owner.refresh();

		resetAfterSuccess();
	}

	// -------------------------------------------------------------------------
	// Internal helpers
	// -------------------------------------------------------------------------

	private void resetAfterSuccess() {
		// Clear rubberband + state
		rubberband = null;
		first = null;

		if (owner != null) {
			owner.refresh();
		}

		if (oneShot && controller != null) {
			controller.resetToDefault();
		}
	}

	private void cancel() {
		// Cancel without callback
		Rubberband rb = rubberband;
		rubberband = null;
		if (rb != null) {
			rb.cancel();
		}

		first = null;
		
		second = null;

		if (owner != null) {
			owner.refresh();
		}
	}
	
	//like when you click on nothing
	private void abortToDefault() {
	    cancel(); // cancels rubberband + clears state + refresh
	    if (controller != null) {
	        controller.resetToDefault();
	    }
	}


	/**
	 * Choose an anchor point for connections.
	 * <p>
	 * For now: use the click point (works immediately). Later: switch to item
	 * center/focus/port location.
	 * </p>
	 */
	private static Point anchorFor(IContainer c, AItem item, Point clickPt) {
		// If you have a real anchor API later, replace this.
		return item.getFocusPoint(c);
	}

	/**
	 * Pick the topmost clickable item at the given point.
	 * <p>
	 * You need to wire this to your container's actual hit-test API.
	 * </p>
	 */
	private static AItem pickClickableItem(IContainer c, Point p) {
		AItem item = c.getItemAtPoint(p);

		if (item == null)
			return null;

		// If you have isClickable():
		return item.isConnectable() ? item : null;

	}

	private static ToolController safeController(ToolContext ctx) {
		try {
			return (ctx == null) ? null : ctx.controller();
		} catch (RuntimeException ex) {
			return null;
		}
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public String toolTip() {
		return "Connect two items";
	}
}
