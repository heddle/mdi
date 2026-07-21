package edu.cnu.mdi.view.demo.geoslice;

import java.util.List;

import edu.cnu.mdi.view.AbstractViewInfo;

/**
 * View information for the Geometry Slice Demo view.
 *
 * <p>
 * This information panel explains the purpose of the demo rather than the
 * implementation details of any one detector or engineering system. The view
 * is intended to show why a derived 2D slice of 3D geometry can be more useful
 * for inspection and feedback than a full 3D rendering.
 * </p>
 */
public class GeometrySliceViewInfo extends AbstractViewInfo {

    @Override
    public String getTitle() {
        return "Geometry Slice Demo View";
    }

    @Override
    public String getPurpose() {
        return """
                This view demonstrates how a two-dimensional MDI view can display
                a meaningful slice through a three-dimensional model. The purpose
                is not to make a 2D drawing look three-dimensional, but to show a
                derived cross section that is easier to inspect, measure, and
                interrogate than the full 3D object.

                The example is inspired by scientific detector displays, where a
                3D object may contain many layers, wires, surfaces, or volumes.
                A movable slicing plane can reveal exactly which parts of the
                model intersect the plane, turning a complex spatial structure
                into ordinary MDI items such as polygons, polylines, lines, and
                points.
                """;
    }

    @Override
    public List<String> getUsageBullets() {
        return List.of(
                "Use the slice control to change the azimuthal angle of the slicing plane.",
                "The displayed 2D objects represent intersections between the 3D model and the current slice plane.",
                "Polygon and polyline items show projected chamber or volume boundaries.",
                "Point items show projected wire crossings, sample points, or simulated hits.",
                "Use mouse feedback to inspect the derived 2D slice coordinates and, when available, the originating 3D object information.",
                "Use the mouse wheel to zoom the 2D slice, just as in other MDI container-backed views."
        );
    }

    @Override
    public String getTechnicalNotes() {
        return """
                The underlying model is three-dimensional, but the rendered view is
                intentionally two-dimensional. For a selected constant-phi plane,
                3D lines and surfaces are intersected with that plane. The resulting
                3D intersection points are then projected into 2D slice coordinates,
                typically using one coordinate along the beam or longitudinal axis
                and one coordinate measuring distance away from that axis within the
                slice plane.

                Once projected, the results are ordinary MDI items on ordinary MDI
                layers. This is the important design point: the geometry package
                computes the slice, while the existing item/layer/container system
                provides drawing, styling, feedback, selection, zooming, and view
                integration.
                """;
    }

    @Override
    public String getFooter() {
        return "MDI geometry demo: 3D model, 2D diagnostic slice.";
    }

    @Override
    protected String getAccentColorHex() {
        return "#2c7a7b";
    }
}