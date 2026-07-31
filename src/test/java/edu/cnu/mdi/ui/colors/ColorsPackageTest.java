package edu.cnu.mdi.ui.colors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class ColorsPackageTest {

    @Test
    void interpolationHandlesEndpointsAndValidatesInput() {
        Color[] scale = { Color.BLACK, Color.WHITE };
        assertEquals(Color.BLACK, ScientificColorMap.interpolate(scale, -1));
        assertEquals(Color.WHITE, ScientificColorMap.interpolate(scale, 2));
        assertEquals(new Color(127, 127, 127), ScientificColorMap.interpolate(scale, 0.5));
        assertThrows(IllegalArgumentException.class,
                () -> ScientificColorMap.interpolate(scale, Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> ScientificColorMap.interpolate(new Color[0], 0.5));
    }

    @Test
    void scaleBarDefensivelyCopiesFallbackScale() {
        Color[] scale = { Color.BLACK, Color.WHITE };
		ColorScaleBar bar = new ColorScaleBar(scale);
		scale[0] = Color.RED;
		assertThrows(NullPointerException.class, () -> bar.setLabels(null, "max"));
		bar.setSize(100, 50);
		BufferedImage image = new BufferedImage(100, 50, BufferedImage.TYPE_INT_ARGB);
		var graphics = image.createGraphics();
		try {
			bar.paint(graphics);
		} finally {
			graphics.dispose();
		}
		Color expected = ScientificColorMap.interpolate(
				new Color[] { Color.BLACK, Color.WHITE }, 1.0 / 80.0);
		assertEquals(expected.getRGB(), image.getRGB(11, 6));
    }

    @Test
    void x11LookupIsLocaleIndependent() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(new Color(240, 255, 255), X11Colors.getX11Color("AZURE"));
        } finally {
            Locale.setDefault(original);
        }
    }

	@Test
	void colorControlsHandleReversibleNoColorState() {
		ColorButton button = new ColorButton("Fill", null);
		assertEquals("x", button.getText());
		button.setColor(Color.BLUE);
		assertEquals("Fill", button.getText());

		assertDoesNotThrow(() -> new ColorLabel(Color.RED, null, null));
	}
}
