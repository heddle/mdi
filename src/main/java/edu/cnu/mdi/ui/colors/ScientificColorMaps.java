package edu.cnu.mdi.ui.colors;

import java.awt.Color;

public class ScientificColorMaps {

    // --- VIRIDIS: The Standard ---
    // Best Use Case: General purpose. Best balance of perception and colorblind safety.
    public static final Color[] VIRIDIS = {
        new Color(68, 1, 84), new Color(59, 82, 139), new Color(33, 145, 140), 
        new Color(94, 201, 98), new Color(253, 231, 37)
    };

    // --- MAGMA: High Contrast ---
    // Best Use Case: Heatmaps where you want small peaks of data to "pop" out of a dark background.
    public static final Color[] MAGMA = {
        new Color(0, 0, 4), new Color(80, 18, 123), new Color(182, 54, 121), 
        new Color(251, 136, 97), new Color(252, 253, 191)
    };

    // --- INFERNO: Thermal Dynamics ---
    // Best Use Case: Ideal for representing physical heat or fire-related data.
    public static final Color[] INFERNO = {
        new Color(0, 0, 4), new Color(87, 15, 109), new Color(187, 55, 84), 
        new Color(249, 142, 9), new Color(252, 255, 164)
    };

    // --- PLASMA: Bold & Punchy ---
    // Best Use Case: Digital displays and artistic data-viz; has higher saturation than Viridis.
    public static final Color[] PLASMA = {
        new Color(13, 8, 135), new Color(126, 3, 160), new Color(203, 71, 120), 
        new Color(248, 149, 64), new Color(240, 249, 33)
    };

    // --- TURBO: The Improved "Heat Map" ---
    // Best Use Case: When you need the high-detail "Rainbow" look, but want to avoid the 
    // visual errors and lack of detail found in the old "Jet" or "Rainbow" maps.
    public static final Color[] TURBO = {
        new Color(48, 18, 59), new Color(70, 107, 227), new Color(40, 188, 235), 
        new Color(90, 238, 161), new Color(201, 239, 52), new Color(246, 121, 11), 
        new Color(122, 4, 3)
    };

    /**
     * Interpolates through a given color scale.
     * @param scale The Color[] array to use (e.g., ScientificColorMaps.VIRIDIS)
     * @param value A double between 0.0 and 1.0
     */
    public static Color getInterpolatedColor(Color[] scale, double value) {
        value = Math.max(0.0, Math.min(1.0, value));
        double pos = value * (scale.length - 1);
        int index = (int) pos;
        double fraction = pos - index;

        if (index >= scale.length - 1) return scale[scale.length - 1];

        Color c1 = scale[index];
        Color c2 = scale[index + 1];

        return new Color(
            (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * fraction),
            (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * fraction),
            (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * fraction)
        );
    }
}