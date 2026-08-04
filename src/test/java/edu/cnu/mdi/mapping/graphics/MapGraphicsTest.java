package edu.cnu.mdi.mapping.graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.mapping.projection.IMapProjection;
import edu.cnu.mdi.mapping.projection.MercatorProjection;
import edu.cnu.mdi.mapping.projection.MollweideProjection;
import edu.cnu.mdi.mapping.theme.MapTheme;

class MapGraphicsTest {

	@Test
	void seamCrossingsAreRefinedOnBothWrappedProjections() {
		assertRefinedCrossing(new MercatorProjection(MapTheme.light()));
		assertRefinedCrossing(new MollweideProjection(MapTheme.light()));
	}

	private static void assertRefinedCrossing(IMapProjection projection) {
		Point2D.Double before = point(108.0, -12.0);
		Point2D.Double after = point(114.0, -28.0);

		MapGraphics.SeamCrossing crossing =
				MapGraphics.refineSeamCrossing(projection, before, after);

		assertTrue(projection.crossesSeam(crossing.before().x, crossing.after().x));
		assertEquals(crossing.before().y, crossing.after().y, 1.0e-9);

		Point2D.Double beforeXy = new Point2D.Double();
		Point2D.Double afterXy = new Point2D.Double();
		projection.latLonToXY(crossing.before(), beforeXy);
		projection.latLonToXY(crossing.after(), afterXy);
		assertTrue(beforeXy.x * afterXy.x < 0.0);
		assertEquals(Math.abs(beforeXy.x), Math.abs(afterXy.x), 1.0e-10);
	}

	private static Point2D.Double point(double longitudeDegrees, double latitudeDegrees) {
		return new Point2D.Double(Math.toRadians(longitudeDegrees), Math.toRadians(latitudeDegrees));
	}
}
