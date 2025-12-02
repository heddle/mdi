package edu.cnu.mdi.graphics.text;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public final class TextPainter {

    private TextPainter() { }

    public static void drawSnippets(Graphics g, int x, int y, Font font,
                                    String text, Component component,
                                    double azimuth) {
        g.setFont(font);
        ArrayList<Snippet> snippets = Snippet.getSnippets(font, text, component);
        if (snippets == null) {
            return;
        }
        for (Snippet s : snippets) {
            s.drawSnippet(g, x, y, azimuth);
        }
    }

    // existing drawRotatedText, just make sure signature matches:
    public static void drawRotatedText(Graphics2D g, String s, Font font,
                                       int xo, int yo, int delX, int delY,
                                       double angleDegrees) {

        Object oldAA = g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (Math.abs(angleDegrees) < 0.5) {
            g.setFont(font);
            g.drawString(s, xo + delX, yo + delY);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldAA);
            return;
        }

        AffineTransform oldTx = g.getTransform();

        AffineTransform rotation = new AffineTransform();
        rotation.rotate(Math.toRadians(angleDegrees));

        Point2D.Double offset = new Point2D.Double(delX, delY);
        Point2D.Double rotOffset = new Point2D.Double();
        rotation.transform(offset, rotOffset);

        AffineTransform tx = AffineTransform.getTranslateInstance(xo + rotOffset.x, yo + rotOffset.y);
        tx.concatenate(rotation);

        g.setTransform(tx);
        g.setFont(font);
        g.drawString(s, 0, 0);

        g.setTransform(oldTx);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldAA);
    }
}
