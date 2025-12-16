package edu.cnu.mdi.splot.demo;

import java.awt.EventQueue;
import java.util.Random;

import javax.swing.JFrame;

import org.apache.commons.math3.fitting.GaussianCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import edu.cnu.mdi.splot.model.BoundsPolicy;
import edu.cnu.mdi.splot.model.MutableCurve;
import edu.cnu.mdi.splot.model.Plot2D;
import edu.cnu.mdi.splot.view.CurveStyle;
import edu.cnu.mdi.splot.view.DefaultTickLabelFormatter;
import edu.cnu.mdi.splot.view.Plot2DRenderer;
import edu.cnu.mdi.splot.view.PlotTheme;
import edu.cnu.mdi.splot.view.SPlotPanel;

/**
 * Demo frame for splot:
 * <ul>
 *   <li>synthetic Gaussian data with Y error bars</li>
 *   <li>Gaussian fit (Apache Commons Math)</li>
 *   <li>renders data + fitted curve + legend</li>
 * </ul>
 *
 * This demo intentionally avoids {@code FitResult} so it matches your current model.
 */
@SuppressWarnings("serial")
public class SPlotDemoFrame extends JFrame {

    public SPlotDemoFrame() {
        super("splot demo: Gaussian fit");

        // ---------------- Model ----------------
        Plot2D plot = new Plot2D();
        plot.setTitle("Gaussian Fit Demo");
        plot.setBoundsPolicy(BoundsPolicy.AUTO);

        // Data curve: enable sigmaY storage so error bars can be drawn.
        MutableCurve data = new MutableCurve("Data", 128, false, true);

        // True parameters for synthetic data
        double trueA = 2.0e4;
        double trueMu = 0.40;
        double trueSigma = 0.08;

        // Noise / error bars
        double sigmaY = 800.0;
        Random rng = new Random(12345);

        int n = 60;
        double xMin = 0.05;
        double xMax = 0.95;

        WeightedObservedPoints obs = new WeightedObservedPoints();

        for (int i = 0; i < n; i++) {
            double x = xMin + i * (xMax - xMin) / (n - 1);
            double yTrue = gaussian(x, trueA, trueMu, trueSigma);
            double yNoisy = yTrue + sigmaY * rng.nextGaussian();

            data.add(x, yNoisy, sigmaY);

            // Weight = 1/sigma^2 for constant sigma
            double w = 1.0 / (sigmaY * sigmaY);
            obs.add(new WeightedObservedPoint(w, x, yNoisy));
        }

        // Fit: Commons Math returns [norm, mean, sigma]
        double[] p = GaussianCurveFitter.create()
                .withMaxIterations(1000)
                .fit(obs.toList());

        double fitA = p[0];
        double fitMu = p[1];
        double fitSigma = Math.abs(p[2]);

        // Fit curve: just sample the fitted function densely
        MutableCurve fit = new MutableCurve("Gaussian fit", 512, false, false);

        int m = 300;
        for (int i = 0; i < m; i++) {
            double x = xMin + i * (xMax - xMin) / (m - 1);
            double y = gaussian(x, fitA, fitMu, fitSigma);
            fit.add(x, y);
        }

        // Add curves to plot
        // If your method name differs, this is the only line(s) to adjust.
        plot.addCurve(data);
        plot.addCurve(fit);

        // ---------------- View ----------------
        PlotTheme theme = new PlotTheme()
                .setInwardTicks(true)
                .setDrawTopTicks(true)
                .setDrawRightTicks(true)
                .setLabelTopTicks(false)
                .setLabelRightTicks(false)
                .setDrawFrame(true)
                .setTickLabelFormatter(DefaultTickLabelFormatter.scientific(3));

        Plot2DRenderer renderer = new Plot2DRenderer();
        renderer.setCurveStyleProvider(c -> {
            // simplest: curve name check (CurveSnapshot usually carries name; if not, use hasSigmaY heuristic)
            if (c.hasSigmaY()) return CurveStyle.points(7);  // data
            return CurveStyle.lines();                       // fit
        });

        renderer.setTheme(theme);
        renderer.setDrawLegend(true);
        renderer.setDrawFitOverlay(false); // we're drawing the fit as its own curve

        SPlotPanel panel = new SPlotPanel(plot, renderer);

        setContentPane(panel);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
    }

    private static double gaussian(double x, double a, double mu, double sigma) {
        double z = (x - mu) / sigma;
        return a * Math.exp(-0.5 * z * z);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new SPlotDemoFrame().setVisible(true));
    }
}
