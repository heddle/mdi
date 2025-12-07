package edu.cnu.mdi.mapping;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Objects;

import edu.cnu.mdi.container.IContainer;

/**
 * Renders a collection of {@link GeoJsonCountryLoader.CountryFeature} instances
 * onto an {@link IContainer} using a supplied {@link IMapProjection} and
 * {@link MapTheme}.
 */
public class CountryFeatureRenderer {

	private final List<GeoJsonCountryLoader.CountryFeature> countryFeatures;
	private final IMapProjection projection;
	private final MapTheme mapTheme;

	// Rendering flags
	private boolean fillLand = true;
	private boolean drawBorders = true;
	private boolean useAntialias = true;

	public CountryFeatureRenderer(List<GeoJsonCountryLoader.CountryFeature> countryFeatures,
			IMapProjection projection) {
		this.countryFeatures = Objects.requireNonNull(countryFeatures, "countryFeatures");
		this.projection = Objects.requireNonNull(projection, "projection");
		this.mapTheme = this.projection.getTheme();
	}

	public void setFillLand(boolean fillLand) {
		this.fillLand = fillLand;
	}

	public void setDrawBorders(boolean drawBorders) {
		this.drawBorders = drawBorders;
	}

	public void setUseAntialias(boolean useAntialias) {
		this.useAntialias = useAntialias;
	}

	/**
	 * Render all configured country features onto the given graphics context within
	 * the supplied container.
	 */
	public void render(Graphics2D g2, IContainer container) {
		Objects.requireNonNull(g2, "g2");
		Objects.requireNonNull(container, "container");

		Component comp = container.getComponent(); // adjust if your IContainer differs
		int width = comp.getWidth();
		int height = comp.getHeight();
		if (width <= 0 || height <= 0) {
			return;
		}

		Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		if (useAntialias) {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}

		Rectangle2D xyBounds = projection.getXYBounds();
		if (xyBounds == null || xyBounds.isEmpty()) {
			resetAntialias(g2, oldAA);
			return;
		}

		Stroke oldStroke = g2.getStroke();
		Color oldColor = g2.getColor();

		Color landColor = mapTheme.getLandColor();
		Color borderColor = mapTheme.getBorderColor();
		Stroke borderStroke = mapTheme.getBorderStroke();
		if (borderStroke == null) {
			borderStroke = new BasicStroke(0.5f);
		}

		for (GeoJsonCountryLoader.CountryFeature feature : countryFeatures) {
			drawCountryFeature(g2, container, feature, landColor, borderColor, borderStroke);
		}

		g2.setStroke(oldStroke);
		g2.setColor(oldColor);
		resetAntialias(g2, oldAA);
	}

	private AffineTransform createWorldToScreenTransform(Rectangle2D xyBounds, int width, int height) {
		double worldWidth = xyBounds.getWidth();
		double worldHeight = xyBounds.getHeight();
		if (worldWidth <= 0.0 || worldHeight <= 0.0) {
			return new AffineTransform();
		}

		double margin = 0.05; // 5% margin
		double scaleX = (1.0 - 2 * margin) * width / worldWidth;
		double scaleY = (1.0 - 2 * margin) * height / worldHeight;
		double scale = Math.min(scaleX, scaleY);

		double centerX = xyBounds.getCenterX();
		double centerY = xyBounds.getCenterY();

		AffineTransform at = new AffineTransform();
		at.translate(width / 2.0, height / 2.0);
		at.scale(scale, -scale); // flip Y so north is up
		at.translate(-centerX, -centerY);

		return at;
	}

	private void drawCountryFeature(Graphics2D g2, IContainer container, GeoJsonCountryLoader.CountryFeature feature,
			Color landColor, Color borderColor, Stroke borderStroke) {

		for (List<Point2D.Double> ring : feature.getPolygons()) {
			if (ring.isEmpty()) {
				continue;
			}

			Path2D path = new Path2D.Double(Path2D.WIND_NON_ZERO);
			boolean hasPoint = false;
			Point screenPoint = new Point();

			for (Point2D.Double lonLatDeg : ring) {
				double lonDeg = lonLatDeg.x;
				double latDeg = lonLatDeg.y;

				double lonRad = Math.toRadians(lonDeg);
				double latRad = Math.toRadians(latDeg);

// lambda = longitude, phi = latitude
				LatLon ll = new LatLon(lonRad, latRad);

				XY xy = projection.latLonToXY(ll);
				if (xy == null || !projection.isPointOnMap(xy)) {
					continue; // skip points off the map (e.g., far side of Lambert)
				}

				container.worldToLocal(screenPoint, xy.x(), xy.y());
	
				if (!hasPoint) {
					path.moveTo(screenPoint.x, screenPoint.y);
					hasPoint = true;
				} else {
					path.lineTo(screenPoint.x, screenPoint.y);
				}
			}

// If no visible points in this ring, skip it entirely
			if (!hasPoint) {
				continue;
			}

			path.closePath();

			if (!path.getBounds2D().isEmpty()) {
				if (fillLand && landColor != null) {
					g2.setColor(landColor);
					g2.fill(path);
				}

				if (drawBorders && borderColor != null) {
					g2.setColor(borderColor);
					g2.setStroke(borderStroke);
					g2.draw(path);
				}
			}
		}
	}

	private void resetAntialias(Graphics2D g2, Object oldAA) {
		if (oldAA != null) {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
		}
	}
}
