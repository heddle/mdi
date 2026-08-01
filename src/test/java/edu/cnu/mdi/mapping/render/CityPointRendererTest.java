package edu.cnu.mdi.mapping.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.mapping.projection.MercatorProjection;
import edu.cnu.mdi.mapping.theme.MapTheme;
import edu.cnu.mdi.ui.fonts.Fonts;

class CityPointRendererTest {

    @Test
    void defaultLabelFontIsOnePointLargerThanTinyFont() {
        float tinySize = (Fonts.tinyFont != null)
                ? Fonts.tinyFont.getSize2D()
                : Fonts.plainFontDelta(-4).getSize2D();
        CityPointRenderer renderer = new CityPointRenderer(
                Collections.emptyList(),
                new MercatorProjection(MapTheme.light()));

        assertEquals(tinySize + 1.0f,
                renderer.getLabelFont().getSize2D());
    }

    @Test
    void labelSizeIsClampedAndCopiedWithStyle() {
        CityPointRenderer source = new CityPointRenderer(
                Collections.emptyList(),
                new MercatorProjection(MapTheme.light()));
        CityPointRenderer target = new CityPointRenderer(
                Collections.emptyList(),
                new MercatorProjection(MapTheme.dark()));

        source.setLabelFontSize(14.5f);
        target.copyStyleFrom(source);
        assertEquals(14.5f, target.getLabelFont().getSize2D());

        target.setLabelFontSize(100.0f);
        assertEquals(36.0f, target.getLabelFont().getSize2D());
    }
}
