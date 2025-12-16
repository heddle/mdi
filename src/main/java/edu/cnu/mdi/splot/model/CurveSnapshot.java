package edu.cnu.mdi.splot.model;

import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable snapshot of curve data.
 * <p>
 * Renderers should prefer working with snapshots so they have a stable view
 * even if the backing curve is dynamic.
 * </p>
 */
public final class CurveSnapshot implements ICurveData {

    private final String name;
    private final double[] x;
    private final double[] y;
    private final double[] sigmaX; // nullable
    private final double[] sigmaY; // nullable
    private final boolean[] valid; // nullable; if null, validity determined by NaN rule
    private final FitResult fitResult; // nullable

    private final Rectangle2D.Double dataBounds; // computed once

    public CurveSnapshot(String name,
                         double[] x,
                         double[] y,
                         double[] sigmaX,
                         double[] sigmaY,
                         boolean[] valid,
                         FitResult fitResult) {

        this.name = Objects.requireNonNull(name, "name");
        this.x = Objects.requireNonNull(x, "x").clone();
        this.y = Objects.requireNonNull(y, "y").clone();

        if (this.x.length != this.y.length) {
            throw new IllegalArgumentException("x and y must have same length");
        }

        if (sigmaX != null && sigmaX.length != this.x.length) {
            throw new IllegalArgumentException("sigmaX length must match x/y length");
        }
        if (sigmaY != null && sigmaY.length != this.x.length) {
            throw new IllegalArgumentException("sigmaY length must match x/y length");
        }
        if (valid != null && valid.length != this.x.length) {
            throw new IllegalArgumentException("valid length must match x/y length");
        }

        this.sigmaX = (sigmaX == null) ? null : sigmaX.clone();
        this.sigmaY = (sigmaY == null) ? null : sigmaY.clone();
        this.valid = (valid == null) ? null : valid.clone();
        this.fitResult = fitResult;

        this.dataBounds = computeBounds();
    }

    public String getName() {
        return name;
    }

    public FitResult getFitResult() {
        return fitResult;
    }

    @Override
    public int size() {
        return x.length;
    }

    @Override
    public double getX(int i) {
        return x[i];
    }

    @Override
    public double getY(int i) {
        return y[i];
    }

    @Override
    public boolean hasSigmaX() {
        return sigmaX != null;
    }

    @Override
    public boolean hasSigmaY() {
        return sigmaY != null;
    }

    @Override
    public double getSigmaX(int i) {
        if (sigmaX == null) throw new IllegalStateException("sigmaX not present");
        return sigmaX[i];
    }

    @Override
    public double getSigmaY(int i) {
        if (sigmaY == null) throw new IllegalStateException("sigmaY not present");
        return sigmaY[i];
    }

    @Override
    public boolean isValid(int i) {
        if (valid != null) {
            return valid[i];
        }
        return ICurveData.super.isValid(i);
    }

    /**
     * Bounds of the valid data points: minX..maxX and minY..maxY.
     * Invalid (masked) points are ignored.
     *
     * @return data bounds; if no valid points exist, returns an "empty" rectangle
     *         with NaN coordinates.
     */
    public Rectangle2D.Double getDataBounds() {
        return (Rectangle2D.Double) dataBounds.clone();
    }

    /** @return defensive copy of x */
    public double[] copyX() {
        return x.clone();
    }

    /** @return defensive copy of y */
    public double[] copyY() {
        return y.clone();
    }

    /** @return defensive copy of sigmaX or null */
    public double[] copySigmaX() {
        return (sigmaX == null) ? null : sigmaX.clone();
    }

    /** @return defensive copy of sigmaY or null */
    public double[] copySigmaY() {
        return (sigmaY == null) ? null : sigmaY.clone();
    }

    /** @return defensive copy of validity mask or null */
    public boolean[] copyValidMask() {
        return (valid == null) ? null : valid.clone();
    }

    private Rectangle2D.Double computeBounds() {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        boolean any = false;
        for (int i = 0; i < x.length; i++) {
            if (!isValid(i)) continue;
            double xi = x[i];
            double yi = y[i];
            if (Double.isNaN(xi) || Double.isNaN(yi)) continue;

            any = true;
            minX = Math.min(minX, xi);
            maxX = Math.max(maxX, xi);
            minY = Math.min(minY, yi);
            maxY = Math.max(maxY, yi);
        }

        if (!any) {
            return new Rectangle2D.Double(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        }
        return new Rectangle2D.Double(minX, minY, (maxX - minX), (maxY - minY));
    }

    @Override
    public String toString() {
        return "CurveSnapshot[name=" + name + ", n=" + x.length
                + ", sigmaX=" + (sigmaX != null) + ", sigmaY=" + (sigmaY != null)
                + ", fit=" + (fitResult != null) + "]";
    }
}
