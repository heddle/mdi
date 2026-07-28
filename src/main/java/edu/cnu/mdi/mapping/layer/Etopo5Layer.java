package edu.cnu.mdi.mapping.layer;

import java.awt.Component;
import java.awt.Graphics2D;
import java.util.Objects;
import java.util.function.Supplier;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.mapping.projection.IMapProjection;
import edu.cnu.mdi.mapping.render.Etopo5Renderer;

/**
 * Layer that renders ETOPO5 terrain and bathymetry.
 *
 * <p>
 * Both the renderer and projection are obtained through suppliers because the
 * owning map view may install the renderer after the layer is constructed and
 * may replace the active projection later.
 * </p>
 */
@SuppressWarnings("serial")
public class Etopo5Layer extends Layer {

    /** Container that owns this layer. */
    private final IContainer container;

    /** Supplies the current ETOPO5 renderer. */
    private final Supplier<Etopo5Renderer> rendererSupplier;

    /** Supplies the current map projection. */
    private final Supplier<IMapProjection> projectionSupplier;

    /**
     * Creates an ETOPO5 layer.
     *
     * @param container          owning container
     * @param name               layer name
     * @param rendererSupplier   supplier of the current ETOPO5 renderer
     * @param projectionSupplier supplier of the current map projection
     */
    public Etopo5Layer(
            IContainer container,
            String name,
            Supplier<Etopo5Renderer> rendererSupplier,
            Supplier<IMapProjection> projectionSupplier) {

        super(container, name);

        this.container =
                Objects.requireNonNull(
                        container,
                        "container");

        this.rendererSupplier =
                Objects.requireNonNull(
                        rendererSupplier,
                        "rendererSupplier");

        this.projectionSupplier =
                Objects.requireNonNull(
                        projectionSupplier,
                        "projectionSupplier");
    }

    /**
     * Renders the ETOPO5 terrain using the current renderer and projection.
     */
    @Override
    public void beforeDraw(
            Graphics2D g2,
            IContainer container) {

        Etopo5Renderer renderer =
                rendererSupplier.get();

        IMapProjection projection =
                projectionSupplier.get();

        if (renderer != null && projection != null) {
            renderer.render(
                    g2,
                    container,
                    projection);
        }
    }

    /**
     * Opens the ETOPO5 style editor.
     */
    @Override
    public void edit(Component parentComponent) {
        Etopo5Renderer renderer =
                rendererSupplier.get();

        if (renderer == null) {
            return;
        }

        MapLayerStyle initial =
                new MapLayerStyle();

        initial.setOpacity(
                renderer.getOpacity());

        MapLayerStyle result =
                MapLayerStyleDialog.showDialog(
                        parentComponent,
                        "ETOPO5 Style",
                        MapLayerStyleBits.OPACITY,
                        initial);

        if (result == null) {
            return;
        }

        renderer.setOpacity(result.getOpacity());

        container.refresh();
    }
}