package edu.cnu.mdi.mapping.render;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.Objects;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.mapping.loader.Etopo5Loader;
import edu.cnu.mdi.mapping.projection.IMapProjection;

/**
 * Renders ETOPO5 terrain and bathymetry using the active map projection.
 *
 * <p>
 * The renderer samples the visible screen area rather than projecting the
 * complete ETOPO5 grid. Each screen block is converted back to geographic
 * coordinates, sampled from the ETOPO5 data set, and filled with an elevation
 * or bathymetry color.
 * </p>
 *
 * <p>
 * The renderer owns display settings such as opacity and sampling step but does
 * not own an MDI layer. A {@code MapView2D} may place this renderer on any
 * ordinary {@code Layer}.
 * </p>
 */
public class Etopo5Renderer {

    /** Default terrain opacity. */
    public static final float DEFAULT_OPACITY = 0.75f;

    /** Default screen-space sampling step in pixels. */
    public static final int DEFAULT_STEP = 2;

    /** Loaded ETOPO5 data. */
    private final Etopo5Loader loader;

    /** Terrain opacity in the range [0, 1]. */
    private float opacity = DEFAULT_OPACITY;

    /** Screen-space sampling step in pixels. */
    private int step = DEFAULT_STEP;

    /**
     * Creates an ETOPO5 renderer.
     *
     * @param loader loaded ETOPO5 data; must not be {@code null}
     */
    public Etopo5Renderer(Etopo5Loader loader) {
        this.loader = Objects.requireNonNull(loader, "loader");
    }

