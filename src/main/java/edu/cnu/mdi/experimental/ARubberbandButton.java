package edu.cnu.mdi.experimental;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Objects;

import javax.swing.JToggleButton;

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
	@SuppressWarnings("serial")
	public abstract class ARubberbandButton extends JToggleButton implements MouseListener, IRubberbanded {

		/** Minimum width/height in pixels for a creation gesture to be accepted. */
		private final int minSizePx;

		/** Component that owns the current gesture (null when idle). */
		protected Component canvas;

		/** Toolbar that owns this tool. */
		protected AToolBar toolBar;
		
		/** Rubberband policy to use. */
		protected Rubberband.Policy policy;
		
		/** Cached rubber band */
		protected Rubberband rubberband;
		
		/**
		 * Create a rubber-band based tool.
		 *
		 * @param canvas    component on which rubber-banding occurs.
		 * @param toolBar   toolbar that owns this tool.
		 * @param policy    rubber-band policy to use (e.g.,
		 * 				{@link Rubberband.Policy#OVAL}).
		 * @param minSizePx minimum pixel size for bounds to be considered valid.
		 */
		protected ARubberbandButton(Component canvas, AToolBar toolBar, Rubberband.Policy policy, int minSizePx) {
			Objects.requireNonNull(canvas, "canvas");
			Objects.requireNonNull(toolBar, "toolBar");
			Objects.requireNonNull(policy, "policy");
			this.canvas = canvas;
			this.toolBar = toolBar;
			this.policy = policy;
			this.minSizePx = Math.max(1, minSizePx);
		}

		/**
		 * @return rubber-band policy to use (e.g., {@link Rubberband.Policy#OVAL}).
		 */
		protected Rubberband.Policy rubberbandPolicy() {
			return policy;
		}

		/**
		 * @return cursor to use while active. Default is crosshair.
		 */
		protected Cursor activeCursor() {
			return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
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
					System.out.println("Rubberband bounds invalid: " + bounds);
					return;
				}
				Point[] vertices = rb.getRubberbandVertices();
				handleRubberbanding(bounds, vertices);

				canvas.repaint();

			} finally {
			}
		}
		
		/**
		 * Handle a completed rubber-band gesture with valid bounds.
		 *
		 * @param bounds   the rubber-band bounds
		 * @param vertices the rubber-band vertices
		 */
		public abstract void handleRubberbanding(Rectangle bounds, Point[] vertices);

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
		

		@Override
		public void mouseClicked(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}

		@Override
		public void mousePressed(MouseEvent e) {
			if (rubberband == null) {
				Rubberband.Policy policy = Objects.requireNonNull(rubberbandPolicy(), "rubberbandPolicy");
				rubberband = new Rubberband(canvas, this, policy);
			}
			
			rubberband.setActive(true);
			rubberband.startRubberbanding(e.getPoint());
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}


		

}
