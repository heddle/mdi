package edu.cnu.mdi.mapping.layer;

import java.awt.Component;
import java.awt.Graphics2D;
import java.util.Objects;
import java.util.function.Supplier;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.mapping.render.CityPointRenderer;

/**
 * Layer that renders and edits city markers and labels.
 */
@SuppressWarnings("serial")
public class CityLayer extends Layer {

    /** Container that owns this layer. */
    private final IContainer container;

    /** Supplies the current city renderer. */
    private final Supplier<CityPointRenderer> rendererSupplier;

    /**
     * Creates a city layer.
     *
     * @param container        owning container
     * @param name             layer name
     * @param rendererSupplier supplier of the current city renderer
     */
    public CityLayer(
            IContainer container,
            String name,
            Supplier<CityPointRenderer> rendererSupplier) {

        super(container, name);

        this.container =
                Objects.requireNonNull(
                        container,
                        "container");

        this.rendererSupplier =
                Objects.requireNonNull(
                        rendererSupplier,
                        "rendererSupplier");
    }

    /**
     * Renders the current city markers and labels.
     */
    @Override
    public void beforeDraw(
            Graphics2D g2,
            IContainer container) {

        CityPointRenderer renderer =
                rendererSupplier.get();

        if (renderer != null) {
            renderer.render(g2, container);
        }
    }

    /**
     * Opens the city appearance and filtering editor.
     */
    @Override
    public void edit(Component parentComponent) {
        CityPointRenderer renderer =
                rendererSupplier.get();

        if (renderer == null) {
            return;
        }

        MapLayerStyle initial =
                new MapLayerStyle();

        initial.setPointColor(
                renderer.getPointColor());

        initial.setLabelColor(
                renderer.getLabelColor());

        initial.setLabelFontSize(
                renderer.getLabelFont().getSize2D());

        initial.setPointSize(
                renderer.getPointRadius());

        initial.setDrawLabels(
                renderer.isDrawLabels());

        initial.setMinimumPopulation(
                renderer.getMinPopulation());

        long bits =
                MapLayerStyleBits.POINT_COLOR
                        | MapLayerStyleBits.LABEL_COLOR
                        | MapLayerStyleBits.LABEL_FONT_SIZE
                        | MapLayerStyleBits.POINT_SIZE
                        | MapLayerStyleBits.DRAW_LABELS
                        | MapLayerStyleBits.MIN_POPULATION;

        MapLayerStyle result =
                MapLayerStyleDialog.showDialog(
                        parentComponent,
                        "City Style",
                        bits,
                        initial);

        if (result == null) {
            return;
        }

        renderer.setPointColor(
                result.getPointColor());

        renderer.setLabelColor(
                result.getLabelColor());

        renderer.setLabelFontSize(
                result.getLabelFontSize());

        renderer.setPointRadius(
                result.getPointSize());

        renderer.setDrawLabels(
                result.isDrawLabels());

        renderer.setMinPopulation(
                result.getMinimumPopulation());

        container.refresh();
    }
}
