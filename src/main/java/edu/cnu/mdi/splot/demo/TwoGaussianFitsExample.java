package edu.cnu.mdi.splot.demo;

import java.util.Random;

import org.apache.commons.math3.fitting.GaussianCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import edu.cnu.mdi.graphics.style.LineStyle;
import edu.cnu.mdi.graphics.style.SymbolType;
import edu.cnu.mdi.splot.model.BoundsPolicy;
import edu.cnu.mdi.splot.model.FitResult;
import edu.cnu.mdi.splot.model.FitSpec;
import edu.cnu.mdi.splot.model.MutableCurve;
import edu.cnu.mdi.splot.model.Plot2D;
import edu.cnu.mdi.splot.view.CurvePaint;
import edu.cnu.mdi.splot.view.DefaultTickLabelFormatter;
import edu.cnu.mdi.splot.view.Plot2DRenderer;
import edu.cnu.mdi.splot.view.PlotTheme;

/**
 * Demonstrates multiple data curves and multiple fit curves on the same plot.
 * Two synthetic peaks with different parameters; each gets its own fit curve and FitResult.
 */
public class TwoGaussianFitsExample extends AExamplePlot {

    @Override
    public String getId() {
        return "two-gaussian-fits";
    }

    @Override
    public String getDisplayName() {
        return "Two datasets + two Gaussian fits";
    }

    @Override
    public String getDescription() {
        return "Two synthetic Gaussian datasets with σy and separate Gaussian fits.";
    }

    @Override
    public Plot2D buildPlot() {
        Plot2D plot = new Plot2D();
        plot.setTitle("Two Gaussian Fits");
        plot.setBoundsPolicy(BoundsPolicy.AUTO);

        plot.getXAxis().setLabel("x");
        plot.getYAxis().setLabel("Counts");

        // Shared x grid
        int n = 80;
        double xMin = 0.0;
        double xMax = 1.0;

        // Dataset A
        MutableCurve dataA = new MutableCurve("Data A", 128, false, true);
        MutableCurve fitA = new MutableCurve("Fit A", 512, false, false);

        // Dataset B
        MutableCurve dataB = new MutableCurve("Data B", 128, false, true);
        MutableCurve fitB = new MutableCurve("Fit B", 512, false, false);

        Random rng = new Random(424242);

        // True params (two peaks)
        double A1 = 1.8e4, mu1 = 0.38, sig1 = 0.075;
        double A2 = 9.0e3, mu2 = 0.62, sig2 = 0.060;

        double sigmaY_A = 700.0;
        double sigmaY_B = 500.0;

        // Build observations for each
        WeightedObservedPoints obsA = new WeightedObservedPoints();
        WeightedObservedPoints obsB = new WeightedObservedPoints();

        for (int i = 0; i < n; i++) {
            double x = xMin + i * (xMax - xMin) / (n - 1);

            // A: peak + small baseline wiggle
            double yTrueA = gaussian(x, A1, mu1, sig1) + 300.0 * Math.sin(10 * x);
            double yNoisyA = yTrueA + sigmaY_A * rng.nextGaussian();
            dataA.add(x, yNoisyA, sigmaY_A);
            obsA.add(new WeightedObservedPoint(1.0 / (sigmaY_A * sigmaY_A), x, yNoisyA));

            // B: different peak + different baseline
            double yTrueB = gaussian(x, A2, mu2, sig2) + 150.0 * Math.cos(12 * x);
            double yNoisyB = yTrueB + sigmaY_B * rng.nextGaussian();
            dataB.add(x, yNoisyB, sigmaY_B);
            obsB.add(new WeightedObservedPoint(1.0 / (sigmaY_B * sigmaY_B), x, yNoisyB));
        }

        // Fit both independently
        double[] pA = GaussianCurveFitter.create().withMaxIterations(2000).fit(obsA.toList());
        double[] pB = GaussianCurveFitter.create().withMaxIterations(2000).fit(obsB.toList());

        double fitA_A = pA[0], fitA_mu = pA[1], fitA_sig = Math.abs(pA[2]);
        double fitB_A = pB[0], fitB_mu = pB[1], fitB_sig = Math.abs(pB[2]);

        FitResult frA = new FitResult(
                FitSpec.gaussian(),
                new double[] { fitA_A, fitA_mu, fitA_sig },
                xx -> gaussian(xx, fitA_A, fitA_mu, fitA_sig),
                xMin, xMax,
                null
        );

        FitResult frB = new FitResult(
                FitSpec.gaussian(),
                new double[] { fitB_A, fitB_mu, fitB_sig },
                xx -> gaussian(xx, fitB_A, fitB_mu, fitB_sig),
                xMin, xMax,
                null
        );

        // Sample fit curves
        int m = 400;
        for (int i = 0; i < m; i++) {
            double x = xMin + i * (xMax - xMin) / (m - 1);
            fitA.add(x, frA.f(x));
            fitB.add(x, frB.f(x));
        }

        // Attach fit results (keep on both for hover convenience)
        dataA.setFitResult(frA);
        fitA.setFitResult(frA);

        dataB.setFitResult(frB);
        fitB.setFitResult(frB);

        // Add in a good draw order (data then fit for each)
        plot.addCurve(dataA);
        plot.addCurve(fitA);
        plot.addCurve(dataB);
        plot.addCurve(fitB);

        return plot;
    }

    @Override
    public Plot2DRenderer buildRenderer() {
        PlotTheme theme = new PlotTheme()
                .setInwardTicks(true)
                .setDrawTopTicks(true)
                .setDrawRightTicks(true)
                .setLabelTopTicks(false)
                .setLabelRightTicks(false)
                .setDrawFrame(true)
                .setTickLabelFormatter(DefaultTickLabelFormatter.scientific(3));

        Plot2DRenderer r = new Plot2DRenderer();
        r.setTheme(theme);
        r.setDrawLegend(true);
        r.setDrawFitOverlay(false);

        // Styles: show data as points with different sizes; fits as lines
        r.setCurvePaintProvider(snap -> snap.hasSigmaY()
                ? CurvePaint.dataPoints(null, SymbolType.SQUARE, 7)          // data
                : CurvePaint.fitLine(null, LineStyle.SOLID, 1.0f));          // fit
;

        return r;
    }

    private static double gaussian(double x, double a, double mu, double sigma) {
        double z = (x - mu) / sigma;
        return a * Math.exp(-0.5 * z * z);
    }
}
