package edu.cnu.mdi.mapping.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

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
}
