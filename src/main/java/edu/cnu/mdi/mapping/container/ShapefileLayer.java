package edu.cnu.mdi.mapping.container;

import java.awt.Graphics2D;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.mapping.shapefile.ShapeFeatureRenderer;

public class ShapefileLayer extends Layer {

    private ShapeFeatureRenderer renderer;

    public ShapefileLayer(
            IContainer container,
            String name,
            ShapeFeatureRenderer renderer) {

        super(container, name);
        this.renderer = renderer;
    }

    @Override
    public void draw(Graphics2D g, IContainer container) {
        if (!isVisible() || renderer == null) {
            return;
        }

        renderer.render((Graphics2D) g, container);
        super.draw(g, container);
    }

    public void setRenderer(ShapeFeatureRenderer renderer) {
        this.renderer = renderer;
    }
}