package edu.cnu.mdi.mapping;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.style.LineStyle;
import edu.cnu.mdi.graphics.world.WorldGraphicsUtils;
import edu.cnu.mdi.graphics.world.WorldPolygon;

/**
 * Renders a collection of {@link GeoJsonCountryLoader.CountryFeature} instances
 * onto an {@link IContainer} using a supplied {@link IMapProjection} and
 * {@link MapTheme}.
 * <p>
 * This renderer uses a {@link CountryShapeCache} to store the projected
 * country polygons in world (projection XY) coordinates. At render time,
 * the cached world paths are transformed to screen space using the
 * container's {@code worldToLocal} transform so that zoom and pan are
 * automatically respected.
 * </p>
 * <p>
 * The same cached polygons are also used for hit-testing, enabling efficient
 * mouseover feedback (e.g., "which country is under the mouse?").
 * </p>
 */
public class CountryRenderer {

    /** Original country features as loaded from GeoJSON. */
    private final List<GeoJsonCountryLoader.CountryFeature> countryFeatures;

    /** Projection used to convert lon/lat into world XY coordinates. */
    private final IMapProjection projection;


    // Rendering flags
    private boolean fillLand = true;
    private boolean drawBorders = true;
    private boolean useAntialias = true;
    
	private List<CountryCache> _countryCache = new ArrayList<>();


    /**
     * Construct a new renderer for the given country features and projection.
     * The current theme is obtained from the supplied projection.
     *
     * @param countryFeatures the country features to render
     * @param projection      the map projection to use for rendering
     */
    public CountryRenderer(List<GeoJsonCountryLoader.CountryFeature> countryFeatures,
                                  IMapProjection projection) {
        this.countryFeatures = Objects.requireNonNull(countryFeatures, "countryFeatures");
        this.projection = Objects.requireNonNull(projection, "projection");
    }

    /**
     * Enable or disable filling country polygons with the land color.
     *
     * @param fillLand {@code true} to fill land areas; {@code false} to draw
     *                 only borders
     */
    public void setFillLand(boolean fillLand) {
        this.fillLand = fillLand;
    }

    /**
     * Enable or disable drawing country borders.
     *
     * @param drawBorders {@code true} to draw borders; {@code false} to skip
     *                    border strokes
     */
    public void setDrawBorders(boolean drawBorders) {
        this.drawBorders = drawBorders;
    }

    /**
     * Enable or disable antialiasing during rendering.
     *
     * @param useAntialias {@code true} to enable antialiasing;
     *                     {@code false} to use the existing hint
     */
    public void setUseAntialias(boolean useAntialias) {
        this.useAntialias = useAntialias;
    }


    /**
     * Render all configured country features onto the given graphics context
     * within the supplied container.
     *
     * @param g2        graphics context to draw into
     * @param container container providing the world-to-local transform
     */
    public void render(Graphics2D g2, IContainer container) {
        Objects.requireNonNull(g2, "g2");
        Objects.requireNonNull(container, "container");
        
        _countryCache.clear();

        Component comp = container.getComponent();
        int width = comp.getWidth();
        int height = comp.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        if (useAntialias) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }

        Rectangle2D xyBounds = projection.getXYBounds();
        if (xyBounds == null || xyBounds.isEmpty()) {
            resetAntialias(g2, oldAA);
            return;
        }

        Stroke oldStroke = g2.getStroke();
        Color oldColor = g2.getColor();

        Color landColor = fillLand ? projection.getTheme().getLandColor() : null;
        Color borderColor = drawBorders ? projection.getTheme().getBorderColor() : null;
        Stroke borderStroke = projection.getTheme().getBorderStroke();
        if (borderStroke == null) {
            borderStroke = new BasicStroke(0.5f);
        }

