package edu.cnu.mdi.mapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.cnu.mdi.view.AbstractViewInfo;

public class MapViewInfo extends AbstractViewInfo {

    @Override
    public String getTitle() {
        return "Sample 2D Map View";
    }

    @Override
    public String getPurpose() {
        return "This view provides a flattened, 2D representation of geospatial data using " +
               "configurable projections. It is designed for analyzing city density and " +
               "global coordinates.";
    }

    @Override
    public List<String> getUsageBullets() {
        return List.of(
            "Use the Toolbar to zoom, pan, and measure distances.",
            "Adjust the Minimum Population slider to filter visible cities.",
            "Toggle Map Theme to switch between Light, Dark, and Blue modes."
        );
    }

    @Override
    public Map<String, String> getKeyboardShortcuts() {
        Map<String, String> keys = new LinkedHashMap<>();
        keys.put("Scroll Wheel", "Zoom In/Out");
        keys.put("Right Click + Drag", "Pan Map");
        keys.put("Ctrl + R", "Reset Projection");
        return keys;
    }

    @Override
    public String getTechnicalNotes() {
        return "Rendering Engine: Mercator Projection (WGS84). Currently loading 1251 cities.";
    }
}