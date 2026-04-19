package edu.cnu.mdi.mapping;

import java.awt.BasicStroke;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.awt.Color;
import java.awt.Stroke;

/**
 * Configures the visual appearance of a {@link ShapeFeatureRenderer} layer.
 *
 * <p>{@code ShapeFeatureStyle} is a mutable configuration object intentionally
 * designed for fluent construction at the call site. Unlike the immutable
 * {@link MapTheme}, styles for individual layers are expected to be created
 * once per layer and not shared or modified afterward.</p>
 *
 * <h2>Per-geometry-type defaults</h2>
 * <p>The defaults are chosen so that a style can be used with any geometry
 * type without explicit configuration, but callers will almost always want to
 * set at least the stroke or fill color to match their data:</p>
 * <ul>
 *   <li><b>Fill color</b>: {@code null} — polygons are not filled by default.
 *       Set via {@link #fillColor(Color)} for lakes, land cover, etc.</li>
 *   <li><b>Stroke color</b>: {@link Color#BLUE} — a visible default for
 *       polylines such as rivers. Set via {@link #strokeColor(Color)}.</li>
 *   <li><b>Stroke width</b>: 1.0 pt.</li>
 *   <li><b>Point radius</b>: 3.0 screen pixels.</li>
 *   <li><b>Point color</b>: {@link Color#RED}.</li>
 *   <li><b>Label field</b>: {@code null} — no labels drawn by default.</li>
 *   <li><b>Antialiasing</b>: enabled.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Rivers — thin blue lines, no fill, no labels
 * ShapeFeatureStyle riverStyle = new ShapeFeatureStyle()
 *         .strokeColor(new Color(0x6B9FD4))
 *         .strokeWidth(0.8f);
 *
 * // Lakes — filled blue polygons with a slightly darker border
 * ShapeFeatureStyle lakeStyle = new ShapeFeatureStyle()
 *         .fillColor(new Color(0x6B9FD4))
 *         .strokeColor(new Color(0x4A7FB5))
 *         .strokeWidth(0.5f)
 *         .labelField("name");
 *
 * // Urban areas — semi-transparent fill, no border
 * ShapeFeatureStyle urbanStyle = new ShapeFeatureStyle()
 *         .fillColor(new Color(180, 120, 80, 120))
 *         .strokeColor(null);
 * }</pre>
 */
public final class ShapeFeatureStyle {

    // -------------------------------------------------------------------------
    // Polygon / polyline appearance
    // -------------------------------------------------------------------------

    /**
     * Fill color for polygon features, or {@code null} to skip filling.
     * Has no effect on polyline or point features.
     */
    private Color fillColor = null;

    /**
     * Stroke color for polygon outlines and polylines, or {@code null} to
     * skip stroking entirely.
     */
    private Color strokeColor = Color.BLUE;

    /** Stroke width in points for polygon outlines and polylines. */
    private float strokeWidth = 1.0f;

    // -------------------------------------------------------------------------
    // Point appearance
    // -------------------------------------------------------------------------

    /** Fill color for point marker dots. */
    private Color pointColor = Color.RED;

    /** Radius in screen pixels of point marker dots. */
    private double pointRadius = 3.0;

    // -------------------------------------------------------------------------
    // Labels
    // -------------------------------------------------------------------------

    /**
     * Name of the {@code .dbf} field whose value is used as the feature
     * label, or {@code null} to suppress labels entirely.
     *
     * <p>For Natural Earth data common choices are {@code "name"},
     * {@code "NAME"}, or {@code "name_en"}.</p>
     */
    private String labelField = null;

    /** Color used to draw feature labels. */
    private Color labelColor = Color.DARK_GRAY;

    /**
     * Ordered list of {@code .dbf} field names whose values are concatenated
     * to form the tooltip shown in the feedback panel on mouse-over.
     *
     * <p>If empty, the tooltip falls back to {@link #labelField} when set,
     * or no tooltip is shown if both are absent. Multiple fields are joined
     * with two spaces: {@code "Lake Superior  area: 82103"}.</p>
     */
    private final List<String> tooltipFields = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Rendering quality
    // -------------------------------------------------------------------------

    /** Whether to use antialiased rendering. Defaults to {@code true}. */
    private boolean antialias = true;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a new style with all defaults applied. See the class-level
     * documentation for the default values.
     */
    public ShapeFeatureStyle() {}

    // -------------------------------------------------------------------------
    // Fluent setters — polygon / polyline
    // -------------------------------------------------------------------------

    /**
     * Sets the fill color for polygon features.
     *
     * <p>Pass {@code null} to skip polygon filling (outline only). The fill
     * color may include an alpha channel for semi-transparent overlays:
     * {@code new Color(r, g, b, alpha)}.</p>
     *
     * @param fillColor fill color, or {@code null} for no fill
     * @return this style, for chaining
     */
    public ShapeFeatureStyle fillColor(Color fillColor) {
        this.fillColor = fillColor;
        return this;
    }

    /**
     * Sets the stroke color for polygon outlines and polylines.
     *
     * <p>Pass {@code null} to suppress stroking entirely. For polygons with
     * a fill but no border, pass {@code null} here and set
     * {@link #fillColor(Color)}.</p>
     *
     * @param strokeColor stroke color, or {@code null} for no stroke
     * @return this style, for chaining
     */
    public ShapeFeatureStyle strokeColor(Color strokeColor) {
        this.strokeColor = strokeColor;
        return this;
    }