    /**
     * Renders the visible portion of the ETOPO5 data.
     *
     * @param g2         graphics context
     * @param container  map container
     * @param projection active map projection
     */
    public void render(
            Graphics2D g2,
            IContainer container,
            IMapProjection projection) {

        if (g2 == null || container == null || projection == null) {
            return;
        }

        Shape mapClip = projection.createClipShape(container);
        if (mapClip == null) {
            return;
        }

        /*
         * Layer.draw(...) supplies a private Graphics2D copy, so changes to the
         * clip, composite, and color do not leak into later layers.
         */
        g2.clip(mapClip);
        g2.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, opacity));

        Rectangle componentBounds =
                container.getComponent().getBounds();

        /*
         * Component bounds may be expressed relative to the parent. Rendering
         * coordinates are local to the component.
         */
        componentBounds.x = 0;
        componentBounds.y = 0;

        Rectangle drawBounds =
                mapClip.getBounds().intersection(componentBounds);

        Rectangle graphicsClip = g2.getClipBounds();
        if (graphicsClip != null) {
            drawBounds = drawBounds.intersection(graphicsClip);
        }

        if (drawBounds.isEmpty()) {
            return;
        }

        Point screen = new Point();
        Point2D.Double world = new Point2D.Double();
        Point2D.Double latLon = new Point2D.Double();

        int xMax = drawBounds.x + drawBounds.width;
        int yMax = drawBounds.y + drawBounds.height;

        for (int y = drawBounds.y; y < yMax; y += step) {
            for (int x = drawBounds.x; x < xMax; x += step) {

                screen.setLocation(
                        x + step / 2,
                        y + step / 2);

                if (!mapClip.contains(screen)) {
                    continue;
                }

                container.localToWorld(screen, world);
                projection.latLonFromXY(latLon, world);

                if (!Double.isFinite(latLon.x)
                        || !Double.isFinite(latLon.y)
                        || !projection.isPointVisible(latLon)) {
                    continue;
                }

                double longitudeDeg = Math.toDegrees(latLon.x);
                double latitudeDeg = Math.toDegrees(latLon.y);

                double elevation =
                        loader.getInterpolatedElevationMeters(
                                latitudeDeg,
                                longitudeDeg);

                if (Double.isNaN(elevation)) {
                    continue;
                }

                g2.setColor(colorForElevation(elevation));
                g2.fillRect(x, y, step, step);
            }
        }
    }

    /**
     * Returns interpolated elevation or bathymetry.
     *
     * @param latitudeDeg  latitude in degrees
     * @param longitudeDeg longitude in degrees
     * @return elevation in metres, or {@code NaN} if unavailable
     */
    public double getElevation(
            double latitudeDeg,
            double longitudeDeg) {

        return loader.getInterpolatedElevationMeters(
                latitudeDeg,
                longitudeDeg);
    }

    /**
     * Returns the terrain opacity.
     *
     * @return opacity in the range [0, 1]
     */
    public float getOpacity() {
        return opacity;
    }

    /**
     * Sets the terrain opacity.
     *
     * @param opacity opacity in the range [0, 1]
     */
    public void setOpacity(float opacity) {
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity));
    }

    /**
     * Returns the screen-space sampling step.
     *
     * @return step in pixels
     */
    public int getStep() {
        return step;
    }

    /**
     * Sets the screen-space sampling step.
     *
     * <p>
     * Smaller values produce smoother terrain but require more projection and
     * elevation calculations.
     * </p>
     *
     * @param step step in pixels; must be positive
     */
    public void setStep(int step) {
        if (step < 1) {
            throw new IllegalArgumentException(
                    "ETOPO5 sampling step must be positive.");
        }

        this.step = step;
    }

    /**
     * Returns the display color for an elevation.
     */
    private static Color colorForElevation(double elevationMeters) {
        return (elevationMeters < 0.0)
                ? waterColor(elevationMeters)
                : landColor(elevationMeters);
    }

    /**
     * Returns a bathymetry color for a negative elevation.
     */
    private static Color waterColor(double elevationMeters) {
        double z = clamp(elevationMeters, -11000.0, 0.0);

        if (z < -6000.0) {
            return interpolate(
                    new Color(5, 20, 70),
                    new Color(15, 60, 130),
                    (z + 11000.0) / 5000.0);
        }

        if (z < -3000.0) {
            return interpolate(
                    new Color(15, 60, 130),
                    new Color(35, 105, 175),
                    (z + 6000.0) / 3000.0);
        }

        if (z < -1000.0) {
            return interpolate(
                    new Color(35, 105, 175),
                    new Color(90, 155, 205),
                    (z + 3000.0) / 2000.0);
        }

        return interpolate(
                new Color(90, 155, 205),
                new Color(185, 220, 240),
                (z + 1000.0) / 1000.0);
    }

    /**
     * Returns a terrain color for a non-negative elevation.
     */
    private static Color landColor(double elevationMeters) {
        double z = clamp(elevationMeters, 0.0, 9000.0);

        if (z < 500.0) {
            return interpolate(
                    new Color(80, 150, 80),
                    new Color(150, 190, 100),
                    z / 500.0);
        }

        if (z < 1500.0) {
            return interpolate(
                    new Color(150, 190, 100),
                    new Color(210, 185, 120),
                    (z - 500.0) / 1000.0);
        }

        if (z < 3000.0) {
            return interpolate(
                    new Color(210, 185, 120),
                    new Color(170, 120, 80),
                    (z - 1500.0) / 1500.0);
        }

        if (z < 6000.0) {
            return interpolate(
                    new Color(170, 120, 80),
                    new Color(190, 170, 150),
                    (z - 3000.0) / 3000.0);
        }

        return interpolate(
                new Color(190, 170, 150),
                new Color(245, 245, 245),
                (z - 6000.0) / 3000.0);
    }

    /**
     * Linearly interpolates between two colors.
     */
    private static Color interpolate(
            Color c0,
            Color c1,
            double t) {

        t = clamp(t, 0.0, 1.0);

        int red = (int) Math.round(
                c0.getRed() + t * (c1.getRed() - c0.getRed()));

        int green = (int) Math.round(
                c0.getGreen() + t * (c1.getGreen() - c0.getGreen()));

        int blue = (int) Math.round(
                c0.getBlue() + t * (c1.getBlue() - c0.getBlue()));

        return new Color(red, green, blue);
    }

    /**
     * Clamps a value to a closed interval.
     */
    private static double clamp(
            double value,
            double minimum,
            double maximum) {

        return Math.max(minimum, Math.min(maximum, value));
    }
}