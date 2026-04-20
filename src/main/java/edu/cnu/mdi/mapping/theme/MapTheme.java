package edu.cnu.mdi.mapping.theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;

import edu.cnu.mdi.mapping.projection.IMapProjection;
import edu.cnu.mdi.mapping.render.CityPointRenderer;
import edu.cnu.mdi.mapping.render.CountryRenderer;

/**
 * Immutable value object that encapsulates the complete visual style used when
 * rendering a map and its decorations (graticule, country borders, city
 * markers, labels, ocean fill, and projection outline).
 *
 * <h2>Usage</h2>
 * <p>Obtain an instance either from one of the built-in presets
 * ({@link #light()}, {@link #dark()}, {@link #blue()}) or by constructing a
 * custom theme with the nested {@link Builder}:</p>
 * <pre>{@code
 * MapTheme custom = new MapTheme.Builder()
 *         .backgroundColor(Color.WHITE)
 *         .oceanColor(new Color(0xADD8E6))
 *         .landColor(new Color(0x90EE90))
 *         .build();
 * }</pre>
 *
 * <p>To tweak an existing theme without rebuilding it from scratch use
 * {@link #toBuilder()}, which copies every field into a new {@link Builder}
 * ready for modification.</p>
 *
 * <h2>Thread safety</h2>
 * <p>{@code MapTheme} instances are effectively immutable once constructed and
 * may be shared freely across threads and rendering layers.</p>
 *
 * <h2>Projection integration</h2>
 * <p>Each {@link IMapProjection} holds a reference to its active
 * {@code MapTheme}. The projection consults the theme when drawing the map
 * outline, graticule lines, and the ocean fill. Renderers such as
 * {@link CountryRenderer} and {@link CityPointRenderer} obtain the theme via
 * the projection so that all layers remain visually consistent.</p>
 */
public final class MapTheme {

    // -------------------------------------------------------------------------
    // Colors
    // -------------------------------------------------------------------------

    /** Background color drawn behind the entire map panel. */
    private final Color backgroundColor;

    /** Color used to fill the ocean region within the projection boundary. */
    private final Color oceanColor;

    /** Color used to fill land polygons (country shapes). */
    private final Color landColor;

    /** Color used to stroke the outer boundary of the projection domain. */
    private final Color outlineColor;

    /** Color used to draw graticule lines (parallels and meridians). */
    private final Color graticuleColor;

    /** Color used for text labels such as city names. */
    private final Color labelColor;

    /** Color used for city marker dots. Falls back to {@link #labelColor} if {@code null}. */
    private final Color cityColor;

    /** Color used to stroke country political borders. */
    private final Color borderColor;

    // -------------------------------------------------------------------------
    // Stroke widths / strokes
    // -------------------------------------------------------------------------

    /** Width in points of the projection boundary stroke. */
    private final float outlineStrokeWidth;

    /** Width in points of the graticule line stroke. */
    private final float graticuleStrokeWidth;

