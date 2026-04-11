package edu.cnu.mdi.sim.ga.triimage;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Random;

import edu.cnu.mdi.sim.ga.GASolution;

/**
 * A fixed-length chromosome encoding a set of semi-transparent triangles used
 * to approximate a target image.
 *
 * <h2>Encoding</h2>
 * <p>
 * Each triangle occupies {@value #DOUBLES_PER_TRIANGLE} consecutive doubles in
 * a flat {@code double[]} array:
 * </p>
 * 
 * <pre>
 *   index  0  1  2  3  4  5  6  7  8  9
 *          x1 y1 x2 y2 x3 y3  r  g  b  a
 * </pre>
 * <p>
 * All values are normalized to {@code [0, 1]}. Position values are mapped to
 * pixel coordinates by multiplying by the target image dimensions at render
 * time, so the same chromosome renders correctly at any resolution.
 * </p>
 *
 * <h2>Rendering</h2>
 * <p>
 * Two render paths are provided:
 * </p>
 * <ul>
 * <li>{@link #renderInto(BufferedImage)} — renders into a caller-supplied
 * (reusable) image. Used by fitness evaluation to avoid per-call
 * allocation.</li>
 * <li>{@link #render(int, int)} — allocates a fresh image and renders into it.
 * Used by the view for display-quality output.</li>
 * </ul>
 *
 * <h2>Thread safety</h2>
 * <p>
 * Instances are not thread-safe. Each simulation thread and each display thread
 * should operate on separate instances. {@link #copy()} produces a fully
 * independent deep copy.
 * </p>
 */
public final class PolygonChromosome implements GASolution {

	/**
	 * Number of {@code double} values encoding a single triangle: three (x, y)
	 * vertex pairs plus (r, g, b, a) color components.
	 */
	public static final int DOUBLES_PER_TRIANGLE = 10;

	/**
	 * Flat gene array. Layout for triangle {@code t}:
	 * {@code genes[t*10 .. t*10+9] = [x1,y1, x2,y2, x3,y3, r,g,b,a]}. All values
	 * are in {@code [0, 1]}.
	 */
	final double[] genes;

	/** Number of triangles encoded by this chromosome. */
	final int numTriangles;

	/**
	 * Construct a zero-initialized chromosome for the given number of triangles.
	 * All gene values are {@code 0.0}; use
	 * {@link ImageApproximationProblem#randomIndividual(Random)} to produce a
	 * randomly initialized instance.
	 *
	 * @param numTriangles number of triangles (must be &gt; 0)
	 * @throws IllegalArgumentException if {@code numTriangles} is not positive
	 */
	public PolygonChromosome(int numTriangles) {
		if (numTriangles <= 0) {
			throw new IllegalArgumentException("numTriangles must be > 0");
		}
		this.numTriangles = numTriangles;
		this.genes = new double[numTriangles * DOUBLES_PER_TRIANGLE];
	}

	/**
	 * Private copy constructor. Used by {@link #copy()}.
	 *
	 * @param genes        gene array to copy (non-null)
	 * @param numTriangles number of triangles
	 */
	private PolygonChromosome(double[] genes, int numTriangles) {
		this.numTriangles = numTriangles;
		this.genes = Arrays.copyOf(genes, genes.length);
	}

