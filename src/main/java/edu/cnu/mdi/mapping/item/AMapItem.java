package edu.cnu.mdi.mapping.item;

import java.awt.Point;
import java.awt.geom.Point2D;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.mapping.container.MapContainer;

public abstract class AMapItem extends AItem {

	/**
	 * Constructs a new AMapItem with the given layer and key-value pairs.
	 * @param layer the layer this item belongs to; must not be {@code null}
	 * @param keyVals  alternating keys and values for this item; must be an even number of arguments, with keys as Strings
	 */
	public AMapItem(Layer layer, Object[] keyVals) {
		super(layer, keyVals);
	}
	
    /**
     * Return the world-coordinate focus of this item.
     *
     * <p>The focus is the item's conceptual center: for point-like items it
     * is their location; for polygon items it is typically the centroid.
     * Returns {@code null} if no focus has been set.</p>
     *
     * @return the world-coordinate focus, or {@code null}
     */
    public Point2D.Double getFocus() {
        return _focus;
    }

    /**
     * Set the lat-lon focus of this item.
     *
     * <p>Subclasses should override this to enforce any constraints on the
     * focus (e.g. ensuring it stays within the item's bounding geometry).
     * The base implementation simply stores the point.</p>
     *
     * @param latlon the new focus; may be {@code null} to clear. The point's x and y 
     * are interpreted as longitude and latitude in radians, respectively, 
     * with longitude wrapped to (-π, π].
     * 
     */
    public void setFocus(Point2D.Double latlon) {
        _focus = latlon;
    }

    /**
     * Return the screen-coordinate (pixel) location of this item's focus
     * point, or {@code null} if the focus is not set.
     *
     * @param container the container rendering this item (used for the
     *                  world-to-screen transform)
     * @return the pixel location of the focus, or {@code null}
     */
    @Override
    public Point getFocusPoint(IContainer container) {
    	if (container instanceof MapContainer) {
    		MapContainer mapContainer = (MapContainer) container;
    	    Point2D.Double latlon = getFocus();
    	    if (latlon == null) return null;
            Point pp = new Point();
    	    mapContainer.latLonToLocal(pp, latlon);
    	    return pp;
    	}
 
        return null;
    }

    /**
     * Hook called when geometry changes and the focus point should be
     * recomputed.
     *
     * <p>The default implementation is a no-op. Subclasses that maintain a
     * derived focus (e.g. a centroid) should override this to recompute it
     * from the current geometry.</p>
     */
    protected void updateFocus() {
        // default: nothing to do
    }


}
