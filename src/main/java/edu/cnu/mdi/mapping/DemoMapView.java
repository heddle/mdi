package edu.cnu.mdi.mapping;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.JCheckBox;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.rubberband.ARubberband;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.mapping.container.MapContainer;
import edu.cnu.mdi.mapping.loader.Etopo5Loader;
import edu.cnu.mdi.mapping.loader.GeoJsonCityLoader;
import edu.cnu.mdi.mapping.loader.GeoJsonCountryLoader;
import edu.cnu.mdi.mapping.loader.GeoJsonCountryLoader.CountryFeature;
import edu.cnu.mdi.mapping.milsym.NatoIconPicker;
import edu.cnu.mdi.util.Environment;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.ContainerFactory;

@SuppressWarnings("serial")
public class DemoMapView extends MapView2D {
	
	private boolean showEtopo5 = false;
	
	/**
	 * Etopo5Loader used to load the ETOPO5 elevation dataset
	 */
	private Etopo5Loader etopo5;

	/**
	 * Constructor for the DemoMapView class.
	 * Initializes the map view with default settings, loads ETOPO5 terrain data,
	 * country and city data from GeoJSON resources, and sets up a NATO icon picker.
	 * Also adds a checkbox to toggle the display of ETOPO5 terrain.
	 */
	public DemoMapView() {
		super(defaults());
		setBackground(java.awt.Color.BLACK);
		
		try {
			String resPrefix = Environment.MDI_RESOURCE_PATH;

			
			// ETOPO5 terrain demo — small enough to load from a single file
			etopo5 = Etopo5Loader.loadDefaultResource();

			// Countries from GeoJSON resource — small enough to load from a single file,
			// and the
			// population slider works when cities are loaded from GeoJSON, so use GeoJSON
			// for both
			List<CountryFeature> countries = GeoJsonCountryLoader
					.loadFromResourceStatic(resPrefix + MapResources.COUNTRIES_GEOJSON);
			setCountries(countries);

			// Cities — use GeoJSON so the population slider works
			setCities(GeoJsonCityLoader.loadFromResourceStatic(resPrefix + MapResources.CITIES_GEOJSON));

			NatoIconPicker picker = new NatoIconPicker();

			// addWestPanel uses a double-invokeLater to run after all
			// construction placement has settled.
			addWestPanel(picker);

		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//add the display etopo5 checkbox 
		if (controlPanel != null) {
			controlPanel.addCheckbox("Display ETOPO5", showEtopo5, e -> {
				handleEtopo5(e);
			});
		}
	}
	
	@Override
	public double getElevation(double lat, double lon) {
		if (etopo5 == null) {
			return Double.NaN;
		}
		return etopo5.getInterpolatedElevationMeters(lat, lon);
	}

	@Override
	protected void afterCountryDraw(Graphics2D g, IContainer container) {
		if (showEtopo5) {
			drawEtopo5(g, container);
		}
	}
	
	   /**
     * Draws ETOPO5 terrain and bathymetry over the visible part of the map.
     *
     * <p>
     * The loop is intentionally restricted to the intersection of the projection
     * clip, component bounds, and current graphics clip. Projection clip bounds can
     * become very large when zoomed in, so iterating over the raw map-clip bounds
     * would waste time checking many offscreen pixels.
     * </p>
     *
     * @param g         graphics context
     * @param container rendering container
     */
    private void drawEtopo5(Graphics2D g, IContainer container) {
        if (etopo5 == null || container == null) {
            return;
        }

        Shape mapClip = getProjection().createClipShape(container);
        if (mapClip == null) {
            return;
        }

        Shape oldClip = g.getClip();
        Composite oldComposite = g.getComposite();
        Color oldColor = g.getColor();

        try {
            g.clip(mapClip);
            
            g.setComposite(
                    AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, 0.75f));


            Rectangle visible = container.getComponent().getBounds();
            visible.x = 0;
            visible.y = 0;

            Rectangle drawBounds = mapClip.getBounds().intersection(visible);

            Rectangle graphicsClip = g.getClipBounds();
            if (graphicsClip != null) {
                drawBounds = drawBounds.intersection(graphicsClip);
            }

            if (drawBounds.isEmpty()) {
                return;
            }

            final int step = 2;

            Point screen = new Point();
            Point2D.Double world = new Point2D.Double();
            Point2D.Double latLon = new Point2D.Double();

            int xMax = drawBounds.x + drawBounds.width;
            int yMax = drawBounds.y + drawBounds.height;

            for (int y = drawBounds.y; y < yMax; y += step) {
                for (int x = drawBounds.x; x < xMax; x += step) {
                    screen.setLocation(x + step / 2, y + step / 2);

                    if (!mapClip.contains(screen)) {
                        continue;
                    }

                    container.localToWorld(screen, world);
                    getProjection().latLonFromXY(latLon, world);

                    if (!Double.isFinite(latLon.x) || !Double.isFinite(latLon.y)) {
                        continue;
                    }

                    if (!getProjection().isPointVisible(latLon)) {
                        continue;
                    }

                    double lonDeg = Math.toDegrees(latLon.x);
                    double latDeg = Math.toDegrees(latLon.y);

                    double elevation = etopo5.getInterpolatedElevationMeters(latDeg, lonDeg);
                    if (Double.isNaN(elevation)) {
                        continue;
                    }

                    g.setColor(etopo5Color(elevation));
                    g.fillRect(x, y, step, step);
                }
            }
        } finally {
            g.setClip(oldClip);
            g.setComposite(oldComposite);
            g.setColor(oldColor);
        }
    }
	
