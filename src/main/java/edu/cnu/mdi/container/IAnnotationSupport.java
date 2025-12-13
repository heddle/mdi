package edu.cnu.mdi.container;

import java.awt.Point;
import java.awt.Rectangle;

import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.ItemList;
/**
 * This interface provides annotation support methods for containers.
 * 
 * @author heddle
 *
 */
public interface IAnnotationSupport {
    AItem createEllipseItem(ItemList list, Rectangle bounds);
    AItem createRectangleItem(ItemList list, Rectangle bounds);
    AItem createLineItem(ItemList list, Point p0, Point p1);
    AItem createPolygonItem(ItemList list, Point[] pts);
    AItem createPolylineItem(ItemList list, Point[] pts);
    AItem createRadArcItem(ItemList list, Point pc, Point p1, double arcAngle);
}
