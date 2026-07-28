package edu.cnu.mdi.mapping.layer;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Objects;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.mapping.projection.IMapProjection;
import edu.cnu.mdi.mapping.shapefile.ShapeFeatureRenderer;
import edu.cnu.mdi.mapping.shapefile.ShapeFeatureStyle;
import edu.cnu.mdi.mapping.shapefile.ShapefileGeometryReader;

/**
 * MDI layer that hosts one shapefile renderer.
 *
 * <p>
 * The layer owns visibility, ordering, editing, and picking. The renderer
 * remains responsible only for projecting and drawing the shapefile geometry.
 * </p>
 */
@SuppressWarnings("serial")
public class ShapefileLayer extends Layer {

    /** Owning container. */
    private final IContainer container;

    /** Renderer hosted by this layer. */
    private final ShapeFeatureRenderer renderer;

    /**
     * Creates a shapefile layer.
     *
     * @param container owning container
     * @param name      display name
     * @param renderer  shapefile renderer
     */
    public ShapefileLayer(
            IContainer container,
            String name,
            ShapeFeatureRenderer renderer) {

        super(container, name);

        this.container =
                Objects.requireNonNull(container, "container");

        this.renderer =
                Objects.requireNonNull(renderer, "renderer");

        setEditable(true);
    }

    /**
     * Returns the hosted renderer.
     *
     * @return shapefile renderer
     */
    public ShapeFeatureRenderer getRenderer() {
        return renderer;
    }

    /**
     * Updates the renderer's active map projection.
     *
     * @param projection current projection
     */
    public void setProjection(IMapProjection projection) {
        renderer.setProjection(projection);
    }

    /**
     * Draws the shapefile geometry.
     */
    @Override
    public void beforeDraw(
            Graphics2D g2,
            IContainer container) {

        renderer.render(g2, container);
    }

    /**
     * Delegates shapefile hit testing when this layer is visible.
     *
     * @param point     local screen point
     * @param container rendering container
     * @return tooltip text, or {@code null}
     */
    public String pick(
            Point point,
            IContainer container) {

        return isVisible()
                ? renderer.pick(point, container)
                : null;
    }

    /**
     * Opens a geometry-appropriate shapefile style editor.
     */
    @Override
    public void edit(Component parentComponent) {
        ShapeFeatureStyle current =
                renderer.getStyle();

        ShapeFeatureStyle working =
                new ShapeFeatureStyle(current);

        MapLayerStyle initial =
                new MapLayerStyle();

        long bits;

        switch (renderer.getShapeType()) {

        case ShapefileGeometryReader.TYPE_POLYGON:
            initial.setFillColor(
                    current.getFillColor());

            initial.setBoundaryColor(
                    current.getStrokeColor());

            initial.setLineWidth(
                    current.getStrokeWidth());

            bits = MapLayerStyleBits.FILL_COLOR
                    | MapLayerStyleBits.BOUNDARY_COLOR
                    | MapLayerStyleBits.LINE_WIDTH;
            break;

        case ShapefileGeometryReader.TYPE_POLYLINE:
            initial.setBoundaryColor(
                    current.getStrokeColor());

            initial.setLineWidth(
                    current.getStrokeWidth());

            bits = MapLayerStyleBits.BOUNDARY_COLOR
                    | MapLayerStyleBits.LINE_WIDTH;
            break;

        case ShapefileGeometryReader.TYPE_POINT:
        case ShapefileGeometryReader.TYPE_MULTIPOINT:
            initial.setPointColor(
                    current.getPointColor());

            initial.setPointSize(
                    current.getPointRadius());

            initial.setLabelColor(
                    current.getLabelColor());

            /*
             * DRAW_LABELS is meaningful only when a DBF label field has
             * already been configured.
             */
            initial.setDrawLabels(
                    current.getLabelField() != null);

            bits = MapLayerStyleBits.POINT_COLOR
                    | MapLayerStyleBits.POINT_SIZE
                    | MapLayerStyleBits.LABEL_COLOR;

            if (current.getLabelField() != null) {
                bits |= MapLayerStyleBits.DRAW_LABELS;
            }
            break;

        default:
            return;
        }

        MapLayerStyle result =
                MapLayerStyleDialog.showDialog(
                        parentComponent,
                        getName() + " Style",
                        bits,
                        initial);

        if (result == null) {
            return;
        }

        switch (renderer.getShapeType()) {

        case ShapefileGeometryReader.TYPE_POLYGON:
            working.fillColor(
                    result.getFillColor());

            working.strokeColor(
                    result.getBoundaryColor());

            working.strokeWidth(
                    result.getLineWidth());
            break;

        case ShapefileGeometryReader.TYPE_POLYLINE:
            working.strokeColor(
                    result.getBoundaryColor());

            working.strokeWidth(
                    result.getLineWidth());
            break;

        case ShapefileGeometryReader.TYPE_POINT:
        case ShapefileGeometryReader.TYPE_MULTIPOINT:
            working.pointColor(
                    result.getPointColor());

            working.pointRadius(
                    result.getPointSize());

            working.labelColor(
                    result.getLabelColor());

            if (!result.isDrawLabels()) {
                working.labelField(null);
            }
            break;

        default:
            return;
        }

        renderer.setStyle(working);
        container.refresh();
    }
}