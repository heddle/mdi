package edu.cnu.mdi.splot.view;

import edu.cnu.mdi.splot.model.CurveSnapshot;

@FunctionalInterface
public interface CurvePaintProvider {
    CurvePaint paintFor(CurveSnapshot snap);
}
