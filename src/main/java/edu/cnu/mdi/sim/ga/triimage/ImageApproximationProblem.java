package edu.cnu.mdi.sim.ga.triimage;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.cnu.mdi.sim.ga.GAPopulation;
import edu.cnu.mdi.sim.ga.GAProblem;
import edu.cnu.mdi.sim.ga.SimpleGAPopulation;

/**
 * A {@link GAProblem} that evolves a population of {@link PolygonChromosome}
 * instances to approximate a target image using semi-transparent triangles.
 *
 * <h2>Fitness</h2>
 * <p>
 * Color fitness is the negated mean squared error (MSE) between the rendered
 * chromosome and target RGB channels:
 * </p>
 * 
 * <pre>
 *   fitness = −MSE
 *   MSE     = Σ [(ΔR² + ΔG² + ΔB²) / 255²] / (pixels × 3)
 * </pre>
 * <p>
 * Following the GA convention of higher = better, a perfect match has fitness
 * {@code 0.0} and a maximally wrong solution has fitness {@code -1.0}. The
 * alpha channel is excluded from the error metric — it affects appearance
 * through blending but is not directly penalized.
 * </p>
 * <p>{@link ImageFitnessMode#LINE_AWARE} blends 70% of this color error with
 * 30% normalized Sobel luminance-edge error. This rewards candidates whose
 * major contours align with the target even before their colors are exact.</p>
 *
 * <h2>Performance</h2>
 * <p>
 * Three optimizations keep fitness evaluation fast enough for interactive
 * population sizes (≥ 100 individuals):
 * </p>
 * <ol>
 * <li><b>Reduced fitness resolution.</b> The target image is pre-scaled to
 * {@link #FITNESS_W} × {@link #FITNESS_H} pixels at construction. All fitness
 * evaluations render at this small size. The view calls
 * {@link PolygonChromosome#render(int, int)} at full display resolution
 * independently. Typical speedup: 6–10× over full-resolution evaluation.</li>
 * <li><b>Thread-local render buffers.</b> The {@link BufferedImage} and pixel
 * array used during each {@code fitness()} call are allocated once per thread
 * and reused for every subsequent call on that thread. This eliminates
 * per-evaluation heap allocation and GC pressure. The buffers are stored in
 * {@link ThreadLocal} fields, keeping this fitness implementation safe if a
 * caller evaluates individuals concurrently. Typical speedup: 2–3× over
 * per-call allocation.</li>
 * <li><b>Integer MSE accumulation.</b> The inner pixel loop accumulates squared
 * channel differences as {@code long} integers and divides by {@code 255²}
 * exactly once after the loop, avoiding per-pixel floating-point division.
 * Typical speedup: 1.1–1.2×.</li>
 * </ol>
 *
 * <h2>Usage</h2>
 * 
 * <pre>{@code
 * BufferedImage target = ImageIO.read(new File("mona_lisa.png"));
 * GAProblem<PolygonChromosome> problem = new ImageApproximationProblem(target, 50);
 *
 * GAOperators<PolygonChromosome> ops = new GAOperators<>(new TournamentSelection<>(5), new UniformBlendCrossover(),
 * 		new GaussianMutation(0.05), new ElitistReplacement<>(2));
 *
 * GeneticAlgorithmSimulation<PolygonChromosome> sim = new GeneticAlgorithmSimulation<>(problem, GAConfig.defaults(),
 * 		ops);
 * }</pre>
 */
public final class ImageApproximationProblem implements GAProblem<PolygonChromosome> {

	private volatile double maxSpanFraction = 1.0; // start unconstrained

	// -------------------------------------------------------------------------
	// Fitness resolution
	// -------------------------------------------------------------------------

	/**
	 * Width in pixels used for fitness evaluation rendering.
	 * <p>
	 * Reducing this value increases evaluation speed at the cost of a coarser
	 * fitness signal. Values in the range 60–100 work well for triangle counts up
	 * to ~100. The view always renders at the original image dimensions via
	 * {@link PolygonChromosome#render(int, int)}.
	 * </p>
	 */
	public static final int FITNESS_W = 50;

