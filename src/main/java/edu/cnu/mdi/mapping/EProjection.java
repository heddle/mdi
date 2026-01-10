package edu.cnu.mdi.mapping;

import edu.cnu.mdi.component.EnumComboBox;

/**
 * Enumeration of supported map projection types.
 * <p>
 * Each enum value has a human-readable display name accessible via
 * {@link #getName()}.
 * </p>
 */
public enum EProjection {

	/** Mercator projection (cylindrical, conformal). */
	MERCATOR("Mercator"),

	/** Orthographic projection (planar, perspective view of a hemisphere). */
	ORTHOGRAPHIC("Orthographic"),

	/** Mollweide equal-area pseudocylindrical projection. */
	MOLLWEIDE("Mollweide"),

	/** Lambert azimuthal equal-area projection (hemisphere). */
	LAMBERT_EQUAL_AREA("Lambert Azimuthal Equal-Area");

	private final String displayName;

	EProjection(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Returns a human-readable name for this projection type.
	 *
	 * @return the display name
	 */
	public String getName() {
		return displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}

	/**
	 * Creates an EnumComboBox for selecting EProjection values.
	 *
	 * @return a new EnumComboBox configured for EProjection
	 */
	public static EnumComboBox<EProjection> createComboBox() {
		return new EnumComboBox<>(EProjection.class, null, EProjection::projectionLabel);
	}

	public static String projectionLabel(EProjection p) {
		return switch (p) {
		case MERCATOR -> "Mercator";
		case ORTHOGRAPHIC -> "Orthographic";
		case MOLLWEIDE -> "Mollweide";
		case LAMBERT_EQUAL_AREA -> "Lambert Equal-Area";
		};
	}
}
