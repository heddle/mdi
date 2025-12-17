package edu.cnu.mdi.splot.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Stroke;

/**
 * Styling for splot rendering (colors/fonts/strokes). View-layer only.
 */
public class PlotTheme {

    // ---- tick layout ----

    /** Whether ticks point inward (classic scientific look). */
    private boolean inwardTicks = true;

    /** Draw ticks on the top axis as well as bottom. */
    private boolean drawTopTicks = true;

    /** Draw ticks on the right axis as well as left. */
    private boolean drawRightTicks = true;

    /** Whether to draw labels on top ticks (usually false). */
    private boolean labelTopTicks = false;

    /** Whether to draw labels on right ticks (usually false). */
    private boolean labelRightTicks = false;

    /** Pixel padding between tick marks and labels. */
    private int tickLabelPadding = 2;

    // ---- frame & grid ----

    /** Draw a frame rectangle around the plot area. */
    private boolean drawFrame = true;

    /** Whether to draw grid lines. */
    private boolean drawGrid = false;

    /** Whether to draw minor grid lines (if drawGrid is true). */
    private boolean drawMinorGrid = false;

    // ---- fonts ----

    private Font titleFont = new Font("SansSerif", Font.BOLD, 18);
    private Font axisFont  = new Font("SansSerif", Font.PLAIN, 12);
    private Font legendFont = new Font("SansSerif", Font.PLAIN, 12);

    // ---- paints ----

    private Paint axisPaint = Color.BLACK;
    private Paint tickPaint = Color.BLACK;
    private Paint labelPaint = Color.BLACK;

    private Paint gridMajorPaint = new Color(0, 0, 0, 35);
    private Paint gridMinorPaint = new Color(0, 0, 0, 18);

    private Paint legendFramePaint = Color.BLACK;
    private Paint legendBackgroundPaint = new Color(255, 255, 255, 220);

    private Paint curvePaint = Color.BLACK;
    private Paint fitPaint = Color.BLACK;

    // ---- strokes ----

    private Stroke axisStroke = new BasicStroke(1.2f);
    private Stroke frameStroke = new BasicStroke(1.6f);
    private Stroke curveStroke = new BasicStroke(1.5f);
    private Stroke fitStroke = new BasicStroke(
            1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
            10f, new float[]{6f, 4f}, 0f
    );
    private Stroke gridMajorStroke = new BasicStroke(1.0f);
    private Stroke gridMinorStroke = new BasicStroke(1.0f);

    /** If true, legend box is filled with legendBackgroundPaint. */
    private boolean fillLegendBackground = true;

    /** Tick label formatter (default: compact). */
    private TickLabelFormatter tickLabelFormatter = new DefaultTickLabelFormatter();

    // ---- getters/setters ----

    public boolean isInwardTicks() { return inwardTicks; }
    public PlotTheme setInwardTicks(boolean inwardTicks) { this.inwardTicks = inwardTicks; return this; }

    public boolean isDrawTopTicks() { return drawTopTicks; }
    public PlotTheme setDrawTopTicks(boolean drawTopTicks) { this.drawTopTicks = drawTopTicks; return this; }

    public boolean isDrawRightTicks() { return drawRightTicks; }
    public PlotTheme setDrawRightTicks(boolean drawRightTicks) { this.drawRightTicks = drawRightTicks; return this; }

    public boolean isLabelTopTicks() { return labelTopTicks; }
    public PlotTheme setLabelTopTicks(boolean labelTopTicks) { this.labelTopTicks = labelTopTicks; return this; }

    public boolean isLabelRightTicks() { return labelRightTicks; }
    public PlotTheme setLabelRightTicks(boolean labelRightTicks) { this.labelRightTicks = labelRightTicks; return this; }

    public int getTickLabelPadding() { return tickLabelPadding; }
    public PlotTheme setTickLabelPadding(int tickLabelPadding) { this.tickLabelPadding = Math.max(0, tickLabelPadding); return this; }

    public boolean isDrawFrame() { return drawFrame; }
    public PlotTheme setDrawFrame(boolean drawFrame) { this.drawFrame = drawFrame; return this; }

