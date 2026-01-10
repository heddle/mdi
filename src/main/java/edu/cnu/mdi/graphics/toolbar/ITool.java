package edu.cnu.mdi.graphics.toolbar;

import java.awt.Cursor;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * A pluggable interaction tool for a canvas owned by an {@code IContainer}.
 * <p>
 * Tools encapsulate mouse-driven behavior (pan, zoom box, selection, drawing,
 * etc.) independently of the UI widget used to activate them (typically a
 * {@code JToggleButton}). This decoupling makes tool behavior easy to
 * customize, test, and reuse.
 * </p>
 * <h2>Lifecycle</h2>
 * <ul>
 * <li>{@link #onSelected(ToolContext)} is called when the tool becomes
 * active.</li>
 * <li>{@link #onDeselected(ToolContext)} is called when another tool replaces
 * it.</li>
 * </ul>
 * <p>
 * Mouse events are delivered by the toolbar/controller to the currently active
 * tool using the corresponding methods.
 * </p>
 * <p>
 * All methods have default no-op implementations so that concrete tools only
 * override what they need.
 * </p>
 *
 * @author heddle
 */
public interface ITool {

	/**
	 * A stable, unique identifier for this tool.
	 * <p>
	 * This id is used for selection, registration, persistence, logging, and
	 * programmatic activation.
	 * </p>
	 *
	 * @return the unique tool id (non-null).
	 */
	String id();

	/**
	 * The tooltip text suitable for the toolbar button that activates this tool.
	 *
	 * @return the tooltip text (may be null).
	 */
	String toolTip();

	/**
	 * Called when this tool becomes active.
	 * <p>
	 * Tools may initialize internal state here (e.g., clear rubber-bands, reset
	 * drag state, prime caches, etc.).
	 * </p>
	 *
	 * @param ctx the tool context (never null).
	 */
	default void onSelected(ToolContext ctx) {
	}

	/**
	 * Called when this tool is deactivated.
	 * <p>
	 * Tools should release temporary resources and return the UI to a clean state
	 * (e.g., close magnify windows, clear preview buffers, remove overlays, etc.).
	 * </p>
	 *
	 * @param ctx the tool context (never null).
	 */
	default void onDeselected(ToolContext ctx) {
	}

	/**
	 * Called when a mouse click occurs on the canvas.
	 *
	 * @param ctx the tool context (never null).
	 * @param e   the mouse event (never null).
	 */
	default void mouseClicked(ToolContext ctx, MouseEvent e) {
	}

	/**
	 * Called when a mouse double-click (or higher click count) occurs on the
	 * canvas.
	 *
	 * @param ctx the tool context (never null).
	 * @param e   the mouse event (never null).
	 */
	default void mouseDoubleClicked(ToolContext ctx, MouseEvent e) {
	}

	/**
	 * Called when a mouse press occurs on the canvas.
	 *
	 * @param ctx the tool context (never null).
	 * @param e   the mouse event (never null).
	 */
	default void mousePressed(ToolContext ctx, MouseEvent e) {
	}

	/**
	 * Called when a mouse release occurs on the canvas.
	 * <p>
	 * Releases may be delivered even if the pointer has moved outside the canvas.
	 * </p>
	 *
	 * @param ctx the tool context (never null).
	 * @param e   the mouse event (never null).
	 */
	default void mouseReleased(ToolContext ctx, MouseEvent e) {
	}

	/**
	 * Called when the mouse is dragged on the canvas.
	 *
	 * @param ctx the tool context (never null).
	 * @param e   the mouse event (never null).
	 */
	default void mouseDragged(ToolContext ctx, MouseEvent e) {
	}

	/**
	 * Called when the mouse moves on the canvas (without a button pressed).
	 *
	 * @param ctx the tool context (never null).
	 * @param e   the mouse event (never null).
	 */
	default void mouseMoved(ToolContext ctx, MouseEvent e) {
	}

	/**
	 * Called when the mouse enters the canvas area while this tool is active.
	 *
	 * @param ctx the tool context (never null).
	 * @param e   the mouse event (never null).
	 */
	default void mouseEntered(ToolContext ctx, MouseEvent e) {
	}

	/**
	 * Called when the mouse exits the canvas area while this tool is active.
	 *
	 * @param ctx the tool context (never null).
	 * @param e   the mouse event (never null).
	 */
	default void mouseExited(ToolContext ctx, MouseEvent e) {
	}

	/**
	 * Get the appropriate cursor for this tool.
	 *
	 * @param ctx the tool context (never null).
	 * @return The cursor appropriate when the mouse is in the container. The
	 *         default will be the default cursor.
	 */
	default Cursor cursor(ToolContext ctx) {
		return Cursor.getDefaultCursor();
	}

	/**
	 * Called when a key is pressed while this tool is active.
	 *
	 * @param ctx the tool context (never null).
	 * @param e   the key event (never null).
	 */
	default void keyPressed(ToolContext ctx, KeyEvent e) {
		// no-op
	}

}
