package edu.cnu.mdi.splot.model;

/**
 * Read-only access to curve data (x, y, optional sigmaX, optional sigmaY).
 * Implementations can be array-backed, computed, streaming, etc.
 *
 * Rendering and analysis code should generally depend on this interface.
 */
public interface ICurveData {

    /** @return number of points */
    int size();

    /** @return x coordinate for point i */
    double getX(int i);

    /** @return y coordinate for point i */
    double getY(int i);

    /**
     * @return true if sigmaX is available for this curve.
     */
    boolean hasSigmaX();

    /**
     * @return true if sigmaY is available for this curve.
     */
    boolean hasSigmaY();

    /**
     * @return sigmaX for point i (only valid if {@link #hasSigmaX()} is true).
     */
    double getSigmaX(int i);

    /**
     * @return sigmaY for point i (only valid if {@link #hasSigmaY()} is true).
     */
    double getSigmaY(int i);

    /**
     * Validity test for point i. Useful for masked points.
     * A renderer can skip invalid points, and a polyline renderer can break
     * a line when invalid points occur.
     *
     * Default implementation considers NaN values invalid.
     */
    default boolean isValid(int i) {
        double x = getX(i);
        double y = getY(i);
        return !(Double.isNaN(x) || Double.isNaN(y));
    }
}
