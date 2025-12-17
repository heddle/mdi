package edu.cnu.mdi.splot.demo;

import java.awt.geom.Rectangle2D;
import java.util.Random;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
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
 * Demonstrates a power-law fit by linearizing in log space:
 * ln(y) = ln(A) + n ln(x)
 *
 * The plot shows original (x, y) data and the fitted power-law curve y = A x^n.
 * This avoids requiring a true log axis in the renderer (for now).
 */
public class PowerLawExample extends AExamplePlot {

    @Override
    public String getId() {
        return "power-law";
    }

    @Override
    public String getDisplayName() {
        return "Power-law (fit via log-space)";
    }

    @Override
    public String getDescription() {
        return "Synthetic y = A x^n data (with noise), fit via ln(y)=ln(A)+n ln(x).";
    }

    @Override
    public Plot2D buildPlot() {
        Plot2D plot = new Plot2D();
        plot.setTitle("Power Law Demo");
        plot.setBoundsPolicy(BoundsPolicy.MANUAL);
        

        plot.getXAxis().setLabel("x");
        plot.getYAxis().setLabel("y");

        // Data curve (no error bars here; could add sigmaY similarly)
        MutableCurve data = new MutableCurve("Data", 256, false, false);
        MutableCurve fit = new MutableCurve("Power-law fit", 512, false, false);

        Random rng = new Random(20250101);

        // True params
        double trueA = 120.0;
        double trueN = -1.20;

        // Generate x in (xmin..xmax] avoiding 0 for logs
        int n = 50;
        double xViewMin = 0.0; // for view bounds
        double xMin = 0.05;
        double xMax = 1.3;
        
        plot.setViewBounds(new Rectangle2D.Double(
                0.0,        // xMin (force axis to start at 0)
                Double.NaN, // yMin = auto
                xMax - xViewMin, // width
                Double.NaN  // yMax = auto
        ));


        // multiplicative (log-normal-ish) noise for power laws
        double relNoise = 0.15;

        WeightedObservedPoints obs = new WeightedObservedPoints();

        for (int i = 0; i < n; i++) {
            double x = xMin * Math.pow(xMax / xMin, i / (double) (n - 1)); // log-spaced x
            double yTrue = trueA * Math.pow(x, trueN);

            double yNoisy = yTrue * (1.0 + relNoise * rng.nextGaussian());
            if (yNoisy <= 0) yNoisy = 1e-9;

            data.add(x, yNoisy);

            // Fit in log space: Y = b0 + b1 X where X=ln(x), Y=ln(y)
            double X = Math.log(x);
            double Y = Math.log(yNoisy);

            obs.add(new WeightedObservedPoint(1.0, X, Y));
        }

        // Linear polynomial fit in log space: degree 1
        double[] b = PolynomialCurveFitter.create(1)
                .withMaxIterations(1000)
                .fit(obs.toList());

        // Commons Math polynomial fitter returns coeffs [b0, b1]
        double b0 = b[0];
        double b1 = b[1];

        double fitA = Math.exp(b0);
        double fitN = b1;

        FitResult fr = new FitResult(
                FitSpec.custom("power-law", java.util.List.of("A", "n")),
                new double[] { fitA, fitN },
                xx -> fitA * Math.pow(xx, fitN),
                xMin, xMax,
                null
        );

        // Sample fit curve densely over same x span
        int m = 400;
        for (int i = 0; i < m; i++) {
            double x = xMin * Math.pow(xMax / xMin, i / (double) (m - 1));
            double y = fr.f(x);
            fit.add(x, y);
        }

        // Attach fit metadata for legend/dialog
        data.setFitResult(fr);
        fit.setFitResult(fr);

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

        r.setCurvePaintProvider(snap -> {
            String name = snap.getName().toLowerCase();
            boolean isFit = name.contains("fit");

            if (isFit) {
                return CurvePaint.fitLine(null, LineStyle.SOLID, 1.2f);
            }
            // data
            return CurvePaint.dataPoints(null, SymbolType.SQUARE, 7);
        });
        return r;
    }
}
