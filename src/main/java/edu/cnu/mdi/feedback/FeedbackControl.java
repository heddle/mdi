package edu.cnu.mdi.feedback;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.EventListenerList;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.util.TextUtils;

/**
 * Controller for managing mouse-over feedback for a given {@link IContainer}.
 * <p>
 * A {@code FeedbackControl} instance coordinates a set of
 * {@link IFeedbackProvider} listeners. Whenever the mouse moves, each provider
 * is asked to contribute feedback strings based on the current mouse position
 * in both screen and world coordinates. If the set of feedback strings has
 * changed since the last update, the container's {@link FeedbackPane} (if any)
 * is updated accordingly.
 * </p>
 *
 * <p>
 * Feedback is suppressed when the Control key is held down during mouse
 * movement.
 * </p>
 */
public class FeedbackControl {

	/**
	 * The parent container that owns this feedback controller.
	 */
	private final IContainer container;

	/**
	 * List of feedback providers for the parent container. Lazily created when the
	 * first provider is added.
	 */
	private EventListenerList listenerList;

	/**
	 * The newly acquired feedback strings for the current mouse position.
	 */
	private final List<String> newFeedbackStrings = new ArrayList<>(50);

	/**
	 * The feedback strings from the previous update. Used to avoid unnecessary UI
	 * updates when the feedback content has not changed.
	 */
	private final List<String> oldFeedbackStrings = new ArrayList<>(50);

	/**
	 * Creates a feedback controller for the specified container.
	 *
	 * @param container the {@link IContainer} whose feedback is being managed; must
	 *                  not be {@code null}
	 */
	public FeedbackControl(IContainer container) {
		this.container = container;
	}

	/**
	 * Requests feedback strings from all registered {@link IFeedbackProvider}s for
	 * the given mouse position.
	 *
	 * @param pp the screen-space location of the mouse (pixel coordinates)
	 * @param wp the corresponding world-space position
	 */
	private void requestFeedbackStrings(Point pp, Point2D.Double wp) {

		newFeedbackStrings.clear();

		if (listenerList == null) {
			return;
		}

		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();

		// Notify all providers in the order they were added
		for (int i = 0; i < listeners.length; i += 2) {
			if (listeners[i] == IFeedbackProvider.class) {
				((IFeedbackProvider) listeners[i + 1]).getFeedbackStrings(container, pp, wp, newFeedbackStrings);
			}
		}
	}

	/**
	 * Adds a feedback provider to this controller.
	 * <p>
	 * If the provider is already registered, it is first removed so that it is not
	 * stored twice.
	 * </p>
	 *
	 * @param provider the feedback provider listener to add; must not be
	 *                 {@code null}
	 */
	public void addFeedbackProvider(IFeedbackProvider provider) {

		if (listenerList == null) {
			listenerList = new EventListenerList();
		}

		// Avoid duplicates by removing any existing instance first
		listenerList.remove(IFeedbackProvider.class, provider);
		listenerList.add(IFeedbackProvider.class, provider);
	}

	/**
	 * Removes a feedback provider from this controller.
	 *
	 * @param provider the feedback provider to remove; if {@code null}, or if there
	 *                 is no listener list yet, this method does nothing
	 */
	public void removeFeedbackProvider(IFeedbackProvider provider) {

		if (provider == null || listenerList == null) {
			return;
		}

		listenerList.remove(IFeedbackProvider.class, provider);
	}

	/**
	 * Updates the feedback in response to a mouse movement.
	 * <p>
	 * The controller:
	 * </p>
	 * <ol>
	 * <li>Skips processing entirely if the Control key is held down.</li>
	 * <li>Requests fresh feedback strings from all providers.</li>
	 * <li>Compares the new strings with the previous set; if unchanged, no update
	 * is performed.</li>
	 * <li>If changed and a {@link FeedbackPane} is attached to the container,
	 * updates the pane.</li>
	 * </ol>
	 *
	 * @param mouseEvent the mouse event, used for the screen location and
	 *                   modifier-state checks
	 * @param wp         the corresponding world-space point for the mouse location
	 * @param dragging   {@code true} if the mouse is currently being dragged;
	 *                   currently not used in the logic but reserved for possible
	 *                   future policy changes
	 */
	public void updateFeedback(MouseEvent mouseEvent, Point2D.Double wp, boolean dragging) {

		// Skip feedback if Control is pressed
		if (mouseEvent.isControlDown()) {
			return;
		}

		// Get strings from all providers for the current location
		requestFeedbackStrings(mouseEvent.getPoint(), wp);

		// Don't update if the feedback is unchanged
		if (TextUtils.equalStringLists(oldFeedbackStrings, newFeedbackStrings)) {
			return;
		}

		// Update feedback pane if there is one
		if (container.getFeedbackPane() != null) {
			container.getFeedbackPane().updateFeedback(newFeedbackStrings);
		}

		// Copy new strings into oldStrings for the next comparison
		oldFeedbackStrings.clear();
		oldFeedbackStrings.addAll(newFeedbackStrings);
		newFeedbackStrings.clear();
	}
}
