package edu.cnu.mdi.splot.view;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Collection of ticks for an axis (major + minor).
 */
public final class TickSet {

    private final List<Tick> ticks;
    private final double min;
    private final double max;

    public TickSet(List<Tick> ticks, double min, double max) {
        this.ticks = Collections.unmodifiableList(Objects.requireNonNull(ticks, "ticks"));
        this.min = min;
        this.max = max;
    }

    public List<Tick> getTicks() {
        return ticks;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }
}

