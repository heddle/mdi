package edu.cnu.mdi.splot.demo;

import java.awt.Color;
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

public class GaussianFitExample extends AExamplePlot {

    @Override
    public String getId() {
        return "gaussian-fit";
    }

    @Override
    public String getDisplayName() {
        return "Gaussian fit (data + error bars)";
    }

    @Override
    public String getDescription() {
        return "Synthetic Gaussian data with constant σy, fit using Apache Commons Math.";
    }

    @Override
    public Plot2D buildPlot() {
        Plot2D plot = new Plot2D();
        plot.setTitle("Gaussian Fit Demo");
        plot.setBoundsPolicy(BoundsPolicy.AUTO);

        plot.getXAxis().setLabel("x");
        plot.getYAxis().setLabel("Counts");

        MutableCurve data = new MutableCurve("Data", 128, false, true);

        double trueA = 2.0e4, trueMu = 0.40, trueSigma = 0.08;
        double sigmaY = 800.0;
        Random rng = new Random(12345);

        int n = 60;
        double xMin = 0.0, xMax = 1.0;

        WeightedObservedPoints obs = new WeightedObservedPoints();
        for (int i = 0; i < n; i++) {
            double x = xMin + i * (xMax - xMin) / (n - 1);
            double yTrue = gaussian(x, trueA, trueMu, trueSigma);
            double yNoisy = yTrue + sigmaY * rng.nextGaussian();

            data.add(x, yNoisy, sigmaY);

            double w = 1.0 / (sigmaY * sigmaY);
            obs.add(new WeightedObservedPoint(w, x, yNoisy));
        }

        double[] p = GaussianCurveFitter.create()
                .withMaxIterations(1000)
                .fit(obs.toList());

        double fitA = p[0], fitMu = p[1], fitSigma = Math.abs(p[2]);

        FitResult fitResult = new FitResult(
                FitSpec.gaussian(),
                new double[] { fitA, fitMu, fitSigma },
                xx -> gaussian(xx, fitA, fitMu, fitSigma),
                xMin, xMax,
                null
        );

        // Fit curve samples
        MutableCurve fit = new MutableCurve("Gaussian fit", 512, false, false);
        int m = 300;
        for (int i = 0; i < m; i++) {
            double x = xMin + i * (xMax - xMin) / (m - 1);
            fit.add(x, gaussian(x, fitA, fitMu, fitSigma));
        }

        // Keep FitResult on both for hover/right-click convenience
        data.setFitResult(fitResult);
        fit.setFitResult(fitResult);

        plot.addCurve(data);
        plot.addCurve(fit);

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

        r.setCurvePaintProvider(snap -> snap.hasSigmaY()
                ? CurvePaint.dataPoints(Color.red, SymbolType.SQUARE, 7)          // data
                : CurvePaint.fitLine(null, LineStyle.SOLID, 1.0f));          // fit
        return r;
    }

    private static double gaussian(double x, double a, double mu, double sigma) {
        double z = (x - mu) / sigma;
        return a * Math.exp(-0.5 * z * z);
    }
}
