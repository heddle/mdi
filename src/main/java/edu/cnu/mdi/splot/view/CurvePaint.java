package edu.cnu.mdi.splot.view;

import java.awt.Color;
import java.util.Objects;

import edu.cnu.mdi.graphics.style.LineStyle;
import edu.cnu.mdi.graphics.style.SymbolType;

/** Visual styling for a curve (renderer-layer only). */
public final class CurvePaint {

    private final CurveDrawMode drawMode;

    private final Color lineColor;
    private final LineStyle lineStyle;
    private final float lineWidth;

    private final SymbolType symbol;
    private final int symbolSize;
    private final Color symbolLineColor;
    private final Color symbolFillColor;

    public CurvePaint(CurveDrawMode drawMode,
                      Color lineColor,
                      LineStyle lineStyle,
                      float lineWidth,
                      SymbolType symbol,
                      int symbolSize,
                      Color symbolLineColor,
                      Color symbolFillColor) {

        this.drawMode = Objects.requireNonNull(drawMode, "drawMode");
        this.lineColor = lineColor; // allow null -> renderer default
        this.lineStyle = Objects.requireNonNull(lineStyle, "lineStyle");
        this.lineWidth = Math.max(0.5f, lineWidth);

        this.symbol = Objects.requireNonNull(symbol, "symbol");
        this.symbolSize = Math.max(2, symbolSize);
        this.symbolLineColor = symbolLineColor;
        this.symbolFillColor = symbolFillColor;
    }

    public CurveDrawMode getDrawMode() { return drawMode; }

    public Color getLineColor() { return lineColor; }
    public LineStyle getLineStyle() { return lineStyle; }
    public float getLineWidth() { return lineWidth; }

    public SymbolType getSymbol() { return symbol; }
    public int getSymbolSize() { return symbolSize; }
    public Color getSymbolLineColor() { return symbolLineColor; }
    public Color getSymbolFillColor() { return symbolFillColor; }

    // Convenience factories
    public static CurvePaint dataPoints(Color c, SymbolType s, int size) {
        return new CurvePaint(CurveDrawMode.POINTS, c, LineStyle.SOLID, 1.0f, s, size, c, c);
    }
    public static CurvePaint fitLine(Color c, LineStyle ls, float w) {
        return new CurvePaint(CurveDrawMode.LINES, c, ls, w, SymbolType.NOSYMBOL, 6, c, c);
    }
    public static CurvePaint lineAndPoints(Color c, LineStyle ls, float w, SymbolType s, int size) {
        return new CurvePaint(CurveDrawMode.LINES_AND_POINTS, c, ls, w, s, size, c, c);
    }
}
