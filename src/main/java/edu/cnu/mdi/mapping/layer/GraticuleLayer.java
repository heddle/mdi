package edu.cnu.mdi.mapping.layer;

import java.awt.Component;
import java.awt.Graphics2D;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.mapping.render.GraticuleRenderer;

/**
 * Layer that renders and edits the standard map graticule.
 */
public class GraticuleLayer extends Layer {

    /** Container that owns this layer. */
    private final IContainer container;

    /** Supplies the current graticule renderer. */
    private final Supplier<GraticuleRenderer> rendererSupplier;

    /** Supplies the owning view's standard-graticule policy. */
    private final BooleanSupplier enabledSupplier;

    /**
     * Creates a graticule layer.
     *
     * @param container        owning container
     * @param name             layer name
     * @param rendererSupplier supplier of the current renderer
     * @param enabledSupplier  supplier of the standard-graticule policy
     */
    public GraticuleLayer(
            IContainer container,
            String name,
            Supplier<GraticuleRenderer> rendererSupplier,
            BooleanSupplier enabledSupplier) {

        super(container, name);

        this.container =
                Objects.requireNonNull(
                        container,
                        "container");

        this.rendererSupplier =
                Objects.requireNonNull(
                        rendererSupplier,
                        "rendererSupplier");

        this.enabledSupplier =
                Objects.requireNonNull(
                        enabledSupplier,
                        "enabledSupplier");
    }

    /**
     * Renders the graticule when permitted by the owning view.
     */
    @Override
    public void beforeDraw(
            Graphics2D g2,
            IContainer container) {

        if (!enabledSupplier.getAsBoolean()) {
            return;
        }

        GraticuleRenderer renderer =
                rendererSupplier.get();

        if (renderer != null) {
            renderer.render(g2, container);
        }
    }

    /**
     * Opens the graticule editor.
     */
    @Override
    public void edit(Component parentComponent) {
        GraticuleRenderer renderer =
                rendererSupplier.get();

        if (renderer == null) {
            return;
        }

        MapLayerStyle initial =
                new MapLayerStyle();

        initial.setLabelColor(
                renderer.getLabelColor());

        initial.setAdaptive(
                renderer.isAdaptive());

        initial.setDrawLabels(
                renderer.isDrawLabels());

        initial.setDrawOutline(
                renderer.isDrawOutline());

        initial.setLatitudeStepDeg(
                Math.toDegrees(
                        renderer.getLatitudeStepRad()));

        initial.setLongitudeStepDeg(
                Math.toDegrees(
                        renderer.getLongitudeStepRad()));

        long bits =
                MapLayerStyleBits.LABEL_COLOR
                        | MapLayerStyleBits.ADAPTIVE
                        | MapLayerStyleBits.DRAW_LABELS
                        | MapLayerStyleBits.DRAW_OUTLINE
                        | MapLayerStyleBits.LATITUDE_STEP
                        | MapLayerStyleBits.LONGITUDE_STEP;

        MapLayerStyle result =
                MapLayerStyleDialog.showDialog(
                        parentComponent,
                        "Graticule Style",
                        bits,
                        initial);

        if (result == null) {
            return;
        }

        renderer.setLabelColor(
                result.getLabelColor());

        renderer.setAdaptive(
                result.isAdaptive());

        renderer.setDrawLabels(
                result.isDrawLabels());

        renderer.setDrawOutline(
                result.isDrawOutline());

        renderer.setLatitudeStepDeg(
                result.getLatitudeStepDeg());

        renderer.setLongitudeStepDeg(
                result.getLongitudeStepDeg());

        container.refresh();
    }
}