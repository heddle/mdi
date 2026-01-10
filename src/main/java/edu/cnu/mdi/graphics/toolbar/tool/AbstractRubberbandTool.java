package edu.cnu.mdi.graphics.toolbar.tool;

import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Objects;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.rubberband.IRubberbanded;
import edu.cnu.mdi.graphics.rubberband.Rubberband;
import edu.cnu.mdi.graphics.toolbar.ToolContext;
import edu.cnu.mdi.graphics.toolbar.ToolController;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.Layer;

/**
 * Base class for tools that create an {@link AItem} using a {@link Rubberband}
 * bounds-based gesture (e.g. rectangle, ellipse) on a container that supports
 * annotation creation.
 * <p>
 * This base class handles:
 * </p>
 * <ul>
 * <li>starting a rubber-band gesture on mouse press</li>
 * <li>validating the rubber-band bounds</li>
 * <li>creating an item in the annotation list</li>
 * <li>clearing selection and returning to the default tool</li>
 * <li>cancelling an in-progress gesture when deselected</li>
 * </ul>
 *
 * <h2>Lifecycle note</h2> {@link #doneRubberbanding()} is invoked by
 * {@link Rubberband} and does not receive a {@link ToolContext}. It may arrive
 * after the tool has been deselected. To avoid NPEs, this class snapshots
 * required collaborators at the start of {@code doneRubberbanding()} and clears
 * instance fields early.
 *
 * @author heddle
 */
public abstract class AbstractRubberbandTool implements ITool, IRubberbanded {

	/** Minimum width/height in pixels for a creation gesture to be accepted. */
	private final int minSizePx;

	/** Active rubber-band session (null when idle). */
	private Rubberband rubberband;

	/** Container that owns the current gesture (null when idle). */
	private IContainer container;

	/**
	 * Cached controller so we can reset tool selection from doneRubberbanding().
	 */
	private ToolController controller;

	/**
	 * Create a rubber-band based tool.
	 *
	 * @param minSizePx minimum pixel size for bounds to be considered valid.
	 */
	protected AbstractRubberbandTool(int minSizePx) {
		this.minSizePx = Math.max(1, minSizePx);
	}

	/**
	 * @return rubber-band policy to use (e.g., {@link Rubberband.Policy#OVAL}).
	 */
	protected abstract Rubberband.Policy rubberbandPolicy();

	/**
	 * Create the new item for the given bounds (screen coordinates).
	 *
	 * @param layer  the annotation layer (never null)
	 * @param bounds rubber-band bounds in screen pixels (never null)
	 * @return the created item, or null if none was created
	 */
	protected abstract AItem createItem(Layer layer, Rectangle bounds);

	/**
	 * Hook for subclasses to configure the newly created item (drag/resize/etc).
	 * Default is no-op.
	 *
	 * @param item created item (never null)
	 */
	protected void configureItem(AItem item) {
		// no-op
	}

	/**
	 * @return cursor to use while active. Default is crosshair.
	 */
	protected Cursor activeCursor() {
		return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
	}

	@Override
	public Cursor cursor(ToolContext ctx) {
		return activeCursor();
	}

	@Override
	public void onSelected(ToolContext ctx) {
		controller = (ctx == null) ? null : safeController(ctx);
	}

	/**
	 * Starts rubber-banding on mouse press if the active container supports
	 * {@link IAnnotationSupport}.
	 */
	@Override
	public final void mousePressed(ToolContext ctx, MouseEvent e) {

		if (ctx == null || e == null) {
			return;
		}

		container = ctx.container();
		// Ignore if already active.
		if (rubberband != null) {
			return;
		}

		// Defensive: if a temporary override tool is activated without onSelected,
		// still obtain the controller here.
		if (controller == null) {
			controller = safeController(ctx);
		}

		Rubberband.Policy policy = Objects.requireNonNull(rubberbandPolicy(), "rubberbandPolicy");
		rubberband = new Rubberband(container.getComponent(), this, policy);
		rubberband.setActive(true);
		rubberband.startRubberbanding(e.getPoint());
	}

	/**
	 * Called by {@link Rubberband} when the gesture completes. Creates an item if
	 * bounds are valid, then returns to the default tool.
	 */
	@Override
	public final void doneRubberbanding() {
		// Snapshot fields first, then clear instance state.
		final Rubberband rb = this.rubberband;
		final ToolController tc = this.controller;

		this.rubberband = null;
		this.controller = null;

		try {
			if (rb == null) {
				return;
			}

			Rectangle bounds = rb.getRubberbandBounds();
			if (!isValidBounds(bounds)) {
				return;
			}

			// Create the item and place it on the annotation layer.
			Layer layer = container.getAnnotationLayer();
			AItem item = createItem(layer, bounds);

			if (item != null) {
				configureItem(item);
			}

			container.selectAllItems(false);

			if (tc != null) {
				tc.resetToDefault();
			}

			container.refresh();

		} finally {
		}
	}

	@Override
	public void onDeselected(ToolContext ctx) {
		cancelRubberband();
		controller = null;
	}

	protected final void cancelRubberband() {
		Rubberband rb = rubberband;
		rubberband = null;
		if (rb != null) {
			rb.cancel();
		}
	}

	private boolean isValidBounds(Rectangle b) {
		return (b != null) && (b.width >= minSizePx) && (b.height >= minSizePx);
	}

	private static ToolController safeController(ToolContext ctx) {
		try {
			return (ctx == null) ? null : ctx.controller();
		} catch (RuntimeException ex) {
			return null;
		}
	}
}
