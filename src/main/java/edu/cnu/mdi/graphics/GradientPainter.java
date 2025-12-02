package edu.cnu.mdi.graphics;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

/**
 * Utility class providing methods for rendering shaded or three–dimensional
 * looking circular objects using {@link RadialGradientPaint}.
 * <p>
 * The main feature is a method that draws a filled circle with a configurable
 * highlight or shading mode, giving the appearance of depth. This type of
 * rendering is useful for UI elements, node visualization, particle markers,
 * or any graphical asset that benefits from a spherical or glossy appearance.
 * </p>
 *
 * <h2>Rendering Modes</h2>
 * <ul>
 *     <li><b>Mode 1</b> — Transparent highlight:
 *         <br>Creates a glossy effect by blending a transparent color at the
 *         highlight point into the base fill color.
 *     </li>
 *     <li><b>Mode 2</b> — White highlight:
 *         <br>Simulates a bright, specular reflection using pure white at the
 *         highlight point, transitioning into the base color.
 *     </li>
 *     <li><b>Mode 3</b> — Darker rim shading:
 *         <br>Produces a subtle “shadowed edge” effect by using a darker version
 *         of the fill color at the outer radius, giving a soft 3D feel.
 *     </li>
 * </ul>
 *
 * <p>
 * The class is declared {@code final} and has a private constructor because it
 * is intended solely as a static utility container.
 * </p>
 *
 * <p><b>Thread Safety:</b> This class is thread-safe as long as the caller
 * provides exclusive access to the {@link Graphics} context.</p>
 *
 * <p><b>Performance:</b> Creating {@link RadialGradientPaint} objects is relatively
 * lightweight and appropriate for per-frame drawing in moderately complex UIs.</p>
 */
public final class GradientPainter {

    /**
     * Private constructor prevents instantiation of utility class.
     */
    private GradientPainter() { }

    /**
     * Draws a shaded, gradient-filled circle (a “3D sphere” effect) at the
     * specified location using the supplied {@link Graphics} context.
     *
     * <p>The circle is rendered using a {@link RadialGradientPaint} with two
     * color stops. The highlight or shading behavior is determined by the
     * {@code mode} argument.</p>
     *
     * <h3>Mode Behavior</h3>
     * <ul>
     *     <li><b>1 — Transparent highlight:</b>
     *         <br>Uses the fill color at full opacity at the edges but uses a
     *         fully transparent version of the fill color at a highlight
     *         position offset toward the upper-left. This creates a soft
     *         glossy effect without introducing white.
     *     </li>
     *     <li><b>2 — White highlight:</b>
     *         <br>Uses white at the highlight focus and the fill color at the
     *         edge. This provides a bright, “shiny” appearance commonly used
     *         in UI bubble effects.
     *     </li>
     *     <li><b>3 — Darkened rim:</b>
     *         <br>Uses the fill color inside and a darker version at the rim,
     *         giving a shaded or dimly lit spherical appearance.
     *     </li>
     * </ul>
     *
     * @param g
     *        the graphics context to draw into. Must be an instance convertible
     *        to {@link Graphics2D}. The method casts it internally.
     *
     * @param radius
     *        the radius of the circle in pixels. The final diameter will be
     *        {@code 2 * radius}.
     *
     * @param fillColor
     *        the base color used for the sphere. Its alpha component is honored.
     *
     * @param frameColor
     *        an optional color used to draw the outline of the circle. If
     *        {@code null}, no outline is drawn.
     *
     * @param center
     *        the center of the circle in device (pixel) coordinates. The circle
     *        will extend {@code radius} pixels outward from this point.
     *
     * @param mode
     *        determines the shading/highlight style:
     *        <ul>
     *            <li>1 = transparent highlight</li>
     *            <li>2 = white highlight</li>
     *            <li>3 = darker rim</li>
     *        </ul>
     *
     * @throws NullPointerException
     *         if {@code g}, {@code fillColor}, or {@code center} is null
     *
     * @see RadialGradientPaint
     * @see Graphics2D#setPaint(java.awt.Paint)
     */
    public static void drawGradientCircle(Graphics g,
                                          float radius,
                                          Color fillColor,
                                          Color frameColor,
                                          Point2D.Float center,
                                          int mode) {

        Graphics2D g2d = (Graphics2D) g;

        // Focus point for the radial gradient highlight
        Point2D.Float focus;

        // Two-element gradient: inner color and outer color
        Color[] colors = new Color[2];

        // Distances represent the normalized positions (0.0 to 1.0)
        float[] dist = {0f, 1f};

        switch (mode) {
            case 1:
                // Transparent highlight based on same RGB values
                focus = new Point2D.Float(center.x - 0.5f * radius,
                                          center.y - 0.5f * radius);
                colors[0] = new Color(fillColor.getRed(),
                                      fillColor.getGreen(),
                                      fillColor.getBlue(),
                                      0);         // fully transparent
                colors[1] = new Color(fillColor.getRed(),
                                      fillColor.getGreen(),
                                      fillColor.getBlue(),
                                      255);       // fully opaque
                break;

            case 2:
                // White highlight against the main fill color
                focus = new Point2D.Float(center.x - 0.5f * radius,
                                          center.y - 0.5f * radius);
                colors[0] = Color.white;
                colors[1] = fillColor;
                break;

            default:
                // Darker rim shading
                focus = new Point2D.Float(center.x - 0.25f * radius,
                                          center.y - 0.25f * radius);
                colors[0] = fillColor;
                colors[1] = fillColor.darker();
                break;
        }

        // Create the radial gradient paint with no cycling behavior
        RadialGradientPaint rgp =
                new RadialGradientPaint(center, radius, focus, dist, colors,
                                        CycleMethod.NO_CYCLE);

        // Render the filled sphere
        g2d.setPaint(rgp);
        g2d.fill(new Ellipse2D.Double(center.getX() - radius,
                                      center.getY() - radius,
                                      radius * 2,
                                      radius * 2));

        // Optional outline
        if (frameColor != null) {
            g2d.setColor(frameColor);
            int width = (int) (2 * radius);
            g2d.drawOval((int) (center.x - radius),
                         (int) (center.y - radius),
                         width, width);
        }
    }
}