	// -------------------------------------------------------------------------
	// GASolution implementation
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * <p>
	 * Returns an independent deep copy of this chromosome. The copy shares no state
	 * with the original and is safe to mutate independently.
	 * </p>
	 */
	@SuppressWarnings("unchecked")
	@Override
	public PolygonChromosome copy() {
		return new PolygonChromosome(genes, numTriangles);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Returns the total number of gene values: {@code numTriangles × }
	 * {@link #DOUBLES_PER_TRIANGLE}.
	 * </p>
	 */
	@Override
	public int length() {
		return genes.length;
	}

	// -------------------------------------------------------------------------
	// Rendering
	// -------------------------------------------------------------------------

	// In ImageApproximationProblem — pass current bestMSE to renderInto
	// Or simpler: make maxSpanFraction a field on PolygonChromosome set by the
	// problem

	// In renderInto(), add a maxSpanFraction parameter (0.0 to 1.0):
	/**
	 * Render this chromosome into an existing {@link BufferedImage}.
	 *
	 * <p>
	 * This method is the preferred path for fitness evaluation: the caller supplies
	 * a reusable image (e.g. from a {@link ThreadLocal}) so no allocation occurs
	 * per call. The image is completely overwritten — no blending with previous
	 * content occurs.
	 * </p>
	 *
	 * <p>
	 * The background is filled with opaque black before any triangles are painted.
	 * {@link AlphaComposite#SRC} is used for the background fill so that any alpha
	 * channel left from a previous render is fully overwritten. Triangles are
	 * painted with {@link AlphaComposite#SRC_OVER} (the default), allowing
	 * semi-transparent layering.
	 * </p>
	 *
	 * @param img             the image to render into (non-null); its dimensions
	 *                        determine the pixel-space coordinates used for all
	 *                        triangles
	 * @param maxSpanFraction maximum allowed span of any triangle as a fraction of
	 *                        the image dimensions (0.0 to 1.0); if &gt; 0,
	 * @throws NullPointerException if {@code img} is null
	 */

	public void renderInto(BufferedImage img, double maxSpanFraction) {
		int w = img.getWidth();
		int h = img.getHeight();
		int maxSpan = (int) (maxSpanFraction * Math.min(w, h));

		Graphics2D g2 = img.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setComposite(AlphaComposite.Src);
		g2.setColor(Color.BLACK);
		g2.fillRect(0, 0, w, h);
		g2.setComposite(AlphaComposite.SrcOver);

		int[] px = new int[3];
		int[] py = new int[3];

		for (int t = 0; t < numTriangles; t++) {
			int base = t * DOUBLES_PER_TRIANGLE;
			for (int v = 0; v < 3; v++) {
				px[v] = (int) (genes[base + v * 2] * w);
				py[v] = (int) (genes[base + v * 2 + 1] * h);
			}

			// Soft size clamp — scale toward centroid if triangle is too large
			if (maxSpan > 0) {
				int spanX = Math.max(px[0], Math.max(px[1], px[2])) - Math.min(px[0], Math.min(px[1], px[2]));
				int spanY = Math.max(py[0], Math.max(py[1], py[2])) - Math.min(py[0], Math.min(py[1], py[2]));
				int span = Math.max(spanX, spanY);

				if (span > maxSpan) {
					double cx = (px[0] + px[1] + px[2]) / 3.0;
					double cy = (py[0] + py[1] + py[2]) / 3.0;
					double scale = (double) maxSpan / span;
					for (int v = 0; v < 3; v++) {
						px[v] = (int) (cx + (px[v] - cx) * scale);
						py[v] = (int) (cy + (py[v] - cy) * scale);
					}
				}
			}

			float r = (float) genes[base + 6];
			float g = (float) genes[base + 7];
			float b = (float) genes[base + 8];
			float a = 0.1f + (float) (genes[base + 9] * 0.8);
			g2.setColor(new Color(r, g, b, a));
			g2.fillPolygon(px, py, 3);
		}
		g2.dispose();
	}

	// Keep the no-arg version for the display path — no size limit
	public void renderInto(BufferedImage img) {
		renderInto(img, 0.0); // 0 = no clamping
	}

	/**
	 * Allocate a new {@link BufferedImage} of the given dimensions, render this
	 * chromosome into it, and return it.
	 *
	 * <p>
	 * This method is intended for display-quality rendering by the view. It
	 * allocates a fresh image on every call and should <em>not</em> be called from
	 * {@code fitness()} — use {@link #renderInto(BufferedImage)} with a reusable
	 * buffer instead.
	 * </p>
	 *
	 * @param width  image width in pixels (must be &gt; 0)
	 * @param height image height in pixels (must be &gt; 0)
	 * @return a newly allocated {@link BufferedImage} of type
	 *         {@link BufferedImage#TYPE_INT_ARGB} containing the rendered triangles
	 * @throws IllegalArgumentException if {@code width} or {@code height} is not
	 *                                  positive
	 */
	public BufferedImage render(int width, int height) {
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException("width and height must be > 0, got " + width + "x" + height);
		}
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		renderInto(img);
		return img;
	}

	// -------------------------------------------------------------------------
	// Accessors
	// -------------------------------------------------------------------------

	/**
	 * Return the number of triangles encoded by this chromosome.
	 *
	 * @return number of triangles
	 */
	public int getNumTriangles() {
		return numTriangles;
	}

	/**
	 * Return a defensive copy of the raw gene array.
	 * <p>
	 * Prefer direct field access ({@code genes[i]}) within the package for
	 * performance-sensitive paths such as crossover and mutation.
	 * </p>
	 *
	 * @return copy of the gene array
	 */
	public double[] getGenesCopy() {
		return Arrays.copyOf(genes, genes.length);
	}
}