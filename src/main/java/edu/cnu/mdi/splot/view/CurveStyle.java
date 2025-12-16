package edu.cnu.mdi.splot.view;

import java.util.Objects;

/** Rendering style for a single curve (view-layer only). */
public final class CurveStyle {
    private final CurveDrawMode mode;
    private final int pointSize;

    public CurveStyle(CurveDrawMode mode, int pointSize) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.pointSize = Math.max(2, pointSize);
    }

    public CurveDrawMode getMode() {
        return mode;
    }

    public int getPointSize() {
        return pointSize;
    }

    public static CurveStyle lines() { return new CurveStyle(CurveDrawMode.LINES, 6); }
    public static CurveStyle points(int size) { return new CurveStyle(CurveDrawMode.POINTS, size); }
    public static CurveStyle linesAndPoints(int size) { return new CurveStyle(CurveDrawMode.LINES_AND_POINTS, size); }
}
