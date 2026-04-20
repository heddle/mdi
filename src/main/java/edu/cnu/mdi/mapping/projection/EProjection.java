package edu.cnu.mdi.mapping.projection;

import edu.cnu.mdi.component.EnumComboBox;
import edu.cnu.mdi.mapping.MapConstants;
import edu.cnu.mdi.mapping.MapProjectionMenu;
import edu.cnu.mdi.mapping.MapView2D;

/**
 * Enumeration of supported map projection types.
 *
 * <p>Each constant carries a human-readable display name accessible via
 * {@link #getName()} and {@link #toString()}. The enum is the single source of
 * truth for which projections the application supports; adding a new projection
 * requires adding a constant here, a case in {@link ProjectionFactory}, and a
 * concrete {@link IMapProjection} implementation.</p>
 *
 * <p>The application's initial/default projection is
 * {@link MapConstants#DEFAULT_PROJECTION}, which is also what
 * {@link MapProjectionMenu} pre-selects and {@link MapView2D} activates on
 * construction. Keeping the default in {@link MapConstants} ensures both sites
 * stay in sync.</p>
 */
public enum EProjection {

    /**
     * Spherical Mercator projection (cylindrical, conformal).
     *
     * <p>Maps the whole globe except the extreme poles (clamped at ±89°) to a
     * rectangle. Longitude lines are equally spaced vertical lines; latitude
     * lines are horizontal but increasingly spread toward the poles. Area is
     * not preserved.</p>
     */
    MERCATOR("Mercator"),

    /**
     * Orthographic azimuthal projection (perspective view of one hemisphere).
     *
     * <p>Simulates the view of the Earth from an infinitely distant point in
     * space. Only the hemisphere facing the chosen center point is visible.
     * Area distortion increases toward the limb.</p>
     */
    ORTHOGRAPHIC("Orthographic"),

    /**
     * Mollweide equal-area pseudocylindrical projection.
     *
     * <p>Maps the entire globe to an ellipse whose area element is everywhere
     * proportional to the corresponding surface area on the sphere. Latitude
     * lines are straight and parallel; meridians are elliptical arcs.</p>
     */
    MOLLWEIDE("Mollweide"),

    /**
     * Lambert azimuthal equal-area projection (whole globe on a disk).
     *
     * <p>Maps the entire sphere to a disk of radius 2 (for a unit sphere).
     * Like the orthographic projection it is azimuthal, but unlike it the
     * whole globe is visible and area is preserved everywhere.</p>
     */
    LAMBERT_EQUAL_AREA("Lambert Azimuthal Equal-Area");

    // -------------------------------------------------------------------------

    private final String displayName;

    EProjection(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns a human-readable name suitable for use in UI labels and menus.
     *
     * @return the display name of this projection
     */
    public String getName() {
        return displayName;
    }

    /**
     * Returns the same value as {@link #getName()}, making this enum safe to
     * use directly in combo-box and list models that call {@code toString()}.
     *
     * @return the display name of this projection
     */
    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Creates an {@link EnumComboBox} pre-populated with every
     * {@code EProjection} value and labeled via {@link #projectionLabel(EProjection)}.
     *
     * <p>The initially selected item is {@code null} (no selection), which
     * callers may override after construction if a specific default is
     * needed.</p>
     *
     * @return a new {@link EnumComboBox} configured for {@code EProjection}
     */
    public static EnumComboBox<EProjection> createComboBox() {
        return new EnumComboBox<>(EProjection.class, null, EProjection::projectionLabel);
    }

    /**
     * Returns the short UI label used in combo boxes and menus for the given
     * projection. This label is intentionally kept concise compared with the
     * full {@link #getName()} value.
     *
     * @param p the projection whose label is requested; must not be
     *          {@code null}
     * @return a short, human-readable label string
     */
    public static String projectionLabel(EProjection p) {
        return switch (p) {
            case MERCATOR       -> "Mercator";
            case ORTHOGRAPHIC   -> "Orthographic";
            case MOLLWEIDE      -> "Mollweide";
            case LAMBERT_EQUAL_AREA -> "Lambert Equal-Area";
        };
    }
}
