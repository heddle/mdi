package edu.cnu.mdi.mapping.layer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    private List<String> availableFeedbackFields = Collections.emptyList();
    private List<String> selectedFeedbackFields = Collections.emptyList();
    
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

        availableFeedbackFields = new ArrayList<>(source.availableFeedbackFields);
        selectedFeedbackFields = new ArrayList<>(source.selectedFeedbackFields);
    }

    /** @return immutable DBF fields available for feedback selection */
    public List<String> getAvailableFeedbackFields() {
        return Collections.unmodifiableList(availableFeedbackFields);
    }

    /** @param fields DBF fields available for feedback selection */
    public void setAvailableFeedbackFields(List<String> fields) {
        availableFeedbackFields = fields == null
                ? Collections.emptyList()
                : new ArrayList<>(fields);
    }

    /** @return immutable DBF fields selected for hit-test feedback */
    public List<String> getSelectedFeedbackFields() {
        return Collections.unmodifiableList(selectedFeedbackFields);
    }

    /** @param fields DBF fields selected for hit-test feedback */
    public void setSelectedFeedbackFields(List<String> fields) {
        selectedFeedbackFields = fields == null
                ? Collections.emptyList()
                : new ArrayList<>(fields);
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

    /** @return polygon fill color, or {@code null} when no fill is configured */
    public Color getFillColor() {
        return fillColor;
    }

    /** @param fillColor polygon fill color; {@code null} disables filling */
    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    /** @return line or polygon-boundary color */
    public Color getBoundaryColor() {
        return boundaryColor;
    }

    /** @param boundaryColor line or polygon-boundary color */
    public void setBoundaryColor(Color boundaryColor) {
        this.boundaryColor = boundaryColor;
    }

    /** @return point-marker color */
    public Color getPointColor() {
        return pointColor;
    }

    /** @param pointColor point-marker color */
    public void setPointColor(Color pointColor) {
        this.pointColor = pointColor;
    }

    /** @return feature-label color */
    public Color getLabelColor() {
        return labelColor;
    }

    /** @param labelColor feature-label color */
    public void setLabelColor(Color labelColor) {
        this.labelColor = labelColor;
    }

    /** @return line width in pixels */
    public float getLineWidth() {
        return lineWidth;
    }

    /** @param lineWidth line width in pixels; values below {@code 0.1} are clamped */
    public void setLineWidth(float lineWidth) {
        this.lineWidth = Math.max(0.1f, lineWidth);
    }

    /** @return point-marker radius in pixels */
    public double getPointSize() {
        return pointSize;
    }

    /** @param pointSize point-marker radius in pixels; values below {@code 0.1} are clamped */
    public void setPointSize(double pointSize) {
        this.pointSize = Math.max(0.1, pointSize);
    }

    /** @return layer opacity in the range {@code [0, 1]} */
    public double getOpacity() {
        return opacity;
    }

    /** @param opacity layer opacity; values are clamped to {@code [0, 1]} */
    public void setOpacity(double opacity) {
        this.opacity = Math.max(0.0, Math.min(1.0, opacity));
    }

    /** @return whether adaptive graticule spacing is enabled */
    public boolean isAdaptive() {
        return adaptive;
    }

    /** @param adaptive whether to adapt graticule spacing to the visible extent */
    public void setAdaptive(boolean adaptive) {
        this.adaptive = adaptive;
    }

    /** @return whether feature labels are drawn */
    public boolean isDrawLabels() {
        return drawLabels;
    }

    /** @param drawLabels whether feature labels should be drawn */
    public void setDrawLabels(boolean drawLabels) {
        this.drawLabels = drawLabels;
    }

    /** @return whether the projection or layer outline is drawn */
    public boolean isDrawOutline() {
        return drawOutline;
    }

    /** @param drawOutline whether the projection or layer outline should be drawn */
    public void setDrawOutline(boolean drawOutline) {
        this.drawOutline = drawOutline;
    }

    /** @return fixed latitude-line spacing in degrees */
    public double getLatitudeStepDeg() {
        return latitudeStepDeg;
    }

    /** @param latitudeStepDeg latitude-line spacing in degrees; clamped to at least one arc-second */
    public void setLatitudeStepDeg(double latitudeStepDeg) {
        this.latitudeStepDeg =
                Math.max(1.0 / 3600.0, latitudeStepDeg);
    }

    /** @return fixed longitude-line spacing in degrees */
    public double getLongitudeStepDeg() {
        return longitudeStepDeg;
    }

    /** @param longitudeStepDeg longitude-line spacing in degrees; clamped to at least one arc-second */
    public void setLongitudeStepDeg(double longitudeStepDeg) {
        this.longitudeStepDeg =
                Math.max(1.0 / 3600.0, longitudeStepDeg);
    }
}