        for (GeoJsonCountryLoader.CountryFeature country : countryFeatures) {
            drawCountryShape(g2, container, country,
                    landColor, borderColor, borderStroke);
        }

        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
        resetAntialias(g2, oldAA);
    }


    /**
     * Draw a single cached country shape. The shape is stored in world
     * coordinates; this method converts it to a screen-space path using
     * the container's {@code worldToLocal} transform.
     *
     * @param g2           graphics context
     * @param container    container providing world-to-local transform
     * @param country      the country to draw
     * @param landColor    fill color for land
     * @param borderColor  stroke color for borders
     * @param borderStroke stroke used for borders
     */
    private void drawCountryShape(Graphics2D g2,
                                  IContainer container,
                                  GeoJsonCountryLoader.CountryFeature country,
                                  Color landColor,
                                  Color borderColor,
                                  Stroke borderStroke) {

		EProjection proj = projection.getProjection();

		for (List<Point2D.Double> ll : country.getPolygons()) {

			if ((proj == EProjection.MERCATOR) || (proj == EProjection.MOLLWEIDE)) {
				// skip antarctica
				if (country.getAdminName().toLowerCase().startsWith("antarc")) {
					continue;
				}
			}

			WorldPolygon oneSide = new WorldPolygon();
			WorldPolygon otherSide = new WorldPolygon();
			WorldPolygon currentPoly = oneSide;
			boolean first = true;
			double prevLon = 0;

			for (Point2D.Double lonLat : ll) {

				if (projection.isPointVisible(lonLat)) {

					Point2D.Double xy = new Point2D.Double();
					projection.latLonToXY(lonLat, xy);

					if (first) {
						first = false;
					}

					else {
						if (projection.crossesSeam(lonLat.x, prevLon)) {
							// switch sides
							if (currentPoly == oneSide) {
								currentPoly = otherSide;
							} else {
								currentPoly = oneSide;
							}
						}
					}
					currentPoly.addPoint(xy.x, xy.y);
					prevLon = lonLat.x;
				}

			}

			if (oneSide.npoints > 2) {
				WorldGraphicsUtils.drawWorldPolygon(g2, container, oneSide, landColor, borderColor, 0.5f);
			}
			if (otherSide.npoints > 2) {
				WorldGraphicsUtils.drawWorldPolygon(g2, container, otherSide, landColor, borderColor, 0.5f);
			}

			_countryCache.add(new CountryCache(country, oneSide, otherSide));
		}
	}

    /**
     * Perform a hit-test on the cached country shapes to determine which
     * country (if any) lies under the given mouse position.
     *
     * @param mouseLocal mouse position in the container's local coordinate space
     * @param container  container providing the local-to-world transform
     * @return the first {@link GeoJsonCountryLoader.CountryFeature} whose
     *         projected polygon contains the mouse position, or {@code null}
     *         if no country was hit
     */
    public GeoJsonCountryLoader.CountryFeature pickCountry(Point mouseLocal,
                                                           IContainer container) {
        Objects.requireNonNull(mouseLocal, "mouseLocal");
        Objects.requireNonNull(container, "container");
		Point2D.Double worldPt = new Point2D.Double();
		container.localToWorld(mouseLocal, worldPt);
      
        for (CountryCache cc : _countryCache) {
			if (cc.contains(worldPt, container)) {
				return cc.country;
			}
        }
        return null;
    }

    /**
     * Restore the antialiasing hint after rendering.
     *
     * @param g2    graphics context
     * @param oldAA previous antialiasing hint (may be {@code null})
     */
    private void resetAntialias(Graphics2D g2, Object oldAA) {
        if (oldAA != null) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        }
    }

    class CountryCache {
    	GeoJsonCountryLoader.CountryFeature country;
    	WorldPolygon oneSide;
    	WorldPolygon otherSide;
    	public CountryCache(GeoJsonCountryLoader.CountryFeature country, WorldPolygon oneSide, WorldPolygon otherSide) {
    		this.country = country;
			this.oneSide = oneSide;
			this.otherSide = otherSide;
    	}
    	
    	public boolean contains(Point2D.Double worldPt, IContainer container) {
			if (oneSide.npoints > 2) {
				if (oneSide.contains(worldPt.x, worldPt.y)) {
					return true;
				}
			}
			if (otherSide.npoints > 2) {
				if (otherSide.contains(worldPt.x, worldPt.y)) {
					return true;
				}
			}
			return false;
		}
    }

}
