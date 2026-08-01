package edu.cnu.mdi.mapping.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFormattedTextField;

import org.junit.jupiter.api.Test;

class MapLayerStyleTest {

    @Test
    void feedbackFieldListsAreDefensivelyCopied() {
        List<String> available = new ArrayList<>(List.of("NAME", "TYPE"));
        MapLayerStyle style = new MapLayerStyle();
        style.setAvailableFeedbackFields(available);
        style.setSelectedFeedbackFields(List.of("TYPE", "NAME"));
        available.add("CHANGED");

        MapLayerStyle copy = new MapLayerStyle(style);
        style.setSelectedFeedbackFields(List.of("NAME"));

        assertEquals(List.of("NAME", "TYPE"), copy.getAvailableFeedbackFields());
        assertEquals(List.of("TYPE", "NAME"), copy.getSelectedFeedbackFields());
    }

    @Test
    void labelFontSizeIsClampedAndCopied() {
        MapLayerStyle style = new MapLayerStyle();
        style.setLabelFontSize(13.5f);

        assertEquals(13.5f, new MapLayerStyle(style).getLabelFontSize());

        style.setLabelFontSize(2.0f);
        assertEquals(6.0f, style.getLabelFontSize());
        style.setLabelFontSize(100.0f);
        assertEquals(36.0f, style.getLabelFontSize());
    }

    @Test
    void numericEditorsAllowReplacingExistingText() throws Exception {
        JFormattedTextField decimal =
                MapLayerStyleDialog.createDoubleField(10.0, 6.0, 36.0);
        decimal.selectAll();
        decimal.replaceSelection("");
        assertEquals("", decimal.getText());
        decimal.replaceSelection("12.5");
        decimal.commitEdit();
        assertEquals(12.5, ((Number) decimal.getValue()).doubleValue());

        JFormattedTextField integer =
                MapLayerStyleDialog.createLongField(1_000_000L, 0L, Long.MAX_VALUE);
        integer.selectAll();
        integer.replaceSelection("250000");
        integer.commitEdit();
        assertEquals(250_000L, ((Number) integer.getValue()).longValue());
    }
}
