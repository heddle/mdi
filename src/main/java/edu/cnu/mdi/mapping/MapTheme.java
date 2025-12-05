package edu.cnu.mdi.mapping;

import java.awt.Color;

/**
 * Encapsulates the visual style used when rendering maps and graticules.
 * <p>
 * A {@code MapTheme} specifies colors and stroke widths for common
 * map elements such as:
 * <ul>
 *   <li>background (desktop or container fill)</li>
 *   <li>ocean / water</li>
 *   <li>land areas</li>
 *   <li>map outline (projection boundary)</li>
 *   <li>graticule lines (latitude/longitude grid)</li>
 *   <li>text labels</li>
 * </ul>
 * Instances are immutable and can be shared across views and projections.
 * Use the {@link MapTheme.Builder} to construct custom themes, or one of the
 * provided presets such as {@link #light()} or {@link #dark()}.
 */
public final class MapTheme {

    /** Overall background color (e.g., view or desktop background). */
    private final Color backgroundColor;

    /** Color used to fill oceans or water areas. */
    private final Color oceanColor;

    /** Color used to fill land areas. */
    private final Color landColor;

    /** Color used to draw the map outline (projection boundary). */
    private final Color outlineColor;

    /** Color used to draw latitude/longitude grid lines. */
    private final Color graticuleColor;

    /** Color used for text labels (lat/lon labels, titles, etc.). */
    private final Color labelColor;

    /** Stroke width (in pixels) for the map outline. */
    private final float outlineStrokeWidth;

    /** Stroke width (in pixels) for graticule lines. */
    private final float graticuleStrokeWidth;

    private MapTheme(Builder b) {
        this.backgroundColor = b.backgroundColor;
        this.oceanColor = b.oceanColor;
        this.landColor = b.landColor;
        this.outlineColor = b.outlineColor;
        this.graticuleColor = b.graticuleColor;
        this.labelColor = b.labelColor;
        this.outlineStrokeWidth = b.outlineStrokeWidth;
        this.graticuleStrokeWidth = b.graticuleStrokeWidth;
    }

    /**
     * Returns the background color, typically used to clear the view or
     * desktop behind the map.
     *
     * @return the background color
     */
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Returns the color used to fill oceans or water areas.
     *
     * @return the ocean color
     */
    public Color getOceanColor() {
        return oceanColor;
    }

    /**
     * Returns the color used to fill land areas.
     *
     * @return the land color
     */
    public Color getLandColor() {
        return landColor;
    }

    /**
     * Returns the color used to draw the map outline (projection boundary).
     *
     * @return the map outline color
     */
    public Color getOutlineColor() {
        return outlineColor;
    }

    /**
     * Returns the color used to draw graticule (latitude/longitude) lines.
     *
     * @return the graticule color
     */
    public Color getGraticuleColor() {
        return graticuleColor;
    }

    /**
     * Returns the color used for map labels (e.g., latitude/longitude text,
     * titles, annotations).
     *
     * @return the label color
     */
    public Color getLabelColor() {
        return labelColor;
    }

    /**
     * Returns the stroke width in pixels used for the map outline.
     *
     * @return the outline stroke width
     */
    public float getOutlineStrokeWidth() {
        return outlineStrokeWidth;
    }

    /**
     * Returns the stroke width in pixels used for graticule lines.
     *
     * @return the graticule stroke width
     */
    public float getGraticuleStrokeWidth() {
        return graticuleStrokeWidth;
    }

    /**
     * Creates a new builder pre-populated with this theme's values,
     * which can be modified to derive a new theme.
     *
     * @return a builder initialized from this theme
     */
    public Builder toBuilder() {
        return new Builder()
                .backgroundColor(backgroundColor)
                .oceanColor(oceanColor)
                .landColor(landColor)
                .outlineColor(outlineColor)
                .graticuleColor(graticuleColor)
                .labelColor(labelColor)
                .outlineStrokeWidth(outlineStrokeWidth)
                .graticuleStrokeWidth(graticuleStrokeWidth);
    }

    @Override
    public String toString() {
        return "MapTheme[" +
                "background=" + backgroundColor +
                ", ocean=" + oceanColor +
                ", land=" + landColor +
                ", outline=" + outlineColor +
                ", graticule=" + graticuleColor +
                ", label=" + labelColor +
                ", outlineStrokeWidth=" + outlineStrokeWidth +
                ", graticuleStrokeWidth=" + graticuleStrokeWidth +
                ']';
    }

    // ------------------------------------------------------------------------
    // Presets
    // ------------------------------------------------------------------------

