package edu.cnu.mdi.geometry;

/**
 * Shared numerical constants and array utilities for the geometry package.
 * <p>
 * This class centralizes the small tolerance used throughout the package for
 * "effectively zero" comparisons, so that {@link Vector}, {@link Line},
 * {@link Plane}, and others share a single definition rather than each declaring
 * their own copy. It also hosts the general-purpose array arithmetic that is not
 * specific to three-dimensional geometry.
 * </p>
 * <p>
 * The class is non-instantiable and contains only static members.
 * </p>
 *
 * @author heddle
 */
public final class GeoUtil {

	/** Effectively zero for parallelism, degeneracy, and normalization tests. */
	public static final double TINY = 1.0e-20;

	/**
	 * Prevents instantiation.
	 */
	private GeoUtil() {
	}

	/**
	 * Tests whether a value is effectively zero.
	 *
	 * @param v the value to test
	 * @return {@code true} if {@code abs(v) < TINY}
	 */
	public static boolean tiny(double v) {
		return Math.abs(v) < TINY;
	}

	/**
	 * Multiplies every element of an arbitrary numeric vector by a scalar.
	 * <p>
	 * This utility is for array-based vector data, not necessarily for 3D geometry
	 * vectors.
	 * </p>
	 *
	 * @param vector the array to multiply
	 * @param scalar the scalar multiplier
	 * @return a new array containing the scaled values
	 * @throws NullPointerException if {@code vector} is {@code null}
	 */
	public static double[] scalarMultiply(double[] vector, double scalar) {
		double[] result = new double[vector.length];
		for (int i = 0; i < vector.length; i++) {
			result[i] = vector[i] * scalar;
		}
		return result;
	}

	/**
	 * Adds one or more array-based vectors element by element.
	 * <p>
	 * This utility is for array-based vector data, not necessarily for 3D geometry
	 * vectors. All arrays must have the same length.
	 * </p>
	 *
	 * @param vectors the arrays to add
	 * @return a new array containing the element-wise sum
	 * @throws IllegalArgumentException if no arrays are supplied or if the arrays do
	 *                                  not all have the same length
	 * @throws NullPointerException     if {@code vectors} or any array in it is
	 *                                  {@code null}
	 */
	public static double[] addVectors(double[]... vectors) {
		if (vectors.length == 0) {
			throw new IllegalArgumentException("At least one vector is required for addition.");
		}

		int length = vectors[0].length;
		for (double[] vector : vectors) {
			if (vector.length != length) {
				throw new IllegalArgumentException("All vectors must be of the same length.");
			}
		}

		double[] result = new double[length];
		for (double[] vector : vectors) {
			for (int i = 0; i < length; i++) {
				result[i] += vector[i];
			}
		}
		return result;
	}
}