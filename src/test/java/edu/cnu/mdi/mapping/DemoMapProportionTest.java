package edu.cnu.mdi.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.awt.geom.Point2D;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.mapping.projection.EProjection;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.ContainerFactory;

/** The demo map keeps Mercator in proportion. */
class DemoMapProportionTest {

	// A map view with the demo's container but no toolbar, which needs a display.
	private static MapView2D mapView() {
		return new MapView2D(PropertyUtils.TITLE, "Proportions", PropertyUtils.VISIBLE, false,
				PropertyUtils.WIDTH, 400, PropertyUtils.HEIGHT, 300,
				PropertyUtils.CONTAINERFACTORY,
				(ContainerFactory) DemoMapView.ProportionedMapContainer::new);
	}

	/** Pixels per world unit, across and down. */
	private static double[] scales(DemoMapView.ProportionedMapContainer container) {
		container.setDirty(true);
		container.setAffineTransforms();
		Point origin = new Point();
		Point across = new Point();
		Point up = new Point();
		container.worldToLocal(origin, new Point2D.Double(0, 0));
		container.worldToLocal(across, new Point2D.Double(2, 0));
		container.worldToLocal(up, new Point2D.Double(0, 1));
		return new double[] { (across.x - origin.x) / 2.0, origin.y - up.y };
	}

	@Test
	void mercatorHasEqualScalesOnCanvasesOfDifferentShapes() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MapView2D view = mapView();
			var container = (DemoMapView.ProportionedMapContainer) view.getIContainer();
			double width = container.getWorldSystem().width;

			for (int[] size : new int[][] { { 1000, 560 }, { 600, 700 }, { 1600, 500 } }) {
				container.setBounds(0, 0, size[0], size[1]);
				double[] scale = scales(container);

				assertEquals(scale[0], scale[1], 1.0, size[0] + " x " + size[1]);
				assertEquals(width, container.getWorldSystem().width, 1.0e-9, "all longitudes still show");
				assertEquals(0, container.getWorldSystem().getCenterY(), 1.0e-9, "centered on the equator");
			}
		});
	}

	@Test
	void otherProjectionsStillFillTheCanvas() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MapView2D view = mapView();
			var container = (DemoMapView.ProportionedMapContainer) view.getIContainer();
			view.setProjection(EProjection.MOLLWEIDE);
			double height = container.getWorldSystem().height;

			container.setBounds(0, 0, 1000, 200);
			scales(container);

			assertEquals(height, container.getWorldSystem().height, 1.0e-9);
			view.setProjection(EProjection.MERCATOR);
			double[] scale = scales(container);
			assertTrue(Math.abs(scale[0] - scale[1]) <= 1.0, "in proportion again after switching back");
		});
	}
}
