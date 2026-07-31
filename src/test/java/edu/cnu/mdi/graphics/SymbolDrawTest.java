package edu.cnu.mdi.graphics;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.graphics.style.SymbolType;

class SymbolDrawTest {

    @Test
    void drawingRestoresCallerStroke() {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        BasicStroke original = new BasicStroke(4);
        graphics.setStroke(original);
        try {
            SymbolDraw.drawSymbol(graphics, 10, 10, SymbolType.STAR, 8,
                    Color.BLACK, Color.WHITE);
            assertSame(original, graphics.getStroke());
        } finally {
            graphics.dispose();
        }
    }

    @Test
    void nullAndNonpositiveSymbolsAreHarmlessNoOps() {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        BasicStroke original = new BasicStroke(4);
        graphics.setStroke(original);
        try {
            SymbolDraw.drawSymbol(graphics, 10, 10, null, 8, Color.BLACK, Color.WHITE);
            SymbolDraw.drawSymbol(graphics, 10, 10, SymbolType.CIRCLE, 0,
                    Color.BLACK, Color.WHITE);
            assertSame(original, graphics.getStroke());
        } finally {
            graphics.dispose();
        }
    }
}
