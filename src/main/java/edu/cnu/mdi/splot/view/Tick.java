package edu.cnu.mdi.splot.view;

/**
 * A single axis tick.
 */
public final class Tick {

    private final double value;
    private final String label;
    private final boolean major;

    public Tick(double value, String label, boolean major) {
        this.value = value;
        this.label = label;
        this.major = major;
    }

    public double getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public boolean isMajor() {
        return major;
    }
}
