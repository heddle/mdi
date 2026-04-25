package edu.cnu.mdi.transfer;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Installs and manages the drag side of a palette drag-and-drop interaction.
 *
 * <h2>Purpose</h2>
 * <p>
 * Every palette panel that lets the user drag items onto a canvas needs the
 * same Swing DnD boilerplate: create a {@link DragSource} gesture recogniser,
 * start the drag with an optional drag image, and clean up the selection when
 * the drag ends. {@code PaletteDragSupport} encapsulates that ceremony so that
 * palette panels only have to supply two things:
 * </p>
 * <ol>
 *   <li>A {@link PaletteDragSource} callback that decides, for a given drag
 *       origin point, what {@link Transferable} to carry — or {@code null} to
 *       cancel the drag.</li>
 *   <li>Optionally, a {@link DragImageProvider} callback that builds a
 *       {@link BufferedImage} to display under the cursor during the drag.</li>
 * </ol>
 *
 * <h2>Usage</h2>
 * <pre>
 * // In a palette panel constructor:
 * new PaletteDragSupport(myTable, origin -> {
 *     int row = myTable.rowAtPoint(origin);
 *     int col = myTable.columnAtPoint(origin);
 *     if (row &lt; 0 || col &lt; 1) return null;           // cancel drag
 *     MyDescriptor d = buildDescriptor(row, col);
 *     return d != null ? new MyTransferable(d) : null;
 * });
 * </pre>
 *
 * <p>To supply a drag image in addition to the transferable:</p>
 * <pre>
 * new PaletteDragSupport(myTable,
 *     origin -&gt; buildTransferable(origin),
 *     origin -&gt; buildDragImage(origin),
 *     32);   // drag image size in pixels
 * </pre>
 *
 * <h2>Cleanup callback</h2>
 * <p>
 * {@code PaletteDragSupport} calls {@link PaletteDragSource#onDragEnd()} on the
 * EDT after every drag attempt — regardless of whether the drop succeeded or
 * was cancelled, and regardless of whether the drag was even started (i.e. the
 * callback returned {@code null}). Palette panels use this to clear their
 * selection highlight.
 * </p>
 *
 * <h2>Thread safety</h2>
 * <p>
 * All Swing DnD callbacks arrive on the EDT. {@code PaletteDragSupport} does
 * not introduce any additional threading.
 * </p>
 *
 * <h2>Design notes</h2>
 * <p>
 * Only the drag side of DnD is abstracted here. Drop targets are
 * domain-specific: a map canvas and a network-layout canvas need completely
 * different logic to unpack a payload and create the right item type. Keeping
 * drop-target logic in the canvas implementations avoids forcing unrelated
 * domains into a shared type hierarchy.
 * </p>
 *
 * @see PaletteDragSource
 * @see DragImageProvider
 */
public final class PaletteDragSupport {

    // -------------------------------------------------------------------------
    // Nested callback interfaces
    // -------------------------------------------------------------------------

    /**
     * Supplies the drag payload and receives end-of-drag notification.
     *
     * <p>Implement this interface (typically as a lambda) and pass it to the
     * {@link PaletteDragSupport} constructor. The two methods correspond to the
     * two moments the palette cares about: "what are we dragging?" and "the
     * drag is over, clean up".</p>
     */
    @FunctionalInterface
    public interface PaletteDragSource {

        /**
         * Return the {@link Transferable} payload for a drag starting at
         * {@code dragOrigin}, or {@code null} to cancel the drag.
         *
         * <p>Called on the EDT from the {@link DragSource} gesture recogniser.
         * Returning {@code null} suppresses the drag entirely — no cursor change,
         * no drop target notification. This is the correct way to reject a drag
         * that starts on a non-draggable area (e.g. a label column in a table).
         * </p>
         *
         * @param dragOrigin the point, in the source component's coordinate
         *                   space, where the drag gesture began
         * @return the payload to carry, or {@code null} to cancel
         */
        Transferable createTransferable(Point dragOrigin);

        /**
         * Called on the EDT after every drag attempt ends — success, failure,
         * or cancellation.
         *
         * <p>The default implementation is a no-op. Override to clear a
         * selection highlight or reset any transient visual state.</p>
         */
        default void onDragEnd() {
            // no-op
        }
    }

    /**
     * Optionally supplies a {@link BufferedImage} to display under the cursor
     * during a drag.
     *
     * <p>Only called when {@link DragSource#isDragImageSupported()} returns
     * {@code true}. Returning {@code null} falls back to the default system
     * drag cursor with no custom image.</p>
     */
    @FunctionalInterface
    public interface DragImageProvider {

        /**
         * Build and return a drag image for the item being dragged from
         * {@code dragOrigin}, or {@code null} to use the default cursor.
         *
         * @param dragOrigin the point where the drag gesture began, in the
         *                   source component's coordinate space
         * @return the image to display under the cursor, or {@code null}
         */
        BufferedImage buildDragImage(Point dragOrigin);
    }

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Instance state
    // -------------------------------------------------------------------------

    /** Application callback that decides what to drag and handles cleanup. */
    private final PaletteDragSource dragSource;

    /** Optional provider of a visual drag image; {@code null} for no image. */
    private final DragImageProvider imageProvider;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Install drag support on {@code source} with no drag image.
     *
     * <p>The drag cursor will be the platform default (typically a small arrow
     * with a copy badge). Use
     * {@link #PaletteDragSupport(JComponent, PaletteDragSource, DragImageProvider)}
     * to supply a custom drag image.</p>
     *
     * @param source     the Swing component to install the gesture recogniser
     *                   on; must not be {@code null}
     * @param dragSource callback that creates the payload and handles cleanup;
     *                   must not be {@code null}
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public PaletteDragSupport(JComponent source, PaletteDragSource dragSource) {
        this(source, dragSource, null);
    }

    /**
     * Install drag support on {@code source} with a custom drag image.
     *
     * <p>The image is only used when {@link DragSource#isDragImageSupported()}
     * returns {@code true}. On platforms that do not support drag images the
     * {@code imageProvider} is never called and the drag falls back to the
     * default cursor.</p>
     *
     * <p>The size of the drag image is entirely the responsibility of the
     * {@link DragImageProvider} — it returns a fully-rendered
     * {@link java.awt.image.BufferedImage} of whatever dimensions are
     * appropriate for the palette's content. {@link #scaleToDragImage} is
     * provided as a convenience for the common case of scaling an existing
     * icon image.</p>
     *
     * @param source        the Swing component to install the gesture recogniser
     *                      on; must not be {@code null}
     * @param dragSource    callback that creates the payload and handles cleanup;
     *                      must not be {@code null}
     * @param imageProvider callback that builds the drag image, or {@code null}
     *                      for no custom image
     * @throws IllegalArgumentException if {@code source} or {@code dragSource}
     *                                  is {@code null}
     */
    public PaletteDragSupport(JComponent source,
                               PaletteDragSource dragSource,
                               DragImageProvider imageProvider) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        if (dragSource == null) {
            throw new IllegalArgumentException("dragSource must not be null");
        }
        this.dragSource    = dragSource;
        this.imageProvider = imageProvider;

        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
                source, DnDConstants.ACTION_COPY, this::onDragGesture);
    }

    // -------------------------------------------------------------------------
    // Private drag handling
    // -------------------------------------------------------------------------

    /**
     * Receives the raw drag gesture from the Swing DnD machinery and
     * orchestrates the full drag lifecycle.
     *
     * <ol>
     *   <li>Ask the {@link PaletteDragSource} for a {@link Transferable}.
     *       If it returns {@code null}, cancel silently.</li>
     *   <li>Attempt to build a drag image (when the platform supports it).</li>
     *   <li>Start the drag via {@link DragGestureEvent#startDrag}, attaching
     *       a {@link DragSourceAdapter} that calls
     *       {@link PaletteDragSource#onDragEnd()} on the EDT after the drag
     *       terminates.</li>
     *   <li>On any exception, call {@link PaletteDragSource#onDragEnd()}
     *       immediately so the palette is never left in a dirty state.</li>
     * </ol>
     *
     * @param dge the gesture event delivered by the Swing DnD layer
     */
    private void onDragGesture(DragGestureEvent dge) {
        Point origin = dge.getDragOrigin();

        Transferable payload = dragSource.createTransferable(origin);
        if (payload == null) {
            // Drag originated on a non-draggable area — cancel silently.
            return;
        }

        DragSourceAdapter listener = new DragSourceAdapter() {
            @Override
            public void dragDropEnd(DragSourceDropEvent dsde) {
                SwingUtilities.invokeLater(dragSource::onDragEnd);
            }
        };

        try {
            if (imageProvider != null && DragSource.isDragImageSupported()) {
                BufferedImage img = imageProvider.buildDragImage(origin);
                if (img != null) {
                    Point offset = new Point(img.getWidth() / 2, img.getHeight() / 2);
                    dge.startDrag(DragSource.DefaultCopyDrop, img, offset, payload, listener);
                    return;
                }
            }
            // Fall back: no drag image — use default system cursor only.
            dge.startDrag(DragSource.DefaultCopyDrop, payload, listener);

        } catch (Exception ex) {
            // startDrag can throw InvalidDnDOperationException if the DnD
            // subsystem is in an inconsistent state. Clean up immediately so
            // the palette is not left with a stale selection.
            dragSource.onDragEnd();
        }
    }

    // -------------------------------------------------------------------------
    // Static factory helpers
    // -------------------------------------------------------------------------

    /**
     * Scale a {@link java.awt.Image} into a new square {@link BufferedImage}
     * suitable for use as a drag image.
     *
     * <p>This is provided as a convenience for {@link DragImageProvider}
     * implementations that already have a loaded image and only need to scale
     * it. Rendering hints are set for bilinear interpolation, quality rendering,
     * and anti-aliasing.</p>
     *
     * @param source   the source image to scale; must not be {@code null}
     * @param sidePixels the side length in pixels of the output square image;
     *                   must be positive
     * @return a new {@code BufferedImage} of size
     *         {@code sidePixels × sidePixels}
     * @throws IllegalArgumentException if {@code source} is {@code null} or
     *                                  {@code sidePixels} is not positive
     */
    public static BufferedImage scaleToDragImage(java.awt.Image source, int sidePixels) {
        if (source == null) {
            throw new IllegalArgumentException("source image must not be null");
        }
        if (sidePixels <= 0) {
            throw new IllegalArgumentException("sidePixels must be positive, got " + sidePixels);
        }
        BufferedImage bi = new BufferedImage(sidePixels, sidePixels, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(source, 0, 0, sidePixels, sidePixels, null);
        g2.dispose();
        return bi;
    }
}