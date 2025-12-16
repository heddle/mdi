package edu.cnu.mdi.splot.model;

import java.util.Objects;

/**
 * Simple metadata for an axis (label + unit). This is part of the data model
 * (semantic meaning), not a rendering concern (fonts, colors, ticks).
 */
public final class AxisMeta {

    /** Axis label (e.g., "Energy"). */
    private final String label;

    /** Unit string (e.g., "GeV"), or empty if unitless. */
    private final String unit;

    /**
     * Create axis metadata.
     *
     * @param label axis label (non-null; may be empty)
     * @param unit unit string (non-null; may be empty)
     */
    public AxisMeta(String label, String unit) {
        this.label = Objects.requireNonNull(label, "label");
        this.unit = Objects.requireNonNull(unit, "unit");
    }

    /** @return the axis label (non-null) */
    public String getLabel() {
        return label;
    }

    /** @return the unit string (non-null; may be empty) */
    public String getUnit() {
        return unit;
    }

    /**
     * @return a display label such as "Energy (GeV)" if unit is present, otherwise "Energy".
     */
    public String getDisplayLabel() {
        if (unit.isEmpty()) {
            return label;
        }
        if (label.isEmpty()) {
            return unit;
        }
        return label + " (" + unit + ")";
    }

    @Override
    public String toString() {
        return "AxisMeta[label=" + label + ", unit=" + unit + "]";
    }
}