    /** Stroke used to draw country political borders. */
    private final Stroke borderStroke;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    private MapTheme(Builder b) {
        this.backgroundColor    = b.backgroundColor;
        this.oceanColor         = b.oceanColor;
        this.landColor          = b.landColor;
        this.outlineColor       = b.outlineColor;
        this.graticuleColor     = b.graticuleColor;
        this.labelColor         = b.labelColor;
        this.cityColor          = b.cityColor;
        this.borderColor        = b.borderColor;
        this.borderStroke       = b.borderStroke;
        this.outlineStrokeWidth    = b.outlineStrokeWidth;
        this.graticuleStrokeWidth  = b.graticuleStrokeWidth;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Returns the background color drawn behind the entire map panel.
     *
     * @return background {@link Color}; never {@code null}
     */
    public Color getBackgroundColor() { return backgroundColor; }

    /**
     * Returns the color used to fill the ocean region inside the projection
     * boundary.
     *
     * @return ocean fill {@link Color}; never {@code null}
     */
    public Color getOceanColor() { return oceanColor; }

    /**
     * Returns the color used to fill land (country) polygons.
     *
     * @return land fill {@link Color}; never {@code null}
     */
    public Color getLandColor() { return landColor; }

    /**
     * Returns the color used to stroke the outer boundary of the projection
     * domain (e.g., the bounding rectangle for Mercator or the ellipse for
     * Mollweide).
     *
     * @return outline stroke {@link Color}; never {@code null}
     */
    public Color getOutlineColor() { return outlineColor; }

    /**
     * Returns the color used to draw graticule lines (latitude parallels and
     * longitude meridians).
     *
     * <p>All projection implementations must use this value rather than
     * hardcoding a color so that theme switching takes full effect.</p>
     *
     * @return graticule {@link Color}; never {@code null}
     */
    public Color getGraticuleColor() { return graticuleColor; }

    /**
     * Returns the color used to render text labels such as city names.
     *
     * @return label text {@link Color}; never {@code null}
     */
    public Color getLabelColor() { return labelColor; }

    /**
     * Returns the color used to draw city marker dots.
     *
     * <p>{@link CityPointRenderer} uses this color for point markers and falls
     * back to {@link #getLabelColor()} if this value is {@code null}.</p>
     *
     * @return city marker {@link Color}, or {@code null} to use the label color
     */
    public Color getCityColor() { return cityColor; }

    /**
     * Returns the color used to stroke country political borders.
     *
     * @return border stroke {@link Color}; never {@code null}
     */
    public Color getBorderColor() { return borderColor; }

    /**
     * Returns the stroke width in points used for the projection boundary
     * outline.
     *
     * @return outline stroke width (&ge; 0)
     */
    public float getOutlineStrokeWidth() { return outlineStrokeWidth; }

    /**
     * Returns the stroke width in points used for graticule lines.
     *
     * @return graticule stroke width (&ge; 0)
     */
    public float getGraticuleStrokeWidth() { return graticuleStrokeWidth; }

    /**
     * Returns the {@link Stroke} used to draw country political borders.
     *
     * <p>Renderers that respect the theme (e.g., {@link CountryRenderer})
     * should use this stroke rather than constructing their own.</p>
     *
     * @return border {@link Stroke}; never {@code null}
     */
    public Stroke getBorderStroke() { return borderStroke; }

    // -------------------------------------------------------------------------
    // Builder cloning
    // -------------------------------------------------------------------------

    /**
     * Creates a new {@link Builder} pre-populated with every field from this
     * theme. Useful for creating a variation without specifying every value:
     * <pre>{@code
     * MapTheme brighter = existingTheme.toBuilder()
     *         .landColor(Color.GREEN)
     *         .build();
     * }</pre>
     *
     * @return a mutable {@link Builder} that reproduces this theme
     */
    public Builder toBuilder() {
        return new Builder()
                .backgroundColor(backgroundColor)
                .oceanColor(oceanColor)
                .landColor(landColor)
                .cityColor(cityColor)
                .outlineColor(outlineColor)
                .graticuleColor(graticuleColor)
                .labelColor(labelColor)
                .outlineStrokeWidth(outlineStrokeWidth)
                .graticuleStrokeWidth(graticuleStrokeWidth)
                .borderColor(borderColor)
                .borderStroke(borderStroke);
    }

    // -------------------------------------------------------------------------
    // Built-in presets
    // -------------------------------------------------------------------------

    /**
     * Returns a light-background theme suitable for daytime use or printing.
     *
     * <ul>
     *   <li>Background: near-white ({@code #F0F0F0})</li>
     *   <li>Ocean: light blue ({@code #CDE8FF})</li>
     *   <li>Land: pale green ({@code #E3F2D9})</li>
     *   <li>Cities: red</li>
     * </ul>
     *
     * @return a new light {@link MapTheme} instance
     */
    public static MapTheme light() {
        return new Builder()
                .backgroundColor(new Color(0xF0F0F0))
                .oceanColor(new Color(0xCDE8FF))
                .landColor(new Color(0xE3F2D9))
                .cityColor(Color.RED)
                .outlineColor(new Color(0x505050))
                .graticuleColor(new Color(0xB0B0B0))
                .labelColor(Color.DARK_GRAY)
                .borderColor(new Color(0x404040))
                .borderStroke(new BasicStroke(0.8f))
                .outlineStrokeWidth(1.5f)
                .graticuleStrokeWidth(0.8f)
                .build();
    }

    /**
     * Returns a dark-background theme suitable for low-light environments or
     * stylistic preference.
     *
     * <ul>
     *   <li>Background: near-black ({@code #20232A})</li>
     *   <li>Ocean: dark navy ({@code #273746})</li>
     *   <li>Land: dark blue-grey ({@code #34495E})</li>
     *   <li>Cities: cyan</li>
     * </ul>
     *
     * @return a new dark {@link MapTheme} instance
     */
    public static MapTheme dark() {
        return new Builder()
                .backgroundColor(new Color(0x20232A))
                .oceanColor(new Color(0x273746))
                .landColor(new Color(0x34495E))
                .cityColor(Color.cyan)
                .outlineColor(new Color(0xECF0F1))
                .graticuleColor(new Color(0x7F8C8D))
                .labelColor(new Color(0xECF0F1))
                .borderColor(new Color(0xFFFFFF))
                .borderStroke(new BasicStroke(0.9f))
                .outlineStrokeWidth(1.5f)
                .graticuleStrokeWidth(0.7f)
                .build();
    }

    /**
     * Returns a blue-toned theme with a nautical aesthetic.
     *
     * <ul>
     *   <li>Background: pale blue ({@code #E9F2FF})</li>
     *   <li>Ocean: medium blue ({@code #90CAF9})</li>
     *   <li>Land: desaturated teal ({@code #D8E5E9})</li>
     *   <li>Cities: deep navy ({@code #0D47A1})</li>
     * </ul>
     *
     * @return a new blue {@link MapTheme} instance
     */
    public static MapTheme blue() {
        return new Builder()
                .backgroundColor(new Color(0xE9F2FF))
                .oceanColor(new Color(0x90CAF9))
                .landColor(new Color(0xD8E5E9))
                .cityColor(new Color(0x0D47A1))
                .outlineColor(new Color(0x1E88E5))
                .graticuleColor(new Color(0x64B5F6))
                .labelColor(new Color(0x0D47A1))
                .borderColor(new Color(0x1E3A8A))
                .borderStroke(new BasicStroke(0.8f))
                .outlineStrokeWidth(1.4f)
                .graticuleStrokeWidth(0.8f)
                .build();
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /**
     * Mutable builder for {@link MapTheme}.
     *
     * <p>All setter methods return {@code this} to support fluent chaining.
     * Stroke widths are clamped to &ge; 0 to prevent negative values being
     * stored.</p>
     */
    public static final class Builder {

        private Color backgroundColor   = Color.WHITE;
        private Color oceanColor        = new Color(0xCCE5FF);
        private Color landColor         = new Color(0xE5F5E0);
        private Color outlineColor      = Color.DARK_GRAY;
        private Color graticuleColor    = Color.LIGHT_GRAY;
        private Color labelColor        = Color.BLACK;
        private Color cityColor         = Color.RED;
        private Color borderColor       = Color.BLACK;
        private Stroke borderStroke     = new BasicStroke(0.7f);
        private float outlineStrokeWidth   = 1.0f;
        private float graticuleStrokeWidth = 0.5f;

        /** Sets the panel background color. */
        public Builder backgroundColor(Color c) { this.backgroundColor = c; return this; }

        /** Sets the ocean fill color. */
        public Builder oceanColor(Color c) { this.oceanColor = c; return this; }

        /** Sets the land (country polygon) fill color. */
        public Builder landColor(Color c) { this.landColor = c; return this; }

        /** Sets the city marker dot color. */
        public Builder cityColor(Color c) { this.cityColor = c; return this; }

        /** Sets the projection boundary outline stroke color. */
        public Builder outlineColor(Color c) { this.outlineColor = c; return this; }

        /** Sets the graticule line color. */
        public Builder graticuleColor(Color c) { this.graticuleColor = c; return this; }

        /** Sets the text label color. */
        public Builder labelColor(Color c) { this.labelColor = c; return this; }

        /**
         * Sets the projection boundary outline stroke width.
         *
         * @param w stroke width in points; clamped to &ge; 0
         */
        public Builder outlineStrokeWidth(float w) { this.outlineStrokeWidth = Math.max(0f, w); return this; }

        /**
         * Sets the graticule line stroke width.
         *
         * @param w stroke width in points; clamped to &ge; 0
         */
        public Builder graticuleStrokeWidth(float w) { this.graticuleStrokeWidth = Math.max(0f, w); return this; }

        /** Sets the country political border stroke color. */
        public Builder borderColor(Color c) { this.borderColor = c; return this; }

        /** Sets the country political border {@link Stroke}. */
        public Builder borderStroke(Stroke s) { this.borderStroke = s; return this; }

        /**
         * Constructs an immutable {@link MapTheme} from the current builder
         * state.
         *
         * @return a new {@link MapTheme} instance
         */
        public MapTheme build() { return new MapTheme(this); }
    }
}
