package edu.cnu.mdi.graphics;

import java.awt.BasicStroke;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import edu.cnu.mdi.graphics.style.LineStyle;

/**
 * A factory and caching facility for {@link BasicStroke} instances used in the MDI
 * graphics system.
 *
 * <p>
 * Creating {@code BasicStroke} objects is inexpensive, but repeated creation of
 * identical strokes—especially in rendering-intensive code—can produce unnecessary
 * allocation churn. This factory provides a lightweight cache keyed by line width
 * and {@link LineStyle}, ensuring that repeated calls return shared stroke
 * instances with identical configuration.
 * </p>
 *
 * <p>
 * The class also provides several predefined dash-based highlight strokes, along
 * with a convenience method to copy an existing stroke while changing only its
 * width. It is safe for concurrent use: the cache is a {@link ConcurrentHashMap}.
 * </p>
 *
 * <p>
 * The class is {@code final} with a private constructor, because it is intended
 * exclusively as a static utility and should not be instantiated.
 * </p>
 */
public final class StrokeFactory {

    /**
     * A standard dash pattern used for many predefined strokes.
     * The pattern consists of a repeated {@code 8px} dash.
     */
    private static final float[] DASH = { 8.0f };

    // -------------------------------------------------------------------------
    // Predefined highlight strokes
    // These mimic common visual highlight styles from bCNU while using BasicStroke.
    // -------------------------------------------------------------------------

    /** Highlight stroke: 1px width, long dash, zero phase. */
    public static final BasicStroke HIGHLIGHT_DASH1 =
            new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                    8.0f, DASH, 0.0f);

    /** Highlight stroke: 1px width, long dash, phase shifted. */
    public static final BasicStroke HIGHLIGHT_DASH2 =
            new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                    8.0f, DASH, DASH[0]);

    /** Highlight stroke: 2px width, long dash. */
    public static final BasicStroke HIGHLIGHT_DASH1_2 =
            new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                    8.0f, DASH, 0.0f);

    /** Highlight stroke: 2px width, long dash, phase shifted. */
    public static final BasicStroke HIGHLIGHT_DASH2_2 =
            new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                    8.0f, DASH, DASH[0]);

    /** Highlight stroke: 4px width, long dash. */
    public static final BasicStroke HIGHLIGHT_DASH1_2T =
            new BasicStroke(4.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                    8.0f, DASH, 0.0f);

    /** Highlight stroke: 4px width, long dash, phase shifted. */
    public static final BasicStroke HIGHLIGHT_DASH2_2T =
            new BasicStroke(4.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                    8.0f, DASH, DASH[0]);

    /** Simple dashed stroke: 1px width, single dash pattern. */
    public static final BasicStroke SIMPLE_DASH =
            new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                    8.0f, DASH, DASH[0]);

    /** Simple dashed stroke with an alternative dash phase (0.5). */
    public static final BasicStroke SIMPLE_DASH2 =
            new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                    8.0f, DASH, 0.5f);

    /**
     * Thread-safe cache mapping stroke configurations to shared {@link BasicStroke}
     * instances.
     *
     * <p>
     * Keys are strings of the form {@code "LW_2.0_LS_DASH"} representing line width
     * and line style. Using a {@code ConcurrentHashMap} ensures lock-free lookups.
     * </p>
     */
    private static final Map<String, BasicStroke> CACHE = new ConcurrentHashMap<>();

    /** Private constructor to prevent instantiation. */
    private StrokeFactory() { }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns a cached {@link BasicStroke} corresponding to the requested width
     * and {@link LineStyle}. If no matching stroke exists, a new one is created,
     * cached, and then returned.
     *
     * <p>
     * This method guarantees that repeated requests with identical parameters
     * return the same {@link BasicStroke} object, which may improve rendering
     * performance in many repeated-drawing scenarios.
     * </p>
     *
     * @param lineWidth the width of the stroke, in pixels
     * @param lineStyle the desired line style (solid, dashed, dot-dash, etc.)
     * @return a cached {@link BasicStroke} instance for the given configuration
     */
    public static BasicStroke get(float lineWidth, LineStyle lineStyle) {
        String key = "LW_" + lineWidth + "_LS_" + lineStyle;
        return CACHE.computeIfAbsent(key, k -> create(lineWidth, lineStyle));
    }

    /**
     * Creates a new {@link BasicStroke} instance corresponding to a given width
     * and {@link LineStyle}. This method is invoked internally by the cache.
     *
     * @param w     the stroke width
     * @param style the line style enumeration
     * @return a newly constructed {@link BasicStroke}
     */
    private static BasicStroke create(float w, LineStyle style) {
        switch (style) {
            case SOLID:
                return new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

            case DASH:
                return new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 8.0f,
                        new float[] { 10.0f, 10.0f }, 0.0f);

            case DOT_DASH:
                return new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 8.0f,
                        new float[] { 4.0f, 4.0f, 10.0f, 4.0f }, 0.0f);

            case DOT:
                return new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 8.0f,
                        new float[] { 4.0f, 4.0f }, 0.0f);

            case DOUBLE_DASH:
                return new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 8.0f,
                        new float[] { 10.0f, 4.0f, 10.0f, 10.0f }, 0.0f);

            case LONG_DASH:
                return new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 8.0f,
                        new float[] { 15.0f, 15.0f }, 0.0f);

            case LONG_DOT_DASH:
                return new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 8.0f,
                        new float[] { 6.0f, 4.0f, 15.0f, 4.0f }, 0.0f);

            default:
                return new BasicStroke(w);
        }
    }

    /**
     * Creates a copy of an existing {@link BasicStroke} while replacing only its
     * line width.
     *
     * <p>
     * This is useful when a rendering context changes dynamically between
     * zoom-levels while preserving dash patterns, cap styles, and join types.
     * </p>
     *
     * @param original the stroke whose settings should be cloned
     * @param newWidth the new width in pixels
     * @return a stroke identical to {@code original} except for the width
     */
    public static BasicStroke copyWithNewWidth(BasicStroke original, float newWidth) {
        return new BasicStroke(
                newWidth,
                original.getEndCap(),
                original.getLineJoin(),
                original.getMiterLimit(),
                original.getDashArray(),
                original.getDashPhase()
        );
    }
}
