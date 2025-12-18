package edu.cnu.mdi.container;

import java.awt.Point;
import java.awt.Rectangle;

import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.Layer;
/**
 * This interface provides annotation support methods for containers.
 * 
 * @author heddle
 *
 */
public interface IAnnotationSupport {
    AItem createEllipseItem(Layer list, Rectangle bounds);
    AItem createRectangleItem(Layer list, Rectangle bounds);
    AItem createLineItem(Layer list, Point p0, Point p1);
    AItem createPolygonItem(Layer list, Point[] pts);
    AItem createPolylineItem(Layer list, Point[] pts);
    AItem createRadArcItem(Layer list, Point pc, Point p1, double arcAngle);
}
