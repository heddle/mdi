package edu.cnu.mdi.sim.ga.triimage;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.JPanel;

/**
 * Custom drawing panel for the image evolution GA demo.
 *
 * <h2>Layout</h2>
 * <p>
 * The panel is divided into three horizontal regions:
 * </p>
 * <pre>
 *  ┌──────────────────┬──────────────────┬──────────────────┐
 *  │                  │                  │                  │
 *  │   Best so far    │  Population grid │   Target image   │
 *  │   (full res)     │  (N thumbnails)  │                  │
 *  │                  │                  │                  │
 *  └──────────────────┴──────────────────┴──────────────────┘
 * </pre>
 * <p>
 * The three regions share the available width equally. All rendering happens
 * in {@link #paintComponent(Graphics)} on the EDT, reading volatile references
 * set by the simulation refresh callback.
 * </p>
 *
 * <h2>Threading</h2>
 * <p>
 * updateBest(BufferedImage) and updatePopulation(List) are
 * called from {@code onSimulationRefresh}, which is already on the EDT
 * (guaranteed by {@link edu.cnu.mdi.sim.SimulationEngine}). No additional
 * synchronization is needed.
 * </p>
 */
@SuppressWarnings("serial")
public class ImageEvolutionPanel extends JPanel {

    // -------------------------------------------------------------------------
    // Layout constants
    // -------------------------------------------------------------------------

    /** Pixel gap between the three main regions. */
    private static final int REGION_GAP = 6;

    /** Pixel gap between thumbnails in the population grid. */
    private static final int THUMB_GAP = 2;

    /** Height of the label strip below each main region. */
    private static final int LABEL_HEIGHT = 18;

    /** Background color for the entire panel. */
    private static final Color BG_COLOR = new Color(30, 30, 30);

    /** Border color drawn around each thumbnail. */
    private static final Color THUMB_BORDER = new Color(60, 60, 60);

    /** Color for region labels. */
    private static final Color LABEL_COLOR = new Color(180, 180, 180);

