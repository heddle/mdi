package edu.cnu.mdi.graphics.toolbar;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Objects;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.rubberband.IRubberbanded;
import edu.cnu.mdi.graphics.rubberband.Rubberband;
import edu.cnu.mdi.util.Environment;

/**
 * Tool that performs a rubber-band (box) zoom on an {@link IContainer}.
 * <p>
 * This tool delegates interactive rubber-banding to {@link Rubberband}, which in this
 * codebase installs temporary mouse listeners directly on the container's canvas
 * component. When the gesture completes, {@link Rubberband} calls back to
 * {@link #doneRubberbanding()}.
 * </p>
 *
 * <h2>Event routing with the new tool framework</h2>
 * <p>
 * The toolbar/controller continues to receive mouse events, but during an active
 * rubber-band gesture, the {@link Rubberband}'s own listeners do the real work.
 * Consequently, this tool only needs to start the gesture on mouse press and
 * handle completion via the callback.
 * </p>
 *
 * <h2>Cancellation</h2>
 * <p>
 * If the tool is deselected while a rubber-band gesture is active, the tool forces
 * the rubber-band to terminate (so it can remove its temporary listeners) but
 * suppresses the zoom action.
 * </p>
 *
 * @author heddle
 */
public class BoxZoomTool implements ITool, IRubberbanded {

    /** Tool id used by {@link ToolController} and {@link ToolToggleButton}. */
    public static final String ID = "boxZoom";

    /** Rubber-band policy (default preserves aspect ratio). */
    private Rubberband.Policy policy = Rubberband.Policy.RECTANGLE_PRESERVE_ASPECT;

    /** Active rubber-band instance (non-null only during a gesture). */
    private Rubberband rubberband;

    /** The container associated with the current gesture. */
    private IContainer owner;

    /** If true, we are terminating a gesture due to cancellation (no zoom). */
    private boolean cancelling;

    /**
     * Create a new box-zoom tool with the default policy
     * {@link Rubberband.Policy#RECTANGLE_PRESERVE_ASPECT}.
     */
    public BoxZoomTool() {
    }

    /**
     * Create a new box-zoom tool using the specified rubber-band policy.
     *
     * @param policy the rubber-band policy (must not be null).
     */
    public BoxZoomTool(Rubberband.Policy policy) {
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String toolTip() {
        return "Rubberband zoom";
    }

    @Override
    public Cursor cursor(ToolContext ctx) {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    /**
     * Start a rubber-band gesture on mouse press.
     * <p>
     * The {@link Rubberband} object installs its own mouse listeners and manages
     * dragging/release internally, so this method is the only mouse hook we need.
     * </p>
     */
    @Override
    public void mousePressed(ToolContext ctx, MouseEvent e) {
        if (rubberband != null) {
            return;
        }

        IContainer c = ctx.container();
        if (c == null) {
            return;
        }

        owner = c;
        cancelling = false;

        Environment.getInstance().setDragging(true);

        rubberband = new Rubberband(owner, this, policy);
        rubberband.setHighlightColor1(Color.gray);
        rubberband.setHighlightColor2(Color.green);
        rubberband.setActive(true);

        // After this call, Rubberband's own listeners drive the gesture.
        rubberband.startRubberbanding(e.getPoint());
    }

    /**
     * Called by {@link Rubberband} when it has finished rubber-banding and removed
     * its temporary listeners.
     * <p>
     * If the gesture ended normally, the selected bounds are forwarded to
     * {@link IContainer#rubberBanded(Rectangle)}.
     * If the gesture was canceled (tool deselected), the bounds are ignored.
     * </p>
     */
    @Override
    public void doneRubberbanding() {
        try {
            if (rubberband == null || owner == null) {
                return;
            }

            Rectangle bounds = rubberband.getRubberbandBounds();

            // Suppress action if we canceled the gesture.
            if (!cancelling && bounds != null) {
                owner.rubberBanded(bounds);
            }
        } finally {
            cleanup();
        }
    }

    /**
     * Cancel any in-progress rubber-band gesture when the tool is deactivated.
     * <p>
     * Since {@link Rubberband} installs listeners, we must ensure it terminates so it can
     * remove them. We do this by calling {@link Rubberband#endRubberbanding(java.awt.Point)}
     * using its current point, but we mark the termination as a cancellation so the
     * zoom action is suppressed in {@link #doneRubberbanding()}.
     * </p>
     */
    @Override
    public void onDeselected(ToolContext ctx) {
        if (rubberband != null && rubberband.isActive()) {
            cancelling = true;

            // Force the rubberband to end so it removes its temporary listeners.
            // This will trigger doneRubberbanding(), where we ignore the bounds.
            rubberband.endRubberbanding(rubberband.getCurrentPt());
            // doneRubberbanding() will call cleanup().
        } else {
            cleanup();
        }
    }

    /**
     * Set the rubber-band policy used by this tool.
     *
     * @param policy the new policy (must not be null).
     */
    public void setPolicy(Rubberband.Policy policy) {
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
    }

    /**
     * Get the rubber-band policy used by this tool.
     *
     * @return the policy (never null).
     */
    public Rubberband.Policy getPolicy() {
        return policy;
    }

    /**
     * Reset internal state after completion/cancellation.
     */
    private void cleanup() {
        try {
            if (rubberband != null) {
                rubberband.setActive(false);
            }
        } finally {
            rubberband = null;
            owner = null;
            cancelling = false;
            Environment.getInstance().setDragging(false);
        }
    }
}
