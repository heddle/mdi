package edu.cnu.mdi.graphics.rubberband;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

class ARubberbandTest {

    @Test
    void clampsInComponentCoordinatesRegardlessOfParentLocation() {
        JPanel component = new JPanel();
        component.setBounds(100, 200, 50, 40);
        TestRubberband rubberband = new TestRubberband(component);

        Point low = rubberband.clamp(new Point(-10, -20));
        Point high = rubberband.clamp(new Point(80, 90));

        assertEquals(new Point(0, 0), low);
        assertEquals(new Point(49, 39), high);
        rubberband.cancel();
    }

    @Test
    void rejectsNullAnchorExplicitly() {
        JPanel component = new JPanel();
        TestRubberband rubberband = new TestRubberband(component);
        assertThrows(NullPointerException.class, () -> rubberband.begin(null));
        rubberband.cancel();
    }

    private static final class TestRubberband extends ARubberband {
        TestRubberband(JPanel component) {
            super(component, () -> { }, Policy.RECTANGLE);
        }

        Point clamp(Point point) {
            modifyCurrentPoint(point);
            return point;
        }

        @Override
        protected void draw(Graphics2D graphics) { }

        @Override
        public Rectangle getRubberbandBounds() {
            return new Rectangle(startPt.x, startPt.y,
                    Math.abs(currentPt.x - startPt.x),
                    Math.abs(currentPt.y - startPt.y));
        }
    }
}
