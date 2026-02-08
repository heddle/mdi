package edu.cnu.mdi.mapping;

import java.util.LinkedHashMap;
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
               "configurable projections. It is designed for analyzing city density and global coordinates.";
    }

    @Override
    public String getUsage() {
        // You can return raw strings or simple HTML here
        return "<ul>" +
               "<li>Use the <b>Toolbar</b> to zoom, pan, and measure distances.</li>" +
               "<li>Adjust the <b>Minimum Population</b> slider to filter visible cities.</li>" +
               "<li>Toggle <b>Map Theme</b> to switch between Light, Dark, and Blue modes.</li>" +
               "</ul>";
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