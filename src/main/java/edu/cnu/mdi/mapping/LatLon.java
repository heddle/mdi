package edu.cnu.mdi.mapping;

import java.awt.geom.Point2D;

/**
 * Represents a mutable geographic coordinate using standard spherical
 * coordinates:
 * <ul>
 *   <li><b>λ (lambda)</b> — longitude in radians</li>
 *   <li><b>φ (phi)</b> — latitude in radians</li>
 * </ul>
 * <p>
 * This class provides setters for both components as well as convenience
 * methods for working in degrees. No range constraints are enforced unless
 * explicitly requested via {@link #normalize()}.
 * </p>
 */
public class LatLon {

    /** Longitude λ in radians. */
    private double lambda;

    /** Latitude φ in radians. */
    private double phi;

    /**
     * Creates a new {@code LatLon} at (λ = 0, φ = 0).
     */
    public LatLon() {
        this(0.0, 0.0);
    }

    /**
     * Creates a new {@code LatLon} with the given longitude and latitude
     * in radians.
     *
     * @param lambda longitude λ in radians
     * @param phi    latitude φ in radians
     */
    public LatLon(double lambda, double phi) {
        this.lambda = lambda;
        this.phi = phi;
    }

    /**
     * Returns the longitude λ in radians.
     *
     * @return the longitude in radians
     */
    public double lambda() {
        return lambda;
    }

    /**
     * Returns the latitude φ in radians.
     *
     * @return the latitude in radians
     */
    public double phi() {
        return phi;
    }

    /**
     * Sets the longitude λ in radians.
     *
     * @param lambda the longitude in radians
     */
    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

    /**
     * Sets the latitude φ in radians.
     *
     * @param phi the latitude in radians
     */
    public void setPhi(double phi) {
        this.phi = phi;
    }

    /**
     * Sets both longitude λ and latitude φ in radians.
     *
     * @param lambda longitude in radians
     * @param phi    latitude in radians
     */
    public void set(double lambda, double phi) {
        this.lambda = lambda;
        this.phi = phi;
    }

    /**
     * Returns the longitude in degrees.
     *
     * @return λ in degrees
     */
    public double lambdaDeg() {
        return Math.toDegrees(lambda);
    }

    /**
     * Returns the latitude in degrees.
     *
     * @return φ in degrees
     */
    public double phiDeg() {
        return Math.toDegrees(phi);
    }

    /**
     * Sets the longitude using degrees instead of radians.
     *
     * @param lambdaDeg longitude in degrees
     */
    public void setLambdaDeg(double lambdaDeg) {
        this.lambda = Math.toRadians(lambdaDeg);
    }

    /**
     * Sets the latitude using degrees instead of radians.
     *
     * @param phiDeg latitude in degrees
     */
    public void setPhiDeg(double phiDeg) {
        this.phi = Math.toRadians(phiDeg);
    }

    /**
     * Normalizes this coordinate such that longitude is wrapped to [-π, π)
     * and latitude is clamped to [-π/2, π/2].
     *
     * @return this same {@code LatLon}, for chaining
     */
    public LatLon normalize() {
        lambda = wrapLongitude(lambda);
        phi = clampLatitude(phi);
        return this;
    }

    /** Wraps longitude to [-π, π). */
    private static double wrapLongitude(double lon) {
        lon = ((lon + Math.PI) % (2 * Math.PI));
        if (lon < 0) lon += 2 * Math.PI;
        return lon - Math.PI;
    }

    /** Clamps latitude to [-π/2, π/2]. */
    private static double clampLatitude(double lat) {
        return Math.max(-Math.PI / 2, Math.min(Math.PI / 2, lat));
    }

    /**
     * Converts this coordinate to a {@link Point2D.Double} in the mapping
     * convention: longitude in {@code x}, latitude in {@code y}.
     *
     * @return a new {@link Point2D.Double}
     */
    public Point2D.Double toPoint2D() {
        return new Point2D.Double(lambda, phi);
    }

    @Override
    public String toString() {
        return String.format("LatLon[λ=%.6f rad, φ=%.6f rad]", lambda, phi);
    }
}
