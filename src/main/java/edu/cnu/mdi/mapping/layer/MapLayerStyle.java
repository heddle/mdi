package edu.cnu.mdi.mapping.layer;

import java.awt.Color;

/**
 * Mutable style values used by {@link MapLayerStyleDialog}.
 *
 * <p>
 * Only values corresponding to enabled capability bits are presented by the
 * dialog. The same object can therefore be used by several different map-layer
 * types.
 * </p>
 */
public class MapLayerStyle {

    private Color fillColor;
    private Color boundaryColor;
    private Color pointColor;
    private Color labelColor;

    private float lineWidth = 1.0f;
    private double pointSize = 4.0;
    private double opacity = 1.0;

    private boolean adaptive = true;
    private boolean drawLabels = true;
    private boolean drawOutline = true;

    private double latitudeStepDeg = 15.0;
    private double longitudeStepDeg = 15.0;
    
    /**
     * Minimum population required for a feature to be displayed.
     *
     * <p>
     * A nonpositive value disables population filtering.
     * </p>
     */
    private long minimumPopulation;
    
    /**
     * Creates an empty style with default numeric values.
     */
    public MapLayerStyle() {
    }

    /**
     * Copy constructor.
     *
     * @param source source style
     */
    public MapLayerStyle(MapLayerStyle source) {
        if (source == null) {
            return;
        }

        fillColor = source.fillColor;
        boundaryColor = source.boundaryColor;
        pointColor = source.pointColor;
        labelColor = source.labelColor;

        lineWidth = source.lineWidth;
        pointSize = source.pointSize;
        opacity = source.opacity;
        
        adaptive = source.adaptive;
        drawLabels = source.drawLabels;
        drawOutline = source.drawOutline;

        latitudeStepDeg = source.latitudeStepDeg;
        longitudeStepDeg = source.longitudeStepDeg;
        
        minimumPopulation = source.minimumPopulation;
    }
    
    /**
     * Returns the minimum population threshold.
     *
     * @return minimum population, or a nonpositive value when filtering is disabled
     */
    public long getMinimumPopulation() {
        return minimumPopulation;
    }

    /**
     * Sets the minimum population threshold.
     *
     * @param minimumPopulation minimum population; negative values are stored as zero
     */
    public void setMinimumPopulation(long minimumPopulation) {
        this.minimumPopulation =
                Math.max(0L, minimumPopulation);
    }

    public Color getFillColor() {
        return fillColor;
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    public Color getBoundaryColor() {
        return boundaryColor;
    }

    public void setBoundaryColor(Color boundaryColor) {
        this.boundaryColor = boundaryColor;
    }

    public Color getPointColor() {
        return pointColor;
    }

    public void setPointColor(Color pointColor) {
        this.pointColor = pointColor;
    }

    public Color getLabelColor() {
        return labelColor;
    }

    public void setLabelColor(Color labelColor) {
        this.labelColor = labelColor;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(float lineWidth) {
        this.lineWidth = Math.max(0.1f, lineWidth);
    }

    public double getPointSize() {
        return pointSize;
    }

    public void setPointSize(double pointSize) {
        this.pointSize = Math.max(0.1, pointSize);
    }

    public double getOpacity() {
        return opacity;
    }

    public void setOpacity(double opacity) {
        this.opacity = Math.max(0.0, Math.min(1.0, opacity));
    }
    
    public boolean isAdaptive() {
        return adaptive;
    }

    public void setAdaptive(boolean adaptive) {
        this.adaptive = adaptive;
    }

    public boolean isDrawLabels() {
        return drawLabels;
    }

    public void setDrawLabels(boolean drawLabels) {
        this.drawLabels = drawLabels;
    }

    public boolean isDrawOutline() {
        return drawOutline;
    }

    public void setDrawOutline(boolean drawOutline) {
        this.drawOutline = drawOutline;
    }

    public double getLatitudeStepDeg() {
        return latitudeStepDeg;
    }

    public void setLatitudeStepDeg(double latitudeStepDeg) {
        this.latitudeStepDeg =
                Math.max(1.0 / 3600.0, latitudeStepDeg);
    }

    public double getLongitudeStepDeg() {
        return longitudeStepDeg;
    }

    public void setLongitudeStepDeg(double longitudeStepDeg) {
        this.longitudeStepDeg =
                Math.max(1.0 / 3600.0, longitudeStepDeg);
    }
}