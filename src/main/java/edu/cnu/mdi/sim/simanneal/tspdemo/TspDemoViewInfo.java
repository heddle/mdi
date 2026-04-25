package edu.cnu.mdi.sim.simanneal.tspdemo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.cnu.mdi.view.AbstractViewInfo;

/**
 * Describes the {@link TspDemoView} for the in-app help / info dialog.
 *
 * <p>Returned by {@link TspDemoView#getViewInfo()} and displayed when the user
 * clicks the info button in the view's title bar.</p>
 */
public class TspDemoViewInfo extends AbstractViewInfo {

    // -------------------------------------------------------------------------
    // Mandatory content
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @return {@code "TSP Simulated Annealing Demo"}
     */
    @Override
    public String getTitle() {
        return "TSP Simulated Annealing Demo";
    }

    /**
     * {@inheritDoc}
     *
     * @return a short description of the demo's purpose
     */
    @Override
    public String getPurpose() {
        return "Demonstrates the Simulated Annealing algorithm solving the "
             + "Traveling Salesperson Problem (TSP): finding the shortest closed "
             + "tour through a set of randomly placed cities. An optional river "
             + "adds a configurable crossing penalty or bonus, changing the shape "
             + "of the optimal solution. The Energy vs. Temperature scatter plot "
             + "reveals the Metropolis acceptance criterion operating across the "
             + "full cooling schedule.";
    }

    // -------------------------------------------------------------------------
    // Structured usage
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Step-by-step instructions for running and experimenting with the demo.</p>
     */
    @Override
    public List<String> getUsageSteps() {
        return List.of(
                "Press Play (\u25b6) to start the annealer. The tour on the canvas "
              + "will rapidly untangle from its initial random state into a "
              + "near-optimal route.",

                "Press Pause (\u23f8) at any time to freeze the simulation and "
              + "inspect the current best tour. Press Play again to continue.",

                "Press Stop (\u25a0) to terminate the run cleanly. The best tour "
              + "found so far remains visible on the canvas.",

                "Adjust Number Cities (10\u2013" + TspDemoControlPanel.MAX_CITIES
              + ") to change the problem size, then press Reset to generate a new "
              + "random layout. More cities means a harder problem and a longer "
              + "run before convergence.",

                "Adjust River Crossing Penalty or Bonus (\u22121.0 to +1.0) to "
              + "experiment with the effect of the river. A positive penalty "
              + "pushes the optimal tour to avoid the river; a negative bonus "
              + "pulls it to cross as often as possible. Press Reset to rebuild "
              + "with the new value.",

                "Press Reset at any time (when paused or stopped) to generate a "
              + "fresh random city layout using the current slider values. "
              + "Reset is always available in the ready, paused, or terminated "
              + "states."
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Additional notes about the demo's visual elements and algorithm.</p>
     */
    @Override
    public List<String> getUsageBullets() {
        return List.of(
                "Canvas (left, 60%): cities are shown as black dots; the current "
              + "best tour is drawn as a red closed path. The view updates at "
              + "\u223c30 Hz during a run.",

                "River: when enabled, a vertical blue line marks the river "
              + "position. Edges that cross it carry the penalty (or bonus) set "
              + "by the slider. The river\u2019s x-position is chosen randomly "
              + "at Reset.",

                "E vs T plot (right, 40%): each accepted move is recorded as a "
              + "gray point (temperature, energy). New best solutions are shown "
              + "as connected red squares. Together they reveal how the Metropolis "
              + "criterion accepts occasional uphill moves early in the run to "
              + "escape local minima.",

                "Slider changes do not take effect mid-run. The label "
              + "\u201cChanges apply on Reset\u201d appears next to the Reset "
              + "button as a reminder. Sliders are disabled while the simulation "
              + "is running.",

                "The move operator is a 2-opt reversal: a random contiguous "
              + "segment of the tour is reversed, reconnecting two edges. "
              + "The exact \u0394E is computed in O(1) without re-evaluating the "
              + "full tour.",

                "The initial temperature T\u2080 is estimated automatically by "
              + "the EnergyDistributionHeuristic, which samples random solutions "
              + "to calibrate the acceptance rate at the start of the run."
        );
    }

    // -------------------------------------------------------------------------
    // Keyboard shortcuts
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Returns media-control keyboard shortcuts in display order.</p>
     */
    @Override
    public Map<String, String> getKeyboardShortcuts() {
        Map<String, String> shortcuts = new LinkedHashMap<>();
        shortcuts.put("Space",   "Play / Pause toggle");
        shortcuts.put("S",       "Stop (terminate) the simulation");
        shortcuts.put("R",       "Reset (when paused or stopped)");
        return shortcuts;
    }

    // -------------------------------------------------------------------------
    // Technical notes
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Returns implementation notes for developers extending or studying
     * the demo.</p>
     */
    @Override
    public String getTechnicalNotes() {
        return "Algorithm: Simulated Annealing with a geometric cooling schedule "
             + "(GeometricAnnealingSchedule). Move operator: 2-opt segment "
             + "reversal (TspMove) with O(1) delta-E via edge-difference formula. "
             + "The move object is cached and reused across steps to avoid "
             + "per-step allocation. "
             + "Initial temperature T\u2080: estimated by EnergyDistributionHeuristic "
             + "sampling random tours to calibrate the Metropolis acceptance rate. "
             + "Threading: the annealer runs on a dedicated simulation thread; "
             + "the canvas reads only a volatile tour snapshot (bestTourSnapshot) "
             + "written by newBest() and onSimulationProgress() callbacks, so "
             + "no locking is required in the drawing hooks. "
             + "Reset: stopping the current engine, building a new TspModel and "
             + "SimulatedAnnealingSimulation from the control panel values, and "
             + "restarting. A ThreadLocal (BUNDLE_TL) is used to pass the new "
             + "TspModel across the super() constructor boundary. "
             + "River penalty: added to the Euclidean distance of any edge that "
             + "crosses the river x-position; can be negative, making the "
             + "effective edge length less than the Euclidean distance.";
    }

    // -------------------------------------------------------------------------
    // Appearance
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Uses a warm red accent to echo the tour color used on the canvas.</p>
     */
    @Override
    protected String getAccentColorHex() {
        // Matches Color.RED.darker() approximately.
        return "#cc0000";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code "TSP Simulated Annealing Demo \u2014 MDI Framework"}
     */
    @Override
    public String getFooter() {
        return "TSP Simulated Annealing Demo \u2014 MDI Framework";
    }
}