    /**
     * Sets the stroke width in points for polygon outlines and polylines.
     *
     * @param strokeWidth stroke width; clamped to a minimum of 0.1
     * @return this style, for chaining
     */
    public ShapeFeatureStyle strokeWidth(float strokeWidth) {
        this.strokeWidth = Math.max(0.1f, strokeWidth);
        return this;
    }

    // -------------------------------------------------------------------------
    // Fluent setters — points
    // -------------------------------------------------------------------------

    /**
     * Sets the color of point marker dots.
     *
     * @param pointColor point color; must not be {@code null}
     * @return this style, for chaining
     */
    public ShapeFeatureStyle pointColor(Color pointColor) {
        this.pointColor = pointColor;
        return this;
    }

    /**
     * Sets the radius of point marker dots in screen pixels.
     *
     * @param pointRadius radius; clamped to a minimum of 0.5
     * @return this style, for chaining
     */
    public ShapeFeatureStyle pointRadius(double pointRadius) {
        this.pointRadius = Math.max(0.5, pointRadius);
        return this;
    }

    // -------------------------------------------------------------------------
    // Fluent setters — labels
    // -------------------------------------------------------------------------

    /**
     * Sets the {@code .dbf} field name whose value is drawn as a label next
     * to each feature.
     *
     * <p>Pass {@code null} to suppress labels. The field name is
     * case-sensitive and must match a column in the shapefile's {@code .dbf}
     * table. Common values for Natural Earth data: {@code "name"},
     * {@code "NAME"}, {@code "name_en"}.</p>
     *
     * @param labelField field name to use for labels, or {@code null}
     * @return this style, for chaining
     */
    public ShapeFeatureStyle labelField(String labelField) {
        this.labelField = labelField;
        return this;
    }

    /**
     * Sets the color used to draw feature labels.
     *
     * @param labelColor label color; must not be {@code null}
     * @return this style, for chaining
     */
    public ShapeFeatureStyle labelColor(Color labelColor) {
        this.labelColor = labelColor;
        return this;
    }

    /**
     * Sets the {@code .dbf} field names whose values appear in the
     * mouse-over feedback tooltip, in the order given.
     *
     * <p>Replaces any previously configured tooltip fields. Pass no
     * arguments (or call with an empty array) to clear tooltip fields.
     * Example:</p>
     * <pre>{@code
     * new ShapeFeatureStyle()
     *         .fillColor(new Color(0x6B9FD4))
     *         .tooltipFields("name", "scalerank");
     * }</pre>
     *
     * @param fields zero or more {@code .dbf} field names (case-sensitive)
     * @return this style, for chaining
     */
    public ShapeFeatureStyle tooltipFields(String... fields) {
        this.tooltipFields.clear();
        if (fields != null) {
            for (String f : fields) {
                if (f != null && !f.isEmpty()) this.tooltipFields.add(f);
            }
        }
        return this;
    }

    // -------------------------------------------------------------------------
    // Fluent setters — quality
    // -------------------------------------------------------------------------

    /**
     * Enables or disables antialiased rendering for this layer.
     *
     * @param antialias {@code true} to enable antialiasing (default)
     * @return this style, for chaining
     */
    public ShapeFeatureStyle antialias(boolean antialias) {
        this.antialias = antialias;
        return this;
    }

    // -------------------------------------------------------------------------
    // Getters (used by ShapeFeatureRenderer)
    // -------------------------------------------------------------------------

    /**
     * Returns the polygon fill color, or {@code null} if polygons are not
     * filled.
     *
     * @return fill color or {@code null}
     */
    public Color getFillColor() { return fillColor; }

    /**
     * Returns the stroke color for polygon outlines and polylines, or
     * {@code null} if stroking is suppressed.
     *
     * @return stroke color or {@code null}
     */
    public Color getStrokeColor() { return strokeColor; }

    /**
     * Returns the stroke width in points.
     *
     * @return stroke width (&ge; 0.1)
     */
    public float getStrokeWidth() { return strokeWidth; }

    /**
     * Returns a {@link Stroke} built from the current {@link #getStrokeWidth()}.
     * Convenience method used by {@link ShapeFeatureRenderer}.
     *
     * @return a {@link BasicStroke} with the configured width
     */
    public Stroke buildStroke() { return new BasicStroke(strokeWidth); }

    /**
     * Returns the point marker color.
     *
     * @return point color; never {@code null}
     */
    public Color getPointColor() { return pointColor; }

    /**
     * Returns the point marker radius in screen pixels.
     *
     * @return point radius (&ge; 0.5)
     */
    public double getPointRadius() { return pointRadius; }

    /**
     * Returns the {@code .dbf} field name used for labels, or {@code null}
     * if labels are suppressed.
     *
     * @return label field name or {@code null}
     */
    public String getLabelField() { return labelField; }

    /**
     * Returns the label text color.
     *
     * @return label color; never {@code null}
     */
    public Color getLabelColor() { return labelColor; }

    /**
     * Returns an unmodifiable view of the tooltip field names.
     *
     * <p>If empty, the tooltip falls back to {@link #getLabelField()} when
     * set. If both are absent, no tooltip is produced.</p>
     *
     * @return unmodifiable ordered list of tooltip field names
     */
    public List<String> getTooltipFields() {
        return Collections.unmodifiableList(tooltipFields);
    }

    /**
     * Returns whether antialiased rendering is enabled.
     *
     * @return {@code true} if antialiasing is on
     */
    public boolean isAntialias() { return antialias; }
}