    /** Font for region labels. */
    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);

    // -------------------------------------------------------------------------
    // State updated from the EDT on each refresh
    // -------------------------------------------------------------------------

    /** Most recent best-individual render. Null until first refresh. */
    private BufferedImage bestImage;

    /** Current generation index, for the "Gen N" label. */
    private long generation;

    /** Best fitness value, for display in the label. */
    private double bestFitness = Double.NaN;

    /**
     * Thumbnail renders of the current population.
     * Each element is a small {@link BufferedImage} produced by
     * {@link PolygonChromosome#render(int, int)}.
     * Null until the first refresh.
     */
    private List<BufferedImage> populationThumbs;

    /** The target image, set once at construction and never changed. */
    private final BufferedImage targetImage;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Construct the panel with the given target image.
     *
     * @param targetImage the image being approximated (non-null); displayed in
     *                    the right-hand region and never modified
     */
    public ImageEvolutionPanel(BufferedImage targetImage) {
        this.targetImage = targetImage;
        setBackground(BG_COLOR);
        setPreferredSize(new Dimension(1100, 500));
    }

    // -------------------------------------------------------------------------
    // EDT update methods (called from onSimulationRefresh)
    // -------------------------------------------------------------------------

    /**
     * Update the best-individual image and associated statistics label.
     * <p>
     * Must be called on the EDT. The supplied image is displayed as-is; the
     * caller is responsible for rendering it at an appropriate display
     * resolution before calling this method.
     * </p>
     *
     * @param bestRendered full-resolution render of the best individual
     * @param generation   current generation index
     * @param bestFitness  current best fitness (higher = better, range [-1, 0])
     */
    public void updateBest(BufferedImage bestRendered, long generation, double bestFitness) {
        this.bestImage   = bestRendered;
        this.generation  = generation;
        this.bestFitness = bestFitness;
        repaint();
    }

    /**
     * Update the population thumbnail grid.
     * <p>
     * Must be called on the EDT. Each element of {@code thumbs} is a small
     * pre-rendered image (e.g. 40×40 px) of one individual. The panel arranges
     * them in a grid that fills the center region.
     * </p>
     *
     * @param thumbs thumbnail renders of all current individuals (non-null)
     */
    public void updatePopulation(List<BufferedImage> thumbs) {
        this.populationThumbs = thumbs;
        repaint();
    }

    // -------------------------------------------------------------------------
    // Painting
    // -------------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Fill background
        g2.setColor(BG_COLOR);
        g2.fillRect(0, 0, w, h);

        // Divide width equally into three regions
        int regionW = (w - 4 * REGION_GAP) / 3;
        int imageH  = h - LABEL_HEIGHT - 2 * REGION_GAP;

        int xBest   = REGION_GAP;
        int xGrid   = xBest  + regionW + REGION_GAP;
        int xTarget = xGrid  + regionW + REGION_GAP;
        int yImg    = REGION_GAP;

        // ── Best individual ──────────────────────────────────────────────────
        paintScaledImage(g2, bestImage, xBest, yImg, regionW, imageH, "No data yet");
        paintLabel(g2, buildBestLabel(), xBest, yImg + imageH, regionW);

        // ── Population grid ──────────────────────────────────────────────────
        paintPopulationGrid(g2, xGrid, yImg, regionW, imageH);
        paintLabel(g2, "Population", xGrid, yImg + imageH, regionW);

        // ── Target image ─────────────────────────────────────────────────────
        paintScaledImage(g2, targetImage, xTarget, yImg, regionW, imageH, "No target");
        paintLabel(g2, "Target", xTarget, yImg + imageH, regionW);

        g2.dispose();
    }

    // ── Private painting helpers ─────────────────────────────────────────────

    /**
     * Paint a {@link BufferedImage} scaled to fit within the given bounds,
     * preserving aspect ratio and centering. If {@code img} is null, paint
     * a placeholder with {@code placeholderText}.
     */
    private void paintScaledImage(Graphics2D g2, BufferedImage img,
                                   int x, int y, int w, int h,
                                   String placeholderText) {
        // Dark inset background
        g2.setColor(new Color(20, 20, 20));
        g2.fillRect(x, y, w, h);

        if (img == null) {
            paintCenteredString(g2, placeholderText, LABEL_COLOR, x, y, w, h);
            return;
        }

        // Scale to fit, preserving aspect ratio
        double scaleX = (double) w / img.getWidth();
        double scaleY = (double) h / img.getHeight();
        double scale  = Math.min(scaleX, scaleY);

        int dw = (int) (img.getWidth()  * scale);
        int dh = (int) (img.getHeight() * scale);
        int dx = x + (w - dw) / 2;
        int dy = y + (h - dh) / 2;

        g2.drawImage(img, dx, dy, dw, dh, null);
    }

    /**
     * Paint the population grid into the given region.
     * Thumbnails are laid out in a grid that fills the available space as
     * tightly as possible given the number of individuals.
     */
    private void paintPopulationGrid(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(new Color(20, 20, 20));
        g2.fillRect(x, y, w, h);

        List<BufferedImage> thumbs = populationThumbs;
        if (thumbs == null || thumbs.isEmpty()) {
            paintCenteredString(g2, "No population yet", LABEL_COLOR, x, y, w, h);
            return;
        }

        int n    = thumbs.size();
        int cols = (int) Math.ceil(Math.sqrt(n));
        int rows = (int) Math.ceil((double) n / cols);

        int thumbW = (w - (cols + 1) * THUMB_GAP) / cols;
        int thumbH = (h - (rows + 1) * THUMB_GAP) / rows;

        // Clamp to positive dimensions — can happen if panel is very small
        thumbW = Math.max(thumbW, 1);
        thumbH = Math.max(thumbH, 1);

        for (int i = 0; i < n; i++) {
            int col = i % cols;
            int row = i / cols;

            int tx = x + THUMB_GAP + col * (thumbW + THUMB_GAP);
            int ty = y + THUMB_GAP + row * (thumbH + THUMB_GAP);

            BufferedImage thumb = thumbs.get(i);
            if (thumb != null) {
                g2.drawImage(thumb, tx, ty, thumbW, thumbH, null);
            }

            // Subtle border around each thumbnail
            g2.setColor(THUMB_BORDER);
            g2.drawRect(tx, ty, thumbW - 1, thumbH - 1);
        }
    }

    /**
     * Paint a label string centered horizontally below a region.
     */
    private void paintLabel(Graphics2D g2, String text, int x, int y, int w) {
        g2.setFont(LABEL_FONT);
        g2.setColor(LABEL_COLOR);
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (w - fm.stringWidth(text)) / 2;
        int ty = y + fm.getAscent() + 2;
        g2.drawString(text, tx, ty);
    }

    /**
     * Paint a string centered within a rectangular region.
     */
    private void paintCenteredString(Graphics2D g2, String text, Color color,
                                      int x, int y, int w, int h) {
        g2.setFont(LABEL_FONT);
        g2.setColor(color);
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (w - fm.stringWidth(text)) / 2;
        int ty = y + (h + fm.getAscent()) / 2;
        g2.drawString(text, tx, ty);
    }

    /**
     * Build the label string shown under the best-individual panel.
     * Includes the current generation and best fitness (converted back to MSE).
     * MSE stands for mean squared error, which is the original metric being minimized.
     */
    private String buildBestLabel() {
        if (Double.isNaN(bestFitness)) {
            return "Best individual";
        }
        // bestFitness = -MSE, so MSE = -bestFitness
        // Multiply by 255^2 to get back to familiar 0-65025 scale, or just show directly
         return String.format("Best  gen %d  MSE=%.4f", generation, -bestFitness);
    }
}