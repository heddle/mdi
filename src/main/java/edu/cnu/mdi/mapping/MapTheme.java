package edu.cnu.mdi.mapping;

import java.awt.Color;
import java.awt.Stroke;
import java.awt.BasicStroke;

/**
 * Encapsulates the visual style used when rendering maps and graticules.
 * <p>
 * A {@code MapTheme} specifies colors and stroke widths for common map
 * elements including background, ocean, land, graticules, labels, and now
 * political border colors and strokes.
 * <p>
 * Instances are immutable and can be shared across views.
 */
public final class MapTheme {

    // ------------------------------------------------------------------------
    // Colors
    // ------------------------------------------------------------------------

    private final Color backgroundColor;
    private final Color oceanColor;
    private final Color landColor;
    private final Color outlineColor;
    private final Color graticuleColor;
    private final Color labelColor;
    private final Color cityColor;

    /** Color used to draw political borders (country outlines). */
    private final Color borderColor;

    // ------------------------------------------------------------------------
    // Stroke widths and strokes
    // ------------------------------------------------------------------------

    private final float outlineStrokeWidth;
    private final float graticuleStrokeWidth;

    /** Stroke used for political borders. */
    private final Stroke borderStroke;

    private MapTheme(Builder b) {
        this.backgroundColor = b.backgroundColor;
        this.oceanColor = b.oceanColor;
        this.landColor = b.landColor;
        this.outlineColor = b.outlineColor;
        this.graticuleColor = b.graticuleColor;
        this.labelColor = b.labelColor;
        this.cityColor = b.cityColor;

        this.borderColor = b.borderColor;
        this.borderStroke = b.borderStroke;

        this.outlineStrokeWidth = b.outlineStrokeWidth;
        this.graticuleStrokeWidth = b.graticuleStrokeWidth;
    }

    // ------------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------------

    public Color getBackgroundColor() { return backgroundColor; }

    public Color getOceanColor() { return oceanColor; }

    public Color getLandColor() { return landColor; }

    public Color getOutlineColor() { return outlineColor; }

    public Color getGraticuleColor() { return graticuleColor; }

    public Color getLabelColor() { return labelColor; }

    public float getOutlineStrokeWidth() { return outlineStrokeWidth; }

    public float getGraticuleStrokeWidth() { return graticuleStrokeWidth; }

    /** Returns the political border color. */
    public Color getBorderColor() { return borderColor; }
    
    /** Returns the city point color. */
    public Color getCityColor() { return cityColor; }

    /** Returns the stroke used to draw political borders. */
    public Stroke getBorderStroke() { return borderStroke; }

    // ------------------------------------------------------------------------
    // Builder cloning
    // ------------------------------------------------------------------------

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

    // ------------------------------------------------------------------------
    // Presets
    // ------------------------------------------------------------------------

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

    public static MapTheme blue() {
        return new Builder()
                .backgroundColor(new Color(0xE9F2FF))
                .oceanColor(new Color(0x90CAF9))
                .landColor(new Color(0xE8F5E9))
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

    // ------------------------------------------------------------------------
    // Builder
    // ------------------------------------------------------------------------

    public static final class Builder {

        private Color backgroundColor = Color.WHITE;
        private Color oceanColor = new Color(0xCCE5FF);
        private Color landColor = new Color(0xE5F5E0);
        private Color outlineColor = Color.DARK_GRAY;
        private Color graticuleColor = Color.LIGHT_GRAY;
        private Color labelColor = Color.BLACK;
        private Color cityColor = Color.RED;

        private Color borderColor = Color.BLACK; // NEW DEFAULT
        private Stroke borderStroke = new BasicStroke(0.7f); // NEW DEFAULT

        private float outlineStrokeWidth = 1.0f;
        private float graticuleStrokeWidth = 0.5f;

        public Builder backgroundColor(Color c) { this.backgroundColor = c; return this; }
        public Builder oceanColor(Color c) { this.oceanColor = c; return this; }
        public Builder landColor(Color c) { this.landColor = c; return this; }
        public Builder cityColor(Color c) { this.cityColor = c; return this; }
        public Builder outlineColor(Color c) { this.outlineColor = c; return this; }
        public Builder graticuleColor(Color c) { this.graticuleColor = c; return this; }
        public Builder labelColor(Color c) { this.labelColor = c; return this; }

        public Builder outlineStrokeWidth(float w) { this.outlineStrokeWidth = Math.max(0f, w); return this; }
        public Builder graticuleStrokeWidth(float w) { this.graticuleStrokeWidth = Math.max(0f, w); return this; }

        /** Sets the political border color. */
        public Builder borderColor(Color c) { this.borderColor = c; return this; }

        /** Sets the stroke used to draw political boundaries. */
        public Builder borderStroke(Stroke s) { this.borderStroke = s; return this; }

        public MapTheme build() { return new MapTheme(this); }
    }
}
