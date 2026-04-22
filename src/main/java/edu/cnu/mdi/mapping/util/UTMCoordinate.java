package edu.cnu.mdi.mapping.util;

public class UTMCoordinate {
    public final double easting;
    public final double northing;
    public final int zone;
    public final char letter; // Latitude band
    private boolean ousideRange = false;

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
	 */
    public UTMCoordinate(boolean ousideRange) {
		this.easting = 0;
		this.northing = 0;
		this.zone = 0;
		this.letter = ' ';
		this.ousideRange = ousideRange;
	}

    @Override
    public String toString() {
    	return ousideRange ? "outside valid range" :
        String.format("%d%c %.2f E, %.2f N", zone, letter, easting, northing);
    }
}