	/**
	 * Height in pixels used for fitness evaluation rendering.
	 *
	 * @see #FITNESS_W
	 */
	public static final int FITNESS_H = 50;

	// -------------------------------------------------------------------------
	// Thread-local reusable buffers (Fix 2)
	// -------------------------------------------------------------------------

	/**
	 * Per-thread reusable render target.
	 * <p>
	 * Allocated once per thread on first use; dimensions are always
	 * {@link #FITNESS_W} × {@link #FITNESS_H}. {@code renderInto} overwrites the
	 * entire image including alpha, so stale content from a previous call never
	 * affects the result.
	 * </p>
	 */
	private static final ThreadLocal<BufferedImage> RENDER_BUFFER = ThreadLocal
			.withInitial(() -> new BufferedImage(FITNESS_W, FITNESS_H, BufferedImage.TYPE_INT_ARGB));

	/**
	 * Per-thread reusable pixel extraction array.
	 * <p>
	 * Reused by {@link BufferedImage#getRGB(int, int, int, int, int[], int, int)}
	 * to avoid allocating a new {@code int[]} on every fitness call.
	 * </p>
	 */
	private static final ThreadLocal<int[]> PIXEL_BUFFER = ThreadLocal
			.withInitial(() -> new int[FITNESS_W * FITNESS_H]);
	private static final ThreadLocal<int[]> LUMA_BUFFER = ThreadLocal
			.withInitial(() -> new int[FITNESS_W * FITNESS_H]);

	// -------------------------------------------------------------------------
	// Fields
	// -------------------------------------------------------------------------

	/**
	 * Pre-scaled target pixel data at {@link #FITNESS_W} × {@link #FITNESS_H}.
	 * Packed ARGB format, row-major. Computed once at construction.
	 */
	private final int[] targetPixels;
	private final int[] targetLuminance;
	private final ImageFitnessMode fitnessMode;

	/**
	 * Original target image width, used by the view for display rendering.
	 */
	private final int displayWidth;

	/**
	 * Original target image height, used by the view for display rendering.
	 */
	private final int displayHeight;

	/**
	 * Number of triangles per chromosome. All individuals in the population
	 * produced by this problem have this chromosome length.
	 */
	private final int numTriangles;

	/** Average target color used as the chromosome background. */
	private final int backgroundRgb;

	// -------------------------------------------------------------------------
	// Constructor
	// -------------------------------------------------------------------------

	/**
	 * Construct an image approximation problem for the given target image and
	 * triangle count.
	 *
	 * <p>
	 * The target image is immediately scaled to {@link #FITNESS_W} ×
	 * {@link #FITNESS_H} using bilinear interpolation and its pixel data is
	 * extracted into an {@code int[]} array. This pre-processing is done once at
	 * construction so that {@link #fitness} can operate purely on primitive arrays
	 * without touching the original image.
	 * </p>
	 *
	 * @param target       the image to approximate (non-null; any dimensions)
	 * @param numTriangles number of triangles per chromosome (must be &gt; 0)
	 * @throws NullPointerException     if {@code target} is null
	 * @throws IllegalArgumentException if {@code numTriangles} is not positive
	 */
	public ImageApproximationProblem(BufferedImage target, int numTriangles) {
		this(target, numTriangles, ImageFitnessMode.COLOR_MSE);
	}