    public boolean isDrawGrid() { return drawGrid; }
    public PlotTheme setDrawGrid(boolean drawGrid) { this.drawGrid = drawGrid; return this; }

    public boolean isDrawMinorGrid() { return drawMinorGrid; }
    public PlotTheme setDrawMinorGrid(boolean drawMinorGrid) { this.drawMinorGrid = drawMinorGrid; return this; }

    public Font getTitleFont() { return titleFont; }
    public PlotTheme setTitleFont(Font titleFont) { this.titleFont = titleFont; return this; }

    public Font getAxisLabelFont() { return axisFont; }
    public PlotTheme setAxisFont(Font axisFont) { this.axisFont = axisFont; return this; }

    public Font getLegendFont() { return legendFont; }
    public PlotTheme setLegendFont(Font legendFont) { this.legendFont = legendFont; return this; }

    public Paint getAxisPaint() { return axisPaint; }
    public PlotTheme setAxisPaint(Paint axisPaint) { this.axisPaint = axisPaint; return this; }

    public Paint getTickPaint() { return tickPaint; }
    public PlotTheme setTickPaint(Paint tickPaint) { this.tickPaint = tickPaint; return this; }

    public Paint getLabelPaint() { return labelPaint; }
    public PlotTheme setLabelPaint(Paint labelPaint) { this.labelPaint = labelPaint; return this; }

    public Paint getGridMajorPaint() { return gridMajorPaint; }
    public PlotTheme setGridMajorPaint(Paint gridMajorPaint) { this.gridMajorPaint = gridMajorPaint; return this; }

    public Paint getGridMinorPaint() { return gridMinorPaint; }
    public PlotTheme setGridMinorPaint(Paint gridMinorPaint) { this.gridMinorPaint = gridMinorPaint; return this; }

    public Paint getLegendFramePaint() { return legendFramePaint; }
    public PlotTheme setLegendFramePaint(Paint legendFramePaint) { this.legendFramePaint = legendFramePaint; return this; }

    public Paint getLegendBackgroundPaint() { return legendBackgroundPaint; }
    public PlotTheme setLegendBackgroundPaint(Paint legendBackgroundPaint) { this.legendBackgroundPaint = legendBackgroundPaint; return this; }

    public boolean isFillLegendBackground() { return fillLegendBackground; }
    public PlotTheme setFillLegendBackground(boolean fillLegendBackground) { this.fillLegendBackground = fillLegendBackground; return this; }

    public Paint getCurvePaint() { return curvePaint; }
    public PlotTheme setCurvePaint(Paint curvePaint) { this.curvePaint = curvePaint; return this; }

    public Paint getFitPaint() { return fitPaint; }
    public PlotTheme setFitPaint(Paint fitPaint) { this.fitPaint = fitPaint; return this; }

    public Stroke getAxisStroke() { return axisStroke; }
    public PlotTheme setAxisStroke(Stroke axisStroke) { this.axisStroke = axisStroke; return this; }

    public Stroke getFrameStroke() { return frameStroke; }
    public PlotTheme setFrameStroke(Stroke frameStroke) { this.frameStroke = frameStroke; return this; }

    public Stroke getCurveStroke() { return curveStroke; }
    public PlotTheme setCurveStroke(Stroke curveStroke) { this.curveStroke = curveStroke; return this; }

    public Stroke getFitStroke() { return fitStroke; }
    public PlotTheme setFitStroke(Stroke fitStroke) { this.fitStroke = fitStroke; return this; }

    public Stroke getGridMajorStroke() { return gridMajorStroke; }
    public PlotTheme setGridMajorStroke(Stroke gridMajorStroke) { this.gridMajorStroke = gridMajorStroke; return this; }

    public Stroke getGridMinorStroke() { return gridMinorStroke; }
    public PlotTheme setGridMinorStroke(Stroke gridMinorStroke) { this.gridMinorStroke = gridMinorStroke; return this; }

    public TickLabelFormatter getTickLabelFormatter() { return tickLabelFormatter; }
    public PlotTheme setTickLabelFormatter(TickLabelFormatter tickLabelFormatter) {
        this.tickLabelFormatter = (tickLabelFormatter == null) ? new DefaultTickLabelFormatter() : tickLabelFormatter;
        return this;
    }
}
