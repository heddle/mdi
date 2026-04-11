package edu.cnu.mdi.sim.ga.triimage;

import java.util.List;

import edu.cnu.mdi.view.AbstractViewInfo;

/**
 * View metadata for the Image Evolution GA demo.
 */
public class ImageEvolutionViewInfo extends AbstractViewInfo {

    @Override
    public String getTitle() {
        return "Image Evolution Demo";
    }

    @Override
    public String getPurpose() {
        return "Demonstrates the MDI genetic algorithm framework by evolving a population of "
                + "semi-transparent triangles to approximate a target image. Each individual in "
                + "the population is a fixed-length chromosome encoding triangle positions and "
                + "colors. Over successive generations, selection, crossover, and mutation drive "
                + "the population toward increasingly accurate approximations of the target.";
    }

    @Override
    public List<String> getUsageBullets() {
        return List.of(
                "Press Run to start evolving. The best individual (left panel) updates each refresh.",
                "The population grid (center) shows all current individuals as thumbnails — "
                        + "watch diversity collapse as the population converges.",
                "The target image (top-right) is the image being approximated.",
                "Fitness and diversity plots (bottom-right) show convergence over generations.",
                "Press Pause at any time to inspect the current best individual.",
                "Use the control panel sliders to change triangle count or population size, "
                        + "then press Reset to start a new run."
        );
    }

    @Override
    public String getTechnicalNotes() {
        return "Fitness is the negated mean squared error (MSE) between the rendered chromosome "
                + "and the target image, evaluated at a reduced resolution ("
                + ImageApproximationProblem.FITNESS_W + "×"
                + ImageApproximationProblem.FITNESS_H + " px) for performance. "
                + "Full-resolution rendering is used for display only. "
                + "See: Johansson, R. (2008). Genetic Programming: Evolution of Mona Lisa. "
                + "roger.johansson.se/articles/evolving-mona-lisa.";
    }
}