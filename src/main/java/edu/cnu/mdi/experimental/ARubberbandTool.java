package edu.cnu.mdi.experimental;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Objects;

import edu.cnu.mdi.graphics.rubberband.IRubberbanded;
import edu.cnu.mdi.graphics.rubberband.Rubberband;

	/**
	 * Base class for tools using a {@link Rubberband} bounds-based gesture (e.g.
	 * rectangle, ellipse) on a component.
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
	 * {@link Rubberband}. It may arrive after the tool has been deselected. To
	 * avoid NPEs, this class snapshots required collaborators at the start of
	 * {@code doneRubberbanding()} and clears instance fields early.
	 *
	 * @author heddle
	 */
	public abstract class ARubberbandTool implements IRubberbanded {

		/** Minimum width/height in pixels for a creation gesture to be accepted. */
		private final int minSizePx;

		/** Active rubber-band session (null when idle). */
		private Rubberband rubberband;

		/** Component that owns the current gesture (null when idle). */
		private Component canvas;

		/** Toolbar that owns this tool. */
		private AToolBar toolBar;

		/**
		 * Create a rubber-band based tool.
		 *
		 * @param canvas    component on which rubber-banding occurs.
		 * @param toolBar   toolbar that owns this tool.
		 * @param minSizePx minimum pixel size for bounds to be considered valid.
		 */
		protected ARubberbandTool(Component canvas, AToolBar toolBar, int minSizePx) {
			Objects.requireNonNull(canvas, "canvas");
			Objects.requireNonNull(toolBar, "toolBar");
			this.canvas = canvas;
			this.toolBar = toolBar;
			this.minSizePx = Math.max(1, minSizePx);
		}

		/**
		 * @return rubber-band policy to use (e.g., {@link Rubberband.Policy#OVAL}).
		 */
		protected abstract Rubberband.Policy rubberbandPolicy();

		/**
		 * @return cursor to use while active. Default is crosshair.
		 */
		protected Cursor activeCursor() {
			return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
		}

		/**
		 * Starts rubber-banding on mouse press. {@link IAnnotationSupport}.
		 */
		public final void mousePressed(MouseEvent e) {

			if (rubberband != null) {
				return;
			}

			Rubberband.Policy policy = Objects.requireNonNull(rubberbandPolicy(), "rubberbandPolicy");
			rubberband = new Rubberband(canvas, this, policy);
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

			this.rubberband = null;

			try {
				if (rb == null) {
					return;
				}

				Rectangle bounds = rb.getRubberbandBounds();
				if (!isValidBounds(bounds)) {
					return;
				}

				toolBar.resetDefaultToggleButton();
				canvas.repaint();

			} finally {
			}
		}

		/**
		 * Cancel an in-progress rubber-band gesture.
		 */
		protected final void cancelRubberband() {
			Rubberband rb = rubberband;
			rubberband = null;
			if (rb != null) {
				rb.cancel();
			}
		}

		// checks minimum size of rubberband bounds
		private boolean isValidBounds(Rectangle b) {
			return (b != null) && (b.width >= minSizePx) && (b.height >= minSizePx);
		}

}
