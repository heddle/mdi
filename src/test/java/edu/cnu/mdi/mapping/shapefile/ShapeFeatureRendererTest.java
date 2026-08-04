package edu.cnu.mdi.mapping.shapefile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.mapping.projection.MercatorProjection;
import edu.cnu.mdi.mapping.theme.MapTheme;

class ShapeFeatureRendererTest {

    @Test
    @SuppressWarnings("deprecation")
    void tooltipFieldAliasesDelegateToFeedbackFieldApi() {
        ShapeFeatureStyle style = new ShapeFeatureStyle()
                .tooltipFields("NAME", "TYPE");

        assertEquals(List.of("NAME", "TYPE"), style.getFeedbackFields());
        assertEquals(style.getFeedbackFields(), style.getTooltipFields());

        style.feedbackFields("FULLNAME");
        assertEquals(List.of("FULLNAME"), style.getTooltipFields());
    }

    @Test
    void availablePropertyNamesPreserveDbfOrderAndIncludeUnion() {
        Map<String, String> firstProperties = new LinkedHashMap<>();
        firstProperties.put("LINEARID", "1");
        firstProperties.put("FULLNAME", "State Hwy 42");
        firstProperties.put("RTTYP", "S");

        Map<String, String> secondProperties = new LinkedHashMap<>();
        secondProperties.put("LINEARID", "2");
        secondProperties.put("FULLNAME", "State Hwy 67");
        secondProperties.put("MTFCC", "S1100");

        ShapeFeatureRenderer renderer = new ShapeFeatureRenderer(
                List.of(feature(firstProperties), feature(secondProperties)),
                new MercatorProjection(MapTheme.light()),
                new ShapeFeatureStyle());

        List<String> names = renderer.getAvailablePropertyNames();
        assertEquals(List.of("LINEARID", "FULLNAME", "RTTYP", "MTFCC"), names);
        assertThrows(UnsupportedOperationException.class, () -> names.add("OTHER"));
    }

    @Test
    void feedbackIncludesFieldNamesColorAndOneValuePerLine() {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("FULLNAME", "I-70");
        properties.put("RTTYP", "I");
        properties.put("EMPTY", "");

        ShapeFeature feature = feature(properties);
        ShapeFeatureStyle style = new ShapeFeatureStyle()
                .feedbackFields("FULLNAME", "EMPTY", "RTTYP");
        ShapeFeatureRenderer renderer = new ShapeFeatureRenderer(
                List.of(feature),
                new MercatorProjection(MapTheme.light()),
                style);

        assertEquals("$light green$FULLNAME: I-70\nRTTYP: I",
                renderer.buildFeedback(feature));
    }

    private static ShapeFeature feature(Map<String, String> properties) {
        return new ShapeFeature(
                ShapefileGeometryReader.TYPE_POLYLINE,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.unmodifiableMap(properties));
    }
}
