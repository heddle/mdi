package edu.cnu.mdi.mapping.item;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

import javax.swing.ImageIcon;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.mapping.milsym.MilSymbolDescriptor;

/**
 * Map-native military symbol item.
 *
 * <p>The anchor position is stored geographically as longitude/latitude
 * in radians, so the item stays attached to the same location when the
 * map projection changes. The icon itself is drawn at a fixed pixel size
 * for readability.</p>
 */
public class MapMilSymbolItem extends MapPointItem {

    /** Default drawn size in pixels. */
    public static final int DEFAULT_DRAW_SIZE = 16;

    /** Symbol metadata. */
    private final MilSymbolDescriptor descriptor;

    /** Cached symbol image. */
    private final ImageIcon icon;

    /** Drawn size in pixels. */
    private int drawSize = DEFAULT_DRAW_SIZE;

    /**
     * Creates a new geolocated military symbol item.
     *
     * @param layer annotation layer
     * @param location geographic location in radians
     * @param descriptor symbol metadata
     * @param image rendered icon image
     */
    public MapMilSymbolItem(Layer layer, Point2D.Double location,
            MilSymbolDescriptor descriptor, ImageIcon icon) {
        super(layer, location);
        this.descriptor = descriptor;
        this.icon = icon;

        setDisplayName((descriptor == null) ? "MIL Symbol"
                : descriptor.getDisplayName());
        setSelectable(true);
        setDraggable(true);
        setRightClickable(true);
        setDeletable(true);
        setResizable(false);
        setRotatable(false);
    }

    /**
     * Gets the symbol descriptor.
     *
     * @return descriptor
     */
    public MilSymbolDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * Sets the on-screen icon size in pixels.
     *
     * @param drawSize new draw size, clamped to at least 8 pixels
     */
    public void setDrawSize(int drawSize) {
        this.drawSize = Math.max(8, drawSize);
        setDirty(true);
    }

    /**
     * Gets the on-screen icon size in pixels.
     *
     * @return draw size
     */
    public int getDrawSize() {
        return drawSize;
    }

    @Override
    public void drawItem(Graphics2D g2, IContainer container) {
        Point p = getFocusPoint(container);
        if (p == null) {
            return;
        }

        if (icon != null) {
            int half = drawSize / 2;
            g2.drawImage(icon.getImage(), p.x - half, p.y - half, drawSize, drawSize, null);

            if (isSelected()) {
                g2.drawRect(p.x - half - 2, p.y - half - 2,
                        drawSize + 4, drawSize + 4);
            }
        } else {
            super.drawItem(g2, container);
        }
    }

    @Override
    public Rectangle getBounds(IContainer container) {
        Point p = getFocusPoint(container);
        if (p == null) {
            return null;
        }
        int half = drawSize / 2;
        return new Rectangle(p.x - half, p.y - half, drawSize, drawSize);
    }

    @Override
    public boolean contains(IContainer container, Point screenPoint) {
        Rectangle r = getBounds(container);
        return (r != null) && r.contains(screenPoint);
    }
}