	private static Color etopo5Color(double elevationMeters) {
	    if (elevationMeters < 0.0) {
	        return waterColor(elevationMeters);
	    }

	    return landColor(elevationMeters);
	}
	
	private static Color waterColor(double elevationMeters) {
	    // Clamp to a useful bathymetry range.
	    // ETOPO5 has deeper points, but this gives a good visual spread.
	    double z = clamp(elevationMeters, -11000.0, 0.0);

	    if (z < -6000.0) {
	        return interpolate(
	                new Color(5, 20, 70),
	                new Color(15, 60, 130),
	                (z + 11000.0) / 5000.0);
	    }

	    if (z < -3000.0) {
	        return interpolate(
	                new Color(15, 60, 130),
	                new Color(35, 105, 175),
	                (z + 6000.0) / 3000.0);
	    }

	    if (z < -1000.0) {
	        return interpolate(
	                new Color(35, 105, 175),
	                new Color(90, 155, 205),
	                (z + 3000.0) / 2000.0);
	    }

	    return interpolate(
	            new Color(90, 155, 205),
	            new Color(185, 220, 240),
	            (z + 1000.0) / 1000.0);
	}
	
	private static Color landColor(double elevationMeters) {
	    // Clamp to a useful terrain range.
	    double z = clamp(elevationMeters, 0.0, 9000.0);

	    if (z < 500.0) {
	        return interpolate(
	                new Color(80, 150, 80),
	                new Color(150, 190, 100),
	                z / 500.0);
	    }

	    if (z < 1500.0) {
	        return interpolate(
	                new Color(150, 190, 100),
	                new Color(210, 185, 120),
	                (z - 500.0) / 1000.0);
	    }

	    if (z < 3000.0) {
	        return interpolate(
	                new Color(210, 185, 120),
	                new Color(170, 120, 80),
	                (z - 1500.0) / 1500.0);
	    }

	    if (z < 6000.0) {
	        return interpolate(
	                new Color(170, 120, 80),
	                new Color(190, 170, 150),
	                (z - 3000.0) / 3000.0);
	    }

	    return interpolate(
	            new Color(190, 170, 150),
	            new Color(245, 245, 245),
	            (z - 6000.0) / 3000.0);
	}
	
	private static Color interpolate(Color c0, Color c1, double t) {
	    t = clamp(t, 0.0, 1.0);

	    int r = (int) Math.round(c0.getRed()   + t * (c1.getRed()   - c0.getRed()));
	    int g = (int) Math.round(c0.getGreen() + t * (c1.getGreen() - c0.getGreen()));
	    int b = (int) Math.round(c0.getBlue()  + t * (c1.getBlue()  - c0.getBlue()));

	    return new Color(r, g, b);
	}

	private static double clamp(double value, double min, double max) {
	    return Math.max(min, Math.min(max, value));
	}
	
	// Handle the checkbox action event to toggle the display of ETOPO5 terrain
	private void handleEtopo5(ActionEvent e) {
		JCheckBox checkbox = (JCheckBox) e.getSource();
		showEtopo5 = checkbox.isSelected();
		refresh();
	}
	
	private static Object[] defaults() {
		ContainerFactory mapContainerFactory = MapContainer::new;

		return new Object[] {
				PropertyUtils.TITLE, "Sample 2D Map View",
				PropertyUtils.FRACTION, 0.6,
				PropertyUtils.BOXZOOMRBPOLICY, ARubberband.Policy.RECTANGLE,
				PropertyUtils.ASPECT, 1.2, 
				PropertyUtils.CONTAINERFACTORY, mapContainerFactory,
				PropertyUtils.TOOLBARBITS, ToolBits.MAPTOOLS | ToolBits.ZOOMTOOLS, 
				PropertyUtils.WHEELZOOM, true		};
	}


}
