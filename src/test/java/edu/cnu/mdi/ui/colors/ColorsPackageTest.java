package edu.cnu.mdi.ui.colors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.UIManager;
import javax.swing.BorderFactory;

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
		assertThrows(IllegalArgumentException.class, () -> bar.setTickLabels("only"));
		assertThrows(NullPointerException.class, () -> bar.setTickLabels("min", null, "max"));
		bar.setTickLabels("0", "0.5", "1");
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

	@Test
	void scaleBarHonorsBorderInsets() {
		ColorScaleBar bar = new ColorScaleBar(ScientificColorMap.TURBO);
		bar.setBorder(BorderFactory.createTitledBorder("Scale"));
		bar.setSize(200, 54);
		BufferedImage image = new BufferedImage(200, 54, BufferedImage.TYPE_INT_ARGB);
		var graphics = image.createGraphics();
		try {
			bar.paint(graphics);
		} finally {
			graphics.dispose();
		}
		Insets insets = bar.getInsets();
		int left = insets.left + 10;
		int right = bar.getWidth() - insets.right - 10;
		assertEquals(ScientificColorMap.TURBO.colorAt(1.0 / (right - left - 1)).getRGB(),
				image.getRGB(left + 1, insets.top + 4));
	}

	@Test
	void colorPanelPreferredSizeAccommodatesThePlatformChooser() {
		ColorPanel panel = new ColorPanel();
		Dimension preferred = panel.getPreferredSize();
		Dimension chooserPreferred = panel.colorChooser.getPreferredSize();
		Insets insets = panel.getInsets();

		assertEquals(Math.max(ColorPanel.minw, chooserPreferred.width + insets.left + insets.right),
				preferred.width);
		assertEquals(Math.max(ColorPanel.minh, chooserPreferred.height + insets.top + insets.bottom),
				preferred.height);
	}

	@Test
	void colorPanelOffersSwatchesAndPreciseRgbControls() {
		ColorPanel panel = new ColorPanel();
		Set<String> chooserNames = Stream.of(panel.colorChooser.getChooserPanels())
				.map(chooserPanel -> chooserPanel.getDisplayName())
				.collect(Collectors.toSet());

		assertEquals(Set.of(UIManager.getString("ColorChooser.swatchesNameText"),
				UIManager.getString("ColorChooser.rgbNameText")), chooserNames);
	}
}
