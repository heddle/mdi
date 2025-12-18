package edu.cnu.mdi.item;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.world.WorldGraphicsUtils;

/**
 * A simple {@link AItem} representing a world-coordinate line segment.
 * <p>
 * The line geometry is stored in {@link #_line} (world coordinates). Rendering
 * and hit-testing are performed in screen space via the supplied
 * {@link IContainer} transforms.
 * </p>
 * <p>
 * This item supports {@link ItemModification.Type#DRAG} and
 * {@link ItemModification.Type#RESIZE} modifications. Rotation is currently
 * a no-op (consistent with the prior implementation).
 * </p>
 */
public class LineItem extends AItem {

    /** Default pick tolerance in pixels (screen space). */
    private static final double DEFAULT_PICK_TOLERANCE_PX = 8.0;

    /**
     * Create a world line item.
     *
     * @param layer the z layer this item is on (and which supplies the container)
     * @param wp0      start of the line (world coordinates)
     * @param wp1      end of the line (world coordinates)
     */
    public LineItem(Layer layer, Point2D.Double wp0, Point2D.Double wp1) {
        super(layer);

        // Keep geometry non-null so callers don't have to null-check.
        _line = new Line2D.Double(wp0, wp1);
        updateFocus();

        // Typical behavior for interactive line items.
        setDraggable(true);
        setResizable(true);
    }

    /**
     * Check whether the item (as rendered) contains the given screen point.
     * <p>
     * Hit-testing is performed in screen space using the distance from the point
     * to the rendered line segment (not the infinite line).
     * </p>
     *
     * @param container   the graphical container rendering the item
     * @param screenPoint a pixel location in the container component
     * @return {@code true} if the rendered line segment is within the pick tolerance
     */
    @Override
    public boolean contains(IContainer container, Point screenPoint) {
        if (_line == null) {
            return false;
        }

        Point p0 = new Point();
        Point p1 = new Point();
        container.worldToLocal(p0, _line.x1, _line.y1);
        container.worldToLocal(p1, _line.x2, _line.y2);

        double dist = Line2D.ptSegDist(p0.x, p0.y, p1.x, p1.y, screenPoint.x, screenPoint.y);
        return dist <= getPickTolerancePx(container);
    }

    /**
     * Custom drawer for the item.
     *
     * @param g         the graphics context
     * @param container the graphical container being rendered
     */
    @Override
    public void drawItem(Graphics2D g, IContainer container) {
        if (_line == null) {
            return;
        }
        WorldGraphicsUtils.drawWorldLine(g, container, _line.x1, _line.y1, _line.x2, _line.y2, _style);
    }

    /**
     * Additional visibility test beyond {@link #isVisible()}.
     *
     * @param g         the graphics context
     * @param container the graphical container being rendered
     * @return {@code true} if the item should be drawn
     */
    @Override
    public boolean shouldDraw(Graphics g, IContainer container) {
        if (_line == null) {
            return false;
        }

        Rectangle r = WorldGraphicsUtils.getBounds(container, _line.x1, _line.y1, _line.x2, _line.y2);

        // Inflate slightly so thin lines near the edge don't flicker due to rounding.
        int pad = (int) Math.ceil(getPickTolerancePx(container));
        r.grow(pad, pad);

        return container.getComponent().getBounds().intersects(r);
    }

    /**
     * Selection handles for this item (the two endpoints in screen space).
     *
     * @param container the container used for coordinate conversion
     * @return an array of 2 points in screen space
     */
    @Override
    public Point[] getSelectionPoints(IContainer container) {
        Point[] points = new Point[] { new Point(), new Point() };
        if (_line != null) {
            container.worldToLocal(points[0], _line.x1, _line.y1);
            container.worldToLocal(points[1], _line.x2, _line.y2);
        }
        return points;
    }

    /**
     * A modification such as a drag, resize or rotate is continuing.
     */
    @Override
    public void modify() {
        if (_modification == null || _line == null) {
            return;
        }

        switch (_modification.getType()) {
            case DRAG: {
                _line = (Line2D.Double) _modification.getStartLine().clone();

                Point2D.Double swp = _modification.getStartWorldPoint();
                Point2D.Double cwp = _modification.getCurrentWorldPoint();
                double dx = cwp.x - swp.x;
                double dy = cwp.y - swp.y;

                _line.x1 += dx;
                _line.y1 += dy;
                _line.x2 += dx;
                _line.y2 += dy;

                updateFocus();
                break;
            }

            case RESIZE: {
                _line = (Line2D.Double) _modification.getStartLine().clone();

                Point2D.Double swp = _modification.getStartWorldPoint();
                Point2D.Double cwp = _modification.getCurrentWorldPoint();
                double dx = cwp.x - swp.x;
                double dy = cwp.y - swp.y;

                if (_modification.getSelectIndex() == 0) {
                    _line.x1 += dx;
                    _line.y1 += dy;
                } else {
                    _line.x2 += dx;
                    _line.y2 += dy;
                }

                updateFocus();
                break;
            }

            case ROTATE:
            default:
                // Intentionally a no-op for now.
                break;
        }

        setDirty(true);
        _modification.getContainer().refresh();
    }

    /**
     * Update the item's focus point (midpoint of the segment).
     */
    @Override
    protected void updateFocus() {
        if (_line == null) {
            _focus = new Point2D.Double(0, 0);
            return;
        }
        _focus = new Point2D.Double(0.5 * (_line.x1 + _line.x2), 0.5 * (_line.y1 + _line.y2));
    }

    /**
     * Get the world bounding rectangle of the item.
     * <p>
     * This method never returns {@code null}. If the line is not defined,
     * an empty rectangle is returned.
     * </p>
     *
     * @return the world bounds (possibly empty)
     */
    @Override
    public Rectangle2D.Double getWorldBounds() {
        if (_line == null) {
            return new Rectangle2D.Double(0, 0, 0, 0);
        }
        Rectangle2D r2d = _line.getBounds2D();
        return new Rectangle2D.Double(r2d.getX(), r2d.getY(), r2d.getWidth(), r2d.getHeight());
    }

    /**
     * Pick tolerance in pixels for hit-testing.
     * Kept as a method so subclasses/themes can override if desired.
     */
    @Override
    protected double getPickTolerancePx(IContainer container) {
        return DEFAULT_PICK_TOLERANCE_PX;
    }
}
