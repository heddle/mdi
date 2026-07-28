package edu.cnu.mdi.mapping.layer;

import java.awt.Component;
import java.awt.Graphics2D;
import java.util.Objects;
import java.util.function.Supplier;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.mapping.render.CountryRenderer;

/**
 * Layer that renders country polygon interiors.
 */
@SuppressWarnings("serial")
public class CountryFillLayer extends Layer {

	/** Container that owns this layer. */
    private final IContainer container;

    /** Supplies the current country renderer. */
    private final Supplier<CountryRenderer> rendererSupplier;

    public CountryFillLayer(
            IContainer container,
            String name,
            Supplier<CountryRenderer> rendererSupplier) {

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

    @Override
    public void beforeDraw(
            Graphics2D g2,
            IContainer container) {

        CountryRenderer renderer =
                rendererSupplier.get();

        if (renderer != null) {
            renderer.renderFill(g2, container);
        }
    }

    @Override
    public void edit(Component parentComponent) {
        CountryRenderer renderer =
                rendererSupplier.get();

        if (renderer == null) {
            return;
        }

        MapLayerStyle initial =
                new MapLayerStyle();

        initial.setFillColor(
                renderer.getFillColor());

        MapLayerStyle result =
                MapLayerStyleDialog.showDialog(
                        parentComponent,
                        "Country Fill Style",
                        MapLayerStyleBits.FILL_COLOR,
                        initial);

        if (result == null) {
            return;
        }

        renderer.setFillColor(
                result.getFillColor());

        container.refresh();
    }
}