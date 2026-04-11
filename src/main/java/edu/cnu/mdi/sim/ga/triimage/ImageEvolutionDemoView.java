package edu.cnu.mdi.sim.ga.triimage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationEngine;
import edu.cnu.mdi.sim.SimulationEngineConfig;
import edu.cnu.mdi.sim.ga.GAConfig;
import edu.cnu.mdi.sim.ga.GAOperators;
import edu.cnu.mdi.sim.ga.GAState;
import edu.cnu.mdi.sim.ga.GeneticAlgorithmSimulation;
import edu.cnu.mdi.sim.ui.SimulationView;
import edu.cnu.mdi.util.Environment;
import edu.cnu.mdi.view.AbstractViewInfo;

/**
 * MDI {@link SimulationView} that evolves a population of semi-transparent
 * triangles to approximate a target image using the MDI genetic algorithm
 * framework.
 *
 * <h2>Layout</h2>
 * <p>
 * The view's world container is replaced by a custom
 * {@link ImageEvolutionPanel} installed as the CENTER component. It renders
 * three regions side-by-side:
 * </p>
 * <ul>
 * <li><b>Left</b> — best individual found so far, at full display
 * resolution.</li>
 * <li><b>Center</b> — thumbnail grid of the entire current population.</li>
 * <li><b>Right</b> — the fixed target image.</li>
 * </ul>
 * <p>
 * Fitness and diversity diagnostic plots are installed on the right side of a
 * split pane via the {@link SimulationView} diagnostics mechanism.
 * </p>
 *
 * <h2>Threading</h2>
 * <p>
 * The GA runs on the simulation thread. On each refresh callback (EDT), the
 * view asks the simulation for its best individual, renders it at display
 * resolution, renders all population thumbnails, then hands both to
 * {@link ImageEvolutionPanel} and calls {@code repaint()}.
 * </p>
 *
 * <h2>Reset</h2>
 * <p>
 * The view implements {@link IImageEvolutionResettable} so the control panel
 * can request a fresh run with different parameters without knowing the view's
 * concrete type. Reset is delegated to
 * {@link SimulationView#requestEngineReset}.
 * </p>
 */
@SuppressWarnings("serial")
public class ImageEvolutionDemoView extends SimulationView implements IImageEvolutionResettable {

	// -------------------------------------------------------------------------
	// Defaults
	// -------------------------------------------------------------------------

	/** Default number of triangles per chromosome. */
	public static final int DEFAULT_NUM_TRIANGLES = 150;

	/** Default population size. */
	public static final int DEFAULT_POPULATION_SIZE = 200;

	/**
	 * Pixel size used when rendering population thumbnails. Small enough to be
	 * fast; large enough to see shape.
	 */
	private static final int THUMB_SIZE = 24;

	/**
	 * Size used when rendering the best individual for display in the left panel.
	 * Larger than fitness resolution but not full-screen, to stay fast.
	 */
	private static final int DISPLAY_SIZE = 200;

	// -------------------------------------------------------------------------
	// Fields
	// -------------------------------------------------------------------------

	/** The target image loaded at construction. Never null after construction. */
	private final BufferedImage targetImage;

	/** The custom drawing panel (CENTER component). */
	private final ImageEvolutionPanel evolutionPanel;

	/** Typed reference to the running simulation. Updated on reset. */
	private GeneticAlgorithmSimulation<PolygonChromosome> sim;
	
	/** Typed reference to the diagnostics panel for pushing GA state updates. */
	private GADiagnosticPlotPanel diagnosticPlots;
	// -------------------------------------------------------------------------
	// Constructor
	// -------------------------------------------------------------------------

	/**
	 * Create the image evolution demo view.
	 *
	 * @param keyVals standard {@link edu.cnu.mdi.view.BaseView} key-value args
	 */
	public ImageEvolutionDemoView(Object... keyVals) {
		super(createSimulation(loadDefaultTarget(), DEFAULT_NUM_TRIANGLES, DEFAULT_POPULATION_SIZE),
			      new SimulationEngineConfig(33, 500, 0, false),
			      true,
			      (SimulationView.ControlPanelFactory) ImageEvolutionControlPanel::new,
			      true,                              // was false
			      GADiagnosticPlotPanel::new,        // for plot
			      0.70,                              // main panel gets 70% of width
			      keyVals);
		
		// Retrieve typed references after super() completes.
		this.diagnosticPlots = (GADiagnosticPlotPanel) getDiagnosticsComponent();

		this.sim = castSim(getSimulationEngine());

		// Load target image (same call used in createSimulation; cheap second load).
		this.targetImage = loadDefaultTarget();

		// Install our custom drawing panel as CENTER.
		// BaseView installs an IContainer in CENTER; we replace it with our own
		// panel so we have full control over the three-region layout.
		evolutionPanel = new ImageEvolutionPanel(targetImage);
		installCenterPanel(evolutionPanel);

		pack();
		startSimulation();
	}

