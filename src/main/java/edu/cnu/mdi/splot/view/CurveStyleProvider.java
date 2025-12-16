package edu.cnu.mdi.splot.view;

import edu.cnu.mdi.splot.model.CurveSnapshot;

/** Supplies per-curve view styling without polluting the model. */
@FunctionalInterface
public interface CurveStyleProvider {
    CurveStyle styleFor(CurveSnapshot curve);
}
