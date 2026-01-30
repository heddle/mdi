package edu.cnu.mdi.ui.colors;

import java.awt.Color;

public enum ScientificColorMap {

    // --- VIRIDIS: The Standard ---
    // Best Use Case: General purpose. Best balance of perception and colorblind safety.
    VIRIDIS("Viridis", new Color[] {
            new Color(68, 1, 84),
            new Color(59, 82, 139),
            new Color(33, 145, 140),
            new Color(94, 201, 98),
            new Color(253, 231, 37)
    }),

    // --- MAGMA: High Contrast ---
    MAGMA("Magma", new Color[] {
            new Color(0, 0, 4),
            new Color(80, 18, 123),
            new Color(182, 54, 121),
            new Color(251, 136, 97),
            new Color(252, 253, 191)
    }),

    // --- INFERNO: Thermal Dynamics ---
    INFERNO("Inferno", new Color[] {
            new Color(0, 0, 4),
            new Color(87, 15, 109),
            new Color(187, 55, 84),
            new Color(249, 142, 9),
            new Color(252, 255, 164)
    }),

    // --- PLASMA: Bold & Punchy ---
    PLASMA("Plasma", new Color[] {
            new Color(13, 8, 135),
            new Color(126, 3, 160),
            new Color(203, 71, 120),
            new Color(248, 149, 64),
            new Color(240, 249, 33)
    }),

    // --- TURBO: The Improved "Heat Map" ---
    TURBO("Turbo", new Color[] {
            new Color(48, 18, 59),
            new Color(70, 107, 227),
            new Color(40, 188, 235),
            new Color(90, 238, 161),
            new Color(201, 239, 52),
            new Color(246, 121, 11),
            new Color(122, 4, 3)
    }),

    // --- PERCEPTUAL GRAYSCALE ---
    GRAYSCALE("Grayscale", new Color[] {
            new Color(0, 0, 0),
            new Color(64, 64, 64),
            new Color(128, 128, 128),
            new Color(192, 192, 192),
            new Color(255, 255, 255)
    });

    private final String _label;
    private final Color[] _scale;

    ScientificColorMap(String label, Color[] scale) {
        _label = label;
        _scale = scale;
    }

    @Override
    public String toString() {
        return _label;
    }

    /** The control points for this map (do not modify). */
    public Color[] scale() {
        return _scale.clone();
    }

    /** Interpolated color for value in [0,1]. */
    public Color colorAt(double value01) {
        return interpolate(_scale, value01);
    }

    /** Utility interpolation (shared by UI components). */
    public static Color interpolate(Color[] scale, double value01) {
        double v = Math.max(0.0, Math.min(1.0, value01));
        double pos = v * (scale.length - 1);
        int index = (int) pos;
        double fraction = pos - index;

        if (index >= scale.length - 1) {
            return scale[scale.length - 1];
        }

        Color c1 = scale[index];
        Color c2 = scale[index + 1];

        return new Color(
                (int) (c1.getRed()   + (c2.getRed()   - c1.getRed())   * fraction),
                (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * fraction),
                (int) (c1.getBlue()  + (c2.getBlue()  - c1.getBlue())  * fraction)
        );
    }
}