	// -------------------------------------------------------------------------
	// SimulationView overrides
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * <p>
	 * On each refresh:
	 * </p>
	 * <ol>
	 * <li>Ask the simulation for a copy of the best individual.</li>
	 * <li>Render it at {@link #DISPLAY_SIZE}×{@link #DISPLAY_SIZE} for the left
	 * panel.</li>
	 * <li>Render all current population individuals as
	 * {@link #THUMB_SIZE}×{@link #THUMB_SIZE} thumbnails for the grid.</li>
	 * <li>Push both to {@link ImageEvolutionPanel} and repaint.</li>
	 * </ol>
	 * <p>
	 * All rendering here uses {@link PolygonChromosome#render(int, int)}, which
	 * allocates a new image — this is intentional for display paths. Fitness
	 * evaluation uses the allocation-free {@link PolygonChromosome#renderInto} path
	 * via thread-local buffers.
	 * </p>
	 */
	@Override
	protected void onSimulationRefresh(SimulationContext ctx) {
		// Note: do NOT call super — we are managing our own panel, not an IContainer.

		GeneticAlgorithmSimulation<PolygonChromosome> s = sim;
		if (s == null)
			return;

		long gen = s.getGeneration();

		// Best individual — update every refresh
		PolygonChromosome best = s.getBestIndividualCopy();
		BufferedImage bestImg = (best != null) ? best.render(DISPLAY_SIZE, DISPLAY_SIZE) : null;
		
		GAState state = s.getState();
		
		
		// Update the left panel with the new best image and generation number. The panel
		// will also display the MSE as a quality metric, but it es expecting bestFitness
		// which is -mse.
		evolutionPanel.updateBest(bestImg, gen, state.bestFitness());

		// Population grid — update every 5th refresh only (every 250 generations)
		if (gen % 250 == 0) {
			evolutionPanel.updatePopulation(buildThumbnails(s));
		}
		
		// Update diagnostic plots with the new GA state. This will add a new point to the MSE vs. generation 
		// plot and trigger a repaint. We do this every refresh so the plot updates smoothly, but it could 
		// be throttled if performance is an issue.
		if (diagnosticPlots != null) {
		    diagnosticPlots.update(state);
		}
	}
		
	@Override
	public AbstractViewInfo getViewInfo() {
		return new ImageEvolutionViewInfo();
	}

	// -------------------------------------------------------------------------
	// IImageEvolutionResettable
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * <p>
	 * Stops the current engine, builds a new simulation with the supplied
	 * parameters, swaps the engine, and restarts. The target image is reloaded from
	 * the default resource; a future version should accept a user-chosen image.
	 * </p>
	 */
	@Override
	public void requestReset(int numTriangles, int populationSize) {
	    requestEngineReset(
	        () -> createSimulation(loadDefaultTarget(), numTriangles, populationSize),

	        (SimulationEngine newEngine) -> {
	            this.sim = castSim(newEngine);
	            this.sim.setEngine(newEngine);
	            // Clear plots here — after the swap, on the EDT,
	            // when the old engine is fully stopped
	            if (diagnosticPlots != null) {
	                diagnosticPlots.clearAllPlots();
	            }
	        },

	        true,
	        true
	    );
	    // Remove the clearAllPlots() call that was here
	}
	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	/**
	 * Build thumbnail renders for every individual in the current population.
	 * Returns an empty list if the population is not yet available.
	 */
	private List<BufferedImage> buildThumbnails(GeneticAlgorithmSimulation<PolygonChromosome> s) {

		// getPopulationSnapshot() returns a point-in-time copy of the individual
		// list so we don't hold a reference into the live population across a
		// generation boundary. See GeneticAlgorithmSimulation for this accessor.
		List<PolygonChromosome> individuals = s.getPopulationSnapshot();
		List<BufferedImage> thumbs = new ArrayList<>(individuals.size());
		for (PolygonChromosome c : individuals) {
			thumbs.add(c.render(THUMB_SIZE, THUMB_SIZE));
		}
		return thumbs;
	}

	/**
	 * Load the default target image from the classpath resource
	 * {@code /edu/cnu/mdi/sim/ga/demo/image/resources/target.png}. Falls back to a
	 * generated gradient image if the resource is not found, so the demo is always
	 * runnable even without an image on the classpath.
	 */
	private static BufferedImage loadDefaultTarget() {
		String path = Environment.MDI_RESOURCE_PATH + "images/target.png";
		try (InputStream in = ImageEvolutionDemoView.class.getResourceAsStream(path)) {
			if (in != null) {
				return ImageIO.read(in);
			}
		} catch (IOException ignored) {
			// fall through to fallback
		}
		return generateFallbackTarget(200, 200);
	}

	/**
	 * Generate a simple RGB gradient image as a fallback target when no classpath
	 * resource is available. This ensures the demo launches and produces visible
	 * evolution even without a bundled image.
	 */
	private static BufferedImage generateFallbackTarget(int w, int h) {
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int r = (x * 255) / w;
				int g = (y * 255) / h;
				int b = 128;
				img.setRGB(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
			}
		}
		return img;
	}

	/**
	 * Static factory method used in the {@code super(...)} call. Builds a fully
	 * configured {@link GeneticAlgorithmSimulation} for the image approximation
	 * problem.
	 */
	private static GeneticAlgorithmSimulation<PolygonChromosome> createSimulation(BufferedImage target,
			int numTriangles, int populationSize) {

		ImageApproximationProblem problem = new ImageApproximationProblem(target, numTriangles);

		GAConfig cfg = new GAConfig(populationSize, 
				100_000, // maxGenerations — effectively unlimited for demo purposes
				0.4, // crossoverRate
				0.025, // mutationRate — now actually used
				5, // eliteCount
				50, // progressEveryGens — update progress every 50 generations
				40, // refreshEveryGens — update display every 10 generations (every 300ms at 30Hz refresh)
				0L); // randomSeed — set to 0 for true randomness; non-zero for reproducibility

		GAOperators<PolygonChromosome> operators = new GAOperators<>(new TournamentSelection<>(2), // was 5
				new UniformBlendCrossover(), new GaussianMutation(cfg.mutationRate(), 0.08, 0.15), // rate from cfg,
																									// sigma=0.08, 10%
																									// resets
				new ElitistReplacement<>(cfg.eliteCount()));

		GeneticAlgorithmSimulation<PolygonChromosome> sim = new GeneticAlgorithmSimulation<>(problem, cfg, operators);

		// Engine reference is injected after construction in the view constructor.
		// The simulation runs fine without it; it just won't post UI hints until
		// setEngine() is called.
		return sim;
	}

	/**
	 * Cast the simulation held by {@code engine} to the concrete type. Also injects
	 * the engine back into the simulation so it can post progress/refresh hints.
	 */
	@SuppressWarnings("unchecked")
	private GeneticAlgorithmSimulation<PolygonChromosome> castSim(SimulationEngine engine) {
		GeneticAlgorithmSimulation<PolygonChromosome> s = (GeneticAlgorithmSimulation<PolygonChromosome>) engine
				.getSimulation();
		s.setEngine(engine);
		return s;
	}

	/**
	 * Install {@code panel} as the main (left) content area.
	 * <p>
	 * If {@link SimulationView} has already wrapped CENTER in a
	 * {@link JSplitPane} for diagnostics, we replace only the left component
	 * of that split pane — preserving the diagnostics panel on the right.
	 * If there is no split pane (diagnostics disabled), we replace CENTER
	 * directly as before.
	 * </p>
	 */
	private void installCenterPanel(ImageEvolutionPanel panel) {
	    // If a diagnostics split pane exists, replace its left component only.
	    // This preserves the diagnostics panel on the right side.
	    if (diagnosticsSplitPane != null) {
	        diagnosticsSplitPane.setLeftComponent(panel);
	        diagnosticsSplitPane.revalidate();
	        return;
	    }

	    // No split pane — replace CENTER directly (diagnostics disabled case)
	    java.awt.Container cp = getContentPane();
	    if (cp.getLayout() instanceof java.awt.BorderLayout bl) {
	        java.awt.Component current = bl.getLayoutComponent(
	                cp, java.awt.BorderLayout.CENTER);
	        if (current != null) {
	            cp.remove(current);
	        }
	    }
	    cp.add(panel, java.awt.BorderLayout.CENTER);
	    cp.revalidate();
	}
}