	/**
	 * Construct a problem with an explicit fitness metric.
	 *
	 * @param target target image
	 * @param numTriangles number of triangles per chromosome
	 * @param fitnessMode error metric to minimize
	 */
	public ImageApproximationProblem(BufferedImage target, int numTriangles,
			ImageFitnessMode fitnessMode) {
		if (target == null) {
			throw new NullPointerException("target must not be null");
		}
		if (numTriangles <= 0) {
			throw new IllegalArgumentException("numTriangles must be > 0");
		}

		this.displayWidth = target.getWidth();
		this.displayHeight = target.getHeight();
		this.numTriangles = numTriangles;
		this.fitnessMode = java.util.Objects.requireNonNull(fitnessMode, "fitnessMode");

		// Scale the target down to fitness resolution once.
		// All subsequent fitness evaluations compare against this scaled version.
		BufferedImage scaled = new BufferedImage(FITNESS_W, FITNESS_H, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = scaled.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.drawImage(target, 0, 0, FITNESS_W, FITNESS_H, null);
		g2.dispose();

		this.targetPixels = scaled.getRGB(0, 0, FITNESS_W, FITNESS_H, null, 0, FITNESS_W);
		this.targetLuminance = luminance(targetPixels, new int[targetPixels.length]);
		this.backgroundRgb = averageRgb(targetPixels);
	}

	// -------------------------------------------------------------------------
	// GAProblem implementation
	// -------------------------------------------------------------------------

	/**
	 * Compute fitness as the negated configured error between the rendered image
	 * and target.
	 *
	 * <p>
	 * The chromosome is rendered into a thread-local {@link BufferedImage} at
	 * {@link #FITNESS_W} × {@link #FITNESS_H} pixels. Pixel differences are
	 * accumulated as integer squared errors (Fix 3) and normalized to
	 * {@code [0, 1]} after the loop. The result is negated so that higher fitness
	 * corresponds to a closer match.
	 * </p>
	 *
	 * <p>
	 * Fitness values lie in {@code [-1, 0]}:
	 * </p>
	 * <ul>
	 * <li>{@code  0.0} — perfect match</li>
	 * <li>{@code -1.0} — maximally wrong (every channel maximally wrong)</li>
	 * </ul>
	 *
	 * <p>
	 * This method is safe to call from multiple threads simultaneously because it
	 * uses only {@link ThreadLocal} buffers, and its one shared mutable field
	 * ({@code maxSpanFraction}) is {@code volatile} for safe cross-thread
	 * visibility. In the current implementation nothing ever writes
	 * {@code maxSpanFraction} after construction (it stays at its initial
	 * unconstrained value of {@code 1.0}), so it behaves as an effectively
	 * immutable configuration value today.
	 * </p>
	 *
	 * @param c the chromosome to evaluate (non-null)
	 * @return fitness in {@code [-1, 0]}, higher is better
	 */
	@Override
	public double fitness(PolygonChromosome c) {
		BufferedImage buf = RENDER_BUFFER.get();
		int[] pixels = PIXEL_BUFFER.get();
		c.renderInto(buf, maxSpanFraction); // pass current limit
		buf.getRGB(0, 0, FITNESS_W, FITNESS_H, pixels, 0, FITNESS_W);

		// Fix 3: accumulate as long integers; divide by 255^2 once at the end
		long mseInt = 0;
		for (int i = 0; i < targetPixels.length; i++) {
			int tp = targetPixels[i];
			int rp = pixels[i];

			int dr = ((tp >> 16) & 0xFF) - ((rp >> 16) & 0xFF);
			int dg = ((tp >> 8) & 0xFF) - ((rp >> 8) & 0xFF);
			int db = (tp & 0xFF) - (rp & 0xFF);

			mseInt += (long) (dr * dr + dg * dg + db * db);
		}

		// Normalize: max possible mseInt = 3 * 255^2 * pixelCount
		double colorMse = mseInt / (targetPixels.length * 3.0 * 255.0 * 255.0);
		if (fitnessMode == ImageFitnessMode.COLOR_MSE) {
			return -colorMse;
		}
		int[] renderedLuminance = luminance(pixels, LUMA_BUFFER.get());
		double edgeMse = edgeMse(targetLuminance, renderedLuminance);
		return -(0.70 * colorMse + 0.30 * edgeMse);
	}

	private static int[] luminance(int[] pixels, int[] result) {
		for (int index = 0; index < pixels.length; index++) {
			int pixel = pixels[index];
			int red = (pixel >>> 16) & 0xFF;
			int green = (pixel >>> 8) & 0xFF;
			int blue = pixel & 0xFF;
			result[index] = (77 * red + 150 * green + 29 * blue) >>> 8;
		}
		return result;
	}

	private static double edgeMse(int[] target, int[] rendered) {
		long squaredError = 0;
		int samples = 0;
		for (int y = 1; y < FITNESS_H - 1; y++) {
			for (int x = 1; x < FITNESS_W - 1; x++) {
				int targetEdge = sobelMagnitude(target, x, y);
				int renderedEdge = sobelMagnitude(rendered, x, y);
				int difference = targetEdge - renderedEdge;
				squaredError += (long) difference * difference;
				samples++;
			}
		}
		return squaredError / (samples * 1020.0 * 1020.0);
	}

	private static int sobelMagnitude(int[] luminance, int x, int y) {
		int rowAbove = (y - 1) * FITNESS_W;
		int row = y * FITNESS_W;
		int rowBelow = (y + 1) * FITNESS_W;
		int gx = -luminance[rowAbove + x - 1] + luminance[rowAbove + x + 1]
				- 2 * luminance[row + x - 1] + 2 * luminance[row + x + 1]
				- luminance[rowBelow + x - 1] + luminance[rowBelow + x + 1];
		int gy = -luminance[rowAbove + x - 1] - 2 * luminance[rowAbove + x]
				- luminance[rowAbove + x + 1] + luminance[rowBelow + x - 1]
				+ 2 * luminance[rowBelow + x] + luminance[rowBelow + x + 1];
		return Math.min(1020, Math.abs(gx) + Math.abs(gy));
	}

	/** @return configured fitness metric */
	public ImageFitnessMode getFitnessMode() {
		return fitnessMode;
	}

	/**
	 * Generate a random chromosome in which all gene values are drawn independently
	 * from {@code Uniform(0, 1)}.
	 *
	 * @param rng source of randomness (non-null)
	 * @return a new randomly initialized {@link PolygonChromosome}
	 */
	@Override
	public PolygonChromosome randomIndividual(Random rng) {
		PolygonChromosome c = new PolygonChromosome(numTriangles, backgroundRgb);
		for (int i = 0; i < c.genes.length; i++) {
			c.genes[i] = rng.nextDouble();
		}
		return c;
	}

	/**
	 * Generate an initial population of {@code size} randomly initialized
	 * chromosomes.
	 *
	 * <p>
	 * Each individual is produced by {@link #randomIndividual(Random)}
	 * independently. The returned population is a {@link SimpleGAPopulation} backed
	 * by a fresh {@link ArrayList}.
	 * </p>
	 *
	 * @param size number of individuals (must be &gt; 0)
	 * @param rng  source of randomness (non-null)
	 * @return a new population of {@code size} random individuals
	 */
	@Override
	public GAPopulation<PolygonChromosome> initialPopulation(int size, Random rng) {
		List<PolygonChromosome> pop = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			pop.add(randomIndividual(rng));
		}
		return SimpleGAPopulation.of(pop);
	}

	private static int averageRgb(int[] pixels) {
		long red = 0;
		long green = 0;
		long blue = 0;
		for (int pixel : pixels) {
			red += (pixel >>> 16) & 0xFF;
			green += (pixel >>> 8) & 0xFF;
			blue += pixel & 0xFF;
		}
		int count = pixels.length;
		return 0xFF000000
				| ((int) (red / count) << 16)
				| ((int) (green / count) << 8)
				| (int) (blue / count);
	}

	// -------------------------------------------------------------------------
	// Accessors
	// -------------------------------------------------------------------------

	/**
	 * Return the original target image width in pixels.
	 * <p>
	 * The view uses this to render chromosomes at display quality via
	 * {@link PolygonChromosome#render(int, int)}.
	 * </p>
	 *
	 * @return display width
	 */
	public int getDisplayWidth() {
		return displayWidth;
	}

	/**
	 * Return the original target image height in pixels.
	 *
	 * @return display height
	 * @see #getDisplayWidth()
	 */
	public int getDisplayHeight() {
		return displayHeight;
	}

	/**
	 * Return the number of triangles per chromosome for this problem.
	 *
	 * @return triangle count
	 */
	public int getNumTriangles() {
		return numTriangles;
	}

}