    /**
     * Returns a light theme suitable for typical UI backgrounds.
     *
     * @return a light {@code MapTheme}
     */
    public static MapTheme light() {
        return new Builder()
                .backgroundColor(new Color(0xF0F0F0))
                .oceanColor(new Color(0xCDE8FF))
                .landColor(new Color(0xE3F2D9))
                .outlineColor(new Color(0x505050))
                .graticuleColor(new Color(0xB0B0B0))
                .labelColor(Color.DARK_GRAY)
                .outlineStrokeWidth(1.5f)
                .graticuleStrokeWidth(0.8f)
                .build();
    }

    /**
     * Returns a darker theme intended for use on dark UIs or to reduce
     * glare in low-light environments.
     *
     * @return a dark {@code MapTheme}
     */
    public static MapTheme dark() {
        return new Builder()
                .backgroundColor(new Color(0x20232A))
                .oceanColor(new Color(0x273746))
                .landColor(new Color(0x34495E))
                .outlineColor(new Color(0xECF0F1))
                .graticuleColor(new Color(0x7F8C8D))
                .labelColor(new Color(0xECF0F1))
                .outlineStrokeWidth(1.5f)
                .graticuleStrokeWidth(0.7f)
                .build();
    }

    /**
     * Returns a theme with a blue emphasis, suitable for ocean-centric
     * visualizations.
     *
     * @return a blue {@code MapTheme}
     */
    public static MapTheme blue() {
        return new Builder()
                .backgroundColor(new Color(0xE9F2FF))
                .oceanColor(new Color(0x90CAF9))
                .landColor(new Color(0xE8F5E9))
                .outlineColor(new Color(0x1E88E5))
                .graticuleColor(new Color(0x64B5F6))
                .labelColor(new Color(0x0D47A1))
                .outlineStrokeWidth(1.4f)
                .graticuleStrokeWidth(0.8f)
                .build();
    }

    // ------------------------------------------------------------------------
    // Builder
    // ------------------------------------------------------------------------

    /**
     * Builder for {@link MapTheme} instances. All properties have sensible
     * defaults, so you can modify only what you care about.
     */
    public static final class Builder {

        private Color backgroundColor = Color.WHITE;
        private Color oceanColor = new Color(0xCCE5FF);
        private Color landColor = new Color(0xE5F5E0);
        private Color outlineColor = Color.DARK_GRAY;
        private Color graticuleColor = Color.LIGHT_GRAY;
        private Color labelColor = Color.BLACK;

        private float outlineStrokeWidth = 1.0f;
        private float graticuleStrokeWidth = 0.5f;

        /**
         * Sets the background color.
         *
         * @param color the background color
         * @return this builder (for chaining)
         */
        public Builder backgroundColor(Color color) {
            this.backgroundColor = color;
            return this;
        }

        /**
         * Sets the ocean (water) fill color.
         *
         * @param color the ocean color
         * @return this builder (for chaining)
         */
        public Builder oceanColor(Color color) {
            this.oceanColor = color;
            return this;
        }

        /**
         * Sets the land fill color.
         *
         * @param color the land color
         * @return this builder (for chaining)
         */
        public Builder landColor(Color color) {
            this.landColor = color;
            return this;
        }

        /**
         * Sets the map outline color.
         *
         * @param color the outline color
         * @return this builder (for chaining)
         */
        public Builder outlineColor(Color color) {
            this.outlineColor = color;
            return this;
        }

        /**
         * Sets the graticule line color.
         *
         * @param color the graticule color
         * @return this builder (for chaining)
         */
        public Builder graticuleColor(Color color) {
            this.graticuleColor = color;
            return this;
        }

        /**
         * Sets the label color.
         *
         * @param color the label color
         * @return this builder (for chaining)
         */
        public Builder labelColor(Color color) {
            this.labelColor = color;
            return this;
        }

        /**
         * Sets the stroke width in pixels for the map outline.
         *
         * @param width outline stroke width (pixels, non-negative)
         * @return this builder (for chaining)
         */
        public Builder outlineStrokeWidth(float width) {
            this.outlineStrokeWidth = Math.max(0f, width);
            return this;
        }

        /**
         * Sets the stroke width in pixels for graticule lines.
         *
         * @param width graticule stroke width (pixels, non-negative)
         * @return this builder (for chaining)
         */
        public Builder graticuleStrokeWidth(float width) {
            this.graticuleStrokeWidth = Math.max(0f, width);
            return this;
        }

        /**
         * Builds a new immutable {@link MapTheme} instance.
         *
         * @return a new {@code MapTheme}
         */
        public MapTheme build() {
            return new MapTheme(this);
        }
    }
}
