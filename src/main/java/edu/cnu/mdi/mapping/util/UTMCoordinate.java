package edu.cnu.mdi.mapping.util;

/**
 * Immutable Universal Transverse Mercator coordinate.
 *
 * <p>Normal instances contain an easting, northing, longitudinal zone, and
 * latitude-band letter. An instance created with
 * {@link #UTMCoordinate(boolean)} can instead represent a geographic location
 * outside the latitude range supported by UTM.</p>
 */
public class UTMCoordinate {
    /** Easting in meters. */
    public final double easting;

    /** Northing in meters. */
    public final double northing;

    /** Longitudinal UTM zone in the range 1–60. */
    public final int zone;

    /** UTM latitude-band letter. */
    public final char letter;

    private boolean outsideRange = false;

    /**
	 * Constructs a UTM coordinate with the given parameters.
	 *
	 * @param e the easting value in meters
	 * @param n the northing value in meters
	 * @param z the UTM zone number (1-60)
	 * @param l the latitude band letter (C-X, excluding I and O)
	 */
    public UTMCoordinate(double e, double n, int z, char l) {
        this.easting = e;
        this.northing = n;
        this.zone = z;
        this.letter = l;
    }
    
    /**
	 * Constructs a UTM coordinate representing an out-of-range location.
	 *
	 * @param outsideRange whether this coordinate should be formatted as outside
	 *                     the supported UTM latitude range
	 */
    public UTMCoordinate(boolean outsideRange) {
		this.easting = 0;
		this.northing = 0;
		this.zone = 0;
		this.letter = ' ';
		this.outsideRange = outsideRange;
	}

    /**
     * Returns the coordinate in a compact human-readable form.
     *
     * @return formatted zone, easting, and northing, or
     *         {@code "outside valid range"}
     */
    @Override
    public String toString() {
    	return outsideRange ? "outside valid range" :
        String.format("%d%c %.2f E, %.2f N", zone, letter, easting, northing);
    }
}
