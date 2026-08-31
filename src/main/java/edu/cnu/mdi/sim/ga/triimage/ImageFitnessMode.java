package edu.cnu.mdi.sim.ga.triimage;

/** Fitness metrics available to the image-evolution demonstration. */
public enum ImageFitnessMode {
	/** Minimize channel-wise RGB mean squared error. */
	COLOR_MSE("Color MSE"),
	/** Combine RGB error with luminance-edge error to favor target contours. */
	LINE_AWARE("Line-aware");

	private final String label;
	ImageFitnessMode(String label) { this.label = label; }
	@Override public String toString() { return label; }
}
