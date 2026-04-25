package edu.cnu.mdi.sim.demo.network;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.cnu.mdi.view.AbstractViewInfo;

/**
 * Describes the {@link NetworkDeclutterDemoView} for the in-app help / info
 * dialog.
 *
 * <p>Returned by {@link NetworkDeclutterDemoView#getViewInfo()} and displayed
 * when the user clicks the info button in the view's title bar.</p>
 */
public class NetworkDeclutterViewInfo extends AbstractViewInfo {

    // -------------------------------------------------------------------------
    // Mandatory content
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @return {@code "Network Decluttering Demo"}
     */
    @Override
    public String getTitle() {
        return "Network Decluttering Demo";
    }

    /**
     * {@inheritDoc}
     *
     * @return a short description of the demo's purpose
     */
    @Override
    public String getPurpose() {
        return "Demonstrates force-directed graph layout applied to a randomly "
             + "generated server/client/printer network. The simulation relaxes "
             + "an initially tangled layout into one where connected nodes cluster "
             + "naturally and icons no longer overlap, using a physics model of "
             + "springs, pairwise repulsion, and weak centering gravity. Three "
             + "diagnostic plots track the energy components, convergence metrics, "
             + "and icon separation in real time.";
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
                "Press Play (\u25b6) to start the relaxation. Watch the network "
              + "untangle: servers spread apart, clients pull toward their servers, "
              + "and printers (red edges) cluster near their assigned clients.",

                "Press Pause (\u23f8) to freeze the layout and inspect the "
              + "current state. The diagnostic plots continue to show the history "
              + "up to the pause point. Press Play to continue.",

                "Press Stop (\u25a0) to terminate the run. The simulation also "
              + "stops automatically when the average node speed falls below the "
              + "settle threshold (after a minimum of 250 steps) or when the "
              + "step limit is reached.",

                "Use the Number Servers, Number Clients, and Number Printers "
              + "sliders to configure the next network ("
              + NetworkDeclutterControlPanel.MIN_SERVERS + "\u2013"
              + NetworkDeclutterControlPanel.MAX_SERVERS + " servers, "
              + NetworkDeclutterControlPanel.MIN_CLIENTS + "\u2013"
              + NetworkDeclutterControlPanel.MAX_CLIENTS + " clients, "
              + NetworkDeclutterControlPanel.MIN_PRINTERS + "\u2013"
              + NetworkDeclutterControlPanel.MAX_PRINTERS + " printers), "
              + "then press Reset to generate a fresh random layout.",

                "Press Reset at any time when paused or stopped to generate a "
              + "new random network with the current slider values. The diagnostic "
              + "plots are cleared and the new layout starts relaxing from scratch."
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Describes the visual elements and the three diagnostic plots.</p>
     */
    @Override
    public List<String> getUsageBullets() {
        return List.of(
                "Canvas (left, 70%): servers (blue workstation icons) and clients "
              + "(gray workstation icons) are connected by black edges. Printers "
              + "(gray printer icons) are connected to their clients by red edges. "
              + "All coordinates are normalized to the unit square.",

                "Potential (pseudo)Energy vs. Step: tracks the four energy "
              + "components on a log step axis. Spring energy (blue) measures "
              + "how far connected nodes are from their equilibrium distance. "
              + "Repulsion energy (red) measures pairwise crowding. Centering "
              + "energy (gold) measures displacement from (0.5, 0.5). Total "
              + "(green squares) is the sum. All should decrease monotonically "
              + "toward a stable minimum in a well-tuned run.",

                "Convergence Metrics vs. Step: Average Speed (black) and Force "
              + "RMS (red) both decay on log\u2013log axes. The Force RMS / "
              + "Average Speed ratio (green) is a diagnostic for detecting "
              + "oscillation \u2014 a rising ratio while speed is low indicates "
              + "nodes are being pushed but not moving, which signals parameter "
              + "tuning may be needed.",

                "Min Pairwise Separation vs. Step: the minimum ratio of "
              + "inter-node distance to combined icon radii across all node pairs. "
              + "A value below 1.0 (the horizontal reference line) means at least "
              + "two icons are overlapping. A well-converged layout holds this "
              + "comfortably above 1.0.",

                "Slider changes do not affect a running simulation. The label "
              + "\u201cChanges apply on Reset\u201d is a reminder. Sliders are "
              + "enabled in the ready, paused, and terminated states only.",

                "Each printer is randomly assigned 1\u20134 clients. Client\u2013"
              + "server assignments are random (one server per client). Printer "
              + "springs use a stiffer spring constant than client\u2013server "
              + "springs, so printers resist being dragged away from their clients."
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
        shortcuts.put("Space", "Play / Pause toggle");
        shortcuts.put("S",     "Stop (terminate) the simulation");
        shortcuts.put("R",     "Reset (when paused or stopped)");
        return shortcuts;
    }

    // -------------------------------------------------------------------------
    // Technical notes
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Returns implementation notes for developers studying or extending the
     * demo.</p>
     */
    @Override
    public String getTechnicalNotes() {
        return "Force model: three additive force components per step. "
             + "(1) Springs on each edge: F = k\u00b7(r \u2212 r\u2080)\u00b7r\u0302, "
             + "where r\u2080 = 0.05 world units. Printer springs use a "
             + "printerKBoost multiplier (default 1.2\u00d7). "
             + "(2) Pairwise repulsion: F \u223c repulsionC\u00a0/\u00a0(r\u00b2 + \u03b5) "
             + "along r\u0302. Server pairs use a serverRepulsion multiplier "
             + "(default 6\u00d7). Overlapping icons get a further overlapBoost "
             + "(default 3\u00d7). "
             + "(3) Centering: F = \u2212centerK\u00b7(x\u22120.5, y\u22120.5) "
             + "prevents boundary evacuation. "
             + "Integration: v \u2190 damping\u00b7v + dt\u00b7F, "
             + "x \u2190 x + v, with per-component velocity clamped to \u00b1vmax. "
             + "Convergence: stops when average speed < settleVel after minSteps, "
             + "or at maxSteps. "
             + "Note: the repulsion pseudo-energy uses strength/\u221a(r\u00b2+\u03b5) "
             + "while the integration force uses strength/(r\u00b2+\u03b5); "
             + "force \u2260 \u2212\u2207U by design \u2014 this is a relaxation "
             + "demo, not a Hamiltonian system. "
             + "Reference: Handbook of Graph Drawing and Visualization "
             + "(Tamassia ed.), Chapter 12: Force-Directed Drawing Algorithms.";
    }

    // -------------------------------------------------------------------------
    // Appearance
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Uses a dark blue accent to echo the server/edge colors on the canvas.</p>
     */
    @Override
    protected String getAccentColorHex() {
        return "#1a4a8a";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code "Network Decluttering Demo \u2014 MDI Framework"}
     */
    @Override
    public String getFooter() {
        return "Network Decluttering Demo \u2014 MDI Framework";
    }
}